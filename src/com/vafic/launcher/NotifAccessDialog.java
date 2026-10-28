package com.vafic.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
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

/** Asks the user to grant notification access to the launcher. */
public class NotifAccessDialog extends VaficDialog {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final View firstRow;

    public NotifAccessDialog(Activity a) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        density = a.getResources().getDisplayMetrics().density;

        card = new GlassPanel(a, density, 30);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(26), dp(22), dp(26), dp(16));
        box.setClipChildren(false);
        box.setClipToPadding(false);
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView title = new TextView(a);
        title.setText(Txt.s(a, "Нужен доступ к уведомлениям", "Notification access needed"));
        title.setTextSize(22);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView body = new TextView(a);
        body.setText(Txt.s(a,
                "Чтобы показывать уведомления приложений, лаунчеру нужно разрешение на их чтение. "
                        + "Откроются системные настройки: найдите Vafic Launcher и включите доступ.",
                "To show app notifications, the launcher needs permission to read them. "
                        + "System settings will open: find Vafic Launcher and turn access on."));
        body.setTextSize(14);
        body.setTextColor(0xB3FFFFFF);
        body.setLineSpacing(0f, 1.15f);
        body.setPadding(0, dp(10), 0, dp(14));
        box.addView(body, new LinearLayout.LayoutParams(MATCH, WRAP));

        if (Build.VERSION.SDK_INT >= 33) {
            body.setPadding(0, dp(10), 0, dp(8));
            TextView hint = new TextView(a);
            hint.setText(Txt.s(a,
                    "Если система пишет «Настройки с ограниченным доступом», нажмите "
                            + "«Снять ограничение» ниже, затем снова «Выдать доступ».",
                    "If the system says “Restricted settings”, press “Remove restriction” "
                            + "below, then “Grant access” again."));
            hint.setTextSize(12.5f);
            hint.setTextColor(0xFFFFD9A0);
            hint.setPadding(0, 0, 0, dp(14));
            box.addView(hint, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        View grant = row(Txt.s(a, "Выдать доступ", "Grant access"), true);
        grant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifAccess.markAsked();
                dismiss();
                NotifAccess.open(activity);
            }
        });
        box.addView(grant, lp(0));
        firstRow = grant;

        if (Build.VERSION.SDK_INT >= 33) {
            View unlock = row(Txt.s(a, "Снять ограничение", "Remove restriction"), false);
            unlock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NotifAccess.markAsked();
                    dismiss();
                    NotifAccess.openAppInfo(activity);
                }
            });
            box.addView(unlock, lp(6));
        }

        View later = row(Txt.s(a, "Позже", "Later"), false);
        later.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifAccess.markAsked();
                dismiss();
            }
        });
        box.addView(later, lp(6));

        View never = row(Txt.s(a, "Больше не напоминать", "Don't remind me"), false);
        never.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotifAccess.neverAsk(activity);
                dismiss();
            }
        });
        box.addView(never, lp(6));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — выбрать      •      BACK — закрыть",
                "OK — select      •      BACK — close"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(12), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(card, new ViewGroup.LayoutParams(dp(560), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams wl = w.getAttributes();
            wl.dimAmount = 0.6f;
            wl.width = dp(560);
            wl.height = WRAP;
            wl.gravity = Gravity.CENTER;
            w.setAttributes(wl);
        }
        MainActivity.installHomeKey(this, a);
    }

    private View row(String label, boolean accent) {
        LinearLayout r = new LinearLayout(activity);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(8), dp(16), dp(8));
        r.setMinimumHeight(dp(48));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        TextView t = new TextView(activity);
        t.setText(label);
        t.setTextSize(15.5f);
        t.setTextColor(accent ? 0xFF8FE3A4 : 0xFFF5F5F7);
        if (accent) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        r.addView(t, new LinearLayout.LayoutParams(0, WRAP, 1f));
        return r;
    }

    private LinearLayout.LayoutParams lp(int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.topMargin = dp(topDp);
        return p;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        firstRow.post(new Runnable() {
            @Override
            public void run() {
                firstRow.requestFocus();
            }
        });
        card.setAlpha(0f);
        card.setScaleX(0.96f);
        card.setScaleY(0.96f);
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
}
