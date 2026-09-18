package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qcl.launcher.launcher.QCLApplication;

/**
 * ★★★ 1.1.1：SDL3 支持的 native hook 入口（移植自 FCL 的 FCLBridge.initializeHooks）。
 *
 * 为什么需要这套 hook：
 *  MC 26.x 起部分版本改用 SDL3 作为窗口/输入后端（版本 json 里是 lwjgl-sdl 而不是 lwjgl-glfw）。
 *  FCL 的做法是用 bytehook 在游戏 JVM 启动时把 SDL 的初始化、窗口复用、EGL 兼容、
 *  以及 dlopen("libSDL3.so") 全部挂钩（native_hooks/sdl_hook.c、sdl_dlopen_hook.c），
 *  另外用 exit hook 捕获游戏的 exit() 退出码，好让启动器显示退出界面。
 *
 * 只在版本确实需要 SDL 时才加载，普通 GLFW 版本不产生额外开销。
 */
public final class QCLHooks {

    private static final String TAG = "QCLHooks";
    private static boolean sLoaded = false;

    private QCLHooks() {
    }

    /** 加载并初始化 native hooks；失败不抛异常，只记录日志（最坏情况退回原来的行为）。 */
    public static synchronized void initializeHooks() {
        try {
            if (!sLoaded) {
                System.loadLibrary("pojavexec");
                sLoaded = true;
            }
            nativeInitializeHooks();
        } catch (Throwable e) {
            Log.e(TAG, "initializeHooks failed", e);
        }
    }

    /** 由 native 层在游戏调用 exit() 后回调（见 native_hooks/exit_hook.c → nominal_exit）。 */
    public static void onExit(int code) {
        try {
            Context ctx = QCLApplication.getContext();
            if (ctx != null) {
                Intent intent = new Intent(ctx, ExitActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("exit_code", code);
                ctx.startActivity(intent);
            }
        } catch (Throwable ignored) {
        }
    }

    private static native void nativeInitializeHooks();
}
