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
