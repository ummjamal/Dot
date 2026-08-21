package com.alshuibi.grocery.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.Customer;
import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.model.Order;
import com.alshuibi.grocery.utils.LocalFormat;
import com.alshuibi.grocery.view.MainActivity;
import com.alshuibi.grocery.view.adapters.CustomerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CustomersFragment extends Fragment {
    private final ArrayList<Customer> all = new ArrayList<>();
    private final ArrayList<Customer> visible = new ArrayList<>();
    private CustomerAdapter adapter;
    private RecyclerView list;
    private LinearLayout empty;
    private TextView countView, receivablesView;

    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
                ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results == null || results.isEmpty()) return;
                handleVoiceCredit(results.get(0));
            });

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customers, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(R.id.customerList);
        empty = view.findViewById(R.id.customerEmpty);
        countView = view.findViewById(R.id.customersCount);
        receivablesView = view.findViewById(R.id.totalReceivables);
        SearchView search = view.findViewById(R.id.customerSearch);
        ExtendedFloatingActionButton add = view.findViewById(R.id.addCustomerButton);
        FloatingActionButton voice = view.findViewById(R.id.voiceDebtButton);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CustomerAdapter(visible, requireContext(), this::showCustomerAccount);
        list.setAdapter(adapter);

        search.setIconifiedByDefault(false);
        search.setIconified(false);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { filter(query); return true; }
            @Override public boolean onQueryTextChange(String newText) { filter(newText); return true; }
        });

        add.setOnClickListener(v -> showCustomerEditor(null));
        voice.setOnClickListener(v -> startVoiceEntry());
        load();
    }

    @Override public void onResume() {
        super.onResume();
        if (adapter != null) load();
    }

    private void load() {
        all.clear(); visible.clear();
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            all.addAll(db.getCustomers());
            visible.addAll(all);
            countView.setText(all.size() + " عميل");
            receivablesView.setText("إجمالي الديون: " + LocalFormat.getCurrencyFormat(db.getTotalReceivables()));
        }
        refresh();
    }

    private void filter(String q) {
        String query = normalize(q);
        visible.clear();
        if (query.isEmpty()) visible.addAll(all);
        else for (Customer c : all) {
            if (normalize(c.getName()).contains(query) || normalize(c.getPhone()).contains(query)) visible.add(c);
        }
        refresh();
    }

    private void refresh() {
        adapter.notifyDataSetChanged();
        boolean isEmpty = visible.isEmpty();
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showCustomerEditor(@Nullable Customer existing) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_customer, null, false);
        TextInputEditText name = content.findViewById(R.id.customerNameInput);
        TextInputEditText phone = content.findViewById(R.id.customerPhoneInput);
        TextInputEditText address = content.findViewById(R.id.customerAddressInput);
        TextInputEditText limit = content.findViewById(R.id.customerLimitInput);
        if (existing != null) {
            name.setText(existing.getName());
            phone.setText(existing.getPhone());
            address.setText(existing.getAddress());
            limit.setText(String.valueOf(existing.getCreditLimit()));
        }
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(existing == null ? "إضافة عميل" : "تعديل العميل")
                .setView(content)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("حفظ", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = text(name);
            if (n.isEmpty()) { name.setError("اسم العميل مطلوب"); return; }
            Customer c = existing == null ? new Customer() : existing;
            c.setName(n);
            c.setPhone(text(phone));
            c.setAddress(text(address));
            c.setCreditLimit(parseMoney(text(limit)));
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                if (!db.saveCustomer(c)) { Toast.makeText(requireContext(), "تعذر حفظ العميل", Toast.LENGTH_LONG).show(); return; }
            }
            dialog.dismiss();
            load();
        }));
        dialog.show();
    }

    private void showCustomerAccount(Customer customer) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_customer_account, null, false);
        TextView name = content.findViewById(R.id.accountCustomerName);
        TextView balance = content.findViewById(R.id.accountBalance);
        TextView meta = content.findViewById(R.id.accountMeta);
        TextView ledger = content.findViewById(R.id.accountLedger);
        TextInputEditText payment = content.findViewById(R.id.accountPaymentAmount);
        Button payButton = content.findViewById(R.id.accountPaymentButton);

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(content)
                .setNegativeButton("إغلاق", null)
                .setNeutralButton("تعديل", null)
                .create();

        Runnable refreshAccount = () -> {
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                Customer fresh = db.getCustomerByGlobalId(customer.getGlobalId());
                if (fresh == null) return;
                customer.setBalance(fresh.getBalance());
                name.setText(fresh.getName());
                balance.setText("الرصيد المستحق: " + LocalFormat.getCurrencyFormat(fresh.getBalance()));
                meta.setText((fresh.getPhone().isEmpty() ? "بدون هاتف" : fresh.getPhone()) +
                        (fresh.getAddress().isEmpty() ? "" : "  •  " + fresh.getAddress()));
                ArrayList<String> lines = db.getCustomerLedgerLines(fresh.getGlobalId());
                ledger.setText(lines.isEmpty() ? "لا توجد حركات حتى الآن" : android.text.TextUtils.join("\n\n", lines));
            }
        };
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> { dialog.dismiss(); showCustomerEditor(customer); });
            payButton.setOnClickListener(v -> {
                long amount = parseMoney(text(payment));
                if (amount <= 0) { payment.setError("أدخل مبلغًا صحيحًا"); return; }
                String worker = MainActivity.currentUser == null ? null : MainActivity.currentUser.getGlobalID();
                try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                    if (!db.addCustomerPayment(customer.getGlobalId(), amount, "سداد دين", worker)) {
                        Toast.makeText(requireContext(), "تعذر تسجيل السداد أو لا يوجد رصيد مستحق", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                payment.setText("");
                refreshAccount.run();
                load();
                Toast.makeText(requireContext(), "تم تسجيل السداد", Toast.LENGTH_SHORT).show();
            });
        });
        dialog.show();
        refreshAccount.run();
    }

    private void startVoiceEntry() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "مثال: محمد علي أخذ حبتين ماء وحليب واحد على الحساب");
        try { speechLauncher.launch(i); }
        catch (Exception e) { Toast.makeText(requireContext(), "خدمة التعرف الصوتي غير متاحة على هذا الهاتف", Toast.LENGTH_LONG).show(); }
    }

    private void handleVoiceCredit(String spoken) {
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            Customer customer = db.findCustomerInText(spoken);
            ArrayList<Item> inventory = new ArrayList<>();
            db.readItem(inventory);
            LinkedHashMap<Item, Integer> matched = new LinkedHashMap<>();
            String normalizedSpeech = normalize(spoken);
            for (Item item : inventory) {
                String product = normalize(item.getName());
                if (product.length() < 2) continue;
                int idx = normalizedSpeech.indexOf(product);
                if (idx >= 0) {
                    int qty = extractQuantity(normalizedSpeech, idx, idx + product.length());
                    qty = Math.max(1, Math.min(qty, item.getStock()));
                    if (item.getStock() > 0) matched.put(item, qty);
                }
            }
            if (customer == null || matched.isEmpty()) {
                String missing = (customer == null ? "لم أتعرف على اسم العميل. " : "") + (matched.isEmpty() ? "لم أتعرف على أصناف مسجلة. " : "");
                new AlertDialog.Builder(requireContext())
                        .setTitle("مراجعة الكلام")
                        .setMessage("سمعت:\n\"" + spoken + "\"\n\n" + missing + "جرّب ذكر اسم العميل واسم الصنف كما هما مسجلان في التطبيق.")
                        .setPositiveButton("حسنًا", null)
                        .show();
                return;
            }
            ArrayList<Item> cart = new ArrayList<>();
            long total = 0;
            StringBuilder preview = new StringBuilder("العميل: ").append(customer.getName()).append("\n\n");
            for (Map.Entry<Item,Integer> entry : matched.entrySet()) {
                Item source = entry.getKey();
                int qty = entry.getValue();
                Item cartItem = new Item(source.getGlobalID(), source.getName(), source.getPrice(), source.getTax(), source.getSku(), (long) qty);
                cartItem.setWholesalePrice(source.getWholesalePrice());
                cartItem.setStock(source.getStock());
                cartItem.setUnitType(source.getUnitType());
                cart.add(cartItem);
                long line = Math.round(source.getPrice()) * qty;
                total += line;
                preview.append("• ").append(source.getName()).append(" × ").append(qty)
                        .append(" = ").append(LocalFormat.getCurrencyFormat(line)).append("\n");
            }
            long finalTotal = total;
            preview.append("\nالإجمالي على الحساب: ").append(LocalFormat.getCurrencyFormat(finalTotal));
            new AlertDialog.Builder(requireContext())
                    .setTitle("تأكيد التسجيل الصوتي")
                    .setMessage(preview.toString())
                    .setNegativeButton("إلغاء", null)
                    .setPositiveButton("تأكيد على الحساب", (d, w) -> {
                        String worker = MainActivity.currentUser == null ? null : MainActivity.currentUser.getGlobalID();
                        try (GroceryDatabase writeDb = new GroceryDatabase(requireContext())) {
                            Order sale = writeDb.completeSale(cart, "credit", 0, 0, "إدخال صوتي: " + spoken, customer.getGlobalId(), worker);
                            Toast.makeText(requireContext(), "تمت إضافة فاتورة #" + sale.getOrderNumber() + " إلى حساب " + customer.getName(), Toast.LENGTH_LONG).show();
                            load();
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "تعذر تسجيل الدين: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .show();
        }
    }

    private int extractQuantity(String speech, int startIndex, int endIndex) {
        if (speech == null || speech.isEmpty()) return 1;

        // افحص ما حول اسم الصنف واختر أقرب كمية إليه. هذا يدعم:
        // "حبتين ماء" و"ماء حبتين" وحتى الجمل التي تحتوي عدة أصناف.
        int from = Math.max(0, startIndex - 24);
        int to = Math.min(speech.length(), endIndex + 24);
        String around = speech.substring(from, to);
        int localStart = startIndex - from;
        int localEnd = endIndex - from;

        int bestQty = 1;
        int bestDistance = Integer.MAX_VALUE;

        java.util.regex.Matcher numberMatcher = java.util.regex.Pattern
                .compile("(^|\\s)([0-9٠-٩]+)(?=\\s|$)")
                .matcher(around);
        while (numberMatcher.find()) {
            int qty = (int) Math.min(999, parseMoney(numberMatcher.group(2)));
            if (qty <= 0) continue;
            int pos = numberMatcher.start(2);
            int distance = pos < localStart ? localStart - pos : Math.max(0, pos - localEnd);
            if (distance < bestDistance) { bestDistance = distance; bestQty = qty; }
        }

        Map<String,Integer> words = new java.util.LinkedHashMap<>();
        words.put("حبتين",2); words.put("ثنتين",2); words.put("اثنين",2); words.put("اثنان",2);
        words.put("ثلاثه",3); words.put("ثلاث",3); words.put("اربعه",4); words.put("اربع",4);
        words.put("خمسه",5); words.put("خمس",5); words.put("سته",6); words.put("ست",6);
        words.put("سبعه",7); words.put("سبع",7); words.put("ثمانيه",8); words.put("ثمان",8);
        words.put("تسعه",9); words.put("تسع",9); words.put("عشره",10); words.put("عشر",10);
        words.put("واحده",1); words.put("واحد",1); words.put("حبه",1);

        for (Map.Entry<String,Integer> e : words.entrySet()) {
            int searchFrom = 0;
            while (true) {
                int pos = around.indexOf(e.getKey(), searchFrom);
                if (pos < 0) break;
                int distance = pos < localStart ? localStart - (pos + e.getKey().length()) : Math.max(0, pos - localEnd);
                if (distance < bestDistance) { bestDistance = distance; bestQty = e.getValue(); }
                searchFrom = pos + e.getKey().length();
            }
        }
        return bestQty;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ة','ه')
                .replace("ـ", "");
    }

    private static String text(TextInputEditText v) { return v.getText() == null ? "" : v.getText().toString().trim(); }

    private static long parseMoney(String value) {
        if (value == null) return 0;
        String n = value.trim().replace("٠","0").replace("١","1").replace("٢","2").replace("٣","3")
                .replace("٤","4").replace("٥","5").replace("٦","6").replace("٧","7").replace("٨","8").replace("٩","9")
                .replace(",","").replace("٬","");
        try { return n.isEmpty() ? 0 : Long.parseLong(n); } catch (Exception e) { return 0; }
    }
}
