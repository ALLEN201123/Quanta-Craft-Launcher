package com.qcl.launcher.launcher.mod.multimc;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.IOUtils;
import java.io.IOException;
import java.util.List;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

/* loaded from: classes2.dex */
public final class MultiMCManifest {

    @SerializedName("components")
    private final List<MultiMCManifestComponent> components;

    @SerializedName("formatVersion")
    private final int formatVersion;

    public MultiMCManifest(int i, List<MultiMCManifestComponent> list) {
        this.formatVersion = i;
        this.components = list;
    }

    public int getFormatVersion() {
        return this.formatVersion;
    }

    public List<MultiMCManifestComponent> getComponents() {
        return this.components;
    }

    public static MultiMCManifest readMultiMCModpackManifest(ZipFile zipFile, String str) throws IOException {
        ZipArchiveEntry entry = zipFile.getEntry(str + "mmc-pack.json");
        if (entry == null) {
            return null;
        }
        MultiMCManifest multiMCManifest = (MultiMCManifest) JsonUtils.fromNonNullJson(IOUtils.readFullyAsString(zipFile.getInputStream(entry)), MultiMCManifest.class);
        if (multiMCManifest.getComponents() != null) {
            return multiMCManifest;
        }
        throw new IOException("mmc-pack.json malformed.");
    }

    /* loaded from: classes2.dex */
    public static final class MultiMCManifestCachedRequires {

        @SerializedName("equals")
        private final String equalsVersion;

        @SerializedName("suggests")
        private final String suggests;

        @SerializedName("uid")
        private final String uid;

        public MultiMCManifestCachedRequires(String str, String str2, String str3) {
            this.equalsVersion = str;
            this.uid = str2;
            this.suggests = str3;
        }

        public String getEqualsVersion() {
            return this.equalsVersion;
        }

        public String getUid() {
            return this.uid;
        }

        public String getSuggests() {
            return this.suggests;
        }
    }

    /* loaded from: classes2.dex */
    public static final class MultiMCManifestComponent {

        @SerializedName("cachedName")
        private final String cachedName;

        @SerializedName("cachedRequires")
        private final List<MultiMCManifestCachedRequires> cachedRequires;

        @SerializedName("cachedVersion")
        private final String cachedVersion;

        @SerializedName("dependencyOnly")
        private final boolean dependencyOnly;

        @SerializedName("important")
        private final boolean important;

        @SerializedName("uid")
        private final String uid;

        @SerializedName("version")
        private final String version;

        public MultiMCManifestComponent(boolean z, boolean z2, String str, String str2) {
            this(null, null, null, z, z2, str, str2);
        }

        public MultiMCManifestComponent(String str, List<MultiMCManifestCachedRequires> list, String str2, boolean z, boolean z2, String str3, String str4) {
            this.cachedName = str;
            this.cachedRequires = list;
            this.cachedVersion = str2;
            this.important = z;
            this.dependencyOnly = z2;
            this.uid = str3;
            this.version = str4;
        }

        public String getCachedName() {
            return this.cachedName;
        }

        public List<MultiMCManifestCachedRequires> getCachedRequires() {
            return this.cachedRequires;
        }

        public String getCachedVersion() {
            return this.cachedVersion;
        }

        public boolean isImportant() {
            return this.important;
        }

        public boolean isDependencyOnly() {
            return this.dependencyOnly;
        }

        public String getUid() {
            return this.uid;
        }

        public String getVersion() {
            return this.version;
        }
    }
}
