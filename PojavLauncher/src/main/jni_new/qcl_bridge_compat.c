//
// QCL 1.1.0 渲染桥移植的兼容符号层
//
// 背景：1.1.0 把 Pojav 渲染桥从 HMCL-PE 旧版（egl_bridge.c 只支持 GL4ES/OSMesa）
// 移植为 FCL 新版（ctxbridges 模块化渲染桥，支持 Mesa zink-on-Vulkan 等）。
//
// FCL 的新渲染桥会引用一些 QCL 旧后端没有的符号。本文件集中提供这些符号的
// 安全实现（stub / 适配），避免直接污染 QCL 原有 native 文件。
//

#include "egl_bridge.h"
#include "environ/environ.h"
#include <android/log.h>

// FCL 的新 egl_bridge.c 在 pojavInit() 里调用 updateMonitorSize() 上报
// monitor 尺寸到 Java 侧 GLFW（FCL 的 CallbackBridge 有 internalChangeMonitorSize）。
//
// ★★★ 2026-09-17 修复（"画面缩在左下角/没全屏"的根因）：
// 此处原本是**空实现**，(void)width; (void)height; 直接把尺寸丢掉。
// 但 internalChangeMonitorSize 是 GLFW stub 里**唯一给 mGLFWWindowWidth/mGLFWWindowHeight
// 赋值的通路**（GLFW.java: internalChangeMonitorSize → mGLFWWindowWidth = width）：
//
//   native pojavInit/pumpEvents → updateMonitorSize(savedWidth, savedHeight)
//       → GLFW.internalChangeMonitorSize(w, h) → mGLFWWindowWidth/Height
//   native → updateWindowSize(window)
//       → GLFW.internalWindowSizeChanged(win)
//           → glfwSetWindowSize(win, mGLFWWindowWidth, mGLFWWindowHeight)
//           → FramebufferSize / WindowSize 回调 → Minecraft 据此算 viewport
//
// QCL 里 updateWindowSize 的通路在（input_bridge_v3.c:510），但 upst ream 尺寸
// 因为这里被吞掉而恒为初始值 0 → 游戏算出的 framebuffer 尺寸错误
// → 画面只占左下角一小块、且 glViewport 与实际 Surface 不匹配。
// 现改为与 FCL 完全一致的转发实现。
void updateMonitorSize(int width, int height) {
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                        "updateMonitorSize(%d,%d): env=%p cls=%p mid=%p",
                        width, height, pojav_environ->glfwThreadVmEnv,
                        pojav_environ->vmGlfwClass,
                        pojav_environ->method_internalChangeMonitorSize);
    if (pojav_environ->glfwThreadVmEnv == NULL) return;
    if (pojav_environ->vmGlfwClass == NULL) return;
    // ★ method_internalChangeMonitorSize 由 nativeInitializeGLFWNativeBridge 赋值。
    //   若 Java 侧尚未跑到那一步（或 GetStaticMethodID 失败留下 pending exception），
    //   这里就是 NULL，直接 CallStaticVoidMethod(NULL, ...) 会把 GL 线程打死。
    //   宁可跳过这次尺寸上报（后续 pumpEvents / onSurfaceTextureSizeChanged 还会再报），
    //   也不能让 GL 线程停住 —— 那正是"画面冻结"的表现。
    if (pojav_environ->method_internalChangeMonitorSize == NULL) return;
    (*pojav_environ->glfwThreadVmEnv)->CallStaticVoidMethod(
            pojav_environ->glfwThreadVmEnv, pojav_environ->vmGlfwClass,
            pojav_environ->method_internalChangeMonitorSize, width, height);
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                        "updateMonitorSize(%d,%d): pushed to GLFW", width, height);
    // ★ 清理潜在异常：本函数在 GLThread（pojavInit 内）被调用，若这次 JNI 调用
    //   留下 pending exception 而不清，之后该线程上一切 JNI 调用都会静默失败，
    //   表现就是 GL 线程再也跑不起来（u/stime 停滞、voluntary_ctxt_switches 一直涨）。
    if ((*pojav_environ->glfwThreadVmEnv)->ExceptionCheck(pojav_environ->glfwThreadVmEnv)) {
        (*pojav_environ->glfwThreadVmEnv)->ExceptionDescribe(pojav_environ->glfwThreadVmEnv);
        (*pojav_environ->glfwThreadVmEnv)->ExceptionClear(pojav_environ->glfwThreadVmEnv);
    }
}
