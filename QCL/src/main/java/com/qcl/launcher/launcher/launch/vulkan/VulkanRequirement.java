package com.qcl.launcher.launcher.launch.vulkan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ★ 1.5.0 新增：Minecraft 26.2+ 的 Vulkan 渲染后端依赖表（照 FCL 的 VulkanRequirements 移植）。
 *
 * <p>背景（用户 2026-10-08）：官方 26.3 对着色器要求更严格，部分设备需要 Vulkan 支持。
 * FCL 自 26.2 起给游戏接了 Vulkan 后端，并为此写了整套设备能力检测 + 引导。
 * QCL 是纯 Java 工程，本类只保留「版本 → 依赖」的判定表（不引 Kotlin / 不引 native）。
 *
 * <p>依赖表口径（与 FCL VulkanDependency.kt 一致，改一处必须改两处）：
 * <pre>
 *   扩展 EXTENSIONS：
 *     VK_KHR_dynamic_rendering           26.2 ~ 26.3（26.3 起不再必需）
 *     VK_KHR_push_descriptor             26.2 ~ 26.3（26.3 起不再必需）
 *     VK_KHR_synchronization2            26.2+
 *     VK_EXT_vertex_attribute_divisor    26.2+
 *     VK_KHR_swapchain                   26.2+
 *   功能 FEATURES：
 *     multiDrawIndirect                  26.2+
 *     fillModeNonSolid                   26.2 必需；26.3 起降为可选
 *     drawIndirectFirstInstance          26.2 不用；26.3 起必需 ★ 26.3 变严的关键项
 *     samplerAnisotropy                  26.2+
 *     shaderDrawParameters               26.2+
 *     timelineSemaphore                  26.2+
 *     hostQueryReset                     26.2+
 *     synchronization2                   26.2+
 *     dynamicRendering                   26.2 ~ 26.3
 *     vertexAttributeInstanceRateDivisor 26.2+
 * </pre>
 */
public final class VulkanRequirement {

    private VulkanRequirement() {
    }

    /** 首个提供 Vulkan 后端的 Minecraft 版本（与 FCL VulkanRequirements.MIN_MC_VERSION 一致）。 */
    public static final String MIN_MC_VERSION = "26.2";

    /** 依赖表版本：表变了就 +1，用于让旧的检测缓存失效（对齐 FCL VULKAN_REQUIREMENTS_VERSION）。 */
    public static final int REQUIREMENTS_VERSION = 1;

    /** 依赖级别。 */
    public enum Level {
        /** 必需：缺失则无法以 Vulkan 启动 */
        REQUIRED,
        /** 可选：缺失仍可启动 */
        OPTIONAL,
        /** 不使用 */
        UNUSED
    }

    /**
     * 单条依赖：一个 Vulkan 扩展或功能，以及它在哪些版本区间是必需/可选/不用。
     * 区间用「起始版本（含）」+「排他上界（不含，null=无上界）」表示，便于切分版本段。
     */
    public static final class Dependency {
        public final String name;
        public final boolean extension;
        /** 需要的版本区间列表 [since, until)，until 为 null 表示无上界 */
        private final List<String[]> requiredSpans = new ArrayList<>();
        private final List<String[]> optionalSpans = new ArrayList<>();
        private final List<String[]> unusedSpans = new ArrayList<>();

        Dependency(String name, boolean extension) {
            this.name = name;
            this.extension = extension;
        }

        Dependency required(String since, String untilExclusive) {
            requiredSpans.add(new String[]{since, untilExclusive});
            return this;
        }

        Dependency optional(String since, String untilExclusive) {
            optionalSpans.add(new String[]{since, untilExclusive});
            return this;
        }

        Dependency unused(String since, String untilExclusive) {
            unusedSpans.add(new String[]{since, untilExclusive});
            return this;
        }

        /** 该依赖在指定 MC 版本上的级别。 */
        public Level levelAt(String mcVersion) {
            String v = normalizeMcVersion(mcVersion);
            if (inSpans(requiredSpans, v)) return Level.REQUIRED;
            if (inSpans(optionalSpans, v)) return Level.OPTIONAL;
            return Level.UNUSED;
        }

        private static boolean inSpans(List<String[]> spans, String v) {
            for (String[] span : spans) {
                if (VersionCompare.compare(v, span[0]) >= 0
                        && (span[1] == null || VersionCompare.compare(v, span[1]) < 0)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** 各版本依赖的 Vulkan 扩展。 */
    public static final List<Dependency> EXTENSIONS;

    /** 各版本依赖的 Vulkan 功能。 */
    public static final List<Dependency> FEATURES;

    /** 依赖需求发生变化的所有版本分界点（升序去重），用于切分版本段。 */
    public static final List<String> PROFILE_VERSIONS;

    static {
        List<Dependency> ext = new ArrayList<>();
        ext.add(new Dependency("VK_KHR_dynamic_rendering", true).required(MIN_MC_VERSION, "26.3"));
        ext.add(new Dependency("VK_KHR_push_descriptor", true).required(MIN_MC_VERSION, "26.3"));
        ext.add(new Dependency("VK_KHR_synchronization2", true).required(MIN_MC_VERSION, null));
        ext.add(new Dependency("VK_EXT_vertex_attribute_divisor", true).required(MIN_MC_VERSION, null));
        ext.add(new Dependency("VK_KHR_swapchain", true).required(MIN_MC_VERSION, null));
        EXTENSIONS = Collections.unmodifiableList(ext);

        List<Dependency> feat = new ArrayList<>();
        feat.add(new Dependency("multiDrawIndirect", false).required(MIN_MC_VERSION, null));
        feat.add(new Dependency("fillModeNonSolid", false)
                .required(MIN_MC_VERSION, "26.3").optional("26.3", null));
        feat.add(new Dependency("drawIndirectFirstInstance", false)
                .unused(MIN_MC_VERSION, "26.3").required("26.3", null));
        feat.add(new Dependency("samplerAnisotropy", false).required(MIN_MC_VERSION, null));
        feat.add(new Dependency("shaderDrawParameters", false).required(MIN_MC_VERSION, null));
        feat.add(new Dependency("timelineSemaphore", false).required(MIN_MC_VERSION, null));
        feat.add(new Dependency("hostQueryReset", false).required(MIN_MC_VERSION, null));
        feat.add(new Dependency("synchronization2", false).required(MIN_MC_VERSION, null));
        feat.add(new Dependency("dynamicRendering", false).required(MIN_MC_VERSION, "26.3"));
        feat.add(new Dependency("vertexAttributeInstanceRateDivisor", false).required(MIN_MC_VERSION, null));
        FEATURES = Collections.unmodifiableList(feat);

        List<String> versions = new ArrayList<>();
        for (Dependency d : concat(EXTENSIONS, FEATURES)) {
            collectSpans(d.requiredSpans, versions);
            collectSpans(d.optionalSpans, versions);
            collectSpans(d.unusedSpans, versions);
        }
        Collections.sort(versions, VersionCompare::compare);
        List<String> distinct = new ArrayList<>();
        for (String v : versions) {
            if (distinct.isEmpty() || VersionCompare.compare(distinct.get(distinct.size() - 1), v) != 0) {
                distinct.add(v);
            }
        }
        PROFILE_VERSIONS = Collections.unmodifiableList(distinct);
    }

    private static List<Dependency> concat(List<Dependency> a, List<Dependency> b) {
        List<Dependency> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }

    private static void collectSpans(List<String[]> spans, List<String> out) {
        for (String[] s : spans) {
            out.add(s[0]);
            if (s[1] != null) {
                out.add(s[1]);
            }
        }
    }

    /** 该版本是否带 Vulkan 渲染后端（26.2 起；快照/pre/rc 归一化后判断）。 */
    public static boolean hasVulkanBackend(String gameVersion) {
        if (gameVersion == null) {
            return false;
        }
        String v = normalizeMcVersion(gameVersion);
        return VersionCompare.compare(v, MIN_MC_VERSION) >= 0;
    }

    /**
     * 把快照 / pre / rc 版本归一化为对应的目标正式版本：
     * {@code 26.3-snapshot-3 → 26.3}、{@code 1.21.11-pre2 → 1.21.11}、{@code 1.21.11-rc1 → 1.21.11}。
     *
     * <p>★ 编号前的连字符可有可无：官方既写 {@code -snapshot-3} / {@code -pre-2}，
     * 也写 {@code -pre2} / {@code -rc1}，两边都要吃掉。
     */
    public static String normalizeMcVersion(String mcVersion) {
        if (mcVersion == null) {
            return "";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(.+?)-(?:snapshot|pre|rc)-?\\d+$")
                .matcher(mcVersion);
        return m.find() ? m.group(1) : mcVersion;
    }
}
