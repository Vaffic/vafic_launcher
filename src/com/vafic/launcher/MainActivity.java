package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * tvOS-style home: clock, status cluster, a dock of up to five apps (or one
 * Add apps card when it is empty) and a full app library below it.
 * A single FocusRing glides between cards with a soft running highlight around the contour.
 */
public class MainActivity extends Activity {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    private static final int GRID_COLS = 5;
    // The library is a fixed three-row window. The cursor sits on the middle
    // row whenever another row exists below, so every step is one clean page
    // movement: one row leaves the top and one new row arrives at the bottom.
    private static final int LIB_PAGE_ROWS = 3;
    private static final String LIB_SETTINGS_TAG = "@launcher";

    /** How long the screen stays on after the last key press. */
    private static final long KEEP_AWAKE_MS = 60000L;

    /** Request code for the system image picker. */
    private static final int REQ_WALLPAPER = 77;

    /** Package broadcasts arrive in bursts; one delayed rebuild covers the whole burst. */
    private static final long PACKAGE_REFRESH_DELAY_MS = 450L;

    /** How long a heads-up banner stays on screen before sliding back up. */
    private static final long BANNER_VISIBLE_MS = 4200L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat timeFmt;
    private SimpleDateFormat dateFmt;

    private float density;

    private FrameLayout root;
    private FrameLayout homePage;
    private FrameLayout libraryPage;
    private Backdrop backdrop;
    private FrameLayout dock;
    private LinearLayout dockRow;
    private SmoothColumn libScroll;
    private GridLayout grid;
    private TextView clock;
    private TextView date;
    private TextView weatherNow;
    private TextView weatherForecast;
    private TextView allHint;
    private TextView hint;
    private TextView libraryNotice;
    private TextView libraryBackHint;
    private StatusBar statusBar;
    private FocusRing ring;
    private boolean weatherLoading;
    private boolean weatherFailed;

    private boolean libraryOpen;
    private boolean libraryBuilt;
    private boolean libraryGliding;
    private boolean firstRender = true;
    private int dockIndex;
    /** The dock card the cursor is on. It is updated at once, so a held
     *  direction walks the dock card by card. */
    private int dockCursor;
    private int libraryCloseFocus = -1;
    private final List<Dialog> dialogs = new ArrayList<>();
    private String lastLibPkg;
    private String pendingLibraryFocus;
    private boolean restoringLibraryFocus;
    private String movePkg;
    private String libraryMovePkg;
    private boolean libraryMoving;
    private String renderedKey;
    private String installedSignature;
    private boolean packageReceiverRegistered;
    private final Set<String> changedPackages = new HashSet<>();

    private FrameLayout bannerLayer;
    private View bannerView;
    private final List<Banner> bannerQueue = new ArrayList<>();
    private boolean bannerShowing;

    private boolean pageAnimating;
    private boolean pageAnimOpening;
    private long pageAnimStart;
    private long pageAnimDuration;
    private int pageAnimTarget;
    private float pageFromLibY;
    private float pageToLibY;
    private float pageFromLibA;
    private float pageToLibA;
    private float pageFromHomeY;
    private float pageToHomeY;
    private float pageFromHomeA;
    private float pageToHomeA;

    private MediaPlayer music;
    private Runnable musicFade;
    private boolean musicLoading;
    private SoundPool sfx;
    private int tickSound;
    private int forwardSound;
    private int backSound;
    private boolean tickReady;
    private boolean forwardReady;
    private boolean backReady;
    private long lastNavSound;
    private long lastForwardSound;
    private long lastBackSound;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            if (statusBar != null) statusBar.refresh();
            handler.postDelayed(this, 15000);
        }
    };

    private final BroadcastReceiver notifReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (i != null && NotifListener.ACTION_HEADS_UP.equals(i.getAction())) {
                enqueueBanner(i);
            }
            if (statusBar != null) statusBar.refresh();
        }
    };

    /**
     * Package installs, updates and uninstalls arrive as a short burst of
     * broadcasts. The UI is rebuilt once, after the burst has settled.
     */
    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (i == null) return;
            String pkg = i.getData() == null ? null : i.getData().getSchemeSpecificPart();
            synchronized (changedPackages) {
                changedPackages.add(pkg == null ? "" : pkg);
            }
            handler.removeCallbacks(packageRefresh);
            handler.postDelayed(packageRefresh, PACKAGE_REFRESH_DELAY_MS);
        }
    };

    private final Runnable packageRefresh = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            Set<String> changed;
            synchronized (changedPackages) {
                changed = new HashSet<>(changedPackages);
                changedPackages.clear();
            }
            boolean all = changed.isEmpty();
            for (String pkg : changed) {
                if (pkg == null || pkg.isEmpty()) all = true;
                else AppRepo.invalidate(pkg);
            }
            if (all) AppRepo.invalidateAll();
            refreshAppsNow();
        }
    };

    private final Runnable hideBanner = new Runnable() {
        @Override
        public void run() {
            if (bannerView == null) {
                bannerShowing = false;
                return;
            }
            final View v = bannerView;
            v.animate().translationY(-dp(200)).alpha(0f).setDuration(240)
                    .setInterpolator(new AccelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (bannerLayer != null) bannerLayer.removeView(v);
                            if (bannerView == v) bannerView = null;
                            bannerShowing = false;
                            if (!bannerQueue.isEmpty()) handler.postDelayed(nextBanner, 160);
                        }
                    }).start();
        }
    };

    private final Runnable nextBanner = new Runnable() {
        @Override
        public void run() {
            showNextBanner();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;
        Locale locale = Txt.locale(this);
        timeFmt = new SimpleDateFormat("HH:mm", locale);
        dateFmt = new SimpleDateFormat("EEEE, d MMMM", locale);
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        // The screen stays on while the user is busy with the remote and is
        // released again afterwards, so the TV is allowed to fall asleep.
        keepAwake();
        setContentView(build());
        Ui.disableSystemClicks(root);
        // Drop the old MediaPlayer cache copy from earlier versions.
        new File(getCacheDir(), "vafic-ambient-48.wav").delete();
        initSoundEffects();
        if (backdrop != null) backdrop.setDim(Prefs.wallpaperDim(this));
        loadWallpaperAsync();
        applyWeather();
        // The artwork of the installed apps is decoded in the background, so
        // the library opens without a stall.
        AppRepo.prefetchArtwork(this);
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
                new ViewTreeObserver.OnGlobalFocusChangeListener() {
                    @Override
                    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                        // Every focusable control that can be pressed gets the
                        // same tick: dock tiles, the top status icons and the
                        // rows of the menus. The window check keeps the tick
                        // inside the launcher and its own dialogs only.
                        if (newFocus == null || newFocus == oldFocus
                                || !newFocus.isClickable()) return;
                        View root = newFocus.getRootView();
                        if (root == null || !root.hasWindowFocus()) return;
                        // The framework would add its own click on top of our
                        // tick; every focused control is silent instead.
                        newFocus.setSoundEffectsEnabled(false);
                        playNavTick();
                    }
                });
        updateClock();
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    private View build() {
        root = new FrameLayout(this);
        root.setClipChildren(false);
        backdrop = new Backdrop(this);
        root.addView(backdrop, new FrameLayout.LayoutParams(MATCH, MATCH));

        homePage = buildHome();
        root.addView(homePage, new FrameLayout.LayoutParams(MATCH, MATCH));

        libraryPage = buildLibrary();
        libraryPage.setVisibility(View.GONE);
        root.addView(libraryPage, new FrameLayout.LayoutParams(MATCH, MATCH));

        ring = new FocusRing(this);
        // The dock and the focused cards use elevation. Keep the focus overlay
        // above them, otherwise its moving highlight is tinted by the glass.
        ring.setElevation(dp(80));
        ring.setTranslationZ(dp(80));
        // The ring is a small view that rides on the focused card. It only
        // needs to be as big as the biggest card plus the room for the glow.
        root.addView(ring, new FrameLayout.LayoutParams(dp(180), dp(112)));

        // Heads-up banners live above the ring and both pages. The layer never
        // takes focus or touches, so D-pad navigation continues underneath it.
        bannerLayer = new FrameLayout(this);
        bannerLayer.setClipChildren(false);
        bannerLayer.setClipToPadding(false);
        bannerLayer.setFocusable(false);
        bannerLayer.setClickable(false);
        bannerLayer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        bannerLayer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        root.addView(bannerLayer, new FrameLayout.LayoutParams(MATCH, MATCH));

        root.getViewTreeObserver().addOnGlobalFocusChangeListener(
                new ViewTreeObserver.OnGlobalFocusChangeListener() {
                    @Override
                    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                        boolean libTile = newFocus instanceof TileView
                                && libraryOpen && libraryBuilt && libScroll != null
                                && grid.indexOfChild(newFocus) >= 0;
                        // When the page becomes visible Android may grab the first
                        // visible card before focusLibrary() runs. Do not let that
                        // temporary focus overwrite the remembered library tile.
                        if (libTile && restoringLibraryFocus) return;
                        if (newFocus instanceof TileView) {
                            if (libTile) {
                                if (newFocus.getTag() instanceof String) {
                                    lastLibPkg = (String) newFocus.getTag();
                                }
                                // Move the page first: that decides whether the
                                // outline should ride the card or ease normally.
                                scrollLibraryTo(newFocus);
                                if (libraryGliding) ring.track(newFocus);
                                else ring.follow(newFocus);
                            } else {
                                ring.follow(newFocus);
                            }
                        } else {
                            ring.hide();
                        }
                        if (newFocus != null) {
                            int di = dockRow.indexOfChild(newFocus);
                            if (di >= 0) {
                                dockIndex = di;
                                dockCursor = di;
                            }
                        }
                    }
                });

        renderDock(true);
        return root;
    }

    // ------------------------------------------------------------------ home

    private FrameLayout buildHome() {
        FrameLayout page = new FrameLayout(this);
        page.setClipChildren(false);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.TOP);
        header.setPadding(dp(64), dp(44), dp(64), 0);

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.START);

        clock = new TextView(this);
        clock.setTextColor(0xFFFFFFFF);
        clock.setTextSize(54);
        clock.setIncludeFontPadding(false);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setLetterSpacing(0.02f);
        clockBox.addView(clock, new LinearLayout.LayoutParams(WRAP, WRAP));

        date = new TextView(this);
        date.setTextColor(0x8AFFFFFF);
        date.setTextSize(15);
        date.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        date.setLetterSpacing(0.07f);
        date.setPadding(0, dp(8), 0, 0);
        clockBox.addView(date, new LinearLayout.LayoutParams(WRAP, WRAP));

        weatherNow = new TextView(this);
        weatherNow.setTextColor(0xE6FFFFFF);
        weatherNow.setTextSize(16);
        weatherNow.setLetterSpacing(0.01f);
        weatherNow.setPadding(0, dp(10), 0, 0);
        weatherNow.setVisibility(View.GONE);
        clockBox.addView(weatherNow, new LinearLayout.LayoutParams(WRAP, WRAP));

        weatherForecast = new TextView(this);
        weatherForecast.setTextColor(0x80FFFFFF);
        weatherForecast.setTextSize(12);
        weatherForecast.setPadding(0, dp(4), 0, 0);
        weatherForecast.setVisibility(View.GONE);
        clockBox.addView(weatherForecast, new LinearLayout.LayoutParams(WRAP, WRAP));

        header.addView(clockBox, new LinearLayout.LayoutParams(WRAP, WRAP));
        header.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));

        statusBar = new StatusBar(this);
        statusBar.setListener(new StatusBar.Listener() {
            @Override
            public void onNotifications() {
                showNotifs();
            }

            @Override
            public void onNetwork() {
                showNetwork();
            }

            @Override
            public void onSettings() {
                showSettings();
            }
        });
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        stlp.topMargin = dp(10);
        header.addView(statusBar, stlp);

        page.addView(header, new FrameLayout.LayoutParams(MATCH, WRAP));

        // Bottom cluster: hint, then the fixed glass dock.
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER_HORIZONTAL);

        allHint = new TextView(this);
        allHint.setText(Txt.s(this, "все приложения  \u2193", "all apps  \u2193"));
        allHint.setTextSize(12);
        allHint.setTextColor(0x73FFFFFF);
        allHint.setLetterSpacing(0.08f);
        allHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(MATCH, WRAP);
        alp.bottomMargin = dp(9);
        bottom.addView(allHint, alp);

        hint = new TextView(this);
        hint.setText(Txt.s(this, "\u2190  \u2192   переместить        OK   готово",
                "\u2190  \u2192   move        OK   done"));
        hint.setTextSize(13);
        hint.setTextColor(0xE6FFFFFF);
        hint.setLetterSpacing(0.03f);
        GradientDrawable hb = new GradientDrawable();
        hb.setColor(0x66000000);
        hb.setCornerRadius(dp(22));
        hb.setStroke(Math.max(1, dp(1)), 0x33FFFFFF);
        hint.setBackground(hb);
        hint.setPadding(dp(22), dp(12), dp(22), dp(12));
        hint.setVisibility(View.GONE);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = dp(10);
        bottom.addView(hint, hlp);

        dock = new FrameLayout(this);
        GradientDrawable dockBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x4DA8D0EE, 0x1E2A4A6E});
        dockBg.setCornerRadius(dp(30));
        dockBg.setStroke(Math.max(1, dp(1)), 0x73B4D8F4);
        // A layer of Aero glass over the bar makes the dock read as a
        // floating window instead of a flat rounded rectangle.
        GlassDrawable dockGlass = new GlassDrawable(density, 30, GlassDrawable.FILL);
        dockGlass.setFocus(0.35f);
        dock.setBackground(new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{dockBg, dockGlass}));
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        dock.setElevation(dp(12));

        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER);
        dockRow.setClipChildren(false);
        dockRow.setClipToPadding(false);
        dockRow.setPadding(dp(22), dp(13), dp(22), dp(13));
        dock.addView(dockRow, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.CENTER));

        // The plate hugs the cards: with three apps it is a smaller island,
        // with eight it grows wider and the cards become compact.
        bottom.addView(dock, new LinearLayout.LayoutParams(WRAP, WRAP));

        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM);
        blp.leftMargin = dp(64);
        blp.rightMargin = dp(64);
        blp.bottomMargin = dp(28);
        page.addView(bottom, blp);

        return page;
    }

    private void renderDock(boolean cascade) {
        int focused = -1;
        String focusedPkg = null;
        View f = dockRow.findFocus();
        if (f != null) {
            focused = dockRow.indexOfChild(f);
            if (f.getTag() instanceof String) focusedPkg = (String) f.getTag();
        }

        dockRow.removeAllViews();
        List<AppRepo.App> apps = AppRepo.homeApps(this);
        int capacity = Prefs.dockSize(this);
        boolean showLabels = Prefs.dockLabels(this);
        int tileDp = dockTileWidth(capacity);
        // Hiding the captions frees vertical space; make the cards taller so
        // the artwork fills it instead of leaving a sparse dock.
        int tileHdp = Math.round(tileDp * (showLabels ? 82f : 96f) / 146f);
        int labelSp = Math.max(8, Math.min(14, Math.round(12f * tileDp / 146f)));
        int corner = Math.max(12, Math.min(24, Math.round(20f * tileDp / 146f)));
        int gap = capacity >= 7 ? dp(8) : dp(12);
        int padH = Math.max(dp(8), dp(22f * tileDp / 146f));
        int padV = Math.max(dp(5), dp(13f * tileDp / 146f));
        dockRow.setPadding(padH, padV, padH, padV);

        if (apps.isEmpty()) {
            TileView add = tile(Txt.s(this, "Добавить", "Add apps"), tileDp, tileHdp,
                    labelSp, corner, showLabels);
            add.setGlyph("+", Math.max(22, Math.round(34f * tileDp / 146f)));
            add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openLibraryFromPlus();
                }
            });
            dockRow.addView(add, new LinearLayout.LayoutParams(WRAP, WRAP));
        }
        for (int i = 0; i < apps.size(); i++) {
            final AppRepo.App a = apps.get(i);
            final int pos = i;
            final TileView v = tile(a.label, tileDp, tileHdp, labelSp, corner, showLabels);
            v.setTag(a.pkg);
            bindDockArt(v, a, showLabels);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launch(a.pkg);
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showDockMenu(a, pos);
                    return true;
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            if (i > 0) lp.leftMargin = gap;
            dockRow.addView(v, lp);
        }

        renderedKey = TextUtils.join(",", Prefs.home(this));

        boolean first = firstRender;
        firstRender = false;
        if (first && cascade) {
            for (int i = 0; i < dockRow.getChildCount(); i++) {
                View ch = dockRow.getChildAt(i);
                ch.setAlpha(0f);
                ch.setTranslationY(dp(24));
                ch.animate().alpha(1f).translationY(0f)
                        .setStartDelay(80 + i * 55L)
                        .setDuration(420)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        }

        int restore = focused;
        if (focusedPkg != null) {
            int byPkg = dockIndexOf(focusedPkg);
            if (byPkg >= 0) restore = byPkg;
        }
        if (restore >= 0) {
            focusDock(restore);
        } else if (first) {
            focusDock(0);
        }
    }

    /** Index of a dock card by package, or -1 when it is not on the dock. */
    private int dockIndexOf(String pkg) {
        if (pkg == null || dockRow == null) return -1;
        for (int i = 0; i < dockRow.getChildCount(); i++) {
            Object tag = dockRow.getChildAt(i).getTag();
            if (tag instanceof String && pkg.equals(tag)) return i;
        }
        return -1;
    }

    /**
     * Rebuilds the dock and the library after a package change. The app that
     * had focus is kept when it still exists; otherwise the cursor lands on
     * the card that took its place in the library.
     */
    private void refreshAppsNow() {
        if (isFinishing() || isDestroyed()) return;
        cleanMissingApps();
        installedSignature = AppRepo.installedSignature(this);

        String focusedLib = lastLibPkg;
        int focusedLibIndex = libraryIndexOf(focusedLib);
        renderDock(false);
        if (libraryBuilt) renderLibrary();

        if (libraryOpen) {
            if (gridTile(focusedLib) != null) focusLibrary(focusedLib);
            else focusLibraryIndex(focusedLibIndex);
        } else if (homePage != null && homePage.findFocus() == null) {
            focusDock(dockIndex);
        }
    }

    /** Drops prefs for packages that were uninstalled while the receiver was off. */
    private void cleanMissingApps() {
        for (String pkg : Prefs.knownPackages(this)) {
            if (!AppRepo.isInstalled(this, pkg)) Prefs.forget(this, pkg);
        }
    }

    /**
     * Dock card width in dp for the chosen capacity. Three apps get wide
     * covers, eight compact ones; the result is clamped so the plate always
     * fits between the side margins of the bottom cluster.
     */
    private int dockTileWidth(int count) {
        float desiredDp = 180f - (count - 3) * 16.8f;
        int padDp = Math.max(10, Math.round(22f * desiredDp / 146f));
        int gapDp = count >= 7 ? 8 : 12;
        int screenDp = Math.round(getResources().getDisplayMetrics().widthPixels / density);
        int fit = (screenDp - 128 - padDp * 2 - gapDp * (count - 1)) / count;
        return Math.min(Math.round(desiredDp), Math.max(72, fit));
    }

    private void focusDock(final int index) {
        dockRow.post(new Runnable() {
            @Override
            public void run() {
                if (dockRow.getChildCount() == 0) return;
                int i = Math.max(0, Math.min(index, dockRow.getChildCount() - 1));
                View target = dockRow.getChildAt(i);
                target.requestFocus();
                // If this card already had focus, no global focus-change event fires.
                // Re-follow it explicitly so HOME always restores the outline.
                if (ring != null) ring.follow(target);
            }
        });
    }

    /**
     * Moves the dock cursor sideways. The dock is the lowest row of the screen,
     * so the default focus search jumps up to the status cluster as soon as the
     * last card is reached. The cursor stays inside the dock instead.
     */
    private void moveDockFocus(int delta) {
        int count = dockRow.getChildCount();
        if (count == 0) return;
        int next = Math.max(0, Math.min(dockCursor + delta, count - 1));
        if (next == dockCursor) return;
        // The cursor moves at once, so a held direction walks card by card
        // instead of queueing several moves on the same card.
        dockCursor = next;
        dockIndex = next;
        focusDock(next);
    }

    // --------------------------------------------------------------- library

    private FrameLayout buildLibrary() {
        FrameLayout page = new FrameLayout(this);
        page.setClipChildren(false);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF12243A, 0xFF0C1A2C, 0xFF060C16});
        bg.setCornerRadius(0);
        page.setBackground(bg);

        final int headerH = dp(78);
        final int fadeH = dp(14);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(64), headerH + fadeH, dp(64), dp(18));
        content.setClipChildren(false);
        content.setClipToPadding(false);
        page.addView(content, new FrameLayout.LayoutParams(MATCH, MATCH));

        // Fixed header: the dock and grid slide underneath it. It is drawn last,
        // so even a tile edge can never end up on top of the title.
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(dp(64), dp(18), dp(64), dp(10));
        GradientDrawable hbg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF12243A, 0xFF0F1E32});
        head.setBackground(hbg);

        TextView title = new TextView(this);
        title.setText(Txt.s(this, "Все приложения", "All apps"));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(30);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setLetterSpacing(0.02f);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView back = new TextView(this);
        back.setText(Txt.s(this, "\u2191 назад", "\u2191 back"));
        back.setTextColor(0x59FFFFFF);
        back.setTextSize(12);
        back.setSingleLine(true);
        back.setLetterSpacing(0.04f);
        libraryBackHint = back;
        head.addView(back, new LinearLayout.LayoutParams(WRAP, WRAP));

        libScroll = new SmoothColumn(this);
        libScroll.setDpadFocusOnly(true);
        libScroll.setOnSettledListener(new SmoothColumn.OnSettledListener() {
            @Override
            public void onSettled() {
                libraryGliding = false;
                if (ring == null) return;
                ring.settleTracking();
                // The page has arrived: bring the outline back around the
                // card that actually holds focus now.
                if (libraryOpen && dialogs.isEmpty()) {
                    View f = getCurrentFocus();
                    if (f instanceof TileView && grid.indexOfChild(f) >= 0) {
                        ring.follow((TileView) f);
                    }
                }
            }
        });
        // The title stays fixed above the list; clip the scrolling dock and grid
        // so they slide underneath it instead of drawing over the label.
        libScroll.setClipChildren(true);
        libScroll.setClipToPadding(true);
        libScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(MATCH, 0, 1f);
        content.addView(libScroll, glp);

        // The library is a clean three-row grid. There is no extra dock block
        // above it: all three rows, including their labels, must fit on screen.
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setClipChildren(false);
        inner.setClipToPadding(false);
        // Extra room below the last row lets the final three-row page align
        // under the header instead of leaving a clipped sliver of the top row.
        inner.setPadding(0, 0, 0, dp(48));
        libScroll.addView(inner, new FrameLayout.LayoutParams(MATCH, WRAP));

        grid = new GridLayout(this);
        grid.setColumnCount(GRID_COLS);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(4), 0, dp(8));
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(MATCH, WRAP);
        gridLp.topMargin = dp(14);
        inner.addView(grid, gridLp);

        // Soft edge under the title: content fades out before it reaches the label.
        View fade = new View(this);
        GradientDrawable fg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF12243A, 0x0012243A});
        fade.setBackground(fg);
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(MATCH, fadeH, Gravity.TOP);
        flp.topMargin = headerH;
        page.addView(fade, flp);

        // The grid is taller than three rows. Fade the last sliver and then
        // cover it completely, so a page always reads as three complete rows.
        View bottomFade = new View(this);
        GradientDrawable bfg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x00060C16, 0xFF060C16});
        bottomFade.setBackground(bfg);
        FrameLayout.LayoutParams bflp = new FrameLayout.LayoutParams(MATCH, dp(16), Gravity.BOTTOM);
        bflp.bottomMargin = dp(42);
        page.addView(bottomFade, bflp);
        View bottomSolid = new View(this);
        bottomSolid.setBackgroundColor(0xFF060C16);
        page.addView(bottomSolid, new FrameLayout.LayoutParams(MATCH, dp(42), Gravity.BOTTOM));

        page.addView(head, new FrameLayout.LayoutParams(MATCH, headerH, Gravity.TOP));

        libraryNotice = new TextView(this);
        libraryNotice.setTextSize(14.5f);
        libraryNotice.setTextColor(0xF2FFFFFF);
        libraryNotice.setGravity(Gravity.CENTER);
        libraryNotice.setPadding(dp(24), dp(14), dp(24), dp(14));
        libraryNotice.setMaxWidth(dp(840));
        GradientDrawable nb = new GradientDrawable();
        nb.setColor(0xF20A0E15);
        nb.setCornerRadius(dp(22));
        nb.setStroke(Math.max(1, dp(1)), 0x38FFFFFF);
        libraryNotice.setBackground(nb);
        libraryNotice.setElevation(dp(40));
        libraryNotice.setVisibility(View.GONE);
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(WRAP, WRAP,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        nlp.bottomMargin = dp(30);
        page.addView(libraryNotice, nlp);
        return page;
    }

    private void renderLibrary() {
        grid.removeAllViews();
        List<AppRepo.App> apps = AppRepo.allApps(this);
        for (AppRepo.App a : apps) {
            final AppRepo.App app = a;
            final TileView v = tile(a.label, 148, 83, 11, 18);
            bindArt(v, a);
            v.setTag(a.pkg);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launch(app.pkg);
                }
            });
            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    showAppMenu(app);
                    return true;
                }
            });
            grid.addView(v, gridParams());
        }

        libraryBuilt = true;
    }

    /**
     * Focus-centred three-row paging. The cursor normally stays on the middle
     * row: reaching the next row scrolls the whole page by one row, so the top
     * row leaves and a new bottom row arrives. On the last row there is nothing
     * below to bring in, so the focus moves down to the third row instead.
     */
    private void scrollLibraryTo(View tile) {
        if (libScroll == null || grid == null || tile == null) return;
        int index = grid.indexOfChild(tile);
        if (index < 0) return;

        int count = grid.getChildCount();
        int totalRows = (count + GRID_COLS - 1) / GRID_COLS;
        if (totalRows <= 0) return;

        int step = libraryRowStep();
        if (step <= 0) {
            libScroll.ensureVisible(tile);
            return;
        }

        int focusRow = index / GRID_COLS;
        int pageRows = Math.min(LIB_PAGE_ROWS, totalRows);
        int maxStart = Math.max(0, totalRows - pageRows);
        int start;
        if (focusRow <= 0) {
            start = 0;
        } else if (focusRow >= totalRows - 1) {
            // The final row has no row below it, so it may sit on the bottom.
            start = maxStart;
        } else {
            // Keep the focused row in the middle of the three-row window.
            start = focusRow - 1;
        }
        start = Math.max(0, Math.min(start, maxStart));

        int target = 0;
        if (start > 0) {
            // Scroll by whole row steps. Aligning a row to the top of the
            // ScrollView instead would shift every page up by the grid's top
            // margin, so the "middle" slot would move and the cursor would
            // appear to jump upward when the page recentres.
            target = start * step;
        }
        int current = libScroll.getScrollY();
        libScroll.glideTo(target);
        int destination = libScroll.getTargetY();
        boolean glide = Math.abs(destination - current) > 1 || libScroll.isGliding();
        libraryGliding = glide;
    }

    /** Distance from the top of one library row to the top of the next. */
    private int libraryRowStep() {
        if (grid == null || grid.getChildCount() == 0) return 0;
        if (grid.getChildCount() > GRID_COLS) {
            View first = grid.getChildAt(0);
            View second = grid.getChildAt(GRID_COLS);
            if (first != null && second != null) {
                int step = second.getTop() - first.getTop();
                if (step > 0) return step;
            }
        }
        View first = grid.getChildAt(0);
        return first == null ? 0 : first.getHeight() + dp(36);
    }

    private void focusLibrary() {
        focusLibrary(lastLibPkg);
    }

    private void focusLibrary(String wanted) {
        if (grid == null || grid.getChildCount() == 0) return;
        View target = null;
        if (wanted != null) {
            for (int i = 0; i < grid.getChildCount(); i++) {
                View v = grid.getChildAt(i);
                if (wanted.equals(v.getTag())) {
                    target = v;
                    break;
                }
            }
        }
        if (target == null) target = grid.getChildAt(0);
        final View t = target;
        t.post(new Runnable() {
            @Override
            public void run() {
                // A fast BACK can close the page before this post runs.
                if (!libraryOpen) {
                    restoringLibraryFocus = false;
                    return;
                }
                if (t.getParent() == null) return;
                restoringLibraryFocus = true;
                t.requestFocus();
                restoringLibraryFocus = false;
                if (t.getTag() instanceof String) lastLibPkg = (String) t.getTag();
                // Do not let ScrollView focus handling scroll before the target
                // is known; move the page ourselves and keep the outline on it.
                scrollLibraryTo(t);
                if (ring != null) {
                    if (libraryGliding) ring.track(t);
                    else ring.follow(t);
                }
            }
        });
    }

    private void openLibrary() {
        if (libraryOpen) return;
        playForward();
        startPageTransition(true, 0);
    }

    /**
     * The empty-dock card opens the library and explains the only way to put
     * an app on the dock. The longer notice is intentional: a first-time user
     * has to find the long-press action in the app menu.
     */
    private void openLibraryFromPlus() {
        // The empty-dock card is an onboarding action: start at the first app,
        // not at whichever library tile happened to have focus last time.
        lastLibPkg = null;
        openLibrary();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (libraryOpen && !isFinishing()) {
                    showLibraryNotice(Txt.s(MainActivity.this,
                            "Удерживайте OK на приложении и выберите «Добавить на док»",
                            "Hold OK on an app, then choose \u201CAdd to dock\u201D"), 6500);
                }
            }
        }, 440);
    }

    private final Runnable hideLibraryNotice = new Runnable() {
        @Override
        public void run() {
            if (libraryNotice == null || libraryNotice.getVisibility() != View.VISIBLE) return;
            libraryNotice.animate().alpha(0f).translationY(dp(8)).setDuration(220)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            libraryNotice.setVisibility(View.GONE);
                        }
                    }).start();
        }
    };

    /** Releases the screen so the television can go to sleep. */
    private final Runnable sleepAllow = new Runnable() {
        @Override
        public void run() {
            allowSleep();
        }
    };

    /** Small in-app notice; keeps transient feedback inside the launcher's visual language. */
    private void showLibraryNotice(String text, long durationMs) {
        if (libraryNotice == null) return;
        handler.removeCallbacks(hideLibraryNotice);
        libraryNotice.animate().cancel();
        libraryNotice.setText(text);
        libraryNotice.setVisibility(View.VISIBLE);
        libraryNotice.setAlpha(0f);
        libraryNotice.setTranslationY(dp(10));
        libraryNotice.animate().alpha(1f).translationY(0f).setDuration(180)
                .setInterpolator(new DecelerateInterpolator()).start();
        if (durationMs > 0) handler.postDelayed(hideLibraryNotice, durationMs);
    }

    private void hideLibraryNoticeNow() {
        handler.removeCallbacks(hideLibraryNotice);
        if (libraryNotice == null) return;
        libraryNotice.animate().cancel();
        libraryNotice.setVisibility(View.GONE);
        libraryNotice.setAlpha(1f);
        libraryNotice.setTranslationY(0f);
    }

    private void closeLibrary() {
        if (!libraryOpen) return;
        if (libraryMoving) cancelLibraryMove();
        hideLibraryNoticeNow();
        playBack();
        final int target = libraryCloseFocus >= 0 ? libraryCloseFocus : dockIndex;
        libraryCloseFocus = -1;
        startPageTransition(false, target);
    }

    /**
     * Home <-> library transition. It is driven by our own frame clock instead of
     * ViewPropertyAnimator because this box runs Android animation scale at 0.5x,
     * which made the old 240-280 ms page slide look like an instant cut. The page
     * rises/falls with an ease curve while home parallaxes and fades behind it.
     */
    private void startPageTransition(boolean opening, int target) {
        if (root == null || libraryPage == null || homePage == null) return;

        if (opening) {
            // Capture the tile before the page becomes visible: Android can
            // temporarily focus a visible card during the transition.
            pendingLibraryFocus = lastLibPkg;
            restoringLibraryFocus = true;
            libraryOpen = true;
            if (!libraryBuilt) renderLibrary();
            if (pendingLibraryFocus == null && libScroll != null) {
                // A fresh visit starts at the very first app, not in the
                // middle of the list where the cursor was left last time.
                libScroll.jumpTo(0);
            }

            homePage.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        } else {
            pendingLibraryFocus = null;
            restoringLibraryFocus = false;
            libraryOpen = false;
            homePage.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            // The home page was hidden after the open transition. Bring it back
            // before the library starts sliding away.
            homePage.setVisibility(View.VISIBLE);
            if (backdrop != null) backdrop.setVisibility(View.VISIBLE);
        }
        if (ring != null) ring.hide();

        int h = root.getHeight() > 0 ? root.getHeight() : dp(540);
        float travel = Math.max(dp(120), h * 0.30f);

        if (opening && libraryPage.getVisibility() != View.VISIBLE) {
            libraryPage.setVisibility(View.VISIBLE);
            libraryPage.setTranslationY(travel);
            libraryPage.setAlpha(0f);
        }

        root.removeCallbacks(pageStep);
        pageAnimating = true;
        pageAnimOpening = opening;
        pageAnimTarget = target;
        pageAnimStart = SystemClock.uptimeMillis();
        pageAnimDuration = opening ? 380L : 320L;

        pageFromLibY = libraryPage.getTranslationY();
        pageFromLibA = libraryPage.getAlpha();
        pageFromHomeY = homePage.getTranslationY();
        pageFromHomeA = homePage.getAlpha();

        if (opening) {
            pageToLibY = 0f;
            pageToLibA = 1f;
            pageToHomeY = -dp(104);
            pageToHomeA = 0.08f;
        } else {
            pageToLibY = Math.max(travel, pageFromLibY);
            pageToLibA = 0f;
            pageToHomeY = 0f;
            pageToHomeA = 1f;
        }
        root.postOnAnimation(pageStep);
    }

    private void cancelPageTransition() {
        pageAnimating = false;
        pendingLibraryFocus = null;
        restoringLibraryFocus = false;
        if (root != null) root.removeCallbacks(pageStep);
    }

    private final Runnable pageStep = new Runnable() {
        @Override
        public void run() {
            if (!pageAnimating) return;

            long now = SystemClock.uptimeMillis();
            float t = pageAnimDuration > 0
                    ? (now - pageAnimStart) / (float) pageAnimDuration
                    : 1f;
            if (t > 1f) t = 1f;
            float e = pageAnimOpening ? easeOutCubic(t) : easeInOutCubic(t);

            libraryPage.setTranslationY(pageFromLibY + (pageToLibY - pageFromLibY) * e);
            libraryPage.setAlpha(pageFromLibA + (pageToLibA - pageFromLibA) * e);
            homePage.setTranslationY(pageFromHomeY + (pageToHomeY - pageFromHomeY) * e);
            homePage.setAlpha(pageFromHomeA + (pageToHomeA - pageFromHomeA) * e);

            if (t < 1f) {
                root.postOnAnimation(this);
                return;
            }

            pageAnimating = false;
            if (pageAnimOpening) {
                libraryPage.setTranslationY(0f);
                libraryPage.setAlpha(1f);
                // The library background is opaque. Hide the covered home page
                // and wallpaper so scrolling does not composite two full screens.
                homePage.setVisibility(View.INVISIBLE);
                if (backdrop != null) backdrop.setVisibility(View.INVISIBLE);
                String wanted = pendingLibraryFocus;
                pendingLibraryFocus = null;
                focusLibrary(wanted);
            } else {
                libraryPage.setVisibility(View.GONE);
                libraryPage.setTranslationY(0f);
                libraryPage.setAlpha(1f);
                focusDock(pageAnimTarget);
            }
        }
    };

    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f
                ? 4f * t * t * t
                : 1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    /**
     * Hardware HOME: leave whatever menu or page is open and land on the first dock card.
     * The system may or may not deliver KEYCODE_HOME to us, so onNewIntent() calls this too.
     */
    private void goHome() {
        if (libraryMoving) cancelLibraryMove();
        if (movePkg != null) {
            stopMove();
            playBack();
        }
        if (!dialogs.isEmpty()) playBack();
        for (Dialog d : new ArrayList<>(dialogs)) {
            try {
                d.dismiss();
            } catch (Exception ignored) {
            }
        }
        dialogs.clear();

        // Leaving the launcher forgets the library cursor. The next visit to
        // "All apps" starts at the first app again.
        lastLibPkg = null;

        if (ring != null) ring.hide();
        cancelPageTransition();

        if (libraryOpen) {
            libraryCloseFocus = 0;
            closeLibrary();
        } else {
            if (libraryPage != null) {
                libraryPage.setVisibility(View.GONE);
                libraryPage.setTranslationY(0f);
                libraryPage.setAlpha(1f);
            }
            homePage.setTranslationY(0f);
            homePage.setAlpha(1f);
            homePage.setVisibility(View.VISIBLE);
            if (backdrop != null) backdrop.setVisibility(View.VISIBLE);
            homePage.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            focusDock(0);
        }
    }

    // ----------------------------------------------------------------- menus

    private void showDockMenu(final AppRepo.App a, final int pos) {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        tracked(new MenuDialog(this, a.label,
                new String[]{Txt.s(this, "Переместить", "Move"),
                        Txt.s(this, "Убрать с дока", "Remove from dock"),
                        Txt.s(this, "Скрыть", "Hide"),
                        Txt.s(this, "Удалить", "Delete")},
                new MenuDialog.Listener() {
                    @Override
                    public void onItem(int which) {
                        if (which == 3) {
                            deleteApp(a);
                            return;
                        }
                        if (which == 2) {
                            hideApp(a);
                            return;
                        }
                        if (which == 0) {
                            startMove(a.pkg, pos);
                        } else {
                            Prefs.remove(MainActivity.this, a.pkg);
                            renderDock(false);
                            focusDock(0);
                        }
                    }
                })).show();
    }

    private void showAppMenu(final AppRepo.App a) {
        if (isFinishing()) return;
        playForward();
        final boolean onDock = Prefs.home(this).contains(a.pkg);
        String[] items = onDock
                ? new String[]{Txt.s(this, "Убрать с дока", "Remove from dock"),
                        Txt.s(this, "Переместить", "Move"),
                        Txt.s(this, "Скрыть", "Hide"),
                        Txt.s(this, "Удалить", "Delete")}
                : new String[]{Txt.s(this, "Добавить на док", "Add to dock"),
                        Txt.s(this, "Переместить", "Move"),
                        Txt.s(this, "Скрыть", "Hide"),
                        Txt.s(this, "Удалить", "Delete")};
        ring.hide();
        tracked(new MenuDialog(this, a.label, items, new MenuDialog.Listener() {
            @Override
            public void onItem(int which) {
                if (which == 3) {
                    deleteApp(a);
                    return;
                }
                if (which == 2) {
                    hideApp(a);
                    return;
                }
                if (which == 0) {
                    if (onDock) {
                        Prefs.remove(MainActivity.this, a.pkg);
                        renderDock(false);
                    } else {
                        if (!Prefs.toggle(MainActivity.this, a.pkg)) {
                            int maxDock = Prefs.dockSize(MainActivity.this);
                            showLibraryNotice(Txt.s(MainActivity.this,
                                    "На доке максимум " + maxDock + " приложений",
                                    "The dock holds up to " + maxDock + " apps"), 3500);
                            return;
                        }
                        renderDock(false);
                        showLibraryNotice(Txt.s(MainActivity.this,
                                "Добавлено на док", "Added to dock"), 3000);
                    }
                    focusLibrary();
                } else {
                    startLibraryMove(a.pkg);
                }
            }
        })).show();
    }

    /**
     * Opens the system uninstall screen. The package receiver rebuilds the
     * dock and library when the package is actually gone; cancelling the
     * system dialog leaves everything untouched.
     */
    private void deleteApp(AppRepo.App a) {
        if (a == null || isFinishing()) return;
        playForward();
        try {
            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg)));
        } catch (Exception e) {
            if (isFinishing()) return;
            tracked(new InfoDialog(this,
                    Txt.s(this, "Не удалось удалить", "Could not uninstall"),
                    Txt.s(this, "На этом телевизоре нет системного окна удаления приложений.",
                            "This television has no system uninstall screen."))).show();
        }
    }

    /**
     * Hides an app from the launcher. Hidden apps leave the library and the
     * dock as well; they can be brought back from the launcher settings.
     */
    private void hideApp(AppRepo.App a) {
        int index = libraryIndexOf(a.pkg);
        Prefs.setHidden(this, a.pkg, true);
        Prefs.remove(this, a.pkg);
        renderDock(false);
        if (libraryBuilt) renderLibrary();
        if (libraryOpen) {
            focusLibraryIndex(index);
            showLibraryNotice(Txt.s(this, "Скрыто: ", "Hidden: ") + a.label, 3000);
        } else {
            focusDock(Math.max(0, Math.min(index, Math.max(0, dockRow.getChildCount() - 1))));
        }
    }

    private void startMove(String pkg, int pos) {
        if (libraryMoving) cancelLibraryMove();
        movePkg = pkg;
        allHint.setVisibility(View.GONE);
        hint.setVisibility(View.VISIBLE);
        hint.setAlpha(0f);
        hint.animate().alpha(1f).setDuration(180).start();
        focusDock(pos);
    }

    private void stopMove() {
        movePkg = null;
        hint.animate().alpha(0f).setDuration(150).withEndAction(new Runnable() {
            @Override
            public void run() {
                hint.setVisibility(View.GONE);
                allHint.setVisibility(View.VISIBLE);
            }
        }).start();
    }

    private View gridTile(String pkg) {
        if (grid == null || pkg == null) return null;
        for (int i = 0; i < grid.getChildCount(); i++) {
            View v = grid.getChildAt(i);
            Object tag = v.getTag();
            if (tag instanceof String && pkg.equals(tag)) return v;
        }
        return null;
    }

    /** Position of a package inside the library grid, or -1 when it is not there. */
    private int libraryIndexOf(String pkg) {
        if (grid == null || pkg == null) return -1;
        for (int i = 0; i < grid.getChildCount(); i++) {
            Object tag = grid.getChildAt(i).getTag();
            if (tag instanceof String && pkg.equals(tag)) return i;
        }
        return -1;
    }

    /** Focuses the card that took the given slot, used when a card disappears. */
    private void focusLibraryIndex(int index) {
        if (grid == null || grid.getChildCount() == 0) return;
        int i = Math.max(0, Math.min(index, grid.getChildCount() - 1));
        View t = grid.getChildAt(i);
        if (t == null) return;
        if (t.getTag() instanceof String) lastLibPkg = (String) t.getTag();
        focusLibrary(lastLibPkg);
    }

    private List<String> currentLibraryOrder() {
        List<String> out = new ArrayList<>();
        if (grid == null) return out;
        for (int i = 0; i < grid.getChildCount(); i++) {
            Object tag = grid.getChildAt(i).getTag();
            if (tag instanceof String && !LIB_SETTINGS_TAG.equals(tag)) {
                out.add((String) tag);
            }
        }
        return out;
    }

    private void startLibraryMove(String pkg) {
        View t = gridTile(pkg);
        if (t == null) return;
        libraryMoving = true;
        libraryMovePkg = pkg;
        lastLibPkg = pkg;
        hideLibraryNoticeNow();
        if (libraryBackHint != null) {
            libraryBackHint.setText(Txt.s(this,
                    "\u2190 \u2192 \u2191 \u2193  двигать    OK  готово",
                    "\u2190 \u2192 \u2191 \u2193  move    OK  done"));
            libraryBackHint.setTextColor(0xE6FFFFFF);
        }
        t.requestFocus();
        if (ring != null) ring.follow(t);
    }

    private void moveLibrary(int code) {
        List<String> order = currentLibraryOrder();
        int i = order.indexOf(libraryMovePkg);
        if (i < 0) return;
        int j = i;
        if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (i % GRID_COLS != 0) j = i - 1;
        } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (i % GRID_COLS != GRID_COLS - 1 && i + 1 < order.size()) j = i + 1;
        } else if (code == KeyEvent.KEYCODE_DPAD_UP) {
            if (i >= GRID_COLS) j = i - GRID_COLS;
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (i + GRID_COLS < order.size()) j = i + GRID_COLS;
        }
        if (j == i) return;
        String other = order.get(j);
        order.set(i, other);
        order.set(j, libraryMovePkg);
        Prefs.saveLibraryOrder(this, order);
        lastLibPkg = libraryMovePkg;
        renderLibrary();
        focusLibrary();
    }

    private void cancelLibraryMove() {
        libraryMoving = false;
        libraryMovePkg = null;
        if (libraryBackHint != null) {
            libraryBackHint.setText(Txt.s(this, "\u2191 назад", "\u2191 back"));
            libraryBackHint.setTextColor(0x59FFFFFF);
        }
    }

    private void finishLibraryMove(boolean back) {
        String pkg = lastLibPkg;
        cancelLibraryMove();
        if (back) playBack();
        View t = gridTile(pkg);
        if (t == null) return;
        scrollLibraryTo(t);
        t.requestFocus();
        if (ring != null) {
            if (libraryGliding) ring.track(t);
            else ring.follow(t);
        }
    }

    private void showNotifs() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        try {
            tracked(new NotifPanel(this)).show();
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------ heads-up

    private void enqueueBanner(Intent i) {
        if (i == null || bannerLayer == null) return;
        String pkg = i.getStringExtra(NotifListener.EXTRA_PKG);
        if (TextUtils.isEmpty(pkg)) return;
        if (bannerQueue.size() >= 4) bannerQueue.remove(0);
        bannerQueue.add(new Banner(pkg,
                i.getStringExtra(NotifListener.EXTRA_APP),
                i.getStringExtra(NotifListener.EXTRA_TITLE),
                i.getStringExtra(NotifListener.EXTRA_TEXT)));
        showNextBanner();
    }

    /** Shows the next queued popup, unless one is already sliding. */
    private void showNextBanner() {
        if (bannerShowing || bannerQueue.isEmpty() || isFinishing() || bannerLayer == null) return;
        Banner b = bannerQueue.remove(0);
        bannerShowing = true;

        final View v = buildBannerView(b);
        bannerView = v;
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int width = Math.min(dp(680), Math.max(dp(320), screenW - dp(96)));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, WRAP,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lp.topMargin = dp(22);
        v.setAlpha(0f);
        v.setTranslationY(-dp(200));
        bannerLayer.addView(v, lp);

        v.post(new Runnable() {
            @Override
            public void run() {
                if (bannerView != v) return;
                v.animate().translationY(0f).alpha(1f).setDuration(280)
                        .setInterpolator(new DecelerateInterpolator()).start();
            }
        });

        handler.removeCallbacks(hideBanner);
        handler.postDelayed(hideBanner, BANNER_VISIBLE_MS);
    }

    private View buildBannerView(Banner b) {
        GlassPanel panel = new GlassPanel(this, density, 22);
        panel.setFocusable(false);
        panel.setClickable(false);
        panel.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        panel.setElevation(dp(40));
        panel.setClipChildren(false);
        panel.setClipToPadding(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(14), dp(20), dp(14));

        ImageView icon = new ImageView(this);
        Drawable art = AppRepo.icon(this, b.pkg);
        if (art == null) art = getPackageManager().getDefaultActivityIcon();
        icon.setImageDrawable(art);
        row.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), 0, 0, 0);

        String app = TextUtils.isEmpty(b.app) ? b.pkg : b.app;
        String heading = TextUtils.isEmpty(b.title) ? app : b.title;

        TextView appView = new TextView(this);
        appView.setText(app);
        appView.setTextSize(11);
        appView.setTextColor(0x8AFFFFFF);
        appView.setLetterSpacing(0.06f);
        appView.setSingleLine(true);
        appView.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(appView, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView title = new TextView(this);
        title.setText(heading);
        title.setTextSize(17);
        title.setTextColor(0xFFF5F5F7);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        if (!TextUtils.isEmpty(b.text)) {
            TextView text = new TextView(this);
            text.setText(b.text);
            text.setTextSize(13);
            text.setTextColor(0xB3FFFFFF);
            text.setMaxLines(2);
            text.setEllipsize(TextUtils.TruncateAt.END);
            text.setPadding(0, dp(2), 0, 0);
            col.addView(text, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        row.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));
        panel.addView(row, new FrameLayout.LayoutParams(MATCH, WRAP));
        String body = b.text == null ? "" : b.text;
        panel.setContentDescription((app + " " + heading + " " + body).trim());
        return panel;
    }

    /** Removes a banner immediately, used when the activity leaves the screen. */
    private void clearBanners() {
        handler.removeCallbacks(hideBanner);
        handler.removeCallbacks(nextBanner);
        bannerQueue.clear();
        View v = bannerView;
        bannerView = null;
        bannerShowing = false;
        if (v != null) {
            v.animate().cancel();
            if (bannerLayer != null) bannerLayer.removeView(v);
        }
    }

    private static final class Banner {
        final String pkg;
        final String app;
        final String title;
        final String text;

        Banner(String pkg, String app, String title, String text) {
            this.pkg = pkg;
            this.app = app;
            this.title = title;
            this.text = text;
        }
    }

    private void showNetwork() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        try {
            tracked(new NetworkPanel(this)).show();
        } catch (Exception ignored) {
        }
    }

    private void showSettings() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        tracked(new SettingsPanel(this, new SettingsPanel.Listener() {
            @Override
            public void onOpenSound() {
                showSoundSettings();
            }

            @Override
            public void onOpenDock() {
                showDockSettings();
            }

            @Override
            public void onOpenWeather() {
                showWeatherSettings();
            }

            @Override
            public void onOpenWallpaper() {
                showWallpaperSettings();
            }

            @Override
            public void onLanguageChanged() {
                recreate();
            }

            @Override
            public void onOpenNotifSettings() {
                playForward();
                ring.hide();
                try {
                    tracked(new NotifSettingsDialog(MainActivity.this)).show();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onOpenSystemSettings() {
                showSystemSettings();
            }

            @Override
            public void onOpenHiddenApps() {
                showHiddenApps();
            }

            @Override
            public void onClose() {
                playBack();
            }
        })).show();
    }

    private void showSoundSettings() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        tracked(new SoundSettingsDialog(this, new SoundSettingsDialog.Listener() {
            @Override
            public void onMusicChanged(boolean on) {
                if (on) startMusic();
                else stopMusic();
            }

            @Override
            public void onSfxChanged(boolean on) {
                if (on) playForward();
            }

            @Override
            public void onMusicVolume(int percent) {
                applyMusicVolume();
            }

            @Override
            public void onSfxVolume(int percent) {
                // A tick at the new level: the slider is silent otherwise.
                playNavTick();
            }
        })).show();
    }

    private void showDockSettings() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        tracked(new DockSettingsDialog(this, new DockSettingsDialog.Listener() {
            @Override
            public void onDockChanged(int count) {
                renderDock(false);
                focusDock(Math.min(dockIndex, Math.max(0, dockRow.getChildCount() - 1)));
            }

            @Override
            public void onDockLabelsChanged(boolean shown) {
                renderDock(false);
            }
        })).show();
    }

    private void showWeatherSettings() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        tracked(new WeatherSettingsDialog(this, new WeatherSettingsDialog.Listener() {
            @Override
            public void onWeatherChanged() {
                weatherFailed = false;
                applyWeather();
                if (Prefs.weather(MainActivity.this)) refreshWeather(true);
                scheduleWeather();
            }

            @Override
            public void onForecastChanged() {
                applyWeather();
            }

            @Override
            public void onPlacePicked(String name, double lat, double lon) {
                weatherFailed = false;
                applyWeather();
                refreshWeather(true);
                scheduleWeather();
            }

            @Override
            public void onCityTyped(String name) {
                resolveWeatherCity(name);
            }

            @Override
            public void onRefresh() {
                weatherFailed = false;
                refreshWeather(true);
                scheduleWeather();
            }
        })).show();
    }

    private void showWallpaperSettings() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        tracked(new WallpaperDialog(this, new WallpaperDialog.Listener() {
            @Override
            public void onPickImage() {
                pickWallpaper();
            }

            @Override
            public void onReset() {
                Prefs.setWallpaper(MainActivity.this, "");
                File f = new File(getFilesDir(), "wallpaper.jpg");
                if (f.exists()) f.delete();
                if (backdrop != null) backdrop.setCustom(null);
            }

            @Override
            public void onDimChanged(int percent) {
                if (backdrop != null) backdrop.setDim(percent);
            }
        })).show();
    }

    private void showSystemSettings() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        try {
            Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception ignored) {
        }
    }

    /** The list of apps hidden from the library, with a way to bring them back. */
    private void showHiddenApps() {
        if (isFinishing()) return;
        playForward();
        ring.hide();
        try {
            tracked(new HiddenAppsDialog(this, new HiddenAppsDialog.Listener() {
                @Override
                public void onChanged() {
                    renderDock(false);
                    if (libraryBuilt) renderLibrary();
                    if (libraryOpen) focusLibrary();
                }
            })).show();
        } catch (Exception ignored) {
        }
    }

    private void launch(String pkg) {
        if (movePkg != null || libraryMoving) return;
        Intent i = AppRepo.launch(this, pkg);
        if (i != null) {
            playForward();
            try {
                startActivity(i);
            } catch (Exception ignored) {
            }
        }
    }

    private TileView tile(String label, int wDp, int hDp, int labelSp, int cornerDp) {
        return tile(label, wDp, hDp, labelSp, cornerDp, true);
    }

    private TileView tile(String label, int wDp, int hDp, int labelSp, int cornerDp,
                          boolean withLabel) {
        TileView v = new TileView(this, dp(wDp), dp(hDp), labelSp, cornerDp, withLabel);
        v.setLabel(label);
        return v;
    }

    private void bindArt(TileView v, AppRepo.App a) {
        bindArt(v, a, false);
    }

    /** Dock artwork with a tighter frame when the captions are hidden. */
    private void bindDockArt(TileView v, AppRepo.App a, boolean showLabels) {
        if (a.banner != null) {
            v.setBanner(a.banner);
        } else {
            v.setIcon(a.icon, showLabels ? 14 : 12, showLabels ? 9 : 7);
        }
    }

    /**
     * Puts the artwork on the card. The dock uses tighter icon margins, so the
     * artwork fills the glass card.
     */
    private void bindArt(TileView v, AppRepo.App a, boolean compact) {
        if (a.banner != null) {
            v.setBanner(a.banner);
        } else if (compact) {
            v.setIcon(a.icon, 14, 9);
        } else {
            v.setIcon(a.icon, 26, 16);
        }
    }

    private GridLayout.LayoutParams gridParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = WRAP;
        lp.height = WRAP;
        lp.setMargins(dp(4), dp(6), dp(14), dp(14));
        return lp;
    }

    // ----------------------------------------------------------------- clock

    private void updateClock() {
        if (clock == null) return;
        Date now = new Date();
        clock.setText(timeFmt.format(now));
        String d = dateFmt.format(now);
        if (d.length() > 0) {
            d = d.substring(0, 1).toUpperCase(Txt.locale(this)) + d.substring(1);
        }
        date.setText(d);
    }

    // --------------------------------------------------------------- weather

    /** Re-renders the clock-area weather from the cache; no network here. */
    private void applyWeather() {
        if (weatherNow == null || weatherForecast == null) return;
        if (!Prefs.weather(this)) {
            weatherNow.setVisibility(View.GONE);
            weatherForecast.setVisibility(View.GONE);
            return;
        }

        WeatherRepo.Data d = WeatherRepo.cached(this);
        if (d == null) {
            // Before the first answer (or while the network is down) the line
            // stays out of the way unless a refresh has already failed.
            if (weatherFailed && !weatherLoading) {
                weatherNow.setText(Txt.s(this, "Погода недоступна", "Weather unavailable"));
                weatherNow.setVisibility(View.VISIBLE);
            } else {
                weatherNow.setVisibility(View.GONE);
            }
            weatherForecast.setVisibility(View.GONE);
            return;
        }

        String city = d.city.isEmpty() ? "" : d.city + "  \u00B7  ";
        weatherNow.setText(city + WeatherRepo.signed(d.temp) + "\u00B0  "
                + WeatherRepo.condition(this, d.code));
        weatherNow.setVisibility(View.VISIBLE);

        if (Prefs.weatherForecast(this) && !d.days.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (WeatherRepo.Day day : d.days) {
                if (b.length() > 0) b.append("    ");
                b.append(WeatherRepo.dayName(this, day.iso)).append(' ')
                        .append(WeatherRepo.signed(day.max)).append("\u00B0/")
                        .append(WeatherRepo.signed(day.min)).append("\u00B0");
            }
            weatherForecast.setText(b.toString());
            weatherForecast.setVisibility(View.VISIBLE);
        } else {
            weatherForecast.setVisibility(View.GONE);
        }
    }

    /**
     * Asks for fresh weather when the cache is old. A manual refresh passes
     * force, the automatic one is happy with data younger than half an hour.
     */
    private void refreshWeather(boolean force) {
        if (!Prefs.weather(this)) {
            applyWeather();
            return;
        }
        if (!force && !WeatherRepo.isStale(this)) {
            applyWeather();
            scheduleWeather();
            return;
        }
        if (weatherLoading) return;
        weatherLoading = true;
        WeatherRepo.refresh(this, new WeatherRepo.Callback() {
            @Override
            public void onData(WeatherRepo.Data d) {
                weatherLoading = false;
                weatherFailed = d == null;
                applyWeather();
                scheduleWeather();
            }
        });
    }

    private void scheduleWeather() {
        handler.removeCallbacks(weatherTick);
        if (!Prefs.weather(this)) return;
        // A failed request is retried sooner; a good cache waits the full half hour.
        long delay = weatherFailed ? 5L * 60L * 1000L : WeatherRepo.STALE_MS;
        handler.postDelayed(weatherTick, delay);
    }

    /** Geocodes a typed city, then refreshes the forecast for it. */
    private void resolveWeatherCity(String name) {
        // A switched-off weather section must not start any network work,
        // not even a geocoding lookup for a city typed in the settings.
        if (!Prefs.weather(this)) return;
        WeatherRepo.resolveCity(this, name, new WeatherRepo.PlaceCallback() {
            @Override
            public void onPlace(boolean ok) {
                if (ok) {
                    weatherFailed = false;
                    applyWeather();
                    refreshWeather(true);
                    scheduleWeather();
                } else if (!isFinishing()) {
                    tracked(new InfoDialog(MainActivity.this,
                            Txt.s(MainActivity.this, "Город не найден", "City not found"),
                            Txt.s(MainActivity.this,
                                    "Проверьте название и попробуйте ещё раз.",
                                    "Check the name and try again."))).show();
                }
            }
        });
    }

    // ------------------------------------------------------------- wallpaper

    private static final int MAX_WALLPAPER_SIDE = 1920;

    /** Loads the saved photo from the app storage, if there is one. */
    private void loadWallpaperAsync() {
        final String path = Prefs.wallpaper(this);
        final File file = path.isEmpty() ? null : new File(path);
        if (file == null || !file.exists()) {
            if (!path.isEmpty()) Prefs.setWallpaper(this, "");
            if (backdrop != null) backdrop.setCustom(null);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Exception ignored) {
                }
                final Bitmap bitmap = decodeWallpaperFile(file, wallpaperMaxSide());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || bitmap == null) {
                            if (bitmap != null) bitmap.recycle();
                            return;
                        }
                        if (backdrop != null) backdrop.setCustom(bitmap);
                    }
                });
            }
        }, "vafic-wallpaper-load").start();
    }

    /** Copies the picked image into the launcher storage and shows it. */
    private void saveWallpaper(final Uri uri) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                } catch (Exception ignored) {
                }

                Bitmap bitmap = null;
                try {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    InputStream in = getContentResolver().openInputStream(uri);
                    BitmapFactory.decodeStream(in, null, bounds);
                    if (in != null) in.close();
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new Exception();

                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight,
                            MAX_WALLPAPER_SIDE);
                    in = getContentResolver().openInputStream(uri);
                    bitmap = BitmapFactory.decodeStream(in, null, opts);
                    if (in != null) in.close();
                    if (bitmap == null) throw new Exception();

                    File file = new File(getFilesDir(), "wallpaper.jpg");
                    FileOutputStream out = new FileOutputStream(file);
                    try {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out);
                    } finally {
                        out.close();
                    }
                    Prefs.setWallpaper(MainActivity.this, file.getAbsolutePath());
                } catch (Throwable t) {
                    if (bitmap != null) bitmap.recycle();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing() || isDestroyed()) return;
                            tracked(new InfoDialog(MainActivity.this,
                                    Txt.s(MainActivity.this, "Не удалось открыть изображение",
                                            "Could not open the image"),
                                    Txt.s(MainActivity.this,
                                            "Попробуйте другой файл: JPG или PNG.",
                                            "Try another file: JPG or PNG."))).show();
                        }
                    });
                    return;
                }

                bitmap.recycle();
                final Bitmap shown = decodeWallpaperFile(
                        new File(getFilesDir(), "wallpaper.jpg"), wallpaperMaxSide());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || shown == null) {
                            if (shown != null) shown.recycle();
                            return;
                        }
                        if (backdrop != null) backdrop.setCustom(shown);
                    }
                });
            }
        }, "vafic-wallpaper-save").start();
    }

    /** Half the screen size is plenty for a background behind glass cards. */
    private int wallpaperMaxSide() {
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        return Math.max(640, Math.max(w, h) / 2);
    }

    private static Bitmap decodeWallpaperFile(File file, int maxSide) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide);
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Largest power of two that keeps the decoded side above maxSide. */
    private static int sampleSize(int width, int height, int maxSide) {
        int sample = 1;
        int longest = Math.max(width, height);
        while (longest / (sample * 2) >= maxSide) sample *= 2;
        return sample;
    }

    private void pickWallpaper() {
        Intent open = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        open.addCategory(Intent.CATEGORY_OPENABLE);
        open.setType("image/*");
        try {
            startActivityForResult(open, REQ_WALLPAPER);
            return;
        } catch (ActivityNotFoundException ignored) {
        }

        Intent get = new Intent(Intent.ACTION_GET_CONTENT);
        get.addCategory(Intent.CATEGORY_OPENABLE);
        get.setType("image/*");
        try {
            startActivityForResult(get, REQ_WALLPAPER);
        } catch (ActivityNotFoundException e) {
            if (isFinishing()) return;
            tracked(new InfoDialog(this,
                    Txt.s(this, "Нет выбора файлов", "No file picker"),
                    Txt.s(this, "На этом телевизоре нет приложения для выбора изображений.",
                            "This television has no app for choosing images."))).show();
        }
    }

    private final Runnable weatherTick = new Runnable() {
        @Override
        public void run() {
            refreshWeather(true);
        }
    };

    // ---------------------------------------------------------------- music

    private float musicVolume() {
        return Prefs.musicVolume(this);
    }

    /**
     * The background track is the full lossless FLAC, shipped as an asset and
     * copied to the cache once. It is played by a MediaPlayer with looping
     * enabled. The track itself fades out at the end and fades in at the
     * beginning, so the loop point is smooth and the sound stays untouched.
     */
    private void startMusic() {
        if (!Prefs.music(this) || musicLoading) return;
        try {
            if (music == null) {
                final File f = new File(getCacheDir(), "vafic-ambient.flac");
                if (!f.exists() || f.length() < 4096) {
                    // The track is large, so it is unpacked in the background
                    // and the music starts a moment later.
                    musicLoading = true;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // The lowest priority: unpacking the track
                                // must not disturb the app in front.
                                android.os.Process.setThreadPriority(
                                        android.os.Process.THREAD_PRIORITY_BACKGROUND);
                            } catch (Exception ignored) {
                            }
                            try {
                                copyAsset("ambient.flac", f);
                            } catch (Exception ignored) {
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    musicLoading = false;
                                    startMusic();
                                }
                            });
                        }
                    }, "vafic-music").start();
                    return;
                }
                music = new MediaPlayer();
                music.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build());
                music.setDataSource(f.getAbsolutePath());
                music.setLooping(true);
                music.setVolume(0f, 0f);
                music.prepare();
            }
            if (!music.isPlaying()) {
                music.setVolume(0f, 0f);
                music.start();
                fadeMusicIn();
            }
        } catch (Exception e) {
            releaseMusic();
        }
    }

    private void fadeMusicIn() {
        if (music == null) return;
        if (musicFade != null) handler.removeCallbacks(musicFade);
        final long start = SystemClock.uptimeMillis();
        final float target = musicVolume();
        musicFade = new Runnable() {
            @Override
            public void run() {
                if (music == null) return;
                float t = Math.min(1f, (SystemClock.uptimeMillis() - start) / 1600f);
                // The track stays under the interface sounds: it is a
                // background, not the main event. The level is user-tunable.
                float v = target * t * t * (3f - 2f * t);
                try {
                    music.setVolume(v, v);
                } catch (Exception ignored) {
                }
                if (t < 1f) handler.postDelayed(this, 40);
            }
        };
        handler.post(musicFade);
    }

    private void stopMusic() {
        if (musicFade != null) {
            handler.removeCallbacks(musicFade);
            musicFade = null;
        }
        try {
            if (music != null && music.isPlaying()) {
                music.pause();
            }
        } catch (Exception ignored) {
        }
    }

    /** Applies a new music level while the track is already playing. */
    private void applyMusicVolume() {
        if (music == null) return;
        if (musicFade != null) {
            handler.removeCallbacks(musicFade);
            musicFade = null;
        }
        float v = musicVolume();
        try {
            music.setVolume(v, v);
        } catch (Exception ignored) {
        }
    }

    private void releaseMusic() {
        if (musicFade != null) {
            handler.removeCallbacks(musicFade);
            musicFade = null;
        }
        try {
            if (music != null) music.release();
        } catch (Exception ignored) {
        }
        music = null;
    }

    // ------------------------------------------------------------ navigation sfx

    /**
     * Holds the screen on while the user is busy and lets the TV sleep again
     * afterwards. A launcher that keeps the screen on forever would leave the
     * television running all night.
     */
    private void keepAwake() {
        try {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception ignored) {
        }
        handler.removeCallbacks(sleepAllow);
        handler.postDelayed(sleepAllow, KEEP_AWAKE_MS);
    }

    private void allowSleep() {
        try {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception ignored) {
        }
    }

    private void initSoundEffects() {
        try {
            sfx = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .build();
            sfx.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    if (status != 0) return;
                    if (sampleId == tickSound) tickReady = true;
                    else if (sampleId == forwardSound) forwardReady = true;
                    else if (sampleId == backSound) backReady = true;
                }
            });
            tickSound = loadSound("tick.wav", "vafic-tick.wav");
            forwardSound = loadSound("forward.wav", "vafic-forward.wav");
            backSound = loadSound("back.wav", "vafic-back.wav");
        } catch (Exception e) {
            if (sfx != null) {
                try {
                    sfx.release();
                } catch (Exception ignored) {
                }
            }
            sfx = null;
            tickSound = 0;
            forwardSound = 0;
            backSound = 0;
            tickReady = false;
            forwardReady = false;
            backReady = false;
        }
    }

    private int loadSound(String asset, String cacheName) throws Exception {
        File f = new File(getCacheDir(), cacheName);
        copyAsset(asset, f);
        return sfx.load(f.getAbsolutePath(), 1);
    }

    /**
     * A short glass tick on every focus move. Rapid movement plays it a little
     * higher and softer; slower movement gets the full, warmer tick. The tiny
     * minimum interval prevents the SoundPool from machine-gunning.
     */
    void playNavTick() {
        if (!Prefs.sfx(this) || sfx == null || tickSound == 0 || !tickReady) return;
        long now = SystemClock.uptimeMillis();
        long dt = now - lastNavSound;
        if (dt < 28) return;
        lastNavSound = now;
        float rate = dt < 120 ? 1.07f : 0.99f;
        float level = Prefs.sfxVolume(this);
        float volume = (dt < 120 ? 0.38f : 0.50f) * level;
        sfx.play(tickSound, volume, volume, 1, 0, rate);
    }

    /** Rising glass note: opening a menu, the library or an app. */
    void playForward() {
        if (!Prefs.sfx(this) || sfx == null || forwardSound == 0 || !forwardReady) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastForwardSound < 110) return;
        lastForwardSound = now;
        float volume = 0.36f * Prefs.sfxVolume(this);
        sfx.play(forwardSound, volume, volume, 1, 0, 1f);
    }

    /** Falling glass note: back, closing a panel or leaving the library. */
    void playBack() {
        if (!Prefs.sfx(this) || sfx == null || backSound == 0 || !backReady) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastBackSound < 110) return;
        lastBackSound = now;
        float volume = 0.34f * Prefs.sfxVolume(this);
        sfx.play(backSound, volume, volume, 1, 0, 1f);
    }

    private void copyAsset(String name, File out) throws Exception {
        InputStream in = getAssets().open(name);
        FileOutputStream os = new FileOutputStream(out);
        byte[] buf = new byte[16384];
        int n;
        try {
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        } finally {
            try {
                os.close();
            } catch (Exception ignored) {
            }
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }

    // -------------------------------------------------------------- lifecycle

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_HOME) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) goHome();
            return true;
        }
        // Any key means the user is at the remote: hold the screen on.
        if (event.getAction() == KeyEvent.ACTION_DOWN) keepAwake();
        boolean direction = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN;
        if (direction && event.getAction() == KeyEvent.ACTION_DOWN) Ui.nav();

        if (libraryMoving) {
            boolean relevant = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                    || code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                    || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_BACK;
            if (!relevant) return dispatchToWindow(event);
            if (event.getAction() != KeyEvent.ACTION_DOWN) return true;

            if (code == KeyEvent.KEYCODE_BACK) {
                finishLibraryMove(true);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                finishLibraryMove(false);
                return true;
            }
            moveLibrary(code);
            return true;
        }

        if (movePkg != null) {
            boolean relevant = code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT
                    || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_BACK;
            if (!relevant) return dispatchToWindow(event);
            if (event.getAction() != KeyEvent.ACTION_DOWN) return true;

            if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER) {
                if (code == KeyEvent.KEYCODE_BACK) playBack();
                stopMove();
                return true;
            }
            int delta = code == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1;
            int before = Prefs.home(this).indexOf(movePkg);
            Prefs.move(this, movePkg, delta);
            int after = Prefs.home(this).indexOf(movePkg);
            if (after != before && after >= 0) {
                renderDock(false);
                focusDock(after);
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (libraryOpen) {
                if (code == KeyEvent.KEYCODE_BACK) {
                    closeLibrary();
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_UP) {
                    View f = getCurrentFocus();
                    int i = f == null ? -1 : grid.indexOfChild(f);
                    if (i >= 0 && i < GRID_COLS) {
                        closeLibrary();
                        return true;
                    }
                }
                return dispatchToWindow(event);
            }

            View f = getCurrentFocus();
            boolean inStatus = f != null && f.getParent() == statusBar;
            boolean inDock = f != null && f.getParent() == dockRow;

            // An intentionally empty dock has no focusable card. Down must still
            // open the library, otherwise there is no way back to the app list
            // or to the launcher settings.
            if (code == KeyEvent.KEYCODE_DPAD_DOWN && dockRow.getChildCount() == 0) {
                openLibrary();
                return true;
            }

            if (code == KeyEvent.KEYCODE_BACK) {
                if (inStatus) {
                    playBack();
                    focusDock(dockIndex);
                }
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_UP && inDock) {
                statusBar.focusFirst();
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_DOWN && inStatus) {
                focusDock(dockIndex);
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_DOWN && inDock) {
                openLibrary();
                return true;
            }
            if (inDock && (code == KeyEvent.KEYCODE_DPAD_LEFT
                    || code == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                moveDockFocus(code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
                return true;
            }
        }

        return dispatchToWindow(event);
    }

    /**
     * Dispatches to the view tree, then moves D-pad focus itself if nothing
     * handled the event. Doing the move here means ViewRootImpl never reaches
     * its own focus navigation and never plays the system tick.
     */
    private boolean dispatchToWindow(KeyEvent event) {
        if (super.dispatchKeyEvent(event)) return true;
        return FocusNav.handle(getWindow() == null ? null : getWindow().getDecorView(), event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_HOME)) {
            goHome();
        }
    }

    /** Dialogs live in their own windows, so they get the HOME key directly. */
    static void installHomeKey(final Dialog d, final Activity a) {
        d.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_HOME) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && a instanceof MainActivity) {
                        ((MainActivity) a).goHome();
                    }
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP
                        && a instanceof MainActivity) {
                    ((MainActivity) a).playBack();
                }
                return false;
            }
        });
        // A dialog lives in its own window, so the focus listener of the
        // activity never sees the rows inside it. Every window installs its
        // own tick listener, otherwise the menus would move silently.
        Window win = d.getWindow();
        if (win != null) {
            win.getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener(
                    new ViewTreeObserver.OnGlobalFocusChangeListener() {
                        @Override
                        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                            if (newFocus == null || newFocus == oldFocus
                                    || !newFocus.isClickable()) return;
                            if (a instanceof MainActivity) ((MainActivity) a).playNavTick();
                        }
                    });
        }
    }

    private <T extends Dialog> T tracked(final T d) {
        dialogs.add(d);
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogs.remove(d);
            }
        });
        return d;
    }

    @Override
    protected void onStart() {
        super.onStart();
        AppRepo.setPrefetchPaused(false);
        if (ring != null) ring.resume();
        IntentFilter f = new IntentFilter(NotifListener.ACTION);
        f.addAction(NotifListener.ACTION_HEADS_UP);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(notifReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(notifReceiver, f);
        }

        // Package broadcasts come from the system, so this receiver is
        // exported on Android 13+. It only schedules a refresh; it never
        // trusts the broadcast as proof that a package actually changed.
        if (!packageReceiverRegistered) {
            IntentFilter pf = new IntentFilter();
            pf.addAction(Intent.ACTION_PACKAGE_ADDED);
            pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
            pf.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
            pf.addAction(Intent.ACTION_PACKAGE_REPLACED);
            pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
            pf.addDataScheme("package");
            try {
                if (Build.VERSION.SDK_INT >= 33) {
                    registerReceiver(packageReceiver, pf, Context.RECEIVER_EXPORTED);
                } else {
                    registerReceiver(packageReceiver, pf);
                }
                packageReceiverRegistered = true;
            } catch (Exception ignored) {
            }
        }
        startMusic();
        applyWeather();
        refreshWeather(false);
        scheduleWeather();
    }

    @Override
    protected void onStop() {
        // The artwork is warmed up in the background. While another app is in
        // front the launcher must not take the CPU away from it.
        AppRepo.setPrefetchPaused(true);
        if (ring != null) ring.pause();
        handler.removeCallbacks(sleepAllow);
        handler.removeCallbacks(weatherTick);
        allowSleep();
        stopMusic();
        if (libraryMoving) cancelLibraryMove();
        hideLibraryNoticeNow();
        clearBanners();
        handler.removeCallbacks(packageRefresh);
        synchronized (changedPackages) {
            changedPackages.clear();
        }
        if (ring != null) ring.hide();
        try {
            unregisterReceiver(notifReceiver);
        } catch (Exception ignored) {
        }
        if (packageReceiverRegistered) {
            try {
                unregisterReceiver(packageReceiver);
            } catch (Exception ignored) {
            }
            packageReceiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateClock();
        if (statusBar != null) statusBar.refresh();
        handler.removeCallbacks(tick);
        handler.post(tick);

        if (!libraryOpen) {
            homePage.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }

        String key = TextUtils.join(",", Prefs.home(this));
        boolean needRender = renderedKey != null && !renderedKey.equals(key);

        String sig = AppRepo.installedSignature(this);
        if (installedSignature == null) {
            installedSignature = sig;
        } else if (!installedSignature.equals(sig)) {
            installedSignature = sig;
            AppRepo.invalidateAll();
            cleanMissingApps();
            needRender = true;
        }

        if (needRender) {
            refreshAppsNow();
        } else if (!libraryOpen && dockRow.getChildCount() > 0
                && homePage != null && homePage.findFocus() == null) {
            focusDock(dockIndex);
        } else if (libraryOpen && dialogs.isEmpty()) {
            View f = getCurrentFocus();
            if (!(f instanceof TileView) || grid == null || grid.indexOfChild(f) < 0) {
                focusLibrary();
            }
        }

        if (!Prefs.music(this)) stopMusic();

        View f = getCurrentFocus();
        if (f instanceof TileView && ring != null) ring.follow((TileView) f);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_WALLPAPER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) saveWallpaper(uri);
        }
    }

    @Override
    protected void onDestroy() {
        clearBanners();
        handler.removeCallbacksAndMessages(null);
        releaseMusic();
        if (sfx != null) {
            try {
                sfx.release();
            } catch (Exception ignored) {
            }
            sfx = null;
        }
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) return;
        View f = getCurrentFocus();
        if (f instanceof TileView) {
            if (ring != null) ring.follow((TileView) f);
            return;
        }
        // A cold start can leave the home screen without a focused card. The
        // focus then belongs to the dock, otherwise the first key press would
        // send the cursor to a random place.
        if (!libraryOpen && homePage != null && homePage.findFocus() == null) {
            focusDock(dockIndex);
        } else if (libraryOpen && dialogs.isEmpty()) {
            if (!(f instanceof TileView) || grid == null || grid.indexOfChild(f) < 0) {
                focusLibrary();
            }
        }
        if (ring != null) ring.hide();
    }
}
