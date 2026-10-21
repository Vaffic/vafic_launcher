package com.vafic.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.view.View;

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

    /** 30 frames per second, the rotation is slow enough to look smooth. */
    private static final long FRAME_MS = 33L;

    /**
     * Room around the outline for the soft glow. The glow is wider than the
     * line itself, and it must not be cut off by the edge of the view.
     */
    private static final float GLOW_MARGIN_DP = 11f;

    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SweepGradient sweep;
    private final Matrix sweepMatrix = new Matrix();
    private final RectF rect = new RectF();
    private final RectF drawRect = new RectF();
    private final RectF target = new RectF();
    private final RectF oldRect = new RectF();
    private final Rect dirty = new Rect();
    private final Rect visibleRect = new Rect();
    private final int[] ringLoc = new int[2];
    private final int[] viewLoc = new int[2];
    private final float density;
    private final float corner;

    private View focused;
    private boolean visible;
    private boolean running;
    private boolean tracked;
    private float alpha;
    private long rotationStart;

    public FocusRing(Context c) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;
        corner = 20 * density;
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
        int tint = 0xD8ECFF;
        int[] colors = new int[]{
                0x28FFFFFF, 0x32FFFFFF, 0x4AFFFFFF, 0x72FFFFFF,
                0xA6FFFFFF, 0xD8FFFFFF, 0xF4FFFFFF, 0xFFFFFFFF,
                0xF4FFFFFF, 0xD8FFFFFF, 0xA6FFFFFF, 0x72FFFFFF,
                0x4AFFFFFF, 0x32FFFFFF, 0x28FFFFFF
        };
        for (int i = 0; i < colors.length; i++) {
            int a = colors[i] >>> 24;
            colors[i] = (a << 24) | tint;
        }
        float[] positions = new float[]{
                0.00f, 0.07f, 0.16f, 0.26f, 0.36f, 0.43f, 0.48f,
                0.50f, 0.52f, 0.57f, 0.64f, 0.74f, 0.84f, 0.93f, 1.00f
        };
        sweep = new SweepGradient(0f, 0f, colors, positions);

        shine.setStyle(Paint.Style.STROKE);
        shine.setStrokeWidth(2.4f * density);
        shine.setStrokeCap(Paint.Cap.ROUND);
        shine.setStrokeJoin(Paint.Join.ROUND);
        shine.setShader(sweep);

        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeWidth(10f * density);
        glow.setStrokeCap(Paint.Cap.ROUND);
        glow.setStrokeJoin(Paint.Join.ROUND);
        glow.setShader(sweep);

        rotationStart = SystemClock.uptimeMillis();
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
        if (!card.getGlobalVisibleRect(visibleRect)
                || visibleRect.width() < card.getWidth() * 0.5f
                || visibleRect.height() < card.getHeight() * 0.5f) {
            out.setEmpty();
            return;
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
        float pad = 1.4f * density;
        out.set(cx - w / 2f - pad, cy - h / 2f - pad, cx + w / 2f + pad, cy + h / 2f + pad);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (alpha <= 0.01f || rect.isEmpty()) return;

        // The outline is drawn inside its own view, with room for the glow.
        // The view itself is scaled and moved onto the card.
        float vw = getWidth();
        float vh = getHeight();
        float margin = GLOW_MARGIN_DP * density;
        float r = Math.min(corner, Math.min(vw, vh) / 2f);
        drawRect.set(margin, margin, vw - margin, vh - margin);

        long now = SystemClock.uptimeMillis();
        float angle = ((now - rotationStart) % ROTATION_MS) * (360f / ROTATION_MS);
        sweepMatrix.setRotate(angle);
        sweepMatrix.postTranslate(drawRect.centerX(), drawRect.centerY());
        sweep.setLocalMatrix(sweepMatrix);

        // Static hairline keeps the card outline readable at the dim end of the sweep.
        base.setAlpha((int) (alpha * 78));
        canvas.drawRoundRect(drawRect, r, r, base);

        // The soft halo of the Aero glass: three strokes that fade outwards.
        glow.setShader(sweep);
        glow.setStrokeWidth(20f * density);
        glow.setAlpha((int) (alpha * 22));
        canvas.drawRoundRect(drawRect, r, r, glow);
        glow.setStrokeWidth(12f * density);
        glow.setAlpha((int) (alpha * 34));
        canvas.drawRoundRect(drawRect, r, r, glow);
        glow.setStrokeWidth(6f * density);
        glow.setAlpha((int) (alpha * 58));
        canvas.drawRoundRect(drawRect, r, r, glow);

        // The bright line that travels around the contour.
        shine.setAlpha((int) (alpha * 246));
        canvas.drawRoundRect(drawRect, r, r, shine);
    }
}
