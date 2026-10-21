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

/** Launcher settings: audio, language, the notification filter and a link to the system settings. */
public class SettingsPanel extends Dialog {

    public interface Listener {
        void onMusicChanged(boolean on);

        void onSfxChanged(boolean on);

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
    private final TextView musicValue;
    private final TextView sfxValue;
    private View firstRow;
    private View languageRow;
    private LanguagePopup languagePopup;
    private boolean music;
    private boolean sfx;

    public SettingsPanel(Activity a, final Listener listener) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        density = a.getResources().getDisplayMetrics().density;
        music = Prefs.music(a);
        sfx = Prefs.sfx(a);

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
        sub.setPadding(0, dp(2), 0, dp(12));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        final LinearLayout musicRow = row(a, Txt.s(a, "Фоновая музыка", "Background music"),
                onOff(music), music);
        firstRow = musicRow;
        musicValue = (TextView) musicRow.getChildAt(1);
        musicRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                music = !music;
                Prefs.setMusic(activity, music);
                musicValue.setText(onOff(music));
                musicValue.setTextColor(music ? 0xFF8FE3A4 : 0x8AFFFFFF);
                listener.onMusicChanged(music);
            }
        });
        box.addView(musicRow, lp(0));

        LinearLayout sfxRow = row(a, Txt.s(a, "Звуки интерфейса", "Interface sounds"),
                onOff(sfx), sfx);
        sfxValue = (TextView) sfxRow.getChildAt(1);
        sfxRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sfx = !sfx;
                Prefs.setSfx(activity, sfx);
                sfxValue.setText(onOff(sfx));
                sfxValue.setTextColor(sfx ? 0xFF8FE3A4 : 0x8AFFFFFF);
                listener.onSfxChanged(sfx);
            }
        });
        box.addView(sfxRow, lp(6));

        this.languageRow = row(a, Txt.s(a, "Язык", "Language"),
                Txt.languageName(a) + "  \u25BE", false);
        this.languageRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (languagePopup != null) return;
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                languagePopup = new LanguagePopup(activity, loc[0], loc[1],
                        v.getWidth(), v.getHeight(), new LanguagePopup.Listener() {
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
                v.clearFocus();
                languagePopup.show();
            }
        });
        box.addView(this.languageRow, lp(6));

        LinearLayout notifRow = row(a, Txt.s(a, "Уведомления приложений", "App notifications"),
                "\u203A", false);
        notifRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenNotifSettings();
            }
        });
        box.addView(notifRow, lp(6));

        LinearLayout hiddenRow = row(a, Txt.s(a, "Скрытые приложения", "Hidden apps"),
                hiddenValue(), false);
        hiddenRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenHiddenApps();
            }
        });
        box.addView(hiddenRow, lp(6));

        LinearLayout systemRow = row(a, Txt.s(a, "Системные настройки", "System settings"),
                "\u203A", false);
        systemRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                listener.onOpenSystemSettings();
            }
        });
        box.addView(systemRow, lp(6));

        LinearLayout closeRow = row(a, Txt.s(a, "Закрыть", "Close"), "", false);
        closeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClose();
                dismiss();
            }
        });
        box.addView(closeRow, lp(12));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — выбрать      •      BACK — закрыть",
                "OK — select      •      BACK — close"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(12), 0, 0);
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

    private LinearLayout row(Activity a, String label, String value, boolean accent) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(7), dp(16), dp(7));
        r.setMinimumHeight(dp(46));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        TextView name = new TextView(a);
        name.setText(label);
        name.setTextSize(15.5f);
        name.setTextColor(0xFFF5F5F7);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView val = new TextView(a);
        val.setText(value);
        val.setTextSize(13);
        val.setTextColor(accent ? 0xFF8FE3A4 : 0x8AFFFFFF);
        val.setPadding(dp(12), 0, 0, 0);
        r.addView(val, new LinearLayout.LayoutParams(WRAP, WRAP));
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
