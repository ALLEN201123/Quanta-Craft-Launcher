//
// Created by maks on 21.09.2022.
//
#include <stdlib.h>
#include <dlfcn.h>
#include <android/log.h>
#include <string.h>
#include "egl_loader.h"
#include "loader_dlopen.h"

// 逃逸命名空间：定义于此、经 global_state.h 声明，由 loader_dlopen 兜底 dlopen 失败
struct android_namespace_t* app_escapeNs;

EGLBoolean (*eglMakeCurrent_p) (EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
EGLBoolean (*eglDestroyContext_p) (EGLDisplay dpy, EGLContext ctx);
EGLBoolean (*eglDestroySurface_p) (EGLDisplay dpy, EGLSurface surface);
EGLBoolean (*eglTerminate_p) (EGLDisplay dpy);
EGLBoolean (*eglReleaseThread_p) (void);
EGLContext (*eglGetCurrentContext_p) (void);
EGLDisplay (*eglGetDisplay_p) (NativeDisplayType display);
EGLBoolean (*eglInitialize_p) (EGLDisplay dpy, EGLint *major, EGLint *minor);
EGLBoolean (*eglChooseConfig_p) (EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config);
EGLBoolean (*eglGetConfigAttrib_p) (EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value);
EGLBoolean (*eglBindAPI_p) (EGLenum api);
EGLSurface (*eglCreatePbufferSurface_p) (EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list);
EGLSurface (*eglCreateWindowSurface_p) (EGLDisplay dpy, EGLConfig config, NativeWindowType window, const EGLint *attrib_list);
EGLBoolean (*eglSwapBuffers_p) (EGLDisplay dpy, EGLSurface draw);
EGLint (*eglGetError_p) (void);
EGLContext (*eglCreateContext_p) (EGLDisplay dpy, EGLConfig config, EGLContext share_list, const EGLint *attrib_list);
EGLBoolean (*eglSwapInterval_p) (EGLDisplay dpy, EGLint interval);
EGLSurface (*eglGetCurrentSurface_p) (EGLint readdraw);
EGLBoolean (*eglQuerySurface_p)( 	EGLDisplay display,
                                    EGLSurface surface,
                                    EGLint attribute,
                                    EGLint * value);
__eglMustCastToProperFunctionPointerType (*eglGetProcAddress_p) (const char *procname);

// ★★★★★ 1.6.0：SFPEW 包装生命周期入口（照 FCL 1.3.4.4）。
// 非 SFPEW 渲染器保持 NULL —— bridge_* 系列据此回落系统 EGL。
EGLBoolean (*sfpew_eglMakeCurrent_p) (EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
EGLBoolean (*sfpew_eglSwapBuffers_p) (EGLDisplay dpy, EGLSurface draw);
EGLContext (*sfpew_eglCreateContext_p) (EGLDisplay dpy, EGLConfig config, EGLContext share_list, const EGLint *attrib_list);
EGLBoolean (*sfpew_eglDestroyContext_p) (EGLDisplay dpy, EGLContext ctx);

bool dlsym_EGL() {
    char* gles = getenv("LIBGL_GLES");
    char* eglName = (strncmp(gles ? gles : "", "libGLESv2_angle.so", 18) == 0) ? "libEGL_angle.so" : getenv("POJAVEXEC_EGL");
    // 普通 dlopen 失败时 loader_dlopen 内部会改用逃逸命名空间重试（mesa 等随驱动一起打包的库）
    __android_log_print(ANDROID_LOG_ERROR, "QCL_EGL", "dlsym_EGL: eglName=%s POJAVEXEC_EGL=%s",
                        eglName ? eglName : "(null)", getenv("POJAVEXEC_EGL") ? getenv("POJAVEXEC_EGL") : "(null)");
    void* dl_handle = loader_dlopen(eglName,"libEGL.so", RTLD_LOCAL|RTLD_LAZY);
    __android_log_print(ANDROID_LOG_ERROR, "QCL_EGL", "dlsym_EGL: dl_handle=%p err=%s", dl_handle, dlerror());
    if(dl_handle == NULL) return false;
    eglGetProcAddress_p = dlsym(dl_handle, "eglGetProcAddress");
    if(eglGetProcAddress_p == NULL) {
        printf("%s\n", dlerror());
        return false;
    }
    eglBindAPI_p = (void*) eglGetProcAddress_p("eglBindAPI");
    eglChooseConfig_p = (void*) eglGetProcAddress_p("eglChooseConfig");
    eglCreateContext_p = (void*) eglGetProcAddress_p("eglCreateContext");
    eglCreatePbufferSurface_p = (void*) eglGetProcAddress_p("eglCreatePbufferSurface");
    eglCreateWindowSurface_p = (void*) eglGetProcAddress_p("eglCreateWindowSurface");
    eglDestroyContext_p = (void*) eglGetProcAddress_p("eglDestroyContext");
    eglDestroySurface_p = (void*) eglGetProcAddress_p("eglDestroySurface");
    eglGetConfigAttrib_p = (void*) eglGetProcAddress_p("eglGetConfigAttrib");
    eglGetCurrentContext_p = (void*) eglGetProcAddress_p("eglGetCurrentContext");
    eglGetDisplay_p = (void*) eglGetProcAddress_p("eglGetDisplay");
    eglGetError_p = (void*) eglGetProcAddress_p("eglGetError");
    eglInitialize_p = (void*) eglGetProcAddress_p("eglInitialize");
    eglMakeCurrent_p = (void*) eglGetProcAddress_p("eglMakeCurrent");
    eglSwapBuffers_p = (void*) eglGetProcAddress_p("eglSwapBuffers");
    eglReleaseThread_p = (void*) eglGetProcAddress_p("eglReleaseThread");
    eglSwapInterval_p = (void*) eglGetProcAddress_p("eglSwapInterval");
    eglTerminate_p = (void*) eglGetProcAddress_p("eglTerminate");
    eglGetCurrentSurface_p = (void*) eglGetProcAddress_p("eglGetCurrentSurface");
    eglQuerySurface_p = (void*) eglGetProcAddress_p("eglQuerySurface");
    // ★ 1.1.0 诊断：打印关键符号地址（eglGetProcAddress 可能对某些符号返回 NULL，
    // 原版不检查 -> gl_init 调用 NULL 函数指针 -> SIGSEGV fault addr 0x0）
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "EGL handle=%p syms: getProcAddr=%p getDisplay=%p init=%p chooseConfig=%p createContext=%p createWindowSurface=%p makeCurrent=%p swapBuffers=%p",
           dl_handle, eglGetProcAddress_p, eglGetDisplay_p, eglInitialize_p,
           eglChooseConfig_p, eglCreateContext_p, eglCreateWindowSurface_p,
           eglMakeCurrent_p, eglSwapBuffers_p);
    if (eglGetDisplay_p == NULL || eglInitialize_p == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "FATAL - essential EGL symbols missing (getDisplay=%p init=%p)",
               eglGetDisplay_p, eglInitialize_p);
        return false;
    }

    // ★★★★★ 1.6.0：**SFPEW 兼容层**（照 FCL 1.3.4.4 ctxbridges/egl_loader.c）。
    //
    //   这是「给老版本补固定管线」真正生效的地方 —— 之前只在 Java 层注入 env 是没用的，
    //   因为 native 侧不认那些变量，包装层根本没被 dlopen 过，游戏仍走原路径 → 黑屏。
    //
    //   做什么：
    //     · 从 libSimpleFPEWrapper.so 解析包装生命周期入口（makeCurrent / swapBuffers /
    //       createContext / destroyContext）；
    //     · gl_bridge / egl_bridge 里的 bridge_* 函数据此**优先转发给包装层**，
    //       包装层记录线程本地上下文、并在 swap 前清空立即模式批次；
    //     · 于是游戏的固定管线（glBegin/glEnd、glMatrixMode）被翻译成 GLES 调用，
    //       后端（MobileGlues 等）只承担 EGL 与驱动。
    //
    //   失败处理（与 FCL 一致，绝不硬崩）：
    //     · 包装库 dlopen 失败 / 缺 eglGetProcAddress → 全部保持 NULL，回落系统 EGL；
    //     · 生命周期入口（makeCurrent、swapBuffers）缺失 → 整体回落，避免半接管；
    //     · 建/毁上下文入口缺失 → 只有它们回落底座，避免「包装层建、底座毁」的所有权错配。
    if (getenv("SFPEW_LAYER") != NULL) {
        void* wrapper = dlopen("libSimpleFPEWrapper.so", RTLD_NOW | RTLD_LOCAL);
        void* (*sfpew_get_proc)(const char* char_sym) =
                wrapper != NULL ? (void* (*)(const char*)) dlsym(wrapper, "eglGetProcAddress") : NULL;
        __android_log_print(ANDROID_LOG_ERROR, "QCL_SFPEW",
                "SFPEW_LAYER set: wrapper=%p get_proc=%p err=%s", wrapper, sfpew_get_proc, dlerror());
        if (sfpew_get_proc != NULL) {
            sfpew_eglMakeCurrent_p = (void*) sfpew_get_proc("eglMakeCurrent");
            sfpew_eglSwapBuffers_p = (void*) sfpew_get_proc("eglSwapBuffers");
        }
        if (sfpew_eglMakeCurrent_p == NULL || sfpew_eglSwapBuffers_p == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, "QCL_SFPEW",
                    "lifecycle resolution failed (makeCurrent=%p swapBuffers=%p), falling back to system EGL",
                    sfpew_eglMakeCurrent_p, sfpew_eglSwapBuffers_p);
            sfpew_eglMakeCurrent_p = NULL;
            sfpew_eglSwapBuffers_p = NULL;
        } else {
            // 生命周期入口就绪后才接管建/毁：逐上下文自动开关要求建上下文经包装层解析
            sfpew_eglCreateContext_p = (void*) sfpew_get_proc("eglCreateContext");
            sfpew_eglDestroyContext_p = (void*) sfpew_get_proc("eglDestroyContext");
            if (sfpew_eglCreateContext_p == NULL || sfpew_eglDestroyContext_p == NULL) {
                __android_log_print(ANDROID_LOG_ERROR, "QCL_SFPEW",
                        "context entry points unavailable, context creation stays on base EGL");
                sfpew_eglCreateContext_p = NULL;
                sfpew_eglDestroyContext_p = NULL;
            } else {
                __android_log_print(ANDROID_LOG_ERROR, "QCL_SFPEW",
                        "layer ACTIVE: makeCurrent=%p swapBuffers=%p createContext=%p destroyContext=%p",
                        sfpew_eglMakeCurrent_p, sfpew_eglSwapBuffers_p,
                        sfpew_eglCreateContext_p, sfpew_eglDestroyContext_p);
            }
        }
    }
    return true;
}

// 供 glxshim 等外部库转发 eglGetProcAddress
__attribute__((visibility("default")))
void *getProcAddress(const char *procname) {
    // SDL 路径不经过 pojavInitOpenGL，首次调用时补齐 EGL 符号解析
    if (eglGetProcAddress_p == NULL && !dlsym_EGL()) return NULL;
    return eglGetProcAddress_p(procname);
}
