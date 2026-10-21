package org.lwjgl.glfw;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * ★★★★★ 2026-10-21：**LWJGL2 输入桥（lwjglx）**。
 *
 * <p><b>背景 / 根因</b>：
 * 玩家反馈「我的世界 1.1 版本鼠标根本点不动」。
 *
 * <p>排查结论：
 * <ul>
 *   <li>1.1 属于 LWJGL 2 时代（`detectLwjglVersion` 返回 2.8.5 / 2.9.0）
 *       → `PojavLauncher` 会给它加上 `lwjgl-lwjglx.jar`（LWJGL3 模拟 LWJGL2 API）</li>
 *   <li>`lwjglx` 的 `org.lwjgl.input.Mouse` 从
 *       `GLFWInputImplementation.pollMouse()` 取数据，而
 *       **`GLFWInputImplementation.createMouse()` 并没有注册
 *       `glfwSetMouseButtonCallback` / `glfwSetCursorPosCallback`**。</li>
 *   <li>也就是说 `lwjglx` 是**设计成由外部主动喂事件**的 ——
 *       它提供了 `public void putMouseEventWithCoords(byte button, byte state,
 *       int x, int y, int dz, long nanos)` 和
 *       `public void putKeyboardEvent(int key, byte state, int ch, long nanos,
 *       boolean repeat)`。</li>
 *   <li>而 `CallbackBridge` 只调了 **native** 的
 *       `nativeSendMouseButton` / `nativeSendKey`——那条路只喂 GLFW（LWJGL3），
 *       **从来没有喂给 `lwjglx`**。</li>
 * </ul>
 *
 * <p>⇒ 结果：LWJGL3 时代（1.13+）一切正常；**LWJGL2 时代（≤1.12，含 1.1）
 * 的鼠标/键盘事件根本没进游戏**，表现就是"点不动"。
 *
 * <p><b>修法</b>：这里用**反射**把事件同时喂给 `GLFWInputImplementation`。
 * 之所以必须反射：`CallbackBridge` 在 PojavLauncher 模块，编译期根本没有
 * `lwjglx` 的类（它是运行时才加进 classpath 的 jar）。
 *
 * <p>★ 找不到 `GLFWInputImplementation`（非 LWJGL2 版本）时**静默跳过**，
 * 不影响任何现代版本。
 */
public final class LwjglXBridge {

    private static Object impl;
    private static Method putMouse;
    private static Method putKey;
    private static Field fieldMouseX;
    private static Field fieldMouseY;
    private static Field fieldLastPhysicalX;
    private static Field fieldLastPhysicalY;
    private static boolean tried;
    private static boolean available;

    /** 最近一次喂进去的按键状态（用于 `setMouseButtonInGrabMode` 之类的兼容）。 */
    private static final boolean[] mouseDown = new boolean[8];

    private LwjglXBridge() {
    }

    /**
     * 惰性初始化（第一次调用时反射找类）。
     *
     * @return true 表示 lwjglx 存在且可用
     */
    private static boolean ensure() {
        if (tried) {
            return available;
        }
        tried = true;
        try {
            Class<?> c = Class.forName("org.lwjgl.input.GLFWInputImplementation");
            Field singleton = c.getField("singleton");
            impl = singleton.get(null);
            if (impl == null) {
                return false;
            }
            putMouse = c.getMethod("putMouseEventWithCoords",
                    byte.class, byte.class, int.class, int.class, int.class, long.class);
            putKey = c.getMethod("putKeyboardEvent",
                    int.class, byte.class, int.class, long.class, boolean.class);
            try {
                fieldMouseX = c.getField("mouseX");
                fieldMouseY = c.getField("mouseY");
            } catch (Throwable ignored) {
            }
            try {
                fieldLastPhysicalX = c.getField("lastPhysicalX");
                fieldLastPhysicalY = c.getField("lastPhysicalY");
            } catch (Throwable ignored) {
            }
            available = true;
        } catch (Throwable t) {
            available = false;
        }
        return available;
    }

    /** 当前 lwjglx 是否存在（用于诊断）。 */
    public static boolean isAvailable() {
        return ensure();
    }

    /**
     * 投递鼠标按键。
     *
     * @param button GLFW 鼠标键号（0=左 1=右 2=中）
     * @param down   按下 true / 抬起 false
     * @param x      物理 X（-1 表示沿用上一次）
     * @param y      物理 Y（-1 表示沿用上一次）
     */
    public static void mouseButton(int button, boolean down, int x, int y) {
        if (!ensure()) {
            return;
        }
        try {
            if (button >= 0 && button < mouseDown.length) {
                mouseDown[button] = down;
            }
            putMouse.invoke(impl, (byte) button, (byte) (down ? 1 : 0),
                    x, y, 0, System.nanoTime());
        } catch (Throwable ignored) {
        }
    }

    /** 投递滚轮（dz：正=上滚，负=下滚）。 */
    public static void scroll(int dz) {
        if (!ensure()) {
            return;
        }
        try {
            putMouse.invoke(impl, (byte) 0, (byte) 0, -1, -1, dz, System.nanoTime());
        } catch (Throwable ignored) {
        }
    }

    /** 投递键盘按键。 */
    public static void key(int key, boolean down) {
        if (!ensure()) {
            return;
        }
        try {
            putKey.invoke(impl, key, (byte) (down ? 1 : 0), 0, System.nanoTime(), false);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 同步光标物理坐标（不产生按键事件）。
     *
     * <p>`lwjglx` 的 `pollMouse` 在 grab 模式下返回 `mouseX - mouseLastX`，
     * 所以必须把当前坐标写进 `mouseX` / `mouseY`，否则视角不会转。
     */
    public static void cursorPos(int x, int y) {
        if (!ensure()) {
            return;
        }
        try {
            if (fieldMouseX != null) {
                fieldMouseX.setInt(impl, x);
            }
            if (fieldMouseY != null) {
                fieldMouseY.setInt(impl, y);
            }
            if (fieldLastPhysicalX != null) {
                fieldLastPhysicalX.setInt(impl, x);
            }
            if (fieldLastPhysicalY != null) {
                fieldLastPhysicalY.setInt(impl, y);
            }
        } catch (Throwable ignored) {
        }
    }
}
