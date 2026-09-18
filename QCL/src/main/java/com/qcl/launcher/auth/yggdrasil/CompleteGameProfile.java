package com.qcl.launcher.auth.yggdrasil;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/* loaded from: classes2.dex */
public class CompleteGameProfile extends GameProfile {

    @JsonAdapter(PropertyMapSerializer.class)
    private final Map<String, String> properties;

    public CompleteGameProfile(UUID uuid, String str, Map<String, String> map) {
        super(uuid, str);
        this.properties = (Map) Objects.requireNonNull(map);
    }

    public CompleteGameProfile(GameProfile gameProfile, Map<String, String> map) {
        this(gameProfile.getId(), gameProfile.getName(), map);
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override // com.qcl.launcher.auth.yggdrasil.GameProfile, com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        super.validate();
        if (this.properties == null) {
            throw new JsonParseException("Game profile properties cannot be null");
        }
    }
}
