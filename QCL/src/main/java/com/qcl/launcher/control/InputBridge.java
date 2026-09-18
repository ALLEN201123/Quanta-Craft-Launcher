package com.qcl.launcher.control;

import org.lwjgl.glfw.CallbackBridge;

/* loaded from: classes2.dex */
public class InputBridge {
    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_MIDDLE = 2;
    public static final int MOUSE_RIGHT = 1;
    public static final int MOUSE_SCROLL_DOWN = 4;
    public static final int MOUSE_SCROLL_UP = 3;

    public static int getKeyCode(int i, int i2) {
        return i2;
    }

    public static int getMouseEvent(int i, int i2) {
        if (i2 == 0) {
            return 0;
        }
        if (i2 == 1) {
            return 1;
        }
        if (i2 == 2) {
            return 2;
        }
        if (i2 != 3) {
            return i2 != 4 ? -1 : 11;
        }
        return 10;
    }

    public static void sendEvent(int i, int i2, boolean z) {
        if (i2 == 0 || i2 == 1 || i2 == 2 || i2 == 3 || i2 == 4) {
            sendMouseEvent(i, i2, z);
        } else {
            sendKeycode(i, getKeyCode(i, i2), z);
        }
    }

    public static void sendKeycode(int i, int i2, boolean z) {
        CallbackBridge.sendKeyPress(i2, CallbackBridge.getCurrentMods(), z);
    }

    public static void sendMouseEvent(int i, int i2, boolean z) {
        int mouseEvent = getMouseEvent(i, i2);
        if (mouseEvent == 10) {
            if (z) {
                CallbackBridge.sendScroll(0.0d, 1.0d);
            }
        } else if (mouseEvent != 11) {
            CallbackBridge.sendMouseButton(mouseEvent, z);
        } else if (z) {
            CallbackBridge.sendScroll(0.0d, -1.0d);
        }
    }

    public static boolean setPointer(int i, int i2, int i3) {
        return CallbackBridge.sendCursorPos(i2, i3);
    }

    public static void sendKeyChar(int i, char c) {
        CallbackBridge.sendChar(c, CallbackBridge.getCurrentMods());
    }
}
