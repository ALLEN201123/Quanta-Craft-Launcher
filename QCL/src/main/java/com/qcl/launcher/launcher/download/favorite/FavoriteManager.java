package com.qcl.launcher.launcher.download.favorite;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ★ 2026-10-09 用户要求：下载页「收藏」的存取。
 *
 * <p>语义对齐 FCL 的 {@code com/mio/data/FavoriteManager.kt}：
 * 内存镜像（列表 + id 集合，便于列表里同步判断"是否已收藏"）+ 持久化 +
 * 「写库后立刻同步内存」（FCL 原注释：避免快速连点读到过期状态）。
 *
 * <p>★ 存储用 JSON 文件而不是 Room：QCL 是纯 Java 工程，引 Room 要加
 * runtime + compiler 依赖并改构建（有构建风险）。**数据模型完全一致**，以后要换 Room 只改这一层。
 * 文件放**外部私有目录**（和 debug/ 同级），方便调试时直接删。
 */
public final class FavoriteManager {

    private static final String FILE_NAME = "download_favorites.json";
    private static final Gson GSON = new Gson();

    private static final List<DownloadFavorite> LIST = new ArrayList<>();
    private static final Set<String> IDS = new HashSet<>();
    /**
     * ★ 2026-10-09 修「收藏看起来没持久化」：**备用匹配键 `source:slug`**。
     *
     * <p>根因：收藏时存的是平台 id（`IMod.getRemoteId()`，Modrinth 是 projectId），
     * 但**重新进列表时同一条目可能是另一种 `IMod` 实现**（如搜索结果），`getRemoteId()` 返回 null
     * ⇒ 退化成 slug ⇒ 与存下来的 id 对不上 ⇒ **星标又变回 ☆**（玩家以为没保存，其实文件里明明有）。
     * ⇒ 这里同时维护「id 集合」与「slug 集合」，任一命中即视为已收藏。
     */
    private static final Set<String> SLUG_KEYS = new HashSet<>();
    private static boolean loaded;
    /** 最后一次成功读/写时的条目数；用于「空列表不许覆盖有内容的文件」这道防丢数据守卫。 */
    private static int lastLoadedCount;

    private FavoriteManager() {
    }

    /**
     * 幂等初始化。
     *
     * <p>★★★ 2026-10-09 **数据丢失修复**：「之前收藏的东西全没了，重进之后就空了」。
     * <p>原实现有两个致命点：
     * <ol>
     *   <li>`loaded = true` 写在**读文件之前** ⇒ 只要读取失败（文件还没生成 / 解析失败 / 目录拿不到），
     *       内存里就是空列表，而 `loaded` 已是 true ⇒ **永远不会重试**；</li>
     *   <li>随后任何一次 `toggle()` 都会 `save()` ⇒ **把空列表覆盖回文件** ⇒ 历史收藏被清空。</li>
     * </ol>
     * ⇒ 现在：**读成功才置 loaded**；并记下"最后一次读到的条目数"，`save()` 用它做防覆盖守卫。
     */
    public static synchronized void init(Context context) {
        if (loaded) {
            return;
        }
        try {
            File f = file(context);
            if (f.isFile() && f.length() > 0) {
                List<DownloadFavorite> list = GSON.fromJson(
                        new String(readAll(f), StandardCharsets.UTF_8),
                        new TypeToken<List<DownloadFavorite>>() { }.getType());
                if (list != null) {
                    apply(list);
                    lastLoadedCount = list.size();
                }
            }
            loaded = true;      // ★ 只有读成功（或文件确实不存在）才置真
            lastLoadedCount = LIST.size();
        } catch (Throwable t) {
            // ★ 读失败**不置 loaded** ⇒ 下次还会重试；并且禁止随后把空列表写回文件
            loaded = false;
            android.util.Log.w("jrelog", "[收藏] 读取收藏文件失败（已跳过，稍后重试）：" + t);
        }
    }

    /** 是否已收藏（列表里同步判断用）。 */
    public static synchronized boolean isFavorite(String source, String modId) {
        return isFavorite(source, modId, null);
    }

    /** ★ 是否已收藏：**id 或 slug 任一命中**（避免 getRemoteId() 为 null 时状态读不回来）。 */
    public static synchronized boolean isFavorite(String source, String modId, String slug) {
        if (modId != null && !modId.isEmpty()
                && IDS.contains(DownloadFavorite.makeId(source, modId))) {
            return true;
        }
        return slug != null && !slug.isEmpty()
                && SLUG_KEYS.contains(DownloadFavorite.makeId(source, slug));
    }

    /**
     * 切换收藏状态；返回**切换后**是否为已收藏。
     *
     * <p>与 FCL 一致：写盘后立刻同步内存镜像，不等任何回调，避免快速连点读到过期状态。
     */
    public static synchronized boolean toggle(Context context, DownloadFavorite item) {
        init(context);
        if (item == null) {
            return false;
        }
        String id = item.ensureId();
        // ★★★★★ 2026-10-16 **修「取消收藏后大退重进，收藏页还显示旧的收藏」**。
        //
        //   用户实测：「取消收藏之后大退，然后重新点击收藏页，收藏页还是会显示之前的收藏」。
        //
        //   ★ 根因：判定「已收藏」有**两个键**，但删除时**只按 id 删**：
        //     <pre>
        //       判定： IDS.contains(id)                                  // 键A = "source:modId"
        //           || SLUG_KEYS.contains(makeId(source, slug))         // 键B = "source:slug"
        //       删除： for (...) if (id.equals(LIST.get(i).id)) LIST.remove(i);
        //                                                    ↑ 只按【键A】比对
        //     </pre>
        //     当这次是靠**键B（slug）**命中时（典型：收藏时 modId 拿到的是 CurseForge 的数字 id，
        //     取消时传进来的 modId 为空或不同，但 slug 一致），循环里 `id.equals(...)`
        //     **匹配不到任何一项** ⇒ 列表项没被移除；随后 `save(context)` 又把这份
        //     **未删除的列表**写回 `download_favorites.json` ⇒ **重启后旧收藏还在**。
        //     （历史上 `download_favorites.json` 里 modId 为空/不一致的条目就会这样"删不掉"。）
        //
        //   ★ 修法：删除时**按 id 与 slug 两个键一起匹配**，命中就整条移除，
        //     并用**被移除项自己的** id / slug 去清理两个索引，保证索引与列表不脱节。
        String slugKey = (item.slug == null || item.slug.isEmpty())
                ? null : DownloadFavorite.makeId(item.source, item.slug);
        boolean already = IDS.contains(id) || (slugKey != null && SLUG_KEYS.contains(slugKey));
        if (already) {
            removeMatching(id, slugKey);
            save(context);
            return false;
        }
        item.favoriteTime = System.currentTimeMillis();
        if (item.groups == null) {
            item.groups = new ArrayList<>();
        }
        LIST.add(item);
        IDS.add(id);
        if (slugKey != null) {
            SLUG_KEYS.add(slugKey);
        }
        save(context);
        return true;
    }

    /**
     * ★ 2026-10-16 新增：从列表里移除所有「按 id **或** 按 slug」匹配的收藏项，
     * 并同步清理两个索引。返回移除条数。
     *
     * <p>抽出来是因为 `toggle()`（取消收藏）和 `remove()` 都要用，
     * 且必须保证「列表」与「IDS / SLUG_KEYS」两个索引**永远一致**。
     */
    private static int removeMatching(String id, String slugKey) {
        int removed = 0;
        for (int i = LIST.size() - 1; i >= 0; i--) {
            DownloadFavorite f = LIST.get(i);
            String fid = f.ensureId();
            String fslug = (f.slug == null || f.slug.isEmpty())
                    ? null : DownloadFavorite.makeId(f.source, f.slug);
            boolean hit = (id != null && id.equals(fid))
                    || (slugKey != null && slugKey.equals(fslug));
            if (hit) {
                LIST.remove(i);
                IDS.remove(fid);
                if (fslug != null) {
                    SLUG_KEYS.remove(fslug);
                }
                removed++;
            }
        }
        // 兜底：即使列表里没找到，也把这两个键从索引里剔掉，
        // 避免"索引说已收藏、列表里却没有"的错位状态。
        if (id != null) {
            IDS.remove(id);
        }
        if (slugKey != null) {
            SLUG_KEYS.remove(slugKey);
        }
        return removed;
    }

    /**
     * ★ 2026-10-09：**自愈** —— 点「打开」重新拉到详情后，把最新的图标/标题写回收藏条目。
     *
     * <p>为什么需要：收藏时存下的是**当时的** iconUrl；项目换图标、或当初没拿到 URL 时，
     * 收藏页就永远显示不出来（图标的锅不在加载器，而在**数据是旧的**）。
     * 每次成功拉取就把数据刷新一遍，下次进来图标就有了。
     */
    public static synchronized void refresh(Context context, DownloadFavorite f,
                                            String iconUrl, String title) {
        if (f == null) {
            return;
        }
        boolean changed = false;
        if (iconUrl != null && !iconUrl.isEmpty() && !iconUrl.equals(f.iconUrl)) {
            f.iconUrl = iconUrl;
            changed = true;
        }
        if (title != null && !title.isEmpty() && !title.equals(f.title)) {
            f.title = title;
            changed = true;
        }
        if (changed) {
            save(context);
        }
    }

    /**
     * 按 id 移除一条收藏。
     *
     * ★★★★★ 2026-10-16 一并加固：原来同样**只按 id 删**，
     * 遇到「json 里 modId 为空/与本次传入不一致」的老条目一样删不掉（取消收藏无效）。
     * 现在改走 {@link #removeMatching}，并顺带按 slug 兜底匹配。
     */
    public static synchronized void remove(Context context, String id) {
        init(context);
        if (id == null) {
            return;
        }
        // 先尝试把 id 直接当「平台:slug」再匹配一次（有些调用方传的是 slug 键）
        removeMatching(id, id);
        save(context);
    }

    /**
     * ★★★★★ 1.5.7（2026-10-27）新增：**按整个对象**取消收藏，返回是否真的删掉了。
     *
     * <p><b>为什么需要这个重载</b>：收藏页的「★」按钮原本调的是
     * {@code remove(context, f.id)}，只拿一个字符串。只要 `f.id` 与内存/文件里的条目
     * 有一点不一致（历史数据里 `id` 与 `source:modId` 脱节、`modId` 为空只剩 slug、
     * 或者条目是从旧版本 json 读出来的），`removeMatching` 就**一条都命中不了**，
     * 于是 `save()` 把**没变的列表**写回去 —— 界面上（`rebuild()`）看起来"删掉了"，
     * 一重启原样回来。这就是用户报的「取消收藏，大退重进还在」的另一半原因。
     *
     * <p>这里改成**三路兜底**：`id` → `source:slug` → `source:modId`，任意一路命中就删掉；
     * 全都命中不了时打一条 warn（带全部候选键），方便下一次复现时直接从日志定位。
     */
    public static synchronized boolean remove(Context context, DownloadFavorite f) {
        init(context);
        if (f == null) {
            return false;
        }
        final String id = f.ensureId();
        final String slugKey = (f.slug == null || f.slug.isEmpty())
                ? null : DownloadFavorite.makeId(f.source, f.slug);
        int removed = removeMatching(id, slugKey);
        if (removed == 0 && f.modId != null && !f.modId.isEmpty()) {
            // 第 3 路：历史数据里 `id` 字段可能与 `source:modId` 不一致，再按 modId 试一次
            removed += removeMatching(DownloadFavorite.makeId(f.source, f.modId), slugKey);
        }
        if (removed == 0) {
            android.util.Log.w("jrelog", "[收藏] 取消收藏没有命中任何条目！id=" + id
                    + " slugKey=" + slugKey + " modId=" + f.modId + " title=" + f.title
                    + " 当前共 " + LIST.size() + " 条");
        } else {
            android.util.Log.d("jrelog", "[收藏] 已取消收藏 " + removed + " 条，剩余 " + LIST.size() + " 条");
        }
        save(context);
        return removed > 0;
    }

    /** 收藏列表（按收藏时间倒序，最新在前 —— 与 FCL 一致）。 */
    public static synchronized List<DownloadFavorite> list() {
        List<DownloadFavorite> out = new ArrayList<>(LIST);
        Collections.sort(out, (a, b) -> Long.compare(b.favoriteTime, a.favoriteTime));
        return out;
    }

    // ------------------------------------------------------------------

    private static void apply(List<DownloadFavorite> list) {
        LIST.clear();
        IDS.clear();
        SLUG_KEYS.clear();
        for (DownloadFavorite f : list) {
            if (f == null) {
                continue;
            }
            if (f.categories == null) {
                f.categories = new ArrayList<>();
            }
            if (f.groups == null) {
                f.groups = new ArrayList<>();
            }
            LIST.add(f);
            IDS.add(f.ensureId());
            if (f.slug != null && !f.slug.isEmpty()) {
                SLUG_KEYS.add(DownloadFavorite.makeId(f.source, f.slug));
            }
        }
    }

    private static void save(Context context) {
        try {
            File f = file(context);
            // ★★ 防丢数据守卫：没成功读取过 → 拒绝写（内存里的空列表不代表真实情况）。
            if (!loaded) {
                android.util.Log.w("jrelog", "[收藏] 尚未成功读取收藏文件，拒绝写入以免清空历史");
                return;
            }
            // ★★★★★ 1.5.7（2026-10-27）**删除旧守卫**：
            //   <pre>
            //     if (LIST.isEmpty() && f.isFile() && f.length() > 0 && lastLoadedCount > 0) {
            //         Log.w("...内存列表为空但文件里有 N 条，拒绝覆盖（防丢数据）");
            //         return;     // ← 不写盘
            //     }
            //   </pre>
            //   这条「防丢数据」守卫本身就是**取消收藏不生效**的根因：
            //   玩家在收藏页把收藏一条条取消，取消到最后一条时 `LIST.isEmpty()` 成立 ⇒
            //   `save()` 直接 return，`download_favorites.json` **保持旧内容** ⇒
            //   大退重进，之前取消掉的收藏**全部复活**（用户报的就是这个）。
            //
            //   为什么可以放心删：`loaded == true` 的语义是「内存列表是文件的忠实镜像」
            //   （`init()` 只在**读取成功**或**文件确实不存在**时才置真，解析异常一律不置真），
            //   所以此后的空列表是**玩家的真实意图**，必须落盘。
            //   真正防丢数据靠三件事，不靠这条守卫：
            //     ① `loaded == false` 时拒写（上面那条，保留）；
            //     ② 原子写 + 旧文件先改名成 `.bak`（下面，保留）；
            //     ③ 写后回读校验（本方法末尾，1.5.7 新增）。
            File parent = f.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                return;
            }
            // ★ 原子写：先写 .tmp 再 rename，并留一份 .bak，避免写一半崩掉把文件写坏
            File tmp = new File(f.getAbsolutePath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(GSON.toJson(LIST).getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (f.isFile()) {
                File bak = new File(f.getAbsolutePath() + ".bak");
                if (bak.exists() && !bak.delete()) {
                    // 删不掉也继续，rename 会覆盖
                }
                if (!f.renameTo(bak)) {
                    android.util.Log.w("jrelog", "[收藏] 备份旧文件失败（继续写入）");
                }
            }
            if (!tmp.renameTo(f)) {
                android.util.Log.w("jrelog", "[收藏] 原子改名失败，退回直接写");
                try (FileOutputStream out = new FileOutputStream(f)) {
                    out.write(GSON.toJson(LIST).getBytes(StandardCharsets.UTF_8));
                    out.getFD().sync();
                }
            }
            // ★★★★★ 1.5.7（2026-10-27）：**写后回读校验**。
            //   为什么加：用户报「取消收藏大退重进又回来」，光看代码很难判断到底是
            //   「没删对」还是「删对了但没写进去」。这里把**文件里最终有几条**打进日志，
            //   下次复现前先看 `jrelog` 就能定位，不用靠猜。
            //   （`jrelog` 是 QCL 现有的统一日志 tag，用户拉 logcat 时按它过滤。）
            final int expect = LIST.size();
            try {
                String back = new String(readAll(f), StandardCharsets.UTF_8).trim();
                List<DownloadFavorite> verify = GSON.fromJson(back,
                        new TypeToken<List<DownloadFavorite>>() { }.getType());
                int got = verify == null ? 0 : verify.size();
                if (got == expect) {
                    android.util.Log.d("jrelog", "[收藏] 写盘校验通过：文件内 " + got
                            + " 条（内存 " + expect + " 条）");
                } else {
                    android.util.Log.w("jrelog", "[收藏] 写盘校验不一致！文件内 " + got
                            + " 条，内存 " + expect + " 条，文件内容="
                            + (back.length() > 300 ? back.substring(0, 300) + "…" : back));
                }
            } catch (Throwable ignored) {
            }
            lastLoadedCount = LIST.size();
        } catch (Throwable t) {
            android.util.Log.w("jrelog", "[收藏] 写盘失败：" + t);
        }
    }

    private static File file(Context context) {
        File dir = null;
        try {
            dir = context.getExternalFilesDir("debug");
        } catch (Throwable ignored) {
        }
        if (dir == null) {
            dir = context.getFilesDir();
        }
        return new File(dir, FILE_NAME);
    }

    private static byte[] readAll(File f) throws java.io.IOException {
        try (FileInputStream in = new FileInputStream(f)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }
}
