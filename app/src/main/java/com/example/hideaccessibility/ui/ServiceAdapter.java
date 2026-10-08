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

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    public interface OnServiceToggleListener {
        void onServiceToggled(String serviceId, boolean hidden);
    }

    private final List<ServiceItem> items = new ArrayList<>();
    private final OnServiceToggleListener listener;

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

    public ServiceAdapter(OnServiceToggleListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ServiceItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public int getHiddenCount() {
        int count = 0;
        for (ServiceItem item : items) {
            if (item.isHidden) count++;
        }
        return count;
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
        holder.switchHidden.setChecked(item.isHidden);
        holder.switchHidden.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.isHidden = isChecked;
            if (listener != null) {
                listener.onServiceToggled(item.serviceId, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textServiceName;
        final TextView textServiceId;
        final Switch switchHidden;

        ViewHolder(View itemView) {
            super(itemView);
            textServiceName = itemView.findViewById(R.id.text_service_name);
            textServiceId = itemView.findViewById(R.id.text_service_id);
            switchHidden = itemView.findViewById(R.id.switch_hidden);
        }
    }
}
