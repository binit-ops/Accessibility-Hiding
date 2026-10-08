package com.example.hideaccessibility.hooks;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.provider.Settings;

import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class HookSettings {

    private static final String TAG = "HideA11y-Settings";
    private final XC_LoadPackage.LoadPackageParam lpparam;
    private final ConfigManager config;

    public HookSettings(XC_LoadPackage.LoadPackageParam lpparam, ConfigManager config) {
        this.lpparam = lpparam;
        this.config = config;
    }

    public void install() {
        hookGetStringForUser();
        hookGetString();
        hookGetIntForUser();
        hookGetInt();
    }

    /**
     * Settings.Secure.getStringForUser(ContentResolver, String, int)
     * Primary method used by apps to check accessibility settings.
     */
    private void hookGetStringForUser() {
        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getStringForUser",
                android.content.ContentResolver.class,
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[1];
                        handleSecureStringQuery(key, param);
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getStringForUser hook failed: " + t.getMessage());
        }
    }

    /**
     * Settings.Secure.getString(ContentResolver, String)
     */
    private void hookGetString() {
        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getString",
                android.content.ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[1];
                        handleSecureStringQuery(key, param);
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getString hook failed: " + t.getMessage());
        }
    }

    /**
     * Settings.Secure.getIntForUser(ContentResolver, String, int, int)
     */
    private void hookGetIntForUser() {
        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getIntForUser",
                android.content.ContentResolver.class,
                String.class,
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[1];
                        handleSecureIntQuery(key, param);
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getIntForUser hook failed: " + t.getMessage());
        }
    }

    /**
     * Settings.Secure.getInt(ContentResolver, String, int)
     */
    private void hookGetInt() {
        try {
            XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getInt",
                android.content.ContentResolver.class,
                String.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String key = (String) param.args[1];
                        handleSecureIntQuery(key, param);
                    }
                }
            );
        } catch (Throwable t) {
            Logger.error(TAG, "getInt hook failed: " + t.getMessage());
        }
    }

    private void handleSecureStringQuery(String key, XC_MethodHook.MethodHookParam param) {
        if (key == null) return;

        switch (key) {
            case Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES:
                String result = (String) param.getResult();
                String filtered = config.filterServiceList(result);
                param.setResult(filtered);
                Logger.debug(TAG, "Filtered ENABLED_ACCESSIBILITY_SERVICES");
                break;

            case Settings.Secure.ACCESSIBILITY_ENABLED:
                if (config.isHidingAllServices()) {
                    param.setResult("0");
                    Logger.debug(TAG, "Set ACCESSIBILITY_ENABLED to 0");
                }
                break;

            case Settings.Secure.TOUCH_EXPLORATION_ENABLED:
                if (config.shouldHideTouchExploration()) {
                    param.setResult("0");
                }
                break;

            case "accessibility_display_magnification_enabled":
                if (config.isHidingAllServices()) {
                    param.setResult("0");
                }
                break;

            case Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS:
                String targets = (String) param.getResult();
                if (targets != null && !config.isHidingAllServices()) {
                    param.setResult(config.filterServiceList(targets));
                }
                break;
        }
    }

    private void handleSecureIntQuery(String key, XC_MethodHook.MethodHookParam param) {
        if (key == null) return;

        switch (key) {
            case Settings.Secure.ACCESSIBILITY_ENABLED:
                if (config.isHidingAllServices()) {
                    param.setResult(0);
                }
                break;

            case Settings.Secure.TOUCH_EXPLORATION_ENABLED:
                if (config.shouldHideTouchExploration()) {
                    param.setResult(0);
                }
                break;
        }
    }
              }
