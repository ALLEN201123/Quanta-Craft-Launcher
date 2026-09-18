package com.qcl.launcher.launcher.list.install;

/* loaded from: classes2.dex */
public class DownloadTaskListBean {
    public String fallbackUrl;
    public String name;
    public String path;
    public int progress = 0;
    public String sha1;
    public String url;

    public DownloadTaskListBean withFallback(String str) {
        if (str == null || str.equals(this.url)) {
            str = null;
        }
        this.fallbackUrl = str;
        return this;
    }

    public String urlForAttempt(int i) {
        String str;
        return (i < 2 || (str = this.fallbackUrl) == null) ? this.url : str;
    }

    public DownloadTaskListBean(String str, String str2, String str3, String str4) {
        this.name = str;
        this.url = str2;
        this.path = str3;
        this.sha1 = str4;
    }
}
