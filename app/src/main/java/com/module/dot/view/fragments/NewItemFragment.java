package com.module.dot.view.fragments;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.model.Item;
import com.module.dot.utils.FileManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class NewItemFragment extends Fragment {
    private static final String BARCODE_KEY = "new_item_barcode";
    private static final String ADD_CATEGORY = "➕ إضافة قسم جديد";
    private ImageView image;
    private TextInputEditText name, salePrice, purchasePrice, stock, barcode, minStock, description;
    private Spinner category, unit;
    private ArrayAdapter<String> categoryAdapter;
    private final ArrayList<String> categories = new ArrayList<>();
    private boolean imageSelected = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_item_, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        image = view.findViewById(R.id.newItemImage);
        name = view.findViewById(R.id.itemNameText);
        salePrice = view.findViewById(R.id.unitPriceText);
        purchasePrice = view.findViewById(R.id.wholesalesPrice);
        stock = view.findViewById(R.id.stockText);
        barcode = view.findViewById(R.id.SKUText);
        minStock = view.findViewById(R.id.minStockText);
        description = view.findViewById(R.id.productDescriptionText);
        category = view.findViewById(R.id.productCategoryText);
        unit = view.findViewById(R.id.unitSpinner);
        Button scan = view.findViewById(R.id.scanBarcodeButton);
        Button save = view.findViewById(R.id.saveButton);
        Button cancel = view.findViewById(R.id.cancelButton);

        setupCategories();
        unit.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("حبة", "علبة", "كرتون", "باكت", "كيس", "كيلو", "جرام", "لتر", "نصف لتر", "درزن", "ربطة")));
        purchasePrice.setText("0");
        stock.setText("0");
        minStock.setText("5");

        getParentFragmentManager().setFragmentResultListener(BARCODE_KEY, getViewLifecycleOwner(), (key, bundle) ->
                barcode.setText(bundle.getString(BarcodeScannerDialogFragment.RESULT_BARCODE, "")));
        scan.setOnClickListener(v -> BarcodeScannerDialogFragment.newInstance(BARCODE_KEY)
                .show(getParentFragmentManager(), "newItemScanner"));

        ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        image.setImageURI(result.getData().getData());
                        imageSelected = true;
                    }
                });
        image.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            imagePicker.launch(intent);
        });
        save.setOnClickListener(v -> saveItem());
        cancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupCategories() {
        reloadCategories();
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        category.setAdapter(categoryAdapter);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < categories.size() && ADD_CATEGORY.equals(categories.get(position))) {
                    category.setSelection(0);
                    showAddCategory();
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void reloadCategories() {
        categories.clear();
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { categories.addAll(db.getCategories()); }
        categories.add(ADD_CATEGORY);
        if (categoryAdapter != null) categoryAdapter.notifyDataSetChanged();
    }

    private void showAddCategory() {
        EditText input = new EditText(requireContext());
        input.setHint("اسم القسم الجديد");
        input.setSingleLine(true);
        new AlertDialog.Builder(requireContext()).setTitle("إضافة قسم جديد").setView(input)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إضافة", (d, w) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) return;
                    try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                        if (!db.addCategory(value)) {
                            Toast.makeText(requireContext(), "القسم موجود مسبقًا", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    reloadCategories();
                    int i = categories.indexOf(value);
                    if (i >= 0) category.setSelection(i);
                }).show();
    }

    private void saveItem() {
        try {
            String itemName = String.valueOf(name.getText()).trim();
            String saleText = normalize(String.valueOf(salePrice.getText()));
            String purchaseText = normalize(String.valueOf(purchasePrice.getText()));
            String stockText = normalize(String.valueOf(stock.getText()));
            String minText = normalize(String.valueOf(minStock.getText()));

            if (itemName.isEmpty()) { name.setError("اسم الصنف مطلوب"); name.requestFocus(); return; }
            if (saleText.isEmpty()) { salePrice.setError("سعر البيع مطلوب"); salePrice.requestFocus(); return; }

            double sale = Double.parseDouble(saleText);
            double purchase = purchaseText.isEmpty() ? 0 : Double.parseDouble(purchaseText);
            int qty = stockText.isEmpty() ? 0 : Integer.parseInt(stockText);
            int min = minText.isEmpty() ? 5 : Integer.parseInt(minText);

            if (sale <= 0) { salePrice.setError("سعر البيع يجب أن يكون أكبر من صفر"); return; }
            if (purchase < 0 || qty < 0 || min < 0) {
                Toast.makeText(requireContext(), "السعر والكمية وحد التنبيه لا يمكن أن تكون سالبة", Toast.LENGTH_LONG).show();
                return;
            }

            String selectedCategory = String.valueOf(category.getSelectedItem());
            if (ADD_CATEGORY.equals(selectedCategory)) selectedCategory = "أخرى";
            String id = UUID.randomUUID().toString();
            Item item = new Item(itemName, sale, selectedCategory, String.valueOf(barcode.getText()).trim(),
                    String.valueOf(unit.getSelectedItem()), qty, purchase, 0, String.valueOf(description.getText()).trim(), min);
            item.setGlobalID(id);
            item.setCreatorID("local-admin");

            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                // First commit validated business data. This prevents orphaned images when a barcode is duplicated.
                db.createItem(item);
                if (imageSelected) {
                    Drawable drawable = image.getDrawable();
                    if (drawable != null) {
                        FileManager.saveImageLocally(requireContext(), drawable, "Items", id);
                        item.setImagePath(id);
                        db.updateItem(item);
                    }
                }
            }

            Toast.makeText(requireContext(), "تمت إضافة الصنف بنجاح", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new ItemsFragment()).commit();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "تحقق من السعر والكمية وحد التنبيه", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر إضافة الصنف: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String normalize(String value) {
        return value.trim().replace("٠","0").replace("١","1").replace("٢","2").replace("٣","3")
                .replace("٤","4").replace("٥","5").replace("٦","6").replace("٧","7")
                .replace("٨","8").replace("٩","9").replace("٫",".").replace("٬","").replace(",","");
    }
}
