package com.qcl.launcher.launcher.download.liteloader;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.launcher.game.Library;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/* loaded from: classes2.dex */
public final class LiteLoaderBranch {

    @SerializedName("libraries")
    private final Collection<Library> libraries;

    @SerializedName("com.mumfrey:liteloader")
    private final Map<String, LiteLoaderVersion> liteLoader;

    public LiteLoaderBranch() {
        this(Collections.emptySet(), Collections.emptyMap());
    }

    public LiteLoaderBranch(Collection<Library> collection, Map<String, LiteLoaderVersion> map) {
        this.libraries = collection;
        this.liteLoader = map;
    }

    public Collection<Library> getLibraries() {
        return Collections.unmodifiableCollection(this.libraries);
    }

    public Map<String, LiteLoaderVersion> getLiteLoader() {
        return Collections.unmodifiableMap(this.liteLoader);
    }
}
