package com.example.hideaccessibility.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hideaccessibility.R;

import java.util.ArrayList;
import java.util.List;

public class TargetAppAdapter extends RecyclerView.Adapter<TargetAppAdapter.ViewHolder> {

    public interface OnTargetToggleListener {
        void onTargetToggled(String packageName, boolean enabled);
    }

    private final List<TargetAppItem> items = new ArrayList<>();
    private final OnTargetToggleListener listener;

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

    public TargetAppAdapter(OnTargetToggleListener listener) {
        this.listener = listener;
    }

    public void setItems(List<TargetAppItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public int getTargetCount() {
        int count = 0;
        for (TargetAppItem item : items) {
            if (item.isTarget) count++;
        }
        return count;
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
        holder.switchTarget.setChecked(item.isTarget);
        holder.switchTarget.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isTarget = isChecked;
            if (listener != null) {
                listener.onTargetToggled(item.packageName, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textAppName;
        final TextView textPackageName;
        final Switch switchTarget;

        ViewHolder(View itemView) {
            super(itemView);
            textAppName = itemView.findViewById(R.id.text_app_name);
            textPackageName = itemView.findViewById(R.id.text_package_name);
            switchTarget = itemView.findViewById(R.id.switch_target);
        }
    }
}
