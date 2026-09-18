package com.qcl.launcher.launcher.mod.curse;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public final class CurseMetaMod {

    @SerializedName(alternate = {"downloadUrl"}, value = "DownloadURL")
    private final String downloadURL;

    @SerializedName(alternate = {"fileName"}, value = "FileName")
    private final String fileName;

    @SerializedName("FileNameOnDisk")
    private final String fileNameOnDisk;

    @SerializedName(alternate = {"id"}, value = "Id")
    private final int id;

    public CurseMetaMod() {
        this(0, "", "", "");
    }

    public CurseMetaMod(int i, String str, String str2, String str3) {
        this.id = i;
        this.fileName = str;
        this.fileNameOnDisk = str2;
        this.downloadURL = str3;
    }

    public int getId() {
        return this.id;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileNameOnDisk() {
        return this.fileNameOnDisk;
    }

    public String getDownloadURL() {
        return this.downloadURL;
    }
}
