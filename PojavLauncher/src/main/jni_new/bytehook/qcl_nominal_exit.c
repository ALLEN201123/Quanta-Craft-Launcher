//
// ★★★ 1.1.1：QCL 版的 nominal_exit —— 替代 FCL 的 fcl/fcl_loader.c 里同名实现。
//
// 本文件编进 libpojavexec.so（与 native_hooks 同库），所以能直接读 pojav_environ 与
// get_attached_env()。游戏调用 exit() 后，exit_hook.c 的 custom_atexit 会调用这里，
// 反射调用 Java 静态方法 QCLHooks.onExit(int) 通知启动器弹出退出界面，然后终止进程。
#include "qcl_internal.h"
#include <jni.h>
#include <signal.h>
#include <unistd.h>
#include <android/log.h>
#include "environ/environ.h"
#include "utils.h"

static void hard_kill(void) {
    killpg(getpgrp(), SIGTERM);
    _exit(1);
}

_Noreturn void nominal_exit(int code) {
    JavaVM *jvm = (pojav_environ != NULL) ? pojav_environ->runtimeJavaVMPtr : NULL;
    if (jvm == NULL && pojav_environ != NULL) jvm = pojav_environ->dalvikJavaVMPtr;
    if (jvm == NULL) {
        __android_log_print(ANDROID_LOG_WARN, "qclhooks", "nominal_exit(%d): no JavaVM", code);
        hard_kill();
    }
    JNIEnv *env = get_attached_env(jvm);
    if (env == NULL) {
        __android_log_print(ANDROID_LOG_WARN, "qclhooks", "nominal_exit(%d): attach failed", code);
        hard_kill();
    }
    jclass cls = (*env)->FindClass(env, "com/qcl/launcher/launcher/launch/QCLHooks");
    if (cls == NULL) {
        (*env)->ExceptionClear(env);
        __android_log_print(ANDROID_LOG_WARN, "qclhooks", "nominal_exit(%d): QCLHooks not found", code);
        hard_kill();
    }
    jmethodID mid = (*env)->GetStaticMethodID(env, cls, "onExit", "(I)V");
    if (mid == NULL) {
        (*env)->ExceptionClear(env);
        __android_log_print(ANDROID_LOG_WARN, "qclhooks", "nominal_exit(%d): onExit not found", code);
        hard_kill();
    }
    (*env)->CallStaticVoidMethod(env, cls, mid, code);
    hard_kill();
}
