package com.vafic.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Focusable settings slider. A television remote has no touch bar, so the
 * value is changed with the left and right keys; the row shows a thin glass
 * track with a lit fill and a knob.
 */
public class SliderRow extends LinearLayout {

    public interface Listener {
        void onChanged(int value);
    }

    /** Converts a numeric slider position into a word, e.g. "Средний". */
    public interface Labeler {
        String label(int value);
    }

    private final float density;
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final TextView valueView;
    private final LevelBar bar;
    private int value;
    private Listener listener;
    private Labeler labeler;

    public SliderRow(Context c, float density, String label, int min, int max, int step,
                     int value, String suffix) {
        super(c);
        this.density = density;
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.suffix = suffix == null ? "" : suffix;

        setOrientation(VERTICAL);
        setPadding(dp(16), dp(8), dp(16), dp(10));
        setMinimumHeight(dp(58));
        setFocusable(true);
        setFocusableInTouchMode(false);
        setClickable(true);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);
        setClipChildren(false);
        setClipToPadding(false);
        setBackground(Styles.row(density, 16));

        LinearLayout line = new LinearLayout(c);
        line.setOrientation(HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = new TextView(c);
        name.setText(label);
        name.setTextSize(15.5f);
        name.setTextColor(0xFFF5F5F7);
        line.addView(name, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        valueView = new TextView(c);
        valueView.setTextSize(13);
        valueView.setTextColor(0x8AFFFFFF);
        valueView.setGravity(Gravity.END);
        line.addView(valueView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));

        addView(line, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        bar = new LevelBar(c);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                dp(8));
        blp.topMargin = dp(9);
        addView(bar, blp);

        setValue(value, false);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void setLabeler(Labeler l) {
        labeler = l;
        refreshValueText();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int v, boolean notify) {
        int clamped = Math.max(min, Math.min(max, v));
        // Keep the value on the step grid so a remote with key repeat cannot
        // drift into odd numbers after many presses.
        if ((clamped - min) % step != 0) {
            clamped = min + Math.round((clamped - min) / (float) step) * step;
            clamped = Math.max(min, Math.min(max, clamped));
        }
        boolean changed = clamped != value;
        value = clamped;
        refreshValueText();
        bar.setProgress((value - min) / (float) Math.max(1, max - min));
        if (notify && changed && listener != null) listener.onChanged(value);
    }

    private void refreshValueText() {
        valueView.setText(labeler != null ? labeler.label(value) : value + suffix);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            setValue(value - step, true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            setValue(value + step, true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private int dp(float v) {
        return Math.round(v * density);
    }

    /** Thin glass track with a filled part and a small knurled knob. */
    private class LevelBar extends View {

        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint knob = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float progress;

        LevelBar(Context c) {
            super(c);
            track.setColor(0x33FFFFFF);
            fill.setColor(0xFFA8D8F5);
            knob.setColor(0xFFF2FAFF);
        }

        void setProgress(float p) {
            progress = Math.max(0f, Math.min(1f, p));
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float h = getHeight();
            float r = h / 2f;
            rect.set(0, 0, getWidth(), h);
            canvas.drawRoundRect(rect, r, r, track);
            float w = getWidth() * progress;
            if (w > 0) {
                rect.set(0, 0, Math.max(w, h), h);
                canvas.drawRoundRect(rect, r, r, fill);
            }
            float cx = Math.max(r, Math.min(getWidth() - r, w));
            canvas.drawCircle(cx, h / 2f, Math.max(dp(3), h * 0.75f), knob);
        }
    }
}
