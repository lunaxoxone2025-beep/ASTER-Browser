package com.aster.browser;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight EasyList-compatible host-rule engine for Android WebView.
 * It intentionally imports only simple domain network rules and exceptions;
 * cosmetic selectors, scriptlets and full ABP option semantics need a browser engine
 * with deeper integration and are not claimed here.
 */
public final class AdBlockEngine {
    private static final String PREF_RULES = "adblock_domain_rules";
    private static final String PREF_ALLOW = "adblock_allow_rules";
    private static final String PREF_UPDATED = "adblock_last_update";
    private static final long UPDATE_INTERVAL = 7L * 24L * 60L * 60L * 1000L;
    private static final int MAX_RULES = 120000;
    private static final Pattern BLOCK_RULE = Pattern.compile("^\\|\\|([a-z0-9.-]+)\\^");
    private static final Pattern ALLOW_RULE = Pattern.compile("^@@\\|\\|([a-z0-9.-]+)\\^");
    private final SharedPreferences prefs;
    private volatile Set<String> blocked = Collections.emptySet();
    private volatile Set<String> allowed = Collections.emptySet();

    public AdBlockEngine(Context context) {
        prefs = context.getSharedPreferences("aster", Context.MODE_PRIVATE);
        loadCachedRules();
    }

    private void loadCachedRules() {
        Set<String> b = new HashSet<>();
        Set<String> a = new HashSet<>();
        addLines(b, prefs.getString(PREF_RULES, ""));
        addLines(a, prefs.getString(PREF_ALLOW, ""));
        addBuiltIns(b);
        blocked = Collections.unmodifiableSet(b);
        allowed = Collections.unmodifiableSet(a);
    }

    private void addLines(Set<String> target, String raw) {
        if (raw == null) return;
        for (String line : raw.split("\\n")) {
            String host = normalize(line);
            if (host != null) target.add(host);
        }
    }

    private void addBuiltIns(Set<String> target) {
        String[] hosts = {
            "doubleclick.net", "googlesyndication.com", "googleadservices.com",
            "adservice.google.com", "adnxs.com", "adsrvr.org", "adform.net",
            "taboola.com", "outbrain.com", "criteo.com", "rubiconproject.com",
            "pubmatic.com", "openx.net", "casalemedia.com", "quantserve.com",
            "scorecardresearch.com", "google-analytics.com", "analytics.google.com",
            "connect.facebook.net", "pixel.facebook.com", "analytics.tiktok.com",
            "bat.bing.com", "clarity.ms", "hotjar.com", "segment.io",
            "cdn.segment.com", "api.segment.io", "mixpanel.com", "amplitude.com"
        };
        Collections.addAll(target, hosts);
    }

    public boolean shouldBlock(String rawHost) {
        String host = normalize(rawHost);
        if (host == null) return false;
        if (matches(allowed, host)) return false;
        return matches(blocked, host);
    }

    private boolean matches(Set<String> rules, String host) {
        String candidate = host;
        while (candidate != null) {
            if (rules.contains(candidate)) return true;
            int dot = candidate.indexOf('.');
            candidate = dot < 0 ? null : candidate.substring(dot + 1);
        }
        return false;
    }

    private String normalize(String host) {
        if (host == null) return null;
        String value = host.trim().toLowerCase(Locale.ROOT);
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        if (value.startsWith(".")) value = value.substring(1);
        return value.matches("[a-z0-9.-]+") && value.contains(".") ? value : null;
    }

    public int getRuleCount() { return blocked.size(); }
    public long getLastUpdate() { return prefs.getLong(PREF_UPDATED, 0L); }
    public boolean needsUpdate() { return System.currentTimeMillis() - getLastUpdate() > UPDATE_INTERVAL; }

    public void updateAsync(Callback callback) {
        new Thread(() -> {
            Set<String> newBlocked = new HashSet<>();
            Set<String> newAllowed = new HashSet<>();
            addBuiltIns(newBlocked);
            int parsed = 0;
            String[] sources = {
                "https://easylist.to/easylist/easylist.txt",
                "https://easylist.to/easylist/easyprivacy.txt"
            };
            for (String source : sources) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(source).openConnection();
                    connection.setConnectTimeout(12000);
                    connection.setReadTimeout(20000);
                    connection.setRequestProperty("User-Agent", "ASTER-Browser/0.12.0");
                    connection.setRequestProperty("Accept", "text/plain");
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) continue;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null && parsed < MAX_RULES) {
                            String rule = line.trim().toLowerCase(Locale.ROOT);
                            if (rule.isEmpty() || rule.startsWith("!") || rule.startsWith("[") || rule.startsWith("@@") && !ALLOW_RULE.matcher(rule).find()) continue;
                            Matcher allow = ALLOW_RULE.matcher(rule);
                            Matcher block = BLOCK_RULE.matcher(rule);
                            if (allow.find()) {
                                String host = normalize(allow.group(1));
                                if (host != null) newAllowed.add(host);
                            } else if (block.find()) {
                                String host = normalize(block.group(1));
                                if (host != null) { newBlocked.add(host); parsed++; }
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Keep the last known good rules if a source is unavailable.
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            boolean success = parsed > 1000;
            if (success) {
                StringBuilder b = new StringBuilder();
                for (String host : newBlocked) b.append(host).append('\\n');
                StringBuilder a = new StringBuilder();
                for (String host : newAllowed) a.append(host).append('\\n');
                prefs.edit().putString(PREF_RULES, b.toString()).putString(PREF_ALLOW, a.toString())
                    .putLong(PREF_UPDATED, System.currentTimeMillis()).apply();
                blocked = Collections.unmodifiableSet(newBlocked);
                allowed = Collections.unmodifiableSet(newAllowed);
            }
            if (callback != null) callback.onComplete(success, parsed, getRuleCount());
        }, "ASTER-adblock-update").start();
    }

    public interface Callback {
        void onComplete(boolean success, int downloadedRules, int totalRules);
    }
}
