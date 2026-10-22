package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

/** Dark liquid-glass alert: a title plus a short list of focusable items. */
public class MenuDialog extends VaficDialog {

    public interface Listener {
        void onItem(int index);
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final float density;
    private final List<TextView> rows = new ArrayList<>();
    private final FrameLayout card;

    public MenuDialog(Context c, String title, String[] items, final Listener listener) {
        super(c);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        density = c.getResources().getDisplayMetrics().density;

        card = new GlassPanel(c, density, 30);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(26), dp(22), dp(26), dp(22));
        box.setClipChildren(false);
        box.setClipToPadding(false);
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView heading = new TextView(c);
        heading.setText(title);
        heading.setTextSize(20);
        heading.setGravity(Gravity.CENTER);
        heading.setTextColor(0xFFFFFFFF);
        heading.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        heading.setPadding(dp(8), 0, dp(8), dp(16));
        box.addView(heading, new LinearLayout.LayoutParams(MATCH, WRAP));

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            TextView item = new TextView(c);
            item.setText(items[i]);
            item.setTextSize(17);
            item.setTextColor(0xFFF5F5F7);
            item.setGravity(Gravity.CENTER);
            item.setMinHeight(dp(52));
            item.setPadding(dp(16), 0, dp(16), 0);
            item.setFocusable(true);
            item.setFocusableInTouchMode(true);
            item.setClickable(true);
            item.setBackground(Styles.row(density, 16));

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                    listener.onItem(index);
                }
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = dp(8);
            box.addView(item, lp);
            rows.add(item);
        }

        setContentView(card, new ViewGroup.LayoutParams(dp(470), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.55f;
            lp.width = dp(470);
            lp.height = WRAP;
            lp.gravity = Gravity.CENTER;
            w.setAttributes(lp);
        }
        if (c instanceof Activity) MainActivity.installHomeKey(this, (Activity) c);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        if (!rows.isEmpty()) {
            final TextView first = rows.get(0);
            first.post(new Runnable() {
                @Override
                public void run() {
                    first.requestFocus();
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
