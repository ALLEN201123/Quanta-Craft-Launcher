package com.qcl.launcher.launcher.download.liteloader;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.Map;

/* loaded from: classes2.dex */
public final class LiteLoaderVersionsRoot {

    @SerializedName("meta")
    private final LiteLoaderVersionsMeta meta;

    @SerializedName("versions")
    private final Map<String, LiteLoaderGameVersions> versions;

    public LiteLoaderVersionsRoot() {
        this(Collections.emptyMap(), null);
    }

    public LiteLoaderVersionsRoot(Map<String, LiteLoaderGameVersions> map, LiteLoaderVersionsMeta liteLoaderVersionsMeta) {
        this.versions = map;
        this.meta = liteLoaderVersionsMeta;
    }

    public Map<String, LiteLoaderGameVersions> getVersions() {
        return Collections.unmodifiableMap(this.versions);
    }

    public LiteLoaderVersionsMeta getMeta() {
        return this.meta;
    }
}
