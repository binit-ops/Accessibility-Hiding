package com.example.hideaccessibility;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import com.example.hideaccessibility.hooks.HookSettings;
import com.example.hideaccessibility.hooks.HookAccessibilityManager;
import com.example.hideaccessibility.hooks.HookContentResolver;
import com.example.hideaccessibility.hooks.HookPackageManager;
import com.example.hideaccessibility.hooks.HookNativeDetection;
import com.example.hideaccessibility.config.ConfigManager;
import com.example.hideaccessibility.util.Logger;

public class MainHook implements IXposedHookLoadPackage {

    private static final String MODULE_PACKAGE = "com.example.hideaccessibility";
    private static final String PREFS_FILE = "module_config";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // Don't hook ourselves
        if (lpparam.packageName.equals(MODULE_PACKAGE)) {
            return;
        }

        // Don't hook system framework (too dangerous)
        if (lpparam.packageName.equals("android")) {
            return;
        }

        // Load configuration
        XSharedPreferences prefs = new XSharedPreferences(MODULE_PACKAGE, PREFS_FILE);
        prefs.makeWorldReadable();

        if (!prefs.getFile().exists()) {
            XposedBridge.log("[HideA11y] Config file not found, skipping");
            return;
        }

        ConfigManager config = new ConfigManager(prefs);

        if (!config.shouldHook(lpparam.packageName)) {
            Logger.debug("Skipping " + lpparam.packageName + " (not in target list)");
            return;
        }

        Logger.info("Initializing hooks for: " + lpparam.packageName +
                    " (v" + config.getVersion() + ")");

        try {
            // 1. Hook Settings.Secure queries
            new HookSettings(lpparam, config).install();
            Logger.info("✓ Settings.Secure hooks installed");

            // 2. Hook AccessibilityManager
            new HookAccessibilityManager(lpparam, config).install();
            Logger.info("✓ AccessibilityManager hooks installed");

            // 3. Hook ContentResolver for direct queries
            new HookContentResolver(lpparam, config).install();
            Logger.info("✓ ContentResolver hooks installed");

            // 4. Hook PackageManager for service discovery
            new HookPackageManager(lpparam, config).install();
            Logger.info("✓ PackageManager hooks installed");

            // 5. Hook native detection methods
            new HookNativeDetection(lpparam, config).install();
            Logger.info("✓ Native detection hooks installed");

            Logger.info("All hooks installed successfully for " + lpparam.packageName);

        } catch (Throwable t) {
            Logger.error("Failed to install hooks: " + t.getMessage());
            XposedBridge.log(t);
        }
    }
              }
