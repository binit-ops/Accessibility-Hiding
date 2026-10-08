package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.Intent;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class HookPackageManager {

    private static final String TAG = "HideA11y-PM";
    private static final String A11Y_SERVICE_ACTION =
        "android.accessibilityservice.AccessibilityService";

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
    }

    /**
     * Apps can discover accessibility services by querying for the
     * ACCESSIBILITY_SERVICE intent action via PackageManager directly,
     * bypassing AccessibilityManager entirely.
     */
    private void hookQueryIntentServices() {
        try {
            Class<?> pmClass = XposedHelpers.findClass(
                "android.app.ApplicationPackageManager", lpparam.classLoader);

            // queryIntentServices(Intent, int)
            try {
                XposedHelpers.findAndHookMethod(
                    pmClass, "queryIntentServices",
                    Intent.class, int.class,
                    serviceListHook());
                Logger.debug(TAG, "Hooked queryIntentServices(Intent, int)");
            } catch (Throwable ignored) {
                // Method signature may vary across Android versions
            }

            // queryIntentServices(Intent, int, int) — userId variant (API 24+)
            try {
                XposedHelpers.findAndHookMethod(
                    pmClass, "queryIntentServices",
                    Intent.class, int.class, int.class,
                    serviceListHook());
                Logger.debug(TAG, "Hooked queryIntentServices(Intent, int, int)");
            } catch (Throwable ignored) {
            }

        } catch (Throwable t) {
            Logger.error(TAG, "queryIntentServices hook failed: " + t.getMessage());
        }
    }

    /**
     * Shared hook: filters hidden accessibility services out of
     * PackageManager service query results.
     */
    private XC_MethodHook serviceListHook() {
        return new XC_MethodHook() {
            @Override
            @SuppressWarnings("unchecked")
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object result = param.getResult();
                if (!(result instanceof List)) return;

                Intent intent = (Intent) param.args[0];
                if (intent == null) return;
                
                // Only filter accessibility service queries —
                // don't touch unrelated service lookups
                String action = intent.getAction();
                if (action == null || !A11Y_SERVICE_ACTION.equals(action)) {
                    return;
                }

                List<Object> list = (List<Object>) result;
                List<Object> filtered = new ArrayList<>();

                for (Object item : list) {
                    if (item instanceof ResolveInfo) {
                        ResolveInfo ri = (ResolveInfo) item;
                        if (ri.serviceInfo != null) {
                            String fullId = ri.serviceInfo.packageName
                                    + "/" + ri.serviceInfo.name;

                            if (config.isServiceHidden(fullId)) {
                                Logger.debug(TAG, "Hid service from PM query: " + fullId);
                                continue; // skip — this one is hidden
                            }
                        }
                    }
                    filtered.add(item);
                }

                if (filtered.size() != list.size()) {
                    param.setResult(filtered);
                }
            }
        };
    }
}
