package com.qcl.launcher.utils.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.qcl.launcher.utils.gson.tools.TolerableValidationException;
import com.qcl.launcher.utils.gson.tools.Validation;
import java.io.IOException;

/* loaded from: classes2.dex */
public final class ValidationTypeAdapterFactory implements TypeAdapterFactory {
    public static final ValidationTypeAdapterFactory INSTANCE = new ValidationTypeAdapterFactory();

    @Override // com.google.gson.TypeAdapterFactory
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        final TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, typeToken);
        return new TypeAdapter<T>() { // from class: com.qcl.launcher.utils.gson.ValidationTypeAdapterFactory.1
            @Override // com.google.gson.TypeAdapter
            public void write(JsonWriter jsonWriter, T t) throws IOException {
                if (t instanceof Validation) {
                    try {
                        ((Validation) t).validate();
                    } catch (TolerableValidationException unused) {
                        delegateAdapter.write(jsonWriter, null);
                        return;
                    }
                }
                delegateAdapter.write(jsonWriter, t);
            }

            @Override // com.google.gson.TypeAdapter
            public T read(JsonReader jsonReader) throws IOException {
                T t = (T) delegateAdapter.read(jsonReader);
                if (!(t instanceof Validation)) {
                    return t;
                }
                try {
                    ((Validation) t).validate();
                    return t;
                } catch (TolerableValidationException unused) {
                    return null;
                }
            }
        };
    }
}
