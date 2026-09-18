package com.qcl.launcher.launcher.mod;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.qcl.launcher.launcher.mod.FabricModMetadata;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/* loaded from: classes2.dex */
public final class FabricModMetadata {
    private final List<FabricModAuthor> authors;
    private final Map<String, String> contact;
    private final String description;
    private final String icon;
    private final String id;
    private final String name;
    private final String version;

    public FabricModMetadata() {
        this("", "", "", "", "", Collections.emptyList(), Collections.emptyMap());
    }

    public FabricModMetadata(String str, String str2, String str3, String str4, String str5, List<FabricModAuthor> list, Map<String, String> map) {
        this.id = str;
        this.name = str2;
        this.version = str3;
        this.icon = str4;
        this.description = str5;
        this.authors = list;
        this.contact = map;
    }

    public static LocalModFile fromFile(ModManager modManager, Path path) throws IOException, JsonParseException {
        String readNormalMeta = ZipTools.readNormalMeta(path.toString(), "fabric.mod.json");
        if (StringUtils.isBlank(readNormalMeta)) {
            throw new IOException("File " + path + " is not a Fabric mod.");
        }
        FabricModMetadata fabricModMetadata = (FabricModMetadata) JsonUtils.fromNonNullJson(readNormalMeta, FabricModMetadata.class);
        List<FabricModAuthor> list = fabricModMetadata.authors;
        String str = list == null ? "" : (String) list.stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.FabricModMetadata$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String str2;
                str2 = ((FabricModMetadata.FabricModAuthor) obj).name;
                return str2;
            }
        }).collect(Collectors.joining(", "));
        LocalMod localMod = modManager.getLocalMod(fabricModMetadata.id, ModLoaderType.FABRIC);
        String str2 = fabricModMetadata.name;
        LocalModFile.Description description = new LocalModFile.Description(fabricModMetadata.description);
        String str3 = fabricModMetadata.version;
        Map<String, String> map = fabricModMetadata.contact;
        return new LocalModFile(modManager, localMod, path, str2, description, str, str3, "", map != null ? map.getOrDefault("homepage", "") : "", fabricModMetadata.icon);
    }

    @JsonAdapter(FabricModAuthorSerializer.class)
    /* loaded from: classes2.dex */
    public static final class FabricModAuthor {
        private final String name;

        public FabricModAuthor() {
            this("");
        }

        public FabricModAuthor(String str) {
            this.name = str;
        }
    }

    /* loaded from: classes2.dex */
    public static final class FabricModAuthorSerializer implements JsonSerializer<FabricModAuthor>, JsonDeserializer<FabricModAuthor> {
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // com.google.gson.JsonDeserializer
        public FabricModAuthor deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return jsonElement.isJsonPrimitive() ? new FabricModAuthor(jsonElement.getAsString()) : new FabricModAuthor(jsonElement.getAsJsonObject().getAsJsonPrimitive("name").getAsString());
        }

        @Override // com.google.gson.JsonSerializer
        public JsonElement serialize(FabricModAuthor fabricModAuthor, Type type, JsonSerializationContext jsonSerializationContext) {
            if (fabricModAuthor != null) {
                return new JsonPrimitive(fabricModAuthor.name);
            }
            return JsonNull.INSTANCE;
        }
    }
}
