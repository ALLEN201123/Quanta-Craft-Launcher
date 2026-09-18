package com.qcl.launcher.launcher.download.liteloader;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public final class LiteLoaderRepository {

    @SerializedName("classifier")
    private final String classifier;

    @SerializedName("stream")
    private final String stream;

    @SerializedName("type")
    private final String type;

    @SerializedName("url")
    private final String url;

    public LiteLoaderRepository() {
        this("", "", "", "");
    }

    public LiteLoaderRepository(String str, String str2, String str3, String str4) {
        this.stream = str;
        this.type = str2;
        this.url = str3;
        this.classifier = str4;
    }

    public String getStream() {
        return this.stream;
    }

    public String getType() {
        return this.type;
    }

    public String getUrl() {
        return this.url;
    }

    public String getClassifier() {
        return this.classifier;
    }
}
