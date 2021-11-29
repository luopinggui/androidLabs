package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {
    private static final String TAG = "PhotoFragment";
    private RecyclerView mPhotoRecyclerView;
    private List<MarsItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private SQLiteDatabase mDatabase;
    private static final int VERSION = 1;
    private static final String NAME = "nasa";

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();  //异步执行，获取网站上的json内容

        mDatabase = new MyDatabaseHelper(getActivity(),NAME,null,VERSION).getWritableDatabase();

        Handler responseHandler = new Handler(); //创建主线程的handler，负责在IU上显示HandlerThread线程下载的图片
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownload(PhotoHolder target, Bitmap thumbnail,byte[] bytes,String url) {
                ContentValues values = new ContentValues();    //将图片的二进制形式插入数据库
                values.put("url",url);
                values.put("image",bytes);
                mDatabase.insert("nasa",null,values);
                Drawable drawable = new BitmapDrawable(getResources(),thumbnail);
                target.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();    //实例化并开始Downloder,并且实例化了抽象类Listener
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery,container,false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),3));

        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        mThumbnailDownloader.clearQueue();
        Log.i(TAG,"Background thread destroyed");
    }

    private class FetchItemsTask extends AsyncTask<Void,Void,List<MarsItem>>{
        @Override
        protected List<MarsItem> doInBackground(Void... voids) {
            return new FlickrFetcher().fetchItems("https://api.nasa.gov/mars-photos/api/v1/rovers/curiosity/photos?sol=1000&api_key=DEMO_KEY");
        }

        @Override
        protected void onPostExecute(List<MarsItem> marsItems) {
            mItems = marsItems;
            setupAdapter();
        }
    }

    public class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView mItemImageView;

        //在构造方法中实例化视图组件 ImageView
        public PhotoHolder(View itemView){
            super(itemView);
            mItemImageView = (ImageView)itemView.findViewById(R.id.mars_item_image_view);
        }
        //每次有新的drawable要在PhotoHolder中显示时，都要调用它一次。
        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdaper extends RecyclerView.Adapter<PhotoHolder>{
        private List<MarsItem> mItems;

        public PhotoAdaper(List<MarsItem> items){
            mItems = items;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //加载布局 三个参数：布局ID 父布局 是否直接添加到父布局
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_mars,parent,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            MarsItem marsItem = mItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.jianfei);
            holder.bindDrawable(placeholder);
            //mThumbnailDownloader.queueThumbnail(holder,marsItem.getUrl());
            Cursor cursor = mDatabase.query(
                    "nasa",
                    null,
                    "url = ? ",
                    new String[] {marsItem.getUrl()},
                    null,
                    null,
                    null
            );
            if(cursor.getCount() == 0){   //如果数据库中没有该MarsItem的图片
                mThumbnailDownloader.queueThumbnail(holder,marsItem.getUrl());   //建立message,并发送给handler处理
            }
            else{    //如果数据库中存在，取出二进制数组，绑定在UI
                cursor.moveToFirst();
                @SuppressLint("Range") byte[] bytes = cursor.getBlob(cursor.getColumnIndex("image"));
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                Drawable drawable = new BitmapDrawable(getResources(),bitmap);
                holder.bindDrawable(drawable);
            }
            cursor.close();
        }
    }

    //将Adapter和RecyclerView关联起来
    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecyclerView.setAdapter(new PhotoAdaper(mItems));
        }
    }
}
