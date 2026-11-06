package com.aster.browser;

import android.util.Xml;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Arcade data: free games (Epic Games Store's official promotions feed and GamerPower giveaways
 * for Steam, Xbox, PlayStation, GOG, mobile and more) and gaming news from public RSS feeds.
 */
final class Arcade {
    private Arcade() { }

    private static final String EPIC = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=%s&country=%s";
    private static final String GP_GAMES = "https://www.gamerpower.com/api/giveaways?type=game&sort-by=popularity";
    private static final String GP_LOOT = "https://www.gamerpower.com/api/giveaways?type=loot&sort-by=popularity";
    private static final String[][] FEEDS = {
        // id, name, url, language ("" = all)
        {"gamespot", "GameSpot", "https://www.gamespot.com/feeds/news/", ""},
        {"pcgamer", "PC Gamer", "https://www.pcgamer.com/rss/", ""},
        {"xbox", "Xbox Wire", "https://news.xbox.com/en-us/feed/", ""},
        {"playstation", "PlayStation Blog", "https://blog.playstation.com/feed/", ""},
        {"vandal", "Vandal", "https://vandal.elespanol.com/xml.cgi", "es"}
    };
    private static final Pattern IMG = Pattern.compile("<img[^>]+src=[\"'](https://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    /** Fetches everything in parallel. Sources that fail are simply left empty. */
    static JSONObject fetchAll(final boolean spanish, String country) {
        ExecutorService pool = Executors.newFixedThreadPool(6);
        JSONObject out = new JSONObject();
        try {
            final String locale = spanish ? "es-ES" : "en-US";
            final String cc = country == null || country.length() != 2 ? "US" : country.toUpperCase(Locale.ROOT);
            Future<JSONObject> epic = pool.submit(() -> epic(String.format(Locale.ROOT, EPIC, locale, cc)));
            Future<JSONArray> games = pool.submit(() -> gamerPower(GP_GAMES));
            Future<JSONArray> loot = pool.submit(() -> gamerPower(GP_LOOT));
            List<Future<List<JSONObject>>> feeds = new ArrayList<>();
            for (final String[] f : FEEDS) {
                if (!f[3].isEmpty() && !(spanish && "es".equals(f[3]))) continue;
                feeds.add(pool.submit(() -> rss(f[0], f[1], f[2])));
            }
            out.put("epic", get(epic, new JSONObject()));
            out.put("games", get(games, new JSONArray()));
            out.put("loot", get(loot, new JSONArray()));
            List<JSONObject> news = new ArrayList<>();
            for (Future<List<JSONObject>> f : feeds) news.addAll(get(f, new ArrayList<JSONObject>()));
            Collections.sort(news, new Comparator<JSONObject>() {
                @Override public int compare(JSONObject a, JSONObject b) { return Long.compare(b.optLong("time"), a.optLong("time")); }
            });
            JSONArray n = new JSONArray();
            for (int i = 0; i < news.size() && i < 80; i++) n.put(news.get(i));
            out.put("news", n);
            out.put("fetched", System.currentTimeMillis());
        } catch (Exception ignored) {
        } finally {
            pool.shutdownNow();
        }
        return out;
    }

    private static <T> T get(Future<T> f, T fallback) {
        try {
            T v = f.get(20, TimeUnit.SECONDS);
            return v == null ? fallback : v;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) ASTER-Browser/" + BuildConfig.VERSION_NAME);
        return c;
    }

    private static String text(String url) throws Exception {
        HttpURLConnection c = open(url);
        try {
            if (c.getResponseCode() != 200) return null;
            return new String(Store.readAll(c.getInputStream(), 6 * 1024 * 1024), "UTF-8");
        } finally {
            c.disconnect();
        }
    }

    // ------------------------------------------------------------------ Epic Games Store

    static JSONObject epic(String url) throws Exception {
        JSONObject result = new JSONObject();
        JSONArray now = new JSONArray();
        JSONArray soon = new JSONArray();
        String body = text(url);
        if (body != null) {
            JSONObject root = new JSONObject(body);
            JSONObject data = root.optJSONObject("data");
            JSONObject catalog = data == null ? null : data.optJSONObject("Catalog");
            JSONObject store = catalog == null ? null : catalog.optJSONObject("searchStore");
            JSONArray elements = store == null ? null : store.optJSONArray("elements");
            if (elements != null) for (int i = 0; i < elements.length(); i++) {
                JSONObject e = elements.optJSONObject(i);
                if (e == null) continue;
                JSONObject promos = e.optJSONObject("promotions");
                if (promos == null) continue;
                String[] current = freeWindow(promos.optJSONArray("promotionalOffers"));
                String[] upcoming = freeWindow(promos.optJSONArray("upcomingPromotionalOffers"));
                if (current == null && upcoming == null) continue;
                JSONObject g = new JSONObject();
                g.put("title", e.optString("title"));
                g.put("desc", e.optString("description"));
                g.put("image", epicImage(e.optJSONArray("keyImages")));
                g.put("url", epicUrl(e));
                JSONObject price = e.optJSONObject("price");
                JSONObject total = price == null ? null : price.optJSONObject("totalPrice");
                JSONObject fmt = total == null ? null : total.optJSONObject("fmtPrice");
                g.put("worth", fmt == null ? "" : fmt.optString("originalPrice", ""));
                if (current != null) {
                    g.put("end", current[1]);
                    now.put(g);
                } else {
                    g.put("start", upcoming[0]);
                    g.put("end", upcoming[1]);
                    soon.put(g);
                }
            }
        }
        result.put("now", now);
        result.put("soon", soon);
        return result;
    }

    /** Returns {start, end} of the first 100%-off offer in a promotion list, or null. */
    private static String[] freeWindow(JSONArray groups) {
        if (groups == null) return null;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject group = groups.optJSONObject(i);
            JSONArray offers = group == null ? null : group.optJSONArray("promotionalOffers");
            if (offers == null) continue;
            for (int j = 0; j < offers.length(); j++) {
                JSONObject o = offers.optJSONObject(j);
                JSONObject d = o == null ? null : o.optJSONObject("discountSetting");
                if (d != null && d.optInt("discountPercentage", -1) == 0) {
                    return new String[]{o.optString("startDate"), o.optString("endDate")};
                }
            }
        }
        return null;
    }

    private static String epicImage(JSONArray images) {
        if (images == null) return "";
        String[] preferred = {"OfferImageWide", "DieselStoreFrontWide", "featuredMedia", "Thumbnail", "OfferImageTall"};
        for (String type : preferred) {
            for (int i = 0; i < images.length(); i++) {
                JSONObject im = images.optJSONObject(i);
                if (im != null && type.equals(im.optString("type")) && im.optString("url").startsWith("https://")) return im.optString("url");
            }
        }
        return "";
    }

    private static String epicUrl(JSONObject e) {
        String slug = slugFrom(e.optJSONObject("catalogNs") == null ? null : e.optJSONObject("catalogNs").optJSONArray("mappings"));
        if (slug == null) slug = slugFrom(e.optJSONArray("offerMappings"));
        if (slug == null) {
            String p = e.optString("productSlug", "");
            if (!p.isEmpty() && !"null".equals(p)) slug = p.replace("/home", "");
        }
        return slug == null ? "https://store.epicgames.com/free-games" : "https://store.epicgames.com/p/" + slug;
    }

    private static String slugFrom(JSONArray mappings) {
        if (mappings == null) return null;
        for (int i = 0; i < mappings.length(); i++) {
            JSONObject m = mappings.optJSONObject(i);
            String s = m == null ? "" : m.optString("pageSlug", "");
            if (!s.isEmpty()) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ GamerPower giveaways

    static JSONArray gamerPower(String url) throws Exception {
        JSONArray out = new JSONArray();
        String body = text(url);
        if (body == null || !body.trim().startsWith("[")) return out;
        JSONArray all = new JSONArray(body);
        for (int i = 0; i < all.length() && out.length() < 60; i++) {
            JSONObject g = all.optJSONObject(i);
            if (g == null) continue;
            String link = g.optString("open_giveaway_url", g.optString("gamerpower_url", ""));
            if (!link.startsWith("https://")) continue;
            JSONObject o = new JSONObject();
            o.put("title", g.optString("title"));
            o.put("worth", g.optString("worth"));
            o.put("image", g.optString("image", g.optString("thumbnail")));
            o.put("platforms", g.optString("platforms"));
            o.put("end", g.optString("end_date"));
            o.put("type", g.optString("type"));
            o.put("url", link);
            String desc = g.optString("description");
            o.put("desc", desc.length() > 220 ? desc.substring(0, 220) + "…" : desc);
            out.put(o);
        }
        return out;
    }

    // ------------------------------------------------------------------ RSS news

    static List<JSONObject> rss(String id, String name, String url) {
        List<JSONObject> items = new ArrayList<>();
        HttpURLConnection c = null;
        try {
            c = open(url);
            if (c.getResponseCode() != 200) return items;
            InputStream in = c.getInputStream();
            XmlPullParser p = Xml.newPullParser();
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            p.setInput(in, null);
            JSONObject cur = null;
            String tag = null;
            StringBuilder desc = new StringBuilder();
            int ev = p.getEventType();
            while (ev != XmlPullParser.END_DOCUMENT && items.size() < 25) {
                if (ev == XmlPullParser.START_TAG) {
                    tag = p.getName();
                    if ("item".equals(tag) || "entry".equals(tag)) {
                        cur = new JSONObject();
                        cur.put("source", name);
                        cur.put("sid", id);
                        desc.setLength(0);
                    } else if (cur != null) {
                        if (("media:content".equals(tag) || "media:thumbnail".equals(tag) || "enclosure".equals(tag)) && !cur.has("image")) {
                            String u = p.getAttributeValue(null, "url");
                            String type = p.getAttributeValue(null, "type");
                            if (u != null && u.startsWith("https://") && (type == null || type.startsWith("image"))) cur.put("image", u);
                        } else if ("link".equals(tag)) {
                            String href = p.getAttributeValue(null, "href");
                            if (href != null && href.startsWith("https://")) cur.put("url", href);
                        }
                    }
                } else if (ev == XmlPullParser.TEXT) {
                    if (cur != null && tag != null) {
                        String v = p.getText();
                        if (v != null) {
                            switch (tag) {
                                case "title": cur.put("title", cur.optString("title") + v); break;
                                case "link": if (v.trim().startsWith("https://")) cur.put("url", v.trim()); break;
                                case "pubDate": case "published": case "updated": case "dc:date":
                                    if (!cur.has("time")) cur.put("time", parseDate(v.trim()));
                                    break;
                                case "description": case "content:encoded": case "summary": case "content":
                                    if (desc.length() < 20000) desc.append(v);
                                    break;
                                default: break;
                            }
                        }
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    String end = p.getName();
                    if (cur != null && ("item".equals(end) || "entry".equals(end))) {
                        finishItem(cur, desc.toString(), items);
                        cur = null;
                    }
                    tag = null;
                }
                ev = p.next();
            }
        } catch (Exception ignored) {
            // Truncated or malformed feeds still return the items parsed so far.
        } finally {
            if (c != null) c.disconnect();
        }
        return items;
    }

    private static void finishItem(JSONObject cur, String desc, List<JSONObject> items) {
        try {
            String title = clean(cur.optString("title"));
            String link = cur.optString("url");
            if (title.isEmpty() || !link.startsWith("https://")) return;
            cur.put("title", title);
            if (!cur.has("image")) {
                Matcher m = IMG.matcher(desc);
                if (m.find()) cur.put("image", m.group(1).replace("&amp;", "&"));
            }
            String summary = clean(TAGS.matcher(desc).replaceAll(" "));
            cur.put("desc", summary.length() > 200 ? summary.substring(0, 200) + "…" : summary);
            if (!cur.has("time")) cur.put("time", 0L);
            items.add(cur);
        } catch (Exception ignored) { }
    }

    private static String clean(String s) {
        if (s == null) return "";
        return s.replace("&#8217;", "’").replace("&#8216;", "‘").replace("&#8220;", "“").replace("&#8221;", "”")
            .replace("&#8211;", "–").replace("&#8212;", "—").replace("&#038;", "&").replace("&amp;", "&")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replaceAll("\\s+", " ").trim();
    }

    private static long parseDate(String v) {
        String[] patterns = {"EEE, dd MMM yyyy HH:mm:ss Z", "EEE, d MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssZ"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern, Locale.US).parse(v).getTime();
            } catch (Exception ignored) { }
        }
        return 0L;
    }
}
