/*
 * V3 input bridge implementation.
 *
 * Status:
 * - Active development
 * - Works with some bugs:
 *  + Modded versions gives broken stuff..
 *
 * 
 * - Implements glfwSetCursorPos() to handle grab camera pos correctly.
 */
 
#include <assert.h>
#include <dlfcn.h>
#include <jni.h>
#include <android/log.h>
#include <libgen.h>
#include <stdlib.h>
#include <string.h>
#include <stdatomic.h>
#include <math.h>

#include "log.h"
#include "utils.h"
#include "environ/environ.h"
#include "native_hooks/native_hooks.h"

extern void updateMonitorSize(int width, int height);

#define EVENT_TYPE_CHAR 1000
#define EVENT_TYPE_CHAR_MODS 1001
#define EVENT_TYPE_CURSOR_ENTER 1002
#define EVENT_TYPE_CURSOR_POS 1003
#define EVENT_TYPE_FRAMEBUFFER_SIZE 1004
#define EVENT_TYPE_KEY 1005
#define EVENT_TYPE_MOUSE_BUTTON 1006
#define EVENT_TYPE_SCROLL 1007
#define EVENT_TYPE_WINDOW_SIZE 1008

jint (*orig_ProcessImpl_forkAndExec)(JNIEnv *env, jobject process, jint mode, jbyteArray helperpath, jbyteArray prog, jbyteArray argBlock, jint argc, jbyteArray envBlock, jint envc, jbyteArray dir, jintArray std_fds, jboolean redirectErrorStream);

typedef void GLFW_invoke_Char_func(void* window, unsigned int codepoint);
typedef void GLFW_invoke_CharMods_func(void* window, unsigned int codepoint, int mods);
typedef void GLFW_invoke_CursorEnter_func(void* window, int entered);
typedef void GLFW_invoke_CursorPos_func(void* window, double xpos, double ypos);
typedef void GLFW_invoke_FramebufferSize_func(void* window, int width, int height);
typedef void GLFW_invoke_Key_func(void* window, int key, int scancode, int action, int mods);
typedef void GLFW_invoke_MouseButton_func(void* window, int button, int action, int mods);
typedef void GLFW_invoke_Scroll_func(void* window, double xoffset, double yoffset);
typedef void GLFW_invoke_WindowSize_func(void* window, int width, int height);

static float grabCursorX, grabCursorY, lastCursorX, lastCursorY;

jclass inputBridgeClass_ANDROID, inputBridgeClass_JRE;
jmethodID inputBridgeMethod_ANDROID, inputBridgeMethod_JRE;
jclass bridgeClazz;
// ★★★ 2026-09-18：isGrabbing 原来在**本文件**里又定义了一份（与 environ.h 结构体的
// `jboolean isGrabbing` 形成又一套「两套存储」）。FCL 只用 pojav_environ->isGrabbing，
// 这里照 FCL 删掉本文件那份，全链路统一读结构体字段。
// （grabCursor* / lastCursor* 是 QCL 触屏抓取独有，FCL 没有，继续保留在本文件。）

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeNotifyLauncher(JNIEnv* env, __attribute__((unused)) jclass clazz, jint type, jintArray action) {
    TRY_ATTACH_ENV(dvm_env, pojav_environ->dalvikJavaVMPtr, "nativeNotifyLauncher failed!\n", return JNI_FALSE;);
    // ★ 1.1.3 热修：解析不到就现场补解析（实现在 utils.c），避免 SDL 集成被静默跳过。
    ensureNotifyLauncher(dvm_env);
        if (pojav_environ->method_notifyLauncher == NULL || pojav_environ->bridgeClazz == NULL) {
                return JNI_FALSE;
    }
    jintArray converted = convertIntArrayJVM(env, dvm_env, action);
    jboolean result = (*dvm_env)->CallStaticBooleanMethod(dvm_env, pojav_environ->bridgeClazz,
                                                          pojav_environ->method_notifyLauncher, type, converted);
    if ((*dvm_env)->ExceptionCheck(dvm_env)) {
        (*dvm_env)->ExceptionDescribe(dvm_env);
        (*dvm_env)->ExceptionClear(dvm_env);
        return JNI_FALSE;
    }
        return result;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    if (dalvikJavaVMPtr == NULL) {
        //Save dalvik global JavaVM pointer
        dalvikJavaVMPtr = vm;
        // ★ 同步写进 pojav_environ（FCL 全链路读的是结构体字段，utils.h 的 static 是
        //   Pojav 旧桥遗留）。两边保持一致，避免将来某处读结构体那份拿到 NULL。
        pojav_environ->dalvikJavaVMPtr = vm;
        (*vm)->GetEnv(vm, (void**) &dalvikJNIEnvPtr_ANDROID, JNI_VERSION_1_4);
        bridgeClazz = (*dalvikJNIEnvPtr_ANDROID)->NewGlobalRef(dalvikJNIEnvPtr_ANDROID,(*dalvikJNIEnvPtr_ANDROID) ->FindClass(dalvikJNIEnvPtr_ANDROID,"org/lwjgl/glfw/CallbackBridge"));
        assert(bridgeClazz != NULL);
        pojav_environ->bridgeClazz = bridgeClazz;
        pojav_environ->method_notifyLauncher = (*dalvikJNIEnvPtr_ANDROID)->GetStaticMethodID(
                dalvikJNIEnvPtr_ANDROID, bridgeClazz, "notifyLauncher", "(I[I)Z");
        pojav_environ->isUseStackQueueCall = JNI_FALSE;
    } else if (dalvikJavaVMPtr != vm) {
        runtimeJavaVMPtr = vm;
        pojav_environ->runtimeJavaVMPtr = vm;
        (*vm)->GetEnv(vm, (void**) &runtimeJNIEnvPtr_JRE, JNI_VERSION_1_4);
        hookExec();
        // ★ 1.1.3（照搬 FCL）：把 LWJGL 的 ndlopen 重定向到 libpojavexec，绕过 linker namespace 限制。
        //   缺了它，MC 的图形后端拿不到 libEGL.so 句柄 → "Could not retrieve EGL function eglGetDisplay"。
        installLwjglDlopenHook(runtimeJNIEnvPtr_JRE);
    }
    
    pojav_environ->isGrabbing = JNI_FALSE;

    return JNI_VERSION_1_4;
}

// Should be?
void JNI_OnUnload(JavaVM* vm, void* reserved) {
/*
    if (dalvikJavaVMPtr == vm) {
    } else {
    }
    
    DetachCurrentThread(vm);
*/

    //dalvikJNIEnvPtr_JRE = NULL;
    runtimeJNIEnvPtr_ANDROID = NULL;
}

/*
 * 初始化 GLFW/JVM 侧通信所需的方法 ID 与按键缓冲。
 * 由定制版 GLFW.java 的静态块在类加载后调用（GLFW.java:559-560 的
 * System.loadLibrary("pojavexec") + nativeInitializeGLFWNativeBridge()）。
 * GLFW 的加载时机远晚于 pojavexec 被 dlopen，此时 FindClass 才能成功。
 * [1.1.0 移植自 FCL，QCL 原版缺失此函数 -> UnsatisfiedLinkError]
 */
JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_GLFW_nativeInitializeGLFWNativeBridge(__attribute__((unused)) JNIEnv *env,
                                                          __attribute__((unused)) jclass clazz) {
    // ★★★ 1.1.0 修复（2026-09-16 模拟器实测）：
    // 原实现直接解引用 pojav_environ->runtimeJavaVMPtr，但该**结构体字段**从未被赋值
    // （JNI_OnLoad 里设置的是同名的**静态全局变量** runtimeJavaVMPtr），于是这里是 NULL
    // -> (*NULL)->GetEnv 直接 SIGSEGV（实测崩溃地址 = 本函数 +35 字节）。
    // 三层兜底：结构体字段 → 静态全局 → 从传入的 JNIEnv 反查。全拿不到就安全返回。
    JavaVM *qclVm = pojav_environ->runtimeJavaVMPtr;
    if (qclVm == NULL) qclVm = runtimeJavaVMPtr;
    if (qclVm == NULL && env != NULL) (*env)->GetJavaVM(env, &qclVm);
    if (qclVm == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "nativeInitializeGLFWNativeBridge: JavaVM unavailable, skip");
        return;
    }
    pojav_environ->runtimeJavaVMPtr = qclVm;

    JNIEnv *vmEnv = NULL;
    jint qclEnvRet = (*qclVm)->GetEnv(qclVm, (void **) &vmEnv, JNI_VERSION_1_4);
    if (qclEnvRet != JNI_OK || vmEnv == NULL) {
        if ((*qclVm)->AttachCurrentThread(qclVm, &vmEnv, NULL) != JNI_OK || vmEnv == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "nativeInitializeGLFWNativeBridge: attach failed (%d)", qclEnvRet);
            return;
        }
    }
    pojav_environ->vmGlfwClass = (*vmEnv)->NewGlobalRef(vmEnv,
                                                        (*vmEnv)->FindClass(vmEnv,
                                                                            "org/lwjgl/glfw/GLFW"));
    // ★★★ 2026-09-17 诊断（b1.7.3 / 1.20.6 画面冻结）：
    // FindClass 可能失败 —— GLFW 类是从**独立的 URLClassLoader**（lwjgl-glfw.jar）加载的，
    // 而本函数若在非 app classloader 线程上跑，FindClass 会抛 ClassNotFoundException 并返回 NULL。
    // 一旦 vmGlfwClass == NULL：
    //   · 下面的 GetStaticMethodID 全部抛异常返回 NULL（还会留下 pending exception）
    //   · updateMonitorSize() 里 `vmGlfwClass == NULL` 直接 return → 尺寸永远停在 0x0
    //   · internalWindowSizeChanged 为 NULL → 窗口尺寸回调永不触发
    //   · 游戏主循环认为窗口是 0x0 → GLThread 空转 → 画面冻结
    // 这里把实际返回值落到 logcat，一眼可见。
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                        "nativeInitializeGLFWNativeBridge: vmGlfwClass=%p", pojav_environ->vmGlfwClass);
    if (pojav_environ->vmGlfwClass == NULL) {
        if ((*vmEnv)->ExceptionCheck(vmEnv)) {
            (*vmEnv)->ExceptionDescribe(vmEnv);
            (*vmEnv)->ExceptionClear(vmEnv);
        }
        // 兜底重试：用 Thread.currentThread().getContextClassLoader() 再找一次。
        // MC 在 LaunchClassLoader 里跑，GLFW 由 URLClassLoader 加载，context classloader
        // 通常就是那个 URLClassLoader —— 这是跨 classloader 拿类的标准做法。
        jclass threadCls = (*vmEnv)->FindClass(vmEnv, "java/lang/Thread");
        if (threadCls != NULL) {
            jmethodID curThread = (*vmEnv)->GetStaticMethodID(vmEnv, threadCls, "currentThread",
                                                              "()Ljava/lang/Thread;");
            jmethodID getCcl = (*vmEnv)->GetMethodID(vmEnv, threadCls, "getContextClassLoader",
                                                     "()Ljava/lang/ClassLoader;");
            if (curThread != NULL && getCcl != NULL) {
                jobject th = (*vmEnv)->CallStaticObjectMethod(vmEnv, threadCls, curThread);
                jobject ccl = (*vmEnv)->CallObjectMethod(vmEnv, th, getCcl);
                if (ccl != NULL) {
                    jclass clCls = (*vmEnv)->FindClass(vmEnv, "java/lang/ClassLoader");
                    jmethodID loadCls = (*vmEnv)->GetMethodID(vmEnv, clCls, "loadClass",
                                                              "(Ljava/lang/String;)Ljava/lang/Class;");
                    jstring nm = (*vmEnv)->NewStringUTF(vmEnv, "org.lwjgl.glfw.GLFW");
                    jclass found = (jclass) (*vmEnv)->CallObjectMethod(vmEnv, ccl, loadCls, nm);
                    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                                        "nativeInitializeGLFWNativeBridge: fallback loadClass=%p", found);
                    if (found != NULL) {
                        pojav_environ->vmGlfwClass = (*vmEnv)->NewGlobalRef(vmEnv, found);
                    }
                }
                if ((*vmEnv)->ExceptionCheck(vmEnv)) (*vmEnv)->ExceptionClear(vmEnv);
            }
            if ((*vmEnv)->ExceptionCheck(vmEnv)) (*vmEnv)->ExceptionClear(vmEnv);
        }
        if ((*vmEnv)->ExceptionCheck(vmEnv)) (*vmEnv)->ExceptionClear(vmEnv);
        if (pojav_environ->vmGlfwClass == NULL) {
            __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                                "nativeInitializeGLFWNativeBridge: GLFW class NOT FOUND, giving up");
            return;
        }
    }
    pojav_environ->method_glftSetWindowAttrib = (*vmEnv)->GetStaticMethodID(
            vmEnv, pojav_environ->vmGlfwClass, "glfwSetWindowAttrib", "(JII)V");
    pojav_environ->method_internalWindowSizeChanged = (*vmEnv)->GetStaticMethodID(
            vmEnv, pojav_environ->vmGlfwClass, "internalWindowSizeChanged", "(J)V");
    pojav_environ->method_internalChangeMonitorSize = (*vmEnv)->GetStaticMethodID(
            vmEnv, pojav_environ->vmGlfwClass, "internalChangeMonitorSize", "(II)V");
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                        "nativeInitializeGLFWNativeBridge: setAttrib=%p windowSize=%p monitorSize=%p",
                        pojav_environ->method_glftSetWindowAttrib,
                        pojav_environ->method_internalWindowSizeChanged,
                        pojav_environ->method_internalChangeMonitorSize);
    if ((*vmEnv)->ExceptionCheck(vmEnv)) {
        (*vmEnv)->ExceptionDescribe(vmEnv);
        (*vmEnv)->ExceptionClear(vmEnv);
    }
    jfieldID field_keyDownBuffer = (*vmEnv)->GetStaticFieldID(
            vmEnv, pojav_environ->vmGlfwClass, "keyDownBuffer", "Ljava/nio/ByteBuffer;");
    jobject keyDownBufferJ = (*vmEnv)->GetStaticObjectField(
            vmEnv, pojav_environ->vmGlfwClass, field_keyDownBuffer);
    pojav_environ->keyDownBuffer = (*vmEnv)->GetDirectBufferAddress(vmEnv, keyDownBufferJ);
    jfieldID field_mouseDownBuffer = (*vmEnv)->GetStaticFieldID(
            vmEnv, pojav_environ->vmGlfwClass, "mouseDownBuffer", "Ljava/nio/ByteBuffer;");
    jobject mouseDownBufferJ = (*vmEnv)->GetStaticObjectField(
            vmEnv, pojav_environ->vmGlfwClass, field_mouseDownBuffer);
    pojav_environ->mouseDownBuffer = (*vmEnv)->GetDirectBufferAddress(vmEnv, mouseDownBufferJ);
}
/*
 * ★★★ 2026-09-17 深夜：本宏是 1.20.6 / 26.2「鼠标能移动、按不了按钮」的根因所在。
 *
 * 【症状】全量 logcat：
 *   sendMouseButton ... GLFW_invoke_MouseButton=0x0
 *   Pump MOUSE_BUTTON btn=0 act=1 mods=0 invoke=0x0
 *   → isInputReady=1、isUseStackQueueCall=1、环形队列写入正常、
 *     pojavPumpEvents 也确实读到了事件 —— 唯独回调指针是 NULL。
 *
 * 【根因】这里原来被写成往**本文件自己新建的全局变量** GLFW_invoke_##NAME 里写
 *   （宏体里多了一行 `GLFW_invoke_##NAME##_func* GLFW_invoke_##NAME;`），
 *   而 pojavPumpEvents() 读的是 **pojav_environ 结构体字段**
 *   pojav_environ->GLFW_invoke_##NAME（environ.h:76-86 定义）。两套存储，永不相交。
 *
 * 【为什么 b1.7.3 却正常】低版本 isUseStackQueueCall=0，走「直接回调分支」，
 *   那一支读的正好是同名全局变量 —— 与本宏写的是同一份，所以能用。
 *   高版本（minimumLauncherVersion>=21）走队列分支，读结构体字段 → 恒 NULL。
 *   ⇒ 这就是用户问的「为什么只有 b1.7.3 才能触屏有效」的答案。
 *
 * 【修法】严格对齐 FCL 原版（FCL/jni/input_bridge_v3.c:125-130）：
 *   全局只保留结构体字段一份存储，注册写它、直接回调读它、队列 pump 也读它。
 *   下面那些 JNIEXPORT 函数里原本的裸 `GLFW_invoke_XXX` 读，
 *   也一并改成了 `pojav_environ->GLFW_invoke_XXX`。
 *
 * 【另注】`return *oldCallback` 在赋值之后取，所以返回的是新值而非旧值
 *   （FCL 原版同样如此）。GLFW 只在卸载回调时用到返回值，不影响功能，先跟 FCL 保持一致。
 */
#define ADD_CALLBACK_WWIN(NAME) \
JNIEXPORT jlong JNICALL Java_org_lwjgl_glfw_GLFW_nglfwSet##NAME##Callback(JNIEnv * env, jclass cls, jlong window, jlong callbackptr) { \
    void** oldCallback = (void**) &pojav_environ->GLFW_invoke_##NAME; \
    pojav_environ->GLFW_invoke_##NAME = (GLFW_invoke_##NAME##_func*) (uintptr_t) callbackptr; \
    if (getenv("QCL_DBG_INPUT")) { \
        __android_log_print(ANDROID_LOG_INFO, "QCL_INPUT", \
            "SetCallback " #NAME " window=0x%llx ptr=0x%llx (old=0x%llx)", \
            (unsigned long long) window, (unsigned long long) callbackptr, \
            (unsigned long long) (uintptr_t) *oldCallback); \
    } \
    return (jlong) (uintptr_t) *oldCallback; \
}

ADD_CALLBACK_WWIN(Char);
ADD_CALLBACK_WWIN(CharMods);
ADD_CALLBACK_WWIN(CursorEnter);
ADD_CALLBACK_WWIN(CursorPos);
ADD_CALLBACK_WWIN(FramebufferSize);
ADD_CALLBACK_WWIN(Key);
ADD_CALLBACK_WWIN(MouseButton);
ADD_CALLBACK_WWIN(Scroll);
ADD_CALLBACK_WWIN(WindowSize);

#undef ADD_CALLBACK_WWIN

jboolean attachThread(bool isAndroid, JNIEnv** secondJNIEnvPtr) {
#ifdef DEBUG
    LOGD("Debug: Attaching %s thread to %s, javavm.isNull=%d\n", isAndroid ? "Android" : "JRE", isAndroid ? "JRE" : "Android", (isAndroid ? runtimeJavaVMPtr : dalvikJavaVMPtr) == NULL);
#endif

    if (*secondJNIEnvPtr != NULL || (!pojav_environ->isUseStackQueueCall)) return JNI_TRUE;

    if (isAndroid && runtimeJavaVMPtr) {
        (*runtimeJavaVMPtr)->AttachCurrentThread(runtimeJavaVMPtr, secondJNIEnvPtr, NULL);
        return JNI_TRUE;
    } else if (!isAndroid && dalvikJavaVMPtr) {
        (*dalvikJavaVMPtr)->AttachCurrentThread(dalvikJavaVMPtr, secondJNIEnvPtr, NULL);
        return JNI_TRUE;
    }
    
    return JNI_FALSE;
}

/*
 * ★★★ 2026-09-17 修复「1.20.6 / 26.2 触屏完全失效，b1.7.3 正常」根因。
 *
 * 原实现（旧 Pojav 模式）：把事件通过 CallStaticVoidMethod 回调 Java 静态方法
 *   org.lwjgl.glfw.CallbackBridge.receiveCallback(IIIII)V
 * 但 **QCL 的 CallbackBridge.java 里根本没有这个方法** ——
 * （FCL 的 CallbackBridge 也没有，因为 FCL 根本不用这套机制）。
 * 于是 setClass() 里的 GetStaticMethodID 返回 NULL，sendData 每次都撞上
 *   if (inputBridgeClass_ANDROID == NULL) return;
 * 事件被静默丢弃。
 *
 * 为什么 b1.7.3 没事、1.20.6 就废：
 *   b1.7.3 的 minimumLauncherVersion=7 → isHighVersion=false → isUseStackQueueCall=false
 *   → 输入走 **直接回调分支**（GLFW_invoke_* 直调），完全不经过 sendData。
 *   1.20.6 / 26.2 的 minimumLauncherVersion=21 → true → 走 **队列分支** → 全部丢进黑洞。
 *
 * 修法（照抄参照物 FCL 的 input_bridge_v3.c）：sendData 改为把事件写进
 * pojav_environ 里的 **native 环形缓冲**（events[] + 原子 eventCounter），
 * 由 GLFW 每帧调用 pojavStartPumping/pojavPumpEvents/pojavStopPumping 消费。
 * 这套消费端 QCL 在 1.1.0 移植渲染栈时已经带了（见下面的 pojavPumpEvents），
 * environ.h 的字段也齐备 —— 只差这个生产端还是旧实现。
 *
 * 好处：跨线程零 JNI 调用，不再依赖任何 Java 侧方法，也不会踩
 * 「在非 GL 线程上 CallStaticVoidMethod 到 GLFW」的坑。
 */
void sendData(int type, int i1, int i2, int i3, int i4) {
    GLFWInputEvent *event = &pojav_environ->events[pojav_environ->inEventIndex];
    event->type = type;
    event->i1 = i1;
    event->i2 = i2;
    event->i3 = i3;
    event->i4 = i4;

    if (++pojav_environ->inEventIndex >= EVENT_WINDOW_SIZE)
        pojav_environ->inEventIndex -= EVENT_WINDOW_SIZE;

    atomic_fetch_add_explicit(&pojav_environ->eventCounter, 1, memory_order_acquire);
}

void closeGLFWWindow() {
    /*
    jclass glfwClazz = (*runtimeJNIEnvPtr_JRE)->FindClass(runtimeJNIEnvPtr_JRE, "org/lwjgl/glfw/GLFW");
    assert(glfwClazz != NULL);
    jmethodID glfwMethod = (*runtimeJNIEnvPtr_JRE)->GetStaticMethodID(runtimeJNIEnvPtr_JRE, glfwMethod, "glfwSetWindowShouldClose", "(JZ)V");
    assert(glfwMethod != NULL);
    
    (*runtimeJNIEnvPtr_JRE)->CallStaticVoidMethod(
        runtimeJNIEnvPtr_JRE,
        glfwClazz, glfwMethod,
        (jlong) showingWindow, JNI_TRUE
    );
    */
    exit(-1);
}

/**
 * Hooked version of java.lang.UNIXProcess.forkAndExec()
 * which is used to handle the "open" command.
 */
jint
hooked_ProcessImpl_forkAndExec(JNIEnv *env, jobject process, jint mode, jbyteArray helperpath, jbyteArray prog, jbyteArray argBlock, jint argc, jbyteArray envBlock, jint envc, jbyteArray dir, jintArray std_fds, jboolean redirectErrorStream) {
    char *pProg = (char *)((*env)->GetByteArrayElements(env, prog, NULL));

    // Here we only handle the "xdg-open" command
    if (strcmp(basename(pProg), "xdg-open")) {
        (*env)->ReleaseByteArrayElements(env, prog, (jbyte *)pProg, 0);
        return orig_ProcessImpl_forkAndExec(env, process, mode, helperpath, prog, argBlock, argc, envBlock, envc, dir, std_fds, redirectErrorStream);
    }
    (*env)->ReleaseByteArrayElements(env, prog, (jbyte *)pProg, 0);

    Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(env, NULL, /* CLIPBOARD_OPEN */ 2002, argBlock);
    return 0;
}

void hookExec() {
    jclass cls;
    orig_ProcessImpl_forkAndExec = dlsym(RTLD_DEFAULT, "Java_java_lang_UNIXProcess_forkAndExec");
    if (!orig_ProcessImpl_forkAndExec) {
        orig_ProcessImpl_forkAndExec = dlsym(RTLD_DEFAULT, "Java_java_lang_ProcessImpl_forkAndExec");
        cls = (*runtimeJNIEnvPtr_JRE)->FindClass(runtimeJNIEnvPtr_JRE, "java/lang/ProcessImpl");
    } else {
        cls = (*runtimeJNIEnvPtr_JRE)->FindClass(runtimeJNIEnvPtr_JRE, "java/lang/UNIXProcess");
    }
    JNINativeMethod methods[] = {
        {"forkAndExec", "(I[B[B[BI[BI[B[IZ)I", (void *)&hooked_ProcessImpl_forkAndExec}
    };
    (*runtimeJNIEnvPtr_JRE)->RegisterNatives(runtimeJNIEnvPtr_JRE, cls, methods, 1);
    printf("Registered forkAndExec\n");
}

JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_CallbackBridge_nativeSetUseInputStackQueue(JNIEnv *env, jclass clazz,
                                                               jboolean use_input_stack_queue) {
    pojav_environ->isUseStackQueueCall = (int) use_input_stack_queue;
    // ★ 诊断：确认 Java 侧到底传了什么进来
    __android_log_print(ANDROID_LOG_INFO, "QCL_INPUT",
        "nativeSetUseInputStackQueue(%d)", (int) use_input_stack_queue);
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeAttachThreadToOther(JNIEnv* env, jclass clazz, jboolean isAndroid, jboolean isUseStackQueueBool) {
#ifdef DEBUG
    LOGD("Debug: JNI attaching thread, isUseStackQueue=%d\n", isUseStackQueueBool);
#endif

    jboolean result;

    //isUseStackQueueCall = (int) isUseStackQueueBool;
    if (isAndroid) {
        result = attachThread(true, &runtimeJNIEnvPtr_ANDROID);
    } /* else {
        result = attachThread(false, &dalvikJNIEnvPtr_JRE);
        // getJavaInputBridge(&inputBridgeClass_JRE, &inputBridgeMethod_JRE);
    } */
    
    if (pojav_environ->isUseStackQueueCall && isAndroid && result) {
        isPrepareGrabPos = true;
    }
    return result;
}

JNIEXPORT jstring JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(JNIEnv* env, jclass clazz, jint action, jbyteArray copySrc) {
#ifdef DEBUG
    LOGD("Debug: Clipboard access is going on\n", pojav_environ->isUseStackQueueCall);
#endif

    JNIEnv *dalvikEnv;
    (*dalvikJavaVMPtr)->AttachCurrentThread(dalvikJavaVMPtr, &dalvikEnv, NULL);
    assert(dalvikEnv != NULL);
    assert(bridgeClazz != NULL);
    LOGD("Clipboard: Obtaining method\n");
    jmethodID bridgeMethod = (*dalvikEnv)->GetStaticMethodID(dalvikEnv, bridgeClazz, "accessAndroidClipboard", "(ILjava/lang/String;)Ljava/lang/String;");
    assert(bridgeMethod != NULL);
    
    LOGD("Clipboard: Converting string\n");
    char *copySrcC = NULL;
    jstring copyDst = NULL;
    if (copySrc) {
        copySrcC = (char *)((*env)->GetByteArrayElements(env, copySrc, NULL));
        copyDst = (*dalvikEnv)->NewStringUTF(dalvikEnv, copySrcC);
    }

    LOGD("Clipboard: Calling 2nd\n");
    jstring pasteDst = convertStringJVM(dalvikEnv, env, (jstring) (*dalvikEnv)->CallStaticObjectMethod(dalvikEnv, bridgeClazz, bridgeMethod, action, copyDst));

    if (copySrc) {
        (*dalvikEnv)->DeleteLocalRef(dalvikEnv, copyDst);    
        (*env)->ReleaseByteArrayElements(env, copySrc, (jbyte *)copySrcC, 0);
    }
    (*dalvikJavaVMPtr)->DetachCurrentThread(dalvikJavaVMPtr);
    return pasteDst;
}

JNIEXPORT jboolean JNICALL
JavaCritical_org_lwjgl_glfw_CallbackBridge_nativeSetInputReady(jboolean inputReady) {
    pojav_environ->isInputReady = inputReady;
    return pojav_environ->isUseStackQueueCall;
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSetInputReady(JNIEnv* env, jclass clazz, jboolean inputReady) {
#ifdef DEBUG
    LOGD("Debug: Changing input state, isReady=%d, isUseStackQueueCall=%d\n", inputReady, pojav_environ->isUseStackQueueCall);
#endif
    // ★ 诊断：这条链路决定"输入是否放行"以及"走队列还是走直连"
    __android_log_print(ANDROID_LOG_INFO, "QCL_INPUT",
        "nativeSetInputReady(%d) -> isUseStackQueueCall=%d (env=%d)",
        (int) inputReady, (int) pojav_environ->isUseStackQueueCall,
        (int) pojav_environ->isUseStackQueueCall);
    pojav_environ->isInputReady = inputReady;
    return pojav_environ->isUseStackQueueCall;
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSetGrabbing(JNIEnv* env, jclass clazz, jboolean grabbing, jint xset, jint yset) {
    pojav_environ->isGrabbing = grabbing;
    if (pojav_environ->isGrabbing == JNI_TRUE) {
        grabCursorX = xset; // savedWidth / 2;
        grabCursorY = yset; // savedHeight / 2;
        isPrepareGrabPos = true;
    }
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeIsGrabbing(JNIEnv* env, jclass clazz) {
    return pojav_environ->isGrabbing;
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendChar(JNIEnv* env, jclass clazz, jchar codepoint /* jint codepoint */) {
    if (pojav_environ->GLFW_invoke_Char && pojav_environ->isInputReady) {
        if (pojav_environ->isUseStackQueueCall) {
            sendData(EVENT_TYPE_CHAR, codepoint, 0, 0, 0);
        } else {
            pojav_environ->GLFW_invoke_Char((void*) pojav_environ->showingWindow, (unsigned int) codepoint);
            // return lwjgl2_triggerCharEvent(codepoint);
        }
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendCharMods(JNIEnv* env, jclass clazz, jchar codepoint, jint mods) {
    if (pojav_environ->GLFW_invoke_CharMods && pojav_environ->isInputReady) {
        if (pojav_environ->isUseStackQueueCall) {
            sendData(EVENT_TYPE_CHAR_MODS, (unsigned int) codepoint, mods, 0, 0);
        } else {
            pojav_environ->GLFW_invoke_CharMods((void*) pojav_environ->showingWindow, codepoint, mods);
        }
        return JNI_TRUE;
    }
    return JNI_FALSE;
}
/*
JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendCursorEnter(JNIEnv* env, jclass clazz, jint entered) {
    if (pojav_environ->GLFW_invoke_CursorEnter && isInputReady) {
        pojav_environ->GLFW_invoke_CursorEnter(showingWindow, entered);
    }
}
*/
JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendCursorPos(JNIEnv* env, jclass clazz, jfloat x, jfloat y) {
#ifdef DEBUG
    LOGD("Sending cursor position \n");
#endif
    if (pojav_environ->GLFW_invoke_CursorPos && pojav_environ->isInputReady) {
#ifdef DEBUG
        LOGD("pojav_environ->GLFW_invoke_CursorPos && isInputReady \n");
#endif
        if (!pojav_environ->isCursorEntered) {
            if (pojav_environ->GLFW_invoke_CursorEnter) {
                pojav_environ->isCursorEntered = true;
                if (pojav_environ->isUseStackQueueCall) {
                    sendData(EVENT_TYPE_CURSOR_ENTER, 1, 0, 0, 0);
                } else {
                    pojav_environ->GLFW_invoke_CursorEnter((void*) pojav_environ->showingWindow, 1);
                }
            } else if (pojav_environ->isGrabbing) {
                // Some Minecraft versions does not use GLFWCursorEnterCallback
                // This is a smart check, as Minecraft will not in grab mode if already not.
                pojav_environ->isCursorEntered = true;
            }
        }

        if (!pojav_environ->isUseStackQueueCall) {
            // ★★★ 1.1.8：直接回调模式也更新 cursorX/cursorY。超老版本（infdev 等）的 LWJGL
            // Mouse 用「轮询」（Mouse.getX → nglfwGetCursorPos 读 cursorX/cursorY），直接回调分支
            // 若不更新，轮询读到的永远是旧值 → 滑动视角无效。b1.7.3 走回调不受影响，赋值无害。
            pojav_environ->cursorX = x;
            pojav_environ->cursorY = y;
            pojav_environ->GLFW_invoke_CursorPos((void*) pojav_environ->showingWindow, (double) (x), (double) (y));
        } else {
            // ★★★ 2026-09-19 修复「1.20.6 转视角迟钝/极难转动」（b1.7.3 同机丝滑）：
            //   严格对齐 FCL FCL/src/main/jni/input_bridge_v3.c:567-573 —— 队列模式**纯赋值绝对坐标**。
            //   原 QCL 私货：isGrabbing 时写 grabCursorX（`grabCursorX += x - lastCursorX` 从 0 累积的
            //   相对位移），poke 给 MC 的位置序列是「0 起步的累积值」，而 MC 的 MouseHandler 用
            //   屏幕中心作初始基准算 delta → delta 整体错乱（首帧就是 -中心X 的巨大跳变）→
            //   表现为「视角极难转动 / 怎么滑都转不动」。
            //   低版本（b1.7.3 <21）走直接回调分支传原始绝对坐标，所以同机丝滑 —— 与用户观察一致。
            pojav_environ->cursorX = x;
            pojav_environ->cursorY = y;
        }
        
        lastCursorX = x;
        lastCursorY = y;
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendKey(JNIEnv* env, jclass clazz, jint key, jint scancode, jint action, jint mods) {
    if (pojav_environ->GLFW_invoke_Key && pojav_environ->isInputReady) {
        if (pojav_environ->isUseStackQueueCall) {
            sendData(EVENT_TYPE_KEY, key, scancode, action, mods);
        } else {
            pojav_environ->GLFW_invoke_Key((void*) pojav_environ->showingWindow, key, scancode, action, mods);
        }
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendMouseButton(JNIEnv* env, jclass clazz, jint button, jint action, jint mods) {
    // ★ 2026-09-17 诊断打点：1.20.6 鼠标能移动但按不下去，这里把真实分支打出来。
    // ★ 2026-09-18 补注：状态位统一到 pojav_environ 后，日志里 isInputReady 与
    //   pojav_environ->isInputReady 已是**同一份**（原来打两份是为了暴露"两套存储"病灶）。
    if (getenv("QCL_DBG_INPUT")) {
        __android_log_print(ANDROID_LOG_INFO, "QCL_INPUT",
            "sendMouseButton btn=%d act=%d mods=%d | isInputReady=%d isUseStackQueueCall=%d "
            "invoke_MouseButton=%p",
            button, action, mods,
            (int) pojav_environ->isInputReady, (int) pojav_environ->isUseStackQueueCall,
            (void*) pojav_environ->GLFW_invoke_MouseButton);
    }
    if (pojav_environ->isInputReady) {
        if (button == -1) {
            // Notify to prepare set new grab pos
            isPrepareGrabPos = true;
        } else if (pojav_environ->GLFW_invoke_MouseButton) {
            if (pojav_environ->isUseStackQueueCall) {
                sendData(EVENT_TYPE_MOUSE_BUTTON, button, action, mods, 0);
            } else {
                pojav_environ->GLFW_invoke_MouseButton((void*) pojav_environ->showingWindow, button, action, mods);
            }
        }
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendScreenSize(JNIEnv* env, jclass clazz, jint width, jint height) {
    // ★★★ 2026-09-18：同 showingWindow / isGrabbing，savedWidth/Height 也曾经是
    // 「utils.h 的 static」与「environ.h 结构体字段」两份 —— 这里写 static 那份，
    // 而 egl_bridge.c 写 pojav_environ->savedWidth、pojavStartPumping 读
    // pojav_environ->savedWidth 做 monitor size 上报，于是屏幕尺寸永远对不上。
    // 对齐 FCL/.../jni/input_bridge_v3.c:640-641，统一写结构体。
    pojav_environ->savedWidth = width;
    pojav_environ->savedHeight = height;
    
    if (pojav_environ->isInputReady) {
        if (pojav_environ->GLFW_invoke_FramebufferSize) {
            if (pojav_environ->isUseStackQueueCall) {
                sendData(EVENT_TYPE_FRAMEBUFFER_SIZE, width, height, 0, 0);
            } else {
                pojav_environ->GLFW_invoke_FramebufferSize((void*) pojav_environ->showingWindow, width, height);
            }
        }
        
        if (pojav_environ->GLFW_invoke_WindowSize) {
            if (pojav_environ->isUseStackQueueCall) {
                sendData(EVENT_TYPE_WINDOW_SIZE, width, height, 0, 0);
            } else {
                pojav_environ->GLFW_invoke_WindowSize((void*) pojav_environ->showingWindow, width, height);
            }
        }
    }
    
    // return (isInputReady && (pojav_environ->GLFW_invoke_FramebufferSize || pojav_environ->GLFW_invoke_WindowSize));
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendScroll(JNIEnv* env, jclass clazz, jdouble xoffset, jdouble yoffset) {
    if (pojav_environ->GLFW_invoke_Scroll && pojav_environ->isInputReady) {
        if (pojav_environ->isUseStackQueueCall) {
            sendData(EVENT_TYPE_SCROLL, xoffset, yoffset, 0, 0);
        } else {
            pojav_environ->GLFW_invoke_Scroll((void*) pojav_environ->showingWindow, (double) xoffset, (double) yoffset);
        }
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_GLFW_nglfwSetShowingWindow(JNIEnv* env, jclass clazz, jlong window) {
    pojav_environ->showingWindow = (long) window;
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSetWindowAttrib(JNIEnv* env, jclass clazz, jint attrib, jint value) {
    if (!pojav_environ->showingWindow || !pojav_environ->isUseStackQueueCall) {
        // If the window is not shown, there is nothing to do yet.
        // For Minecraft < 1.13, calling to JNI functions here crashes the JVM for some reason, therefore it is skipped for now.
        return;
    }

    jclass glfwClazz = (*runtimeJNIEnvPtr_JRE)->FindClass(runtimeJNIEnvPtr_JRE, "org/lwjgl/glfw/GLFW");
    assert(glfwClazz != NULL);
    jmethodID glfwMethod = (*runtimeJNIEnvPtr_JRE)->GetStaticMethodID(runtimeJNIEnvPtr_JRE, glfwClazz, "glfwSetWindowAttrib", "(JII)V");
    assert(glfwMethod != NULL);

    (*runtimeJNIEnvPtr_JRE)->CallStaticVoidMethod(
        runtimeJNIEnvPtr_JRE,
        glfwClazz, glfwMethod,
        (jlong) pojav_environ->showingWindow, attrib, value
    );
}

JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_CallbackBridge_setClass(JNIEnv *env, jclass clazz) {
    inputBridgeMethod_ANDROID = (*env)->GetStaticMethodID(env, clazz, "receiveCallback", "(IIIII)V");
    inputBridgeClass_ANDROID = (*env)->NewGlobalRef(env, clazz);
}


/*
 * [1.1.0 移植自 FCL] GLFW 事件泵函数。
 * 定制版 GLFW.java 的 GLFW$Functions 需要 pojavStartPumping/pojavPumpEvents/
 * pojavStopPumping 三个符号（apiGetFunctionAddress 查找），QCL 原版缺失
 * -> "A required function is missing: pojavPumpEvents" -> GLFW 初始化失败。
 */

void updateWindowSize(void *window) {
    (*pojav_environ->glfwThreadVmEnv)->CallStaticVoidMethod(
            pojav_environ->glfwThreadVmEnv, pojav_environ->vmGlfwClass,
            pojav_environ->method_internalWindowSizeChanged, (jlong) window);
}

void pojavPumpEvents(void *window) {
    if (pojav_environ->shouldUpdateMouse) {
        pojav_environ->GLFW_invoke_CursorPos(window, floor(pojav_environ->cursorX),
                                             floor(pojav_environ->cursorY));
    }
    if (pojav_environ->shouldUpdateMonitorSize) {
        updateWindowSize(window);
    }

    size_t index = pojav_environ->outEventIndex;
    size_t targetIndex = pojav_environ->outTargetIndex;

    while (targetIndex != index) {
        GLFWInputEvent event = pojav_environ->events[index];
        switch (event.type) {
            case EVENT_TYPE_CHAR:
                if (pojav_environ->GLFW_invoke_Char)
                    pojav_environ->GLFW_invoke_Char(window, event.i1);
                break;
            case EVENT_TYPE_CHAR_MODS:
                if (pojav_environ->GLFW_invoke_CharMods)
                    pojav_environ->GLFW_invoke_CharMods(window, event.i1, event.i2);
                break;
            case EVENT_TYPE_KEY:
                if (pojav_environ->GLFW_invoke_Key)
                    pojav_environ->GLFW_invoke_Key(window, event.i1, event.i2, event.i3, event.i4);
                break;
            case EVENT_TYPE_MOUSE_BUTTON:
                // ★ 诊断：确认队列消费端真的收到了鼠标按键
                if (getenv("QCL_DBG_INPUT")) {
                    __android_log_print(ANDROID_LOG_INFO, "QCL_INPUT",
                        "Pump MOUSE_BUTTON btn=%d act=%d mods=%d invoke=%p",
                        event.i1, event.i2, event.i3,
                        (void*) pojav_environ->GLFW_invoke_MouseButton);
                }
                if (pojav_environ->GLFW_invoke_MouseButton)
                    pojav_environ->GLFW_invoke_MouseButton(window, event.i1, event.i2, event.i3);
                break;
            case EVENT_TYPE_CURSOR_ENTER:
                if (pojav_environ->GLFW_invoke_CursorEnter)
                    pojav_environ->GLFW_invoke_CursorEnter(window, event.i1);
                break;
            case EVENT_TYPE_SCROLL:
                if (pojav_environ->GLFW_invoke_Scroll)
                    pojav_environ->GLFW_invoke_Scroll(window, event.i1, event.i2);
                break;
        }

        index++;
        if (index >= EVENT_WINDOW_SIZE)
            index -= EVENT_WINDOW_SIZE;
    }
}

/** Prepare the library for sending out callbacks to all windows */
void pojavStartPumping() {
    size_t counter = atomic_load_explicit(&pojav_environ->eventCounter, memory_order_acquire);
    size_t index = pojav_environ->outEventIndex;

    unsigned targetIndex = index + counter;
    if (targetIndex >= EVENT_WINDOW_SIZE)
        targetIndex -= EVENT_WINDOW_SIZE;

    // Only accessed by one unique thread, no need to atomic store
    pojav_environ->inEventCount = counter;
    pojav_environ->outTargetIndex = targetIndex;

    //PumpEvents is called for every window, so this logic should be there in order to correctly distribute events to all windows.
    if ((pojav_environ->cLastX != pojav_environ->cursorX ||
         pojav_environ->cLastY != pojav_environ->cursorY) && pojav_environ->GLFW_invoke_CursorPos) {
        pojav_environ->cLastX = pojav_environ->cursorX;
        pojav_environ->cLastY = pojav_environ->cursorY;
        pojav_environ->shouldUpdateMouse = true;
    }
    if (pojav_environ->shouldUpdateMonitorSize) {
        // Perform a monitor size update here to avoid doing it on every single window
        updateMonitorSize(pojav_environ->savedWidth, pojav_environ->savedHeight);
        // Mark the monitor size as consumed (since GLFW was made aware of it)
        pojav_environ->monitorSizeConsumed = true;
    }
}

/** Prepare the library for the next round of new events */
void pojavStopPumping() {
    pojav_environ->outEventIndex = pojav_environ->outTargetIndex;

    // New events may have arrived while pumping, so remove only the difference before the start and end of execution
    atomic_fetch_sub_explicit(&pojav_environ->eventCounter, pojav_environ->inEventCount,
                              memory_order_acquire);
    // Make sure the next frame won't send mouse or monitor updates if it's unnecessary
    pojav_environ->shouldUpdateMouse = false;
    // Only reset the update flag if the monitor size was consumed by pojavStartPumping. This
    // will delay the update to next frame if it had occured between pojavStartPumping and pojavStopPumping,
    // but it's better than not having it apply at all
    if (pojav_environ->shouldUpdateMonitorSize && pojav_environ->monitorSizeConsumed) {
        pojav_environ->shouldUpdateMonitorSize = false;
        pojav_environ->monitorSizeConsumed = false;
    }
}

/*
 * ★★★ 1.1.2 修复「进存档后 Ticking screen 崩溃」（照搬 FCL input_bridge_v3.c，一个字不改）：
 *   MC 崩溃报告：java.lang.UnsatisfiedLinkError:
 *     'void org.lwjgl.glfw.GLFW.glfwSetCursorPos(long, double, double)'
 *     at eyv.a(MouseHandler:489) → Ticking screen → 游戏退出（随后关音频踩已销毁 mutex 报 SIGABRT，属二次伤害）。
 *   根因：QCL 移植 input_bridge_v3.c 时只搬了注释、漏掉了下面这一组 JNI 实现。
 *   MC 的 MouseHandler 在屏幕 tick 中调 glfwSetCursorPos 归中镜头，缺符号即崩；
 *   nglfwGetCursorPos / nglfwGetCursorPosA 是 MC 轮询光标位置的同族符号，一并补齐。
 *   （手柄/SDL 的 JNI 符号 FCL 虽有，但 QCL Java 侧无对应 native 声明、零引用，不搬。）
 */

JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_GLFW_nglfwGetCursorPos(JNIEnv *env, __attribute__((unused)) jclass clazz,
                                           __attribute__((unused)) jlong window, jobject xpos,
                                           jobject ypos) {
    *(double *) (*env)->GetDirectBufferAddress(env, xpos) = pojav_environ->cursorX;
    *(double *) (*env)->GetDirectBufferAddress(env, ypos) = pojav_environ->cursorY;
}

JNIEXPORT void JNICALL
JavaCritical_org_lwjgl_glfw_GLFW_nglfwGetCursorPosA(__attribute__((unused)) jlong window,
                                                    jint lengthx, jdouble *xpos, jint lengthy,
                                                    jdouble *ypos) {
    *xpos = pojav_environ->cursorX;
    *ypos = pojav_environ->cursorY;
}

JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_GLFW_nglfwGetCursorPosA(JNIEnv *env, __attribute__((unused)) jclass clazz,
                                            __attribute__((unused)) jlong window,
                                            jdoubleArray xpos, jdoubleArray ypos) {
    (*env)->SetDoubleArrayRegion(env, xpos, 0, 1, &pojav_environ->cursorX);
    (*env)->SetDoubleArrayRegion(env, ypos, 0, 1, &pojav_environ->cursorY);
}

JNIEXPORT void JNICALL
JavaCritical_org_lwjgl_glfw_GLFW_glfwSetCursorPos(__attribute__((unused)) jlong window,
                                                  jdouble xpos,
                                                  jdouble ypos) {
    pojav_environ->cLastX = pojav_environ->cursorX = xpos;
    pojav_environ->cLastY = pojav_environ->cursorY = ypos;
}

JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_GLFW_glfwSetCursorPos(__attribute__((unused)) JNIEnv *env,
                                          __attribute__((unused)) jclass clazz,
                                          __attribute__((unused)) jlong window, jdouble xpos,
                                          jdouble ypos) {
    JavaCritical_org_lwjgl_glfw_GLFW_glfwSetCursorPos(window, xpos, ypos);
}

