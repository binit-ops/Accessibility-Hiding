package com.example.hideaccessibility.config;

import de.robv.android.xposed.XSharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final XSharedPreferences prefs;
    private final Set<String> hiddenServices;
    private final Set<String> targetPackages;
    private final boolean hideAll;
    private final boolean hideTouchExploration;
    private final boolean filterPackageManager;
    private final boolean verboseLogging;
    private final int configVersion;

    public ConfigManager(XSharedPreferences prefs) {
        this.prefs = prefs;
        this.prefs.reload();
        this.hiddenServices = safeGetStringSet("hidden_services");
        this.targetPackages = safeGetStringSet("target_packages");
        this.hideAll = prefs.getBoolean("hide_all", false);
        this.hideTouchExploration = prefs.getBoolean("hide_touch_exploration", false);
        this.filterPackageManager = prefs.getBoolean("filter_package_manager", true);
        this.verboseLogging = prefs.getBoolean("verbose_logging", false);
        this.configVersion = prefs.getInt("config_version", 1);
    }

    private Set<String> safeGetStringSet(String key) {
        Set<String> result = prefs.getStringSet(key, null);
        return result != null ? new HashSet<>(result) : new HashSet<>();
    }

    // ── Target checking ──

    public boolean shouldHook(String packageName) {
        return targetPackages.contains(packageName);
    }

    public boolean isHidingAllServices() {
        return hideAll;
    }

    public boolean shouldHideTouchExploration() {
        return hideTouchExploration;
    }

    public boolean shouldFilterPackageManager() {
        return filterPackageManager;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public int getVersion() {
        return configVersion;
    }

    // ── Service checking ──

    public boolean isServiceHidden(String serviceId) {
        if (hideAll) return true;
        if (serviceId == null || serviceId.isEmpty()) return false;

        for (String hidden : hiddenServices) {
            if (serviceId.contains(hidden) || hidden.contains(serviceId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Filter ENABLED_ACCESSIBILITY_SERVICES string.
     * Format: "com.pkg1/.Service1:com.pkg2/.Service2"
     */
    public String filterServiceList(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        if (hideAll) return "";

        String[] services = input.split(":");
        List<String> kept = new ArrayList<>();

        for (String service : services) {
            String trimmed = service.trim();
            if (!trimmed.isEmpty() && !isServiceHidden(trimmed)) {
                kept.add(trimmed);
            }
        }

        String result = String.join(":", kept);
        if (verboseLogging) {
            de.robv.android.xposed.XposedBridge.log(
                "[HideA11y] Filtered services: '" + input + "' -> '" + result + "'");
        }

        return result;
    }

    public Set<String> getHiddenServices() {
        return Collections.unmodifiableSet(hiddenServices);
    }

    public Set<String> getTargetPackages() {
        return Collections.unmodifiableSet(targetPackages);
    }
}
