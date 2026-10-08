LOCAL_PATH := $(call my-dir)
HERE_PATH := $(LOCAL_PATH)

# ★★★★★ 1.5.0：**-DADRENO_POSSIBLE**（照 FCL CMakeLists.txt:17-20）
#   FCL：`if (${ANDROID_ABI} STREQUAL "arm64-v8a") add_definitions(-DADRENO_POSSIBLE) endif ()`
#   为什么至关重要（QCL 之前踩的最大的坑）：
#     · egl_bridge.c 里 `loadTurnipVulkan()` 与 `load_vulkan()` 中加载 Turnip 的那段
#       **整段被 `#ifdef ADRENO_POSSIBLE` 包着**；
#     · QCL 的 Android.mk **从来没定义过这个宏** ⇒ 预处理器把那段代码**整段剔除**
#       ⇒ `loadTurnipVulkan()` 从未进入二进制、`libvulkan_freedreno.so` 永远用不上
#       ⇒ 无硬件 Vulkan 的设备只能回落 OpenGL ⇒ 26.3+ 除零崩溃。
#   只在 arm64-v8a 定义：Turnip 的 so（libvulkan_freedreno.so，10MB）只随 arm64 打包，
#   其它架构定义了也加载不到，白占体积。
#   ★ 用 TARGET_ARCH_ABI（= arm64-v8a）而不是 TARGET_ARCH（= arm64）——
#     上一版写成 TARGET_ARCH 导致条件永不成立，宏没定义、Turnip 依旧被剔除。
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    ADRENO_FLAG := -DADRENO_POSSIBLE
endif

include $(CLEAR_VARS)
# 1.1.1：bytehook 预编译库（bytedance），供 SDL3 native hooks 链接（BYTEHOOK_CALL_PREV 等宏）。
LOCAL_MODULE := bytehook
LOCAL_SRC_FILES := bytehook/prebuilt/$(TARGET_ARCH_ABI)/libbytehook.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
# ★★★ 2026-09-18 用户指令：**把旧桥的名字给新桥，不能改 GLFW 里的**。
# 因此本模块名由 `pojavexec_new` 改回 `pojavexec` —— 产出的 so 就是 `libpojavexec.so`。
#
# 这么做的好处（正是用户要的）：
#   · GLFW stub（定制版 GLFW.class）里 -Dqcl.pojavexec.lib 的**默认值就是 "pojavexec"**，
#     改名后即使属性丢失/被清空，加载路径自动正确，不再有"属性错配 → 加载到不存在的库"风险。
#   · 与 FCL 命名完全一致（FCL 也只有一个 libpojavexec.so）。
#   · 旧的 libpojavexec.so（v1.0.9 遗留）已从 jni/Android.mk 删除，不会与它撞名。
LOCAL_LDLIBS := -ldl -llog -landroid
LOCAL_MODULE := pojavexec
LOCAL_SHARED_LIBRARIES := bytehook
LOCAL_SRC_FILES := \
    egl_bridge.c \
    qcl_bridge_compat.c \
    input_bridge_v3.c \
    jre_launcher.c \
    utils.c \
    ctxbridges/loader_dlopen.c \
    ctxbridges/gl_bridge.c \
    ctxbridges/osm_bridge.c \
    ctxbridges/egl_loader.c \
    ctxbridges/osmesa_loader.c \
    ctxbridges/swap_interval_no_egl.c \
    environ/environ.c \
    virgl/virgl.c \
    androidnsbypass/android_linker_ns.cpp \
    androidnsbypass/elf_soname_patcher.c \
    androidnsbypass/nsbypass.c \
    androidnsbypass/nsbypass_dlfcn.c \
    androidnsbypass/utils.c \
    native_hooks/sdl_hook.c \
    native_hooks/sdl_dlopen_hook.c \
    native_hooks/exit_hook.c \
    native_hooks/chmod_hook.c \
    jvm_hooks/lwjgl_dlopen_hook.c \
    bytehook/qcl_nominal_exit.c
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH) \
    $(LOCAL_PATH)/ctxbridges \
    $(LOCAL_PATH)/environ \
    $(LOCAL_PATH)/virgl \
    $(LOCAL_PATH)/GL \
    $(LOCAL_PATH)/androidnsbypass \
    $(LOCAL_PATH)/androidnsbypass/include \
    $(LOCAL_PATH)/androidnsbypass/include/androidnsbypass \
    $(LOCAL_PATH)/androidnsbypass/include/fasthook \
    $(LOCAL_PATH)/androidnsbypass/include/linkernsbypass_compat \
    $(LOCAL_PATH)/androidnsbypass/liblinkernsbypass_compat \
    $(LOCAL_PATH)/bytehook \
    $(LOCAL_PATH)/native_hooks
LOCAL_CFLAGS := -fvisibility=default $(ADRENO_FLAG)
# ★★★★★ 1.5.0：arm64 时补链 EGL / GLESv2（照 FCL CMakeLists.txt:182-189）
#   `checkAdrenoGraphics()`（在 ADRENO_POSSIBLE 块里）**直接调用** eglGetDisplay / eglInitialize /
#   eglChooseConfig / eglCreateContext / eglTerminate / glGetString —— 这些是真实的库符号，
#   必须显式链接。原先没定义 ADRENO_POSSIBLE 时那段代码被剔除，所以一直没暴露这个需求；
#   一旦按 FCL 定义了宏，不补这两个库就会 ld.lld 报
#   `undefined symbol: eglGetDisplay / eglInitialize / ...`。
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_LDLIBS += -lEGL -lGLESv2
endif
include $(BUILD_SHARED_LIBRARY)

# ===== ★★★ 1.5.0：liblinkerhook —— Turnip Vulkan 驱动加载器（移植自 FCL）=====
# 作用：拦截 libvulkan 加载器的 android_dlopen_ext，让它把 libvulkan_freedreno.so（Turnip，Mesa 的
#      Vulkan 驱动）加载进来。**没有它，包里带的 Turnip 永远用不上**，26.3+ 就只能回落 OpenGL →
#      DeviceLimits.minUniformOffsetAlignment() 返回 0 → Mth.roundToward 除零崩溃。
# 原理：egl_bridge.c 的 loadTurnipVulkan() 先建一个隔离 namespace，把本库 dlopen 进去（符号先进
#      符号表），随后系统 libvulkan.so 解析 android_dlopen_ext 时就会命中我们的实现。
# ★ 必须**独立成 so**（不能并进 pojavexec）：要靠 -z global 让符号在 namespace 内优先于 linker 解析。
# ★ androidnsbypass 的 5 个源文件在这里**再编一份**（QCL 没有独立的 libandroidnsbypass.so），
#   与 pojavexec 里那份互不干扰，各自定义符号。
include $(CLEAR_VARS)
LOCAL_MODULE := linkerhook
LOCAL_SRC_FILES := \
    driver_helper/internal_android_dlopen_hook/turnip/hook.c \
    androidnsbypass/android_linker_ns.cpp \
    androidnsbypass/elf_soname_patcher.c \
    androidnsbypass/nsbypass.c \
    androidnsbypass/nsbypass_dlfcn.c \
    androidnsbypass/utils.c
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH) \
    $(LOCAL_PATH)/androidnsbypass \
    $(LOCAL_PATH)/androidnsbypass/include \
    $(LOCAL_PATH)/androidnsbypass/include/androidnsbypass \
    $(LOCAL_PATH)/androidnsbypass/include/fasthook \
    $(LOCAL_PATH)/androidnsbypass/include/linkernsbypass_compat \
    $(LOCAL_PATH)/androidnsbypass/liblinkernsbypass_compat \
    $(LOCAL_PATH)/driver_helper/internal_android_dlopen_hook/turnip
LOCAL_CFLAGS := -fvisibility=default
LOCAL_CPPFLAGS := -std=c++17 -fvisibility=default -Wno-unused-parameter
LOCAL_LDFLAGS := -Wl,-z,global
LOCAL_LDLIBS := -llog -ldl
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
# ★★★ 1.1.0（移植自 FCL）：jsound —— OpenJDK libjsound 核心 + OpenAL 平台后端。
# 为四套 JRE（8/17/21/25）提供 javax.sound.sampled 的原生实现。
# 远古版本（LWJGL 2 时代）走 Java Sound 播放音频，Android 上的 JRE 不带 libjsound，
# 所以 Pojav 后端远古版本"没声音"—— 这个库补上该实现（FCL 已用同方案修复）。
# 核心 .c/.h vendor 自 openjdk/jdk17u（GPL-2.0 + Classpath，与 GPL-3.0 兼容）。
LOCAL_LDLIBS := -llog -ldl
LOCAL_MODULE := jsound
LOCAL_SRC_FILES := \
    jsound/Utilities.c \
    jsound/Platform.c \
    jsound/DirectAudioDevice.c \
    jsound/DirectAudioDeviceProvider.c \
    jsound/PortMixer.c \
    jsound/PortMixerProvider.c \
    jsound/MidiOutDevice.c \
    jsound/MidiOutDeviceProvider.c \
    jsound/MidiInDevice.c \
    jsound/MidiInDeviceProvider.c \
    jsound/PlatformMidi.c \
    jsound/jsound_openal.c \
    jsound/jdk8_compat.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/jsound
LOCAL_CFLAGS := -DX_PLATFORM=X_LINUX -D_LITTLE_ENDIAN -DUSE_DAUDIO=TRUE -DUSE_PORTS=FALSE -DUSE_PLATFORM_MIDI_OUT=FALSE -DUSE_PLATFORM_MIDI_IN=FALSE -fvisibility=default -Wno-error
include $(BUILD_SHARED_LIBRARY)
