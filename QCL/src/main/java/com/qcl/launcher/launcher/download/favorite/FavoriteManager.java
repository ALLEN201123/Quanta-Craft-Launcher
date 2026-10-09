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
        if (IDS.contains(id) || (item.slug != null && !item.slug.isEmpty()
                && SLUG_KEYS.contains(DownloadFavorite.makeId(item.source, item.slug)))) {
            for (int i = 0; i < LIST.size(); i++) {
                if (id.equals(LIST.get(i).id)) {
                    LIST.remove(i);
                    break;
                }
            }
            IDS.remove(id);
            if (item.slug != null) {
                SLUG_KEYS.remove(DownloadFavorite.makeId(item.source, item.slug));
            }
            save(context);
            return false;
        }
        item.favoriteTime = System.currentTimeMillis();
        if (item.groups == null) {
            item.groups = new ArrayList<>();
        }
        LIST.add(item);
        IDS.add(id);
        if (item.slug != null && !item.slug.isEmpty()) {
            SLUG_KEYS.add(DownloadFavorite.makeId(item.source, item.slug));
        }
        save(context);
        return true;
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

    public static synchronized void remove(Context context, String id) {
        init(context);
        if (id == null) {
            return;
        }
        for (int i = 0; i < LIST.size(); i++) {
            if (id.equals(LIST.get(i).id)) {
                LIST.remove(i);
                break;
            }
        }
        IDS.remove(id);
        save(context);
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
            // ★★ 防丢数据守卫：没成功读取过、或要把「有内容的文件」写成空列表 → 一律拒绝写。
            if (!loaded && LIST.isEmpty()) {
                android.util.Log.w("jrelog", "[收藏] 尚未成功读取收藏文件，拒绝写入以免清空历史");
                return;
            }
            if (LIST.isEmpty() && f.isFile() && f.length() > 0 && lastLoadedCount > 0) {
                android.util.Log.w("jrelog",
                        "[收藏] 内存列表为空但文件里有 " + lastLoadedCount + " 条，拒绝覆盖（防丢数据）");
                return;
            }
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
