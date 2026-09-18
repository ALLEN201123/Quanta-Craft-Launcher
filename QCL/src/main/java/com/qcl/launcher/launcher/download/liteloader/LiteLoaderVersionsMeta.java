package com.qcl.launcher.launcher.download.liteloader;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public final class LiteLoaderVersionsMeta {

    @SerializedName("authors")
    private final String authors;

    @SerializedName("description")
    private final String description;

    @SerializedName("url")
    private final String url;

    public LiteLoaderVersionsMeta() {
        this("", "", "");
    }

    public LiteLoaderVersionsMeta(String str, String str2, String str3) {
        this.description = str;
        this.authors = str2;
        this.url = str3;
    }

    public String getDescription() {
        return this.description;
    }

    public String getAuthors() {
        return this.authors;
    }

    public String getUrl() {
        return this.url;
    }
}
