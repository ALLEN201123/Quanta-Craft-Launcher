package com.qcl.launcher.launcher.game;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.gson.tools.TolerableValidationException;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.platform.Architecture;
import com.qcl.launcher.utils.platform.OperatingSystem;
import com.qcl.launcher.utils.string.ToStringBuilder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/* loaded from: classes2.dex */
public class Library implements Comparable<Library>, Validation {

    @SerializedName("name")
    private final Artifact artifact;
    private final List<String> checksums;
    private final LibrariesDownloadInfo downloads;
    private final ExtractRules extract;

    @SerializedName(alternate = {"MMC-filename"}, value = "filename")
    private final String fileName;

    @SerializedName(alternate = {"MMC-hint"}, value = "hint")
    private final String hint;
    private final Map<OperatingSystem, String> natives;
    private final List<CompatibilityRule> rules;
    private final String url;

    public Library(Artifact artifact) {
        this(artifact, null, null);
    }

    public Library(Artifact artifact, String str) {
        this(artifact, str, null);
    }

    public Library(Artifact artifact, String str, LibrariesDownloadInfo librariesDownloadInfo) {
        this(artifact, str, librariesDownloadInfo, null, null, null, null, null, null);
    }

    public Library(Artifact artifact, String str, LibrariesDownloadInfo librariesDownloadInfo, List<String> list, ExtractRules extractRules, Map<OperatingSystem, String> map, List<CompatibilityRule> list2, String str2, String str3) {
        this.artifact = artifact;
        this.url = str;
        this.downloads = librariesDownloadInfo;
        this.extract = extractRules;
        this.natives = map;
        this.rules = list2;
        this.checksums = list;
        this.hint = str2;
        this.fileName = str3;
    }

    public String getGroupId() {
        return this.artifact.getGroup();
    }

    public String getArtifactId() {
        return this.artifact.getName();
    }

    public String getArtifactFileName() {
        return this.artifact.getFileName();
    }

    public String getName() {
        return this.artifact.toString();
    }

    public String getVersion() {
        return this.artifact.getVersion();
    }

    public String getClassifier() {
        if (this.artifact.getClassifier() == null) {
            Map<OperatingSystem, String> map = this.natives;
            if (map == null || !map.containsKey(OperatingSystem.CURRENT_OS)) {
                return null;
            }
            return this.natives.get(OperatingSystem.CURRENT_OS).replace("${arch}", Architecture.SYSTEM_ARCH.getBits().getBit());
        }
        return this.artifact.getClassifier();
    }

    public ExtractRules getExtract() {
        ExtractRules extractRules = this.extract;
        return extractRules == null ? ExtractRules.EMPTY : extractRules;
    }

    public boolean appliesToCurrentEnvironment() {
        return CompatibilityRule.appliesToCurrentEnvironment(this.rules);
    }

    public boolean isNative() {
        return this.natives != null && appliesToCurrentEnvironment();
    }

    protected LibraryDownloadInfo getRawDownloadInfo() {
        if (this.downloads == null) {
            return null;
        }
        if (isNative()) {
            return this.downloads.getClassifiers().get(getClassifier());
        }
        return this.downloads.getArtifact();
    }

    public String getPath() {
        LibraryDownloadInfo rawDownloadInfo = getRawDownloadInfo();
        if (rawDownloadInfo != null && rawDownloadInfo.getPath() != null) {
            return rawDownloadInfo.getPath();
        }
        return this.artifact.setClassifier(getClassifier()).getPath();
    }

    public LibraryDownloadInfo getDownload() {
        LibraryDownloadInfo rawDownloadInfo = getRawDownloadInfo();
        String path = getPath();
        return new LibraryDownloadInfo(path, (String) Optional.ofNullable(rawDownloadInfo).map(new Function() { // from class: com.qcl.launcher.launcher.game.Library$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((LibraryDownloadInfo) obj).getUrl();
            }
        }).orElse(((String) Optional.ofNullable(this.url).orElse("https://libraries.minecraft.net")).replaceAll("/+$", "") + "/" + path), rawDownloadInfo != null ? rawDownloadInfo.getSha1() : null, rawDownloadInfo != null ? rawDownloadInfo.getSize() : 0);
    }

    public boolean hasDownloadURL() {
        LibraryDownloadInfo rawDownloadInfo = getRawDownloadInfo();
        return rawDownloadInfo != null ? rawDownloadInfo.getUrl() != null : this.url != null;
    }

    public List<String> getChecksums() {
        return this.checksums;
    }

    public List<CompatibilityRule> getRules() {
        return this.rules;
    }

    public String getHint() {
        return this.hint;
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean is(String str, String str2) {
        return getGroupId().equals(str) && getArtifactId().equals(str2);
    }

    public String toString() {
        return new ToStringBuilder(this).append("name", getName()).toString();
    }

    @Override // java.lang.Comparable
    public int compareTo(Library library) {
        if (getName().compareTo(library.getName()) == 0) {
            return Boolean.compare(isNative(), library.isNative());
        }
        return getName().compareTo(library.getName());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Library)) {
            return false;
        }
        Library library = (Library) obj;
        return getName().equals(library.getName()) && isNative() == library.isNative();
    }

    public int hashCode() {
        return Objects.hash(getName(), Boolean.valueOf(isNative()));
    }

    public Library setClassifier(String str) {
        return new Library(this.artifact.setClassifier(str), this.url, this.downloads, this.checksums, this.extract, this.natives, this.rules, this.hint, this.fileName);
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException, TolerableValidationException {
        if (this.artifact == null) {
            throw new JsonParseException("Library.name cannot be null");
        }
    }
}
