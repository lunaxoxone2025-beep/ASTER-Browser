# ASTER Browser

Current Android version: **1.1.0** (1.0 was a full rebuild; 1.1 adds smoothness, Spanish UI, YouTube and Microsoft Rewards improvements).

ASTER is a privacy-first Android browser with a gaming-style interface. Version 1.0 combines the ideas people love in **Brave** (Shields, built-in blocking, privacy stats) and **Opera GX** (GX themes, Fast Action Button, GX Control, GX Corner) with its own identity.

> ASTER renders pages with **Android System WebView**. It is not a fork of Chromium, so it can't install browser extensions, and some browser-engine features (full uBlock Origin semantics, Tor, sync) are out of reach.

## Highlights

### ⛨ ASTER Shields (Brave-style)
- Network blocking from **EasyList, EasyPrivacy, AdGuard Base / Tracking / Mobile and Peter Lowe's list**, refreshed every 24 h. Rules are stored in files, not in SharedPreferences.
- **Cosmetic filtering**: generic and site-specific `##selector` rules hide ad slots, banners and empty ad containers.
- **Standard / Aggressive** modes — standard leaves first-party requests alone, aggressive blocks them too.
- **Per-site panel**: Shields up/down, block fingerprinting, hide ad elements, block scripts, list of blocked domains.
- **Fingerprinting protection** (canvas/audio noise, generic hardware and WebGL values), Global Privacy Control and DNT signals.
- **Pop-up blocking**, HTTPS-only navigation, third-party cookies blocked, invalid certificates never bypassed.
- YouTube: ad UI hidden, skip buttons pressed and in-stream ads fast-forwarded when they slip through (best effort).
- Start-page counters: **ads & trackers blocked, data saved, time saved**.

### ⚡ GX features (Opera GX-style)
- **8 themes**: ASTER Night, Shields Dark, GX Classic, GX Purple Haze, GX Hackerman, GX Ultraviolet, GX Frutti di Mare, GX Ice Cold, Liquid Glass.
- **Fast Action Button**: optional floating button that fans out into quick actions. Swipe it ← / → for back/forward, ↑ for tabs, long-press for a new tab.
- **GX Control**: live RAM usage, **RAM limiter** (puts least-used tabs to sleep), **Hot Tabs Killer**, data saver (no images) and cache cleaner.
- **GX Corner**: free-game giveaways on the start page (GamerPower API).
- **GX sounds & haptics**: synthesized interface sounds and light vibration (both optional).
- **Animated start-page wallpapers**: Nebula, Synthwave grid, Aurora, Code rain, Minimal.

### 🎮 YouTube & Microsoft Rewards
- **YouTube**: ad data stripped from the player, ad endpoints blocked, full-screen with auto-landscape, **picture-in-picture** and optional background playback.
- **Microsoft Rewards**: Rewards shortcut, one-tap switch to Bing, Microsoft sign-in stays logged in. Search normally with Bing while signed in to earn points (automating searches breaks Microsoft's rules).

### 🧭 Browsing
- **Spanish or English interface** (automatic by device language).
- Toolbar **hides while scrolling**; **swipe on the address bar** to switch tabs; tablet/foldable layout with a **tab strip**.
- Up to 100 tabs with **tab sleeping**, visual **tab switcher** with thumbnails and swipe-to-close.
- Address bar with **search suggestions**, history/bookmark matches and "paste and go"; can sit at the **top or bottom**.
- **Full-screen video** (with auto-landscape), **long-press menus** for links and images, open links in new / background / private tabs.
- `target=_blank` links and user-initiated pop-ups open as new tabs.
- **Pull to refresh**, **find in page** bar, **reader mode** (dark or sepia), **night mode** for websites, **desktop site** per tab, **translate**, **add to home screen**, **voice search**.
- Bookmarks, history (searchable, grouped by day) and downloads managers; editable **speed dial**.
- Search engines: Brave Search (default for new installs), DuckDuckGo, Google, Bing, Startpage, Ecosia, Qwant.
- **Background media playback** (optional).
- Opens links, shared text and web searches from other apps; can be set as the default browser.
- Keyboard shortcuts: Ctrl+L, Ctrl+T, Ctrl+W, Ctrl+R, Ctrl+F, Ctrl+D, Ctrl+Shift+N, Ctrl+Tab / Ctrl+Shift+Tab.

### 👁 Private windows
The full ASTER browser in a separate process with its own WebView profile (Android 9+). Nothing is written to history, sessions, bookmarks or speed dial; site data is wiped when the window closes and any leftover private profile is deleted on the next start. Settings changed inside a private window don't affect normal windows.

### 💾 Backup & restore
Exports bookmarks, history, speed dial, per-site Shields, settings and open tabs to a JSON file you control (format v2). Backups from 0.x (v1) can still be imported. Cookies and passwords are never included.

### 🔄 Updates
ASTER checks GitHub Releases and can download the new APK for Android's installer. Installing over an existing copy requires every release to be signed with the same permanent key.

## Project layout

| File | Purpose |
| --- | --- |
| `MainActivity.java` | Browser UI, tabs, WebView clients, menus, settings, panels |
| `PrivateActivity.java` | Private window (separate process + WebView profile) |
| `ShieldEngine.java` | Filter-list download/parsing, network + cosmetic blocking, per-site settings, stats |
| `StartPage.java` | New-tab page HTML, search engines, wallpapers, GX Corner |
| `Scripts.java` | Injected JavaScript (cosmetic CSS, YouTube, fingerprinting, reader, night mode) |
| `Store.java` | Bookmarks, history, speed dial, downloads and session (JSON files) |
| `FabMenu.java` | GX Fast Action Button |
| `AsterWebView.java`, `Tab.java` | WebView subclass (pull-to-refresh, scroll) and tab model |
| `Theme.java`, `GxSounds.java`, `Updater.java` | Themes, interface sounds, GitHub updater |
| `I18n.java` | Spanish/English interface strings |

## Build

Plain Java + Android framework, no third-party dependencies. GitHub Actions builds the APK (`.github/workflows/build-android.yml`); commits containing `[release]` publish a GitHub Release with `ASTER-Browser.apk`.
