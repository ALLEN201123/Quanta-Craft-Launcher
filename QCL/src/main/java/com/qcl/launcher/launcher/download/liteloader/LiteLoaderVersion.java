package com.qcl.launcher.launcher.download.liteloader;

import com.qcl.launcher.launcher.game.Library;
import java.util.Collection;
import java.util.Collections;

/* loaded from: classes2.dex */
public final class LiteLoaderVersion {
    private final String file;
    private final int lastSuccessfulBuild;
    private final Collection<Library> libraries;
    private final String md5;
    private final String timestamp;
    private final String tweakClass;
    private final String version;

    public LiteLoaderVersion() {
        this("", "", "", "", "", 0, Collections.emptySet());
    }

    public LiteLoaderVersion(String str, String str2, String str3, String str4, String str5, int i, Collection<Library> collection) {
        this.tweakClass = str;
        this.file = str2;
        this.version = str3;
        this.md5 = str4;
        this.timestamp = str5;
        this.lastSuccessfulBuild = i;
        this.libraries = collection;
    }

    public String getTweakClass() {
        return this.tweakClass;
    }

    public String getFile() {
        return this.file;
    }

    public String getVersion() {
        return this.version;
    }

    public String getMd5() {
        return this.md5;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public int getLastSuccessfulBuild() {
        return this.lastSuccessfulBuild;
    }

    public Collection<Library> getLibraries() {
        return Collections.unmodifiableCollection(this.libraries);
    }
}
