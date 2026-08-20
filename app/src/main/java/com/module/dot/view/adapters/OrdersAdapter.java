package com.module.dot.view.adapters;

import static com.module.dot.utils.LocalFormat.getCurrencyFormat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.model.Order;

import java.util.ArrayList;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.Holder> {
    private final ArrayList<Order> orders;
    private final Context context;
    public OrdersAdapter(ArrayList<Order> orders, Context context) { this.orders = orders; this.context = context; }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.orders_design, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Order order = orders.get(position);
        h.number.setText("#" + order.getOrderNumber());
        h.date.setText(order.getOrderDate());
        h.time.setText(order.getOrderTime());
        h.status.setText("مكتملة");
        h.items.setText(order.getOrderTotalItems() + " صنف/وحدة");
        h.total.setText(getCurrencyFormat(order.getOrderTotalAmount()));
        SelectedItemsAdapter adapter = new SelectedItemsAdapter(order.getSelectedItemList() == null ? new ArrayList<>() : order.getSelectedItemList(), context);
        h.selected.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        h.selected.setAdapter(adapter);
    }

    @Override public int getItemCount() { return orders.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView number,date,time,status,items,total; RecyclerView selected;
        Holder(@NonNull View v) { super(v);
            number=v.findViewById(R.id.orderNumber); date=v.findViewById(R.id.orderDate); time=v.findViewById(R.id.orderTime);
            status=v.findViewById(R.id.orderStatus); items=v.findViewById(R.id.orderTotalItems); total=v.findViewById(R.id.orderTotal);
            selected=v.findViewById(R.id.selectedItemRV);
        }
    }
}
