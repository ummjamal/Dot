package com.module.dot.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.module.dot.BuildConfig;
import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.utils.BackupManager;
import com.module.dot.utils.LocalFormat;
import com.module.dot.view.MainActivity;

import java.io.InputStream;
import java.io.OutputStream;

public class SettingsFragment extends Fragment {

    private final ActivityResultLauncher<String> createBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"), this::writeBackup);

    private final ActivityResultLauncher<String[]> restoreBackupLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::restoreBackup);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText store = view.findViewById(R.id.settingsStoreName);
        TextInputEditText owner = view.findViewById(R.id.settingsOwnerName);
        TextInputEditText email = view.findViewById(R.id.settingsEmail);
        TextInputEditText phone = view.findViewById(R.id.settingsPhone);
        TextInputEditText address = view.findViewById(R.id.settingsAddress);
        TextInputEditText password = view.findViewById(R.id.settingsPassword);
        Button save = view.findViewById(R.id.settingsSave);
        Button backup = view.findViewById(R.id.settingsBackup);
        Button restore = view.findViewById(R.id.settingsRestore);
        Button developerCall = view.findViewById(R.id.developerCall);
        Button developerWhatsapp = view.findViewById(R.id.developerWhatsapp);
        Button developerEmail = view.findViewById(R.id.developerEmail);
        TextView version = view.findViewById(R.id.settingsVersion);

        try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
            store.setText(db.getSetting("store_name", "بقالة الشعيبي"));
            owner.setText(db.getSetting("owner_name", getString(R.string.owner_default_name)));
            email.setText(db.getSetting("owner_email", getString(R.string.owner_default_email)));
            phone.setText(db.getSetting("owner_phone", ""));
            address.setText(db.getSetting("store_address", "الضالع - اليمن"));
        }

        version.setText("الإصدار " + BuildConfig.VERSION_NAME + " • يعمل دون إنترنت");

        save.setOnClickListener(v -> {
            String storeValue = String.valueOf(store.getText()).trim();
            String ownerValue = String.valueOf(owner.getText()).trim();
            String emailValue = String.valueOf(email.getText()).trim();
            String phoneValue = String.valueOf(phone.getText()).trim();
            String addressValue = String.valueOf(address.getText()).trim();
            String passValue = String.valueOf(password.getText());

            if (storeValue.isEmpty() || ownerValue.isEmpty() || emailValue.isEmpty()) {
                Toast.makeText(requireContext(), "اسم البقالة واسم المالك والبريد مطلوبة", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                email.setError("أدخل بريدًا صحيحًا");
                return;
            }
            if (!passValue.isEmpty() && passValue.length() < 6) {
                Toast.makeText(requireContext(), "كلمة المرور الجديدة يجب ألا تقل عن 6 أحرف", Toast.LENGTH_SHORT).show();
                return;
            }

            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                db.setSetting("store_name", storeValue);
                db.setSetting("owner_name", ownerValue);
                db.setSetting("owner_email", emailValue);
                db.setSetting("owner_phone", phoneValue);
                db.setSetting("store_address", addressValue);
                if (!db.updateAdmin(ownerValue, emailValue, passValue.isEmpty() ? null : passValue)) {
                    Toast.makeText(requireContext(), "تعذر تحديث حساب المالك", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            password.setText("");
            ((MainActivity) requireActivity()).reloadCurrentUser();
            Toast.makeText(requireContext(), "تم حفظ الإعدادات وحساب المالك", Toast.LENGTH_SHORT).show();
        });

        backup.setOnClickListener(v -> {
            String[] dt = LocalFormat.getCurrentDateTime();
            String safeDate = dt[0].replace("-", "") + "-" + dt[1].replace(":", "");
            createBackupLauncher.launch("بقالة-الشعيبي-نسخة-احتياطية-" + safeDate + ".zip");
        });

        restore.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setTitle("استعادة نسخة احتياطية")
                .setMessage("سيتم استبدال بيانات البقالة الحالية بالبيانات الموجودة في النسخة الاحتياطية. يفضل إنشاء نسخة جديدة قبل الاستعادة. هل تريد المتابعة؟")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("متابعة", (dialog, which) ->
                        restoreBackupLauncher.launch(new String[]{"application/zip", "application/octet-stream"}))
                .show());

        developerCall.setOnClickListener(v -> openDialer(getString(R.string.developer_phone)));
        developerWhatsapp.setOnClickListener(v -> openWhatsApp(getString(R.string.developer_whatsapp)));
        developerEmail.setOnClickListener(v -> openEmail(getString(R.string.developer_email)));
    }

    private void openDialer(String number) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWhatsApp(String number) {
        try {
            String digits = number.replace("+", "").replace(" ", "");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر فتح واتساب", Toast.LENGTH_SHORT).show();
        }
    }

    private void openEmail(String email) {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "تواصل من تطبيق بقالة الشعيبي");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "تعذر فتح تطبيق البريد", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeBackup(Uri uri) {
        if (uri == null || !isAdded()) return;
        try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri)) {
            BackupManager.createBackup(requireContext(), out);
            Toast.makeText(requireContext(), "تم حفظ النسخة الاحتياطية بنجاح", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "فشل إنشاء النسخة الاحتياطية: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restoreBackup(Uri uri) {
        if (uri == null || !isAdded()) return;
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            BackupManager.restoreBackup(requireContext(), in);
            Toast.makeText(requireContext(), "تمت استعادة البيانات. سيتم تحديث التطبيق الآن.", Toast.LENGTH_LONG).show();
            requireActivity().recreate();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "فشل استعادة النسخة: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
