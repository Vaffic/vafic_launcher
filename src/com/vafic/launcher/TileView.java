package com.vafic.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Glass card: wide banner or stretched icon, check badge, springy focus animation. */
public class TileView extends LinearLayout {

    private final FrameLayout iconBox;
    private final ImageView iconView;
    private final TextView label;
    private final TextView badge;
    private final GlassDrawable fill;
    private final GlassDrawable rim;
    private final float density;
    private final float focusScale = 1.045f;

    private ValueAnimator focusAnim;
    private FocusListener bottomListener;
    private TextView glyphView;

    public interface FocusListener {
        void onFocusChanged(TileView tile, boolean hasFocus);
    }

    public TileView(Context c, int wPx, int hPx, int labelSp, int cornerDp, boolean withLabel) {
        super(c);
        density = c.getResources().getDisplayMetrics().density;

        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setFocusable(true);
        setFocusableInTouchMode(false);
        setClickable(true);
        setClipChildren(false);
        setClipToPadding(false);
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS);

        fill = new GlassDrawable(density, cornerDp, GlassDrawable.FILL);
        rim = new GlassDrawable(density, cornerDp, GlassDrawable.RIM);

        iconBox = new FrameLayout(c);
        iconBox.setBackground(fill);
        iconBox.setClipChildren(true);
        iconBox.setClipToOutline(true);
        // A small resting elevation makes the card float above the wallpaper
        // instead of lying flat on it.
        iconBox.setElevation(6 * density);
        addView(iconBox, new LayoutParams(wPx, hPx));

        iconView = new ImageView(c);
        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iconView.setFocusable(false);
        iconView.setClickable(false);
        iconBox.addView(iconView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        View rimView = new View(c);
        rimView.setBackground(rim);
        rimView.setFocusable(false);
        rimView.setClickable(false);
        iconBox.addView(rimView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        badge = new TextView(c);
        badge.setText("\u2713");
        badge.setTextSize(12);
        badge.setTextColor(0xFF101010);
        badge.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable bb = new android.graphics.drawable.GradientDrawable();
        bb.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        bb.setColor(0xFFFFFFFF);
        badge.setBackground(bb);
        badge.setVisibility(GONE);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                (int) (24 * density), (int) (24 * density), Gravity.END | Gravity.TOP);
        blp.topMargin = (int) (10 * density);
        blp.rightMargin = (int) (10 * density);
        iconBox.addView(badge, blp);

        label = new TextView(c);
        label.setTextSize(labelSp);
        label.setTextColor(0xB3FFFFFF);
        label.setGravity(Gravity.CENTER);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setFocusable(false);
        label.setClickable(false);
        LayoutParams llp = new LayoutParams(wPx, LayoutParams.WRAP_CONTENT);
        llp.topMargin = (int) (10 * density);
        label.setVisibility(withLabel ? View.VISIBLE : View.GONE);
        addView(label, llp);

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                animateFocus(hasFocus);
                if (bottomListener != null) bottomListener.onFocusChanged(TileView.this, hasFocus);
            }
        });
    }

    public void setFocusListener(FocusListener l) {
        this.bottomListener = l;
    }

    /** Wide artwork (android:banner) filling the whole card. */
    public void setBanner(Drawable d) {
        iconView.setPadding(0, 0, 0, 0);
        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iconView.setImageDrawable(d);
    }

    /** Square icon stretched to the wide card, used when the app has no banner. */
    public void setIcon(Drawable d) {
        setIcon(d, 26, 16);
    }

    /**
     * Square icon stretched to the wide card with a custom inner margin.
     * The dock uses a smaller margin, so the artwork fills the glass card and
     * the frame around it stays narrow.
     */
    public void setIcon(Drawable d, int padH, int padV) {
        iconView.setPadding((int) (padH * density), (int) (padV * density),
                (int) (padH * density), (int) (padV * density));
        iconView.setScaleType(ImageView.ScaleType.FIT_XY);
        iconView.setImageDrawable(d);
    }

    /** Square icon for the small decorative dock in the library header. */
    public void setIconCompact(Drawable d) {
        iconView.setPadding((int) (12 * density), (int) (7 * density),
                (int) (12 * density), (int) (7 * density));
        iconView.setScaleType(ImageView.ScaleType.FIT_XY);
        iconView.setImageDrawable(d);
    }

    /** Icon kept square and centred (used for the "edit" card). */
    public void setIconCentered(Drawable d, int sizePx) {
        iconView.setPadding(0, 0, 0, 0);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(sizePx, sizePx, Gravity.CENTER);
        iconView.setLayoutParams(lp);
        iconView.setImageDrawable(d);
    }

    /**
     * Replaces the artwork with a centred glyph. Used by the single "+" card
     * that is shown while the dock has no apps.
     */
    public void setGlyph(String glyph, int sizeSp) {
        iconView.setVisibility(View.GONE);
        if (glyphView == null) {
            glyphView = new TextView(getContext());
            glyphView.setGravity(Gravity.CENTER);
            glyphView.setIncludeFontPadding(false);
            glyphView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            glyphView.setTextColor(0xE6FFFFFF);
            GradientDrawable plate = new GradientDrawable();
            plate.setShape(GradientDrawable.OVAL);
            plate.setColor(0x1AFFFFFF);
            plate.setStroke(Math.max(1, Math.round(1.2f * density)), 0x3DFFFFFF);
            glyphView.setBackground(plate);
            int size = Math.round(58 * density);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size, Gravity.CENTER);
            iconBox.addView(glyphView, lp);
        }
        glyphView.setText(glyph);
        glyphView.setTextSize(sizeSp);
        glyphView.setVisibility(View.VISIBLE);
    }

    public void setLabel(String s) {
        label.setText(s);
    }

    public void setBadge(boolean on) {
        badge.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    /** The icon pane, used by the global PS5 focus ring. */
    public View cardView() {
        return iconBox;
    }

    private void animateFocus(boolean focused) {
        // While the user holds a direction, cards settle fast and flat instead of springing.
        boolean fast = Ui.fast();

        if (focusAnim != null) focusAnim.cancel();
        focusAnim = ValueAnimator.ofFloat(fill.getFocus(), focused ? 1f : 0f);
        focusAnim.setDuration(fast ? 90 : 180);
        focusAnim.setInterpolator(new DecelerateInterpolator());
        focusAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator a) {
                float v = (float) a.getAnimatedValue();
                fill.setFocus(v);
                rim.setFocus(v * 0.10f);
            }
        });
        focusAnim.start();

        float target = focused ? (fast ? 1.025f : focusScale) : 1f;
        iconBox.animate()
                .scaleX(target).scaleY(target)
                .setDuration(fast ? (focused ? 90 : 60) : (focused ? 200 : 150))
                .setInterpolator(fast || !focused ? new DecelerateInterpolator() : new OvershootInterpolator(0.4f))
                .start();
        iconBox.setElevation(focused ? 20 * density : 6 * density);
        label.setTextColor(focused ? 0xFFFFFFFF : 0xB3FFFFFF);
        if (fast) {
            label.setAlpha(focused ? 1f : 0.85f);
        } else {
            label.animate().alpha(focused ? 1f : 0.85f).setDuration(160).start();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            iconBox.animate().scaleX(focusScale * 0.965f).scaleY(focusScale * 0.965f)
                    .setDuration(90).setInterpolator(new DecelerateInterpolator()).start();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            float target = isFocused() ? focusScale : 1f;
            iconBox.animate().scaleX(target).scaleY(target)
                    .setDuration(260).setInterpolator(new OvershootInterpolator(0.6f)).start();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        animateFocus(selected);
    }
}
