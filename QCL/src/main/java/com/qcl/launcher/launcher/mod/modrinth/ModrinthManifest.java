package com.qcl.launcher.launcher.mod.modrinth;

import com.google.gson.JsonParseException;
import com.qcl.launcher.launcher.mod.ModpackManifest;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.gson.tools.TolerableValidationException;
import com.qcl.launcher.utils.gson.tools.Validation;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/* loaded from: classes2.dex */
public class ModrinthManifest implements ModpackManifest, Validation {
    private final Map<String, String> dependencies;
    private final List<File> files;
    private final int formatVersion;
    private final String game;
    private final String name;
    private final String summary;
    private final String versionId;

    public ModrinthManifest(String str, int i, String str2, String str3, String str4, List<File> list, Map<String, String> map) {
        this.game = str;
        this.formatVersion = i;
        this.versionId = str2;
        this.name = str3;
        this.summary = str4;
        this.files = list;
        this.dependencies = map;
    }

    public String getGame() {
        return this.game;
    }

    public int getFormatVersion() {
        return this.formatVersion;
    }

    public String getVersionId() {
        return this.versionId;
    }

    public String getName() {
        return this.name;
    }

    public String getSummary() {
        String str = this.summary;
        return str == null ? "" : str;
    }

    public List<File> getFiles() {
        return this.files;
    }

    public Map<String, String> getDependencies() {
        return this.dependencies;
    }

    public String getGameVersion() {
        return this.dependencies.get("minecraft");
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackManifest
    public ModpackProvider getProvider() {
        return ModrinthModpackProvider.INSTANCE;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException, TolerableValidationException {
        Map<String, String> map = this.dependencies;
        if (map == null || map.get("minecraft") == null) {
            throw new JsonParseException("missing Modrinth.dependencies.minecraft");
        }
    }

    /* loaded from: classes2.dex */
    public static class File {
        private final List<URL> downloads;
        private final Map<String, String> env;
        private final int fileSize;
        private final Map<String, String> hashes;
        private final String path;

        public File(String str, Map<String, String> map, Map<String, String> map2, List<URL> list, int i) {
            this.path = str;
            this.hashes = map;
            this.env = map2;
            this.downloads = list;
            this.fileSize = i;
        }

        public String getPath() {
            return this.path;
        }

        public Map<String, String> getHashes() {
            return this.hashes;
        }

        public Map<String, String> getEnv() {
            return this.env;
        }

        public List<URL> getDownloads() {
            return this.downloads;
        }

        public int getFileSize() {
            return this.fileSize;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            File file = (File) obj;
            return this.fileSize == file.fileSize && this.path.equals(file.path) && this.hashes.equals(file.hashes) && this.env.equals(file.env) && this.downloads.equals(file.downloads);
        }

        public int hashCode() {
            return Objects.hash(this.path, this.hashes, this.env, this.downloads, Integer.valueOf(this.fileSize));
        }
    }
}
