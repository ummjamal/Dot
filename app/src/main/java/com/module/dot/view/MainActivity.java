package com.module.dot.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.module.dot.R;
import com.module.dot.data.local.GroceryDatabase;
import com.module.dot.model.User;
import com.module.dot.utils.SessionManager;
import com.module.dot.view.fragments.HomeFragment;
import com.module.dot.view.fragments.ItemsFragment;
import com.module.dot.view.fragments.LoginFragment;
import com.module.dot.view.fragments.OrdersFragment;
import com.module.dot.view.fragments.SettingsFragment;
import com.module.dot.view.fragments.TransactionsFragment;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private View navigationHeader;
    private SessionManager sessionManager;
    public static User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureSystemBars();

        sessionManager = new SessionManager(this);
        try (GroceryDatabase db = new GroceryDatabase(this)) {
            if (!sessionManager.isFirstRunComplete()) sessionManager.completeFirstRunAndLogin();
            if (sessionManager.isLoggedIn()) currentUser = db.getAdminUser();
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        navigationHeader = navigationView.getHeaderView(0);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // V3 owner-first: multiple accounts remain prepared at DB level, but no hidden developer/admin backdoor exists.
        navigationView.getMenu().findItem(R.id.nav_users).setVisible(false);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
                else if (getSupportFragmentManager().getBackStackEntryCount() > 0) getSupportFragmentManager().popBackStack();
                else finish();
            }
        });

        if (sessionManager.isLoggedIn() && currentUser != null) enterApplication(savedInstanceState == null);
        else showLogin();
    }

    @SuppressWarnings("deprecation")
    private void configureSystemBars() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.brand_primary_dark));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.white));
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);
    }

    private void enterApplication(boolean openHome) {
        toolbar.setVisibility(View.VISIBLE);
        refreshHeader();
        if (openHome) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private void showLogin() {
        toolbar.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }

    public void onLoginSuccess(User user) {
        currentUser = user;
        sessionManager.setLoggedIn(true);
        enterApplication(true);
    }

    public void reloadCurrentUser() {
        try (GroceryDatabase db = new GroceryDatabase(this)) { currentUser = db.getAdminUser(); }
        refreshHeader();
    }

    public void refreshHeader() {
        if (navigationHeader == null || toolbar == null) return;
        try (GroceryDatabase db = new GroceryDatabase(this)) {
            String storeName = db.getSetting("store_name", "بقالة الشعيبي");
            String ownerName = db.getSetting("owner_name", getString(R.string.owner_default_name));
            String email = db.getSetting("owner_email", getString(R.string.owner_default_email));
            toolbar.setTitle(storeName);
            toolbar.setSubtitle("إدارة ومبيعات");
            toolbar.setSubtitleTextColor(ContextCompat.getColor(this, R.color.brand_primary_soft));

            CircleImageView image = navigationHeader.findViewById(R.id.iv_profile_image);
            TextView initial = navigationHeader.findViewById(R.id.tv_initials);
            TextView fullName = navigationHeader.findViewById(R.id.tv_fullName);
            TextView role = navigationHeader.findViewById(R.id.tv_role);
            TextView emailView = navigationHeader.findViewById(R.id.tv_email);
            image.setVisibility(View.GONE);
            initial.setVisibility(View.VISIBLE);
            initial.setText(ownerName.trim().isEmpty() ? "ع" : ownerName.trim().substring(0, 1));
            fullName.setText(ownerName);
            role.setText(R.string.owner_role);
            emailView.setText(email);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
        } else if (id == R.id.nav_items) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new ItemsFragment()).commit();
        } else if (id == R.id.nav_orders) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new OrdersFragment()).commit();
        } else if (id == R.id.nav_transactions) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new TransactionsFragment()).commit();
        } else if (id == R.id.nav_settings) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SettingsFragment()).commit();
        } else if (id == R.id.nav_logout) {
            confirmLogout();
        }
        if (id != R.id.nav_logout) navigationView.setCheckedItem(id);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage("تسجيل الخروج لا يحذف الأصناف أو المبيعات أو الصور أو النسخ الاحتياطية.")
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    sessionManager.setLoggedIn(false);
                    currentUser = null;
                    showLogin();
                    Toast.makeText(this, "تم تسجيل الخروج بأمان", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    public void enableNavigationViews(int visibility) { toolbar.setVisibility(visibility); }
}
