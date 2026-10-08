package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class HookContentResolver {

    private static final String TAG = "HideA11y-CR";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ConfigManager config;

    public HookContentResolver(XC_LoadPackage.LoadPackageParam lpparam, ConfigManager config) {
        this.lpparam = lpparam;
        this.config = config;
    }

    public void install() {
        hookQuery();
    }

    /**
     * Hook ContentResolver.query() to intercept direct settings queries.
     * Some apps query settings/secure URI directly instead of using Settings API.
     */
    private void hookQuery() {
        try {
            XposedHelpers.findAndHookMethod(
                ContentResolver.class,
                "query",
                Uri.class, String[].class, String.class,
                String[].class, String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Uri uri = (Uri) param.args[0];
                        if (uri == null) return;

                        String uriString = uri.toString();

                        // Check if querying secure settings
                        if (uriString.contains("settings/secure")) {
                            Cursor cursor = (Cursor) param.getResult();
                            if (cursor != null) {
                                param.setResult(new FilteredCursor(cursor, config));
                                Logger.debug(TAG, "Wrapped secure settings cursor");
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "query hook failed: " + t.getMessage());
        }
    }

    /**
     * FilteredCursor wraps a cursor and modifies accessibility-related values
     */
    private static class FilteredCursor implements Cursor {

        private final Cursor mCursor;
        private final ConfigManager mConfig;

        FilteredCursor(Cursor cursor, ConfigManager config) {
            mCursor = cursor;
            mConfig = config;
        }

        @Override
        public String getString(int columnIndex) {
            String value = mCursor.getString(columnIndex);

            int nameIndex = mCursor.getColumnIndex("name");
            if (nameIndex >= 0 && columnIndex == mCursor.getColumnIndex("value")) {
                String name = mCursor.getString(nameIndex);
                if (name != null) {
                    switch (name) {
                        case "enabled_accessibility_services":
                            return mConfig.filterServiceList(value);
                        case "accessibility_enabled":
                            return mConfig.isHidingAllServices() ? "0" : value;
                        case "touch_exploration_enabled":
                            return mConfig.shouldHideTouchExploration() ? "0" : value;
                    }
                }
            }

            return value;
        }

        @Override
        public int getInt(int columnIndex) {
            String value = getString(columnIndex);
            try {
                return value != null ? Integer.parseInt(value) : 0;
            } catch (NumberFormatException e) {
                return mCursor.getInt(columnIndex);
            }
        }

        // ── Delegate all other methods ──

        @Override public int getCount() { return mCursor.getCount(); }
        @Override public int getPosition() { return mCursor.getPosition(); }
        @Override public boolean move(int offset) { return mCursor.move(offset); }
        @Override public boolean moveToPosition(int position) { return mCursor.moveToPosition(position); }
        @Override public boolean moveToFirst() { return mCursor.moveToFirst(); }
        @Override public boolean moveToLast() { return mCursor.moveToLast(); }
        @Override public boolean moveToNext() { return mCursor.moveToNext(); }
        @Override public boolean moveToPrevious() { return mCursor.moveToPrevious(); }
        @Override public boolean isFirst() { return mCursor.isFirst(); }
        @Override public boolean isLast() { return mCursor.isLast(); }
        @Override public boolean isBeforeFirst() { return mCursor.isBeforeFirst(); }
        @Override public boolean isAfterLast() { return mCursor.isAfterLast(); }
        @Override public int getColumnIndex(String columnName) { return mCursor.getColumnIndex(columnName); }
        @Override public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException { return mCursor.getColumnIndexOrThrow(columnName); }
        @Override public String getColumnName(int columnIndex) { return mCursor.getColumnName(columnIndex); }
        @Override public String[] getColumnNames() { return mCursor.getColumnNames(); }
        @Override public int getColumnCount() { return mCursor.getColumnCount(); }
        @Override public byte[] getBlob(int columnIndex) { return mCursor.getBlob(columnIndex); }
        @Override public float getFloat(int columnIndex) { return mCursor.getFloat(columnIndex); }
        @Override public long getLong(int columnIndex) { return mCursor.getLong(columnIndex); }
        @Override public short getShort(int columnIndex) { return mCursor.getShort(columnIndex); }
        @Override public double getDouble(int columnIndex) { return mCursor.getDouble(columnIndex); }
        @Override public boolean isNull(int columnIndex) { return mCursor.isNull(columnIndex); }
        @Override public boolean isClosed() { return mCursor.isClosed(); }
        @Override public void close() { mCursor.close(); }
        @Override public void registerContentObserver(android.database.ContentObserver observer) { mCursor.registerContentObserver(observer); }
        @Override public void unregisterContentObserver(android.database.ContentObserver observer) { mCursor.unregisterContentObserver(observer); }
        @Override public void registerDataSetObserver(android.database.DataSetObserver observer) { mCursor.registerDataSetObserver(observer); }
        @Override public void unregisterDataSetObserver(android.database.DataSetObserver observer) { mCursor.unregisterDataSetObserver(observer); }
        @Override public void setNotificationUri(ContentResolver cr, Uri uri) { mCursor.setNotificationUri(cr, uri); }
        @Override public Uri getNotificationUri() { return mCursor.getNotificationUri(); }
        @Override public boolean getWantsAllOnMoveCalls() { return mCursor.getWantsAllOnMoveCalls(); }
        @Override public void setExtras(android.os.Bundle extras) { mCursor.setExtras(extras); }
        @Override public android.os.Bundle getExtras() { return mCursor.getExtras(); }
        @Override public android.os.Bundle respond(android.os.Bundle extras) { return mCursor.respond(extras); }

        @Deprecated
        @Override public void setNotificationUri(ContentResolver cr, Uri notifyUri, boolean syncToNetwork) {
            mCursor.setNotificationUri(cr, notifyUri, syncToNetwork);
        }
    }
}
