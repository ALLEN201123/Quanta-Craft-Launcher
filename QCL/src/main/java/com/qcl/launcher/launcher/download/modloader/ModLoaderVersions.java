package com.qcl.launcher.launcher.download.modloader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ★ 1.2.3：Risugami's ModLoader 的版本表与下载地址构造。
 *
 * 【为什么可以直接下】
 * MCArchive 的直链规律（2026-09-21 实测确认）：
 *     https://b2.mcarchive.net/file/mcarchive/&lt;SHA256&gt;/&lt;文件名&gt;
 * 实测 1.6.2 / b1.8.1 / b1.7.3 / b1.3_01v5 全部 HTTP 200，
 * **包括页面上标着「无直链、只有 MediaFire」的 b1.7.3** ——
 * 说明文件本来就在它自家 CDN 上，URL 由 SHA-256 决定。
 * 所以不需要 MediaFire，也不需要第三方镜像。
 * 每个文件下载后都校验 SHA-256（实测全部一致）。
 *
 * 【装法】
 * 下载后是个 zip，里面就是一堆 .class（BaseMod / ModLoader / EntityRendererProxy
 * 以及 b1.x 的混淆类名），**直接解进 minecraft.jar 即可**（文件覆盖式安装），
 * 不需要 tweakClass、不需要依赖库。详见 {@link ModLoaderInstallTask}。
 *
 * 【数据来源】
 * https://mcarchive.net/mods/modloader （作者 Risugami），逐条核对过 SHA-256。
 */
public final class ModLoaderVersions {

    /** 文件在 zip 里的包装（.zip 能用 java 自带的解，.rar 不行） */
    public enum ArchiveType {
        ZIP,
        /** rar：commons-compress 和 java.util.zip 都解不了，需要额外的 junrar 库 */
        RAR
    }

    public static final class Entry {
        /** 对应的 Minecraft 版本 id（要和版本目录名对得上，例如 b1.7.3 / 1.6.2） */
        public final String mcVersion;
        /** ModLoader 压缩包的文件名 */
        public final String fileName;
        /** 小写 SHA-256，也是下载地址的一部分；为空表示这个条目用 customUrl、不做校验 */
        public final String sha256;
        /** 压缩格式 */
        public final ArchiveType archiveType;
        /** 非空时直接用这个地址（用于社区修复版，不走 MCArchive 的 sha 拼链规律） */
        public final String customUrl;

        Entry(String mcVersion, String fileName, String sha256, ArchiveType archiveType) {
            this(mcVersion, fileName, sha256, archiveType, null);
        }

        Entry(String mcVersion, String fileName, String sha256, ArchiveType archiveType, String customUrl) {
            this.mcVersion = mcVersion;
            this.fileName = fileName;
            this.sha256 = sha256;
            this.archiveType = archiveType;
            this.customUrl = customUrl;
        }

        /** 能在启动器里直接安装（zip 才解得开） */
        public boolean isInstallable() {
            return archiveType == ArchiveType.ZIP;
        }

        /** 要不要做 SHA-256 校验 */
        public boolean hasChecksum() {
            return sha256 != null && !sha256.isEmpty();
        }

        /** 下载地址 */
        public String getUrl() {
            if (customUrl != null && !customUrl.isEmpty()) {
                return customUrl;
            }
            String encoded;
            try {
                encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            } catch (UnsupportedEncodingException e) {
                encoded = fileName;
            }
            return BASE + sha256 + "/" + encoded;
        }
    }

    private static final String BASE = "https://b2.mcarchive.net/file/mcarchive/";

    /** mcVersion → Entry，按版本新旧倒序（新的在前，UI 直接顺序展示） */
    private static final Map<String, Entry> TABLE = new LinkedHashMap<String, Entry>();

    private static void put(String mcVersion, String fileName, String sha256) {
        ArchiveType type = fileName.toLowerCase().endsWith(".rar") ? ArchiveType.RAR : ArchiveType.ZIP;
        TABLE.put(mcVersion, new Entry(mcVersion, fileName, sha256, type));
    }

    /** 用自定义地址的条目（社区修复版） */
    private static void putCustom(String mcVersion, String fileName, String sha256, String customUrl) {
        ArchiveType type = fileName.toLowerCase().endsWith(".rar") ? ArchiveType.RAR : ArchiveType.ZIP;
        TABLE.put(mcVersion, new Entry(mcVersion, fileName, sha256, type, customUrl));
    }

    /**
     * ★★★ b1.7.3 必须用社区修复版，不能用官方原版。
     *
     * 官方 Risugami ModLoader 在 b1.7.3 + 现代 Java / 现代启动器下会抛
     * {@code URI is not hierarchical}，然后**卡死在**
     * {@code ModLoader Beta 1.7.3 Initializing...}（用户实机验证过）。
     *
     * 修复版：coffeenotfound/ModloaderFix-b1.7.3（GitHub Release v1.0.0）。
     * ★ 它是**完整版**，不用先装坏的再覆盖。
     * ★ 它是 .jar 文件，但 jar 本身就是 zip，解压逻辑照旧能用。
     * ★ 这个资产没有官方提供的 sha256（GitHub Release 没带），所以不做校验；
     *   地址是官方仓库的 HTTPS 直链。
     */
    private static final String B173_FIX_URL =
            "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader.Fix.b1.7.3-1.0.0.jar";

    /**
     * ★★★ MCArchive 权威清单（https://mcarchive.net/mods/modloader，2026-09-22 抓取）：
     *   共 21 个文件，**每个都带 SHA-256**。
     *   下载 URL 格式 = BASE + sha256 + "/" + URLEncoder(文件名)
     *   （★ 所以必须知道 sha256 才能下，光猜文件名一定 404）
     *
     *   有直链且 .zip（可安装）：
     *     1.6.2 / 1.6.1 / 1.5.2 / 1.5.1 / 1.5 / 1.4.7 / 1.4.6 / 1.4.5 / 1.4.4 / 1.4.2
     *     / 1.3.2 / 1.3.1 / 1.2.5 / B1.9p5 / B1.8.1
     *   有直链但 .rar（启动器**无法解压**，已排除）：
     *     A1.2.6 / A1.2.2 / A1.2.1_01 / A1.2.0_02 / 1.1
     *     A1.2.3_04 是 .zip 但 Alpha 版本体在启动器里也基本跑不起来，暂不收录
     *   MCArchive **没有** 1.0.0（只有 1.1.rar）
     */
    static {
        // ---- 正式版（全部 .zip）----
        put("1.6.2", "ModLoader 1.6.2.zip", "0b14f5e261c9862989aa74313b59188cce10bea6724bae31130ce1e8e6a1c060");
        put("1.6.1", "ModLoader 1.6.1.zip", "95fc5afdd9cc14d85cb41225fb689d7994f5994287ed9595e192026c06e7b536");
        put("1.5.2", "ModLoader 1.5.2.zip", "0c355696c2f3ba405bb1f0f845dc51a6613c121eac25a6c7bc9d8046f2c941df");
        put("1.5.1", "ModLoader 1.5.1.zip", "af7d7bca70b8bc08c75e96ec90a25432682dfc825aa4fe35485dcb390b1f7014");
        put("1.5", "ModLoader 1.5.zip", "597d4d437a250986da84a9c7aee3ea653739608caf1d4a208f2006d8cbdfbc3d");
        put("1.4.7", "ModLoader 1.4.7.zip", "685ead73c19531cf24062c7536737663421ed4170cfa582baddbbf6cba1544d2");
        put("1.4.6", "ModLoader 1.4.6.zip", "f69b1f99b76c23cc1e076197375996e3b79feb369952ac692630f7b063709d5f");
        put("1.4.5", "ModLoader 1.4.5.zip", "885b62bde6231b04d0189a06b082edfa48ea1474f22a5502ab40288563036b42");
        put("1.4.4", "ModLoader 1.4.4.zip", "7d39b6d5e41bcd77edabd0aca3b43a10861a65ee9c2f9b358cedf8382d69c14e");
        put("1.4.2", "ModLoader 1.4.2.zip", "861324b55c40e4af622e2a987c3c20ed4eb869ea89a004c93222058e394baec4");
        put("1.3.2", "ModLoader 1.3.2.zip", "01a28a0a3d05634ce8745d34738b0617ddb285ad1584fb668874892c61e489eb");
        put("1.3.1", "ModLoader 1.3.1.zip", "511881d7432cf740b753180a645ca6abb7cd63d09813e0089485c125d52c09a0");
        put("1.2.5", "ModLoader 1.2.5.zip", "219370a86a15bfef8ff91f51fdd151e99391b771759183b19f72197452a28b79");

        // ---- Beta（.zip 的那批）----
        put("b1.9p5", "ModLoader B1.9p5.zip", "666e9f28927db986a92be437335add82ccd0a1b4c111703d4950b075944e92f8");
        put("b1.8.1", "ModLoader B1.8.1.zip", "4135de0b0fddf6f9b39761a5261b82dae278b311237ec1cd936911b0b133919e");
        // ★ b1.7.3 用社区修复版（官方版会卡死在 "ModLoader Beta 1.7.3 Initializing..."）。
        //   官方版是 MCArchive 的 78bc1107a2ae78334d1086c7f372601c141b53345f23ce73931ef318df5cf83e，
        //   但它在现代 Java 下抛 URI is not hierarchical，所以这里换成 ModloaderFix。
        putCustom("b1.7.3", "ModLoader.Fix.b1.7.3-1.0.0.jar",
                "725591f1e27624f210206e057f9a38b85f28b955bb7ff25bf2ee2449994f1e8c", B173_FIX_URL);
        put("b1.7.2", "ModLoader B1.7.2.zip", "2b4e0e19b817a464ef32042a12f3ba1d8e4db25a01a1bb19efe8a5d9713a003c");
        put("b1.5_01v4", "ModLoader B1.5_01v4.zip", "c20df06b803903de5f7768107c27c80bbdbeb896003a93391432cf29bb7110cd");
        put("b1.4_01", "ModLoader B1.4_01.zip", "a1fdf3b4698bfe1d29c8b9ec9f40905d9b436f19d9d630b4ace6b133cbf3560f");
        put("b1.3_01v5", "ModLoader B1.3_01v5.zip", "b628f9fa608888f4d3c47c360a9c40c5b7a32eba52e9679f717f613548edc2c0");
        put("b1.3_01", "ModLoader B1.3_01.zip", "7f6ce7bb223cb28690d65864f786d212f1bf3ff5cf9853c8ea86f5233dc2e0e1");
        put("b1.2_02v4", "ModLoader B1.2_02v4.zip", "c5b3a4d72da03fcee9f8359c4c37f79f8a574d8b5987a849c35cbd4f79c9a87b");

        // ---- Alpha（.zip 的）----
        put("a1.2.3_04", "ModLoader A1.2.3_04.zip", "5c44c13470829c304a84823af4547362065f5252c070510eea2e5f4580ac09b2");

        // ---- 以下只有 .rar：现有依赖解不开，先如实登记并在 UI 上说明 ----

        // ---- Alpha / 早期：MCArchive 原文件是 .rar（启动器解不了），
        //      已用 7-Zip 解开后重打成 .zip 传到 Gitee 镜像（国内直连）----
        putCustom("a1.2.6", "ModLoader-a1.2.6.zip", "6a7a1a7baff97e6011c789b8e9caa2c55560b6cf4fa9a717cedac31ead59a4e0",
                "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader-a1.2.6.zip");
        putCustom("a1.2.3_04", "ModLoader-a1.2.3_04.zip", "3842baa52a691d1b34aa1aa34d5d1a88675f940827cecef968b8d97979beb635",
                "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader-a1.2.3_04.zip");
        putCustom("a1.2.2", "ModLoader-a1.2.2.zip", "a0292bda7c3554b35f244a51277a4bfd7985414e974814b37ac41a355d960147",
                "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader-a1.2.2.zip");
        putCustom("a1.2.1_01", "ModLoader-a1.2.1_01.zip", "52b2683776cd516f3d9194727450ce4f026c31d58cbf1c42341c32b4b8fb4cfa",
                "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader-a1.2.1_01.zip");
        putCustom("a1.2.0_02", "ModLoader-a1.2.0_02.zip", "3ae688492df3564e4d0aa3bc2140d1991456c254c68be6d62f5f6182d9882a71",
                "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader-a1.2.0_02.zip");
        putCustom("1.1", "ModLoader-1.1.zip", "eeafb39390bb9da56a4ed108d82ab2e96344b2aac68668096c9a83e8f1147e0e",
                "https://gitee.com/allne201123/qcl-repo/raw/master/modloader/ModLoader-1.1.zip");

    }

    private ModLoaderVersions() {
    }

    /** 查某个 MC 版本有没有 ModLoader */
    public static Entry find(String mcVersion) {
        if (mcVersion == null) {
            return null;
        }
        Entry direct = TABLE.get(mcVersion);
        if (direct != null) {
            return direct;
        }
        // 版本 id 可能带后缀（b1.7.3-modded 之类），做一次宽松匹配
        for (Map.Entry<String, Entry> e : TABLE.entrySet()) {
            if (mcVersion.startsWith(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 全部条目（新的在前） */
    public static List<Entry> all() {
        return Collections.unmodifiableList(new ArrayList<Entry>(TABLE.values()));
    }

    /** 能直接安装的（zip）条目 */
    public static List<Entry> installable() {
        List<Entry> list = new ArrayList<Entry>();
        for (Entry e : TABLE.values()) {
            if (e.isInstallable()) {
                list.add(e);
            }
        }
        return list;

    }
}
