package com.example.hideaccessibility.ui;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hideaccessibility.R;
import com.example.hideaccessibility.util.PrefsUtils;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements ServiceAdapter.OnServiceToggleListener,
                   TargetAppAdapter.OnTargetToggleListener {

    private static final String PREFS_NAME = "module_config";

    private SharedPreferences prefs;

    private ProgressBar progressBar;
    private TextView statusText;
    private TextView servicesCountText;
    private TextView targetsCountText;
    private TextView servicesEmptyText;
    private TextView appsEmptyText;

    private ServiceAdapter serviceAdapter;
    private TargetAppAdapter targetAdapter;

    private MaterialSwitch hideAllSwitch;
    private MaterialSwitch touchExplorationSwitch;
    private MaterialSwitch verboseSwitch;
    private MaterialSwitch showSystemSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        PrefsUtils.makeWorldReadable(this, PREFS_NAME);

        initViews();
        loadConfig();
        loadDataAsync();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceAdapter != null) serviceAdapter.destroy();
        if (targetAdapter != null) targetAdapter.destroy();
    }

    private void save(SharedPreferences.Editor editor) {
        editor.commit();
        PrefsUtils.makeWorldReadable(this, PREFS_NAME);
    }

    private void initViews() {
        progressBar = findViewById(R.id.progress_loading);
        statusText = findViewById(R.id.text_status);
        servicesCountText = findViewById(R.id.text_services_count);
        targetsCountText = findViewById(R.id.text_targets_count);
        servicesEmptyText = findViewById(R.id.text_services_empty);
        appsEmptyText = findViewById(R.id.text_apps_empty);

        RecyclerView servicesRecycler = findViewById(R.id.recycler_services);
        RecyclerView targetsRecycler = findViewById(R.id.recycler_targets);

        servicesRecycler.setLayoutManager(new LinearLayoutManager(this));
        targetsRecycler.setLayoutManager(new LinearLayoutManager(this));

        serviceAdapter = new ServiceAdapter(this, getPackageManager());
        targetAdapter = new TargetAppAdapter(this, getPackageManager());

        servicesRecycler.setAdapter(serviceAdapter);
        targetsRecycler.setAdapter(targetAdapter);

        hideAllSwitch = findViewById(R.id.switch_hide_all);
        touchExplorationSwitch = findViewById(R.id.switch_touch_exploration);
        verboseSwitch = findViewById(R.id.switch_verbose);
        showSystemSwitch = findViewById(R.id.switch_show_system);

        hideAllSwitch.setOnCheckedChangeListener((b, checked) -> {
            save(prefs.edit().putBoolean("hide_all", checked));
            updateStatus();
        });

        touchExplorationSwitch.setOnCheckedChangeListener((b, checked) ->
                save(prefs.edit().putBoolean("hide_touch_exploration", checked)));

        verboseSwitch.setOnCheckedChangeListener((b, checked) ->
                save(prefs.edit().putBoolean("verbose_logging", checked)));

        showSystemSwitch.setOnCheckedChangeListener((b, checked) -> {
            save(prefs.edit().putBoolean("show_system_apps", checked));
            loadDataAsync(); // reload app list with new visibility filter
        });

        TextInputEditText searchServices = findViewById(R.id.input_search_services);
        TextInputEditText searchApps = findViewById(R.id.input_search_apps);

        searchServices.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                serviceAdapter.filter(s.toString());
                updateHeaders();
            }
        });

        searchApps.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                targetAdapter.filter(s.toString());
                updateHeaders();
            }
        });
    }

    private void loadConfig() {
        hideAllSwitch.setChecked(prefs.getBoolean("hide_all", false));
        touchExplorationSwitch.setChecked(prefs.getBoolean("hide_touch_exploration", false));
        verboseSwitch.setChecked(prefs.getBoolean("verbose_logging", false));
        showSystemSwitch.setChecked(prefs.getBoolean("show_system_apps", false));
    }

    private void loadDataAsync() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            List<ServiceAdapter.ServiceItem> services = queryServices();
            List<TargetAppAdapter.TargetAppItem> apps = queryApps();

            runOnUiThread(() -> {
                serviceAdapter.setItems(services);
                targetAdapter.setItems(apps);
                progressBar.setVisibility(View.GONE);
                updateStatus();
                updateHeaders();
            });
        }).start();
    }

    private List<ServiceAdapter.ServiceItem> queryServices() {
        List<ServiceAdapter.ServiceItem> items = new ArrayList<>();

        android.view.accessibility.AccessibilityManager am =
                (android.view.accessibility.AccessibilityManager)
                        getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return items;

        List<AccessibilityServiceInfo> installed =
                am.getInstalledAccessibilityServiceList();
        if (installed == null) return items;

        Set<String> hiddenSet = prefs.getStringSet("hidden_services", new HashSet<>());
        PackageManager pm = getPackageManager();

        for (AccessibilityServiceInfo info : installed) {
            String id = info.getId();
            String appName = "Unknown";
            try {
                String pkg = id.contains("/") ? id.split("/")[0] : id;
                appName = pm.getApplicationLabel(
                        pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception ignored) {}

            items.add(new ServiceAdapter.ServiceItem(id, appName, hiddenSet.contains(id)));
        }

        // Hidden services first, then alphabetical
        items.sort((a, b) -> {
            if (a.isHidden != b.isHidden) return a.isHidden ? -1 : 1;
            return a.appName.compareToIgnoreCase(b.appName);
        });

        return items;
    }

    private List<TargetAppAdapter.TargetAppItem> queryApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);

        boolean showSystem = prefs.getBoolean("show_system_apps", false);
        Set<String> targetSet = prefs.getStringSet("target_packages", new HashSet<>());

        List<TargetAppAdapter.TargetAppItem> items = new ArrayList<>();

        for (ApplicationInfo ai : apps) {
            String pkg = ai.packageName;
            boolean isTarget = targetSet.contains(pkg);

            // Hide non-launchable apps by default (cuts the junk you saw
            // like "aidlserverdemo") — targeted apps always stay visible
            if (!showSystem && !isTarget) {
                if (pm.getLaunchIntentForPackage(pkg) == null) continue;
            }

            String appName;
            try {
                appName = pm.getApplicationLabel(ai).toString();
            } catch (Exception e) {
                appName = pkg;
            }

            items.add(new TargetAppAdapter.TargetAppItem(pkg, appName, isTarget));
        }

        // Targeted apps first, then alphabetical
        items.sort((a, b) -> {
            if (a.isTarget != b.isTarget) return a.isTarget ? -1 : 1;
            return a.appName.compareToIgnoreCase(b.appName);
        });

        return items;
    }

    @Override
    public void onServiceToggled(String serviceId, boolean hidden) {
        Set<String> hiddenSet = new HashSet<>(
                prefs.getStringSet("hidden_services", new HashSet<>()));

        if (hidden) hiddenSet.add(serviceId);
        else hiddenSet.remove(serviceId);

        save(prefs.edit().putStringSet("hidden_services", hiddenSet));
        updateStatus();
        updateHeaders();
    }

    @Override
    public void onTargetToggled(String packageName, boolean enabled) {
        Set<String> targetSet = new HashSet<>(
                prefs.getStringSet("target_packages", new HashSet<>()));

        if (enabled) targetSet.add(packageName);
        else targetSet.remove(packageName);

        save(prefs.edit().putStringSet("target_packages", targetSet));
        updateStatus();
        updateHeaders();
    }

    private void updateStatus() {
        int targetCount = targetAdapter != null ? targetAdapter.getTargetCount() : 0;
        int hiddenCount = serviceAdapter != null ? serviceAdapter.getHiddenCount() : 0;

        if (targetCount == 0 && hiddenCount == 0) {
            statusText.setText(R.string.status_not_configured);
        } else {
            statusText.setText(getString(R.string.status_active, targetCount, hiddenCount));
        }
    }

    private void updateHeaders() {
        servicesCountText.setText(getString(R.string.services_count,
                serviceAdapter.getShownCount(),
                serviceAdapter.getTotalCount(),
                serviceAdapter.getHiddenCount()));

        targetsCountText.setText(getString(R.string.targets_count,
                targetAdapter.getTargetCount(),
                targetAdapter.getShownCount(),
                targetAdapter.getTotalCount()));

        servicesEmptyText.setVisibility(
                serviceAdapter.getShownCount() == 0 ? View.VISIBLE : View.GONE);
        appsEmptyText.setVisibility(
                targetAdapter.getShownCount() == 0 ? View.VISIBLE : View.GONE);
    }
        }
