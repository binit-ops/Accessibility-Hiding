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

public class TargetAppAdapter extends RecyclerView.Adapter<TargetAppAdapter.ViewHolder> {

    public interface OnTargetToggleListener {
        void onTargetToggled(String packageName, boolean enabled);
    }

    private final List<TargetAppItem> allItems = new ArrayList<>();
    private final List<TargetAppItem> items = new ArrayList<>();
    private final OnTargetToggleListener listener;
    private final PackageManager pm;

    private final Map<String, Drawable> iconCache = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService iconExecutor = Executors.newSingleThreadExecutor();
    private String query = "";

    public static class TargetAppItem {
        public final String packageName;
        public final String appName;
        public boolean isTarget;

        public TargetAppItem(String packageName, String appName, boolean isTarget) {
            this.packageName = packageName;
            this.appName = appName;
            this.isTarget = isTarget;
        }
    }

    public TargetAppAdapter(OnTargetToggleListener listener, PackageManager pm) {
        this.listener = listener;
        this.pm = pm;
    }

    public void setItems(List<TargetAppItem> newItems) {
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
            for (TargetAppItem it : allItems) {
                if (it.appName.toLowerCase(Locale.ROOT).contains(query)
                        || it.packageName.toLowerCase(Locale.ROOT).contains(query)) {
                    items.add(it);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getShownCount() { return items.size(); }
    public int getTotalCount() { return allItems.size(); }

    public int getTargetCount() {
        int count = 0;
        for (TargetAppItem item : allItems) {
            if (item.isTarget) count++;
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
                .inflate(R.layout.item_target_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TargetAppItem item = items.get(position);

        holder.textAppName.setText(item.appName);
        holder.textPackageName.setText(item.packageName);

        // FIX: clear listener before setChecked (recycling bug)
        holder.switchTarget.setOnCheckedChangeListener(null);
        holder.switchTarget.setChecked(item.isTarget);
        holder.switchTarget.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isTarget = isChecked;
            if (listener != null) {
                listener.onTargetToggled(item.packageName, isChecked);
            }
            updateSelection(holder, item);
        });

        updateSelection(holder, item);
        loadIcon(holder, item.packageName);
    }

    private void updateSelection(ViewHolder holder, TargetAppItem item) {
        if (item.isTarget) {
            int strokePx = (int) (2 * holder.itemView.getResources()
                    .getDisplayMetrics().density);
            holder.card.setStrokeWidth(strokePx);
            holder.card.setStrokeColor(0xFF6750A4);
        } else {
            holder.card.setStrokeWidth(0);
        }
    }

    private void loadIcon(ViewHolder holder, String packageName) {
        holder.imageIcon.setTag(packageName);

        Drawable cached = iconCache.get(packageName);
        if (cached != null) {
            holder.imageIcon.setImageDrawable(cached);
            return;
        }

        holder.imageIcon.setImageDrawable(null);
        iconExecutor.execute(() -> {
            Drawable icon;
            try {
                icon = pm.getApplicationIcon(packageName);
            } catch (Throwable t) {
                icon = pm.getDefaultActivityIcon();
            }
            iconCache.put(packageName, icon);
            Drawable finalIcon = icon;
            mainHandler.post(() -> {
                if (packageName.equals(holder.imageIcon.getTag())) {
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
        final TextView textAppName;
        final TextView textPackageName;
        final MaterialSwitch switchTarget;

        ViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            imageIcon = itemView.findViewById(R.id.image_icon);
            textAppName = itemView.findViewById(R.id.text_app_name);
            textPackageName = itemView.findViewById(R.id.text_package_name);
            switchTarget = itemView.findViewById(R.id.switch_target);
        }
    }
}
