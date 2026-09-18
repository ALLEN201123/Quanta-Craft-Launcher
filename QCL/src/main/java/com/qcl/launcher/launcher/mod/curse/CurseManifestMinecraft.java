package com.qcl.launcher.launcher.mod.curse;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.string.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes2.dex */
public final class CurseManifestMinecraft implements Validation {

    @SerializedName("version")
    private final String gameVersion;

    @SerializedName("modLoaders")
    private final List<CurseManifestModLoader> modLoaders;

    public CurseManifestMinecraft() {
        this.gameVersion = "";
        this.modLoaders = Collections.emptyList();
    }

    public CurseManifestMinecraft(String str, List<CurseManifestModLoader> list) {
        this.gameVersion = str;
        this.modLoaders = new ArrayList(list);
    }

    public String getGameVersion() {
        return this.gameVersion;
    }

    public List<CurseManifestModLoader> getModLoaders() {
        return Collections.unmodifiableList(this.modLoaders);
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (StringUtils.isBlank(this.gameVersion)) {
            throw new JsonParseException("CurseForge Manifest.gameVersion cannot be blank.");
        }
    }
}
