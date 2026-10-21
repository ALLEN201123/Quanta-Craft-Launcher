package org.lwjgl.glfw;

import android.view.Choreographer;
import android.view.MotionEvent;

import net.kdt.pojavlaunch.keyboard.LwjglGlfwKeycode;
import org.libsdl.app.SDLActivity;
import org.libsdl.app.SdlBridge;

public class CallbackBridge {
    public static Choreographer sChoreographer = Choreographer.getInstance();

    /**
     * ★★★★★ 2026-10-11 新增（修「MC 26.4 按钮点不动」）：累积的鼠标按键状态。
     *
     * <p>SDL 的 {@code MotionEvent} 要的是 **buttonState 位掩码**（不是单个按键），
     * 所以按 FCL 的做法在这里累积：按下时置位、抬起时清位，再把整个掩码报给 SDL。
     */
    private static int sMouseButtonState = 0;

    private static volatile boolean isGrabbing = false;
    /** push 通道是否已经收到过回调；没收到则退回 native 轮询（兜底）。 */
    private static volatile boolean grabStatePushed = false;
    /** 是否曾经收到过游戏的 grab=true —— 收到之后就不再抑制绝对光标推送。 */
    private static volatile boolean grabEverReported = false;
    /** ★ 1.4.3：无标题界面版本（Classic / Indev < 20100131）专用开关，由 PojavMinecraftActivity 设置。 */
    private static volatile boolean suppressPointerUntilFirstGrab = false;
    /** 首帧到达时刻（uptimeMillis）。护栏只在这个时刻之后才生效，见 shouldSuppressPointer()。 */
    private static volatile long firstFrameAt = -1L;
    /**
     * 护栏最长生效时长。**这是兜底安全网**：万一某个"其实有标题界面"的版本被误开了护栏，
     * 它只会损失这段时间的鼠标（约 20 秒）就会自动恢复，绝不会整局都推不动光标。
     */
    private static final long SUPPRESS_MAX_MS = 20000L;
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
        // ★★★★★ 2026-10-21：同步物理坐标给 lwjglx（grab 模式下它算的是 mouseX - mouseLastX，
        //   不写这两个字段的话 1.1 的视角永远不转）。
        try {
            LwjglXBridge.cursorPos((int) x, (int) y);
        } catch (Throwable ignored) {
        }
        DEBUG_STRING.append("CursorPos=").append(x).append(", ").append(y).append("\n");
        float dx = x - mouseX;
        float dy = y - mouseY;
        mouseX = x;
        mouseY = y;
        CallbackBridge.nativeSendCursorPos(mouseX, mouseY);
        // ★★★★★ 2026-10-11 修「MC 26.4 能进主菜单但按钮点不动」——**SDL 输入双路**。
        //
        //   根因（子代理逐文件对照 FCL 查出，证据：`git log --all -S onNativeMouse` 为空）：
        //     MC 26.3+ 用 SDL3 窗口后端，它**只从 SDL 事件队列取输入**
        //     （SDL_PollEvent）。而 QCL 的输入出口**只有 GLFW 一条**：
        //       TouchPad → InputBridge → CallbackBridge.nativeSend* →
        //       input_bridge_v3.c 的 pojav_environ 队列 → 只有 pojavPumpEvents() 消费，
        //       而它内部是 GLFW_invoke_CursorPos / GLFW_invoke_MouseButton。
        //     SDL 版 MC **不调用任何 GLFW 函数** ⇒ 队列永远没人读 ⇒ 点击全部丢失。
        //     渲染不受影响（SDL surface 走 egl_bridge 另一条路）⇒ 完美对应
        //     「画面正常、按钮点不动」。
        //   ★ 这不是 1.5.0 改坏的 —— 这段代码 QCL **从来没有过**（1.4.8 也没有），
        //     是「还原被误删的 SDL3 Hook」让 26.4 第一次能进主菜单，才把这个老缺口暴露出来。
        //
        //   修法：投射一份到 SDL（FCL 同款）。闸门必须是 SdlBridge.getSdlEnabled()
        //   —— 它只在 SDL_Init 握手完成后为 true；**不能**用
        //   SDLActivity.getSDLSurface() != null，因为 PojavMinecraftActivity 对
        //   **所有版本**都无条件调 prepareSurface（非 SDL 版本也会成立）⇒ 会对
        //   未初始化的 SDL 调 native 而崩。
        if (!SdlBridge.getSdlEnabled()) {
            return true;
        }
        try {
            if (CallbackBridge.isGrabbing()) {
                // 锁指针（游戏内视角）→ 报**相对增量**
                SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, dx, dy, true);
            } else {
                // 菜单/未锁定 → 报**绝对坐标**
                SDLActivity.onNativeMouse(0, MotionEvent.ACTION_MOVE, x, y, false);
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    public static void sendPrepareGrabInitialPos() {
        DEBUG_STRING.append("Prepare set grab initial posititon: ignored");
    }


    public static void sendKeycode(int keycode, char keychar, int scancode, int modifiers, boolean isDown) {
        DEBUG_STRING.append("KeyCode=").append(keycode).append(", Char=").append(keychar);
        if (keycode != 0) {
            CallbackBridge.nativeSendKey(keycode, scancode, isDown ? 1 : 0, modifiers);
            // ★★★★★ 2026-10-12「SDL 输入双路」第 4 条 —— **虚拟键盘 / 实体键盘**。
            //
            //   用户实测：「现在只有鼠标可以生效，但是虚拟键盘生效不了，根本啥也点不动」，
            //   并提醒「我说的 SDL 不是低版本的」⇒ 是 **SDL3**（MC 26.3+ 的后端）。
            //   与鼠标同一个病根：GLFW 那条发了，但 SDL 版 MC **只读 SDL 事件队列**。
            //
            //   ★★ 关键：**不能透传！** QCL 的 keycode 是 **GLFW 键码**，不是 Android 键码。
            //     证据（QCL/src/main/res/layout/view_group_keyboard.xml）：
            //       Esc 按钮 qcl:keyCode="256" = GLFW_KEY_ESCAPE
            //       F1  按钮 qcl:keyCode="290" = GLFW_KEY_F1
            //       A   按钮 qcl:keyCode="65"  = GLFW_KEY_A（= ASCII 'A'）
            //     而 SDL3 的 SDLActivity.onNativeKeyDown(int) 要的是 **Android 键码**
            //     （SDL 官方在 onKeyDown 里就是 onNativeKeyDown(keyCode)，
            //       keyCode 来自 android.view.KeyEvent）。
            //     透传会把 256 这类 GLFW 值塞给 SDL ⇒ SDLK_* 全对不上 ⇒ 依然点不动。
            //   ⇒ 走 SdlKeycodeConverter 做 **GLFW → Android 一跳转换**
            //     （已按虚拟键盘实际用到的 104 个键逐一核对）。
            //
            //   闸门仍是 SdlBridge.getSdlEnabled()（GLFW 版本为 false，行为不变）。
            //   ★ 这里**不能 return**（下面还要发 char），故用嵌套判断。
            if (SdlBridge.getSdlEnabled()) {
                try {
                    int androidKeycode = org.libsdl.app.SdlKeycodeConverter
                            .toAndroidKeycode(keycode);
                    if (androidKeycode != android.view.KeyEvent.KEYCODE_UNKNOWN) {
                        if (isDown) {
                            SDLActivity.onNativeKeyDown(androidKeycode);
                        } else {
                            SDLActivity.onNativeKeyUp(androidKeycode);
                        }
                    } else {
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (isDown && keychar != '\u0000') {
            CallbackBridge.nativeSendCharMods(keychar, modifiers);
            CallbackBridge.nativeSendChar(keychar);
        }
    }

    public static void sendChar(char keychar, int modifiers) {
        CallbackBridge.nativeSendCharMods(keychar, modifiers);
        CallbackBridge.nativeSendChar(keychar);
        // ★★★★★ 2026-10-12「SDL 输入双路」第 5 条 —— **文本输入**（聊天框 / 告示牌 / 命名）。
        //   照 FCL 的做法：按下 → 抬起 各报一次，让 SDL 产出对应的文本事件。
        //   ★ 与键码那条一样，这里也**不能透传 char** —— SDL 要的是 Android 键码，
        //     所以复用 SdlKeycodeConverter 的 ASCII 区映射（A-Z / 0-9 / 常用符号）。
        //     ★ 关键差异：**注意大小写**。sendKeycode 那条大小写由 Shift 决定，
        //     而同为 A-Z 键码；这里补 Shift 掩码，保证聊天框里大小写正确。
        if (!SdlBridge.getSdlEnabled()) {
            return;
        }
        try {
            int code = org.libsdl.app.SdlKeycodeConverter.toAndroidKeycode(keychar);
            if (code == android.view.KeyEvent.KEYCODE_UNKNOWN) {
                // 中文等非 ASCII：交给系统输入法（SDLInputConnection）那条路，这里跳过
                return;
            }
            boolean upper = keychar >= 'A' && keychar <= 'Z';
            if (upper) {
                // 大写字母：SDL 侧需要看到 Shift 按下，否则会产出小写
                SDLActivity.onNativeKeyDown(android.view.KeyEvent.KEYCODE_SHIFT_LEFT);
            }
            SDLActivity.onNativeKeyDown(code);
            SDLActivity.onNativeKeyUp(code);
            if (upper) {
                SDLActivity.onNativeKeyUp(android.view.KeyEvent.KEYCODE_SHIFT_LEFT);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void sendKeyPress(int keyCode, int modifiers, boolean status) {
        // ★★★★★ 2026-10-21：键盘也补一份给 lwjglx（1.1 的 org.lwjgl.input.Keyboard
        //   同样是等外部调 putKeyboardEvent 喂的，native 那条只喂 GLFW）。
        try {
            LwjglXBridge.key(keyCode, status);
        } catch (Throwable ignored) {
        }
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
        // ★★★★★ 2026-10-21「LWJGL2 时代输入修复」（1.1 / ≤1.12 鼠标点不动）：
        //
        //   上面那条 nativeSendMouseButton 只把事件喂给 **GLFW**（LWJGL 3 那套）。
        //   而 LWJGL2 时代的版本（1.1 ~ 1.12）走的是 `lwjgl-lwjglx.jar` 兼容层，
        //   它的 `org.lwjgl.input.Mouse` 从 `GLFWInputImplementation.pollMouse()`
        //   取数据 —— 而 `GLFWInputImplementation.createMouse()` **从不注册
        //   glfwSetMouseButtonCallback**，也就是说它设计上就是**等外部主动喂事件**的。
        //   我们从来没喂过 ⇒ 1.1 的鼠标点击根本进不了游戏。
        //
        //   ⇒ 这里再补一份给 lwjglx（反射，找不到类就静默跳过，不影响现代版本）。
        try {
            LwjglXBridge.mouseButton(button, isDown, -1, -1);
        } catch (Throwable ignored) {
        }
        // ★★★★★ 2026-10-11「SDL 输入双路」第 2 条（**这条才是按钮点得动的关键**）：
        //   GLFW 那条已经发了，但 SDL 版 MC（26.3+）只读 SDL 事件队列 ⇒ 必须再投一份。
        //   SDL 要的是 **buttonState 位掩码**（不是单个按键），所以这里累积后再上报
        //   —— 与 FCL 的 sMouseButtonState 做法完全一致。
        if (!SdlBridge.getSdlEnabled()) {
            return;
        }
        int aKey = -1;
        switch (button) {
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT:
                aKey = MotionEvent.BUTTON_PRIMARY;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT:
                aKey = MotionEvent.BUTTON_SECONDARY;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE:
                aKey = MotionEvent.BUTTON_TERTIARY;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_4:   // = 3（侧键"后退"）
                aKey = MotionEvent.BUTTON_BACK;
                break;
            case LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_5:   // = 4（侧键"前进"）
                aKey = MotionEvent.BUTTON_FORWARD;
                break;
            default:
                break;
        }
        if (aKey == -1) {
            return;
        }
        if (isDown) {
            sMouseButtonState |= aKey;
        } else {
            sMouseButtonState &= ~aKey;
        }
        try {
            // ★★★★★ 2026-10-15 **恢复传真实坐标（撤销我把坐标归零的那一版）**。
            //
            //   用户实测：「你他妈不传坐标，他妈连那些单人游戏之类的按键都点不动了」
            //   ⇒ 点击事件**必须**携带坐标，SDL 要靠它定位点击位置。
            //     传 (0,0) 等于每次都点在屏幕左上角 ⇒ 按钮全部点不中。
            //
            //   ★ 结论（试过的三条路都不对，记录下来避免重走）：
            //     ① `relative=false` + 真实坐标 → 打开背包后点击大偏移
            //     ② `relative=true`  + 真实坐标 → 随便点就大偏移（更糟）
            //     ③ `(0,0)`                       → 按钮全部点不动
            //   ⇒ 只能回到 ①（与 FCL 逐字一致的原样），这是**唯一能点动按钮**的版本。
            SDLActivity.onNativeMouse(sMouseButtonState,
                    isDown ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP,
                    mouseX, mouseY, false);
        } catch (Throwable ignored) {
        }
    }

    public static void sendMouseKeycode(int keycode) {
        CallbackBridge.sendMouseKeycode(keycode, CallbackBridge.getCurrentMods(), true);
        CallbackBridge.sendMouseKeycode(keycode, CallbackBridge.getCurrentMods(), false);
    }

    public static void sendScroll(double xoffset, double yoffset) {
        DEBUG_STRING.append("ScrollX=").append(xoffset).append(",ScrollY=").append(yoffset);
        CallbackBridge.nativeSendScroll(xoffset, yoffset);
        // ★★★★★ 2026-10-11「SDL 输入双路」第 3 条：滚轮也要投一份给 SDL（26.3+ 只读 SDL 队列）
        if (!SdlBridge.getSdlEnabled()) {
            return;
        }
        try {
            SDLActivity.onNativeMouse(0, MotionEvent.ACTION_SCROLL,
                    (float) xoffset, (float) yoffset, false);
        } catch (Throwable ignored) {
        }
    }

    public static void sendUpdateWindowSize(int w, int h) {
        windowWidth = w;
        windowHeight = h;
        CallbackBridge.nativeSendScreenSize(w, h);
    }

    /**
     * 当前抓取状态（= 游戏是否在接管鼠标）。
     *
     * ★ 1.4.3 修复：原实现是「每次轮询 native + 250ms 缓存」，导致关掉 ESC 菜单 / 背包之后
     *   最多 250ms（实测 137~203ms）才切回游戏模式；这段窗口里 TouchPad 仍按**绝对光标**
     *   处理，于是把光标从「菜单里最后点击的按钮位置」直接搬到你手指落点 → 视角一次性猛偏。
     *   现在改成两条路：
     *   · 正常：native 在状态变化时**主动 push**（onGrabStateChanged），这里零延迟返回；
     *   · 兜底：push 通道没接上时退回轮询 nativeIsGrabbing()，但缓存从 250ms 缩到 16ms（一帧）。
     */
    public static boolean isGrabbing() {
        if (grabStatePushed) {
            return isGrabbing;
        }
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastGrabTime > 16L) {
            isGrabbing = CallbackBridge.nativeIsGrabbing();
            lastGrabTime = currentTime;
        }
        return isGrabbing;
    }

    /** 抓取状态监听（由 BaseMainActivity 注册）。回调发生在游戏线程，实现方自行切主线程。 */
    public interface GrabStateListener {
        void onGrabStateChange(boolean grabbing);
    }

    private static volatile GrabStateListener grabStateListener;

    public static void setGrabStateListener(GrabStateListener listener) {
        grabStateListener = listener;
    }

    /**
     * ★ 1.4.3：给「无标题界面版本」开/关「首次 grab 之前抑制绝对光标推送」。
     * 背景：Classic 全系 + Indev 0.31 里早于 20100131 的构建（标题界面就是 20100131 加入的）
     * 进游戏后**先加载一段时间**、没有标题界面，之后游戏才自己报 grab=true（实测首帧后约 2.7s）。
     * 这段窗口里 QCL 还是「光标模式」，滑动被当**绝对光标位移**投给游戏 → 开局视角一次性大偏。
     * 而这些版本在世界出现前没有任何需要鼠标的 GUI，所以这段窗口里**不投坐标是安全的**。
     */
    public static void setSuppressPointerUntilFirstGrab(boolean suppress) {
        suppressPointerUntilFirstGrab = suppress;
        firstFrameAt = -1L;      // 换版本重新开始计时
    }

    /** 首帧到达时调用（PojavMinecraftActivity.onPicOutput）。护栏的计时从这一刻才开始。 */
    public static void notifyFirstFrame() {
        if (firstFrameAt < 0L) {
            firstFrameAt = android.os.SystemClock.uptimeMillis();
        }
    }

    /**
     * 现在是否应当抑制绝对光标推送。
     *
     * 成立条件（**三条都要满足**，缺一不可）：
     *  ① 只有被标记为「无标题界面版本」的才启用；
     *  ② 游戏还没报过 grab=true；
     *  ③ **首帧已到达**、且距首帧不超过 {@link #SUPPRESS_MAX_MS}。
     *
     * ③ 里「首帧之后才生效」+「有上限」是两道安全网：
     *   · 首帧之前（加载期）不抑制 —— 那时还没世界可转，抑制没有意义，也避免影响加载界面；
     *   · 万一某个**其实有标题界面**的版本被误判，游戏在标题界面永远不会报 grab=true，
     *     但护栏最多持续 20 秒就会自动放行 —— 玩家只损失约 20 秒的鼠标，不会整局推不动。
     */
    public static boolean shouldSuppressPointer() {
        if (!suppressPointerUntilFirstGrab || grabEverReported) {
            return false;
        }
        long start = firstFrameAt;
        if (start < 0L) {
            return false;
        }
        return android.os.SystemClock.uptimeMillis() - start <= SUPPRESS_MAX_MS;
    }

    /**
     * 由 native nativeSetGrabbing 主动调用（照 FCL 的 onGrabStateChanged）。
     * ★ 不要删/改名：native 侧用 GetStaticMethodID 按 "onGrabStateChanged" / "(Z)V" 精确匹配。
     */
    @SuppressWarnings("unused")
    private static void onGrabStateChanged(final boolean grabbing) {
        isGrabbing = grabbing;
        grabStatePushed = true;
        if (grabbing) {
            grabEverReported = true;   // ★ 1.4.3：一旦报过 grab，就不再抑制绝对光标推送
        }
        GrabStateListener listener = grabStateListener;
        if (listener != null) {
            listener.onGrabStateChange(grabbing);
        }
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

    // ===== 1.1.1 SDL3 集成（移植自 FCL CallbackBridge）=====
    public static final int NOTIF_TYPE_SDL = 0;
    public static final int ACTION_INIT_LAUNCHER_INTEGRATION = 0;
    public static final int ACTION_SEND_TEXTBOX_RECT = 1;

    /** ★ LWJGL 的 org.lwjgl.sdl.SDLInit.SDL_Init() 里引用的就是这两个常量名（align with FCL）。 */
    public static final int SDL = NOTIF_TYPE_SDL;
    public static final int INIT = ACTION_INIT_LAUNCHER_INTEGRATION;

    /**
     * ★★★ 2026-09-19 修复 26.3（SDL3）启动崩溃（照 FCL CallbackBridge 逐字对齐）：
     *   LWJGL 组件内的 {@code org.lwjgl.sdl.SDLInit} 把它**声明为 native**，
     *   但 FCL 的注释写明「运行时以本实现为准（避免依赖额外 C 符号）」——
     *   即**在本类里给出 Java 实现**，类加载后就覆盖掉 LWJGL 里的 native 声明。
     *
     *   QCL 此前把它写成了 {@code public static native boolean nativeNotifyLauncher(int, int[])}：
     *   ① 声明为 native，运行时需要 JNI 符号 {@code Java_org_lwjgl_glfw_CallbackBridge_nativeNotifyLauncher}，
     *      而 QCL 侧根本没有该符号 → 调用即跳到 NULL（SIGSEGV, rip=0，tombstone 实测）；
     *   ② 签名也不对 —— SDLInit 调的是 {@code (SDL, INIT)} 两个 int，而不是 int[]。
     */
    public static void nativeNotifyLauncher(int type, int... action) {
        notifyLauncher(type, action);
    }

    /**
     * ★★★★★ 2026-10-12 新增（移植自 FCL 的 `CallbackBridge.resetInputState`）：
     * **复位输入状态** —— 修用户实测的「打开一次 ESC/背包后卡在鼠标模式」。
     *
     * <p>FCL 会在两个时机调用它（`JVMActivity`）：
     * <pre>
     *   onDestroy()                        → 退出游戏时
     *   onWindowFocusChanged(false)        → **窗口失焦时**
     * </pre>
     * QCL **两个都没有**，于是：
     * <ul>
     *   <li>SDL3 那条路我新加的 `sMouseButtonState`（按键位掩码累积）会残留 ——
     *       按下过一次左键后若没收到抬起事件，掩码一直是"按着"；</li>
     *   <li>`holdingShift/Ctrl/Alt` 等修饰键状态会残留；</li>
     *   <li>切菜单/切窗口时游戏侧与启动器侧的按下状态就此错位。</li>
     * </ul>
     * 这正是"进了鼠标模式就出不回来、视角跟着点的地方跑、方向轮盘消失"的成因之一。
     *
     * <p>做法与 FCL 一致：先让原生层复位，再把 SDL 侧还没抬起的按键补一个 ACTION_UP
     * （否则 SDL 会认为鼠标键一直按着），最后清掉 Java 侧所有残留状态。
     */
    public static void resetInputState() {
        try {
            nativeResetInputState();
        } catch (Throwable ignored) {
        }
        if (org.libsdl.app.SdlBridge.getSdlEnabled() && sMouseButtonState != 0) {
            try {
                SDLActivity.onNativeMouse(0, MotionEvent.ACTION_UP, mouseX, mouseY, false);
            } catch (Throwable ignored) {
            }
        }
        sMouseButtonState = 0;
        holdingAlt = false;
        holdingCapslock = false;
        holdingCtrl = false;
        holdingNumlock = false;
        holdingShift = false;
    }

    /**
     * ★★★ 1.1.1（移植自 FCL）：重置 SDL 相关状态，由 SdlBridge.reset() 调用。
     *
     * <p>★ 2026-10-12 修复：原来这里是**空实现**（注释写"暂无手柄直通/增量字段"），
     * 但 FCL 的同名方法会清 `sMouseButtonState`。QCL 现在也有这个字段了（SDL 双路要用），
     * 所以必须一起清 —— 否则退出/重进游戏时按键状态会跨会话残留。
     */
    public static void clearSdlBridgeState() {
        sMouseButtonState = 0;
        holdingAlt = false;
        holdingCapslock = false;
        holdingCtrl = false;
        holdingNumlock = false;
        holdingShift = false;
    }

    /** ★ 2026-10-12：原生侧复位输入（对齐 FCL 的 nativeResetInputState）。 */
    private static native void nativeResetInputState();

    /** 由 native 层（input_bridge_v3.c 的 nativeNotifyLauncher）回调：加载 SDL3 并绑定 surface。 */
    @SuppressWarnings("unused")
    public static boolean notifyLauncher(int type, int... action) {
        if (action == null || action.length == 0) {
            return false;
        }
        if (type == NOTIF_TYPE_SDL && action[0] == ACTION_INIT_LAUNCHER_INTEGRATION) {
            boolean first = org.libsdl.app.SdlBridge.markSdlInitialized();
            if (!first) {
                return true;
            }
            try {
                System.loadLibrary("SDL3");
                // ★★★ 1.1.3：这里**不要**再 System.loadLibrary("SDL2")。
                //   QCL 只发布 SDL3（APK 里没有 libSDL2.so），而 loadLibrary 会去 java.library.path /
                //   系统库路径搜索；在 MuMu 上会命中系统里那份与设备不匹配的 libSDL2.so，
                //   由 houdini 转译加载 → 在它的 C++ 静态构造里崩（实测：pthread_mutex_lock(NULL)，
                //   arm64 libc++ iostream 初始化）。这一句是 26.3 启动崩溃的直接触发点，已删除。
                //   （FCL 保留它是因为 FCL 还支持 SDL2 版本；QCL 无此需求。）
                org.libsdl.app.SdlBridge.setupJNI();
                org.libsdl.app.SdlBridge.setSdlEnabled(true);
                org.libsdl.app.SDLSurface surface = org.libsdl.app.SDLActivity.getSDLSurface();
                if (surface != null) {
                    surface.surfaceChanged();
                    if (windowWidth > 0 && windowHeight > 0) {
                        surface.nativeResize(windowWidth, windowHeight);
                    }
                }
                return true;
            } catch (Throwable e) {
                e.printStackTrace();
                org.libsdl.app.SdlBridge.setSdlEnabled(false);
                org.libsdl.app.SdlBridge.clearSdlInitialized();
                return false;
            }
        }
        return false;
    }

    // ==================== ★ 2026-10-12 手柄直通（照 FCL 补齐） ====================
    //
    //  全量对照 FCL 时发现的漏抄项：FCL 的 CallbackBridge 有这两个 buffer，
    //  指向原生侧 `pojav_environ->gamepadState`（QCL 的 environ.h 里**早就有这个字段**，
    //  只是没人用）⇒ 游戏侧直接读写这块内存即可拿到手柄状态，零 JNI 拷贝开销。
    //
    //  GLFWgamepadstate 布局（environ.h:33-36）：
    //      unsigned char buttons[15];
    //      float         axes[6];
    //
    //  ★ 顺手把 `nativeResetInputState` 的声明放在这里（见上面的 resetInputState）。

    /** 手柄按键状态（15 字节，直接映射 GLFWgamepadstate.buttons）。 */
    public static final java.nio.ByteBuffer sGamepadButtonBuffer;

    /** 手柄轴状态（6 个 float，小端，直接映射 GLFWgamepadstate.axes）。 */
    public static final java.nio.FloatBuffer sGamepadAxisBuffer;

    private static native java.nio.ByteBuffer nativeCreateGamepadButtonBuffer();

    private static native java.nio.ByteBuffer nativeCreateGamepadAxisBuffer();

    static {
        DEBUG_STRING = new StringBuilder();
        System.loadLibrary("pojavexec");
        // ★ 这两个 buffer 必须在这里初始化：游戏侧一加载 CallbackBridge 就要能读到，
        //   晚一步初始化会出现"手柄状态全 0"的窗口期。
        java.nio.ByteBuffer btn = null;
        java.nio.FloatBuffer ax = null;
        try {
            btn = nativeCreateGamepadButtonBuffer();
            ax = nativeCreateGamepadAxisBuffer()
                    .order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        } catch (Throwable ignored) {
            // 原生还没就绪（理论上不会；兜底避免类初始化失败把游戏拖崩）
        }
        sGamepadButtonBuffer = btn;
        sGamepadAxisBuffer = ax;
    }
}

