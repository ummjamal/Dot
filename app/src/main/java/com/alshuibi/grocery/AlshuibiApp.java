package com.alshuibi.grocery;

import android.app.Application;
import com.alshuibi.grocery.data.local.GroceryDatabase;

public class AlshuibiApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try (GroceryDatabase database = new GroceryDatabase(this)) {
            database.getWritableDatabase();
        }
    }
}
