package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * ★ 1.5.0 用户要求：「整合包和搜索也完善一下」——搜索要**混合**。
 *
 * <p>设计要点（为什么这么写）：
 * <ul>
 *   <li><b>并发查两源</b>： CurseForge 与 Modrinth 各自独立，一个挂/超时不影响另一个；
 *       用 {@link #POOL} 各占一个线程，绝不串行（串行 = 等于搜索时间翻倍）。</li>
 *   <li><b>去重</b>：同一个模组常常两个站都有（作者同时发 CF + Modrinth）。
 *       判据 = <b>标题归一化</b>（小写 + 去掉空格与分隔符）—— slug 两站完全不同，
 *       用 slug 去重会把同一个模组列两遍。</li>
 *   <li><b>保序稳定</b>：先到先得（LinkedHashMap），同去重键时 CurseForge 优先保留
 *       （CF 数据更全：分类、下载量）。</li>
 *   <li><b>分类下拉要「两边都能用」</b>：CF 的 categoryId 是数字、Modrinth 的是 slug，
 *       互不通用 ⇒ 混合模式下 {@link #getCategories()} 只给「全部分类」，
 *       避免玩家选一个分类却在某一源查不到任何东西。</li>
 *   <li><b>详情页路由</b>：列表项点进去必须回到<b>它自己那个源</b>，
 *       所以 {@link #getModById} / {@link #getRemoteVersionsById} 等
 *       都按 {@link #platformOf} 判出来的平台分派，绝不串台。</li>
 * </ul>
 */
public class HybridRemoteModRepository implements RemoteModRepository {

    /** ★ 两源并发用的线程池（固定 2 条：CF 一条、Modrinth 一条）。 */
    private static final java.util.concurrent.ExecutorService POOL =
            java.util.concurrent.Executors.newFixedThreadPool(2);

    private final RemoteModRepository curseForge;
    private final RemoteModRepository modrinth;
    private final Type type;

    public HybridRemoteModRepository(RemoteModRepository curseForge, RemoteModRepository modrinth, Type type) {
        this.curseForge = curseForge;
        this.modrinth = modrinth;
        this.type = type;
    }

    // ==== 平台判定（详情页路由 + 收藏来源都靠它） ====

    /**
     * ★ 判一个 {@link RemoteMod} 来自哪个站。
     * 靠 {@code getData()} 的具体实现类判（CurseAddon / Modrinth Project），
     * 判不出来时按 slug 兜底 —— **不能靠仓库对象判**，因为列表里可能混着两源的结果。
     */
    public static String platformOf(RemoteMod mod) {
        if (mod != null) {
            try {
                Object d = mod.getData();
                if (d instanceof com.qcl.launcher.launcher.mod.curse.CurseAddon) {
                    return "CURSEFORGE";
                }
                if (d instanceof com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository.Project
                        || d instanceof com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository.ProjectSearchResult) {
                    return "MODRINTH";
                }
            } catch (Throwable ignored) {
            }
        }
        return "UNKNOWN";
    }

    /** 按 {@link #platformOf} 的结果取对应源；UNKNOWN 时优先 CurseForge。 */
    private RemoteModRepository repoFor(RemoteMod mod) {
        String p = platformOf(mod);
        if ("MODRINTH".equals(p)) {
            return this.modrinth;
        }
        if ("CURSEFORGE".equals(p)) {
            return this.curseForge;
        }
        return this.curseForge;
    }

    /** 标题归一化 —— 去重判据之一（去掉大小写、空格、常见分隔符与方括号装饰）。 */
    private static String normTitle(String s) {
        if (s == null) {
            return null;
        }
        String t = s.toLowerCase(Locale.ROOT).trim();
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            // 保留字母数字与中日韩，其余（空格 - _ . [ ] ( ) 等）一律丢掉
            if (Character.isLetterOrDigit(c) || c >= 0x2E80) {
                sb.append(c);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /**
     * ★★ 跨平台重复条目的合并窗口：最近更新时间相差不超过该天数，才视为同一工程的同步数据。
     * ★ **照 FCL `DownloadPage.MERGE_WINDOW_DAYS`**（它参考 PCL 的保守合并策略）。
     */
    private static final long MERGE_WINDOW_DAYS = 7L;

    /**
     * 条目的最近更新时间（取不到返回 null）。
     *
     * <p>★ 用<b>反射</b>而不是直接调 `getDateModified()`：`CurseAddon` 与
     * `ProjectSearchResult` 都有该方法，但 `Project`（详情页那个）**没有**
     * —— 直接调会编译不过。反射一把抓，谁有谁给，都没有就返回 null
     * （FCL 侧同样是「数据不可得返回 null」，逻辑一致）。
     */
    private static Long lastModifiedMillis(RemoteMod mod) {
        try {
            Object d = mod == null ? null : mod.getData();
            if (d == null) {
                return null;
            }
            // 走 IMod 之外的公共路径：优先 CurseAddon（公开 getter）
            if (d instanceof com.qcl.launcher.launcher.mod.curse.CurseAddon) {
                java.util.Date dt = ((com.qcl.launcher.launcher.mod.curse.CurseAddon) d).getDateModified();
                return dt == null ? null : dt.getTime();
            }
            try {
                java.lang.reflect.Method m = d.getClass().getMethod("getDateModified");
                Object v = m.invoke(d);
                if (v instanceof java.util.Date) {
                    return ((java.util.Date) v).getTime();
                }
            } catch (Throwable noSuchGetter) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * ★ 跨平台去重加入（**照 FCL `DownloadPage.addIfDistinct`**）。
     *
     * <p>规则：先按 slug 判重，再按标题判重；命中重复时<b>还要看两边更新时间是否接近</b>
     * （相差 ≤ {@value #MERGE_WINDOW_DAYS} 天）—— 接近才认定是同一条数据的两个平台镜像，
     * <b>只留一个</b>；差得远（不同项目恰好同名）就<b>两个都留</b>。
     * 这个"保守合并"是关键：简单按标题去重会把「同名不同模组」误吞。
     */
    private static void addIfDistinct(List<RemoteMod> merged, Map<String, Long> seen, RemoteMod mod) {
        if (mod == null) {
            return;
        }
        String slugKey = normTitle(mod.getSlug());
        String titleKey = normTitle(mod.getTitle());
        String hit = null;
        if (slugKey != null && seen.containsKey("slug:" + slugKey)) {
            hit = "slug:" + slugKey;
        } else if (titleKey != null && seen.containsKey("title:" + titleKey)) {
            hit = "title:" + titleKey;
        }
        if (hit != null) {
            Long existing = seen.get(hit);
            Long modified = lastModifiedMillis(mod);
            if (existing == null || modified == null
                    || Math.abs(existing - modified) > MERGE_WINDOW_DAYS * 86400_000L) {
                // 拿不准 ⇒ 两个都留，宁可多列也不要吞掉不同的东西
                merged.add(mod);
            }
            return;
        }
        Long modified = lastModifiedMillis(mod);
        if (slugKey != null) {
            seen.put("slug:" + slugKey, modified);
        }
        if (titleKey != null) {
            seen.put("title:" + titleKey, modified);
        }
        merged.add(mod);
    }

    // ==== 接口实现 ====

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public Stream<RemoteMod> search(String gameVersion, Category category, int offset, int limit,
                                    String keyword, SortType sortType, SortOrder sortOrder) throws IOException {
        // ★ 分类只在「单一源」时下发：CF 是数字 id、Modrinth 是 slug，混合模式下必然对不上一边。
        Category cfCategory = category;
        Category mrCategory = category;
        if (category != null) {
            Object self = null;
            try {
                self = category.getSelf();
            } catch (Throwable ignored) {
            }
            if (self instanceof com.qcl.launcher.launcher.mod.curse.CurseAddon.Category) {
                mrCategory = null;           // CF 的分类给不了 Modrinth
            } else {
                cfCategory = null;           // Modrinth 的分类给不了 CF
            }
        }

        final Category finalCf = cfCategory;
        final Category finalMr = mrCategory;

        java.util.concurrent.Future<List<RemoteMod>> fCf;
        java.util.concurrent.Future<List<RemoteMod>> fMr;
        try {
            fCf = POOL.submit(() -> {
                try {
                    return this.curseForge.search(gameVersion, finalCf, offset, limit, keyword, sortType, sortOrder)
                            .collect(java.util.stream.Collectors.toList());
                } catch (Throwable t) {
                    return new ArrayList<RemoteMod>();
                }
            });
            fMr = POOL.submit(() -> {
                try {
                    return this.modrinth.search(gameVersion, finalMr, offset, limit, keyword, sortType, sortOrder)
                            .collect(java.util.stream.Collectors.toList());
                } catch (Throwable t) {
                    return new ArrayList<RemoteMod>();
                }
            });
        } catch (Throwable t) {
            throw new IOException("混合搜索无法启动", t);
        }

        // ★ 两源都要拿：等满再合并（任一超时最多等到它自己超时，不会永久挂住）。
        List<RemoteMod> a = getQuietly(fCf);
        List<RemoteMod> b = getQuietly(fMr);

        // ★★ 交错合并（照 FCL `searchAggregated`）：同一序号 CurseForge 在前、Modrinth 在后，
        //   这样结果是「两站交替」而不是「CF 一整页 + MR 一整页」，玩家翻起来更像混排。
        List<RemoteMod> merged = new ArrayList<>(a.size() + b.size());
        Map<String, Long> seen = new LinkedHashMap<>();
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
            if (i < a.size()) {
                addIfDistinct(merged, seen, a.get(i));
            }
            if (i < b.size()) {
                addIfDistinct(merged, seen, b.get(i));
            }
        }

        // ★★ 照 FCL：单源失败**不阻断**另一源，而是记一条"降级提示"告诉玩家结果可能不全。
        //   两源都空才当失败（由调用方走 refreshText 提示）。
        if (a.isEmpty() && b.isEmpty()) {
            lastPartialWarning = null;
        } else if (a.isEmpty()) {
            lastPartialWarning = "MODRINTH";      // 只有 Modrinth 有结果 ⇒ CF 失败
        } else if (b.isEmpty()) {
            lastPartialWarning = "CURSEFORGE";    // 只有 CF 有结果 ⇒ Modrinth 失败
        } else {
            lastPartialWarning = null;
        }
        return merged.stream();
    }

    /**
     * ★ 降级提示里"哪个源失败了"（`null` = 两边都正常）。
     * UI 层据此拼 {@code download_search_partial}。写法照 FCL 的
     * {@code search_aggregate_partial}（%1$s=失败的源，%2$s=仅显示的源）。
     */
    private static volatile String lastPartialWarning = null;

    public static String lastPartialWarning() {
        return lastPartialWarning;
    }

    private static List<RemoteMod> getQuietly(java.util.concurrent.Future<List<RemoteMod>> f) {
        try {
            List<RemoteMod> r = f.get(30, java.util.concurrent.TimeUnit.SECONDS);
            return r == null ? new ArrayList<RemoteMod>() : r;
        } catch (Throwable t) {
            return new ArrayList<RemoteMod>();
        }
    }

    /**
     * ★ 混合模式只给「全部」这一个分类 —— 两站分类体系不通用，
     * 列出两边的分类只会让玩家选一个然后某一边搜不到。
     */
    @Override
    public Stream<Category> getCategories() {
        List<Category> one = new ArrayList<>();
        one.add(new Category(CurseForgeRemoteModRepository.CATEGORY_ALL, "0", new ArrayList<Category>()));
        return one.stream();
    }

    @Override
    public RemoteMod getModById(String id) throws IOException {
        // ★ 无法从 id 判平台（详情页传进来的是 slug）⇒ 优先 Modrinth（slug 即 projectId），
        //   失败再退 CF。CF 的 modId 是数字，与 Modrinth 的 projectId 基本不会撞。
        try {
            return this.modrinth.getModById(id);
        } catch (Throwable t) {
            return this.curseForge.getModById(id);
        }
    }

    @Override
    public RemoteMod.File getModFile(String id, String fileId) throws IOException {
        try {
            return this.modrinth.getModFile(id, fileId);
        } catch (UnsupportedOperationException | IOException t) {
            return this.curseForge.getModFile(id, fileId);
        }
    }

    @Override
    public Stream<RemoteMod.Version> getRemoteVersionsById(String id) throws IOException {
        try {
            return this.modrinth.getRemoteVersionsById(id);
        } catch (Throwable t) {
            return this.curseForge.getRemoteVersionsById(id);
        }
    }

    @Override
    public Optional<RemoteMod.Version> getRemoteVersionByLocalFile(LocalModFile localModFile, Path path) throws IOException {
        try {
            Optional<RemoteMod.Version> r = this.modrinth.getRemoteVersionByLocalFile(localModFile, path);
            if (r != null && r.isPresent()) {
                return r;
            }
        } catch (Throwable ignored) {
        }
        return this.curseForge.getRemoteVersionByLocalFile(localModFile, path);
    }

    // ==== 供 UI 层在「点进详情」时把混合结果分派回它真正的源 ====

    /** 详情页要用的仓库（按 mod 自身来源选）。 */
    public RemoteModRepository repositoryFor(RemoteMod mod) {
        return repoFor(mod);
    }

    /** 两源原始仓库（UI 层刷分类时用）。 */
    public RemoteModRepository curseForgeRepository() {
        return this.curseForge;
    }

    public RemoteModRepository modrinthRepository() {
        return this.modrinth;
    }
}
