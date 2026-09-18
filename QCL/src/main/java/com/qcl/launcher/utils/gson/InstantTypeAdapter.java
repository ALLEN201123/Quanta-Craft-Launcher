package com.qcl.launcher.utils.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/* loaded from: classes2.dex */
public final class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
    public static final InstantTypeAdapter INSTANCE = new InstantTypeAdapter();

    private InstantTypeAdapter() {
    }

    @Override // com.google.gson.JsonDeserializer
    public Instant deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (!(jsonElement instanceof JsonPrimitive)) {
            throw new JsonParseException("The instant should be a string value");
        }
        Instant parse = Instant.parse(jsonElement.getAsString());
        if (type == Instant.class) {
            return parse;
        }
        throw new IllegalArgumentException(getClass() + " cannot be deserialized to " + type);
    }

    @Override // com.google.gson.JsonSerializer
    public JsonElement serialize(Instant instant, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault()).format(instant));
    }
}
