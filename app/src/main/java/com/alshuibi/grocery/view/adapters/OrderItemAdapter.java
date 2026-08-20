package com.alshuibi.grocery.view.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.utils.LocalFormat;

import java.util.ArrayList;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.Holder> {
    public interface OnCartChangeListener {
        void onIncrease(int position);
        void onDecrease(int position);
        void onRemove(int position);
    }

    private final ArrayList<Item> items;
    private final Context context;
    private final OnCartChangeListener listener;

    public OrderItemAdapter(ArrayList<Item> items, Context context) {
        this(items, context, null);
    }

    public OrderItemAdapter(ArrayList<Item> items, Context context, OnCartChangeListener listener) {
        this.items = items;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item_design, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Item item = items.get(position);
        holder.name.setText(item.getName());
        holder.unitPrice.setText(LocalFormat.getCurrencyFormat(item.getPrice()));
        holder.quantity.setText(String.valueOf(item.getQuantity()));
        holder.total.setText(LocalFormat.getCurrencyFormat(item.getPrice() * item.getQuantity()));
        boolean editable = listener != null;
        holder.plus.setVisibility(editable ? View.VISIBLE : View.GONE);
        holder.minus.setVisibility(editable ? View.VISIBLE : View.GONE);
        holder.remove.setVisibility(editable ? View.VISIBLE : View.GONE);
        holder.plus.setOnClickListener(v -> { if (listener != null) listener.onIncrease(holder.getBindingAdapterPosition()); });
        holder.minus.setOnClickListener(v -> { if (listener != null) listener.onDecrease(holder.getBindingAdapterPosition()); });
        holder.remove.setOnClickListener(v -> { if (listener != null) listener.onRemove(holder.getBindingAdapterPosition()); });
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView name, quantity, unitPrice, total;
        ImageButton plus, minus, remove;
        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName_design);
            quantity = itemView.findViewById(R.id.unitTotal_design);
            unitPrice = itemView.findViewById(R.id.unitPrice_design);
            total = itemView.findViewById(R.id.totalPrice_design);
            plus = itemView.findViewById(R.id.cartPlusButton);
            minus = itemView.findViewById(R.id.cartMinusButton);
            remove = itemView.findViewById(R.id.cartRemoveButton);
        }
    }
}
