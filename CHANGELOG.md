# Changelog

## [1.5.0] - 2026-11-06
Free game alerts and crash reports.

### Added
- **Free game alerts** (Settings → Arcade): a background check every few hours, only with a network connection, notifies you of new free games on the stores you choose (Epic Games Store, Steam, Xbox, PlayStation, GOG, mobile). Notifications show the game art and open the store page; the first check only records current offers so you aren't flooded with old ones. Survives reboots.
- **Crash reports**: if ASTER closes unexpectedly, the next start shows the saved report with Copy and Share buttons. Nothing is sent automatically.

## [1.4.0] - 2026-11-06
Arcade grows into a full gaming hub.

### Added
- **Arcade screen** (start page → "See all", or menu → Arcade) with two tabs:
  - **Free games**: this week's free games on the **Epic Games Store** (official promotions feed) with upcoming ones, plus giveaways filterable by **Steam, Epic, Xbox, PlayStation, GOG, PC, mobile, Switch** and **DLC & extras** (GamerPower).
  - **News**: latest gaming news from **GameSpot, PC Gamer, Xbox Wire, PlayStation Blog** and, in Spanish, **Vandal**, filterable by outlet.
- Data is cached for 30 minutes and refreshed in place; links open in a new tab.

## [1.3.1] - 2026-11-06
Smoothness fixes.

### Fixed
- Auto-hiding bottom toolbar no longer resizes the page: it now floats over the page and slides away, which removes the jumps and flicker when it hid or reappeared.
- The start page could refuse to scroll down and snap back to the top (the toolbar resize changed the page height, and Arcade/statistics updates reloaded the whole page). Statistics and Arcade now update in place and keep your scroll position.
- The toolbar only reacts to scrolling you do with your finger (and the fling after it), not to scroll changes caused by the page or by layout, and it stays put on short pages.
- Pull-to-refresh no longer triggers when you start a normal scroll or a sideways swipe.

### Improved
- The bottom toolbar hides while you type in the address bar and returns afterwards.
- Opening the tab switcher scrolls to the current tab.

## [1.3.0] - 2026-11-06
Original identity and a new Liquid Glass.

### Changed
- ASTER now has its own names: privacy protection is **ASTER Halo**, the memory panel is **ASTER Core**, the start-page games feed is **Arcade**, the floating navigation is the **Orbit button**, and themes are **Nova, Nebula, Aurora, Comet, Solar Flare, Glacier and Ember** (existing theme choices migrate automatically).
- Interface sounds and memory saver renamed accordingly.

### Added
- Rebuilt **Liquid Glass** theme and new **Liquid Glass Light**: floating translucent toolbar and bottom capsules with specular sheen and luminous rim over a colourful backdrop; menus, panels and pages sit on a blurred snapshot of the page behind them; start page uses real backdrop blur and saturation.
- Light themes switch the status and navigation bar icons to dark.

## [1.2.0] - 2026-11-06
Integrated, fluid interface.

### Changed
- New integrated browser menu (Brave + Opera GX style): quick-action strip (forward, bookmark, downloads, share, reload), current-page card with Shields status, in-place quick toggles (night mode, desktop site, Shields, data saver, background play, reader) and a tile grid.
- All panels (menu, Shields, GX Control, themes, link menu) are now drawn inside the browser window instead of separate dialogs: they open instantly, animate smoothly and can be dragged down to close.
- Tabs, settings, history, bookmarks and downloads open as sliding full-screen pages; the back gesture closes them.
- Every button reacts with a short press animation; page loading progress animates smoothly.

## [1.1.0] - 2026-11-06
Smoothness, comfort, YouTube and Microsoft Rewards.

### Added
- Spanish interface (automatic by device language, or choose Español/English in Settings → Language).
- Bottom toolbar hides while scrolling down and returns when scrolling up.
- Swipe left/right on the address bar to switch tabs.
- Tablet / foldable / large-window layout: desktop-style tab strip plus back, forward, home, new tab and menu in the toolbar.
- Picture-in-picture for full-screen videos (automatic on Android 12+).
- YouTube ad removal (experimental): strips ad data from player responses and blocks YouTube ad endpoints.
- Microsoft Rewards: Rewards shortcut on the start page and in the menu, one-time offer to switch to Bing, Microsoft sign-in domains keep third-party cookies so you stay signed in.
- Per-site "Allow third-party cookies" Shields toggle for sites whose sign-in needs it.

### Improved
- Smoother scrolling: the visible tab is pre-rastered and background tabs get lower renderer priority.
- Start page is no longer rebuilt every time you switch to it.

## [1.0.0] - 2026-11-06
Complete rebuild of ASTER as a modular browser inspired by Brave and Opera GX.

### Added
- ASTER Shields panel per site: Shields up/down, fingerprinting protection, cosmetic ad hiding, script blocking and blocked-domain list.
- Cosmetic filtering from EasyList/AdGuard element-hiding rules (generic and site-specific).
- Standard and Aggressive blocking modes; ads and trackers counted separately.
- Start-page statistics: ads & trackers blocked, data saved, time saved.
- Fingerprinting protection, Global Privacy Control / DNT signals, pop-up blocking.
- GX themes (Classic, Purple Haze, Hackerman, Ultraviolet, Frutti di Mare, Ice Cold) plus Shields Dark.
- Fast Action Button navigation with swipe gestures.
- GX Control: RAM usage, RAM limiter with tab sleeping, Hot Tabs Killer, data saver, cache cleaner.
- GX Corner free-games feed, animated start-page wallpapers, GX sounds and haptics.
- Tab switcher with thumbnails and swipe-to-close; up to 100 tabs with lazy session restore.
- Search suggestions, paste-and-go, address bar at top or bottom, seven search engines (Brave Search default for new installs).
- Full-screen video, long-press link/image menu, background tabs, `target=_blank` and pop-ups as tabs.
- Pull to refresh, find-in-page bar, new reader mode, night mode for websites, per-tab desktop mode, add to home screen, voice search.
- Bookmark, history and download managers; editable speed dial.
- Background media playback option, clear-site-data-on-exit option.
- Private windows are now the full browser (own process and WebView profile) with leftover-profile cleanup.
- Recovery when the WebView renderer crashes or is killed.
- Accepts shared text and web-search intents from other apps.

### Changed
- Filter lists and browsing data moved from SharedPreferences to files.
- Backup format v2 (bookmarks, history, speed dial, per-site Shields, settings, tabs); v1 backups still import.

### Limitations
- Rendering still uses Android System WebView; extensions, Tor and sync are not available.
- Some YouTube in-stream ads served from video servers can't be blocked reliably.
- Android doesn't allow apps to cap CPU/bandwidth, so GX Control manages memory and data instead.

## [0.15.1] - 2026-11-03
### Fixed
- Forces a one-time download of the expanded ASTER Shield filter set after upgrading, even if the previous version refreshed its older lists less than 24 hours ago.


## [0.15.0] - 2026-11-03
### Improved
- Expanded ASTER Shield with EasyList, EasyPrivacy, AdGuard Base, AdGuard Tracking Protection, AdGuard Mobile Ads, and Peter Lowe ad-server domain filters.
- Increased the supported domain-rule cache to 200,000 entries and refreshes filter lists every 24 hours.
- Added cosmetic ad hiding for common ad containers and YouTube ad overlays, plus automatic activation of YouTube's visible Skip Ad button when available.

### Limitations
- Android WebView cannot reliably block YouTube server-side/in-stream ads that share delivery domains with normal video; this release reduces supported ad requests and hides some ad UI but cannot guarantee an ad-free YouTube experience.
- Advanced cosmetic filters, scriptlets, and full uBlock Origin/Brave filter semantics are not supported.


## [0.14.0] - 2026-11-03
### Added
- Added a responsive Android layout that adjusts browser chrome spacing and navigation width for compact phones and expanded tablet, foldable, and desktop-sized app windows.
- Added desktop-site mode with a desktop-style WebView user agent and wide viewport rendering.
- Added Reader Mode with a simplified article view and a one-tap exit control.
- Added page translation through Google Translate, with selectable target languages: Spanish, English, Slovenian, Japanese, Korean, French, and German.
- Added adjustable webpage text zoom: 85%, 100%, 115%, 130%, and 150%.
- Added support for file uploads on websites through Android's system document picker.
- Added external HTTP/HTTPS link handling so ASTER can be selected as an Android browser.
- Added hardware keyboard shortcuts for address focus, new/close tab, reload, and cycling tabs.
- Extended JSON backup and restore to include the new display and browsing preferences.

### Notes
- Responsive behavior supports Android devices and resizable Android windows; it does not make this Android app available on iOS, Windows, or macOS.
- Desktop mode requests desktop versions of websites but remains rendered by Android System WebView.
- Translation opens the selected page through Google Translate and requires internet access.
- Reader Mode extracts visible page text; complex pages may not be parsed perfectly.


## [0.13.0] - 2026-11-03
### Added
- Added start-page customization: independently show or hide ASTER branding, quick-access shortcuts, and saved bookmarks.
- Added four start-page background themes: Violet, Midnight, Ocean, and Crimson.
- Rebuilt the tab switcher as a two-column visual card grid with active-tab state, page title, domain, close controls, and a prominent new-tab action.
- Kept ASTER Shield accessible directly from the browser toolbar alongside per-site protection controls.

### Fixed
- Enabled Android BuildConfig generation, fixing the previous release build's missing BuildConfig compilation error.
- Corrected optional start-page sections so hidden shortcuts and bookmarks are not rendered.

### Limitations
- The browser continues to use Android System WebView; the visual redesign does not make it a standalone Chromium engine.
- ASTER Shield applies supported domain network rules, not full cosmetic or scriptlet filtering.


## [0.12.0] - 2026-11-03
### Added
- Replaced ASTER Shield's small static-only block check with a cached domain-rule engine that imports supported network rules and exceptions from EasyList and EasyPrivacy.
- Added automatic weekly filter refresh, manual filter updates, and a live domain-rule count in the protection panel.
- Refreshed the start page and browser chrome with a dark violet ASTER visual theme, updated search field, shortcut tiles, and matching controls.

### Limitations
- The WebView integration currently imports domain-based network rules only. Cosmetic selectors, scriptlets, advanced ABP options, and first-party ad replacement are not supported; some ads may remain.
- ASTER continues to use Android System WebView rather than a Chromium-based independent engine.


## [0.11.0] - 2026-11-03
### Added
- Added ASTER Update: checks GitHub Releases on startup, every 24 hours while the app is active, and on demand from Settings.
- Added in-app APK download and handoff to Android's package installer, with user confirmation.
- Standardized the release APK asset name as `ASTER-Browser.apk` so the app can find it automatically.

### Notes
- Android still requires the user to approve installation; ASTER cannot silently install updates.
- Updating an existing installation requires the same permanent signing key. Until that key is configured, published debug APKs may be rejected as updates.


## [0.10.0] - 2026-11-03
### Added
- Redesigned the tab manager with a clearer list of open pages, active-tab highlighting, one-tap switching, and individual close controls.
- Added a more visual ASTER Shield privacy panel with protection status, per-page/session counters, global controls, site exceptions, and blocked-domain details.
- Added a deliberate major-version release workflow: commits marked [release] on main and version tags v* publish a GitHub Release.

### Notes
- When persistent signing is configured, the release includes an install-over-compatible signed APK.
- Until permanent signing is configured, GitHub Releases contain source and release notes only; debug APKs remain separate test artifacts.
- ASTER Shield uses a compact built-in host list and is not a full cosmetic or filter-list engine.

## [0.9.0] - 2026-11-03
### Added
- Introduced ASTER Shield, a lightweight WebView request blocker for a built-in set of known advertising and analytics domains.
- Added global protection controls, per-host exceptions, per-page and session counters, and a blocked-domain list.
- Included Shield preferences and per-site exceptions in JSON backups.

### Notes
- The built-in host list is intentionally lightweight and is not a full EasyList/uBlock filter engine.
- Request interception cannot remove every ad, cosmetic element, or first-party tracking script.
- Rendering still uses Android System WebView.

## [0.8.0] - 2026-11-03
### Added
- Added local JSON backup export through Android's document picker.
- Backups include bookmarks, browsing history, homepage and search-engine settings, tab-restore preference, open tabs, and the selected tab.
- Added backup import with format/version validation, URL filtering, tab limits, and a confirmation before replacing current browser data.
- Kept backup files under the user's control; no account or remote storage is required.

### Notes
- Website cookies, passwords, and cached website data are not included in backups.
- Android System WebView remains the rendering engine.

## [0.7.1] - 2026-11-02
### Changed
- Prepared a persistent Android release-signing configuration so future APK updates can be installed over existing installations.
- Increased the Android version code for the first stable-signature migration release.

### Notes
- A one-time uninstall of APKs signed with the previous temporary debug key may still be required before installing the first permanently signed APK.
- The signed release is not published until the repository's four signing secrets are configured.
- Rendering still uses Android System WebView.

## [0.7.0] - 2026-11-02
### Changed
- Replaced the large branded header with compact browser controls so websites receive more screen space.
- Redesigned the new-tab page as a minimal dark dashboard with a centered ASTER mark, search field, quick-access tiles, and bookmarks.
- Simplified navigation controls and added a dedicated new-tab action.
- Kept website rendering inside the normal browser viewport without start-page content layered over websites.

### Notes
- The browser still uses Android System WebView; this is a UI redesign, not a custom rendering engine.


## [0.6.1] - 2026-11-02
### Changed
- Refined ASTER's visual identity with a warm amber accent inspired by modern privacy-focused browsers.
- Kept ASTER's own name, iconography, and layout rather than copying another browser's branding.


## [0.6.0] - 2026-11-02
### Changed
- Redesigned the Android browser chrome with a compact ASTER brand header.
- Added a privacy information control beside the address field.
- Moved reload into the address toolbar.
- Simplified bottom navigation to back, forward, home, tabs, and menu.
- Rebuilt the start page with a modern dashboard, search field, shortcuts, and saved pages.
- Improved touch target sizing and density-aware spacing.
- Added automatic GitHub Releases after successful APK builds.

### Notes
- Rendering still uses Android System WebView.
- Advanced tracker blocking and a custom browser engine are not implemented yet.
- This version requires real-device testing, including on the Redmi 12.
