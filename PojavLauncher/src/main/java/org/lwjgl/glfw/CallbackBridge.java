package org.lwjgl.glfw;

import android.view.Choreographer;

public class CallbackBridge {
    public static Choreographer sChoreographer = Choreographer.getInstance();
    private static boolean isGrabbing = false;
    private static long lastGrabTime = System.currentTimeMillis();
    public static final int ANDROID_TYPE_GRAB_STATE = 0;
    public static final int CLIPBOARD_COPY = 2000;
    public static final int CLIPBOARD_PASTE = 2001;
    public static final int CLIPBOARD_OPEN = 2002;
    public static volatile int windowWidth;
    public static volatile int windowHeight;
    public static volatile int physicalWidth;
    public static volatile int physicalHeight;
    public static float mouseX;
    public static float mouseY;
    public static StringBuilder DEBUG_STRING;
    private static boolean threadAttached;
    public static volatile boolean holdingAlt;
    public static volatile boolean holdingCapslock;
    public static volatile boolean holdingCtrl;
    public static volatile boolean holdingNumlock;
    public static volatile boolean holdingShift;

    public static void putMouseEventWithCoords(int button, float x, float y) {
        CallbackBridge.putMouseEventWithCoords(button, true, x, y);
        sChoreographer.postFrameCallbackDelayed(l -> CallbackBridge.putMouseEventWithCoords(button, false, x, y), 33L);
    }

    public static void putMouseEventWithCoords(int button, boolean isDown, float x, float y) {
        CallbackBridge.sendCursorPos(x, y);
        CallbackBridge.sendMouseKeycode(button, CallbackBridge.getCurrentMods(), isDown);
    }

    public static boolean sendCursorPos(float x, float y) {
        DEBUG_STRING.append("CursorPos=").append(x).append(", ").append(y).append("\n");
        mouseX = x;
        mouseY = y;
        CallbackBridge.nativeSendCursorPos(mouseX, mouseY);
        return true;
    }

    public static void sendPrepareGrabInitialPos() {
        DEBUG_STRING.append("Prepare set grab initial posititon: ignored");
    }

    public static void sendKeycode(int keycode, char keychar, int scancode, int modifiers, boolean isDown) {
        DEBUG_STRING.append("KeyCode=").append(keycode).append(", Char=").append(keychar);
        if (keycode != 0) {
            CallbackBridge.nativeSendKey(keycode, scancode, isDown ? 1 : 0, modifiers);
        }
        if (isDown && keychar != '\u0000') {
            CallbackBridge.nativeSendCharMods(keychar, modifiers);
            CallbackBridge.nativeSendChar(keychar);
        }
    }

    public static void sendChar(char keychar, int modifiers) {
        CallbackBridge.nativeSendCharMods(keychar, modifiers);
        CallbackBridge.nativeSendChar(keychar);
    }

    public static void sendKeyPress(int keyCode, int modifiers, boolean status) {
        CallbackBridge.sendKeyPress(keyCode, 0, modifiers, status);
    }

    public static void sendKeyPress(int keyCode, int scancode, int modifiers, boolean status) {
        CallbackBridge.sendKeyPress(keyCode, '\u0000', scancode, modifiers, status);
    }

    public static void sendKeyPress(int keyCode, char keyChar, int scancode, int modifiers, boolean status) {
        CallbackBridge.sendKeycode(keyCode, keyChar, scancode, modifiers, status);
    }

    public static void sendKeyPress(int keyCode) {
        CallbackBridge.sendKeyPress(keyCode, CallbackBridge.getCurrentMods(), true);
        CallbackBridge.sendKeyPress(keyCode, CallbackBridge.getCurrentMods(), false);
    }

    public static void sendMouseButton(int button, boolean status) {
        CallbackBridge.sendMouseKeycode(button, CallbackBridge.getCurrentMods(), status);
    }

    public static void sendMouseKeycode(int button, int modifiers, boolean isDown) {
        DEBUG_STRING.append("MouseKey=").append(button).append(", down=").append(isDown).append("\n");
        CallbackBridge.nativeSendMouseButton(button, isDown ? 1 : 0, modifiers);
    }

    public static void sendMouseKeycode(int keycode) {
        CallbackBridge.sendMouseKeycode(keycode, CallbackBridge.getCurrentMods(), true);
        CallbackBridge.sendMouseKeycode(keycode, CallbackBridge.getCurrentMods(), false);
    }

    public static void sendScroll(double xoffset, double yoffset) {
        DEBUG_STRING.append("ScrollX=").append(xoffset).append(",ScrollY=").append(yoffset);
        CallbackBridge.nativeSendScroll(xoffset, yoffset);
    }

    public static void sendUpdateWindowSize(int w, int h) {
        windowWidth = w;
        windowHeight = h;
        CallbackBridge.nativeSendScreenSize(w, h);
    }

    public static boolean isGrabbing() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGrabTime > 250L) {
            isGrabbing = CallbackBridge.nativeIsGrabbing();
            lastGrabTime = currentTime;
        }
        return isGrabbing;
    }

    public static int getCurrentMods() {
        int currMods = 0;
        if (holdingAlt) {
            currMods |= 4;
        }
        if (holdingCapslock) {
            currMods |= 0x10;
        }
        if (holdingCtrl) {
            currMods |= 2;
        }
        if (holdingNumlock) {
            currMods |= 0x20;
        }
        if (holdingShift) {
            currMods |= 1;
        }
        return currMods;
    }

    public static void setModifiers(int keyCode, boolean isDown) {
        switch (keyCode) {
            case 340: {
                holdingShift = isDown;
                return;
            }
            case 341: {
                holdingCtrl = isDown;
                return;
            }
            case 342: {
                holdingAlt = isDown;
                return;
            }
            case 280: {
                holdingCapslock = isDown;
                return;
            }
            case 282: {
                holdingNumlock = isDown;
                return;
            }
        }
    }

    public static native void nativeSetUseInputStackQueue(boolean var0);

    public static native boolean nativeAttachThreadToOther(boolean var0, boolean var1);

    private static native boolean nativeSendChar(char var0);

    private static native boolean nativeSendCharMods(char var0, int var1);

    private static native void nativeSendKey(int var0, int var1, int var2, int var3);

    private static native void nativeSendCursorPos(float var0, float var1);

    private static native void nativeSendMouseButton(int var0, int var1, int var2);

    private static native void nativeSendScroll(double var0, double var2);

    private static native void nativeSendScreenSize(int var0, int var1);

    public static native void nativeSetWindowAttrib(int var0, int var1);

    public static native boolean nativeIsGrabbing();

    static {
        DEBUG_STRING = new StringBuilder();
        System.loadLibrary("pojavexec");
    }
}

