package com.vafic.launcher;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.FrameLayout;

/** Dark Aero-style frosted window with a bright glass rim, shared by the launcher dialogs. */
public class GlassPanel extends FrameLayout {

    public GlassPanel(Context c, float density, float radiusDp) {
        super(c);

        GradientDrawable base = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xF2243352, 0xF2101A2A, 0xF00A1018});
        base.setCornerRadius(radiusDp * density);
        base.setStroke(Math.max(1, Math.round(density)), 0x66A8C8E8);

        GlassDrawable fill = new GlassDrawable(density, radiusDp, GlassDrawable.FILL);
        fill.setFocus(0.45f);
        setBackground(new LayerDrawable(new android.graphics.drawable.Drawable[]{base, fill}));

        GlassDrawable rim = new GlassDrawable(density, radiusDp, GlassDrawable.RIM);
        rim.setFocus(0.35f);
        setForeground(rim);

        setClipChildren(false);
    }
}
