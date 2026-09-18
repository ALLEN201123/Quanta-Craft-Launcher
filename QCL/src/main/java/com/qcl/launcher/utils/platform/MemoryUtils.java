package com.qcl.launcher.utils.platform;

import android.app.ActivityManager;
import android.content.Context;

/* loaded from: classes2.dex */
public class MemoryUtils {
    public static int getTotalDeviceMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return (int) (memoryInfo.totalMem / 1048576);
    }

    public static int getFreeDeviceMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return (int) (memoryInfo.availMem / 1048576);
    }

    private static boolean isProcess64Bit() {
        try {
            return ((Boolean) Class.forName("android.os.Process").getMethod("is64Bit", new Class[0]).invoke(null, new Object[0])).booleanValue();
        } catch (Throwable unused) {
            return true;
        }
    }

    public static int findBestRAMAllocation(Context context) {
        if (!isProcess64Bit()) {
            return 1024;
        }
        int totalDeviceMemory = getTotalDeviceMemory(context);
        if (totalDeviceMemory < 1024) {
            return 512;
        }
        return totalDeviceMemory < 2048 ? 1024 : 2048;
    }

    public static int getMaxAllowedRam(Context context) {
        return isProcess64Bit() ? 32768 : 8192;
    }
}
