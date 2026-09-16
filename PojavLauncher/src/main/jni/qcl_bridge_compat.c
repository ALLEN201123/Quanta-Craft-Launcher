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

// FCL 的新 egl_bridge.c 在 pojavInit() 里调用 updateMonitorSize() 上报
// monitor 尺寸到 Java 侧 GLFW（FCL 的 CallbackBridge 有 internalChangeMonitorSize）。
// QCL 的 LWJGL 3.2.3 CallbackBridge 没有对应方法，这里提供安全空实现：
// 尺寸链路仍由 QCL 原有的 onSurfaceTextureAvailable / onSurfaceTextureSizeChanged
// → CallbackBridge.sendUpdateWindowSize 负责，功能不受影响。
void updateMonitorSize(int width, int height) {
    (void) width;
    (void) height;
}
