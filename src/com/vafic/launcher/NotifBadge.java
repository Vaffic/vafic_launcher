package com.vafic.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Circular notification counter shown in the top status cluster. */
public class NotifBadge extends View {

    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    private int count;
    private float focus;
    private ValueAnimator anim;

    public NotifBadge(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        setFocusable(true);
        setClickable(true);

        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Math.max(1f, dp(1.2f)));
        ring.setColor(0x59FFFFFF);

        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                animateGlow(hasFocus ? 1f : 0f);
                // The circle is drawn with a margin, and the scale is small
                // enough to stay inside the view. Nothing can be clipped into
                // a square, not even by a parent that clips its children.
                v.animate().scaleX(hasFocus ? 1.05f : 1f).scaleY(hasFocus ? 1.05f : 1f)
                        .setDuration(140).start();
            }
        });
    }

    public void setCount(int n) {
        if (count == n) return;
        count = n;
        setContentDescription(Txt.s(getContext(), "Уведомления: ", "Notifications: ") + n);
        invalidate();
    }

    private float dp(float v) {
        return v * density;
    }

    private void animateGlow(float target) {
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(focus, target);
        anim.setDuration(140);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                focus = (float) a.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) / 2f - dp(1.8f);

        body.setColor(Color.argb((int) (64 + 50 * focus), 255, 255, 255));
        c.drawCircle(cx, cy, r, body);
        c.drawCircle(cx, cy, r, ring);

        String s = count > 99 ? "99+" : String.valueOf(count);
        text.setTextSize(dp(s.length() > 2 ? 12 : 15));
        float baseline = cy - (text.ascent() + text.descent()) / 2f;
        c.drawText(s, cx, baseline, text);
    }
}
