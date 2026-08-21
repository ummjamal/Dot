package com.alshuibi.grocery.view.adapters;

import static com.alshuibi.grocery.utils.LocalFormat.getCurrencyFormat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alshuibi.grocery.R;
import com.alshuibi.grocery.model.Transaction;

import java.util.ArrayList;

public class TransactionRecyclerAdapter extends RecyclerView.Adapter<TransactionRecyclerAdapter.Holder> {
    private final ArrayList<Transaction> transactions;
    public TransactionRecyclerAdapter(ArrayList<Transaction> transactions) { this.transactions = transactions; }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_design, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Transaction t = transactions.get(position);
        h.id.setText(t.getGlobalID());
        h.order.setText("فاتورة #" + t.getOrderNumber());
        h.date.setText(t.getTransactionDate());
        h.time.setText(t.getTransactionTime());
        boolean cancelled = "Cancelled".equalsIgnoreCase(t.getTransactionStatus());
        h.status.setText(cancelled ? "ملغاة" : "مكتملة");
        h.status.setBackgroundResource(cancelled ? R.drawable.badge_error : R.drawable.badge_success);
        h.status.setTextColor(ContextCompat.getColor(h.itemView.getContext(), cancelled ? R.color.brand_error : R.color.brand_success));
        h.total.setText(getCurrencyFormat(t.getTransactionTotal()));
        h.total.setAlpha(cancelled ? 0.55f : 1f);
        h.itemView.setAlpha(cancelled ? 0.72f : 1f);
        String pm = t.getPaymentMethod();
        if ("credit".equalsIgnoreCase(pm)) h.payment.setImageResource(R.drawable.ic_customers_24);
        else if ("transfer".equalsIgnoreCase(pm) || "wallet".equalsIgnoreCase(pm)) h.payment.setImageResource(R.drawable.baseline_compare_arrows_24);
        else h.payment.setImageResource(R.drawable.baseline_money_24);
    }
    @Override public int getItemCount() { return transactions.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        TextView date,time,order,id,status,total; ImageView payment;
        Holder(@NonNull View v) { super(v); date=v.findViewById(R.id.transactionDate); time=v.findViewById(R.id.transactionTime);
            order=v.findViewById(R.id.transactionOrderNumber); id=v.findViewById(R.id.transactionID); status=v.findViewById(R.id.transactionStatus);
            total=v.findViewById(R.id.transactionTotal); payment=v.findViewById(R.id.PaymentType); }
    }
}
