package com.qcl.launcher.launcher.mod.modrinth;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.auth.offline.YggdrasilServer$$ExternalSyntheticLambda1;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.launcher.mod.ModLoaderType;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;
import com.qcl.launcher.utils.DigestUtils;
import com.qcl.launcher.utils.Hex;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Pair;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.HttpRequest;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.io.ResponseCodeException;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public final class ModrinthRemoteModRepository implements RemoteModRepository {
    private static final String PREFIX = "https://api.modrinth.com";
    private final String projectType;
    public static final ModrinthRemoteModRepository MODS = new ModrinthRemoteModRepository("mod");
    public static final ModrinthRemoteModRepository MODPACKS = new ModrinthRemoteModRepository("modpack");
    public static final ModrinthRemoteModRepository RESOURCE_PACKS = new ModrinthRemoteModRepository("resourcepack");
    public static final ModrinthRemoteModRepository SHADERS = new ModrinthRemoteModRepository("shader");
    public static final Category CATEGORY_ALL = new Category();

    private ModrinthRemoteModRepository(String str) {
        this.projectType = str;
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteModRepository.Type getType() {
        return RemoteModRepository.Type.MOD;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$4, reason: invalid class name */
    /* loaded from: classes2.dex */
    public static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType;

        static {
            int[] iArr = new int[RemoteModRepository.SortType.values().length];
            $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType = iArr;
            try {
                iArr[RemoteModRepository.SortType.DATE_CREATED.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.CATEGORY.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.GAME_VERSION.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.POPULARITY.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.NAME.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.AUTHOR.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.LAST_UPDATED.ordinal()] = 7;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.TOTAL_DOWNLOADS.ordinal()] = 8;
            } catch (NoSuchFieldError unused8) {
            }
        }
    }

    private static String convertSortType(RemoteModRepository.SortType sortType) {
        switch (AnonymousClass4.$SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[sortType.ordinal()]) {
            case 1:
            case 2:
            case 3:
                return "newest";
            case 4:
            case 5:
            case 6:
                return "relevance";
            case 7:
                return "updated";
            case 8:
                return "downloads";
            default:
                throw new IllegalArgumentException("Unsupported sort type " + sortType);
        }
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod> search(String str, RemoteModRepository.Category category, int i, int i2, String str2, RemoteModRepository.SortType sortType, RemoteModRepository.SortOrder sortOrder) throws IOException {
        ArrayList arrayList = new ArrayList();
        arrayList.add(Collections.singletonList("project_type:" + this.projectType));
        if (StringUtils.isNotBlank(str)) {
            arrayList.add(Collections.singletonList("versions:" + str));
        }
        if (category != null && StringUtils.isNotBlank(category.getId()) && !category.getId().equals("all") && !category.getId().equals("0")) {
            arrayList.add(Collections.singletonList("categories:" + category.getId()));
        }
        return ((Response) HttpRequest.GET(NetworkUtils.withQuery("https://api.modrinth.com/v2/search", Lang.mapOf(Pair.pair("query", str2), Pair.pair("facets", JsonUtils.UGLY_GSON.toJson(arrayList)), Pair.pair("offset", Integer.toString(i)), Pair.pair("limit", Integer.toString(i2)), Pair.pair("index", convertSortType(sortType))))).getJson(new TypeToken<Response<ProjectSearchResult>>() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository.1
        }.getType())).getHits().stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ModrinthRemoteModRepository.ProjectSearchResult) obj).toMod();
            }
        });
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Optional<RemoteMod.Version> getRemoteVersionByLocalFile(LocalModFile localModFile, Path path) throws IOException {
        try {
            return ((ProjectVersion) HttpRequest.GET("https://api.modrinth.com/v2/version_file/" + Hex.encodeHex(DigestUtils.digest("SHA-1", path)), Pair.pair("algorithm", "sha1")).getJson(ProjectVersion.class)).toVersion();
        } catch (ResponseCodeException e) {
            if (e.getResponseCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteMod getModById(String str) throws IOException {
        return ((Project) HttpRequest.GET("https://api.modrinth.com/v2/project/" + StringUtils.removePrefix(str, "local-")).getJson(Project.class)).toMod();
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteMod.File getModFile(String str, String str2) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod.Version> getRemoteVersionsById(String str) throws IOException {
        return ((List) HttpRequest.GET("https://api.modrinth.com/v2/project/" + StringUtils.removePrefix(str, "local-") + "/version").getJson(new TypeToken<List<ProjectVersion>>() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository.2
        }.getType())).stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ModrinthRemoteModRepository.ProjectVersion) obj).toVersion();
            }
        }).flatMap(YggdrasilServer$$ExternalSyntheticLambda1.INSTANCE);
    }

    public List<Category> getCategoriesImpl() throws IOException {
        return (List) ((List) HttpRequest.GET("https://api.modrinth.com/v2/tag/category").getJson(new TypeToken<List<Category>>() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository.3
        }.getType())).stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ModrinthRemoteModRepository.this.m458x34f2c86((ModrinthRemoteModRepository.Category) obj);
            }
        }).collect(Collectors.toList());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getCategoriesImpl$0$com-qcl-launcher-launcher-mod-modrinth-ModrinthRemoteModRepository, reason: not valid java name */
    public /* synthetic */ boolean m458x34f2c86(Category category) {
        return category.getProjectType().equals(this.projectType);
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteModRepository.Category> getCategories() throws IOException {
        return getCategoriesImpl().stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((ModrinthRemoteModRepository.Category) obj).toCategory();
            }
        });
    }

    /* loaded from: classes2.dex */
    public static class Category {
        private final String icon;
        private final String name;

        @SerializedName("project_type")
        private final String projectType;

        public Category() {
            this("", "", "");
        }

        public Category(String str, String str2, String str3) {
            this.icon = str;
            this.name = str2;
            this.projectType = str3;
        }

        public String getIcon() {
            return this.icon;
        }

        public String getName() {
            return this.name;
        }

        public String getProjectType() {
            return this.projectType;
        }

        public RemoteModRepository.Category toCategory() {
            return new RemoteModRepository.Category(this, this.name, Collections.emptyList());
        }
    }

    /* loaded from: classes2.dex */
    public static class Project implements RemoteMod.IMod {
        private final String body;
        private final List<String> categories;
        private final String description;
        private final int downloads;

        @SerializedName("icon_url")
        private final String iconUrl;
        private final String id;

        @SerializedName("project_type")
        private final String projectType;
        private final Date published;
        private final String slug;
        private final String team;
        private final String title;
        private final Date updated;
        private final List<String> versions;

        public Project(String str, String str2, String str3, List<String> list, String str4, String str5, int i, String str6, String str7, String str8, Date date, Date date2, List<String> list2) {
            this.slug = str;
            this.title = str2;
            this.description = str3;
            this.categories = list;
            this.body = str4;
            this.projectType = str5;
            this.downloads = i;
            this.iconUrl = str6;
            this.id = str7;
            this.team = str8;
            this.published = date;
            this.updated = date2;
            this.versions = list2;
        }

        public String getSlug() {
            return this.slug;
        }

        public String getTitle() {
            return this.title;
        }

        public String getDescription() {
            return this.description;
        }

        public List<String> getCategories() {
            return this.categories;
        }

        public String getBody() {
            return this.body;
        }

        public String getProjectType() {
            return this.projectType;
        }

        public int getDownloads() {
            return this.downloads;
        }

        public String getIconUrl() {
            return this.iconUrl;
        }

        public String getId() {
            return this.id;
        }

        public String getTeam() {
            return this.team;
        }

        public Date getPublished() {
            return this.published;
        }

        public Date getUpdated() {
            return this.updated;
        }

        public List<String> getVersions() {
            return this.versions;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteMod.IMod
        public List<RemoteMod> loadDependencies(RemoteModRepository remoteModRepository) throws IOException {
            Set<String> set = (Set) remoteModRepository.getRemoteVersionsById(getId()).flatMap(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$Project$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    Stream stream;
                    stream = ((RemoteMod.Version) obj).getDependencies().stream();
                    return stream;
                }
            }).collect(Collectors.toSet());
            ArrayList arrayList = new ArrayList();
            for (String str : set) {
                if (StringUtils.isNotBlank(str)) {
                    arrayList.add(remoteModRepository.getModById(str));
                }
            }
            return arrayList;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteMod.IMod
        public Stream<RemoteMod.Version> loadVersions(RemoteModRepository remoteModRepository) throws IOException {
            return remoteModRepository.getRemoteVersionsById(getId());
        }

        public RemoteMod toMod() {
            return new RemoteMod(this.slug, "", this.title, this.description, this.categories, null, this.iconUrl, this);
        }
    }

    /* loaded from: classes2.dex */
    public static class Dependency {

        @SerializedName("dependency_type")
        private final String dependencyType;

        @SerializedName("project_id")
        private final String projectId;

        @SerializedName("version_id")
        private final String versionId;

        public Dependency(String str, String str2, String str3) {
            this.versionId = str;
            this.projectId = str2;
            this.dependencyType = str3;
        }

        public String getVersionId() {
            return this.versionId;
        }

        public String getProjectId() {
            return this.projectId;
        }

        public String getDependencyType() {
            return this.dependencyType;
        }
    }

    /* loaded from: classes2.dex */
    public static class ProjectVersion implements RemoteMod.IVersion {

        @SerializedName("author_id")
        private final String authorId;
        private final String changelog;

        @SerializedName("changelog_url")
        private final String changelogUrl;

        @SerializedName("date_published")
        private final Date datePublished;
        private final List<Dependency> dependencies;
        private final int downloads;
        private final boolean featured;
        private final List<ProjectVersionFile> files;

        @SerializedName("game_versions")
        private final List<String> gameVersions;
        private final String id;
        private final List<String> loaders;
        private final String name;

        @SerializedName("project_id")
        private final String projectId;

        @SerializedName("version_number")
        private final String versionNumber;

        @SerializedName("version_type")
        private final String versionType;

        public ProjectVersion(String str, String str2, String str3, List<Dependency> list, List<String> list2, String str4, List<String> list3, boolean z, String str5, String str6, String str7, Date date, int i, String str8, List<ProjectVersionFile> list4) {
            this.name = str;
            this.versionNumber = str2;
            this.changelog = str3;
            this.dependencies = list;
            this.gameVersions = list2;
            this.versionType = str4;
            this.loaders = list3;
            this.featured = z;
            this.id = str5;
            this.projectId = str6;
            this.authorId = str7;
            this.datePublished = date;
            this.downloads = i;
            this.changelogUrl = str8;
            this.files = list4;
        }

        public String getName() {
            return this.name;
        }

        public String getVersionNumber() {
            return this.versionNumber;
        }

        public String getChangelog() {
            return this.changelog;
        }

        public List<Dependency> getDependencies() {
            return this.dependencies;
        }

        public List<String> getGameVersions() {
            return this.gameVersions;
        }

        public String getVersionType() {
            return this.versionType;
        }

        public List<String> getLoaders() {
            return this.loaders;
        }

        public boolean isFeatured() {
            return this.featured;
        }

        public String getId() {
            return this.id;
        }

        public String getProjectId() {
            return this.projectId;
        }

        public String getAuthorId() {
            return this.authorId;
        }

        public Date getDatePublished() {
            return this.datePublished;
        }

        public int getDownloads() {
            return this.downloads;
        }

        public String getChangelogUrl() {
            return this.changelogUrl;
        }

        public List<ProjectVersionFile> getFiles() {
            return this.files;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteMod.IVersion
        public RemoteMod.Type getType() {
            return RemoteMod.Type.MODRINTH;
        }

        public Optional<RemoteMod.Version> toVersion() {
            RemoteMod.VersionType versionType;
            if ("release".equals(this.versionType)) {
                versionType = RemoteMod.VersionType.Release;
            } else if ("beta".equals(this.versionType)) {
                versionType = RemoteMod.VersionType.Beta;
            } else if ("alpha".equals(this.versionType)) {
                versionType = RemoteMod.VersionType.Alpha;
            } else {
                versionType = RemoteMod.VersionType.Release;
            }
            RemoteMod.VersionType versionType2 = versionType;
            if (this.files.size() == 0) {
                return Optional.empty();
            }
            return Optional.of(new RemoteMod.Version(this, this.projectId, this.name, this.versionNumber, this.changelog, this.datePublished, versionType2, this.files.get(0).toFile(), (List) this.dependencies.stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$ProjectVersion$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ((ModrinthRemoteModRepository.Dependency) obj).getProjectId();
                }
            }).filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$ProjectVersion$$ExternalSyntheticLambda2
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    boolean nonNull;
                    nonNull = Objects.nonNull((String) obj);
                    return nonNull;
                }
            }).collect(Collectors.toList()), this.gameVersions, (List) this.loaders.stream().flatMap(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$ProjectVersion$$ExternalSyntheticLambda1
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    return ModrinthRemoteModRepository.ProjectVersion.lambda$toVersion$0((String) obj);
                }
            }).collect(Collectors.toList())));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ Stream lambda$toVersion$0(String str) {
            return "fabric".equalsIgnoreCase(str) ? Stream.of(ModLoaderType.FABRIC) : "forge".equalsIgnoreCase(str) ? Stream.of(ModLoaderType.FORGE) : Stream.empty();
        }
    }

    /* loaded from: classes2.dex */
    public static class ProjectVersionFile {
        private final String filename;
        private final Map<String, String> hashes;
        private final boolean primary;
        private final int size;
        private final String url;

        public ProjectVersionFile(Map<String, String> map, String str, String str2, boolean z, int i) {
            this.hashes = map;
            this.url = str;
            this.filename = str2;
            this.primary = z;
            this.size = i;
        }

        public Map<String, String> getHashes() {
            return this.hashes;
        }

        public String getUrl() {
            return this.url;
        }

        public String getFilename() {
            return this.filename;
        }

        public boolean isPrimary() {
            return this.primary;
        }

        public int getSize() {
            return this.size;
        }

        public RemoteMod.File toFile() {
            return new RemoteMod.File(this.hashes, this.url, this.filename);
        }
    }

    /* loaded from: classes2.dex */
    public static class ProjectSearchResult implements RemoteMod.IMod {
        private final String author;
        private final List<String> categories;

        @SerializedName("date_created")
        private final Date dateCreated;

        @SerializedName("date_modified")
        private final Date dateModified;
        private final String description;
        private final int downloads;

        @SerializedName("icon_url")
        private final String iconUrl;

        @SerializedName("latest_version")
        private final String latestVersion;

        @SerializedName("project_id")
        private final String projectId;

        @SerializedName("project_type")
        private final String projectType;
        private final String slug;
        private final String title;
        private final List<String> versions;

        public ProjectSearchResult(String str, String str2, String str3, List<String> list, String str4, int i, String str5, String str6, String str7, List<String> list2, Date date, Date date2, String str8) {
            this.slug = str;
            this.title = str2;
            this.description = str3;
            this.categories = list;
            this.projectType = str4;
            this.downloads = i;
            this.iconUrl = str5;
            this.projectId = str6;
            this.author = str7;
            this.versions = list2;
            this.dateCreated = date;
            this.dateModified = date2;
            this.latestVersion = str8;
        }

        public String getSlug() {
            return this.slug;
        }

        public String getTitle() {
            return this.title;
        }

        public String getDescription() {
            return this.description;
        }

        public List<String> getCategories() {
            return this.categories;
        }

        public String getProjectType() {
            return this.projectType;
        }

        public int getDownloads() {
            return this.downloads;
        }

        public String getIconUrl() {
            return this.iconUrl;
        }

        public String getProjectId() {
            return this.projectId;
        }

        public String getAuthor() {
            return this.author;
        }

        public List<String> getVersions() {
            return this.versions;
        }

        public Date getDateCreated() {
            return this.dateCreated;
        }

        public Date getDateModified() {
            return this.dateModified;
        }

        public String getLatestVersion() {
            return this.latestVersion;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteMod.IMod
        public List<RemoteMod> loadDependencies(RemoteModRepository remoteModRepository) throws IOException {
            Set<String> set = (Set) remoteModRepository.getRemoteVersionsById(getProjectId()).flatMap(new Function() { // from class: com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository$ProjectSearchResult$$ExternalSyntheticLambda0
                @Override // java.util.function.Function
                public final Object apply(Object obj) {
                    Stream stream;
                    stream = ((RemoteMod.Version) obj).getDependencies().stream();
                    return stream;
                }
            }).collect(Collectors.toSet());
            ArrayList arrayList = new ArrayList();
            for (String str : set) {
                if (StringUtils.isNotBlank(str)) {
                    arrayList.add(remoteModRepository.getModById(str));
                }
            }
            return arrayList;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteMod.IMod
        public Stream<RemoteMod.Version> loadVersions(RemoteModRepository remoteModRepository) throws IOException {
            return remoteModRepository.getRemoteVersionsById(getProjectId());
        }

        public RemoteMod toMod() {
            return new RemoteMod(this.slug, this.author, this.title, this.description, this.categories, String.format("https://modrinth.com/%s/%s", this.projectType, this.projectId), this.iconUrl, this);
        }
    }

    /* loaded from: classes2.dex */
    public static class Response<T> {
        private final List<T> hits;
        private final int limit;
        private final int offset;

        @SerializedName("total_hits")
        private final int totalHits;

        public Response() {
            this(0, 0, Collections.emptyList());
        }

        public Response(int i, int i2, List<T> list) {
            this.offset = i;
            this.limit = i2;
            this.totalHits = list.size();
            this.hits = list;
        }

        public int getOffset() {
            return this.offset;
        }

        public int getLimit() {
            return this.limit;
        }

        public int getTotalHits() {
            return this.totalHits;
        }

        public List<T> getHits() {
            return this.hits;
        }
    }
}
