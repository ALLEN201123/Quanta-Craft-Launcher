//
// 1.1.3：LWJGL dlopen 钩子（移植自 FCL 的 jvm_hooks/lwjgl_dlopen_hook.c）
//
// 背景：QCL 之前**完全没有**这个模块（FCL 有），导致 26.3 走到图形后端初始化时报
//   `com.mojang.renderpearl.api.device.BackendCreationException:
//      OpenGL is not supported: Could not retrieve EGL function eglGetDisplay`
//
// 原因：MC 的 GL 后端用 LWJGL 的 org.lwjgl.system.linux.DynamicLinkLoader.ndlopen 去加载
//       libEGL.so / 渲染器库等「不在应用私有 native 目录」的库。LWJGL 自带的 ndlopen 实现在
//       liblwjgl.so 里，受 Android linker namespace 限制，拿不到这些库的句柄 →
//       dlsym(handle=NULL, "eglGetDisplay") 自然失败。
//
// 做法（与 FCL 一致）：把该 native 方法重定向到 libpojavexec.so 里的实现 ——
//       libpojavexec 处在应用 classloader 的 linker namespace 中，dlopen 能找到系统库。
//
#include <dlfcn.h>
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "log.h"

/**
 * 与 FCL 的 ndlopen_bugfix 等价（QCL 不需要 FCL 专有的 RENDERER_HANDLE / vulkan 分支，
 * QCL 的 Vulkan 由 OSMDroid 路径自行加载）。
 *
 * 这里同时打日志：能直接看出「哪个库、用什么 mode、dlopen 成功还是失败」，
 * 是定位图形后端 EGL/GL 加载问题的第一手证据。
 */
static jlong ndlopen_bugfix(JNIEnv *env, jclass clazz, jlong filename_ptr, jint jmode) {
    const char *filename = (const char *) (intptr_t) filename_ptr;
    int mode = (int) jmode;
    void *handle = NULL;
    if (filename != NULL) {
        dlerror();
        handle = dlopen(filename, mode);
        if (handle == NULL) {
            FCL_LOG("LWJGL ndlopen hook: dlopen(\"%s\", 0x%x) 失败: %s", filename, mode, dlerror());
        } else {
            FCL_LOG("LWJGL ndlopen hook: dlopen(\"%s\", 0x%x) -> %p", filename, mode, handle);
        }
    }
    return (jlong) (intptr_t) handle;
}

/**
 * 安装 LWJGL dlopen 钩子。必须在**游戏 JVM** 里调用（env 为该 JVM 的 env）。
 */
void installLwjglDlopenHook(JNIEnv *env) {
    if (env == NULL) return;
    jclass dynamicLinkLoader = (*env)->FindClass(env, "org/lwjgl/system/linux/DynamicLinkLoader");
    if (dynamicLinkLoader == NULL) {
        FCL_LOG("LWJGL ndlopen hook: 找不到 org/lwjgl/system/linux/DynamicLinkLoader");
        (*env)->ExceptionClear(env);
        return;
    }
    JNINativeMethod methods[] = {
            {"ndlopen", "(JI)J", (void *) &ndlopen_bugfix}
    };
    if ((*env)->RegisterNatives(env, dynamicLinkLoader, methods, 1) != 0) {
        FCL_LOG("LWJGL ndlopen hook: RegisterNatives 失败");
        (*env)->ExceptionClear(env);
        return;
    }
    FCL_LOG("LWJGL ndlopen hook: 已安装（ndlopen 现在走 libpojavexec 的 namespace）");
}
