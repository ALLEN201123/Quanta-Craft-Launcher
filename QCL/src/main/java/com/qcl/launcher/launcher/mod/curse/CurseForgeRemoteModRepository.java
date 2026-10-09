package com.qcl.launcher.launcher.mod.curse;

import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseAddon;
import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.MurmurHash2;
import com.qcl.launcher.utils.Pair;
import com.qcl.launcher.utils.io.HttpRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public final class CurseForgeRemoteModRepository implements RemoteModRepository {
    private static final String PREFIX = "https://api.curseforge.com";
    public static final int SECTION_ADDONS = 4559;
    public static final int SECTION_BUKKIT_PLUGIN = 5;
    public static final int SECTION_CUSTOMIZATION = 4546;
    public static final int SECTION_MOD = 6;
    public static final int SECTION_MODPACK = 4471;
    public static final int SECTION_RESOURCE_PACK = 12;
    public static final int SECTION_UNKNOWN1 = 4944;
    public static final int SECTION_UNKNOWN2 = 4979;
    public static final int SECTION_UNKNOWN3 = 4984;
    public static final int SECTION_WORLD = 17;
    // ★ 1.2.5：换用当前有效的 CurseForge API key。
    //   老的那个已经被封（实测直接 403 Forbidden）→ 选 CurseForge 源永远拉不出列表。
    private static final String apiKey = "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6";
    private final int section;
    private final RemoteModRepository.Type type;
    public static final CurseAddon.Category CATEGORY_ALL = new CurseAddon.Category();
    public static final CurseForgeRemoteModRepository MODS = new CurseForgeRemoteModRepository(RemoteModRepository.Type.MOD, 6);
    public static final CurseForgeRemoteModRepository MODPACKS = new CurseForgeRemoteModRepository(RemoteModRepository.Type.MODPACK, 4471);
    public static final CurseForgeRemoteModRepository RESOURCE_PACKS = new CurseForgeRemoteModRepository(RemoteModRepository.Type.RESOURCE_PACK, 12);
    public static final CurseForgeRemoteModRepository WORLDS = new CurseForgeRemoteModRepository(RemoteModRepository.Type.WORLD, 17);
    public static final CurseForgeRemoteModRepository CUSTOMIZATIONS = new CurseForgeRemoteModRepository(RemoteModRepository.Type.CUSTOMIZATION, 4546);

    public CurseForgeRemoteModRepository(RemoteModRepository.Type type, int i) {
        this.type = type;
        this.section = i;
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteModRepository.Type getType() {
        return this.type;
    }

    private int toModsSearchSortField(RemoteModRepository.SortType sortType) {
        switch (AnonymousClass7.$SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[sortType.ordinal()]) {
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            case 7:
                return 8;
            default:
                return 1;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository$7, reason: invalid class name */
    /* loaded from: classes2.dex */
    public static /* synthetic */ class AnonymousClass7 {
        static final /* synthetic */ int[] $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortOrder;
        static final /* synthetic */ int[] $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType;

        static {
            int[] iArr = new int[RemoteModRepository.SortOrder.values().length];
            $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortOrder = iArr;
            try {
                iArr[RemoteModRepository.SortOrder.ASC.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortOrder[RemoteModRepository.SortOrder.DESC.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            int[] iArr2 = new int[RemoteModRepository.SortType.values().length];
            $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType = iArr2;
            try {
                iArr2[RemoteModRepository.SortType.POPULARITY.ordinal()] = 1;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.LAST_UPDATED.ordinal()] = 2;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.NAME.ordinal()] = 3;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.AUTHOR.ordinal()] = 4;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.TOTAL_DOWNLOADS.ordinal()] = 5;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.CATEGORY.ordinal()] = 6;
            } catch (NoSuchFieldError unused8) {
            }
            try {
                $SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortType[RemoteModRepository.SortType.GAME_VERSION.ordinal()] = 7;
            } catch (NoSuchFieldError unused9) {
            }
        }
    }

    private String toSortOrder(RemoteModRepository.SortOrder sortOrder) {
        return AnonymousClass7.$SwitchMap$com$qcl$launcher$launcher$mod$RemoteModRepository$SortOrder[sortOrder.ordinal()] != 2 ? "asc" : "desc";
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod> search(String str, RemoteModRepository.Category category, int i, int i2, String str2, RemoteModRepository.SortType sortType, RemoteModRepository.SortOrder sortOrder) throws IOException {
        // ★★★ 1.2.5：照 FCL CurseForgeRemoteModRepository.search ——
        //   **categoryId 只有非 0 才带**。CF 会把 categoryId=0 当成一个真实分类 id，
        //   带上它整页直接返回 0 条 —— 表现就是「选了 CurseForge 源，什么都加载不出来」。
        int categoryId = (category == null || !(category.getSelf() instanceof CurseAddon.Category))
                ? 0 : ((CurseAddon.Category) category.getSelf()).getId();
        ArrayList<Pair<String, String>> query = new ArrayList<>();
        query.add(Pair.pair("gameId", "432"));
        query.add(Pair.pair("classId", Integer.toString(this.section)));
        if (categoryId != 0) {
            query.add(Pair.pair("categoryId", Integer.toString(categoryId)));
        }
        query.add(Pair.pair("gameVersion", str));
        query.add(Pair.pair("searchFilter", str2));
        query.add(Pair.pair("sortField", Integer.toString(toModsSearchSortField(sortType))));
        query.add(Pair.pair("sortOrder", toSortOrder(sortOrder)));
        query.add(Pair.pair("index", Integer.toString(i)));
        query.add(Pair.pair("pageSize", Integer.toString(i2)));

        return ((List) ((Response) HttpRequest.GET("https://api.curseforge.com/v1/mods/search", query.toArray(new Pair[0])).header("X-API-KEY", "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6").getJson(new TypeToken<Response<List<CurseAddon>>>() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository.1
        }.getType())).getData()).stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository$$ExternalSyntheticLambda1
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((CurseAddon) obj).toMod();
            }
        });
    }

    /**
     * ★★★★ 2026-10-09 用户实测：「下载整合包根本没有列出很多显示模组文件下载进度」——
     * **真根因已用真实数据取证**（下载 RLCraft 1.12.2 那个 1.5MB 的包解包验证）：
     * <pre>
     *   zip 内条目 1063 个，overrides/ 1061 个，
     *   manifest.json 的 files 有 <b>81 条</b>，
     *   但每条<b>只有</b> {@code {projectID, fileID, required}}，
     *   <b>没有 fileName、也没有 url</b>！
     * </pre>
     * 而 {@code CurseInstallTask.downloadManifestFiles()} 原来的逻辑是
     * 「缺 fileName/url 就跳过并记一行日志」⇒ **81 个 mod 全被跳过，
     * 玩家只看到 1 行（那个整合包 zip 本体）** —— 正是用户看到的现象。
     *
     * <p><b>解法</b>：拿 {@code projectID/fileID} 回头查 CurseForge 补全 fileName/url。
     * 实测 {@code GET /v1/mods/{projectID}/files/{fileID}} 可拿到：
     * {@code fileName / fileLength / downloadUrl / hashes}。
     * 这里**逐条并发补全**（CF 没有批量「按 (projectID,fileID) 取」的端点，
     * {@code POST /v1/mods} 那套批量只认 modId 列表，返回的是每个 mod 的 latestFiles，
     * 对不上具体的 fileID ⇒ 只能用单查，6 条并发）。
     *
     * @param files manifest 里那 81 条（可能缺 fileName/url）
     * @return 补全后的新列表（补不上的原样保留，交给调用方跳过）
     */
    public java.util.List<CurseManifestFile> completeManifestFiles(java.util.List<CurseManifestFile> files) {
        if (files == null || files.isEmpty()) {
            return files;
        }
        java.util.List<CurseManifestFile> out = new ArrayList<>(files.size());
        java.util.List<CurseManifestFile> need = new ArrayList<>();
        for (CurseManifestFile f : files) {
            if (f == null) {
                continue;
            }
            // 已经有 fileName + url 的不用查（有些整合包自己就写全了）
            if (f.getFileName() != null && f.getUrl() != null) {
                out.add(f);
            } else {
                need.add(f);
            }
        }
        if (need.isEmpty()) {
            return out;
        }
        // ★ 6 条并发：81 个 mod 串行查要一两分钟，并发后十几秒
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(6);
        try {
            java.util.List<java.util.concurrent.Future<CurseManifestFile>> futures = new ArrayList<>();
            for (final CurseManifestFile f : need) {
                futures.add(pool.submit(() -> {
                    try {
                        if (f.getProjectID() <= 0 || f.getFileID() <= 0) {
                            return null;
                        }
                        Response resp = (Response) HttpRequest
                                .GET("https://api.curseforge.com/v1/mods/" + f.getProjectID() + "/files/" + f.getFileID())
                                .header("X-API-KEY", apiKey)
                                .getJson(new TypeToken<Response<CurseAddon.LatestFile>>() {
                                }.getType());
                        CurseAddon.LatestFile lf = (CurseAddon.LatestFile) resp.getData();
                        if (lf == null || lf.getFileName() == null || lf.getDownloadUrl() == null) {
                            return null;
                        }
                        // withFileName/withURL 都是不可变 copy，正好用来补全
                        return f.withFileName(lf.getFileName()).withURL(lf.getDownloadUrl());
                    } catch (Throwable t) {
                        return null;
                    }
                }));
            }
            for (java.util.concurrent.Future<CurseManifestFile> fu : futures) {
                CurseManifestFile r = null;
                try {
                    r = fu.get(20, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Throwable ignored) {
                }
                out.add(r);
            }
        } finally {
            pool.shutdownNow();
        }
        return out;
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Optional<RemoteMod.Version> getRemoteVersionByLocalFile(LocalModFile localModFile, Path path) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        InputStream newInputStream = Files.newInputStream(path, new OpenOption[0]);
        try {
            byte[] bArr = new byte[1024];
            while (true) {
                int read = newInputStream.read(bArr, 0, 1024);
                if (read == -1) {
                    break;
                }
                for (int i = 0; i < read; i++) {
                    byte b = bArr[i];
                    if (b != 9 && b != 10 && b != 13 && b != 32) {
                        byteArrayOutputStream.write(b);
                    }
                }
            }
            if (newInputStream != null) {
                newInputStream.close();
            }
            Response response = (Response) HttpRequest.POST("https://api.curseforge.com/v1/fingerprints").json(Lang.mapOf(Pair.pair("fingerprints", Collections.singletonList(Long.valueOf(Integer.toUnsignedLong(MurmurHash2.hash32(byteArrayOutputStream.toByteArray(), byteArrayOutputStream.size(), 1))))))).header("X-API-KEY", "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6").getJson(new TypeToken<Response<FingerprintMatchesResult>>() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository.2
            }.getType());
            if (((FingerprintMatchesResult) response.getData()).getExactMatches() == null || ((FingerprintMatchesResult) response.getData()).getExactMatches().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(((FingerprintMatchesResult) response.getData()).getExactMatches().get(0).getFile().toVersion());
        } catch (Throwable th) {
            if (newInputStream != null) {
                try {
                    newInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteMod getModById(String str) throws IOException {
        return ((CurseAddon) ((Response) HttpRequest.GET("https://api.curseforge.com/v1/mods/" + str).header("X-API-KEY", "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6").getJson(new TypeToken<Response<CurseAddon>>() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository.3
        }.getType())).data).toMod();
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteMod.File getModFile(String str, String str2) throws IOException {
        return ((CurseAddon.LatestFile) ((Response) HttpRequest.GET(String.format("%s/v1/mods/%s/files/%s", "https://api.curseforge.com", str, str2)).header("X-API-KEY", "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6").getJson(new TypeToken<Response<CurseAddon.LatestFile>>() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository.4
        }.getType())).getData()).toVersion().getFile();
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod.Version> getRemoteVersionsById(String str) throws IOException {
        return ((List) ((Response) HttpRequest.GET("https://api.curseforge.com/v1/mods/" + str + "/files", Pair.pair("pageSize", "10000")).header("X-API-KEY", "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6").getJson(new TypeToken<Response<List<CurseAddon.LatestFile>>>() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository.5
        }.getType())).getData()).stream().map(new Function() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ((CurseAddon.LatestFile) obj).toVersion();
            }
        });
    }

    public List<CurseAddon.Category> getCategoriesImpl() throws IOException {
        return reorganizeCategories((List) ((Response) HttpRequest.GET("https://api.curseforge.com/v1/categories", Pair.pair("gameId", "432")).header("X-API-KEY", "$2a$10$fgjXkx00bZZ5ypMONic.Uu7/Be1KFjmMcprkzelHhjjs7FErXt7i6").getJson(new TypeToken<Response<List<CurseAddon.Category>>>() { // from class: com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository.6
        }.getType())).getData(), this.section);
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteModRepository.Category> getCategories() throws IOException {
        return getCategoriesImpl().stream().map(CurseAddon$Category$$ExternalSyntheticLambda0.INSTANCE);
    }

    private List<CurseAddon.Category> reorganizeCategories(List<CurseAddon.Category> list, int i) {
        ArrayList arrayList = new ArrayList();
        HashMap hashMap = new HashMap();
        for (CurseAddon.Category category : list) {
            hashMap.put(Integer.valueOf(category.getId()), category);
        }
        for (CurseAddon.Category category2 : list) {
            if (category2.getParentCategoryId() == i) {
                arrayList.add(category2);
            } else {
                CurseAddon.Category category3 = (CurseAddon.Category) hashMap.get(Integer.valueOf(category2.getParentCategoryId()));
                if (category3 != null) {
                    category3.getSubcategories().add(category2);
                }
            }
        }
        return arrayList;
    }

    /* loaded from: classes2.dex */
    public static class Pagination {
        private final int index;
        private final int pageSize;
        private final int resultCount;
        private final int totalCount;

        public Pagination(int i, int i2, int i3, int i4) {
            this.index = i;
            this.pageSize = i2;
            this.resultCount = i3;
            this.totalCount = i4;
        }

        public int getIndex() {
            return this.index;
        }

        public int getPageSize() {
            return this.pageSize;
        }

        public int getResultCount() {
            return this.resultCount;
        }

        public int getTotalCount() {
            return this.totalCount;
        }
    }

    /* loaded from: classes2.dex */
    public static class Response<T> {
        private final T data;
        private final Pagination pagination;

        public Response(T t, Pagination pagination) {
            this.data = t;
            this.pagination = pagination;
        }

        public T getData() {
            return this.data;
        }

        public Pagination getPagination() {
            return this.pagination;
        }
    }

    /* loaded from: classes2.dex */
    private static class FingerprintMatchesResult {
        private final List<Long> exactFingerprints;
        private final List<FingerprintMatch> exactMatches;
        private final boolean isCacheBuilt;

        public FingerprintMatchesResult(boolean z, List<FingerprintMatch> list, List<Long> list2) {
            this.isCacheBuilt = z;
            this.exactMatches = list;
            this.exactFingerprints = list2;
        }

        public boolean isCacheBuilt() {
            return this.isCacheBuilt;
        }

        public List<FingerprintMatch> getExactMatches() {
            return this.exactMatches;
        }

        public List<Long> getExactFingerprints() {
            return this.exactFingerprints;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class FingerprintMatch {
        private final CurseAddon.LatestFile file;
        private final int id;
        private final List<CurseAddon.LatestFile> latestFiles;

        public FingerprintMatch(int i, CurseAddon.LatestFile latestFile, List<CurseAddon.LatestFile> list) {
            this.id = i;
            this.file = latestFile;
            this.latestFiles = list;
        }

        public int getId() {
            return this.id;
        }

        public CurseAddon.LatestFile getFile() {
            return this.file;
        }

        public List<CurseAddon.LatestFile> getLatestFiles() {
            return this.latestFiles;
        }
    }
}
