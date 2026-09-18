package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.RemoteModRepository;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public class RemoteMod {
    private final String author;
    private final List<String> categories;
    private final IMod data;
    private final String description;
    private final String iconUrl;
    private final String pageUrl;
    private final String slug;
    private final String title;

    /* loaded from: classes2.dex */
    public interface IMod {
        List<RemoteMod> loadDependencies(RemoteModRepository remoteModRepository) throws IOException;

        Stream<Version> loadVersions(RemoteModRepository remoteModRepository) throws IOException;
    }

    /* loaded from: classes2.dex */
    public interface IVersion {
        Type getType();
    }

    /* loaded from: classes2.dex */
    public enum Type {
        CURSEFORGE,
        MODRINTH
    }

    /* loaded from: classes2.dex */
    public enum VersionType {
        Release,
        Beta,
        Alpha
    }

    public RemoteMod(String str, String str2, String str3, String str4, List<String> list, String str5, String str6, IMod iMod) {
        this.slug = str;
        this.author = str2;
        this.title = str3;
        this.description = str4;
        this.categories = list;
        this.pageUrl = str5;
        this.iconUrl = str6;
        this.data = iMod;
    }

    public String getSlug() {
        return this.slug;
    }

    public String getAuthor() {
        return this.author;
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

    public String getPageUrl() {
        return this.pageUrl;
    }

    public String getIconUrl() {
        return this.iconUrl;
    }

    public IMod getData() {
        return this.data;
    }

    /* loaded from: classes2.dex */
    public static class Version {
        private final String changelog;
        private final Date datePublished;
        private final List<String> dependencies;
        private final File file;
        private final List<String> gameVersions;
        private final List<ModLoaderType> loaders;
        private final String modid;
        private final String name;
        private final IVersion self;
        private final String version;
        private final VersionType versionType;

        public Version(IVersion iVersion, String str, String str2, String str3, String str4, Date date, VersionType versionType, File file, List<String> list, List<String> list2, List<ModLoaderType> list3) {
            this.self = iVersion;
            this.modid = str;
            this.name = str2;
            this.version = str3;
            this.changelog = str4;
            this.datePublished = date;
            this.versionType = versionType;
            this.file = file;
            this.dependencies = list;
            this.gameVersions = list2;
            this.loaders = list3;
        }

        public IVersion getSelf() {
            return this.self;
        }

        public String getModid() {
            return this.modid;
        }

        public String getName() {
            return this.name;
        }

        public String getVersion() {
            return this.version;
        }

        public String getChangelog() {
            return this.changelog;
        }

        public Date getDatePublished() {
            return this.datePublished;
        }

        public VersionType getVersionType() {
            return this.versionType;
        }

        public File getFile() {
            return this.file;
        }

        public List<String> getDependencies() {
            return this.dependencies;
        }

        public List<String> getGameVersions() {
            return this.gameVersions;
        }

        public List<ModLoaderType> getLoaders() {
            return this.loaders;
        }
    }

    /* loaded from: classes2.dex */
    public static class File {
        private final String filename;
        private final Map<String, String> hashes;
        private final String url;

        public File(Map<String, String> map, String str, String str2) {
            this.hashes = map;
            this.url = str;
            this.filename = str2;
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
    }

    public static RemoteModRepository.SortType getSortTypeByPosition(int i) {
        switch (i) {
            case 1:
                return RemoteModRepository.SortType.POPULARITY;
            case 2:
                return RemoteModRepository.SortType.LAST_UPDATED;
            case 3:
                return RemoteModRepository.SortType.NAME;
            case 4:
                return RemoteModRepository.SortType.AUTHOR;
            case 5:
                return RemoteModRepository.SortType.TOTAL_DOWNLOADS;
            case 6:
                return RemoteModRepository.SortType.CATEGORY;
            case 7:
                return RemoteModRepository.SortType.GAME_VERSION;
            default:
                return RemoteModRepository.SortType.DATE_CREATED;
        }
    }
}
