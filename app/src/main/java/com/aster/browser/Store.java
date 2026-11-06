package com.aster.browser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small JSON-file store for bookmarks, history, speed dial, downloads and the tab session.
 * Private windows open the store read-only so nothing they do is persisted.
 */
final class Store {
    static final class Entry {
        String title;
        String url;
        long time;

        Entry(String title, String url, long time) {
            this.title = title == null ? "" : title;
            this.url = url;
            this.time = time;
        }

        String label() {
            if (title != null && !title.trim().isEmpty()) return title.trim();
            return hostOf(url);
        }

        JSONObject toJson() throws Exception {
            JSONObject o = new JSONObject();
            o.put("t", title);
            o.put("u", url);
            o.put("ts", time);
            return o;
        }

        static Entry fromJson(JSONObject o) {
            if (o == null) return null;
            String url = o.optString("u", "");
            if (url.isEmpty()) return null;
            return new Entry(o.optString("t", ""), url, o.optLong("ts", 0L));
        }
    }

    static final int MAX_HISTORY = 2500;
    static final int MAX_DOWNLOADS = 150;

    final List<Entry> bookmarks = new ArrayList<>();
    final List<Entry> history = new ArrayList<>();
    final List<Entry> speedDial = new ArrayList<>();
    final List<Long> downloads = new ArrayList<>();

    private final File dir;
    private final boolean readOnly;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    Store(Context context, boolean readOnly) {
        this.readOnly = readOnly;
        dir = new File(context.getFilesDir(), "aster");
        if (!dir.exists()) dir.mkdirs();
        SharedPreferences prefs = context.getSharedPreferences("aster", Context.MODE_PRIVATE);
        loadList("bookmarks.json", bookmarks);
        loadList("history.json", history);
        boolean hasDial = loadList("speeddial.json", speedDial);
        loadDownloads();
        if (!readOnly) migrateLegacy(prefs);
        if (!hasDial && speedDial.isEmpty()) {
            addDefaultDial();
            saveSpeedDial();
        }
    }

    private void addDefaultDial() {
        String[][] defaults = {
            {"YouTube", "https://m.youtube.com"},
            {"Twitch", "https://m.twitch.tv"},
            {"Discord", "https://discord.com/app"},
            {"Reddit", "https://www.reddit.com"},
            {"GitHub", "https://github.com"},
            {"Wikipedia", "https://www.wikipedia.org"},
            {"Steam", "https://store.steampowered.com"},
            {"Brave Search", "https://search.brave.com"}
        };
        for (String[] d : defaults) speedDial.add(new Entry(d[0], d[1], 0L));
    }

    private void migrateLegacy(SharedPreferences prefs) {
        boolean changed = false;
        String oldBookmarks = prefs.getString("bookmarks", null);
        if (oldBookmarks != null) {
            for (String line : oldBookmarks.split("\n")) {
                String u = line.trim();
                if (isWeb(u) && indexOf(bookmarks, u) < 0) bookmarks.add(new Entry(hostOf(u), u, System.currentTimeMillis()));
            }
            saveBookmarks();
            changed = true;
        }
        String oldHistory = prefs.getString("history", null);
        if (oldHistory != null) {
            for (String line : oldHistory.split("\n")) {
                String u = line.trim();
                if (isWeb(u) && history.size() < MAX_HISTORY) history.add(new Entry(hostOf(u), u, 0L));
            }
            saveHistory();
            changed = true;
        }
        if (changed) prefs.edit().remove("bookmarks").remove("history").apply();
    }

    // ---------------------------------------------------------------- bookmarks

    boolean isBookmarked(String url) { return url != null && indexOf(bookmarks, url) >= 0; }

    boolean toggleBookmark(String title, String url) {
        int i = indexOf(bookmarks, url);
        if (i >= 0) {
            bookmarks.remove(i);
            saveBookmarks();
            return false;
        }
        bookmarks.add(0, new Entry(title, url, System.currentTimeMillis()));
        saveBookmarks();
        return true;
    }

    void saveBookmarks() { saveList("bookmarks.json", bookmarks); }

    // ---------------------------------------------------------------- history

    void addHistory(String title, String url) {
        if (readOnly || !isWeb(url)) return;
        int i = indexOf(history, url);
        if (i >= 0) history.remove(i);
        history.add(0, new Entry(title, url, System.currentTimeMillis()));
        while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
        saveHistory();
    }

    void updateHistoryTitle(String url, String title) {
        if (readOnly || url == null || title == null) return;
        int i = indexOf(history, url);
        if (i >= 0 && !title.equals(history.get(i).title)) {
            history.get(i).title = title;
            saveHistory();
        }
    }

    void clearHistory() {
        history.clear();
        saveHistory();
    }

    /** History changes on every page load, so writes are coalesced. */
    void saveHistory() {
        if (readOnly || historyPending) return;
        historyPending = true;
        main.postDelayed(historyWriter, 2000);
    }

    void flush() {
        if (!historyPending) return;
        main.removeCallbacks(historyWriter);
        historyWriter.run();
    }

    private boolean historyPending;
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable historyWriter = new Runnable() {
        @Override public void run() {
            historyPending = false;
            saveList("history.json", history);
        }
    };

    // ---------------------------------------------------------------- speed dial

    void saveSpeedDial() { saveList("speeddial.json", speedDial); }

    // ---------------------------------------------------------------- downloads

    void addDownload(long id) {
        if (readOnly) return;
        downloads.remove(Long.valueOf(id));
        downloads.add(0, id);
        while (downloads.size() > MAX_DOWNLOADS) downloads.remove(downloads.size() - 1);
        final JSONArray a = new JSONArray();
        for (Long l : downloads) a.put(l.longValue());
        write("downloads.json", a.toString());
    }

    private void loadDownloads() {
        String raw = read("downloads.json");
        if (raw == null) return;
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) downloads.add(a.optLong(i));
        } catch (Exception ignored) { }
    }

    // ---------------------------------------------------------------- session

    void saveSession(JSONObject session) {
        if (readOnly) return;
        write("session.json", session.toString());
    }

    JSONObject loadSession() {
        String raw = read("session.json");
        if (raw == null) return null;
        try { return new JSONObject(raw); } catch (Exception e) { return null; }
    }

    // ---------------------------------------------------------------- search

    List<Entry> suggest(String query, int limit) {
        List<Entry> out = new ArrayList<>();
        if (query == null) return out;
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return out;
        for (Entry e : bookmarks) {
            if (out.size() >= limit) break;
            if (matches(e, q)) out.add(e);
        }
        for (Entry e : history) {
            if (out.size() >= limit) break;
            if (matches(e, q) && indexOf(out, e.url) < 0) out.add(e);
        }
        return out;
    }

    private boolean matches(Entry e, String q) {
        String u = e.url.toLowerCase(Locale.ROOT).replaceFirst("^https?://(www\\.)?", "");
        return u.startsWith(q) || u.contains("." + q) || (e.title != null && e.title.toLowerCase(Locale.ROOT).contains(q));
    }

    // ---------------------------------------------------------------- export / import

    JSONArray toJson(List<Entry> list) {
        JSONArray a = new JSONArray();
        for (Entry e : list) {
            try { a.put(e.toJson()); } catch (Exception ignored) { }
        }
        return a;
    }

    void replace(List<Entry> target, JSONArray source, int max) {
        target.clear();
        if (source == null) return;
        for (int i = 0; i < source.length() && target.size() < max; i++) {
            Entry e = Entry.fromJson(source.optJSONObject(i));
            if (e != null && isWeb(e.url)) target.add(e);
        }
    }

    // ---------------------------------------------------------------- helpers

    static int indexOf(List<Entry> list, String url) {
        if (url == null) return -1;
        for (int i = 0; i < list.size(); i++) if (url.equals(list.get(i).url)) return i;
        return -1;
    }

    static boolean isWeb(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://"));
    }

    static String hostOf(String url) {
        if (url == null) return "";
        try {
            String h = android.net.Uri.parse(url).getHost();
            if (h == null) return url;
            return h.startsWith("www.") ? h.substring(4) : h;
        } catch (Exception e) {
            return url;
        }
    }

    private boolean loadList(String name, List<Entry> target) {
        String raw = read(name);
        if (raw == null) return false;
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                Entry e = Entry.fromJson(a.optJSONObject(i));
                if (e != null) target.add(e);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void saveList(String name, List<Entry> list) {
        if (readOnly) return;
        write(name, toJson(list).toString());
    }

    private void write(final String name, final String content) {
        if (readOnly) return;
        io.execute(new Runnable() {
            @Override public void run() {
                File tmp = new File(dir, name + ".tmp");
                try (FileOutputStream out = new FileOutputStream(tmp)) {
                    out.write(content.getBytes("UTF-8"));
                    out.getFD().sync();
                } catch (Exception e) {
                    return;
                }
                File target = new File(dir, name);
                if (!tmp.renameTo(target)) {
                    target.delete();
                    tmp.renameTo(target);
                }
            }
        });
    }

    private String read(String name) {
        File f = new File(dir, name);
        if (!f.exists()) return null;
        try (InputStream in = new FileInputStream(f)) {
            return new String(readAll(in, 16 * 1024 * 1024), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    static byte[] readAll(InputStream in, int limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > limit) throw new IllegalStateException("File is too large");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    static void removeWhere(List<Entry> list, String url) {
        Iterator<Entry> it = list.iterator();
        while (it.hasNext()) if (it.next().url.equals(url)) it.remove();
    }
}
