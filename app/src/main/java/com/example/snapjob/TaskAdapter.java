package com.example.snapjob;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<TaskModel> taskList;

    public TaskAdapter(List<TaskModel> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_card, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = taskList.get(position);
        holder.tvTaskName.setText(task.getTaskName());
        holder.tvDesc.setText(task.getDescription());
        holder.tvAmount.setText("₹" + task.getAmount());
        holder.tvStatus.setText(task.getStatus());
        holder.tvFromTo.setText(task.getFromLocation() + " → " + task.getToLocation());

        // Load image if exists
        if (task.getImageUrl() != null && !task.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(task.getImageUrl())
                    .placeholder(R.drawable.baseline_add_task_24)
                    .into(holder.imgTask);
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskName, tvDesc, tvAmount, tvStatus, tvFromTo;
        ImageView imgTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvDesc = itemView.findViewById(R.id.tvDescription);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvFromTo = itemView.findViewById(R.id.tvFromTo);
            imgTask = itemView.findViewById(R.id.imgTask);
        }
    }
}
