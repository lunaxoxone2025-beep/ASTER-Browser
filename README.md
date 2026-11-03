# ASTER Browser

Current Android version: **0.12.0**.

ASTER is an Android-first browser prototype with a dark violet interface, tabbed browsing, bookmarks, history, downloads, and privacy controls. Its visual direction is original and takes broad inspiration from modern browsers such as Brave and Opera GX.

## Engine and platform

ASTER currently uses Android System WebView. It is not a standalone Chromium browser engine, so some browser-level capabilities and extensions are outside its reach.

## ASTER Shield — ad and tracker blocking

- Uses a local cache of supported domain network rules imported from EasyList and EasyPrivacy.
- Includes built-in advertising and analytics domains.
- Refreshes filter lists weekly when a network connection is available; users can also request an update from the Shield panel.
- Supports global protection and per-site exceptions.
- Tracks blocked requests per page and per session.

**Important limitation:** the current WebView integration extracts domain-based network rules. It does not implement cosmetic CSS selectors, scriptlets, every ABP option, or advanced first-party ad removal. Therefore it can block many ad/tracker requests, but it cannot guarantee an ad-free page or match Brave's native Shields engine.

## Backup and restore

Use **Backup & restore** from the browser menu to export or import bookmarks, history, settings, and open tabs as JSON. Backups do not include cookies, passwords, cache, or WebView form data.

## Updates

ASTER checks GitHub Releases and can download an APK update for Android's package installer. Android requires user confirmation. Updating an installed copy also requires every release to use the same permanent signing key; until repository signing is configured, debug APKs are test builds and may not install over an existing copy.

## Build

The Android project uses Gradle and Android Gradle Plugin. GitHub Actions builds the APK. See `.github/workflows/build-android.yml`.



Release 0.12.0 includes the ASTER Shield filter engine, refreshed browser chrome, a new start page, and a grid-based browser menu.
