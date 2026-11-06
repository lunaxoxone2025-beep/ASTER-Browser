package com.aster.browser;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * GX-style Fast Action Button: a floating button that fans out into quick actions.
 * Swipe the button left/right for back/forward and up for the tab switcher.
 */
final class FabMenu extends FrameLayout {
    static final class Item {
        final String icon;
        final String label;
        final Runnable action;
        Item(String icon, String label, Runnable action) {
            this.icon = icon;
            this.label = label;
            this.action = action;
        }
    }

    interface Gestures {
        void swipeLeft();
        void swipeRight();
        void swipeUp();
        void longPress();
        void feedback();
    }

    private final View scrim;
    private final TextView fab;
    private final List<View> minis = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private Gestures gestures;
    private Theme theme;
    private boolean expanded;
    private int bottomOffset;

    FabMenu(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        scrim = new View(context);
        scrim.setBackgroundColor(Color.argb(150, 0, 0, 0));
        scrim.setVisibility(GONE);
        scrim.setClickable(true);
        scrim.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) { collapse(); }
        });
        addView(scrim, new LayoutParams(-1, -1));
        fab = new TextView(context);
        fab.setText("✦");
        fab.setTextSize(24);
        fab.setTypeface(Typeface.DEFAULT_BOLD);
        fab.setTextColor(Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        fab.setContentDescription("Fast action button. Tap for actions, swipe left for back, right for forward, up for tabs");
        fab.setElevation(dp(10));
        LayoutParams lp = new LayoutParams(dp(60), dp(60), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = dp(18);
        addView(fab, lp);
        fab.setOnTouchListener(new FabTouch());
    }

    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    void setBottomOffset(int px) {
        bottomOffset = px;
        LayoutParams lp = (LayoutParams) fab.getLayoutParams();
        lp.bottomMargin = dp(18) + px;
        fab.setLayoutParams(lp);
        for (View v : minis) {
            LayoutParams m = (LayoutParams) v.getLayoutParams();
            m.bottomMargin = dp(22) + px;
            v.setLayoutParams(m);
        }
    }

    void setup(Theme theme, List<Item> items, Gestures gestures) {
        this.theme = theme;
        this.items = items;
        this.gestures = gestures;
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{theme.accent, theme.accent2});
        d.setShape(GradientDrawable.OVAL);
        d.setStroke(dp(2), Theme.withAlpha(Color.WHITE, 60));
        fab.setBackground(d);
        for (View v : minis) removeView(v);
        minis.clear();
        for (final Item item : items) {
            LinearLayout box = new LinearLayout(getContext());
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView icon = new TextView(getContext());
            icon.setText(item.icon);
            icon.setTextSize(19);
            icon.setTextColor(theme.text);
            icon.setGravity(Gravity.CENTER);
            GradientDrawable c = new GradientDrawable();
            c.setShape(GradientDrawable.OVAL);
            c.setColor(theme.surface2);
            c.setStroke(dp(1), Theme.withAlpha(theme.accent, 150));
            icon.setBackground(c);
            box.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));
            TextView label = new TextView(getContext());
            label.setText(item.label);
            label.setTextSize(10);
            label.setTextColor(Color.WHITE);
            label.setGravity(Gravity.CENTER);
            label.setShadowLayer(4, 0, 1, Color.BLACK);
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(dp(76), -2);
            llp.topMargin = dp(3);
            box.addView(label, llp);
            box.setContentDescription(item.label);
            box.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    if (FabMenu.this.gestures != null) FabMenu.this.gestures.feedback();
                    collapse();
                    item.action.run();
                }
            });
            box.setVisibility(GONE);
            LayoutParams lp = new LayoutParams(dp(76), -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lp.bottomMargin = dp(22) + bottomOffset;
            addView(box, lp);
            minis.add(box);
        }
        bringChildToFront(fab);
    }

    boolean isExpanded() { return expanded; }

    void toggle() { if (expanded) collapse(); else expand(); }

    void expand() {
        if (expanded || minis.isEmpty()) return;
        expanded = true;
        scrim.setVisibility(VISIBLE);
        scrim.setAlpha(0f);
        scrim.animate().alpha(1f).setDuration(160).start();
        fab.animate().rotation(135f).setDuration(220).start();
        int n = minis.size();
        float radius = dp(n > 5 ? 128 : 112);
        for (int i = 0; i < n; i++) {
            View v = minis.get(i);
            double angle = Math.PI - (Math.PI * i / Math.max(1, n - 1));
            float tx = (float) (Math.cos(angle) * radius);
            float ty = (float) (-Math.sin(angle) * radius) - dp(16);
            v.setVisibility(VISIBLE);
            v.setTranslationX(0);
            v.setTranslationY(0);
            v.setScaleX(0.3f);
            v.setScaleY(0.3f);
            v.setAlpha(0f);
            v.animate().translationX(tx).translationY(ty).scaleX(1f).scaleY(1f).alpha(1f)
                .setStartDelay(i * 18L).setDuration(260).setInterpolator(new OvershootInterpolator(1.4f)).start();
        }
    }

    void collapse() {
        if (!expanded) return;
        expanded = false;
        scrim.animate().alpha(0f).setDuration(140).withEndAction(new Runnable() {
            @Override public void run() { if (!expanded) scrim.setVisibility(GONE); }
        }).start();
        fab.animate().rotation(0f).setDuration(200).start();
        for (final View v : minis) {
            v.animate().translationX(0).translationY(0).scaleX(0.3f).scaleY(0.3f).alpha(0f)
                .setStartDelay(0).setDuration(160).withEndAction(new Runnable() {
                    @Override public void run() { if (!expanded) v.setVisibility(GONE); }
                }).start();
        }
    }

    void setFabVisible(boolean visible) {
        if (expanded) return;
        fab.animate().translationY(visible ? 0 : dp(110)).setDuration(180).start();
    }

    private final class FabTouch implements OnTouchListener {
        private float sx;
        private float sy;
        private long downAt;
        private boolean longFired;
        private final Runnable longPress = new Runnable() {
            @Override public void run() {
                longFired = true;
                fab.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (gestures != null) gestures.longPress();
            }
        };

        @Override public boolean onTouch(View v, MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    sx = e.getRawX();
                    sy = e.getRawY();
                    downAt = System.currentTimeMillis();
                    longFired = false;
                    fab.animate().scaleX(0.9f).scaleY(0.9f).setDuration(90).start();
                    fab.postDelayed(longPress, 520);
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getRawX() - sx;
                    float dy = e.getRawY() - sy;
                    if (Math.abs(dx) > dp(12) || Math.abs(dy) > dp(12)) fab.removeCallbacks(longPress);
                    fab.setTranslationX(Math.max(-dp(40), Math.min(dp(40), dx * 0.35f)));
                    fab.setTranslationY(Math.max(-dp(40), Math.min(0, dy * 0.35f)));
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    fab.removeCallbacks(longPress);
                    fab.animate().scaleX(1f).scaleY(1f).translationX(0).translationY(0).setDuration(150).start();
                    if (e.getActionMasked() == MotionEvent.ACTION_CANCEL || longFired) return true;
                    float dx = e.getRawX() - sx;
                    float dy = e.getRawY() - sy;
                    float threshold = dp(44);
                    if (gestures != null && !expanded) {
                        if (dx < -threshold && Math.abs(dx) > Math.abs(dy)) { gestures.feedback(); gestures.swipeLeft(); return true; }
                        if (dx > threshold && Math.abs(dx) > Math.abs(dy)) { gestures.feedback(); gestures.swipeRight(); return true; }
                        if (dy < -threshold && Math.abs(dy) > Math.abs(dx)) { gestures.feedback(); gestures.swipeUp(); return true; }
                    }
                    if (System.currentTimeMillis() - downAt < 500 && Math.abs(dx) < dp(16) && Math.abs(dy) < dp(16)) {
                        if (gestures != null) gestures.feedback();
                        toggle();
                    }
                    return true;
                }
                default:
                    return false;
            }
        }
    }
}
