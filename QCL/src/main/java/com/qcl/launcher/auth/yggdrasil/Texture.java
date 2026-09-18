package com.qcl.launcher.auth.yggdrasil;

import java.util.Map;

/* loaded from: classes2.dex */
public final class Texture {
    private final Map<String, String> metadata;
    private final String url;

    public Texture() {
        this(null, null);
    }

    public Texture(String str, Map<String, String> map) {
        this.url = str;
        this.metadata = map;
    }

    public String getUrl() {
        return this.url;
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }
}
