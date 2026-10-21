package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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

import java.util.List;

/**
 * The apps hidden from the "All apps" library. Every row brings its app back;
 * the list is the only place where a hidden app can be restored.
 */
public class HiddenAppsDialog extends Dialog {

    public interface Listener {
        void onChanged();
    }

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final LinearLayout list;
    private final SmoothColumn listScroll;
    private final TextView summary;
    private final Listener listener;
    private View firstRow;

    public HiddenAppsDialog(Activity a, final Listener listener) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        this.listener = listener;
        density = a.getResources().getDisplayMetrics().density;

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
        title.setText(Txt.s(a, "Скрытые приложения", "Hidden apps"));
        title.setTextSize(22);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1f));

        summary = new TextView(a);
        summary.setTextSize(12);
        summary.setTextColor(0x8AFFFFFF);
        summary.setGravity(Gravity.CENTER);
        summary.setBackground(Styles.chip(density, 12, 0x1FFFFFFF));
        summary.setPadding(dp(12), dp(5), dp(12), dp(5));
        head.addView(summary, new LinearLayout.LayoutParams(WRAP, WRAP));
        box.addView(head, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText(Txt.s(a, "Этих приложений нет в списке «Все приложения»",
                "These apps are not listed under All apps"));
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setPadding(0, dp(5), 0, dp(10));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        listScroll = new SmoothColumn(a, dp(300));
        listScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listScroll.setClipChildren(true);
        listScroll.setClipToPadding(true);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, WRAP);
        slp.topMargin = dp(4);
        box.addView(listScroll, slp);

        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setClipToPadding(false);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        listScroll.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP));

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — показать      •      BACK — закрыть",
                "OK — restore      •      BACK — close"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(12), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        rebuild();

        setContentView(card, new ViewGroup.LayoutParams(dp(640), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.6f;
            lp.width = dp(640);
            lp.height = WRAP;
            lp.gravity = Gravity.CENTER;
            w.setAttributes(lp);
        }
        MainActivity.installHomeKey(this, a);
    }

    private void rebuild() {
        list.removeAllViews();
        firstRow = null;

        List<AppRepo.App> apps = AppRepo.hiddenApps(activity);
        if (apps.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText(Txt.s(activity, "Скрытых приложений нет", "No hidden apps"));
            empty.setTextColor(0x66FFFFFF);
            empty.setTextSize(15);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            list.addView(empty, new LinearLayout.LayoutParams(MATCH, WRAP));
        } else {
            for (AppRepo.App app : apps) {
                View row = buildRow(app);
                if (firstRow == null) firstRow = row;
                list.addView(row, rowParams());
            }
            View all = buildShowAllRow();
            LinearLayout.LayoutParams alp = rowParams();
            alp.topMargin = dp(10);
            list.addView(all, alp);
        }

        int n = apps.size();
        summary.setText(n == 0
                ? Txt.s(activity, "Нет", "None")
                : Txt.s(activity, "Скрыто: ", "Hidden: ") + n);
        summary.setTextColor(n == 0 ? 0x80FFFFFF : 0xFFFFD9A0);
    }

    private View buildRow(final AppRepo.App app) {
        LinearLayout r = new LinearLayout(activity);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), dp(8), dp(16), dp(8));
        r.setMinimumHeight(dp(54));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        r.setClipChildren(false);
        r.setClipToPadding(false);

        ImageView icon = new ImageView(activity);
        if (app.icon != null) icon.setImageDrawable(app.icon);
        r.addView(icon, new LinearLayout.LayoutParams(dp(32), dp(32)));

        TextView name = new TextView(activity);
        name.setText(app.label);
        name.setTextSize(15);
        name.setTextColor(0xFFF5F5F7);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(dp(12), 0, dp(8), 0);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView state = new TextView(activity);
        state.setText(Txt.s(activity, "Показать  \u203A", "Restore  \u203A"));
        state.setTextSize(13);
        state.setTextColor(0xFF8FE3A4);
        r.addView(state, new LinearLayout.LayoutParams(WRAP, WRAP));

        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.setHidden(activity, app.pkg, false);
                if (listener != null) listener.onChanged();
                rebuild();
                if (firstRow != null) firstRow.requestFocus();
            }
        });

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) listScroll.ensureVisible(v);
            }
        });

        r.setContentDescription(app.label);
        return r;
    }

    private View buildShowAllRow() {
        LinearLayout r = new LinearLayout(activity);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(14), dp(8), dp(16), dp(8));
        r.setMinimumHeight(dp(50));
        r.setFocusable(true);
        r.setFocusableInTouchMode(true);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        r.setClipChildren(false);
        r.setClipToPadding(false);

        TextView name = new TextView(activity);
        name.setText(Txt.s(activity, "Показать все", "Restore all"));
        name.setTextSize(14);
        name.setTextColor(0xB3FFFFFF);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.clearHidden(activity);
                if (listener != null) listener.onChanged();
                rebuild();
            }
        });

        r.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) listScroll.ensureVisible(v);
            }
        });
        return r;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.topMargin = dp(6);
        return p;
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    @Override
    public void show() {
        super.show();
        final View target = firstRow;
        if (target != null) {
            target.post(new Runnable() {
                @Override
                public void run() {
                    target.requestFocus();
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
