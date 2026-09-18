package com.qcl.launcher.utils.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.io.File;
import java.lang.reflect.Type;

/* loaded from: classes2.dex */
public final class FileTypeAdapter implements JsonSerializer<File>, JsonDeserializer<File> {
    public static final FileTypeAdapter INSTANCE = new FileTypeAdapter();

    private FileTypeAdapter() {
    }

    @Override // com.google.gson.JsonSerializer
    public JsonElement serialize(File file, Type type, JsonSerializationContext jsonSerializationContext) {
        if (file == null) {
            return JsonNull.INSTANCE;
        }
        return new JsonPrimitive(file.getPath());
    }

    /* JADX WARN: Can't rename method to resolve collision */
    @Override // com.google.gson.JsonDeserializer
    public File deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement == null) {
            return null;
        }
        return new File(jsonElement.getAsString());
    }
}
