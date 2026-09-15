package com.qcl.launcher.utils.platform;

import android.app.ActivityManager;
import android.content.Context;

public class MemoryUtils {

    public static int getTotalDeviceMemory(Context ctx){
        ActivityManager actManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (int) (memInfo.totalMem / 1048576L);
    }

    public static int getFreeDeviceMemory(Context ctx){
        ActivityManager actManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return (int) (memInfo.availMem / 1048576L);
    }

    /** 当前进程是否 64 位（32 位进程装不下大堆，默认必须给小一点） */
    private static boolean isProcess64Bit() {
        try {
            Class<?> process = Class.forName("android.os.Process");
            return (Boolean) process.getMethod("is64Bit").invoke(null);
        } catch (Throwable t) {
            return true;
        }
    }

    public static int findBestRAMAllocation(Context context) {
        // 32 位进程：地址空间有限，默认直接给 1GB（给 2GB 会导致 VM 起不来）
        if (!isProcess64Bit()) {
            return 1024;
        }
        int totalDeviceMemory = getTotalDeviceMemory(context);
        if (totalDeviceMemory < 1024) {
            return 512;
        } else if (totalDeviceMemory < 2048) {
            return 1024;
        } else if (totalDeviceMemory < 4096) {
            return 2048;
        } else {
            // 大内存机型默认也给 2GB（用户要求：默认从 4GB 降到 2GB）
            return 2048;
        }
    }


    /** 设置界面允许的最大内存：32 位进程 8GB、64 位进程 32GB（电脑模拟器大内存也能填） */
    public static int getMaxAllowedRam(Context context) {
        return isProcess64Bit() ? 32768 : 8192;
    }
}
