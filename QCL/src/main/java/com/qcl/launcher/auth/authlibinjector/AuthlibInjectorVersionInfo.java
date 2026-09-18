package com.qcl.launcher.auth.authlibinjector;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

/* loaded from: classes2.dex */
public class AuthlibInjectorVersionInfo {

    @SerializedName("build_number")
    public int buildNumber;

    @SerializedName("checksums")
    public Map<String, String> checksums;

    @SerializedName("download_url")
    public String downloadUrl;

    @SerializedName("version")
    public String version;
}
