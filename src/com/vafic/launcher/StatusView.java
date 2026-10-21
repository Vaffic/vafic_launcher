package com.vafic.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Status icon: Wi-Fi, Ethernet, offline or a settings gear, plus a focus glow. */
public class StatusView extends View {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float density;
    private final Drawable ethernetIcon;
    private final boolean gear;
    private int mode = NetInfo.OFFLINE;
    private int level = 3;
    private float glow;
    private ValueAnimator anim;

    public StatusView(Context c) {
        this(c, false);
    }

    public StatusView(Context c, boolean gear) {
        super(c);
        this.gear = gear;
        density = c.getResources().getDisplayMetrics().density;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Math.max(1f, dp(1.2f)));
        setFocusable(true);
        setClickable(true);
        // The standard Android Ethernet symbol.
        ethernetIcon = c.getResources().getDrawable(R.drawable.ic_ethernet);
        if (gear) {
            setContentDescription(Txt.s(c, "Настройки лаунчера", "Launcher settings"));
        }
        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                glowTo(hasFocus ? 1f : 0f);
                // The glow is drawn well inside the view and the scale stays
                // below the margin, so the circle is never cut into a square.
                v.animate().scaleX(hasFocus ? 1.05f : 1f).scaleY(hasFocus ? 1.05f : 1f)
                        .setDuration(140).start();
            }
        });
    }

    public void refresh() {
        if (gear) return;
        NetInfo.State s = NetInfo.read(getContext());
        mode = s.mode;
        level = s.level;
        String label = s.mode == NetInfo.OFFLINE
                ? Txt.s(getContext(), "Нет сети", "No network") : s.label;
        setContentDescription(label + (s.mode == NetInfo.WIFI ? ", " + s.percent() + "%" : ""));
        invalidate();
    }

    private float dp(float v) {
        return v * density;
    }

    private void glowTo(float target) {
        if (anim != null) anim.cancel();
        anim = ValueAnimator.ofFloat(glow, target);
        anim.setDuration(140);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                glow = (float) a.getAnimatedValue();
                invalidate();
            }
        });
        anim.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // The platform symbol keeps its own proportions.
        float size = Math.min(w, h) * 0.60f;
        float ratio = 48f / 48f;
        float iw = size * ratio;
        float ih = size;
        ethernetIcon.setBounds(Math.round((w - iw) / 2f), Math.round((h - ih) / 2f),
                Math.round((w + iw) / 2f), Math.round((h + ih) / 2f));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - dp(1.8f);

        if (glow > 0.01f) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb((int) (44 * glow), 255, 255, 255));
            canvas.drawCircle(cx, cy, radius, p);
            ring.setColor(Color.argb((int) (105 * glow), 255, 255, 255));
            canvas.drawCircle(cx, cy, radius, ring);
        }

        float icon = Math.min(w, h) * 0.60f;
        int alpha = 0xCC + (int) (0x33 * glow);
        p.setColor(Color.argb(alpha, 255, 255, 255));

        if (gear) {
            float r = icon * 0.28f;
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(1f, dp(2.2f)));
            canvas.drawCircle(cx, cy, r, p);
            canvas.drawCircle(cx, cy, r * 0.38f, p);
            p.setStrokeWidth(Math.max(1f, dp(2.8f)));
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8;
                float x1 = (float) (cx + Math.cos(a) * r * 1.02f);
                float y1 = (float) (cy + Math.sin(a) * r * 1.02f);
                float x2 = (float) (cx + Math.cos(a) * r * 1.38f);
                float y2 = (float) (cy + Math.sin(a) * r * 1.38f);
                canvas.drawLine(x1, y1, x2, y2, p);
            }
            return;
        }

        if (mode == NetInfo.WIFI) {
            float baseY = cy + icon * 0.30f;
            p.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, baseY, icon * 0.075f, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(icon * 0.075f);
            float[] radii = {icon * 0.26f, icon * 0.44f, icon * 0.62f};
            for (int i = 0; i < 3; i++) {
                boolean on = i < level;
                p.setColor(Color.argb(on ? alpha : (int) (alpha * 0.25f), 255, 255, 255));
                oval.set(cx - radii[i], baseY - radii[i], cx + radii[i], baseY + radii[i]);
                canvas.drawArc(oval, 222f, 96f, false, p);
            }
        } else if (mode == NetInfo.ETHERNET) {
            // The standard Android Ethernet symbol, the same vector the system
            // status bar uses, without the small mark on the right.
            ethernetIcon.draw(canvas);
        } else {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(icon * 0.085f);
            float bw = icon * 0.58f;
            float left = cx - bw / 2f;
            float top = cy - bw / 2f;
            oval.set(left, top, left + bw, top + bw);
            canvas.drawRoundRect(oval, icon * 0.16f, icon * 0.16f, p);
            p.setStyle(Paint.Style.FILL);
            canvas.drawRect(left + bw * 0.28f, cy - icon * 0.03f,
                    left + bw * 0.72f, cy + icon * 0.03f, p);
        }
    }
}
