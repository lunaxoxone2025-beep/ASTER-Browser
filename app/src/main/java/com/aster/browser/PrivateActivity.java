package com.aster.browser;

import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;

import java.io.File;

/**
 * Private window: the full ASTER browser running in its own process with a separate WebView
 * data directory (Android 9+). History, sessions and saved data are never written, site data is
 * wiped when the window closes, and any leftover private profile is deleted on the next start.
 */
public class PrivateActivity extends MainActivity {
    private static boolean profileReady;

    @Override protected boolean isPrivateWindow() { return true; }

    @Override protected void onCreate(Bundle state) {
        if (Build.VERSION.SDK_INT >= 28 && !profileReady) {
            // Fresh private process: remove anything a previous private session left on disk,
            // then select the private profile before the first WebView is created.
            deleteMatching(getDataDir(), "aster_private");
            deleteMatching(getCacheDir(), "aster_private");
            try {
                WebView.setDataDirectorySuffix("aster_private");
            } catch (IllegalStateException alreadyInitialized) {
                // WebView was already initialised in this process with the private profile.
            }
            profileReady = true;
        }
        super.onCreate(state);
    }

    private static void deleteMatching(File parent, String token) {
        File[] children = parent == null ? null : parent.listFiles();
        if (children == null) return;
        for (File f : children) if (f.getName().contains(token)) deleteRecursive(f);
    }

    private static void deleteRecursive(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteRecursive(c);
        f.delete();
    }
}
