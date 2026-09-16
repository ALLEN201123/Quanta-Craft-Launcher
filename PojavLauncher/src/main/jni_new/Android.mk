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
