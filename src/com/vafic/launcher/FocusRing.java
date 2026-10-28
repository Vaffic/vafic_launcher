package com.vafic.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * PS5-style focus outline. A thin border hugs the focused card and glides
 * between cards. During a library page glide the outline rides the selected
 * card instead of staying behind at a fixed screen position. A smooth white
 * gradient revolves around the contour, fading from a dim pearl white into a
 * bright highlight and back without hard edges.
 *
 * The outline is a small view that rides on the card: it is moved and scaled
 * onto the card instead of painting anywhere on the screen. A full screen
 * overlay would make the GPU repaint the glass under it on every frame of the
 * rotation, and on the weak GPUs of TV boxes that costs half of the CPU.
 */
public class FocusRing extends View {

    private static final long ROTATION_MS = 3200L;

    /** Display-rate updates while the outline rides a moving dock card. */
    private static final long FRAME_MS = 16L;

    /**
     * Room around the outline for the soft glow. The glow is wider than the
     * line itself, and it must not be cut off by the edge of the view.
     */
    private static final float GLOW_MARGIN_DP = 11f;

    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SweepGradient sweep;
    private final int[] sweepColors;
    private int sweepW = -1;
    private int sweepH = -1;
    private final Matrix sweepMatrix = new Matrix();
    private final RectF rect = new RectF();
    private final RectF drawRect = new RectF();
    private final RectF target = new RectF();
    private final RectF oldRect = new RectF();
    private final Path clipPath = new Path();
    private final Rect dirty = new Rect();
    private final Rect visibleRect = new Rect();
    private final int[] ringLoc = new int[2];
    private final int[] viewLoc = new int[2];
    private final int[] clipLoc = new int[2];
    private final int[] parentLoc = new int[2];
    private final float density;
    /** Default outline corner, used for controls that are not TileViews. */
    private final float defaultCorner;
    private float cornerScreenPx;
    private float viewScaleX = 1f;
    private float viewScaleY = 1f;

    /**
     * Optional viewport the outline has to stay inside. The dock uses it while
     * its row slides: the card that is still crossing the plate edge must not
     * get a free-floating outline in the empty area next to the dock.
     */
    private View clipView;
    private float clipFadePx;
    private float clipRadiusPx;
    private float clipMaskLeft;
    private float clipMaskRight;
    private final PorterDuffXfermode maskMode =
            new PorterDuffXfermode(PorterDuff.Mode.DST_IN);

    private View focused;
    private boolean visible;
    private boolean running;
    private boolean tracked;
    private float alpha;
    private long rotationStart;

    public FocusRing(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        defaultCorner = 20 * density;
        cornerScreenPx = defaultCorner;
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        base.setStyle(Paint.Style.STROKE);
        base.setStrokeWidth(1.7f * density);
        base.setStrokeCap(Paint.Cap.ROUND);
        base.setStrokeJoin(Paint.Join.ROUND);
        base.setColor(0xFFD8ECFF);

        // Smooth, symmetric pearl-white ramp with a cool Aero tint. The first
        // and last stops match, so the highlight wraps around the rounded
        // rectangle without a seam.
        sweepColors = new int[]{
                0xFF8FB8DC, 0xFFD8ECFF, 0xFFFFFFFF, 0xFFD8ECFF, 0xFF8FB8DC
        };

        shine.setStyle(Paint.Style.STROKE);
        shine.setStrokeWidth(2.4f * density);
        shine.setStrokeCap(Paint.Cap.ROUND);
        shine.setStrokeJoin(Paint.Join.ROUND);

        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(10f * density);
        glow.setStrokeCap(Paint.Cap.ROUND);
        glow.setStrokeJoin(Paint.Join.ROUND);

        rotationStart = SystemClock.uptimeMillis();
    }

    /**
     * While the outline rides a moving card (page glide, dock slide), it is put
     * onto the card right before every frame is drawn. A timer cannot do that:
     * it runs at an arbitrary moment between two frames, so the outline would
     * sometimes be placed before the page moved and sometimes after it, and a
     * fast scroll made it shake against the card.
     */
    private final ViewTreeObserver.OnPreDrawListener preDraw =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (tracked && visible && focused != null && focused.isAttachedToWindow()) {
                        computeTarget(target);
                        if (!target.isEmpty() && differs(target, rect)) {
                            rect.set(target);
                            applyRect();
                        }
                    }
                    return true;
                }
            };

    private static boolean differs(RectF a, RectF b) {
        return Math.abs(a.left - b.left) > 0.05f || Math.abs(a.top - b.top) > 0.05f
                || Math.abs(a.right - b.right) > 0.05f || Math.abs(a.bottom - b.bottom) > 0.05f;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(preDraw);
    }

    @Override
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnPreDrawListener(preDraw);
        super.onDetachedFromWindow();
    }

    /** Follow a focused card with a soft ease. Pass a TileView. */
    public void follow(View v) {
        if (v == null) {
            hide();
            return;
        }
        tracked = false;
        // Re-show the outline when it faded away while the card kept the
        // focus, for example after a panel was closed.
        if (v != focused || !visible || alpha < 0.02f) {
            focused = v;
            visible = true;
            alpha = 0f;
            rotationStart = SystemClock.uptimeMillis();
            setVisibility(VISIBLE);
            snap();
        }
        if (!running) {
            running = true;
            postOnAnimation(tick);
        }
    }

    /**
     * Restricts the outline to the content window of a container. Cards that
     * are halfway through the dock edge are clipped exactly like the artwork
     * underneath them, so the border never floats over the glass margin.
     * Pass null when the focused card is not inside a clipped viewport.
     */
    public void setClipView(View v) {
        if (clipView == v) return;
        clipView = v;
        invalidate();
    }

    /**
     * Feather width at the viewport edge, in screen pixels. The dock uses the
     * same width for its card mask, so the outline dissolves exactly where the
     * artwork under it is clipped. Set 0 for a hard clip.
     */
    public void setClipFade(float px) {
        px = Math.max(0f, px);
        if (Math.abs(px - clipFadePx) < 0.5f) return;
        clipFadePx = px;
        invalidate();
    }

    /** Corner radius of the viewport, so the outline cannot paint into a rounded cutout. */
    public void setClipRadius(float px) {
        px = Math.max(0f, px);
        if (Math.abs(px - clipRadiusPx) < 0.5f) return;
        clipRadiusPx = px;
        invalidate();
    }

    /**
     * Snap onto the newly focused card and ride it while the library page
     * glides. Direct tracking keeps the outline attached to the card at every
     * frame, so it never looks frozen over moving content.
     */
    public void track(View v) {
        if (v == null) {
            hide();
            return;
        }
        if (!visible) {
            visible = true;
            alpha = 0f;
            rotationStart = SystemClock.uptimeMillis();
            setVisibility(VISIBLE);
        }
        focused = v;
        tracked = true;
        computeTarget(target);
        rect.set(target);
        applyRect();
        invalidate();
        if (!running) {
            running = true;
            postOnAnimation(tick);
        }
    }

    /** Return to eased following after the page has settled. */
    public void settleTracking() {
        tracked = false;
        if (focused != null && !running) {
            running = true;
            postOnAnimation(tick);
        }
    }

    public void hide() {
        focused = null;
        tracked = false;
        clipView = null;
        clipFadePx = 0f;
        if (!running) {
            running = true;
            postOnAnimation(tick);
        }
    }

    /**
     * Stops the animation. The launcher calls this when it goes to the
     * background, so it does not keep the CPU of the TV busy while another
     * app is in front.
     */
    public void pause() {
        running = false;
        removeCallbacks(tick);
        if (focused == null) {
            // The outline was on its way out, finish it right here.
            alpha = 0f;
            visible = false;
            setVisibility(INVISIBLE);
            invalidate();
        }
    }

    /** Starts the animation again after the launcher comes back. */
    public void resume() {
        if (!running && focused != null) {
            running = true;
            postOnAnimation(tick);
        }
    }

    private void snap() {
        computeTarget(target);
        rect.set(target);
        applyRect();
        invalidate();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (focused == null || !focused.isAttachedToWindow() || focused.getWindowToken() == null) {
                alpha += (0f - alpha) * 0.25f;
                if (alpha < 0.01f) {
                    alpha = 0f;
                    visible = false;
                    running = false;
                    setVisibility(INVISIBLE);
                    invalidate();
                    return;
                }
                invalidate();
                postOnAnimation(this);
                return;
            }

            computeTarget(target);
            if (target.isEmpty()) {
                // The card is scrolled under a header or is otherwise mostly hidden.
                if (!rect.isEmpty()) {
                    rect.setEmpty();
                    invalidate();
                }
                postDelayed(this, FRAME_MS);
                return;
            }
            oldRect.set(rect);
            if (tracked || rect.isEmpty()) {
                rect.set(target);
            } else {
                float k = 0.45f;
                rect.left += (target.left - rect.left) * k;
                rect.top += (target.top - rect.top) * k;
                rect.right += (target.right - rect.right) * k;
                rect.bottom += (target.bottom - rect.bottom) * k;
            }

            alpha += (1f - alpha) * 0.30f;
            applyRect();
            invalidate();
            postDelayed(this, FRAME_MS);
        }
    };

    /**
     * Moves the ring onto the card. The view keeps a fixed size and is only
     * moved and scaled, so the layer under it never has to be redrawn.
     */
    private void applyRect() {
        if (rect.isEmpty() || getWidth() == 0 || getHeight() == 0) return;
        float vw = getWidth();
        float vh = getHeight();
        float margin = GLOW_MARGIN_DP * density;
        float pw = Math.max(1f, vw - 2f * margin);
        float ph = Math.max(1f, vh - 2f * margin);
        float sx = rect.width() / pw;
        float sy = rect.height() / ph;
        viewScaleX = sx;
        viewScaleY = sy;
        setScaleX(sx);
        setScaleY(sy);
        // The outline is centred on the card, the view is only the carrier.
        setTranslationX(rect.centerX() - vw / 2f);
        setTranslationY(rect.centerY() - vh / 2f);
    }

    private void computeTarget(RectF out) {
        View card = focused instanceof TileView ? ((TileView) focused).cardView() : focused;
        if (card == null || card.getWidth() == 0) {
            out.setEmpty();
            return;
        }
        // In a clipped viewport the visible part is handled by onDraw, so a
        // card that is only half-way through the dock edge still gets its real
        // outline and then gets clipped with the artwork.
        if (clipView == null) {
            if (!card.getGlobalVisibleRect(visibleRect)
                    || visibleRect.width() < card.getWidth() * 0.5f
                    || visibleRect.height() < card.getHeight() * 0.5f) {
                out.setEmpty();
                return;
            }
        }
        card.getLocationOnScreen(viewLoc);
        // The ring is a small view that rides on the card, so the target has
        // to be measured against the parent, not against the ring itself.
        View parent = getParent() instanceof View ? (View) getParent() : null;
        if (parent == null) {
            out.setEmpty();
            return;
        }
        parent.getLocationOnScreen(ringLoc);
        float sx = card.getScaleX();
        float sy = card.getScaleY();
        float w = card.getWidth() * sx;
        float h = card.getHeight() * sy;
        float cx = viewLoc[0] - ringLoc[0] + w / 2f;
        float cy = viewLoc[1] - ringLoc[1] + h / 2f;
        // The contour sits exactly on the card edge. The scale of the card is
        // already part of its size, so the corner follows the same scale.
        float corner = focused instanceof TileView
                ? ((TileView) focused).ringCornerPx() : defaultCorner;
        cornerScreenPx = corner * sx;
        out.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f);
    }

    /**
     * Starts a feathered clip when the focused card is crossing the viewport
     * edge. The outline is drawn into a small layer and later cut with the same
     * horizontal alpha ramp as the dock cards, so it dissolves at the edge
     * instead of ending in a bright vertical line.
     */
    private int beginViewportMask(Canvas canvas) {
        if (clipView == null || rect.isEmpty() || getWidth() == 0 || getHeight() == 0) {
            return -1;
        }
        View parent = getParent() instanceof View ? (View) getParent() : null;
        if (parent == null) return -1;
        clipView.getLocationOnScreen(clipLoc);
        parent.getLocationOnScreen(parentLoc);
        // The whole container is the viewport (the dock plate), not an inner
        // padded window. A card may travel through the glass margin.
        float left = clipLoc[0] - parentLoc[0];
        float right = left + clipView.getWidth();
        float top = clipLoc[1] - parentLoc[1];
        float bottom = top + clipView.getHeight();
        if (rect.left >= left && rect.right <= right
                && rect.top >= top && rect.bottom <= bottom) {
            return -1;
        }
        float pivotX = getWidth() / 2f;
        float pivotY = getHeight() / 2f;
        float sx = getScaleX();
        float sy = getScaleY();
        if (sx == 0f || sy == 0f) return -1;
        float localLeft = pivotX + (left - getLeft() - getTranslationX() - pivotX) / sx;
        float localRight = pivotX + (right - getLeft() - getTranslationX() - pivotX) / sx;
        float localTop = pivotY + (top - getTop() - getTranslationY() - pivotY) / sy;
        float localBottom = pivotY + (bottom - getTop() - getTranslationY() - pivotY) / sy;
        clipMaskLeft = Math.min(localLeft, localRight);
        clipMaskRight = Math.max(localLeft, localRight);
        float clipTop = Math.min(localTop, localBottom);
        float clipBottom = Math.max(localTop, localBottom);
        int save = canvas.saveLayer(0f, 0f, getWidth(), getHeight(), null);
        if (clipRadiusPx > 0.5f) {
            // Match the rounded plate so the ring cannot paint into the corner
            // cutouts when a card is leaving the dock.
            clipPath.reset();
            clipPath.addRoundRect(clipMaskLeft, clipTop, clipMaskRight, clipBottom,
                    Math.min(clipRadiusPx / sx, (clipMaskRight - clipMaskLeft) / 2f),
                    Math.min(clipRadiusPx / sy, (clipBottom - clipTop) / 2f),
                    Path.Direction.CW);
            canvas.clipPath(clipPath);
        } else {
            canvas.clipRect(clipMaskLeft, clipTop, clipMaskRight, clipBottom);
        }
        return save;
    }

    /** Applies the feather and restores the layer opened by beginViewportMask. */
    private void endViewportMask(Canvas canvas, int save) {
        if (save < 0) return;
        float width = clipMaskRight - clipMaskLeft;
        float fade = clipFadePx / Math.max(0.01f, getScaleX());
        fade = Math.min(fade, width / 2f - 0.5f);
        if (fade > 0.5f && width > 2f) {
            float edge = fade / width;
            LinearGradient shader = new LinearGradient(clipMaskLeft, 0f, clipMaskRight, 0f,
                    new int[]{0x00FFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0x00FFFFFF},
                    new float[]{0f, edge, 1f - edge, 1f},
                    Shader.TileMode.CLAMP);
            maskPaint.setShader(shader);
            maskPaint.setXfermode(maskMode);
            maskPaint.setAlpha(255);
            canvas.drawRect(clipMaskLeft, 0f, clipMaskRight, getHeight(), maskPaint);
        }
        canvas.restoreToCount(save);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (alpha <= 0.01f || rect.isEmpty()) return;

        int maskSave = beginViewportMask(canvas);

        // The outline is drawn inside its own view, with room for the glow.
        // The view itself is scaled and moved onto the card.
        float vw = getWidth();
        float vh = getHeight();
        float margin = GLOW_MARGIN_DP * density;
        drawRect.set(margin, margin, vw - margin, vh - margin);
        // The carrier is wider than the cards, so the two axes have different
        // scales. Convert the screen corner back into carrier units separately,
        // otherwise tall cards get an oval corner.
        float rx = cornerScreenPx / Math.max(0.01f, viewScaleX);
        float ry = cornerScreenPx / Math.max(0.01f, viewScaleY);
        rx = Math.min(rx, drawRect.width() / 2f);
        ry = Math.min(ry, drawRect.height() / 2f);

        ensureSweep();

        long now = SystemClock.uptimeMillis();
        float angle = ((now - rotationStart) % ROTATION_MS) * (360f / ROTATION_MS);
        sweepMatrix.setRotate(angle, drawRect.centerX(), drawRect.centerY());
        sweep.setLocalMatrix(sweepMatrix);

        float a = alpha;

        // Static hairline keeps the card outline readable at the dim end of the sweep.
        base.setAlpha((int) (a * 78));
        canvas.drawRoundRect(drawRect, rx, ry, base);

        // The soft halo of the Aero glass: three strokes that fade outwards.
        glow.setShader(sweep);
        glow.setStrokeWidth(20f * density);
        glow.setAlpha((int) (a * 22));
        canvas.drawRoundRect(drawRect, rx, ry, glow);
        glow.setStrokeWidth(12f * density);
        glow.setAlpha((int) (a * 34));
        canvas.drawRoundRect(drawRect, rx, ry, glow);
        glow.setStrokeWidth(6f * density);
        glow.setAlpha((int) (a * 58));
        canvas.drawRoundRect(drawRect, rx, ry, glow);

        // The bright line that travels around the contour.
        shine.setAlpha((int) (a * 246));
        canvas.drawRoundRect(drawRect, rx, ry, shine);

        endViewportMask(canvas, maskSave);
    }

    /**
     * Builds the sweep at the centre of the carrier. A sweep gradient whose
     * centre sits outside the view is culled by the hardware renderer on this
     * TV box, so the old "centre at 0,0 and translate" setup painted nothing.
     */
    private void ensureSweep() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (sweep != null && w == sweepW && h == sweepH) return;
        sweepW = w;
        sweepH = h;
        sweep = new SweepGradient(w / 2f, h / 2f, sweepColors, null);
        shine.setShader(sweep);
        glow.setShader(sweep);
    }
}
