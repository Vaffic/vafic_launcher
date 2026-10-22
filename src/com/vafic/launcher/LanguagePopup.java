package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
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
 * Anchored language menu. It opens directly under the Language row instead of
 * cycling through the values, so the available choices are always visible.
 */
public class LanguagePopup extends VaficDialog {

    public interface Listener {
        void onPicked(String language);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private View selectedRow;

    public LanguagePopup(Activity a, int anchorX, int anchorY, int anchorW, int anchorH,
                         final Listener listener) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        density = a.getResources().getDisplayMetrics().density;

        final String[] values = new String[]{Txt.SYSTEM, Txt.RU, Txt.EN};
        final String[] labels = new String[]{
                Txt.s(a, "Системный", "System"),
                "Русский",
                "English"
        };
        final String current = Prefs.language(a);

        card = new GlassPanel(a, density, 24);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        for (int i = 0; i < values.length; i++) {
            final String value = values[i];
            final boolean selected = value.equals(current);

            LinearLayout item = new LinearLayout(a);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(16), 0, dp(16), 0);
            item.setMinimumHeight(dp(48));
            item.setFocusable(true);
            item.setFocusableInTouchMode(true);
            item.setClickable(true);
            item.setBackground(Styles.row(density, 14));
            item.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

            TextView name = new TextView(a);
            name.setText(labels[i]);
            name.setTextSize(15.5f);
            name.setTextColor(0xFFF5F5F7);
            item.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

            TextView check = new TextView(a);
            check.setText(selected ? "\u2713" : "");
            check.setTextSize(15);
            check.setTextColor(0xFF8FE3A4);
            check.setGravity(Gravity.END);
            item.addView(check, new LinearLayout.LayoutParams(WRAP, WRAP));

            if (selected) selectedRow = item;

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean changed = !value.equals(Prefs.language(activity));
                    dismiss();
                    if (changed) {
                        Prefs.setLanguage(activity, value);
                        listener.onPicked(value);
                    }
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            if (i > 0) lp.topMargin = dp(6);
            box.addView(item, lp);
        }

        int width = Math.min(Math.max(dp(240), anchorW), dp(300));
        setContentView(card, new ViewGroup.LayoutParams(width, WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            int screenW = a.getResources().getDisplayMetrics().widthPixels;
            int screenH = a.getResources().getDisplayMetrics().heightPixels;
            int preferredX = anchorX + anchorW - width;
            int x = Math.max(dp(8), Math.min(preferredX, screenW - width - dp(8)));
            int estimatedH = dp(184);
            int y = anchorY + anchorH + dp(6);
            if (y + estimatedH > screenH - dp(12)) {
                y = Math.max(dp(12), anchorY - estimatedH - dp(6));
            }
            lp.width = width;
            lp.height = WRAP;
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.x = x;
            lp.y = y;
            w.setAttributes(lp);
        }

        setCanceledOnTouchOutside(true);
        MainActivity.installHomeKey(this, a);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        if (selectedRow != null) {
            selectedRow.requestFocus();
        }
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(-dp(6));
            card.animate().alpha(1f).translationY(0f).setDuration(150)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }
}
