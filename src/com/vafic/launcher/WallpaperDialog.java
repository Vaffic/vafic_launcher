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
 * Wallpaper section: pick a personal photo, tune how much it is darkened for
 * readability, or return to the built-in Aero backdrop. The chosen image is
 * copied into the launcher's private storage, so the original can be moved or
 * deleted without breaking the home screen.
 */
public class WallpaperDialog extends VaficDialog {

    public interface Listener {
        void onPickImage();

        void onReset();

        void onDimChanged(int percent);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final View firstRow;

    public WallpaperDialog(Activity a, final Listener listener) {
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
        title.setText(Txt.s(a, "Фон", "Wallpaper"));
        title.setTextSize(23);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText(Txt.s(a, "Своя фотография вместо стандартного фона",
                "Your own photo instead of the built-in background"));
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setPadding(0, dp(2), 0, dp(12));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout pick = row(Txt.s(a, "Выбрать изображение", "Choose an image"), "\u203A");
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onPickImage();
            }
        });
        firstRow = pick;
        box.addView(pick, lp(0));

        SliderRow dim = new SliderRow(a, density,
                Txt.s(a, "Затемнение фона", "Background dim"),
                0, 80, 5, Prefs.wallpaperDim(a), "%");
        dim.setListener(new SliderRow.Listener() {
            @Override
            public void onChanged(int value) {
                Prefs.setWallpaperDim(activity, value);
                listener.onDimChanged(value);
            }
        });
        box.addView(dim, lp(6));

        LinearLayout reset = row(Txt.s(a, "Стандартный фон", "Default background"), "");
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onReset();
            }
        });
        box.addView(reset, lp(6));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — выбрать      •      BACK — назад",
                "OK — select      •      BACK — back"));
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

    private LinearLayout row(String label, String value) {
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
        val.setTextColor(0x8AFFFFFF);
        val.setPadding(dp(12), 0, 0, 0);
        r.addView(val, new LinearLayout.LayoutParams(WRAP, WRAP));
        return r;
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
