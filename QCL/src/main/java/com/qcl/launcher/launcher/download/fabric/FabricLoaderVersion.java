package com.qcl.launcher.launcher.download.fabric;

/* loaded from: classes2.dex */
public class FabricLoaderVersion {
    public int build;
    public String maven;
    public String separator;
    public boolean stable;
    public String version;

    public FabricLoaderVersion(String str, int i, String str2, String str3, boolean z) {
        this.separator = str;
        this.build = i;
        this.maven = str2;
        this.version = str3;
        this.stable = z;
    }
}
