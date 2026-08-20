package com.alshuibi.grocery.view.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.model.Order;
import com.alshuibi.grocery.utils.LocalFormat;
import com.alshuibi.grocery.utils.FeedbackManager;
import com.alshuibi.grocery.view.MainActivity;
import com.alshuibi.grocery.view.adapters.ItemAdapter;
import com.alshuibi.grocery.view.adapters.OrderItemAdapter;

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

        configureSearch(searchView, "ابحث باسم الصنف أو الباركود");

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
                String name = item.getName() == null ? "" : item.getName();
                String category = item.getCategory() == null ? "" : item.getCategory();
                String sku = item.getSku() == null ? "" : item.getSku();
                if (name.toLowerCase(Locale.ROOT).contains(query)
                        || category.toLowerCase(Locale.ROOT).contains(query)
                        || sku.toLowerCase(Locale.ROOT).contains(query)) {
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
            FeedbackManager.error(requireContext());
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
                FeedbackManager.error(requireContext());
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

        checkout.setText("مراجعة الدفع وإتمام البيع");
        checkout.setOnClickListener(v -> showCheckoutDialog(dialog));

        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }
    }

    private void showCheckoutDialog(Dialog cartDialog) {
        if (selectedItems.isEmpty()) return;
        Dialog checkoutDialog = new Dialog(requireContext());
        checkoutDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        checkoutDialog.setContentView(R.layout.dialog_checkout);

        TextView gross = checkoutDialog.findViewById(R.id.checkoutGrossTotal);
        TextView net = checkoutDialog.findViewById(R.id.checkoutNetTotal);
        TextView change = checkoutDialog.findViewById(R.id.checkoutChange);
        com.google.android.material.textfield.TextInputEditText discount = checkoutDialog.findViewById(R.id.checkoutDiscount);
        com.google.android.material.textfield.TextInputEditText received = checkoutDialog.findViewById(R.id.checkoutReceived);
        com.google.android.material.textfield.TextInputEditText notes = checkoutDialog.findViewById(R.id.checkoutNotes);
        com.google.android.material.textfield.TextInputLayout receivedLayout = checkoutDialog.findViewById(R.id.checkoutReceivedLayout);
        Spinner payment = checkoutDialog.findViewById(R.id.checkoutPaymentMethod);
        Button cancel = checkoutDialog.findViewById(R.id.checkoutCancel);
        Button confirm = checkoutDialog.findViewById(R.id.checkoutConfirm);

        String[] paymentLabels = {"نقدي", "تحويل"};
        payment.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentLabels));
        gross.setText(LocalFormat.getCurrencyFormat(cartTotal()));
        discount.setText("0");
        received.setText(String.valueOf(cartTotal()));

        Runnable refresh = () -> {
            long total = cartTotal();
            long discountValue = parseMoney(discount.getText() == null ? "" : discount.getText().toString());
            discountValue = Math.max(0, Math.min(discountValue, total));
            long netValue = total - discountValue;
            boolean transfer = payment.getSelectedItemPosition() == 1;
            receivedLayout.setEnabled(!transfer);
            if (transfer) {
                String expected = String.valueOf(netValue);
                String current = received.getText() == null ? "" : received.getText().toString();
                if (!expected.equals(current)) {
                    received.setText(expected);
                    received.setSelection(received.length());
                    return;
                }
            }
            long receivedValue = parseMoney(received.getText() == null ? "" : received.getText().toString());
            long changeValue = transfer ? 0 : Math.max(0, receivedValue - netValue);
            net.setText(LocalFormat.getCurrencyFormat(netValue));
            change.setText(LocalFormat.getCurrencyFormat(changeValue));
        };

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refresh.run(); }
            @Override public void afterTextChanged(Editable s) { }
        };
        discount.addTextChangedListener(watcher);
        received.addTextChangedListener(watcher);
        payment.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { refresh.run(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        refresh.run();

        cancel.setOnClickListener(v -> checkoutDialog.dismiss());
        confirm.setOnClickListener(v -> {
            long grossValue = cartTotal();
            long discountValue = Math.max(0, Math.min(parseMoney(discount.getText() == null ? "" : discount.getText().toString()), grossValue));
            long due = grossValue - discountValue;
            boolean transfer = payment.getSelectedItemPosition() == 1;
            long receivedValue = transfer ? due : parseMoney(received.getText() == null ? "" : received.getText().toString());
            if (!transfer && receivedValue < due) {
                received.setError("المبلغ المستلم أقل من المطلوب");
                FeedbackManager.error(requireContext());
                return;
            }
            confirm.setEnabled(false);
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                Order sale = db.completeSale(
                        selectedItems,
                        transfer ? "transfer" : "cash",
                        discountValue,
                        receivedValue,
                        notes.getText() == null ? "" : notes.getText().toString());
                selectedItems.clear();
                checkoutDialog.dismiss();
                cartDialog.dismiss();
                loadData();
                FeedbackManager.success(requireContext());
                StringBuilder message = new StringBuilder()
                        .append("رقم الفاتورة: #").append(sale.getOrderNumber())
                        .append("\nالإجمالي: ").append(LocalFormat.getCurrencyFormat(sale.getOrderTotalAmount()));
                if (sale.getDiscountAmount() > 0) {
                    message.append("\nالخصم: ").append(LocalFormat.getCurrencyFormat(sale.getDiscountAmount()));
                }
                if (sale.getChangeAmount() > 0) {
                    message.append("\nالباقي للعميل: ").append(LocalFormat.getCurrencyFormat(sale.getChangeAmount()));
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("تم البيع بنجاح")
                        .setMessage(message.toString())
                        .setPositiveButton("حسنًا", null)
                        .show();
            } catch (Exception e) {
                confirm.setEnabled(true);
                FeedbackManager.error(requireContext());
                Toast.makeText(requireContext(), "تعذر إتمام البيع: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        checkoutDialog.show();
        Window window = checkoutDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private long parseMoney(String value) {
        if (value == null) return 0;
        String normalized = value.trim()
                .replace("٠", "0").replace("١", "1").replace("٢", "2").replace("٣", "3")
                .replace("٤", "4").replace("٥", "5").replace("٦", "6").replace("٧", "7")
                .replace("٨", "8").replace("٩", "9").replace("٬", "").replace(",", "");
        if (normalized.isEmpty()) return 0;
        try { return Math.max(0, Long.parseLong(normalized)); }
        catch (NumberFormatException ignored) { return 0; }
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
