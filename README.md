# ASTER Browser

Current Android version: **1.3.0**.

ASTER is an original, privacy-first Android browser with a night-sky identity: built-in ad and tracker blocking (**ASTER Halo**), memory management (**ASTER Core**), a living start page with free games (**Arcade**), the **Orbit button** and a set of hand-made themes — including a full **Liquid Glass** look.

> ASTER renders pages with **Android System WebView**. It can't install browser extensions, and some engine-level features (full uBlock Origin semantics, Tor, sync) are out of reach.

## Highlights

### ⛨ ASTER Halo — privacy protection
- Network blocking from **EasyList, EasyPrivacy, AdGuard Base / Tracking / Mobile and Peter Lowe's list**, refreshed every 24 h.
- **Cosmetic filtering** hides ad slots, banners and empty ad containers.
- **Standard / Aggressive** modes.
- **Per-site controls**: Halo on/off, fingerprinting protection, hide ad elements, block scripts, allow third-party cookies, blocked-domain list.
- Fingerprinting protection, Global Privacy Control and DNT signals, pop-up blocking, HTTPS-only navigation, third-party cookies blocked, invalid certificates never bypassed.
- Start-page counters: **ads & trackers blocked, data saved, time saved**.

### 🎨 Themes
- **Liquid Glass** and **Liquid Glass Light**: floating translucent capsules with a specular sheen and luminous rim over a colourful backdrop; menus and pages sit on a **frosted, blurred snapshot** of the page behind them; the start page uses real backdrop blur.
- **ASTER Night, Nova, Nebula, Aurora, Comet, Solar Flare, Glacier, Ember**.
- Animated start-page wallpapers: Nebula, Synthwave grid, Aurora, Code rain, Minimal.

### ⚡ ASTER Core
Live memory usage, **memory saver** (puts least-used tabs to sleep), open-tab overview with sleep/close, data saver (no images) and cache cleaner.

### 🕹 Arcade
Free-game giveaways on the start page (GamerPower API).

### 🧭 Browsing
- **Integrated menu**: quick actions, current-page card with Halo status, in-place quick toggles and a tile grid. All panels open inside the browser and can be dragged down to close; pages slide in and close with back.
- Toolbar hides while scrolling; swipe the address bar to switch tabs; **Orbit button** navigation option; tablet/foldable layout with a tab strip.
- Tabs with sleeping, thumbnails and swipe-to-close; search suggestions; full-screen video with **picture-in-picture**; long-press link/image menu; pull to refresh; find in page; reader mode; night mode for websites; desktop site per tab; translate; add to home screen; voice search.
- Bookmarks, history and downloads managers; editable speed dial.
- **YouTube** ad removal (experimental) and background playback option.
- **Microsoft Rewards**: Rewards shortcut, one-tap switch to Bing, Microsoft sign-in stays logged in.
- Spanish or English interface (automatic by device language).
- Private windows: separate process and WebView profile, nothing saved, site data wiped on close.
- Backup & restore to a JSON file you control.

## Project layout

| File | Purpose |
| --- | --- |
| `MainActivity.java` | Browser UI, tabs, WebView clients, menu, panels, settings |
| `PrivateActivity.java` | Private window (separate process + WebView profile) |
| `ShieldEngine.java` | ASTER Halo: filter lists, network + cosmetic blocking, per-site settings, stats |
| `Glass.java` | Liquid Glass materials and blurred backdrops |
| `StartPage.java` | Start page HTML, search engines, wallpapers, Arcade |
| `Scripts.java` | Injected JavaScript (cosmetic CSS, YouTube, fingerprinting, reader, night mode) |
| `Store.java` | Bookmarks, history, speed dial, downloads and session |
| `FabMenu.java` | Orbit button |
| `AsterWebView.java`, `Tab.java` | WebView subclass and tab model |
| `Theme.java`, `UiSounds.java`, `Updater.java`, `I18n.java` | Themes, interface sounds, updater, translations |

## Build

Plain Java + Android framework, no third-party dependencies. GitHub Actions builds the APK; commits containing `[release]` publish a GitHub Release with `ASTER-Browser.apk`.
