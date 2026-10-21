package com.vafic.launcher;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

/** Shared dark-glass styling for focusable rows. */
final class Styles {

    private Styles() {
    }

    static StateListDrawable row(float density, float radiusDp) {
        StateListDrawable state = new StateListDrawable();

        // A focused row is a lit Aero pane: cool blue glass with a bright
        // blue-white rim and a milky highlight along the top edge.
        GradientDrawable on = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x6AB4D8F4, 0x2E6E9FD0});
        on.setCornerRadius(radiusDp * density);
        on.setStroke(Math.max(1, Math.round(1.6f * density)), 0xD6E4F6FF);

        GradientDrawable off = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x24FFFFFF, 0x12FFFFFF});
        off.setCornerRadius(radiusDp * density);
        off.setStroke(Math.max(1, Math.round(1f * density)), 0x3D9FC8E8);

        state.addState(new int[]{android.R.attr.state_focused}, on);
        state.addState(new int[]{}, off);
        return state;
    }

    static GradientDrawable chip(float density, float radiusDp, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusDp * density);
        return g;
    }
}
