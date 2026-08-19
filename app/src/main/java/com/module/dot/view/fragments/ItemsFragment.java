package com.module.dot.view.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.module.dot.R;
import com.module.dot.data.local.ItemDatabase;
import com.module.dot.model.Item;
import com.module.dot.view.MainActivity;
import com.module.dot.view.adapters.ItemAdapter;

import java.util.ArrayList;

public class ItemsFragment extends Fragment {

    private FragmentActivity fragmentActivity;

    private final ArrayList<Item> itemList =
            new ArrayList<>();

    @Override
    public void onAttach(
            @NonNull Context context
    ) {

        super.onAttach(context);

        fragmentActivity =
                (FragmentActivity) context;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        return inflater.inflate(
                R.layout.fragment_items,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );

        LinearLayout noData =
                view.findViewById(
                        R.id.noDataItemFragmentLL
                );

        RecyclerView recyclerView =
                view.findViewById(
                        R.id.itemList
                );

        FloatingActionButton addItem =
                view.findViewById(
                        R.id.addButton
                );

        itemList.clear();

        try (ItemDatabase itemDatabase =
                     new ItemDatabase(
                             requireContext()
                     )) {

            if (
                    itemDatabase.isTableEmpty(
                            "items"
                    )
            ) {

                itemDatabase.showEmptyStateMessage(
                        recyclerView,
                        noData
                );

            } else {

                itemDatabase.showStateMessage(
                        recyclerView,
                        noData
                );

                itemDatabase.readItem(
                        itemList
                );
            }

        } catch (Exception e) {

            Log.e(
                    "ItemsFragment",
                    "Error loading items",
                    e
            );
        }

        recyclerView.setLayoutManager(
                new GridLayoutManager(
                        requireContext(),
                        2
                )
        );

        recyclerView.setAdapter(
                new ItemAdapter(
                        itemList,
                        requireContext()
                )
        );

        boolean isAdmin =
                MainActivity.currentUser == null ||
                "Administrator".equalsIgnoreCase(
                        MainActivity.currentUser
                                .getPositionTitle()
                );

        addItem.setVisibility(
                isAdmin
                        ? View.VISIBLE
                        : View.GONE
        );

        addItem.setOnClickListener(
                v -> showFragment(
                        new NewItemFragment()
                )
        );
    }

    private void showFragment(
            Fragment fragment
    ) {

        FragmentManager manager =
                fragmentActivity
                        .getSupportFragmentManager();

        FragmentTransaction transaction =
                manager.beginTransaction();

        transaction.replace(
                R.id.fragment_container,
                fragment
        );

        transaction.addToBackStack(null);

        transaction.commit();
    }
}