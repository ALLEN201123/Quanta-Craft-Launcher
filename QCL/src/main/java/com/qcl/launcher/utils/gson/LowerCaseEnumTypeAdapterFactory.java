package com.qcl.launcher.utils.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

/* loaded from: classes2.dex */
public final class LowerCaseEnumTypeAdapterFactory implements TypeAdapterFactory {
    public static final LowerCaseEnumTypeAdapterFactory INSTANCE = new LowerCaseEnumTypeAdapterFactory();

    @Override // com.google.gson.TypeAdapterFactory
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        if (!rawType.isEnum()) {
            return null;
        }
        final HashMap hashMap = new HashMap();
        for (Object obj : rawType.getEnumConstants()) {
            hashMap.put(toLowercase(obj), obj);
        }
        return new TypeAdapter<T>() { // from class: com.qcl.launcher.utils.gson.LowerCaseEnumTypeAdapterFactory.1
            @Override // com.google.gson.TypeAdapter
            public void write(JsonWriter jsonWriter, T t) throws IOException {
                if (t != null) {
                    jsonWriter.value(LowerCaseEnumTypeAdapterFactory.toLowercase(t));
                } else {
                    jsonWriter.nullValue();
                }
            }

            @Override // com.google.gson.TypeAdapter
            public T read(JsonReader jsonReader) throws IOException {
                if (jsonReader.peek() == JsonToken.NULL) {
                    jsonReader.nextNull();
                    return null;
                }
                return (T) hashMap.get(jsonReader.nextString().toLowerCase());
            }
        };
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String toLowercase(Object obj) {
        return obj.toString().toLowerCase(Locale.US);
    }
}
