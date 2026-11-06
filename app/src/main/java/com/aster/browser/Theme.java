package com.aster.browser;

import android.graphics.Color;

/** ASTER's own colour palettes, named after things in the night sky. */
final class Theme {
    final String id;
    final String name;
    /** Short description shown in the theme picker. */
    final String family;
    final int bg;
    final int surface;
    final int surface2;
    final int text;
    final int muted;
    final int accent;
    final int accent2;
    /** Translucent "liquid glass" chrome with blurred backdrops. */
    final boolean glass;
    /** Light appearance (dark text and dark system-bar icons). */
    final boolean light;
    /** Uses the animated grid wallpaper by default. */
    final boolean vivid;

    private Theme(String id, String name, String family, int bg, int surface, int surface2, int text, int muted,
                  int accent, int accent2, boolean glass, boolean light, boolean vivid) {
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
        this.light = light;
        this.vivid = vivid;
    }

    static final Theme[] ALL = {
        new Theme("aster", "ASTER Night", "Dark · violet",
            Color.rgb(13, 11, 18), Color.rgb(30, 24, 39), Color.rgb(44, 34, 58),
            Color.rgb(241, 244, 250), Color.rgb(174, 163, 190), Color.rgb(194, 112, 255), Color.rgb(108, 76, 255), false, false, false),
        new Theme("liquid", "Liquid Glass", "Glass · dark",
            Color.rgb(9, 13, 26), Color.argb(42, 255, 255, 255), Color.argb(66, 255, 255, 255),
            Color.rgb(247, 249, 255), Color.rgb(190, 201, 222), Color.rgb(143, 211, 255), Color.rgb(197, 155, 255), true, false, false),
        new Theme("liquid_light", "Liquid Glass Light", "Glass · light",
            Color.rgb(232, 237, 246), Color.argb(150, 255, 255, 255), Color.argb(205, 255, 255, 255),
            Color.rgb(14, 20, 34), Color.rgb(88, 99, 120), Color.rgb(10, 132, 255), Color.rgb(125, 92, 255), true, true, false),
        new Theme("nova", "Nova", "Dark · crimson",
            Color.rgb(17, 13, 20), Color.rgb(32, 24, 34), Color.rgb(48, 33, 46),
            Color.rgb(255, 244, 247), Color.rgb(196, 164, 176), Color.rgb(255, 59, 92), Color.rgb(176, 22, 74), false, false, true),
        new Theme("nebula", "Nebula", "Dark · purple",
            Color.rgb(16, 11, 27), Color.rgb(31, 21, 49), Color.rgb(47, 32, 72),
            Color.rgb(246, 240, 255), Color.rgb(178, 160, 210), Color.rgb(176, 104, 255), Color.rgb(84, 48, 214), false, false, true),
        new Theme("aurora", "Aurora", "Dark · green",
            Color.rgb(7, 15, 13), Color.rgb(14, 30, 26), Color.rgb(21, 44, 38),
            Color.rgb(228, 255, 245), Color.rgb(134, 186, 168), Color.rgb(46, 230, 160), Color.rgb(0, 150, 170), false, false, true),
        new Theme("comet", "Comet", "Dark · indigo",
            Color.rgb(10, 11, 26), Color.rgb(22, 23, 52), Color.rgb(34, 34, 76),
            Color.rgb(238, 240, 255), Color.rgb(158, 164, 214), Color.rgb(112, 124, 255), Color.rgb(214, 84, 255), false, false, true),
        new Theme("solar", "Solar Flare", "Dark · amber",
            Color.rgb(20, 13, 12), Color.rgb(40, 24, 22), Color.rgb(58, 34, 30),
            Color.rgb(255, 245, 238), Color.rgb(205, 170, 160), Color.rgb(255, 150, 50), Color.rgb(236, 54, 110), false, false, true),
        new Theme("glacier", "Glacier", "Dark · cyan",
            Color.rgb(8, 15, 22), Color.rgb(16, 31, 44), Color.rgb(24, 45, 62),
            Color.rgb(232, 248, 255), Color.rgb(150, 186, 206), Color.rgb(64, 214, 255), Color.rgb(20, 120, 230), false, false, false),
        new Theme("ember", "Ember", "Graphite · orange",
            Color.rgb(24, 25, 30), Color.rgb(37, 38, 46), Color.rgb(50, 52, 62),
            Color.rgb(240, 241, 245), Color.rgb(162, 166, 180), Color.rgb(255, 112, 67), Color.rgb(230, 57, 70), false, false, false)
    };

    static Theme byId(String id) {
        // Map theme ids from 0.x / 1.0 to their current names.
        if (id == null) id = "aster";
        switch (id) {
            case "gx": case "gx_classic": id = "nova"; break;
            case "gx_purple": id = "nebula"; break;
            case "gx_hacker": id = "aurora"; break;
            case "gx_ultra": id = "comet"; break;
            case "gx_frutti": id = "solar"; break;
            case "gx_ice": id = "glacier"; break;
            case "brave": id = "ember"; break;
            default: break;
        }
        for (Theme t : ALL) if (t.id.equals(id)) return t;
        return ALL[0];
    }

    /** Default start-page wallpaper for this theme. */
    String defaultWallpaper() {
        // Static by default: no animation running in the background, so it costs no battery.
        return "minimal";
    }

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
