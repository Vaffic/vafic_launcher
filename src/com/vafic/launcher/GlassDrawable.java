package com.vafic.launcher;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Windows-Aero-style "glass" card surface.
 * FILL paints the frosted body, RIM paints the specular border and top gloss on top of artwork.
 *
 * The Aero recipe: a cool blue translucent body, a bright glossy band along
 * the top edge, a faint diagonal reflection and a border that is white-hot at
 * the top and deep blue at the bottom.
 *
 * Shaders and the clip path are cached per size/focus. Without this the library
 * allocated several gradients for every visible card on every scroll frame,
 * which made the TV drop to roughly 30 fps while the list moved.
 */
public class GlassDrawable extends Drawable {

    public static final int FILL = 0;
    public static final int RIM = 1;

    private final int mode;
    private final float density;
    private final float radiusDp;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint extra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private final Rect cacheBounds = new Rect();
    private final Rect clipBounds = new Rect();
    private Shader mainShader;
    private Shader accentShader;
    private Shader edgeShader;
    private float cacheFocus = -1f;
    private float focus = 0f;

    public GlassDrawable(float density, float radiusDp, int mode) {
        this.density = density;
        this.radiusDp = radiusDp;
        this.mode = mode;
        if (mode == RIM) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.35f * density);
        }
    }

    public void setFocus(float f) {
        if (Math.abs(focus - f) < 0.001f) return;
        focus = f;
        invalidateSelf();
    }

    public float getFocus() {
        return focus;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        float rad = radiusDp * density;

        if (!b.equals(cacheBounds) || focus != cacheFocus) {
            cacheBounds.set(b);
            cacheFocus = focus;
            rebuildShaders(b);
        }
        if (!b.equals(clipBounds)) {
            clipBounds.set(b);
            clip.reset();
            if (mode == FILL) {
                clip.addRoundRect(b.left, b.top, b.right, b.bottom, rad, rad, Path.Direction.CW);
            } else {
                float inset = 0.9f * density;
                float rimRad = Math.max(1f, rad - inset);
                clip.addRoundRect(b.left + inset, b.top + inset,
                        b.right - inset, b.bottom - inset, rimRad, rimRad, Path.Direction.CW);
            }
        }

        rect.set(b);
        paint.setShader(mainShader);
        extra.setShader(accentShader);
        edge.setShader(edgeShader);

        if (mode == FILL) {
            canvas.drawRoundRect(rect, rad, rad, paint);

            // The sheen and the top line are drawn as round rects of their
            // own. A clip path would make every card allocate an extra layer
            // on every frame, and the focus ring repaints the screen at once.
            canvas.drawRoundRect(rect, rad, rad, extra);
            canvas.drawRoundRect(rect, rad, rad, edge);
            return;
        }

        float inset = 0.9f * density;
        rect.inset(inset, inset);
        float rimRad = Math.max(1f, rad - inset);
        canvas.drawRoundRect(rect, rimRad, rimRad, paint);

        // The specular sweep rides on the rim itself, so it needs no clip.
        extra.setStyle(Paint.Style.STROKE);
        extra.setStrokeWidth(paint.getStrokeWidth());
        canvas.drawRoundRect(rect, rimRad, rimRad, extra);
        extra.setStyle(Paint.Style.FILL);
    }

    private void rebuildShaders(Rect b) {
        float w = b.width();
        float h = b.height();
        if (mode == FILL) {
            int aTop = (int) (44 + 30 * focus);
            int aMid = (int) (24 + 18 * focus);
            int aBot = (int) (14 + 12 * focus);
            mainShader = new LinearGradient(0, b.top, 0, b.bottom,
                    new int[]{(aTop << 24) | 0xC8E2F6, (aMid << 24) | 0x74A8D0,
                            (aBot << 24) | 0x2E4468},
                    new float[]{0f, 0.34f, 1f}, Shader.TileMode.CLAMP);

            // The Aero gloss: a bright, milky band that fades out downwards.
            int gloss = (int) (58 + 40 * focus) << 24 | 0xFFFFFF;
            accentShader = new LinearGradient(0, b.top, 0, b.top + h * 0.42f,
                    new int[]{gloss, 0x0AFFFFFF, 0x00FFFFFF},
                    new float[]{0f, 0.42f, 1f}, Shader.TileMode.CLAMP);

            // A narrow reflection streak across the left third of the pane.
            int streak = (int) (18 + 14 * focus) << 24 | 0xFFFFFF;
            edgeShader = new LinearGradient(b.left + w * 0.06f, b.top,
                    b.left + w * 0.42f, b.bottom,
                    new int[]{streak, 0x00FFFFFF},
                    new float[]{0f, 1f}, Shader.TileMode.CLAMP);
            return;
        }

        int rTop = (int) (232 + 20 * focus);
        int rMid = (int) (120 + 60 * focus);
        int rBot = (int) (56 + 40 * focus);
        mainShader = new LinearGradient(0, b.top, 0, b.bottom,
                new int[]{(rTop << 24) | 0xEAF6FF, (rMid << 24) | 0xA8D2F0,
                        (rBot << 24) | 0x4C86BC},
                new float[]{0f, 0.38f, 1f}, Shader.TileMode.CLAMP);

        int gloss = (int) (70 + 50 * focus) << 24 | 0xFFFFFF;
        accentShader = new RadialGradient(b.left + w * 0.28f, b.top,
                w * 0.72f, new int[]{gloss, 0x00FFFFFF},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    public void getOutline(Outline outline) {
        Rect b = getBounds();
        outline.setRoundRect(b.left, b.top, b.right, b.bottom, radiusDp * density);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        extra.setAlpha(alpha);
        edge.setAlpha(alpha);
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
