# ASTER Browser

A modern Android-first browser prototype, with a planned desktop version.

## Project status

Early MVP. The Android app currently uses the platform WebView engine. A fully independent browser rendering engine is a separate, long-term research project.

## Build

The Android project is built with Gradle and Android Gradle Plugin. See the project source and GitHub Actions workflow for build instructions.

## Local backup and restore

Open the browser menu and choose **Backup & restore**. Export creates a JSON file using Android's document picker. Import validates the backup and asks before replacing bookmarks, history, browser settings, and open tabs.

Backups stay on the storage location chosen by the user. They do not include cookies, passwords, cached website files, or WebView form data.

## ASTER Shield

ASTER Shield is a lightweight request blocker implemented with Android WebView request interception. It includes a built-in set of known advertising and analytics hosts, a global enable/disable control, per-host exceptions, and session counters. Its compact built-in list is not a full filter-list engine and cannot remove all ads or cosmetic page elements.

## Versioning and GitHub Releases

Small fixes and internal refinements may be included in the next release. Significant feature updates receive a version bump and a GitHub Release with release notes. The Android workflow publishes a release when a commit on `main` is marked with `[release]`, or when a version tag such as `v0.10.0` is pushed.

When permanent signing is configured, the release includes a signed APK suitable for updating an installation signed with the same key. Until then, releases contain source and notes only; debug APKs remain separate test artifacts.


## ASTER Update

ASTER checks GitHub Releases when the app starts, every 24 hours while it remains active, and on demand from **Settings → Check for updates**. When a newer release is found, the user can download its `ASTER-Browser.apk` asset and continue through Android's package installer. Android always requires user confirmation; silent installation is not supported.

For updates to install over an existing copy, every release must be signed with the same permanent private key. Until repository signing is configured, debug APK releases are test builds and Android may reject them as updates to an already installed copy.

## Direct APK downloads

Major GitHub Releases include a directly downloadable Android APK named `ASTER-Browser.apk`.
