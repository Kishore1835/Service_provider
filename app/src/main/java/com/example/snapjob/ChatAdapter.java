package com.example.snapjob;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private List<ChatMessage> messages;
    private String userEmail;

    public ChatAdapter(List<ChatMessage> messages, String userEmail) {
        this.messages = messages;
        this.userEmail = userEmail;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (msg.isFromSupport()) {
            holder.layoutSupport.setVisibility(View.VISIBLE);
            holder.layoutUser.setVisibility(View.GONE);
            holder.tvSupportMessage.setText(msg.getMessage());
            holder.tvSupportTime.setText(formatTime(msg.getTimestamp()));
        } else {
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.layoutSupport.setVisibility(View.GONE);
            holder.tvUserMessage.setText(msg.getMessage());
            holder.tvUserTime.setText(formatTime(msg.getTimestamp()));
        }
    }

    private String formatTime(java.util.Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutUser, layoutSupport;
        TextView tvUserMessage, tvUserTime, tvSupportMessage, tvSupportTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutUser = itemView.findViewById(R.id.layoutUser);
            layoutSupport = itemView.findViewById(R.id.layoutSupport);
            tvUserMessage = itemView.findViewById(R.id.tvUserMessage);
            tvUserTime = itemView.findViewById(R.id.tvUserTime);
            tvSupportMessage = itemView.findViewById(R.id.tvSupportMessage);
            tvSupportTime = itemView.findViewById(R.id.tvSupportTime);
        }
    }
}
