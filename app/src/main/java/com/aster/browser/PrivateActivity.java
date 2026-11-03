package com.aster.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;

/**
 * Private browser window. It runs in its own Android process and uses a separate
 * WebView data directory on Android 9+. It never writes ASTER history or sessions.
 */
public class PrivateActivity extends Activity {
    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private boolean isFinishingCleanup = false;

    @Override protected void onCreate(Bundle state) {
        // This must happen before the first WebView is created in this process.
        if (Build.VERSION.SDK_INT >= 28) {
            WebView.setDataDirectorySuffix("aster_private");
        }
        super.onCreate(state);

        if (Build.VERSION.SDK_INT < 28) {
            new AlertDialog.Builder(this)
                .setTitle("Private browsing unavailable")
                .setMessage("A fully isolated private WebView profile requires Android 9 or newer. This device cannot safely start ASTER private browsing.")
                .setPositiveButton("Close", (d, w) -> finish())
                .setCancelable(false)
                .show();
            return;
        }

        getWindow().setStatusBarColor(Color.rgb(12, 16, 28));
        getWindow().setNavigationBarColor(Color.rgb(12, 16, 28));
        buildPrivateWindow();
        load("https://www.google.com");
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable surface(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), Color.argb(90, 177, 198, 255));
        return shape;
    }

    private TextView action(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(Color.WHITE);
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(dp(46));
        view.setMinHeight(dp(44));
        view.setBackground(surface(Color.rgb(35, 43, 65), 14));
        return view;
    }

    private void buildPrivateWindow() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(8));
        root.setBackgroundColor(Color.rgb(12, 16, 28));
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(8));

        TextView badge = new TextView(this);
        badge.setText("◉ PRIVATE");
        badge.setTextColor(Color.rgb(185, 204, 255));
        badge.setTextSize(11);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setPadding(dp(8), 0, dp(8), 0);
        header.addView(badge);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.rgb(172, 181, 202));
        address.setHint("Search privately");
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        address.setPadding(dp(13), 0, dp(10), 0);
        address.setBackground(surface(Color.rgb(31, 39, 58), 16));
        header.addView(address, new LinearLayout.LayoutParams(0, dp(46), 1));
        address.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadInput(address.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        TextView close = action("×");
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        closeParams.leftMargin = dp(7);
        header.addView(close, closeParams);
        close.setOnClickListener(v -> finish());
        root.addView(header);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(151, 179, 255)));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setDatabaseEnabled(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(false);
        cookies.setAcceptThirdPartyCookies(web, false);

        web.setDownloadListener((url, userAgent, disposition, mime, size) ->
            Toast.makeText(this, "Downloads are disabled in private windows", Toast.LENGTH_SHORT).show());

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? android.view.View.GONE : android.view.View.VISIBLE);
                if (value == 100 && view.getUrl() != null) address.setText(view.getUrl());
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception ignored) {
                    Toast.makeText(PrivateActivity.this, "No app can open this link", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                Toast.makeText(PrivateActivity.this, "Unsafe certificate blocked", Toast.LENGTH_LONG).show();
            }

            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                address.setText(url);
            }

            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url);
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, 0);
        TextView back = action("‹");
        TextView forward = action("›");
        TextView home = action("⌂");
        TextView done = action("Done");
        TextView[] buttons = {back, forward, home, done};
        for (TextView button : buttons) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
            params.setMargins(dp(3), 0, dp(3), 0);
            footer.addView(button, params);
        }
        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); });
        forward.setOnClickListener(v -> { if (web.canGoForward()) web.goForward(); });
        home.setOnClickListener(v -> load("https://www.google.com"));
        done.setOnClickListener(v -> finish());
        root.addView(footer);
    }

    private void loadInput(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) return;
        if (value.startsWith("https://") || value.startsWith("http://")) {
            load(value);
        } else if (value.matches("(?i)^[a-z][a-z0-9+.-]*://.*")) {
            Toast.makeText(this, "Only HTTP and HTTPS are supported", Toast.LENGTH_SHORT).show();
        } else if (value.contains(".") && !value.contains(" ")) {
            load("https://" + value);
        } else {
            try { load("https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")); }
            catch (Exception ignored) { load("https://www.google.com"); }
        }
    }

    private void load(String url) {
        if (web != null) web.loadUrl(url);
    }

    private void hideKeyboard() {
        InputMethodManager input = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (input != null && address != null) input.hideSoftInputFromWindow(address.getWindowToken(), 0);
        if (address != null) address.clearFocus();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else finish();
    }

    @Override protected void onDestroy() {
        if (!isFinishingCleanup && web != null) {
            isFinishingCleanup = true;
            web.stopLoading();
            web.clearHistory();
            web.clearCache(true);
            web.clearFormData();
            web.clearSslPreferences();
            WebStorage.getInstance().deleteAllData();
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
