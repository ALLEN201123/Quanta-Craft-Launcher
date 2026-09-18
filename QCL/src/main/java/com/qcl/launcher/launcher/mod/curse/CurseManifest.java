package com.qcl.launcher.launcher.mod.curse;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.launcher.mod.ModpackManifest;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import java.util.Collections;
import java.util.List;

/* loaded from: classes2.dex */
public final class CurseManifest implements ModpackManifest {
    public static final String MINECRAFT_MODPACK = "minecraftModpack";

    @SerializedName("author")
    private final String author;

    @SerializedName("files")
    private final List<CurseManifestFile> files;

    @SerializedName("manifestType")
    private final String manifestType;

    @SerializedName("manifestVersion")
    private final int manifestVersion;

    @SerializedName("minecraft")
    private final CurseManifestMinecraft minecraft;

    @SerializedName("name")
    private final String name;

    @SerializedName("overrides")
    private final String overrides;

    @SerializedName("version")
    private final String version;

    public CurseManifest() {
        this("minecraftModpack", 1, "", "1.0", "", "overrides", new CurseManifestMinecraft(), Collections.emptyList());
    }

    public CurseManifest(String str, int i, String str2, String str3, String str4, String str5, CurseManifestMinecraft curseManifestMinecraft, List<CurseManifestFile> list) {
        this.manifestType = str;
        this.manifestVersion = i;
        this.name = str2;
        this.version = str3;
        this.author = str4;
        this.overrides = str5;
        this.minecraft = curseManifestMinecraft;
        this.files = list;
    }

    public String getManifestType() {
        return this.manifestType;
    }

    public int getManifestVersion() {
        return this.manifestVersion;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getOverrides() {
        return this.overrides;
    }

    public CurseManifestMinecraft getMinecraft() {
        return this.minecraft;
    }

    public List<CurseManifestFile> getFiles() {
        return this.files;
    }

    public CurseManifest setFiles(List<CurseManifestFile> list) {
        return new CurseManifest(this.manifestType, this.manifestVersion, this.name, this.version, this.author, this.overrides, this.minecraft, list);
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackManifest
    public ModpackProvider getProvider() {
        return CurseModpackProvider.INSTANCE;
    }
}
