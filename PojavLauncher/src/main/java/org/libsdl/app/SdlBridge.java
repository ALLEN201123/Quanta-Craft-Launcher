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

    public static boolean setNativeTextInputActive(boolean active) {
        nativeTextInputActive = active;
        return true;
    }

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

    /** 游戏是否运行在 SDL 渲染路径。 */
    public static boolean isSdlRenderActive() {
        return sdlEnabled;
    }

    public static void initializeControllerSubsystems() {
    }
}
