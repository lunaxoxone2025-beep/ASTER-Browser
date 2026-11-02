# Changelog

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
