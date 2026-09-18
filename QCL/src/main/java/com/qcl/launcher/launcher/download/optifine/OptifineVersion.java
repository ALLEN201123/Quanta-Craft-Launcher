package com.qcl.launcher.launcher.download.optifine;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/* loaded from: classes2.dex */
public class OptifineVersion implements Serializable {

    @SerializedName("__v")
    public int __v;

    @SerializedName("filename")
    public String fileName;

    @SerializedName("_id")
    public String id;

    @SerializedName("mcversion")
    public String mcVersion;

    @SerializedName("patch")
    public String patch;

    @SerializedName("type")
    public String type;

    public OptifineVersion(String str, String str2, String str3, String str4, int i, String str5) {
        this.id = str;
        this.mcVersion = str2;
        this.patch = str3;
        this.type = str4;
        this.__v = i;
        this.fileName = str5;
    }
}
