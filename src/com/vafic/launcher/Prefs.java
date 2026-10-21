package com.vafic.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persisted launcher state: dock apps, library order, audio, language and hidden notifications. */
public final class Prefs {

    public static final int MAX_DOCK = 5;

    private static final String FILE = "vafic";
    private static final String KEY_HOME = "home";
    private static final String KEY_LIBRARY_ORDER = "library_order";
    private static final String KEY_MUSIC = "music";
    private static final String KEY_SFX = "sfx";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_BLOCKED = "blocked_notifications";
    private static final String KEY_HIDDEN = "hidden_apps";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Dock apps, never more than five. */
    public static List<String> home(Context c) {
        String s = sp(c).getString(KEY_HOME, null);
        List<String> out = new ArrayList<>();
        if (s != null && !s.trim().isEmpty()) {
            out.addAll(Arrays.asList(s.split(",")));
        }
        if (out.size() > MAX_DOCK) {
            out = new ArrayList<>(out.subList(0, MAX_DOCK));
        }
        return out;
    }

    public static void save(Context c, List<String> pkgs) {
        List<String> cut = pkgs.size() > MAX_DOCK ? pkgs.subList(0, MAX_DOCK) : pkgs;
        sp(c).edit().putString(KEY_HOME, TextUtils.join(",", cut)).apply();
    }

    /** Returns false when the dock is full and the app cannot be added. */
    public static boolean toggle(Context c, String pkg) {
        List<String> cur = home(c);
        if (cur.contains(pkg)) {
            cur.remove(pkg);
            save(c, cur);
            return true;
        }
        if (cur.size() >= MAX_DOCK) return false;
        cur.add(pkg);
        save(c, cur);
        return true;
    }

    public static void remove(Context c, String pkg) {
        List<String> cur = home(c);
        cur.remove(pkg);
        save(c, cur);
    }

    public static void move(Context c, String pkg, int delta) {
        List<String> cur = home(c);
        int i = cur.indexOf(pkg);
        if (i < 0) return;
        int j = Math.max(0, Math.min(cur.size() - 1, i + delta));
        if (i == j) return;
        cur.remove(i);
        cur.add(j, pkg);
        save(c, cur);
    }

    // ---- library order ----

    /** Explicit app order; packages not listed fall back to alphabetical order. */
    public static List<String> libraryOrder(Context c) {
        String s = sp(c).getString(KEY_LIBRARY_ORDER, "");
        List<String> out = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) return out;
        for (String p : s.split(",")) {
            if (!p.trim().isEmpty()) out.add(p.trim());
        }
        return out;
    }

    public static void saveLibraryOrder(Context c, List<String> pkgs) {
        sp(c).edit().putString(KEY_LIBRARY_ORDER, TextUtils.join(",", pkgs)).apply();
    }

    // ---- background music ----

    public static boolean music(Context c) {
        return sp(c).getBoolean(KEY_MUSIC, true);
    }

    public static void setMusic(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_MUSIC, on).apply();
    }

    // ---- interface sounds ----

    public static boolean sfx(Context c) {
        return sp(c).getBoolean(KEY_SFX, true);
    }

    public static void setSfx(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_SFX, on).apply();
    }

    // ---- language ----

    /** "system", "ru" or "en". */
    public static String language(Context c) {
        String s = sp(c).getString(KEY_LANGUAGE, Txt.SYSTEM);
        if (Txt.RU.equals(s) || Txt.EN.equals(s)) return s;
        return Txt.SYSTEM;
    }

    public static void setLanguage(Context c, String language) {
        String value = Txt.RU.equals(language) || Txt.EN.equals(language)
                ? language : Txt.SYSTEM;
        sp(c).edit().putString(KEY_LANGUAGE, value).apply();
    }

    // ---- notification blacklist ----

    public static Set<String> blocked(Context c) {
        Set<String> out = new HashSet<>();
        String s = sp(c).getString(KEY_BLOCKED, "");
        if (s == null || s.trim().isEmpty()) return out;
        for (String p : s.split(",")) {
            if (!p.trim().isEmpty()) out.add(p.trim());
        }
        return out;
    }

    public static boolean blocked(Context c, String pkg) {
        return blocked(c).contains(pkg);
    }

    public static void saveBlocked(Context c, Set<String> pkgs) {
        sp(c).edit().putString(KEY_BLOCKED, TextUtils.join(",", pkgs)).apply();
    }

    public static void toggleBlocked(Context c, String pkg) {
        Set<String> b = blocked(c);
        if (!b.remove(pkg)) b.add(pkg);
        saveBlocked(c, b);
    }

    public static void clearBlocked(Context c) {
        sp(c).edit().remove(KEY_BLOCKED).apply();
    }

    // ---- hidden apps ----

    /** Packages hidden from the "All apps" library. */
    public static Set<String> hidden(Context c) {
        Set<String> out = new HashSet<>();
        String s = sp(c).getString(KEY_HIDDEN, "");
        if (s == null || s.trim().isEmpty()) return out;
        for (String p : s.split(",")) {
            if (!p.trim().isEmpty()) out.add(p.trim());
        }
        return out;
    }

    public static boolean isHidden(Context c, String pkg) {
        return hidden(c).contains(pkg);
    }

    public static void setHidden(Context c, String pkg, boolean hide) {
        Set<String> h = hidden(c);
        if (hide) h.add(pkg); else h.remove(pkg);
        saveHidden(c, h);
    }

    public static void saveHidden(Context c, Set<String> pkgs) {
        sp(c).edit().putString(KEY_HIDDEN, TextUtils.join(",", pkgs)).apply();
    }

    public static void clearHidden(Context c) {
        sp(c).edit().remove(KEY_HIDDEN).apply();
    }
}
