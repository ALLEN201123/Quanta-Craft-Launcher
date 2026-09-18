package com.qcl.launcher.launcher.download.forge;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.utils.gson.tools.Validation;

/* loaded from: classes2.dex */
public final class ForgeInstallProfile implements Validation {

    @SerializedName("install")
    private final ForgeInstall install;

    @SerializedName("versionInfo")
    private final Version versionInfo;

    public ForgeInstallProfile(ForgeInstall forgeInstall, Version version) {
        this.install = forgeInstall;
        this.versionInfo = version;
    }

    public ForgeInstall getInstall() {
        return this.install;
    }

    public Version getVersionInfo() {
        return this.versionInfo;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (this.install == null) {
            throw new JsonParseException("InstallProfile install cannot be null");
        }
        if (this.versionInfo == null) {
            throw new JsonParseException("InstallProfile versionInfo cannot be null");
        }
    }
}
