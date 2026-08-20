package com.alshuibi.grocery.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.Order;
import com.alshuibi.grocery.utils.FeedbackManager;
import com.alshuibi.grocery.view.adapters.OrdersAdapter;

import java.util.ArrayList;

public class OrdersFragment extends Fragment implements OrdersAdapter.OnOrderActionListener {
    private final ArrayList<Order> orderList = new ArrayList<>();
    private RecyclerView list;
    private LinearLayout noData;
    private OrdersAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        noData = view.findViewById(R.id.noOrderFragmentLL);
        list = view.findViewById(R.id.ordersList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrdersAdapter(orderList, requireContext(), this);
        list.setAdapter(adapter);
        load();
    }

    @Override public void onResume() {
        super.onResume();
        if (adapter != null) load();
    }

    private void load() {
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { db.readOrders(orderList); }
        adapter.notifyDataSetChanged();
        boolean empty = orderList.isEmpty();
        noData.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onCancelOrder(Order order, String reason) {
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            if (db.cancelSale(order.getOrderNumber(), reason)) {
                FeedbackManager.success(requireContext());
                Toast.makeText(requireContext(), "تم إلغاء الفاتورة وإعادة المخزون", Toast.LENGTH_LONG).show();
                load();
            } else {
                FeedbackManager.error(requireContext());
                Toast.makeText(requireContext(), "تعذر إلغاء الفاتورة أو أنها ملغاة مسبقًا", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            FeedbackManager.error(requireContext());
            Toast.makeText(requireContext(), "تعذر الإلغاء: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
