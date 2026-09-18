package com.qcl.launcher.auth.yggdrasil;

import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.string.StringUtils;
import java.util.Map;

/* loaded from: classes2.dex */
public final class User implements Validation {
    private final String id;

    @JsonAdapter(PropertyMapSerializer.class)
    private final Map<String, String> properties;

    public User(String str) {
        this(str, null);
    }

    public User(String str, Map<String, String> map) {
        this.id = str;
        this.properties = map;
    }

    public String getId() {
        return this.id;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (StringUtils.isBlank(this.id)) {
            throw new JsonParseException("User id cannot be empty.");
        }
    }
}
