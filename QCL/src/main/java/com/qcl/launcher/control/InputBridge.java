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

    /**
     * ★★★★★ 2026-10-12 修「滑动屏幕出现巨大视角偏移」+「滑到最左就划不动了」。
     *
     * <p>★ 第一版我加了**边界钳制** —— 那是**错的**，用户实测：
     * 「我滑到最左边，它就给我停住了，根本划不动。FCL 压根不会出现屏幕划不动的现象」。
     * 原因：转视角靠的是**位移增量**，`pointerX` 允许（也应该）无限增长；
     * 一旦钳到屏幕宽度，滑到边缘后增量就永远是 0 ⇒ 视角再也转不动。
     * ⇒ **钳制已删除**，只保留坐标系归一（这才是真正的病根）。
     *
     * <p>真正的病根：坐标系不统一 —— 同一份"屏幕坐标"，
     * <pre>
     *   MKManager.java:100/108    → setPointer(x * scaleFactor, ...)   ← 乘了
     *   TouchPad.java:251/288/313 → setPointer(x, ...)                ← 没乘
     *   MenuHelper.java:222       → setPointer(...)                   ← 没乘
     *   ViewManager.java:218/249  → setPointer(...)                   ← 没乘
     * </pre>
     * 游戏收到的光标位置在两个尺度之间**来回跳**，而 MC 用相邻帧位置差算视角 delta
     * ⇒ **滑动时突然巨大偏移**（尤其"开关一次背包/ESC 之后第一下滑动"最明显，
     * 因为那一刻两边状态刚被刷新、尺度差被一次性放大）。
     *
     * <p>修法：在**唯一出口**统一乘一次 `scaleFactor`（并把 MKManager 里原有的那次去掉，
     * 避免双重缩放）⇒ 全项目只有一个缩放点、坐标系唯一。**不做任何钳制。**
     */
    public static boolean setPointer(int i, int i2, int i3) {
        return CallbackBridge.sendCursorPos(scaleX(i2), scaleY(i3));
    }

    /** ★ 2026-10-12：按 scaleFactor 归一到游戏坐标系（**不钳制** —— 转视角需要无限位移）。 */
    private static int scaleX(int x) {
        try {
            float s = com.qcl.launcher.control.MenuHelper.sActiveScaleFactor;
            if (s > 0f && s != 1f) {
                return (int) (x * s);
            }
        } catch (Throwable ignored) {
        }
        return x;
    }

    /** ★ 2026-10-12：同上（y 轴）。 */
    private static int scaleY(int y) {
        try {
            float s = com.qcl.launcher.control.MenuHelper.sActiveScaleFactor;
            if (s > 0f && s != 1f) {
                return (int) (y * s);
            }
        } catch (Throwable ignored) {
        }
        return y;
    }

    public static void sendKeyChar(int i, char c) {
        CallbackBridge.sendChar(c, CallbackBridge.getCurrentMods());
    }
}
