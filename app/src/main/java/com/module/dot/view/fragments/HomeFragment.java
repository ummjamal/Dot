package com.module.dot.view.fragments;

import static com.module.dot.utils.LocalFormat.getCurrentDateTime;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.module.dot.R;
import com.module.dot.data.local.ItemDatabase;
import com.module.dot.data.remote.FirebaseHandler;
import com.module.dot.model.Item;
import com.module.dot.model.Order;
import com.module.dot.model.Transaction;
import com.module.dot.utils.LocalFormat;
import com.module.dot.view.MainActivity;
import com.module.dot.view.adapters.ItemAdapter;
import com.module.dot.view.adapters.OrderItemAdapter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentActivity fragmentActivity;
    private Button chargeButton;

    private final ArrayList<Item> itemList = new ArrayList<>();
    private final ArrayList<Item> selectedItems = new ArrayList<>();

    private final AtomicReference<Double> totalTax =
            new AtomicReference<>(0.00);

    private final AtomicReference<Double> totalPrice =
            new AtomicReference<>(0.00);

    private String currentTax = "0 ر.ي";
    private String currentCharge = "0 ر.ي";

    public Long totalItem = 0L;

    /*
     * ماسح الباركود الجديد.
     *
     * هذه هي الطريقة الوحيدة التي سنستخدمها للمسح.
     * لا يوجد ScannerManager هنا؛ لأن النتيجة يجب أن تصل
     * من ActivityResult callback بعد انتهاء الكاميرا.
     */
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {

                        if (!isAdded()) {
                            return;
                        }

                        /*
                         * المستخدم ضغط رجوع أو أغلق الماسح
                         * بدون قراءة باركود.
                         */
                        if (
                                result == null ||
                                result.getContents() == null ||
                                result.getContents().trim().isEmpty()
                        ) {

                            Toast.makeText(
                                    requireContext(),
                                    "تم إلغاء مسح الباركود",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        String barcode =
                                result.getContents().trim();

                        addScannedBarcodeToCart(barcode);
                    }
            );

    @Override
    public void onAttach(@NonNull Context context) {

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
                R.layout.fragment_home,
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

        MainActivity mainActivity =
                (MainActivity) getActivity();

        if (mainActivity != null) {
            mainActivity.enableNavigationViews(
                    View.VISIBLE
            );
        }

        LinearLayout noData =
                view.findViewById(
                        R.id.noDataHomeFragmentLL
                );

        RecyclerView recyclerView =
                view.findViewById(
                        R.id.itemList
                );

        FloatingActionButton scanButton =
                view.findViewById(
                        R.id.scanButton
                );

        chargeButton =
                view.findViewById(
                        R.id.Charge
                );

        /*
         * مهم عند إعادة إنشاء View:
         * لا نريد تكرار الأصناف في ArrayList.
         */
        itemList.clear();

        try (
                ItemDatabase itemDatabase =
                        new ItemDatabase(
                                requireContext()
                        )
        ) {

            if (
                    itemDatabase.isTableEmpty(
                            "items"
                    )
            ) {

                itemDatabase.showEmptyStateMessage(
                        recyclerView,
                        noData
                );

            } else {

                itemDatabase.showStateMessage(
                        recyclerView,
                        noData
                );

                itemDatabase.readItem(
                        itemList
                );
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "تعذر تحميل الأصناف",
                    e
            );
        }

        recyclerView.setLayoutManager(
                new GridLayoutManager(
                        requireContext(),
                        3
                )
        );

        recyclerView.setAdapter(
                new ItemAdapter(
                        itemList,
                        requireContext(),
                        this
                )
        );

        resetTotals();

        /*
         * زر الإجمالي / سلة البيع.
         */
        chargeButton.setOnClickListener(v -> {

            Dialog dialog =
                    showButtonDialog();

            setBottomSheetHeight(
                    dialog,
                    0.58
            );
        });

        /*
         * زر مسح الباركود.
         *
         * لا نحاول قراءة النتيجة هنا.
         * فقط نفتح الكاميرا.
         *
         * النتيجة ستصل إلى barcodeLauncher أعلاه.
         */
        scanButton.setOnClickListener(v -> {

            if (itemList.isEmpty()) {

                Toast.makeText(
                        requireContext(),
                        getString(
                                R.string.empty_database
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            ScanOptions options =
                    new ScanOptions();

            /*
             * باركود المنتجات المعتاد:
             * EAN / UPC / Code 128 وغيرها.
             */
            options.setDesiredBarcodeFormats(
                    ScanOptions.ONE_D_CODE_TYPES
            );

            options.setPrompt(
                    "وجّه الكاميرا إلى باركود الصنف"
            );

            options.setBeepEnabled(true);

            /*
             * يسمح للماسح باستخدام اتجاه الجهاز المناسب.
             */
            options.setOrientationLocked(false);

            barcodeLauncher.launch(
                    options
            );
        });
    }

    /**
     * إضافة المنتج الذي تم العثور عليه بالباركود إلى سلة البيع.
     */
    private void addScannedBarcodeToCart(
            String barcode
    ) {

        Item foundItem;

        /*
         * نبحث مباشرة في SQLite.
         *
         * أفضل من الاعتماد على ArrayList فقط،
         * لأن قاعدة البيانات هي مصدر البيانات المحلي لدينا.
         */
        try (
                ItemDatabase database =
                        new ItemDatabase(
                                requireContext()
                        )
        ) {

            foundItem =
                    database.getItemBySku(
                            barcode
                    );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "خطأ أثناء البحث عن الباركود",
                    e
            );

            Toast.makeText(
                    requireContext(),
                    "تعذر البحث عن الصنف",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * الباركود غير موجود.
         */
        if (foundItem == null) {

            Toast.makeText(
                    requireContext(),
                    "الباركود غير مسجل في الأصناف",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * المنتج موجود لكن مخزونه صفر.
         */
        if (foundItem.getStock() <= 0) {

            Toast.makeText(
                    requireContext(),
                    "الصنف نافد من المخزون",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * نحسب كم وحدة من نفس المنتج موجودة بالفعل
         * في سلة البيع.
         */
        long quantityInCart =
                getQuantityInCart(
                        foundItem.getGlobalID()
                );

        /*
         * لا نسمح للسلة بتجاوز كمية المخزون.
         */
        if (
                quantityInCart >=
                        foundItem.getStock()
        ) {

            Toast.makeText(
                    requireContext(),
                    "وصلت إلى آخر كمية متوفرة من هذا الصنف",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        /*
         * ننشئ نسخة خاصة بالسلة حتى لا نعدل
         * الكائن الأصلي الخاص بالمخزون.
         */
        Item selectedItem =
                new Item(
                        foundItem.getGlobalID(),
                        foundItem.getName(),
                        foundItem.getPrice(),
                        foundItem.getTax(),
                        foundItem.getSku(),
                        1L
                );

        double tax =
                (foundItem.getTax() / 100.0)
                        *
                        foundItem.getPrice();

        totalItem++;

        addToSelectedItems(
                selectedItem
        );

        updateTax(
                tax
        );

        updateAmount(
                foundItem.getPrice()
        );

        Toast.makeText(
                requireContext(),
                "تمت إضافة " +
                        foundItem.getName() +
                        " إلى سلة البيع",
                Toast.LENGTH_SHORT
        ).show();
    }

    /**
     * معرفة عدد الوحدات الموجودة حاليًا من منتج معين في السلة.
     */
    private long getQuantityInCart(
            String globalId
    ) {

        for (Item item : selectedItems) {

            if (
                    Objects.equals(
                            globalId,
                            item.getGlobalID()
                    )
            ) {

                return item.getQuantity();
            }
        }

        return 0L;
    }

    /**
     * سلة البيع السفلية.
     */
    public Dialog showButtonDialog() {

        final Dialog bottomSheetDialog =
                new Dialog(
                        requireContext()
                );

        bottomSheetDialog.requestWindowFeature(
                Window.FEATURE_NO_TITLE
        );

        bottomSheetDialog.setContentView(
                R.layout.bottomsheet_layout
        );

        TextView taxTotal =
                bottomSheetDialog.findViewById(
                        R.id.taxTotal
                );

        TextView transactionTotal =
                bottomSheetDialog.findViewById(
                        R.id.transactionTotal
                );

        Button checkoutButton =
                bottomSheetDialog.findViewById(
                        R.id.checkoutButton
                );

        RecyclerView bottomSheetRecyclerView =
                bottomSheetDialog.findViewById(
                        R.id.transactionSheetList
                );

        taxTotal.setText(
                currentTax
        );

        transactionTotal.setText(
                currentCharge
        );

        bottomSheetRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        OrderItemAdapter orderItemAdapter =
                new OrderItemAdapter(
                        selectedItems,
                        requireContext()
                );

        bottomSheetRecyclerView.setAdapter(
                orderItemAdapter
        );

        checkoutButton.setOnClickListener(v -> {

            if (selectedItems.isEmpty()) {

                Toast.makeText(
                        requireContext(),
                        getString(
                                R.string.empty_cart
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            bottomSheetDialog.dismiss();

            Bundle result =
                    new Bundle();

            result.putString(
                    "price",
                    currentCharge
            );

            getParentFragmentManager()
                    .setFragmentResult(
                            "priceData",
                            result
                    );

            AlertDialog.Builder checkoutConfirmation =
                    new AlertDialog.Builder(
                            requireContext()
                    );

            checkoutConfirmation
                    .setTitle(
                            getString(
                                    R.string.confirm
                            )
                    )
                    .setMessage(
                            getString(
                                    R.string.confirm_checkout
                            )
                    )
                    .setNegativeButton(
                            getString(
                                    R.string.no
                            ),
                            (dialog, which) ->
                                    dialog.dismiss()
                    )
                    .setPositiveButton(
                            getString(
                                    R.string.yes
                            ),
                            (dialog, which) ->
                                    performLegacyCheckout()
                    )
                    .show();
        });

        bottomSheetDialog.show();

        Window window =
                bottomSheetDialog.getWindow();

        if (window != null) {

            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            window.setBackgroundDrawable(
                    new ColorDrawable(
                            Color.TRANSPARENT
                    )
            );

            window.getAttributes().windowAnimations =
                    R.style.DialogAnimation;

            window.setGravity(
                    Gravity.BOTTOM
            );
        }

        return bottomSheetDialog;
    }

    /**
     * نظام الدفع القديم ما زال يعتمد على Firebase.
     *
     * أبقيناه مؤقتًا حتى لا نكسر الفواتير الحالية.
     *
     * في المرحلة التالية سنستبدله بالكامل ببيع Offline
     * داخل SQLite مع خصم المخزون في Transaction واحدة.
     */
    private void performLegacyCheckout() {

        if (MainActivity.currentUser == null) {

            Toast.makeText(
                    requireContext(),
                    "بيانات المستخدم غير جاهزة",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        try {

            String creatorId =
                    MainActivity.currentUser
                            .getCreatorID();

            if (
                    creatorId == null ||
                    creatorId.trim().isEmpty()
            ) {

                creatorId =
                        MainActivity.currentUser
                                .getGlobalID();
            }

            if (
                    creatorId == null ||
                    creatorId.trim().isEmpty()
            ) {

                Toast.makeText(
                        requireContext(),
                        "تعذر تحديد المستخدم",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String[] dateTime =
                    getCurrentDateTime();

            String orderGlobalID =
                    FirebaseHandler.createOrder(
                            new Order(
                                    creatorId,
                                    dateTime[0],
                                    dateTime[1],
                                    totalPrice.get(),
                                    totalItem,
                                    "Completed",
                                    selectedItems
                            )
                    );

            if (
                    orderGlobalID == null ||
                    orderGlobalID.trim().isEmpty()
            ) {

                Toast.makeText(
                        requireContext(),
                        "تعذر إنشاء الفاتورة",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            FirebaseHandler.readOrder(
                    "orders",
                    requireContext()
            );

            dateTime =
                    getCurrentDateTime();

            FirebaseHandler.createTransaction(
                    new Transaction(
                            orderGlobalID,
                            "APPROVE",
                            totalPrice.get(),
                            "cash",
                            creatorId,
                            dateTime[0],
                            dateTime[1]
                    )
            );

            FirebaseHandler.readTransaction(
                    "transactions",
                    requireContext()
            );

            Bundle orderNumberBundle =
                    new Bundle();

            orderNumberBundle.putString(
                    "orderNumber",
                    orderGlobalID
            );

            getParentFragmentManager()
                    .setFragmentResult(
                            "orderNumberData",
                            orderNumberBundle
                    );

            FragmentManager fragmentManager =
                    fragmentActivity
                            .getSupportFragmentManager();

            FragmentTransaction fragmentTransaction =
                    fragmentManager
                            .beginTransaction();

            fragmentTransaction.replace(
                    R.id.fragment_container,
                    new ConfirmationFragment()
            );

            fragmentTransaction.commit();

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Checkout failed",
                    e
            );

            Toast.makeText(
                    requireContext(),
                    "حدث خطأ أثناء إتمام البيع",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    /**
     * تحديد ارتفاع نافذة السلة.
     */
    private void setBottomSheetHeight(
            Dialog dialog,
            double heightPercentage
    ) {

        Window window =
                dialog.getWindow();

        if (window != null) {

            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (
                            heightPercentage
                                    *
                                    getScreenHeight()
                    )
            );

            window.setGravity(
                    Gravity.BOTTOM
            );
        }
    }

    /**
     * ارتفاع شاشة الهاتف.
     */
    private int getScreenHeight() {

        DisplayMetrics displayMetrics =
                new DisplayMetrics();

        requireActivity()
                .getWindowManager()
                .getDefaultDisplay()
                .getMetrics(
                        displayMetrics
                );

        return displayMetrics.heightPixels;
    }

    /**
     * إضافة صنف للسلة.
     *
     * إذا كان موجودًا مسبقًا نزيد الكمية.
     */
    public void addToSelectedItems(
            Item newItem
    ) {

        if (newItem == null) {
            return;
        }

        for (Item item : selectedItems) {

            if (
                    Objects.equals(
                            item.getGlobalID(),
                            newItem.getGlobalID()
                    )
            ) {

                item.setQuantity(
                        item.getQuantity() + 1
                );

                return;
            }
        }

        selectedItems.add(
                newItem
        );
    }

    /**
     * تحديث المبلغ الإجمالي.
     */
    public void updateAmount(
            double amount
    ) {

        totalPrice.set(
                Math.max(
                        0.0,
                        totalPrice.get() + amount
                )
        );

        currentCharge =
                LocalFormat.getCurrencyFormat(
                        totalPrice.get()
                );

        if (chargeButton != null) {

            chargeButton.setText(
                    currentCharge
            );
        }
    }

    /**
     * تحديث الضريبة.
     */
    public void updateTax(
            double amount
    ) {

        totalTax.set(
                Math.max(
                        0.0,
                        totalTax.get() + amount
                )
        );

        currentTax =
                LocalFormat.getCurrencyFormat(
                        totalTax.get()
                );
    }

    /**
     * تصفير إجمالي السلة عند إنشاء الشاشة.
     */
    private void resetTotals() {

        totalTax.set(
                0.0
        );

        totalPrice.set(
                0.0
        );

        totalItem = 0L;

        currentTax =
                LocalFormat.getCurrencyFormat(
                        0.0
                );

        currentCharge =
                LocalFormat.getCurrencyFormat(
                        0.0
                );

        if (chargeButton != null) {

            chargeButton.setText(
                    currentCharge
            );
        }
    }
}