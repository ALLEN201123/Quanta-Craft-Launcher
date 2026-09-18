#pragma once

#include <stdbool.h>

static JavaVM* runtimeJavaVMPtr;
static JNIEnv* runtimeJNIEnvPtr_ANDROID;
static JNIEnv* runtimeJNIEnvPtr_JRE;

static JavaVM* dalvikJavaVMPtr;
static JNIEnv* dalvikJNIEnvPtr_ANDROID;
//static JNIEnv* dalvikJNIEnvPtr_JRE;

// ★★★ 2026-09-18 用户指令「彻底删除旧栈，全部改成 FCL 新的」+ 修 1.20.6 触屏：
// 原来这里还定义了 `static bool isInputReady, isCursorEntered, isUseStackQueueCall;`
// —— 这是 **Pojav 旧桥的存储方式**，与 FCL 新版（environ.h 的 pojav_environ 结构体字段）
// 形成了「两套存储」：nativeSetInputReady() 写 static 那份，而 GLFW 主循环 / pojavPumpEvents
// 读的是 pojav_environ->isInputReady 那份 → 后者恒为 0 → 所有输入被 `if (isInputReady)` 静默拦掉。
// 这正是 1.20.6「鼠标能移动、按不了按钮」的残留病根（上半场只修了 GLFW_invoke_* 回调指针）。
// FCL 原版里这几个状态位**只存在于 pojav_environ**，utils.h 没有它们 —— 现照 FCL 删除。
// 保留静态的只有 QCL 独有的抓取逻辑变量（isPrepareGrabPos / grabCursor*）与窗口尺寸缓存。
//
// ★ showingWindow 同理：原来这里还有 `static long showingWindow;`，与
// environ.h 结构体的 `long showingWindow` 形成又一套「两套存储」——
// nglfwSetShowingWindow() 写 static 那份，而 FCL 全链路读的是 pojav_environ->showingWindow。
// 现已照 FCL 全部改成 pojav_environ->showingWindow，static 那份删除。
//
// ★ savedWidth/savedHeight 同理：原 static 那份被 nativeSendScreenSize() 写，
// 而 egl_bridge.c 写 pojav_environ->savedWidth、pojavStartPumping 读
// pojav_environ->savedWidth 做 monitor size 上报 —— 两份永远对不上。
// 现已照 FCL 统一到 pojav_environ，static 那份删除。
static bool isPrepareGrabPos;

jboolean attachThread(bool isAndroid, JNIEnv** secondJNIEnvPtr);
char** convert_to_char_array(JNIEnv *env, jobjectArray jstringArray);
jobjectArray convert_from_char_array(JNIEnv *env, char **charArray, int num_rows);
void free_char_array(JNIEnv *env, jobjectArray jstringArray, const char **charArray);
jstring convertStringJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jstring srcStr);

void closeGLFWWindow();
void hookExec();
JNIEXPORT jstring JNICALL Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(JNIEnv* env, jclass clazz, jint action, jbyteArray copySrc);

// ===== 1.1.1 SDL3 集成（移植自 FCL utils.h）=====
#define DECL_DLSYM(fn) typedef typeof(&fn) fn##_t;

#define SET_DLSYM_PTR(handle, fn)                     \
    fn##_t fn##_p;                                   \
    do {                                             \
        dlerror();                                   \
        void *_p = dlsym((handle), #fn);             \
        const char *_e = dlerror();                  \
        if (_e || !_p) {                             \
            __android_log_print(ANDROID_LOG_ERROR, "QCL", "dlsym(" #fn ") failed: %s", \
                                _e ? _e : "unknown error"); \
        }                                            \
        fn##_p = (fn##_t)_p;                         \
    } while (0)

#define TRY_ATTACH_ENV(env_name, vm, error_message, then) JNIEnv* env_name;\
do {                                                                       \
    env_name = get_attached_env(vm);                                       \
    if(env_name == NULL) {                                                 \
        printf(error_message);                                             \
        then                                                               \
    }                                                                      \
} while(0)

JNIEnv* get_attached_env(JavaVM* jvm);
// SDL launcher integration（1.1.1 移植自 FCL）
#define NOTIF_TYPE_SDL 0
#define ACTION_INIT_LAUNCHER_INTEGRATION 0
#define ACTION_SEND_TEXTBOX_RECT 1
bool notifyLauncher(JNIEnv* dvm_env, int type, int actions[], int len);

jintArray convertIntArrayJVM(JNIEnv* srcEnv, JNIEnv* dstEnv, jintArray srcIntArray);
