package com.alshuibi.grocery.view;

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
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.alshuibi.grocery.R;
import com.alshuibi.grocery.data.local.GroceryDatabase;
import com.alshuibi.grocery.model.User;
import com.alshuibi.grocery.utils.SessionManager;
import com.alshuibi.grocery.view.fragments.CustomersFragment;
import com.alshuibi.grocery.view.fragments.HomeFragment;
import com.alshuibi.grocery.view.fragments.ItemsFragment;
import com.alshuibi.grocery.view.fragments.LoginFragment;
import com.alshuibi.grocery.view.fragments.OrdersFragment;
import com.alshuibi.grocery.view.fragments.SettingsFragment;
import com.alshuibi.grocery.view.fragments.TransactionsFragment;
import com.alshuibi.grocery.view.fragments.WorkersFragment;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;
    private View navigationHeader;
    private SessionManager sessionManager;
    private boolean syncingBottomSelection = false;
    public static User currentUser;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configureSystemBars();

        sessionManager = new SessionManager(this);
        try (GroceryDatabase db = new GroceryDatabase(this)) {
            if (!sessionManager.isFirstRunComplete()) sessionManager.completeFirstRunAndLogin();
            if (sessionManager.isLoggedIn()) {
                currentUser = db.getUserByGlobalId(sessionManager.getCurrentUserId());
                if (currentUser == null) {
                    currentUser = db.getAdminUser();
                    if (currentUser != null) sessionManager.setCurrentUserId(currentUser.getGlobalID());
                }
            }
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        navigationHeader = navigationView.getHeaderView(0);

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (syncingBottomSelection) return true;
            int id = item.getItemId();
            if (id == R.id.nav_more) {
                drawerLayout.openDrawer(GravityCompat.START);
                return false;
            }
            handleNavigation(id);
            return true;
        });

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

    private boolean isOwner() {
        return currentUser != null && "Administrator".equalsIgnoreCase(currentUser.getPositionTitle());
    }

    private void applyRoleVisibility() {
        MenuItem workers = navigationView.getMenu().findItem(R.id.nav_workers);
        MenuItem settings = navigationView.getMenu().findItem(R.id.nav_settings);
        if (workers != null) workers.setVisible(isOwner());
        if (settings != null) settings.setVisible(isOwner());
    }

    private void enterApplication(boolean openHome) {
        toolbar.setVisibility(View.VISIBLE);
        bottomNavigation.setVisibility(View.VISIBLE);
        applyRoleVisibility();
        refreshHeader();
        if (openHome) handleNavigation(R.id.nav_home);
    }

    private void showLogin() {
        toolbar.setVisibility(View.GONE);
        bottomNavigation.setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new LoginFragment()).commit();
    }

    public void onLoginSuccess(User user) {
        currentUser = user;
        sessionManager.setCurrentUserId(user.getGlobalID());
        sessionManager.setLoggedIn(true);
        enterApplication(true);
    }

    public void reloadCurrentUser() {
        try (GroceryDatabase db = new GroceryDatabase(this)) {
            User saved = db.getUserByGlobalId(sessionManager.getCurrentUserId());
            currentUser = saved == null ? db.getAdminUser() : saved;
        }
        applyRoleVisibility();
        refreshHeader();
    }

    public void refreshHeader() {
        if (navigationHeader == null || toolbar == null) return;
        try (GroceryDatabase db = new GroceryDatabase(this)) {
            String storeName = db.getSetting("store_name", "بقالة الشعيبي");
            String ownerName = db.getSetting("owner_name", getString(R.string.owner_default_name));
            String email = currentUser == null ? db.getSetting("owner_email", getString(R.string.owner_default_email)) : currentUser.getEmail();
            String displayName = currentUser == null ? ownerName : currentUser.getFullName().trim();
            toolbar.setTitle(storeName);
            toolbar.setSubtitle(isOwner() ? "إدارة البقالة" : "نقطة بيع العامل");

            CircleImageView image = navigationHeader.findViewById(R.id.iv_profile_image);
            TextView initial = navigationHeader.findViewById(R.id.tv_initials);
            TextView fullName = navigationHeader.findViewById(R.id.tv_fullName);
            TextView role = navigationHeader.findViewById(R.id.tv_role);
            TextView emailView = navigationHeader.findViewById(R.id.tv_email);
            image.setVisibility(View.GONE);
            initial.setVisibility(View.VISIBLE);
            initial.setText(displayName.isEmpty() ? "ع" : displayName.substring(0, 1));
            fullName.setText(displayName);
            role.setText(isOwner() ? "مالك ومدير البقالة" : "عامل مبيعات");
            emailView.setText(email);
        }
    }

    @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_logout) confirmLogout();
        else handleNavigation(id);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleNavigation(int id) {
        Fragment fragment;
        int bottomId = id;
        if (id == R.id.nav_home) fragment = new HomeFragment();
        else if (id == R.id.nav_customers) fragment = new CustomersFragment();
        else if (id == R.id.nav_items) fragment = new ItemsFragment();
        else if (id == R.id.nav_orders) fragment = new OrdersFragment();
        else if (id == R.id.nav_transactions) { fragment = new TransactionsFragment(); bottomId = R.id.nav_more; }
        else if (id == R.id.nav_workers) { fragment = new WorkersFragment(); bottomId = R.id.nav_more; }
        else if (id == R.id.nav_settings) { fragment = new SettingsFragment(); bottomId = R.id.nav_more; }
        else return;

        navigateTo(fragment);
        MenuItem drawerItem = navigationView.getMenu().findItem(id);
        if (drawerItem != null) drawerItem.setChecked(true);
        if (bottomId != R.id.nav_more) {
            syncingBottomSelection = true;
            bottomNavigation.setSelectedItemId(bottomId);
            syncingBottomSelection = false;
        }
    }

    private void navigateTo(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fragment_enter, R.anim.fragment_exit)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage("تسجيل الخروج لا يحذف الأصناف أو المبيعات أو العملاء أو الديون.")
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    sessionManager.setLoggedIn(false);
                    currentUser = null;
                    showLogin();
                    Toast.makeText(this, "تم تسجيل الخروج بأمان", Toast.LENGTH_SHORT).show();
                }).show();
    }

    public void enableNavigationViews(int visibility) {
        toolbar.setVisibility(visibility);
        bottomNavigation.setVisibility(visibility);
    }
}
