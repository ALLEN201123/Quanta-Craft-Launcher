LOCAL_PATH := $(call my-dir)
HERE_PATH := $(LOCAL_PATH)

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
LOCAL_CFLAGS := -fvisibility=default
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
