package com.qcl.launcher.update;

import java.util.List;

/* loaded from: classes2.dex */
public class LauncherVersion {
    public String date;
    public String netdiskUrl;
    public String updateLog;
    public List<String> url;
    public int versionCode;
    public String versionName;

    public LauncherVersion(int i, String str, String str2, List<String> list, String str3) {
        this.versionCode = i;
        this.versionName = str;
        this.date = str2;
        this.url = list;
        this.updateLog = str3;
    }
}
