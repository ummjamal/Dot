package com.alshuibi.grocery.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.model.Customer;
import com.alshuibi.grocery.utils.LocalFormat;

import java.util.List;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.Holder> {
    public interface Listener { void onCustomerClick(Customer customer); }
    private final List<Customer> items;
    private final Context context;
    private final Listener listener;

    public CustomerAdapter(List<Customer> items, Context context, Listener listener) {
        this.items = items;
        this.context = context;
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.customer_design, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Customer c = items.get(position);
        h.name.setText(c.getName());
        h.phone.setText(c.getPhone().isEmpty() ? "بدون رقم هاتف" : c.getPhone());
        h.balance.setText(LocalFormat.getCurrencyFormat(c.getBalance()));
        h.balance.setTextColor(ContextCompat.getColor(context, c.getBalance() > 0 ? R.color.brand_error : R.color.brand_success));
        if (c.getCreditLimit() > 0) {
            h.limit.setVisibility(View.VISIBLE);
            h.limit.setText("الحد: " + LocalFormat.getCurrencyFormat(c.getCreditLimit()));
        } else {
            h.limit.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> listener.onCustomerClick(c));
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView name, phone, balance, limit;
        Holder(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.customerName);
            phone = v.findViewById(R.id.customerPhone);
            balance = v.findViewById(R.id.customerBalance);
            limit = v.findViewById(R.id.customerLimit);
        }
    }
}
