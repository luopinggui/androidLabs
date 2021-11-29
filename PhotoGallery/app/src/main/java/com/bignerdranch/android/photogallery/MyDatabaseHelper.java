package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    //创建表 1 定义变量
    public static final String CREATE_NASA = "create table nasa ("
            + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "url text,image BLOB)";

    //数据库的创建
    public MyDatabaseHelper(Context context, String name,
                            SQLiteDatabase.CursorFactory factory, int version){
        super(context,name,factory,version);
    }

    //创建表 2 执行操作
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NASA);
    }
    //数据库升级
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists nasa");
        onCreate(db);
    }
}
