package com.aster.browser;

import android.graphics.Color;

/** Browser-wide colour palettes. GX themes follow the spirit of gaming browsers; ASTER keeps its own identity. */
final class Theme {
    final String id;
    final String name;
    final String family;
    final int bg;
    final int surface;
    final int surface2;
    final int text;
    final int muted;
    final int accent;
    final int accent2;
    final boolean glass;

    private Theme(String id, String name, String family, int bg, int surface, int surface2,
                  int text, int muted, int accent, int accent2, boolean glass) {
        this.id = id;
        this.name = name;
        this.family = family;
        this.bg = bg;
        this.surface = surface;
        this.surface2 = surface2;
        this.text = text;
        this.muted = muted;
        this.accent = accent;
        this.accent2 = accent2;
        this.glass = glass;
    }

    static final Theme[] ALL = {
        new Theme("aster", "ASTER Night", "ASTER",
            Color.rgb(13, 11, 18), Color.rgb(30, 24, 39), Color.rgb(44, 34, 58),
            Color.rgb(241, 244, 250), Color.rgb(174, 163, 190), Color.rgb(194, 112, 255), Color.rgb(108, 76, 255), false),
        new Theme("brave", "Shields Dark", "Privacy",
            Color.rgb(24, 26, 33), Color.rgb(36, 39, 49), Color.rgb(49, 53, 66),
            Color.rgb(240, 241, 245), Color.rgb(160, 165, 180), Color.rgb(251, 84, 43), Color.rgb(255, 32, 120), false),
        new Theme("gx_classic", "GX Classic", "GX",
            Color.rgb(18, 16, 25), Color.rgb(31, 26, 38), Color.rgb(46, 36, 52),
            Color.rgb(255, 244, 248), Color.rgb(190, 160, 172), Color.rgb(250, 30, 78), Color.rgb(160, 10, 50), false),
        new Theme("gx_purple", "GX Purple Haze", "GX",
            Color.rgb(17, 12, 28), Color.rgb(32, 22, 50), Color.rgb(48, 33, 74),
            Color.rgb(246, 240, 255), Color.rgb(178, 160, 210), Color.rgb(170, 90, 255), Color.rgb(90, 40, 210), false),
        new Theme("gx_hacker", "GX Hackerman", "GX",
            Color.rgb(6, 14, 9), Color.rgb(13, 28, 18), Color.rgb(20, 42, 27),
            Color.rgb(226, 255, 234), Color.rgb(130, 185, 145), Color.rgb(0, 255, 120), Color.rgb(0, 160, 80), false),
        new Theme("gx_ultra", "GX Ultraviolet", "GX",
            Color.rgb(10, 10, 26), Color.rgb(22, 22, 52), Color.rgb(34, 33, 76),
            Color.rgb(238, 240, 255), Color.rgb(158, 164, 214), Color.rgb(98, 112, 255), Color.rgb(200, 60, 255), false),
        new Theme("gx_frutti", "GX Frutti di Mare", "GX",
            Color.rgb(20, 12, 14), Color.rgb(40, 22, 26), Color.rgb(58, 32, 36),
            Color.rgb(255, 244, 238), Color.rgb(205, 168, 160), Color.rgb(255, 128, 40), Color.rgb(255, 40, 120), false),
        new Theme("gx_ice", "GX Ice Cold", "GX",
            Color.rgb(8, 16, 24), Color.rgb(16, 32, 46), Color.rgb(24, 46, 64),
            Color.rgb(232, 248, 255), Color.rgb(150, 186, 206), Color.rgb(0, 214, 255), Color.rgb(0, 120, 255), false),
        new Theme("liquid", "Liquid Glass", "ASTER",
            Color.rgb(10, 16, 29), Color.argb(170, 70, 90, 125), Color.argb(190, 92, 112, 148),
            Color.rgb(247, 250, 255), Color.rgb(193, 207, 229), Color.rgb(174, 218, 255), Color.rgb(120, 160, 255), true)
    };

    static Theme byId(String id) {
        if ("gx".equals(id)) id = "gx_classic";
        for (Theme t : ALL) if (t.id.equals(id)) return t;
        return ALL[0];
    }

    boolean isGx() { return "GX".equals(family); }

    static String css(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", 0xFFFFFF & color);
    }

    static String cssA(int color, float alpha) {
        return String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,%.2f)",
            Color.red(color), Color.green(color), Color.blue(color), alpha);
    }

    static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    static int blend(int a, int b, float t) {
        return Color.rgb(
            Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t),
            Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t),
            Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
