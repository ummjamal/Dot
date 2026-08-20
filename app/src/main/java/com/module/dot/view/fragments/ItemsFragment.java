package com.module.dot.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.model.Item;
import com.module.dot.view.adapters.ItemAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ItemsFragment extends Fragment implements ItemAdapter.OnItemActionListener {
    private final ArrayList<Item> allItems = new ArrayList<>();
    private final ArrayList<Item> visibleItems = new ArrayList<>();
    private ItemAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.itemList);
        emptyState = view.findViewById(R.id.noDataItemFragmentLL);
        SearchView search = view.findViewById(R.id.itemSearchView);
        FloatingActionButton add = view.findViewById(R.id.addButton);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ItemAdapter(visibleItems, requireContext(), this);
        recyclerView.setAdapter(adapter);
        add.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new NewItemFragment())
                .addToBackStack(null).commit());
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });
        load();
    }

    @Override public void onResume() { super.onResume(); if (adapter != null) load(); }

    private void load() {
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { db.readItem(allItems); }
        visibleItems.clear(); visibleItems.addAll(allItems); refresh();
    }

    private void filter(String text) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        visibleItems.clear();
        if (q.isEmpty()) visibleItems.addAll(allItems);
        else for (Item item : allItems) {
            if (item.getName().toLowerCase(Locale.ROOT).contains(q)
                    || item.getCategory().toLowerCase(Locale.ROOT).contains(q)
                    || item.getSku().toLowerCase(Locale.ROOT).contains(q)) visibleItems.add(item);
        }
        refresh();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        boolean empty = visibleItems.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onItemLongClick(Item item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(item.getName())
                .setItems(new String[]{"تعديل بيانات الصنف", "تعديل المخزون", "حذف الصنف"}, (d, which) -> {
                    if (which == 0) showEdit(item);
                    else if (which == 1) showStockAdjustment(item);
                    else showDelete(item);
                }).show();
    }

    private void showEdit(Item item) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_item, null, false);
        TextInputEditText name = view.findViewById(R.id.editItemName);
        TextInputEditText barcode = view.findViewById(R.id.editItemBarcode);
        TextInputEditText salePrice = view.findViewById(R.id.editItemSalePrice);
        TextInputEditText purchasePrice = view.findViewById(R.id.editItemPurchasePrice);
        TextInputEditText minStock = view.findViewById(R.id.editItemMinStock);
        TextInputEditText description = view.findViewById(R.id.editItemDescription);
        Spinner category = view.findViewById(R.id.editItemCategory);
        Spinner unit = view.findViewById(R.id.editItemUnit);

        ArrayList<String> categories;
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { categories = db.getCategories(); }
        ArrayList<String> units = new ArrayList<>(Arrays.asList("حبة", "علبة", "كرتون", "باكت", "كيس", "كيلو", "جرام", "لتر", "نصف لتر", "درزن", "ربطة"));
        category.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories));
        unit.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, units));
        name.setText(item.getName());
        barcode.setText(item.getSku());
        salePrice.setText(String.valueOf(Math.round(item.getPrice())));
        purchasePrice.setText(String.valueOf(Math.round(item.getWholesalePrice())));
        minStock.setText(String.valueOf(item.getMinStock()));
        description.setText(item.getDescription());
        int ci = categories.indexOf(item.getCategory()); if (ci >= 0) category.setSelection(ci);
        int ui = units.indexOf(item.getUnitType()); if (ui >= 0) unit.setSelection(ui);

        new AlertDialog.Builder(requireContext()).setTitle("تعديل الصنف").setView(view)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", (d, which) -> {
                    try {
                        item.setName(String.valueOf(name.getText()).trim());
                        item.setSku(String.valueOf(barcode.getText()).trim());
                        item.setPrice(Double.parseDouble(normalize(String.valueOf(salePrice.getText()))));
                        item.setWholesalePrice(Double.parseDouble(normalize(String.valueOf(purchasePrice.getText()))));
                        item.setMinStock(Integer.parseInt(normalize(String.valueOf(minStock.getText()))));
                        item.setDescription(String.valueOf(description.getText()).trim());
                        item.setCategory(String.valueOf(category.getSelectedItem()));
                        item.setUnitType(String.valueOf(unit.getSelectedItem()));
                        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                            if (!db.updateItem(item)) throw new IllegalStateException("قد يكون الباركود مستخدمًا لصنف آخر");
                        }
                        load();
                        Toast.makeText(requireContext(), "تم تحديث الصنف", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "تعذر الحفظ: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void showStockAdjustment(Item item) {
        EditText input = new EditText(requireContext());
        input.setHint("الكمية");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(requireContext())
                .setTitle("المخزون الحالي: " + item.getStock())
                .setView(input)
                .setNegativeButton("إلغاء", null)
                .setNeutralButton("سحب", (d, w) -> adjust(item, input, false))
                .setPositiveButton("إضافة", (d, w) -> adjust(item, input, true))
                .show();
    }

    private void adjust(Item item, EditText input, boolean add) {
        try {
            int qty = Integer.parseInt(normalize(input.getText().toString()));
            if (qty <= 0) return;
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                if (!db.adjustStock(item.getGlobalID(), add ? qty : -qty, add ? "توريد/إضافة مخزون" : "سحب/جرد")) {
                    Toast.makeText(requireContext(), "الكمية غير صالحة", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            load();
        } catch (Exception e) { Toast.makeText(requireContext(), "أدخل كمية صحيحة", Toast.LENGTH_SHORT).show(); }
    }

    private void showDelete(Item item) {
        new AlertDialog.Builder(requireContext()).setTitle("حذف الصنف")
                .setMessage("هل تريد حذف " + item.getName() + "؟ لا يمكن التراجع بعد الحذف.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حذف", (d, w) -> {
                    try (GroceryDatabase db = new GroceryDatabase(requireContext())) { db.deleteItem(item.getGlobalID()); }
                    load();
                }).show();
    }

    private String normalize(String value) {
        return value.trim().replace("٠","0").replace("١","1").replace("٢","2").replace("٣","3")
                .replace("٤","4").replace("٥","5").replace("٦","6").replace("٧","7")
                .replace("٨","8").replace("٩","9").replace("٫",".").replace("٬","").replace(",","");
    }
}
