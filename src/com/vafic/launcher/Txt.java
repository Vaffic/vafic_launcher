package com.vafic.launcher;

import android.content.Context;

import java.util.Locale;

/**
 * Tiny inline localization helper.
 *
 * The project builds with aapt2 only, without a res/ pipeline, so UI strings
 * live next to the code that uses them. Every call carries the Russian and
 * English variant; the preference decides which one is returned.
 */
public final class Txt {

    public static final String SYSTEM = "system";
    public static final String RU = "ru";
    public static final String EN = "en";

    private Txt() {
    }

    public static boolean russian(Context c) {
        String pref = Prefs.language(c);
        if (RU.equals(pref)) return true;
        if (EN.equals(pref)) return false;
        return "ru".equals(Locale.getDefault().getLanguage());
    }

    public static Locale locale(Context c) {
        return russian(c) ? new Locale("ru") : Locale.ENGLISH;
    }

    public static String s(Context c, String ru, String en) {
        return russian(c) ? ru : en;
    }

    public static String languageName(Context c) {
        String pref = Prefs.language(c);
        if (RU.equals(pref)) return "Русский";
        if (EN.equals(pref)) return "English";
        return s(c, "Системный", "System");
    }

}
