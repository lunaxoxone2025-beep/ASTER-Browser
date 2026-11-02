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
    private static final int MAX_TABS = 12;
    private static final int DOWNLOAD_PERMISSION_REQUEST = 701;

    private final int BG = Color.rgb(15, 17, 23);
    private final int PANEL = Color.rgb(27, 31, 42);
    private final int TEXT = Color.rgb(241, 244, 250);
    private final int MUTED = Color.rgb(151, 161, 180);
    private final int ACCENT = Color.rgb(126, 145, 255);

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
        if (!restoreSession()) addTab(getHomepage());
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView button(String label, int size) {
        TextView v = new TextView(this);
        v.setText(label);
        v.setTextColor(TEXT);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        v.setPadding(10, 8, 10, 8);
        return v;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(16, 10, 16, 8);

        TextView brand = button("✳  ASTER", 19);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        brand.setTextColor(ACCENT);
        top.addView(brand, new LinearLayout.LayoutParams(0, 48, 1));

        tabCount = button("1", 14);
        tabCount.setBackground(rounded(PANEL, 18));
        top.addView(tabCount, new LinearLayout.LayoutParams(44, 38));
        tabCount.setOnClickListener(v -> showTabs());
        root.addView(top);

        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(12, 4, 12, 10);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setTextColor(TEXT);
        address.setHintTextColor(MUTED);
        address.setHint("Search or enter address");
        address.setPadding(16, 0, 12, 0);
        address.setSelectAllOnFocus(true);
        address.setBackground(rounded(PANEL, 24));
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setAutofillHints("url");
        toolbar.addView(address, new LinearLayout.LayoutParams(0, 48, 1));
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(address.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });
        root.addView(toolbar);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        progressBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(BG));
        root.addView(progressBar, new LinearLayout.LayoutParams(-1, 3));

        webContainer = new FrameLayout(this);
        root.addView(webContainer, new LinearLayout.LayoutParams(-1, 0, 1));

        bottomBar = new LinearLayout(this);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setPadding(8, 8, 8, 12);
        bottomBar.setBackgroundColor(BG);
        String[] labels = {"‹", "›", "⌂", "↻", "＋", "⋮"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView b = button(labels[i], 24);
            bottomBar.addView(b, new LinearLayout.LayoutParams(0, 48, 1));
            b.setOnClickListener(v -> {
                switch (index) {
                    case 0:
                        if (current().canGoBack()) current().goBack();
                        break;
                    case 1:
                        if (current().canGoForward()) current().goForward();
                        break;
                    case 2:
                        current().loadUrl(getHomepage());
                        break;
                    case 3:
                        current().reload();
                        break;
                    case 4:
                        addTab(getHomepage());
                        break;
                    case 5:
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
                    if (progress == 100 && view.getUrl() != null) address.setText(view.getUrl());
                }
            }

            @Override public void onReceivedTitle(WebView view, String title) {
                if (view == current()) tabCount.setContentDescription("Tabs. Current page: " + title);
            }
        });

        w.setWebViewClient(new WebViewClient() {
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
                if (view == current()) address.setText(url);
                if (isWebUrl(url)) {
                    String old = prefs.getString("history", "");
                    String line = url + "\n";
                    if (!old.contains(line)) prefs.edit().putString("history", line + old).apply();
                }
                saveSession();
            }
        });
        return w;
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
        w.loadUrl(url);
        saveSession();
    }

    private void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        active = index;
        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).setVisibility(i == active ? View.VISIBLE : View.GONE);
        }
        tabCount.setText(String.valueOf(tabs.size()));
        address.setText(current().getUrl() == null ? "" : current().getUrl());
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
                if (isWebUrl(url)) addTab(url);
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
            String url = tabs.get(i).getUrl();
            if (url == null || !isWebUrl(url)) url = getHomepage();
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
                if (which == tabs.size()) addTab(getHomepage());
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
                case 8: addTab(getHomepage()); break;
            }
        }).show();
    }

    private void addBookmark() {
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
        super.onPause();
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