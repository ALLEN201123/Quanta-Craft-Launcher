#include "egl_bridge.h"
#include <jni.h>
#include <assert.h>
#include <dlfcn.h>
#include <limits.h>

#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <unistd.h>

#include <EGL/egl.h>
#include "GL/osmesa.h"
#include "GL/gl.h"
#include "ctxbridges/osmesa_loader.h"
#include "ctxbridges/egl_loader.h"
#include "virgl/virgl.h"

#ifdef GLES_TEST
#include <GLES2/gl2.h>
#endif

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/rect.h>
#include <string.h>
#include <inttypes.h>
#include "environ/environ.h"
#include <android/dlext.h>
#include "ctxbridges/bridge_tbl.h"
#include "ctxbridges/osm_bridge.h"
#include <androidnsbypass/nsbypass_t.h>
#include <androidnsbypass/nsbypass.h>
#include "global_state.h"
#include "log.h"
#include <stdatomic.h>

// 由 input_bridge_v3.c 提供，上报 monitor size 到 Java 侧 GLFW
extern void updateMonitorSize(int width, int height);

#define GLFW_CLIENT_API 0x22001
/* Consider GLFW_NO_API as Vulkan API */
#define GLFW_NO_API 0
#define GLFW_OPENGL_API 0x30001

// This means that the function is an external API and that it will be used
#define EXTERNAL_API __attribute__((used))
// This means that you are forced to have this function/variable for ABI compatibility
#define ABI_COMPAT __attribute__((unused))

EGLConfig config;
struct PotatoBridge potatoBridge;

// MC 最近一次请求的交换间隔；-1 表示尚未请求
static int lastSwapInterval = -1;

#define RENDERER_GL4ES 1
#define RENDERER_VK_ZINK 2
#define RENDERER_VULKAN 4

static atomic_uint fps = 0;

EXTERNAL_API void pojavTerminate() {
    printf("EGLBridge: Terminating\n");

    switch (pojav_environ->config_renderer) {
        case RENDERER_GL4ES: {
            eglMakeCurrent_p(potatoBridge.eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE,
                             EGL_NO_CONTEXT);
            eglDestroySurface_p(potatoBridge.eglDisplay, potatoBridge.eglSurface);
            eglDestroyContext_p(potatoBridge.eglDisplay, potatoBridge.eglContext);
            eglTerminate_p(potatoBridge.eglDisplay);
            eglReleaseThread_p();

            potatoBridge.eglContext = EGL_NO_CONTEXT;
            potatoBridge.eglDisplay = EGL_NO_DISPLAY;
            potatoBridge.eglSurface = EGL_NO_SURFACE;
        }
            break;

            //case RENDERER_VIRGL:
        case RENDERER_VK_ZINK: {
            // Nothing to do here
        }
            break;
    }
}

// ★★★ 2026-09-18 用户指令「彻底删除旧栈，把旧桥的名字给新桥」：本桥已成**唯一**渲染桥，
// 旧的 libpojavexec.so 已删除，历史上"两个 so 注册同名 JNI → 后加载者被忽略"的隐患
// 不复存在。原 `..._setupBridgeWindow` 与 `..._setupBridgeWindowNew` 两份重复实现已合并为
// 一个（保留 Java 侧现在调用的 `setupBridgeWindowNew` 名），避免同函数两份副本漂移。
JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_setupBridgeWindowNew(JNIEnv *env, ABI_COMPAT jclass clazz,
                                                             jobject surface) {
    // 首个窗口由 pojavInit 应用交换间隔；此处处理窗口重建（旋转、分屏等）：
    // 生产者状态会随新窗口重置，若不重新应用，MC 不会再次发起交换间隔调用，帧率会退回锁定在屏幕刷新率
    bool windowRecreated = pojav_environ->pojavWindow != NULL;
    pojav_environ->pojavWindow = ANativeWindow_fromSurface(env, surface);
    if (windowRecreated && pojav_environ->config_renderer != RENDERER_VULKAN) {
        if (lastSwapInterval >= 0) setNativeWindowSwapInterval(pojav_environ->pojavWindow, lastSwapInterval);
        else if (!getenv("POJAV_VSYNC_IN_ZINK")) setNativeWindowSwapInterval(pojav_environ->pojavWindow, 0);
    }
    if (br_setup_window != NULL) br_setup_window();
}


JNIEXPORT void JNICALL
Java_net_kdt_pojavlaunch_utils_JREUtils_releaseBridgeWindow(ABI_COMPAT JNIEnv *env,
                                                            ABI_COMPAT jclass clazz) {
    ANativeWindow_release(pojav_environ->pojavWindow);
}

EXTERNAL_API void *pojavGetCurrentContext() {
    if (pojav_environ->config_renderer == RENDERER_VIRGL) {
        return virglGetCurrentContext();
    }
    // SDL 模式下渲染桥未初始化（br_* 为 NULL），返回 NULL 而非空指针调用
    if (br_get_current == NULL) {
        return NULL;
    }
    return br_get_current();
}

// 已加载的 Vulkan 句柄缓存（set_vulkan_ptr 与 loadTurnipVulkan 共用，各 ABI 都需要）
static void* g_vulkan_ptr = NULL;

#ifdef ADRENO_POSSIBLE

bool checkAdrenoGraphics() {
    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL_NO_DISPLAY || eglInitialize(eglDisplay, NULL, NULL) != EGL_TRUE)
        return false;

    EGLint egl_attributes[] = {
        EGL_BLUE_SIZE, 8, EGL_GREEN_SIZE, 8, EGL_RED_SIZE, 8,
        EGL_ALPHA_SIZE, 8, EGL_DEPTH_SIZE, 24, EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_NONE
    };

    EGLint num_configs = 0;
    if (eglChooseConfig(eglDisplay, egl_attributes, NULL, 0, &num_configs) != EGL_TRUE || num_configs == 0) {
        eglTerminate(eglDisplay);
        return false;
    }

    EGLConfig eglConfig;
    eglChooseConfig(eglDisplay, egl_attributes, &eglConfig, 1, &num_configs);

    const EGLint egl_context_attributes[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    EGLContext context = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, egl_context_attributes);
    if (context == EGL_NO_CONTEXT) {
        eglTerminate(eglDisplay);
        return false;
    }

    if (eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, context) != EGL_TRUE) {
        eglDestroyContext(eglDisplay, context);
        eglTerminate(eglDisplay);
        return false;
    }

    const char* vendor = (const char*)glGetString(GL_VENDOR);
    const char* renderer = (const char*)glGetString(GL_RENDERER);

    bool is_adreno = (vendor && renderer && strcmp(vendor, "Qualcomm") == 0 && strstr(renderer, "Adreno") != NULL);

    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(eglDisplay, context);
    eglTerminate(eglDisplay);

    return is_adreno;
}

static struct android_namespace_t* vulkanLoaderNs;

void* loadTurnipVulkan() {
    if (g_vulkan_ptr) return g_vulkan_ptr;
    if (!checkAdrenoGraphics())
        return NULL;

    const char* cache_dir = getenv("TMPDIR");
    vulkanLoaderNs = private_create_namespace(
            "vulkan-loader-NS",
            NULL,
            NULL,
            ANDROID_NAMESPACE_TYPE_SHARED_ISOLATED,
            NULL,
            NULL,
            __builtin_return_address(0)
            );
    // 先加载 hook，使其符号优先进入符号表从而拦截 android_dlopen_ext
    linker_ns_dlopen("liblinkerhook.so", RTLD_LOCAL | RTLD_NOW, vulkanLoaderNs);
    // 授予命名空间访问系统库的权限
    private_link_namespaces_all_libs(vulkanLoaderNs, get_escape_namespace());
#if defined(__aarch64__) || defined(__x86_64__)
#define VULKAN_LOADER_PATH "/system/lib64/libvulkan.so"
#elif defined(__arm__) || defined(__i386__)
#define VULKAN_LOADER_PATH "/system/lib/libvulkan.so"
#endif
    return linker_ns_dlopen_unique(
            VULKAN_LOADER_PATH,
            cache_dir,
            RTLD_LOCAL | RTLD_NOW,
            vulkanLoaderNs);
}

#endif

static void set_vulkan_ptr(void* ptr) {
    g_vulkan_ptr = ptr;
    char envval[64];
    sprintf(envval, "%"PRIxPTR, (uintptr_t)ptr);
    setenv("VULKAN_PTR", envval, 1);
}

void load_vulkan() {
    if(getenv("VULKAN_DRIVER_SYSTEM") == NULL && android_get_device_api_level() >= 28) {
#ifdef ADRENO_POSSIBLE
        void* result = loadTurnipVulkan();
        if(result != NULL) {
            FCL_LOG("AdrenoSupp: Loaded Turnip, loader address: %p", result);
            set_vulkan_ptr(result);
            return;
        }
#endif
    }
    FCL_LOG("OSMDroid: loading vulkan regularly...");
    void* vulkan_ptr = dlopen("libvulkan.so", RTLD_LAZY | RTLD_LOCAL);
    FCL_LOG("OSMDroid: loaded vulkan, ptr=%p", vulkan_ptr);
    set_vulkan_ptr(vulkan_ptr);
}

int pojavInitOpenGL() {
    // Only affects GL4ES as of now
    const char *forceVsync = getenv("FORCE_VSYNC");
    // 1.1.0 QCL：getenv 可能返回 NULL（env 未设置时），原版直接 strcmp 会 SIGSEGV
    // （真机实测 fault addr 0x45 崩在 JVM Main thread）。Java 侧已补设 FORCE_VSYNC，
    // 这里再兜一层防御，避免将来 env 缺失时再次原生崩溃。
    if (forceVsync != NULL && !strcmp(forceVsync, "true"))
        pojav_environ->force_vsync = true;

    // NOTE: Override for now.
    const char *renderer = getenv("POJAV_RENDERER");
    if (renderer == NULL) renderer = "opengles2"; // 1.1.0 QCL：env 缺失时兜底，避免 strncmp(NULL)
    // ★★★ 2026-09-17 修复（1.20.6 黑屏 / Render thread 忙循环的根因）：
    // "ng_gl4es" 是 QCL Java 侧的渲染器内部名（Krypton Wrapper），**不是** native 认识的协议值。
    // native 侧只认 opengles2 / opengles3 / opengles3_desktopgl_zink_kopper / vulkan_zink /
    // gallium_virgl / gallium_freedreno / custom_gallium。
    // 若把 "ng_gl4es" 直接喂进来，下面所有分支都不匹配 → set_*_bridge_tbl() 从未被调用
    //   → br_init == NULL → 调 NULL 函数指针（实测表现为忙循环烧 CPU 不返回）。
    // Java 侧已修（JREUtils 不再覆盖成 ng_gl4es），这里再兜一层，防止将来别的入口漏改。
    if (!strcmp(renderer, "ng_gl4es")) renderer = "opengles3";
    if (!strncmp("opengles", renderer, 8)) {
        pojav_environ->config_renderer = RENDERER_GL4ES;
        if (!strcmp(renderer, "opengles3_desktopgl_zink_kopper")) {
            load_vulkan();
            setenv("GALLIUM_DRIVER", "zink", 1);
            setenv("MESA_ANDROID_NO_KMS_SWRAST", "1", 1);
            setenv("MESA_LOADER_DRIVER_OVERRIDE", "zink", 1);
        }
        set_gl_bridge_tbl();
    }

    if (!strcmp(renderer, "gallium_virgl")) {
        pojav_environ->config_renderer = RENDERER_VIRGL;
        setenv("GALLIUM_DRIVER", "virpipe", 1);
        loadSymbolsVirGL();
        virglInit();
        return 0;
    }

    if (!strcmp(renderer, "vulkan_zink")) {
        pojav_environ->config_renderer = RENDERER_VK_ZINK;
        load_vulkan();
        setenv("GALLIUM_DRIVER", "zink", 1);
        set_osm_bridge_tbl();
    }

    if (!strcmp(renderer, "gallium_freedreno")) {
        pojav_environ->config_renderer = RENDERER_VK_ZINK;
        load_vulkan();
        setenv("GALLIUM_DRIVER", "freedreno", 1);
        setenv("MESA_LOADER_DRIVER_OVERRIDE", "kgsl", 1);
        set_osm_bridge_tbl();
    }

    if (!strcmp(renderer, "custom_gallium")) {
        pojav_environ->config_renderer = RENDERER_VK_ZINK;
        load_vulkan();
        set_osm_bridge_tbl();
    }

    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInitOpenGL: calling br_init (renderer=%s)", renderer);
    // ★★★ 2026-09-17 兜底：br_init 为 NULL 说明上面没有任何分支匹配（渲染器名写错/新增渲染器漏配），
    // 直接调用会跳 NULL 指针。此处直接报错返回，让上层给出可读的错误而不是黑屏忙循环。
    if (br_init == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG",
                            "pojavInitOpenGL: br_init is NULL (renderer='%s' matched no bridge!)", renderer);
        return -1;
    }
    if (br_init()) {
        __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInitOpenGL: br_init OK, setup_window");
        br_setup_window();
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInitOpenGL: br_init returned false");
    }

    return 0;
}

// 获取当前线程的 JNIEnv（未附着则先 Attach，不 Detach，保留渲染线程的附着状态）
static JNIEnv *get_attached_env(JavaVM *jvm) {
    JNIEnv *jvm_env = NULL;
    jint env_result = (*jvm)->GetEnv(jvm, (void **) &jvm_env, JNI_VERSION_1_4);
    if (env_result == JNI_EDETACHED) {
        env_result = (*jvm)->AttachCurrentThread(jvm, &jvm_env, NULL);
    }
    if (env_result != JNI_OK) {
        printf("get_attached_env failed: %i\n", env_result);
        return NULL;
    }
    return jvm_env;
}

EXTERNAL_API int pojavInit() {
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInit: enter, window=%p", pojav_environ->pojavWindow);
    pojav_environ->glfwThreadVmEnv = get_attached_env(pojav_environ->runtimeJavaVMPtr);
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInit: get_attached_env=%p", pojav_environ->glfwThreadVmEnv);
    if (pojav_environ->glfwThreadVmEnv == NULL) {
        printf("Failed to attach Java-side JNIEnv to GLFW thread\n");
        return 0;
    }
    ANativeWindow_acquire(pojav_environ->pojavWindow);
    pojav_environ->savedWidth = ANativeWindow_getWidth(pojav_environ->pojavWindow);
    pojav_environ->savedHeight = ANativeWindow_getHeight(pojav_environ->pojavWindow);
    ANativeWindow_setBuffersGeometry(pojav_environ->pojavWindow, pojav_environ->savedWidth,
                                     pojav_environ->savedHeight,
                                     AHARDWAREBUFFER_FORMAT_R8G8B8X8_UNORM);
    updateMonitorSize(pojav_environ->savedWidth, pojav_environ->savedHeight);
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInit: calling pojavInitOpenGL");
    pojavInitOpenGL();
    __android_log_print(ANDROID_LOG_ERROR, "QCL_DBG", "pojavInit: pojavInitOpenGL returned");
    // 垂直同步开关关闭时主动切入异步模式，解除帧率对屏幕刷新率的锁定；开启时交由 MC 的交换间隔调用决定
    //
    // ★★★ 2026-09-17（画面冻结根因）—— 此处**刻意不设置** swap interval。
    // 设置 swap interval 需要合法的 EGLDisplay，而 g_EglDisplay 要 eglInitialize /
    // eglMakeCurrent 之后才有效，此刻（pojavInitOpenGL 刚返回、还没建 surface）仍是 NULL。
    // 拿 NULL display 调 eglSwapInterval 属未定义行为：实测在 MuMu 上会让 GLThread
    // 永久阻塞在内核态（utime 不涨、只涨 stime，多次采样数字完全不变）。
    //
    // 正确位置已挪到 ctxbridges/gl_bridge.c 的 gl_make_current() 里 ——
    // 在 eglMakeCurrent_p 成功之后的第一时间调 gl_swap_interval(0)。
    // 那里 display/context 都真实可用，且同样不碰 ANativeWindow 结构（避免
    // MuMu 上 android::Surface::hook_setSwapInterval 崩溃）。
    //
    // 为什么不设成 1 也行不通：MuMu 模拟器没有真实显示同步信号，
    // eg 拿默认的 interval=1 会让 eglSwapBuffers 永远等不到 vsync。
    (void) 0;
    return 1;
}

EXTERNAL_API void pojavSetWindowHint(int hint, int value) {
    if (hint != GLFW_CLIENT_API) return;
    switch (value) {
        case GLFW_NO_API:
            pojav_environ->config_renderer = RENDERER_VULKAN;
            /* Nothing to do: initialization is handled in Java-side */
            // pojavInitVulkan();
            break;
        case GLFW_OPENGL_API: {
            const char *renderer = getenv("POJAV_RENDERER");
            if (renderer == NULL) break; // 1.1.0 QCL：env 缺失时直接跳过，避免 strncmp(NULL)
            // ★★★ 2026-09-17：与 pojavInitOpenGL 同一处修复 —— ng_gl4es 需归一到 opengles3，
            // 否则 config_renderer 不会被置为 RENDERER_GL4ES，后续 swap/uiThread 分支全部走空。
            if (!strcmp(renderer, "ng_gl4es")) renderer = "opengles3";
            if (strncmp(renderer, "opengles", 8) == 0) {
                pojav_environ->config_renderer = RENDERER_GL4ES;
            } else if (!strcmp(renderer, "vulkan_zink")) {
                pojav_environ->config_renderer = RENDERER_VK_ZINK;
            }
            /* Nothing to do: initialization is called in pojavCreateContext */
            // pojavInitOpenGL();
            break;
        }
        default:
            printf("GLFW: Unimplemented API 0x%x\n", value);
            abort();
    }
}

EXTERNAL_API void pojavSwapBuffers() {
    atomic_fetch_add(&fps, 1);
    // SDL 模式（config_renderer 未匹配任何渲染桥）下这些调用必须为 no-op，
    // 否则 br_* 函数指针为 NULL 会空指针崩溃（fixPojavGLContext 路径）
    if (pojav_environ->config_renderer == RENDERER_VK_ZINK
     || pojav_environ->config_renderer == RENDERER_GL4ES) {
        br_swap_buffers();
    }
    if (pojav_environ->config_renderer == RENDERER_VIRGL) {
        virglSwapBuffers();
    }
}


EXTERNAL_API void pojavMakeCurrent(void *window) {
    if (pojav_environ->config_renderer == RENDERER_VK_ZINK
     || pojav_environ->config_renderer == RENDERER_GL4ES) {
        br_make_current((basic_render_window_t *) window);
    }
    if (pojav_environ->config_renderer == RENDERER_VIRGL) {
        virglMakeCurrent(window);
    }
}

EXTERNAL_API void *pojavCreateContext(void *contextSrc) {
    if (pojav_environ->config_renderer == RENDERER_VULKAN)
        return (void *) pojav_environ->pojavWindow;

    if (pojav_environ->config_renderer == RENDERER_VIRGL)
        return virglCreateContext(contextSrc);

    if (br_init_context == NULL)
        return NULL;

    return br_init_context((basic_render_window_t *) contextSrc);
}

void *maybe_load_vulkan() {
    // We use the env var because
    // 1. it's easier to do that
    // 2. it won't break if something will try to load vulkan and osmesa simultaneously
    if (getenv("VULKAN_PTR") == NULL) load_vulkan();
    return (void *) strtoul(getenv("VULKAN_PTR"), NULL, 0x10);
}

EXTERNAL_API JNIEXPORT jlong JNICALL
Java_org_lwjgl_vulkan_VK_getVulkanDriverHandle(ABI_COMPAT JNIEnv *env, ABI_COMPAT jclass thiz) {
    printf("EGLBridge: LWJGL-side Vulkan loader requested the Vulkan handle\n");
    return (jlong) maybe_load_vulkan();
}

EXTERNAL_API void pojavSwapInterval(int interval) {
    lastSwapInterval = interval;

    // 注意：渲染器字符串 "opengles2"/"opengles3_*" 在 pojavInitOpenGL 里统一映射为
    // RENDERER_GL4ES（!strncmp("opengles", renderer, 8)），所以下面这个分支就是 opengles2 的路径。
    if (pojav_environ->config_renderer == RENDERER_VK_ZINK
     || pojav_environ->config_renderer == RENDERER_GL4ES) {
        br_swap_interval(interval);
    }
    if (pojav_environ->config_renderer == RENDERER_VIRGL) {
        virglSwapInterval(interval);
    }
}

JNIEXPORT jint JNICALL
Java_org_lwjgl_glfw_CallbackBridge_getFps(JNIEnv *env, jclass clazz) {
    return atomic_exchange(&fps, 0);
}

// SDL 路径下由 eglSwapBuffers 代理调用来计帧（sdl_hook.c）
EXTERNAL_API void calculateFPS(void) {
    atomic_fetch_add(&fps, 1);
}

EXTERNAL_API JNIEXPORT void JNICALL
Java_org_lwjgl_vulkan_VK_updateFps(ABI_COMPAT JNIEnv *env, ABI_COMPAT jclass thiz) {
    atomic_fetch_add(&fps, 1);
}