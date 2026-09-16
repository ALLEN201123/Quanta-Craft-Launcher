LOCAL_PATH := $(call my-dir)
HERE_PATH := $(LOCAL_PATH)

include $(CLEAR_VARS)
# ★★★ 1.1.0 隔离：第二套渲染桥（FCL 新版 ctxbridges，支持 Mesa zink-on-Vulkan 桌面 GL）。
# 编译成 libpojavexec_new.so —— 与 v1.0.9 的 libpojavexec.so 并存于 APK 的 native 目录，
# 由 Java 层按 MC 版本决定给 GLFW 加载哪一个（老版本→pojavexec，1.20.5+→pojavexec_new）。
# 共用文件（jre_launcher.c / utils.c）在本目录也放了一份副本，与旧桥零交集。
LOCAL_LDLIBS := -ldl -llog -landroid
LOCAL_MODULE := pojavexec_new
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
    androidnsbypass/utils.c
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
    $(LOCAL_PATH)/androidnsbypass/liblinkernsbypass_compat
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
