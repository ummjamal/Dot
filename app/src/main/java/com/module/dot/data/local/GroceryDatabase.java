package com.module.dot.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.module.dot.model.Item;
import com.module.dot.model.Order;
import com.module.dot.model.Transaction;
import com.module.dot.model.User;
import com.module.dot.utils.LocalFormat;
import com.module.dot.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class GroceryDatabase extends SQLiteOpenHelper {
    private static final String TAG = "GroceryDatabase";
    public static final String DATABASE_NAME = "BaqalatAlshuibi.db";
    private static final int DATABASE_VERSION = 1;
    protected final Context context;

    public GroceryDatabase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        getWritableDatabase();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "sort_order INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS items (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "global_id TEXT NOT NULL UNIQUE," +
                "image TEXT," +
                "name TEXT NOT NULL," +
                "price INTEGER NOT NULL DEFAULT 0," +
                "category TEXT NOT NULL DEFAULT 'أخرى'," +
                "sku TEXT UNIQUE," +
                "unit_type TEXT NOT NULL DEFAULT 'حبة'," +
                "stock_quantity INTEGER NOT NULL DEFAULT 0," +
                "wholesales_price INTEGER NOT NULL DEFAULT 0," +
                "tax REAL NOT NULL DEFAULT 0," +
                "description TEXT," +
                "min_stock INTEGER NOT NULL DEFAULT 5," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "item_global_id TEXT NOT NULL," +
                "movement_type TEXT NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "before_qty INTEGER NOT NULL," +
                "after_qty INTEGER NOT NULL," +
                "note TEXT," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS sales (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "global_id TEXT NOT NULL UNIQUE," +
                "sale_date TEXT NOT NULL," +
                "sale_time TEXT NOT NULL," +
                "total_amount INTEGER NOT NULL," +
                "total_cost INTEGER NOT NULL," +
                "total_profit INTEGER NOT NULL," +
                "total_items INTEGER NOT NULL," +
                "payment_method TEXT NOT NULL DEFAULT 'cash'," +
                "status TEXT NOT NULL DEFAULT 'Completed')");

        db.execSQL("CREATE TABLE IF NOT EXISTS sale_items (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER NOT NULL," +
                "item_global_id TEXT NOT NULL," +
                "item_name TEXT NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "unit_price INTEGER NOT NULL," +
                "purchase_price INTEGER NOT NULL," +
                "line_total INTEGER NOT NULL," +
                "line_cost INTEGER NOT NULL," +
                "line_profit INTEGER NOT NULL," +
                "FOREIGN KEY(sale_id) REFERENCES sales(_id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "global_id TEXT NOT NULL UNIQUE," +
                "first_name TEXT NOT NULL," +
                "last_name TEXT NOT NULL DEFAULT ''," +
                "email TEXT NOT NULL UNIQUE," +
                "role TEXT NOT NULL DEFAULT 'Administrator'," +
                "password_hash TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (" +
                "setting_key TEXT PRIMARY KEY," +
                "setting_value TEXT NOT NULL DEFAULT '')");

        seedDefaults(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is a clean database name, so no destructive migration is needed yet.
    }

    private void seedDefaults(SQLiteDatabase db) {
        String[] categories = {
                "المواد الغذائية الأساسية", "الأرز والحبوب", "الدقيق والطحين",
                "السكر والملح", "الزيوت والسمن", "المكرونة والشعيرية",
                "المعلبات", "التونة والسردين", "الفول والفاصوليا والبازلاء",
                "الصلصات ومعجون الطماطم", "البهارات والتوابل", "الشاي والقهوة",
                "الحليب والألبان", "الأجبان", "المياه", "العصائر",
                "المشروبات الغازية", "مشروبات الطاقة", "البسكويت والويفر",
                "الشوكولاتة", "الحلويات والعلك", "الشيبس والتسالي",
                "المكسرات", "التمور", "المخبوزات", "المنظفات",
                "مساحيق الغسيل", "الصابون والمنظفات السائلة", "المناديل الورقية",
                "العناية الشخصية", "الشامبو والصابون", "معجون وفرش الأسنان",
                "حفاضات الأطفال", "مستلزمات الأطفال", "المستلزمات المنزلية",
                "البطاريات واللمبات", "الأكياس البلاستيكية", "أخرى"
        };
        int sort = 1;
        for (String category : categories) {
            ContentValues cv = new ContentValues();
            cv.put("name", category);
            cv.put("sort_order", sort++);
            db.insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }

        putSetting(db, "store_name", "بقالة الشعيبي");
        putSetting(db, "owner_name", "مدير البقالة");
        putSetting(db, "owner_email", "admin@alshuibi.local");
        putSetting(db, "owner_phone", "");
        putSetting(db, "store_address", "الضالع - اليمن");
        putSetting(db, "low_stock_default", "5");

        Cursor c = db.rawQuery("SELECT COUNT(*) FROM users", null);
        boolean empty = c.moveToFirst() && c.getInt(0) == 0;
        c.close();
        if (empty) {
            ContentValues admin = new ContentValues();
            admin.put("global_id", "local-admin");
            admin.put("first_name", "مدير");
            admin.put("last_name", "البقالة");
            admin.put("email", "admin@alshuibi.local");
            admin.put("role", "Administrator");
            admin.put("password_hash", PasswordUtils.hashPassword("123456"));
            db.insertOrThrow("users", null, admin);
        }
    }

    private void putSetting(SQLiteDatabase db, String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("setting_key", key);
        cv.put("setting_value", value);
        db.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public String getSetting(String key, String fallback) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT setting_value FROM app_settings WHERE setting_key=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : fallback;
        }
    }

    public void setSetting(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("setting_key", key);
        cv.put("setting_value", value == null ? "" : value);
        getWritableDatabase().insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User getAdminUser() {
        final String storeName = getSetting("store_name", "بقالة الشعيبي");
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT global_id,first_name,last_name,email,role FROM users WHERE role='Administrator' AND active=1 LIMIT 1", null)) {
            if (!c.moveToFirst()) return null;
            User u = new User();
            u.setGlobalID(c.getString(0));
            u.setCreatorID(c.getString(0));
            u.setFirstName(c.getString(1));
            u.setLastName(c.getString(2));
            u.setEmail(c.getString(3));
            u.setPositionTitle(c.getString(4));
            u.setCompanyName(storeName);
            return u;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load administrator", e);
            return null;
        }
    }

    public User authenticateUser(String email, String password) {
        if (email == null || password == null) return null;
        final String storeName = getSetting("store_name", "بقالة الشعيبي");
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT global_id,first_name,last_name,email,role,password_hash FROM users WHERE lower(email)=lower(?) AND active=1 LIMIT 1", new String[]{email.trim()})) {
            if (!c.moveToFirst()) return null;
            String hash = c.getString(5);
            if (!PasswordUtils.verifyPassword(password, hash)) return null;
            User u = new User();
            u.setGlobalID(c.getString(0));
            u.setCreatorID(c.getString(0));
            u.setFirstName(c.getString(1));
            u.setLastName(c.getString(2));
            u.setEmail(c.getString(3));
            u.setPositionTitle(c.getString(4));
            u.setCompanyName(storeName);
            return u;
        } catch (Exception e) {
            Log.e(TAG, "Authentication failed", e);
            return null;
        }
    }

    public boolean updateAdmin(String fullName, String email, @Nullable String newPassword) {
        String cleanName = fullName == null ? "مدير البقالة" : fullName.trim();
        String first = cleanName;
        String last = "";
        int space = cleanName.indexOf(' ');
        if (space > 0) {
            first = cleanName.substring(0, space);
            last = cleanName.substring(space + 1).trim();
        }
        ContentValues cv = new ContentValues();
        cv.put("first_name", first);
        cv.put("last_name", last);
        cv.put("email", email == null || email.trim().isEmpty() ? "admin@alshuibi.local" : email.trim());
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            cv.put("password_hash", PasswordUtils.hashPassword(newPassword));
        }
        return getWritableDatabase().update("users", cv, "role='Administrator'", null) > 0;
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> list = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT name FROM categories ORDER BY sort_order,name COLLATE NOCASE", null)) {
            while (c.moveToNext()) list.add(c.getString(0));
        }
        return list;
    }

    public boolean addCategory(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        return getWritableDatabase().insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }

    public boolean renameCategory(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues c = new ContentValues();
            c.put("name", newName.trim());
            if (db.update("categories", c, "name=?", new String[]{oldName}) <= 0) return false;
            ContentValues i = new ContentValues();
            i.put("category", newName.trim());
            db.update("items", i, "category=?", new String[]{oldName});
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    public boolean deleteCategory(String name) {
        if (name == null) return false;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM items WHERE category=?", new String[]{name})) {
            if (c.moveToFirst() && c.getInt(0) > 0) return false;
        }
        return getWritableDatabase().delete("categories", "name=?", new String[]{name}) > 0;
    }

    public void createItem(Item item) throws SQLiteException {
        if (item == null || item.getName().trim().isEmpty()) throw new SQLiteException("اسم الصنف مطلوب");
        if (item.getGlobalID() == null || item.getGlobalID().trim().isEmpty()) item.setGlobalID(UUID.randomUUID().toString());
        String sku = item.getSku().trim();
        if (!sku.isEmpty()) {
            Item existing = getItemBySku(sku);
            if (existing != null) throw new SQLiteException("هذا الباركود مسجل لصنف آخر");
        }
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long row = db.insertOrThrow("items", null, itemValues(item));
            if (row < 0) throw new SQLiteException("تعذر حفظ الصنف");
            if (item.getStock() > 0) recordMovement(db, item.getGlobalID(), "OPENING", item.getStock(), 0, item.getStock(), "رصيد افتتاحي");
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public boolean updateItem(Item item) {
        if (item == null || item.getGlobalID() == null) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Item old = getItemByGlobalId(db, item.getGlobalID());
            if (old == null) return false;
            if (!item.getSku().trim().isEmpty()) {
                try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM items WHERE sku=? AND global_id<>?", new String[]{item.getSku().trim(), item.getGlobalID()})) {
                    if (c.moveToFirst() && c.getInt(0) > 0) return false;
                }
            }
            int rows = db.update("items", itemValues(item), "global_id=?", new String[]{item.getGlobalID()});
            int diff = item.getStock() - old.getStock();
            if (rows > 0 && diff != 0) recordMovement(db, item.getGlobalID(), "ADJUSTMENT", diff, old.getStock(), item.getStock(), "تعديل الصنف");
            db.setTransactionSuccessful();
            return rows > 0;
        } finally { db.endTransaction(); }
    }

    public boolean deleteItem(String globalId) {
        if (globalId == null) return false;
        return getWritableDatabase().delete("items", "global_id=?", new String[]{globalId}) > 0;
    }

    public boolean adjustStock(String globalId, int difference, String note) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Item old = getItemByGlobalId(db, globalId);
            if (old == null) return false;
            int after = old.getStock() + difference;
            if (after < 0) return false;
            ContentValues cv = new ContentValues();
            cv.put("stock_quantity", after);
            cv.put("updated_at", LocalFormat.getCurrentDateTime()[0] + " " + LocalFormat.getCurrentDateTime()[1]);
            if (db.update("items", cv, "global_id=?", new String[]{globalId}) <= 0) return false;
            recordMovement(db, globalId, difference >= 0 ? "IN" : "OUT", difference, old.getStock(), after, note);
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    private ContentValues itemValues(Item item) {
        ContentValues cv = new ContentValues();
        cv.put("global_id", item.getGlobalID());
        cv.put("image", item.getImagePath());
        cv.put("name", item.getName().trim());
        cv.put("price", Math.round(item.getPrice()));
        cv.put("category", item.getCategory().isEmpty() ? "أخرى" : item.getCategory());
        cv.put("sku", item.getSku().trim().isEmpty() ? null : item.getSku().trim());
        cv.put("unit_type", item.getUnitType().isEmpty() ? "حبة" : item.getUnitType());
        cv.put("stock_quantity", Math.max(0, item.getStock()));
        cv.put("wholesales_price", Math.round(item.getWholesalePrice()));
        cv.put("tax", item.getTax());
        cv.put("description", item.getDescription());
        cv.put("min_stock", Math.max(0, item.getMinStock()));
        cv.put("updated_at", LocalFormat.getCurrentDateTime()[0] + " " + LocalFormat.getCurrentDateTime()[1]);
        return cv;
    }

    public void readItem(ArrayList<Item> list) {
        list.clear();
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery("SELECT * FROM items ORDER BY name COLLATE NOCASE", null)) {
            while (c.moveToNext()) list.add(cursorToItem(c));
        }
    }

    public Item getItemBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) return null;
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery("SELECT * FROM items WHERE sku=? LIMIT 1", new String[]{sku.trim()})) {
            return c.moveToFirst() ? cursorToItem(c) : null;
        }
    }

    public Item getItemByGlobalId(String id) {
        return getItemByGlobalId(getReadableDatabase(), id);
    }

    private Item getItemByGlobalId(SQLiteDatabase db, String id) {
        if (id == null) return null;
        try (Cursor c = db.rawQuery("SELECT * FROM items WHERE global_id=? LIMIT 1", new String[]{id})) {
            return c.moveToFirst() ? cursorToItem(c) : null;
        }
    }

    private Item cursorToItem(Cursor c) {
        Item item = new Item();
        item.setLocalID(c.getLong(c.getColumnIndexOrThrow("_id")));
        item.setGlobalID(c.getString(c.getColumnIndexOrThrow("global_id")));
        item.setImagePath(c.getString(c.getColumnIndexOrThrow("image")));
        item.setName(c.getString(c.getColumnIndexOrThrow("name")));
        item.setPrice(c.getLong(c.getColumnIndexOrThrow("price")));
        item.setCategory(c.getString(c.getColumnIndexOrThrow("category")));
        item.setSku(c.getString(c.getColumnIndexOrThrow("sku")));
        item.setUnitType(c.getString(c.getColumnIndexOrThrow("unit_type")));
        item.setStock(c.getInt(c.getColumnIndexOrThrow("stock_quantity")));
        item.setWholesalePrice(c.getLong(c.getColumnIndexOrThrow("wholesales_price")));
        item.setTax(c.getDouble(c.getColumnIndexOrThrow("tax")));
        item.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        item.setMinStock(c.getInt(c.getColumnIndexOrThrow("min_stock")));
        return item;
    }

    private void recordMovement(SQLiteDatabase db, String itemId, String type, int qty, int before, int after, String note) {
        ContentValues cv = new ContentValues();
        cv.put("item_global_id", itemId);
        cv.put("movement_type", type);
        cv.put("quantity", qty);
        cv.put("before_qty", before);
        cv.put("after_qty", after);
        cv.put("note", note == null ? "" : note);
        db.insert("stock_movements", null, cv);
    }

    public Order completeSale(List<Item> cart, String paymentMethod) throws SQLiteException {
        if (cart == null || cart.isEmpty()) throw new SQLiteException("سلة البيع فارغة");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long total = 0, cost = 0, totalItems = 0;
            ArrayList<Item> snapshots = new ArrayList<>();
            for (Item cartItem : cart) {
                Item current = getItemByGlobalId(db, cartItem.getGlobalID());
                if (current == null) throw new SQLiteException("أحد الأصناف لم يعد موجودًا");
                long qty = cartItem.getQuantity();
                if (qty <= 0 || qty > current.getStock()) throw new SQLiteException("الكمية المتوفرة من " + current.getName() + " غير كافية");
                Item snapshot = new Item(current.getGlobalID(), current.getName(), current.getPrice(), current.getTax(), current.getSku(), qty);
                snapshot.setWholesalePrice(current.getWholesalePrice());
                snapshot.setStock(current.getStock());
                snapshots.add(snapshot);
                total += Math.round(current.getPrice()) * qty;
                cost += Math.round(current.getWholesalePrice()) * qty;
                totalItems += qty;
            }
            long profit = total - cost;
            String[] dt = LocalFormat.getCurrentDateTime();
            String gid = "S-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.US);
            ContentValues sale = new ContentValues();
            sale.put("global_id", gid);
            sale.put("sale_date", dt[0]);
            sale.put("sale_time", dt[1]);
            sale.put("total_amount", total);
            sale.put("total_cost", cost);
            sale.put("total_profit", profit);
            sale.put("total_items", totalItems);
            sale.put("payment_method", paymentMethod == null ? "cash" : paymentMethod);
            sale.put("status", "Completed");
            long saleId = db.insertOrThrow("sales", null, sale);

            for (Item snapshot : snapshots) {
                long qty = snapshot.getQuantity();
                long unitPrice = Math.round(snapshot.getPrice());
                long purchasePrice = Math.round(snapshot.getWholesalePrice());
                ContentValues line = new ContentValues();
                line.put("sale_id", saleId);
                line.put("item_global_id", snapshot.getGlobalID());
                line.put("item_name", snapshot.getName());
                line.put("quantity", qty);
                line.put("unit_price", unitPrice);
                line.put("purchase_price", purchasePrice);
                line.put("line_total", unitPrice * qty);
                line.put("line_cost", purchasePrice * qty);
                line.put("line_profit", (unitPrice - purchasePrice) * qty);
                db.insertOrThrow("sale_items", null, line);

                int before = snapshot.getStock();
                int after = before - (int) qty;
                ContentValues stock = new ContentValues();
                stock.put("stock_quantity", after);
                db.update("items", stock, "global_id=?", new String[]{snapshot.getGlobalID()});
                recordMovement(db, snapshot.getGlobalID(), "SALE", -(int) qty, before, after, "فاتورة #" + saleId);
            }

            db.setTransactionSuccessful();
            Order result = new Order(gid, saleId, dt[0], dt[1], "Completed", totalItems, (double) total, snapshots);
            return result;
        } finally { db.endTransaction(); }
    }

    public void readOrders(ArrayList<Order> out) {
        out.clear();
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery("SELECT * FROM sales ORDER BY _id DESC", null)) {
            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndexOrThrow("_id"));
                String gid = c.getString(c.getColumnIndexOrThrow("global_id"));
                ArrayList<Item> items = readSaleItems(db, id);
                out.add(new Order(gid, id,
                        c.getString(c.getColumnIndexOrThrow("sale_date")),
                        c.getString(c.getColumnIndexOrThrow("sale_time")),
                        c.getString(c.getColumnIndexOrThrow("status")),
                        c.getLong(c.getColumnIndexOrThrow("total_items")),
                        (double) c.getLong(c.getColumnIndexOrThrow("total_amount")), items));
            }
        }
    }

    private ArrayList<Item> readSaleItems(SQLiteDatabase db, long saleId) {
        ArrayList<Item> items = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT item_global_id,item_name,quantity,unit_price,purchase_price FROM sale_items WHERE sale_id=?", new String[]{String.valueOf(saleId)})) {
            while (c.moveToNext()) {
                Item item = new Item(c.getString(0), c.getString(1), c.getLong(3), 0.0, "", c.getLong(2));
                item.setWholesalePrice(c.getLong(4));
                items.add(item);
            }
        }
        return items;
    }

    public void readTransactions(ArrayList<Transaction> out) {
        out.clear();
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery("SELECT * FROM sales ORDER BY _id DESC", null)) {
            while (c.moveToNext()) {
                out.add(new Transaction(
                        c.getString(c.getColumnIndexOrThrow("global_id")),
                        c.getLong(c.getColumnIndexOrThrow("_id")),
                        c.getString(c.getColumnIndexOrThrow("sale_date")),
                        c.getString(c.getColumnIndexOrThrow("sale_time")),
                        c.getString(c.getColumnIndexOrThrow("status")),
                        c.getLong(c.getColumnIndexOrThrow("total_amount")),
                        c.getString(c.getColumnIndexOrThrow("payment_method"))));
            }
        }
    }

    public String getItemName(String globalId) {
        Item item = getItemByGlobalId(globalId);
        return item == null ? null : item.getName();
    }

    public int getItemCount() { return scalarInt("SELECT COUNT(*) FROM items", null); }
    public int getOutOfStockCount() { return scalarInt("SELECT COUNT(*) FROM items WHERE stock_quantity<=0", null); }
    public int getLowStockCount() { return scalarInt("SELECT COUNT(*) FROM items WHERE stock_quantity>0 AND stock_quantity<=min_stock", null); }
    public int getTodaySalesCount() { return scalarInt("SELECT COUNT(*) FROM sales WHERE sale_date=?", new String[]{LocalFormat.getCurrentDateTime()[0]}); }
    public long getTodaySalesTotal() { return scalarLong("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE sale_date=?", new String[]{LocalFormat.getCurrentDateTime()[0]}); }
    public long getTodayProfit() { return scalarLong("SELECT COALESCE(SUM(total_profit),0) FROM sales WHERE sale_date=?", new String[]{LocalFormat.getCurrentDateTime()[0]}); }

    private int scalarInt(String sql, String[] args) { return (int) scalarLong(sql, args); }
    private long scalarLong(String sql, String[] args) {
        try (Cursor c = getReadableDatabase().rawQuery(sql, args)) { return c.moveToFirst() ? c.getLong(0) : 0; }
    }

    public boolean isTableEmpty(String table) {
        return scalarInt("SELECT COUNT(*) FROM " + table, null) == 0;
    }

    public boolean isTableExists(String table) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?", new String[]{table})) {
            return c.moveToFirst() && c.getInt(0) > 0;
        }
    }

    public void showEmptyStateMessage(View view, LinearLayout noData) {
        view.setVisibility(View.GONE);
        noData.setVisibility(View.VISIBLE);
    }

    public void showStateMessage(View view, LinearLayout noData) {
        view.setVisibility(View.VISIBLE);
        noData.setVisibility(View.GONE);
    }
}
