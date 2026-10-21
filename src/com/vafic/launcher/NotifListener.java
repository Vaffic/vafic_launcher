package com.vafic.launcher;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Collects active notifications: a count for the badge and details for the panel. */
public class NotifListener extends NotificationListenerService {

    public static final String ACTION = "com.vafic.launcher.NOTIFICATIONS_CHANGED";

    private static NotifListener instance;

    public static class Entry {
        public final String pkg;
        public final String app;
        public final String title;
        public final String text;
        public final long time;
        public final Icon small;
        public final Icon large;
        public final String key;
        public final boolean clearable;

        Entry(String pkg, String app, String title, String text, long time, Icon small, Icon large,
              String key, boolean clearable) {
            this.pkg = pkg;
            this.app = app;
            this.title = title;
            this.text = text;
            this.time = time;
            this.small = small;
            this.large = large;
            this.key = key;
            this.clearable = clearable;
        }
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static List<Entry> entries() {
        synchronized (ENTRIES) {
            return new ArrayList<>(ENTRIES);
        }
    }

    public static int count() {
        synchronized (ENTRIES) {
            return ENTRIES.size();
        }
    }

    /** Re-reads the active notifications after the per-app filter changes. */
    public static void reload() {
        NotifListener s = instance;
        if (s != null) {
            try {
                s.refresh();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Dismisses every notification the shade currently shows and that the system
     * allows to be cleared. Ongoing and FLAG_NO_CLEAR notifications are kept.
     */
    public static int clearVisible() {
        NotifListener s = instance;
        if (s == null) return 0;
        int cleared = 0;
        try {
            Set<String> blocked = Prefs.blocked(s);
            StatusBarNotification[] active = s.getActiveNotifications();
            if (active == null) return 0;
            for (StatusBarNotification sbn : active) {
                if (sbn == null || !keep(sbn) || !sbn.isClearable()) continue;
                if (blocked.contains(sbn.getPackageName())) continue;
                try {
                    s.cancelNotification(sbn.getKey());
                    cleared++;
                } catch (Exception ignored) {
                }
            }
            if (cleared > 0) s.refresh();
        } catch (Exception ignored) {
        }
        return cleared;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onListenerConnected() {
        instance = this;
        refresh();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        refresh();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        refresh();
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    private void refresh() {
        List<Entry> list = new ArrayList<>();
        try {
            Set<String> blocked = Prefs.blocked(getApplicationContext());
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                List<StatusBarNotification> sorted = new ArrayList<>();
                for (StatusBarNotification sbn : active) {
                    if (sbn != null) sorted.add(sbn);
                }
                Collections.sort(sorted, new Comparator<StatusBarNotification>() {
                    @Override
                    public int compare(StatusBarNotification a, StatusBarNotification b) {
                        return Long.compare(b.getPostTime(), a.getPostTime());
                    }
                });

                PackageManager pm = getPackageManager();
                for (StatusBarNotification sbn : sorted) {
                    if (!keep(sbn)) continue;
                    String pkg = sbn.getPackageName();
                    if (blocked.contains(pkg)) continue;
                    Notification n = sbn.getNotification();
                    Bundle ex = n.extras;
                    CharSequence title = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TITLE);
                    CharSequence text = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TEXT);
                    if (isEmpty(text) && ex != null) {
                        text = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
                    }
                    if (isEmpty(text)) text = n.tickerText;

                    list.add(new Entry(pkg, appName(pm, pkg), str(title), str(text),
                            sbn.getPostTime(), n.getSmallIcon(), n.getLargeIcon(),
                            sbn.getKey(), sbn.isClearable()));
                    if (list.size() >= 24) break;
                }
            }
        } catch (Exception ignored) {
        }

        synchronized (ENTRIES) {
            ENTRIES.clear();
            ENTRIES.addAll(list);
        }
        Intent i = new Intent(ACTION);
        i.setPackage(getPackageName());
        sendBroadcast(i);
    }

    /** System chatter (USB debugging, accessibility, cast) is noise on a TV. */
    private static boolean keep(StatusBarNotification sbn) {
        if (sbn == null) return false;
        String pkg = sbn.getPackageName();
        if (pkg == null) return false;
        if (pkg.equals("android") || pkg.startsWith("com.android.systemui")
                || pkg.startsWith("com.android.tv.settings")) return false;

        Notification n = sbn.getNotification();
        if (n == null) return false;
        int group = Notification.FLAG_GROUP_SUMMARY;
        return (n.flags & group) == 0;
    }

    private static String appName(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private static boolean isEmpty(CharSequence s) {
        return s == null || s.toString().trim().isEmpty();
    }

    private static String str(CharSequence s) {
        return s == null ? "" : s.toString().trim();
    }
}
