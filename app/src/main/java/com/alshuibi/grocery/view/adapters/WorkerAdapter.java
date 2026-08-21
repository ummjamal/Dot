package com.alshuibi.grocery.view.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.model.User;

import java.util.List;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.Holder> {
    public interface Listener { void onClick(User user); }
    private final List<User> users;
    private final Listener listener;

    public WorkerAdapter(List<User> users, Listener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.worker_design, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        User u = users.get(position);
        h.name.setText(u.getFullName().trim());
        h.email.setText(u.getEmail());
        h.role.setText("Administrator".equalsIgnoreCase(u.getPositionTitle()) ? "مالك / مدير" : "عامل مبيعات");
        h.itemView.setOnClickListener(v -> listener.onClick(u));
    }

    @Override public int getItemCount() { return users.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView name,email,role;
        Holder(@NonNull View v) {
            super(v);
            name=v.findViewById(R.id.workerName);
            email=v.findViewById(R.id.workerEmail);
            role=v.findViewById(R.id.workerRole);
        }
    }
}
