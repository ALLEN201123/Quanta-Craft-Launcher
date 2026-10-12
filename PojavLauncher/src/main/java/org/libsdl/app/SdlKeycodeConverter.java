package org.libsdl.app;

import android.view.KeyEvent;

/**
 * ★★★★★ 2026-10-12 新增：**GLFW 键码 → Android 键码**转换。
 *
 * <p>用户实测（原话）：「现在只有鼠标可以生效，但是虚拟键盘生效不了，根本啥也点不动」，
 * 并提醒「我说的 SDL 不是低版本的」—— 即 **SDL3**（MC 26.3+ 的后端）。
 *
 * <p>为什么要转换：QCL 虚拟键盘（{@code view_group_keyboard.xml}）与
 * {@code InputBridge} 全程使用 **GLFW 键码**：
 * <pre>
 *   Esc 按钮  qcl:keyCode="256"   = GLFW_KEY_ESCAPE
 *   A 按钮    qcl:keyCode="65"    = GLFW_KEY_A（= ASCII 'A'）
 *   F1 按钮   qcl:keyCode="290"   = GLFW_KEY_F1
 * </pre>
 * 而 SDL3 的 {@code SDLActivity.onNativeKeyDown(int)} / {@code onNativeKeyUp(int)}
 * 要的是 **Android 键码**（SDL 官方自己在 {@code onKeyDown} 里就是
 * {@code onNativeKeyDown(keyCode)}，keyCode 来自 {@code android.view.KeyEvent}）。
 * ⇒ **必须转换，不能透传**（透传会把 256 这类 GLFW 值塞给 SDL，SDLK_* 全对不上）。
 *
 * <p>为什么不用 FCL 那套（{@code EfficientAndroidLWJGLKeycode}）：
 * 它依赖 {@code KeyCharacterMap}（**设备键盘布局相关**），且要先把 Android 键码转成
 * GLFW 再转回来，两跳且结果随设备变化。而我们**输入侧本来就是 GLFW 键码**，
 * 直接做 GLFW→Android 一跳即可，确定、可测、与设备无关。
 *
 * <p>为什么放在 {@code org.libsdl.app}（PojavLauncher 模块）而不是 QCL 模块：
 * 调用方 {@code org.lwjgl.glfw.CallbackBridge} 也在 PojavLauncher 模块；
 * 反向依赖 QCL 模块会造成模块循环依赖。
 *
 * <p>GLFW 键码规则（恰好让转换很干净）：
 * <ul>
 *   <li>{@code 32..96}：**就是 ASCII 码**（GLFW_KEY_SPACE=32 … GLFW_KEY_GRAVE_ACCENT=96）</li>
 *   <li>{@code 256..348}：特殊键（ESC / 方向 / F1-F12 / 小键盘 / 修饰键…）</li>
 * </ul>
 * 覆盖范围已按 {@code view_group_keyboard.xml} 实际用到的 **104 个键**逐一核对。
 */
public final class SdlKeycodeConverter {

    private SdlKeycodeConverter() {
    }

    /**
     * GLFW 键码 → Android 键码。
     *
     * @param glfwCode GLFW 键码（虚拟键盘 / InputBridge 传进来的值）
     * @return Android KeyEvent 键码；无法映射时返回 {@link KeyEvent#KEYCODE_UNKNOWN}
     */
    public static int toAndroidKeycode(int glfwCode) {
        // ① ASCII 区（32..96）：GLFW 直接用 ASCII 码
        if (glfwCode >= 32 && glfwCode <= 96) {
            int k = asciiToAndroid(glfwCode);
            if (k != KeyEvent.KEYCODE_UNKNOWN) {
                return k;
            }
        }
        // ② 特殊键区（256+）
        switch (glfwCode) {
            // ---- 编辑 / 控制 ----
            case 256: return KeyEvent.KEYCODE_ESCAPE;          // GLFW_KEY_ESCAPE
            case 257: return KeyEvent.KEYCODE_ENTER;           // GLFW_KEY_ENTER
            case 258: return KeyEvent.KEYCODE_TAB;             // GLFW_KEY_TAB
            case 259: return KeyEvent.KEYCODE_DEL;             // GLFW_KEY_BACKSPACE
            case 260: return KeyEvent.KEYCODE_INSERT;          // GLFW_KEY_INSERT
            case 261: return KeyEvent.KEYCODE_FORWARD_DEL;     // GLFW_KEY_DELETE
            case 262: return KeyEvent.KEYCODE_DPAD_RIGHT;      // GLFW_KEY_RIGHT
            case 263: return KeyEvent.KEYCODE_DPAD_LEFT;       // GLFW_KEY_LEFT
            case 264: return KeyEvent.KEYCODE_DPAD_DOWN;       // GLFW_KEY_DOWN
            case 265: return KeyEvent.KEYCODE_DPAD_UP;         // GLFW_KEY_UP
            case 266: return KeyEvent.KEYCODE_PAGE_UP;         // GLFW_KEY_PAGE_UP
            case 267: return KeyEvent.KEYCODE_PAGE_DOWN;       // GLFW_KEY_PAGE_DOWN
            case 268: return KeyEvent.KEYCODE_MOVE_HOME;       // GLFW_KEY_HOME
            case 269: return KeyEvent.KEYCODE_MOVE_END;        // GLFW_KEY_END
            // ---- 功能键 F1..F12（GLFW 290..301）----
            case 290: return KeyEvent.KEYCODE_F1;
            case 291: return KeyEvent.KEYCODE_F2;
            case 292: return KeyEvent.KEYCODE_F3;
            case 293: return KeyEvent.KEYCODE_F4;
            case 294: return KeyEvent.KEYCODE_F5;
            case 295: return KeyEvent.KEYCODE_F6;
            case 296: return KeyEvent.KEYCODE_F7;
            case 297: return KeyEvent.KEYCODE_F8;
            case 298: return KeyEvent.KEYCODE_F9;
            case 299: return KeyEvent.KEYCODE_F10;
            case 300: return KeyEvent.KEYCODE_F11;
            case 301: return KeyEvent.KEYCODE_F12;
            // ---- 小键盘（GLFW 320..336）----
            case 320: return KeyEvent.KEYCODE_NUMPAD_0;
            case 321: return KeyEvent.KEYCODE_NUMPAD_1;
            case 322: return KeyEvent.KEYCODE_NUMPAD_2;
            case 323: return KeyEvent.KEYCODE_NUMPAD_3;
            case 324: return KeyEvent.KEYCODE_NUMPAD_4;
            case 325: return KeyEvent.KEYCODE_NUMPAD_5;
            case 326: return KeyEvent.KEYCODE_NUMPAD_6;
            case 327: return KeyEvent.KEYCODE_NUMPAD_7;
            case 328: return KeyEvent.KEYCODE_NUMPAD_8;
            case 329: return KeyEvent.KEYCODE_NUMPAD_9;
            case 331: return KeyEvent.KEYCODE_NUMPAD_DIVIDE;
            case 332: return KeyEvent.KEYCODE_NUMPAD_MULTIPLY;
            case 333: return KeyEvent.KEYCODE_NUMPAD_SUBTRACT;
            case 334: return KeyEvent.KEYCODE_NUMPAD_ADD;
            case 335: return KeyEvent.KEYCODE_NUMPAD_ENTER;
            case 336: return KeyEvent.KEYCODE_NUMPAD_EQUALS;
            // ---- 修饰键 / 其它（GLFW 340..348）----
            case 340: return KeyEvent.KEYCODE_SHIFT_LEFT;
            case 341: return KeyEvent.KEYCODE_CTRL_LEFT;
            case 342: return KeyEvent.KEYCODE_ALT_LEFT;
            case 343: return KeyEvent.KEYCODE_META_LEFT;
            case 344: return KeyEvent.KEYCODE_SHIFT_RIGHT;
            case 345: return KeyEvent.KEYCODE_CTRL_RIGHT;
            case 346: return KeyEvent.KEYCODE_ALT_RIGHT;
            case 347: return KeyEvent.KEYCODE_META_RIGHT;
            case 348: return KeyEvent.KEYCODE_MENU;            // GLFW_KEY_MENU
            // ---- 补充常用 ----
            case 280: return KeyEvent.KEYCODE_CAPS_LOCK;       // GLFW_KEY_CAPS_LOCK
            case 281: return KeyEvent.KEYCODE_SCROLL_LOCK;     // GLFW_KEY_SCROLL_LOCK
            case 282: return KeyEvent.KEYCODE_NUM_LOCK;        // GLFW_KEY_NUM_LOCK
            case 283: return KeyEvent.KEYCODE_SYSRQ;           // GLFW_KEY_PRINT_SCREEN
            case 284: return KeyEvent.KEYCODE_BREAK;           // GLFW_KEY_PAUSE
            default:
                return KeyEvent.KEYCODE_UNKNOWN;
        }
    }

    /**
     * ASCII 码（GLFW 的 32..96 区）→ Android 键码。
     *
     * <p>注意：<b>不区分大小写</b> —— 大小写由 Shift 修饰键决定，
     * Android 的 A-Z 键码本身只有一份（{@code KEYCODE_A} 等）。
     */
    private static int asciiToAndroid(int ascii) {
        if (ascii >= 'a' && ascii <= 'z') {
            return KeyEvent.KEYCODE_A + (ascii - 'a');
        }
        if (ascii >= 'A' && ascii <= 'Z') {
            return KeyEvent.KEYCODE_A + (ascii - 'A');
        }
        if (ascii >= '0' && ascii <= '9') {
            return KeyEvent.KEYCODE_0 + (ascii - '0');
        }
        switch (ascii) {
            case 32: return KeyEvent.KEYCODE_SPACE;            // GLFW_KEY_SPACE
            case 39: return KeyEvent.KEYCODE_APOSTROPHE;       // GLFW_KEY_APOSTROPHE
            case 44: return KeyEvent.KEYCODE_COMMA;            // GLFW_KEY_COMMA
            case 45: return KeyEvent.KEYCODE_MINUS;            // GLFW_KEY_MINUS
            case 46: return KeyEvent.KEYCODE_PERIOD;           // GLFW_KEY_PERIOD
            case 47: return KeyEvent.KEYCODE_SLASH;            // GLFW_KEY_SLASH
            case 59: return KeyEvent.KEYCODE_SEMICOLON;        // GLFW_KEY_SEMICOLON
            case 61: return KeyEvent.KEYCODE_EQUALS;           // GLFW_KEY_EQUAL
            case 91: return KeyEvent.KEYCODE_LEFT_BRACKET;     // GLFW_KEY_LEFT_BRACKET
            case 92: return KeyEvent.KEYCODE_BACKSLASH;        // GLFW_KEY_BACKSLASH
            case 93: return KeyEvent.KEYCODE_RIGHT_BRACKET;    // GLFW_KEY_RIGHT_BRACKET
            case 96: return KeyEvent.KEYCODE_GRAVE;            // GLFW_KEY_GRAVE_ACCENT
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }
}
