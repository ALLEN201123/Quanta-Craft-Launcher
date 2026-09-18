package com.qcl.launcher.auth.yggdrasil;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.qcl.launcher.utils.gson.UUIDTypeAdapter;
import com.qcl.launcher.utils.gson.tools.Validation;
import java.util.Objects;
import java.util.UUID;

/* loaded from: classes2.dex */
public class GameProfile implements Validation {

    @JsonAdapter(UUIDTypeAdapter.class)
    private final UUID id;
    private final String name;

    public GameProfile(UUID uuid, String str) {
        this.id = (UUID) Objects.requireNonNull(uuid);
        this.name = (String) Objects.requireNonNull(str);
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        Validation.requireNonNull(this.id, "Game profile id cannot be null");
        Validation.requireNonNull(this.name, "Game profile name cannot be null");
    }
}
