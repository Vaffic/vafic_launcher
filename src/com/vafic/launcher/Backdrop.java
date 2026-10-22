package com.vafic.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * Procedural tvOS-style wallpaper: bold colour fields softened into a dark base.
 * Rendered once into a half-resolution bitmap, so the launcher stays light on memory.
 * When the user picks a photo, it is drawn instead; both the photo and the
 * generated wallpaper are darkened by the chosen amount, and the generated
 * wallpaper comes back as soon as the photo is removed.
 */
public class Backdrop extends View {

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect dst = new Rect();
    private final RectF photoDst = new RectF();
    private Bitmap cached;
    private Bitmap custom;
    private int dimPercent = 35;

    public Backdrop(Context c) {
        super(c);
    }

    /** Replaces the generated backdrop with a personal photo; null restores it. */
    public void setCustom(Bitmap b) {
        if (custom != null && custom != b) custom.recycle();
        custom = b;
        if (b == null) rebuild();
        invalidate();
    }

    /** How much the wallpaper is darkened, 0..80 percent. */
    public void setDim(int percent) {
        dimPercent = Math.max(0, Math.min(80, percent));
        invalidate();
    }

    private void rebuild() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;
        if (cached != null) cached.recycle();
        cached = Bitmap.createBitmap(Math.max(1, width / 2), Math.max(1, height / 2),
                Bitmap.Config.ARGB_8888);
        render(cached);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);
        if (width <= 0 || height <= 0) return;
        if (custom == null) {
            rebuild();
        } else if (cached != null) {
            // The photo covers the whole view; the generated bitmap is not needed.
            cached.recycle();
            cached = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (custom != null && !custom.isRecycled() && custom.getWidth() > 0
                && custom.getHeight() > 0) {
            float scale = Math.max(getWidth() / (float) custom.getWidth(),
                    getHeight() / (float) custom.getHeight());
            float w = custom.getWidth() * scale;
            float h = custom.getHeight() * scale;
            float left = (getWidth() - w) / 2f;
            float top = (getHeight() - h) / 2f;
            photoDst.set(left, top, left + w, top + h);
            canvas.drawBitmap(custom, null, photoDst, bitmapPaint);
        } else {
            if (cached == null) return;
            dst.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(cached, null, dst, bitmapPaint);
        }
        // The dim is a property of the wallpaper as a whole, so it applies to
        // the generated backdrop as well as to a user photo.
        int alpha = Math.round(dimPercent / 100f * 255f);
        if (alpha > 0) canvas.drawColor(alpha << 24);
    }

    private void render(Bitmap bmp) {
        Canvas c = new Canvas(bmp);
        float w = bmp.getWidth();
        float h = bmp.getHeight();

        Paint base = new Paint();
        base.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{0xFF102A46, 0xFF0B1B30, 0xFF050A12},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, base);

        blob(c, w * 0.10f, h * 0.08f, w * 0.50f, 0x7FD4FF, 120);
        blob(c, w * 0.52f, -h * 0.04f, w * 0.42f, 0xA8E0FF, 110);
        blob(c, w * 0.90f, h * 0.14f, w * 0.36f, 0xC8E8FF, 95);
        blob(c, w * 1.06f, h * 0.60f, w * 0.46f, 0x2A6AC0, 115);
        blob(c, w * 0.04f, h * 0.90f, w * 0.44f, 0x1E7AC0, 105);
        blob(c, w * 0.46f, h * 1.06f, w * 0.40f, 0x2AB0D0, 90);
        blob(c, w * 0.64f, h * 0.50f, w * 0.24f, 0xE8F4FF, 38);

        // Aero streaks: a few wide, very soft diagonal bands of light.
        Paint streak = new Paint(Paint.ANTI_ALIAS_FLAG);
        streak.setShader(new LinearGradient(0, 0, w * 0.6f, h,
                new int[]{0x14FFFFFF, 0x06FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.save();
        c.rotate(-18f, w * 0.5f, h * 0.5f);
        c.drawRect(-w * 0.2f, h * 0.06f, w * 1.2f, h * 0.22f, streak);
        c.drawRect(-w * 0.2f, h * 0.40f, w * 1.2f, h * 0.48f, streak);
        c.restore();

        // The overhead light every Aero surface reflects.
        Paint light = new Paint(Paint.ANTI_ALIAS_FLAG);
        light.setShader(new RadialGradient(w * 0.5f, -h * 0.12f, w * 0.85f,
                new int[]{0x2ED8EEFF, 0x0AFFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, light);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1f, w * 0.0016f));
        line.setColor(0x12FFFFFF);
        c.drawCircle(w * 0.30f, h * 0.62f, w * 0.24f, line);
        c.drawCircle(w * 0.34f, h * 0.66f, w * 0.31f, line);
        c.drawCircle(w * 0.38f, h * 0.70f, w * 0.39f, line);

        // scrims keep the clock, badge and dock readable
        Paint scrim = new Paint();
        scrim.setShader(new LinearGradient(0, 0, 0, h * 0.30f,
                new int[]{0x66000000, 0x00000000}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h * 0.30f, scrim);

        scrim.setShader(new LinearGradient(0, h * 0.60f, 0, h,
                new int[]{0x00000000, 0x73000000}, null, Shader.TileMode.CLAMP));
        c.drawRect(0, h * 0.60f, w, h, scrim);

        Paint vig = new Paint(Paint.ANTI_ALIAS_FLAG);
        vig.setShader(new RadialGradient(w * 0.5f, h * 0.5f, w * 0.78f,
                new int[]{0x00000000, 0x00000000, 0x59000000},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, vig);
    }

    private void blob(Canvas c, float cx, float cy, float r, int rgb, int alpha) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int solid = (Math.min(255, alpha) << 24) | (rgb & 0xFFFFFF);
        int soft = ((alpha * 55 / 100) << 24) | (rgb & 0xFFFFFF);
        p.setShader(new RadialGradient(cx, cy, r,
                new int[]{solid, soft, 0x00000000},
                new float[]{0f, 0.52f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(cx - r, cy - r, cx + r, cy + r, p);
    }
}
