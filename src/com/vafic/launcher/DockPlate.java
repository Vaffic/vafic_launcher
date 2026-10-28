package com.vafic.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.widget.FrameLayout;

/**
 * The rounded glass plate of the dock. The cards live in a sliding row inside
 * it; during a slide the row is masked with a horizontal alpha ramp at both
 * ends of the content window. Without the ramp, a card that is halfway over
 * the plate edge is cut with a perfectly straight line, which reads as a
 * bright rectangle standing on the glass.
 *
 * Only the children are masked: the plate background is painted before
 * dispatchDraw, so the glass margin and its rim stay crisp.
 */
public class DockPlate extends FrameLayout {

    /** Strength of the edge mask, 0 = off, 1 = full. */
    private float edgeFade;
    private float fadeWidth;
    private int shaderLeft = -1;
    private int shaderRight = -1;
    private float shaderFade = -1f;

    private final Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PorterDuffXfermode cutOut =
            new PorterDuffXfermode(PorterDuff.Mode.DST_IN);
    private LinearGradient shader;

    public DockPlate(Context c) {
        super(c);
        mask.setXfermode(cutOut);
    }

    /** Width of the soft edge inside the content window, in pixels. */
    public void setFadeWidth(float px) {
        if (Math.abs(px - fadeWidth) < 0.5f) return;
        fadeWidth = px;
        shader = null;
        invalidate();
    }

    /** 0 disables the mask; 1 applies the full-width edge ramp. */
    public void setEdgeFade(float value) {
        value = Math.max(0f, Math.min(1f, value));
        if (Math.abs(value - edgeFade) < 0.01f) return;
        edgeFade = value;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (edgeFade <= 0.01f) {
            super.dispatchDraw(canvas);
            return;
        }

        // The mask starts at the rounded plate frame, not at an inner content
        // window: the glass margin is part of the scroll viewport.
        int left = 0;
        int right = getWidth();
        int width = right - left;
        if (width <= 2) {
            super.dispatchDraw(canvas);
            return;
        }
        float fade = Math.max(0f, Math.min(fadeWidth * edgeFade, width / 2f - 1f));
        if (fade <= 0.5f) {
            super.dispatchDraw(canvas);
            return;
        }
        ensureShader(left, right, width, fade);

        int save = canvas.saveLayer(left, 0f, right, getHeight(), null);
        super.dispatchDraw(canvas);
        mask.setShader(shader);
        mask.setAlpha(255);
        canvas.drawRect(left, 0f, right, getHeight(), mask);
        canvas.restoreToCount(save);
    }

    private void ensureShader(int left, int right, int width, float fade) {
        if (shader != null && left == shaderLeft && right == shaderRight
                && Math.abs(fade - shaderFade) < 0.5f) {
            return;
        }
        shaderLeft = left;
        shaderRight = right;
        shaderFade = fade;
        float edge = fade / width;
        shader = new LinearGradient(left, 0f, right, 0f,
                new int[]{0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF},
                new float[]{0f, edge, 1f - edge, 1f},
                Shader.TileMode.CLAMP);
    }
	@Override
    protected void measureChildWithMargins(android.view.View child,
            int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec;
        if (lp.width > 0) {
            // Разрешаем dockRow иметь полную ширину всех карточек (rowWidth),
            // не обрезая его шириной самого окна DockPlate
            childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        } else {
            childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin + widthUsed,
                    lp.width);
        }

        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin + heightUsed,
                lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
}
