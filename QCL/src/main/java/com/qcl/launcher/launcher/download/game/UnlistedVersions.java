package com.qcl.launcher.launcher.download.game;

import android.content.Context;

import com.google.gson.Gson;
import com.qcl.launcher.utils.file.AssetsUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ★ 1.5.0：**官方清单「漏收」的版本**索引。
 *
 * <p>背景（群员 2026-10-08 反馈「Combat Test / 1.18 实验快照 / unobfuscated 全都没有」）：
 * Mojang 的 {@code version_manifest_v2.json}（918 条）里<b>根本没有</b>这几类版本：
 * <ul>
 *   <li><b>Combat Test</b> 分支快照（{@code 1.14_combat-*} / {@code 1.15_combat-*} / {@code 1.16_combat-*}，共 12 个）</li>
 *   <li><b>实验性快照</b> {@code 1.18_experimental-snapshot-1..7}、{@code 1.19_deep_dark_experimental_snapshot-1}</li>
 *   <li><b>未混淆构建</b> {@code *_unobfuscated}（官方给模组开发者发布的反混淆版，2024 起）</li>
 *   <li>11w~13w 期间被官方清单删掉的个别周快照（{@code 13w12~} / {@code 12w39a} …）</li>
 * </ul>
 * 数据源直接搬 FCL 的 {@code assets/game/unlisted-versions.json}（205 条），
 * 这里只留 QCL 当前**真缺**的 41 条（其余远古版本 QCL 的
 * {@link LegacyVersionArchive} 已用 Betacraft 归档覆盖，不能重复添加，
 * 否则同一个版本会以两种命名在列表里出现两次）。
 *
 * <p>★★ 与归档版的**关键区别**：这些条目**自带 piston-meta 的 version JSON 地址**
 * （形如 {@code https://piston-meta.mojang.com/v1/packages/<sha1>/<id>.json}），
 * 所以它们走**正常的 Mojang 安装流程**，不是归档流程。
 * 判据就是 url 后缀：归档 = {@code .info}（Betacraft），本类 = {@code .json}（Mojang）。
 *
 * <p>类型沿用 FCL 的 {@code RemoteVersion.Type} 命名，这样「快照版」分类的口径
 * 与 FCL 的 {@code VersionInstallPage} 完全一致（FCL：PENDING / UNOBFUSCATED / SNAPSHOT
 * 三种都归「快照版」）。
 */
public final class UnlistedVersions {

    /** FCL 的 {@code Type.PENDING}：预发布 / 分支快照（Combat Test、实验性快照）。 */
    public static final String TYPE_PENDING = "pending";
    /** FCL 的 {@code Type.UNOBFUSCATED}：官方未混淆构建。 */
    public static final String TYPE_UNOBFUSCATED = "unobfuscated";

    private static final String ASSET_PATH = "unlisted_versions.json";

    private static List<VersionManifest.Version> cache;

    private UnlistedVersions() {
    }

    private static final class Entry {
        private String id;
        private String type;
        private String url;
        private String time;
        private String releaseTime;
    }

    private static final class Root {
        private String source;
        private String note;
        private int count;
        private List<Entry> entries;
    }

    /**
     * 返回「官方清单漏收」的版本，包装成 {@link VersionManifest.Version}。
     * 永不抛异常、永不返回 null（与 {@link LegacyVersionArchive#entries} 同样风格：
     * 资产缺失时只是少几条，绝不能让整个下载页打不开）。
     */
    public static synchronized List<VersionManifest.Version> entries(Context context) {
        if (cache != null) return cache;

        List<VersionManifest.Version> result = new ArrayList<>();
        try {
            String raw = AssetsUtils.readAssetsTxt(context, ASSET_PATH);
            Root root = new Gson().fromJson(raw, Root.class);
            if (root != null && root.entries != null) {
                // VersionManifest 的内部类是非 static 的，需要一个外壳实例。
                VersionManifest shell = new VersionManifest(null, new VersionManifest.Version[0]);
                for (Entry entry : root.entries) {
                    if (entry == null || entry.id == null || entry.id.isEmpty()) continue;
                    if (entry.url == null || !entry.url.startsWith("https://")) continue;
                    String type = entry.type == null ? TYPE_PENDING : entry.type;
                    Date when = parseDate(entry.releaseTime != null ? entry.releaseTime : entry.time);
                    result.add(shell.new Version(entry.id, type, entry.url, when, when));
                }
            }
        } catch (Throwable ignored) {
            // 索引缺失不能拖垮正常版本列表。
        }
        cache = result;
        return cache;
    }

    /** 是否是本类提供的「官方漏收」类型（用于 UI 归类与安装分派判定）。 */
    public static boolean isUnlistedType(String type) {
        return TYPE_PENDING.equals(type) || TYPE_UNOBFUSCATED.equals(type);
    }

    /**
     * 极简 ISO-8601 解析（{@code 2025-12-09T12:43:15+00:00}）。
     * 不引外部依赖：Gson 的 Date 反序列化对非标准格式会退化成
     * {@code java.text.ParseException}，这里自己解析更稳。
     */
    private static Date parseDate(String iso) {
        if (iso == null || iso.length() < 19) return null;
        try {
            int year = Integer.parseInt(iso.substring(0, 4));
            int month = Integer.parseInt(iso.substring(5, 7));
            int day = Integer.parseInt(iso.substring(8, 10));
            int hour = Integer.parseInt(iso.substring(11, 13));
            int minute = Integer.parseInt(iso.substring(14, 16));
            int second = Integer.parseInt(iso.substring(17, 19));
            // 时区：只认 +HH:MM / -HH:MM，缺省当 UTC
            int offsetMinutes = 0;
            if (iso.length() >= 25) {
                char sign = iso.charAt(19);
                if (sign == '+' || sign == '-') {
                    int oh = Integer.parseInt(iso.substring(20, 22));
                    int om = Integer.parseInt(iso.substring(23, 25));
                    offsetMinutes = oh * 60 + om;
                    if (sign == '-') offsetMinutes = -offsetMinutes;
                }
            }
            @SuppressWarnings("deprecation")
            Date d = new Date(year - 1900, month - 1, day, hour, minute, second);
            return new Date(d.getTime() - offsetMinutes * 60000L);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
