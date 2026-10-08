package com.example.hideaccessibility.util;

import android.content.Context;

import java.io.File;

public class PrefsUtils {

    /**
     * Makes the SharedPreferences XML file readable by the Xposed hook
     * process (XSharedPreferences).
     *
     * MODE_WORLD_READABLE throws SecurityException on Android 7+,
     * so we set POSIX-style permissions on the file directly instead.
     *
     * - data dir:     traversable by others
     * - shared_prefs: traversable + readable by others
     * - prefs file:   readable by others
     */
    public static void makeWorldReadable(Context context, String prefsName) {
        try {
            File dataDir = new File(context.getApplicationInfo().dataDir);
            File sharedPrefsDir = new File(dataDir, "shared_prefs");
            File prefsFile = new File(sharedPrefsDir, prefsName + ".xml");

            dataDir.setExecutable(true, false);
            sharedPrefsDir.setExecutable(true, false);
            sharedPrefsDir.setReadable(true, false);

            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
                // Write access stays owner-only
                prefsFile.setWritable(true, true);
            }
        } catch (Throwable ignored) {
            // Never crash the UI over permission fixes
        }
    }
}
