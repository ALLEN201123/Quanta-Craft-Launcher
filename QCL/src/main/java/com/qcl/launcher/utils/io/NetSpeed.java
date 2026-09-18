package com.qcl.launcher.utils.io;

import android.net.TrafficStats;

/* loaded from: classes2.dex */
public class NetSpeed {
    private static final String TAG = "NetSpeed";
    private long lastTotalRxBytes = 0;
    private long lastTimeStamp = 0;

    public String getNetSpeed(int i) {
        long totalRxBytes = getTotalRxBytes(i);
        long currentTimeMillis = System.currentTimeMillis();
        long j = ((totalRxBytes - this.lastTotalRxBytes) * 1000) / (currentTimeMillis - this.lastTimeStamp);
        this.lastTimeStamp = currentTimeMillis;
        this.lastTotalRxBytes = totalRxBytes;
        if (j < 1024) {
            return String.valueOf(j) + " B/s";
        }
        if (j >= 1024 && j < 1048576) {
            return String.valueOf(j / 1024) + " KB/s";
        }
        return String.valueOf(j / 1048576) + " MB/s";
    }

    public long getTotalRxBytes(int i) {
        if (TrafficStats.getUidRxBytes(i) == -1) {
            return 0L;
        }
        return TrafficStats.getTotalRxBytes();
    }
}
