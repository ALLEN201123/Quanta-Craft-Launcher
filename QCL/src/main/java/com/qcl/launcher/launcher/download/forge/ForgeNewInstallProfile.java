package com.qcl.launcher.launcher.download.forge;

import com.google.gson.JsonParseException;
import com.qcl.launcher.auth.yggdrasil.YggdrasilSession$$ExternalSyntheticLambda3;
import com.qcl.launcher.launcher.download.forge.ForgeNewInstallProfile;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.utils.gson.tools.TolerableValidationException;
import com.qcl.launcher.utils.gson.tools.Validation;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/* loaded from: classes2.dex */
public class ForgeNewInstallProfile implements Validation {
    private final Map<String, Datum> data;
    private final String json;
    private final List<Library> libraries;
    private final String minecraft;
    private final Artifact path;
    private final List<Processor> processors;
    private final int spec;
    private final String version;

    public ForgeNewInstallProfile(int i, String str, String str2, String str3, Artifact artifact, List<Library> list, List<Processor> list2, Map<String, Datum> map) {
        this.spec = i;
        this.minecraft = str;
        this.json = str2;
        this.version = str3;
        this.path = artifact;
        this.libraries = list;
        this.processors = list2;
        this.data = map;
    }

    public int getSpec() {
        return this.spec;
    }

    public String getMinecraft() {
        return this.minecraft;
    }

    public String getJson() {
        return this.json;
    }

    public String getVersion() {
        return this.version;
    }

    public Optional<Artifact> getPath() {
        return Optional.ofNullable(this.path);
    }

    public List<Library> getLibraries() {
        List<Library> list = this.libraries;
        return list == null ? Collections.emptyList() : list;
    }

    public List<Processor> getProcessors() {
        List<Processor> list = this.processors;
        return list == null ? Collections.emptyList() : (List) list.stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.download.forge.ForgeNewInstallProfile$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean isSide;
                isSide = ((ForgeNewInstallProfile.Processor) obj).isSide("client");
                return isSide;
            }
        }).collect(Collectors.toList());
    }

    public Map<String, String> getData() {
        Map<String, Datum> map = this.data;
        if (map == null) {
            return new HashMap();
        }
        return (Map) map.entrySet().stream().collect(Collectors.toMap(YggdrasilSession$$ExternalSyntheticLambda3.INSTANCE, new Function() { // from class: com.qcl.launcher.launcher.download.forge.ForgeNewInstallProfile$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String client;
                client = ((ForgeNewInstallProfile.Datum) ((Map.Entry) obj).getValue()).getClient();
                return client;
            }
        }));
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException, TolerableValidationException {
        if (this.minecraft == null || this.json == null || this.version == null) {
            throw new JsonParseException("ForgeNewInstallProfile is malformed");
        }
    }

    /* loaded from: classes2.dex */
    public static class Processor implements Validation {
        private final List<String> args;
        private final List<Artifact> classpath;
        private final Artifact jar;
        private final Map<String, String> outputs;
        private final List<String> sides;

        public Processor(List<String> list, Artifact artifact, List<Artifact> list2, List<String> list3, Map<String, String> map) {
            this.sides = list;
            this.jar = artifact;
            this.classpath = list2;
            this.args = list3;
            this.outputs = map;
        }

        public boolean isSide(String str) {
            List<String> list = this.sides;
            return list == null || list.contains(str);
        }

        public Artifact getJar() {
            return this.jar;
        }

        public List<Artifact> getClasspath() {
            List<Artifact> list = this.classpath;
            return list == null ? Collections.emptyList() : list;
        }

        public List<String> getArgs() {
            List<String> list = this.args;
            return list == null ? Collections.emptyList() : list;
        }

        public Map<String, String> getOutputs() {
            Map<String, String> map = this.outputs;
            return map == null ? Collections.emptyMap() : map;
        }

        @Override // com.qcl.launcher.utils.gson.tools.Validation
        public void validate() throws JsonParseException, TolerableValidationException {
            if (this.jar == null) {
                throw new JsonParseException("Processor::jar cannot be null");
            }
        }
    }

    /* loaded from: classes2.dex */
    public static class Datum {
        private final String client;

        public Datum(String str) {
            this.client = str;
        }

        public String getClient() {
            return this.client;
        }
    }
}
