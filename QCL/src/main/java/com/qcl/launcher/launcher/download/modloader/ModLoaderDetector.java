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
            // ★ 顺序有讲究：Babric 的 json 里也有 fabric-loader，但 .babric 标记在上面已经拦掉了
            if (low.contains("fabric-loader")) {
                return FABRIC;
            }
            if (low.contains("quilt-loader")) {
                return QUILT;
            }
            if (low.contains("liteloader")) {
                return LITELOADER;
            }
            // NeoForge 要放在 Forge 前面认：它的库名里同样有 "forge" 字样
            if (low.contains("neoforged") || low.contains("neoforge")
                    || low.contains("neo-form")) {
                return NEOFORGE;
            }
            if (low.contains("minecraftforge") || low.contains("fmlcore")
                    || low.contains("fmlloader")) {
                return FORGE;
            }
        } catch (Exception ignored) {
        }
        return null;
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
