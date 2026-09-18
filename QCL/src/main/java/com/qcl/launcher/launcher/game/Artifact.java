package com.qcl.launcher.launcher.game;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.nio.file.Path;

/* loaded from: classes2.dex */
public class Artifact {
    private final String classifier;
    private final String descriptor;
    private final String extension;
    private final String fileName;
    private final String group;
    private final String name;
    private final String path;
    private final String version;

    public Artifact(String str, String str2, String str3) {
        this(str, str2, str3, null);
    }

    public Artifact(String str, String str2, String str3, String str4) {
        this(str, str2, str3, str4, null);
    }

    public Artifact(String str, String str2, String str3, String str4, String str5) {
        this.group = str;
        this.name = str2;
        this.version = str3;
        this.classifier = str4;
        str5 = str5 == null ? "jar" : str5;
        this.extension = str5;
        String str6 = str2 + "-" + str3;
        String str7 = (str4 != null ? str6 + "-" + str4 : str6) + "." + str5;
        this.fileName = str7;
        this.path = String.format("%s/%s/%s/%s", str.replace(".", "/"), str2, str3, str7);
        String format = String.format("%s:%s:%s", str, str2, str3);
        format = str4 != null ? format + ":" + str4 : format;
        this.descriptor = "jar".equals(str5) ? format : format + "@" + str5;
    }

    public static Artifact fromDescriptor(String str) {
        String str2;
        String[] split = str.split(":", 4);
        if (split.length != 3 && split.length != 4) {
            throw new IllegalArgumentException("Artifact name is malformed");
        }
        int length = split.length - 1;
        String[] split2 = split[length].split("@");
        if (split2.length == 2) {
            split[length] = split2[0];
            str2 = split2[1];
        } else {
            if (split2.length > 2) {
                throw new IllegalArgumentException("Artifact name is malformed");
            }
            str2 = null;
        }
        return new Artifact(split[0].replace("\\", "/"), split[1], split[2], split.length >= 4 ? split[3] : null, str2);
    }

    public String getGroup() {
        return this.group;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getClassifier() {
        return this.classifier;
    }

    public Artifact setClassifier(String str) {
        return new Artifact(this.group, this.name, this.version, str, this.extension);
    }

    public String getExtension() {
        return this.extension;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getPath() {
        return this.path;
    }

    public Path getPath(Path path) {
        return path.resolve(this.path);
    }

    public String toString() {
        return this.descriptor;
    }

    /* loaded from: classes2.dex */
    public static class Serializer implements JsonDeserializer<Artifact>, JsonSerializer<Artifact> {
        @Override // com.google.gson.JsonSerializer
        public JsonElement serialize(Artifact artifact, Type type, JsonSerializationContext jsonSerializationContext) {
            return artifact == null ? JsonNull.INSTANCE : new JsonPrimitive(artifact.toString());
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.google.gson.JsonDeserializer
        public Artifact deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if (jsonElement.isJsonPrimitive()) {
                return Artifact.fromDescriptor(jsonElement.getAsJsonPrimitive().getAsString());
            }
            return null;
        }
    }
}
