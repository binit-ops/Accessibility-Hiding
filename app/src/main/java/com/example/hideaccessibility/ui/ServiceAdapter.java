package com.example.hideaccessibility.ui;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hideaccessibility.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    public interface OnServiceToggleListener {
        void onServiceToggled(String serviceId, boolean hidden);
    }

    private final List<ServiceItem> allItems = new ArrayList<>();
    private final List<ServiceItem> items = new ArrayList<>();
    private final OnServiceToggleListener listener;
    private final PackageManager pm;

    private final Map<String, Drawable> iconCache = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService iconExecutor = Executors.newSingleThreadExecutor();
    private String query = "";

    public static class ServiceItem {
        public final String serviceId;
        public final String appName;
        public boolean isHidden;

        public ServiceItem(String serviceId, String appName, boolean isHidden) {
            this.serviceId = serviceId;
            this.appName = appName;
            this.isHidden = isHidden;
        }
    }

    public ServiceAdapter(OnServiceToggleListener listener, PackageManager pm) {
        this.listener = listener;
        this.pm = pm;
    }

    public void setItems(List<ServiceItem> newItems) {
        allItems.clear();
        allItems.addAll(newItems);
        applyFilter();
    }

    public void filter(String newQuery) {
        query = newQuery == null ? "" : newQuery.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private void applyFilter() {
        items.clear();
        if (query.isEmpty()) {
            items.addAll(allItems);
        } else {
            for (ServiceItem it : allItems) {
                if (it.appName.toLowerCase(Locale.ROOT).contains(query)
                        || it.serviceId.toLowerCase(Locale.ROOT).contains(query)) {
                    items.add(it);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getShownCount() { return items.size(); }
    public int getTotalCount() { return allItems.size(); }

    public int getHiddenCount() {
        int count = 0;
        for (ServiceItem item : allItems) {
            if (item.isHidden) count++;
        }
        return count;
    }

    public void destroy() {
        iconExecutor.shutdown();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceItem item = items.get(position);

        holder.textServiceName.setText(item.appName);
        holder.textServiceId.setText(item.serviceId);

        // FIX: clear listener before setChecked — recycled views otherwise
        // fire stale listeners and corrupt config
        holder.switchHidden.setOnCheckedChangeListener(null);
        holder.switchHidden.setChecked(item.isHidden);
        holder.switchHidden.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isHidden = isChecked;
            if (listener != null) {
                listener.onServiceToggled(item.serviceId, isChecked);
            }
            updateSelection(holder, item);
        });

        updateSelection(holder, item);
        loadIcon(holder, item.serviceId);
    }

    private void updateSelection(ViewHolder holder, ServiceItem item) {
        if (item.isHidden) {
            int strokePx = (int) (2 * holder.itemView.getResources()
                    .getDisplayMetrics().density);
            holder.card.setStrokeWidth(strokePx);
            holder.card.setStrokeColor(0xFF6750A4);
        } else {
            holder.card.setStrokeWidth(0);
        }
    }

    private void loadIcon(ViewHolder holder, String serviceId) {
        String pkg = serviceId.contains("/") ? serviceId.split("/")[0] : serviceId;
        holder.imageIcon.setTag(pkg);

        Drawable cached = iconCache.get(pkg);
        if (cached != null) {
            holder.imageIcon.setImageDrawable(cached);
            return;
        }

        holder.imageIcon.setImageDrawable(null);
        iconExecutor.execute(() -> {
            Drawable icon;
            try {
                icon = pm.getApplicationIcon(pkg);
            } catch (Throwable t) {
                icon = pm.getDefaultActivityIcon();
            }
            iconCache.put(pkg, icon);
            Drawable finalIcon = icon;
            mainHandler.post(() -> {
                // Tag check: row may have been recycled while loading
                if (pkg.equals(holder.imageIcon.getTag())) {
                    holder.imageIcon.setImageDrawable(finalIcon);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final ImageView imageIcon;
        final TextView textServiceName;
        final TextView textServiceId;
        final MaterialSwitch switchHidden;

        ViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            imageIcon = itemView.findViewById(R.id.image_icon);
            textServiceName = itemView.findViewById(R.id.text_service_name);
            textServiceId = itemView.findViewById(R.id.text_service_id);
            switchHidden = itemView.findViewById(R.id.switch_hidden);
        }
    }
                }
