package com.aster.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;
import android.view.View;

/**
 * Liquid Glass rendering: layered translucent materials with a specular top sheen and a bright
 * rim, plus frosted backdrops made from a blurred snapshot of what is behind a panel.
 */
final class Glass {
    private Glass() { }

    interface BitmapCallback { void done(Bitmap blurred); }

    static float[] radii(float all) { return new float[]{all, all, all, all, all, all, all, all}; }

    static float[] topRadii(float r) { return new float[]{r, r, r, r, 0, 0, 0, 0}; }

    /**
     * A glass material: tinted translucent body, a soft highlight across the upper part
     * (light catching the curved surface) and a thin luminous rim.
     */
    static Drawable material(Theme t, float[] radii, float density, int bodyAlpha) {
        GradientDrawable body = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, t.light
            ? new int[]{Color.argb(Math.min(255, bodyAlpha + 40), 255, 255, 255), Color.argb(bodyAlpha, 250, 251, 255)}
            : new int[]{Color.argb(bodyAlpha, 52, 62, 92), Color.argb(Math.max(0, bodyAlpha - 30), 18, 22, 40)});
        body.setCornerRadii(radii);

        GradientDrawable sheen = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
            Color.argb(t.light ? 150 : 70, 255, 255, 255), Color.argb(t.light ? 30 : 12, 255, 255, 255), Color.TRANSPARENT});
        sheen.setCornerRadii(radii);

        GradientDrawable rim = new GradientDrawable();
        rim.setColor(Color.TRANSPARENT);
        rim.setCornerRadii(radii);
        rim.setStroke(Math.max(1, Math.round(density)), Color.argb(t.light ? 235 : 105, 255, 255, 255));

        GradientDrawable innerShade = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{
            Color.argb(t.light ? 18 : 40, 0, 0, 0), Color.TRANSPARENT, Color.TRANSPARENT});
        innerShade.setCornerRadii(radii);

        return new LayerDrawable(new Drawable[]{body, innerShade, sheen, rim});
    }

    /** Colourful background behind glass chrome, so the translucency has something to refract. */
    static Drawable backdrop(Theme t, int widthPx) {
        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TL_BR, t.light
            ? new int[]{Color.rgb(214, 228, 255), Color.rgb(240, 230, 255), Color.rgb(214, 245, 238)}
            : new int[]{Color.rgb(8, 22, 54), Color.rgb(26, 10, 52), Color.rgb(4, 36, 44)});
        GradientDrawable blobA = radial(Theme.withAlpha(t.accent, t.light ? 110 : 150), widthPx * 0.9f, 0.15f, 0.05f);
        GradientDrawable blobB = radial(Theme.withAlpha(t.accent2, t.light ? 100 : 140), widthPx * 0.9f, 0.95f, 0.55f);
        GradientDrawable blobC = radial(t.light ? Color.argb(90, 255, 160, 200) : Color.argb(110, 0, 200, 190), widthPx * 0.8f, 0.2f, 0.95f);
        return new LayerDrawable(new Drawable[]{base, blobA, blobB, blobC});
    }

    private static GradientDrawable radial(int color, float radius, float cx, float cy) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{color, Color.TRANSPARENT});
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(radius);
        g.setGradientCenter(cx, cy);
        return g;
    }

    /** Takes a small snapshot of {@code view} as currently on screen and returns a blurred copy. */
    static void capture(Activity activity, final View view, final BitmapCallback cb) {
        final int w = view.getWidth();
        final int h = view.getHeight();
        if (w <= 0 || h <= 0) { cb.done(null); return; }
        final int sw = Math.max(8, w / 10);
        final int sh = Math.max(8, h / 10);
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                final Bitmap small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
                int[] loc = new int[2];
                view.getLocationInWindow(loc);
                Rect src = new Rect(loc[0], loc[1], loc[0] + w, loc[1] + h);
                PixelCopy.request(activity.getWindow(), src, small, result -> {
                    if (result == PixelCopy.SUCCESS) cb.done(blur(small));
                    else cb.done(drawn(view, w, h, sw, sh));
                }, new Handler(Looper.getMainLooper()));
                return;
            } catch (Throwable ignored) {
                // Fall back to a software draw below.
            }
        }
        cb.done(drawn(view, w, h, sw, sh));
    }

    private static Bitmap drawn(View view, int w, int h, int sw, int sh) {
        try {
            Bitmap b = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.scale(sw / (float) w, sh / (float) h);
            view.draw(c);
            return blur(b);
        } catch (Throwable e) {
            return null;
        }
    }

    /** Three passes of a separable box blur approximate a gaussian blur on the small snapshot. */
    static Bitmap blur(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        int[] tmp = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        int r = Math.max(1, Math.min(w, h) / 24 + 1);
        for (int pass = 0; pass < 3; pass++) {
            boxPass(px, tmp, w, h, r, true);
            boxPass(tmp, px, w, h, r, false);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    private static void boxPass(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w;
        int len = horizontal ? w : h;
        int window = r * 2 + 1;
        for (int line = 0; line < lines; line++) {
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int c = in[index(line, clamp(i, len), w, horizontal)];
                sa += c >>> 24; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255;
            }
            for (int i = 0; i < len; i++) {
                out[index(line, i, w, horizontal)] = ((sa / window) << 24) | ((sr / window) << 16) | ((sg / window) << 8) | (sb / window);
                int add = in[index(line, clamp(i + r + 1, len), w, horizontal)];
                int rem = in[index(line, clamp(i - r, len), w, horizontal)];
                sa += (add >>> 24) - (rem >>> 24);
                sr += ((add >> 16) & 255) - ((rem >> 16) & 255);
                sg += ((add >> 8) & 255) - ((rem >> 8) & 255);
                sb += (add & 255) - (rem & 255);
            }
        }
    }

    private static int index(int line, int i, int w, boolean horizontal) {
        return horizontal ? line * w + i : i * w + line;
    }

    private static int clamp(int i, int len) { return i < 0 ? 0 : (i >= len ? len - 1 : i); }
}
