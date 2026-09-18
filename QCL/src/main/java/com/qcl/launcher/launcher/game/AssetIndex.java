package com.qcl.launcher.launcher.game;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.string.ToStringBuilder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes2.dex */
public final class AssetIndex {

    @SerializedName("map_to_resources")
    private final boolean mapToResources;

    @SerializedName("objects")
    private final Map<String, AssetObject> objects;

    @SerializedName("virtual")
    private final boolean virtual;

    public AssetIndex() {
        this(false, Collections.emptyMap());
    }

    public AssetIndex(boolean z, Map<String, AssetObject> map) {
        this.mapToResources = z;
        this.virtual = z;
        this.objects = new HashMap(map);
    }

    public boolean isVirtual() {
        return this.virtual || this.mapToResources;
    }

    public Map<String, AssetObject> getObjects() {
        return Collections.unmodifiableMap(this.objects);
    }

    public String toString() {
        return new ToStringBuilder(this).append("virtual", Boolean.valueOf(this.virtual)).append("objects", this.objects).toString();
    }
}
