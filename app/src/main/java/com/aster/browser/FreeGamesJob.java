package com.aster.browser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Background check (every few hours, only with a network) for new free games on the stores the
 * user chose. The first check only records what is already free, so it never floods you with
 * notifications for old offers.
 */
public class FreeGamesJob extends JobService {
    static final int JOB_ID = 4201;
    static final String CHANNEL = "free_games";
    static final String ACTION_OPEN_ARCADE = "com.aster.browser.OPEN_ARCADE";
    static final String[] STORE_IDS = {"epic", "steam", "xbox", "ps", "gog", "mobile"};
    static final String[] STORE_NAMES = {"Epic Games Store", "Steam", "Xbox", "PlayStation", "GOG", "Mobile (Android / iOS)"};
    private static final long PERIOD = 4L * 60L * 60L * 1000L;
    private static final int MAX_SEEN = 400;

    static Set<String> defaultStores() {
        Set<String> s = new LinkedHashSet<>();
        s.add("epic");
        s.add("steam");
        s.add("xbox");
        return s;
    }

    static void schedule(Context c, boolean on) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (js == null) return;
        if (!on) {
            js.cancel(JOB_ID);
            return;
        }
        // Keep an existing schedule instead of restarting its period on every app launch.
        for (JobInfo pending : js.getAllPendingJobs()) if (pending.getId() == JOB_ID) return;
        JobInfo info = new JobInfo.Builder(JOB_ID, new ComponentName(c, FreeGamesJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(PERIOD)
            .setPersisted(true)
            .build();
        try { js.schedule(info); } catch (Exception ignored) { }
    }

    @Override public boolean onStartJob(final JobParameters params) {
        new Thread(() -> {
            try {
                check(getApplicationContext(), true);
            } catch (Throwable ignored) {
            } finally {
                jobFinished(params, false);
            }
        }, "ASTER-free-games").start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }

    /** Fetches offers; posts notifications for ones not seen before (when {@code notify}). Runs off the UI thread. */
    static void check(Context c, boolean notify) {
        SharedPreferences prefs = c.getSharedPreferences("aster", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("notify_free_games", false) && notify) return;
        I18n.init(prefs.getString("language", "auto"));
        Set<String> stores = prefs.getStringSet("notify_stores", defaultStores());
        boolean baseline = !prefs.getBoolean("notify_baseline_done", false);
        Set<String> seen = new LinkedHashSet<>(prefs.getStringSet("notify_seen", new HashSet<String>()));

        List<JSONObject> fresh = new ArrayList<>();
        boolean fetchedSomething = false;
        try {
            if (stores.contains("epic")) {
                JSONObject epic = Arcade.epic(String.format(Locale.ROOT,
                    "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=%s&country=%s",
                    I18n.es ? "es-ES" : "en-US", countryCode()));
                JSONArray now = epic.optJSONArray("now");
                if (now != null) {
                    fetchedSomething = true;
                    for (int i = 0; i < now.length(); i++) {
                        JSONObject g = now.optJSONObject(i);
                        if (g == null) continue;
                        g.put("store", "Epic Games Store");
                        String key = "epic:" + g.optString("title") + ":" + g.optString("end");
                        if (seen.add(key)) fresh.add(g);
                    }
                }
            }
        } catch (Exception ignored) { }
        try {
            JSONArray games = Arcade.gamerPower("https://www.gamerpower.com/api/giveaways?type=game&sort-by=date");
            if (games.length() > 0) fetchedSomething = true;
            for (int i = 0; i < games.length(); i++) {
                JSONObject g = games.optJSONObject(i);
                if (g == null) continue;
                String store = matchStore(g.optString("platforms"), stores);
                if (store == null) continue;
                g.put("store", store);
                if (seen.add("gp:" + g.optString("url"))) fresh.add(g);
            }
        } catch (Exception ignored) { }
        if (!fetchedSomething) return;

        while (seen.size() > MAX_SEEN) seen.remove(seen.iterator().next());
        prefs.edit().putStringSet("notify_seen", seen).putBoolean("notify_baseline_done", true).apply();
        if (baseline || !notify || fresh.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        post(c, fresh);
    }

    private static String countryCode() {
        String cc = Locale.getDefault().getCountry();
        return cc == null || cc.length() != 2 ? "US" : cc;
    }

    /** Returns a display name if the giveaway's platforms match one of the chosen stores. */
    static String matchStore(String platforms, Set<String> stores) {
        String p = platforms == null ? "" : platforms.toLowerCase(Locale.ROOT);
        if (stores.contains("epic") && p.contains("epic")) return "Epic Games Store";
        if (stores.contains("steam") && p.contains("steam")) return "Steam";
        if (stores.contains("xbox") && p.contains("xbox")) return "Xbox";
        if (stores.contains("ps") && (p.contains("ps4") || p.contains("ps5") || p.contains("playstation"))) return "PlayStation";
        if (stores.contains("gog") && p.contains("gog")) return "GOG";
        if (stores.contains("mobile") && (p.contains("android") || p.contains("ios"))) return I18n.t("Mobile");
        return null;
    }

    private static void post(Context c, List<JSONObject> games) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        ensureChannel(c, nm);
        int shown = 0;
        for (JSONObject g : games) {
            if (shown >= 3) break;
            String url = g.optString("url");
            if (!url.startsWith("https://")) continue;
            Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            open.setClass(c, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(c, url.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            String worth = g.optString("worth");
            String text = I18n.f("Free on %s", g.optString("store"))
                + (worth.isEmpty() || "N/A".equals(worth) ? "" : " · " + I18n.f("was %s", worth));
            Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : legacyBuilder(c);
            b.setSmallIcon(R.drawable.ic_stat_aster)
                .setContentTitle("🎮 " + g.optString("title"))
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setColor(0xFFC270FF);
            Bitmap art = download(g.optString("image"));
            if (art != null) {
                b.setLargeIcon(art);
                b.setStyle(new Notification.BigPictureStyle().bigPicture(art).setSummaryText(text));
            }
            nm.notify(("free:" + url).hashCode(), b.build());
            shown++;
        }
        if (games.size() > shown) {
            Intent arcade = new Intent(ACTION_OPEN_ARCADE);
            arcade.setClass(c, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(c, 4202, arcade, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : legacyBuilder(c);
            b.setSmallIcon(R.drawable.ic_stat_aster)
                .setContentTitle(I18n.f("%d more free games", games.size() - shown))
                .setContentText(I18n.t("Open Arcade"))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setColor(0xFFC270FF);
            nm.notify(4202, b.build());
        }
    }

    @SuppressWarnings("deprecation")
    private static Notification.Builder legacyBuilder(Context c) { return new Notification.Builder(c); }

    static void ensureChannel(Context c, NotificationManager nm) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, I18n.t("Free game alerts"), NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription(I18n.t("New free games on the stores you follow"));
        nm.createNotificationChannel(ch);
    }

    private static Bitmap download(String url) {
        if (url == null || !url.startsWith("https://")) return null;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            if (conn.getResponseCode() != 200) return null;
            byte[] data;
            try (InputStream in = conn.getInputStream()) { data = Store.readAll(in, 4 * 1024 * 1024); }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int sample = 1;
            while (o.outWidth / (sample * 2) >= 720) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(data, 0, data.length, d);
        } catch (Throwable e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
