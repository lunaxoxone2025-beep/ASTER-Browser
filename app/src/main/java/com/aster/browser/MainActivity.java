package com.aster.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int MAX_TABS = 8;
    private static final String HOME_PAGE = "aster://start";
    private static final int DOWNLOAD_PERMISSION_REQUEST = 701;

    private final int BG = Color.rgb(15, 17, 23);
    private final int PANEL = Color.rgb(27, 31, 42);
    private final int TEXT = Color.rgb(241, 244, 250);
    private final int MUTED = Color.rgb(151, 161, 180);
    private final int ACCENT = Color.rgb(255, 153, 74);

    private LinearLayout root, toolbar, bottomBar;
    private FrameLayout webContainer;
    private EditText address;
    private TextView tabCount;
    private ProgressBar progressBar;
    private String pendingDownloadUrl, pendingDownloadUserAgent, pendingDownloadMime, pendingDownloadDisposition;
    private final List<WebView> tabs = new ArrayList<>();
    private int active = 0;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("aster", MODE_PRIVATE);
        buildUi();
        if (!restoreSession()) addTab(HOME_PAGE);
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView button(String label, int size) {
        TextView v = new TextView(this);
        v.setText(label);
        v.setTextColor(TEXT);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(10), dp(8), dp(10), dp(8));
        return v;
    }

    private TextView chromeButton(String label, int size, String description) {
        TextView v = button(label, size);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setMinWidth(dp(44));
        v.setMinHeight(dp(44));
        v.setBackground(rounded(PANEL, 16));
        v.setContentDescription(description);
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                    android.view.WindowInsets.Type.systemBars() |
                    android.view.WindowInsets.Type.displayCutout()
                );
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
            root.requestApplyInsets();
        }

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(8), dp(16), dp(8));

        LinearLayout brandGroup = new LinearLayout(this);
        brandGroup.setOrientation(LinearLayout.VERTICAL);
        brandGroup.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = button("ASTER", 18);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setTextColor(ACCENT);
        brand.setPadding(0, 0, 0, 0);
        TextView brandSub = button("BROWSER", 9);
        brandSub.setTextColor(MUTED);
        brandSub.setLetterSpacing(0.18f);
        brandSub.setPadding(0, 0, 0, 0);
        brandGroup.addView(brand);
        brandGroup.addView(brandSub);
        top.addView(brandGroup, new LinearLayout.LayoutParams(0, dp(48), 1));

        tabCount = chromeButton("▢ 1", 14, "Open tab switcher");
        tabCount.setPadding(dp(10), 0, dp(10), 0);
        tabCount.setOnClickListener(v -> showTabs());
        top.addView(tabCount, new LinearLayout.LayoutParams(-2, dp(42)));
        root.addView(top);

        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(4), dp(12), dp(10));

        TextView privacy = chromeButton("◈", 19, "ASTER privacy information");
        privacy.setTextColor(ACCENT);
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(dp(44), dp(48));
        privacyParams.rightMargin = dp(8);
        toolbar.addView(privacy, privacyParams);
        privacy.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("ASTER protection")
            .setMessage("ASTER blocks mixed insecure content and disables third-party cookies. Advanced tracker blocking is planned for a future version.")
            .setPositiveButton("Got it", null).show());

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setTextColor(TEXT);
        address.setHintTextColor(MUTED);
        address.setHint("Search or enter address");
        address.setContentDescription("Search or enter a web address");
        address.setPadding(dp(14), 0, dp(10), 0);
        address.setSelectAllOnFocus(true);
        address.setBackground(rounded(PANEL, 18));
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setMinHeight(dp(48));
        if (Build.VERSION.SDK_INT >= 26) address.setAutofillHints("url");
        toolbar.addView(address, new LinearLayout.LayoutParams(0, dp(48), 1));
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(address.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        TextView reload = chromeButton("↻", 21, "Reload page");
        LinearLayout.LayoutParams reloadParams = new LinearLayout.LayoutParams(dp(44), dp(48));
        reloadParams.leftMargin = dp(8);
        toolbar.addView(reload, reloadParams);
        reload.setOnClickListener(v -> current().reload());
        root.addView(toolbar);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BG));
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, dp(2)));

        webContainer = new FrameLayout(this);
        root.addView(webContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomBar = new LinearLayout(this);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(dp(8), dp(7), dp(8), dp(8));
        bottomBar.setBackgroundColor(Color.rgb(20, 23, 31));
        String[] labels = {"‹", "›", "⌂", "▢", "⋯"};
        String[] descriptions = {"Go back", "Go forward", "Open homepage", "Tabs", "Browser menu"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView b = button(labels[i], 25);
            b.setMinHeight(dp(48));
            b.setBackground(rounded(index == 2 ? Color.rgb(38, 43, 59) : Color.rgb(29, 33, 44), 17));
            b.setContentDescription(descriptions[i]);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, dp(48), 1);
            itemParams.setMargins(dp(4), 0, dp(4), 0);
            bottomBar.addView(b, itemParams);
            b.setOnClickListener(v -> {
                switch (index) {
                    case 0:
                        if (current().canGoBack()) current().goBack();
                        break;
                    case 1:
                        if (current().canGoForward()) current().goForward();
                        break;
                    case 2:
                        current().setTag(null);
                        current().loadUrl(getHomepage());
                        break;
                    case 3:
                        showTabs();
                        break;
                    case 4:
                        showMenu();
                        break;
                }
            });
        }
        root.addView(bottomBar);
    }

    private WebView current() {
        return tabs.get(active);
    }

    private WebView createWebView() {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.WHITE);

        WebSettings settings = w.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setOffscreenPreRaster(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);

        CookieManager.getInstance().setAcceptThirdPartyCookies(w, false);

        w.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            confirmDownload(url, userAgent, contentDisposition, mimeType));

        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int progress) {
                if (view == current()) {
                    progressBar.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
                    progressBar.setProgress(progress);
                    if (progress == 100 && view.getUrl() != null) address.setText(HOME_PAGE.equals(view.getTag()) ? "" : view.getUrl());
                }
            }

            @Override public void onReceivedTitle(WebView view, String title) {
                if (view == current()) tabCount.setContentDescription("Tabs. Current page: " + title);
            }
        });

        w.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (!"https://aster.start/".equals(url)) view.setTag(null);
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "No app can open this link", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                boolean isStartPage = HOME_PAGE.equals(view.getTag()) && "https://aster.start/".equals(url);
                if (view == current()) address.setText(isStartPage ? "" : url);
                if (!isStartPage && isWebUrl(url)) {
                    String old = prefs.getString("history", "");
                    String line = url + "\n";
                    if (!old.contains(line)) prefs.edit().putString("history", line + old).apply();
                }
                saveSession();
            }
        });
        return w;
    }

    private void loadStartPage(WebView target) {
        String engine = prefs.getString("search_engine", "google");
        String searchAction = "https://www.google.com/search";
        if ("duckduckgo".equals(engine)) searchAction = "https://duckduckgo.com/";
        else if ("bing".equals(engine)) searchAction = "https://www.bing.com/search";

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<title>ASTER Home</title><style>");
        html.append("*{box-sizing:border-box}body{margin:0;background:#10131b;color:#f4f6fc;font-family:Arial,sans-serif}");
        html.append(".wrap{max-width:760px;margin:0 auto;padding:30px 20px 42px}.brand{display:flex;align-items:center;gap:10px;color:#ffb875;font-size:13px;font-weight:800;letter-spacing:2.4px}");
        html.append(".orb{width:34px;height:34px;border-radius:12px;background:linear-gradient(145deg,#ffc184,#e77c36);display:grid;place-items:center;color:#111522;font-size:19px;box-shadow:0 6px 24px #7588ff33}");
        html.append(".hero{padding:34px 0 26px}h1{font-size:38px;line-height:1.08;margin:0 0 10px;letter-spacing:-1.5px}.sub{color:#a0a9bd;font-size:14px;line-height:1.5;margin-bottom:24px}");
        html.append(".searchbox{display:flex;gap:8px;background:#202532;border:1px solid #343d50;border-radius:18px;padding:7px;box-shadow:0 12px 35px #0003}");
        html.append("input{min-width:0;flex:1;background:transparent;border:0;outline:0;color:#fff;font-size:16px;padding:11px 12px}");
        html.append("button{border:0;border-radius:13px;padding:0 18px;background:#ffb875;color:#111522;font-weight:800;font-size:14px}");
        html.append(".privacy{display:flex;align-items:center;gap:10px;margin-top:16px;padding:13px 15px;border:1px solid #303849;border-radius:14px;background:#171c27;color:#aeb8cc;font-size:12px}.dot{width:8px;height:8px;border-radius:50%;background:#ffad62;box-shadow:0 0 12px #ffad6288}");
        html.append("h2{font-size:12px;color:#9ea9bf;letter-spacing:1.8px;text-transform:uppercase;margin:30px 0 13px}");
        html.append(".grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}");
        html.append(".card{display:flex;align-items:center;gap:12px;min-height:72px;text-decoration:none;color:#edf0fa;background:#1c222f;border:1px solid #2b3445;border-radius:16px;padding:13px;font-size:14px;transition:background .15s,border-color .15s}");
        html.append(".card:active{background:#293248;border-color:#d88442}.site-icon{flex:0 0 38px;height:38px;border-radius:12px;background:#30394d;color:#ffd0a3;display:grid;place-items:center;font-size:17px;font-weight:800}");
        html.append(".card span{display:block;color:#929db2;font-size:11px;margin-top:4px;overflow-wrap:anywhere}.empty{color:#8792aa;font-size:13px;padding:14px 0}");
        html.append(".foot{color:#68748b;font-size:11px;margin-top:34px;border-top:1px solid #252c39;padding-top:18px}");
        html.append("@media(max-width:420px){.wrap{padding:24px 16px 34px}.hero{padding-top:28px}h1{font-size:34px}.card{padding:11px;gap:9px}.site-icon{flex-basis:34px;height:34px}}");
        html.append("</style></head><body><main class='wrap'>");
        html.append("<div class='brand'><div class='orb'>✦</div> ASTER BROWSER</div>");
        html.append("<section class='hero'><h1>A better way<br>to explore.</h1>");
        html.append("<div class='sub'>A calmer, cleaner space for everything you do online.</div>");
        html.append("<form class='searchbox' method='get' action='").append(searchAction).append("'>");
        html.append("<input name='q' type='search' placeholder='Search or enter a question' autocomplete='off' aria-label='Search the web'>");
        html.append("<button type='submit'>Search</button></form>");
        html.append("<div class='privacy'><i class='dot'></i><div><b>Privacy basics are on</b><br>Third-party cookies are disabled in ASTER.</div></div></section>");
        html.append("<h2>Your shortcuts</h2><div class='grid'>");
        appendStartCard(html, "Google", "Search the web", "https://www.google.com");
        appendStartCard(html, "YouTube", "Watch videos", "https://www.youtube.com");
        appendStartCard(html, "Wikipedia", "Explore knowledge", "https://www.wikipedia.org");
        appendStartCard(html, "GitHub", "Build something", "https://github.com");
        appendStartCard(html, "Reddit", "Join communities", "https://www.reddit.com");
        appendStartCard(html, "DuckDuckGo", "Private search", "https://duckduckgo.com");
        html.append("</div><h2>Saved pages</h2><div class='grid'>");

        List<String> saved = new ArrayList<>();
        for (String line : prefs.getString("bookmarks", "").split("\\n")) {
            if (!line.trim().isEmpty() && isWebUrl(line.trim())) saved.add(line.trim());
            if (saved.size() == 6) break;
        }
        if (saved.isEmpty()) {
            html.append("<div class='empty'>Your saved pages will appear here.</div>");
        } else {
            for (String url : saved) {
                String label = Uri.parse(url).getHost();
                if (label == null || label.isEmpty()) label = url;
                html.append("<a class='card' href='").append(escapeHtml(url)).append("'>")
                    .append(escapeHtml(label)).append("<span>")
                    .append(escapeHtml(url)).append("</span></a>");
            }
        }
        html.append("</div><div class='foot'>ASTER Browser · Built for your everyday web.</div></main></body></html>");
        target.setTag(HOME_PAGE);
        target.loadDataWithBaseURL("https://aster.start/", html.toString(), "text/html", "UTF-8", null);
    }

    private void appendStartCard(StringBuilder html, String title, String subtitle, String url) {
        String icon = title.isEmpty() ? "•" : title.substring(0, 1).toUpperCase();
        html.append("<a class='card' href='").append(url).append("'><b class='site-icon'>")
            .append(icon).append("</b><div>").append(title).append("<span>")
            .append(subtitle).append("</span></div></a>");
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private void addTab(String url) {
        if (tabs.size() >= MAX_TABS) {
            Toast.makeText(this, "ASTER supports up to " + MAX_TABS + " tabs", Toast.LENGTH_SHORT).show();
            return;
        }
        WebView w = createWebView();
        tabs.add(w);
        webContainer.addView(w, new FrameLayout.LayoutParams(-1, -1));
        selectTab(tabs.size() - 1);
        if (HOME_PAGE.equals(url)) loadStartPage(w);
        else w.loadUrl(url);
        saveSession();
    }

    private void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (active >= 0 && active < tabs.size()) tabs.get(active).onPause();
        active = index;
        for (int i = 0; i < tabs.size(); i++) {
            WebView tab = tabs.get(i);
            if (i == active) {
                tab.setVisibility(View.VISIBLE);
                tab.onResume();
            } else {
                tab.setVisibility(View.GONE);
                tab.onPause();
            }
        }
        tabCount.setText(String.valueOf(tabs.size()));
        address.setText(HOME_PAGE.equals(current().getTag()) || current().getUrl() == null ? "" : current().getUrl());
        progressBar.setVisibility(View.GONE);
        saveSession();
    }

    private boolean restoreSession() {
        if (!prefs.getBoolean("restore_tabs", true)) return false;
        try {
            JSONArray saved = new JSONArray(prefs.getString("session_tabs", "[]"));
            int limit = Math.min(saved.length(), MAX_TABS);
            for (int i = 0; i < limit; i++) {
                String url = saved.optString(i, "");
                if (isWebUrl(url) || HOME_PAGE.equals(url)) addTab(url);
            }
            if (!tabs.isEmpty()) {
                selectTab(Math.min(prefs.getInt("active_tab", 0), tabs.size() - 1));
                return true;
            }
        } catch (Exception ignored) {
            prefs.edit().remove("session_tabs").apply();
        }
        return false;
    }

    private void saveSession() {
        if (prefs == null || tabs.isEmpty()) return;
        JSONArray saved = new JSONArray();
        for (int i = 0; i < tabs.size() && i < MAX_TABS; i++) {
            String url = HOME_PAGE.equals(tabs.get(i).getTag()) ? HOME_PAGE : tabs.get(i).getUrl();
            if (url == null || (!isWebUrl(url) && !HOME_PAGE.equals(url))) url = getHomepage();
            saved.put(url);
        }
        prefs.edit()
            .putString("session_tabs", saved.toString())
            .putInt("active_tab", Math.min(active, tabs.size() - 1))
            .apply();
    }

    private boolean isWebUrl(String value) {
        return value != null && (value.regionMatches(true, 0, "https://", 0, 8)
            || value.regionMatches(true, 0, "http://", 0, 7));
    }

    private String getHomepage() {
        return prefs.getString("homepage", "https://www.google.com");
    }

    private String getSearchEngineName() {
        String engine = prefs.getString("search_engine", "google");
        if ("duckduckgo".equals(engine)) return "DuckDuckGo";
        if ("bing".equals(engine)) return "Bing";
        return "Google";
    }

    private String makeSearchUrl(String query) throws Exception {
        String encoded = URLEncoder.encode(query, "UTF-8");
        String engine = prefs.getString("search_engine", "google");
        if ("duckduckgo".equals(engine)) return "https://duckduckgo.com/?q=" + encoded;
        if ("bing".equals(engine)) return "https://www.bing.com/search?q=" + encoded;
        return "https://www.google.com/search?q=" + encoded;
    }

    private void navigate(String input) {
        String value = input.trim();
        if (value.isEmpty()) return;

        current().setTag(null);
        if (isWebUrl(value)) {
            current().loadUrl(value);
        } else if (value.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            Toast.makeText(this, "Only HTTP and HTTPS addresses are supported", Toast.LENGTH_SHORT).show();
        } else if (value.contains(".") && !value.contains(" ")) {
            current().loadUrl("https://" + value);
        } else {
            try {
                current().loadUrl(makeSearchUrl(value));
            } catch (Exception e) {
                current().loadUrl("https://www.google.com");
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(address.getWindowToken(), 0);
        address.clearFocus();
    }

    private void showTabs() {
        String[] names = new String[tabs.size() + 1];
        for (int i = 0; i < tabs.size(); i++) {
            String title = tabs.get(i).getTitle();
            names[i] = (i == active ? "✓ " : "") + (title == null || title.trim().isEmpty() ? "New tab" : title);
        }
        names[tabs.size()] = "+ New tab";
        new AlertDialog.Builder(this)
            .setTitle("ASTER tabs (" + tabs.size() + "/" + MAX_TABS + ")")
            .setItems(names, (d, which) -> {
                if (which == tabs.size()) addTab(HOME_PAGE);
                else selectTab(which);
            })
            .setNegativeButton("Close active tab", (d, w) -> closeTab())
            .show();
    }

    private void closeTab() {
        if (tabs.size() <= 1) {
            current().loadUrl(getHomepage());
            return;
        }
        WebView w = tabs.remove(active);
        webContainer.removeView(w);
        w.stopLoading();
        w.destroy();
        selectTab(Math.max(0, active - 1));
        saveSession();
    }

    private void showMenu() {
        String[] items = {
            "Add bookmark", "Bookmarks", "History", "Find in page",
            "Share page", "Downloads", "Clear browsing data", "Settings", "New tab"
        };
        new AlertDialog.Builder(this).setTitle("ASTER menu").setItems(items, (d, which) -> {
            switch (which) {
                case 0: addBookmark(); break;
                case 1: showSavedList("Bookmarks", "bookmarks"); break;
                case 2: showSavedList("History", "history"); break;
                case 3: findInPage(); break;
                case 4: sharePage(); break;
                case 5:
                    try {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    } catch (Exception e) {
                        Toast.makeText(this, "Downloads are managed by Android", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 6: confirmClearBrowsingData(); break;
                case 7: showSettings(); break;
                case 8: addTab(HOME_PAGE); break;
            }
        }).show();
    }

    private void addBookmark() {
        if (HOME_PAGE.equals(current().getTag())) return;
        String url = current().getUrl();
        if (!isWebUrl(url)) return;
        Set<String> entries = new LinkedHashSet<>();
        String old = prefs.getString("bookmarks", "");
        for (String line : old.split("\n")) if (!line.trim().isEmpty()) entries.add(line.trim());
        if (entries.contains(url)) {
            Toast.makeText(this, "Already bookmarked", Toast.LENGTH_SHORT).show();
            return;
        }
        entries.add(url);
        StringBuilder result = new StringBuilder();
        for (String item : entries) result.append(item).append('\n');
        prefs.edit().putString("bookmarks", result.toString()).apply();
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show();
    }

    private void showSettings() {
        String homepage = getHomepage();
        String[] items = {
            "Search engine: " + getSearchEngineName(),
            "Homepage: " + homepage,
            "Restore tabs on startup: " + (prefs.getBoolean("restore_tabs", true) ? "On" : "Off")
        };
        new AlertDialog.Builder(this).setTitle("Browser settings").setItems(items, (d, which) -> {
            if (which == 0) chooseSearchEngine();
            else if (which == 1) editHomepage();
            else chooseRestoreTabs();
        }).show();
    }

    private void chooseSearchEngine() {
        String[] names = {"Google", "DuckDuckGo", "Bing"};
        String[] keys = {"google", "duckduckgo", "bing"};
        String selected = prefs.getString("search_engine", "google");
        int checked = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(selected)) checked = i;
        new AlertDialog.Builder(this).setTitle("Default search engine")
            .setSingleChoiceItems(names, checked, (dialog, which) -> {
                prefs.edit().putString("search_engine", keys[which]).apply();
                dialog.dismiss();
                Toast.makeText(this, names[which] + " selected", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Cancel", null).show();
    }

    private void editHomepage() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://example.com");
        input.setText(getHomepage());
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this).setTitle("Set homepage").setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String value = input.getText().toString().trim();
                if (value.isEmpty() || value.contains(" ")) {
                    Toast.makeText(this, "Enter a valid web address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!value.regionMatches(true, 0, "https://", 0, 8)
                    && !value.regionMatches(true, 0, "http://", 0, 7)) value = "https://" + value;
                prefs.edit().putString("homepage", value).apply();
                Toast.makeText(this, "Homepage updated", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void chooseRestoreTabs() {
        boolean enabled = prefs.getBoolean("restore_tabs", true);
        String[] options = {"On", "Off"};
        new AlertDialog.Builder(this).setTitle("Restore tabs on startup")
            .setSingleChoiceItems(options, enabled ? 0 : 1, (dialog, which) -> {
                prefs.edit().putBoolean("restore_tabs", which == 0).apply();
                dialog.dismiss();
            }).setNegativeButton("Cancel", null).show();
    }

    private void confirmClearBrowsingData() {
        new AlertDialog.Builder(this).setTitle("Clear browsing data?")
            .setMessage("This removes saved history, cookies, cached files and website data. Your bookmarks will remain.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear", (d, w) -> clearBrowsingData()).show();
    }

    private void clearBrowsingData() {
        prefs.edit().remove("history").apply();
        CookieManager cookies = CookieManager.getInstance();
        cookies.removeAllCookies(null);
        cookies.flush();
        WebStorage.getInstance().deleteAllData();
        for (WebView tab : tabs) {
            tab.clearCache(true);
            tab.clearHistory();
            tab.clearFormData();
        }
        Toast.makeText(this, "Browsing data cleared", Toast.LENGTH_SHORT).show();
    }

    private void confirmDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        pendingDownloadUrl = url;
        pendingDownloadUserAgent = userAgent;
        pendingDownloadDisposition = contentDisposition;
        pendingDownloadMime = mimeType;
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        new AlertDialog.Builder(this)
            .setTitle("Download file?")
            .setMessage(fileName + "\n\nSave to your Downloads folder?")
            .setNegativeButton("Cancel", (d, w) -> clearPendingDownload())
            .setPositiveButton("Download", (d, w) -> {
                if (Build.VERSION.SDK_INT <= 28 &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, DOWNLOAD_PERMISSION_REQUEST);
                } else startPendingDownload();
            }).show();
    }

    private void startPendingDownload() {
        if (pendingDownloadUrl == null) return;
        try {
            String fileName = URLUtil.guessFileName(pendingDownloadUrl, pendingDownloadDisposition, pendingDownloadMime);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(pendingDownloadUrl));
            request.setTitle(fileName);
            request.setDescription("Downloading with ASTER Browser");
            if (pendingDownloadMime != null && !pendingDownloadMime.trim().isEmpty()) {
                request.setMimeType(pendingDownloadMime);
            }
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            if (pendingDownloadUserAgent != null) request.addRequestHeader("User-Agent", pendingDownloadUserAgent);
            String cookies = CookieManager.getInstance().getCookie(pendingDownloadUrl);
            if (cookies != null) request.addRequestHeader("Cookie", cookies);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("Download service unavailable");
            manager.enqueue(request);
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not start download: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        clearPendingDownload();
    }

    private void clearPendingDownload() {
        pendingDownloadUrl = null;
        pendingDownloadUserAgent = null;
        pendingDownloadDisposition = null;
        pendingDownloadMime = null;
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == DOWNLOAD_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPendingDownload();
            } else {
                Toast.makeText(this, "Storage permission is required for this Android version", Toast.LENGTH_LONG).show();
                clearPendingDownload();
            }
        }
    }

    private void findInPage() {
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Text to find");
        new AlertDialog.Builder(this).setTitle("Find in page").setView(query)
            .setNegativeButton("Close", (d, w) -> current().clearMatches())
            .setNeutralButton("Clear", (d, w) -> current().clearMatches())
            .setPositiveButton("Find", (d, w) -> {
                String value = query.getText().toString().trim();
                if (!value.isEmpty()) current().findAllAsync(value);
            }).show();
    }

    private void sharePage() {
        if (HOME_PAGE.equals(current().getTag())) return;
        String url = current().getUrl();
        if (!isWebUrl(url)) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(share, "Share page"));
    }

    private void showSavedList(String heading, String key) {
        String raw = prefs.getString(key, "");
        List<String> entries = new ArrayList<>();
        for (String line : raw.split("\n")) if (!line.trim().isEmpty()) entries.add(line.trim());
        if (entries.isEmpty()) {
            new AlertDialog.Builder(this).setTitle(heading).setMessage("Nothing saved yet.")
                .setPositiveButton("Close", null).show();
            return;
        }
        String[] values = entries.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle(heading).setItems(values, (d, which) -> {
            String selected = values[which];
            if ("bookmarks".equals(key)) {
                String[] actions = {"Open", "Remove bookmark"};
                new AlertDialog.Builder(this).setTitle(selected).setItems(actions, (dialog, action) -> {
                    if (action == 0) current().loadUrl(selected);
                    else {
                        entries.remove(selected);
                        StringBuilder updated = new StringBuilder();
                        for (String item : entries) updated.append(item).append('\n');
                        prefs.edit().putString("bookmarks", updated.toString()).apply();
                        Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
                    }
                }).show();
            } else {
                current().loadUrl(selected);
            }
        }).setPositiveButton("Close", null).show();
    }

    @Override protected void onPause() {
        saveSession();
        for (WebView tab : tabs) tab.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        for (int i = 0; i < tabs.size(); i++) {
            if (i == active) tabs.get(i).onResume();
            else tabs.get(i).onPause();
        }
    }

    @Override public void onBackPressed() {
        if (current().canGoBack()) current().goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        saveSession();
        for (WebView w : tabs) {
            w.stopLoading();
            w.destroy();
        }
        super.onDestroy();
    }
}