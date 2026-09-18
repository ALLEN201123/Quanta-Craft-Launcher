package com.qcl.launcher.launcher.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/* loaded from: classes2.dex */
public final class LibrariesDownloadInfo {
    private final LibraryDownloadInfo artifact;
    private final Map<String, LibraryDownloadInfo> classifiers;

    public LibrariesDownloadInfo(LibraryDownloadInfo libraryDownloadInfo) {
        this(libraryDownloadInfo, null);
    }

    public LibrariesDownloadInfo(LibraryDownloadInfo libraryDownloadInfo, Map<String, LibraryDownloadInfo> map) {
        this.artifact = libraryDownloadInfo;
        this.classifiers = map == null ? null : new HashMap(map);
    }

    public LibraryDownloadInfo getArtifact() {
        return this.artifact;
    }

    public Map<String, LibraryDownloadInfo> getClassifiers() {
        Map<String, LibraryDownloadInfo> map = this.classifiers;
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }
}
