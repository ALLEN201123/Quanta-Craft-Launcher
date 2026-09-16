LOCAL_PATH := $(call my-dir)
HERE_PATH := $(LOCAL_PATH)

# include $(HERE_PATH)/crash_dump/libbase/Android.mk
# include $(HERE_PATH)/crash_dump/libbacktrace/Android.mk
# include $(HERE_PATH)/crash_dump/debuggerd/Android.mk


LOCAL_PATH := $(HERE_PATH)


include $(CLEAR_VARS)
LOCAL_MODULE     := xhook
        LOCAL_SRC_FILES  := xhook/xhook.c \
                    xhook/xh_core.c \
                    xhook/xh_elf.c \
                    xhook/xh_jni.c \
                    xhook/xh_log.c \
                    xhook/xh_util.c \
                    xhook/xh_version.c
        LOCAL_C_INCLUDES := $(LOCAL_PATH)/xhook
LOCAL_CFLAGS     := -Wall -Wextra -Werror -fvisibility=hidden
LOCAL_CONLYFLAGS := -std=c11
LOCAL_LDLIBS     := -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
# Link GLESv2 for test
LOCAL_LDLIBS := -ldl -llog -landroid
# -lGLESv2
LOCAL_MODULE := pojavexec
# LOCAL_CFLAGS += -DDEBUG
# -DGLES_TEST
# 1.1.0：渲染桥从 HMCL-PE 旧版（egl_bridge.c 仅 GL4ES/OSMesa）移植为 FCL 新版
# （ctxbridges 模块化渲染桥，支持 Mesa zink-on-Vulkan 桌面 GL）。
# 新增：ctxbridges/*（渲染桥）、environ/*（渲染状态）、virgl/*（virgl 支持）、
#       androidnsbypass/*（linker 命名空间绕过，加载驱动库用）、qcl_bridge_compat.c（兼容层）
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
    $(LOCAL_PATH)/androidnsbypass/include
LOCAL_CPPFLAGS := -std=c++17
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := istdio
LOCAL_SHARED_LIBRARIES := xhook
LOCAL_SRC_FILES := \
    stdio_is.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/xhook
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := pojavexec_awt
LOCAL_SRC_FILES := \
    awt_bridge.c
include $(BUILD_SHARED_LIBRARY)

# Helper to get current thread
# include $(CLEAR_VARS)
# LOCAL_MODULE := thread64helper
# LOCAL_SRC_FILES := thread_helper.cpp
# include $(BUILD_SHARED_LIBRARY)

# fake lib for linker
include $(CLEAR_VARS)
LOCAL_MODULE := awt_headless
include $(BUILD_SHARED_LIBRARY)

# libawt_xawt without X11, used to get Caciocavallo working
LOCAL_PATH := $(HERE_PATH)/awt_xawt
include $(CLEAR_VARS)
LOCAL_MODULE := awt_xawt
# LOCAL_CFLAGS += -DHEADLESS
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_SHARED_LIBRARIES := awt_headless
LOCAL_SRC_FILES := xawt_fake.c
include $(BUILD_SHARED_LIBRARY)

# delete fake libs after linked
$(info $(shell (rm $(HERE_PATH)/../jniLibs/*/libawt_headless.so)))

