package com.qcl.launcher.launcher.mod;

import com.google.gson.JsonParseException;
import com.qcl.launcher.utils.gson.tools.Validation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes2.dex */
public final class ModpackConfiguration<T> implements Validation {
    private final T manifest;
    private final String name;
    private final List<FileInformation> overrides;
    private final String type;
    private final String version;

    public ModpackConfiguration() {
        this(null, null, "", null, Collections.emptyList());
    }

    public ModpackConfiguration(T t, String str, String str2, String str3, List<FileInformation> list) {
        this.manifest = t;
        this.type = str;
        this.name = str2;
        this.version = str3;
        this.overrides = new ArrayList(list);
    }

    public T getManifest() {
        return this.manifest;
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public ModpackConfiguration<T> setManifest(T t) {
        return new ModpackConfiguration<>(t, this.type, this.name, this.version, this.overrides);
    }

    public ModpackConfiguration<T> setOverrides(List<FileInformation> list) {
        return new ModpackConfiguration<>(this.manifest, this.type, this.name, this.version, list);
    }

    public ModpackConfiguration<T> setVersion(String str) {
        return new ModpackConfiguration<>(this.manifest, this.type, this.name, str, this.overrides);
    }

    public List<FileInformation> getOverrides() {
        return Collections.unmodifiableList(this.overrides);
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (this.manifest == null) {
            throw new JsonParseException("MinecraftInstanceConfiguration missing `manifest`");
        }
        if (this.type == null) {
            throw new JsonParseException("MinecraftInstanceConfiguration missing `type`");
        }
    }

    /* loaded from: classes2.dex */
    public static class FileInformation implements Validation {
        private final String downloadURL;
        private final String hash;
        private final String path;

        public FileInformation() {
            this(null, null);
        }

        public FileInformation(String str, String str2) {
            this(str, str2, null);
        }

        public FileInformation(String str, String str2, String str3) {
            this.path = str;
            this.hash = str2;
            this.downloadURL = str3;
        }

        public String getPath() {
            return this.path;
        }

        public String getDownloadURL() {
            return this.downloadURL;
        }

        public String getHash() {
            return this.hash;
        }

        @Override // com.qcl.launcher.utils.gson.tools.Validation
        public void validate() throws JsonParseException {
            if (this.path == null) {
                throw new JsonParseException("FileInformation missing `path`.");
            }
            if (this.hash == null) {
                throw new JsonParseException("FileInformation missing file hash code.");
            }
        }
    }
}
