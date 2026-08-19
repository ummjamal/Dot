package com.module.dot.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LocalFormat {

    private LocalFormat() {
        // Utility class - لا نحتاج إنشاء كائن منها
    }

    /**
     * تنسيق المبالغ بالريال اليمني.
     *
     * أمثلة:
     * 0       -> 0 ر.ي
     * 500     -> 500 ر.ي
     * 1500    -> 1,500 ر.ي
     * 25000   -> 25,000 ر.ي
     */
    public static String getCurrencyFormat(double amount) {

        DecimalFormatSymbols symbols =
                new DecimalFormatSymbols(Locale.US);

        DecimalFormat formatter =
                new DecimalFormat("#,##0", symbols);

        formatter.setGroupingUsed(true);

        long roundedAmount =
                Math.round(amount);

        return formatter.format(roundedAmount)
                + " ر.ي";
    }

    /**
     * إرجاع التاريخ والوقت الحاليين.
     *
     * index 0 = التاريخ
     * index 1 = الوقت
     *
     * مثال:
     * 2026-08-20
     * 02:15:30
     */
    public static String[] getCurrentDateTime() {

        Date now = new Date();

        SimpleDateFormat dateFormatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        SimpleDateFormat timeFormatter =
                new SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.US
                );

        return new String[]{
                dateFormatter.format(now),
                timeFormatter.format(now)
        };
    }
}