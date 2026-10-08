package com.qcl.launcher.launcher.download.game;

import android.content.Context;

import com.google.gson.Gson;
import com.qcl.launcher.utils.file.AssetsUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Read-only index of historical builds that are absent from Mojang's own version manifest.
 *
 * <p>The list is a snapshot of the Betacraft archive. It exists so the download page can show the
 * complete history instead of silently dropping builds, and it deliberately stays read-only:
 * those builds have no Mojang metadata, so they are listed and marked, never faked as installable.
 */
public final class LegacyVersionArchive {

    /** Marker type used by the download list so the UI can label archive-only entries. */
    public static final String TYPE_ARCHIVE = "archive";
    /**
     * ★ 1.4.9：周快照（11w47a ~ 13w12a 这类 id 形如 {@code 13w16a}）也走归档源
     *   （Mojang 官方早已删除、Betacraft 有备份），但它们**不是「远古版」**——
     *   它们本来就是快照，应当出现在「快照版」分类里，
     *   否则玩家要去「远古版」里翻周快照（用户明确要求）。
     *   <p>
     *   ★★ 判定字符下标（这里数错过两次，记清楚）：
     *       {@code 11w47a} 的字符是 1,1,w,4,7,a
     *            下标：      0 1 2 3 4 5
     *       所以 {@code 'w'} 在<b>下标 2</b>，它<b>后面一位（下标 3）是数字</b>。
     *   · 先前错写成 {@code charAt(1)=='w'} → 74 个周快照一个没命中、全落进「远古版」
     *     （用户实测："那些快照归档版还是在远古版本里面一个没动"）。
     *   · 又一度想用 {@code \\d{2}w[a-z]}，但下标 3 是数字不是字母，同样不命中。
     *   正确条件：<b>前两位是数字 + 下标 2 是 'w' + 下标 3 是数字</b>。
     *   命中：11w47a / 12w05a / 12w05b / 13w12~-1439
     *   不误伤：a1.0.14 / b1.8 / rd-132211 / c0.0.13a / in-20100223
     */
    public static final String TYPE_SNAPSHOT = "snapshot";

    private static boolean isWeeklySnapshot(String id) {
        if (id == null || id.length() < 4) {
            return false;
        }
        char c0 = id.charAt(0);
        char c1 = id.charAt(1);
        char c2 = id.charAt(2);
        char c3 = id.charAt(3);
        return c0 >= '0' && c0 <= '9'
                && c1 >= '0' && c1 <= '9'
                && c2 == 'w'
                && c3 >= '0' && c3 <= '9';
    }

    private static String typeOf(Entry entry) {
        String id = entry.id == null ? "" : entry.id;
        return isWeeklySnapshot(id) ? TYPE_SNAPSHOT : TYPE_ARCHIVE;
    }

    /**
     * ★★★ 1.5.0：**这个版本必须走 Betacraft 归档安装流程吗？**
     *
     * <p>原来两处分派（{@code GameInstallDialog.downloadMinecraft()} 与
     * {@code MinecraftInstallTask.doInBackground()}）是**按 type 判**的：
     * {@code TYPE_ARCHIVE} 或 {@code TYPE_SNAPSHOT} → 归档流程。
     * 但 1.5.0 新增的 {@link UnlistedVersions} 里，10 个周快照（{@code 13w12~} / {@code 12w39a} …）
     * 的 type 也是 {@code snapshot}，而它们的地址是 **piston-meta 的 {@code .json}**，
     * 走归档流程必然失败（归档流程只认 Betacraft 的 {@code .info}）。
     *
     * <p>⇒ 判据改成**看地址**，这才是唯一可靠的分水岭：
     * <ul>
     *   <li>归档（Betacraft）= 地址以 {@code .info} 结尾（或 type 显式为 {@code archive}）</li>
     *   <li>正常（Mojang / piston-meta）= 地址以 {@code .json} 结尾</li>
     * </ul>
     */
    public static boolean isArchiveBuild(VersionManifest.Version version) {
        if (version == null) return false;
        if (TYPE_ARCHIVE.equals(version.type)) return true;
        String url = version.url == null ? "" : version.url;
        return url.endsWith(".info");
    }

    private static final String ASSET_PATH = "legacy_version_archive.json";

    private static List<VersionManifest.Version> cache;

    private LegacyVersionArchive() {
    }

    private static final class Entry {
        private String id;
        private String otherName;
        private long releaseTime;
        private long compileTime;
        /** Date used for ordering and display; falls back to the compile date when the archive
         *  only stored a placeholder release date. */
        private long sortTime;
        private String category;
        /** Real, downloadable source: the Betacraft archive entry for this build. */
        private String infoUrl;
        private boolean officialId;
    }

    private static final class Root {
        private String source;
        private String note;
        private int count;
        private List<Entry> entries;
    }

    /** Returns the archive entries wrapped as manifest versions. Never throws, never returns null. */
    public static synchronized List<VersionManifest.Version> entries(Context context) {
        if (cache != null) return cache;

        List<VersionManifest.Version> result = new ArrayList<>();
        try {
            String raw = AssetsUtils.readAssetsTxt(context, ASSET_PATH);
            Root root = new Gson().fromJson(raw, Root.class);
            if (root != null && root.entries != null) {
                // Inner classes of VersionManifest are non-static, so they need an enclosing instance.
                VersionManifest shell = new VersionManifest(null, new VersionManifest.Version[0]);
                for (Entry entry : root.entries) {
                    if (entry == null || entry.id == null || entry.id.isEmpty()) continue;
                    // ★ 1.2.3：不再过滤 officialId 的条目（用户明确要求「不准过滤」）。
                    //   原来这里把 b1.7.3 这种 officialId=true 的版本跳过了，
                    //   导致整合包要自动下载本体时，在归档清单里找不到 b1.7.3，
                    //   报「没有它的下载地址」。现在不过滤，全部保留。
                    long stamp = entry.sortTime > 0 ? entry.sortTime
                            : (entry.compileTime > 0 ? entry.compileTime : entry.releaseTime);
                    Date when = stamp > 0 ? new Date(stamp) : null;
                    result.add(shell.new Version(entry.id, typeOf(entry),
                            entry.infoUrl == null ? "" : entry.infoUrl, when, when));
                }
            }
        } catch (Exception ignored) {
            // A missing or unreadable index must never break the normal version list.
        }
        cache = result;
        return cache;
    }
}
