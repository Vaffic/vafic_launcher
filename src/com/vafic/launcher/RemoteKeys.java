package com.vafic.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.KeyEvent;

/**
 * Hardware keys that a TV remote sends to the foreground app: the gear
 * (KEYCODE_SETTINGS) and the "TV" button (KEYCODE_TV). Some firmwares let their
 * stock launcher handle these, so a third-party launcher has to do it itself.
 *
 * The key is only swallowed when something was actually started. Otherwise it
 * is passed on, so the system can still deal with it.
 */
public final class RemoteKeys {

    /** Live TV apps that exist on common Android TV builds. */
    private static final String[] LIVE_TV_PACKAGES = {
            "com.google.android.tv",
            "com.android.tv"
    };

    private static boolean consumed;

    private RemoteKeys() {
    }

    /** Returns true when the event was handled and must not go any further. */
    public static boolean handle(Activity a, KeyEvent e) {
        int code = e.getKeyCode();
        if (code != KeyEvent.KEYCODE_SETTINGS && code != KeyEvent.KEYCODE_TV) return false;

        if (e.getAction() == KeyEvent.ACTION_DOWN) {
            if (e.getRepeatCount() > 0) return consumed;
            consumed = code == KeyEvent.KEYCODE_SETTINGS ? openSettings(a) : openLiveTv(a);
            return consumed;
        }
        boolean result = consumed;
        if (e.getAction() == KeyEvent.ACTION_UP) consumed = false;
        return result;
    }

    private static boolean openSettings(Activity a) {
        return start(a, new Intent(Settings.ACTION_SETTINGS));
    }

    private static boolean openLiveTv(Activity a) {
        // The standard way: ask for the TV channel list.
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse("content://android.media.tv/channel"));
        if (canStart(a, view) && start(a, view)) return true;

        // Known Live TV apps.
        PackageManager pm = a.getPackageManager();
        for (String pkg : LIVE_TV_PACKAGES) {
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i != null && start(a, i)) return true;
        }
        return false;
    }

    private static boolean canStart(Activity a, Intent i) {
        try {
            return i.resolveActivity(a.getPackageManager()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean start(Activity a, Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
