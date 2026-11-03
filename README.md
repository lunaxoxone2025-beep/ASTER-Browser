# ASTER Browser

Current Android version: **0.13.0**.

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

## Start page and tabs

- Customize the start page by showing or hiding ASTER branding, quick-access shortcuts, and bookmarks.
- Choose one of four backgrounds: Violet, Midnight, Ocean, or Crimson.
- View open tabs in a two-column card layout with page titles, domains, active-tab highlighting, close controls, and a quick new-tab action.

## Backup and restore

Use **Backup & restore** from the browser menu to export or import bookmarks, history, settings, and open tabs as JSON. Backups do not include cookies, passwords, cache, or WebView form data.

## Updates

ASTER checks GitHub Releases and can download an APK update for Android's package installer. Android requires user confirmation. Updating an installed copy also requires every release to use the same permanent signing key; until repository signing is configured, debug APKs are test builds and may not install over an existing copy.

## Build

The Android project uses Gradle and Android Gradle Plugin. GitHub Actions builds the APK. See `.github/workflows/build-android.yml`.



Release 0.13.0 adds start-page customization, four visual themes, and a card-based tab switcher, while retaining ASTER Shield network filtering.
