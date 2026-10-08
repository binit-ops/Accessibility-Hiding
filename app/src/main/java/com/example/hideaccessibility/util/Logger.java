package com.example.hideaccessibility.util;

import de.robv.android.xposed.XposedBridge;

public class Logger {

    private static final String PREFIX = "[HideA11y] ";
    private static boolean verbose = false;

    public static void setVerbose(boolean enabled) {
        verbose = enabled;
    }

    public static void info(String message) {
        XposedBridge.log(PREFIX + message);
    }

    public static void info(String tag, String message) {
        XposedBridge.log(PREFIX + tag + ": " + message);
    }

    public static void debug(String message) {
        if (verbose) {
            XposedBridge.log(PREFIX + "DEBUG: " + message);
        }
    }

    public static void debug(String tag, String message) {
        if (verbose) {
            XposedBridge.log(PREFIX + tag + " DEBUG: " + message);
        }
    }

    public static void error(String message) {
        XposedBridge.log(PREFIX + "ERROR: " + message);
    }

    public static void error(String tag, String message) {
        XposedBridge.log(PREFIX + tag + " ERROR: " + message);
    }

    public static void error(String tag, String message, Throwable t) {
        XposedBridge.log(PREFIX + tag + " ERROR: " + message);
        XposedBridge.log(t);
    }
}
