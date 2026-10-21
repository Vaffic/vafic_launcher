package com.vafic.launcher;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

/** Top-right cluster: notifications, network and launcher settings. All are D-pad focusable. */
public class StatusBar extends LinearLayout {

    public interface Listener {
        void onNotifications();

        void onNetwork();

        void onSettings();
    }

    private final float density;
    private final NotifBadge badge;
    private final StatusView network;
    private final StatusView settings;
    private Listener listener;

    public StatusBar(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        // The focused badge and icons grow slightly. Without this the cluster
        // cuts the glow into a flat edge, which looks like a clipped circle.
        setClipChildren(false);
        setClipToPadding(false);

        badge = new NotifBadge(c);
        addView(badge, new LayoutParams(dp(36), dp(36)));
        badge.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onNotifications();
            }
        });

        network = new StatusView(c);
        LayoutParams lp = new LayoutParams(dp(36), dp(36));
        lp.leftMargin = dp(10);
        addView(network, lp);
        network.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onNetwork();
            }
        });

        settings = new StatusView(c, true);
        LayoutParams slp = new LayoutParams(dp(36), dp(36));
        slp.leftMargin = dp(10);
        addView(settings, slp);
        settings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) listener.onSettings();
            }
        });
    }

    public void setListener(Listener l) {
        listener = l;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    /** Focus the first visible status icon, even when the notification badge is hidden. */
    public void focusFirst() {
        if (badge.getVisibility() == VISIBLE) badge.requestFocus();
        else network.requestFocus();
    }

    public void focusNotifications() {
        badge.requestFocus();
    }

    public void focusNetwork() {
        network.requestFocus();
    }

    public void refresh() {
        network.refresh();
        int n = NotifListener.count();
        badge.setCount(n);
        badge.setVisibility(n > 0 ? VISIBLE : GONE);
    }
}
