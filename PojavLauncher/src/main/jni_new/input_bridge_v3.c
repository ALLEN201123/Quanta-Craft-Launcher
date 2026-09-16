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
jboolean isGrabbing;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    if (dalvikJavaVMPtr == NULL) {
        //Save dalvik global JavaVM pointer
        dalvikJavaVMPtr = vm;
        (*vm)->GetEnv(vm, (void**) &dalvikJNIEnvPtr_ANDROID, JNI_VERSION_1_4);
        bridgeClazz = (*dalvikJNIEnvPtr_ANDROID)->NewGlobalRef(dalvikJNIEnvPtr_ANDROID,(*dalvikJNIEnvPtr_ANDROID) ->FindClass(dalvikJNIEnvPtr_ANDROID,"org/lwjgl/glfw/CallbackBridge"));
        assert(bridgeClazz != NULL);
        isUseStackQueueCall = JNI_FALSE;
    } else if (dalvikJavaVMPtr != vm) {
        runtimeJavaVMPtr = vm;
        (*vm)->GetEnv(vm, (void**) &runtimeJNIEnvPtr_JRE, JNI_VERSION_1_4);
        hookExec();
    }
    
    isGrabbing = JNI_FALSE;
    
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
    pojav_environ->method_glftSetWindowAttrib = (*vmEnv)->GetStaticMethodID(
            vmEnv, pojav_environ->vmGlfwClass, "glfwSetWindowAttrib", "(JII)V");
    pojav_environ->method_internalWindowSizeChanged = (*vmEnv)->GetStaticMethodID(
            vmEnv, pojav_environ->vmGlfwClass, "internalWindowSizeChanged", "(J)V");
    pojav_environ->method_internalChangeMonitorSize = (*vmEnv)->GetStaticMethodID(
            vmEnv, pojav_environ->vmGlfwClass, "internalChangeMonitorSize", "(II)V");
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
#define ADD_CALLBACK_WWIN(NAME) \
GLFW_invoke_##NAME##_func* GLFW_invoke_##NAME; \
JNIEXPORT jlong JNICALL Java_org_lwjgl_glfw_GLFW_nglfwSet##NAME##Callback(JNIEnv * env, jclass cls, jlong window, jlong callbackptr) { \
    void** oldCallback = (void**) &GLFW_invoke_##NAME; \
    GLFW_invoke_##NAME = (GLFW_invoke_##NAME##_func*) (uintptr_t) callbackptr; \
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

    if (*secondJNIEnvPtr != NULL || (!isUseStackQueueCall)) return JNI_TRUE;

    if (isAndroid && runtimeJavaVMPtr) {
        (*runtimeJavaVMPtr)->AttachCurrentThread(runtimeJavaVMPtr, secondJNIEnvPtr, NULL);
        return JNI_TRUE;
    } else if (!isAndroid && dalvikJavaVMPtr) {
        (*dalvikJavaVMPtr)->AttachCurrentThread(dalvikJavaVMPtr, secondJNIEnvPtr, NULL);
        return JNI_TRUE;
    }
    
    return JNI_FALSE;
}

void sendData(int type, int i1, int i2, int i3, int i4) {
#ifdef DEBUG
    LOGD("Debug: Send data, jnienv.isNull=%d\n", runtimeJNIEnvPtr_ANDROID == NULL);
#endif
    if (runtimeJNIEnvPtr_ANDROID == NULL) {
        LOGE("BUG: Input is ready but thread is not attached yet.");
        return;
    }
    if(inputBridgeClass_ANDROID == NULL) return;
    (*runtimeJNIEnvPtr_ANDROID)->CallStaticVoidMethod(
        runtimeJNIEnvPtr_ANDROID,
        inputBridgeClass_ANDROID,
        inputBridgeMethod_ANDROID,
        type,
        i1, i2, i3, i4
    );
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
    isUseStackQueueCall = (int) use_input_stack_queue;
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
    
    if (isUseStackQueueCall && isAndroid && result) {
        isPrepareGrabPos = true;
    }
    return result;
}

JNIEXPORT jstring JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(JNIEnv* env, jclass clazz, jint action, jbyteArray copySrc) {
#ifdef DEBUG
    LOGD("Debug: Clipboard access is going on\n", isUseStackQueueCall);
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

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSetInputReady(JNIEnv* env, jclass clazz, jboolean inputReady) {
#ifdef DEBUG
    LOGD("Debug: Changing input state, isReady=%d, isUseStackQueueCall=%d\n", inputReady, isUseStackQueueCall);
#endif
    isInputReady = inputReady;
    return isUseStackQueueCall;
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSetGrabbing(JNIEnv* env, jclass clazz, jboolean grabbing, jint xset, jint yset) {
    isGrabbing = grabbing;
    if (isGrabbing == JNI_TRUE) {
        grabCursorX = xset; // savedWidth / 2;
        grabCursorY = yset; // savedHeight / 2;
        isPrepareGrabPos = true;
    }
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeIsGrabbing(JNIEnv* env, jclass clazz) {
    return isGrabbing;
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendChar(JNIEnv* env, jclass clazz, jchar codepoint /* jint codepoint */) {
    if (GLFW_invoke_Char && isInputReady) {
        if (isUseStackQueueCall) {
            sendData(EVENT_TYPE_CHAR, codepoint, 0, 0, 0);
        } else {
            GLFW_invoke_Char((void*) showingWindow, (unsigned int) codepoint);
            // return lwjgl2_triggerCharEvent(codepoint);
        }
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendCharMods(JNIEnv* env, jclass clazz, jchar codepoint, jint mods) {
    if (GLFW_invoke_CharMods && isInputReady) {
        if (isUseStackQueueCall) {
            sendData(EVENT_TYPE_CHAR_MODS, (unsigned int) codepoint, mods, 0, 0);
        } else {
            GLFW_invoke_CharMods((void*) showingWindow, codepoint, mods);
        }
        return JNI_TRUE;
    }
    return JNI_FALSE;
}
/*
JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendCursorEnter(JNIEnv* env, jclass clazz, jint entered) {
    if (GLFW_invoke_CursorEnter && isInputReady) {
        GLFW_invoke_CursorEnter(showingWindow, entered);
    }
}
*/
JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendCursorPos(JNIEnv* env, jclass clazz, jfloat x, jfloat y) {
#ifdef DEBUG
    LOGD("Sending cursor position \n");
#endif
    if (GLFW_invoke_CursorPos && isInputReady) {
#ifdef DEBUG
        LOGD("GLFW_invoke_CursorPos && isInputReady \n");
#endif
        if (!isCursorEntered) {
            if (GLFW_invoke_CursorEnter) {
                isCursorEntered = true;
                if (isUseStackQueueCall) {
                    sendData(EVENT_TYPE_CURSOR_ENTER, 1, 0, 0, 0);
                } else {
                    GLFW_invoke_CursorEnter((void*) showingWindow, 1);
                }
            } else if (isGrabbing) {
                // Some Minecraft versions does not use GLFWCursorEnterCallback
                // This is a smart check, as Minecraft will not in grab mode if already not.
                isCursorEntered = true;
            }
        }

        if (isGrabbing) {
            if (!isPrepareGrabPos) {
                grabCursorX += x - lastCursorX;
                grabCursorY += y - lastCursorY;
            }
            
            lastCursorX = x;
            lastCursorY = y;
            
            if (isPrepareGrabPos) {
                isPrepareGrabPos = false;
                return;
            }
        }

        if (!isUseStackQueueCall) {
            GLFW_invoke_CursorPos((void*) showingWindow, (double) (x), (double) (y));
        } else {
            sendData(EVENT_TYPE_CURSOR_POS, (isGrabbing ? grabCursorX : x), (isGrabbing ? grabCursorY : y), 0, 0);
        }
        
        lastCursorX = x;
        lastCursorY = y;
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendKey(JNIEnv* env, jclass clazz, jint key, jint scancode, jint action, jint mods) {
    if (GLFW_invoke_Key && isInputReady) {
        if (isUseStackQueueCall) {
            sendData(EVENT_TYPE_KEY, key, scancode, action, mods);
        } else {
            GLFW_invoke_Key((void*) showingWindow, key, scancode, action, mods);
        }
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendMouseButton(JNIEnv* env, jclass clazz, jint button, jint action, jint mods) {
    if (isInputReady) {
        if (button == -1) {
            // Notify to prepare set new grab pos
            isPrepareGrabPos = true;
        } else if (GLFW_invoke_MouseButton) {
            if (isUseStackQueueCall) {
                sendData(EVENT_TYPE_MOUSE_BUTTON, button, action, mods, 0);
            } else {
                GLFW_invoke_MouseButton((void*) showingWindow, button, action, mods);
            }
        }
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendScreenSize(JNIEnv* env, jclass clazz, jint width, jint height) {
    savedWidth = width;
    savedHeight = height;
    
    if (isInputReady) {
        if (GLFW_invoke_FramebufferSize) {
            if (isUseStackQueueCall) {
                sendData(EVENT_TYPE_FRAMEBUFFER_SIZE, width, height, 0, 0);
            } else {
                GLFW_invoke_FramebufferSize((void*) showingWindow, width, height);
            }
        }
        
        if (GLFW_invoke_WindowSize) {
            if (isUseStackQueueCall) {
                sendData(EVENT_TYPE_WINDOW_SIZE, width, height, 0, 0);
            } else {
                GLFW_invoke_WindowSize((void*) showingWindow, width, height);
            }
        }
    }
    
    // return (isInputReady && (GLFW_invoke_FramebufferSize || GLFW_invoke_WindowSize));
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSendScroll(JNIEnv* env, jclass clazz, jdouble xoffset, jdouble yoffset) {
    if (GLFW_invoke_Scroll && isInputReady) {
        if (isUseStackQueueCall) {
            sendData(EVENT_TYPE_SCROLL, xoffset, yoffset, 0, 0);
        } else {
            GLFW_invoke_Scroll((void*) showingWindow, (double) xoffset, (double) yoffset);
        }
    }
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_GLFW_nglfwSetShowingWindow(JNIEnv* env, jclass clazz, jlong window) {
    showingWindow = (long) window;
}

JNIEXPORT void JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeSetWindowAttrib(JNIEnv* env, jclass clazz, jint attrib, jint value) {
    if (!showingWindow || !isUseStackQueueCall) {
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
        (jlong) showingWindow, attrib, value
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

