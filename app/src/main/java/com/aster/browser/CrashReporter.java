package com.aster.browser;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Saves a report when ASTER crashes so it can be shown (and shared) on the next start.
 * Nothing is sent anywhere automatically.
 */
final class CrashReporter {
    private static final String FILE = "last_crash.txt";
    private static boolean installed;

    private CrashReporter() { }

    static synchronized void install(Context context) {
        if (installed) return;
        installed = true;
        final File file = new File(context.getFilesDir(), FILE);
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("ASTER Browser " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
                pw.println("Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ") · " + Build.MANUFACTURER + " " + Build.MODEL);
                pw.println("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()));
                pw.println("Thread: " + thread.getName());
                pw.println();
                error.printStackTrace(pw);
                pw.flush();
                String report = sw.toString();
                if (report.length() > 60000) report = report.substring(0, 60000);
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(report.getBytes("UTF-8"));
                }
            } catch (Throwable ignored) {
                // Never let the reporter itself hide the original crash.
            }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    /** The report from the last crash, or null. */
    static String pending(Context context) {
        File f = new File(context.getFilesDir(), FILE);
        if (!f.exists()) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            return new String(Store.readAll(in, 256 * 1024), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    static void clear(Context context) {
        new File(context.getFilesDir(), FILE).delete();
    }
}
