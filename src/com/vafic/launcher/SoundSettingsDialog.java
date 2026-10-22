package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
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
 * Sound section: the background music and the interface sounds can be switched
 * off independently and each has its own level. Everything applies live, so
 * the user hears the result while moving the slider.
 */
public class SoundSettingsDialog extends VaficDialog {

    public interface Listener {
        void onMusicChanged(boolean on);

        void onSfxChanged(boolean on);

        void onMusicVolume(int percent);

        void onSfxVolume(int percent);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final TextView musicValue;
    private final TextView sfxValue;
    private final View firstRow;
    private boolean music;
    private boolean sfx;

    public SoundSettingsDialog(Activity a, final Listener listener) {
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
        title.setText(Txt.s(a, "Звук", "Sound"));
        title.setTextSize(23);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText(Txt.s(a, "Музыка и звуки интерфейса настраиваются отдельно",
                "Music and interface sounds are tuned separately"));
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setPadding(0, dp(2), 0, dp(12));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout musicRow = row(Txt.s(a, "Фоновая музыка", "Background music"), music);
        musicValue = (TextView) musicRow.getChildAt(1);
        musicRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                music = !music;
                Prefs.setMusic(activity, music);
                updateToggle(musicValue, music);
                listener.onMusicChanged(music);
            }
        });
        firstRow = musicRow;
        box.addView(musicRow, lp(0));

        SliderRow musicLevel = new SliderRow(a, density,
                Txt.s(a, "Громкость музыки", "Music volume"),
                0, 100, 5, Math.round(Prefs.musicVolume(a) * 100f), "%");
        musicLevel.setListener(new SliderRow.Listener() {
            @Override
            public void onChanged(int value) {
                Prefs.setMusicVolume(activity, value / 100f);
                listener.onMusicVolume(value);
            }
        });
        box.addView(musicLevel, lp(6));

        LinearLayout sfxRow = row(Txt.s(a, "Звуки интерфейса", "Interface sounds"), sfx);
        sfxValue = (TextView) sfxRow.getChildAt(1);
        sfxRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sfx = !sfx;
                Prefs.setSfx(activity, sfx);
                updateToggle(sfxValue, sfx);
                listener.onSfxChanged(sfx);
            }
        });
        box.addView(sfxRow, lp(10));

        SliderRow sfxLevel = new SliderRow(a, density,
                Txt.s(a, "Громкость звуков", "Interface volume"),
                0, 100, 5, Math.round(Prefs.sfxVolume(a) * 100f), "%");
        sfxLevel.setListener(new SliderRow.Listener() {
            @Override
            public void onChanged(int value) {
                Prefs.setSfxVolume(activity, value / 100f);
                listener.onSfxVolume(value);
            }
        });
        box.addView(sfxLevel, lp(6));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "\u2190 \u2192 — уровень      •      OK — вкл/выкл      •      BACK — назад",
                "\u2190 \u2192 — level      •      OK — on/off      •      BACK — back"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(14), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(card, new ViewGroup.LayoutParams(dp(620), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wl = w.getAttributes();
            wl.dimAmount = 0.55f;
            wl.width = dp(620);
            wl.height = WRAP;
            wl.gravity = Gravity.CENTER;
            w.setAttributes(wl);
        }
        MainActivity.installHomeKey(this, a);
    }

    private LinearLayout row(String label, boolean on) {
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
        val.setTextSize(13);
        val.setPadding(dp(12), 0, 0, 0);
        r.addView(val, new LinearLayout.LayoutParams(WRAP, WRAP));
        updateToggle(val, on);
        return r;
    }

    private void updateToggle(TextView v, boolean on) {
        v.setText(Txt.s(activity, on ? "Вкл" : "Выкл", on ? "On" : "Off"));
        v.setTextColor(on ? 0xFF8FE3A4 : 0x8AFFFFFF);
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
        card.setAlpha(0f);
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
}
