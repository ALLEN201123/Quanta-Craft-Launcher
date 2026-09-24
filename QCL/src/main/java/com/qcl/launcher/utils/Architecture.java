package com.qcl.launcher.utils;

import android.os.Build;

/* loaded from: classes2.dex */
public class Architecture {
    public static int ARCH_ARM = 2;
    public static int ARCH_ARM64 = 1;
    public static int ARCH_X86 = 4;
    public static int ARCH_X86_64 = 8;
    public static int UNSUPPORTED_ARCH = -1;
    private static int bitMode;

    public static int getInstalledAbiArchitecture() {
        // ★ 1.3.1 修复：**不再用 APK 的 primaryCpuAbi** ✗
        //   全架构（universal）APK 在 MuMu 等 x86_64 模拟器上会被系统装成 arm64，
        //   于是 primaryCpuAbi="arm64-v8a" → 误判成 ARM64 → 装了 aarch64 的 JRE →
        //   libjvm.so 架构不符 → JVM 起不来（表现为游戏冻死）✓
        //   JRE 必须匹配**设备**，所以一律以设备首选 ABI 为准 ✓
        try {
            int dev = preferredDeviceArch();
            if (dev != UNSUPPORTED_ARCH) {
                return dev;
            }
        } catch (Throwable unused) {
        }
        try {
            boolean booleanValue = ((Boolean) Class.forName("android.os.Process").getMethod("is64Bit", new Class[0]).invoke(null, new Object[0])).booleanValue();
            return isx86Device() ? booleanValue ? ARCH_X86_64 : ARCH_X86 : booleanValue ? ARCH_ARM64 : ARCH_ARM;
        } catch (Throwable unused2) {
            return getDeviceArchitecture();
        }
    }

    /** ★ 1.3.1：设备真实架构 —— 一律以设备「首选 ABI」SUPPORTED_ABIS[0] 为准 ✓
     *  （MuMu 的 SUPPORTED_64_BIT_ABIS 可能只报 arm64-v8a，会把 x86_64 设备误判成 arm64 ✗） */
    public static int preferredDeviceArch() {
        String[] preferred = Build.SUPPORTED_ABIS;
        if (preferred != null && preferred.length > 0) {
            int a = archAsInt(preferred[0]);
            if (a != UNSUPPORTED_ARCH) {
                return a;
            }
        }
        return UNSUPPORTED_ARCH;
    }

    public static boolean is64BitsDevice() {
        return Build.SUPPORTED_64_BIT_ABIS.length != 0;
    }

    public static boolean is32BitsDevice() {
        return !is64BitsDevice();
    }

    public static int getDeviceArchitecture() {
        // ★ 1.3.1：优先用设备首选 ABI（不再依赖 SUPPORTED_64_BIT_ABIS ✗）
        int preferred = preferredDeviceArch();
        if (preferred != UNSUPPORTED_ARCH) {
            return preferred;
        }
        return isx86Device() ? is64BitsDevice() ? ARCH_X86_64 : ARCH_X86 : is64BitsDevice() ? ARCH_ARM64 : ARCH_ARM;
    }

    public static boolean isx86Device() {
        String[] strArr = is64BitsDevice() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
        int i = is64BitsDevice() ? ARCH_X86_64 : ARCH_X86;
        for (String str : strArr) {
            if (archAsInt(str) == i) {
                return true;
            }
        }
        return false;
    }

    public static boolean isArmDevice() {
        return !isx86Device();
    }

    public static int archAsInt(String str) {
        String replace = str.toLowerCase().trim().replace(" ", "");
        if (replace.contains("arm64") || replace.equals("aarch64")) {
            return ARCH_ARM64;
        }
        if (replace.contains("arm") || replace.equals("aarch32")) {
            return ARCH_ARM;
        }
        if (replace.contains("x86_64") || replace.contains("amd64")) {
            return ARCH_X86_64;
        }
        if (replace.contains("x86") || (replace.startsWith("i") && replace.endsWith("86"))) {
            return ARCH_X86;
        }
        return UNSUPPORTED_ARCH;
    }

    public static String archAsString(int i) {
        return i == ARCH_ARM64 ? "arm64" : i == ARCH_ARM ? "arm" : i == ARCH_X86_64 ? "x86_64" : i == ARCH_X86 ? "x86" : "UNSUPPORTED_ARCH";
    }

    public static void setBitMode(int i) {
        bitMode = i;
    }

    public static boolean isProcess64Bit() {
        try {
            return ((Boolean) Class.forName("android.os.Process").getMethod("is64Bit", new Class[0]).invoke(null, new Object[0])).booleanValue();
        } catch (Throwable unused) {
            return true;
        }
    }

    public static int getRuntimeArchitecture() {
        int installedAbiArchitecture = getInstalledAbiArchitecture();
        boolean z = installedAbiArchitecture == ARCH_X86 || installedAbiArchitecture == ARCH_X86_64;
        return (bitMode == 1 && isProcess64Bit()) ? z ? ARCH_X86_64 : ARCH_ARM64 : (bitMode != 2 || isProcess64Bit()) ? installedAbiArchitecture : z ? ARCH_X86 : ARCH_ARM;
    }
}
