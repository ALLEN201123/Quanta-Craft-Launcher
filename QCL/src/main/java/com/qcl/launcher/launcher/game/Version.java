package com.qcl.launcher.launcher.game;

import com.google.gson.JsonParseException;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.gson.JsonMap;
import com.qcl.launcher.utils.gson.tools.Validation;
import com.qcl.launcher.utils.string.StringUtils;
import com.qcl.launcher.utils.string.ToStringBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

/* loaded from: classes2.dex */
public class Version implements Comparable<Version>, Validation {
    public static final String DEFAULT_INDEX_URL = "https://s3.amazonaws.com/Minecraft.Download/indexes/";
    public static final String DEFAULT_LIBRARY_URL = "https://libraries.minecraft.net/";
    public static final String DEFAULT_VERSION_DOWNLOAD_URL = "https://s3.amazonaws.com/Minecraft.Download/versions/";
    private final Arguments arguments;
    private final AssetIndexInfo assetIndex;
    private final String assets;
    private final List<CompatibilityRule> compatibilityRules;
    private final Integer complianceLevel;
    private final JsonMap<DownloadType, DownloadInfo> downloads;
    private final Boolean hidden;
    private final String id;
    private final String inheritsFrom;
    private final String jar;
    private final GameJavaVersion javaVersion;
    private final List<Library> libraries;
    private final JsonMap<DownloadType, LoggingInfo> logging;
    private final String mainClass;
    private final String minecraftArguments;
    private final Integer minimumLauncherVersion;
    private final List<Version> patches;
    private final Integer priority;
    private final Date releaseTime;
    private final transient boolean resolved;
    private final Boolean root;
    private final Date time;
    private final ReleaseType type;
    private final String version;

    public Version(String str) {
        this(false, str, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false, true, null);
    }

    public Version(String str, String str2, int i, Arguments arguments, String str3, List<Library> list) {
        this(false, str, str2, Integer.valueOf(i), null, arguments, str3, null, null, null, null, null, null, list, null, null, null, null, null, null, null, null, null, null);
    }

    public Version(boolean z, String str, String str2, Integer num, String str3, Arguments arguments, String str4, String str5, String str6, AssetIndexInfo assetIndexInfo, String str7, Integer num2, GameJavaVersion gameJavaVersion, List<Library> list, List<CompatibilityRule> list2, Map<DownloadType, DownloadInfo> map, Map<DownloadType, LoggingInfo> map2, ReleaseType releaseType, Date date, Date date2, Integer num3, Boolean bool, Boolean bool2, List<Version> list3) {
        this.resolved = z;
        this.id = str;
        this.version = str2;
        this.priority = num;
        this.minecraftArguments = str3;
        this.arguments = arguments;
        this.mainClass = str4;
        this.inheritsFrom = str5;
        this.jar = str6;
        this.assetIndex = assetIndexInfo;
        this.assets = str7;
        this.complianceLevel = num2;
        this.javaVersion = gameJavaVersion;
        this.libraries = Lang.copyList(list);
        this.compatibilityRules = Lang.copyList(list2);
        this.downloads = map == null ? null : new JsonMap<>(map);
        this.logging = map2 == null ? null : new JsonMap<>(map2);
        this.type = releaseType;
        this.time = date == null ? null : (Date) date.clone();
        this.releaseTime = date2 != null ? (Date) date2.clone() : null;
        this.minimumLauncherVersion = num3;
        this.hidden = bool;
        this.root = bool2;
        this.patches = Lang.copyList(list3);
    }

    public Optional<String> getMinecraftArguments() {
        return Optional.ofNullable(this.minecraftArguments);
    }

    public Optional<Arguments> getArguments() {
        return Optional.ofNullable(this.arguments);
    }

    public String getMainClass() {
        return this.mainClass;
    }

    public Date getTime() {
        return this.time;
    }

    public String getId() {
        return this.id;
    }

    public String getVersion() {
        return this.version;
    }

    public int getPriority() {
        Integer num = this.priority;
        if (num == null) {
            return Integer.MIN_VALUE;
        }
        return num.intValue();
    }

    public ReleaseType getType() {
        ReleaseType releaseType = this.type;
        return releaseType == null ? ReleaseType.UNKNOWN : releaseType;
    }

    public Date getReleaseTime() {
        return this.releaseTime;
    }

    public String getJar() {
        return this.jar;
    }

    public String getInheritsFrom() {
        return this.inheritsFrom;
    }

    public int getMinimumLauncherVersion() {
        Integer num = this.minimumLauncherVersion;
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    public Integer getComplianceLevel() {
        return this.complianceLevel;
    }

    public GameJavaVersion getJavaVersion() {
        return this.javaVersion;
    }

    public boolean isHidden() {
        Boolean bool = this.hidden;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    public boolean isRoot() {
        Boolean bool = this.root;
        if (bool == null) {
            return false;
        }
        return bool.booleanValue();
    }

    public boolean isResolved() {
        return this.resolved;
    }

    public boolean isResolvedPreservingPatches() {
        return this.inheritsFrom == null && !this.resolved;
    }

    public List<Version> getPatches() {
        List<Version> list = this.patches;
        return list == null ? Collections.emptyList() : list;
    }

    public Map<DownloadType, LoggingInfo> getLogging() {
        JsonMap<DownloadType, LoggingInfo> jsonMap = this.logging;
        return jsonMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(jsonMap);
    }

    public List<Library> getLibraries() {
        List<Library> list = this.libraries;
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public List<CompatibilityRule> getCompatibilityRules() {
        List<CompatibilityRule> list = this.compatibilityRules;
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public Map<DownloadType, DownloadInfo> getDownloads() {
        JsonMap<DownloadType, DownloadInfo> jsonMap = this.downloads;
        return jsonMap == null ? Collections.emptyMap() : Collections.unmodifiableMap(jsonMap);
    }

    public DownloadInfo getDownloadInfo() {
        JsonMap<DownloadType, DownloadInfo> jsonMap = this.downloads;
        DownloadInfo downloadInfo = jsonMap == null ? null : jsonMap.get(DownloadType.CLIENT);
        String str = this.jar;
        if (str == null) {
            str = this.id;
        }
        return downloadInfo == null ? new DownloadInfo(String.format("%s%s/%s.jar", "https://s3.amazonaws.com/Minecraft.Download/versions/", str, str)) : downloadInfo;
    }

    public AssetIndexInfo getAssetIndex() {
        String str = this.assets;
        if (str == null) {
            str = "legacy";
        }
        AssetIndexInfo assetIndexInfo = this.assetIndex;
        return assetIndexInfo == null ? new AssetIndexInfo(str, "https://s3.amazonaws.com/Minecraft.Download/indexes/" + str + ".json") : assetIndexInfo;
    }

    public boolean appliesToCurrentEnvironment() {
        return CompatibilityRule.appliesToCurrentEnvironment(this.compatibilityRules);
    }

    public Version resolve(VersionProvider versionProvider) throws VersionNotFoundException {
        return isResolved() ? this : resolve(versionProvider, new HashSet()).markAsResolved();
    }

    public Version merge(Version version, boolean z) {
        String str = this.id;
        String str2 = this.minecraftArguments;
        if (str2 == null) {
            str2 = version.minecraftArguments;
        }
        String str3 = str2;
        Arguments merge = Arguments.merge(version.arguments, this.arguments);
        String str4 = this.mainClass;
        if (str4 == null) {
            str4 = version.mainClass;
        }
        String str5 = str4;
        String str6 = this.jar;
        if (str6 == null) {
            str6 = version.jar;
        }
        String str7 = str6;
        AssetIndexInfo assetIndexInfo = this.assetIndex;
        if (assetIndexInfo == null) {
            assetIndexInfo = version.assetIndex;
        }
        AssetIndexInfo assetIndexInfo2 = assetIndexInfo;
        String str8 = this.assets;
        if (str8 == null) {
            str8 = version.assets;
        }
        String str9 = str8;
        Integer num = this.complianceLevel;
        GameJavaVersion gameJavaVersion = this.javaVersion;
        if (gameJavaVersion == null) {
            gameJavaVersion = version.javaVersion;
        }
        GameJavaVersion gameJavaVersion2 = gameJavaVersion;
        List merge2 = Lang.merge(this.libraries, version.libraries);
        List merge3 = Lang.merge(version.compatibilityRules, this.compatibilityRules);
        JsonMap<DownloadType, DownloadInfo> jsonMap = this.downloads;
        if (jsonMap == null) {
            jsonMap = version.downloads;
        }
        JsonMap<DownloadType, DownloadInfo> jsonMap2 = jsonMap;
        JsonMap<DownloadType, LoggingInfo> jsonMap3 = this.logging;
        if (jsonMap3 == null) {
            jsonMap3 = version.logging;
        }
        JsonMap<DownloadType, LoggingInfo> jsonMap4 = jsonMap3;
        ReleaseType releaseType = this.type;
        if (releaseType == null) {
            releaseType = version.type;
        }
        ReleaseType releaseType2 = releaseType;
        Date date = this.time;
        if (date == null) {
            date = version.time;
        }
        Date date2 = date;
        Date date3 = this.releaseTime;
        if (date3 == null) {
            date3 = version.releaseTime;
        }
        Date date4 = date3;
        Integer num2 = (Integer) Lang.merge(this.minimumLauncherVersion, version.minimumLauncherVersion, new BinaryOperator() { // from class: com.qcl.launcher.launcher.game.Version$$ExternalSyntheticLambda0
            @Override // java.util.function.BiFunction
            public final Object apply(Object obj, Object obj2) {
                int max;
                max = Math.max(((Integer) obj).intValue(), ((Integer) obj2).intValue());
                return Integer.valueOf(max);
            }
        });
        Boolean bool = this.hidden;
        List<Version> list = version.patches;
        if (!z) {
            list = Lang.merge(Lang.merge(list, Collections.singleton(toPatch())), this.patches);
        }
        return new Version(true, str, null, null, str3, merge, str5, null, str7, assetIndexInfo2, str9, num, gameJavaVersion2, merge2, merge3, jsonMap2, jsonMap4, releaseType2, date2, date4, num2, bool, true, list);
    }

    protected Version resolve(VersionProvider versionProvider, Set<String> set) throws VersionNotFoundException {
        Version merge;
        if (this.inheritsFrom == null) {
            Version patches = isRoot() ? new Version(this.id).setPatches(this.patches) : this;
            String str = this.jar;
            if (str == null) {
                str = this.id;
            }
            merge = patches.setJar(str);
        } else if (!set.add(this.id)) {
            Logging.LOG.log(Level.WARNING, "Found circular dependency versions: " + set);
            merge = this.jar == null ? setJar(this.id) : this;
        } else {
            merge = merge(versionProvider.getVersion(this.inheritsFrom).resolve(versionProvider, set), false);
        }
        List<Version> list = this.patches;
        if (list != null && !list.isEmpty()) {
            Iterator it = ((List) this.patches.stream().sorted(Comparator.comparing(new Function() { // from class: com.qcl.launcher.launcher.game.Version$$ExternalSyntheticLambda2
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return Integer.valueOf(((Version) obj).getPriority());
                }
            })).collect(Collectors.toList())).iterator();
            while (it.hasNext()) {
                merge = ((Version) it.next()).setJar(null).merge(merge, true);
            }
        }
        return merge.setId(this.id);
    }

    private Version toPatch() {
        return clearPatches().setHidden(true).setId("resolved." + getId());
    }

    public Version resolvePreservingPatches(VersionProvider versionProvider) throws VersionNotFoundException {
        return resolvePreservingPatches(versionProvider, new HashSet());
    }

    protected Version mergePreservingPatches(Version version) {
        return version.addPatch(toPatch()).addPatches(this.patches);
    }

    protected Version resolvePreservingPatches(VersionProvider versionProvider, Set<String> set) throws VersionNotFoundException {
        Version addPatches = isRoot() ? this : new Version(this.id).addPatch(toPatch()).addPatches(getPatches());
        if (this.inheritsFrom != null) {
            if (!set.add(this.id)) {
                Logging.LOG.log(Level.WARNING, "Found circular dependency versions: " + set);
            } else {
                addPatches = mergePreservingPatches(versionProvider.getVersion(this.inheritsFrom).resolvePreservingPatches(versionProvider, set));
            }
        }
        return addPatches.setId(this.id).setJar(resolve(versionProvider).getJar());
    }

    private Version markAsResolved() {
        return new Version(true, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version markAsUnresolved() {
        return new Version(false, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    private Version setHidden(Boolean bool) {
        return new Version(true, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, bool, this.root, this.patches);
    }

    public Version setId(String str) {
        return new Version(this.resolved, str, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setVersion(String str) {
        return new Version(this.resolved, this.id, str, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setPriority(Integer num) {
        return new Version(this.resolved, this.id, this.version, num, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setMinecraftArguments(String str) {
        return new Version(this.resolved, this.id, this.version, this.priority, str, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setArguments(Arguments arguments) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setMainClass(String str) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, str, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setInheritsFrom(String str) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, str, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setJar(String str) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, str, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setLibraries(List<Library> list) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, list, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setLogging(Map<DownloadType, LoggingInfo> map) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, map, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, this.patches);
    }

    public Version setPatches(List<Version> list) {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, list);
    }

    public Version addPatch(Version... versionArr) {
        return addPatches(Arrays.asList(versionArr));
    }

    public Version addPatches(List<Version> list) {
        final Set emptySet = list == null ? Collections.emptySet() : (Set) list.stream().map(new Function() { // from class: com.qcl.launcher.launcher.game.Version$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((Version) obj).getId();
            }
        }).collect(Collectors.toSet());
        List<Version> list2 = this.patches;
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, Lang.merge(list2 == null ? null : (Collection) list2.stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.game.Version$$ExternalSyntheticLambda5
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return Version.lambda$addPatches$0(emptySet, (Version) obj);
            }
        }).collect(Collectors.toList()), list));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$addPatches$0(Set set, Version version) {
        return !set.contains(version.getId());
    }

    public Version clearPatches() {
        return new Version(this.resolved, this.id, this.version, this.priority, this.minecraftArguments, this.arguments, this.mainClass, this.inheritsFrom, this.jar, this.assetIndex, this.assets, this.complianceLevel, this.javaVersion, this.libraries, this.compatibilityRules, this.downloads, this.logging, this.type, this.time, this.releaseTime, this.minimumLauncherVersion, this.hidden, this.root, null);
    }

    public Version removePatchById(final String str) {
        Boolean bool;
        List list;
        boolean z = this.resolved;
        String str2 = this.id;
        String str3 = this.version;
        Integer num = this.priority;
        String str4 = this.minecraftArguments;
        Arguments arguments = this.arguments;
        String str5 = this.mainClass;
        String str6 = this.inheritsFrom;
        String str7 = this.jar;
        AssetIndexInfo assetIndexInfo = this.assetIndex;
        String str8 = this.assets;
        Integer num2 = this.complianceLevel;
        GameJavaVersion gameJavaVersion = this.javaVersion;
        List<Library> list2 = this.libraries;
        List<CompatibilityRule> list3 = this.compatibilityRules;
        JsonMap<DownloadType, DownloadInfo> jsonMap = this.downloads;
        JsonMap<DownloadType, LoggingInfo> jsonMap2 = this.logging;
        ReleaseType releaseType = this.type;
        Date date = this.time;
        Date date2 = this.releaseTime;
        Integer num3 = this.minimumLauncherVersion;
        Boolean bool2 = this.hidden;
        Boolean bool3 = this.root;
        List<Version> list4 = this.patches;
        if (list4 == null) {
            list = null;
            bool = bool3;
        } else {
            bool = bool3;
            list = (List) list4.stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.game.Version$$ExternalSyntheticLambda4
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return Version.lambda$removePatchById$1(str, (Version) obj);
                }
            }).collect(Collectors.toList());
        }
        return new Version(z, str2, str3, num, str4, arguments, str5, str6, str7, assetIndexInfo, str8, num2, gameJavaVersion, list2, list3, jsonMap, jsonMap2, releaseType, date, date2, num3, bool2, bool, list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$removePatchById$1(String str, Version version) {
        return !str.equals(version.getId());
    }

    public boolean hasPatch(final String str) {
        List<Version> list = this.patches;
        return list != null && list.stream().anyMatch(new Predicate() { // from class: com.qcl.launcher.launcher.game.Version$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean equals;
                equals = str.equals(((Version) obj).getId());
                return equals;
            }
        });
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public boolean equals(Object obj) {
        return (obj instanceof Version) && Objects.equals(this.id, ((Version) obj).id);
    }

    @Override // java.lang.Comparable
    public int compareTo(Version version) {
        return this.id.compareTo(version.id);
    }

    public String toString() {
        return new ToStringBuilder(this).append("id", this.id).toString();
    }

    @Override // com.qcl.launcher.utils.gson.tools.Validation
    public void validate() throws JsonParseException {
        if (StringUtils.isBlank(this.id)) {
            throw new JsonParseException("Version ID cannot be blank");
        }
        JsonMap<DownloadType, DownloadInfo> jsonMap = this.downloads;
        if (jsonMap != null) {
            for (Map.Entry<DownloadType, DownloadInfo> entry : jsonMap.entrySet()) {
                if (!(entry.getKey() instanceof DownloadType)) {
                    throw new JsonParseException("Version downloads key must be DownloadType");
                }
                if (!(entry.getValue() instanceof DownloadInfo)) {
                    throw new JsonParseException("Version downloads value must be DownloadInfo");
                }
            }
        }
        JsonMap<DownloadType, LoggingInfo> jsonMap2 = this.logging;
        if (jsonMap2 != null) {
            for (Map.Entry<DownloadType, LoggingInfo> entry2 : jsonMap2.entrySet()) {
                if (!(entry2.getKey() instanceof DownloadType)) {
                    throw new JsonParseException("Version logging key must be DownloadType");
                }
                if (!(entry2.getValue() instanceof LoggingInfo)) {
                    throw new JsonParseException("Version logging value must be LoggingInfo");
                }
            }
        }
    }
}
