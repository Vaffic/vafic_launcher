package com.vafic.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * Vertical list that glides just enough to keep the focused card visible.
 * Used by the app library and the notification filter.
 *
 * The position follows the target with a critically damped spring rather than
 * restarting an OverScroller on every key press. Rapid D-pad navigation keeps
 * its velocity, so the list does not stutter or stop between rows.
 */
public class SmoothColumn extends ScrollView {

    private static final float OMEGA = 17.5f;

    public interface OnSettledListener {
        void onSettled();
    }

    private final int maxHeight;
    private int targetY;
    private float velocity;
    private boolean running;
    private long lastFrame;
    private boolean dpadFocusOnly;
    private OnSettledListener settledListener;

    public SmoothColumn(Context c) {
        this(c, 0);
    }

    public SmoothColumn(Context c, int maxHeight) {
        super(c);
        this.maxHeight = maxHeight;
        setSmoothScrollingEnabled(false);
        setVerticalScrollBarEnabled(false);
        setClipChildren(false);
        setClipToPadding(false);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (maxHeight > 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        // Focus-driven scrolling is handled by ensureVisible(), which receives the actual
        // focused card and can therefore keep a comfortable margin on both sides.
        return true;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        // ScrollView.requestChildFocus() immediately calls scrollBy() to reveal the
        // newly focused child. During fast D-pad navigation that instant jump fights
        // our spring (which is already moving towards the next page), producing the
        // torn / teleporting scroll. Suppress the built-in reveal for this call: the
        // spring is the only thing that is allowed to move the list.
        if (focused != null) {
            boolean reveal = focused.getRevealOnFocusHint();
            if (reveal) focused.setRevealOnFocusHint(false);
            try {
                super.requestChildFocus(child, focused);
            } finally {
                if (reveal) focused.setRevealOnFocusHint(true);
            }
            return;
        }
        super.requestChildFocus(child, focused);
    }

    @Override
    public boolean executeKeyEvent(KeyEvent event) {
        if (!dpadFocusOnly) return super.executeKeyEvent(event);
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        int direction;
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            direction = View.FOCUS_UP;
        } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            direction = View.FOCUS_DOWN;
        } else {
            return super.executeKeyEvent(event);
        }
        // The library handles D-pad movement itself. ScrollView's fallback
        // arrow scrolling would otherwise nudge the list by a few pixels on
        // every key repeat at the first/last row; the spring then pulls it back
        // and the whole cursor appears to bounce up and down.
        View current = findFocus();
        if (current == null) return true;
        View next = FocusFinder.getInstance().findNextFocus(this, current, direction);
        if (next != null && next != current && isInside(next)) {
            next.requestFocus(direction);
        }
        return true;
    }

    /** True when v is this column or one of its descendants. */
    private boolean isInside(View v) {
        View p = v;
        while (p != null) {
            if (p == this) return true;
            if (!(p.getParent() instanceof View)) return false;
            p = (View) p.getParent();
        }
        return false;
    }

    /** Use explicit focus search for the library instead of ScrollView's arrow scroll. */
    public void setDpadFocusOnly(boolean on) {
        dpadFocusOnly = on;
    }

    public void setOnSettledListener(OnSettledListener l) {
        settledListener = l;
    }

    /** The clamped content offset the spring is currently heading to. */
    public int getTargetY() {
        return targetY;
    }

    public boolean isGliding() {
        return running;
    }

    /** Glides the minimum distance needed to show the card with a soft margin. */
    public void ensureVisible(View v) {
        if (v == null) return;
        int viewHeight = getHeight();
        if (viewHeight <= 0) return;

        int[] loc = new int[2];
        int[] self = new int[2];
        v.getLocationOnScreen(loc);
        getLocationOnScreen(self);

        int current = getScrollY();
        int top = loc[1] - self[1] + current;
        int bottom = top + v.getHeight();
        int margin = Math.round(viewHeight * 0.10f);
        int target = current;

        if (top - margin < current) {
            target = top - margin;
        } else if (bottom + margin > current + viewHeight) {
            target = bottom + margin - viewHeight;
        }
        glideTo(target);
    }

    /**
     * Stops the spring and puts the list at an exact offset right away.
     * Used when a page has to open at the top instead of where it was left.
     */
    public void jumpTo(int y) {
        removeCallbacks(step);
        running = false;
        velocity = 0f;
        int max = Math.max(0, computeVerticalScrollRange() - getHeight());
        targetY = Math.max(0, Math.min(y, max));
        scrollTo(0, targetY);
    }

    /**
     * Glides to an exact content offset. The library uses this to move in whole
     * rows: the spring keeps its velocity if the user keeps pressing, but every
     * stop is aligned to a page boundary instead of an arbitrary pixel offset.
     */
    public void glideTo(int y) {
        int viewHeight = getHeight();
        if (viewHeight <= 0) return;
        int max = Math.max(0, computeVerticalScrollRange() - viewHeight);
        targetY = Math.max(0, Math.min(y, max));
        if (!running) {
            int x = getScrollY();
            if (Math.abs(targetY - x) <= 1) {
                if (x != targetY) scrollTo(0, targetY);
                return;
            }
            running = true;
            lastFrame = SystemClock.uptimeMillis();
            postOnAnimation(step);
        }
    }

    private final Runnable step = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            float dt = (now - lastFrame) / 1000f;
            lastFrame = now;
            // A dropped or inflated frame must not make the spring explode.
            dt = Math.max(0.001f, Math.min(dt, 0.032f));

            int max = Math.max(0, computeVerticalScrollRange() - getHeight());
            targetY = Math.max(0, Math.min(targetY, max));

            float x = getScrollY();
            float diff = targetY - x;
            // getScrollY() is an integer, so a spring can stall one or two pixels
            // short of the target when its remaining velocity is just under the
            // amount needed to round up. Snap that last sliver instead of
            // posting animation frames forever.
            if (Math.abs(diff) <= 2f || (Math.abs(diff) < 0.7f && Math.abs(velocity) < 10f)) {
                if (x != targetY) scrollTo(0, targetY);
                velocity = 0f;
                running = false;
                if (settledListener != null) settledListener.onSettled();
                return;
            }

            velocity += (OMEGA * OMEGA * diff - 2f * OMEGA * velocity) * dt;
            float next = x + velocity * dt;
            if (next < 0f) {
                next = 0f;
                if (velocity < 0f) velocity = 0f;
            } else if (next > max) {
                next = max;
                if (velocity > 0f) velocity = 0f;
            }

            scrollTo(0, Math.round(next));
            postOnAnimation(this);
        }
    };

    @Override
    public void fling(int velocityY) {
        // the focus decides where the list stops
    }
}
