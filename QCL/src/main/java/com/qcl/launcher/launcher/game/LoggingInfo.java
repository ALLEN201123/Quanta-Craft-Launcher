package com.qcl.launcher.launcher.game;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public class LoggingInfo {

    @SerializedName("argument")
    public String argument;

    @SerializedName("file")
    public IdDownloadInfo file;

    @SerializedName("type")
    public String type;

    public LoggingInfo() {
        this(new IdDownloadInfo());
    }

    public LoggingInfo(IdDownloadInfo idDownloadInfo) {
        this(idDownloadInfo, "");
    }

    public LoggingInfo(IdDownloadInfo idDownloadInfo, String str) {
        this(idDownloadInfo, str, "");
    }

    public LoggingInfo(IdDownloadInfo idDownloadInfo, String str, String str2) {
        this.file = idDownloadInfo;
        this.argument = str;
        this.type = str2;
    }
}
