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
# ★★★ 2026-09-18 用户指令「彻底删除旧栈，全部改成 FCL 新的」：
# 原 `pojavexec` 模块（旧桥 libpojavexec.so，由 egl_bridge.c + input_bridge_v3.c +
# jre_launcher.c + utils.c 编译）**已删除**。
# 删除原因：
#   1. 旧桥与新桥导出**完全相同的 17 个 JNI 符号**，ART 按"已加载库"顺序解析 native 方法，
#      先加载者胜 —— 语义上极脆弱，历史上已因此让高版本输入全落到 isInputReady 恒 0 的旧桥，
#      表现为"画面正常但点击毫无反应"。
#   2. 输入链路的两套存储问题（注册写文件内全局变量 / pump 读 pojav_environ 结构体字段）
#      在旧桥里无法用"只改一处"修好，因为旧桥根本没有 pojav_environ。
#   3. FCL 全版本统一走一套渲染桥，QCL 照 FCL 同款做法：只保留一套（名沿用 libpojavexec.so）。
# 备份：D:\_qcl_test\deleted_old_bridge\（含本文件与 4 个源文件的删除前副本）
#
# ⚠️ 注意：以下模块仍**必须保留**（与渲染桥无关的独立功能）：
#   · xhook   —— istdio（stdout 接管）依赖
#   · istdio  —— 日志窗的 native stdout 接管
#   · pojavexec_awt / awt_headless / awt_xawt —— Caciocavallo 的 AWT 原生支持
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

# ★★★ 2026-09-18：**唯一**的渲染/输入桥（FCL 新版 ctxbridges，支持 Mesa zink 桌面 GL）。
# 下面 include 进来的 jni_new/Android.mk 现在产出 `pojavexec`（即 libpojavexec.so）——
# 这是用户指令「把旧桥的名字给新桥」的结果：旧桥已删，名字留给新桥，与 GLFW.class 内置
# 默认值一致。全版本（b1.x / 1.7.x / 中间地带 / 1.20.5+ / 26.x）统一用它。
# ⚠️ 必须放在本文件最后：jni_new/Android.mk 会重设 LOCAL_PATH，不能影响上面的模块。
include $(HERE_PATH)/../jni_new/Android.mk

# delete fake libs after linked
$(info $(shell (rm $(HERE_PATH)/../jniLibs/*/libawt_headless.so)))
