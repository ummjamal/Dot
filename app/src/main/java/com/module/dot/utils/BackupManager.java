package com.module.dot.utils;

import android.content.Context;

import com.module.dot.data.local.GroceryDatabase;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * نسخة احتياطية محلية تشمل قاعدة البيانات وصور الأصناف.
 * تستخدم Storage Access Framework من الواجهة، لذلك لا تحتاج أذونات ملفات عامة.
 */
public final class BackupManager {

    private static final String DB_ENTRY = "database/" + GroceryDatabase.DATABASE_NAME;
    private static final String IMAGE_PREFIX = "images/";

    private BackupManager() { }

    public static void createBackup(Context context, OutputStream outputStream) throws IOException {
        if (outputStream == null) throw new IOException("تعذر فتح ملف النسخة الاحتياطية");

        // فتح وإغلاق القاعدة يضمن إتمام العمليات المعلقة قبل النسخ.
        try (GroceryDatabase ignored = new GroceryDatabase(context)) {
            // no-op
        }

        File dbFile = context.getDatabasePath(GroceryDatabase.DATABASE_NAME);
        if (!dbFile.exists()) throw new IOException("قاعدة البيانات غير موجودة");

        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(outputStream))) {
            addFile(zip, dbFile, DB_ENTRY);

            File imagesDir = context.getDir("Items", Context.MODE_PRIVATE);
            File[] images = imagesDir.listFiles();
            if (images != null) {
                for (File image : images) {
                    if (image.isFile()) {
                        addFile(zip, image, IMAGE_PREFIX + image.getName());
                    }
                }
            }

            ZipEntry info = new ZipEntry("backup-version.txt");
            zip.putNextEntry(info);
            zip.write("BaqalatAlshuibiBackupV1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
                if (DB_ENTRY.equals(name)) {
                    target = tempDb;
                    foundDatabase = true;
                } else if (name.startsWith(IMAGE_PREFIX)) {
                    String imageName = name.substring(IMAGE_PREFIX.length());
                    if (!imageName.isEmpty() && !imageName.contains("/") && !imageName.contains("\\")) {
                        target = new File(tempImages, imageName);
                    }
                }

                if (target != null) {
                    File parent = target.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        int read;
                        while ((read = zip.read(buffer)) != -1) out.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
            }
        }

        if (!foundDatabase || !tempDb.exists() || tempDb.length() == 0) {
            deleteRecursive(tempDir);
            throw new IOException("ملف النسخة الاحتياطية غير صالح");
        }

        // أغلق أي helper قبل استبدال ملف SQLite.
        try (GroceryDatabase ignored = new GroceryDatabase(context)) {
            // no-op
        }

        File dbFile = context.getDatabasePath(GroceryDatabase.DATABASE_NAME);
        File parent = dbFile.getParentFile();
        if (parent != null) parent.mkdirs();

        deleteIfExists(new File(dbFile.getAbsolutePath() + "-wal"));
        deleteIfExists(new File(dbFile.getAbsolutePath() + "-shm"));
        deleteIfExists(new File(dbFile.getAbsolutePath() + "-journal"));

        copyFile(tempDb, dbFile);

        File imagesDir = context.getDir("Items", Context.MODE_PRIVATE);
        File[] oldImages = imagesDir.listFiles();
        if (oldImages != null) {
            for (File file : oldImages) deleteRecursive(file);
        }
        File[] restoredImages = tempImages.listFiles();
        if (restoredImages != null) {
            for (File file : restoredImages) {
                if (file.isFile()) copyFile(file, new File(imagesDir, file.getName()));
            }
        }

        deleteRecursive(tempDir);

        // تحقق سريع أن القاعدة المستعادة قابلة للفتح.
        try (GroceryDatabase db = new GroceryDatabase(context)) {
            db.getReadableDatabase();
        } catch (Exception e) {
            throw new IOException("تم نسخ الملف ولكن قاعدة البيانات المستعادة غير قابلة للفتح", e);
        }
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
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            out.getFD().sync();
        }
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
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
