# Keep Xposed entry point
-keep class com.example.hideaccessibility.MainHook
-keep class com.example.hideaccessibility.MainHook { *; }

# Keep hook classes (called via reflection)
-keep class com.example.hideaccessibility.hooks.** { *; }
-keep class com.example.hideaccessibility.config.** { *; }

# Keep Xposed API
-dontwarn de.robv.android.xposed.**

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
