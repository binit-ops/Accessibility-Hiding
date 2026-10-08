package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class HookAccessibilityManager {

    private static final String TAG = "HideA11y-AM";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ConfigManager config;

    public HookAccessibilityManager(XC_LoadPackage.LoadPackageParam lpparam, ConfigManager config) {
        this.lpparam = lpparam;
        this.config = config;
    }

    public void install() {
        hookGetEnabledAccessibilityServiceList();
        hookGetInstalledAccessibilityServiceList();
        hookIsEnabled();
        hookIsTouchExplorationEnabled();
        hookGetEnabledAccessibilityServiceListVariants();
    }

    /**
     * AccessibilityManager.getEnabledAccessibilityServiceList(int feedbackType)
     */
    private void hookGetEnabledAccessibilityServiceList() {
        try {
            XposedHelpers.findAndHookMethod(
                AccessibilityManager.class,
                "getEnabledAccessibilityServiceList",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        filterServiceListResult(param);
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getEnabledAccessibilityServiceList failed: " + t.getMessage());
        }
    }

    /**
     * AccessibilityManager.getInstalledAccessibilityServiceList()
     */
    private void hookGetInstalledAccessibilityServiceList() {
        try {
            XposedHelpers.findAndHookMethod(
                AccessibilityManager.class,
                "getInstalledAccessibilityServiceList",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        filterServiceListResult(param);
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getInstalledAccessibilityServiceList failed: " + t.getMessage());
        }
    }

    /**
     * Hook any additional service list method variants across Android versions
     */
    private void hookGetEnabledAccessibilityServiceListVariants() {
        try {
            // Some OEMs / Android versions have additional methods
            Class<?> amClass = AccessibilityManager.class;

            // Try hooking getAccessibilityServiceList (deprecated but still used)
            try {
                XposedHelpers.findAndHookMethod(
                    amClass, "getAccessibilityServiceList", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            filterServiceListResult(param);
                        }
                    }
                );
            } catch (Throwable ignored) {}

            // Hook sendAccessibilityEvent to potentially block event observation
            try {
                XposedHelpers.findAndHookMethod(
                    amClass, "sendAccessibilityEvent", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // Don't block events entirely - could cause crashes
                            // Just monitor
                        }
                    }
                );
            } catch (Throwable ignored) {}

        } catch (Throwable t) {
            Logger.error(TAG, "Variant hooks failed: " + t.getMessage());
        }
    }

    /**
     * AccessibilityManager.isEnabled()
     */
    private void hookIsEnabled() {
        try {
            XposedHelpers.findAndHookMethod(
                AccessibilityManager.class,
                "isEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (config.isHidingAllServices()) {
                            param.setResult(false);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "isEnabled hook failed: " + t.getMessage());
        }
    }

    /**
     * AccessibilityManager.isTouchExplorationEnabled()
     */
    private void hookIsTouchExplorationEnabled() {
        try {
            XposedHelpers.findAndHookMethod(
                AccessibilityManager.class,
                "isTouchExplorationEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (config.shouldHideTouchExploration()) {
                            param.setResult(false);
                        }
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "isTouchExplorationEnabled hook failed: " + t.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void filterServiceListResult(XC_MethodHook.MethodHookParam param) {
        List<AccessibilityServiceInfo> result =
            (List<AccessibilityServiceInfo>) param.getResult();

        if (result == null || result.isEmpty()) return;

        List<AccessibilityServiceInfo> filtered = new ArrayList<>();
        for (AccessibilityServiceInfo info : result) {
            String serviceId = info.getId();
            if (!config.isServiceHidden(serviceId)) {
                filtered.add(info);
            } else {
                Logger.debug(TAG, "Hid service: " + serviceId);
            }
        }

        param.setResult(filtered);
    }
}
