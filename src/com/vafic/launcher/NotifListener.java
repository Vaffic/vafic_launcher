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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Collects active notifications: a count for the badge, details for the panel and heads-up events. */
public class NotifListener extends NotificationListenerService {

    public static final String ACTION = "com.vafic.launcher.NOTIFICATIONS_CHANGED";
    public static final String ACTION_HEADS_UP = "com.vafic.launcher.NOTIFICATION_HEADS_UP";

    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_APP = "app";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_TIME = "time";

    private static NotifListener instance;
    private boolean knownReady;

    /**
     * Notification key -> title/text fingerprint. Existing notifications are
     * seeded here when the listener connects, so only genuinely new content
     * produces a popup. An app that updates the text of an existing
     * notification counts as new content, while a silent re-post does not.
     */
    private static final Map<String, String> KNOWN = new HashMap<>();

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
        knownReady = false;
    }

    @Override
    public void onListenerConnected() {
        instance = this;
        refresh();
        syncKnown();
        knownReady = true;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!knownReady) {
            // The service can receive a post before the connected callback.
            // Treat the current screen as the baseline instead of popping a
            // banner for every notification that was already there.
            syncKnown();
            knownReady = true;
        }
        boolean announce = false;
        if (sbn != null) {
            String key = sbn.getKey();
            String fingerprint = fingerprint(sbn);
            synchronized (KNOWN) {
                String previous = KNOWN.put(key, fingerprint);
                announce = previous == null || !previous.equals(fingerprint);
            }
        }
        refresh();
        if (announce) announceHeadsUp(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) {
            synchronized (KNOWN) {
                KNOWN.remove(sbn.getKey());
            }
        }
        refresh();
    }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        knownReady = false;
        synchronized (KNOWN) {
            KNOWN.clear();
        }
        super.onDestroy();
    }

    /** Seeds the fingerprint map with everything that is already on screen. */
    private void syncKnown() {
        Map<String, String> next = new HashMap<>();
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    if (sbn != null) next.put(sbn.getKey(), fingerprint(sbn));
                }
            }
        } catch (Exception ignored) {
        }
        synchronized (KNOWN) {
            KNOWN.clear();
            KNOWN.putAll(next);
        }
    }

    /**
     * Sends one popup event. The system chatter and per-app filter are checked
     * here, and foreground-service notifications (media players, downloads)
     * are kept out of the way; their text changes far too often for a banner.
     */
    private void announceHeadsUp(StatusBarNotification sbn) {
        try {
            if (!keep(sbn)) return;
            String pkg = sbn.getPackageName();
            if (pkg == null || Prefs.blocked(this, pkg)) return;
            Notification n = sbn.getNotification();
            if (n == null) return;
            if ((n.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0) return;

            Entry e = read(getPackageManager(), sbn);
            Intent i = new Intent(ACTION_HEADS_UP);
            i.setPackage(getPackageName());
            i.putExtra(EXTRA_PKG, e.pkg);
            i.putExtra(EXTRA_APP, e.app);
            i.putExtra(EXTRA_TITLE, e.title);
            i.putExtra(EXTRA_TEXT, e.text);
            i.putExtra(EXTRA_TIME, e.time);
            sendBroadcast(i);
        } catch (Exception ignored) {
        }
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
                    list.add(read(pm, sbn));
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

    /** Reads the fields the panel and the popup both need. */
    private static Entry read(PackageManager pm, StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        Notification n = sbn.getNotification();
        Bundle ex = n == null ? null : n.extras;
        CharSequence title = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TEXT);
        if (isEmpty(text) && ex != null) {
            text = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }
        if (isEmpty(text) && n != null) text = n.tickerText;
        return new Entry(pkg, appName(pm, pkg), str(title), str(text), sbn.getPostTime(),
                n == null ? null : n.getSmallIcon(), n == null ? null : n.getLargeIcon(),
                sbn.getKey(), sbn.isClearable());
    }

    /**
     * The visible content of a notification. The post time is intentionally
     * left out: progress updates and chronometers would otherwise pop the
     * banner every few seconds.
     */
    private static String fingerprint(StatusBarNotification sbn) {
        Notification n = sbn == null ? null : sbn.getNotification();
        Bundle ex = n == null ? null : n.extras;
        CharSequence title = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = ex == null ? null : ex.getCharSequence(Notification.EXTRA_TEXT);
        if (isEmpty(text) && ex != null) {
            text = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        }
        if (isEmpty(text) && n != null) text = n.tickerText;
        return str(title) + "\u0000" + str(text);
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
