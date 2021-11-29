package com.bignerdranch.android.photogallery;


import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.ConcurrentHashMap;

public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;//标识下载请求信息

    private Boolean mHasQuit = false;
    private Handler mRequestHandler;   //下载图片的handler，自己创建
    private Handler mResponseHandler;   //回复设置UI的handler，由主线程传过来，其实是主线程的handler
    private ConcurrentHashMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{   //抽象类
        void onThumbnailDownload(T target,Bitmap thumbnail,byte[] bytes,String url);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler requestHandler){
        super(TAG);
        mResponseHandler = requestHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler(){  //handler处理一个就下载一个显示一个
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what==MESSAGE_DOWNLOAD){
                    T target = (T)msg.obj;
                    Log.i(TAG,"Got a request for URL: "+
                            mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * PhotoAdapter在其onBindViewHolder(...)实现方法中会调用此方法
     * @param target 标识具体哪次下载
     * @param url   下载链接
     */
    public void queueThumbnail(T target,String url){ //建立message,并发送给handler处理，handler会下载，并且调用responsehandler显示
        Log.i(TAG,"Got a url: "+url);

        if(url == null){
            mRequestMap.remove(target);
        }else{
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    //handleRequest()方法是下载执行的地方，确认URL有效后，就将它传递给FlickrFetchr新实例。
    private void handleRequest(final T target){   //下载图片
        try{
            final String url = mRequestMap.get(target);

            if(url==null){
                return;
            }

            final byte[] bitmapBytes = new com.bignerdranch.android.photogallery.FlickrFetcher().getUrlBytes(url);
            //使用BitmapFactory把getUrlBytes(...)返回的字节数组转换为位图
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);
            Log.i(TAG,"Bitmap created");

            mResponseHandler.post(new Runnable() {      //post新建消息，传给handler处理，其实是在主线程中完成
                @Override
                public void run() {
                    if(mRequestMap.get(target)!=url||mHasQuit){
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownload(target,bitmap,bitmapBytes,url);
                }
            });
        }catch (Exception e){
            Log.e(TAG,"Error downloading image",e);
        }
    }
}
