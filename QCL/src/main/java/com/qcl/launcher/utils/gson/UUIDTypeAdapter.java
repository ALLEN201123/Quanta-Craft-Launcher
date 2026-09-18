package com.qcl.launcher.utils.gson;

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.UUID;

/* loaded from: classes2.dex */
public final class UUIDTypeAdapter extends TypeAdapter<UUID> {
    public static final UUIDTypeAdapter INSTANCE = new UUIDTypeAdapter();

    @Override // com.google.gson.TypeAdapter
    public void write(JsonWriter jsonWriter, UUID uuid) throws IOException {
        jsonWriter.value(uuid == null ? null : fromUUID(uuid));
    }

    @Override // com.google.gson.TypeAdapter
    public UUID read(JsonReader jsonReader) throws IOException {
        try {
            return fromString(jsonReader.nextString());
        } catch (IllegalArgumentException unused) {
            throw new JsonParseException("UUID malformed");
        }
    }

    public static String fromUUID(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public static UUID fromString(String str) {
        return UUID.fromString(str.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }
}
