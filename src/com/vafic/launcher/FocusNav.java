package com.vafic.launcher;

import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Performs the framework's D-pad focus move inside the app.
 *
 * <p>ViewRootImpl plays a system navigation tick itself whenever it handles a
 * D-pad event, and that tick cannot be disabled per view or with a theme. By
 * moving focus and consuming the event in the window callback first, the
 * launcher keeps the system sound out entirely and only its own tick is heard.
 */
final class FocusNav {

    private FocusNav() {
    }

    static boolean handle(View root, KeyEvent event) {
        if (root == null || event.getAction() != KeyEvent.ACTION_DOWN) return false;

        int direction;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                direction = View.FOCUS_LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                direction = View.FOCUS_RIGHT;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                direction = View.FOCUS_UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                direction = View.FOCUS_DOWN;
                break;
            default:
                return false;
        }
        // Alt+arrow and similar combinations are shortcuts, not navigation.
        if (!event.hasNoModifiers()) return false;

        View focused = root.findFocus();
        if (focused == null) return root.restoreDefaultFocus();

        View next = focused.focusSearch(direction);
        if (next != null && next != focused) {
            // Convert the old focus rectangle into the next view's coordinates.
            Rect rect = new Rect();
            focused.getFocusedRect(rect);
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                group.offsetDescendantRectToMyCoords(focused, rect);
                group.offsetRectIntoDescendantCoords(next, rect);
            }
            if (next.requestFocus(direction, rect)) return true;
        }

        // Give containers such as the library a last chance (same as the framework).
        return root instanceof ViewGroup
                && ((ViewGroup) root).dispatchUnhandledMove(focused, direction);
    }
}
