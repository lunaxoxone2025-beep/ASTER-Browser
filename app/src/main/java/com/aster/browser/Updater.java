package com.aster.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

/** Checks GitHub Releases for a newer ASTER APK and hands it to Android's installer. */
final class Updater {
    static final String REPO_URL = "https://github.com/lunaxoxone2025-beep/ASTER-Browser";
    private static final String RELEASES_API = "https://api.github.com/repos/lunaxoxone2025-beep/ASTER-Browser/releases/latest";
    private static final String APK_NAME = "ASTER-Browser.apk";
    private static final long THROTTLE = 6L * 60L * 60L * 1000L;

    private final Activity activity;
    private final SharedPreferences prefs;
    private boolean registered;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            long expected = prefs.getLong("update_download_id", -1L);
            if (expected >= 0 && id == expected) openDownloaded(expected);
        }
    };

    Updater(Activity activity, SharedPreferences prefs) {
        this.activity = activity;
        this.prefs = prefs;
    }

    void register() {
        if (registered) return;
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        else activity.registerReceiver(receiver, filter);
        registered = true;
    }

    void unregister() {
        if (!registered) return;
        try { activity.unregisterReceiver(receiver); } catch (Exception ignored) { }
        registered = false;
    }

    void resumePending() {
        long pending = prefs.getLong("update_download_id", -1L);
        if (pending >= 0 && (Build.VERSION.SDK_INT < 26 || activity.getPackageManager().canRequestPackageInstalls())) {
            openDownloaded(pending);
        }
    }

    void check(final boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("update_last_check", 0L) < THROTTLE) return;
        prefs.edit().putLong("update_last_check", now).apply();
        new Thread(new Runnable() {
            @Override public void run() {
                HttpURLConnection c = null;
                try {
                    c = (HttpURLConnection) new URL(RELEASES_API).openConnection();
                    c.setConnectTimeout(10000);
                    c.setReadTimeout(15000);
                    c.setRequestProperty("Accept", "application/vnd.github+json");
                    c.setRequestProperty("User-Agent", "ASTER-Browser-Android");
                    if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        if (force) toast("Could not reach GitHub Releases");
                        return;
                    }
                    JSONObject release = new JSONObject(new String(Store.readAll(c.getInputStream(), 2 * 1024 * 1024), "UTF-8"));
                    final String tag = release.optString("tag_name", "").trim();
                    JSONArray assets = release.optJSONArray("assets");
                    String apk = null;
                    if (assets != null) {
                        for (int i = 0; i < assets.length(); i++) {
                            JSONObject a = assets.optJSONObject(i);
                            if (a != null && APK_NAME.equals(a.optString("name"))) {
                                apk = a.optString("browser_download_url", null);
                                break;
                            }
                        }
                    }
                    if (apk == null || !apk.startsWith("https://") || compare(tag, BuildConfig.VERSION_NAME) <= 0) {
                        if (force) toast("ASTER " + BuildConfig.VERSION_NAME + " is up to date");
                        return;
                    }
                    final String url = apk;
                    activity.runOnUiThread(new Runnable() {
                        @Override public void run() { offer(tag, url); }
                    });
                } catch (Exception e) {
                    if (force) toast("Update check failed");
                } finally {
                    if (c != null) c.disconnect();
                }
            }
        }, "ASTER-update").start();
    }

    private void toast(final String msg) {
        activity.runOnUiThread(new Runnable() {
            @Override public void run() { Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show(); }
        });
    }

    static int compare(String remote, String local) {
        String[] a = remote.replaceFirst("^[vV]", "").split("\\.");
        String[] b = local.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int av = i < a.length ? part(a[i]) : 0;
            int bv = i < b.length ? part(b[i]) : 0;
            if (av != bv) return av < bv ? -1 : 1;
        }
        return 0;
    }

    private static int part(String p) {
        try { return Integer.parseInt(p.replaceAll("[^0-9].*$", "")); } catch (Exception e) { return 0; }
    }

    private void offer(String version, final String url) {
        if (activity.isFinishing()) return;
        new AlertDialog.Builder(activity)
            .setTitle("ASTER " + version + " is available")
            .setMessage("A newer version of ASTER Browser is ready. Download it now? Android will ask you to confirm the installation.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Download", (d, w) -> download(url))
            .show();
    }

    private void download(String url) {
        try {
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.setTitle("ASTER Browser update");
            r.setMimeType("application/vnd.android.package-archive");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, APK_NAME);
            long id = dm.enqueue(r);
            prefs.edit().putLong("update_download_id", id).apply();
            Toast.makeText(activity, "Downloading ASTER update…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(activity, "Could not download update", Toast.LENGTH_LONG).show();
        }
    }

    private void openDownloaded(long id) {
        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;
        Cursor cursor = null;
        try {
            cursor = dm.query(new DownloadManager.Query().setFilterById(id));
            if (cursor == null || !cursor.moveToFirst()) return;
            int col = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (col < 0 || cursor.getInt(col) != DownloadManager.STATUS_SUCCESSFUL) return;
        } finally {
            if (cursor != null) cursor.close();
        }
        Uri apk = dm.getUriForDownloadedFile(id);
        if (apk == null) return;
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                .setTitle("Allow ASTER updates")
                .setMessage("Allow ASTER Browser to install apps, then come back to finish the update.")
                .setNegativeButton("Later", null)
                .setPositiveButton("Open settings", (d, w) -> activity.startActivity(new Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName()))))
                .show();
            return;
        }
        try {
            prefs.edit().putLong("update_download_id", -1L).apply();
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apk, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(install);
        } catch (Exception e) {
            Toast.makeText(activity, "Android could not open the installer", Toast.LENGTH_LONG).show();
        }
    }
}
