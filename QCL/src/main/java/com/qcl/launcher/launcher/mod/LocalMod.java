package com.qcl.launcher.launcher.mod;

import java.util.HashSet;
import java.util.Objects;

/* loaded from: classes2.dex */
public class LocalMod {
    private final String id;
    private final ModLoaderType modLoaderType;
    private final HashSet<LocalModFile> files = new HashSet<>();
    private final HashSet<LocalModFile> oldFiles = new HashSet<>();

    public LocalMod(String str, ModLoaderType modLoaderType) {
        this.id = str;
        this.modLoaderType = modLoaderType;
    }

    public String getId() {
        return this.id;
    }

    public ModLoaderType getModLoaderType() {
        return this.modLoaderType;
    }

    public HashSet<LocalModFile> getFiles() {
        return this.files;
    }

    public HashSet<LocalModFile> getOldFiles() {
        return this.oldFiles;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LocalMod localMod = (LocalMod) obj;
        return Objects.equals(this.id, localMod.id) && this.modLoaderType == localMod.modLoaderType;
    }

    public int hashCode() {
        return Objects.hash(this.id, this.modLoaderType);
    }
}
