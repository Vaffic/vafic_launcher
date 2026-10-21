package com.vafic.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Three-by-three dots glyph used for the "all apps" tile. */
public class DotsDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int color;
    private final float cell;

    public DotsDrawable(int color, float sizePx) {
        this.color = color;
        this.cell = sizePx / 9f;
        paint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        RectF r = new RectF();
        float s = cell * 0.62f;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                float cx = cell + x * cell * 2.6f;
                float cy = cell + y * cell * 2.6f;
                r.set(cx, cy, cx + s, cy + s);
                canvas.drawRoundRect(r, s * 0.32f, s * 0.32f, paint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
