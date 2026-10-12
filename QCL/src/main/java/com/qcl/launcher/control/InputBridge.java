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
     * ★★★★★ 2026-10-12 最终版：**坐标原样送达，不做任何缩放、不做任何钳制**。
     *
     * <p>用户实测：「我他妈想要往左滑、往右滑，他直接给我一直居中。你只抄了表面！」
     *
     * <p>这个"一直居中"是我自己绕出来的 —— 我先后在 `TouchPad` 里除以 `scaleFactor`、
     * 又在**这里**乘 `scaleFactor`（两边抵消等于没做，却把坐标空间搅乱），
     * 还在 `MKManager` 里保留了一次乘法 ⇒ 同一条链路上有三个地方在动同一个坐标，
     * 任何一处不一致都会让游戏看到的位置对不上、表现为视角被拉回中心。
     *
     * <p>FCL 的语义其实很简单：**控制层算好绝对坐标，原样交给游戏**，
     * 由 MC 自己用「本帧位置 − 上一帧位置」算 delta。
     * 坐标缩放（`scaleFactor`）在 FCL 里只作用于**触摸点 → 游戏坐标**的换算，
     * 不参与这里的指针投递。
     *
     * <p>⇒ 现在把整条链路拉直：**统一使用屏幕坐标，一个缩放点都不留**。
     * 这样 `pointerX/pointerY`、MC 的光标位置、手指位置三者始终在同一空间里，
     * 不可能再出现"对不上 → 被拉回中心"。
     */
    public static boolean setPointer(int i, int i2, int i3) {
        return CallbackBridge.sendCursorPos(i2, i3);
    }

    public static void sendKeyChar(int i, char c) {
        CallbackBridge.sendChar(c, CallbackBridge.getCurrentMods());
    }
}
