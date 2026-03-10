package com.example.snapjob;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    private List<NotificationModel> notifications;

    public NotificationsAdapter(List<NotificationModel> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel model = notifications.get(position);
        holder.tvTitle.setText(model.getTitle());
        holder.tvMessage.setText(model.getMessage());

        if (model.getTimestamp() != null) {
            String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(model.getTimestamp());
            holder.tvTime.setText(formatted);
        }

        // Change icon based on notification type
        if ("booking".equalsIgnoreCase(model.getType())) {
            holder.imgTypeIcon.setImageResource(R.drawable.outline_add_ad_24);
        } else if ("payment".equalsIgnoreCase(model.getType())) {
            holder.imgTypeIcon.setImageResource(R.drawable.outline_account_balance_wallet_24);
        } else {
            holder.imgTypeIcon.setImageResource(R.drawable.outline_all_inbox_24);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView imgTypeIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            imgTypeIcon = itemView.findViewById(R.id.imgTypeIcon);
        }
    }
}
