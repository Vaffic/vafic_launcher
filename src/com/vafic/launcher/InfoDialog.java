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

/** Small message window in the launcher style, used for rare notices. */
public class InfoDialog extends VaficDialog {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final float density;
    private final FrameLayout card;
    private final TextView okRow;

    public InfoDialog(Activity a, String title, String message) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        density = a.getResources().getDisplayMetrics().density;

        card = new GlassPanel(a, density, 28);
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(24), dp(20), dp(24), dp(16));
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView heading = new TextView(a);
        heading.setText(title);
        heading.setTextSize(20);
        heading.setTextColor(0xFFFFFFFF);
        heading.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));
        heading.setGravity(Gravity.CENTER);
        box.addView(heading, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView body = new TextView(a);
        body.setText(message);
        body.setTextSize(14);
        body.setTextColor(0xB3FFFFFF);
        body.setGravity(Gravity.CENTER);
        body.setLineSpacing(dp(3), 1f);
        body.setPadding(0, dp(10), 0, dp(16));
        box.addView(body, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView ok = new TextView(a);
        ok.setText(Txt.s(a, "Понятно", "OK"));
        ok.setTextSize(16);
        ok.setTextColor(0xFFF5F5F7);
        ok.setGravity(Gravity.CENTER);
        ok.setMinHeight(dp(48));
        ok.setFocusable(true);
        ok.setFocusableInTouchMode(true);
        ok.setClickable(true);
        ok.setBackground(Styles.row(density, 16));
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        box.addView(ok, new LinearLayout.LayoutParams(MATCH, WRAP));
        okRow = ok;

        setContentView(card, new ViewGroup.LayoutParams(dp(540), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wl = w.getAttributes();
            wl.dimAmount = 0.6f;
            wl.width = dp(540);
            wl.height = WRAP;
            wl.gravity = Gravity.CENTER;
            w.setAttributes(wl);
        }
        MainActivity.installHomeKey(this, a);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        okRow.post(new Runnable() {
            @Override
            public void run() {
                okRow.requestFocus();
            }
        });
        card.setAlpha(0f);
        card.setScaleX(0.95f);
        card.setScaleY(0.95f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(190)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
}
