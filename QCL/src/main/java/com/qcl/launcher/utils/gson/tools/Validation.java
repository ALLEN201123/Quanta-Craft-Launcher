package com.qcl.launcher.utils.gson.tools;

import com.google.gson.JsonParseException;

/* loaded from: classes2.dex */
public interface Validation {
    void validate() throws JsonParseException, TolerableValidationException;

    static void requireNonNull(Object obj, String str) throws JsonParseException {
        if (obj == null) {
            throw new JsonParseException(str);
        }
    }
}
