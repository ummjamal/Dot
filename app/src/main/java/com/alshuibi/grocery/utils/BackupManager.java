package com.alshuibi.grocery.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.alshuibi.grocery.data.local.GroceryDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * نسخة احتياطية محلية تشمل قاعدة البيانات وصور الأصناف.
 * تستخدم Storage Access Framework؛ لا تحتاج إذن وصول عام للملفات.
 */
public final class BackupManager {
    private static final String DB_ENTRY = "database/" + GroceryDatabase.DATABASE_NAME;
    private static final String IMAGE_PREFIX = "images/";
    private static final String VERSION_ENTRY = "backup-version.txt";
    private static final String BACKUP_VERSION = "BaqalatAlshuibiBackupV5";

    private static final long MAX_DATABASE_BYTES = 128L * 1024L * 1024L;
    private static final long MAX_IMAGE_BYTES = 25L * 1024L * 1024L;
    private static final long MAX_TOTAL_RESTORE_BYTES = 256L * 1024L * 1024L;

    private static final String[] REQUIRED_TABLES = {
            "categories", "items", "stock_movements", "sales",
            "sale_items", "users", "app_settings"
    };

    private BackupManager() { }

    public static void createBackup(Context context, OutputStream outputStream) throws IOException {
        if (outputStream == null) throw new IOException("تعذر فتح ملف النسخة الاحتياطية");

        // Force WAL contents into the main DB before copying it.
        try (GroceryDatabase helper = new GroceryDatabase(context)) {
            SQLiteDatabase db = helper.getWritableDatabase();
            try (Cursor ignored = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)) {
                if (ignored.moveToFirst()) { /* checkpoint executed */ }
            }
        }

        File dbFile = context.getDatabasePath(GroceryDatabase.DATABASE_NAME);
        if (!dbFile.exists()) throw new IOException("قاعدة البيانات غير موجودة");

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
            addFile(zip, dbFile, DB_ENTRY);

            File imagesDir = context.getDir("Items", Context.MODE_PRIVATE);
            File[] images = imagesDir.listFiles();
            if (images != null) {
                for (File image : images) {
                    if (image.isFile()) addFile(zip, image, IMAGE_PREFIX + image.getName());
                }
            }

            ZipEntry info = new ZipEntry(VERSION_ENTRY);
            zip.putNextEntry(info);
            zip.write(BACKUP_VERSION.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    public static void restoreBackup(Context context, InputStream inputStream) throws IOException {
        if (inputStream == null) throw new IOException("تعذر فتح النسخة الاحتياطية");

        File tempDir = new File(context.getCacheDir(), "restore_backup");
        deleteRecursive(tempDir);
        if (!tempDir.mkdirs() && !tempDir.exists()) throw new IOException("تعذر إنشاء مجلد الاستعادة");

        File tempDb = new File(tempDir, GroceryDatabase.DATABASE_NAME);
        File tempImages = new File(tempDir, "images");
        //noinspection ResultOfMethodCallIgnored
        tempImages.mkdirs();
        boolean foundDatabase = false;
        long totalExtracted = 0;

        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            byte[] buffer = new byte[16 * 1024];
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    zip.closeEntry();
                    continue;
                }

                File target = null;
                long maxEntryBytes = 0;
                if (DB_ENTRY.equals(name)) {
                    target = tempDb;
                    maxEntryBytes = MAX_DATABASE_BYTES;
                    foundDatabase = true;
                } else if (name.startsWith(IMAGE_PREFIX)) {
                    String imageName = name.substring(IMAGE_PREFIX.length());
                    if (isSafeSimpleName(imageName)) {
                        target = new File(tempImages, imageName);
                        maxEntryBytes = MAX_IMAGE_BYTES;
                    }
                }

                if (target != null) {
                    File parent = target.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    long entryExtracted = 0;
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) {
                            entryExtracted += read;
                            totalExtracted += read;
                            if (entryExtracted > maxEntryBytes || totalExtracted > MAX_TOTAL_RESTORE_BYTES) {
                                throw new IOException("حجم النسخة الاحتياطية يتجاوز الحد الآمن");
                            }
                            out.write(buffer, 0, read);
                        }
                        out.getFD().sync();
                    }
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            deleteRecursive(tempDir);
            throw e;
        }

        if (!foundDatabase || !tempDb.exists() || tempDb.length() == 0 || !isSQLiteDatabase(tempDb)) {
            deleteRecursive(tempDir);
            throw new IOException("ملف النسخة الاحتياطية غير صالح أو قاعدة البيانات تالفة");
        }

        // Validate integrity and required schema BEFORE touching the user's current database.
        validateBackupDatabase(tempDb);

        File dbFile = context.getDatabasePath(GroceryDatabase.DATABASE_NAME);
        File dbParent = dbFile.getParentFile();
        if (dbParent != null && !dbParent.exists()) dbParent.mkdirs();

        // Keep a local rollback copy until the restored database opens successfully.
        File rollbackDb = new File(tempDir, "rollback-current.db");
        if (dbFile.exists()) copyFile(dbFile, rollbackDb);

        try {
            // Ask any helper created here to checkpoint/close before file replacement.
            try (GroceryDatabase helper = new GroceryDatabase(context)) {
                SQLiteDatabase db = helper.getWritableDatabase();
                try (Cursor ignored = db.rawQuery("PRAGMA wal_checkpoint(FULL)", null)) {
                    if (ignored.moveToFirst()) { /* checkpoint executed */ }
                }
            }

            deleteIfExists(new File(dbFile.getAbsolutePath() + "-wal"));
            deleteIfExists(new File(dbFile.getAbsolutePath() + "-shm"));
            deleteIfExists(new File(dbFile.getAbsolutePath() + "-journal"));
            copyFile(tempDb, dbFile);

            File imagesDir = context.getDir("Items", Context.MODE_PRIVATE);
            File oldImagesBackup = new File(tempDir, "rollback-images");
            //noinspection ResultOfMethodCallIgnored
            oldImagesBackup.mkdirs();
            copyDirectoryContents(imagesDir, oldImagesBackup);

            try {
                clearDirectory(imagesDir);
                copyDirectoryContents(tempImages, imagesDir);

                // Reopen through our helper so migrations and schema access are also verified.
                try (GroceryDatabase db = new GroceryDatabase(context)) {
                    db.getReadableDatabase();
                    db.getItemCount();
                }
            } catch (Exception restoreFailure) {
                // Roll back both database and images if post-copy validation fails.
                if (rollbackDb.exists()) copyFile(rollbackDb, dbFile);
                clearDirectory(imagesDir);
                copyDirectoryContents(oldImagesBackup, imagesDir);
                throw restoreFailure;
            }
        } catch (Exception e) {
            // If replacing the database itself failed, restore the last known-good copy when available.
            try {
                if (rollbackDb.exists()) {
                    deleteIfExists(new File(dbFile.getAbsolutePath() + "-wal"));
                    deleteIfExists(new File(dbFile.getAbsolutePath() + "-shm"));
                    deleteIfExists(new File(dbFile.getAbsolutePath() + "-journal"));
                    copyFile(rollbackDb, dbFile);
                }
            } catch (Exception ignored) {
                // Preserve the original restore exception; the UI will instruct the user that restore failed.
            }
            deleteRecursive(tempDir);
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("تعذر استعادة النسخة الاحتياطية بأمان", e);
        }

        deleteRecursive(tempDir);
    }

    private static void validateBackupDatabase(File file) throws IOException {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

            try (Cursor integrity = db.rawQuery("PRAGMA integrity_check", null)) {
                if (!integrity.moveToFirst() || !"ok".equalsIgnoreCase(integrity.getString(0))) {
                    throw new IOException("قاعدة البيانات داخل النسخة الاحتياطية غير سليمة");
                }
            }

            Set<String> tables = new HashSet<>();
            try (Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)) {
                while (c.moveToNext()) tables.add(c.getString(0));
            }
            for (String required : REQUIRED_TABLES) {
                if (!tables.contains(required)) {
                    throw new IOException("النسخة الاحتياطية لا تحتوي بنية بقالة الشعيبي المطلوبة");
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("تعذر التحقق من قاعدة البيانات داخل النسخة الاحتياطية", e);
        } finally {
            if (db != null && db.isOpen()) db.close();
        }
    }

    private static boolean isSQLiteDatabase(File file) {
        byte[] header = new byte[16];
        try (FileInputStream in = new FileInputStream(file)) {
            if (in.read(header) != header.length) return false;
            return "SQLite format 3\u0000".equals(new String(header, StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isSafeSimpleName(String name) {
        return name != null
                && !name.isEmpty()
                && !name.contains("/")
                && !name.contains("\\")
                && !name.contains("..")
                && !name.startsWith(".");
    }

    private static void addFile(ZipOutputStream zip, File file, String entryName) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zip.putNextEntry(entry);
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) zip.write(buffer, 0, read);
        }
        zip.closeEntry();
    }

    private static void copyFile(File source, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.getFD().sync();
        }
    }

    private static void copyDirectoryContents(File sourceDir, File targetDir) throws IOException {
        if (sourceDir == null || targetDir == null) return;
        if (!targetDir.exists() && !targetDir.mkdirs()) throw new IOException("تعذر تجهيز مجلد الصور");
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile()) copyFile(file, new File(targetDir, file.getName()));
        }
    }

    private static void clearDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) for (File file : files) deleteRecursive(file);
    }

    private static void deleteIfExists(File file) {
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
