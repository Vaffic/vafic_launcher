package com.vafic.launcher;

import android.os.SystemClock;

/** Shared UI state: remembers whether the user is holding a D-pad direction (fast navigation). */
public final class Ui {

    private static long lastNav;
    private static long fastUntil;

    private Ui() {
    }

    /** Called on every D-pad direction press. */
    public static void nav() {
        long now = SystemClock.uptimeMillis();
        if (now - lastNav < 240) fastUntil = now + 340;
        lastNav = now;
    }

    /** True while the user scrolls quickly; cards then use a shorter, calmer animation. */
    public static boolean fast() {
        return SystemClock.uptimeMillis() < fastUntil;
    }

    /**
     * Turns off the framework's click sound for a whole window. The launcher
     * plays its own ticks and notes, so a second system click is unwanted.
     */
    static void disableSystemClicks(android.view.View view) {
        if (view == null) return;
        view.setSoundEffectsEnabled(false);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                disableSystemClicks(group.getChildAt(i));
            }
        }
    }
}
