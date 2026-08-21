package com.alshuibi.grocery.data.local;

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

import com.alshuibi.grocery.model.Item;
import com.alshuibi.grocery.model.Customer;
import com.alshuibi.grocery.model.Order;
import com.alshuibi.grocery.model.Transaction;
import com.alshuibi.grocery.model.User;
import com.alshuibi.grocery.utils.LocalFormat;
import com.alshuibi.grocery.utils.PasswordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class GroceryDatabase extends SQLiteOpenHelper {
    private static final String TAG = "GroceryDatabase";
    public static final String DATABASE_NAME = "BaqalatAlshuibi.db";
    private static final int DATABASE_VERSION = 5;
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
                "discount_amount INTEGER NOT NULL DEFAULT 0," +
                "paid_amount INTEGER NOT NULL DEFAULT 0," +
                "change_amount INTEGER NOT NULL DEFAULT 0," +
                "notes TEXT NOT NULL DEFAULT ''," +
                "customer_global_id TEXT," +
                "worker_global_id TEXT," +
                "sync_status TEXT NOT NULL DEFAULT 'LOCAL'," +
                "status TEXT NOT NULL DEFAULT 'Completed')");

        db.execSQL("CREATE TABLE IF NOT EXISTS sale_items (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER NOT NULL," +
                "item_global_id TEXT NOT NULL," +
                "item_name TEXT NOT NULL," +
                "image_path TEXT," +
                "quantity INTEGER NOT NULL," +
                "unit_price INTEGER NOT NULL," +
                "purchase_price INTEGER NOT NULL," +
                "line_total INTEGER NOT NULL," +
                "line_cost INTEGER NOT NULL," +
                "line_profit INTEGER NOT NULL," +
                "FOREIGN KEY(sale_id) REFERENCES sales(_id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS customers (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "global_id TEXT NOT NULL UNIQUE," +
                "name TEXT NOT NULL," +
                "phone TEXT NOT NULL DEFAULT ''," +
                "address TEXT NOT NULL DEFAULT ''," +
                "credit_limit INTEGER NOT NULL DEFAULT 0," +
                "active INTEGER NOT NULL DEFAULT 1," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS customer_ledger (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "global_id TEXT NOT NULL UNIQUE," +
                "customer_global_id TEXT NOT NULL," +
                "entry_type TEXT NOT NULL," +
                "amount INTEGER NOT NULL," +
                "sale_global_id TEXT," +
                "worker_global_id TEXT," +
                "note TEXT NOT NULL DEFAULT ''," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "entity_type TEXT NOT NULL," +
                "entity_id TEXT NOT NULL," +
                "operation TEXT NOT NULL," +
                "payload TEXT NOT NULL DEFAULT ''," +
                "state TEXT NOT NULL DEFAULT 'PENDING'," +
                "attempts INTEGER NOT NULL DEFAULT 0," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

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

        db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "action TEXT NOT NULL," +
                "entity_type TEXT NOT NULL," +
                "entity_id TEXT," +
                "details TEXT NOT NULL DEFAULT ''," +
                "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        createIndexes(db);
        seedDefaults(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // V3 branding/ownership migration. Only replace untouched V2 placeholders.
            db.execSQL("UPDATE app_settings SET setting_value='علي صالح الشعيبي' WHERE setting_key='owner_name' AND (setting_value='' OR setting_value='مدير البقالة')");
            db.execSQL("UPDATE app_settings SET setting_value='alisaleh10302040@gmail.com' WHERE setting_key='owner_email' AND (setting_value='' OR setting_value='admin@alshuibi.local')");
            db.execSQL("UPDATE users SET first_name='علي صالح', last_name='الشعيبي', email='alisaleh10302040@gmail.com' WHERE role='Administrator' AND email='admin@alshuibi.local'");
        }
        if (oldVersion < 3) {
            addColumnIfMissing(db, "sales", "discount_amount", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, "sales", "paid_amount", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, "sales", "change_amount", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, "sales", "notes", "TEXT NOT NULL DEFAULT ''");
            db.execSQL("CREATE TABLE IF NOT EXISTS audit_log (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "action TEXT NOT NULL," +
                    "entity_type TEXT NOT NULL," +
                    "entity_id TEXT," +
                    "details TEXT NOT NULL DEFAULT ''," +
                    "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            putSetting(db, "receipt_footer", "شكرًا لتسوقكم من بقالة الشعيبي");
            putSetting(db, "currency", "ر.ي");
        }
        if (oldVersion < 4) {
            addColumnIfMissing(db, "sales", "customer_global_id", "TEXT");
            addColumnIfMissing(db, "sales", "worker_global_id", "TEXT");
            addColumnIfMissing(db, "sales", "sync_status", "TEXT NOT NULL DEFAULT 'LOCAL'");
            db.execSQL("CREATE TABLE IF NOT EXISTS customers (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "global_id TEXT NOT NULL UNIQUE," +
                    "name TEXT NOT NULL," +
                    "phone TEXT NOT NULL DEFAULT ''," +
                    "address TEXT NOT NULL DEFAULT ''," +
                    "credit_limit INTEGER NOT NULL DEFAULT 0," +
                    "active INTEGER NOT NULL DEFAULT 1," +
                    "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_ledger (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "global_id TEXT NOT NULL UNIQUE," +
                    "customer_global_id TEXT NOT NULL," +
                    "entry_type TEXT NOT NULL," +
                    "amount INTEGER NOT NULL," +
                    "sale_global_id TEXT," +
                    "worker_global_id TEXT," +
                    "note TEXT NOT NULL DEFAULT ''," +
                    "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "entity_type TEXT NOT NULL," +
                    "entity_id TEXT NOT NULL," +
                    "operation TEXT NOT NULL," +
                    "payload TEXT NOT NULL DEFAULT ''," +
                    "state TEXT NOT NULL DEFAULT 'PENDING'," +
                    "attempts INTEGER NOT NULL DEFAULT 0," +
                    "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            putSetting(db, "store_address", "اليمن - محافظة الضالع - مديرية قعطبة - حي المحكمة - جوار محكمة قعطبة الابتدائية");
            db.execSQL("UPDATE app_settings SET setting_value='اليمن - محافظة الضالع - مديرية قعطبة - حي المحكمة - جوار محكمة قعطبة الابتدائية' WHERE setting_key='store_address'");
            putSetting(db, "cloud_sync_enabled", "0");
            putSetting(db, "voice_credit_enabled", "1");
            createIndexes(db);
        }
        if (oldVersion < 5) {
            addColumnIfMissing(db, "sale_items", "image_path", "TEXT");
            createIndexes(db);
        }
    }

    private void addColumnIfMissing(SQLiteDatabase db, String table, String column, String definition) {
        boolean exists = false;
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIndex >= 0 && column.equalsIgnoreCase(c.getString(nameIndex))) {
                    exists = true;
                    break;
                }
            }
        }
        if (!exists) db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private void createIndexes(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_name ON items(name)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_category ON items(category)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_items_stock ON items(stock_quantity)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sales_date_status ON sales(sale_date,status)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sale_items_sale ON sale_items(sale_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_stock_item_created ON stock_movements(item_global_id,created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customer_ledger_customer ON customer_ledger(customer_global_id,created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sales_customer ON sales(customer_global_id,sale_date)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sales_worker ON sales(worker_global_id,sale_date)");
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
        putSetting(db, "owner_name", "علي صالح الشعيبي");
        putSetting(db, "owner_email", "alisaleh10302040@gmail.com");
        putSetting(db, "owner_phone", "");
        putSetting(db, "store_address", "اليمن - محافظة الضالع - مديرية قعطبة - حي المحكمة - جوار محكمة قعطبة الابتدائية");
        putSetting(db, "low_stock_default", "5");
        putSetting(db, "receipt_footer", "شكرًا لتسوقكم من بقالة الشعيبي");
        putSetting(db, "currency", "ر.ي");
        putSetting(db, "cloud_sync_enabled", "0");
        putSetting(db, "voice_credit_enabled", "1");

        Cursor c = db.rawQuery("SELECT COUNT(*) FROM users", null);
        boolean empty = c.moveToFirst() && c.getInt(0) == 0;
        c.close();
        if (empty) {
            ContentValues admin = new ContentValues();
            admin.put("global_id", "local-admin");
            admin.put("first_name", "علي صالح");
            admin.put("last_name", "الشعيبي");
            admin.put("email", "alisaleh10302040@gmail.com");
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
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT setting_value FROM app_settings WHERE setting_key=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : fallback;
        }
    }

    public void setSetting(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("setting_key", key);
        cv.put("setting_value", value == null ? "" : value);
        getWritableDatabase().insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean isDefaultAdminPassword() {
        User admin = getAdminUser();
        return admin != null && authenticateUser(admin.getEmail(), "123456") != null;
    }

    private void recordAudit(SQLiteDatabase db, String action, String entityType, String entityId, String details) {
        ContentValues cv = new ContentValues();
        cv.put("action", action == null ? "" : action);
        cv.put("entity_type", entityType == null ? "" : entityType);
        cv.put("entity_id", entityId);
        cv.put("details", details == null ? "" : details);
        db.insert("audit_log", null, cv);
    }

    public User getAdminUser() {
        final String storeName = getSetting("store_name", "بقالة الشعيبي");
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT global_id,first_name,last_name,email,role FROM users WHERE role='Administrator' AND active=1 LIMIT 1", null)) {
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
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT global_id,first_name,last_name,email,role,password_hash FROM users WHERE lower(email)=lower(?) AND active=1 LIMIT 1", new String[]{email.trim()})) {
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
        String cleanName = fullName == null ? "علي صالح الشعيبي" : fullName.trim();
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
        cv.put("email", email == null || email.trim().isEmpty() ? "alisaleh10302040@gmail.com" : email.trim());
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            cv.put("password_hash", PasswordUtils.hashPassword(newPassword));
        }
        return getWritableDatabase().update("users", cv, "role='Administrator'", null) > 0;
    }

    public User getUserByGlobalId(String globalId) {
        if (globalId == null || globalId.trim().isEmpty()) return null;
        final String storeName = getSetting("store_name", "بقالة الشعيبي");
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT global_id,first_name,last_name,email,role FROM users WHERE global_id=? AND active=1 LIMIT 1", new String[]{globalId})) {
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
        }
    }

    public ArrayList<User> getActiveUsers() {
        ArrayList<User> out = new ArrayList<>();
        final String storeName = getSetting("store_name", "بقالة الشعيبي");
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT global_id,first_name,last_name,email,role FROM users WHERE active=1 ORDER BY CASE WHEN role='Administrator' THEN 0 ELSE 1 END, first_name", null)) {
            while (c.moveToNext()) {
                User u = new User();
                u.setGlobalID(c.getString(0));
                u.setCreatorID(c.getString(0));
                u.setFirstName(c.getString(1));
                u.setLastName(c.getString(2));
                u.setEmail(c.getString(3));
                u.setPositionTitle(c.getString(4));
                u.setCompanyName(storeName);
                out.add(u);
            }
        }
        return out;
    }

    public boolean createWorker(String fullName, String email, String password) {
        if (fullName == null || fullName.trim().isEmpty() || email == null || email.trim().isEmpty() || password == null || password.length() < 6) return false;
        String clean = fullName.trim();
        String first = clean;
        String last = "";
        int space = clean.indexOf(' ');
        if (space > 0) { first = clean.substring(0, space); last = clean.substring(space + 1).trim(); }
        ContentValues cv = new ContentValues();
        cv.put("global_id", "W-" + UUID.randomUUID());
        cv.put("first_name", first);
        cv.put("last_name", last);
        cv.put("email", email.trim().toLowerCase(Locale.ROOT));
        cv.put("role", "Worker");
        cv.put("password_hash", PasswordUtils.hashPassword(password));
        cv.put("active", 1);
        try {
            long id = getWritableDatabase().insertOrThrow("users", null, cv);
            return id > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create worker", e);
            return false;
        }
    }

    public boolean setWorkerActive(String globalId, boolean active) {
        if (globalId == null || "local-admin".equals(globalId)) return false;
        ContentValues cv = new ContentValues();
        cv.put("active", active ? 1 : 0);
        return getWritableDatabase().update("users", cv, "global_id=? AND role<>'Administrator'", new String[]{globalId}) > 0;
    }

    public ArrayList<Customer> getCustomers() {
        ArrayList<Customer> out = new ArrayList<>();
        String sql = "SELECT c._id,c.global_id,c.name,c.phone,c.address,c.credit_limit,c.active,c.created_at," +
                "COALESCE(SUM(l.amount),0) balance FROM customers c LEFT JOIN customer_ledger l ON l.customer_global_id=c.global_id " +
                "WHERE c.active=1 GROUP BY c._id ORDER BY balance DESC,c.name COLLATE NOCASE";
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) {
                Customer customer = new Customer();
                customer.setLocalId(c.getLong(0));
                customer.setGlobalId(c.getString(1));
                customer.setName(c.getString(2));
                customer.setPhone(c.getString(3));
                customer.setAddress(c.getString(4));
                customer.setCreditLimit(c.getLong(5));
                customer.setActive(c.getInt(6) == 1);
                customer.setCreatedAt(c.getString(7));
                customer.setBalance(c.getLong(8));
                out.add(customer);
            }
        }
        return out;
    }

    public Customer getCustomerByGlobalId(String globalId) {
        if (globalId == null || globalId.trim().isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT _id,global_id,name,phone,address,credit_limit,active,created_at FROM customers WHERE global_id=? LIMIT 1", new String[]{globalId})) {
            if (!c.moveToFirst()) return null;
            Customer customer = new Customer();
            customer.setLocalId(c.getLong(0));
            customer.setGlobalId(c.getString(1));
            customer.setName(c.getString(2));
            customer.setPhone(c.getString(3));
            customer.setAddress(c.getString(4));
            customer.setCreditLimit(c.getLong(5));
            customer.setActive(c.getInt(6) == 1);
            customer.setCreatedAt(c.getString(7));
            customer.setBalance(getCustomerBalance(globalId));
            return customer;
        }
    }

    public Customer findCustomerInText(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        String hay = normalizeArabic(text);
        Customer best = null;
        int bestLen = 0;
        for (Customer c : getCustomers()) {
            String name = normalizeArabic(c.getName());
            if (!name.isEmpty() && hay.contains(name) && name.length() > bestLen) {
                best = c;
                bestLen = name.length();
            }
        }
        return best;
    }

    private String normalizeArabic(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase(Locale.ROOT)
                .replace('أ','ا').replace('إ','ا').replace('آ','ا')
                .replace('ى','ي').replace('ة','ه').replace("ـ", "");
    }

    public boolean saveCustomer(Customer customer) {
        if (customer == null || customer.getName().trim().isEmpty()) return false;
        if (customer.getGlobalId().trim().isEmpty()) customer.setGlobalId("C-" + UUID.randomUUID());
        ContentValues cv = new ContentValues();
        cv.put("global_id", customer.getGlobalId());
        cv.put("name", customer.getName().trim());
        cv.put("phone", customer.getPhone().trim());
        cv.put("address", customer.getAddress().trim());
        cv.put("credit_limit", Math.max(0, customer.getCreditLimit()));
        cv.put("active", 1);
        cv.put("updated_at", LocalFormat.getCurrentDateTime()[0] + " " + LocalFormat.getCurrentDateTime()[1]);
        SQLiteDatabase db = getWritableDatabase();
        long row = db.insertWithOnConflict("customers", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        if (row == -1) {
            return db.update("customers", cv, "global_id=?", new String[]{customer.getGlobalId()}) > 0;
        }
        recordAudit(db, "CREATE", "CUSTOMER", customer.getGlobalId(), customer.getName());
        queueSync(db, "CUSTOMER", customer.getGlobalId(), "UPSERT", customer.getName());
        return true;
    }

    public long getCustomerBalance(String customerGlobalId) {
        return scalarLong("SELECT COALESCE(SUM(amount),0) FROM customer_ledger WHERE customer_global_id=?", new String[]{customerGlobalId});
    }

    public long getTotalReceivables() {
        return scalarLong("SELECT COALESCE(SUM(CASE WHEN x.balance>0 THEN x.balance ELSE 0 END),0) FROM (SELECT customer_global_id,SUM(amount) balance FROM customer_ledger GROUP BY customer_global_id) x", null);
    }

    public boolean addCustomerPayment(String customerGlobalId, long amount, String note, String workerGlobalId) {
        if (customerGlobalId == null || amount <= 0) return false;
        long current = getCustomerBalance(customerGlobalId);
        long safe = Math.min(amount, Math.max(0, current));
        if (safe <= 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        String gid = "L-" + UUID.randomUUID();
        ContentValues cv = new ContentValues();
        cv.put("global_id", gid);
        cv.put("customer_global_id", customerGlobalId);
        cv.put("entry_type", "PAYMENT");
        cv.put("amount", -safe);
        cv.put("worker_global_id", workerGlobalId);
        cv.put("note", note == null ? "سداد من العميل" : note.trim());
        long row = db.insertOrThrow("customer_ledger", null, cv);
        recordAudit(db, "PAYMENT", "CUSTOMER", customerGlobalId, "سداد " + safe);
        queueSync(db, "LEDGER", gid, "CREATE", String.valueOf(-safe));
        return row > 0;
    }

    public ArrayList<String> getCustomerLedgerLines(String customerGlobalId) {
        ArrayList<String> out = new ArrayList<>();
        String sql = "SELECT entry_type,amount,note,created_at FROM customer_ledger WHERE customer_global_id=? ORDER BY _id DESC LIMIT 100";
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, new String[]{customerGlobalId})) {
            while (c.moveToNext()) {
                String type = c.getString(0);
                long amount = c.getLong(1);
                String note = c.getString(2);
                String at = c.getString(3);
                String label = "PAYMENT".equals(type) ? "سداد" : ("CREDIT_SALE".equals(type) ? "شراء آجل" : ("CREDIT_CANCEL".equals(type) ? "إلغاء شراء آجل" : "تعديل"));
                out.add(at + "\n" + label + " • " + LocalFormat.getCurrencyFormat(Math.abs(amount)) + (note == null || note.isEmpty() ? "" : " • " + note));
            }
        }
        return out;
    }

    private void queueSync(SQLiteDatabase db, String entityType, String entityId, String operation, String payload) {
        ContentValues cv = new ContentValues();
        cv.put("entity_type", entityType);
        cv.put("entity_id", entityId);
        cv.put("operation", operation);
        cv.put("payload", payload == null ? "" : payload);
        cv.put("state", "PENDING");
        db.insert("sync_queue", null, cv);
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT name FROM categories ORDER BY sort_order,name COLLATE NOCASE", null)) {
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
            recordAudit(db, "CREATE", "ITEM", item.getGlobalID(), item.getName());
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
            if (rows > 0) recordAudit(db, "UPDATE", "ITEM", item.getGlobalID(), item.getName());
            db.setTransactionSuccessful();
            return rows > 0;
        } finally { db.endTransaction(); }
    }

    public boolean deleteItem(String globalId) {
        if (globalId == null) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Item old = getItemByGlobalId(db, globalId);
            int rows = db.delete("items", "global_id=?", new String[]{globalId});
            if (rows > 0) recordAudit(db, "DELETE", "ITEM", globalId, old == null ? "" : old.getName());
            db.setTransactionSuccessful();
            return rows > 0;
        } finally { db.endTransaction(); }
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
            recordAudit(db, "STOCK", "ITEM", globalId, (note == null ? "" : note) + " | " + difference);
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
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM items ORDER BY name COLLATE NOCASE", null)) {
            while (c.moveToNext()) list.add(cursorToItem(c));
        }
    }

    public Item getItemBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM items WHERE sku=? LIMIT 1", new String[]{sku.trim()})) {
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
        return completeSale(cart, paymentMethod, 0, 0, "", null, null);
    }

    public Order completeSale(List<Item> cart, String paymentMethod, long discountAmount,
                              long receivedAmount, String notes) throws SQLiteException {
        return completeSale(cart, paymentMethod, discountAmount, receivedAmount, notes, null, null);
    }

    public Order completeSale(List<Item> cart, String paymentMethod, long discountAmount,
                              long receivedAmount, String notes, @Nullable String customerGlobalId,
                              @Nullable String workerGlobalId) throws SQLiteException {
        if (cart == null || cart.isEmpty()) throw new SQLiteException("سلة البيع فارغة");
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long grossTotal = 0, cost = 0, totalItems = 0;
            ArrayList<Item> snapshots = new ArrayList<>();
            for (Item cartItem : cart) {
                Item current = getItemByGlobalId(db, cartItem.getGlobalID());
                if (current == null) throw new SQLiteException("أحد الأصناف لم يعد موجودًا");
                long qty = cartItem.getQuantity();
                if (qty <= 0 || qty > current.getStock()) {
                    throw new SQLiteException("الكمية المتوفرة من " + current.getName() + " غير كافية");
                }
                Item snapshot = new Item(current.getGlobalID(), current.getName(), current.getPrice(), current.getTax(), current.getSku(), qty);
                snapshot.setWholesalePrice(current.getWholesalePrice());
                snapshot.setImagePath(current.getImagePath());
                snapshot.setStock(current.getStock());
                snapshot.setUnitType(current.getUnitType());
                snapshots.add(snapshot);
                grossTotal += Math.round(current.getPrice()) * qty;
                cost += Math.round(current.getWholesalePrice()) * qty;
                totalItems += qty;
            }

            long safeDiscount = Math.max(0, Math.min(discountAmount, grossTotal));
            long total = grossTotal - safeDiscount;
            long profit = total - cost;
            String method = paymentMethod == null || paymentMethod.trim().isEmpty() ? "cash" : paymentMethod.trim();
            boolean cash = "cash".equals(method);
            boolean credit = "credit".equals(method);
            boolean electronic = "transfer".equals(method) || "wallet".equals(method);
            if (credit && (customerGlobalId == null || customerGlobalId.trim().isEmpty())) {
                throw new SQLiteException("اختر العميل عند البيع على الحساب");
            }
            long paid = credit ? 0 : (electronic ? total : (receivedAmount <= 0 ? total : receivedAmount));
            if (cash && paid < total) throw new SQLiteException("المبلغ المستلم أقل من إجمالي الفاتورة");
            long change = cash ? Math.max(0, paid - total) : 0;

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
            sale.put("payment_method", method);
            sale.put("discount_amount", safeDiscount);
            sale.put("paid_amount", paid);
            sale.put("change_amount", change);
            sale.put("notes", notes == null ? "" : notes.trim());
            sale.put("customer_global_id", customerGlobalId);
            sale.put("worker_global_id", workerGlobalId);
            sale.put("sync_status", "LOCAL");
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
                line.put("image_path", snapshot.getImagePath());
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
                stock.put("updated_at", dt[0] + " " + dt[1]);
                db.update("items", stock, "global_id=?", new String[]{snapshot.getGlobalID()});
                recordMovement(db, snapshot.getGlobalID(), "SALE", -(int) qty, before, after, "فاتورة #" + saleId);
            }

            if (credit) {
                Customer customer = getCustomerByGlobalId(customerGlobalId);
                if (customer == null) throw new SQLiteException("العميل المحدد غير موجود");
                long newBalance = getCustomerBalance(customerGlobalId) + total;
                if (customer.getCreditLimit() > 0 && newBalance > customer.getCreditLimit()) {
                    throw new SQLiteException("العملية تتجاوز الحد الائتماني للعميل");
                }
                String ledgerId = "L-" + UUID.randomUUID();
                ContentValues ledger = new ContentValues();
                ledger.put("global_id", ledgerId);
                ledger.put("customer_global_id", customerGlobalId);
                ledger.put("entry_type", "CREDIT_SALE");
                ledger.put("amount", total);
                ledger.put("sale_global_id", gid);
                ledger.put("worker_global_id", workerGlobalId);
                ledger.put("note", "فاتورة #" + saleId);
                db.insertOrThrow("customer_ledger", null, ledger);
                queueSync(db, "LEDGER", ledgerId, "CREATE", String.valueOf(total));
            }

            recordAudit(db, "SALE", "SALE", gid, "فاتورة #" + saleId + " | " + total);
            queueSync(db, "SALE", gid, "CREATE", String.valueOf(total));
            db.setTransactionSuccessful();
            Order result = new Order(gid, saleId, dt[0], dt[1], "Completed", totalItems, (double) total, snapshots);
            result.setPaymentMethod(method);
            result.setDiscountAmount(safeDiscount);
            result.setPaidAmount(paid);
            result.setChangeAmount(change);
            result.setNotes(notes == null ? "" : notes.trim());
            return result;
        } finally { db.endTransaction(); }
    }

    public boolean cancelSale(long saleId, String reason) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            String status;
            String gid;
            String paymentMethod;
            String customerGlobalId;
            long saleTotal;
            try (Cursor sale = db.rawQuery("SELECT global_id,status,payment_method,customer_global_id,total_amount FROM sales WHERE _id=? LIMIT 1", new String[]{String.valueOf(saleId)})) {
                if (!sale.moveToFirst()) return false;
                gid = sale.getString(0);
                status = sale.getString(1);
                paymentMethod = sale.getString(2);
                customerGlobalId = sale.isNull(3) ? null : sale.getString(3);
                saleTotal = sale.getLong(4);
            }
            if (!"Completed".equalsIgnoreCase(status)) return false;

            try (Cursor lines = db.rawQuery("SELECT item_global_id,quantity,item_name FROM sale_items WHERE sale_id=?", new String[]{String.valueOf(saleId)})) {
                while (lines.moveToNext()) {
                    String itemId = lines.getString(0);
                    int qty = lines.getInt(1);
                    Item current = getItemByGlobalId(db, itemId);
                    if (current == null) continue;
                    int before = current.getStock();
                    int after = before + qty;
                    ContentValues stock = new ContentValues();
                    stock.put("stock_quantity", after);
                    db.update("items", stock, "global_id=?", new String[]{itemId});
                    recordMovement(db, itemId, "SALE_CANCEL", qty, before, after, "إلغاء فاتورة #" + saleId);
                }
            }

            ContentValues update = new ContentValues();
            update.put("status", "Cancelled");
            String cleanReason = reason == null ? "" : reason.trim();
            update.put("notes", cleanReason.isEmpty() ? "تم إلغاء الفاتورة" : "إلغاء: " + cleanReason);
            int rows = db.update("sales", update, "_id=? AND status='Completed'", new String[]{String.valueOf(saleId)});
            if (rows <= 0) return false;
            if ("credit".equalsIgnoreCase(paymentMethod) && customerGlobalId != null && !customerGlobalId.isEmpty()) {
                String ledgerId = "L-" + UUID.randomUUID();
                ContentValues ledger = new ContentValues();
                ledger.put("global_id", ledgerId);
                ledger.put("customer_global_id", customerGlobalId);
                ledger.put("entry_type", "CREDIT_CANCEL");
                ledger.put("amount", -saleTotal);
                ledger.put("sale_global_id", gid);
                ledger.put("note", "عكس فاتورة ملغاة #" + saleId);
                db.insertOrThrow("customer_ledger", null, ledger);
                queueSync(db, "LEDGER", ledgerId, "CREATE", String.valueOf(-saleTotal));
            }
            recordAudit(db, "CANCEL", "SALE", gid, "فاتورة #" + saleId + " | " + cleanReason);
            queueSync(db, "SALE", gid, "UPDATE", "Cancelled");
            db.setTransactionSuccessful();
            return true;
        } finally { db.endTransaction(); }
    }

    public void readOrders(ArrayList<Order> out) {
        out.clear();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM sales ORDER BY _id DESC", null)) {
            while (c.moveToNext()) {
                long id = c.getLong(c.getColumnIndexOrThrow("_id"));
                String gid = c.getString(c.getColumnIndexOrThrow("global_id"));
                ArrayList<Item> items = readSaleItems(db, id);
                Order order = new Order(gid, id,
                        c.getString(c.getColumnIndexOrThrow("sale_date")),
                        c.getString(c.getColumnIndexOrThrow("sale_time")),
                        c.getString(c.getColumnIndexOrThrow("status")),
                        c.getLong(c.getColumnIndexOrThrow("total_items")),
                        (double) c.getLong(c.getColumnIndexOrThrow("total_amount")), items);
                order.setPaymentMethod(c.getString(c.getColumnIndexOrThrow("payment_method")));
                order.setDiscountAmount(c.getLong(c.getColumnIndexOrThrow("discount_amount")));
                order.setPaidAmount(c.getLong(c.getColumnIndexOrThrow("paid_amount")));
                order.setChangeAmount(c.getLong(c.getColumnIndexOrThrow("change_amount")));
                order.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
                out.add(order);
            }
        }
    }

    private ArrayList<Item> readSaleItems(SQLiteDatabase db, long saleId) {
        ArrayList<Item> items = new ArrayList<>();
        String sql = "SELECT si.item_global_id,si.item_name,si.quantity,si.unit_price,si.purchase_price," +
                "COALESCE(NULLIF(si.image_path,''),i.image) " +
                "FROM sale_items si LEFT JOIN items i ON i.global_id=si.item_global_id WHERE si.sale_id=?";
        try (Cursor c = db.rawQuery(sql, new String[]{String.valueOf(saleId)})) {
            while (c.moveToNext()) {
                Item item = new Item(c.getString(0), c.getString(1), c.getLong(3), 0.0, "", c.getLong(2));
                item.setWholesalePrice(c.getLong(4));
                item.setImagePath(c.isNull(5) ? null : c.getString(5));
                items.add(item);
            }
        }
        return items;
    }

    public void readTransactions(ArrayList<Transaction> out) {
        out.clear();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM sales ORDER BY _id DESC", null)) {
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
    public int getWorkerTodaySalesCount(String workerId) {
        if (workerId == null) return 0;
        return scalarInt("SELECT COUNT(*) FROM sales WHERE sale_date=? AND status='Completed' AND worker_global_id=?", new String[]{LocalFormat.getCurrentDateTime()[0], workerId});
    }
    public long getWorkerTodaySalesTotal(String workerId) {
        if (workerId == null) return 0;
        return scalarLong("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE sale_date=? AND status='Completed' AND worker_global_id=?", new String[]{LocalFormat.getCurrentDateTime()[0], workerId});
    }
    public long getWorkerLifetimeSalesTotal(String workerId) {
        if (workerId == null) return 0;
        return scalarLong("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE status='Completed' AND worker_global_id=?", new String[]{workerId});
    }

    public int getTodaySalesCount() { return scalarInt("SELECT COUNT(*) FROM sales WHERE sale_date=? AND status='Completed'", new String[]{LocalFormat.getCurrentDateTime()[0]}); }
    public long getTodaySalesTotal() { return scalarLong("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE sale_date=? AND status='Completed'", new String[]{LocalFormat.getCurrentDateTime()[0]}); }
    public long getTodayProfit() { return scalarLong("SELECT COALESCE(SUM(total_profit),0) FROM sales WHERE sale_date=? AND status='Completed'", new String[]{LocalFormat.getCurrentDateTime()[0]}); }
    public long getInventoryRetailValue() { return scalarLong("SELECT COALESCE(SUM(price * stock_quantity),0) FROM items", null); }
    public long getInventoryCostValue() { return scalarLong("SELECT COALESCE(SUM(wholesales_price * stock_quantity),0) FROM items", null); }
    public long getLifetimeSalesTotal() { return scalarLong("SELECT COALESCE(SUM(total_amount),0) FROM sales WHERE status='Completed'", null); }
    public long getLifetimeProfit() { return scalarLong("SELECT COALESCE(SUM(total_profit),0) FROM sales WHERE status='Completed'", null); }

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
