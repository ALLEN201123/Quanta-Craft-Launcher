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
     * ★★★★★ 2026-10-12 修复「滑动屏幕出现巨大视角偏移」（用户确认：**所有版本**都有，
     * 不只是 SDL 版本 ⇒ QCL 原生老 bug）。
     *
     * <p>真凶：坐标系不统一。
     * <pre>
     *   MKManager.java:100/108   → setPointer(x * scaleFactor, ...)   ← 乘了
     *   TouchPad.java:251/288/313→ setPointer(x, ...)                ← 没乘
     *   MenuHelper.java:222      → setPointer(...)                   ← 没乘
     *   ViewManager.java:218/249 → setPointer(...)                   ← 没乘
     * </pre>
     * 同一份"屏幕坐标"有的乘、有的不乘 ⇒ 游戏收到的光标位置在两个尺度之间**来回跳**，
     * MC 用相邻帧位置差算视角 delta ⇒ **滑动时突然巨大偏移**。
     *
     * <p>另一个问题是**完全没有边界钳制**：`TouchPad` 的抓取分支把每帧位移
     * （还乘了玩家的 `mouseSpeed`）**无界累加**到 `pointerX/pointerY`，
     * 越滑越远、可达数万像素 ⇒ 一旦游戏把它当绝对位置用就是从屏幕角落出发的巨大跳变。
     *
     * <p>★ 修法（照 FCL，但收在**唯一出口**这一处，比改 8 个调用点安全得多）：
     * <ol>
     *   <li>统一乘 `scaleFactor`（与 MKManager 原本的做法一致）—— 让所有调用点归一；</li>
     *   <li>钳到屏幕范围内 —— 消灭无界累加带来的离谱坐标。</li>
     * </ol>
     * 这样无论调用方乘没乘，出去的坐标都在同一尺度、且永远在屏内。
     *
     * @param i      launcher 标识（1/2），直接透传
     * @param i2     x（屏幕坐标，调用方可以乘也可以不乘 scaleFactor）
     * @param i3     y（同上）
     */
    public static boolean setPointer(int i, int i2, int i3) {
        return CallbackBridge.sendCursorPos(scaleAndClampX(i2), scaleAndClampY(i3));
    }

    /** ★ 2026-10-12：按 scaleFactor 归一 + 钳到屏内（x 轴）。 */
    private static int scaleAndClampX(int x) {
        try {
            int sw = screenW();
            // ① 先在**屏幕坐标系**钳一次（调用方传来的应该是屏幕坐标）
            if (sw > 0) {
                if (x < 0) x = 0;
                else if (x > sw) x = sw;
            }
            // ② 再乘 scaleFactor 归一到游戏坐标系
            float s = com.qcl.launcher.control.MenuHelper.sActiveScaleFactor;
            if (s > 0f && s != 1f) {
                x = (int) (x * s);
            }
            // ③ 最后在**游戏坐标系**再钳一次（防止调用方已经乘过 scaleFactor）
            int gw = gameW();
            if (gw > 0) {
                if (x < 0) x = 0;
                else if (x > gw) x = gw;
            }
        } catch (Throwable ignored) {
        }
        return x;
    }

    /** ★ 2026-10-12：按 scaleFactor 归一 + 钳到屏内（y 轴）。 */
    private static int scaleAndClampY(int y) {
        try {
            int sh = screenH();
            if (sh > 0) {
                if (y < 0) y = 0;
                else if (y > sh) y = sh;
            }
            float s = com.qcl.launcher.control.MenuHelper.sActiveScaleFactor;
            if (s > 0f && s != 1f) {
                y = (int) (y * s);
            }
            int gh = gameH();
            if (gh > 0) {
                if (y < 0) y = 0;
                else if (y > gh) y = gh;
            }
        } catch (Throwable ignored) {
        }
        return y;
    }

    private static int screenW() {
        int w = org.lwjgl.glfw.CallbackBridge.physicalWidth;
        return w > 0 ? w : org.lwjgl.glfw.CallbackBridge.windowWidth;
    }

    private static int screenH() {
        int h = org.lwjgl.glfw.CallbackBridge.physicalHeight;
        return h > 0 ? h : org.lwjgl.glfw.CallbackBridge.windowHeight;
    }

    private static int gameW() {
        int w = org.lwjgl.glfw.CallbackBridge.windowWidth;
        if (w <= 0) {
            w = org.lwjgl.glfw.CallbackBridge.physicalWidth;
        }
        return w;
    }

    private static int gameH() {
        int h = org.lwjgl.glfw.CallbackBridge.windowHeight;
        if (h <= 0) {
            h = org.lwjgl.glfw.CallbackBridge.physicalHeight;
        }
        return h;
    }

    public static void sendKeyChar(int i, char c) {
        CallbackBridge.sendChar(c, CallbackBridge.getCurrentMods());
    }
}
