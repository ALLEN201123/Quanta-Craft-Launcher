//
// ★★★ 1.1.1：native_hooks 的 QCL 适配头 —— 替代 FCL 的 "fcl/fcl_internal.h"。
//
// FCL 的 exit_hook.c 依赖两样东西：
//   · _Noreturn void nominal_exit(int code)        —— 游戏退出时通知启动器（弹 ExitActivity）
//   · int android_get_device_api_level(void)       —— 判定是否需要在低于 API 29 上挂载 chmod hook
// 这里给出 QCL 版本：nominal_exit 由 QCL 自己的 qcl_nominal_exit.c 实现（反射调用 Java 静态方法
// QCLHooks.onExit(code)），api level 直接用 NDK 的 <android/api-level.h>。
// FCL_LOG / FCL_INTERNAL_LOG 两个宏沿用 QCL 的 jni_new/log.h（已映射成 __android_log_print）。

#ifndef QCL_NATIVE_HOOKS_QCL_INTERNAL_H
#define QCL_NATIVE_HOOKS_QCL_INTERNAL_H

#include <jni.h>
#include <android/api-level.h>
#include <android/log.h>
#include <stdio.h>
#include <stdlib.h>
#include "log.h"

_Noreturn void nominal_exit(int code);

#endif // QCL_NATIVE_HOOKS_QCL_INTERNAL_H
