package com.vafic.launcher;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Переводит стики, крестовину (HAT) и кнопки геймпада в стандартные события пульта.
 */
final class GamepadInput {

    private static final float DEADZONE = 0.55f;
    private static final long INITIAL_REPEAT_MS = 280L;
    private static final long REPEAT_MS = 110L;

    private static int activeDirKey = 0;
    private static Activity targetActivity;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static final Runnable repeatStep = new Runnable() {
        @Override
        public void run() {
            if (activeDirKey == 0 || targetActivity == null || targetActivity.isFinishing()) return;
            sendSyntheticKey(targetActivity, activeDirKey);
            handler.postDelayed(this, REPEAT_MS);
        }
    };

    private GamepadInput() {
    }

    /** Нормализует коды кнопок геймпада в коды пульта (OK / BACK). */
    static int normalizeKeyCode(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                return KeyEvent.KEYCODE_DPAD_CENTER;
            case KeyEvent.KEYCODE_BUTTON_B:
                return KeyEvent.KEYCODE_BACK;
            default:
                return keyCode;
        }
    }

    /** Подменяет KeyEvent от кнопок A / B на DPAD_CENTER / BACK, чтобы работали все View и Dialog. */
    static KeyEvent normalizeKeyEvent(KeyEvent event) {
        int mapped = normalizeKeyCode(event.getKeyCode());
        if (mapped == event.getKeyCode()) return event;
        return new KeyEvent(event.getDownTime(), event.getEventTime(),
                event.getAction(), mapped, event.getRepeatCount(),
                event.getMetaState(), event.getDeviceId(), event.getScanCode(),
                event.getFlags(), event.getSource());
    }

    /** Обрабатывает левый стик и аналоговую крестовину (AXIS_HAT_X / AXIS_HAT_Y). */
    static boolean handleMotion(Activity activity, MotionEvent event) {
        if (event == null || activity == null) return false;
        int src = event.getSource();
        boolean isJoystick = (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
        if (!isJoystick || event.getActionMasked() != MotionEvent.ACTION_MOVE) return false;

        float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        float stickX = event.getAxisValue(MotionEvent.AXIS_X);
        float stickY = event.getAxisValue(MotionEvent.AXIS_Y);

        float x = Math.abs(hatX) > Math.abs(stickX) ? hatX : stickX;
        float y = Math.abs(hatY) > Math.abs(stickY) ? hatY : stickY;

        int newKey = 0;
        if (Math.abs(x) > Math.abs(y)) {
            if (x < -DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_LEFT;
            else if (x > DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_RIGHT;
        } else {
            if (y < -DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_UP;
            else if (y > DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_DOWN;
        }

        if (newKey != activeDirKey) {
            handler.removeCallbacks(repeatStep);
            activeDirKey = newKey;
            targetActivity = activity;
            if (newKey != 0) {
                sendSyntheticKey(activity, newKey);
                handler.postDelayed(repeatStep, INITIAL_REPEAT_MS);
            }
        }
        return newKey != 0;
    }

    static void stop() {
        handler.removeCallbacks(repeatStep);
        activeDirKey = 0;
        targetActivity = null;
    }

    private static void sendSyntheticKey(Activity activity, int keyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0,
                -1, 0, 0, InputDevice.SOURCE_DPAD);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0,
                -1, 0, 0, InputDevice.SOURCE_DPAD);
        activity.dispatchKeyEvent(down);
        activity.dispatchKeyEvent(up);
    }

    /** Подключает геймпад (стики и кнопки A/B) к любому открытому диалогу. */
    static void attachToDialog(final Dialog dialog, final Activity activity) {
        if (dialog.getWindow() == null) return;
        final android.view.Window.Callback orig = dialog.getWindow().getCallback();
        if (orig == null) return;
        dialog.getWindow().setCallback(new android.view.Window.Callback() {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                return orig.dispatchKeyEvent(normalizeKeyEvent(event));
            }
            @Override
            public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                return orig.dispatchKeyShortcutEvent(event);
            }
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                return orig.dispatchTouchEvent(event);
            }
            @Override
            public boolean dispatchTrackballEvent(MotionEvent event) {
                return orig.dispatchTrackballEvent(event);
            }
            @Override
            public boolean dispatchGenericMotionEvent(MotionEvent event) {
                // Стик внутри диалога двигает фокус прямо в окне диалога
                if (handleDialogMotion(dialog, event)) return true;
                return orig.dispatchGenericMotionEvent(event);
            }
            @Override
            public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
                return orig.dispatchPopulateAccessibilityEvent(event);
            }
            @Override
            public android.view.View onCreatePanelView(int featureId) {
                return orig.onCreatePanelView(featureId);
            }
            @Override
            public boolean onCreatePanelMenu(int featureId, android.view.Menu menu) {
                return orig.onCreatePanelMenu(featureId, menu);
            }
            @Override
            public boolean onPreparePanel(int featureId, android.view.View view, android.view.Menu menu) {
                return orig.onPreparePanel(featureId, view, menu);
            }
            @Override
            public boolean onMenuOpened(int featureId, android.view.Menu menu) {
                return orig.onMenuOpened(featureId, menu);
            }
            @Override
            public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
                return orig.onMenuItemSelected(featureId, item);
            }
            @Override
            public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams attrs) {
                orig.onWindowAttributesChanged(attrs);
            }
            @Override
            public void onContentChanged() {
                orig.onContentChanged();
            }
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                orig.onWindowFocusChanged(hasFocus);
            }
            @Override
            public void onAttachedToWindow() {
                orig.onAttachedToWindow();
            }
            @Override
            public void onDetachedFromWindow() {
                orig.onDetachedFromWindow();
            }
            @Override
            public void onPanelClosed(int featureId, android.view.Menu menu) {
                orig.onPanelClosed(featureId, menu);
            }
            @Override
            public boolean onSearchRequested() {
                return orig.onSearchRequested();
            }
            @Override
            public boolean onSearchRequested(android.view.SearchEvent searchEvent) {
                return orig.onSearchRequested(searchEvent);
            }
            @Override
            public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback) {
                return orig.onWindowStartingActionMode(callback);
            }
            @Override
            public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback, int type) {
                return orig.onWindowStartingActionMode(callback, type);
            }
            @Override
            public void onActionModeStarted(android.view.ActionMode mode) {
                orig.onActionModeStarted(mode);
            }
            @Override
            public void onActionModeFinished(android.view.ActionMode mode) {
                orig.onActionModeFinished(mode);
            }
        });
    }

    private static int dialogDirKey = 0;

    private static boolean handleDialogMotion(Dialog dialog, MotionEvent event) {
        if (event == null || dialog == null || dialog.getWindow() == null) return false;
        int src = event.getSource();
        boolean isJoystick = (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
        if (!isJoystick || event.getActionMasked() != MotionEvent.ACTION_MOVE) return false;

        float x = Math.abs(event.getAxisValue(MotionEvent.AXIS_HAT_X)) > Math.abs(event.getAxisValue(MotionEvent.AXIS_X))
                ? event.getAxisValue(MotionEvent.AXIS_HAT_X) : event.getAxisValue(MotionEvent.AXIS_X);
        float y = Math.abs(event.getAxisValue(MotionEvent.AXIS_HAT_Y)) > Math.abs(event.getAxisValue(MotionEvent.AXIS_Y))
                ? event.getAxisValue(MotionEvent.AXIS_HAT_Y) : event.getAxisValue(MotionEvent.AXIS_Y);

        int newKey = 0;
        if (Math.abs(x) > Math.abs(y)) {
            if (x < -DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_LEFT;
            else if (x > DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_RIGHT;
        } else {
            if (y < -DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_UP;
            else if (y > DEADZONE) newKey = KeyEvent.KEYCODE_DPAD_DOWN;
        }

        if (newKey != dialogDirKey) {
            dialogDirKey = newKey;
            if (newKey != 0) {
                long now = SystemClock.uptimeMillis();
                KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, newKey, 0);
                KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, newKey, 0);
                dialog.getWindow().superDispatchKeyEvent(down);
                dialog.getWindow().superDispatchKeyEvent(up);
            }
        }
        return newKey != 0;
    }
}