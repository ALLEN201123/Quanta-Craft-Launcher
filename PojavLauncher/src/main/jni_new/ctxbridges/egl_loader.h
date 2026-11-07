//
// Created by maks on 21.09.2022.
//
#include <EGL/egl.h>
#include <stdbool.h>
#ifndef POJAVLAUNCHER_EGL_LOADER_H
#define POJAVLAUNCHER_EGL_LOADER_H

extern EGLBoolean (*eglMakeCurrent_p) (EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
extern EGLBoolean (*eglDestroyContext_p) (EGLDisplay dpy, EGLContext ctx);
extern EGLBoolean (*eglDestroySurface_p) (EGLDisplay dpy, EGLSurface surface);
extern EGLBoolean (*eglTerminate_p) (EGLDisplay dpy);
extern EGLBoolean (*eglReleaseThread_p) (void);
extern EGLContext (*eglGetCurrentContext_p) (void);
extern EGLDisplay (*eglGetDisplay_p) (NativeDisplayType display);
extern EGLBoolean (*eglInitialize_p) (EGLDisplay dpy, EGLint *major, EGLint *minor);
extern EGLBoolean (*eglChooseConfig_p) (EGLDisplay dpy, const EGLint *attrib_list, EGLConfig *configs, EGLint config_size, EGLint *num_config);
extern EGLBoolean (*eglGetConfigAttrib_p) (EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value);
extern EGLBoolean (*eglBindAPI_p) (EGLenum api);
extern EGLSurface (*eglCreatePbufferSurface_p) (EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list);
extern EGLSurface (*eglCreateWindowSurface_p) (EGLDisplay dpy, EGLConfig config, NativeWindowType window, const EGLint *attrib_list);
extern EGLBoolean (*eglSwapBuffers_p) (EGLDisplay dpy, EGLSurface draw);
extern EGLint (*eglGetError_p) (void);
extern EGLContext (*eglCreateContext_p) (EGLDisplay dpy, EGLConfig config, EGLContext share_list, const EGLint *attrib_list);
extern EGLBoolean (*eglSwapInterval_p) (EGLDisplay dpy, EGLint interval);
extern EGLSurface (*eglGetCurrentSurface_p) (EGLint readdraw);
extern __eglMustCastToProperFunctionPointerType (*eglGetProcAddress_p) (const char *procname);
extern EGLBoolean (*eglQuerySurface_p)( 	EGLDisplay display,
                                           EGLSurface surface,
                                           EGLint attribute,
                                           EGLint * value);

// ★★★★★ 1.6.0：SFPEW 兼容层（SFPEW_LAYER=1 时启用）：从 libSimpleFPEWrapper.so 解析的
// 包装生命周期入口。非 SFPEW 渲染器保持 NULL，调用方回落系统 EGL。
// 作用：包装层记录线程本地上下文（供 SFPEW_RELAXED_CONTEXT 的严格/宽松模式判定），
// 并在 eglSwapBuffers 前清空立即模式批次 —— 这就是「给老版本补固定管线」的落地点。
// 照 FCL 1.3.4.4 ctxbridges/egl_loader.h。
extern EGLBoolean (*sfpew_eglMakeCurrent_p) (EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx);
extern EGLBoolean (*sfpew_eglSwapBuffers_p) (EGLDisplay dpy, EGLSurface draw);
extern EGLContext (*sfpew_eglCreateContext_p) (EGLDisplay dpy, EGLConfig config, EGLContext share_list, const EGLint *attrib_list);
extern EGLBoolean (*sfpew_eglDestroyContext_p) (EGLDisplay dpy, EGLContext ctx);

bool dlsym_EGL();
void* getProcAddress(const char* procname);

#endif //POJAVLAUNCHER_EGL_LOADER_H
