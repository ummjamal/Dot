package com.module.dot.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF = "alshuibi_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_FIRST_RUN = "first_run_complete";
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean value) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply();
    }

    public boolean isFirstRunComplete() {
        return prefs.getBoolean(KEY_FIRST_RUN, false);
    }

    public void completeFirstRunAndLogin() {
        prefs.edit()
                .putBoolean(KEY_FIRST_RUN, true)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }
}
