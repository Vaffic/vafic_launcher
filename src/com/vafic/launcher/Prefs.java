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

    /** The dock can hold 3 to 8 apps; the limit is user-configurable. */
    public static final int MIN_DOCK = 3;
    public static final int MAX_DOCK = 8;
    public static final int DEFAULT_DOCK = 5;

    private static final String FILE = "vafic";
    private static final String KEY_HOME = "home";
    private static final String KEY_LIBRARY_ORDER = "library_order";
    private static final String KEY_MUSIC = "music";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_SFX = "sfx";
    private static final String KEY_SFX_VOLUME = "sfx_volume";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_BLOCKED = "blocked_notifications";
    private static final String KEY_HIDDEN = "hidden_apps";
    private static final String KEY_DOCK_SIZE = "dock_size";
    private static final String KEY_WEATHER = "weather";
    private static final String KEY_WEATHER_FORECAST = "weather_forecast";
    private static final String KEY_WEATHER_CITY = "weather_city";
    private static final String KEY_WEATHER_LAT = "weather_lat";
    private static final String KEY_WEATHER_LON = "weather_lon";
    private static final String KEY_WEATHER_CACHE = "weather_cache";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_WALLPAPER_DIM = "wallpaper_dim";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Dock apps, never more than the configured dock size. */
    public static List<String> home(Context c) {
        String s = sp(c).getString(KEY_HOME, null);
        List<String> out = new ArrayList<>();
        if (s != null && !s.trim().isEmpty()) {
            out.addAll(Arrays.asList(s.split(",")));
        }
        int max = dockSize(c);
        if (out.size() > max) {
            out = new ArrayList<>(out.subList(0, max));
        }
        return out;
    }

    public static void save(Context c, List<String> pkgs) {
        int max = dockSize(c);
        List<String> cut = pkgs.size() > max ? pkgs.subList(0, max) : pkgs;
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
        if (cur.size() >= dockSize(c)) return false;
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

    /** Background music level, 0..1. The default keeps it well under the interface sounds. */
    public static float musicVolume(Context c) {
        return clamp(readFloat(sp(c), KEY_MUSIC_VOLUME, 0.16f), 0f, 1f);
    }

    public static void setMusicVolume(Context c, float volume) {
        sp(c).edit().putFloat(KEY_MUSIC_VOLUME, clamp(volume, 0f, 1f)).apply();
    }

    /** Interface sound level, 0..1. 1 is the level the launcher was tuned with. */
    public static float sfxVolume(Context c) {
        return clamp(readFloat(sp(c), KEY_SFX_VOLUME, 1f), 0f, 1f);
    }

    public static void setSfxVolume(Context c, float volume) {
        sp(c).edit().putFloat(KEY_SFX_VOLUME, clamp(volume, 0f, 1f)).apply();
    }

    // ---- dock ----

    /** How many apps the dock can hold, 3 to 8. */
    public static int dockSize(Context c) {
        return Math.max(MIN_DOCK, Math.min(MAX_DOCK, readInt(sp(c), KEY_DOCK_SIZE, DEFAULT_DOCK)));
    }

    /** Shrinking the dock drops the apps that no longer fit, so they never come back by surprise. */
    public static void setDockSize(Context c, int size) {
        int value = Math.max(MIN_DOCK, Math.min(MAX_DOCK, size));
        sp(c).edit().putInt(KEY_DOCK_SIZE, value).apply();
        List<String> cur = home(c);
        save(c, cur);
    }

    // ---- weather ----

    public static boolean weather(Context c) {
        return sp(c).getBoolean(KEY_WEATHER, false);
    }

    public static void setWeather(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_WEATHER, on).apply();
    }

    public static boolean weatherForecast(Context c) {
        return sp(c).getBoolean(KEY_WEATHER_FORECAST, true);
    }

    public static void setWeatherForecast(Context c, boolean on) {
        sp(c).edit().putBoolean(KEY_WEATHER_FORECAST, on).apply();
    }

    /** City name shown to the user; empty means "detect by IP". */
    public static String weatherCity(Context c) {
        String s = sp(c).getString(KEY_WEATHER_CITY, "");
        return s == null ? "" : s;
    }

    /** True once a place (city name + coordinates) has been resolved. */
    public static boolean hasWeatherPlace(Context c) {
        return !Float.isNaN(sp(c).getFloat(KEY_WEATHER_LAT, Float.NaN));
    }

    public static float weatherLat(Context c) {
        return sp(c).getFloat(KEY_WEATHER_LAT, Float.NaN);
    }

    public static float weatherLon(Context c) {
        return sp(c).getFloat(KEY_WEATHER_LON, Float.NaN);
    }

    public static void setWeatherPlace(Context c, String city, double lat, double lon) {
        sp(c).edit()
                .putString(KEY_WEATHER_CITY, city == null ? "" : city)
                .putFloat(KEY_WEATHER_LAT, (float) lat)
                .putFloat(KEY_WEATHER_LON, (float) lon)
                .apply();
    }

    public static void setWeatherCityOnly(Context c, String city) {
        sp(c).edit().putString(KEY_WEATHER_CITY, city == null ? "" : city).apply();
    }

    public static void clearWeatherPlace(Context c) {
        sp(c).edit().remove(KEY_WEATHER_CITY).remove(KEY_WEATHER_LAT)
                .remove(KEY_WEATHER_LON).apply();
    }

    public static String weatherCache(Context c) {
        String s = sp(c).getString(KEY_WEATHER_CACHE, "");
        return s == null ? "" : s;
    }

    public static void setWeatherCache(Context c, String value) {
        sp(c).edit().putString(KEY_WEATHER_CACHE, value).apply();
    }

    // ---- wallpaper ----

    /** Absolute path of the custom wallpaper, or an empty string for the built-in one. */
    public static String wallpaper(Context c) {
        String s = sp(c).getString(KEY_WALLPAPER, "");
        return s == null ? "" : s;
    }

    public static void setWallpaper(Context c, String path) {
        sp(c).edit().putString(KEY_WALLPAPER, path == null ? "" : path).apply();
    }

    /** How much the wallpaper is darkened for readability, 0..80 percent. */
    public static int wallpaperDim(Context c) {
        return Math.max(0, Math.min(80, readInt(sp(c), KEY_WALLPAPER_DIM, 35)));
    }

    public static void setWallpaperDim(Context c, int percent) {
        sp(c).edit().putInt(KEY_WALLPAPER_DIM, Math.max(0, Math.min(80, percent))).apply();
    }

    /**
     * SharedPreferences throws when a key holds another type. Early test builds
     * of 1.2 stored the volume as an integer percent, so values are read as a
     * number and converted instead of trusting getFloat().
     */
    private static float readFloat(SharedPreferences sp, String key, float def) {
        try {
            Object value = sp.getAll().get(key);
            float v;
            if (value instanceof Number) {
                v = ((Number) value).floatValue();
            } else if (value instanceof String) {
                v = Float.parseFloat((String) value);
            } else {
                return def;
            }
            // A whole percent (16, 50, 100) is folded back into 0..1.
            if (v > 1f && v <= 100f) v /= 100f;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    private static int readInt(SharedPreferences sp, String key, int def) {
        try {
            Object value = sp.getAll().get(key);
            if (value instanceof Number) return ((Number) value).intValue();
            if (value instanceof String) return Integer.parseInt((String) value);
        } catch (Exception e) {
            // Fall through to the default.
        }
        return def;
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : (v > max ? max : v);
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
