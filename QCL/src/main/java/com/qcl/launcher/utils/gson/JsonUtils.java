package com.qcl.launcher.utils.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/* loaded from: classes2.dex */
public final class JsonUtils {
    public static final Gson GSON = defaultGsonBuilder().create();
    public static final Gson UGLY_GSON = new GsonBuilder().registerTypeAdapterFactory(JsonTypeAdapterFactory.INSTANCE).registerTypeAdapterFactory(ValidationTypeAdapterFactory.INSTANCE).registerTypeAdapterFactory(LowerCaseEnumTypeAdapterFactory.INSTANCE).create();

    private JsonUtils() {
    }

    public static <T> T fromNonNullJson(String str, Class<T> cls) throws JsonParseException {
        T t = (T) GSON.fromJson(str, (Class) cls);
        if (t != null) {
            return t;
        }
        throw new JsonParseException("Json object cannot be null.");
    }

    public static <T> T fromNonNullJson(String str, Type type) throws JsonParseException {
        T t = (T) GSON.fromJson(str, type);
        if (t != null) {
            return t;
        }
        throw new JsonParseException("Json object cannot be null.");
    }

    public static <T> T fromMaybeMalformedJson(String str, Class<T> cls) throws JsonParseException {
        try {
            return (T) GSON.fromJson(str, (Class) cls);
        } catch (JsonSyntaxException unused) {
            return null;
        }
    }

    public static <T> T fromMaybeMalformedJson(String str, Type type) throws JsonParseException {
        try {
            return (T) GSON.fromJson(str, type);
        } catch (JsonSyntaxException unused) {
            return null;
        }
    }

    public static GsonBuilder defaultGsonBuilder() {
        return new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().disableHtmlEscaping().registerTypeAdapter(Instant.class, InstantTypeAdapter.INSTANCE).registerTypeAdapter(Date.class, DateTypeAdapter.INSTANCE).registerTypeAdapter(UUID.class, UUIDTypeAdapter.INSTANCE).registerTypeAdapter(File.class, FileTypeAdapter.INSTANCE).registerTypeAdapterFactory(ValidationTypeAdapterFactory.INSTANCE).registerTypeAdapterFactory(LowerCaseEnumTypeAdapterFactory.INSTANCE).registerTypeAdapterFactory(JsonTypeAdapterFactory.INSTANCE);
    }
}
