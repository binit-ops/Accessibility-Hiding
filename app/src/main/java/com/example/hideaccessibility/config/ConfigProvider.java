package com.example.hideaccessibility.config;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.content.Context;

public class ConfigProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.hideaccessibility.config";
    public static final String PREFS_NAME = "module_config";

    private SharedPreferences prefs;

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);
        }
        return prefs != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                       String[] selectionArgs, String sortOrder) {
        if (prefs == null) return null;

        MatrixCursor cursor = new MatrixCursor(new String[]{"key", "value"});

        if (selectionArgs != null && selectionArgs.length > 0) {
            String key = selectionArgs[0];
            String value = prefs.getString(key, null);
            if (value != null) {
                cursor.addRow(new Object[]{key, value});
            }
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.com.example.hideaccessibility.config";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
