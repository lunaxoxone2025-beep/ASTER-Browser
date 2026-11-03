# ASTER Browser

Current Android version: **0.15.0**.

ASTER is an Android-first browser prototype with a dark violet interface, tabbed browsing, bookmarks, history, downloads, and privacy controls. Its visual direction is original and takes broad inspiration from modern browsers such as Brave and Opera GX.

## Engine and platform

ASTER currently uses Android System WebView. It is not a standalone Chromium browser engine, so some browser-level capabilities and extensions are outside its reach.

## ASTER Shield — ad and tracker blocking

- Uses a local cache of supported domain network rules imported from EasyList, EasyPrivacy, AdGuard Base, AdGuard Tracking Protection, AdGuard Mobile Ads, and Peter Lowe's ad-server list.
- Includes built-in advertising and analytics domains.
- Refreshes filter lists every 24 hours when a network connection is available; users can also request an update from the Shield panel.
- Supports global protection and per-site exceptions.
- Tracks blocked requests per page and per session.

**Important limitation:** the current WebView integration extracts domain-based network rules. It does not implement cosmetic CSS selectors, scriptlets, every ABP option, or advanced first-party ad removal. Therefore it can block many ad/tracker requests, but it cannot guarantee an ad-free page or match Brave's native Shields engine.

## Start page and tabs

- Customize the start page by showing or hiding ASTER branding, quick-access shortcuts, and bookmarks.
- Choose one of four backgrounds: Violet, Midnight, Ocean, or Crimson.
- View open tabs in a two-column card layout with page titles, domains, active-tab highlighting, close controls, and a quick new-tab action.

## Cross-device usability and navigation

ASTER adapts its browser chrome to compact Android phones and expanded windows on tablets, foldables, and other resizable Android devices. It also supports:
- Desktop-site mode.
- Reader Mode for simplified article reading.
- Page translation through Google Translate, with a selectable target language.
- Adjustable webpage text size.
- File uploads through Android's system file picker.
- Opening HTTP/HTTPS links from other apps when ASTER is selected as the browser.
- Hardware keyboard shortcuts: Ctrl+L (address bar), Ctrl+T (new tab), Ctrl+W (close tab), Ctrl+R (reload), Ctrl+Tab / Ctrl+Shift+Tab (cycle tabs), and Esc (exit Reader Mode).
- Backup and restore for the new display preferences.

ASTER remains an Android application; responsive layouts do not make it a native iOS, Windows, or macOS browser. Desktop-site mode still uses Android System WebView. Translation requires an internet connection, and Reader Mode may not extract every complex page perfectly.

## Backup and restore

Use **Backup & restore** from the browser menu to export or import bookmarks, history, settings, and open tabs as JSON. Backups do not include cookies, passwords, cache, or WebView form data.

## Updates

ASTER checks GitHub Releases and can download an APK update for Android's package installer. Android requires user confirmation. Updating an installed copy also requires every release to use the same permanent signing key; until repository signing is configured, debug APKs are test builds and may not install over an existing copy.

## Build

The Android project uses Gradle and Android Gradle Plugin. GitHub Actions builds the APK. See `.github/workflows/build-android.yml`.




Release 0.14.0 delivers responsive Android layouts, Reader Mode, desktop-site mode, page translation, adjustable text size, system file uploads, external link handling, and keyboard navigation.
