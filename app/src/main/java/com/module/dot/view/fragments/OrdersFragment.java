package com.module.dot.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.model.Order;
import com.module.dot.view.adapters.OrdersAdapter;

import java.util.ArrayList;

public class OrdersFragment extends Fragment {
    private final ArrayList<Order> orderList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout noData = view.findViewById(R.id.noOrderFragmentLL);
        RecyclerView list = view.findViewById(R.id.ordersList);
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { db.readOrders(orderList); }
        boolean empty = orderList.isEmpty();
        noData.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new OrdersAdapter(orderList, requireContext()));
    }
}
