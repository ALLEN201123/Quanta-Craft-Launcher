package com.qcl.launcher.launcher.download.favorite;

import java.util.ArrayList;
import java.util.List;

/**
 * ★ 2026-10-09 用户要求：下载页的「收藏」功能。
 *
 * <p><b>字段与语义照抄 FCL 的 {@code com/mio/data/favorite/DownloadFavoriteEntity.kt}</b>
 * （它的 Room 表 {@code download_favorites} / {@code favorite_groups}）：
 * <pre>
 *   id            形如 "CURSEFORGE:12345" / "MODRINTH:xxxx"（主键）
 *   source        平台（CurseForge / Modrinth）
 *   type          资源类别：MOD / MODPACK / RESOURCE_PACK / SHADER_PACK / WORLD
 *   modId         平台侧项目 id
 *   slug / title / description / iconUrl / pageUrl / downloadCount / categories
 *   favoriteTime  收藏时间（毫秒）
 *   groups        所属自定义分组 id 列表（可属于多个分组）
 * </pre>
 *
 * <p>★ 唯一与 FCL 不同的是**存储介质**：FCL 用 Room + Flow；QCL 是纯 Java 工程，
 * 这里用 Gson 存成一个 JSON 文件（数据模型完全一致，以后要换 Room 也不用改字段）。
 * 详见 {@link FavoriteManager}。
 */
public class DownloadFavorite {

    public String id;
    public String source;
    public String type;
    public String modId;
    public String slug;
    public String title;
    public String description;
    public String iconUrl;
    public String pageUrl;
    public int downloadCount;
    public List<String> categories = new ArrayList<>();
    public long favoriteTime;
    public List<String> groups = new ArrayList<>();

    public DownloadFavorite() {
    }

    /** 与 FCL 一致的复合主键："平台:项目id"。 */
    public static String makeId(String source, String modId) {
        return (source == null ? "" : source) + ":" + (modId == null ? "" : modId);
    }

    public String ensureId() {
        if (id == null || id.isEmpty()) {
            id = makeId(source, modId);
        }
        return id;
    }
}
