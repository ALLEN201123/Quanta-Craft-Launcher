package com.qcl.launcher.utils.io;

import java.io.IOException;
import java.net.URL;

/* loaded from: classes2.dex */
public class ResponseCodeException extends IOException {
    private final String data;
    private final int responseCode;
    private final URL url;

    public ResponseCodeException(URL url, int i) {
        super("Unable to request url " + url + ", response code: " + i);
        this.url = url;
        this.responseCode = i;
        this.data = null;
    }

    public ResponseCodeException(URL url, int i, String str) {
        super("Unable to request url " + url + ", response code: " + i + ", data: " + str);
        this.url = url;
        this.responseCode = i;
        this.data = str;
    }

    public URL getUrl() {
        return this.url;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getData() {
        return this.data;
    }
}
