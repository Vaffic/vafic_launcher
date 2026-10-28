package com.vafic.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Checks and requests the "notification access" special permission. */
public final class NotifAccess {

    private static final String PREFS = "vafic_notif_access";
    private static final String KEY_NEVER_ASK = "never_ask";

    private static boolean askedThisSession;

    private NotifAccess() {
    }

    /** True when the user enabled this launcher in the system notification access list. */
    public static boolean isGranted(Context c) {
        try {
            String flat = Settings.Secure.getString(c.getContentResolver(),
                    "enabled_notification_listeners");
            if (flat == null || flat.isEmpty()) return false;
            String me = c.getPackageName();
            for (String part : flat.split(":")) {
                ComponentName cn = ComponentName.unflattenFromString(part);
                if (cn != null && me.equals(cn.getPackageName())) return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** Whether the launcher should offer the permission on its own (once per start). */
    public static boolean shouldAskOnLaunch(Context c) {
        if (askedThisSession || isGranted(c)) return false;
        return !c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_NEVER_ASK, false);
    }

    public static void markAsked() {
        askedThisSession = true;
    }

    /** "Don't remind me": the status stays available in the settings. */
    public static void neverAsk(Context c) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        e.putBoolean(KEY_NEVER_ASK, true);
        e.apply();
    }

    /**
     * Opens the app info screen. On Android 13+ the "Allow restricted settings"
     * item for sideloaded apps lives in its overflow menu.
     */
    public static void openAppInfo(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", c.getPackageName(), null));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            Toast.makeText(c, Txt.s(c,
                    "Нажмите ⋮ → «Разрешить ограниченные настройки», затем снова «Выдать доступ»",
                    "Press ⋮ → “Allow restricted settings”, then “Grant access” again"),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(c, Txt.s(c,
                    "Настройки → Приложения → Vafic Launcher → ⋮ → Разрешить ограниченные настройки",
                    "Settings → Apps → Vafic Launcher → ⋮ → Allow restricted settings"),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Opens the system screen where notification access is granted. */
    public static void open(Context c) {
        List<Intent> tries = new ArrayList<>();
        ComponentName cn = new ComponentName(c, NotifListener.class);
        if (Build.VERSION.SDK_INT >= 30) {
            Intent detail = new Intent("android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS");
            detail.putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME",
                    cn.flattenToString());
            tries.add(detail);
        }
        tries.add(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        tries.add(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", c.getPackageName(), null)));
        tries.add(new Intent(Settings.ACTION_SETTINGS));

        for (Intent i : tries) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return;
            } catch (Exception ignored) {
            }
        }
        Toast.makeText(c, Txt.s(c,
                "Не удалось открыть настройки. Включите доступ вручную: "
                        + "Настройки → Приложения → Специальный доступ → Доступ к уведомлениям",
                "Could not open settings. Enable it manually: Settings → Apps → "
                        + "Special access → Notification access"), Toast.LENGTH_LONG).show();
    }
}
