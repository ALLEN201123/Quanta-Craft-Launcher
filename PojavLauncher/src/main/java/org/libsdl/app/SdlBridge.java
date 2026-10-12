package org.libsdl.app;

import android.app.Activity;
import android.view.Surface;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * ★★★ 1.1.1：SDL 集成状态桥（移植自 FCL 的 com.tungsten.fcl.game.sdl.SdlBridge，纯 Java 版）。
 *
 * 由「启动器 JVM」和「游戏 JVM」共享：启动器在窗口创建时调用 prepareSurface 绑定 Android Surface，
 * 游戏 JVM 在 SDL_Init 时通过 CallbackBridge.nativeNotifyLauncher → notifyLauncher 触发
 * setupJNI + setSdlEnabled + surfaceChanged。
 *
 * 放在 PojavLauncher 模块（org.libsdl.app 包），这样 org.lwjgl.glfw.CallbackBridge 能直接引用。
 */
public final class SdlBridge {

    private static volatile boolean sdlEnabled = false;
    private static volatile boolean sdlInitialized = false;
    private static volatile boolean jniReady = false;
    private static volatile boolean nativeTextInputActive = false;

    private static WeakReference<Activity> activityRef;
    private static WeakReference<ViewGroup> layoutRef;
    private static Surface currentSurface;
    private static Object currentSource;

    private SdlBridge() {
    }

    public static boolean getSdlEnabled() {
        return sdlEnabled;
    }

    public static void setSdlEnabled(boolean enabled) {
        sdlEnabled = enabled;
        if (!enabled) {
            nativeTextInputActive = false;
        }
    }

    /** SDL 请求唤起输入法时启动器侧是否响应。QCL 目前默认跟随 sdlEnabled。 */
    public static boolean getSdlImeAutoShowEnabled() {
        return sdlEnabled;
    }

    /**
     * ★★★★★ 2026-10-12 补齐（全量对照 FCL 后发现的漏抄项）：
     * **把「是否需要 SDL 文本输入」真正下发给 SDL3**。
     *
     * <p>原来这里只是记一个 Java 布尔值就 `return true`，**根本没调用 SDL** ——
     * 于是 SDL3 的文本输入通道（IME / 软键盘）从来没被打开过，
     * 聊天框、告示牌、命名框都收不到输入法。
     *
     * <p>SDL3 的文本输入 API 要求**在主线程调用**，所以原生侧用
     * `SDL_RunOnMainThread` 投递（照 FCL 的做法），不阻塞等待；
     * 激活结果由 SDL 的 showTextInput 回调异步镜像回 Java。
     */
    public static boolean setNativeTextInputActive(boolean active) {
        nativeTextInputActive = active;
        try {
            return nativeSetNativeTextInputActive(active);
        } catch (Throwable ignored) {
            // 原生不可用（非 SDL 版本 / so 未就绪）⇒ 保持 Java 标记，行为与以前一致
            return true;
        }
    }

    /** ★ 2026-10-12：原生实现（input_bridge_v3.c）。 */
    private static native boolean nativeSetNativeTextInputActive(boolean active);

    /**
     * ★ 2026-10-12：SDL 渲染路径是否真的活着 —— 以**首个 SDL 窗口已创建**为准
     * （FCL 的判据：`sdlHookGetPrimaryWindow() != NULL`）。
     *
     * <p>为什么不能只看 Java 标记：仅手柄子系统初始化 SDL（例如 MC 26.2 挂 Controlify）时
     * 也会走 SDL_Init，但**没有窗口**、也就没有 SDL 渲染路径。
     */
    private static native boolean nativeIsSdlRenderActive();

    public static boolean getNativeTextInputActive() {
        return nativeTextInputActive;
    }

    /** 初始化 SDL 的 JNI 绑定（nativeSetupJNI 等）。 */
    public static synchronized boolean setupJNI() {
        if (jniReady) {
            return true;
        }
        SDL.setupJNI();
        jniReady = true;
        return true;
    }

    /**
     * ★★★ 1.1.3 修复 26.3（SDL3）启动崩溃：由启动器侧（dalvik VM）**提前**完成 libSDL3.so 的首次加载。
     *
     * 为什么必须这么做：
     *  1. libSDL3.so 里**没有任何 `Java_org_libsdl_app_*` 导出符号**（已核实），
     *     SDL 的 Java 端方法（nativeSetupJNI / SDLSurface 等）**全部依赖 JNI_OnLoad 里的 RegisterNatives**。
     *  2. 游戏侧（LWJGL）加载 libSDL3.so 时，`sdl_dlopen_hook.c` 会**故意跳过** JNI_OnLoad 注册
     *     （隔离双 VM 边界，FCL 同款做法），于是 SDL 停在「JNI 未就绪」状态
     *     （实测日志：`W/SDL: Request to get environment variables before JNI is ready`）。
     *  3. 之后 dalvik 侧再走 loadLibrary("SDL3") 触发 JNI_OnLoad 时，SDL 内部已处于脏状态 → SIGSEGV。
     *
     * 让启动器在**游戏 JVM 启动之前**先加载 SDL3，JNI_OnLoad 就会在 dalvik 里干净执行并完成注册；
     * 之后游戏侧再加载时隔离机制生效，SDL 原生全局状态始终指向 dalvik + SDLActivity。FCL 即此顺序。
     */
    public static synchronized void preloadSdl3() {
        try {
            // 先确保 libpojavexec.so 由**启动器侧（dalvik VM）**完成首次加载：
            // JNI_OnLoad 只有在首次加载时才会把 dalvikJavaVMPtr/bridgeClazz/method_notifyLauncher
            // 指向 dalvik；若让游戏 JVM 抢先加载，整套 SDL 通知链就会指向错误的 VM。
            System.loadLibrary("pojavexec");
        } catch (Throwable e) {
        }
        try {
            System.loadLibrary("SDL3");
        } catch (Throwable e) {
        }
    }

    public static synchronized boolean markSdlInitialized() {
        if (sdlInitialized) {
            return false;
        }
        sdlInitialized = true;
        return true;
    }

    public static synchronized void clearSdlInitialized() {
        sdlInitialized = false;
    }

    /** 启动器在窗口 Surface 创建/变化时调用，绑定 SDL 渲染 surface。 */
    public static void prepareSurface(Activity activity, Surface surface, ViewGroup layout, Object source) {
        activityRef = new WeakReference<>(activity);
        layoutRef = layout == null ? null : new WeakReference<>(layout);
        currentSurface = surface;
        currentSource = source;

        if (SDLActivity.getSDLSurface() == null) {
            SDL.initialize();
            SDL.setContext(activity);
            SDLActivity.externalInitialize(new SDLSurface(activity), layout, surface);
        } else {
            SDLSurface.setNativeSurface(surface);
        }
    }

    public static void registerSurface(Activity activity, Surface surface, ViewGroup layout) {
        activityRef = new WeakReference<>(activity);
        layoutRef = layout == null ? null : new WeakReference<>(layout);
        currentSurface = surface;
    }

    public static boolean beginSurfaceDestroy(Object source, Surface surface) {
        return source != null && currentSource == source && surface != null && currentSurface == surface;
    }

    public static void unregisterSurface(Surface surface) {
        if (surface != null && currentSurface == surface) {
            currentSurface = null;
            currentSource = null;
        }
    }

    public static synchronized void reset() {
        currentSurface = null;
        currentSource = null;
        activityRef = null;
        layoutRef = null;
        jniReady = false;
        sdlInitialized = false;
        sdlEnabled = false;
        nativeTextInputActive = false;
        org.lwjgl.glfw.CallbackBridge.clearSdlBridgeState();
        SDLSurface.clearNativeSurface();
    }

    /**
     * 游戏是否运行在 SDL 渲染路径。
     *
     * <p>★ 2026-10-12 改进（照 FCL）：以原生侧的**首个 SDL 窗口是否已创建**为准，
     * 而不是只看 Java 标记位。原因：仅手柄子系统初始化 SDL
     * （例如 MC 26.2 挂 Controlify）时也会走 SDL_Init、`sdlEnabled` 也会变 true，
     * 但**没有窗口、也没有 SDL 渲染路径** ⇒ 只看标记会误判。
     */
    public static boolean isSdlRenderActive() {
        try {
            return nativeIsSdlRenderActive();
        } catch (Throwable ignored) {
            return sdlEnabled;   // 原生不可用就退回原判据
        }
    }

    /**
     * ★ 2026-10-12（照 FCL）：初始化 SDL 的手柄子系统。
     *
     * <p>QCL 原来这里是**空实现**。MC 26.2+ 挂 Controlify 等手柄 mod 时会请求初始化
     * gamepad/joystick 子系统；不初始化就收不到手柄输入。
     * 具体初始化在原生侧（input_bridge_v3.c），这里只做转发。
     */
    public static void initializeControllerSubsystems() {
        try {
            nativeInitializeControllerSubsystems();
        } catch (Throwable ignored) {
        }
    }

    /** ★ 2026-10-12：原生实现（input_bridge_v3.c）。 */
    private static native void nativeInitializeControllerSubsystems();
}
