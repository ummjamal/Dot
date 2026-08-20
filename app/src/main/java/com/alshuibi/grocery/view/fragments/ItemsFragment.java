package com.alshuibi.grocery.view.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.utils.FileManager;
import com.alshuibi.grocery.utils.LocalFormat;
import com.alshuibi.grocery.view.adapters.ItemAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class ItemsFragment extends Fragment implements ItemAdapter.OnItemActionListener {
    private final ArrayList<Item> allItems = new ArrayList<>();
    private final ArrayList<Item> visibleItems = new ArrayList<>();
    private ItemAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private TextView summaryCount, summaryCost, summaryRetail;
    private String currentQuery = "";
    private int stockFilter = 0; // 0 all, 1 low, 2 out

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
        ExtendedFloatingActionButton add = view.findViewById(R.id.addButton);
        summaryCount = view.findViewById(R.id.inventorySummaryCount);
        summaryCost = view.findViewById(R.id.inventorySummaryCost);
        summaryRetail = view.findViewById(R.id.inventorySummaryRetail);
        com.google.android.material.chip.ChipGroup filters = view.findViewById(R.id.inventoryFilterChips);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new ItemAdapter(visibleItems, requireContext(), this);
        recyclerView.setAdapter(adapter);
        add.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new NewItemFragment())
                .addToBackStack(null).commit());
        configureSearch(search, "ابحث بالاسم أو القسم أو الباركود");

        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { currentQuery = query == null ? "" : query; applyFilters(); return true; }
            @Override public boolean onQueryTextChange(String newText) { currentQuery = newText == null ? "" : newText; applyFilters(); return true; }
        });
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.filterAll : checkedIds.get(0);
            stockFilter = id == R.id.filterLow ? 1 : (id == R.id.filterOut ? 2 : 0);
            applyFilters();
        });
        load();
    }

    @Override public void onResume() { super.onResume(); if (adapter != null) load(); }

    private void load() {
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            db.readItem(allItems);
            summaryCount.setText(db.getItemCount() + " صنف");
            summaryCost.setText("تكلفة " + LocalFormat.getCurrencyFormat(db.getInventoryCostValue()));
            summaryRetail.setText("بيع " + LocalFormat.getCurrencyFormat(db.getInventoryRetailValue()));
        }
        applyFilters();
    }

    private void applyFilters() {
        String q = currentQuery == null ? "" : currentQuery.trim().toLowerCase(Locale.ROOT);
        visibleItems.clear();
        for (Item item : allItems) {
            String name = item.getName() == null ? "" : item.getName();
            String category = item.getCategory() == null ? "" : item.getCategory();
            String sku = item.getSku() == null ? "" : item.getSku();
            boolean textMatch = q.isEmpty()
                    || name.toLowerCase(Locale.ROOT).contains(q)
                    || category.toLowerCase(Locale.ROOT).contains(q)
                    || sku.toLowerCase(Locale.ROOT).contains(q);
            boolean stockMatch = stockFilter == 0
                    || (stockFilter == 1 && item.getStock() > 0 && item.getStock() <= item.getMinStock())
                    || (stockFilter == 2 && item.getStock() <= 0);
            if (textMatch && stockMatch) visibleItems.add(item);
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
                .setItems(new String[]{"تعديل بيانات الصنف", "إضافة/سحب مخزون", "حذف الصنف"}, (d, which) -> {
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

        new AlertDialog.Builder(requireContext()).setTitle("تعديل بيانات الصنف").setView(view)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", (d, which) -> {
                    try {
                        String cleanName = String.valueOf(name.getText()).trim();
                        double sale = Double.parseDouble(normalize(String.valueOf(salePrice.getText())));
                        double purchase = String.valueOf(purchasePrice.getText()).trim().isEmpty() ? 0 : Double.parseDouble(normalize(String.valueOf(purchasePrice.getText())));
                        int min = String.valueOf(minStock.getText()).trim().isEmpty() ? 5 : Integer.parseInt(normalize(String.valueOf(minStock.getText())));
                        if (cleanName.isEmpty() || sale <= 0 || purchase < 0 || min < 0) throw new IllegalArgumentException("تحقق من الاسم والأسعار وحد التنبيه");

                        item.setName(cleanName);
                        item.setSku(String.valueOf(barcode.getText()).trim());
                        item.setPrice(sale);
                        item.setWholesalePrice(purchase);
                        item.setMinStock(min);
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
        input.setHint("أدخل الكمية");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setPadding(32, 16, 32, 16);
        new AlertDialog.Builder(requireContext())
                .setTitle("المخزون الحالي: " + item.getStock() + " " + item.getUnitType())
                .setView(input)
                .setNegativeButton("إلغاء", null)
                .setNeutralButton("سحب", (d, w) -> adjust(item, input, false))
                .setPositiveButton("إضافة", (d, w) -> adjust(item, input, true))
                .show();
    }

    private void adjust(Item item, EditText input, boolean add) {
        try {
            int qty = Integer.parseInt(normalize(input.getText().toString()));
            if (qty <= 0) throw new NumberFormatException();
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                if (!db.adjustStock(item.getGlobalID(), add ? qty : -qty, add ? "توريد/إضافة مخزون" : "سحب/جرد")) {
                    Toast.makeText(requireContext(), "لا يمكن أن يصبح المخزون سالبًا", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            load();
            Toast.makeText(requireContext(), add ? "تمت إضافة المخزون" : "تم تسجيل السحب", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { Toast.makeText(requireContext(), "أدخل كمية صحيحة أكبر من صفر", Toast.LENGTH_SHORT).show(); }
    }

    private void showDelete(Item item) {
        new AlertDialog.Builder(requireContext()).setTitle("حذف الصنف")
                .setMessage("هل تريد حذف «" + item.getName() + "»؟ سيتم حذف الصنف من المخزون، بينما تبقى الفواتير القديمة محفوظة.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حذف", (d, w) -> {
                    boolean deleted;
                    try (GroceryDatabase db = new GroceryDatabase(requireContext())) { deleted = db.deleteItem(item.getGlobalID()); }
                    if (deleted) {
                        FileManager.deleteImageLocally(requireContext(), "Items", item.getImagePath());
                        Toast.makeText(requireContext(), "تم حذف الصنف", Toast.LENGTH_SHORT).show();
                    }
                    load();
                }).show();
    }

    private String normalize(String value) {
        return value.trim().replace("٠","0").replace("١","1").replace("٢","2").replace("٣","3")
                .replace("٤","4").replace("٥","5").replace("٦","6").replace("٧","7")
                .replace("٨","8").replace("٩","9").replace("٫",".").replace("٬","").replace(",","");
    }
    private void configureSearch(SearchView searchView, String hint) {
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setQueryHint(hint);
        searchView.setFocusable(true);
        searchView.setFocusableInTouchMode(true);

        android.widget.AutoCompleteTextView input = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (input != null) {
            input.setSingleLine(true);
            input.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text));
            input.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.brand_text_secondary));

            View.OnClickListener focusSearch = v -> {
                searchView.setIconified(false);
                input.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            };
            input.setOnClickListener(focusSearch);
            searchView.setOnClickListener(focusSearch);
        }

        searchView.clearFocus();
    }

}
