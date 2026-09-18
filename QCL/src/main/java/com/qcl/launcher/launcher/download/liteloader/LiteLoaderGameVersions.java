package com.qcl.launcher.launcher.download.liteloader;

import com.google.gson.annotations.SerializedName;

/* loaded from: classes2.dex */
public final class LiteLoaderGameVersions {

    @SerializedName("artefacts")
    private final LiteLoaderBranch artifacts;

    @SerializedName("repo")
    private final LiteLoaderRepository repoitory;

    @SerializedName("snapshots")
    private final LiteLoaderBranch snapshots;

    public LiteLoaderGameVersions() {
        this(null, null, null);
    }

    public LiteLoaderGameVersions(LiteLoaderRepository liteLoaderRepository, LiteLoaderBranch liteLoaderBranch, LiteLoaderBranch liteLoaderBranch2) {
        this.repoitory = liteLoaderRepository;
        this.artifacts = liteLoaderBranch;
        this.snapshots = liteLoaderBranch2;
    }

    public LiteLoaderRepository getRepoitory() {
        return this.repoitory;
    }

    public LiteLoaderBranch getArtifacts() {
        return this.artifacts;
    }

    public LiteLoaderBranch getSnapshots() {
        return this.snapshots;
    }
}
