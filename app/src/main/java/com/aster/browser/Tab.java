package com.aster.browser;

import android.graphics.Bitmap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** One browser tab. When the GX RAM limiter puts a tab to sleep its WebView is destroyed and only the URL is kept. */
final class Tab {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    final int id = NEXT_ID.getAndIncrement();
    AsterWebView web;
    String url;
    String title = "";
    Bitmap thumbnail;
    long lastUsed = System.currentTimeMillis();
    boolean start;
    boolean reader;
    boolean readerPending;
    boolean desktop;
    Bitmap favicon;
    boolean loading;
    int progress;
    volatile String topHost;
    final AtomicInteger blockedAds = new AtomicInteger();
    final AtomicInteger blockedTrackers = new AtomicInteger();
    final Map<String, Integer> blockedHosts = new LinkedHashMap<>();

    Tab(String url) {
        this.url = url;
    }

    boolean sleeping() { return web == null; }

    int blocked() { return blockedAds.get() + blockedTrackers.get(); }

    void resetCounters() {
        blockedAds.set(0);
        blockedTrackers.set(0);
        synchronized (blockedHosts) { blockedHosts.clear(); }
    }

    void recordBlocked(String host, boolean tracker) {
        if (tracker) blockedTrackers.incrementAndGet(); else blockedAds.incrementAndGet();
        synchronized (blockedHosts) {
            Integer n = blockedHosts.get(host);
            if (n == null && blockedHosts.size() >= 300) return;
            blockedHosts.put(host, n == null ? 1 : n + 1);
        }
    }

    String displayTitle() {
        if (start) return "New tab";
        if (title != null && !title.trim().isEmpty()) return title.trim();
        return Store.hostOf(url);
    }
}
