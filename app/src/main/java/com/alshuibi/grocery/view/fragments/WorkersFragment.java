package com.alshuibi.grocery.view.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.User;
import com.alshuibi.grocery.view.MainActivity;
import com.alshuibi.grocery.view.adapters.WorkerAdapter;

import java.util.ArrayList;

public class WorkersFragment extends Fragment {
    private final ArrayList<User> users = new ArrayList<>();
    private WorkerAdapter adapter;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workers, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView list = view.findViewById(R.id.workersList);
        ExtendedFloatingActionButton add = view.findViewById(R.id.addWorkerButton);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkerAdapter(users, this::showWorker);
        list.setAdapter(adapter);
        add.setOnClickListener(v -> addWorker());
        load();
    }

    private boolean isOwner() {
        return MainActivity.currentUser != null && "Administrator".equalsIgnoreCase(MainActivity.currentUser.getPositionTitle());
    }

    private void load() {
        users.clear();
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) { users.addAll(db.getActiveUsers()); }
        adapter.notifyDataSetChanged();
    }

    private void addWorker() {
        if (!isOwner()) { Toast.makeText(requireContext(), "إضافة العمال متاحة للمالك فقط", Toast.LENGTH_LONG).show(); return; }
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_worker, null, false);
        TextInputEditText name = v.findViewById(R.id.workerNameInput);
        TextInputEditText email = v.findViewById(R.id.workerEmailInput);
        TextInputEditText password = v.findViewById(R.id.workerPasswordInput);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("حساب عامل جديد")
                .setView(v)
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إنشاء", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String n = text(name), e = text(email), p = text(password);
            if (n.isEmpty()) { name.setError("مطلوب"); return; }
            if (e.isEmpty()) { email.setError("مطلوب"); return; }
            if (p.length() < 6) { password.setError("6 أحرف على الأقل"); return; }
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                if (!db.createWorker(n, e, p)) { Toast.makeText(requireContext(), "تعذر إنشاء العامل. تأكد أن البريد غير مستخدم.", Toast.LENGTH_LONG).show(); return; }
            }
            dialog.dismiss(); load();
            Toast.makeText(requireContext(), "تم إنشاء حساب العامل", Toast.LENGTH_SHORT).show();
        }));
        dialog.show();
    }

    private void showWorker(User user) {
        boolean admin = "Administrator".equalsIgnoreCase(user.getPositionTitle());
        String msg;
        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            msg = user.getFullName() + "\n" + user.getEmail() + "\n" + (admin ? "مالك / مدير" : "عامل مبيعات")
                    + "\n\nعمليات اليوم: " + db.getWorkerTodaySalesCount(user.getGlobalID())
                    + "\nمبيعات اليوم: " + com.alshuibi.grocery.utils.LocalFormat.getCurrencyFormat(db.getWorkerTodaySalesTotal(user.getGlobalID()))
                    + "\nإجمالي المبيعات المسجلة: " + com.alshuibi.grocery.utils.LocalFormat.getCurrencyFormat(db.getWorkerLifetimeSalesTotal(user.getGlobalID()));
        }
        AlertDialog.Builder b = new AlertDialog.Builder(requireContext()).setTitle("بيانات المستخدم").setMessage(msg).setNegativeButton("إغلاق", null);
        if (!admin && isOwner()) {
            b.setPositiveButton("تعطيل الحساب", (d,w) -> {
                try (GroceryDatabase db = new GroceryDatabase(requireContext())) { db.setWorkerActive(user.getGlobalID(), false); }
                load();
            });
        }
        b.show();
    }

    private static String text(TextInputEditText v) { return v.getText() == null ? "" : v.getText().toString().trim(); }
}
