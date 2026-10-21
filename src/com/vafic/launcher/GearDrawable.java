package com.vafic.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Simple procedural gear used for the launcher settings tile. */
public class GearDrawable extends Drawable {

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public GearDrawable(int color, float density) {
        this.density = density;
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void draw(Canvas c) {
        float w = getBounds().width();
        float h = getBounds().height();
        float cx = getBounds().left + w / 2f;
        float cy = getBounds().top + h / 2f;
        float r = Math.min(w, h) * 0.34f;

        p.setStrokeWidth(Math.max(1f, 2.4f * density));
        c.drawCircle(cx, cy, r, p);
        c.drawCircle(cx, cy, r * 0.38f, p);

        p.setStrokeWidth(Math.max(1f, 3.2f * density));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            float x1 = (float) (cx + Math.cos(a) * r * 1.02f);
            float y1 = (float) (cy + Math.sin(a) * r * 1.02f);
            float x2 = (float) (cx + Math.cos(a) * r * 1.38f);
            float y2 = (float) (cy + Math.sin(a) * r * 1.38f);
            c.drawLine(x1, y1, x2, y2, p);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        p.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        p.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
