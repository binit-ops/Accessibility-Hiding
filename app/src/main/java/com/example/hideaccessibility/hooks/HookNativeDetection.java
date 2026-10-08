package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.lang.reflect.Method;

import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class HookNativeDetection {

    private static final String TAG = "HideA11y-Native";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ConfigManager config;

    // Common file paths checked for root/accessibility detection
    private static final String[] HIDDEN_PATHS = {
        "/proc/self/maps",      // Checking loaded libraries
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        "/data/local/xposed",
        "/data/local/lspd",
    };

    public HookNativeDetection(XC_LoadPackage.LoadPackageParam lpparam, ConfigManager config) {
        this.lpparam = lpparam;
        this.config = config;
    }

    public void install() {
        hookFileExists();
        hookSystemProperties();
        hookRuntimeExec();
    }

    /**
     * Hook File.exists() to hide specific paths
     */
    private void hookFileExists() {
        try {
            XposedHelpers.findAndHookMethod(
                File.class,
                "exists",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        File file = (File) param.thisObject;
                        String path = file.getAbsolutePath();

                        // Hide LSPosed/Xposed artifacts
                        if (path.contains("lspd") ||
                            path.contains("xposed") ||
                            path.contains("riru") ||
                            path.contains("zygisk")) {
                            param.setResult(false);
                            Logger.debug(TAG, "Hid file: " + path);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "File.exists hook failed: " + t.getMessage());
        }
    }

    /**
     * Hook SystemProperties.get() to hide Xposed-related properties
     */
    private void hookSystemProperties() {
        try {
            Class<?> sysPropClass = XposedHelpers.findClass(
                "android.os.SystemProperties", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                sysPropClass,
                "get",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[0];
                        String result = (String) param.getResult();

                        // Hide Xposed-related system properties
                        if (key != null && result != null) {
                            if (key.contains("xposed") ||
                                key.contains("lsposed") ||
                                key.contains("riru") ||
                                key.contains("zygisk")) {
                                param.setResult("");
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "SystemProperties hook failed: " + t.getMessage());
        }
    }

    /**
     * Hook Runtime.exec() to intercept command-line detection
     */
    private void hookRuntimeExec() {
        try {
            Class<?> runtimeClass = Runtime.class;

            XposedHelpers.findAndHookMethod(
                runtimeClass,
                "exec",
                String[].class, String[].class, File.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String[] cmdArray = (String[]) param.args[0];
                        if (cmdArray == null) return;

                        for (String cmd : cmdArray) {
                            if (cmd != null && (cmd.contains("accessibility") ||
                                cmd.contains("getprop") ||
                                cmd.contains("settings"))) {
                                Logger.debug(TAG, "Intercepted exec: " + cmd);
                                // Could modify or allow through
                                break;
                            }
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "Runtime.exec hook failed: " + t.getMessage());
        }
    }
}
