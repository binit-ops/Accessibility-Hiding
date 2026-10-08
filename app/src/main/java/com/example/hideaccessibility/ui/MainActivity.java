package com.example.hideaccessibility.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hideaccessibility.R;
import com.example.hideaccessibility.util.PrefsUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements ServiceAdapter.OnServiceToggleListener,
                   TargetAppAdapter.OnTargetToggleListener {

    private static final String PREFS_NAME = "module_config";

    private SharedPreferences prefs;
    private RecyclerView servicesRecycler;
    private RecyclerView targetsRecycler;
    private ServiceAdapter serviceAdapter;
    private TargetAppAdapter targetAdapter;
    private Switch hideAllSwitch;
    private Switch touchExplorationSwitch;
    private Switch verboseSwitch;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // MODE_PRIVATE + manual chmod — MODE_WORLD_READABLE
        // throws SecurityException on Android 7+
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        PrefsUtils.makeWorldReadable(this, PREFS_NAME);

        initViews();
        loadConfig();
        loadServices();
        loadTargetApps();
    }

    /**
     * Synchronous save, then re-apply world-readable permissions
     * so XSharedPreferences in the hook process can read the file.
     */
    private void save(SharedPreferences.Editor editor) {
        editor.commit();
        PrefsUtils.makeWorldReadable(this, PREFS_NAME);
    }

    private void initViews() {
        servicesRecycler = findViewById(R.id.recycler_services);
        targetsRecycler = findViewById(R.id.recycler_targets);
        hideAllSwitch = findViewById(R.id.switch_hide_all);
        touchExplorationSwitch = findViewById(R.id.switch_touch_exploration);
        verboseSwitch = findViewById(R.id.switch_verbose);
        statusText = findViewById(R.id.text_status);

        servicesRecycler.setLayoutManager(new LinearLayoutManager(this));
        targetsRecycler.setLayoutManager(new LinearLayoutManager(this));

        serviceAdapter = new ServiceAdapter(this);
        targetAdapter = new TargetAppAdapter(this);

        servicesRecycler.setAdapter(serviceAdapter);
        targetsRecycler.setAdapter(targetAdapter);

        hideAllSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            save(prefs.edit().putBoolean("hide_all", isChecked));
            updateStatus();
        });

        touchExplorationSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            save(prefs.edit().putBoolean("hide_touch_exploration", isChecked)));

        verboseSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
            save(prefs.edit().putBoolean("verbose_logging", isChecked)));
    }

    private void loadConfig() {
        hideAllSwitch.setChecked(prefs.getBoolean("hide_all", false));
        touchExplorationSwitch.setChecked(prefs.getBoolean("hide_touch_exploration", false));
        verboseSwitch.setChecked(prefs.getBoolean("verbose_logging", false));
    }

    private void loadServices() {
        android.view.accessibility.AccessibilityManager am =
            (android.view.accessibility.AccessibilityManager)
                getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> installed =
            am.getInstalledAccessibilityServiceList();

        if (installed == null) return;

        Set<String> hiddenSet = prefs.getStringSet("hidden_services", new HashSet<>());

        List<ServiceAdapter.ServiceItem> items = new ArrayList<>();
        for (AccessibilityServiceInfo info : installed) {
            String id = info.getId();
            String appName = "Unknown";
            try {
                String pkg = id.split("/")[0];
                appName = getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(pkg, 0)).toString();
            } catch (Exception ignored) {}

            boolean isHidden = hiddenSet.contains(id);
            items.add(new ServiceAdapter.ServiceItem(id, appName, isHidden));
        }

        serviceAdapter.setItems(items);
    }

    private void loadTargetApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        Set<String> targetSet = prefs.getStringSet("target_packages", new HashSet<>());

        List<TargetAppAdapter.TargetAppItem> items = new ArrayList<>();
        for (ApplicationInfo appInfo : apps) {
            String appName = pm.getApplicationLabel(appInfo).toString();
            String packageName = appInfo.packageName;
            boolean isTarget = targetSet.contains(packageName);
            items.add(new TargetAppAdapter.TargetAppItem(packageName, appName, isTarget));
        }

        // Sort: targeted apps first, then alphabetical
        items.sort((a, b) -> {
            if (a.isTarget != b.isTarget) return a.isTarget ? -1 : 1;
            return a.appName.compareToIgnoreCase(b.appName);
        });

        targetAdapter.setItems(items);
    }

    @Override
    public void onServiceToggled(String serviceId, boolean hidden) {
        Set<String> hiddenSet = new HashSet<>(
            prefs.getStringSet("hidden_services", new HashSet<>()));

        if (hidden) {
            hiddenSet.add(serviceId);
        } else {
            hiddenSet.remove(serviceId);
        }

        save(prefs.edit().putStringSet("hidden_services", hiddenSet));
        updateStatus();
    }

    @Override
    public void onTargetToggled(String packageName, boolean enabled) {
        Set<String> targetSet = new HashSet<>(
            prefs.getStringSet("target_packages", new HashSet<>()));

        if (enabled) {
            targetSet.add(packageName);
        } else {
            targetSet.remove(packageName);
        }

        save(prefs.edit().putStringSet("target_packages", targetSet));
        updateStatus();
    }

    private void updateStatus() {
        int targetCount = targetAdapter != null ? targetAdapter.getTargetCount() : 0;
        int hiddenCount = serviceAdapter != null ? serviceAdapter.getHiddenCount() : 0;

        statusText.setText(String.format(
            "Active in %d apps | Hiding %d services",
            targetCount, hiddenCount));
    }
        }
