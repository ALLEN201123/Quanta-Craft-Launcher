package com.qcl.launcher.launcher.mod.curse;

import com.qcl.launcher.launcher.mod.ModLoaderType;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseAddon;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public class CurseAddon implements RemoteMod.IMod {
    private final boolean allowModDistribution;
    private final List<Author> authors;
    private final List<Category> categories;
    private final int classId;
    private final Date dateCreated;
    private final Date dateModified;
    private final Date dateReleased;
    private final int downloadCount;
    private final int gameId;
    private final int gamePopularityRank;
    private final int id;
    private final boolean isAvailable;
    private final boolean isFeatured;
    private final List<LatestFileIndex> latestFileIndices;
    private final List<LatestFile> latestFiles;
    private final Links links;
    private final Logo logo;
    private final int mainFileId;
    private final String name;
    private final int primaryCategoryId;
    private final String slug;
    private final int status;
    private final String summary;
    private final int thumbsUpCount;

    public CurseAddon(int i, int i2, String str, String str2, Links links, String str3, int i3, int i4, boolean z, int i5, List<Category> list, int i6, List<Author> list2, Logo logo, int i7, List<LatestFile> list3, List<LatestFileIndex> list4, Date date, Date date2, Date date3, boolean z2, int i8, boolean z3, int i9) {
        this.id = i;
        this.gameId = i2;
        this.name = str;
        this.slug = str2;
        this.links = links;
        this.summary = str3;
        this.status = i3;
        this.downloadCount = i4;
        this.isFeatured = z;
        this.primaryCategoryId = i5;
        this.categories = list;
        this.classId = i6;
        this.authors = list2;
        this.logo = logo;
        this.mainFileId = i7;
        this.latestFiles = list3;
        this.latestFileIndices = list4;
        this.dateCreated = date;
        this.dateModified = date2;
        this.dateReleased = date3;
        this.allowModDistribution = z2;
        this.gamePopularityRank = i8;
        this.isAvailable = z3;
        this.thumbsUpCount = i9;
    }

    public int getId() {
        return this.id;
    }

    public int getGameId() {
        return this.gameId;
    }

    public String getName() {
        return this.name;
    }

    public String getSlug() {
        return this.slug;
    }

    public Links getLinks() {
        return this.links;
    }

    public String getSummary() {
        return this.summary;
    }

    public int getStatus() {
        return this.status;
    }

    public int getDownloadCount() {
        return this.downloadCount;
    }

    public boolean isFeatured() {
        return this.isFeatured;
    }

    public int getPrimaryCategoryId() {
        return this.primaryCategoryId;
    }

    public List<Category> getCategories() {
        return this.categories;
    }

    public int getClassId() {
        return this.classId;
    }

    public List<Author> getAuthors() {
        return this.authors;
    }

    public Logo getLogo() {
        return this.logo;
    }

    public int getMainFileId() {
        return this.mainFileId;
    }

    public List<LatestFile> getLatestFiles() {
        return this.latestFiles;
    }

    public List<LatestFileIndex> getLatestFileIndices() {
        return this.latestFileIndices;
    }

    public Date getDateCreated() {
        return this.dateCreated;
    }

    public Date getDateModified() {
        return this.dateModified;
    }

    public Date getDateReleased() {
        return this.dateReleased;
    }

    public boolean isAllowModDistribution() {
        return this.allowModDistribution;
    }

    public int getGamePopularityRank() {
        return this.gamePopularityRank;
    }

    public boolean isAvailable() {
        return this.isAvailable;
    }

    public int getThumbsUpCount() {
        return this.thumbsUpCount;
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteMod.IMod
    public List<RemoteMod> loadDependencies(RemoteModRepository remoteModRepository) throws IOException {
        Set set = (Set) this.latestFiles.stream().flatMap(new Function() { // from class: com.qcl.launcher.launcher.mod.curse.CurseAddon$$ExternalSyntheticLambda2
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                Stream stream;
                stream = ((CurseAddon.LatestFile) obj).getDependencies().stream();
                return stream;
            }
        }).filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.curse.CurseAddon$$ExternalSyntheticLambda4
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return CurseAddon.lambda$loadDependencies$1((CurseAddon.Dependency) obj);
            }
        }).map(new Function() { // from class: com.qcl.launcher.launcher.mod.curse.CurseAddon$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return Integer.valueOf(((CurseAddon.Dependency) obj).getModId());
            }
        }).collect(Collectors.toSet());
        ArrayList arrayList = new ArrayList();
        Iterator it = set.iterator();
        while (it.hasNext()) {
            // ★ 1.2.5：单个依赖查不到（下架/受限）就跳过，不要让整个版本列表打不开
            try {
                arrayList.add(remoteModRepository.getModById(Integer.toString(((Integer) it.next()).intValue())));
            }
            catch (IOException ignored) {
            }
        }
        return arrayList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$loadDependencies$1(Dependency dependency) {
        return dependency.getRelationType() == 3;
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteMod.IMod
    public Stream<RemoteMod.Version> loadVersions(RemoteModRepository remoteModRepository) throws IOException {
        return remoteModRepository.getRemoteVersionsById(Integer.toString(this.id));
    }

    public RemoteMod toMod() {
        return new RemoteMod(this.slug, "", this.name, this.summary, (List) this.categories.stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.curse.CurseAddon$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                String num;
                num = Integer.toString(((CurseAddon.Category) obj).getId());
                return num;
            }
        }).collect(Collectors.toList()), this.links.websiteUrl, (String) Optional.ofNullable(this.logo).map(new Function() { // from class: com.qcl.launcher.launcher.mod.curse.CurseAddon$$ExternalSyntheticLambda3
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((CurseAddon.Logo) obj).getThumbnailUrl();
            }
        }).orElse(""), this);
    }

    /* loaded from: classes2.dex */
    public static class Links {
        private final String issuesUrl;
        private final String sourceUrl;
        private final String websiteUrl;
        private final String wikiUrl;

        public Links(String str, String str2, String str3, String str4) {
            this.websiteUrl = str;
            this.wikiUrl = str2;
            this.issuesUrl = str3;
            this.sourceUrl = str4;
        }

        public String getWebsiteUrl() {
            return this.websiteUrl;
        }

        public String getWikiUrl() {
            return this.wikiUrl;
        }

        public String getIssuesUrl() {
            return this.issuesUrl;
        }

        public String getSourceUrl() {
            return this.sourceUrl;
        }
    }

    /* loaded from: classes2.dex */
    public static class Author {
        private final int id;
        private final String name;
        private final String url;

        public Author(int i, String str, String str2) {
            this.id = i;
            this.name = str;
            this.url = str2;
        }

        public int getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String getUrl() {
            return this.url;
        }
    }

    /* loaded from: classes2.dex */
    public static class Logo {
        private final String description;
        private final int id;
        private final int modId;
        private final String thumbnailUrl;
        private final String title;
        private final String url;

        public Logo(int i, int i2, String str, String str2, String str3, String str4) {
            this.id = i;
            this.modId = i2;
            this.title = str;
            this.description = str2;
            this.thumbnailUrl = str3;
            this.url = str4;
        }

        public int getId() {
            return this.id;
        }

        public int getModId() {
            return this.modId;
        }

        public String getTitle() {
            return this.title;
        }

        public String getDescription() {
            return this.description;
        }

        public String getThumbnailUrl() {
            return this.thumbnailUrl;
        }

        public String getUrl() {
            return this.url;
        }
    }

    /* loaded from: classes2.dex */
    public static class Attachment {
        private final String description;
        private final int id;
        private final boolean isDefault;
        private final int projectId;
        private final int status;
        private final String thumbnailUrl;
        private final String title;
        private final String url;

        public Attachment(int i, int i2, String str, boolean z, String str2, String str3, String str4, int i3) {
            this.id = i;
            this.projectId = i2;
            this.description = str;
            this.isDefault = z;
            this.thumbnailUrl = str2;
            this.title = str3;
            this.url = str4;
            this.status = i3;
        }

        public int getId() {
            return this.id;
        }

        public int getProjectId() {
            return this.projectId;
        }

        public String getDescription() {
            return this.description;
        }

        public boolean isDefault() {
            return this.isDefault;
        }

        public String getThumbnailUrl() {
            return this.thumbnailUrl;
        }

        public String getTitle() {
            return this.title;
        }

        public String getUrl() {
            return this.url;
        }

        public int getStatus() {
            return this.status;
        }
    }

    /* loaded from: classes2.dex */
    public static class Dependency {
        private final int modId;
        private final int relationType;

        public Dependency() {
            this(0, 1);
        }

        public Dependency(int i, int i2) {
            this.modId = i;
            this.relationType = i2;
        }

        public int getModId() {
            return this.modId;
        }

        public int getRelationType() {
            return this.relationType;
        }
    }

    /* loaded from: classes2.dex */
    public static class LatestFileHash {
        private final int algo;
        private final String value;

        public LatestFileHash(String str, int i) {
            this.value = str;
            this.algo = i;
        }

        public String getValue() {
            return this.value;
        }

        public int getAlgo() {
            return this.algo;
        }
    }

    /* loaded from: classes2.dex */
    public static class LatestFile implements RemoteMod.IVersion {
        private final int alternateFileId;
        private final List<Dependency> dependencies;
        private final String displayName;
        private final int downloadCount;
        private final String downloadUrl;
        private final Date fileDate;
        private final long fileFingerprint;
        private final int fileLength;
        private final String fileName;
        private final int fileStatus;
        private final int gameId;
        private final List<String> gameVersions;
        private final List<LatestFileHash> hashes;
        private final int id;
        private final boolean isAvailable;
        private final boolean isServerPack;
        private final int modId;
        private final int releaseType;

        public LatestFile(int i, int i2, int i3, boolean z, String str, String str2, int i4, int i5, List<LatestFileHash> list, Date date, int i6, int i7, String str3, List<String> list2, List<Dependency> list3, int i8, boolean z2, long j) {
            this.id = i;
            this.gameId = i2;
            this.modId = i3;
            this.isAvailable = z;
            this.displayName = str;
            this.fileName = str2;
            this.releaseType = i4;
            this.fileStatus = i5;
            this.hashes = list;
            this.fileDate = date;
            this.fileLength = i6;
            this.downloadCount = i7;
            this.downloadUrl = str3;
            this.gameVersions = list2;
            this.dependencies = list3;
            this.alternateFileId = i8;
            this.isServerPack = z2;
            this.fileFingerprint = j;
        }

        public int getId() {
            return this.id;
        }

        public int getGameId() {
            return this.gameId;
        }

        public int getModId() {
            return this.modId;
        }

        public boolean isAvailable() {
            return this.isAvailable;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getFileName() {
            return this.fileName;
        }

        public int getReleaseType() {
            return this.releaseType;
        }

        public int getFileStatus() {
            return this.fileStatus;
        }

        public List<LatestFileHash> getHashes() {
            return this.hashes;
        }

        public Date getFileDate() {
            return this.fileDate;
        }

        public int getFileLength() {
            return this.fileLength;
        }

        public int getDownloadCount() {
            return this.downloadCount;
        }

        public String getDownloadUrl() {
            String str = this.downloadUrl;
            return str == null ? String.format("https://edge.forgecdn.net/files/%d/%d/%s", Integer.valueOf(this.id / 1000), Integer.valueOf(this.id % 1000), this.fileName) : str;
        }

        public List<String> getGameVersions() {
            return this.gameVersions;
        }

        public List<Dependency> getDependencies() {
            return this.dependencies;
        }

        public int getAlternateFileId() {
            return this.alternateFileId;
        }

        public boolean isServerPack() {
            return this.isServerPack;
        }

        public long getFileFingerprint() {
            return this.fileFingerprint;
        }

        @Override // com.qcl.launcher.launcher.mod.RemoteMod.IVersion
        public RemoteMod.Type getType() {
            return RemoteMod.Type.CURSEFORGE;
        }

        public RemoteMod.Version toVersion() {
            RemoteMod.VersionType versionType;
            ModLoaderType modLoaderType;
            int releaseType = getReleaseType();
            if (releaseType == 1) {
                versionType = RemoteMod.VersionType.Release;
            } else if (releaseType == 2) {
                versionType = RemoteMod.VersionType.Beta;
            } else if (releaseType == 3) {
                versionType = RemoteMod.VersionType.Alpha;
            } else {
                versionType = RemoteMod.VersionType.Release;
            }
            RemoteMod.VersionType versionType2 = versionType;
            if (this.gameVersions.contains("Forge")) {
                modLoaderType = ModLoaderType.FORGE;
            } else if (this.gameVersions.contains("Fabric")) {
                modLoaderType = ModLoaderType.FABRIC;
            } else {
                modLoaderType = ModLoaderType.UNKNOWN;
            }
            return new RemoteMod.Version(this, Integer.toString(this.modId), getDisplayName(), getFileName(), null, getFileDate(), versionType2, new RemoteMod.File(Collections.emptyMap(), getDownloadUrl(), getFileName()), Collections.emptyList(), (List) this.gameVersions.stream().filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.curse.CurseAddon$LatestFile$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return CurseAddon.LatestFile.lambda$toVersion$0((String) obj);
                }
            }).collect(Collectors.toList()), Collections.singletonList(modLoaderType));
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public static /* synthetic */ boolean lambda$toVersion$0(String str) {
            return str.startsWith("1.") || str.contains("w");
        }
    }

    /* loaded from: classes2.dex */
    public static class LatestFileIndex {
        private final int fileId;
        private final String filename;
        private final String gameVersion;
        private final int gameVersionTypeId;
        private final int modLoader;
        private final int releaseType;

        public LatestFileIndex(String str, int i, String str2, int i2, int i3, int i4) {
            this.gameVersion = str;
            this.fileId = i;
            this.filename = str2;
            this.releaseType = i2;
            this.gameVersionTypeId = i3;
            this.modLoader = i4;
        }

        public String getGameVersion() {
            return this.gameVersion;
        }

        public int getFileId() {
            return this.fileId;
        }

        public String getFilename() {
            return this.filename;
        }

        public int getReleaseType() {
            return this.releaseType;
        }

        public int getGameVersionTypeId() {
            return this.gameVersionTypeId;
        }

        public int getModLoader() {
            return this.modLoader;
        }
    }

    /* loaded from: classes2.dex */
    public static class Category {
        private final int classId;
        private final Date dateModified;
        private final int gameId;
        private final String iconUrl;
        private final int id;
        private final boolean isClass;
        private final String name;
        private final int parentCategoryId;
        private final String slug;
        private final transient List<Category> subcategories;
        private final String url;

        public Category() {
            this(0, 0, "", "", "", "", new Date(), false, 0, 0);
        }

        public Category(int i, int i2, String str, String str2, String str3, String str4, Date date, boolean z, int i3, int i4) {
            this.id = i;
            this.gameId = i2;
            this.name = str;
            this.slug = str2;
            this.url = str3;
            this.iconUrl = str4;
            this.dateModified = date;
            this.isClass = z;
            this.classId = i3;
            this.parentCategoryId = i4;
            this.subcategories = new ArrayList();
        }

        public int getId() {
            return this.id;
        }

        public int getGameId() {
            return this.gameId;
        }

        public String getName() {
            return this.name;
        }

        public String getSlug() {
            return this.slug;
        }

        public String getUrl() {
            return this.url;
        }

        public String getIconUrl() {
            return this.iconUrl;
        }

        public Date getDateModified() {
            return this.dateModified;
        }

        public boolean isClass() {
            return this.isClass;
        }

        public int getClassId() {
            return this.classId;
        }

        public int getParentCategoryId() {
            return this.parentCategoryId;
        }

        public List<Category> getSubcategories() {
            return this.subcategories;
        }

        public RemoteModRepository.Category toCategory() {
            return new RemoteModRepository.Category(this, Integer.toString(this.id), (List) getSubcategories().stream().map(CurseAddon$Category$$ExternalSyntheticLambda0.INSTANCE).collect(Collectors.toList()));
        }
    }
}
