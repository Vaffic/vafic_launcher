package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Dark glass network card: connection type, signal, IP and a shortcut to system Wi-Fi settings. */
public class NetworkPanel extends VaficDialog {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final float density;
    private final FrameLayout card;
    private final Activity activity;
    private View firstRow;

    public NetworkPanel(Activity a) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        density = a.getResources().getDisplayMetrics().density;
        activity = a;

        NetInfo.State s = NetInfo.read(a);

        card = new GlassPanel(a, density, 30);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(28), dp(22), dp(28), dp(22));
        box.setClipChildren(false);
        box.setClipToPadding(false);
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView title = new TextView(a);
        title.setText(Txt.s(a, "Сеть", "Network"));
        title.setTextSize(24);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText(Txt.s(a, "Текущее подключение", "Current connection"));
        sub.setTextSize(12);
        sub.setTextColor(0x66FFFFFF);
        sub.setLetterSpacing(0.05f);
        sub.setPadding(0, dp(3), 0, dp(16));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout info = new LinearLayout(a);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(20), dp(16), dp(20), dp(16));
        info.setBackground(Styles.chip(density, 18, 0x14FFFFFF));
        box.addView(info, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout modeRow = new LinearLayout(a);
        modeRow.setOrientation(LinearLayout.HORIZONTAL);
        modeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView mode = new TextView(a);
        mode.setTextSize(19);
        mode.setTextColor(0xFFF5F5F7);
        mode.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        mode.setText(s.mode == NetInfo.WIFI ? "Wi-Fi"
                : s.mode == NetInfo.ETHERNET ? "Ethernet"
                : Txt.s(a, "Нет сети", "No network"));
        modeRow.addView(mode, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView level = new TextView(a);
        level.setTextSize(13);
        level.setTextColor(s.mode == NetInfo.OFFLINE ? 0x80FFFFFF : 0x99FFFFFF);
        if (s.mode == NetInfo.WIFI) {
            level.setText(s.percent() + "%");
        } else if (s.mode == NetInfo.ETHERNET) {
            level.setText(Txt.s(a, "кабель", "cable"));
        } else {
            level.setText("offline");
        }
        modeRow.addView(level, new LinearLayout.LayoutParams(WRAP, WRAP));
        info.addView(modeRow, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView ssid = new TextView(a);
        ssid.setTextSize(14);
        ssid.setTextColor(0xA6FFFFFF);
        ssid.setPadding(0, dp(4), 0, 0);
        if (s.mode == NetInfo.WIFI) {
            ssid.setText(s.ssid != null ? s.ssid
                    : Txt.s(a, "Wi-Fi подключён", "Wi-Fi connected"));
        } else if (s.mode == NetInfo.ETHERNET) {
            ssid.setText(Txt.s(a, "Подключение по кабелю", "Wired connection"));
        } else {
            ssid.setText(Txt.s(a, "Проверьте кабель или Wi-Fi", "Check the cable or Wi-Fi"));
        }
        info.addView(ssid, new LinearLayout.LayoutParams(MATCH, WRAP));

        if (s.mode == NetInfo.WIFI) {
            TextView signal = new TextView(a);
            signal.setTextSize(13);
            signal.setTextColor(0x80FFFFFF);
            signal.setPadding(0, dp(8), 0, 0);
            signal.setText(Txt.s(a, "Уровень сигнала ", "Signal level ")
                    + s.percent() + "%   ·   " + s.rssi + " dBm");
            info.addView(signal, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        if (s.ip != null) {
            TextView ip = new TextView(a);
            ip.setTextSize(13);
            ip.setTextColor(0x80FFFFFF);
            ip.setPadding(0, dp(4), 0, 0);
            ip.setText("IP  " + s.ip);
            info.addView(ip, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        View settings = action(Txt.s(a, "Настройки сети", "Network settings"), true);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (activity instanceof MainActivity) ((MainActivity) activity).playForward();
                openNetworkSettings();
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, WRAP);
        slp.topMargin = dp(16);
        box.addView(settings, slp);
        firstRow = settings;

        View close = action(Txt.s(a, "Закрыть", "Close"), false);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity instanceof MainActivity) ((MainActivity) activity).playBack();
                dismiss();
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(MATCH, WRAP);
        clp.topMargin = dp(8);
        box.addView(close, clp);

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — выбрать      •      BACK — закрыть",
                "OK — select      •      BACK — close"));
        foot.setTextSize(12);
        foot.setTextColor(0x59FFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(14), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(card, new ViewGroup.LayoutParams(dp(580), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.55f;
            lp.width = dp(580);
            lp.height = WRAP;
            lp.gravity = Gravity.CENTER;
            w.setAttributes(lp);
        }
        MainActivity.installHomeKey(this, a);
    }

    private View action(String text, boolean primary) {
        TextView v = new TextView(activity);
        v.setText(text);
        v.setTextSize(16);
        v.setTextColor(0xFFF5F5F7);
        v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(52));
        v.setFocusable(true);
        v.setFocusableInTouchMode(true);
        v.setClickable(true);
        v.setBackground(Styles.row(density, 16));
        return v;
    }

    private void openNetworkSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(i);
        } catch (Exception e) {
            try {
                Intent i = new Intent(Settings.ACTION_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(i);
            } catch (Exception ignored) {
            }
        }
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
        if (card != null) {
            card.setAlpha(0f);
            card.setScaleX(0.96f);
            card.setScaleY(0.96f);
            card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }
}
