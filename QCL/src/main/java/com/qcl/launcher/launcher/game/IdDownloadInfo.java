package com.qcl.launcher.launcher.game;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public class IdDownloadInfo extends DownloadInfo {

    @SerializedName("id")
    public String id;

    public IdDownloadInfo() {
        this("", "");
    }

    public IdDownloadInfo(String str, String str2) {
        this(str, str2, null);
    }

    public IdDownloadInfo(String str, String str2, String str3) {
        this(str, str2, str3, 0);
    }

    public IdDownloadInfo(String str, String str2, String str3, int i) {
        super(str2, str3, i);
        this.id = str;
    }
}
