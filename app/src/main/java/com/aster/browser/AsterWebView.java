package com.aster.browser;

import android.content.Context;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.webkit.WebView;

/**
 * WebView with pull-to-refresh and scroll-direction callbacks.
 *
 * Scroll direction is only reported for scrolling the user caused (finger down, or the fling
 * right after lifting it). Scroll changes produced by layout, anchors or the page itself are
 * ignored, which keeps the auto-hiding toolbar from reacting to them.
 */
class AsterWebView extends WebView {
    interface Listener {
        void onPull(AsterWebView view, float distancePx, boolean released);
        void onScrollDirection(AsterWebView view, boolean down);
    }

    private static final long FLING_WINDOW_MS = 1500;

    Tab tab;
    private Listener listener;
    private boolean pullEnabled = true;
    private float downX;
    private float downY;
    private boolean tracking;
    private boolean pulling;
    private boolean touching;
    private long lastTouchUp;
    private int lastDirectionY;
    private int scrollAccumulator;
    private boolean reportedDown;
    private final float density;

    AsterWebView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
    }

    void setListener(Listener listener) { this.listener = listener; }
    void setPullEnabled(boolean enabled) { this.pullEnabled = enabled; }

    /** True when the page is tall enough that hiding the toolbar is worthwhile. */
    private boolean scrollableEnough() {
        return computeVerticalScrollRange() - getHeight() > 220 * density;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touching = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touching = false;
                lastTouchUp = SystemClock.uptimeMillis();
                break;
            default:
                break;
        }
        if (listener != null && pullEnabled) handlePull(event);
        return super.onTouchEvent(event);
    }

    private void handlePull(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                tracking = !canScrollVertically(-1);
                pulling = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!tracking) break;
                float dy = event.getY() - downY;
                float dx = Math.abs(event.getX() - downX);
                if (!pulling) {
                    // Any upward movement or horizontal swipe means the user is scrolling, not pulling.
                    if (dy < -8 * density || dx > 24 * density) { tracking = false; break; }
                    if (dy > 28 * density && dy > dx * 2f && !canScrollVertically(-1)) pulling = true;
                }
                if (pulling) {
                    if (canScrollVertically(-1) || dy < 0) {
                        pulling = false;
                        tracking = false;
                        listener.onPull(this, 0, true);
                    } else {
                        listener.onPull(this, dy, false);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (pulling) {
                    float up = event.getActionMasked() == MotionEvent.ACTION_UP ? event.getY() - downY : 0;
                    listener.onPull(this, canScrollVertically(-1) ? 0 : up, true);
                }
                tracking = false;
                pulling = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (pulling) listener.onPull(this, 0, true);
                tracking = false;
                pulling = false;
                break;
            default:
                break;
        }
    }

    @Override protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener == null) return;
        if (t <= 0) {
            // Back at the top: always bring the toolbar back.
            if (reportedDown) {
                reportedDown = false;
                listener.onScrollDirection(this, false);
            }
            scrollAccumulator = 0;
            return;
        }
        boolean userScroll = touching || SystemClock.uptimeMillis() - lastTouchUp < FLING_WINDOW_MS;
        if (!userScroll) return;
        int dy = t - oldt;
        if (dy == 0) return;
        int dir = dy > 0 ? 1 : -1;
        if (dir != lastDirectionY) {
            scrollAccumulator = 0;
            lastDirectionY = dir;
        }
        scrollAccumulator += Math.abs(dy);
        if (scrollAccumulator < 28 * density) return;
        scrollAccumulator = 0;
        boolean down = dir > 0 && scrollableEnough();
        if (down == reportedDown) return;
        reportedDown = down;
        listener.onScrollDirection(this, down);
    }

    /** Resets the reported state (called when a new page starts or the toolbar is shown). */
    void resetScrollState() {
        reportedDown = false;
        scrollAccumulator = 0;
    }
}
