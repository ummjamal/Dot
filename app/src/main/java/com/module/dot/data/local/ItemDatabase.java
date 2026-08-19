package com.module.dot.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.module.dot.model.Item;

import java.util.ArrayList;

public class ItemDatabase extends MyDatabaseManager {

    private static final String TAG = "ItemDatabase";

    private static final String NAME_TABLE_ITEMS = "items";

    private static final String ID_COLUMN_ITEMS = "_id";
    private static final String GLOBAL_ID_COLUMN_ITEMS = "global_id";
    private static final String IMAGE_COLUMN_ITEMS = "image";
    private static final String NAME_COLUMN_ITEMS = "name";
    private static final String PRICE_COLUMN_ITEMS = "price";
    private static final String CATEGORY_ID_COLUMN_ITEMS = "category_id";
    private static final String SKU_COLUMN_ITEMS = "sku";
    private static final String UNIT_TYPE_COLUMN_ITEMS = "unit_type";
    private static final String STOCK_QUANTITY_COLUMN_ITEMS = "stock_quantity";
    private static final String WS_PRICE_COLUMN_ITEMS = "wholesales_price";
    private static final String TAX_COLUMN_ITEMS = "tax";
    private static final String DESCRIPTION_COLUMN_ITEMS = "description";
    private static final String CREATED_DATE_COLUMN_ITEMS = "create_date";

    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_STOCK_MOVEMENTS = "stock_movements";

    public ItemDatabase(@Nullable Context context) {
        super(context);

        try {
            SQLiteDatabase db = getWritableDatabase();
            createSupportingTables(db);
            seedDefaultCategories(db);
        } catch (Exception e) {
            Log.e(TAG, "Database initialization error", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
        createSupportingTables(db);
        seedDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        createSupportingTables(db);
    }

    protected void createTable(SQLiteDatabase db) {

        String queryItems =
                "CREATE TABLE IF NOT EXISTS " + NAME_TABLE_ITEMS +
                        " (" +
                        ID_COLUMN_ITEMS + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        GLOBAL_ID_COLUMN_ITEMS + " TEXT NOT NULL UNIQUE, " +
                        IMAGE_COLUMN_ITEMS + " TEXT, " +
                        NAME_COLUMN_ITEMS + " TEXT NOT NULL, " +
                        PRICE_COLUMN_ITEMS + " REAL NOT NULL, " +
                        CATEGORY_ID_COLUMN_ITEMS + " TEXT, " +
                        SKU_COLUMN_ITEMS + " TEXT, " +
                        UNIT_TYPE_COLUMN_ITEMS + " TEXT, " +
                        STOCK_QUANTITY_COLUMN_ITEMS + " INTEGER DEFAULT 0, " +
                        WS_PRICE_COLUMN_ITEMS + " REAL DEFAULT 0, " +
                        TAX_COLUMN_ITEMS + " REAL DEFAULT 0, " +
                        DESCRIPTION_COLUMN_ITEMS + " TEXT, " +
                        CREATED_DATE_COLUMN_ITEMS +
                        " DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ");";

        db.execSQL(queryItems);
    }

    private void createSupportingTables(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL UNIQUE," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ");"
        );

        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE_STOCK_MOVEMENTS + " (" +
                        "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "item_global_id TEXT NOT NULL," +
                        "movement_type TEXT NOT NULL," +
                        "quantity INTEGER NOT NULL," +
                        "note TEXT," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ");"
        );
    }

    private void seedDefaultCategories(SQLiteDatabase db) {

        String[] categories = {
                "المواد الغذائية",
                "المشروبات",
                "المياه",
                "العصائر",
                "الألبان",
                "المعلبات",
                "البسكويت",
                "الشوكولاتة والحلويات",
                "الشيبس والتسالي",
                "الشاي والقهوة",
                "السكر",
                "الأرز والحبوب",
                "الدقيق",
                "المكرونة",
                "الزيوت والسمن",
                "البهارات",
                "الصلصات",
                "المنظفات",
                "العناية الشخصية",
                "المستلزمات المنزلية",
                "أخرى"
        };

        for (String category : categories) {
            ContentValues values = new ContentValues();
            values.put("name", category);

            db.insertWithOnConflict(
                    TABLE_CATEGORIES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
            );
        }
    }

    public ArrayList<String> getCategories() {

        ArrayList<String> list = new ArrayList<>();

        try (
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT name FROM " + TABLE_CATEGORIES +
                                " ORDER BY name COLLATE NOCASE ASC",
                        null
                )
        ) {

            while (cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed reading categories", e);
        }

        return list;
    }

    public boolean addCategory(String categoryName) {

        if (categoryName == null || categoryName.trim().isEmpty()) {
            return false;
        }

        ContentValues values = new ContentValues();
        values.put("name", categoryName.trim());

        try (SQLiteDatabase db = getWritableDatabase()) {

            long result = db.insertWithOnConflict(
                    TABLE_CATEGORIES,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
            );

            return result != -1;

        } catch (Exception e) {
            Log.e(TAG, "Failed adding category", e);
            return false;
        }
    }

    public boolean deleteCategory(String categoryName) {

        if (categoryName == null || categoryName.trim().isEmpty()) {
            return false;
        }

        try (SQLiteDatabase db = getWritableDatabase()) {

            try (
                    Cursor cursor = db.rawQuery(
                            "SELECT COUNT(*) FROM " + NAME_TABLE_ITEMS +
                                    " WHERE " + CATEGORY_ID_COLUMN_ITEMS + " = ?",
                            new String[]{categoryName}
                    )
            ) {

                if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                    return false;
                }
            }

            return db.delete(
                    TABLE_CATEGORIES,
                    "name = ?",
                    new String[]{categoryName}
            ) > 0;

        } catch (Exception e) {
            Log.e(TAG, "Failed deleting category", e);
            return false;
        }
    }

    public void createItem(Item newItem) throws SQLiteException {

        try (SQLiteDatabase db = getWritableDatabase()) {

            db.beginTransaction();

            try {

                if (isValueExists(
                        db,
                        NAME_TABLE_ITEMS,
                        GLOBAL_ID_COLUMN_ITEMS,
                        newItem.getGlobalID()
                )) {
                    throw new SQLiteException("Duplicate item ID");
                }

                ContentValues cv = getItemContentValues(newItem);

                long result = db.insertOrThrow(
                        NAME_TABLE_ITEMS,
                        null,
                        cv
                );

                if (result == -1) {
                    throw new SQLiteException("Failed to create item");
                }

                if (newItem.getStock() > 0) {
                    recordStockMovement(
                            db,
                            newItem.getGlobalID(),
                            "OPENING",
                            newItem.getStock(),
                            "رصيد افتتاحي للصنف"
                    );
                }

                db.setTransactionSuccessful();

            } finally {
                db.endTransaction();
            }

        }
    }

    public boolean updateItem(Item item) {

        if (item == null || item.getGlobalID() == null) {
            return false;
        }

        try (SQLiteDatabase db = getWritableDatabase()) {

            db.beginTransaction();

            try {

                int oldStock = 0;

                try (
                        Cursor cursor = db.rawQuery(
                                "SELECT " + STOCK_QUANTITY_COLUMN_ITEMS +
                                        " FROM " + NAME_TABLE_ITEMS +
                                        " WHERE " + GLOBAL_ID_COLUMN_ITEMS + " = ?",
                                new String[]{item.getGlobalID()}
                        )
                ) {

                    if (cursor.moveToFirst()) {
                        oldStock = cursor.getInt(0);
                    }
                }

                int rows = db.update(
                        NAME_TABLE_ITEMS,
                        getItemContentValues(item),
                        GLOBAL_ID_COLUMN_ITEMS + " = ?",
                        new String[]{item.getGlobalID()}
                );

                if (rows > 0) {

                    int difference = item.getStock() - oldStock;

                    if (difference != 0) {
                        recordStockMovement(
                                db,
                                item.getGlobalID(),
                                "ADJUSTMENT",
                                difference,
                                "تعديل يدوي للمخزون"
                        );
                    }
                }

                db.setTransactionSuccessful();

                return rows > 0;

            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed updating item", e);
            return false;
        }
    }

    public boolean deleteItem(String globalId) {

        if (globalId == null || globalId.trim().isEmpty()) {
            return false;
        }

        try (SQLiteDatabase db = getWritableDatabase()) {

            db.beginTransaction();

            try {

                db.delete(
                        TABLE_STOCK_MOVEMENTS,
                        "item_global_id = ?",
                        new String[]{globalId}
                );

                int rows = db.delete(
                        NAME_TABLE_ITEMS,
                        GLOBAL_ID_COLUMN_ITEMS + " = ?",
                        new String[]{globalId}
                );

                db.setTransactionSuccessful();

                return rows > 0;

            } finally {
                db.endTransaction();
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed deleting item", e);
            return false;
        }
    }

    private ContentValues getItemContentValues(Item item) {

        ContentValues cv = new ContentValues();

        cv.put(GLOBAL_ID_COLUMN_ITEMS, item.getGlobalID());
        cv.put(IMAGE_COLUMN_ITEMS, item.getImagePath());
        cv.put(NAME_COLUMN_ITEMS, item.getName());
        cv.put(PRICE_COLUMN_ITEMS, item.getPrice());
        cv.put(CATEGORY_ID_COLUMN_ITEMS, item.getCategory());
        cv.put(SKU_COLUMN_ITEMS, item.getSku());
        cv.put(UNIT_TYPE_COLUMN_ITEMS, item.getUnitType());
        cv.put(STOCK_QUANTITY_COLUMN_ITEMS, item.getStock());
        cv.put(WS_PRICE_COLUMN_ITEMS, item.getWholesalePrice());
        cv.put(TAX_COLUMN_ITEMS, item.getTax());
        cv.put(DESCRIPTION_COLUMN_ITEMS, item.getDescription());

        return cv;
    }

    private void recordStockMovement(
            SQLiteDatabase db,
            String itemGlobalId,
            String type,
            int quantity,
            String note
    ) {

        ContentValues values = new ContentValues();

        values.put("item_global_id", itemGlobalId);
        values.put("movement_type", type);
        values.put("quantity", quantity);
        values.put("note", note);

        db.insert(
                TABLE_STOCK_MOVEMENTS,
                null,
                values
        );
    }

    public void readItem(ArrayList<Item> itemList) {

        Cursor cursor = super.readAllData(NAME_TABLE_ITEMS);

        try {

            while (cursor.moveToNext()) {

                Item item = cursorToItem(cursor);

                itemList.add(item);
            }

        } finally {

            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public Item getItemBySku(String sku) {

        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }

        try (
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT * FROM " + NAME_TABLE_ITEMS +
                                " WHERE " + SKU_COLUMN_ITEMS + " = ? LIMIT 1",
                        new String[]{sku.trim()}
                )
        ) {

            if (cursor.moveToFirst()) {
                return cursorToItem(cursor);
            }

        } catch (Exception e) {
            Log.e(TAG, "Barcode search error", e);
        }

        return null;
    }

    public Item getItemByGlobalId(String globalId) {

        try (
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT * FROM " + NAME_TABLE_ITEMS +
                                " WHERE " + GLOBAL_ID_COLUMN_ITEMS + " = ? LIMIT 1",
                        new String[]{globalId}
                )
        ) {

            if (cursor.moveToFirst()) {
                return cursorToItem(cursor);
            }

        } catch (Exception e) {
            Log.e(TAG, "Item lookup error", e);
        }

        return null;
    }

    private Item cursorToItem(Cursor cursor) {

        Item item = new Item();

        item.setLocalID(cursor.getLong(0));
        item.setGlobalID(cursor.getString(1));
        item.setImagePath(cursor.getString(2));
        item.setName(cursor.getString(3));
        item.setPrice(cursor.getDouble(4));
        item.setCategory(cursor.getString(5));
        item.setSku(cursor.getString(6));
        item.setUnitType(cursor.getString(7));
        item.setStock(cursor.getInt(8));
        item.setWholesalePrice(cursor.getDouble(9));
        item.setTax(cursor.getDouble(10));
        item.setDescription(cursor.getString(11));

        return item;
    }

    public String getItemName(String itemGlobalID) {

        try (
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.rawQuery(
                        "SELECT " + NAME_COLUMN_ITEMS +
                                " FROM " + NAME_TABLE_ITEMS +
                                " WHERE " + GLOBAL_ID_COLUMN_ITEMS + " = ?",
                        new String[]{itemGlobalID}
                )
        ) {

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed getting item name", e);
        }

        return null;
    }
}