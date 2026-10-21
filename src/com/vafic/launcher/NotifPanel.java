package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Dark liquid-glass notification shade listing active notifications. */
public class NotifPanel extends Dialog {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final float density;
    private final SimpleDateFormat timeFmt;
    private final FrameLayout card;
    private SmoothColumn listScroll;
    private View firstRow;

    public NotifPanel(Activity a) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        density = a.getResources().getDisplayMetrics().density;
        timeFmt = new SimpleDateFormat("HH:mm", Txt.locale(a));

        List<NotifListener.Entry> entries = NotifListener.entries();

        card = new GlassPanel(a, density, 30);
        card.setClipChildren(false);
        card.setClipToPadding(false);

        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(26), dp(20), dp(26), dp(16));
        box.setClipChildren(false);
        box.setClipToPadding(false);
        card.addView(box, new FrameLayout.LayoutParams(MATCH, WRAP));

        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(a);
        title.setText(Txt.s(a, "Уведомления", "Notifications"));
        title.setTextSize(22);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView chip = new TextView(a);
        chip.setText(String.valueOf(entries.size()));
        chip.setTextSize(14);
        chip.setTextColor(0xD9FFFFFF);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(Styles.chip(density, 14, 0x1FFFFFFF));
        chip.setMinWidth(dp(32));
        chip.setPadding(dp(11), dp(4), dp(11), dp(4));
        head.addView(chip, new LinearLayout.LayoutParams(WRAP, WRAP));

        box.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        View divider = new View(a);
        divider.setBackgroundColor(0x1FFFFFFF);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(1)));
        dlp.topMargin = dp(14);
        box.addView(divider, dlp);

        listScroll = new SmoothColumn(a, dp(290));
        listScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listScroll.setClipChildren(true);
        listScroll.setClipToPadding(true);

        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setClipToPadding(false);
        list.setPadding(dp(10), dp(10), dp(10), dp(10));

        if (entries.isEmpty()) {
            TextView empty = new TextView(a);
            empty.setText(Txt.s(a, "Нет уведомлений", "No notifications"));
            empty.setTextSize(16);
            empty.setTextColor(0x66FFFFFF);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(52), 0, dp(52));
            list.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
        } else {
            for (NotifListener.Entry e : entries) {
                View row = buildRow(a, e);
                if (firstRow == null) firstRow = row;
                LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(MATCH, WRAP);
                rlp.topMargin = dp(4);
                list.addView(row, rlp);
            }
        }

        listScroll.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, WRAP);
        slp.topMargin = dp(4);
        box.addView(listScroll, slp);

        int clearable = 0;
        for (NotifListener.Entry e : entries) {
            if (e.clearable) clearable++;
        }
        if (clearable > 0) {
            LinearLayout clear = new LinearLayout(a);
            clear.setOrientation(LinearLayout.HORIZONTAL);
            clear.setGravity(Gravity.CENTER_VERTICAL);
            clear.setPadding(dp(18), dp(10), dp(18), dp(10));
            clear.setMinimumHeight(dp(46));
            clear.setFocusable(true);
            clear.setFocusableInTouchMode(true);
            clear.setClickable(true);
            clear.setBackground(Styles.row(density, 14));
            clear.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            clear.setClipChildren(false);
            clear.setClipToPadding(false);

            TextView label = new TextView(a);
            label.setText(Txt.s(a, "Очистить уведомления", "Clear notifications"));
            label.setTextSize(15);
            label.setTextColor(0xFFF5F5F7);
            clear.addView(label, new LinearLayout.LayoutParams(0, WRAP, 1f));

            TextView count = new TextView(a);
            count.setText(String.valueOf(clearable));
            count.setTextSize(12);
            count.setTextColor(0xCCFFFFFF);
            count.setGravity(Gravity.CENTER);
            count.setBackground(Styles.chip(density, 10, 0x1FFFFFFF));
            count.setPadding(dp(8), dp(2), dp(8), dp(2));
            count.setMinWidth(dp(28));
            clear.addView(count, new LinearLayout.LayoutParams(WRAP, WRAP));

            clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int n = NotifListener.clearVisible();
                    dismiss();
                    Toast.makeText(getContext(),
                            n > 0
                                    ? Txt.s(getContext(), "Очищено: ", "Cleared: ") + n
                                    : Txt.s(getContext(), "Нет уведомлений для очистки",
                                            "Nothing to clear"),
                            Toast.LENGTH_SHORT).show();
                }
            });

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(MATCH, WRAP);
            clp.topMargin = dp(8);
            box.addView(clear, clp);
        }

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — открыть      •      BACK — закрыть",
                "OK — open      •      BACK — close"));
        foot.setTextSize(12);
        foot.setTextColor(0x59FFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(12), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        setContentView(card, new ViewGroup.LayoutParams(dp(720), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.55f;
            lp.width = dp(720);
            lp.height = WRAP;
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.y = dp(26);
            w.setAttributes(lp);
        }
        MainActivity.installHomeKey(this, a);
    }

    private View buildRow(Activity a, final NotifListener.Entry e) {
        LinearLayout r = new LinearLayout(a);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(12), dp(10), dp(12), dp(10));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 18));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        r.setClipChildren(false);
        r.setClipToPadding(false);

        ImageView icon = new ImageView(a);
        Drawable d = null;
        try {
            d = a.getPackageManager().getApplicationIcon(e.pkg);
        } catch (Exception ignored) {
        }
        if (d == null) {
            try {
                Icon ic = e.large != null ? e.large : e.small;
                if (ic != null) d = ic.loadDrawable(a);
            } catch (Exception ignored) {
            }
        }
        if (d != null) icon.setImageDrawable(d);
        r.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(14), 0, dp(10), 0);

        String heading = TextUtils.isEmpty(e.title) ? e.app : e.title;
        TextView t = new TextView(a);
        t.setText(heading);
        t.setTextSize(16);
        t.setTextColor(0xFFF5F5F7);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        col.addView(t, new LinearLayout.LayoutParams(MATCH, WRAP));

        if (!TextUtils.isEmpty(e.text)) {
            TextView s = new TextView(a);
            s.setText(e.text);
            s.setTextSize(13);
            s.setTextColor(0x99FFFFFF);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            s.setPadding(0, dp(2), 0, 0);
            col.addView(s, new LinearLayout.LayoutParams(MATCH, WRAP));
        }
        r.addView(col, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView time = new TextView(a);
        time.setText(timeFmt.format(new Date(e.time)));
        time.setTextSize(12);
        time.setTextColor(0x66FFFFFF);
        time.setPadding(dp(8), 0, dp(4), 0);
        r.addView(time, new LinearLayout.LayoutParams(WRAP, WRAP));

        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (v.getContext() instanceof MainActivity) {
                    ((MainActivity) v.getContext()).playForward();
                }
                try {
                    Intent i = v.getContext().getPackageManager().getLaunchIntentForPackage(e.pkg);
                    if (i != null) {
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        v.getContext().startActivity(i);
                    }
                } catch (Exception ignored) {
                }
            }
        });

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && listScroll != null) listScroll.ensureVisible(v);
            }
        });

        r.setContentDescription((heading + " " + e.text).trim());
        return r;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        if (firstRow != null) {
            firstRow.post(new Runnable() {
                @Override
                public void run() {
                    firstRow.requestFocus();
                }
            });
        }
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(-dp(26));
            card.animate().alpha(1f).translationY(0f).setDuration(230)
                    .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

}
