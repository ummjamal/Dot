package com.alshuibi.grocery.data.local;

import android.content.Context;
import androidx.annotation.Nullable;

/** Compatibility wrapper. All item operations now use one unified local database. */
public class ItemDatabase extends GroceryDatabase {
    public ItemDatabase(@Nullable Context context) {
        super(context);
    }
}
