package com.vafic.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves the apps shown on the dock, in the library and in the notification filter. */
public final class AppRepo {

    /**
     * The artwork of every installed app. Decoding a dozen TV icons and
     * banners is the slowest part of opening the library, so it is done in
     * the background and kept here.
     */
    private static final Object ART_LOCK = new Object();
    private static final Map<String, Drawable> ICONS = new HashMap<>();
    private static final Map<String, Drawable> BANNERS = new HashMap<>();
    private static volatile boolean prefetchPaused;

    public static final class App {
        public final String pkg;
        public final String label;
        public final Drawable icon;
        public final Drawable banner;
        public final boolean system;

        App(String pkg, String label, Drawable icon, Drawable banner) {
            this(pkg, label, icon, banner, false);
        }

        App(String pkg, String label, Drawable icon, Drawable banner, boolean system) {
            this.pkg = pkg;
            this.label = label;
            this.icon = icon;
            this.banner = banner;
            this.system = system;
        }
    }

    private AppRepo() {
    }

    public static boolean isLaunchable(Context c, String pkg) {
        return c.getPackageManager().getLaunchIntentForPackage(pkg) != null;
    }

    /** True while the package is installed, even if it has no launcher activity. */
    public static boolean isInstalled(Context c, String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return false;
        try {
            c.getPackageManager().getApplicationInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** A cached app icon, without decoding the wide banner as well. */
    public static Drawable icon(Context c, String pkg) {
        return cachedIcon(c, pkg);
    }

    /** Drops artwork for one package after an update or uninstall. */
    public static void invalidate(String pkg) {
        if (pkg == null) return;
        synchronized (ART_LOCK) {
            ICONS.remove(pkg);
            BANNERS.remove(pkg);
        }
    }

    /** Drops every decoded icon and banner; used when several packages changed at once. */
    public static void invalidateAll() {
        synchronized (ART_LOCK) {
            ICONS.clear();
            BANNERS.clear();
        }
    }

    /**
     * A stable fingerprint of the launchable apps and their update times. The
     * launcher compares it on resume, so an install that happened while the
     * package receiver was unregistered is not missed.
     */
    public static String installedSignature(Context c) {
        PackageManager pm = c.getPackageManager();
        Set<String> seen = new HashSet<>();
        List<String> parts = new ArrayList<>();
        for (ResolveInfo ri : launchables(pm)) {
            String pkg = ri.activityInfo == null ? null : ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            long updated = 0L;
            try {
                PackageInfo pi = pm.getPackageInfo(pkg, 0);
                updated = pi.lastUpdateTime;
            } catch (Exception ignored) {
            }
            parts.add(pkg + ":" + updated);
        }
        Collections.sort(parts);
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() > 0) b.append(',');
            b.append(part);
        }
        return b.toString();
    }

    public static App resolve(Context c, String pkg, String fallbackLabel) {
        PackageManager pm = c.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            Drawable icon = cachedIcon(c, pkg);
            String label = fallbackLabel;
            try {
                CharSequence l = pm.getApplicationLabel(ai);
                if (l != null && l.length() > 0) label = l.toString();
            } catch (Exception ignored) {
            }
            Drawable banner = cachedBanner(c, pkg, ai);
            return new App(pkg, label, icon, banner);
        } catch (Exception e) {
            return null;
        }
    }

    /** The app icon, decoded once and kept for the next time it is needed. */
    private static Drawable cachedIcon(Context c, String pkg) {
        synchronized (ART_LOCK) {
            Drawable d = ICONS.get(pkg);
            if (d != null) return d;
        }
        Drawable d = null;
        try {
            d = c.getPackageManager().getApplicationIcon(pkg);
        } catch (Exception ignored) {
        }
        if (d != null) {
            synchronized (ART_LOCK) {
                ICONS.put(pkg, d);
            }
        }
        return d;
    }

    /** The wide artwork of the app, if it has one. */
    private static Drawable cachedBanner(Context c, String pkg, ApplicationInfo ai) {
        synchronized (ART_LOCK) {
            Drawable d = BANNERS.get(pkg);
            if (d != null) return d;
        }
        Drawable d = null;
        try {
            if (ai.banner != 0) d = c.getPackageManager().getDrawable(pkg, ai.banner, ai);
        } catch (Exception ignored) {
        }
        if (d != null) {
            synchronized (ART_LOCK) {
                BANNERS.put(pkg, d);
            }
        }
        return d;
    }

    /**
     * Decodes the artwork of every launchable app in the background. The
     * library is opened with the remote, and doing that work on the UI thread
     * would stall the page for a moment.
     *
     * The thread runs at the lowest priority and stops as soon as the
     * launcher goes to the background, so it never competes with the app the
     * user is watching.
     */
    public static void prefetchArtwork(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Exception ignored) {
                }
                try {
                    PackageManager pm = app.getPackageManager();
                    for (ResolveInfo ri : launchables(pm)) {
                        if (prefetchPaused) return;
                        String pkg = ri.activityInfo.packageName;
                        if (pkg.equals(app.getPackageName())) continue;
                        cachedIcon(app, pkg);
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                            cachedBanner(app, pkg, ai);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }, "vafic-art-prefetch").start();
    }

    /** The launcher stops warming the artwork as soon as it is not in front. */
    public static void setPrefetchPaused(boolean paused) {
        prefetchPaused = paused;
    }

    public static List<App> homeApps(Context c) {
        List<App> out = new ArrayList<>();
        for (String pkg : Prefs.home(c)) {
            if (!isLaunchable(c, pkg)) continue;
            App a = resolve(c, pkg, pkg);
            if (a != null) out.add(a);
        }
        return out;
    }

    public static List<App> allApps(Context c) {
        PackageManager pm = c.getPackageManager();
        List<ResolveInfo> list = launchables(pm);
        List<App> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> hidden = Prefs.hidden(c);
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            if (hidden.contains(pkg)) continue;
            String label = ri.loadLabel(pm).toString();
            App a = resolve(c, pkg, label);
            if (a == null) {
                Drawable icon = ri.loadIcon(pm);
                a = new App(pkg, label, icon, null);
            }
            out.add(a);
        }
        sort(out);
        applyLibraryOrder(c, out);
        return out;
    }

    /** Launchable apps that are hidden from the library, sorted by label. */
    public static List<App> hiddenApps(Context c) {
        Set<String> hidden = Prefs.hidden(c);
        List<App> out = new ArrayList<>();
        if (hidden.isEmpty()) return out;
        PackageManager pm = c.getPackageManager();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : launchables(pm)) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            if (!hidden.contains(pkg)) continue;
            String label;
            try {
                label = ri.loadLabel(pm).toString();
            } catch (Exception e) {
                label = pkg;
            }
            Drawable icon;
            try {
                icon = ri.loadIcon(pm);
            } catch (Exception e) {
                icon = null;
            }
            out.add(new App(pkg, label, icon, null));
        }
        sort(out);
        return out;
    }

    /** Applies the user's library order, keeping newly installed apps alphabetical at the end. */
    private static void applyLibraryOrder(Context c, List<App> apps) {
        final List<String> order = Prefs.libraryOrder(c);
        if (order.isEmpty()) return;
        final Map<String, Integer> rank = new HashMap<>();
        for (int i = 0; i < order.size(); i++) rank.put(order.get(i), i);
        Collections.sort(apps, new Comparator<App>() {
            @Override
            public int compare(App a, App b) {
                Integer ra = rank.get(a.pkg);
                Integer rb = rank.get(b.pkg);
                int va = ra == null ? Integer.MAX_VALUE : ra;
                int vb = rb == null ? Integer.MAX_VALUE : rb;
                if (va != vb) return va < vb ? -1 : 1;
                return a.label.compareToIgnoreCase(b.label);
            }
        });
    }

    /** Lightweight list for the notification filter: labels and icons only, no banners. */
    public static List<App> notifApps(Context c) {
        return notifApps(c, false);
    }

    /**
     * Notification filter list. With includeSystem=true every installed package is listed,
     * including system, hidden and non-launchable apps; otherwise only launchable apps.
     */
    public static List<App> notifApps(Context c, boolean includeSystem) {
        if (includeSystem) return installedApps(c);

        PackageManager pm = c.getPackageManager();
        List<ResolveInfo> list = launchables(pm);
        List<App> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(c.getPackageName()) || !seen.add(pkg)) continue;
            String label;
            try {
                label = ri.loadLabel(pm).toString();
            } catch (Exception e) {
                label = pkg;
            }
            Drawable icon;
            try {
                icon = ri.loadIcon(pm);
            } catch (Exception e) {
                icon = null;
            }
            out.add(new App(pkg, label, icon, null));
        }
        sort(out);
        return out;
    }

    /** Every installed package, system and hidden included. Icons may be loaded off the UI thread. */
    private static List<App> installedApps(Context c) {
        PackageManager pm = c.getPackageManager();
        List<ApplicationInfo> installed;
        try {
            installed = pm.getInstalledApplications(0);
        } catch (Exception e) {
            installed = new ArrayList<>();
        }
        List<App> out = new ArrayList<>();
        String self = c.getPackageName();
        for (ApplicationInfo ai : installed) {
            if (ai == null || ai.packageName == null || ai.packageName.equals(self)) continue;
            String label;
            try {
                CharSequence l = pm.getApplicationLabel(ai);
                label = l == null ? ai.packageName : l.toString();
            } catch (Exception e) {
                label = ai.packageName;
            }
            if (label == null || label.trim().isEmpty()) label = ai.packageName;
            Drawable icon = null;
            try {
                icon = ai.loadIcon(pm);
            } catch (Exception ignored) {
            }
            boolean system = (ai.flags & (ApplicationInfo.FLAG_SYSTEM
                    | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
            out.add(new App(ai.packageName, label, icon, null, system));
        }
        sort(out);
        return out;
    }

    private static List<ResolveInfo> launchables(PackageManager pm) {
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        if (list.isEmpty()) {
            main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            list = pm.queryIntentActivities(main, 0);
        }
        return list;
    }

    private static void sort(List<App> apps) {
        Collections.sort(apps, new Comparator<App>() {
            @Override
            public int compare(App a, App b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
    }

    public static Intent launch(Context c, String pkg) {
        return c.getPackageManager().getLaunchIntentForPackage(pkg);
    }
}
