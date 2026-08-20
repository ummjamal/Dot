package com.alshuibi.grocery.view.fragments;

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

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.Transaction;
import com.alshuibi.grocery.view.adapters.TransactionRecyclerAdapter;

import java.util.ArrayList;

public class TransactionsFragment extends Fragment {
    private final ArrayList<Transaction> transactions = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout noData = view.findViewById(R.id.noTransactionFragmentLL);
        RecyclerView list = view.findViewById(R.id.transactionList);
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { db.readTransactions(transactions); }
        boolean empty = transactions.isEmpty();
        noData.setVisibility(empty ? View.VISIBLE : View.GONE);
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(new TransactionRecyclerAdapter(transactions));
    }
}
