package com.qcl.launcher.launcher.download.modloader;

import java.io.File;

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
 *   什么都没装      → ModLoader 的、Babric 的都要标；
 *                      只改 class、不依赖任何加载器的才不标。
 *
 * ★ 标记来源：{@link ModLoaderInstallTask#MARKER_NAME}（版本目录下的 `.modloader`）。
 *   Babric 装完要写 `.babric`（Babric 安装任务做完后补）。
 */
public final class ModLoaderDetector {

    /** Risugami ModLoader */
    public static final String MODLOADER = "modloader";

    /** Babric（b1.7.3 的 Fabric 分支） */
    public static final String BABRIC = "babric";

    /** Babric 的标记文件名（装在版本目录下） */
    public static final String BABRIC_MARKER = ".babric";

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
     * 返回当前加载器：{@link #MODLOADER} / {@link #BABRIC} / null（没装）。
     */
    public static String detect(File versionDir) {
        if (hasModLoader(versionDir)) {
            return MODLOADER;
        }
        if (hasBabric(versionDir)) {
            return BABRIC;
        }
        return null;
    }

    /**
     * ★ 这个模组在「当前加载器」下算不算支持？
     *
     * @param currentLoader {@link #detect(File)} 的结果（null = 没装加载器）
     * @param modLoaders    这个模组依赖的加载器列表（小写，如 "modloader"/"fabric"/"forge"）；
     *                      为 null 或空 = **不依赖任何加载器**（多半是纯改 class 的那种）
     * @return true = 可以正常展示（不标警告）；false = 要标「不支持你当前的版本」
     */
    public static boolean isSupported(String currentLoader, java.util.List<String> modLoaders) {
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
            return modLoaders.contains(BABRIC) || modLoaders.contains("fabric");
        }

        return noDependency;
    }
}
