package com.module.dot.view.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.model.Item;
import com.module.dot.model.Order;
import com.module.dot.utils.LocalFormat;
import com.module.dot.view.MainActivity;
import com.module.dot.view.adapters.ItemAdapter;
import com.module.dot.view.adapters.OrderItemAdapter;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {
    private static final String BARCODE_KEY = "home_barcode_result";
    private FragmentActivity fragmentActivity;
    private Button chargeButton;
    private TextView todaySales, todayProfit, todayCount, lowStock;
    private RecyclerView recyclerView;
    private LinearLayout noData;
    private ItemAdapter itemAdapter;
    private final ArrayList<Item> allItems = new ArrayList<>();
    private final ArrayList<Item> visibleItems = new ArrayList<>();
    private final ArrayList<Item> selectedItems = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        fragmentActivity = (FragmentActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).enableNavigationViews(View.VISIBLE);

        todaySales = view.findViewById(R.id.statTodaySales);
        todayProfit = view.findViewById(R.id.statTodayProfit);
        todayCount = view.findViewById(R.id.statTodayCount);
        lowStock = view.findViewById(R.id.statLowStock);
        recyclerView = view.findViewById(R.id.itemList);
        noData = view.findViewById(R.id.noDataHomeFragmentLL);
        FloatingActionButton scanButton = view.findViewById(R.id.scanButton);
        chargeButton = view.findViewById(R.id.Charge);
        SearchView searchView = view.findViewById(R.id.homeSearchView);

        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        itemAdapter = new ItemAdapter(visibleItems, requireContext(), this);
        recyclerView.setAdapter(itemAdapter);

        getParentFragmentManager().setFragmentResultListener(BARCODE_KEY, getViewLifecycleOwner(), (key, bundle) -> {
            String barcode = bundle.getString(BarcodeScannerDialogFragment.RESULT_BARCODE, "");
            if (!barcode.isEmpty()) addBarcodeToCart(barcode);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });

        scanButton.setOnClickListener(v -> BarcodeScannerDialogFragment.newInstance(BARCODE_KEY)
                .show(getParentFragmentManager(), "barcodeScanner"));
        chargeButton.setOnClickListener(v -> showCartDialog());

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (itemAdapter != null) loadData();
    }

    private void loadData() {
        allItems.clear();
        visibleItems.clear();
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            db.readItem(allItems);
            visibleItems.addAll(allItems);
            todaySales.setText(LocalFormat.getCurrencyFormat(db.getTodaySalesTotal()));
            todayProfit.setText(LocalFormat.getCurrencyFormat(db.getTodayProfit()));
            todayCount.setText(String.valueOf(db.getTodaySalesCount()));
            lowStock.setText(String.valueOf(db.getLowStockCount() + db.getOutOfStockCount()));
        }
        refreshListState();
        updateCartButton();
    }

    private void filter(String text) {
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        visibleItems.clear();
        if (query.isEmpty()) visibleItems.addAll(allItems);
        else {
            for (Item item : allItems) {
                if (item.getName().toLowerCase(Locale.ROOT).contains(query)
                        || item.getCategory().toLowerCase(Locale.ROOT).contains(query)
                        || item.getSku().toLowerCase(Locale.ROOT).contains(query)) {
                    visibleItems.add(item);
                }
            }
        }
        refreshListState();
    }

    private void refreshListState() {
        itemAdapter.notifyDataSetChanged();
        boolean empty = visibleItems.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        noData.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    public void addItemToCart(Item source) {
        if (source == null) return;
        if (source.getStock() <= 0) {
            Toast.makeText(requireContext(), "هذا الصنف نافد من المخزون", Toast.LENGTH_SHORT).show();
            return;
        }
        for (Item cartItem : selectedItems) {
            if (Objects.equals(cartItem.getGlobalID(), source.getGlobalID())) {
                if (cartItem.getQuantity() >= source.getStock()) {
                    Toast.makeText(requireContext(), "لا توجد كمية إضافية متوفرة", Toast.LENGTH_SHORT).show();
                    return;
                }
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                updateCartButton();
                return;
            }
        }
        Item cart = new Item(source.getGlobalID(), source.getName(), source.getPrice(), source.getTax(), source.getSku(), 1L);
        cart.setWholesalePrice(source.getWholesalePrice());
        cart.setStock(source.getStock());
        cart.setUnitType(source.getUnitType());
        selectedItems.add(cart);
        updateCartButton();
    }

    // Backward-compatible calls used by older adapters.
    public void addToSelectedItems(Item item) { addItemToCart(item); }
    public void updateAmount(double ignored) { updateCartButton(); }
    public void updateTax(double ignored) { }
    public Long totalItem = 0L;

    private void addBarcodeToCart(String barcode) {
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            Item item = db.getItemBySku(barcode);
            if (item == null) {
                Toast.makeText(requireContext(), "الباركود غير مسجل. أضف الصنف أولًا من قسم الأصناف.", Toast.LENGTH_LONG).show();
                return;
            }
            addItemToCart(item);
            Toast.makeText(requireContext(), "تمت إضافة " + item.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private long cartTotal() {
        long total = 0;
        for (Item item : selectedItems) total += Math.round(item.getPrice()) * item.getQuantity();
        return total;
    }

    private long cartCount() {
        long count = 0;
        for (Item item : selectedItems) count += item.getQuantity();
        totalItem = count;
        return count;
    }

    private void updateCartButton() {
        if (chargeButton == null) return;
        chargeButton.setText("السلة • " + cartCount() + " عنصر • " + LocalFormat.getCurrencyFormat(cartTotal()));
    }

    private void showCartDialog() {
        if (selectedItems.isEmpty()) {
            Toast.makeText(requireContext(), "سلة البيع فارغة", Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheet_layout);
        TextView taxTotal = dialog.findViewById(R.id.taxTotal);
        TextView totalView = dialog.findViewById(R.id.transactionTotal);
        Button checkout = dialog.findViewById(R.id.checkoutButton);
        RecyclerView list = dialog.findViewById(R.id.transactionSheetList);
        taxTotal.setText("0 ر.ي");
        totalView.setText(LocalFormat.getCurrencyFormat(cartTotal()));
        list.setLayoutManager(new LinearLayoutManager(requireContext()));

        OrderItemAdapter adapter = new OrderItemAdapter(selectedItems, requireContext(), new OrderItemAdapter.OnCartChangeListener() {
            @Override public void onIncrease(int position) {
                Item item = selectedItems.get(position);
                if (item.getQuantity() < item.getStock()) item.setQuantity(item.getQuantity() + 1);
                else Toast.makeText(requireContext(), "وصلت للكمية المتوفرة", Toast.LENGTH_SHORT).show();
                list.getAdapter().notifyItemChanged(position);
                totalView.setText(LocalFormat.getCurrencyFormat(cartTotal()));
                updateCartButton();
            }
            @Override public void onDecrease(int position) {
                Item item = selectedItems.get(position);
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    list.getAdapter().notifyItemChanged(position);
                } else {
                    selectedItems.remove(position);
                    list.getAdapter().notifyItemRemoved(position);
                }
                totalView.setText(LocalFormat.getCurrencyFormat(cartTotal()));
                updateCartButton();
                if (selectedItems.isEmpty()) dialog.dismiss();
            }
            @Override public void onRemove(int position) {
                selectedItems.remove(position);
                list.getAdapter().notifyItemRemoved(position);
                totalView.setText(LocalFormat.getCurrencyFormat(cartTotal()));
                updateCartButton();
                if (selectedItems.isEmpty()) dialog.dismiss();
            }
        });
        list.setAdapter(adapter);

        checkout.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("تأكيد البيع")
                .setMessage("الإجمالي: " + LocalFormat.getCurrencyFormat(cartTotal()) + "\nسيتم خصم الكميات من المخزون فورًا.")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إتمام البيع", (d, which) -> {
                    try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                        Order sale = db.completeSale(selectedItems, "cash");
                        selectedItems.clear();
                        dialog.dismiss();
                        loadData();
                        new AlertDialog.Builder(requireContext())
                                .setTitle("تم البيع بنجاح")
                                .setMessage("رقم الفاتورة: #" + sale.getOrderNumber() + "\nالإجمالي: " + LocalFormat.getCurrencyFormat(sale.getOrderTotalAmount()))
                                .setPositiveButton("حسنًا", null)
                                .show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "تعذر إتمام البيع: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show());

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }
}
