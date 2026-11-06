package com.aster.browser;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ASTER Browser — an original, privacy-first Android browser built on Android System WebView.
 */
public class MainActivity extends Activity implements AsterWebView.Listener {
    static final String START = StartPage.BASE_URL;
    private static final int MAX_TABS = 100;
    private static final int REQ_FILE = 8103;
    private static final int REQ_EXPORT = 8101;
    private static final int REQ_IMPORT = 8102;
    private static final int REQ_VOICE = 8104;
    private static final int REQ_DOWNLOAD_PERM = 701;
    private static final int REQ_MEDIA_PERM = 8201;
    private static final int REQ_LOCATION_PERM = 8202;
    static final String REWARDS_URL = "https://rewards.bing.com";
    private static final String[] MICROSOFT_DOMAINS = {"bing.com", "live.com", "microsoft.com", "microsoftonline.com",
        "xbox.com", "msn.com", "office.com", "live.net", "msftauth.net", "msauth.net", "gfx.ms", "microsoftrewards.com"};
    private static final String GX_CORNER_API = "https://www.gamerpower.com/api/giveaways?type=game&sort-by=popularity";

    // ------------------------------------------------------------------ state
    SharedPreferences prefs;
    Theme theme;
    Store store;
    ShieldEngine shields;
    private Updater updater;
    private UiSounds sounds;
    boolean incognito;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Tab> tabs = new ArrayList<>();
    private int active = -1;

    // ------------------------------------------------------------------ views
    private LinearLayout content;
    private LinearLayout toolbar;
    private LinearLayout bottomBar;
    private FrameLayout webArea;
    private EditText address;
    private TextView shieldIcon;
    private TextView shieldBadge;
    private TextView reloadBtn;
    private TextView tabsTop;
    private TextView tabsBottom;
    private TextView backBtn;
    private TextView forwardBtn;
    private TextView newTabBtn;
    private TextView menuBtn;
    private TextView backTop, forwardTop, homeTop, newTabTop, menuTop;
    private HorizontalScrollView tabStripScroll;
    private LinearLayout tabStrip;
    private boolean bottomBarHidden;
    private boolean resumed;
    private ProgressBar progress;
    private ScrollView suggestScroll;
    private LinearLayout suggestBox;
    private LinearLayout findBar;
    private EditText findInput;
    private TextView findCount;
    private TextView pullIndicator;
    private FabMenu fab;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private int orientationBeforeFullscreen = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;

    // ------------------------------------------------------------------ pending requests
    private ValueCallback<Uri[]> fileCallback;
    private String dlUrl, dlAgent, dlDisposition, dlMime;
    private PermissionRequest pendingMedia;
    private String pendingMediaOrigin;
    private String[] pendingMediaResources;
    private GeolocationPermissions.Callback pendingGeo;
    private String pendingGeoOrigin;
    private final AtomicBoolean badgePending = new AtomicBoolean(false);
    private int suggestSeq = 0;
    private final int fpSeed = (int) (Math.random() * 100000);

    /** Overridden by {@link PrivateActivity}. */
    protected boolean isPrivateWindow() { return false; }

    // ================================================================== lifecycle

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        incognito = isPrivateWindow();
        prefs = incognito ? privatePrefs() : getSharedPreferences("aster", MODE_PRIVATE);
        I18n.init(prefs.getString("language", "auto"));
        if (incognito && Build.VERSION.SDK_INT < 28) {
            new I18n.Builder(this).setTitle("Private browsing unavailable")
                .setMessage("An isolated private profile requires Android 9 or newer.")
                .setPositiveButton("Close", (d, w) -> finish()).setCancelable(false).show();
            return;
        }
        migratePrefs();
        theme = Theme.byId(prefs.getString("ui_theme", "aster"));
        if (incognito && theme.light) theme = Theme.byId("liquid");
        store = new Store(this, incognito);
        shields = new ShieldEngine(this, prefs);
        if (prefs.getBoolean("gx_sounds", true)) sounds = new UiSounds();
        buildUi();

        String incoming = incomingUrl(getIntent());
        boolean restored = !incognito && prefs.getBoolean("restore_tabs", true) && restoreSession();
        if (incoming != null) openInNewTab(incoming, false);
        else if (!restored) openInNewTab(null, false);

        if (!incognito) {
            updater = new Updater(this, prefs);
            updater.register();
            updater.check(false);
            if (shields.needsUpdate()) shields.updateAsync((ok, rules) -> ui.post(this::refreshShieldChrome));
            refreshGxCorner(false);
            setupRewardsOnce();
        }
    }

    /** One-time Microsoft Rewards setup: shortcut on the start page and an offer to switch to Bing. */
    private void setupRewardsOnce() {
        if (!prefs.getBoolean("rewards_dial_added", false)) {
            if (store.ensureDial(0, "Rewards", REWARDS_URL)) refreshStartPages();
            prefs.edit().putBoolean("rewards_dial_added", true).apply();
        }
        if (prefs.getBoolean("rewards_prompt_done", false) || "bing".equals(prefs.getString("search_engine", "brave"))) return;
        prefs.edit().putBoolean("rewards_prompt_done", true).apply();
        ui.postDelayed(() -> {
            if (isFinishing()) return;
            new I18n.Builder(this).setTitle("Earn Microsoft Rewards points?")
                .setMessage("Searches made with Bing while you're signed in to your Microsoft account earn Rewards points you can redeem for Xbox gift cards and Game Pass. Use Bing as ASTER's search engine?\n\nYou can change this any time in Settings → Search engine.")
                .setNegativeButton("No thanks", null)
                .setPositiveButton("Use Bing", (d, w) -> {
                    prefs.edit().putString("search_engine", "bing").apply();
                    refreshStartPages();
                    toast("Bing is now your search engine");
                }).show();
        }, 1200);
    }

    void openRewards() {
        Tab t = current();
        if (t != null && t.start) load(t, REWARDS_URL); else openInNewTab(REWARDS_URL, false);
    }

    /**
     * Microsoft sign-in (Bing, Rewards, Xbox, Outlook) shares cookies between its own domains,
     * so those sites keep working with third-party cookies allowed. Other sites follow Shields.
     */
    boolean allowThirdPartyCookies(String host) {
        String h = ShieldEngine.normalize(host);
        if (h == null) return false;
        for (String d : MICROSOFT_DOMAINS) if (h.equals(d) || h.endsWith("." + d)) return true;
        return shields.siteFlag(h, "cookies3p", false) || !shields.shieldsUp(h);
    }

    private void applyCookiePolicy(WebView view, String url) {
        try {
            CookieManager.getInstance().setAcceptThirdPartyCookies(view, allowThirdPartyCookies(Uri.parse(url).getHost()));
        } catch (Exception ignored) { }
    }

    static boolean isYoutube(String host) {
        return host != null && (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.endsWith("youtube-nocookie.com"));
    }

    /**
     * Private windows run in their own process. They get a throw-away copy of the settings so that
     * nothing changed there can overwrite the normal window's preferences file.
     */
    @SuppressWarnings("unchecked")
    private SharedPreferences privatePrefs() {
        SharedPreferences main = getSharedPreferences("aster", MODE_PRIVATE);
        SharedPreferences p = getSharedPreferences("aster_private", MODE_PRIVATE);
        SharedPreferences.Editor e = p.edit().clear();
        for (Map.Entry<String, ?> entry : main.getAll().entrySet()) {
            Object v = entry.getValue();
            String k = entry.getKey();
            if (v instanceof String) e.putString(k, (String) v);
            else if (v instanceof Boolean) e.putBoolean(k, (Boolean) v);
            else if (v instanceof Integer) e.putInt(k, (Integer) v);
            else if (v instanceof Long) e.putLong(k, (Long) v);
            else if (v instanceof Float) e.putFloat(k, (Float) v);
            else if (v instanceof java.util.Set) e.putStringSet(k, (java.util.Set<String>) v);
        }
        e.commit();
        return p;
    }

    private void migratePrefs() {
        SharedPreferences.Editor e = prefs.edit();
        // 0.x stored all filter rules inside SharedPreferences; they now live in files.
        e.remove("adblock_domain_rules").remove("adblock_allow_rules").remove("adblock_last_update").remove("adblock_filter_version");
        if (!prefs.contains("search_engine")) e.putString("search_engine", "brave");
        e.apply();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String url = incomingUrl(intent);
        if (url != null) openInNewTab(url, false);
    }

    private String incomingUrl(Intent intent) {
        if (intent == null) return null;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            String data = intent.getDataString();
            if (Store.isWeb(data)) return data;
        }
        if (Intent.ACTION_SEND.equals(action)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && !text.trim().isEmpty()) return resolveInput(text.trim());
        }
        if (Intent.ACTION_WEB_SEARCH.equals(action)) {
            String q = intent.getStringExtra(android.app.SearchManager.QUERY);
            if (q != null && !q.trim().isEmpty()) return resolveInput(q.trim());
        }
        return null;
    }

    @Override protected void onPause() {
        super.onPause();
        if (shields == null) return;
        shields.persistStats();
        store.flush();
        saveSession();
        resumed = false;
        boolean bg = prefs.getBoolean("background_play", false) || inPip();
        for (Tab t : tabs) {
            if (t.web == null) continue;
            if (bg && tabs.indexOf(t) == active) continue;
            t.web.onPause();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (shields == null) return;
        resumed = true;
        Tab t = current();
        if (t != null && t.web != null) t.web.onResume();
        if (updater != null) {
            updater.resumePending();
            updater.check(false);
        }
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (pendingMedia != null) { try { pendingMedia.deny(); } catch (Exception ignored) { } pendingMedia = null; }
        if (pendingGeo != null && pendingGeoOrigin != null) {
            try { pendingGeo.invoke(pendingGeoOrigin, false, false); } catch (Exception ignored) { }
            pendingGeo = null;
        }
        if (updater != null) updater.unregister();
        if (shields != null) {
            shields.persistStats();
            store.flush();
            saveSession();
        }
        for (Tab t : tabs) destroyWeb(t);
        if (shields != null && (incognito || (isFinishing() && prefs.getBoolean("clear_on_exit", false)))) wipeSiteData();
        if (sounds != null) sounds.release();
        super.onDestroy();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (content != null) layoutChrome();
    }

    @Override public void onBackPressed() {
        if (customView != null) { exitFullscreen(); return; }
        if (!overlays.isEmpty()) { overlays.get(overlays.size() - 1).dismiss(); return; }
        if (fab != null && fab.isExpanded()) { fab.collapse(); return; }
        if (findBar != null && findBar.getVisibility() == View.VISIBLE) { closeFind(); return; }
        if (address != null && address.hasFocus()) { address.clearFocus(); hideKeyboard(); return; }
        Tab t = current();
        if (t != null && t.web != null && t.web.canGoBack()) { t.web.goBack(); return; }
        if (t != null && !t.start && tabs.size() > 1 && t.web != null && !t.web.canGoBack()) {
            closeTab(active);
            return;
        }
        super.onBackPressed();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN && e.isCtrlPressed() && address != null) {
            switch (e.getKeyCode()) {
                case KeyEvent.KEYCODE_L: address.requestFocus(); showKeyboard(address); return true;
                case KeyEvent.KEYCODE_T: openInNewTab(null, false); return true;
                case KeyEvent.KEYCODE_W: closeTab(active); return true;
                case KeyEvent.KEYCODE_R: reload(); return true;
                case KeyEvent.KEYCODE_F: openFind(); return true;
                case KeyEvent.KEYCODE_D: toggleBookmark(); return true;
                case KeyEvent.KEYCODE_N: if (e.isShiftPressed()) { openPrivate(null); return true; } break;
                case KeyEvent.KEYCODE_TAB:
                    if (tabs.size() > 1) select((active + (e.isShiftPressed() ? -1 : 1) + tabs.size()) % tabs.size());
                    return true;
                default: break;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    // ================================================================== UI helpers

    int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    GradientDrawable shape(int fill, float radiusDp, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        else if (theme != null && theme.glass) d.setStroke(dp(1), Color.argb(90, 223, 239, 255));
        return d;
    }

    Drawable pressable(int fill, float radiusDp, int stroke) {
        return new RippleDrawable(ColorStateList.valueOf(Theme.withAlpha(theme.accent, 90)), shape(fill, radiusDp, stroke), null);
    }

    TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(I18n.t(s));
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    TextView bold(String s, float sp, int color) {
        TextView t = text(s, sp, color);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView chromeButton(String icon, float sp, String description) {
        TextView v = text(icon, sp, theme.text);
        v.setGravity(Gravity.CENTER);
        v.setContentDescription(I18n.t(description));
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    void onTap(View v, final Runnable action) {
        v.setOnClickListener(x -> {
            feedback(x);
            bounce(x);
            action.run();
        });
    }

    /** Small press animation so every control reacts to the finger. */
    void bounce(View v) {
        if (v == null) return;
        float s = v.getWidth() > dp(220) ? 0.97f : 0.86f;
        v.animate().cancel();
        v.setScaleX(s);
        v.setScaleY(s);
        v.animate().scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(new OvershootInterpolator(2.2f)).start();
    }

    void feedback(View v) {
        if (prefs.getBoolean("gx_haptics", true) && v != null) v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (sounds != null) sounds.click();
    }

    private void toast(String s) { Toast.makeText(this, I18n.t(s), Toast.LENGTH_SHORT).show(); }

    private void applyInsets(View v) {
        if (Build.VERSION.SDK_INT < 35) return;
        v.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return insets;
        });
        v.requestApplyInsets();
    }

    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && address != null) imm.hideSoftInputFromWindow(address.getWindowToken(), 0);
    }

    // ================================================================== integrated panels
    // Menus, panels and pages are drawn inside the browser window (no separate dialog windows),
    // so they open instantly, animate smoothly and can be dragged closed.

    interface Overlay { void dismiss(); }

    private final List<Overlay> overlays = new ArrayList<>();
    private FrameLayout rootFrame;
    private FrameLayout overlayLayer;

    Sheet sheet(View body) { return new Sheet(body); }

    /** Blurred snapshot of the browser as it is right now (used behind glass panels). */
    private void frost(Glass.BitmapCallback cb) {
        Glass.capture(this, rootFrame, cb);
    }

    /** Bottom panel with drag-to-dismiss, drawn inside the browser window. */
    final class Sheet implements Overlay {
        final ImageView scrim;
        final DragPanel panel;
        private boolean closing;

        Sheet(View body) {
            scrim = new ImageView(MainActivity.this);
            scrim.setScaleType(ImageView.ScaleType.FIT_XY);
            scrim.setBackgroundColor(theme.glass ? Color.argb(theme.light ? 40 : 70, 0, 0, 0) : Color.argb(150, 0, 0, 0));
            scrim.setClickable(true);
            scrim.setOnClickListener(v -> dismiss());
            panel = new DragPanel(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            if (theme.glass) {
                panel.setBackground(Glass.material(theme, Glass.topRadii(dp(30)), getResources().getDisplayMetrics().density, theme.light ? 175 : 150));
            } else {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Theme.blend(theme.bg, theme.surface, 0.35f));
                float r = dp(26);
                bg.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
                bg.setStroke(dp(1), Theme.withAlpha(theme.accent, 80));
                panel.setBackground(bg);
                panel.setElevation(dp(24));
            }
            panel.setClickable(true);
            View handle = new View(MainActivity.this);
            handle.setBackground(shape(Theme.withAlpha(theme.muted, 150), 3, 0));
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(44), dp(5));
            hp.gravity = Gravity.CENTER_HORIZONTAL;
            hp.topMargin = dp(10);
            hp.bottomMargin = dp(4);
            panel.addView(handle, hp);
            panel.scroll = new ScrollView(MainActivity.this);
            panel.scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            panel.scroll.setVerticalScrollBarEnabled(false);
            panel.scroll.addView(body);
            panel.addView(panel.scroll, new LinearLayout.LayoutParams(-1, -2));
        }

        void show() {
            hideKeyboard();
            if (theme.glass) frost(bitmap -> {
                if (bitmap == null) return;
                scrim.setImageBitmap(bitmap);
                scrim.setColorFilter(theme.light ? Color.argb(70, 255, 255, 255) : Color.argb(60, 0, 0, 0), android.graphics.PorterDuff.Mode.SRC_ATOP);
            });
            overlayLayer.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
            int avail = overlayLayer.getWidth() > 0 ? overlayLayer.getWidth() : getResources().getDisplayMetrics().widthPixels;
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.min(avail, dp(620)), -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.topMargin = dp(56);
            overlayLayer.addView(panel, lp);
            overlays.add(this);
            scrim.setAlpha(0f);
            scrim.animate().alpha(1f).setDuration(200).start();
            panel.setTranslationY(dp(420));
            panel.setAlpha(0.6f);
            panel.animate().translationY(0).alpha(1f).setDuration(280).setInterpolator(new DecelerateInterpolator(2.2f)).start();
        }

        @Override public void dismiss() {
            if (closing) return;
            closing = true;
            overlays.remove(this);
            scrim.animate().alpha(0f).setDuration(180).start();
            float h = panel.getHeight() > 0 ? panel.getHeight() : dp(420);
            panel.animate().translationY(h).setDuration(200).setInterpolator(new AccelerateInterpolator(1.6f))
                .withEndAction(() -> {
                    overlayLayer.removeView(panel);
                    overlayLayer.removeView(scrim);
                }).start();
        }
    }

    /** Panel that follows the finger downwards (when its content is at the top) and closes on release. */
    final class DragPanel extends LinearLayout {
        ScrollView scroll;
        private final Sheet owner;
        private float startY;
        private float lastY;
        private long lastT;
        private float velocity;

        DragPanel(Sheet owner) {
            super(MainActivity.this);
            this.owner = owner;
        }

        @Override public boolean onInterceptTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startY = e.getRawY();
                    lastY = startY;
                    lastT = System.currentTimeMillis();
                    velocity = 0;
                    return false;
                case MotionEvent.ACTION_MOVE: {
                    float dy = e.getRawY() - startY;
                    boolean atTop = scroll == null || scroll.getScrollY() == 0;
                    return dy > dp(12) && atTop;
                }
                default:
                    return false;
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startY = e.getRawY();
                    lastY = startY;
                    lastT = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dy = Math.max(0, e.getRawY() - startY);
                    long now = System.currentTimeMillis();
                    if (now > lastT) velocity = (e.getRawY() - lastY) / (now - lastT);
                    lastY = e.getRawY();
                    lastT = now;
                    setTranslationY(dy);
                    owner.scrim.setAlpha(Math.max(0f, 1f - dy / Math.max(1, getHeight())));
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    float dy = e.getRawY() - startY;
                    if (dy > Math.min(dp(150), getHeight() * 0.3f) || velocity > 1.1f) owner.dismiss();
                    else {
                        animate().translationY(0).setDuration(200).setInterpolator(new DecelerateInterpolator(2f)).start();
                        owner.scrim.animate().alpha(1f).setDuration(200).start();
                    }
                    return true;
                }
                default:
                    return true;
            }
        }
    }

    /** Full-screen page (tabs, settings, history, bookmarks, downloads) that slides in over the browser. */
    final class Page implements Overlay {
        final LinearLayout root;
        final LinearLayout body;
        final LinearLayout header;
        final LinearLayout bottom;
        final ScrollView scroll;
        private final boolean fromBottom;
        private boolean closing;
        Runnable onDismiss;

        Page(String title) { this(title, false); }

        Page(String title, boolean fromBottom) {
            this.fromBottom = fromBottom;
            root = new LinearLayout(MainActivity.this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(theme.glass ? Theme.withAlpha(theme.bg, 235) : theme.bg);
            root.setClickable(true);
            root.setElevation(dp(16));
            header = new LinearLayout(MainActivity.this);
            header.setGravity(Gravity.CENTER_VERTICAL);
            header.setPadding(dp(6), dp(6), dp(10), dp(6));
            TextView back = chromeButton(fromBottom ? "⌄" : "←", 22, "Back");
            back.setBackground(pressable(Color.TRANSPARENT, 14, 0));
            onTap(back, this::dismiss);
            header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
            TextView t = bold(title, 19, theme.text);
            t.setSingleLine(true);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            header.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            root.addView(header);
            scroll = new ScrollView(MainActivity.this);
            scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            body = new LinearLayout(MainActivity.this);
            body.setOrientation(LinearLayout.VERTICAL);
            body.setPadding(dp(14), dp(4), dp(14), dp(24));
            scroll.addView(body);
            root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            bottom = new LinearLayout(MainActivity.this);
            bottom.setGravity(Gravity.CENTER);
            bottom.setPadding(dp(12), dp(8), dp(12), dp(10));
            bottom.setVisibility(View.GONE);
            root.addView(bottom);
        }

        TextView action(String label, Runnable r) {
            TextView a = bold(label, 13, theme.accent);
            a.setPadding(dp(12), dp(10), dp(12), dp(10));
            a.setBackground(pressable(theme.surface, 14, 0));
            onTap(a, r);
            header.addView(a);
            return a;
        }

        void show() {
            hideKeyboard();
            if (theme.glass) frost(bitmap -> {
                if (bitmap == null) return;
                android.graphics.drawable.BitmapDrawable frosted = new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
                frosted.setFilterBitmap(true);
                int tint = theme.light ? Color.argb(120, 245, 248, 255) : Color.argb(120, 8, 12, 26);
                root.setBackground(new android.graphics.drawable.LayerDrawable(new Drawable[]{frosted, new ColorDrawable(tint)}));
            });
            overlayLayer.addView(root, new FrameLayout.LayoutParams(-1, -1));
            overlays.add(this);
            root.setAlpha(0f);
            if (fromBottom) {
                root.setTranslationY(dp(90));
                root.setScaleX(0.96f);
                root.setScaleY(0.96f);
            } else {
                root.setTranslationX(overlayLayer.getWidth() > 0 ? overlayLayer.getWidth() * 0.3f : dp(120));
            }
            root.animate().alpha(1f).translationX(0).translationY(0).scaleX(1f).scaleY(1f)
                .setDuration(260).setInterpolator(new DecelerateInterpolator(2.2f)).start();
        }

        @Override public void dismiss() {
            if (closing) return;
            closing = true;
            overlays.remove(this);
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
            if (fromBottom) root.animate().translationY(dp(90)).scaleX(0.96f).scaleY(0.96f);
            else root.animate().translationX(root.getWidth() * 0.3f);
            root.animate().alpha(0f).setDuration(190).setInterpolator(new AccelerateInterpolator(1.5f))
                .withEndAction(() -> {
                    overlayLayer.removeView(root);
                    if (onDismiss != null) onDismiss.run();
                }).start();
        }
    }

    TextView sectionTitle(LinearLayout parent, String title) {
        TextView s = bold(I18n.t(title).toUpperCase(), 11, theme.accent);
        s.setLetterSpacing(0.12f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(18);
        lp.bottomMargin = dp(6);
        lp.leftMargin = dp(4);
        parent.addView(s, lp);
        return s;
    }

    LinearLayout rowCard(LinearLayout parent, String title, String subtitle, Runnable click) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        row.setBackground(pressable(theme.surface, 14, 0));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(title, 15, theme.text));
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView sub = text(subtitle, 12, theme.muted);
            sub.setPadding(0, dp(2), 0, 0);
            texts.addView(sub);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(6);
        parent.addView(row, lp);
        if (click != null) onTap(row, click);
        return row;
    }

    Switch switchRow(LinearLayout parent, String title, String subtitle, final String key, boolean def, final Runnable changed) {
        LinearLayout row = rowCard(parent, title, subtitle, null);
        final Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setThumbTintList(ColorStateList.valueOf(theme.accent));
        sw.setTrackTintList(ColorStateList.valueOf(Theme.withAlpha(theme.accent, 110)));
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        sw.setOnCheckedChangeListener((b, checked) -> {
            feedback(b);
            prefs.edit().putBoolean(key, checked).apply();
            if (changed != null) changed.run();
        });
        return sw;
    }

    void choose(String title, final String[] labels, int checked, final ChoiceListener listener) {
        new I18n.Builder(this).setTitle(title)
            .setSingleChoiceItems(labels, checked, (d, which) -> {
                d.dismiss();
                listener.onChoice(which);
            }).setNegativeButton("Cancel", null).show();
    }

    interface ChoiceListener { void onChoice(int which); }

    private EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setHint(I18n.t(hint));
        e.setText(value == null ? "" : value);
        e.setTextColor(theme.text);
        e.setHintTextColor(theme.muted);
        return e;
    }

    // ================================================================== layout

    private void buildUi() {
        getWindow().setStatusBarColor(theme.bg);
        getWindow().setNavigationBarColor(theme.bg);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        rootFrame = new FrameLayout(this);
        rootFrame.addView(content, new FrameLayout.LayoutParams(-1, -1));
        overlayLayer = new FrameLayout(this);
        rootFrame.addView(overlayLayer, new FrameLayout.LayoutParams(-1, -1));
        setContentView(rootFrame);
        applyInsets(rootFrame);

        // ---- toolbar
        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);

        // Desktop-style controls, only shown on tablets / unfolded foldables / large windows.
        backTop = chromeButton("‹", 28, "Back");
        forwardTop = chromeButton("›", 28, "Forward");
        homeTop = chromeButton("⌂", 20, "Home");
        onTap(backTop, this::goBack);
        onTap(forwardTop, this::goForward);
        onTap(homeTop, () -> { Tab t = current(); if (t != null) loadStart(t); });
        toolbar.addView(backTop, new LinearLayout.LayoutParams(dp(42), dp(44)));
        toolbar.addView(forwardTop, new LinearLayout.LayoutParams(dp(42), dp(44)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(dp(42), dp(44));
        hp.rightMargin = dp(4);
        toolbar.addView(homeTop, hp);

        FrameLayout shieldFrame = new FrameLayout(this);
        shieldIcon = chromeButton("⛨", 19, "ASTER Halo");
        shieldFrame.addView(shieldIcon, new FrameLayout.LayoutParams(-1, -1));
        shieldBadge = bold("", 9, Color.WHITE);
        shieldBadge.setGravity(Gravity.CENTER);
        shieldBadge.setPadding(dp(4), 0, dp(4), 0);
        shieldBadge.setMinWidth(dp(16));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, dp(16), Gravity.TOP | Gravity.END);
        bp.topMargin = dp(3);
        bp.rightMargin = dp(1);
        shieldFrame.addView(shieldBadge, bp);
        onTap(shieldIcon, this::showShields);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(dp(44), dp(44));
        sp.rightMargin = dp(6);
        toolbar.addView(shieldFrame, sp);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(15);
        address.setHint(I18n.t(incognito ? "Search privately" : "Search or type URL"));
        address.setPadding(dp(16), 0, dp(12), 0);
        address.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        address.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        address.setSelectAllOnFocus(true);
        if (Build.VERSION.SDK_INT >= 26) address.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        toolbar.addView(address, new LinearLayout.LayoutParams(0, dp(44), 1));
        address.setOnEditorActionListener((v, action, ev) -> {
            if (action == EditorInfo.IME_ACTION_GO || (ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER && ev.getAction() == KeyEvent.ACTION_DOWN)) {
                navigate(address.getText().toString());
                return true;
            }
            return false;
        });
        address.setOnFocusChangeListener((v, focused) -> {
            Tab t = current();
            if (focused) {
                address.setText(t == null || t.start ? "" : t.url);
                address.selectAll();
                updateSuggestions("");
                setBottomBarHidden(true);
            } else {
                suggestScroll.setVisibility(View.GONE);
                updateAddress();
                setBottomBarHidden(false);
            }
        });
        address.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (address.hasFocus()) updateSuggestions(s.toString());
            }
        });

        reloadBtn = chromeButton("↻", 21, "Reload");
        onTap(reloadBtn, () -> {
            Tab t = current();
            if (t != null && t.loading && t.web != null) t.web.stopLoading(); else reload();
        });
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(42), dp(44));
        rp.leftMargin = dp(4);
        toolbar.addView(reloadBtn, rp);

        tabsTop = chromeButton("1", 13, "Tabs");
        tabsTop.setTypeface(Typeface.DEFAULT_BOLD);
        onTap(tabsTop, this::showTabs);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(40), dp(40));
        tp.leftMargin = dp(4);
        toolbar.addView(tabsTop, tp);

        newTabTop = chromeButton("+", 24, "New tab");
        onTap(newTabTop, () -> openInNewTab(null, false));
        toolbar.addView(newTabTop, new LinearLayout.LayoutParams(dp(42), dp(44)));
        menuTop = chromeButton("⋮", 22, "Menu");
        onTap(menuTop, this::showMenu);
        toolbar.addView(menuTop, new LinearLayout.LayoutParams(dp(40), dp(44)));

        // Swipe left/right on the address bar to switch tabs.
        final GestureDetector swipe = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null || address.hasFocus() || tabs.size() < 2) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) < dp(50) || Math.abs(dx) < Math.abs(dy) * 2) return false;
                switchTabBySwipe(dx < 0 ? 1 : -1);
                return true;
            }
        });
        address.setOnTouchListener((v, e) -> !address.hasFocus() && swipe.onTouchEvent(e));

        tabStripScroll = new HorizontalScrollView(this);
        tabStripScroll.setHorizontalScrollBarEnabled(false);
        tabStrip = new LinearLayout(this);
        tabStrip.setGravity(Gravity.CENTER_VERTICAL);
        tabStrip.setPadding(dp(10), dp(2), dp(10), dp(6));
        tabStripScroll.addView(tabStrip);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);

        // ---- web area with overlays
        webArea = new FrameLayout(this);

        pullIndicator = bold("↻", 22, Color.WHITE);
        pullIndicator.setGravity(Gravity.CENTER);
        pullIndicator.setVisibility(View.GONE);
        pullIndicator.setElevation(dp(8));
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(46), dp(46), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        webArea.addView(pullIndicator, pp);

        buildFindBar();
        suggestScroll = new ScrollView(this);
        suggestBox = new LinearLayout(this);
        suggestBox.setOrientation(LinearLayout.VERTICAL);
        suggestBox.setPadding(dp(8), dp(6), dp(8), dp(10));
        suggestScroll.addView(suggestBox);
        suggestScroll.setVisibility(View.GONE);
        suggestScroll.setElevation(dp(12));
        webArea.addView(suggestScroll, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        fab = new FabMenu(this);
        webArea.addView(fab, new FrameLayout.LayoutParams(-1, -1));

        // ---- bottom bar
        bottomBar = new LinearLayout(this);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        backBtn = chromeButton("‹", 30, "Back");
        forwardBtn = chromeButton("›", 30, "Forward");
        newTabBtn = chromeButton("+", 26, "New tab");
        tabsBottom = chromeButton("1", 13, "Tabs");
        tabsBottom.setTypeface(Typeface.DEFAULT_BOLD);
        menuBtn = chromeButton("⋮", 24, "Menu");
        onTap(backBtn, this::goBack);
        onTap(forwardBtn, this::goForward);
        onTap(newTabBtn, () -> openInNewTab(null, false));
        onTap(tabsBottom, this::showTabs);
        onTap(menuBtn, this::showMenu);
        newTabBtn.setOnLongClickListener(v -> { openPrivate(null); return true; });
        tabsBottom.setOnLongClickListener(v -> { closeTab(active); return true; });
        backBtn.setOnLongClickListener(v -> { showHistory(); return true; });
        TextView[] items = {backBtn, forwardBtn, newTabBtn, tabsBottom, menuBtn};
        for (TextView item : items) bottomBar.addView(item, new LinearLayout.LayoutParams(0, dp(46), 1));
        applyTheme();
        layoutChrome();
    }

    private void buildFindBar() {
        findBar = new LinearLayout(this);
        findBar.setGravity(Gravity.CENTER_VERTICAL);
        findBar.setPadding(dp(8), dp(6), dp(6), dp(6));
        findBar.setVisibility(View.GONE);
        findBar.setElevation(dp(10));
        findInput = new EditText(this);
        findInput.setSingleLine(true);
        findInput.setHint(I18n.t("Find in page"));
        findInput.setTextSize(14);
        findInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        findBar.addView(findInput, new LinearLayout.LayoutParams(0, dp(42), 1));
        findCount = text("0/0", 12, Color.GRAY);
        findCount.setPadding(dp(8), 0, dp(8), 0);
        findBar.addView(findCount);
        TextView up = chromeButton("⌃", 20, "Previous match");
        TextView down = chromeButton("⌄", 20, "Next match");
        TextView close = chromeButton("✕", 16, "Close find");
        findBar.addView(up, new LinearLayout.LayoutParams(dp(40), dp(40)));
        findBar.addView(down, new LinearLayout.LayoutParams(dp(40), dp(40)));
        findBar.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        up.setOnClickListener(v -> { Tab t = current(); if (t != null && t.web != null) t.web.findNext(false); });
        down.setOnClickListener(v -> { Tab t = current(); if (t != null && t.web != null) t.web.findNext(true); });
        close.setOnClickListener(v -> closeFind());
        findInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                Tab t = current();
                if (t == null || t.web == null) return;
                if (s.length() == 0) { t.web.clearMatches(); findCount.setText("0/0"); }
                else t.web.findAllAsync(s.toString());
            }
        });
        findInput.setOnEditorActionListener((v, a, e) -> {
            Tab t = current();
            if (t != null && t.web != null) t.web.findNext(true);
            return true;
        });
        webArea.addView(findBar, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
    }

    private boolean fabMode() { return "fab".equals(prefs.getString("nav_style", "bar")); }
    /** Tablets, unfolded foldables, Chromebooks and large split-screen windows. */
    private boolean wide() { return getResources().getConfiguration().screenWidthDp >= 600; }

    /** Desktop-style tab strip for wide screens. */
    private void rebuildTabStrip() {
        if (tabStrip == null || !wide()) return;
        tabStrip.removeAllViews();
        View activeChip = null;
        for (int i = 0; i < tabs.size(); i++) {
            final Tab t = tabs.get(i);
            final int index = i;
            boolean isActive = i == active;
            LinearLayout chip = new LinearLayout(this);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(12), 0, dp(2), 0);
            chip.setBackground(pressable(isActive ? Theme.withAlpha(theme.accent, 60) : theme.surface, 12,
                isActive ? Theme.withAlpha(theme.accent, 160) : 0));
            TextView title = text((t.sleeping() && !t.start ? "💤 " : "") + t.displayTitle(), 12, isActive ? theme.text : theme.muted);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            title.setMaxWidth(dp(170));
            chip.addView(title);
            TextView x = chromeButton("✕", 12, "Close tab");
            x.setTextColor(theme.muted);
            chip.addView(x, new LinearLayout.LayoutParams(dp(30), dp(34)));
            x.setOnClickListener(v -> { feedback(v); closeTab(index); });
            chip.setOnClickListener(v -> { feedback(v); select(index); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(34));
            lp.rightMargin = dp(6);
            tabStrip.addView(chip, lp);
            if (isActive) activeChip = chip;
        }
        TextView plus = chromeButton("+", 20, "New tab");
        plus.setTextColor(theme.accent);
        onTap(plus, () -> openInNewTab(null, false));
        tabStrip.addView(plus, new LinearLayout.LayoutParams(dp(40), dp(34)));
        final View target = activeChip;
        if (target != null) tabStripScroll.post(() -> tabStripScroll.smoothScrollTo(Math.max(0, target.getLeft() - dp(60)), 0));
    }

    /** Switches tab with a short slide animation (address-bar swipe). */
    private void switchTabBySwipe(int direction) {
        if (tabs.size() < 2) return;
        int next = (active + direction + tabs.size()) % tabs.size();
        feedback(address);
        select(next);
        Tab t = current();
        if (t != null && t.web != null) {
            t.web.setTranslationX(direction * webArea.getWidth() * 0.25f);
            t.web.setAlpha(0.4f);
            t.web.animate().translationX(0).alpha(1f).setDuration(180).start();
        }
    }

    /**
     * Auto-hides the bottom toolbar while reading. The bar floats over the page and only slides,
     * so the page never changes size (no jumps, no fighting with the scroll position).
     */
    private void setBottomBarHidden(boolean hide) {
        if (bottomBarHidden == hide || bottomBar.getParent() == null) return;
        bottomBarHidden = hide;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomBar.getLayoutParams();
        float distance = bottomBar.getHeight() + (lp == null ? 0 : lp.bottomMargin) + dp(12);
        bottomBar.animate().cancel();
        bottomBar.animate()
            .translationY(hide ? distance : 0)
            .alpha(hide ? 0f : 1f)
            .setDuration(hide ? 200 : 240)
            .setInterpolator(hide ? new AccelerateInterpolator(1.4f) : new DecelerateInterpolator(2f))
            .start();
        if (!hide) {
            Tab t = current();
            if (t != null && t.web != null) t.web.resetScrollState();
        }
    }
    private boolean addressBottom() { return "bottom".equals(prefs.getString("address_position", "top")); }

    /** Places toolbar / bottom bar according to navigation preferences and screen width. */
    private void layoutChrome() {
        content.removeAllViews();
        boolean fabOn = fabMode();
        boolean wide = wide();
        boolean bottom = addressBottom() && !wide;
        int side = dp(wide ? 14 : 8);
        toolbar.setPadding(side, dp(6), side, dp(6));
        bottomBar.setPadding(dp(4), dp(4), dp(4), dp(6));
        int desk = wide ? View.VISIBLE : View.GONE;
        backTop.setVisibility(desk);
        forwardTop.setVisibility(desk);
        homeTop.setVisibility(desk);
        newTabTop.setVisibility(desk);
        menuTop.setVisibility(wide || fabOn ? View.VISIBLE : View.GONE);
        tabsTop.setVisibility(fabOn && !wide ? View.VISIBLE : View.GONE);
        bottomBarHidden = false;
        bottomBar.animate().cancel();
        bottomBar.setTranslationY(0);
        bottomBar.setAlpha(1f);
        bottomBar.setVisibility(View.VISIBLE);
        if (bottomBar.getParent() != null) ((ViewGroup) bottomBar.getParent()).removeView(bottomBar);
        LinearLayout.LayoutParams toolLp = new LinearLayout.LayoutParams(-1, -2);
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        if (theme.glass) {
            toolLp.setMargins(dp(10), dp(bottom ? 4 : 6), dp(10), dp(bottom ? 10 : 4));
            barLp.setMargins(dp(16), dp(4), dp(16), dp(10));
            toolbar.setPadding(dp(8), dp(4), dp(8), dp(4));
        }
        if (!bottom) {
            content.addView(toolbar, toolLp);
            if (wide) content.addView(tabStripScroll, new LinearLayout.LayoutParams(-1, -2));
            content.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
            content.addView(webArea, new LinearLayout.LayoutParams(-1, 0, 1));
        } else {
            content.addView(webArea, new LinearLayout.LayoutParams(-1, 0, 1));
            content.addView(progress, new LinearLayout.LayoutParams(-1, dp(2)));
            content.addView(toolbar, toolLp);
        }
        // The bottom bar floats over the page (see setBottomBarHidden).
        if (!fabOn && !wide) webArea.addView(bottomBar, barLp);
        bottomBar.setElevation(theme.glass ? 0 : dp(10));
        if (wide) rebuildTabStrip();
        fab.setVisibility(fabOn ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams sl = (FrameLayout.LayoutParams) suggestScroll.getLayoutParams();
        sl.gravity = bottom ? Gravity.BOTTOM : Gravity.TOP;
        suggestScroll.setLayoutParams(sl);
        if (fabOn) setupFab();
    }

    private void setupFab() {
        List<FabMenu.Item> items = new ArrayList<>();
        items.add(new FabMenu.Item("‹", I18n.t("Back"), this::goBack));
        items.add(new FabMenu.Item("+", I18n.t("New tab"), () -> openInNewTab(null, false)));
        items.add(new FabMenu.Item("🔍", I18n.t("Search"), () -> { address.requestFocus(); showKeyboard(address); }));
        items.add(new FabMenu.Item("▦", I18n.t("Tabs"), this::showTabs));
        items.add(new FabMenu.Item("⛨", I18n.t("Halo"), this::showShields));
        items.add(new FabMenu.Item("⚡", I18n.t("ASTER Core"), this::showGxControl));
        items.add(new FabMenu.Item("☰", I18n.t("Menu"), this::showMenu));
        fab.setup(theme, items, new FabMenu.Gestures() {
            @Override public void swipeLeft() { goBack(); }
            @Override public void swipeRight() { goForward(); }
            @Override public void swipeUp() { showTabs(); }
            @Override public void longPress() { openInNewTab(null, false); }
            @Override public void feedback() { MainActivity.this.feedback(null); }
        });
    }

    private void applyTheme() {
        getWindow().setStatusBarColor(theme.glass ? (theme.light ? Color.rgb(214, 228, 255) : Color.rgb(8, 22, 54)) : theme.bg);
        getWindow().setNavigationBarColor(theme.glass ? (theme.light ? Color.rgb(214, 245, 238) : Color.rgb(4, 36, 44)) : theme.bg);
        applySystemBarAppearance(theme.light);
        int barBg;
        if (theme.glass) {
            // Liquid Glass: colourful backdrop with floating translucent capsules on top.
            rootFrame.setBackground(Glass.backdrop(theme, getResources().getDisplayMetrics().widthPixels));
            content.setBackgroundColor(Color.TRANSPARENT);
            float density = getResources().getDisplayMetrics().density;
            toolbar.setBackground(Glass.material(theme, Glass.radii(dp(26)), density, theme.light ? 150 : 120));
            bottomBar.setBackground(Glass.material(theme, Glass.radii(dp(28)), density, theme.light ? 150 : 120));
            address.setBackground(shape(theme.light ? Color.argb(150, 255, 255, 255) : Color.argb(36, 255, 255, 255), 20,
                theme.light ? Color.argb(200, 255, 255, 255) : Color.argb(70, 255, 255, 255)));
            barBg = Color.TRANSPARENT;
        } else {
            content.setBackgroundColor(theme.bg);
            rootFrame.setBackgroundColor(theme.bg);
            barBg = incognito ? Color.rgb(24, 14, 40) : theme.bg;
            toolbar.setBackgroundColor(barBg);
            bottomBar.setBackgroundColor(barBg);
            address.setBackground(shape(incognito ? Color.rgb(44, 26, 70) : theme.surface, 22, Theme.withAlpha(theme.accent, 45)));
        }
        address.setTextColor(theme.text);
        address.setHintTextColor(theme.muted);
        TextView[] icons = {shieldIcon, reloadBtn, backBtn, forwardBtn, menuBtn, backTop, forwardTop, homeTop, newTabTop, menuTop};
        for (TextView t : icons) {
            t.setTextColor(theme.text);
            t.setBackground(pressable(Color.TRANSPARENT, 14, 0));
        }
        newTabBtn.setTextColor(Color.WHITE);
        GradientDrawable plus = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{theme.accent, theme.accent2});
        plus.setCornerRadius(dp(16));
        newTabBtn.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(80, 255, 255, 255)), plus, null));
        LinearLayout.LayoutParams np = (LinearLayout.LayoutParams) newTabBtn.getLayoutParams();
        if (np != null) { np.setMargins(dp(10), dp(1), dp(10), dp(1)); newTabBtn.setLayoutParams(np); }
        for (TextView t : new TextView[]{tabsTop, tabsBottom}) {
            t.setTextColor(theme.text);
            GradientDrawable box = shape(Color.TRANSPARENT, 7, theme.text);
            box.setStroke(dp(2), theme.text);
            t.setBackground(new android.graphics.drawable.InsetDrawable(box, dp(9), dp(11), dp(9), dp(11)));
        }
        shieldBadge.setBackground(shape(theme.accent, 8, 0));
        progress.setProgressTintList(ColorStateList.valueOf(theme.accent));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(barBg));
        suggestScroll.setBackground(shape(theme.glass ? Theme.withAlpha(theme.light ? Color.WHITE : Color.rgb(14, 20, 38), 242) : theme.bg, 0, 0));
        findBar.setBackgroundColor(theme.glass ? Theme.withAlpha(theme.light ? Color.WHITE : Color.rgb(14, 20, 38), 242) : theme.surface);
        findInput.setTextColor(theme.text);
        findInput.setHintTextColor(theme.muted);
        findCount.setTextColor(theme.muted);
        for (int i = 0; i < findBar.getChildCount(); i++) {
            View c = findBar.getChildAt(i);
            if (c instanceof TextView && !(c instanceof EditText) && c != findCount) ((TextView) c).setTextColor(theme.text);
        }
        GradientDrawable pull = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{theme.accent, theme.accent2});
        pull.setShape(GradientDrawable.OVAL);
        pullIndicator.setBackground(pull);
        if (fabMode()) setupFab();
        refreshShieldChrome();
    }

    /** Dark status/navigation-bar icons for light themes. */
    @SuppressWarnings("deprecation")
    private void applySystemBarAppearance(boolean light) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c == null) return;
            int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            c.setSystemBarsAppearance(light ? mask : 0, mask);
        } else {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            int bits = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | (Build.VERSION.SDK_INT >= 26 ? View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR : 0);
            decor.setSystemUiVisibility(light ? (flags | bits) : (flags & ~bits));
        }
    }

    void setTheme(Theme t) {
        if (incognito && t.light) t = Theme.byId("liquid");
        theme = t;
        prefs.edit().putString("ui_theme", t.id).apply();
        applyTheme();
        refreshStartPages();
        toast(I18n.f("%s applied", t.name));
    }

    // ================================================================== tabs

    Tab current() { return active >= 0 && active < tabs.size() ? tabs.get(active) : null; }

    void openInNewTab(String url, boolean background) {
        if (tabs.size() >= MAX_TABS) {
            toast("Tab limit reached — close some tabs first");
            if (url != null && current() != null) load(current(), url);
            return;
        }
        Tab t = new Tab(url == null ? START : url);
        t.start = url == null;
        t.desktop = prefs.getBoolean("desktop_default", false);
        int index = background && active >= 0 ? active + 1 : tabs.size();
        tabs.add(index, t);
        if (background && active >= index) active++;
        if (sounds != null) sounds.open();
        if (background) {
            createWeb(t);
            if (t.start) loadStart(t); else load(t, t.url);
            t.web.setVisibility(View.GONE);
            t.web.onPause();
            toast("Opened in background");
            enforceLimit();
            updateTabCount();
        } else {
            select(index);
        }
        saveSession();
    }

    void select(int index) {
        if (index < 0 || index >= tabs.size()) return;
        Tab old = current();
        if (old != null && old.web != null && index != active) {
            captureThumb(old);
            old.web.setVisibility(View.GONE);
            old.web.onPause();
        }
        active = index;
        Tab t = tabs.get(index);
        t.lastUsed = System.currentTimeMillis();
        if (t.web == null) {
            createWeb(t);
            if (t.start || !Store.isWeb(t.url)) loadStart(t); else load(t, t.url);
        } else if (t.start) {
            patchStartPage(t);
        }
        for (Tab other : tabs) if (other.web != null) other.web.getSettings().setOffscreenPreRaster(other == t);
        t.web.setVisibility(View.VISIBLE);
        setBottomBarHidden(false);
        if (fab != null && fabMode()) fab.setFabVisible(true);
        t.web.onResume();
        if (findBar.getVisibility() == View.VISIBLE) closeFind();
        enforceLimit();
        updateChrome();
    }

    void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        Tab t = tabs.remove(index);
        destroyWeb(t);
        if (sounds != null) sounds.close();
        if (tabs.isEmpty()) {
            active = -1;
            openInNewTab(null, false);
            return;
        }
        int target;
        if (index == active) target = Math.min(index, tabs.size() - 1);
        else if (index < active) target = active - 1;
        else target = active;
        active = -1;
        select(Math.max(0, target));
        saveSession();
    }

    void closeAllTabs() {
        for (Tab t : tabs) destroyWeb(t);
        tabs.clear();
        active = -1;
        openInNewTab(null, false);
    }

    private void destroyWeb(Tab t) {
        if (t.web == null) return;
        AsterWebView w = t.web;
        t.web = null;
        try {
            w.stopLoading();
            webArea.removeView(w);
            w.setListener(null);
            w.destroy();
        } catch (Exception ignored) { }
    }

    /** Puts a tab to sleep: the WebView is released and only URL/title/thumbnail remain. */
    void sleepTab(Tab t) {
        if (t.web == null || t == current()) return;
        if (!t.start && t.web.getUrl() != null && Store.isWeb(t.web.getUrl())) t.url = t.web.getUrl();
        destroyWeb(t);
    }

    int liveTabs() {
        int n = 0;
        for (Tab t : tabs) if (t.web != null) n++;
        return n;
    }

    /** Memory saver: keeps only the most recently used tabs alive. */
    private void enforceLimit() {
        int max = prefs.getBoolean("gx_ram_limiter", true) ? Math.max(1, prefs.getInt("gx_max_live", 5)) : 16;
        if (liveTabs() <= max) return;
        List<Tab> live = new ArrayList<>();
        for (Tab t : tabs) if (t.web != null && t != current()) live.add(t);
        Collections.sort(live, new Comparator<Tab>() {
            @Override public int compare(Tab a, Tab b) { return Long.compare(a.lastUsed, b.lastUsed); }
        });
        int i = 0;
        while (liveTabs() > max && i < live.size()) sleepTab(live.get(i++));
    }

    private void captureThumb(Tab t) {
        if (t == null || t.web == null) return;
        try {
            int w = t.web.getWidth();
            int h = t.web.getHeight();
            if (w <= 0 || h <= 0) return;
            float scale = 0.4f;
            Bitmap b = Bitmap.createBitmap(Math.max(1, (int) (w * scale)), Math.max(1, (int) (Math.min(h, w * 1.3f) * scale)), Bitmap.Config.RGB_565);
            Canvas c = new Canvas(b);
            c.scale(scale, scale);
            c.translate(-t.web.getScrollX(), -t.web.getScrollY());
            t.web.draw(c);
            t.thumbnail = b;
        } catch (Throwable ignored) { }
    }

    private void saveSession() {
        if (incognito || store == null) return;
        try {
            JSONObject s = new JSONObject();
            JSONArray a = new JSONArray();
            for (Tab t : tabs) {
                JSONObject o = new JSONObject();
                String url = t.start ? START : (t.web != null && Store.isWeb(t.web.getUrl()) ? t.web.getUrl() : t.url);
                o.put("u", url);
                o.put("t", t.title == null ? "" : t.title);
                o.put("d", t.desktop);
                a.put(o);
            }
            s.put("tabs", a);
            s.put("active", Math.max(0, active));
            store.saveSession(s);
        } catch (Exception ignored) { }
    }

    private boolean restoreSession() {
        JSONObject s = store.loadSession();
        if (s == null) return false;
        JSONArray a = s.optJSONArray("tabs");
        if (a == null || a.length() == 0) return false;
        for (int i = 0; i < a.length() && tabs.size() < MAX_TABS; i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            String url = o.optString("u", START);
            Tab t = new Tab(url);
            t.start = !Store.isWeb(url);
            if (t.start) t.url = START;
            t.title = o.optString("t", "");
            t.desktop = o.optBoolean("d", false);
            tabs.add(t);
        }
        if (tabs.isEmpty()) return false;
        select(Math.max(0, Math.min(s.optInt("active", 0), tabs.size() - 1)));
        return true;
    }

    // ================================================================== WebView setup

    private String userAgent(boolean desktop) {
        String ua = WebSettings.getDefaultUserAgent(this);
        String chrome = "Chrome/124.0.0.0";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Chrome/[0-9.]+").matcher(ua);
        if (m.find()) chrome = m.group();
        if (desktop) return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " + chrome + " Safari/537.36";
        // Present as a regular mobile browser: some sites refuse sign-in inside embedded "wv" WebViews.
        return ua.replace("; wv)", ")").replaceFirst(" Version/\\d+(\\.\\d+)*", "");
    }

    private void applySettings(Tab t) {
        if (t.web == null) return;
        WebSettings s = t.web.getSettings();
        s.setUserAgentString(userAgent(t.desktop));
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setTextZoom(prefs.getInt("text_zoom", 100));
        s.setBlockNetworkImage(prefs.getBoolean("data_saver", false));
        boolean night = prefs.getBoolean("night_mode", false);
        if (Build.VERSION.SDK_INT >= 33) {
            s.setAlgorithmicDarkeningAllowed(night);
        } else if (Build.VERSION.SDK_INT >= 29) {
            applyForceDark(s, night);
        }
        t.web.setPullEnabled(prefs.getBoolean("pull_refresh", true));
    }

    @SuppressWarnings("deprecation")
    private void applyForceDark(WebSettings s, boolean night) {
        s.setForceDark(night ? WebSettings.FORCE_DARK_ON : WebSettings.FORCE_DARK_OFF);
    }

    private void applyAllSettings() {
        for (Tab t : tabs) applySettings(t);
    }

    private void createWeb(final Tab t) {
        final AsterWebView w = new AsterWebView(this);
        t.web = w;
        w.tab = t;
        w.setListener(this);
        w.setBackgroundColor(t.start ? theme.bg : Color.WHITE);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        if (Build.VERSION.SDK_INT >= 26) {
            s.setSafeBrowsingEnabled(true);
            // Background tabs get a lower renderer priority so the visible page stays smooth.
            w.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        }
        w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        applySettings(t);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, false);
        w.setDownloadListener((url, agent, disposition, mime, length) -> confirmDownload(url, agent, disposition, mime));
        w.setFindListener((activeMatch, matches, done) -> {
            if (t == current()) findCount.setText(matches == 0 ? "0/0" : (activeMatch + 1) + "/" + matches);
        });
        w.setOnLongClickListener(v -> onWebLongPress(t));
        w.setWebChromeClient(new Chrome(t));
        w.setWebViewClient(new Client(t));
        webArea.addView(w, 0, new FrameLayout.LayoutParams(-1, -1));
    }

    private final class Chrome extends WebChromeClient {
        private final Tab t;
        Chrome(Tab t) { this.t = t; }

        @Override public void onProgressChanged(WebView view, int p) {
            t.progress = p;
            if (t == current()) {
                progress.setVisibility(p >= 100 ? View.GONE : View.VISIBLE);
                if (Build.VERSION.SDK_INT >= 24) progress.setProgress(p, true); else progress.setProgress(p);
            }
        }

        @Override public void onReceivedTitle(WebView view, String title) {
            if (t.start) return;
            t.title = title;
            store.updateHistoryTitle(view.getUrl(), title);
            if (wide()) rebuildTabStrip();
        }

        @Override public void onReceivedIcon(WebView view, Bitmap icon) { t.favicon = icon; }

        @Override public void onShowCustomView(View view, CustomViewCallback callback) { enterFullscreen(view, callback); }

        @Override public void onHideCustomView() { exitFullscreen(); }

        @Override public boolean onCreateWindow(WebView view, boolean isDialog, boolean userGesture, Message resultMsg) {
            if (!userGesture && shields.shieldsUp(t.topHost)) {
                toast("Pop-up blocked");
                return false;
            }
            if (tabs.size() >= MAX_TABS) return false;
            Tab child = new Tab(START);
            child.start = false;
            child.desktop = t.desktop;
            int index = tabs.indexOf(t) + 1;
            tabs.add(index, child);
            if (active >= index) active++;
            createWeb(child);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(child.web);
            resultMsg.sendToTarget();
            select(index);
            return true;
        }

        @Override public void onCloseWindow(WebView window) {
            int i = tabs.indexOf(t);
            if (i >= 0) closeTab(i);
        }

        @Override public void onPermissionRequest(PermissionRequest request) { runOnUiThread(() -> promptMedia(request)); }

        @Override public void onPermissionRequestCanceled(PermissionRequest request) {
            if (request == pendingMedia) { pendingMedia = null; pendingMediaOrigin = null; pendingMediaResources = null; }
        }

        @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            runOnUiThread(() -> promptLocation(origin, callback));
        }

        @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = callback;
            try {
                startActivityForResult(params.createIntent(), REQ_FILE);
                return true;
            } catch (Exception e) {
                fileCallback = null;
                toast("No file picker available");
                return false;
            }
        }
    }

    private final class Client extends WebViewClient {
        private final Tab t;
        Client(Tab t) { this.t = t; }

        @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ("aster".equals(scheme)) {
                if (t.start) handleAsterAction(uri);
                return true;
            }
            if ("http".equals(scheme)) {
                if (request.isForMainFrame()) {
                    load(t, "https://" + uri.toString().substring(7));
                    return true;
                }
                return false;
            }
            if ("https".equals(scheme)) {
                if (request.isForMainFrame()) {
                    applyCookiePolicy(view, uri.toString());
                    boolean noJs = shields.blockScripts(uri.getHost());
                    if (view.getSettings().getJavaScriptEnabled() == noJs) {
                        view.getSettings().setJavaScriptEnabled(!noJs);
                    }
                    if (t.start) t.start = false;
                }
                return false;
            }
            if ("about".equals(scheme) || "data".equals(scheme) || "blob".equals(scheme) || "javascript".equals(scheme)) return false;
            openExternal(uri.toString());
            return true;
        }

        @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
            boolean isStart = url != null && url.startsWith(START);
            t.start = isStart;
            t.reader = t.readerPending;
            t.readerPending = false;
            t.loading = true;
            t.favicon = favicon;
            if (!isStart && url != null && !url.startsWith("data:")) {
                if (!url.equals(t.url)) t.title = "";
                t.url = url;
            }
            t.topHost = isStart ? "aster.start" : ShieldEngine.normalize(Uri.parse(url == null ? "" : url).getHost());
            t.resetCounters();
            view.setBackgroundColor(isStart ? theme.bg : Color.WHITE);
            if (t == current() && !address.hasFocus()) setBottomBarHidden(false);
            if (!isStart && t.topHost != null) {
                applyCookiePolicy(view, url);
                if (isYoutube(t.topHost) && prefs.getBoolean("yt_adblock", true) && shields.shieldsUp(t.topHost)) {
                    view.evaluateJavascript(Scripts.youtubeEarly(), null);
                }
                if (shields.blockFingerprinting(t.topHost)) view.evaluateJavascript(Scripts.fingerprint(fpSeed + t.topHost.hashCode()), null);
                if (prefs.getBoolean("background_play", false)) view.evaluateJavascript(Scripts.backgroundPlay(), null);
            }
            if (t == current()) updateChrome();
        }

        @Override public void onPageCommitVisible(WebView view, String url) {
            injectPageScripts(t, view, url);
        }

        @Override public void onPageFinished(WebView view, String url) {
            t.loading = false;
            injectPageScripts(t, view, url);
            if (!t.start && !t.reader && Store.isWeb(url)) {
                store.addHistory(t.title, url);
                t.url = url;
            }
            if (t == current()) updateChrome();
            saveSession();
        }

        @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            if (request.isForMainFrame()) return null;
            Uri uri = request.getUrl();
            String host = uri.getHost();
            if (host == null) return null;
            if (isYoutube(ShieldEngine.normalize(host)) && prefs.getBoolean("yt_adblock", true) && shields.shieldsUp(t.topHost)) {
                String path = uri.getPath() == null ? "" : uri.getPath();
                if (path.startsWith("/pagead/") || path.startsWith("/api/stats/ads") || path.startsWith("/ptracking")
                    || path.startsWith("/get_midroll_") || path.startsWith("/youtubei/v1/player/ad_break")) {
                    t.recordBlocked(host.toLowerCase(Locale.ROOT) + path, false);
                    shields.totalAds.incrementAndGet();
                    if (badgePending.compareAndSet(false, true)) ui.postDelayed(() -> { badgePending.set(false); refreshShieldChrome(); }, 250);
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
            }
            int verdict = shields.classify(host, t.topHost);
            if (verdict == 0) return null;
            t.recordBlocked(host.toLowerCase(Locale.ROOT), verdict == 2);
            // Runs on a WebView IO thread: never touch the tab list here, just schedule a UI refresh.
            if (badgePending.compareAndSet(false, true)) {
                ui.postDelayed(() -> { badgePending.set(false); refreshShieldChrome(); }, 250);
            }
            return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
        }

        @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            if (t == current()) toast("ASTER blocked a page with an invalid security certificate");
        }

        @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            // The renderer crashed or was killed to free memory: drop this WebView and reload lazily.
            boolean wasActive = t == current();
            destroyWeb(t);
            if (wasActive) {
                active = -1;
                select(tabs.indexOf(t));
                toast("Page reloaded after the renderer stopped");
            }
            return true;
        }
    }

    private void injectPageScripts(Tab t, WebView view, String url) {
        if (t.start || url == null || !Store.isWeb(url)) return;
        String host = ShieldEngine.normalize(Uri.parse(url).getHost());
        if (host == null) return;
        if (shields.cosmetic(host)) {
            view.evaluateJavascript(Scripts.style("__aster_shields", shields.cosmeticCss(host)), null);
            if (host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")) view.evaluateJavascript(Scripts.youtube(), null);
        }
        if (prefs.getBoolean("night_mode", false) && Build.VERSION.SDK_INT < 29) {
            view.evaluateJavascript(Scripts.style("__aster_night", Scripts.nightCss()), null);
        }
        if (prefs.getBoolean("background_play", false)) view.evaluateJavascript(Scripts.backgroundPlay(), null);
    }

    // ================================================================== pull to refresh / scroll

    @Override public void onPull(AsterWebView view, float distance, boolean released) {
        if (view.tab != current()) return;
        float trigger = dp(120);
        if (released) {
            if (distance >= trigger) {
                pullIndicator.animate().rotationBy(360).setDuration(500).withEndAction(this::hidePull).start();
                feedback(view);
                reload();
            } else hidePull();
            return;
        }
        pullIndicator.setVisibility(View.VISIBLE);
        float y = Math.min(distance * 0.45f, dp(90)) - dp(46);
        pullIndicator.setTranslationY(y);
        pullIndicator.setRotation(distance * 0.6f);
        pullIndicator.setAlpha(Math.min(1f, distance / trigger));
        pullIndicator.setScaleX(distance >= trigger ? 1.1f : 0.9f);
        pullIndicator.setScaleY(distance >= trigger ? 1.1f : 0.9f);
    }

    private void hidePull() {
        pullIndicator.animate().alpha(0f).translationY(-dp(46)).setDuration(160)
            .withEndAction(() -> pullIndicator.setVisibility(View.GONE)).start();
    }

    @Override public void onScrollDirection(AsterWebView view, boolean down) {
        if (view.tab != current() || fab == null) return;
        boolean hide = down && prefs.getBoolean("hide_bars_scroll", true);
        if (fabMode()) fab.setFabVisible(!hide);
        else if (!wide()) setBottomBarHidden(hide);
    }

    // ================================================================== navigation

    String resolveInput(String raw) {
        String v = raw.trim();
        if (v.isEmpty()) return null;
        if (v.matches("(?i)^https?://.*")) return v.replaceFirst("(?i)^http://", "https://");
        if (v.matches("(?i)^(about|data|file|javascript|intent|chrome):.*")) return null;
        if (!v.contains(" ") && (v.matches("(?i)^[a-z0-9.-]+\\.[a-z]{2,}(:[0-9]+)?(/.*)?$") || v.matches("^localhost(:[0-9]+)?(/.*)?$"))) {
            return "https://" + v;
        }
        return StartPage.searchUrl(prefs.getString("search_engine", "brave"), v);
    }

    /** Like {@link #resolveInput} but returns null instead of turning plain text into a search. */
    String webAddressOnly(String raw) {
        String v = raw == null ? "" : raw.trim();
        String u = resolveInput(v);
        if (u == null || !Store.isWeb(u)) return null;
        if (u.equals(StartPage.searchUrl(prefs.getString("search_engine", "brave"), v))) return null;
        return u;
    }

    void navigate(String raw) {
        String url = resolveInput(raw);
        if (url == null) return;
        address.clearFocus();
        hideKeyboard();
        Tab t = current();
        if (t == null) { openInNewTab(url, false); return; }
        load(t, url);
    }

    void load(Tab t, String url) {
        if (t.web == null) createWeb(t);
        t.start = false;
        t.url = url;
        boolean noJs = shields.blockScripts(Uri.parse(url).getHost());
        t.web.getSettings().setJavaScriptEnabled(!noJs);
        applyCookiePolicy(t.web, url);
        t.web.setBackgroundColor(Color.WHITE);
        Map<String, String> headers = new HashMap<>();
        headers.put("Sec-GPC", "1");
        headers.put("DNT", "1");
        t.web.loadUrl(url, headers);
        if (t == current()) updateChrome();
    }

    void loadStart(Tab t) {
        if (t.web == null) createWeb(t);
        t.start = true;
        t.url = START;
        t.title = "";
        t.web.getSettings().setJavaScriptEnabled(true);
        t.web.setBackgroundColor(theme.bg);
        String html = StartPage.build(theme, prefs, store, shields, incognito, prefs.getString("gx_corner_cache", ""));
        t.startBuiltAt = System.currentTimeMillis();
        t.web.loadDataWithBaseURL(START, html, "text/html", "UTF-8", START);
        if (t == current()) updateChrome();
    }

    /** Updates statistics and Arcade on open start pages without reloading them (keeps scroll position). */
    void patchStartPages() {
        for (Tab t : tabs) if (t.start && t.web != null) patchStartPage(t);
    }

    private void patchStartPage(Tab t) {
        if (t.web == null || !t.start) return;
        String js = "(function(){try{var s=document.getElementById('stats');if(s)s.innerHTML=" + Scripts.quote(StartPage.statsHtml(shields)) + ";"
            + "var a=document.getElementById('arcade-box');if(a)a.innerHTML=" + Scripts.quote(StartPage.arcadeHtml(prefs.getString("gx_corner_cache", ""))) + ";}catch(e){}})();";
        t.web.evaluateJavascript(js, null);
    }

    void refreshStartPages() {
        for (Tab t : tabs) if (t.start && t.web != null) loadStart(t);
    }

    private void reload() {
        Tab t = current();
        if (t == null || t.web == null) return;
        if (t.start) loadStart(t); else t.web.reload();
    }

    private void goBack() {
        Tab t = current();
        if (t != null && t.web != null && t.web.canGoBack()) t.web.goBack();
        else if (t != null && !t.start) loadStart(t);
    }

    private void goForward() {
        Tab t = current();
        if (t != null && t.web != null && t.web.canGoForward()) t.web.goForward();
    }

    private void openExternal(String url) {
        try {
            Intent intent;
            if (url.startsWith("intent:")) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                intent.setSelector(null);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (Store.isWeb(fallback) && current() != null) load(current(), fallback);
                    else toast("No app can open this link");
                }
                return;
            }
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (Exception e) {
            toast("No app can open this link");
        }
    }

    void openPrivate(String url) {
        if (incognito) { openInNewTab(url, false); return; }
        if (Build.VERSION.SDK_INT < 28) { toast("Private windows need Android 9 or newer"); return; }
        Intent i = new Intent(this, PrivateActivity.class);
        if (url != null) { i.setAction(Intent.ACTION_VIEW); i.setData(Uri.parse(url)); }
        startActivity(i);
    }

    private void handleAsterAction(Uri uri) {
        String host = uri.getHost() == null ? "" : uri.getHost();
        String path = uri.getPath() == null ? "" : uri.getPath();
        switch (host) {
            case "dial":
                if ("/add".equals(path)) editDial(-1);
                else if ("/edit".equals(path)) {
                    try { editDial(Integer.parseInt(uri.getQueryParameter("i"))); } catch (Exception ignored) { }
                } else showDialManager();
                break;
            case "voice": startVoice(); break;
            case "customize": showStartCustomization(); break;
            case "shields": showShields(); break;
            case "bookmarks": showBookmarks(); break;
            case "gxcorner": refreshGxCorner(true); break;
            case "arcade": showArcade(); break;
            default: break;
        }
    }

    // ================================================================== chrome state

    private void updateChrome() {
        Tab t = current();
        if (t == null) return;
        updateAddress();
        progress.setVisibility(t.loading && t.progress < 100 ? View.VISIBLE : View.GONE);
        progress.setProgress(t.progress);
        reloadBtn.setText(t.loading ? "✕" : "↻");
        boolean canBack = t.web != null && (t.web.canGoBack() || !t.start);
        boolean canFwd = t.web != null && t.web.canGoForward();
        backBtn.setAlpha(canBack ? 1f : 0.35f);
        forwardBtn.setAlpha(canFwd ? 1f : 0.35f);
        updateTabCount();
        refreshShieldChrome();
    }

    private void updateAddress() {
        if (address.hasFocus()) return;
        Tab t = current();
        if (t == null || t.start) { address.setText(""); return; }
        String url = t.web != null && t.web.getUrl() != null && !t.reader ? t.web.getUrl() : t.url;
        String shown = url == null ? "" : url.replaceFirst("^https://(www\\.)?", "");
        if (shown.endsWith("/")) shown = shown.substring(0, shown.length() - 1);
        if (t.reader) shown = "📖 " + shown;
        address.setText(shown);
    }

    private void updateTabCount() {
        String n = tabs.size() > 99 ? ":D" : String.valueOf(tabs.size());
        tabsTop.setText(n);
        tabsBottom.setText(n);
        if (wide()) rebuildTabStrip();
    }

    private void refreshShieldChrome() {
        if (shieldIcon == null) return;
        Tab t = current();
        String host = t == null || t.start ? null : t.topHost;
        boolean up = shields.shieldsUp(host);
        shieldIcon.setTextColor(up ? theme.accent : theme.muted);
        int n = t == null ? 0 : t.blocked();
        shieldBadge.setText(n > 99 ? "99+" : String.valueOf(n));
        shieldBadge.setVisibility(n > 0 && up ? View.VISIBLE : View.GONE);
    }

    // ================================================================== suggestions

    private void updateSuggestions(final String q) {
        suggestBox.removeAllViews();
        final String query = q.trim();
        final int seq = ++suggestSeq;
        if (query.isEmpty()) {
            addClipboardSuggestion();
            suggestScroll.setVisibility(suggestBox.getChildCount() > 0 ? View.VISIBLE : View.GONE);
            return;
        }
        String resolved = resolveInput(query);
        boolean looksUrl = resolved != null && !resolved.equals(StartPage.searchUrl(prefs.getString("search_engine", "brave"), query));
        if (looksUrl) addSuggestion("↗", query, "Go to site", () -> navigate(query));
        else addSuggestion("🔍", query, I18n.f("Search %s", StartPage.engineName(prefs.getString("search_engine", "brave"))), () -> navigate(query));
        for (final Store.Entry e : store.suggest(query, 4)) {
            addSuggestion(store.isBookmarked(e.url) ? "★" : "🕘", e.label(), Store.hostOf(e.url), () -> navigate(e.url));
        }
        suggestScroll.setVisibility(View.VISIBLE);
        if (!prefs.getBoolean("suggestions", !incognito) || looksUrl) return;
        ui.postDelayed(() -> {
            if (seq != suggestSeq) return;
            new Thread(() -> {
                final List<String> remote = fetchSuggestions(query);
                ui.post(() -> {
                    if (seq != suggestSeq || !address.hasFocus()) return;
                    int added = 0;
                    for (final String s : remote) {
                        if (s.equalsIgnoreCase(query) || added >= 6) continue;
                        addSuggestion("🔍", s, null, () -> navigate(s));
                        added++;
                    }
                });
            }, "ASTER-suggest").start();
        }, 170);
    }

    private void addClipboardSuggestion() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip() == null || cm.getPrimaryClip().getItemCount() == 0) return;
            CharSequence clip = cm.getPrimaryClip().getItemAt(0).getText();
            if (clip == null) return;
            final String c = clip.toString().trim();
            if (c.isEmpty() || c.length() > 400) return;
            addSuggestion("📋", c.length() > 60 ? c.substring(0, 60) + "…" : c, "Paste and go", () -> navigate(c));
        } catch (Exception ignored) { }
    }

    private List<String> fetchSuggestions(String q) {
        List<String> out = new ArrayList<>();
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(StartPage.suggestUrl(prefs.getString("search_engine", "brave"), q)).openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            c.setRequestProperty("Accept", "application/json");
            if (c.getResponseCode() != 200) return out;
            String body = new String(Store.readAll(c.getInputStream(), 256 * 1024), "UTF-8");
            Object v = new JSONTokener(body).nextValue();
            if (v instanceof JSONArray) {
                JSONArray arr = ((JSONArray) v).optJSONArray(1);
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    Object item = arr.opt(i);
                    if (item instanceof String) out.add((String) item);
                    else if (item instanceof JSONObject) out.add(((JSONObject) item).optString("phrase", ""));
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.disconnect();
        }
        return out;
    }

    private void addSuggestion(String icon, String title, String subtitle, final Runnable action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.setBackground(pressable(Color.TRANSPARENT, 12, 0));
        TextView i = text(icon, 15, theme.accent);
        i.setGravity(Gravity.CENTER);
        row.addView(i, new LinearLayout.LayoutParams(dp(34), -2));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(title, 15, theme.text);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(t);
        if (subtitle != null) {
            TextView s = text(subtitle, 11, theme.muted);
            s.setSingleLine(true);
            texts.addView(s);
        }
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        onTap(row, action);
        suggestBox.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    // ================================================================== long-press menu

    private boolean onWebLongPress(final Tab t) {
        if (t.web == null) return false;
        WebView.HitTestResult r = t.web.getHitTestResult();
        if (r == null) return false;
        int type = r.getType();
        final String extra = r.getExtra();
        if (extra == null) return false;
        if (type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
            if (t.start) {
                int i = Store.indexOf(store.speedDial, extra);
                if (i >= 0) { editDial(i); return true; }
            }
            linkMenu(extra, null);
            return true;
        }
        if (type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            Handler h = new Handler(Looper.getMainLooper(), msg -> {
                String href = msg.getData() == null ? null : msg.getData().getString("url");
                if (t.start && href != null) {
                    int i = Store.indexOf(store.speedDial, href);
                    if (i >= 0) { editDial(i); return true; }
                }
                linkMenu(href != null ? href : extra, extra);
                return true;
            });
            t.web.requestFocusNodeHref(h.obtainMessage());
            return true;
        }
        if (type == WebView.HitTestResult.IMAGE_TYPE) {
            linkMenu(null, extra);
            return true;
        }
        return false;
    }

    private void linkMenu(final String link, final String image) {
        List<String> labels = new ArrayList<>();
        final List<Runnable> actions = new ArrayList<>();
        if (Store.isWeb(link)) {
            labels.add("Open in new tab"); actions.add(() -> openInNewTab(link, false));
            labels.add("Open in background tab"); actions.add(() -> openInNewTab(link, true));
            labels.add("Open in private tab"); actions.add(() -> openPrivate(link));
            labels.add("Copy link"); actions.add(() -> copy(link));
            labels.add("Share link"); actions.add(() -> share(link));
        }
        if (Store.isWeb(image)) {
            labels.add("Open image in new tab"); actions.add(() -> openInNewTab(image, false));
            labels.add("Download image"); actions.add(() -> confirmDownload(image, null, null, null));
            labels.add("Copy image link"); actions.add(() -> copy(image));
        }
        if (labels.isEmpty()) return;
        String title = Store.isWeb(link) ? link : image;
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(4), dp(16), dp(16));
        TextView head = text(title.length() > 120 ? title.substring(0, 120) + "…" : title, 12, theme.muted);
        head.setMaxLines(2);
        head.setPadding(dp(4), dp(2), dp(4), dp(10));
        body.addView(head);
        final Sheet[] d = new Sheet[1];
        for (int i = 0; i < labels.size(); i++) {
            final Runnable action = actions.get(i);
            rowCard(body, labels.get(i), null, () -> { d[0].dismiss(); action.run(); });
        }
        d[0] = sheet(body);
        d[0].show();
    }

    private void copy(String s) {
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("ASTER", s));
        toast("Copied");
    }

    private void share(String s) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, s);
        startActivity(Intent.createChooser(i, "Share"));
    }

    // ================================================================== fullscreen video

    private void enterFullscreen(View view, WebChromeClient.CustomViewCallback callback) {
        if (customView != null) { callback.onCustomViewHidden(); return; }
        customView = view;
        customCallback = callback;
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        view.setBackgroundColor(Color.BLACK);
        decor.addView(view, new FrameLayout.LayoutParams(-1, -1));
        orientationBeforeFullscreen = getRequestedOrientation();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setSystemBarsHidden(true);
        updatePipParams(true);
    }

    private boolean pipAllowed() {
        return Build.VERSION.SDK_INT >= 26 && prefs.getBoolean("pip", true)
            && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }

    /** Android 12+: leaving the app during full-screen video goes straight into picture-in-picture. */
    private void updatePipParams(boolean video) {
        if (Build.VERSION.SDK_INT < 31 || !pipAllowed()) return;
        try {
            setPictureInPictureParams(new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9))
                .setAutoEnterEnabled(video)
                .build());
        } catch (Exception ignored) { }
    }

    @Override protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (customView != null && pipAllowed() && Build.VERSION.SDK_INT < 31) {
            try {
                enterPictureInPictureMode(new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build());
            } catch (Exception ignored) { }
        }
    }

    @Override public void onPictureInPictureModeChanged(boolean inPip, Configuration config) {
        super.onPictureInPictureModeChanged(inPip, config);
        if (inPip) return;
        // Closed (not expanded): stop the video unless background playback is on.
        ui.postDelayed(() -> {
            if (resumed || prefs.getBoolean("background_play", false)) return;
            Tab t = current();
            if (t != null && t.web != null) t.web.onPause();
        }, 400);
    }

    private boolean inPip() {
        return Build.VERSION.SDK_INT >= 24 && isInPictureInPictureMode();
    }

    private void exitFullscreen() {
        if (customView == null) return;
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        decor.removeView(customView);
        customView = null;
        setSystemBarsHidden(false);
        updatePipParams(false);
        setRequestedOrientation(orientationBeforeFullscreen);
        if (customCallback != null) customCallback.onCustomViewHidden();
        customCallback = null;
    }

    @SuppressWarnings("deprecation")
    private void setSystemBarsHidden(boolean on) {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c == null) return;
            if (on) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                c.show(WindowInsets.Type.systemBars());
            }
        } else {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(on
                ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                : 0);
        }
    }

    // ================================================================== find in page

    void openFind() {
        Tab t = current();
        if (t == null || t.start) { toast("Open a web page first"); return; }
        findBar.setVisibility(View.VISIBLE);
        findInput.setText("");
        findInput.requestFocus();
        showKeyboard(findInput);
    }

    private void closeFind() {
        findBar.setVisibility(View.GONE);
        Tab t = current();
        if (t != null && t.web != null) t.web.clearMatches();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(findInput.getWindowToken(), 0);
    }

    // ================================================================== menu

    interface Toggle { boolean run(); }

    /**
     * Integrated browser menu: a quick-action strip, a page card with Halo status, switchable
     * quick toggles and a tile grid. Toggles change in place without closing.
     */
    private void showMenu() {
        final Tab t = current();
        final boolean web = t != null && !t.start && Store.isWeb(t.url);
        final Sheet[] holder = new Sheet[1];
        final Runnable close = () -> { if (holder[0] != null) holder[0].dismiss(); };
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14), dp(2), dp(14), dp(18));

        // 1) quick actions
        LinearLayout strip = new LinearLayout(this);
        strip.setGravity(Gravity.CENTER);
        body.addView(strip, new LinearLayout.LayoutParams(-1, -2));
        boolean canFwd = t != null && t.web != null && t.web.canGoForward();
        stripButton(strip, "›", "Forward", canFwd, () -> { close.run(); goForward(); });
        final boolean saved = web && store.isBookmarked(t.url);
        final TextView[] star = new TextView[1];
        star[0] = stripButton(strip, saved ? "★" : "☆", "Bookmark", web && !incognito, () -> {
            toggleBookmark();
            boolean now = store.isBookmarked(t.url);
            star[0].setText(now ? "★" : "☆");
            star[0].setTextColor(now ? theme.accent : theme.text);
        });
        if (saved) star[0].setTextColor(theme.accent);
        stripButton(strip, "⬇", "Downloads", true, () -> { close.run(); showDownloads(); });
        stripButton(strip, "↗", "Share", web, () -> { close.run(); share(t.url); });
        final boolean loading = t != null && t.loading;
        stripButton(strip, loading ? "✕" : "↻", loading ? "Stop" : "Reload", t != null, () -> {
            close.run();
            Tab c = current();
            if (c != null && c.loading && c.web != null) c.web.stopLoading(); else reload();
        });

        // 2) current page + Shields status
        if (web) {
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(12), dp(10), dp(10), dp(10));
            card.setBackground(pressable(theme.surface, 18, 0));
            String host = Store.hostOf(t.url);
            TextView avatar = bold(host.isEmpty() ? "?" : host.substring(0, 1).toUpperCase(Locale.ROOT), 17, Color.WHITE);
            avatar.setGravity(Gravity.CENTER);
            GradientDrawable av = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{theme.accent, theme.accent2});
            av.setShape(GradientDrawable.OVAL);
            avatar.setBackground(av);
            card.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(12), 0, dp(8), 0);
            TextView title = bold(t.displayTitle(), 14, theme.text);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(title);
            TextView url = text((t.url.startsWith("https://") ? "🔒 " : "") + host, 12, theme.muted);
            url.setSingleLine(true);
            texts.addView(url);
            card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            boolean up = shields.shieldsUp(t.topHost);
            TextView pill = bold("⛨ " + t.blocked(), 12, up ? theme.accent : theme.muted);
            pill.setPadding(dp(10), dp(6), dp(10), dp(6));
            pill.setBackground(shape(up ? Theme.withAlpha(theme.accent, 45) : theme.surface2, 14, 0));
            card.addView(pill);
            onTap(card, () -> { close.run(); showShields(); });
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(-1, -2);
            cl.topMargin = dp(12);
            body.addView(card, cl);
        }

        // 3) quick toggles
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(0, dp(12), 0, dp(4));
        hs.addView(chips);
        body.addView(hs, new LinearLayout.LayoutParams(-1, -2));
        toggleChip(chips, "🌙", "Night mode", prefs.getBoolean("night_mode", false), () -> {
            toggleNight();
            return prefs.getBoolean("night_mode", false);
        });
        if (web) {
            toggleChip(chips, "🖥", "Desktop site", t.desktop, () -> {
                toggleDesktop();
                Tab c = current();
                return c != null && c.desktop;
            });
            toggleChip(chips, "⛨", "Halo", shields.shieldsUp(t.topHost), () -> {
                boolean on = !shields.shieldsUp(t.topHost);
                shields.setSite(t.topHost, "up", on);
                reload();
                refreshShieldChrome();
                return on;
            });
        }
        toggleChip(chips, "🍃", "Data saver", prefs.getBoolean("data_saver", false), () -> {
            boolean on = !prefs.getBoolean("data_saver", false);
            prefs.edit().putBoolean("data_saver", on).apply();
            applyAllSettings();
            reload();
            return on;
        });
        toggleChip(chips, "🎧", "Background play", prefs.getBoolean("background_play", false), () -> {
            boolean on = !prefs.getBoolean("background_play", false);
            prefs.edit().putBoolean("background_play", on).apply();
            return on;
        });
        if (web) toggleChip(chips, "📖", "Reader", t.reader, () -> { close.run(); toggleReader(); return !t.reader; });

        // 4) tiles
        List<Object[]> tiles = new ArrayList<>();
        tiles.add(new Object[]{"+", "New tab", (Runnable) () -> openInNewTab(null, false)});
        tiles.add(new Object[]{"👁", incognito ? "New private tab" : "Private window", (Runnable) () -> openPrivate(null)});
        tiles.add(new Object[]{"★", "Bookmarks", (Runnable) this::showBookmarks});
        tiles.add(new Object[]{"🕘", "History", (Runnable) this::showHistory});
        tiles.add(new Object[]{"🔎", "Find in page", (Runnable) this::openFind});
        tiles.add(new Object[]{"文", "Translate", (Runnable) this::translate});
        tiles.add(new Object[]{"⊕", "Add to Home", (Runnable) this::addToHome});
        tiles.add(new Object[]{"🕹", "Arcade", (Runnable) this::showArcade});
        tiles.add(new Object[]{"🎮", "Rewards", (Runnable) this::openRewards});
        tiles.add(new Object[]{"⚡", "ASTER Core", (Runnable) this::showGxControl});
        tiles.add(new Object[]{"🎨", "Themes", (Runnable) this::showThemes});
        tiles.add(new Object[]{"⚙", "Settings", (Runnable) this::showSettings});
        tiles.add(new Object[]{"⏻", incognito ? "Close private" : "Exit", (Runnable) this::exitBrowser});
        int cols = getResources().getConfiguration().screenWidthDp >= 480 ? 6 : 4;
        LinearLayout row = null;
        for (int i = 0; i < tiles.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
                rl.topMargin = dp(i == 0 ? 10 : 2);
                body.addView(row, rl);
            }
            final Object[] tile = tiles.get(i);
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2), dp(8), dp(2), dp(8));
            cell.setBackground(pressable(Color.TRANSPARENT, 16, 0));
            TextView ic = text((String) tile[0], 20, theme.text);
            ic.setGravity(Gravity.CENTER);
            ic.setBackground(shape(theme.surface, 16, 0));
            cell.addView(ic, new LinearLayout.LayoutParams(dp(50), dp(50)));
            TextView lb = text((String) tile[1], 11, theme.muted);
            lb.setGravity(Gravity.CENTER);
            lb.setMaxLines(2);
            LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, -2);
            ll.topMargin = dp(5);
            cell.addView(lb, ll);
            onTap(cell, () -> {
                close.run();
                ((Runnable) tile[2]).run();
            });
            row.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
        }
        if (row != null) {
            int rem = tiles.size() % cols;
            if (rem != 0) for (int i = rem; i < cols; i++) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        }
        holder[0] = sheet(body);
        holder[0].show();
    }

    private TextView stripButton(LinearLayout strip, String icon, String label, final boolean enabled, final Runnable action) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.setPadding(0, dp(4), 0, dp(4));
        TextView ic = text(icon, 21, theme.text);
        ic.setGravity(Gravity.CENTER);
        GradientDrawable c = new GradientDrawable();
        c.setShape(GradientDrawable.OVAL);
        c.setColor(theme.surface);
        ic.setBackground(c);
        cell.addView(ic, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView lb = text(label, 10, theme.muted);
        lb.setGravity(Gravity.CENTER);
        lb.setSingleLine(true);
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-2, -2);
        ll.topMargin = dp(4);
        cell.addView(lb, ll);
        cell.setAlpha(enabled ? 1f : 0.35f);
        if (enabled) onTap(cell, action);
        strip.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
        return ic;
    }

    private void toggleChip(LinearLayout chips, String icon, String label, boolean on, final Toggle toggle) {
        final LinearLayout chip = new LinearLayout(this);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12), 0, dp(14), 0);
        final TextView ic = text(icon, 15, theme.text);
        final TextView lb = bold(label, 12, theme.text);
        lb.setPadding(dp(6), 0, 0, 0);
        chip.addView(ic);
        chip.addView(lb);
        styleChip(chip, ic, lb, on);
        onTap(chip, () -> styleChip(chip, ic, lb, toggle.run()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(40));
        lp.rightMargin = dp(8);
        chips.addView(chip, lp);
    }

    private void styleChip(LinearLayout chip, TextView ic, TextView lb, boolean on) {
        chip.setBackground(pressable(on ? Theme.withAlpha(theme.accent, 60) : theme.surface, 20, on ? theme.accent : 0));
        lb.setTextColor(on ? theme.accent : theme.text);
    }

    private void exitBrowser() {
        saveSession();
        finishAndRemoveTask();
    }

    void toggleBookmark() {
        Tab t = current();
        if (t == null || t.start || !Store.isWeb(t.url)) { toast("Open a web page first"); return; }
        if (incognito) { toast("Bookmarks can't be changed from a private window"); return; }
        boolean added = store.toggleBookmark(t.displayTitle(), t.web != null && t.web.getUrl() != null ? t.web.getUrl() : t.url);
        toast(added ? "Bookmarked" : "Bookmark removed");
    }

    private void toggleDesktop() {
        Tab t = current();
        if (t == null) return;
        t.desktop = !t.desktop;
        applySettings(t);
        if (!t.start && t.web != null) t.web.reload();
        toast(t.desktop ? "Desktop site on" : "Desktop site off");
    }

    private void toggleNight() {
        boolean on = !prefs.getBoolean("night_mode", false);
        prefs.edit().putBoolean("night_mode", on).apply();
        applyAllSettings();
        Tab t = current();
        if (t != null && t.web != null && !t.start) {
            if (Build.VERSION.SDK_INT < 29) {
                t.web.evaluateJavascript(on ? Scripts.style("__aster_night", Scripts.nightCss()) : Scripts.removeStyle("__aster_night"), null);
            } else t.web.reload();
        }
        toast(on ? "Night mode on for websites" : "Night mode off");
    }

    private void translate() {
        Tab t = current();
        if (t == null || t.start || !Store.isWeb(t.url)) { toast("Open a web page first"); return; }
        String lang = prefs.getString("translate_language", "es");
        openInNewTab("https://translate.google.com/translate?sl=auto&tl=" + lang + "&u=" + StartPage.enc(t.url), false);
    }

    private void toggleReader() {
        final Tab t = current();
        if (t == null || t.web == null || t.start) { toast("Open an article first"); return; }
        if (t.reader) { t.web.goBack(); return; }
        t.web.evaluateJavascript(Scripts.readerExtract(), result -> {
            try {
                Object decoded = new JSONTokener(result).nextValue();
                JSONObject o = new JSONObject(String.valueOf(decoded));
                if (o.optInt("chars", 0) < 350) { toast("No readable article found on this page"); return; }
                String html = readerHtml(o);
                t.readerPending = true;
                String base = o.optString("url", t.url);
                t.web.loadDataWithBaseURL(base, html, "text/html", "UTF-8", base);
            } catch (Exception e) {
                toast("Reader mode could not parse this page");
            }
        });
    }

    private String readerHtml(JSONObject o) {
        boolean dark = !"light".equals(prefs.getString("reader_theme", "dark"));
        String bg = dark ? "#16141c" : "#f6f1e7";
        String fg = dark ? "#e9e6ef" : "#2a2520";
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>")
            .append(StartPage.esc(o.optString("title"))).append("</title><style>")
            .append("body{margin:0;background:").append(bg).append(";color:").append(fg).append(";font:19px/1.75 Georgia,'Noto Serif',serif}")
            .append("main{max-width:700px;margin:0 auto;padding:28px 22px 80px}h1{font:800 30px/1.25 system-ui,sans-serif;margin:8px 0 6px}")
            .append(".site{font:12px system-ui,sans-serif;letter-spacing:2px;text-transform:uppercase;color:").append(Theme.css(theme.accent)).append("}")
            .append("h2{font:700 22px/1.3 system-ui,sans-serif;margin:30px 0 8px}img{max-width:100%;height:auto;border-radius:12px;margin:12px 0}")
            .append("blockquote{border-left:4px solid ").append(Theme.css(theme.accent)).append(";margin:18px 0;padding:2px 16px;opacity:.9}")
            .append("pre{white-space:pre-wrap;font:14px monospace;background:#0003;padding:12px;border-radius:10px}li{margin:6px 0}")
            .append(".bar{position:fixed;right:14px;bottom:14px;display:flex;gap:8px}.bar button{width:46px;height:46px;border-radius:50%;border:0;font:700 16px system-ui;color:#fff;background:")
            .append(Theme.css(theme.accent)).append("}</style></head><body><main><div class='site'>")
            .append(StartPage.esc(o.optString("site"))).append(" · ASTER READER</div><h1>").append(StartPage.esc(o.optString("title"))).append("</h1>");
        JSONArray blocks = o.optJSONArray("blocks");
        boolean inList = false;
        if (blocks != null) for (int i = 0; i < blocks.length(); i++) {
            JSONObject b = blocks.optJSONObject(i);
            if (b == null) continue;
            String type = b.optString("t");
            String v = StartPage.esc(b.optString("v"));
            if ("li".equals(type) && !inList) { h.append("<ul>"); inList = true; }
            if (!"li".equals(type) && inList) { h.append("</ul>"); inList = false; }
            switch (type) {
                case "h": h.append("<h2>").append(v).append("</h2>"); break;
                case "img": if (v.startsWith("https://")) h.append("<img loading='lazy' src='").append(v).append("'>"); break;
                case "li": h.append("<li>").append(v).append("</li>"); break;
                case "q": h.append("<blockquote>").append(v).append("</blockquote>"); break;
                case "pre": h.append("<pre>").append(v).append("</pre>"); break;
                default: h.append("<p>").append(v).append("</p>"); break;
            }
        }
        if (inList) h.append("</ul>");
        h.append("</main><div class='bar'><button onclick=\"z(-1)\">A-</button><button onclick=\"z(1)\">A+</button></div>")
            .append("<script>var s=19;function z(d){s=Math.max(14,Math.min(30,s+d*2));document.body.style.fontSize=s+'px';}</script></body></html>");
        return h.toString();
    }

    private void addToHome() {
        Tab t = current();
        if (t == null || t.start || !Store.isWeb(t.url)) { toast("Open a web page first"); return; }
        if (Build.VERSION.SDK_INT < 26) { toast("Requires Android 8 or newer"); return; }
        ShortcutManager sm = getSystemService(ShortcutManager.class);
        if (sm == null || !sm.isRequestPinShortcutSupported()) { toast("Your launcher doesn't support shortcuts"); return; }
        String url = t.web != null && t.web.getUrl() != null ? t.web.getUrl() : t.url;
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        open.setClass(this, MainActivity.class);
        Bitmap icon = shortcutIcon(t);
        ShortcutInfo info = new ShortcutInfo.Builder(this, "aster-" + Math.abs(url.hashCode()))
            .setShortLabel(t.displayTitle().length() > 24 ? t.displayTitle().substring(0, 24) : t.displayTitle())
            .setIcon(Icon.createWithBitmap(icon))
            .setIntent(open)
            .build();
        sm.requestPinShortcut(info, null);
    }

    private Bitmap shortcutIcon(Tab t) {
        int size = dp(96);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(theme.surface2);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        if (t.favicon != null) {
            int inner = (int) (size * 0.56f);
            Bitmap scaled = Bitmap.createScaledBitmap(t.favicon, inner, inner, true);
            c.drawBitmap(scaled, (size - inner) / 2f, (size - inner) / 2f, null);
        } else {
            p.setColor(theme.accent);
            p.setTextSize(size * 0.48f);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            String letter = Store.hostOf(t.url).isEmpty() ? "A" : Store.hostOf(t.url).substring(0, 1).toUpperCase(Locale.ROOT);
            c.drawText(letter, size / 2f, size / 2f - (p.descent() + p.ascent()) / 2f, p);
        }
        return b;
    }

    // ================================================================== tab switcher

    void showTabs() {
        final Page page = new Page(I18n.t(incognito ? "Private tabs" : "Tabs") + " · " + tabs.size(), true);
        page.action("Close all", () -> new I18n.Builder(this).setTitle("Close all tabs?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Close all", (d, w) -> { page.dismiss(); closeAllTabs(); }).show());
        captureThumb(current());
        buildTabGrid(page);
        page.bottom.setVisibility(View.VISIBLE);
        TextView add = bold("+  " + I18n.t("New tab"), 15, Color.WHITE);
        add.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{theme.accent, theme.accent2});
        g.setCornerRadius(dp(22));
        add.setBackground(g);
        onTap(add, () -> { page.dismiss(); openInNewTab(null, false); });
        page.bottom.addView(add, new LinearLayout.LayoutParams(0, dp(48), 1));
        TextView priv = bold(incognito ? "Private ✓" : "👁  Private", 14, theme.text);
        priv.setGravity(Gravity.CENTER);
        priv.setBackground(pressable(theme.surface, 22, 0));
        onTap(priv, () -> { page.dismiss(); openPrivate(null); });
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(dp(130), dp(48));
        pl.leftMargin = dp(10);
        page.bottom.addView(priv, pl);
        page.show();
    }

    private void buildTabGrid(final Page page) {
        page.body.removeAllViews();
        int cols = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        int screen = getResources().getDisplayMetrics().widthPixels - dp(28);
        int cardW = (screen - dp(10) * (cols - 1)) / cols;
        int cardH = (int) (cardW * 1.32f);
        LinearLayout row = null;
        for (int i = 0; i < tabs.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(this);
                LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, -2);
                rl.bottomMargin = dp(10);
                page.body.addView(row, rl);
            }
            final Tab t = tabs.get(i);
            final int index = i;
            FrameLayout card = new FrameLayout(this);
            card.setClipToOutline(true);
            boolean isActive = i == active;
            card.setBackground(shape(theme.surface, 18, isActive ? theme.accent : Theme.withAlpha(theme.muted, 50)));
            if (isActive) ((GradientDrawable) card.getBackground()).setStroke(dp(2), theme.accent);
            if (isActive) {
                final View activeRow = row;
                page.scroll.post(() -> page.scroll.scrollTo(0, Math.max(0, activeRow.getTop() - dp(40))));
            }
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (t.thumbnail != null && !t.start) img.setImageBitmap(t.thumbnail);
            else {
                GradientDrawable ph = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Theme.withAlpha(theme.accent, 90), theme.surface2});
                img.setImageDrawable(ph);
            }
            FrameLayout.LayoutParams il = new FrameLayout.LayoutParams(-1, -1);
            il.topMargin = dp(38);
            card.addView(img, il);
            LinearLayout top = new LinearLayout(this);
            top.setGravity(Gravity.CENTER_VERTICAL);
            top.setPadding(dp(10), 0, dp(2), 0);
            top.setBackgroundColor(isActive ? Theme.withAlpha(theme.accent, 60) : theme.surface2);
            TextView title = text((t.sleeping() && !t.start ? "💤 " : "") + t.displayTitle(), 12, theme.text);
            title.setSingleLine(true);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
            TextView x = chromeButton("✕", 14, "Close tab");
            x.setTextColor(theme.muted);
            top.addView(x, new LinearLayout.LayoutParams(dp(36), dp(36)));
            card.addView(top, new FrameLayout.LayoutParams(-1, dp(38), Gravity.TOP));
            if (t.start) {
                TextView star = bold("✦", 34, theme.accent);
                star.setGravity(Gravity.CENTER);
                FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-1, -1);
                sl.topMargin = dp(38);
                card.addView(star, sl);
            }
            x.setOnClickListener(v -> {
                feedback(v);
                closeTab(index);
                buildTabGrid(page);
            });
            card.setOnTouchListener(new SwipeToClose(card, () -> {
                closeTab(index);
                buildTabGrid(page);
            }, () -> {
                page.dismiss();
                select(index);
            }));
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(cardW, cardH);
            if (i % cols != 0) cl.leftMargin = dp(10);
            row.addView(card, cl);
        }
    }

    /** Horizontal swipe on a tab card closes it, a tap selects it. */
    private final class SwipeToClose implements View.OnTouchListener {
        private final View card;
        private final Runnable onClose;
        private final Runnable onTapAction;
        private float sx;
        private float sy;
        private boolean dragging;

        SwipeToClose(View card, Runnable onClose, Runnable onTap) {
            this.card = card;
            this.onClose = onClose;
            this.onTapAction = onTap;
        }

        @Override public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    sx = e.getRawX();
                    sy = e.getRawY();
                    dragging = false;
                    card.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - sx;
                    float dy = e.getRawY() - sy;
                    if (!dragging && Math.abs(dx) > dp(14) && Math.abs(dx) > Math.abs(dy)) {
                        dragging = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    if (dragging) {
                        card.setTranslationX(dx);
                        card.setAlpha(Math.max(0.2f, 1f - Math.abs(dx) / card.getWidth()));
                    }
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    float dx = e.getRawX() - sx;
                    float dy = e.getRawY() - sy;
                    card.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                    if (dragging && Math.abs(dx) > card.getWidth() * 0.38f) {
                        feedback(card);
                        card.animate().translationX(dx > 0 ? card.getWidth() * 1.5f : -card.getWidth() * 1.5f).alpha(0f)
                            .setDuration(160).withEndAction(onClose).start();
                    } else if (dragging) {
                        card.animate().translationX(0).alpha(1f).setDuration(160).start();
                    } else if (Math.abs(dx) < dp(10) && Math.abs(dy) < dp(10)) {
                        feedback(card);
                        onTapAction.run();
                    }
                    return true;
                }
                case MotionEvent.ACTION_CANCEL:
                    card.animate().translationX(0).alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
                    return true;
                default:
                    return false;
            }
        }
    }

    // ================================================================== shields panel

    void showShields() {
        final Tab t = current();
        final String host = t == null || t.start ? null : t.topHost;
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(8), dp(18), dp(18));
        final Sheet[] d = new Sheet[1];

        TextView brand = bold("⛨  ASTER HALO", 12, theme.accent);
        brand.setLetterSpacing(0.15f);
        body.addView(brand);
        TextView site = bold(host == null ? "New tab" : host, 21, theme.text);
        site.setPadding(0, dp(6), 0, 0);
        body.addView(site);

        final boolean up = shields.shieldsUp(host);
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusRow.setPadding(dp(16), dp(14), dp(12), dp(14));
        statusRow.setBackground(shape(up ? Theme.withAlpha(theme.accent, 40) : theme.surface, 18, up ? Theme.withAlpha(theme.accent, 120) : 0));
        LinearLayout.LayoutParams srl = new LinearLayout.LayoutParams(-1, -2);
        srl.topMargin = dp(14);
        body.addView(statusRow, srl);
        TextView status = bold(!shields.enabledGlobally() ? "Halo is off everywhere" : (up ? "Halo is ON for this site" : "Halo is OFF for this site"), 15, up ? theme.accent : theme.muted);
        statusRow.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        Switch sw = new Switch(this);
        sw.setChecked(up);
        sw.setEnabled(host != null && shields.enabledGlobally());
        sw.setThumbTintList(ColorStateList.valueOf(theme.accent));
        statusRow.addView(sw);
        sw.setOnCheckedChangeListener((b, checked) -> {
            feedback(b);
            shields.setSite(host, "up", checked);
            d[0].dismiss();
            reload();
            ui.postDelayed(this::showShields, 350);
        });

        int ads = t == null ? 0 : t.blockedAds.get();
        int trk = t == null ? 0 : t.blockedTrackers.get();
        LinearLayout stats = new LinearLayout(this);
        LinearLayout.LayoutParams stl = new LinearLayout.LayoutParams(-1, -2);
        stl.topMargin = dp(12);
        body.addView(stats, stl);
        statTile(stats, String.valueOf(ads + trk), "Blocked on this page");
        statTile(stats, StartPage.formatCount(shields.totalBlocked()), "Blocked all time");
        statTile(stats, StartPage.formatBytes(shields.totalBlocked() * ShieldEngine.BYTES_PER_BLOCK), "Data saved");
        TextView detail = text(I18n.f("%d ads · %d trackers on this page", ads, trk), 12, theme.muted);
        detail.setPadding(dp(4), dp(8), 0, 0);
        body.addView(detail);

        if (host != null && up) {
            sectionTitle(body, I18n.f("Advanced controls for %s", host));
            shieldToggle(body, "Block fingerprinting", "Adds noise to canvas, audio and hardware details", host, "fp", shields.fingerprintDefault(), d);
            shieldToggle(body, "Hide ad elements", "Cosmetic filtering of ad slots and banners", host, "cosmetic", shields.cosmeticEnabled(), d);
            shieldToggle(body, "Block scripts", "Disables JavaScript on this site (may break pages)", host, "scripts", false, d);
            shieldToggle(body, "Allow third-party cookies", "Fixes sign-in on sites that need it", host, "cookies3p", false, d);
        }
        sectionTitle(body, "Global");
        rowCard(body, "Ads & trackers blocking", shields.aggressive() ? "Aggressive — also blocks first-party ads" : "Standard — blocks third-party ads & trackers", () -> {
            d[0].dismiss();
            choose("Blocking mode", new String[]{"Standard", "Aggressive"}, shields.aggressive() ? 1 : 0, w -> {
                prefs.edit().putString("shield_mode", w == 1 ? "aggressive" : "standard").apply();
                reload();
            });
        });
        rowCard(body, "Blocked requests on this page", "See which domains were stopped", () -> { d[0].dismiss(); showBlockedList(t); });
        String updated = shields.lastUpdate() == 0 ? I18n.t("never") : android.text.format.DateUtils.getRelativeTimeSpanString(shields.lastUpdate()).toString();
        rowCard(body, shields.isUpdating() ? "Updating filter lists…" : "Update filter lists",
            I18n.f("%s network rules · %s cosmetic · updated %s", StartPage.formatCount(shields.ruleCount()), StartPage.formatCount(shields.cosmeticCount()), updated), () -> {
                d[0].dismiss();
                updateFilters();
            });
        rowCard(body, shields.enabledGlobally() ? "Turn Halo off everywhere" : "Turn Halo on everywhere", null, () -> {
            prefs.edit().putBoolean("shield_enabled", !shields.enabledGlobally()).apply();
            d[0].dismiss();
            reload();
        });
        TextView note = text("ASTER runs on Android System WebView. Network and cosmetic rules block most ads and trackers, but video ads served from the same servers as the video (e.g. some YouTube ads) can't always be removed.", 11, theme.muted);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        body.addView(note);
        d[0] = sheet(body);
        d[0].show();
    }

    private void updateFilters() {
        if (incognito) { toast("Update filters from a normal window"); return; }
        toast("Updating EasyList, EasyPrivacy & AdGuard filters…");
        shields.updateAsync((ok, rules) -> ui.post(() -> {
            toast(ok ? I18n.f("Filters updated: %s rules", StartPage.formatCount(rules)) : "Couldn't update filters — keeping existing rules");
            refreshShieldChrome();
        }));
    }

    private void shieldToggle(LinearLayout body, String title, String subtitle, final String host, final String key, boolean def, final Sheet[] d) {
        LinearLayout row = rowCard(body, title, subtitle, null);
        Switch sw = new Switch(this);
        sw.setChecked(shields.siteFlag(host, key, def));
        sw.setThumbTintList(ColorStateList.valueOf(theme.accent));
        row.addView(sw);
        row.setOnClickListener(v -> sw.toggle());
        sw.setOnCheckedChangeListener((b, checked) -> {
            feedback(b);
            shields.setSite(host, key, checked);
            reload();
        });
    }

    private void statTile(LinearLayout parent, String value, String label) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(6), dp(12), dp(6), dp(12));
        c.setBackground(shape(theme.surface, 16, 0));
        TextView v = bold(value, 20, theme.accent);
        v.setGravity(Gravity.CENTER);
        c.addView(v);
        TextView l = text(label, 10, theme.muted);
        l.setGravity(Gravity.CENTER);
        c.addView(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(c, lp);
    }

    private void showBlockedList(Tab t) {
        StringBuilder sb = new StringBuilder();
        if (t != null) {
            synchronized (t.blockedHosts) {
                for (Map.Entry<String, Integer> e : t.blockedHosts.entrySet()) sb.append(e.getKey()).append("  ·  ").append(e.getValue()).append('\n');
            }
        }
        new I18n.Builder(this).setTitle("Blocked on this page")
            .setMessage(sb.length() == 0 ? "Nothing blocked on this page yet." : sb.toString())
            .setPositiveButton("OK", null).show();
    }

    // ================================================================== ASTER Core

    void showGxControl() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(8), dp(18), dp(18));
        final Sheet[] d = new Sheet[1];
        TextView title = bold("⚡  ASTER CORE", 13, theme.accent);
        title.setLetterSpacing(0.15f);
        body.addView(title);

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        long appMb = 0;
        long availMb = 0;
        long totalMb = 0;
        if (am != null) {
            try {
                Debug.MemoryInfo[] mi = am.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
                if (mi != null && mi.length > 0) appMb = mi[0].getTotalPss() / 1024L;
            } catch (Exception ignored) { }
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            availMb = info.availMem / (1024L * 1024L);
            totalMb = info.totalMem / (1024L * 1024L);
        }
        sectionTitle(body, "RAM");
        LinearLayout ram = new LinearLayout(this);
        ram.setOrientation(LinearLayout.VERTICAL);
        ram.setPadding(dp(16), dp(14), dp(16), dp(14));
        ram.setBackground(shape(theme.surface, 18, 0));
        body.addView(ram);
        ram.addView(bold(I18n.f("ASTER: %d MB", appMb), 22, theme.text));
        ram.addView(text(I18n.f("Device: %d MB used of %d MB · %d of %d tabs active", totalMb - availMb, totalMb, liveTabs(), tabs.size()), 12, theme.muted));
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(totalMb == 0 ? 0 : (int) ((totalMb - availMb) * 100 / totalMb));
        bar.setProgressTintList(ColorStateList.valueOf(theme.accent));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, dp(8));
        bl.topMargin = dp(10);
        ram.addView(bar, bl);

        switchRow(body, "Memory saver", "Puts the least-used tabs to sleep to free memory", "gx_ram_limiter", true, this::enforceLimit);
        final TextView limitLabel = text(I18n.f("Max active tabs: %d", prefs.getInt("gx_max_live", 5)), 13, theme.text);
        limitLabel.setPadding(dp(6), dp(6), 0, 0);
        body.addView(limitLabel);
        SeekBar seek = new SeekBar(this);
        seek.setMax(9);
        seek.setProgress(prefs.getInt("gx_max_live", 5) - 1);
        seek.setProgressTintList(ColorStateList.valueOf(theme.accent));
        seek.setThumbTintList(ColorStateList.valueOf(theme.accent));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                limitLabel.setText(I18n.f("Max active tabs: %d", p + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) {
                prefs.edit().putInt("gx_max_live", s.getProgress() + 1).apply();
                enforceLimit();
            }
        });
        body.addView(seek, new LinearLayout.LayoutParams(-1, dp(40)));

        sectionTitle(body, "Open tabs");
        List<Tab> sorted = new ArrayList<>(tabs);
        Collections.sort(sorted, new Comparator<Tab>() {
            @Override public int compare(Tab a, Tab b) {
                if (a.sleeping() != b.sleeping()) return a.sleeping() ? 1 : -1;
                return Long.compare(b.lastUsed, a.lastUsed);
            }
        });
        int shown = 0;
        for (final Tab t : sorted) {
            if (shown++ >= 12) break;
            String state = t == current() ? "Active" : (t.sleeping() ? "Sleeping 💤" : "Running in background");
            LinearLayout row = rowCard(body, t.displayTitle(), state, null);
            if (t != current() && !t.sleeping()) {
                TextView sleep = bold("Sleep", 12, theme.accent);
                sleep.setPadding(dp(10), dp(8), dp(10), dp(8));
                onTap(sleep, () -> { sleepTab(t); d[0].dismiss(); showGxControl(); });
                row.addView(sleep);
            }
            TextView kill = bold("✕", 15, theme.muted);
            kill.setPadding(dp(10), dp(8), dp(6), dp(8));
            onTap(kill, () -> { int i = tabs.indexOf(t); if (i >= 0) closeTab(i); d[0].dismiss(); showGxControl(); });
            row.addView(kill);
        }
        rowCard(body, "Sleep all background tabs", null, () -> {
            for (Tab t : new ArrayList<>(tabs)) sleepTab(t);
            d[0].dismiss();
            toast("Background tabs are sleeping");
        });

        sectionTitle(body, "Network");
        switchRow(body, "Data saver", "Don't load images — faster pages on slow or metered networks", "data_saver", false, () -> {
            applyAllSettings();
            reload();
        });
        rowCard(body, "Clear web cache", "Free storage used by cached pages", () -> {
            Tab t = current();
            if (t != null && t.web != null) t.web.clearCache(true);
            toast("Cache cleared");
        });
        TextView note = text("Android doesn't let apps cap CPU or bandwidth, so ASTER Core manages memory by sleeping tabs and saves data by skipping images. WebView renderer memory is reported separately by Android.", 11, theme.muted);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        body.addView(note);
        d[0] = sheet(body);
        d[0].show();
    }

    // ================================================================== themes

    void showThemes() {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(16), dp(8), dp(16), dp(18));
        final Sheet[] d = new Sheet[1];
        TextView title = bold("🎨  THEMES", 13, theme.accent);
        title.setLetterSpacing(0.15f);
        body.addView(title);
        for (final Theme t : Theme.ALL) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackground(pressable(t.id.equals(theme.id) ? Theme.withAlpha(t.accent, 50) : theme.surface, 16, t.id.equals(theme.id) ? t.accent : 0));
            View swatch = new View(this);
            GradientDrawable sw = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{t.accent, t.accent2});
            sw.setShape(GradientDrawable.OVAL);
            sw.setStroke(dp(3), t.bg);
            swatch.setBackground(sw);
            row.addView(swatch, new LinearLayout.LayoutParams(dp(34), dp(34)));
            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setPadding(dp(12), 0, 0, 0);
            texts.addView(bold(t.name, 15, theme.text));
            texts.addView(text(t.family, 11, theme.muted));
            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            if (t.id.equals(theme.id)) row.addView(bold("✓", 18, t.accent));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = dp(8);
            body.addView(row, lp);
            onTap(row, () -> { d[0].dismiss(); setTheme(t); });
        }
        sectionTitle(body, "Start page");
        rowCard(body, "Wallpaper", wallpaperName(), () -> { d[0].dismiss(); chooseWallpaper(); });
        rowCard(body, "Navigation style", fabMode() ? "Orbit button" : "Bottom toolbar", () -> { d[0].dismiss(); chooseNavStyle(); });
        d[0] = sheet(body);
        d[0].show();
    }

    private String wallpaperName() {
        String id = prefs.getString("home_wallpaper", theme.defaultWallpaper());
        for (int i = 0; i < StartPage.WALLPAPER_IDS.length; i++) if (StartPage.WALLPAPER_IDS[i].equals(id)) return StartPage.WALLPAPER_NAMES[i];
        return StartPage.WALLPAPER_NAMES[0];
    }

    private void chooseWallpaper() {
        String id = prefs.getString("home_wallpaper", theme.defaultWallpaper());
        int checked = 0;
        for (int i = 0; i < StartPage.WALLPAPER_IDS.length; i++) if (StartPage.WALLPAPER_IDS[i].equals(id)) checked = i;
        choose("Start page wallpaper", StartPage.WALLPAPER_NAMES, checked, w -> {
            prefs.edit().putString("home_wallpaper", StartPage.WALLPAPER_IDS[w]).apply();
            refreshStartPages();
        });
    }

    private void chooseNavStyle() {
        choose("Navigation style", new String[]{"Bottom toolbar", "Orbit button"}, fabMode() ? 1 : 0, w -> {
            prefs.edit().putString("nav_style", w == 1 ? "fab" : "bar").apply();
            layoutChrome();
            if (w == 1) toast("Tap the button for actions · swipe ← → for back/forward · swipe ↑ for tabs");
        });
    }

    // ================================================================== settings

    void showSettings() {
        final Page p = new Page("Settings");
        LinearLayout b = p.body;

        sectionTitle(b, "Appearance");
        rowCard(b, "Theme", theme.name, () -> { p.dismiss(); showThemes(); });
        rowCard(b, "Navigation style", fabMode() ? "Orbit button" : "Bottom toolbar", () -> { p.dismiss(); chooseNavStyle(); });
        rowCard(b, "Address bar position", addressBottom() ? "Bottom" : "Top", () -> {
            p.dismiss();
            choose("Address bar position", new String[]{"Top", "Bottom"}, addressBottom() ? 1 : 0, w -> {
                prefs.edit().putString("address_position", w == 1 ? "bottom" : "top").apply();
                layoutChrome();
            });
        });
        rowCard(b, "Start page", "Wallpaper, stats, clock, speed dial, Arcade", () -> { p.dismiss(); showStartCustomization(); });
        rowCard(b, "Text size", prefs.getInt("text_zoom", 100) + "%", () -> {
            p.dismiss();
            final int[] sizes = {85, 100, 115, 130, 150, 175};
            String[] labels = new String[sizes.length];
            int checked = 1;
            for (int i = 0; i < sizes.length; i++) { labels[i] = sizes[i] + "%"; if (sizes[i] == prefs.getInt("text_zoom", 100)) checked = i; }
            choose("Text size", labels, checked, w -> { prefs.edit().putInt("text_zoom", sizes[w]).apply(); applyAllSettings(); });
        });
        switchRow(b, "Night mode for websites", "Darkens light web pages", "night_mode", false, () -> { applyAllSettings(); reload(); });
        switchRow(b, "Interface sounds", "Interface sound effects", "gx_sounds", true, () -> {
            if (prefs.getBoolean("gx_sounds", true)) { if (sounds == null) sounds = new UiSounds(); }
            else if (sounds != null) { sounds.release(); sounds = null; }
        });
        switchRow(b, "Haptic feedback", "Vibrate lightly on taps", "gx_haptics", true, null);

        rowCard(b, "Language", I18n.languageName(prefs.getString("language", "auto")), () -> {
            p.dismiss();
            final String[] codes = {"auto", "es", "en"};
            String[] names = {I18n.languageName("auto"), I18n.languageName("es"), I18n.languageName("en")};
            int checked = 0;
            for (int i = 0; i < codes.length; i++) if (codes[i].equals(prefs.getString("language", "auto"))) checked = i;
            choose("Language", names, checked, w -> {
                prefs.edit().putString("language", codes[w]).apply();
                saveSession();
                recreate();
            });
        });

        sectionTitle(b, "Search");
        rowCard(b, "Search engine", StartPage.engineName(prefs.getString("search_engine", "brave")), () -> {
            p.dismiss();
            choose("Search engine", StartPage.ENGINE_NAMES, StartPage.engineIndex(prefs.getString("search_engine", "brave")), w -> {
                prefs.edit().putString("search_engine", StartPage.ENGINE_IDS[w]).apply();
                refreshStartPages();
            });
        });
        switchRow(b, "Search suggestions", "Ask your search engine for suggestions while typing", "suggestions", !incognito, null);

        sectionTitle(b, "Halo & privacy");
        switchRow(b, "Halo", "Block ads and trackers on all sites", "shield_enabled", true, this::refreshShieldChrome);
        rowCard(b, "Blocking mode", shields.aggressive() ? "Aggressive" : "Standard", () -> {
            p.dismiss();
            choose("Blocking mode", new String[]{"Standard", "Aggressive"}, shields.aggressive() ? 1 : 0,
                w -> prefs.edit().putString("shield_mode", w == 1 ? "aggressive" : "standard").apply());
        });
        switchRow(b, "Hide ad elements", "Cosmetic filtering from EasyList & AdGuard", "shield_cosmetic", true, null);
        switchRow(b, "Block fingerprinting", "Randomize canvas, audio and hardware signals", "shield_fingerprint", true, null);
        rowCard(b, "Update filter lists", I18n.f("%s network rules", StartPage.formatCount(shields.ruleCount())), this::updateFilters);
        rowCard(b, "Per-site Halo", I18n.f("%d customized sites", shields.customizedSites().size()), () -> { p.dismiss(); showShieldSites(); });
        rowCard(b, "Site permissions", "Camera, microphone and location", () -> { p.dismiss(); showSitePermissions(); });
        rowCard(b, "Reset Halo statistics", I18n.f("%s blocked so far", StartPage.formatCount(shields.totalBlocked())), () -> {
            shields.resetStats();
            refreshStartPages();
            toast("Statistics reset");
        });
        rowCard(b, "Clear browsing data", "History, cookies, site data and cache", () -> { p.dismiss(); clearDataDialog(); });
        switchRow(b, "Clear site data on exit", "Wipe cookies and cache when you exit ASTER", "clear_on_exit", false, null);

        sectionTitle(b, "Browsing");
        switchRow(b, "Restore tabs on start", null, "restore_tabs", true, null);
        switchRow(b, "Desktop site by default", null, "desktop_default", false, null);
        switchRow(b, "Pull to refresh", null, "pull_refresh", true, this::applyAllSettings);
        switchRow(b, "Hide toolbar while scrolling", "More room for pages; scroll up to bring it back", "hide_bars_scroll", true, null);
        switchRow(b, "Background media playback", "Keep audio and video playing when ASTER is in the background", "background_play", false, null);
        switchRow(b, "Picture-in-picture", "Full-screen videos keep playing in a small window when you leave ASTER", "pip", true, null);

        sectionTitle(b, "YouTube & Rewards");
        switchRow(b, "Remove YouTube ads", "Experimental: strips ad data from the player and blocks ad endpoints", "yt_adblock", true, null);
        rowCard(b, "Microsoft Rewards", "Open your Rewards dashboard (search with Bing while signed in to earn points)", () -> { p.dismiss(); openRewards(); });
        rowCard(b, "Translation language", translationName(prefs.getString("translate_language", "es")), () -> {
            p.dismiss();
            final String[] codes = {"es", "en", "sl", "it", "fr", "de", "pt", "ja", "ko", "zh-CN"};
            String[] names = new String[codes.length];
            int checked = 0;
            for (int i = 0; i < codes.length; i++) { names[i] = translationName(codes[i]); if (codes[i].equals(prefs.getString("translate_language", "es"))) checked = i; }
            choose("Translate pages to", names, checked, w -> prefs.edit().putString("translate_language", codes[w]).apply());
        });
        rowCard(b, "Reader mode style", "dark".equals(prefs.getString("reader_theme", "dark")) ? "Dark" : "Light (sepia)", () -> {
            p.dismiss();
            choose("Reader style", new String[]{"Dark", "Light (sepia)"}, "dark".equals(prefs.getString("reader_theme", "dark")) ? 0 : 1,
                w -> prefs.edit().putString("reader_theme", w == 0 ? "dark" : "light").apply());
        });

        sectionTitle(b, "Data");
        rowCard(b, "Backup & restore", "Bookmarks, history, speed dial, settings and tabs", () -> { p.dismiss(); showBackup(); });
        rowCard(b, "Set as default browser", "Open Android's default apps settings", () -> {
            try { startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)); }
            catch (Exception e) { toast("Open Android Settings → Apps → Default apps"); }
        });

        sectionTitle(b, "About");
        rowCard(b, "ASTER Browser " + BuildConfig.VERSION_NAME, I18n.f("Engine: Android System WebView · %s", Build.MODEL), null);
        rowCard(b, "Check for updates", "Looks for new releases on GitHub", () -> { if (updater != null) updater.check(true); });
        rowCard(b, "Source code", Updater.REPO_URL.replace("https://", ""), () -> { p.dismiss(); openInNewTab(Updater.REPO_URL, false); });
        p.show();
    }

    private String translationName(String code) {
        switch (code) {
            case "en": return "English";
            case "sl": return "Slovenščina";
            case "it": return "Italiano";
            case "fr": return "Français";
            case "de": return "Deutsch";
            case "pt": return "Português";
            case "ja": return "日本語";
            case "ko": return "한국어";
            case "zh-CN": return "中文";
            default: return "Español";
        }
    }

    private void showStartCustomization() {
        final String[] keys = {"home_show_stats", "home_show_clock", "home_show_shortcuts", "home_show_bookmarks", "home_show_gx_corner"};
        final String[] labels = {"Halo statistics", "Clock & date", "Speed dial", "Bookmarks", "Arcade (free games)"};
        final boolean[] checked = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) checked[i] = prefs.getBoolean(keys[i], true);
        new I18n.Builder(this).setTitle("Customize start page")
            .setMultiChoiceItems(labels, checked, (d, w, on) -> checked[w] = on)
            .setNeutralButton("Wallpaper…", (d, w) -> chooseWallpaper())
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                SharedPreferences.Editor e = prefs.edit();
                for (int i = 0; i < keys.length; i++) e.putBoolean(keys[i], checked[i]);
                e.apply();
                refreshStartPages();
                if (checked[4]) refreshGxCorner(false);
            }).show();
    }

    private void showShieldSites() {
        final List<String> sites = shields.customizedSites();
        if (sites.isEmpty()) {
            new I18n.Builder(this).setTitle("Per-site Halo").setMessage("No sites have custom Halo settings yet.")
                .setPositiveButton("OK", null).show();
            return;
        }
        String[] labels = new String[sites.size()];
        for (int i = 0; i < sites.size(); i++) {
            String h = sites.get(i);
            labels[i] = h + (shields.siteFlag(h, "up", true) ? "" : "  · " + I18n.t("Halo off")) + (shields.siteFlag(h, "scripts", false) ? "  · " + I18n.t("scripts blocked") : "");
        }
        new I18n.Builder(this).setTitle("Per-site Halo")
            .setItems(labels, (d, w) -> new I18n.Builder(this).setTitle(sites.get(w))
                .setMessage("Reset this site to the global Halo settings?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (dd, ww) -> { shields.resetSite(sites.get(w)); toast("Reset"); }).show())
            .setPositiveButton("Close", null).show();
    }

    private void clearDataDialog() {
        final String[] labels = {"Browsing history", "Cookies and site data", "Cached images and files", "Halo statistics"};
        final boolean[] checked = {true, true, true, false};
        new I18n.Builder(this).setTitle("Clear browsing data")
            .setMultiChoiceItems(labels, checked, (d, w, on) -> checked[w] = on)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear", (d, w) -> {
                if (checked[0]) store.clearHistory();
                if (checked[1]) {
                    CookieManager.getInstance().removeAllCookies(null);
                    WebStorage.getInstance().deleteAllData();
                }
                if (checked[2]) for (Tab t : tabs) if (t.web != null) { t.web.clearCache(true); break; }
                if (checked[3]) shields.resetStats();
                refreshStartPages();
                toast("Browsing data cleared");
            }).show();
    }

    private void wipeSiteData() {
        try {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            WebStorage.getInstance().deleteAllData();
            WebView tmp = new WebView(this);
            tmp.clearCache(true);
            tmp.destroy();
        } catch (Exception ignored) { }
    }

    // ================================================================== bookmarks / history / downloads

    void showBookmarks() {
        final Page p = new Page("Bookmarks");
        fillEntries(p, store.bookmarks, true, "");
        p.show();
    }

    void showHistory() {
        final Page p = new Page("History");
        p.action("Clear", () -> new I18n.Builder(this).setTitle("Clear all history?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear", (d, w) -> { store.clearHistory(); p.dismiss(); toast("History cleared"); }).show());
        fillEntries(p, store.history, false, "");
        p.show();
    }

    private void fillEntries(final Page p, final List<Store.Entry> list, final boolean bookmarks, String filter) {
        p.body.removeAllViews();
        final EditText search = input(bookmarks ? "Search bookmarks" : "Search history", filter);
        search.setBackground(shape(theme.surface, 16, 0));
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2);
        sl.bottomMargin = dp(10);
        p.body.addView(search, sl);
        final LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        p.body.addView(results);
        final Runnable render = new Runnable() {
            @Override public void run() {
                results.removeAllViews();
                String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
                int shown = 0;
                String lastDay = "";
                for (final Store.Entry e : new ArrayList<>(list)) {
                    if (!q.isEmpty() && !e.url.toLowerCase(Locale.ROOT).contains(q) && !e.label().toLowerCase(Locale.ROOT).contains(q)) continue;
                    if (shown++ >= 300) break;
                    if (!bookmarks && e.time > 0) {
                        String day = android.text.format.DateFormat.getMediumDateFormat(MainActivity.this).format(new java.util.Date(e.time));
                        if (!day.equals(lastDay)) { sectionTitle(results, day); lastDay = day; }
                    }
                    LinearLayout row = rowCard(results, e.label(), Store.hostOf(e.url), () -> { p.dismiss(); openUrlFromList(e.url); });
                    row.setOnLongClickListener(v -> { entryMenu(p, list, bookmarks, e, this); return true; });
                }
                if (shown == 0) {
                    TextView empty = text(bookmarks ? "No bookmarks yet. Tap ☆ in the menu to save a page." : "No history yet.", 13, theme.muted);
                    empty.setPadding(dp(6), dp(20), dp(6), 0);
                    results.addView(empty);
                }
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { render.run(); }
        });
        render.run();
    }

    private void entryMenu(final Page p, final List<Store.Entry> list, final boolean bookmarks, final Store.Entry e, final Runnable refresh) {
        List<String> labels = new ArrayList<>();
        labels.add("Open in new tab");
        labels.add("Open in private tab");
        labels.add("Copy link");
        if (bookmarks) labels.add("Edit");
        labels.add(bookmarks ? "Delete bookmark" : "Remove from history");
        if (!bookmarks) labels.add("Add to speed dial");
        new I18n.Builder(this).setTitle(e.label()).setItems(labels.toArray(new String[0]), (d, w) -> {
            String choice = labels.get(w);
            switch (choice) {
                case "Open in new tab": p.dismiss(); openInNewTab(e.url, false); break;
                case "Open in private tab": openPrivate(e.url); break;
                case "Copy link": copy(e.url); break;
                case "Edit": editBookmark(e, refresh); break;
                case "Add to speed dial":
                    store.speedDial.add(new Store.Entry(e.label(), e.url, 0));
                    store.saveSpeedDial();
                    refreshStartPages();
                    toast("Added to speed dial");
                    break;
                default:
                    if (incognito) { toast("Can't change saved data from a private window"); return; }
                    list.remove(e);
                    if (bookmarks) store.saveBookmarks(); else store.saveHistory();
                    refresh.run();
                    break;
            }
        }).show();
    }

    private void editBookmark(final Store.Entry e, final Runnable refresh) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        final EditText name = input("Name", e.title);
        final EditText url = input("URL", e.url);
        box.addView(name);
        box.addView(url);
        new I18n.Builder(this).setTitle("Edit bookmark").setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String u = webAddressOnly(url.getText().toString());
                if (u == null) { toast("Enter a valid web address"); return; }
                e.title = name.getText().toString().trim();
                e.url = u;
                store.saveBookmarks();
                refresh.run();
            }).show();
    }

    private void openUrlFromList(String url) {
        Tab t = current();
        if (t != null) load(t, url); else openInNewTab(url, false);
    }

    void showDownloads() {
        final Page p = new Page("Downloads");
        p.action("Folder", () -> {
            try { startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); } catch (Exception e) { toast("No downloads app found"); }
        });
        final DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        int shown = 0;
        if (dm != null && !store.downloads.isEmpty()) {
            long[] ids = new long[store.downloads.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = store.downloads.get(i);
            Cursor c = null;
            try {
                c = dm.query(new DownloadManager.Query().setFilterById(ids));
                while (c != null && c.moveToNext()) {
                    final long id = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID));
                    String title = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE));
                    int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    long size = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    String state;
                    switch (status) {
                        case DownloadManager.STATUS_SUCCESSFUL: state = I18n.f("Completed · %s", StartPage.formatBytes(size)); break;
                        case DownloadManager.STATUS_FAILED: state = "Failed"; break;
                        case DownloadManager.STATUS_PAUSED: state = "Paused"; break;
                        default: state = size > 0 ? I18n.f("Downloading %d%% · %s", done * 100 / size, StartPage.formatBytes(size)) : "Downloading…"; break;
                    }
                    final boolean ok = status == DownloadManager.STATUS_SUCCESSFUL;
                    rowCard(p.body, "⬇  " + (title == null ? "File" : title), state, () -> { if (ok) openDownload(dm, id); });
                    shown++;
                }
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.close();
            }
        }
        if (shown == 0) {
            TextView empty = text("Files you download with ASTER will appear here.", 13, theme.muted);
            empty.setPadding(dp(6), dp(20), dp(6), 0);
            p.body.addView(empty);
        }
        p.show();
    }

    private void openDownload(DownloadManager dm, long id) {
        try {
            Uri uri = dm.getUriForDownloadedFile(id);
            String mime = dm.getMimeTypeForDownloadedFile(id);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, mime == null ? "*/*" : mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            toast("No app can open this file");
        }
    }

    // ================================================================== speed dial

    private void showDialManager() {
        final Page p = new Page("Speed dial");
        p.action("Add", () -> { p.dismiss(); editDial(-1); });
        for (int i = 0; i < store.speedDial.size(); i++) {
            final int index = i;
            Store.Entry e = store.speedDial.get(i);
            LinearLayout row = rowCard(p.body, e.label(), Store.hostOf(e.url), () -> { p.dismiss(); editDial(index); });
            if (i > 0) {
                TextView up = bold("↑", 18, theme.muted);
                up.setPadding(dp(10), dp(4), dp(10), dp(4));
                onTap(up, () -> {
                    Collections.swap(store.speedDial, index, index - 1);
                    store.saveSpeedDial();
                    p.dismiss();
                    refreshStartPages();
                    showDialManager();
                });
                row.addView(up);
            }
        }
        p.show();
    }

    void editDial(final int index) {
        if (incognito) { toast("Edit speed dial from a normal window"); return; }
        final Store.Entry existing = index >= 0 && index < store.speedDial.size() ? store.speedDial.get(index) : null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        final EditText name = input("Name", existing == null ? "" : existing.title);
        final EditText url = input("Web address", existing == null ? "" : existing.url);
        box.addView(name);
        box.addView(url);
        AlertDialog.Builder b = new I18n.Builder(this)
            .setTitle(existing == null ? "Add to speed dial" : "Edit shortcut")
            .setView(box)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (d, w) -> {
                String u = webAddressOnly(url.getText().toString());
                if (u == null) { toast("Enter a valid web address"); return; }
                String n = name.getText().toString().trim();
                if (n.isEmpty()) n = Store.hostOf(u);
                if (existing == null) store.speedDial.add(new Store.Entry(n, u, 0));
                else { existing.title = n; existing.url = u; }
                store.saveSpeedDial();
                refreshStartPages();
            });
        if (existing != null) b.setNeutralButton("Remove", (d, w) -> {
            store.speedDial.remove(existing);
            store.saveSpeedDial();
            refreshStartPages();
        });
        b.show();
    }

    // ================================================================== Arcade

    // ================================================================== Arcade screen

    private static final long ARCADE_TTL = 30L * 60L * 1000L;
    private boolean arcadeLoading;

    /** Full Arcade screen: free games per store (Epic, Steam, Xbox, PlayStation, GOG, mobile, DLC) and gaming news. */
    void showArcade() {
        final Page page = new Page("Arcade", true);
        page.scroll.setVisibility(View.GONE);
        final WebView web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(false);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        ws.setTextZoom(prefs.getInt("text_zoom", 100));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("aster://arcade/refresh")) {
                    loadArcadeData(web, true);
                } else if (Store.isWeb(url)) {
                    page.dismiss();
                    openInNewTab(url.replaceFirst("^http://", "https://"), false);
                }
                return true;
            }

            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                page.dismiss();
                return true;
            }
        });
        page.root.addView(web, 2, new LinearLayout.LayoutParams(-1, 0, 1));
        page.onDismiss = () -> {
            try { web.destroy(); } catch (Exception ignored) { }
        };
        String cached = prefs.getString("arcade_cache", "");
        web.loadDataWithBaseURL(ArcadePage.BASE_URL, ArcadePage.build(theme, cached), "text/html", "UTF-8", null);
        page.show();
        long age = System.currentTimeMillis() - prefs.getLong("arcade_time", 0L);
        if (cached.isEmpty() || age > ARCADE_TTL) loadArcadeData(web, false);
    }

    private void loadArcadeData(final WebView web, final boolean manual) {
        if (arcadeLoading) return;
        arcadeLoading = true;
        if (manual) toast("Refreshing Arcade…");
        final String country = Locale.getDefault().getCountry();
        new Thread(() -> {
            JSONObject data = Arcade.fetchAll(I18n.es, country);
            final String json = data.toString();
            boolean ok = data.has("fetched");
            if (ok && !incognito) prefs.edit().putString("arcade_cache", json).putLong("arcade_time", System.currentTimeMillis()).apply();
            final boolean success = ok;
            ui.post(() -> {
                arcadeLoading = false;
                try { web.evaluateJavascript("window.asterUpdate&&asterUpdate(" + json + ");", null); } catch (Exception ignored) { }
                if (!success && manual) toast("Couldn't load Arcade — check your connection");
            });
        }, "ASTER-arcade").start();
    }

    void refreshGxCorner(final boolean force) {
        if (incognito || !prefs.getBoolean("home_show_gx_corner", true)) return;
        long age = System.currentTimeMillis() - prefs.getLong("gx_corner_time", 0L);
        if (!force && age < 6L * 60L * 60L * 1000L && !prefs.getString("gx_corner_cache", "").isEmpty()) return;
        if (force) toast("Refreshing Arcade…");
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                c = (HttpURLConnection) new URL(GX_CORNER_API).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("User-Agent", "ASTER-Browser/" + BuildConfig.VERSION_NAME);
                if (c.getResponseCode() != 200) return;
                String body = new String(Store.readAll(c.getInputStream(), 4 * 1024 * 1024), "UTF-8").trim();
                if (!body.startsWith("[")) return;
                JSONArray all = new JSONArray(body);
                JSONArray slim = new JSONArray();
                for (int i = 0; i < all.length() && slim.length() < 15; i++) {
                    JSONObject g = all.optJSONObject(i);
                    if (g == null) continue;
                    JSONObject o = new JSONObject();
                    o.put("title", g.optString("title"));
                    o.put("worth", g.optString("worth"));
                    o.put("thumbnail", g.optString("thumbnail"));
                    o.put("platforms", g.optString("platforms"));
                    o.put("open_giveaway_url", g.optString("open_giveaway_url"));
                    o.put("gamerpower_url", g.optString("gamerpower_url"));
                    slim.put(o);
                }
                final String json = slim.toString();
                prefs.edit().putString("gx_corner_cache", json).putLong("gx_corner_time", System.currentTimeMillis()).apply();
                ui.post(this::patchStartPages);
            } catch (Exception ignored) {
            } finally {
                if (c != null) c.disconnect();
            }
        }, "ASTER-gx-corner").start();
    }

    // ================================================================== voice

    private void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Search with your voice");
        try { startActivityForResult(i, REQ_VOICE); } catch (ActivityNotFoundException e) { toast("Voice search isn't available on this device"); }
    }

    // ================================================================== backup

    private static final String[] BACKUP_STRINGS = {"ui_theme", "search_engine", "home_wallpaper", "nav_style", "address_position",
        "translate_language", "shield_mode", "reader_theme", "language"};
    private static final String[] BACKUP_BOOLS = {"shield_enabled", "shield_cosmetic", "shield_fingerprint", "home_show_stats",
        "home_show_clock", "home_show_shortcuts", "home_show_bookmarks", "home_show_gx_corner", "night_mode", "gx_sounds",
        "gx_haptics", "suggestions", "restore_tabs", "background_play", "pull_refresh", "desktop_default", "data_saver",
        "gx_ram_limiter", "clear_on_exit", "hide_bars_scroll", "pip", "yt_adblock"};
    private static final String[] BACKUP_INTS = {"text_zoom", "gx_max_live"};

    private void showBackup() {
        if (incognito) { toast("Use a normal window for backups"); return; }
        new I18n.Builder(this).setTitle("Backup & restore")
            .setMessage("Export bookmarks, history, speed dial, Halo settings, preferences and open tabs to a JSON file you control. Cookies and passwords are never included.")
            .setNegativeButton("Import", (d, w) -> {
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try { startActivityForResult(i, REQ_IMPORT); } catch (Exception e) { toast("No file picker available"); }
            })
            .setPositiveButton("Export", (d, w) -> {
                Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE, "aster-backup.json");
                try { startActivityForResult(i, REQ_EXPORT); } catch (Exception e) { toast("No file picker available"); }
            })
            .setNeutralButton("Close", null).show();
    }

    private void exportBackup(Uri uri) {
        try {
            JSONObject b = new JSONObject();
            b.put("format", "ASTER_BROWSER_BACKUP");
            b.put("version", 2);
            b.put("app_version", BuildConfig.VERSION_NAME);
            b.put("exported_at", System.currentTimeMillis());
            b.put("bookmarks", store.toJson(store.bookmarks));
            b.put("history", store.toJson(store.history));
            b.put("speed_dial", store.toJson(store.speedDial));
            b.put("shield_sites", shields.exportSites());
            JSONObject s = new JSONObject();
            for (String k : BACKUP_STRINGS) if (prefs.contains(k)) s.put(k, prefs.getString(k, ""));
            for (String k : BACKUP_BOOLS) if (prefs.contains(k)) s.put(k, prefs.getBoolean(k, false));
            for (String k : BACKUP_INTS) if (prefs.contains(k)) s.put(k, prefs.getInt(k, 0));
            b.put("settings", s);
            JSONArray open = new JSONArray();
            for (Tab t : tabs) if (!t.start && Store.isWeb(t.url)) open.put(t.url);
            b.put("session_tabs", open);
            try (OutputStream out = getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IllegalStateException("Can't write file");
                out.write(b.toString(2).getBytes("UTF-8"));
            }
            toast("Backup exported");
        } catch (Exception e) {
            toast("Backup export failed");
        }
    }

    private void importBackup(Uri uri) {
        try {
            byte[] bytes;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IllegalStateException("Can't read file");
                bytes = Store.readAll(in, 20 * 1024 * 1024);
            }
            final JSONObject b = new JSONObject(new String(bytes, "UTF-8"));
            if (!"ASTER_BROWSER_BACKUP".equals(b.optString("format"))) throw new IllegalStateException("Not an ASTER backup");
            new I18n.Builder(this).setTitle("Restore backup?")
                .setMessage("This replaces your bookmarks, history, speed dial and settings with the backup's contents.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Restore", (d, w) -> applyBackup(b)).show();
        } catch (Exception e) {
            toast("This file isn't a valid ASTER backup");
        }
    }

    private void applyBackup(JSONObject b) {
        int version = b.optInt("version", 1);
        if (version >= 2) {
            store.replace(store.bookmarks, b.optJSONArray("bookmarks"), 10000);
            store.replace(store.history, b.optJSONArray("history"), Store.MAX_HISTORY);
            JSONArray dial = b.optJSONArray("speed_dial");
            if (dial != null) store.replace(store.speedDial, dial, 60);
            shields.importSites(b.optJSONObject("shield_sites"));
        } else {
            store.bookmarks.clear();
            for (String line : b.optString("bookmarks", "").split("\n")) if (Store.isWeb(line.trim())) store.bookmarks.add(new Store.Entry(Store.hostOf(line.trim()), line.trim(), System.currentTimeMillis()));
            store.history.clear();
            for (String line : b.optString("history", "").split("\n")) if (Store.isWeb(line.trim()) && store.history.size() < Store.MAX_HISTORY) store.history.add(new Store.Entry(Store.hostOf(line.trim()), line.trim(), 0));
        }
        store.saveBookmarks();
        store.saveHistory();
        store.saveSpeedDial();
        JSONObject s = b.optJSONObject("settings");
        if (s != null) {
            SharedPreferences.Editor e = prefs.edit();
            for (String k : BACKUP_STRINGS) if (s.has(k)) e.putString(k, s.optString(k));
            for (String k : BACKUP_BOOLS) if (s.has(k)) e.putBoolean(k, s.optBoolean(k));
            for (String k : BACKUP_INTS) if (s.has(k)) e.putInt(k, s.optInt(k));
            e.apply();
        }
        JSONArray open = b.optJSONArray("session_tabs");
        if (open != null) for (int i = 0; i < open.length() && tabs.size() < MAX_TABS; i++) {
            String u = open.optString(i);
            if (Store.isWeb(u)) {
                Tab t = new Tab(u);
                t.title = Store.hostOf(u);
                tabs.add(t);
            }
        }
        theme = Theme.byId(prefs.getString("ui_theme", "aster"));
        applyTheme();
        layoutChrome();
        applyAllSettings();
        refreshStartPages();
        updateChrome();
        toast("Backup restored");
    }

    // ================================================================== downloads

    private void confirmDownload(String url, String agent, String disposition, String mime) {
        if (url == null) return;
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            toast("This page generates its file in the browser; ASTER can't save it yet");
            return;
        }
        dlUrl = url;
        dlAgent = agent;
        dlDisposition = disposition;
        dlMime = mime;
        String name = URLUtil.guessFileName(url, disposition, mime);
        new I18n.Builder(this).setTitle("Download file?")
            .setMessage(name + "\n\n" + I18n.t("Save to your Downloads folder?"))
            .setNegativeButton("Cancel", (d, w) -> dlUrl = null)
            .setPositiveButton("Download", (d, w) -> {
                if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_DOWNLOAD_PERM);
                } else startDownload();
            }).show();
    }

    private void startDownload() {
        if (dlUrl == null) return;
        try {
            String name = URLUtil.guessFileName(dlUrl, dlDisposition, dlMime);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(dlUrl));
            r.setTitle(name);
            r.setDescription("ASTER Browser");
            if (dlMime != null && !dlMime.trim().isEmpty()) r.setMimeType(dlMime);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name);
            r.addRequestHeader("User-Agent", dlAgent != null ? dlAgent : userAgent(false));
            String cookies = CookieManager.getInstance().getCookie(dlUrl);
            if (cookies != null) r.addRequestHeader("Cookie", cookies);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long id = dm.enqueue(r);
            store.addDownload(id);
            toast(I18n.f("Downloading %s", name));
        } catch (Exception e) {
            toast("Could not start download");
        }
        dlUrl = null;
    }

    // ================================================================== site permissions

    private void promptMedia(final PermissionRequest request) {
        if (request == null) return;
        final String origin = request.getOrigin() == null ? "" : request.getOrigin().toString();
        if (!origin.startsWith("https://")) { request.deny(); return; }
        final List<String> approved = new ArrayList<>();
        boolean cam = false;
        boolean mic = false;
        for (String r : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) { approved.add(r); cam = true; }
            else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) { approved.add(r); mic = true; }
            else if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID.equals(r)) { approved.add(r); }
        }
        if (approved.isEmpty()) { request.deny(); return; }
        if (!cam && !mic) { request.grant(approved.toArray(new String[0])); return; }
        final boolean needCam = cam;
        final boolean needMic = mic;
        String what = I18n.t(cam && mic ? "camera and microphone" : (cam ? "camera" : "microphone"));
        new I18n.Builder(this).setTitle("Website permission")
            .setMessage(I18n.f("Allow %s to use your %s?", origin, what))
            .setNegativeButton("Block", (d, w) -> request.deny())
            .setPositiveButton("Allow", (d, w) -> {
                pendingMedia = request;
                pendingMediaOrigin = origin;
                pendingMediaResources = approved.toArray(new String[0]);
                List<String> missing = new ArrayList<>();
                if (needCam && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.CAMERA);
                if (needMic && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO);
                if (!missing.isEmpty()) requestPermissions(missing.toArray(new String[0]), REQ_MEDIA_PERM);
                else grantMedia();
            }).show();
    }

    private void grantMedia() {
        PermissionRequest r = pendingMedia;
        String[] res = pendingMediaResources;
        String origin = pendingMediaOrigin;
        pendingMedia = null;
        pendingMediaResources = null;
        pendingMediaOrigin = null;
        if (r == null || res == null) return;
        List<String> ok = new ArrayList<>();
        for (String s : res) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(s) && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) continue;
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(s) && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) continue;
            ok.add(s);
        }
        if (ok.isEmpty()) { r.deny(); toast("Android permission was not granted"); return; }
        r.grant(ok.toArray(new String[0]));
        for (String s : ok) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(s)) rememberPermission("Camera", origin);
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(s)) rememberPermission("Microphone", origin);
        }
    }

    private void promptLocation(final String origin, final GeolocationPermissions.Callback cb) {
        if (cb == null) return;
        if (origin == null || !origin.startsWith("https://")) { cb.invoke(origin, false, false); return; }
        new I18n.Builder(this).setTitle("Location permission")
            .setMessage(I18n.f("Allow %s to access your location?", origin))
            .setNegativeButton("Block", (d, w) -> cb.invoke(origin, false, false))
            .setPositiveButton("Allow", (d, w) -> {
                pendingGeo = cb;
                pendingGeoOrigin = origin;
                if (hasLocation()) finishLocation(true);
                else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION_PERM);
            }).show();
    }

    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void finishLocation(boolean granted) {
        GeolocationPermissions.Callback cb = pendingGeo;
        String origin = pendingGeoOrigin;
        pendingGeo = null;
        pendingGeoOrigin = null;
        if (cb != null && origin != null) cb.invoke(origin, granted, granted && !incognito);
        if (granted && origin != null) rememberPermission("Location", origin);
    }

    private void rememberPermission(String type, String origin) {
        if (incognito) return;
        java.util.Set<String> set = new java.util.LinkedHashSet<>(prefs.getStringSet("site_permission_log", new java.util.LinkedHashSet<String>()));
        set.add(type + "||" + origin);
        prefs.edit().putStringSet("site_permission_log", set).apply();
    }

    private void showSitePermissions() {
        final List<String> entries = new ArrayList<>(prefs.getStringSet("site_permission_log", new java.util.LinkedHashSet<String>()));
        if (entries.isEmpty()) {
            new I18n.Builder(this).setTitle("Site permissions")
                .setMessage("No website has been given camera, microphone or location access.")
                .setPositiveButton("Android app permissions", (d, w) -> openAppSettings())
                .setNegativeButton("Close", null).show();
            return;
        }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = entries.get(i).replace("||", "  ·  ");
        new I18n.Builder(this).setTitle("Site permissions")
            .setItems(labels, (d, w) -> {
                String[] parts = entries.get(w).split("\\|\\|", 2);
                if ("Location".equals(parts[0]) && parts.length > 1) GeolocationPermissions.getInstance().clear(parts[1]);
                java.util.Set<String> set = new java.util.LinkedHashSet<>(prefs.getStringSet("site_permission_log", new java.util.LinkedHashSet<String>()));
                set.remove(entries.get(w));
                prefs.edit().putStringSet("site_permission_log", set).apply();
                toast("Permission removed");
            })
            .setNeutralButton("Android settings", (d, w) -> openAppSettings())
            .setPositiveButton("Close", null).show();
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            toast("Open ASTER in Android Settings");
        }
    }

    // ================================================================== activity results

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == REQ_MEDIA_PERM) grantMedia();
        else if (code == REQ_LOCATION_PERM) finishLocation(hasLocation());
        else if (code == REQ_DOWNLOAD_PERM) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startDownload();
            else { toast("Storage permission is required on this Android version"); dlUrl = null; }
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == REQ_FILE) {
            if (fileCallback != null) fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        } else if (request == REQ_EXPORT && result == RESULT_OK && data != null && data.getData() != null) {
            exportBackup(data.getData());
        } else if (request == REQ_IMPORT && result == RESULT_OK && data != null && data.getData() != null) {
            importBackup(data.getData());
        } else if (request == REQ_VOICE && result == RESULT_OK && data != null) {
            ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (r != null && !r.isEmpty()) navigate(r.get(0));
        }
    }
}
