package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Launcher settings: sound, dock, weather, wallpaper, language, the
 * notification filter and a link to the system settings. The list scrolls
 * inside a clipped viewport so all of it fits on a television screen.
 */
public class SettingsPanel extends VaficDialog {

    public interface Listener {
        void onOpenSound();

        void onOpenDock();

        void onOpenWeather();

        void onOpenWallpaper();

        void onLanguageChanged();

        void onOpenNotifSettings();

        void onOpenHiddenApps();

        void onOpenSystemSettings();

        void onClose();
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final SmoothColumn listScroll;
    private final TextView soundValue;
    private final TextView dockValue;
    private final TextView weatherValue;
    private final TextView wallpaperValue;
    private View firstRow;
    private View languageRow;
    private LanguagePopup languagePopup;

    public SettingsPanel(Activity a, final Listener listener) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        density = a.getResources().getDisplayMetrics().density;

        card = new GlassPanel(a, density, 30);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(18), dp(24), dp(16));
        box.setClipChildren(false);
        box.setClipToPadding(false);
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView title = new TextView(a);
        title.setText(Txt.s(a, "Настройки лаунчера", "Launcher settings"));
        title.setTextSize(23);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText("Vafic Launcher");
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setLetterSpacing(0.06f);
        sub.setPadding(0, dp(2), 0, dp(10));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        listScroll = new SmoothColumn(a, dp(358));
        listScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listScroll.setClipChildren(true);
        listScroll.setClipToPadding(true);

        LinearLayout rows = new LinearLayout(a);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(false);
        rows.setClipToPadding(false);
        rows.setPadding(dp(2), dp(2), dp(2), dp(2));
        listScroll.addView(rows, new FrameLayout.LayoutParams(MATCH, WRAP));

        // The viewport cuts the rows at the list edge: a ScrollView clips in
        // the content coordinate space, and rows would otherwise be drawn over
        // the title (the same guard used by the notification lists).
        FrameLayout viewport = new FrameLayout(a);
        viewport.setClipChildren(true);
        viewport.setClipToPadding(true);
        viewport.addView(listScroll, new FrameLayout.LayoutParams(MATCH, WRAP));
        box.addView(viewport, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout sound = row(Txt.s(a, "Звук", "Sound"), soundSummary(), false);
        soundValue = (TextView) sound.getChildAt(1);
        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenSound();
            }
        });
        rows.addView(sound, lp(0));
        firstRow = sound;

        LinearLayout dock = row(Txt.s(a, "Док", "Dock"), dockSummary(), false);
        dockValue = (TextView) dock.getChildAt(1);
        dock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenDock();
            }
        });
        rows.addView(dock, lp(6));

        LinearLayout weather = row(Txt.s(a, "Погода", "Weather"),
                onOff(Prefs.weather(a)), Prefs.weather(a));
        weatherValue = (TextView) weather.getChildAt(1);
        weather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenWeather();
            }
        });
        rows.addView(weather, lp(6));

        LinearLayout wallpaper = row(Txt.s(a, "Фон", "Wallpaper"),
                wallpaperSummary(), false);
        wallpaperValue = (TextView) wallpaper.getChildAt(1);
        wallpaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenWallpaper();
            }
        });
        rows.addView(wallpaper, lp(6));

        this.languageRow = row(Txt.s(a, "Язык", "Language"),
                Txt.languageName(a) + "  \u25BE", false);
        this.languageRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (languagePopup != null) return;
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                languagePopup = new LanguagePopup(activity, loc[0], loc[1],
                        v.getWidth(), v.getHeight(),
                        new LanguagePopup.Listener() {
                            @Override
                            public void onPicked(String language) {
                                dismiss();
                                listener.onLanguageChanged();
                            }
                        });
                languagePopup.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        languagePopup = null;
                        if (isShowing() && SettingsPanel.this.languageRow != null) {
                            final View row = SettingsPanel.this.languageRow;
                            row.post(new Runnable() {
                                @Override
                                public void run() {
                                    row.requestFocus();
                                }
                            });
                        }
                    }
                });
                languagePopup.show();
            }
        });
        rows.addView(this.languageRow, lp(6));

        LinearLayout notifRow = row(Txt.s(a, "Уведомления приложений", "App notifications"),
                "\u203A", false);
        notifRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenNotifSettings();
            }
        });
        rows.addView(notifRow, lp(6));

        LinearLayout hiddenRow = row(Txt.s(a, "Скрытые приложения", "Hidden apps"),
                hiddenValue(), false);
        hiddenRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenHiddenApps();
            }
        });
        rows.addView(hiddenRow, lp(6));

        LinearLayout systemRow = row(Txt.s(a, "Системные настройки", "System settings"),
                "\u203A", false);
        systemRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenSystemSettings();
            }
        });
        rows.addView(systemRow, lp(6));

        LinearLayout closeRow = row(Txt.s(a, "Закрыть", "Close"), "", false);
        closeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClose();
                dismiss();
            }
        });
        rows.addView(closeRow, lp(12));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — выбрать      •      BACK — закрыть",
                "OK — select      •      BACK — close"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(10), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(card, new ViewGroup.LayoutParams(dp(600), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wl = w.getAttributes();
            wl.dimAmount = 0.55f;
            wl.width = dp(600);
            wl.height = WRAP;
            wl.gravity = Gravity.CENTER;
            w.setAttributes(wl);
        }
        MainActivity.installHomeKey(this, a);
    }

    private String soundSummary() {
        boolean music = Prefs.music(activity);
        boolean sfx = Prefs.sfx(activity);
        if (!music && !sfx) return Txt.s(activity, "Выкл", "Off");
        return Txt.s(activity, "Вкл", "On");
    }

    private String dockSummary() {
        int n = Prefs.dockSize(activity);
        String size = DockSettingsDialog.sizeName(activity,
                DockSettingsDialog.levelFor(activity));
        if (Txt.russian(activity)) {
            int mod10 = n % 10;
            int mod100 = n % 100;
            String word;
            if (mod10 == 1 && mod100 != 11) word = "приложение";
            else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) word = "приложения";
            else word = "приложений";
            return size + " · " + n + " " + word;
        }
        return size + " · " + n + (n == 1 ? " app" : " apps");
    }

    private String wallpaperSummary() {
        return Prefs.wallpaper(activity).isEmpty()
                ? Txt.s(activity, "Стандартный", "Default")
                : Txt.s(activity, "Свой", "Custom");
    }

    private LinearLayout row(String label, String value, boolean accent) {
        LinearLayout r = new LinearLayout(activity);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(7), dp(16), dp(7));
        r.setMinimumHeight(dp(46));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        TextView name = new TextView(activity);
        name.setText(label);
        name.setTextSize(15.5f);
        name.setTextColor(0xFFF5F5F7);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView val = new TextView(activity);
        val.setText(value);
        val.setTextSize(13);
        val.setTextColor(accent ? 0xFF8FE3A4 : 0x8AFFFFFF);
        val.setPadding(dp(12), 0, 0, 0);
        r.addView(val, new LinearLayout.LayoutParams(WRAP, WRAP));

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) listScroll.ensureVisible(v);
            }
        });
        return r;
    }

    private String onOff(boolean on) {
        return Txt.s(activity, on ? "Вкл" : "Выкл", on ? "On" : "Off");
    }

    /** Shows how many apps are hidden, or a plain arrow when there are none. */
    private String hiddenValue() {
        int n = Prefs.hidden(activity).size();
        if (n <= 0) return "\u203A";
        return Txt.s(activity, "Скрыто: ", "Hidden: ") + n;
    }

    private LinearLayout.LayoutParams lp(int topMarginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.topMargin = dp(topMarginDp);
        return p;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void dismiss() {
        if (languagePopup != null) {
            LanguagePopup popup = languagePopup;
            languagePopup = null;
            popup.dismiss();
        }
        super.dismiss();
    }

    @Override
    public void show() {
        super.show();
        if (firstRow != null) {
            final View f = firstRow;
            f.post(new Runnable() {
                @Override
                public void run() {
                    f.requestFocus();
                }
            });
        }
        if (card != null) {
            card.setAlpha(0f);
            card.setScaleX(0.96f);
            card.setScaleY(0.96f);
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }
}
