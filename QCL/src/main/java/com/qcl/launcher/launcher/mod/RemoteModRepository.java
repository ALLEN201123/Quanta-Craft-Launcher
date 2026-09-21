package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.RemoteMod;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public interface RemoteModRepository {
    public static final String[] DEFAULT_GAME_VERSIONS = {"26.3", "26.2", "26.1.2", "26.1.1", "26.1", "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21", "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20", "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19", "1.18.2", "1.18.1", "1.18", "1.17.1", "1.17", "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16", "1.15.2", "1.15.1", "1.15", "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14", "1.13.2", "1.13.1", "1.13", "1.12.2", "1.12.1", "1.12", "1.11.2", "1.11.1", "1.11", "1.10.2", "1.10.1", "1.10", "1.9.4", "1.9.3", "1.9.2", "1.9.1", "1.9", "1.8.9", "1.8.8", "1.8.7", "1.8.6", "1.8.5", "1.8.4", "1.8.3", "1.8.2", "1.8.1", "1.8", "1.7.10", "1.7.9", "1.7.8", "1.7.7", "1.7.6", "1.7.5", "1.7.4", "1.7.3", "1.7.2", "1.6.4", "1.6.2", "1.6.1", "1.5.2", "1.5.1", "1.4.7", "1.4.5", "1.4.6", "1.4.4", "1.4.2", "1.3.2", "1.3.1", "1.2.5", "1.2.4", "1.2.3", "1.2.2", "1.2.1", "1.1", "1.0", "b1.9p5", "b1.8.1", "b1.7.3", "b1.7.2", "b1.5_01", "b1.4_01", "a1.2.6", "a1.2.3_04", "a1.2.2", "a1.2.1_01", "a1.2.0_02"};

    /* loaded from: classes2.dex */
    public enum SortOrder {
        ASC,
        DESC
    }

    /* loaded from: classes2.dex */
    public enum SortType {
        DATE_CREATED,
        POPULARITY,
        LAST_UPDATED,
        NAME,
        AUTHOR,
        TOTAL_DOWNLOADS,
        CATEGORY,
        GAME_VERSION
    }

    /* loaded from: classes2.dex */
    public enum Type {
        MOD,
        MODPACK,
        RESOURCE_PACK,
        WORLD,
        CUSTOMIZATION
    }

    Stream<Category> getCategories() throws IOException;

    RemoteMod getModById(String str) throws IOException;

    RemoteMod.File getModFile(String str, String str2) throws IOException;

    Optional<RemoteMod.Version> getRemoteVersionByLocalFile(LocalModFile localModFile, Path path) throws IOException;

    Stream<RemoteMod.Version> getRemoteVersionsById(String str) throws IOException;

    Type getType();

    Stream<RemoteMod> search(String str, Category category, int i, int i2, String str2, SortType sortType, SortOrder sortOrder) throws IOException;

    /* loaded from: classes2.dex */
    public static class Category {
        private final String id;
        private final Object self;
        private final List<Category> subcategories;

        public Category(Object obj, String str, List<Category> list) {
            this.self = obj;
            this.id = str;
            this.subcategories = list;
        }

        public Object getSelf() {
            return this.self;
        }

        public String getId() {
            return this.id;
        }

        public List<Category> getSubcategories() {
            return this.subcategories;
        }
    }
}
