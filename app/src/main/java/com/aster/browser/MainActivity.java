package com.aster.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    private static final int MAX_TABS = 8;
    private static final String HOME_PAGE = "aster://start";
    private static final int DOWNLOAD_PERMISSION_REQUEST = 701;
    private static final int BACKUP_EXPORT_REQUEST = 8101;
    private static final int BACKUP_IMPORT_REQUEST = 8102;
    private static final int MAX_BACKUP_BYTES = 10 * 1024 * 1024;
    private static final String RELEASES_API = "https://api.github.com/repos/lunaxoxone2025-beep/ASTER-Browser/releases/latest";
    private static final String UPDATE_APK_NAME = "ASTER-Browser.apk";
    private static final long UPDATE_CHECK_INTERVAL = 24L * 60L * 60L * 1000L;
    private static final long UPDATE_CHECK_THROTTLE = 6L * 60L * 60L * 1000L;

    // Lightweight built-in domains, not a complete EasyList/uBlock filter engine.
    private static final String[] SHIELD_BLOCKED_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "adsrvr.org", "adform.net",
        "taboola.com", "outbrain.com", "criteo.com", "rubiconproject.com",
        "pubmatic.com", "openx.net", "casalemedia.com", "quantserve.com",
        "scorecardresearch.com", "google-analytics.com", "analytics.google.com",
        "connect.facebook.net", "pixel.facebook.com", "analytics.tiktok.com",
        "bat.bing.com", "clarity.ms", "hotjar.com", "segment.io",
        "cdn.segment.com", "api.segment.io", "mixpanel.com", "amplitude.com"
    };

    private final int BG = Color.rgb(13, 11, 18);
    private final int PANEL = Color.rgb(30, 24, 39);
    private final int TEXT = Color.rgb(241, 244, 250);
    private final int MUTED = Color.rgb(174, 163, 190);
    private final int ACCENT = Color.rgb(194, 112, 255);

    private LinearLayout root, toolbar, bottomBar;
    private FrameLayout webContainer;
    private EditText address;
    private TextView tabCount;
    private ProgressBar progressBar;
    private String pendingDownloadUrl, pendingDownloadUserAgent, pendingDownloadMime, pendingDownloadDisposition;
    private final List<WebView> tabs = new ArrayList<>();
    private int active = 0;
    private SharedPreferences prefs;
    private AdBlockEngine adBlockEngine;
    private final Map<WebView, String> topLevelHosts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebView, AtomicInteger> pageBlockedCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> blockedDomainCounts = new ConcurrentHashMap<>();
    private final AtomicInteger totalBlockedRequests = new AtomicInteger(0);
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private long updateDownloadId = -1L;
    private boolean updateReceiverRegistered = false;
    private final Runnable periodicUpdateCheck = new Runnable() {
        @Override public void run() {
            checkForUpdates(false);
            updateHandler.postDelayed(this, UPDATE_CHECK_INTERVAL);
        }
    };
    private final BroadcastReceiver updateDownloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            long expectedId = prefs.getLong("update_download_id", -1L);
            if (completedId != expectedId || expectedId < 0) return;
            openDownloadedUpdate(expectedId);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("aster", MODE_PRIVATE);
        adBlockEngine = new AdBlockEngine(this);
        buildUi();
        if (!restoreSession()) addTab(HOME_PAGE);
        registerUpdateReceiver();
        checkForUpdates(false);
        if (adBlockEngine.needsUpdate()) adBlockEngine.updateAsync(null);
    }

    private void registerUpdateReceiver() {
        if (updateReceiverRegistered) return;
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(updateDownloadReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(updateDownloadReceiver, filter);
        }
        updateReceiverRegistered = true;
    }

    private void checkForUpdates(boolean force) {
        long now = System.currentTimeMillis();
        long lastCheck = prefs.getLong("update_last_check", 0L);
        if (!force && now - lastCheck < UPDATE_CHECK_THROTTLE) return;
        prefs.edit().putLong("update_last_check", now).apply();

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(RELEASES_API).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "ASTER-Browser-Android");
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return;
                String json = new String(readLimited(connection.getInputStream(), 2 * 1024 * 1024), "UTF-8");
                JSONObject release = new JSONObject(json);
                String tag = release.optString("tag_name", "").trim();
                JSONArray assets = release.optJSONArray("assets");
                String apkUrl = null;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset != null && UPDATE_APK_NAME.equals(asset.optString("name"))) {
                            apkUrl = asset.optString("browser_download_url", null);
                            break;
                        }
                    }
                }
                if (apkUrl == null || !apkUrl.startsWith("https://")) return;
                if (compareVersions(tag, "v" + BuildConfig.VERSION_NAME) <= 0) return;
                String finalApkUrl = apkUrl;
                runOnUiThread(() -> showUpdateAvailable(tag, finalApkUrl));
            } catch (Exception ignored) {
                // Update checks are best-effort and must never interrupt browsing.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "ASTER-update-check").start();
    }

    private int compareVersions(String remote, String local) {
        String[] a = remote.replaceFirst("^[vV]", "").split("\\.");
        String[] b = local.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int av = i < a.length ? parseVersionPart(a[i]) : 0;
            int bv = i < b.length ? parseVersionPart(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        String digits = part.replaceAll("[^0-9].*$", "");
        try { return Integer.parseInt(digits); }
        catch (Exception ignored) { return 0; }
    }

    private void showUpdateAvailable(String version, String apkUrl) {
        if (isFinishing()) return;
        new AlertDialog.Builder(this)
            .setTitle("ASTER " + version + " is available")
            .setMessage("A newer version of ASTER Browser is ready. Download it now? Android will ask you to confirm the installation.")
            .setNegativeButton("Later", null)
            .setPositiveButton("Download update", (dialog, which) -> downloadUpdate(apkUrl))
            .show();
    }

    private void downloadUpdate(String apkUrl) {
        try {
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("Download service unavailable");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("ASTER Browser update");
            request.setDescription("Downloading the latest ASTER version");
            request.setMimeType("application/vnd.android.package-archive");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, UPDATE_APK_NAME);
            long id = manager.enqueue(request);
            prefs.edit().putLong("update_download_id", id).apply();
            Toast.makeText(this, "Downloading ASTER update…", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not download update: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openDownloadedUpdate(long downloadId) {
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager == null) return;
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (android.database.Cursor cursor = manager.query(query)) {
            if (cursor == null || !cursor.moveToFirst()) return;
            int statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            if (statusColumn < 0 || cursor.getInt(statusColumn) != DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(this, "ASTER update download failed", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Uri apkUri = manager.getUriForDownloadedFile(downloadId);
        if (apkUri == null) {
            Toast.makeText(this, "Could not open downloaded update", Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                .setTitle("Allow ASTER updates")
                .setMessage("Enable permission to install apps from ASTER Browser, then return to install the update.")
                .setNegativeButton("Later", null)
                .setPositiveButton("Open settings", (d, w) -> {
                    Intent settings = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                    startActivity(settings);
                }).show();
            return;
        }
        try {
            prefs.edit().putLong("update_download_id", -1L).apply();
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception e) {
            Toast.makeText(this, "Android could not open the installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        // Compact browser chrome: the web page gets the visual focus.
        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(9), dp(10), dp(8));
        toolbar.setBackgroundColor(BG);

        TextView privacy = chromeButton("⛨", 18, "ASTER privacy information");
        privacy.setTextColor(ACCENT);
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(dp(42), dp(46));
        privacyParams.rightMargin = dp(7);
        toolbar.addView(privacy, privacyParams);
        privacy.setOnClickListener(v -> showPrivacyPanel());

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
        address.setMinHeight(dp(46));
        if (Build.VERSION.SDK_INT >= 26) address.setAutofillHints("url");
        toolbar.addView(address, new LinearLayout.LayoutParams(0, dp(46), 1));
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(address.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        tabCount = chromeButton("1", 13, "Open tabs");
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(dp(42), dp(46));
        tabsParams.leftMargin = dp(7);
        toolbar.addView(tabCount, tabsParams);
        tabCount.setOnClickListener(v -> showTabs());

        TextView reload = chromeButton("↻", 20, "Reload page");
        LinearLayout.LayoutParams reloadParams = new LinearLayout.LayoutParams(dp(42), dp(46));
        reloadParams.leftMargin = dp(7);
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
        bottomBar.setPadding(dp(8), dp(6), dp(8), dp(7));
        bottomBar.setBackgroundColor(Color.rgb(17, 13, 23));
        String[] labels = {"‹", "›", "⌂", "＋", "⋯"};
        String[] descriptions = {"Go back", "Go forward", "Home", "New tab", "Browser menu"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView item = button(labels[i], 24);
            item.setMinHeight(dp(46));
            item.setBackground(rounded(index == 2 ? Color.rgb(43, 47, 59) : Color.rgb(27, 31, 40), 16));
            item.setContentDescription(descriptions[i]);
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, dp(46), 1);
            itemParams.setMargins(dp(4), 0, dp(4), 0);
            bottomBar.addView(item, itemParams);
            item.setOnClickListener(v -> {
                switch (index) {
                    case 0:
                        if (current().canGoBack()) current().goBack();
                        break;
                    case 1:
                        if (current().canGoForward()) current().goForward();
                        break;
                    case 2:
                        current().setTag(null);
                        loadStartPage(current());
                        break;
                    case 3:
                        addTab(HOME_PAGE);
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
                String host = normalizedHost(Uri.parse(url).getHost());
                if (host == null) topLevelHosts.remove(view);
                else topLevelHosts.put(view, host);
                pageBlockedCounts.put(view, new AtomicInteger(0));
            }

            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (shouldBlockRequest(view, request.getUrl(), request.isForMainFrame())) {
                    return blockedWebResponse();
                }
                return super.shouldInterceptRequest(view, request);
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
                String host = normalizedHost(Uri.parse(url).getHost());
                if (host != null) topLevelHosts.put(view, host);
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

    private boolean shouldBlockRequest(WebView view, Uri uri, boolean isMainFrame) {
        if (isMainFrame || !prefs.getBoolean("shield_enabled", true) || uri == null) return false;
        String host = normalizedHost(uri.getHost());
        if (host == null) return false;

        String firstPartyHost = topLevelHosts.get(view);
        if (isSiteException(firstPartyHost)) return false;
        if (adBlockEngine == null || !adBlockEngine.shouldBlock(host)) return false;

        totalBlockedRequests.incrementAndGet();
        incrementCounter(pageBlockedCounts, view);
        incrementCounter(blockedDomainCounts, host);
        return true;
    }

    private <K> void incrementCounter(ConcurrentHashMap<K, AtomicInteger> counters, K key) {
        AtomicInteger counter = counters.get(key);
        if (counter == null) {
            AtomicInteger created = new AtomicInteger(0);
            AtomicInteger existing = counters.putIfAbsent(key, created);
            counter = existing == null ? created : existing;
        }
        counter.incrementAndGet();
    }

    private WebResourceResponse blockedWebResponse() {
        return new WebResourceResponse(
            "text/plain", "UTF-8", new ByteArrayInputStream(new byte[0])
        );
    }

    private String normalizedHost(String host) {
        if (host == null) return null;
        String value = host.trim().toLowerCase(Locale.ROOT);
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value.isEmpty() ? null : value;
    }

    private boolean isShieldBlockedHost(String host) {
        for (String domain : SHIELD_BLOCKED_HOSTS) {
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        }
        return false;
    }

    private boolean isSiteException(String host) {
        if (host == null) return false;
        String exceptions = prefs.getString("shield_site_exceptions", "");
        for (String item : exceptions.split("\\n")) {
            String allowed = normalizedHost(item);
            if (host.equals(allowed)) return true;
        }
        return false;
    }

    private String currentSiteHost() {
        if (tabs.isEmpty()) return null;
        String url = current().getUrl();
        String host = normalizedHost(url == null ? null : Uri.parse(url).getHost());
        return host != null ? host : topLevelHosts.get(current());
    }

    private void toggleCurrentSiteException() {
        String host = currentSiteHost();
        if (host == null) {
            Toast.makeText(this, "Open a website first", Toast.LENGTH_SHORT).show();
            return;
        }
        Set<String> exceptions = new LinkedHashSet<>();
        String old = prefs.getString("shield_site_exceptions", "");
        for (String line : old.split("\\n")) {
            String value = normalizedHost(line);
            if (value != null) exceptions.add(value);
        }
        boolean wasAllowed = exceptions.remove(host);
        if (!wasAllowed) exceptions.add(host);
        StringBuilder updated = new StringBuilder();
        for (String value : exceptions) updated.append(value).append('\n');
        prefs.edit().putString("shield_site_exceptions", updated.toString()).apply();
        Toast.makeText(this,
            wasAllowed ? "Protection restored for " + host : "Protection paused for " + host,
            Toast.LENGTH_LONG).show();
        current().reload();
    }

    private TextView panelText(String value, int size, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    private void addPanelAction(LinearLayout panel, String label, Runnable action) {
        TextView button = chromeButton(label, 14, label);
        button.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.topMargin = dp(9);
        panel.addView(button, params);
        button.setOnClickListener(v -> action.run());
    }

    private void showPrivacyPanel() {
        if (tabs.isEmpty()) return;
        boolean enabled = prefs.getBoolean("shield_enabled", true);
        String host = currentSiteHost();
        boolean excepted = isSiteException(host);
        AtomicInteger pageCount = pageBlockedCounts.get(current());
        int pageTotal = pageCount == null ? 0 : pageCount.get();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(10), dp(18), dp(14));
        panel.setBackgroundColor(BG);

        TextView heading = panelText("ASTER SHIELD", 19, TEXT);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(heading);

        TextView subtitle = panelText("Ad and tracker blocking powered by downloadable filter lists", 13, MUTED);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        panel.addView(subtitle, subtitleParams);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(14), dp(13), dp(14), dp(13));
        statusCard.setBackground(rounded(PANEL, 18));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(16);
        panel.addView(statusCard, cardParams);

        TextView state = panelText(enabled ? "●  PROTECTION ACTIVE" : "○  PROTECTION PAUSED", 14, enabled ? ACCENT : MUTED);
        state.setTypeface(Typeface.DEFAULT_BOLD);
        statusCard.addView(state);
        TextView site = panelText("Current site: " + (host == null ? "New tab" : host), 13, TEXT);
        LinearLayout.LayoutParams siteParams = new LinearLayout.LayoutParams(-1, -2);
        siteParams.topMargin = dp(8);
        statusCard.addView(site, siteParams);
        TextView siteState = panelText("This site: " + (excepted ? "Protection paused" : "Protected"), 12, MUTED);
        LinearLayout.LayoutParams siteStateParams = new LinearLayout.LayoutParams(-1, -2);
        siteStateParams.topMargin = dp(3);
        statusCard.addView(siteState, siteStateParams);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(-1, -2);
        statsParams.topMargin = dp(12);
        panel.addView(stats, statsParams);
        addShieldStat(stats, String.valueOf(pageTotal), "THIS PAGE");
        addShieldStat(stats, String.valueOf(totalBlockedRequests.get()), "THIS SESSION");
        addShieldStat(stats, String.valueOf(adBlockEngine == null ? 0 : adBlockEngine.getRuleCount()), "DOMAIN RULES");

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Privacy & protection")
            .setView(panel)
            .setNegativeButton("Close", null)
            .create();

        addPanelAction(panel, enabled ? "Pause protection everywhere" : "Enable protection everywhere", () -> {
            prefs.edit().putBoolean("shield_enabled", !prefs.getBoolean("shield_enabled", true)).apply();
            dialog.dismiss();
            current().reload();
            Toast.makeText(this, prefs.getBoolean("shield_enabled", true) ? "ASTER Shield enabled" : "ASTER Shield disabled", Toast.LENGTH_SHORT).show();
        });
        if (host != null) {
            addPanelAction(panel, excepted ? "Restore protection on this site" : "Pause protection on this site", () -> {
                dialog.dismiss();
                toggleCurrentSiteException();
            });
        }
        addPanelAction(panel, "Update ad & tracker filter lists", () -> {
            Toast.makeText(this, "Updating EasyList and EasyPrivacy…", Toast.LENGTH_LONG).show();
            adBlockEngine.updateAsync((success, downloaded, total) -> runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, "Filters updated: " + downloaded + " network rules", Toast.LENGTH_LONG).show();
                    if (!isFinishing()) current().reload();
                } else Toast.makeText(this, "Could not update filters. Existing rules are kept.", Toast.LENGTH_LONG).show();
            }));
        });
        addPanelAction(panel, "View blocked domains", () -> {
            dialog.dismiss();
            showBlockedDomains();
        });

        TextView note = panelText("Uses EasyList and EasyPrivacy domain network rules. WebView cannot apply cosmetic filters or every advanced rule, so some in-page ad spaces may remain.", 11, MUTED);
        note.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(13);
        panel.addView(note, noteParams);
        dialog.show();
    }

    private void addShieldStat(LinearLayout parent, String value, String label) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(rounded(PANEL, 16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.rightMargin = dp(5);
        params.leftMargin = dp(5);
        parent.addView(card, params);
        TextView number = panelText(value, 22, ACCENT);
        number.setTypeface(Typeface.DEFAULT_BOLD);
        number.setGravity(Gravity.CENTER);
        card.addView(number);
        TextView caption = panelText(label, 10, MUTED);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(-1, -2);
        captionParams.topMargin = dp(3);
        card.addView(caption, captionParams);
    }

    private void showBlockedDomains() {
        if (blockedDomainCounts.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Blocked domains")
                .setMessage("No domains have been blocked in this session yet.")
                .setPositiveButton("OK", null).show();
            return;
        }
        List<String> domains = new ArrayList<>(blockedDomainCounts.keySet());
        Collections.sort(domains);
        StringBuilder message = new StringBuilder();
        int shown = 0;
        for (String domain : domains) {
            if (shown++ >= 30) break;
            AtomicInteger count = blockedDomainCounts.get(domain);
            message.append(domain).append("  ·  ")
                .append(count == null ? 0 : count.get()).append('\n');
        }
        if (domains.size() > 30) message.append("\nAnd ").append(domains.size() - 30).append(" more...");
        new AlertDialog.Builder(this).setTitle("Blocked domains (" + totalBlockedRequests.get() + ")")
            .setMessage(message.toString()).setPositiveButton("OK", null).show();
    }

    private void loadStartPage(WebView target) {
        String engine = prefs.getString("search_engine", "google");
        String searchAction = "https://www.google.com/search";
        if ("duckduckgo".equals(engine)) searchAction = "https://duckduckgo.com/";
        else if ("bing".equals(engine)) searchAction = "https://www.bing.com/search";

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>");
        html.append("<meta name='color-scheme' content='dark'><title>New tab - ASTER</title><style>");
        html.append("*{box-sizing:border-box}html,body{min-height:100%;margin:0}");
        html.append("body{background:radial-gradient(ellipse at 50% 8%,#35204d 0%,#171321 42%,#0c0b11 100%);color:#f5f2fa;font-family:Arial, sans-serif}");
        html.append(".page{min-height:100vh;max-width:720px;margin:auto;padding:clamp(42px,12vh,110px) 22px 34px;display:flex;flex-direction:column;align-items:center}");
        html.append(".logo{width:62px;height:62px;border-radius:21px;display:grid;place-items:center;background:linear-gradient(145deg,#f08cff,#8d4dff 62%,#5431b8);color:#fff;font-size:30px;font-weight:900;box-shadow:0 12px 42px #9b4dff55}");
        html.append(".name{margin-top:16px;font-size:23px;font-weight:900;letter-spacing:4px}.tag{margin-top:7px;color:#b5a9c8;font-size:12px;letter-spacing:.7px}");
        html.append("form{width:100%;max-width:590px;margin-top:36px;display:flex;align-items:center;background:#211a2c;border:1px solid #57416f;border-radius:17px;padding:6px;box-shadow:0 14px 42px #0005}");
        html.append("input{min-width:0;flex:1;height:48px;border:0;outline:0;background:transparent;color:#fff;padding:0 13px;font-size:15px}input::placeholder{color:#929bad}");
        html.append("button{height:44px;padding:0 17px;border:0;border-radius:12px;background:linear-gradient(135deg,#d878ff,#8c5cff);color:#fff;font-size:13px;font-weight:800}");
        html.append(".section{width:100%;max-width:590px;margin-top:38px}.heading{margin:0 0 15px;color:#aab2c1;font-size:11px;font-weight:700;letter-spacing:1.8px;text-transform:uppercase}");
        html.append(".sites{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}");
        html.append(".site{min-height:100px;padding:15px 8px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;text-decoration:none;color:#f3edf9;background:linear-gradient(145deg,#211b2b,#17131f);border:1px solid #392b49;border-radius:17px;transition:background .15s,border-color .15s}");
        html.append(".site:active{background:#382448;border-color:#c77cff}.symbol{width:38px;height:38px;border-radius:13px;display:grid;place-items:center;background:#382747;color:#e8b6ff;font-size:16px;font-weight:800}.label{font-size:12px;font-weight:600;overflow-wrap:anywhere;text-align:center}");
        html.append(".saved{display:flex;flex-direction:column;gap:8px}.saved a{padding:13px 15px;border-radius:13px;background:#202631;border:1px solid #303949;color:#e9edf5;text-decoration:none;font-size:13px;overflow-wrap:anywhere}.empty{color:#7f899b;font-size:12px;padding:4px 2px}");
        html.append(".footer{margin-top:auto;padding-top:45px;color:#657083;font-size:10px;letter-spacing:1px}");
        html.append("@media(max-width:380px){.page{padding-left:15px;padding-right:15px}.sites{gap:8px}.site{min-height:88px}.label{font-size:11px}}");
        html.append("</style></head><body><main class='page'>");
        html.append("<div class='logo'>✦</div><div class='name'>ASTER</div><div class='tag'>A fresh start to the web</div>");
        html.append("<form method='get' action='").append(searchAction).append("'>");
        html.append("<input name='q' type='search' placeholder='Search the web' autocomplete='off' aria-label='Search the web'>");
        html.append("<button type='submit'>Search</button></form>");
        html.append("<section class='section'><div class='heading'>Quick access</div><div class='sites'>");
        appendStartCard(html, "Google", "G", "https://www.google.com");
        appendStartCard(html, "YouTube", "▶", "https://www.youtube.com");
        appendStartCard(html, "GitHub", "⌘", "https://github.com");
        appendStartCard(html, "Wikipedia", "W", "https://www.wikipedia.org");
        appendStartCard(html, "Reddit", "r", "https://www.reddit.com");
        appendStartCard(html, "DuckDuckGo", "D", "https://duckduckgo.com");
        html.append("</div></section>");

        List<String> saved = new ArrayList<>();
        for (String line : prefs.getString("bookmarks", "").split("\\n")) {
            if (!line.trim().isEmpty() && isWebUrl(line.trim())) saved.add(line.trim());
            if (saved.size() == 4) break;
        }
        html.append("<section class='section'><div class='heading'>Bookmarks</div><div class='saved'>");
        if (saved.isEmpty()) {
            html.append("<div class='empty'>Pages you bookmark will appear here.</div>");
        } else {
            for (String url : saved) {
                String label = Uri.parse(url).getHost();
                if (label == null || label.isEmpty()) label = url;
                html.append("<a href='").append(escapeHtml(url)).append("'>")
                    .append(escapeHtml(label)).append("</a>");
            }
        }
        html.append("</div></section><div class='footer'>ASTER BROWSER</div></main></body></html>");
        target.setTag(HOME_PAGE);
        target.loadDataWithBaseURL("https://aster.start/", html.toString(), "text/html", "UTF-8", null);
    }

    private void appendStartCard(StringBuilder html, String title, String symbol, String url) {
        html.append("<a class='site' href='").append(url).append("'><b class='symbol'>")
            .append(symbol).append("</b><span class='label'>").append(title)
            .append("</span></a>");
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
        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(list);

        TextView summary = panelText(tabs.size() + " of " + MAX_TABS + " tabs open", 13, MUTED);
        summary.setPadding(dp(4), dp(5), dp(4), dp(12));
        list.addView(summary);

        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            WebView tab = tabs.get(i);
            String title = tab.getTitle();
            if (title == null || title.trim().isEmpty()) title = "New tab";
            String url = tab.getUrl();
            if (url == null || HOME_PAGE.equals(tab.getTag())) url = "ASTER start page";

            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(8), dp(10));
            row.setBackground(rounded(i == active ? Color.rgb(48, 43, 39) : PANEL, 16));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(8);
            list.addView(row, rowParams);

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            details.setPadding(0, dp(2), dp(8), dp(2));
            row.addView(details, new LinearLayout.LayoutParams(0, -2, 1));

            TextView titleView = panelText((i == active ? "●  " : "") + title, 14, TEXT);
            titleView.setTypeface(i == active ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            details.addView(titleView);
            TextView urlView = panelText(url, 11, MUTED);
            urlView.setMaxLines(1);
            urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(-1, -2);
            urlParams.topMargin = dp(3);
            details.addView(urlView, urlParams);

            TextView close = chromeButton("×", 20, "Close tab");
            close.setTextColor(MUTED);
            close.setBackground(rounded(Color.rgb(38, 42, 52), 14));
            row.addView(close, new LinearLayout.LayoutParams(dp(42), dp(42)));
            row.setOnClickListener(v -> {
                selectTab(index);
                dialogSafeDismissTabs();
            });
            close.setOnClickListener(v -> {
                closeTabAt(index);
                dialogSafeDismissTabs();
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Your tabs")
            .setView(scroll)
            .setNegativeButton("Done", null)
            .setPositiveButton("+ New tab", (d, which) -> addTab(HOME_PAGE))
            .create();
        pendingTabsDialog = dialog;
        dialog.show();
    }

    private AlertDialog pendingTabsDialog;

    private void dialogSafeDismissTabs() {
        if (pendingTabsDialog != null && pendingTabsDialog.isShowing()) pendingTabsDialog.dismiss();
        pendingTabsDialog = null;
    }

    private void closeTab() {
        closeTabAt(active);
    }

    private void closeTabAt(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (tabs.size() == 1) {
            current().loadUrl(getHomepage());
            return;
        }
        WebView closing = tabs.remove(index);
        topLevelHosts.remove(closing);
        pageBlockedCounts.remove(closing);
        webContainer.removeView(closing);
        closing.stopLoading();
        closing.destroy();

        if (index < active) active--;
        else if (index == active) active = Math.min(index, tabs.size() - 1);
        selectTab(active);
        saveSession();
    }

    private void showMenu() {
        String[] items = {
            "Add bookmark", "Bookmarks", "History", "Find in page",
            "Share page", "Downloads", "Clear browsing data", "Settings", "Backup & restore", "New tab"
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
                case 8: showBackupRestore(); break;
                case 9: addTab(HOME_PAGE); break;
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

    private void showBackupRestore() {
        String[] actions = {"Export backup", "Import backup"};
        new AlertDialog.Builder(this).setTitle("Backup & restore")
            .setItems(actions, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "ASTER-backup.json");
                    try {
                        startActivityForResult(intent, BACKUP_EXPORT_REQUEST);
                    } catch (Exception e) {
                        Toast.makeText(this, "Could not open the file picker", Toast.LENGTH_LONG).show();
                    }
                } else {
                    new AlertDialog.Builder(this)
                        .setTitle("Import backup?")
                        .setMessage("Importing replaces your current bookmarks, history, browser settings and open tabs. Export a backup first if you want to keep the current data.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Choose file", (confirmDialog, whichAction) -> {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/json");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                startActivityForResult(intent, BACKUP_IMPORT_REQUEST);
                            } catch (Exception e) {
                                Toast.makeText(this, "Could not open the file picker", Toast.LENGTH_LONG).show();
                            }
                        }).show();
                }
            }).show();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri selectedFile = data.getData();
        if (requestCode == BACKUP_EXPORT_REQUEST) exportBackup(selectedFile);
        else if (requestCode == BACKUP_IMPORT_REQUEST) importBackup(selectedFile);
    }

    private void exportBackup(Uri destination) {
        try {
            JSONObject backup = new JSONObject();
            backup.put("format", "ASTER_BROWSER_BACKUP");
            backup.put("version", 1);
            backup.put("exported_at", System.currentTimeMillis());
            backup.put("bookmarks", prefs.getString("bookmarks", ""));
            backup.put("history", prefs.getString("history", ""));

            JSONArray savedTabs = new JSONArray();
            for (WebView tab : tabs) {
                String url = HOME_PAGE.equals(tab.getTag()) ? HOME_PAGE : tab.getUrl();
                if (url == null || (!HOME_PAGE.equals(url) && !isBackupWebUrl(url))) {
                    url = HOME_PAGE.equals(tab.getTag()) ? HOME_PAGE : getHomepage();
                }
                savedTabs.put(url);
            }
            backup.put("session_tabs", savedTabs);
            backup.put("active_tab", Math.max(0, Math.min(active, Math.max(0, tabs.size() - 1))));

            JSONObject settings = new JSONObject();
            settings.put("homepage", getHomepage());
            settings.put("search_engine", prefs.getString("search_engine", "google"));
            settings.put("restore_tabs", prefs.getBoolean("restore_tabs", true));
            settings.put("shield_enabled", prefs.getBoolean("shield_enabled", true));
            settings.put("shield_site_exceptions", prefs.getString("shield_site_exceptions", ""));
            backup.put("settings", settings);

            try (OutputStream output = getContentResolver().openOutputStream(destination, "w")) {
                if (output == null) throw new IllegalStateException("Could not open the selected file");
                output.write(backup.toString(2).getBytes("UTF-8"));
                output.flush();
            }
            Toast.makeText(this, "ASTER backup exported", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importBackup(Uri source) {
        try {
            byte[] bytes;
            try (InputStream input = getContentResolver().openInputStream(source)) {
                if (input == null) throw new IllegalStateException("Could not read the selected file");
                bytes = readLimited(input, MAX_BACKUP_BYTES);
            }

            String json = new String(bytes, "UTF-8");
            if (json.startsWith("\uFEFF")) json = json.substring(1);
            JSONObject backup = new JSONObject(json);
            if (!"ASTER_BROWSER_BACKUP".equals(backup.optString("format"))
                || backup.optInt("version", -1) != 1) {
                throw new IllegalArgumentException("This is not a supported ASTER backup");
            }

            JSONObject settings = backup.optJSONObject("settings");
            JSONArray sourceTabs = backup.optJSONArray("session_tabs");
            if (settings == null || sourceTabs == null) {
                throw new IllegalArgumentException("The backup is missing required data");
            }

            String bookmarks = sanitizeUrlLines(backup.optString("bookmarks", ""));
            String history = sanitizeUrlLines(backup.optString("history", ""));
            String homepage = settings.optString("homepage", "https://www.google.com").trim();
            if (!isBackupWebUrl(homepage)) homepage = "https://www.google.com";

            String engine = settings.optString("search_engine", "google");
            if (!"google".equals(engine) && !"duckduckgo".equals(engine) && !"bing".equals(engine)) {
                engine = "google";
            }

            List<String> restoredTabs = new ArrayList<>();
            for (int i = 0; i < sourceTabs.length() && restoredTabs.size() < MAX_TABS; i++) {
                String url = sourceTabs.optString(i, "").trim();
                if (HOME_PAGE.equals(url) || isBackupWebUrl(url)) restoredTabs.add(url);
            }
            if (restoredTabs.isEmpty()) restoredTabs.add(HOME_PAGE);

            int restoredActive = Math.max(0, Math.min(
                backup.optInt("active_tab", 0), restoredTabs.size() - 1
            ));
            JSONArray safeTabs = new JSONArray();
            for (String url : restoredTabs) safeTabs.put(url);

            prefs.edit()
                .putString("bookmarks", bookmarks)
                .putString("history", history)
                .putString("homepage", homepage)
                .putString("search_engine", engine)
                .putBoolean("restore_tabs", settings.optBoolean("restore_tabs", true))
                .putBoolean("shield_enabled", settings.optBoolean("shield_enabled", true))
                .putString("shield_site_exceptions", sanitizeHostLines(settings.optString("shield_site_exceptions", "")))
                .putString("session_tabs", safeTabs.toString())
                .putInt("active_tab", restoredActive)
                .apply();

            rebuildTabs(restoredTabs, restoredActive);
            Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not restore backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readLimited(InputStream input, int limit) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > limit) throw new IllegalArgumentException("Backup file is too large");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private String sanitizeHostLines(String raw) {
        Set<String> safe = new LinkedHashSet<>();
        if (raw != null) {
            for (String line : raw.split("\\n")) {
                String host = normalizedHost(line);
                if (host != null && host.matches("[a-z0-9.-]+") && host.contains(".")) safe.add(host);
                if (safe.size() >= 500) break;
            }
        }
        StringBuilder result = new StringBuilder();
        for (String host : safe) result.append(host).append('\n');
        return result.toString();
    }

    private String sanitizeUrlLines(String raw) {
        Set<String> safe = new LinkedHashSet<>();
        if (raw == null) return "";
        for (String line : raw.split("\\n")) {
            String url = line.trim();
            if (isBackupWebUrl(url)) safe.add(url);
            if (safe.size() >= 5000) break;
        }
        StringBuilder result = new StringBuilder();
        for (String url : safe) result.append(url).append('\n');
        return result.toString();
    }

    private boolean isBackupWebUrl(String value) {
        if (!isWebUrl(value) || value.length() > 2048) return false;
        try {
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            return uri.getHost() != null
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (Exception ignored) {
            return false;
        }
    }

    private void rebuildTabs(List<String> urls, int selectedIndex) {
        for (WebView tab : tabs) {
            tab.stopLoading();
            topLevelHosts.remove(tab);
            pageBlockedCounts.remove(tab);
            webContainer.removeView(tab);
            tab.destroy();
        }
        tabs.clear();
        webContainer.removeAllViews();
        active = 0;

        for (int i = 0; i < urls.size() && i < MAX_TABS; i++) {
            String url = urls.get(i);
            addTab(HOME_PAGE.equals(url) ? HOME_PAGE : url);
        }
        if (tabs.isEmpty()) addTab(HOME_PAGE);
        selectTab(Math.max(0, Math.min(selectedIndex, tabs.size() - 1)));
    }

    private void showSettings() {
        String homepage = getHomepage();
        String[] items = {
            "Search engine: " + getSearchEngineName(),
            "Homepage: " + homepage,
            "Restore tabs on startup: " + (prefs.getBoolean("restore_tabs", true) ? "On" : "Off"),
            "Check for updates",
            "About ASTER Browser"
        };
        new AlertDialog.Builder(this).setTitle("Browser settings").setItems(items, (d, which) -> {
            if (which == 0) chooseSearchEngine();
            else if (which == 1) editHomepage();
            else if (which == 2) chooseRestoreTabs();
            else if (which == 3) {
                Toast.makeText(this, "Checking for updates…", Toast.LENGTH_SHORT).show();
                checkForUpdates(true);
            } else if (which == 4) {
                new AlertDialog.Builder(this).setTitle("ASTER Browser")
                    .setMessage("Version " + BuildConfig.VERSION_NAME + "\\nAutomatic update checks are enabled. Updates always require your confirmation in Android.")
                    .setPositiveButton("Check now", (dialog, button) -> checkForUpdates(true))
                    .setNegativeButton("Close", null).show();
            }
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
        updateHandler.removeCallbacks(periodicUpdateCheck);
        saveSession();
        for (WebView tab : tabs) tab.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        long pendingUpdate = prefs.getLong("update_download_id", -1L);
        if (pendingUpdate >= 0 && (Build.VERSION.SDK_INT < 26 || getPackageManager().canRequestPackageInstalls())) {
            openDownloadedUpdate(pendingUpdate);
        }
        checkForUpdates(false);
        updateHandler.removeCallbacks(periodicUpdateCheck);
        updateHandler.postDelayed(periodicUpdateCheck, UPDATE_CHECK_INTERVAL);
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
        updateHandler.removeCallbacks(periodicUpdateCheck);
        if (updateReceiverRegistered) {
            try { unregisterReceiver(updateDownloadReceiver); } catch (Exception ignored) {}
            updateReceiverRegistered = false;
        }
        saveSession();
        for (WebView w : tabs) {
            w.stopLoading();
            topLevelHosts.remove(w);
            pageBlockedCounts.remove(w);
            w.destroy();
        }
        super.onDestroy();
    }
}