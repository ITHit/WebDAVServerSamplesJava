package com.ithit.webdav.samples.androidfsstorage.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Objects;

/**
 * Implementation of the SQLLite extended attributes model.
 */
class DatabaseHandlerImpl extends SQLiteOpenHelper implements IDatabaseHandler {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "EXTRA";
    private static final String TABLE_NAME = "EXTRA_INFO";
    private static final String KEY_PATH = "path";
    private static final String KEY_ATTR = "attribute";
    private static final String KEY_INFO = "info";

    DatabaseHandlerImpl(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_INFO_TABLE = "CREATE TABLE " + TABLE_NAME + "("
                + KEY_PATH + " TEXT," + KEY_ATTR + " TEXT," + KEY_INFO + " TEXT" + ")";
        db.execSQL(CREATE_INFO_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP DATABASE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    @Override
    public long saveInfo(String path, String attribute, String content) {
        long res;
        if (Objects.equals(getInfo(path, attribute), "")) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(KEY_PATH, path);
            values.put(KEY_ATTR, attribute);
            values.put(KEY_INFO, content);
            res = db.insert(TABLE_NAME, null, values);
            db.close();
        } else {
            res = updateInfo(path, attribute, content);
        }
        return res;
    }

    private long updateInfo(String path, String attribute, String content) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_PATH, path);
        values.put(KEY_ATTR, attribute);
        values.put(KEY_INFO, content);
        int res = db.update(TABLE_NAME, values, KEY_PATH + " = ? AND " + KEY_ATTR + " = ?", new String[]{path, attribute});
        db.close();
        return res;
    }

    @Override
    public String getInfo(String path, String attribute) {
        SQLiteDatabase db = this.getReadableDatabase();
        String res = "";
        String selectQuery = "SELECT * FROM " + TABLE_NAME + " WHERE " + KEY_PATH + " = '" + path +
                "' AND " + KEY_ATTR + " = '" + attribute + "'";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            res = cursor.getString(2);
        }
        cursor.close();
        db.close();
        return res;
    }

    @Override
    public int deleteInfo(String path, String attribute) {
        SQLiteDatabase db = this.getWritableDatabase();
        int res = db.delete(TABLE_NAME, KEY_PATH + " = ? AND " + KEY_ATTR + " = ?", new String[]{path, attribute});
        db.close();
        return res;
    }

    @Override
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }
}
