package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.ContentResolver;
import android.database.Cursor;
import android.database.CursorWrapper;
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

                        if (uri.toString().contains("settings/secure")) {
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
     * FilteredCursor extends CursorWrapper, which already implements and
     * delegates ALL Cursor methods (including requery, deactivate, getType).
     * We only override the two we need to modify.
     */
    private static class FilteredCursor extends CursorWrapper {

        private final ConfigManager mConfig;

        FilteredCursor(Cursor cursor, ConfigManager config) {
            super(cursor);
            mConfig = config;
        }

        @Override
        public String getString(int columnIndex) {
            String value = super.getString(columnIndex);

            int nameIndex = getColumnIndex("name");
            if (nameIndex >= 0 && columnIndex == getColumnIndex("value")) {
                String name = super.getString(nameIndex);
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
            // Route through our filtered getString() so integer reads
            // also see the modified values
            String value = getString(columnIndex);
            try {
                return value != null ? Integer.parseInt(value) : super.getInt(columnIndex);
            } catch (NumberFormatException e) {
                return super.getInt(columnIndex);
            }
        }
    }
}
