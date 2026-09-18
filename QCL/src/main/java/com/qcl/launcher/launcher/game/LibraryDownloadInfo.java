package com.qcl.launcher.launcher.game;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public class LibraryDownloadInfo extends DownloadInfo {

    @SerializedName("path")
    private final String path;

    public LibraryDownloadInfo() {
        this(null);
    }

    public LibraryDownloadInfo(String str) {
        this(str, "");
    }

    public LibraryDownloadInfo(String str, String str2) {
        this(str, str2, null);
    }

    public LibraryDownloadInfo(String str, String str2, String str3) {
        this(str, str2, str3, 0);
    }

    public LibraryDownloadInfo(String str, String str2, String str3, int i) {
        super(str2, str3, i);
        this.path = str;
    }

    public String getPath() {
        return this.path;
    }
}
