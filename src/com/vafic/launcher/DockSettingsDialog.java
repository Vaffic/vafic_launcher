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

/**
 * Dock section: the user picks how many cards are visible at once and whether
 * the app names are shown below them. The dock itself may hold more apps than
 * fit: the extra cards are reached by scrolling left and right, so changing the
 * size never removes or limits anything on the dock.
 */
public class DockSettingsDialog extends VaficDialog {

    public interface Listener {
        void onDockChanged(int count);

        void onDockLabelsChanged(boolean shown);
    }

    /** Slider levels from the smallest card (level 0, 8 apps) to the largest. */
    private static final int LEVELS = Prefs.MAX_DOCK - Prefs.MIN_DOCK;

    private static final String[] SIZE_RU = {
            "Минимальный", "Малый", "Компактный", "Средний", "Крупный", "Максимальный"
    };
    private static final String[] SIZE_EN = {
            "Minimum", "Small", "Compact", "Medium", "Large", "Maximum"
    };

    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    private static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private final Activity activity;
    private final float density;
    private final FrameLayout card;
    private final View firstRow;
    private final TextView labelsValue;
    private final TextView note;

    public DockSettingsDialog(Activity a, final Listener listener) {
        super(a);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        activity = a;
        density = a.getResources().getDisplayMetrics().density;

        int level = Math.max(0, Math.min(LEVELS, levelFor(a)));

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
        title.setText(Txt.s(a, "Док", "Dock"));
        title.setTextSize(23);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        box.addView(title, new LinearLayout.LayoutParams(MATCH, WRAP));

        TextView sub = new TextView(a);
        sub.setText(Txt.s(a, "Сколько карточек видно и подписи внизу экрана",
                "How many cards are visible, plus captions"));
        sub.setTextSize(12);
        sub.setTextColor(0x59FFFFFF);
        sub.setPadding(0, dp(2), 0, dp(12));
        box.addView(sub, new LinearLayout.LayoutParams(MATCH, WRAP));

        final SliderRow size = new SliderRow(a, density,
                Txt.s(a, "Видно карточек", "Cards on screen"),
                0, LEVELS, 1, level, "");
        size.setLabeler(new SliderRow.Labeler() {
            @Override
            public String label(int value) {
                return sizeName(activity, value);
            }
        });
        size.setListener(new SliderRow.Listener() {
            @Override
            public void onChanged(int value) {
                int capacity = Prefs.MAX_DOCK - value;
                Prefs.setDockSize(activity, capacity);
                listener.onDockChanged(capacity);
                updateNote(value);
            }
        });
        firstRow = size;
        box.addView(size, new LinearLayout.LayoutParams(MATCH, WRAP));

        LinearLayout labels = row(Txt.s(a, "Названия приложений", "App names"));
        labelsValue = (TextView) labels.getChildAt(1);
        labels.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean shown = !Prefs.dockLabels(activity);
                Prefs.setDockLabels(activity, shown);
                updateLabelsValue(shown);
                listener.onDockLabelsChanged(shown);
            }
        });
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(MATCH, WRAP);
        llp.topMargin = dp(10);
        box.addView(labels, llp);
        updateLabelsValue(Prefs.dockLabels(a));

        note = new TextView(a);
        note.setTextSize(12);
        note.setTextColor(0x66FFFFFF);
        note.setLineSpacing(dp(2), 1f);
        note.setPadding(dp(4), dp(14), dp(4), 0);
        box.addView(note, new LinearLayout.LayoutParams(MATCH, WRAP));
        updateNote(level);

        TextView foot = new TextView(a);
        foot.setText(Txt.s(a, "\u2190 \u2192 — видно карточек      •      OK — названия      •      BACK — назад",
                "\u2190 \u2192 — cards on screen      •      OK — names      •      BACK — back"));
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

    /** Current slider level for the saved dock: 0 = smallest, 5 = largest. */
    static int levelFor(Context c) {
        return Math.max(0, Math.min(LEVELS, Prefs.MAX_DOCK - Prefs.dockSize(c)));
    }

    static String sizeName(Context c, int level) {
        int i = Math.max(0, Math.min(LEVELS, level));
        return Txt.russian(c) ? SIZE_RU[i] : SIZE_EN[i];
    }

    private void updateNote(int level) {
        if (note == null) return;
        int capacity = Prefs.MAX_DOCK - level;
        int total = Prefs.home(activity).size();
        if (total > capacity) {
            note.setText(Txt.s(activity,
                    "На экране " + apps(capacity) + ", всего " + total
                            + ". Листайте док влево и вправо \u2014 крайние карточки уходят,"
                            + " следующие приходят.",
                    capacity + " apps on screen, " + total
                            + " in the dock. Scroll left and right: the edge cards slide out"
                            + " and the next ones slide in."));
        } else {
            note.setText(Txt.s(activity,
                    "На экране " + apps(capacity) + ". Все приложения дока видны сразу.",
                    capacity + " apps on screen. Every dock app is visible at once."));
        }
    }

    private String apps(int n) {
        if (!Txt.russian(activity)) return n + (n == 1 ? " app" : " apps");
        int mod10 = n % 10;
        int mod100 = n % 100;
        if (mod10 == 1 && mod100 != 11) return n + " приложение";
        if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
            return n + " приложения";
        }
        return n + " приложений";
    }

    /** A focusable settings row with a value on the right. */
    private LinearLayout row(String label) {
        LinearLayout r = new LinearLayout(activity);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(7), dp(16), dp(7));
        r.setMinimumHeight(dp(46));
        r.setFocusable(true);
        r.setFocusableInTouchMode(false);
        r.setClickable(true);
        r.setBackground(Styles.row(density, 16));
        r.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        TextView name = new TextView(activity);
        name.setText(label);
        name.setTextSize(15.5f);
        name.setTextColor(0xFFF5F5F7);
        r.addView(name, new LinearLayout.LayoutParams(0, WRAP, 1f));

        TextView value = new TextView(activity);
        value.setTextSize(13);
        value.setPadding(dp(12), 0, 0, 0);
        r.addView(value, new LinearLayout.LayoutParams(WRAP, WRAP));
        return r;
    }

    private void updateLabelsValue(boolean shown) {
        if (labelsValue == null) return;
        labelsValue.setText(Txt.s(activity, shown ? "Вкл" : "Выкл",
                shown ? "On" : "Off"));
        labelsValue.setTextColor(shown ? 0xFF8FE3A4 : 0x8AFFFFFF);
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
