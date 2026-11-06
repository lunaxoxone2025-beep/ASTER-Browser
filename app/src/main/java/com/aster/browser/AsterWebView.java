package com.aster.browser;

import android.content.Context;
import android.view.MotionEvent;
import android.webkit.WebView;

/** WebView with pull-to-refresh and scroll-direction callbacks. */
class AsterWebView extends WebView {
    interface Listener {
        void onPull(AsterWebView view, float distancePx, boolean released);
        void onScrollDirection(AsterWebView view, boolean down);
    }

    Tab tab;
    private Listener listener;
    private boolean pullEnabled = true;
    private float downX;
    private float downY;
    private boolean tracking;
    private boolean pulling;
    private int lastDirectionY;
    private int scrollAccumulator;

    AsterWebView(Context context) {
        super(context);
    }

    void setListener(Listener listener) { this.listener = listener; }
    void setPullEnabled(boolean enabled) { this.pullEnabled = enabled; }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (listener != null && pullEnabled) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    tracking = getScrollY() == 0;
                    pulling = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (tracking) {
                        float dy = event.getY() - downY;
                        float dx = Math.abs(event.getX() - downX);
                        if (!pulling && dy > 24 && dy > dx * 1.6f && getScrollY() == 0) pulling = true;
                        if (pulling) {
                            if (getScrollY() != 0 || dy < 0) {
                                pulling = false;
                                tracking = false;
                                listener.onPull(this, 0, true);
                            } else {
                                listener.onPull(this, dy, false);
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (pulling) {
                        float dy = event.getActionMasked() == MotionEvent.ACTION_UP ? event.getY() - downY : 0;
                        listener.onPull(this, getScrollY() == 0 ? dy : 0, true);
                    }
                    if (event.getPointerCount() > 1) listener.onPull(this, 0, true);
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
        return super.onTouchEvent(event);
    }

    @Override protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (listener == null) return;
        int dy = t - oldt;
        if (dy == 0) return;
        int dir = dy > 0 ? 1 : -1;
        if (dir != lastDirectionY) {
            scrollAccumulator = 0;
            lastDirectionY = dir;
        }
        scrollAccumulator += Math.abs(dy);
        if (scrollAccumulator > 48 || t <= 0) {
            listener.onScrollDirection(this, dir > 0 && t > 0);
            scrollAccumulator = 0;
        }
    }
}
