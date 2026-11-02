package com.aster.browser;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(17, 19, 24);
    private final int PANEL = Color.rgb(27, 30, 38);
    private final int TEXT = Color.rgb(239, 242, 248);
    private final int MUTED = Color.rgb(150, 158, 174);

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
        addTab("https://www.google.com");
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius);
        return d;
    }

    private TextView button(String label, int size) {
        TextView v = new TextView(this);
        v.setText(label); v.setTextColor(TEXT); v.setTextSize(size);
        v.setGravity(Gravity.CENTER); v.setPadding(10, 8, 10, 8);
        return v;
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(16, 12, 16, 8);
        TextView brand = button("✳  ASTER", 19);
        brand.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(brand, new LinearLayout.LayoutParams(0, 48, 1));
        tabCount = button("1", 14);
        tabCount.setBackground(rounded(PANEL, 18));
        top.addView(tabCount, new LinearLayout.LayoutParams(42, 38));
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
        toolbar.addView(address, new LinearLayout.LayoutParams(0, 48, 1));
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                navigate(address.getText().toString()); hideKeyboard(); return true;
            }
            return false;
        });
        root.addView(toolbar);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(113, 134, 255)));
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
                    case 0: if (current().canGoBack()) current().goBack(); break;
                    case 1: if (current().canGoForward()) current().goForward(); break;
                    case 2: current().loadUrl("https://www.google.com"); break;
                    case 3: current().reload(); break;
                    case 4: addTab("https://www.google.com"); break;
                    case 5: showMenu(); break;
                }
            });
        }
        root.addView(bottomBar);
    }

    private WebView current() { return tabs.get(active); }

    private void addTab(String url) {
        WebView w = new WebView(this);
        w.setBackgroundColor(Color.WHITE);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setDomStorageEnabled(true);
        w.getSettings().setLoadWithOverviewMode(true);
        w.getSettings().setUseWideViewPort(true);
        w.getSettings().setSupportZoom(true);
        w.getSettings().setBuiltInZoomControls(true);
        WebSettings settings = w.getSettings();
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
                    if (progress == 100) address.setText(view.getUrl());
                }
            }
        });
        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (view == current()) address.setText(url);
                String old = prefs.getString("history", "");
                String line = url + "\n";
                if (!old.contains(line)) prefs.edit().putString("history", line + old).apply();
            }
        });
        tabs.add(w);
        webContainer.addView(w, new FrameLayout.LayoutParams(-1, -1));
        selectTab(tabs.size() - 1);
        w.loadUrl(url);
    }

    private void selectTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        active = index;
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).setVisibility(i == active ? View.VISIBLE : View.GONE);
        tabCount.setText(String.valueOf(tabs.size()));
        address.setText(current().getUrl() == null ? "" : current().getUrl());
    }

    private void navigate(String input) {
        String value = input.trim();
        if (value.isEmpty()) return;
        if (value.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            current().loadUrl(value);
        } else if (value.contains(".") && !value.contains(" ")) {
            current().loadUrl("https://" + value);
        } else {
            try { current().loadUrl("https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")); }
            catch (Exception e) { current().loadUrl("https://www.google.com"); }
        }
    }

    private void hideKeyboard() {
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(address.getWindowToken(), 0);
        address.clearFocus();
    }

    private void showTabs() {
        String[] names = new String[tabs.size() + 1];
        for (int i = 0; i < tabs.size(); i++) names[i] = (i == active ? "✓ " : "") + (tabs.get(i).getTitle() == null ? "New tab" : tabs.get(i).getTitle());
        names[tabs.size()] = "+ New tab";
        new AlertDialog.Builder(this).setTitle("ASTER tabs").setItems(names, (d, which) -> {
            if (which == tabs.size()) addTab("https://www.google.com"); else selectTab(which);
        }).setNegativeButton("Close active tab", (d, w) -> closeTab()).show();
    }

    private void closeTab() {
        if (tabs.size() <= 1) { current().loadUrl("https://www.google.com"); return; }
        WebView w = tabs.remove(active);
        webContainer.removeView(w); w.destroy();
        selectTab(Math.max(0, active - 1));
    }

    private void showMenu() {
        String[] items = {"Add bookmark", "Bookmarks", "History", "Find in page", "Share page", "Downloads", "Clear browsing history", "New tab"};
        new AlertDialog.Builder(this).setTitle("ASTER menu").setItems(items, (d, which) -> {
            switch (which) {
                case 0:
                    String u = current().getUrl();
                    if (u != null) prefs.edit().putString("bookmarks", u + "\n" + prefs.getString("bookmarks", "")).apply();
                    Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show(); break;
                case 1: showSavedList("Bookmarks", "bookmarks"); break;
                case 2: showSavedList("History", "history"); break;
                case 3: findInPage(); break;
                case 4: sharePage(); break;
                case 5:
                    try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); }
                    catch (Exception e) { Toast.makeText(this, "Downloads are managed by Android", Toast.LENGTH_SHORT).show(); }
                    break;
                case 6: prefs.edit().remove("history").apply(); Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show(); break;
                case 7: addTab("https://www.google.com"); break;
            }
        }).show();
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
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 701);
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
            request.setMimeType(pendingDownloadMime);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            if (pendingDownloadUserAgent != null) request.addRequestHeader("User-Agent", pendingDownloadUserAgent);
            String cookies = CookieManager.getInstance().getCookie(pendingDownloadUrl);
            if (cookies != null) request.addRequestHeader("Cookie", cookies);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
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
        if (requestCode == 701) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startPendingDownload();
            else {
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
            .setNegativeButton("Close", null)
            .setNeutralButton("Clear", (d, w) -> current().clearMatches())
            .setPositiveButton("Find", (d, w) -> {
                String value = query.getText().toString().trim();
                if (!value.isEmpty()) current().findAllAsync(value);
            }).show();
    }

    private void sharePage() {
        String url = current().getUrl();
        if (url == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, url);
        startActivity(Intent.createChooser(share, "Share page"));
    }

    private void showSavedList(String heading, String key) {
        String raw = prefs.getString(key, "");
        String[] values = raw.isEmpty() ? new String[]{"Nothing saved yet"} : raw.split("\n");
        new AlertDialog.Builder(this).setTitle(heading).setItems(values, (d, which) -> {
            if (!raw.isEmpty()) current().loadUrl(values[which]);
        }).setPositiveButton("Close", null).show();
    }

    @Override public void onBackPressed() {
        if (current().canGoBack()) current().goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        for (WebView w : tabs) w.destroy();
        super.onDestroy();
    }
}
