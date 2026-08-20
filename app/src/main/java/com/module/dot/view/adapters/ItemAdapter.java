package com.module.dot.view.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.module.dot.R;
import com.module.dot.model.Item;
import com.module.dot.utils.FileManager;
import com.module.dot.utils.LocalFormat;
import com.module.dot.view.fragments.HomeFragment;

import java.util.ArrayList;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {
    public interface OnItemActionListener {
        void onItemLongClick(Item item);
    }

    private final ArrayList<Item> items;
    private final Context context;
    private HomeFragment homeFragment;
    private OnItemActionListener actionListener;

    public ItemAdapter(ArrayList<Item> items, Context context) {
        this.items = items;
        this.context = context;
    }

    public ItemAdapter(ArrayList<Item> items, Context context, HomeFragment homeFragment) {
        this(items, context);
        this.homeFragment = homeFragment;
    }

    public ItemAdapter(ArrayList<Item> items, Context context, OnItemActionListener listener) {
        this(items, context);
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_design, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = items.get(position);
        Drawable image = null;
        if (item.getImagePath() != null && !item.getImagePath().trim().isEmpty()) {
            image = FileManager.loadImageLocally(context, "Items", item.getImagePath());
        }
        if (image == null) image = ContextCompat.getDrawable(context, R.drawable.baseline_no_image_24);
        holder.image.setImageDrawable(image);
        holder.name.setText(item.getName());
        holder.category.setText(item.getCategory().isEmpty() ? "بدون قسم" : item.getCategory());
        holder.price.setText(LocalFormat.getCurrencyFormat(item.getPrice()));
        holder.unit.setText(item.getUnitType());

        if (item.getStock() <= 0) {
            holder.stock.setText("نافد");
            holder.stock.setTextColor(ContextCompat.getColor(context, R.color.brand_error));
            holder.stock.setBackgroundResource(R.drawable.badge_error);
        } else if (item.getStock() <= item.getMinStock()) {
            holder.stock.setText("متبقي " + item.getStock());
            holder.stock.setTextColor(ContextCompat.getColor(context, R.color.brand_warning));
            holder.stock.setBackgroundResource(R.drawable.badge_warning);
        } else {
            holder.stock.setText("المخزون " + item.getStock());
            holder.stock.setTextColor(ContextCompat.getColor(context, R.color.brand_success));
            holder.stock.setBackgroundResource(R.drawable.badge_success);
        }

        holder.itemLayout.setOnClickListener(v -> {
            if (homeFragment != null) homeFragment.addItemToCart(item);
        });
        holder.itemLayout.setOnLongClickListener(v -> {
            if (actionListener != null) {
                actionListener.onItemLongClick(item);
                return true;
            }
            return false;
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        LinearLayout itemLayout;
        ImageView image;
        TextView name, category, price, stock, unit;
        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.itemLL);
            image = itemView.findViewById(R.id.itemImageDesign);
            name = itemView.findViewById(R.id.itemNameHolderDesign);
            category = itemView.findViewById(R.id.itemCategoryDesign);
            price = itemView.findViewById(R.id.itemPriceHolderDesign);
            stock = itemView.findViewById(R.id.itemStockDesign);
            unit = itemView.findViewById(R.id.itemUnitTypeHolderDesign);
        }
    }
}
