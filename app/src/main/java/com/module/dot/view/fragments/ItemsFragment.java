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
import androidx.appcompat.widget.SearchView;
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
import java.util.Locale;

public class ItemsFragment extends Fragment {

    private FragmentActivity fragmentActivity;

    private final ArrayList<Item> allItems =
            new ArrayList<>();

    private final ArrayList<Item> visibleItems =
            new ArrayList<>();

    private ItemAdapter adapter;

    private RecyclerView recyclerView;

    private LinearLayout noData;

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

        noData =
                view.findViewById(
                        R.id.noDataItemFragmentLL
                );

        recyclerView =
                view.findViewById(
                        R.id.itemList
                );

        FloatingActionButton addItem =
                view.findViewById(
                        R.id.addButton
                );

        SearchView searchView =
                view.findViewById(
                        R.id.itemSearchView
                );

        recyclerView.setLayoutManager(
                new GridLayoutManager(
                        requireContext(),
                        2
                )
        );

        adapter =
                new ItemAdapter(
                        visibleItems,
                        requireContext()
                );

        recyclerView.setAdapter(
                adapter
        );

        loadItems();

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {

                    @Override
                    public boolean onQueryTextSubmit(
                            String query
                    ) {

                        filter(query);

                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(
                            String newText
                    ) {

                        filter(newText);

                        return true;
                    }
                }
        );

        boolean isAdmin =
                MainActivity.currentUser == null
                        ||
                        "Administrator"
                                .equalsIgnoreCase(
                                        MainActivity
                                                .currentUser
                                                .getPositionTitle()
                                );

        addItem.setVisibility(
                isAdmin
                        ?
                        View.VISIBLE
                        :
                        View.GONE
        );

        addItem.setOnClickListener(
                v ->
                        showFragment(
                                new NewItemFragment()
                        )
        );
    }

    @Override
    public void onResume() {

        super.onResume();

        if (adapter != null) {
            loadItems();
        }
    }

    private void loadItems() {

        allItems.clear();

        visibleItems.clear();

        try (
                ItemDatabase database =
                        new ItemDatabase(
                                requireContext()
                        )
        ) {

            database.readItem(
                    allItems
            );

            visibleItems.addAll(
                    allItems
            );

        } catch (Exception e) {

            Log.e(
                    "ItemsFragment",
                    "Unable to load products",
                    e
            );
        }

        refreshState();
    }

    private void filter(
            String text
    ) {

        visibleItems.clear();

        String query =
                text == null
                        ?
                        ""
                        :
                        text.trim()
                                .toLowerCase(
                                        Locale.ROOT
                                );

        if (query.isEmpty()) {

            visibleItems.addAll(
                    allItems
            );

        } else {

            for (Item item : allItems) {

                String name =
                        item.getName()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                String category =
                        item.getCategory()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                String barcode =
                        item.getSku()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                if (
                        name.contains(query)
                                ||
                        category.contains(query)
                                ||
                        barcode.contains(query)
                ) {

                    visibleItems.add(
                            item
                    );
                }
            }
        }

        refreshState();
    }

    private void refreshState() {

        adapter.notifyDataSetChanged();

        if (visibleItems.isEmpty()) {

            recyclerView.setVisibility(
                    View.GONE
            );

            noData.setVisibility(
                    View.VISIBLE
            );

        } else {

            recyclerView.setVisibility(
                    View.VISIBLE
            );

            noData.setVisibility(
                    View.GONE
            );
        }
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

        transaction.addToBackStack(
                null
        );

        transaction.commit();
    }
}