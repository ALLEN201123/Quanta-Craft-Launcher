package org.lwjgl.glfw;

import android.view.Choreographer;

public class CallbackBridge {
    public static Choreographer sChoreographer = Choreographer.getInstance();
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

    /** ★★★ 1.1.1（移植自 FCL）：重置 SDL 相关状态，由 SdlBridge.reset() 调用。
     *  QCL 的 CallbackBridge 暂无手柄直通/增量字段，先留空实现（与 FCL 语义对齐）。 */
    public static void clearSdlBridgeState() {
    }

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

    static {
        DEBUG_STRING = new StringBuilder();
        System.loadLibrary("pojavexec");
    }
}

