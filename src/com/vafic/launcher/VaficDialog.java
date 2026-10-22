package com.vafic.launcher;

import android.app.Dialog;
import android.content.Context;
import android.view.KeyEvent;

/**
 * Base class for the launcher's dialogs. D-pad focus is moved by FocusNav
 * before the framework can handle it, which keeps the system navigation tick
 * from mixing with the launcher's own sound.
 */
public class VaficDialog extends Dialog {

    public VaficDialog(Context c) {
        super(c);
    }

    public VaficDialog(Context c, int themeResId) {
        super(c, themeResId);
    }

    public VaficDialog(Context c, boolean cancelable, OnCancelListener listener) {
        super(c, cancelable, listener);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (super.dispatchKeyEvent(event)) return true;
        return FocusNav.handle(getWindow() == null ? null : getWindow().getDecorView(), event);
    }

    @Override
    public void show() {
        super.show();
        if (getWindow() != null) {
            Ui.disableSystemClicks(getWindow().getDecorView());
        }
    }
}
