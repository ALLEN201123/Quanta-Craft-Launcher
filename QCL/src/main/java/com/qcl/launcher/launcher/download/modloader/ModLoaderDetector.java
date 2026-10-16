package com.qcl.launcher.launcher.download.modloader;

import com.qcl.launcher.R;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * ★ 1.2.3：判断「某个版本装了哪个模组加载器」。
 *
 * 用途：模组下载列表要按玩家当前装的加载器，在模组卡片上标
 *      「不支持你当前的版本」。判断规则（用户定的）：
 *
 *   当前是 ModLoader → 依赖 Babric/Fabric/Forge 等的模组要标不支持；
 *                      依赖 ModLoader 的、或**不依赖任何加载器**的，不标。
 *   当前是 Babric    → 只有依赖 Babric 的不标；
 *                      **不依赖任何加载器的也要标**不支持（Babric 不吃原生 mod）。
 *   当前是 Fabric/Forge/NeoForge/Quilt/LiteLoader（高版本那套）→ 依赖同款加载器的、
 *                      以及**依赖未知**的不标（避免 CurseForge 卡片被大面积误标）；
 *                      依赖其它加载器的标。
 *   什么都没装      → ModLoader 的、Babric 的都要标；
 *                      只改 class、不依赖任何加载器的才不标。
 *
 * ★★ 识别来源有两层（1.2.3 修过一次漏判：高版本 Fabric/Forge 装了却被当没装）：
 *   ① 标记文件：`.modloader`（ModLoader）、`.babric`（Babric）—— 自己家装的会写；
 *   ② 版本 json：里面含 `fabric-loader` / `minecraftforge` / `neoforged` /
 *      `quilt-loader` / `liteloader` 这些库名 —— Fabric / Forge / NeoForge /
 *      Quilt 是老功能，不写标记，必须从 json 里认出来。
 */
public final class ModLoaderDetector {

    /** Risugami ModLoader（远古，往 jar 里塞 class 那种） */
    public static final String MODLOADER = "modloader";

    /** Babric（b1.7.3 的 Fabric 分支） */
    public static final String BABRIC = "babric";

    /** Babric 的标记文件名（装在版本目录下） */
    public static final String BABRIC_MARKER = ".babric";

    /** 官方 Fabric（高版本） */
    public static final String FABRIC = "fabric";

    /** Forge */
    public static final String FORGE = "forge";

    /** NeoForge */
    public static final String NEOFORGE = "neoforge";

    /** Quilt */
    public static final String QUILT = "quilt";

    /** LiteLoader */
    public static final String LITELOADER = "liteloader";

    /** 兜底扫目录时不想误认的 json（登记表自身） */
    private static final String REGISTRY_FALLBACK_NAME = "qcl_jarmods.json";

    private ModLoaderDetector() {
    }

    /**
     * 这个版本装了 ModLoader 吗？
     *
     * @param versionDir 版本目录，例如 {@code .minecraft/versions/b1.7.3}
     */
    public static boolean hasModLoader(File versionDir) {
        return versionDir != null
                && new File(versionDir, ModLoaderInstallTask.MARKER_NAME).isFile();
    }

    /**
     * 这个版本装了 Babric 吗？
     */
    public static boolean hasBabric(File versionDir) {
        return versionDir != null && new File(versionDir, BABRIC_MARKER).isFile();
    }

    /**
     * 返回当前加载器：{@link #MODLOADER} / {@link #BABRIC} / {@link #FABRIC} /
     * {@link #FORGE} / {@link #NEOFORGE} / {@link #QUILT} / {@link #LITELOADER}，
     * null = 没装。
     */
    public static String detect(File versionDir) {
        if (hasModLoader(versionDir)) {
            return MODLOADER;
        }
        if (hasBabric(versionDir)) {
            return BABRIC;
        }
        return detectFromJson(versionDir);
    }

    /**
     * ★ 从版本 json 里认加载器（Fabric / Forge / NeoForge / Quilt / LiteLoader
     *   不写标记文件，只能靠 json 里出现的库名 / 主类来判断）。
     */
    private static String detectFromJson(File versionDir) {
        if (versionDir == null || !versionDir.isDirectory()) {
            return null;
        }
        File json = new File(versionDir, versionDir.getName() + ".json");
        if (!json.isFile()) {
            // 兜底：有的版本 json 名和目录名不一致，随便认一个 .json
            File[] fs = versionDir.listFiles();
            if (fs != null) {
                for (File f : fs) {
                    if (f.isFile() && f.getName().endsWith(".json")
                            && !REGISTRY_FALLBACK_NAME.equals(f.getName())) {
                        json = f;
                        break;
                    }
                }
            }
        }
        if (!json.isFile()) {
            return null;
        }
        try {
            String low = new String(Files.readAllBytes(json.toPath()), "UTF-8")
                    .toLowerCase();
            // ★★★★★ 2026-10-16 **修「Forge 被认成 Fabric」**（评测视频 48s/51s/54s：
            //   「安装Forge加载器的整合包会让Forge变成滚木」，实测同一实例在
            //   「模组检测 · OK · Fabric · 14 个模组」与「· Forge · 16 个模组」之间跳变）。
            //
            //   ★ 原实现是**顺序 if-return**，`fabric-loader` 排在 `minecraftforge` **前面**：
            //     <pre>
            //       if (low.contains("fabric-loader"))  return FABRIC;    // ← 先命中就返回
            //       ...
            //       if (low.contains("minecraftforge")) return FORGE;     // ← 永远轮不到
            //     </pre>
            //   Forge 整合包的版本 json 经常**同时**出现 fabric 字样：
            //     · `inheritsFrom` 指向一个曾装过 Fabric 的父版本；
            //     · 整合包自带的 `mods` / `libraries` 里塞了 Fabric 系依赖；
            //     · 打包工具写入了 `fabric-loader` 的残留。
            //   ⇒ 于是 Forge 被判成 Fabric ⇒ 加载器被"覆盖"、模组数量/图标全错。
            //
            //   ★ 修法：改成**加权计分**，证据强的胜出；同分时按
            //     NeoForge > Forge > Quilt > LiteLoader > Fabric 的优先级
            //     （Fabric 最弱，因为它的关键字最容易被别的整合包捎带）。
            //     这样「只有 fabric-loader」仍判 Fabric，而「forge + fabric 混杂」判 Forge。
            int scFabric = countOf(low, "fabric-loader", "fabricloader", "net.fabricmc");
            int scQuilt = countOf(low, "quilt-loader", "quiltmc");
            int scLite = countOf(low, "liteloader", "com.mumfrey.liteloader");
            int scNeo = countOf(low, "neoforged", "neoforge", "neo-form", "net.neoforged");
            int scForge = countOf(low, "minecraftforge", "fmlcore", "fmlloader",
                    "net.minecraftforge", "forgegradle");

            // ★ 关键：Forge / NeoForge 只要**存在**且不弱于 Fabric，就优先判 Forge
            //   （它们的库名在生产 json 里比 Fabric 更专一，Fabric 更容易是被捎带的）。
            if (scNeo > 0 && scNeo >= scFabric) {
                return NEOFORGE;
            }
            if (scForge > 0 && scForge >= scFabric) {
                return FORGE;
            }
            if (scQuilt > 0 && scQuilt > scFabric) {
                return QUILT;
            }
            if (scLite > 0) {
                return LITELOADER;
            }
            if (scFabric > 0) {
                return FABRIC;
            }
            // 兜底：分数全为 0 时（关键字被拼写变体绕过）退回原来的包含判断，
            //       保证绝不比以前更差。
            if (scNeo > 0 || low.contains("neoforge")) {
                return NEOFORGE;
            }
            if (low.contains("fabric-loader")) {
                return FABRIC;
            }
            if (low.contains("quilt-loader")) {
                return QUILT;
            }
            if (low.contains("liteloader")) {
                return LITELOADER;
            }
            if (low.contains("minecraftforge") || low.contains("fmlcore")
                    || low.contains("fmlloader")) {
                return FORGE;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 统计若干关键字在文本里出现的**总次数**（用于加权计分）。 */
    private static int countOf(String haystack, String... needles) {
        int n = 0;
        for (String needle : needles) {
            int from = 0;
            while (true) {
                int idx = haystack.indexOf(needle, from);
                if (idx < 0) {
                    break;
                }
                n++;
                from = idx + needle.length();
            }
        }
        return n;
    }

    /**
     * ★ 1.2.5：这个版本该显示哪个图标（FCL 同款规则 —— 装了哪个加载器就显示哪个）。
     *
     * 供「版本列表 / 主界面下拉 / 版本设置」统一调用，避免各处各写一份导致
     * 同一个版本在列表和设置页显示两个不同图标（1.2.4 就出过这个 bug）。
     *
     * @param versionDir 版本目录
     * @return 图标资源 id；0 = 没装任何加载器（调用方自己用草方块兜底）
     */
    /**
     * ★★★ 1.5.0：**是不是远古版本**（用户要求「远古版本 / 归档版本统一使用原石图标」）。
     *
     * <p>按版本名判断（QCL 的远古版本名就是这几种前缀，与 {@code LegacyVersionArchive} /
     * {@code LegacyChinesePack} 里的口径一致）：
     * <ul>
     *   <li>{@code rd-xxx}                  —— Pre-Classic（RubyDung 时代）</li>
     *   <li>{@code c0.x} / {@code c1.x}     —— Classic</li>
     *   <li>{@code in-xxx}                  —— Indev</li>
     *   <li>{@code inf-xxx}                 —— Infdev</li>
     *   <li>{@code a1.x}                    —— Alpha</li>
     *   <li>{@code b1.x}                    —— Beta</li>
     * </ul>
     * 归档版本（周快照之外的远古归档）名字同样是这些前缀 ⇒ 一并命中。
     *
     * @param versionName 版本名（目录名），可为 null
     * @return true = 远古版本，应统一用原石立方体图标 {@code R.drawable.ic_cobble}
     */
    public static boolean isLegacyVersion(String versionName) {
        if (versionName == null) {
            return false;
        }
        String n = versionName.trim().toLowerCase(java.util.Locale.ROOT);
        if (n.isEmpty()) {
            return false;
        }
        // ★★★ 口径与 VersionSettingUI.isAncientVersion **完全一致** ——
        //   两处判定必须同源，否则同一个版本在列表和设置页会显示两个不同图标。
        //   inf*（Infdev）· in-*（Indev）· a*（Alpha）· b*（Beta）· c0.*（Classic）
        //   · pc-*（Pre-Classic）· rd*（Pre-Classic / RubyDung 时代）· 含 infdev 字样
        return n.startsWith("inf") || n.startsWith("in-") || n.startsWith("a") || n.startsWith("b")
                || n.startsWith("c0.") || n.startsWith("pc-") || n.startsWith("rd")
                || n.contains("infdev");
    }

    public static int iconRes(File versionDir) {
        try {
            String loader = detect(versionDir);
            if (MODLOADER.equals(loader)) {
                return R.drawable.ic_modloader;
            }
            if (BABRIC.equals(loader)) {
                return R.drawable.ic_babric;
            }
            if (FABRIC.equals(loader)) {
                return R.drawable.ic_fabric;
            }
            if (FORGE.equals(loader)) {
                return R.drawable.ic_forge;
            }
            if (NEOFORGE.equals(loader)) {
                return R.drawable.ic_neoforge;
            }
            if (QUILT.equals(loader)) {
                return R.drawable.ic_quilt;
            }
            if (LITELOADER.equals(loader)) {
                return R.drawable.ic_modloader;
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /**
     * ★ 这个模组在「当前加载器」下算不算支持？
     *
     * @param currentLoader {@link #detect(File)} 的结果（null = 没装加载器）
     * @param modLoaders    这个模组依赖的加载器列表（小写，如 "modloader"/"fabric"/"forge"）；
     *                      为 null 或空 = **不依赖任何加载器**（多半是纯改 class 的那种）
     * @return true = 可以正常展示（不标警告）；false = 要标「不支持你当前的版本」
     */
    public static boolean isSupported(String currentLoader, List<String> modLoaders) {
        boolean noDependency = modLoaders == null || modLoaders.isEmpty();

        if (currentLoader == null) {
            // 没装任何加载器：只有「纯改 class、不依赖加载器」的才不标
            return noDependency;
        }

        if (MODLOADER.equals(currentLoader)) {
            if (noDependency) {
                return true;    // 不依赖任何加载器 → 可装
            }
            return modLoaders.contains(MODLOADER);
        }

        if (BABRIC.equals(currentLoader)) {
            // Babric 不吃「不依赖任何加载器」的原生 mod
            if (noDependency) {
                return false;
            }
            return modLoaders.contains(BABRIC) || modLoaders.contains(FABRIC);
        }

        if (FABRIC.equals(currentLoader) || FORGE.equals(currentLoader)
                || NEOFORGE.equals(currentLoader) || QUILT.equals(currentLoader)
                || LITELOADER.equals(currentLoader)) {
            // 高版本加载器：同款 → 不标；其它加载器 → 标。
            // 依赖未知（CurseForge 卡片没有 loader 信息）→ **不标**，避免整页误标。
            if (noDependency) {
                return true;
            }
            return modLoaders.contains(currentLoader);
        }

        return noDependency;
    }
}
