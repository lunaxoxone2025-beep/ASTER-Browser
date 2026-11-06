package com.aster.browser;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ASTER Shields: network (domain) blocking, cosmetic element hiding and per-site settings.
 *
 * Network rules: ||domain^ rules, @@||domain^ exceptions and hosts-file entries from
 * EasyList, EasyPrivacy, AdGuard and Peter Lowe's list.
 * Cosmetic rules: generic "##selector" and site-specific "domain##selector" rules.
 * Procedural filters, scriptlets and full ABP option semantics are intentionally ignored.
 */
final class ShieldEngine {
    static final String FILTER_VERSION = "1.0.0";
    private static final long UPDATE_INTERVAL = 24L * 60L * 60L * 1000L;
    private static final int MAX_NETWORK_RULES = 260000;
    private static final int MAX_GENERIC_COSMETIC = 9000;
    private static final int MAX_SITE_COSMETIC = 40000;
    // Rough per-request estimates used for the start-page counters.
    static final long BYTES_PER_BLOCK = 30L * 1024L;
    static final long MS_PER_BLOCK = 50L;

    private static final Pattern BLOCK_RULE = Pattern.compile("^\\|\\|([a-z0-9.-]+)\\^(\\$(.*))?$");
    private static final Pattern ALLOW_RULE = Pattern.compile("^@@\\|\\|([a-z0-9.-]+)\\^");

    private static final String[][] SOURCES = {
        // url, kind (a = ads, t = trackers)
        {"https://easylist.to/easylist/easylist.txt", "a"},
        {"https://easylist.to/easylist/easyprivacy.txt", "t"},
        {"https://filters.adtidy.org/extension/ublock/filters/2.txt", "a"},
        {"https://filters.adtidy.org/extension/ublock/filters/3.txt", "t"},
        {"https://filters.adtidy.org/extension/ublock/filters/11.txt", "a"},
        {"https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=0&mimetype=plaintext", "a"}
    };

    private static final String[] BUILT_IN_ADS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "adservice.google.com",
        "adnxs.com", "adsrvr.org", "adform.net", "taboola.com", "outbrain.com", "criteo.com",
        "rubiconproject.com", "pubmatic.com", "openx.net", "casalemedia.com", "amazon-adsystem.com",
        "ads-twitter.com", "advertising.com", "bidswitch.net", "smartadserver.com", "lijit.com",
        "sharethrough.com", "yieldmo.com", "33across.com", "media.net", "revcontent.com", "mgid.com",
        "zedo.com", "adroll.com", "moatads.com", "doubleverify.com", "adsafeprotected.com", "serving-sys.com",
        "pagead2.googlesyndication.com", "securepubads.g.doubleclick.net", "tpc.googlesyndication.com",
        "popads.net", "propellerads.com", "exoclick.com", "juicyads.com", "adcash.com", "trafficjunky.net"
    };
    private static final String[] BUILT_IN_TRACKERS = {
        "google-analytics.com", "analytics.google.com", "googletagmanager.com", "connect.facebook.net",
        "pixel.facebook.com", "analytics.tiktok.com", "bat.bing.com", "clarity.ms", "hotjar.com",
        "segment.io", "cdn.segment.com", "api.segment.io", "mixpanel.com", "amplitude.com",
        "analytics.twitter.com", "scorecardresearch.com", "quantserve.com", "bluekai.com", "exelator.com",
        "eyeota.net", "demdex.net", "krxd.net", "agkn.com", "rlcdn.com", "newrelic.com", "nr-data.net",
        "fullstory.com", "mouseflow.com", "crazyegg.com", "branch.io", "app-measurement.com"
    };
    private static final String[] GENERIC_COSMETIC = {
        "ins.adsbygoogle", "[id^=\"google_ads_\"]", "[id^=\"div-gpt-ad\"]", "iframe[src*=\"doubleclick.net\"]",
        "iframe[src*=\"googlesyndication.com\"]", ".adsbygoogle", ".ad-container", ".ad-banner", ".ad-slot",
        ".advertisement", ".sponsored-ad", ".taboola", "#taboola-below-article-thumbnails", ".OUTBRAIN",
        "[data-ad-slot]", "[data-google-query-id]", "amp-ad", "amp-embed[type=\"taboola\"]"
    };

    private final SharedPreferences prefs;
    private final File dir;
    private volatile Set<String> ads = Collections.emptySet();
    private volatile Set<String> trackers = Collections.emptySet();
    private volatile Set<String> allowed = Collections.emptySet();
    private volatile List<String> genericCosmetic = Collections.emptyList();
    private volatile Map<String, List<String>> siteCosmetic = Collections.emptyMap();
    private volatile String genericCss = null;
    private volatile boolean updating = false;
    private final Map<String, JSONObject> siteSettings = new HashMap<>();

    final AtomicLong totalAds = new AtomicLong();
    final AtomicLong totalTrackers = new AtomicLong();
    private long savedAds;
    private long savedTrackers;

    interface Callback { void onComplete(boolean success, int rules); }

    ShieldEngine(Context context, SharedPreferences prefs) {
        this.prefs = prefs;
        dir = new File(context.getFilesDir(), "shields");
        if (!dir.exists()) dir.mkdirs();
        totalAds.set(prefs.getLong("stat_ads", 0L));
        totalTrackers.set(prefs.getLong("stat_trackers", prefs.getLong("stat_blocked_legacy", 0L)));
        savedAds = totalAds.get();
        savedTrackers = totalTrackers.get();
        loadSiteSettings();
        installBuiltIns();
        new Thread(new Runnable() {
            @Override public void run() { loadCached(); }
        }, "ASTER-shields-load").start();
    }

    // ------------------------------------------------------------------ preferences

    boolean enabledGlobally() { return prefs.getBoolean("shield_enabled", true); }
    boolean aggressive() { return "aggressive".equals(prefs.getString("shield_mode", "standard")); }
    boolean cosmeticEnabled() { return prefs.getBoolean("shield_cosmetic", true); }
    boolean fingerprintDefault() { return prefs.getBoolean("shield_fingerprint", true); }

    private void loadSiteSettings() {
        String raw = prefs.getString("shield_sites", "{}");
        try {
            JSONObject all = new JSONObject(raw);
            Iterator<String> keys = all.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject v = all.optJSONObject(k);
                if (v != null) siteSettings.put(k, v);
            }
        } catch (Exception ignored) { }
        // Migrate 0.x "pause protection on this site" exceptions.
        String legacy = prefs.getString("shield_site_exceptions", null);
        if (legacy != null) {
            for (String line : legacy.split("\n")) {
                String host = normalize(line);
                if (host != null) setSite(host, "up", false);
            }
            prefs.edit().remove("shield_site_exceptions").apply();
        }
    }

    private synchronized void saveSiteSettings() {
        JSONObject all = new JSONObject();
        try {
            for (Map.Entry<String, JSONObject> e : siteSettings.entrySet()) all.put(e.getKey(), e.getValue());
        } catch (Exception ignored) { }
        prefs.edit().putString("shield_sites", all.toString()).apply();
    }

    synchronized boolean siteFlag(String host, String key, boolean fallback) {
        if (host == null) return fallback;
        JSONObject o = siteSettings.get(siteKey(host));
        if (o == null || !o.has(key)) return fallback;
        return o.optBoolean(key, fallback);
    }

    synchronized void setSite(String host, String key, boolean value) {
        if (host == null) return;
        String k = siteKey(host);
        JSONObject o = siteSettings.get(k);
        if (o == null) { o = new JSONObject(); siteSettings.put(k, o); }
        try { o.put(key, value); } catch (Exception ignored) { }
        saveSiteSettings();
    }

    synchronized void resetSite(String host) {
        if (host == null) return;
        siteSettings.remove(siteKey(host));
        saveSiteSettings();
    }

    synchronized List<String> customizedSites() {
        List<String> out = new ArrayList<>(siteSettings.keySet());
        Collections.sort(out);
        return out;
    }

    synchronized JSONObject exportSites() {
        JSONObject all = new JSONObject();
        try {
            for (Map.Entry<String, JSONObject> e : siteSettings.entrySet()) all.put(e.getKey(), e.getValue());
        } catch (Exception ignored) { }
        return all;
    }

    synchronized void importSites(JSONObject all) {
        if (all == null) return;
        siteSettings.clear();
        Iterator<String> keys = all.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            JSONObject v = all.optJSONObject(k);
            String h = normalize(k);
            if (v != null && h != null) siteSettings.put(h, v);
        }
        saveSiteSettings();
    }

    boolean shieldsUp(String host) { return enabledGlobally() && siteFlag(host, "up", true); }
    boolean blockScripts(String host) { return shieldsUp(host) && siteFlag(host, "scripts", false); }
    boolean blockFingerprinting(String host) { return shieldsUp(host) && siteFlag(host, "fp", fingerprintDefault()); }
    boolean cosmetic(String host) { return shieldsUp(host) && siteFlag(host, "cosmetic", cosmeticEnabled()); }

    // ------------------------------------------------------------------ blocking

    /** @return 0 = allow, 1 = blocked ad, 2 = blocked tracker */
    int classify(String requestHost, String topHost) {
        String host = normalize(requestHost);
        if (host == null || !shieldsUp(topHost)) return 0;
        if (matches(allowed, host)) return 0;
        boolean tracker = matches(trackers, host);
        boolean ad = !tracker && matches(ads, host);
        if (!tracker && !ad) return 0;
        // Standard mode, like most privacy browsers, does not block first-party requests.
        if (!aggressive() && topHost != null && sameSite(host, topHost)) return 0;
        if (tracker) { totalTrackers.incrementAndGet(); return 2; }
        totalAds.incrementAndGet();
        return 1;
    }

    long totalBlocked() { return totalAds.get() + totalTrackers.get(); }

    void persistStats() {
        long a = totalAds.get();
        long t = totalTrackers.get();
        if (a == savedAds && t == savedTrackers) return;
        savedAds = a;
        savedTrackers = t;
        prefs.edit().putLong("stat_ads", a).putLong("stat_trackers", t).apply();
    }

    void resetStats() {
        totalAds.set(0);
        totalTrackers.set(0);
        persistStats();
    }

    int ruleCount() { return ads.size() + trackers.size(); }
    int cosmeticCount() {
        int n = genericCosmetic.size();
        for (List<String> l : siteCosmetic.values()) n += l.size();
        return n;
    }
    long lastUpdate() { return prefs.getLong("shield_last_update", 0L); }
    boolean isUpdating() { return updating; }

    boolean needsUpdate() {
        return !FILTER_VERSION.equals(prefs.getString("shield_filter_version", ""))
            || System.currentTimeMillis() - lastUpdate() > UPDATE_INTERVAL;
    }

    /** CSS that hides ad containers on the given site (generic + site-specific rules). */
    String cosmeticCss(String host) {
        StringBuilder css = new StringBuilder();
        String generic = genericCss;
        if (generic == null) {
            StringBuilder g = new StringBuilder();
            for (String sel : genericCosmetic) g.append(sel).append("{display:none!important}\n");
            generic = g.toString();
            genericCss = generic;
        }
        css.append(generic);
        String h = normalize(host);
        Map<String, List<String>> site = siteCosmetic;
        while (h != null) {
            List<String> list = site.get(h);
            if (list != null) for (String sel : list) css.append(sel).append("{display:none!important}\n");
            int dot = h.indexOf('.');
            h = dot < 0 ? null : h.substring(dot + 1);
            if (h != null && h.indexOf('.') < 0) break;
        }
        return css.toString();
    }

    // ------------------------------------------------------------------ loading / updating

    private void installBuiltIns() {
        Set<String> a = new HashSet<>();
        Set<String> t = new HashSet<>();
        Collections.addAll(a, BUILT_IN_ADS);
        Collections.addAll(t, BUILT_IN_TRACKERS);
        List<String> g = new ArrayList<>();
        Collections.addAll(g, GENERIC_COSMETIC);
        ads = a;
        trackers = t;
        genericCosmetic = g;
        genericCss = null;
    }

    private void loadCached() {
        Set<String> a = new HashSet<>();
        Set<String> t = new HashSet<>();
        Set<String> allow = new HashSet<>();
        List<String> generic = new ArrayList<>();
        Map<String, List<String>> site = new HashMap<>();
        Collections.addAll(a, BUILT_IN_ADS);
        Collections.addAll(t, BUILT_IN_TRACKERS);
        Collections.addAll(generic, GENERIC_COSMETIC);
        boolean any = readLines("ads.txt", a) | readLines("trackers.txt", t);
        readLines("allow.txt", allow);
        Set<String> gset = new HashSet<>(generic);
        List<String> raw = new ArrayList<>();
        readLinesList("cosmetic_generic.txt", raw);
        for (String s : raw) if (gset.add(s)) generic.add(s);
        raw.clear();
        readLinesList("cosmetic_site.txt", raw);
        for (String line : raw) {
            int tab = line.indexOf('\t');
            if (tab <= 0) continue;
            addSiteRule(site, line.substring(0, tab), line.substring(tab + 1));
        }
        if (any || !raw.isEmpty()) {
            ads = a;
            trackers = t;
            allowed = allow;
            genericCosmetic = generic;
            siteCosmetic = site;
            genericCss = null;
        }
    }

    void updateAsync(final Callback callback) {
        if (updating) return;
        updating = true;
        new Thread(new Runnable() {
            @Override public void run() {
                int count = 0;
                boolean ok = false;
                try {
                    count = download();
                    ok = count > 1000;
                } catch (Throwable ignored) {
                    ok = false;
                } finally {
                    updating = false;
                }
                if (callback != null) callback.onComplete(ok, ruleCount());
            }
        }, "ASTER-shields-update").start();
    }

    private int download() {
        Set<String> a = new HashSet<>();
        Set<String> t = new HashSet<>();
        Set<String> allow = new HashSet<>();
        Set<String> genericSet = new HashSet<>();
        List<String> generic = new ArrayList<>();
        Map<String, List<String>> site = new HashMap<>();
        Collections.addAll(a, BUILT_IN_ADS);
        Collections.addAll(t, BUILT_IN_TRACKERS);
        for (String s : GENERIC_COSMETIC) if (genericSet.add(s)) generic.add(s);
        int network = 0;
        int siteRules = 0;
        for (String[] source : SOURCES) {
            boolean trackerList = "t".equals(source[1]);
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(source[0]).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setRequestProperty("User-Agent", "ASTER-Browser/" + FILTER_VERSION);
                c.setRequestProperty("Accept", "text/plain");
                if (c.getResponseCode() != HttpURLConnection.HTTP_OK) continue;
                BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String rule = line.trim();
                        if (rule.isEmpty() || rule.charAt(0) == '!' || rule.charAt(0) == '[') continue;
                        int hash = rule.indexOf("##");
                        if (hash >= 0) {
                            if (rule.contains("#@#") || rule.contains("#?#") || rule.contains("#$#") || rule.contains("#%#")) continue;
                            String domains = rule.substring(0, hash);
                            String selector = rule.substring(hash + 2).trim();
                            if (!validSelector(selector)) continue;
                            if (domains.isEmpty()) {
                                if (generic.size() < MAX_GENERIC_COSMETIC && genericSet.add(selector)) generic.add(selector);
                            } else if (siteRules < MAX_SITE_COSMETIC) {
                                for (String d : domains.split(",")) {
                                    if (d.startsWith("~")) continue;
                                    String h = normalize(d);
                                    if (h != null && addSiteRule(site, h, selector)) siteRules++;
                                }
                            }
                            continue;
                        }
                        if (rule.charAt(0) == '#') continue;
                        String lower = rule.toLowerCase(Locale.ROOT);
                        Matcher allowM = ALLOW_RULE.matcher(lower);
                        if (allowM.find()) {
                            String h = normalize(allowM.group(1));
                            if (h != null) allow.add(h);
                            continue;
                        }
                        if (lower.startsWith("@@")) continue;
                        Matcher block = BLOCK_RULE.matcher(lower);
                        if (block.find()) {
                            String opts = block.group(3);
                            // Skip rules restricted to specific domains or types we cannot evaluate.
                            if (opts != null && (opts.contains("domain=") || opts.contains("~third-party")
                                || opts.contains("redirect") || opts.contains("removeparam") || opts.contains("csp="))) continue;
                            String h = normalize(block.group(1));
                            if (h != null && network < MAX_NETWORK_RULES && (trackerList ? t.add(h) : a.add(h))) network++;
                            continue;
                        }
                        String[] fields = lower.split("\\s+");
                        if (fields.length >= 2 && ("0.0.0.0".equals(fields[0]) || "127.0.0.1".equals(fields[0]))) {
                            for (int i = 1; i < fields.length && network < MAX_NETWORK_RULES; i++) {
                                if (fields[i].startsWith("#")) break;
                                String h = normalize(fields[i]);
                                if (h != null && !h.endsWith("localhost") && (trackerList ? t.add(h) : a.add(h))) network++;
                            }
                        }
                    }
                } finally {
                    reader.close();
                }
            } catch (Exception ignored) {
                // Keep last known good lists when a source is offline.
            } finally {
                if (c != null) c.disconnect();
            }
        }
        if (network <= 1000) return network;
        // Exceptions win over blocks of the same host.
        a.removeAll(allow);
        t.removeAll(allow);
        writeLines("ads.txt", a);
        writeLines("trackers.txt", t);
        writeLines("allow.txt", allow);
        writeLines("cosmetic_generic.txt", generic);
        List<String> siteLines = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : site.entrySet()) {
            for (String sel : e.getValue()) siteLines.add(e.getKey() + "\t" + sel);
        }
        writeLines("cosmetic_site.txt", siteLines);
        ads = a;
        trackers = t;
        allowed = allow;
        genericCosmetic = generic;
        siteCosmetic = site;
        genericCss = null;
        prefs.edit().putLong("shield_last_update", System.currentTimeMillis())
            .putString("shield_filter_version", FILTER_VERSION).apply();
        return network;
    }

    private static boolean addSiteRule(Map<String, List<String>> site, String host, String selector) {
        List<String> list = site.get(host);
        if (list == null) { list = new ArrayList<>(); site.put(host, list); }
        if (list.size() >= 400 || list.contains(selector)) return false;
        list.add(selector);
        return true;
    }

    private static boolean validSelector(String s) {
        if (s.isEmpty() || s.length() > 300) return false;
        if (s.contains(":-abp") || s.contains(":has-text") || s.contains(":style(") || s.contains(":matches-")
            || s.contains(":xpath") || s.contains(":upward") || s.contains(":remove(") || s.contains("+js(")
            || s.contains(":watch-attr") || s.contains(":min-text-length") || s.contains(":others(")) return false;
        if (s.indexOf('{') >= 0 || s.indexOf('}') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('<') >= 0) return false;
        return true;
    }

    private boolean readLines(String name, Set<String> target) {
        File f = new File(dir, name);
        if (!f.exists()) return false;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) if (!line.isEmpty()) target.add(line);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void readLinesList(String name, List<String> target) {
        File f = new File(dir, name);
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String line;
            while ((line = r.readLine()) != null) if (!line.isEmpty()) target.add(line);
        } catch (Exception ignored) { }
    }

    private void writeLines(String name, java.util.Collection<String> lines) {
        File tmp = new File(dir, name + ".tmp");
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp), "UTF-8"))) {
            for (String l : lines) { w.write(l); w.write('\n'); }
        } catch (Exception e) {
            return;
        }
        File target = new File(dir, name);
        if (!tmp.renameTo(target)) {
            target.delete();
            tmp.renameTo(target);
        }
    }

    // ------------------------------------------------------------------ host helpers

    private static boolean matches(Set<String> rules, String host) {
        String candidate = host;
        while (candidate != null) {
            if (rules.contains(candidate)) return true;
            int dot = candidate.indexOf('.');
            candidate = dot < 0 ? null : candidate.substring(dot + 1);
        }
        return false;
    }

    static String normalize(String host) {
        if (host == null) return null;
        String v = host.trim().toLowerCase(Locale.ROOT);
        while (v.endsWith(".")) v = v.substring(0, v.length() - 1);
        while (v.startsWith(".")) v = v.substring(1);
        if (v.isEmpty() || !v.contains(".")) return null;
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (!((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '.' || ch == '-')) return null;
        }
        return v;
    }

    /** Approximates eTLD+1 without a public-suffix list (handles common two-part suffixes). */
    static String siteKey(String host) {
        String h = normalize(host);
        if (h == null) return host == null ? "" : host;
        if (h.startsWith("www.")) h = h.substring(4);
        return h;
    }

    static String registrable(String host) {
        String h = normalize(host);
        if (h == null) return null;
        String[] p = h.split("\\.");
        if (p.length <= 2) return h;
        String last2 = p[p.length - 2] + "." + p[p.length - 1];
        boolean twoPartSuffix = p[p.length - 2].length() <= 3 && p[p.length - 1].length() == 2
            && (p[p.length - 2].equals("co") || p[p.length - 2].equals("com") || p[p.length - 2].equals("org")
            || p[p.length - 2].equals("net") || p[p.length - 2].equals("ac") || p[p.length - 2].equals("gov")
            || p[p.length - 2].equals("edu") || p[p.length - 2].equals("ne") || p[p.length - 2].equals("or"));
        if (twoPartSuffix && p.length >= 3) return p[p.length - 3] + "." + last2;
        return last2;
    }

    static boolean sameSite(String a, String b) {
        String ra = registrable(a);
        String rb = registrable(b);
        return ra != null && ra.equals(rb);
    }
}
