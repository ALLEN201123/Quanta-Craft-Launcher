package com.qcl.launcher.launcher.game;

/* loaded from: classes2.dex */
public class AssetIndexInfo extends IdDownloadInfo {
    private final long totalSize;

    public AssetIndexInfo() {
        this("", "");
    }

    public AssetIndexInfo(String str, String str2) {
        this(str, str2, null);
    }

    public AssetIndexInfo(String str, String str2, String str3) {
        this(str, str2, str3, 0);
    }

    public AssetIndexInfo(String str, String str2, String str3, int i) {
        this(str, str2, str3, i, 0L);
    }

    public AssetIndexInfo(String str, String str2, String str3, int i, long j) {
        super(str, str2, str3, i);
        this.totalSize = j;
    }

    public long getTotalSize() {
        return this.totalSize;
    }
}
