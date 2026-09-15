package com.qcl.launcher.launcher.list.install;

public class DownloadTaskListBean {

    public String name;
    public String url;
    public String path;
    public String sha1;
    public int progress = 0;
    public String fallbackUrl;

    public DownloadTaskListBean withFallback(String originalUrl) {
        fallbackUrl = originalUrl != null && !originalUrl.equals(url) ? originalUrl : null;
        return this;
    }

    public String urlForAttempt(int attempt) {
        return attempt >= 2 && fallbackUrl != null ? fallbackUrl : url;
    }

    public DownloadTaskListBean(String name,String url,String path,String sha1){
        this.name = name;
        this.url = url;
        this.path = path;
        this.sha1 = sha1;
    }
}
