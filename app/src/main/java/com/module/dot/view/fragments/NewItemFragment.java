package com.module.dot.view.fragments;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.module.dot.R;
import com.module.dot.data.local.ItemDatabase;
import com.module.dot.model.Item;
import com.module.dot.utils.FileManager;
import com.module.dot.view.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class NewItemFragment extends Fragment {

    private static final String ADD_CATEGORY = "➕ إضافة قسم جديد";

    private ImageButton step1Button;
    private ImageButton step2Button;
    private ImageButton step3Button;

    private View contactLeft;
    private View contactRight;
    private View otherRight;

    private TextView stepTwoTextView;
    private TextView stepThreeTextView;

    private ProgressBar progressBar;

    private ImageButton previousButton;
    private ImageButton nextButton;

    private FrameLayout stepContentContainer;

    private View stepOneLayout;
    private View stepTwoLayout;
    private View stepThreeLayout;

    private int currentStep = 1;

    private ImageView itemImage;

    private TextInputEditText itemName;
    private Spinner category;
    private TextInputEditText unitPrice;

    private TextInputEditText sku;
    private Spinner unitType;
    private TextInputEditText itemStock;

    private TextInputEditText wholesalePrice;
    private TextInputEditText itemTax;
    private TextInputEditText itemDescription;

    private Button saveButton;

    private FragmentActivity fragmentActivity;

    private ArrayAdapter<String> categoryAdapter;
    private final ArrayList<String> categoryOptions = new ArrayList<>();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        fragmentActivity = (FragmentActivity) context;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_new_item_,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progress_horizontal);

        step1Button = view.findViewById(R.id.stepOneButton);
        step2Button = view.findViewById(R.id.stepTwoButton);
        step3Button = view.findViewById(R.id.stepThreeButton);

        contactLeft = view.findViewById(R.id.contactLeftView);
        contactRight = view.findViewById(R.id.contactRightView);
        otherRight = view.findViewById(R.id.OtherRightView);

        stepTwoTextView = view.findViewById(R.id.stepTwoTextView);
        stepThreeTextView = view.findViewById(R.id.stepThreeTextView);

        previousButton = view.findViewById(R.id.previousButton);
        nextButton = view.findViewById(R.id.nextButton);
        saveButton = view.findViewById(R.id.saveButton);

        stepContentContainer =
                view.findViewById(R.id.stepContentContainer);

        stepOneLayout = LayoutInflater
                .from(getContext())
                .inflate(R.layout.new_item_form_step_one, null);

        stepTwoLayout = LayoutInflater
                .from(getContext())
                .inflate(R.layout.new_item_form_step_two, null);

        stepThreeLayout = LayoutInflater
                .from(getContext())
                .inflate(R.layout.new_item_form_step_three, null);

        itemImage =
                stepOneLayout.findViewById(R.id.newItemImage);

        itemName =
                stepOneLayout.findViewById(R.id.itemNameText);

        category =
                stepOneLayout.findViewById(R.id.productCategoryText);

        unitPrice =
                stepOneLayout.findViewById(R.id.unitPriceText);

        TextInputLayout skuLayout =
                stepTwoLayout.findViewById(R.id.SKULayout);

        sku =
                stepTwoLayout.findViewById(R.id.SKUText);

        unitType =
                stepTwoLayout.findViewById(R.id.unitSpinner);

        itemStock =
                stepTwoLayout.findViewById(R.id.stockText);

        wholesalePrice =
                stepThreeLayout.findViewById(R.id.wholesalesPrice);

        itemTax =
                stepThreeLayout.findViewById(R.id.taxText);

        itemDescription =
                stepThreeLayout.findViewById(
                        R.id.productDescriptionText
                );

        setupCategories();
        setupUnits();
        setupSteps();

        ActivityResultLauncher<Intent> imagePickerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {

                            if (
                                    result.getResultCode() == RESULT_OK &&
                                    result.getData() != null &&
                                    result.getData().getData() != null
                            ) {

                                Uri uri =
                                        result.getData().getData();

                                itemImage.setImageURI(uri);
                            }
                        }
                );

        itemImage.setOnClickListener(v -> {

            Intent intent = new Intent();

            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);

            imagePickerLauncher.launch(intent);
        });

        ActivityResultLauncher<ScanOptions> barcodeLauncher =
                registerForActivityResult(
                        new ScanContract(),
                        result -> {

                            if (
                                    result != null &&
                                    result.getContents() != null
                            ) {

                                sku.setText(
                                        result.getContents()
                                );
                            }
                        }
                );

        skuLayout.setEndIconOnClickListener(v -> {

            ScanOptions options = new ScanOptions();

            options.setPrompt("امسح باركود الصنف");
            options.setBeepEnabled(true);
            options.setOrientationLocked(false);

            barcodeLauncher.launch(options);
        });

        saveButton.setOnClickListener(v -> saveItem());
    }

    private void setupCategories() {

        categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryOptions
        );

        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        category.setAdapter(categoryAdapter);

        reloadCategories();

        category.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        String selected =
                                categoryOptions.get(position);

                        if (ADD_CATEGORY.equals(selected)) {

                            showAddCategoryDialog();

                            category.setSelection(0);
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent
                    ) {}
                }
        );
    }

    private void reloadCategories() {

        categoryOptions.clear();

        categoryOptions.add("اختر القسم");

        try (ItemDatabase db =
                     new ItemDatabase(requireContext())) {

            categoryOptions.addAll(
                    db.getCategories()
            );
        }

        categoryOptions.add(ADD_CATEGORY);

        categoryAdapter.notifyDataSetChanged();
    }

    private void showAddCategoryDialog() {

        EditText input =
                new EditText(requireContext());

        input.setHint("مثال: المعلبات");
        input.setSingleLine(true);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("إضافة قسم جديد")
                .setView(input)
                .setNegativeButton(
                        "إلغاء",
                        null
                )
                .setPositiveButton(
                        "إضافة",
                        (dialog, which) -> {

                            String categoryName =
                                    input.getText()
                                            .toString()
                                            .trim();

                            if (categoryName.isEmpty()) {

                                Toast.makeText(
                                        requireContext(),
                                        "أدخل اسم القسم",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            try (ItemDatabase db =
                                         new ItemDatabase(
                                                 requireContext()
                                         )) {

                                boolean added =
                                        db.addCategory(
                                                categoryName
                                        );

                                if (added) {

                                    reloadCategories();

                                    int index =
                                            categoryOptions
                                                    .indexOf(
                                                            categoryName
                                                    );

                                    if (index >= 0) {
                                        category.setSelection(
                                                index
                                        );
                                    }

                                } else {

                                    Toast.makeText(
                                            requireContext(),
                                            "القسم موجود مسبقًا",
                                            Toast.LENGTH_SHORT
                                    ).show();
                                }
                            }
                        }
                )
                .show();
    }

    private void setupUnits() {

        ArrayList<String> units =
                new ArrayList<>(
                        Arrays.asList(
                                "اختر الوحدة",
                                "حبة",
                                "علبة",
                                "كرتون",
                                "باكت",
                                "كيس",
                                "كيلو",
                                "نصف كيلو",
                                "جرام",
                                "لتر",
                                "نصف لتر",
                                "درزن",
                                "ربطة"
                        )
                );

        ArrayAdapter<String> unitAdapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        units
                );

        unitAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        unitType.setAdapter(unitAdapter);
    }

    private void setupSteps() {

        progressBar.setProgress(33);

        step1Button.setOnClickListener(
                v -> showStepContent(1)
        );

        step2Button.setOnClickListener(
                v -> showStepContent(2)
        );

        step3Button.setOnClickListener(
                v -> showStepContent(3)
        );

        previousButton.setOnClickListener(v -> {
            currentStep--;
            showStepContent(currentStep);
        });

        nextButton.setOnClickListener(v -> {
            currentStep++;
            showStepContent(currentStep);
        });

        showStepContent(currentStep);
    }

    private void saveItem() {

        Item newItem = getItemFromForm();

        if (newItem == null) {
            return;
        }

        String globalId =
                UUID.randomUUID().toString();

        newItem.setGlobalID(globalId);

        String creatorId = "local-admin";

        if (MainActivity.currentUser != null) {

            if (
                    MainActivity.currentUser.getCreatorID()
                            != null &&
                    !MainActivity.currentUser
                            .getCreatorID()
                            .trim()
                            .isEmpty()
            ) {

                creatorId =
                        MainActivity.currentUser
                                .getCreatorID();

            } else if (
                    MainActivity.currentUser
                            .getGlobalID() != null
            ) {

                creatorId =
                        MainActivity.currentUser
                                .getGlobalID();
            }
        }

        newItem.setCreatorID(creatorId);

        Drawable currentImage =
                itemImage.getDrawable();

        Drawable defaultImage =
                ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.uploading
                );

        boolean hasImage =
                currentImage != null &&
                defaultImage != null &&
                !isImageSame(
                        currentImage,
                        defaultImage
                );

        if (hasImage) {

            newItem.setImagePath(globalId);

            try {

                FileManager.saveImageLocally(
                        requireContext(),
                        currentImage,
                        "Items",
                        globalId
                );

            } catch (Exception e) {

                newItem.setImagePath(null);
            }
        }

        try (ItemDatabase db =
                     new ItemDatabase(requireContext())) {

            db.createItem(newItem);

            Toast.makeText(
                    requireContext(),
                    "تم حفظ الصنف في البقالة بنجاح",
                    Toast.LENGTH_SHORT
            ).show();

            FragmentManager fragmentManager =
                    fragmentActivity
                            .getSupportFragmentManager();

            FragmentTransaction transaction =
                    fragmentManager
                            .beginTransaction();

            transaction.replace(
                    R.id.fragment_container,
                    new ItemsFragment()
            );

            transaction.commit();

        } catch (Exception e) {

            Toast.makeText(
                    requireContext(),
                    "تعذر حفظ الصنف: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private Item getItemFromForm() {

        String name =
                String.valueOf(
                        itemName.getText()
                ).trim();

        String salePriceText =
                normalizeNumber(
                        String.valueOf(
                                unitPrice.getText()
                        )
                );

        String stockText =
                normalizeNumber(
                        String.valueOf(
                                itemStock.getText()
                        )
                );

        if (name.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "أدخل اسم الصنف",
                    Toast.LENGTH_SHORT
            ).show();

            showStepContent(1);

            return null;
        }

        if (
                category.getSelectedItemPosition() <= 0 ||
                ADD_CATEGORY.equals(
                        category.getSelectedItem()
                                .toString()
                )
        ) {

            Toast.makeText(
                    requireContext(),
                    "اختر قسم الصنف",
                    Toast.LENGTH_SHORT
            ).show();

            showStepContent(1);

            return null;
        }

        if (salePriceText.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "أدخل سعر البيع",
                    Toast.LENGTH_SHORT
            ).show();

            showStepContent(1);

            return null;
        }

        if (
                unitType.getSelectedItemPosition() <= 0
        ) {

            Toast.makeText(
                    requireContext(),
                    "اختر وحدة البيع",
                    Toast.LENGTH_SHORT
            ).show();

            showStepContent(2);

            return null;
        }

        if (stockText.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "أدخل كمية المخزون",
                    Toast.LENGTH_SHORT
            ).show();

            showStepContent(2);

            return null;
        }

        try {

            double salePrice =
                    Double.parseDouble(
                            salePriceText
                    );

            int stock =
                    Integer.parseInt(
                            stockText
                    );

            if (salePrice < 0 || stock < 0) {
                throw new NumberFormatException();
            }

            String purchaseText =
                    normalizeNumber(
                            String.valueOf(
                                    wholesalePrice.getText()
                            )
                    );

            String taxText =
                    normalizeNumber(
                            String.valueOf(
                                    itemTax.getText()
                            )
                    );

            double purchasePrice =
                    purchaseText.isEmpty()
                            ? 0
                            : Double.parseDouble(
                                    purchaseText
                            );

            double tax =
                    taxText.isEmpty()
                            ? 0
                            : Double.parseDouble(
                                    taxText
                            );

            return new Item(
                    name,
                    salePrice,
                    category.getSelectedItem()
                            .toString(),
                    String.valueOf(
                            sku.getText()
                    ).trim(),
                    unitType.getSelectedItem()
                            .toString(),
                    stock,
                    purchasePrice,
                    tax,
                    String.valueOf(
                            itemDescription.getText()
                    ).trim()
            );

        } catch (NumberFormatException e) {

            Toast.makeText(
                    requireContext(),
                    "تحقق من السعر والكمية",
                    Toast.LENGTH_SHORT
            ).show();

            return null;
        }
    }

    private String normalizeNumber(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replace("٠", "0")
                .replace("١", "1")
                .replace("٢", "2")
                .replace("٣", "3")
                .replace("٤", "4")
                .replace("٥", "5")
                .replace("٦", "6")
                .replace("٧", "7")
                .replace("٨", "8")
                .replace("٩", "9")
                .replace("٫", ".")
                .replace("٬", "")
                .replace(",", "");
    }

    private void showStepContent(int step) {

        int active =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.brand_primary
                );

        int gray =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.light_gray
                );

        int white =
                ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                );

        if (step < 1) {
            step = 1;
        }

        if (step > 3) {
            step = 3;
        }

        currentStep = step;

        if (step == 1) {

            stepContentContainer.removeAllViews();
            stepContentContainer.addView(
                    stepOneLayout
            );

            step1Button.setClickable(false);
            step2Button.setClickable(true);
            step3Button.setClickable(true);

            previousButton.setClickable(false);
            nextButton.setClickable(true);

            contactLeft.setBackgroundColor(gray);

            step2Button.setBackgroundTintList(
                    ColorStateList.valueOf(gray)
            );

            stepTwoTextView.setTextColor(gray);

            contactRight.setBackgroundColor(gray);

            step3Button.setBackgroundTintList(
                    ColorStateList.valueOf(gray)
            );

            stepThreeTextView.setTextColor(gray);

            otherRight.setBackgroundColor(gray);

            progressBar.setProgress(33);

            previousButton.setBackgroundResource(
                    R.drawable.button_style
            );

            previousButton.setBackgroundTintList(
                    ColorStateList.valueOf(gray)
            );

            previousButton.setColorFilter(white);

            nextButton.setBackgroundResource(
                    R.drawable.button_style
            );

            nextButton.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            saveButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
        }

        else if (step == 2) {

            stepContentContainer.removeAllViews();
            stepContentContainer.addView(
                    stepTwoLayout
            );

            step1Button.setClickable(true);
            step2Button.setClickable(false);
            step3Button.setClickable(true);

            previousButton.setClickable(true);
            nextButton.setClickable(true);

            contactLeft.setBackgroundColor(active);

            step2Button.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            stepTwoTextView.setTextColor(active);

            contactRight.setBackgroundColor(gray);

            step3Button.setBackgroundTintList(
                    ColorStateList.valueOf(gray)
            );

            stepThreeTextView.setTextColor(gray);

            otherRight.setBackgroundColor(gray);

            progressBar.setProgress(66);

            previousButton.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            previousButton.setColorFilter(white);

            nextButton.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            saveButton.setVisibility(View.GONE);
            nextButton.setVisibility(View.VISIBLE);
        }

        else {

            stepContentContainer.removeAllViews();
            stepContentContainer.addView(
                    stepThreeLayout
            );

            step1Button.setClickable(true);
            step2Button.setClickable(true);
            step3Button.setClickable(false);

            previousButton.setClickable(true);

            contactLeft.setBackgroundColor(active);
            contactRight.setBackgroundColor(active);

            step2Button.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            stepTwoTextView.setTextColor(active);

            step3Button.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            stepThreeTextView.setTextColor(active);

            otherRight.setBackgroundColor(active);

            progressBar.setProgress(100);

            previousButton.setBackgroundTintList(
                    ColorStateList.valueOf(active)
            );

            previousButton.setColorFilter(white);

            nextButton.setVisibility(View.GONE);
            saveButton.setVisibility(View.VISIBLE);
        }
    }

    private boolean isImageSame(
            Drawable currentDrawable,
            Drawable uploadingDrawable
    ) {

        try {

            Bitmap current =
                    getBitmapFromDrawable(
                            currentDrawable
                    );

            Bitmap uploading =
                    getBitmapFromDrawable(
                            uploadingDrawable
                    );

            return current.sameAs(uploading);

        } catch (Exception e) {

            return false;
        }
    }

    private Bitmap getBitmapFromDrawable(
            Drawable drawable
    ) {

        if (drawable instanceof BitmapDrawable) {

            return ((BitmapDrawable) drawable)
                    .getBitmap();
        }

        if (drawable instanceof VectorDrawable) {

            Bitmap bitmap =
                    Bitmap.createBitmap(
                            Math.max(
                                    1,
                                    drawable.getIntrinsicWidth()
                            ),
                            Math.max(
                                    1,
                                    drawable.getIntrinsicHeight()
                            ),
                            Bitmap.Config.ARGB_8888
                    );

            Canvas canvas =
                    new Canvas(bitmap);

            drawable.setBounds(
                    0,
                    0,
                    canvas.getWidth(),
                    canvas.getHeight()
            );

            drawable.draw(canvas);

            return bitmap;
        }

        Bitmap bitmap =
                Bitmap.createBitmap(
                        100,
                        100,
                        Bitmap.Config.ARGB_8888
                );

        Canvas canvas =
                new Canvas(bitmap);

        drawable.setBounds(
                0,
                0,
                canvas.getWidth(),
                canvas.getHeight()
        );

        drawable.draw(canvas);

        return bitmap;
    }
}