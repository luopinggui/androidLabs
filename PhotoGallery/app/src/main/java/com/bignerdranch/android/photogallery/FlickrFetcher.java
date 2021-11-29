package com.bignerdranch.android.photogallery;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetcher {
    public static final String TAG = "FlickrFetcher";

    public byte[] getUrlBytes(String urlSpec) throws Exception{
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        String redirect = connection.getHeaderField("Location");
        if (redirect != null){
            connection = (HttpURLConnection)new URL(redirect).openConnection();
        }

        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() +
                        " :with "+
                        urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer,0,bytesRead);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec)throws Exception{
        return new String(getUrlBytes(urlSpec));
    }

    public List<MarsItem> fetchItems(String urlSpec){
        List<MarsItem> items = new ArrayList<>();
        try{
            String jsonString = getUrlString(urlSpec);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);
        }catch (Exception e){
            Log.i(TAG,"Failed!");
        }
        return items;
    }

    //取出每张图片的信息将从url获得的json转换成MarsItem对象，再将它们添加到List中
    private void parseItems(List<MarsItem> items,JSONObject jsonBody)
            throws Exception{
        JSONArray photoJsonArray = jsonBody.getJSONArray("photos");
        for(int i=0;i<photoJsonArray.length();i++){
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            MarsItem item = new MarsItem();
            item.setId(photoJsonObject.getString("id"));
            item.setUrl(photoJsonObject.getString("img_src"));
            items.add(item);
        }
    }
}
