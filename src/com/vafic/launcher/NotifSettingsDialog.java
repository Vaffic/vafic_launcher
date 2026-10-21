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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Per-app notification filter. Shows launchable apps by default; a fixed switch
 * reveals every installed package, including system and hidden apps.
 */
public class NotifSettingsDialog extends Dialog {

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private static class Row {
        final String pkg;
        final TextView state;

        Row(String pkg, TextView state) {
            this.pkg = pkg;
            this.state = state;
        }
    }

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final LinearLayout list;
    private final TextView summary;
    private final SmoothColumn listScroll;
    private final LinearLayout toggleRow;
    private final TextView toggleValue;
    private final List<Row> rows = new ArrayList<>();

    private boolean showSystem;
    private boolean loading;
    private int loadToken;
    private View firstRow;

    public NotifSettingsDialog(Activity a) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
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
        title.setText(Txt.s(a, "Уведомления приложений", "App notifications"));
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
        sub.setText(Txt.s(a, "Скрыть уведомления из верхней строки лаунчера",
                "Hide notifications from the launcher status bar"));
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setPadding(0, dp(5), 0, dp(10));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        toggleRow = new LinearLayout(a);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);
        toggleRow.setPadding(dp(16), dp(8), dp(16), dp(8));
        toggleRow.setMinimumHeight(dp(52));
        toggleRow.setFocusable(true);
        toggleRow.setFocusableInTouchMode(true);
        toggleRow.setClickable(true);
        toggleRow.setBackground(Styles.row(density, 16));
        toggleRow.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        toggleRow.setClipChildren(false);
        toggleRow.setClipToPadding(false);

        TextView toggleName = new TextView(a);
        toggleName.setText(Txt.s(a, "Показать системные и скрытые",
                "Show system and hidden apps"));
        toggleName.setTextSize(15);
        toggleName.setTextColor(0xFFF5F5F7);
        toggleRow.addView(toggleName, new LinearLayout.LayoutParams(0, WRAP, 1f));

        toggleValue = new TextView(a);
        toggleValue.setTextSize(13);
        toggleValue.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        toggleValue.setPadding(dp(12), 0, 0, 0);
        toggleRow.addView(toggleValue, new LinearLayout.LayoutParams(WRAP, WRAP));
        box.addView(toggleRow, new LinearLayout.LayoutParams(MATCH, WRAP));

        toggleRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loading) return;
                showSystem = !showSystem;
                if (!showSystem) {
                    loadToken++;
                    loading = false;
                    buildList(AppRepo.notifApps(activity, false));
                    updateToggle();
                    scrollTop();
                    toggleRow.requestFocus();
                } else {
                    loadFullList();
                }
            }
        });

        listScroll = new SmoothColumn(a, dp(286));
        listScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        listScroll.setClipChildren(true);
        listScroll.setClipToPadding(true);

        list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        list.setClipToPadding(false);
        list.setPadding(dp(8), dp(8), dp(8), dp(8));
        listScroll.addView(list, new FrameLayout.LayoutParams(MATCH, WRAP));

        // The list lives in a frame that cuts it off. A scroll view clips its
        // content in the wrong coordinate space, so the rows would otherwise
        // be drawn over the title above the list.
        FrameLayout viewport = new FrameLayout(a);
        viewport.setClipChildren(true);
        viewport.setClipToPadding(true);
        viewport.addView(listScroll, new FrameLayout.LayoutParams(MATCH, WRAP));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(MATCH, WRAP);
        slp.topMargin = dp(8);
        box.addView(viewport, slp);

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "OK — скрыть или показывать      •      BACK — назад",
                "OK — hide or show      •      BACK — back"));
        foot.setTextSize(12);
        foot.setTextColor(0x4DFFFFFF);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(12), 0, 0);
        box.addView(foot, new LinearLayout.LayoutParams(MATCH, WRAP));

        buildList(AppRepo.notifApps(a, false));
        updateToggle();
        refreshSummary();

        setContentView(card, new ViewGroup.LayoutParams(dp(820), WRAP));

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.6f;
            lp.width = dp(820);
            lp.height = WRAP;
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            lp.y = dp(24);
            w.setAttributes(lp);
        }
        MainActivity.installHomeKey(this, a);
    }

    private void loadFullList() {
        loading = true;
        final int token = ++loadToken;
        updateToggle();

        list.removeAllViews();
        rows.clear();
        firstRow = null;

        TextView wait = new TextView(activity);
        wait.setText(Txt.s(activity, "Загрузка списка…", "Loading apps…"));
        wait.setTextSize(14);
        wait.setTextColor(0x80FFFFFF);
        wait.setGravity(Gravity.CENTER);
        wait.setPadding(0, dp(46), 0, dp(46));
        list.addView(wait, new LinearLayout.LayoutParams(MATCH, WRAP));

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppRepo.App> apps = AppRepo.notifApps(activity, true);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isShowing() || token != loadToken) return;
                        loading = false;
                        buildList(apps);
                        updateToggle();
                        scrollTop();
                        toggleRow.requestFocus();
                    }
                });
            }
        }, "vafic-notif-apps").start();
    }

    private void buildList(List<AppRepo.App> apps) {
        list.removeAllViews();
        rows.clear();
        firstRow = null;

        if (apps.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText(Txt.s(activity, "Нет приложений", "No apps"));
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
        }

        LinearLayout clear = buildClearRow();
        LinearLayout.LayoutParams clp = rowParams();
        clp.topMargin = dp(10);
        list.addView(clear, clp);
    }

    private View buildRow(AppRepo.App app) {
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

        LinearLayout names = new LinearLayout(activity);
        names.setOrientation(LinearLayout.VERTICAL);
        names.setPadding(dp(12), 0, dp(8), 0);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, WRAP, 1f);
        r.addView(names, nlp);

        TextView name = new TextView(activity);
        name.setText(app.label);
        name.setTextSize(15);
        name.setTextColor(0xFFF5F5F7);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        names.addView(name, new LinearLayout.LayoutParams(MATCH, WRAP));

        if (app.system) {
            TextView pkg = new TextView(activity);
            pkg.setText(app.pkg);
            pkg.setTextSize(9.5f);
            pkg.setTextColor(0x59FFFFFF);
            pkg.setSingleLine(true);
            pkg.setEllipsize(TextUtils.TruncateAt.END);
            pkg.setPadding(0, dp(2), 0, 0);
            names.addView(pkg, new LinearLayout.LayoutParams(MATCH, WRAP));
        }

        TextView state = new TextView(activity);
        state.setTextSize(13);
        state.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        r.addView(state, new LinearLayout.LayoutParams(WRAP, WRAP));

        final Row row = new Row(app.pkg, state);
        rows.add(row);
        updateState(row);

        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.toggleBlocked(activity, row.pkg);
                updateState(row);
                refreshSummary();
                NotifListener.reload();
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

    private LinearLayout buildClearRow() {
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
        name.setText(Txt.s(activity, "Показывать все уведомления", "Show all notifications"));
        name.setTextSize(14);
        name.setTextColor(0xB3FFFFFF);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        r.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.clearBlocked(activity);
                for (Row row : rows) updateState(row);
                refreshSummary();
                NotifListener.reload();
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

    private void updateState(Row row) {
        boolean hidden = Prefs.blocked(activity, row.pkg);
        row.state.setText(Txt.s(activity, hidden ? "Скрыто" : "Вкл",
                hidden ? "Hidden" : "On"));
        row.state.setTextColor(hidden ? 0x59FFFFFF : 0xFF8FE3A4);
    }

    private void updateToggle() {
        if (loading) {
            toggleValue.setText(Txt.s(activity, "Загрузка…", "Loading…"));
            toggleValue.setTextColor(0x80FFFFFF);
            return;
        }
        toggleValue.setText(Txt.s(activity, showSystem ? "Вкл" : "Выкл",
                showSystem ? "On" : "Off"));
        toggleValue.setTextColor(showSystem ? 0xFF8FE3A4 : 0x8AFFFFFF);
    }

    private void refreshSummary() {
        Set<String> blocked = Prefs.blocked(activity);
        int n = blocked.size();
        summary.setText(n == 0
                ? Txt.s(activity, "Все включены", "All on")
                : Txt.s(activity, "Скрыто: ", "Hidden: ") + n);
        summary.setTextColor(n == 0 ? 0x80FFFFFF : 0xFFFFD9A0);
    }

    private void scrollTop() {
        listScroll.post(new Runnable() {
            @Override
            public void run() {
                listScroll.scrollTo(0, 0);
            }
        });
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
        final View target = firstRow != null ? firstRow : toggleRow;
        target.post(new Runnable() {
            @Override
            public void run() {
                target.requestFocus();
                if (firstRow != null) listScroll.ensureVisible(firstRow);
            }
        });
        card.setAlpha(0f);
        card.setTranslationY(dp(18));
        card.animate().alpha(1f).translationY(0f).setDuration(230)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
}
