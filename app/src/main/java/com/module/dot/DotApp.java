package com.module.dot;

import android.app.Application;
import com.module.dot.data.local.GroceryDatabase;

public class DotApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try (GroceryDatabase database = new GroceryDatabase(this)) {
            database.getWritableDatabase();
        }
    }
}
