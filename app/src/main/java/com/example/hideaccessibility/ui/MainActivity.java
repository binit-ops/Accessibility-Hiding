package com.example.hideaccessibility.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hideaccessibility.R;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
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

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_WORLD_READABLE);

        initViews();
        loadConfig();
        loadServices();
        loadTargetApps();
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
            prefs.edit().putBoolean("hide_all", isChecked).apply();
            updateStatus();
        });

        touchExplorationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("hide_touch_exploration", isChecked).apply();
        });

        verboseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("verbose_logging", isChecked).apply();
        });
    }

    private void loadConfig() {
        hideAllSwitch.setChecked(prefs.getBoolean("hide_all", false));
        touchExplorationSwitch.setChecked(prefs.getBoolean("hide_touch_exploration", false));
        verboseSwitch.setChecked(prefs.getBoolean("verbose_logging", false));
    }

    private void loadServices() {
        // List all accessibility services on the device
        android.view.accessibility.AccessibilityManager am =
            (android.view.accessibility.AccessibilityManager)
                getSystemService(Context.ACCESSIBILITY_SERVICE);

        List<AccessibilityServiceInfo> installed =
            am.getInstalledAccessibilityServiceList();

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
        // Show installed apps for selecting targets
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        Set<String> targetSet = prefs.getStringSet("target_packages", new HashSet<>());

        List<TargetAppAdapter.TargetAppItem> items = new ArrayList<>();
        for (ApplicationInfo appInfo : apps) {
            // Skip system apps unless explicitly wanted
            String appName = pm.getApplicationLabel(appInfo).toString();
            String packageName = appInfo.packageName;
            boolean isTarget = targetSet.contains(packageName);
            items.add(new TargetAppAdapter.TargetAppItem(packageName, appName, isTarget));
        }

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

        prefs.edit().putStringSet("hidden_services", hiddenSet).apply();
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

        prefs.edit().putStringSet("target_packages", targetSet).apply();
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
