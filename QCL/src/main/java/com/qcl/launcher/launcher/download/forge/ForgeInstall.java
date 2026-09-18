package com.qcl.launcher.launcher.download.forge;

import com.qcl.launcher.launcher.game.Artifact;

/* loaded from: classes2.dex */
public final class ForgeInstall {
    private final String filePath;
    private final String logo;
    private final String minecraft;
    private final String mirrorList;
    private final Artifact path;
    private final String profileName;
    private final String target;
    private final String version;
    private final String welcome;

    public ForgeInstall() {
        this(null, null, null, null, null, null, null, null, null);
    }

    public ForgeInstall(String str, String str2, Artifact artifact, String str3, String str4, String str5, String str6, String str7, String str8) {
        this.profileName = str;
        this.target = str2;
        this.path = artifact;
        this.version = str3;
        this.filePath = str4;
        this.welcome = str5;
        this.minecraft = str6;
        this.mirrorList = str7;
        this.logo = str8;
    }

    public String getProfileName() {
        return this.profileName;
    }

    public String getTarget() {
        return this.target;
    }

    public Artifact getPath() {
        return this.path;
    }

    public String getVersion() {
        return this.version;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getWelcome() {
        return this.welcome;
    }

    public String getMinecraft() {
        return this.minecraft;
    }

    public String getMirrorList() {
        return this.mirrorList;
    }

    public String getLogo() {
        return this.logo;
    }
}
