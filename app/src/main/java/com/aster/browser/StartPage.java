package com.aster.browser;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Locale;

/** Builds the HTML for ASTER's new-tab page: shield stats, clock, search, speed dial and Arcade. */
final class StartPage {
    static final String BASE_URL = "https://aster.start/";

    // ----------------------------------------------------------------- search engines
    static final String[] ENGINE_IDS = {"brave", "duckduckgo", "google", "bing", "startpage", "ecosia", "qwant"};
    static final String[] ENGINE_NAMES = {"Brave Search", "DuckDuckGo", "Google", "Bing", "Startpage", "Ecosia", "Qwant"};
    private static final String[] ENGINE_SEARCH = {
        "https://search.brave.com/search?q=%s",
        "https://duckduckgo.com/?q=%s",
        "https://www.google.com/search?q=%s",
        "https://www.bing.com/search?q=%s",
        "https://www.startpage.com/do/search?q=%s",
        "https://www.ecosia.org/search?q=%s",
        "https://www.qwant.com/?q=%s"
    };
    private static final String[] ENGINE_SUGGEST = {
        "https://search.brave.com/api/suggest?q=%s",
        "https://duckduckgo.com/ac/?type=list&q=%s",
        "https://suggestqueries.google.com/complete/search?client=firefox&q=%s",
        "https://api.bing.com/osjson.aspx?query=%s",
        "https://duckduckgo.com/ac/?type=list&q=%s",
        "https://ac.ecosia.org/autocomplete?type=list&q=%s",
        "https://duckduckgo.com/ac/?type=list&q=%s"
    };

    static int engineIndex(String id) {
        for (int i = 0; i < ENGINE_IDS.length; i++) if (ENGINE_IDS[i].equals(id)) return i;
        return 0;
    }

    static String searchUrl(String engineId, String query) {
        return ENGINE_SEARCH[engineIndex(engineId)].replace("%s", enc(query));
    }

    static String suggestUrl(String engineId, String query) {
        return ENGINE_SUGGEST[engineIndex(engineId)].replace("%s", enc(query));
    }

    static String engineName(String engineId) { return ENGINE_NAMES[engineIndex(engineId)]; }

    static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; }
    }

    static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    static String formatBytes(long bytes) {
        if (bytes < 1024L * 1024L) return Math.max(0, bytes / 1024L) + " KB";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1024) return String.format(Locale.ROOT, mb < 10 ? "%.1f MB" : "%.0f MB", mb);
        return String.format(Locale.ROOT, "%.2f GB", mb / 1024.0);
    }

    static String formatTime(long ms) {
        long s = ms / 1000L;
        if (s < 60) return s + " s";
        if (s < 3600) return (s / 60) + " min";
        if (s < 86400) return String.format(Locale.ROOT, "%.1f h", s / 3600.0);
        return String.format(Locale.ROOT, "%.1f d", s / 86400.0);
    }

    static String formatCount(long n) {
        if (n < 10000) return String.valueOf(n);
        if (n < 1000000) return String.format(Locale.ROOT, "%.1fK", n / 1000.0);
        return String.format(Locale.ROOT, "%.1fM", n / 1000000.0);
    }

    // ----------------------------------------------------------------- page

    static String build(Theme t, SharedPreferences prefs, Store store, ShieldEngine shields, boolean incognito, String gxCornerJson) {
        String engine = prefs.getString("search_engine", "brave");
        String wallpaper = prefs.getString("home_wallpaper", t.defaultWallpaper());
        String accent = Theme.css(t.accent);
        String accent2 = Theme.css(t.accent2);
        String bg = Theme.css(t.bg);
        String surface = Theme.cssA(t.surface, t.glass ? 0.55f : 0.78f);
        String border = Theme.cssA(t.accent, 0.28f);
        String text = Theme.css(t.text);
        String muted = Theme.css(t.muted);

        StringBuilder h = new StringBuilder(16000);
        h.append("<!doctype html><html lang='").append(I18n.es ? "es" : "en").append("'><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,viewport-fit=cover'>")
            .append("<meta name='color-scheme' content='").append(t.light ? "light" : "dark").append("'><title>").append(I18n.t(incognito ? "Private tab" : "New tab")).append("</title><style>")
            .append(":root{--a:").append(accent).append(";--a2:").append(accent2).append(";--bg:").append(bg)
            .append(";--s:").append(surface).append(";--b:").append(border).append(";--t:").append(text).append(";--m:").append(muted).append("}")
            .append("*{box-sizing:border-box;-webkit-tap-highlight-color:transparent}html,body{margin:0;min-height:100%;background:var(--bg);color:var(--t);font-family:system-ui,-apple-system,Roboto,Arial,sans-serif;overscroll-behavior-y:none}")
            .append("#bg{position:fixed;inset:0;z-index:0;overflow:hidden}canvas#fx{position:absolute;inset:0;width:100%;height:100%}")
            .append(".page{position:relative;z-index:1;max-width:760px;margin:0 auto;padding:18px 16px 112px;min-height:100vh;display:flex;flex-direction:column}")
            .append(".stats{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:4px}")
            .append(".stat{background:var(--s);border:1px solid var(--b);border-radius:16px;padding:10px 8px;text-align:center;backdrop-filter:blur(14px);-webkit-backdrop-filter:blur(14px)}")
            .append(".stat b{display:block;font-size:20px;font-weight:800;color:var(--a);letter-spacing:.3px}.stat span{display:block;margin-top:3px;font-size:9.5px;letter-spacing:1px;text-transform:uppercase;color:var(--m)}")
            .append(".clock{margin:30px 0 4px;text-align:center;font-size:56px;font-weight:200;letter-spacing:2px;text-shadow:0 0 30px ").append(Theme.cssA(t.accent, 0.45f)).append("}")
            .append(".date{text-align:center;color:var(--m);font-size:13px;letter-spacing:1px}")
            .append(".brand{display:flex;align-items:center;justify-content:center;gap:10px;margin:34px 0 0}.logo{width:44px;height:44px;border-radius:14px;display:grid;place-items:center;font-weight:900;font-size:22px;color:#fff;background:linear-gradient(140deg,var(--a),var(--a2));box-shadow:0 8px 30px ").append(Theme.cssA(t.accent, 0.5f)).append("}")
            .append(".brand .n{font-weight:900;letter-spacing:5px;font-size:20px}")
            .append("form{margin:24px auto 0;width:100%;max-width:620px;display:flex;align-items:center;gap:6px;background:var(--s);border:1px solid var(--b);border-radius:18px;padding:6px;backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);box-shadow:0 12px 40px rgba(0,0,0,.35)}")
            .append("input{flex:1;min-width:0;height:46px;border:0;outline:0;background:transparent;color:var(--t);font-size:16px;padding:0 12px}input::placeholder{color:var(--m)}")
            .append(".ib{width:44px;height:44px;border:0;border-radius:13px;display:grid;place-items:center;color:#fff;font-size:18px;text-decoration:none;background:linear-gradient(135deg,var(--a),var(--a2))}.ib.ghost{background:transparent;color:var(--m)}")
            .append("h3{margin:30px 2px 12px;font-size:11px;letter-spacing:2px;text-transform:uppercase;color:var(--m);display:flex;justify-content:space-between;align-items:center}h3 a{color:var(--a);text-decoration:none;letter-spacing:1px}")
            .append(".dial{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px 8px}@media(min-width:560px){.dial{grid-template-columns:repeat(6,minmax(0,1fr))}}")
            .append(".tile{display:flex;flex-direction:column;align-items:center;gap:7px;text-decoration:none;color:var(--t);font-size:11.5px;user-select:none;-webkit-user-select:none;-webkit-touch-callout:none}")
            .append(".ic{width:54px;height:54px;border-radius:17px;display:grid;place-items:center;background:var(--s);border:1px solid var(--b);overflow:hidden;position:relative;font-weight:800;font-size:20px;color:var(--a);transition:transform .15s}")
            .append(".tile:active .ic{transform:scale(.9)}.ic img{width:30px;height:30px;border-radius:7px;position:absolute}.lbl{max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}")
            .append(".add .ic{border-style:dashed;color:var(--m);font-weight:300;font-size:28px}")
            .append(".list a{display:flex;gap:10px;align-items:center;padding:11px 13px;margin-bottom:7px;border-radius:13px;background:var(--s);border:1px solid var(--b);color:var(--t);text-decoration:none;font-size:13px;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}")
            .append(".arcade{display:flex;gap:10px;overflow-x:auto;padding-bottom:6px;scroll-snap-type:x mandatory}.arcade::-webkit-scrollbar{display:none}")
            .append(".game{flex:0 0 210px;scroll-snap-align:start;border-radius:16px;overflow:hidden;background:var(--s);border:1px solid var(--b);color:var(--t);text-decoration:none}")
            .append(".game img{width:100%;height:105px;object-fit:cover;display:block;background:#0004}.game div{padding:9px 11px}.game b{display:block;font-size:12.5px;line-height:1.3;max-height:33px;overflow:hidden}.game span{display:block;margin-top:4px;font-size:10.5px;color:var(--a);font-weight:700}")
            .append(".empty{color:var(--m);font-size:12px;padding:6px 2px}")
            .append(".game.more{display:flex;align-items:center;flex-basis:170px}.game.more div{padding:16px}")
            .append(".foot{margin-top:auto;padding-top:36px;display:flex;justify-content:center;gap:10px}.foot a{color:var(--m);text-decoration:none;font-size:11px;letter-spacing:1px;padding:8px 14px;border:1px solid var(--b);border-radius:20px;background:var(--s)}")
            .append(".priv{margin:26px auto 0;max-width:620px;padding:16px 18px;border-radius:18px;background:var(--s);border:1px solid var(--b);font-size:13px;line-height:1.55;color:var(--m)}.priv b{color:var(--t)}")
            .append(wallpaperCss(wallpaper, t))
            .append(t.glass ? glassCss(t) : "")
            .append("</style></head><body><div id='bg'>").append(wallpaperHtml(wallpaper)).append("</div><main class='page'>");

        if (incognito) {
            h.append("<div class='brand'><div class='logo'>&#128065;</div><div class='n'>PRIVATE</div></div>");
            h.append(searchForm(engine, I18n.t("Search privately")));
            h.append("<div class='priv'><b>").append(esc(I18n.t("You are in a private window."))).append("</b><br>")
                .append(esc(I18n.t("ASTER won't save your history, session or site data from this window. Cookies and storage live in a separate, isolated profile and are wiped when you close it. Halo still blocks ads and trackers.")))
                .append("<br><br>").append(esc(I18n.t("Your internet provider, employer or the sites you visit can still see your activity."))).append("</div>");
        } else {
            if (prefs.getBoolean("home_show_stats", true)) {
                h.append("<section class='stats' id='stats'>").append(statsHtml(shields)).append("</section>");
            }
            if (prefs.getBoolean("home_show_clock", true)) {
                h.append("<div class='clock' id='clk'>--:--</div><div class='date' id='dt'></div>");
            } else if (prefs.getBoolean("home_show_branding", true)) {
                h.append("<div class='brand'><div class='logo'>&#10022;</div><div class='n'>ASTER</div></div>");
            }
            h.append(searchForm(engine, I18n.t("Search or type URL")));

            if (prefs.getBoolean("home_show_shortcuts", true)) {
                h.append("<h3><span>").append(esc(I18n.t("Speed dial"))).append("</span><a href='aster://dial/manage'>").append(esc(I18n.t("EDIT"))).append("</a></h3><div class='dial'>");
                for (int i = 0; i < store.speedDial.size(); i++) {
                    Store.Entry e = store.speedDial.get(i);
                    String host = Store.hostOf(e.url);
                    String letter = e.label().isEmpty() ? "?" : e.label().substring(0, 1).toUpperCase(Locale.ROOT);
                    h.append("<a class='tile' href='").append(esc(e.url)).append("' data-i='").append(i).append("'><div class='ic'>")
                        .append(esc(letter))
                        .append("<img src='https://icons.duckduckgo.com/ip3/").append(esc(host)).append(".ico' loading='lazy' onerror='this.remove()'>")
                        .append("</div><div class='lbl'>").append(esc(e.label())).append("</div></a>");
                }
                h.append("<a class='tile add' href='aster://dial/add'><div class='ic'>+</div><div class='lbl'>").append(esc(I18n.t("Add"))).append("</div></a></div>");
            }

            if (prefs.getBoolean("home_show_bookmarks", true) && !store.bookmarks.isEmpty()) {
                h.append("<h3><span>").append(esc(I18n.t("Bookmarks"))).append("</span><a href='aster://bookmarks'>").append(esc(I18n.t("ALL"))).append("</a></h3><div class='list'>");
                int n = 0;
                for (Store.Entry e : store.bookmarks) {
                    if (n++ >= 5) break;
                    h.append("<a href='").append(esc(e.url)).append("'>&#9733; ").append(esc(e.label())).append("</a>");
                }
                h.append("</div>");
            }

            if (prefs.getBoolean("home_show_gx_corner", true)) {
                h.append("<h3><span>Arcade &middot; ").append(esc(I18n.t("Free games"))).append("</span><a href='aster://arcade'>").append(esc(I18n.t("SEE ALL"))).append("</a></h3>");
                h.append("<div id='arcade-box'>").append(gxCorner(gxCornerJson)).append("</div>");
            }
            h.append("<div class='foot'><a href='aster://customize'>&#9881; ").append(esc(I18n.t("CUSTOMIZE"))).append("</a><a href='aster://shields'>&#9960; HALO</a></div>");
        }

        h.append("</main><script>")
            .append("var E=").append(Scripts.quote(ENGINE_SEARCH[engineIndex(engine)])).append(";")
            .append("function go(e){e.preventDefault();var v=document.getElementById('q').value.trim();if(!v)return false;")
            .append("if(/^[a-z][a-z0-9+.-]*:\\/\\//i.test(v)){location.href=v;return false;}")
            .append("if(/^[^\\s]+\\.[a-z]{2,}(\\/\\S*)?$/i.test(v)){location.href='https://'+v;return false;}")
            .append("location.href=E.replace('%s',encodeURIComponent(v));return false;}")
            .append("(function(){var c=document.getElementById('clk'),d=document.getElementById('dt');if(!c)return;")
            .append("var L=").append(I18n.es ? "'es'" : "[]").append(";")
            .append("function u(){var n=new Date();c.textContent=n.toLocaleTimeString(L, {hour:'2-digit',minute:'2-digit'});")
            .append("d.textContent=n.toLocaleDateString(L, {weekday:'long',day:'numeric',month:'long'});}u();setInterval(u,10000);})();")
            .append(wallpaperJs(wallpaper, t))
            .append("</script></body></html>");
        return h.toString();
    }

    /** Liquid Glass surfaces: real backdrop blur, saturation boost, specular rim and top sheen. */
    private static String glassCss(Theme t) {
        String body = t.light ? "linear-gradient(180deg,rgba(255,255,255,.72),rgba(255,255,255,.46))"
            : "linear-gradient(180deg,rgba(255,255,255,.20),rgba(255,255,255,.06))";
        String rim = t.light ? "rgba(255,255,255,.95)" : "rgba(255,255,255,.34)";
        String shadow = t.light ? "0 10px 30px rgba(40,60,110,.16)" : "0 14px 34px rgba(0,0,0,.35)";
        String sel = ".stat,form,.ic,.list a,.game,.foot a,.priv";
        return sel + "{background:" + body + "!important;border:1px solid " + rim + "!important;"
            + "backdrop-filter:blur(26px) saturate(190%);-webkit-backdrop-filter:blur(26px) saturate(190%);"
            + "box-shadow:inset 0 1px 0 rgba(255,255,255," + (t.light ? ".95" : ".45") + "),inset 0 -1px 0 rgba(255,255,255,.10)," + shadow + "!important}"
            + "form{border-radius:26px!important}.ic{border-radius:20px!important}.stat{border-radius:20px!important}"
            + ".ib{box-shadow:inset 0 1px 0 rgba(255,255,255,.5),0 6px 18px " + Theme.cssA(t.accent, 0.35f) + "}"
            + ".clock{text-shadow:0 2px 24px " + (t.light ? "rgba(255,255,255,.9)" : "rgba(0,0,0,.35)") + "}"
            + "#bg{background:" + (t.light
                ? "linear-gradient(135deg,#d6e4ff,#f0e6ff 50%,#d6f5ee)"
                : "linear-gradient(135deg,#081636,#1a0a34 50%,#04242c)") + "}"
            + ".blob{opacity:" + (t.light ? ".55" : ".6") + "!important;filter:blur(50px)!important}";
    }

    private static String searchForm(String engine, String placeholder) {
        return "<form onsubmit='return go(event)'><input id='q' type='search' autocomplete='off' autocapitalize='off' placeholder='"
            + esc(placeholder) + "' aria-label='Search'>"
            + "<a class='ib ghost' href='aster://voice' aria-label='Voice search'>&#127908;</a>"
            + "<button class='ib' type='submit' aria-label='Search'>&#10140;</button></form>";
    }

    /** Inner HTML of the statistics strip (also used to refresh it in place). */
    static String statsHtml(ShieldEngine shields) {
        long blocked = shields.totalBlocked();
        return "<div class='stat'><b>" + formatCount(blocked) + "</b><span>" + esc(I18n.t("Ads & trackers")) + "</span></div>"
            + "<div class='stat'><b>" + formatBytes(blocked * ShieldEngine.BYTES_PER_BLOCK) + "</b><span>" + esc(I18n.t("Data saved")) + "</span></div>"
            + "<div class='stat'><b>" + formatTime(blocked * ShieldEngine.MS_PER_BLOCK) + "</b><span>" + esc(I18n.t("Time saved")) + "</span></div>";
    }

    /** Inner HTML of the Arcade row (also used to refresh it in place). */
    static String arcadeHtml(String json) { return gxCorner(json); }

    private static String gxCorner(String json) {
        if (json == null || json.isEmpty()) return "<div class='empty'>" + esc(I18n.t("Loading free games from GamerPower… tap Refresh if nothing appears.")) + "</div>";
        StringBuilder h = new StringBuilder("<div class='arcade'>");
        int count = 0;
        try {
            JSONArray a = new JSONArray(json);
            for (int i = 0; i < a.length() && count < 12; i++) {
                JSONObject g = a.optJSONObject(i);
                if (g == null) continue;
                String url = g.optString("open_giveaway_url", g.optString("gamerpower_url", ""));
                String img = g.optString("thumbnail", "");
                if (!url.startsWith("https://")) continue;
                String worth = g.optString("worth", "");
                String platform = g.optString("platforms", "");
                if (platform.length() > 28) platform = platform.substring(0, 28) + "…";
                h.append("<a class='game' href='").append(esc(url)).append("'>");
                if (img.startsWith("https://")) h.append("<img loading='lazy' src='").append(esc(img)).append("'>");
                h.append("<div><b>").append(esc(g.optString("title", I18n.t("Free game")))).append("</b><span>")
                    .append("N/A".equals(worth) || worth.isEmpty() ? esc(I18n.t("FREE")) : esc(I18n.f("FREE · was %s", worth)))
                    .append(" &middot; ").append(esc(platform)).append("</span></div></a>");
                count++;
            }
        } catch (Exception ignored) { }
        if (count == 0) return "<div class='empty'>" + esc(I18n.t("No free games right now. Check back later.")) + "</div>";
        h.append("<a class='game more' href='aster://arcade'><div><b>🕹 ").append(esc(I18n.t("Epic, Steam, Xbox & gaming news"))).append("</b><span>")
            .append(esc(I18n.t("Open Arcade"))).append(" →</span></div></a>");
        return h.append("</div>").toString();
    }

    // ----------------------------------------------------------------- wallpapers

    static final String[] WALLPAPER_IDS = {"nebula", "synthwave", "aurora", "matrix", "minimal"};
    static final String[] WALLPAPER_NAMES = {"Nebula (animated stars)", "Synthwave grid (animated)", "Aurora (animated)", "Code rain (animated)", "Minimal gradient"};
    static String wallpaperLabel(int i) { return I18n.t(WALLPAPER_NAMES[i]); }

    private static String wallpaperCss(String w, Theme t) {
        String a = Theme.cssA(t.accent, 0.35f);
        String a2 = Theme.cssA(t.accent2, 0.30f);
        String base = "#bg{background:radial-gradient(ellipse at 50% -10%," + a + ",transparent 60%),radial-gradient(ellipse at 100% 100%," + a2 + ",transparent 55%),var(--bg)}";
        if ("synthwave".equals(w)) {
            return base + ".grid{position:absolute;left:-50%;right:-50%;bottom:-10%;height:60%;"
                + "background-image:linear-gradient(" + Theme.cssA(t.accent, 0.55f) + " 1px,transparent 1px),linear-gradient(90deg," + Theme.cssA(t.accent, 0.55f) + " 1px,transparent 1px);"
                + "background-size:46px 46px;transform:perspective(320px) rotateX(62deg);transform-origin:50% 0;animation:gm 2.4s linear infinite;"
                + "-webkit-mask-image:linear-gradient(to top,#000 30%,transparent);mask-image:linear-gradient(to top,#000 30%,transparent)}"
                + "@keyframes gm{to{background-position:0 46px}}"
                + ".sun{position:absolute;left:50%;bottom:44%;width:220px;height:220px;margin-left:-110px;border-radius:50%;"
                + "background:linear-gradient(" + Theme.css(t.accent) + "," + Theme.css(t.accent2) + ");opacity:.32;filter:blur(1px);"
                + "-webkit-mask-image:repeating-linear-gradient(to bottom,#000 0 14px,transparent 14px 19px);}";
        }
        if ("aurora".equals(w)) {
            return base + ".blob{position:absolute;width:70vmax;height:70vmax;border-radius:50%;filter:blur(60px);opacity:.38;animation:fl 18s ease-in-out infinite alternate}"
                + ".b1{background:" + Theme.css(t.accent) + ";left:-25vmax;top:-30vmax}.b2{background:" + Theme.css(t.accent2) + ";right:-30vmax;top:10vmax;animation-delay:-6s}"
                + ".b3{background:" + Theme.css(Theme.blend(t.accent, 0xFF00E0FF, 0.5f)) + ";left:10vmax;bottom:-40vmax;animation-delay:-11s}"
                + "@keyframes fl{0%{transform:translate(0,0) scale(1)}50%{transform:translate(8vmax,6vmax) scale(1.15)}100%{transform:translate(-6vmax,3vmax) scale(.95)}}";
        }
        return base;
    }

    private static String wallpaperHtml(String w) {
        if ("synthwave".equals(w)) return "<div class='sun'></div><div class='grid'></div>";
        if ("aurora".equals(w)) return "<div class='blob b1'></div><div class='blob b2'></div><div class='blob b3'></div>";
        if ("nebula".equals(w) || "matrix".equals(w)) return "<canvas id='fx'></canvas>";
        return "";
    }

    private static String wallpaperJs(String w, Theme t) {
        String accent = Theme.css(t.accent);
        if ("nebula".equals(w)) {
            return "(function(){var c=document.getElementById('fx');if(!c)return;var x=c.getContext('2d'),W,H,s=[],dpr=Math.min(2,window.devicePixelRatio||1);"
                + "function r(){W=c.width=innerWidth*dpr;H=c.height=innerHeight*dpr;s=[];for(var i=0;i<110;i++)s.push({x:Math.random()*W,y:Math.random()*H,z:Math.random()*0.8+0.2,p:Math.random()*6.28});}"
                + "r();addEventListener('resize',r);var col=" + Scripts.quote(accent) + ";"
                + "function f(t){if(document.hidden){requestAnimationFrame(f);return;}x.clearRect(0,0,W,H);for(var i=0;i<s.length;i++){var p=s[i];p.y+=p.z*0.25*dpr;if(p.y>H)p.y=0;"
                + "var a=0.35+0.65*Math.abs(Math.sin(t/1400+p.p));x.globalAlpha=a*p.z;x.fillStyle=i%5?'#fff':col;x.beginPath();x.arc(p.x,p.y,p.z*1.6*dpr,0,6.28);x.fill();}"
                + "requestAnimationFrame(f);}requestAnimationFrame(f);})();";
        }
        if ("matrix".equals(w)) {
            return "(function(){var c=document.getElementById('fx');if(!c)return;var x=c.getContext('2d'),W,H,cols,drops,fs=14;"
                + "function r(){W=c.width=innerWidth;H=c.height=innerHeight;cols=Math.floor(W/fs);drops=[];for(var i=0;i<cols;i++)drops.push(Math.random()*H/fs);}r();addEventListener('resize',r);"
                + "var col=" + Scripts.quote(accent) + ",ch='01ASTER<>/{}#$%';"
                + "setInterval(function(){if(document.hidden)return;x.fillStyle='rgba(0,0,0,.08)';x.fillRect(0,0,W,H);x.fillStyle=col;x.globalAlpha=.55;x.font=fs+'px monospace';"
                + "for(var i=0;i<cols;i++){x.fillText(ch[Math.floor(Math.random()*ch.length)],i*fs,drops[i]*fs);if(drops[i]*fs>H&&Math.random()>.975)drops[i]=0;drops[i]++;}x.globalAlpha=1;},60);})();";
        }
        return "";
    }
}
