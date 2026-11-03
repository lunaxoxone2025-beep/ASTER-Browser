# Changelog

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
