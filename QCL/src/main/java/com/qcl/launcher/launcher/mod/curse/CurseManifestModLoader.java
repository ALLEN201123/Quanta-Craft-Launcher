package com.qcl.launcher.launcher.mod.curse;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.string.StringUtils;

/* loaded from: classes2.dex */
public final class CurseManifestModLoader implements Validation {

    @SerializedName("id")
    private final String id;

    @SerializedName("primary")
    private final boolean primary;

    public CurseManifestModLoader() {
        this("", false);
    }

    public CurseManifestModLoader(String str, boolean z) {
        this.id = str;
        this.primary = z;
    }

    public String getId() {
        return this.id;
    }

    public boolean isPrimary() {
        return this.primary;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (StringUtils.isBlank(this.id)) {
            throw new JsonParseException("Curse Forge modpack manifest Mod loader id cannot be blank.");
        }
    }
}
