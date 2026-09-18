package com.qcl.launcher.auth.yggdrasil;

import java.util.Map;
import java.util.UUID;

/* loaded from: classes2.dex */
public enum TextureModel {
    STEVE("default"),
    ALEX("slim");

    public final String modelName;

    TextureModel(String str) {
        this.modelName = str;
    }

    public static TextureModel detectModelName(Map<String, String> map) {
        if (map != null && "slim".equals(map.get("model"))) {
            return ALEX;
        }
        return STEVE;
    }

    public static TextureModel detectUUID(UUID uuid) {
        return (uuid.hashCode() & 1) == 1 ? ALEX : STEVE;
    }
}
