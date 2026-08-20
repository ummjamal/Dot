package com.module.dot.view.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.model.User;
import com.module.dot.view.MainActivity;

public class LoginFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MainActivity activity = (MainActivity) requireActivity();
        activity.enableNavigationViews(View.GONE);

        TextInputEditText email = view.findViewById(R.id.email);
        TextInputEditText password = view.findViewById(R.id.passwordText);
        Button login = view.findViewById(R.id.loginButton);
        email.setText("admin@alshuibi.local");

        login.setOnClickListener(v -> {
            String e = String.valueOf(email.getText()).trim();
            String p = String.valueOf(password.getText());
            if (e.isEmpty() || p.isEmpty()) {
                Toast.makeText(requireContext(), "أدخل البريد وكلمة المرور", Toast.LENGTH_SHORT).show();
                return;
            }
            try (GroceryDatabase db = new GroceryDatabase(requireContext())) {
                User user = db.authenticateUser(e, p);
                if (user == null) {
                    Toast.makeText(requireContext(), "بيانات الدخول غير صحيحة", Toast.LENGTH_SHORT).show();
                } else {
                    activity.onLoginSuccess(user);
                }
            }
        });
    }
}
