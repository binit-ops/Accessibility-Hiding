package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class HookPackageManager {

    private static final String TAG = "HideA11y-PM";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ConfigManager config;

    public HookPackageManager(XC_LoadPackage.LoadPackageParam lpparam, ConfigManager config) {
        this.lpparam = lpparam;
        this.config = config;
    }

    public void install() {
        if (!config.shouldFilterPackageManager()) {
            return;
        }
        hookQueryIntentServices();
        hookGetInstalledPackages();
        hookGetPackageInfo();
    }

    /**
     * Hook PackageManager.queryIntentServices for ACCESSIBILITY_SERVICE intent
     */
    private void hookQueryIntentServices() {
        try {
            // Hook the abstract method on PackageManager
            Class<?> pmClass = XposedHelpers.findClass(
                "android.app.ApplicationPackageManager", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                pmClass,
                "queryIntentServices",
                Intent.class, int.class,
                new XC_MethodHookMethod())
                : null;

        } catch (Throwable t) {
            Logger.error(TAG, "queryIntentServices hook failed: " + t.getMessage());
        }
    }

    private XC_MethodHook serviceListHook() {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (result instanceof List) {
                    List<Object> list = (List<Object>) result;
                    List<Object> filtered = new ArrayList<>();

                    for (Object item : list) {
                        if (item instanceof ResolveInfo) {
                            ResolveInfo ri = (ResolveInfo) item;
                            if (ri.serviceInfo != null) {
                                String packageName = ri.serviceInfo.packageName;
                                String className = ri.serviceInfo.name;
                                String fullId = packageName + "/" + className;

                                if (!config.isServiceHidden(fullId)) {
                                    filtered.add(item);
                                } else {
                                    Logger.debug(TAG, "Hid service from query: " + fullId);
                                }
                            } else {
                                filtered.add(item);
                            }
                        } else {
                            filtered.add(item);
                        }
                    }

                    param.setResult(filtered);
                }
            }
        };
    }

    /**
     * Hook getInstalledPackages to potentially hide our module package
     */
    private void hookGetInstalledPackages() {
        try {
            Class<?> pmClass = XposedHelpers.findClass(
                "android.app.ApplicationPackageManager", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                pmClass,
                "getInstalledPackages",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // We could filter out our module package here
                        // But this is aggressive and may break things
                        // Only filter if specifically configured
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getInstalledPackages hook failed: " + t.getMessage());
        }
    }

    /**
     * Hook getPackageInfo to hide our module if queried directly
     */
    private void hookGetPackageInfo() {
        try {
            Class<?> pmClass = XposedHelpers.findClass(
                "android.app.ApplicationPackageManager", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(
                pmClass,
                "getPackageInfo",
                String.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String packageName = (String) param.args[0];
                        // Could throw NameNotFoundException for our module package
                        // This is aggressive; use with caution
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getPackageInfo hook failed: " + t.getMessage());
        }
    }
}
