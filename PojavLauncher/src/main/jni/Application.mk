# NDK_TOOLCHAIN_VERSION := 4.9
APP_PLATFORM := android-21
APP_STL := system
# 1.1.0 正式：全 ABI。AGP externalNativeBuild 场景下由 build.gradle 的
# -Darch 属性（对齐 FCL）决定实际编译架构，本文件供直接 ndk-build 调用时使用。
# （历史教训：此文件先后遗留过 armeabi-v7a / x86_64 两代 TEST-ONLY 配置，
#  导致编出的 so 进不了目标 ABI 的 lib/ 目录，设备跑旧 so、改动"不生效"。）
APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
