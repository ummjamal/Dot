package com.alshuibi.grocery.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "alshuibi_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_FIRST_RUN = "first_run_complete";
    private static final String KEY_USER_ID = "current_user_id";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() { return prefs.getBoolean(KEY_LOGGED_IN, false); }

    public void setLoggedIn(boolean value) {
        SharedPreferences.Editor e = prefs.edit().putBoolean(KEY_LOGGED_IN, value);
        if (!value) e.remove(KEY_USER_ID);
        e.apply();
    }

    public String getCurrentUserId() { return prefs.getString(KEY_USER_ID, ""); }
    public void setCurrentUserId(String id) { prefs.edit().putString(KEY_USER_ID, id == null ? "" : id).apply(); }

    public boolean isFirstRunComplete() { return prefs.getBoolean(KEY_FIRST_RUN, false); }

    public void completeFirstRunAndLogin() {
        prefs.edit()
                .putBoolean(KEY_FIRST_RUN, true)
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ID, "local-admin")
                .apply();
    }
}
