package com.alshuibi.grocery.view.adapters;

import static com.alshuibi.grocery.utils.LocalFormat.getCurrencyFormat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.model.Order;

import java.util.ArrayList;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.Holder> {
    public interface OnOrderActionListener {
        void onCancelOrder(Order order, String reason);
    }

    private final ArrayList<Order> orders;
    private final Context context;
    private final OnOrderActionListener listener;

    public OrdersAdapter(ArrayList<Order> orders, Context context) {
        this(orders, context, null);
    }

    public OrdersAdapter(ArrayList<Order> orders, Context context, OnOrderActionListener listener) {
        this.orders = orders;
        this.context = context;
        this.listener = listener;
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.orders_design, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Order order = orders.get(position);
        boolean cancelled = "Cancelled".equalsIgnoreCase(order.getOrderStatus());
        h.number.setText("فاتورة #" + order.getOrderNumber());
        h.date.setText(order.getOrderDate());
        h.time.setText(order.getOrderTime());
        h.status.setText(cancelled ? "ملغاة" : "مكتملة");
        h.status.setBackgroundResource(cancelled ? R.drawable.badge_error : R.drawable.badge_success);
        h.status.setTextColor(ContextCompat.getColor(context, cancelled ? R.color.brand_error : R.color.brand_success));
        h.items.setText(order.getOrderTotalItems() + " عنصر");
        h.total.setText(getCurrencyFormat(order.getOrderTotalAmount()));
        h.total.setAlpha(cancelled ? 0.55f : 1f);
        h.itemView.setAlpha(cancelled ? 0.72f : 1f);

        SelectedItemsAdapter adapter = new SelectedItemsAdapter(
                order.getSelectedItemList() == null ? new ArrayList<>() : order.getSelectedItemList(), context);
        h.selected.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        h.selected.setAdapter(adapter);
        h.itemView.setOnClickListener(v -> showInvoice(order));
    }

    private void showInvoice(Order order) {
        String invoiceText = buildInvoiceText(order);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("تفاصيل الفاتورة #" + order.getOrderNumber())
                .setMessage(invoiceText)
                .setNegativeButton("إغلاق", null)
                .setNeutralButton("مشاركة", (dialog, which) -> share(invoiceText));

        if ("Completed".equalsIgnoreCase(order.getOrderStatus()) && listener != null) {
            builder.setPositiveButton("إلغاء الفاتورة", (dialog, which) -> askCancelReason(order));
        }
        builder.show();
    }

    private String buildInvoiceText(Order order) {
        String storeName = "بقالة الشعيبي";
        String footer = "شكرًا لتسوقكم من بقالة الشعيبي";
        try (GroceryDatabase db = new GroceryDatabase(context)) {
            storeName = db.getSetting("store_name", storeName);
            footer = db.getSetting("receipt_footer", footer);
        } catch (Exception ignored) { }

        StringBuilder details = new StringBuilder();
        details.append(storeName).append("\n")
                .append("فاتورة #").append(order.getOrderNumber()).append("\n")
                .append(order.getOrderDate()).append(" • ").append(order.getOrderTime()).append("\n")
                .append("الحالة: ").append("Cancelled".equalsIgnoreCase(order.getOrderStatus()) ? "ملغاة" : "مكتملة").append("\n")
                .append("الدفع: ").append(paymentLabel(order.getPaymentMethod())).append("\n\n");

        if (order.getSelectedItemList() != null) {
            for (Item item : order.getSelectedItemList()) {
                details.append("• ").append(item.getName())
                        .append(" × ").append(item.getQuantity())
                        .append(" = ").append(getCurrencyFormat(item.getPrice() * item.getQuantity()))
                        .append("\n");
            }
        }
        if (order.getDiscountAmount() > 0) {
            details.append("\nالخصم: ").append(getCurrencyFormat(order.getDiscountAmount()));
        }
        details.append("\nالإجمالي: ").append(getCurrencyFormat(order.getOrderTotalAmount()));
        if ("cash".equals(order.getPaymentMethod()) && order.getPaidAmount() > 0) {
            details.append("\nالمستلم: ").append(getCurrencyFormat(order.getPaidAmount()));
            details.append("\nالباقي: ").append(getCurrencyFormat(order.getChangeAmount()));
        }
        if (!order.getNotes().trim().isEmpty()) details.append("\nملاحظات: ").append(order.getNotes());
        details.append("\n\n").append(footer);
        return details.toString();
    }

    private String paymentLabel(String method) {
        if ("transfer".equalsIgnoreCase(method)) return "تحويل بنكي";
        if ("wallet".equalsIgnoreCase(method)) return "محفظة إلكترونية";
        if ("credit".equalsIgnoreCase(method)) return "آجل / على الحساب";
        return "نقدي";
    }

    private void share(String text) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(send, "مشاركة الفاتورة"));
    }

    private void askCancelReason(Order order) {
        EditText input = new EditText(context);
        input.setHint("سبب الإلغاء (اختياري)");
        input.setMinLines(2);
        input.setPadding(28, 18, 28, 18);
        new AlertDialog.Builder(context)
                .setTitle("إلغاء الفاتورة #" + order.getOrderNumber())
                .setMessage("سيتم إعادة الكميات إلى المخزون وتسجيل العملية في السجل. لا يمكن تكرار الإلغاء.")
                .setView(input)
                .setNegativeButton("رجوع", null)
                .setPositiveButton("تأكيد الإلغاء", (d, w) -> {
                    if (listener != null) listener.onCancelOrder(order, input.getText().toString().trim());
                })
                .show();
    }

    @Override public int getItemCount() { return orders.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView number, date, time, status, items, total;
        RecyclerView selected;
        Holder(@NonNull View v) {
            super(v);
            number = v.findViewById(R.id.orderNumber);
            date = v.findViewById(R.id.orderDate);
            time = v.findViewById(R.id.orderTime);
            status = v.findViewById(R.id.orderStatus);
            items = v.findViewById(R.id.orderTotalItems);
            total = v.findViewById(R.id.orderTotal);
            selected = v.findViewById(R.id.selectedItemRV);
        }
    }
}
