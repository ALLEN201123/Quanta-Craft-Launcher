package com.qcl.launcher.launcher.launch.vulkan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * ★ 1.5.0 新增：设备 Vulkan 能力检测（**纯 Java**，不引入 native / Kotlin）。
 *
 * <p>为什么是纯 Java：QCL 是纯 Java 工程（无 Kotlin 插件、无 externalNativeBuild / CMake）。
 * FCL 那套检测依赖 Kotlin + kotlinx.serialization + DataStore + 22KB native C
 * + liblinkerhook/androidnsbypass（Turnip 场景），整套搬进来会引入一大串构建依赖与风险。
 * 用户 2026-10-08 明确选择：**纯 Java 移植**（不支持 Turnip 驱动检测，其余照做）。
 *
 * <p>能拿到什么（Android 官方提供的 4 个 Vulkan 系统特性，API 24+）：
 * <ul>
 *   <li>{@code android.hardware.vulkan.version}   → {@code featureVersion}，
 *       值形如 {@code 0x00401000} = Vulkan 1.1.0（高位 22-28 主版本，12-21 次版本，0-11 补丁）</li>
 *   <li>{@code android.hardware.vulkan.level}     → {@code featureVersion}，硬件等级 0~3</li>
 *   <li>{@code android.hardware.vulkan.compute}   → 是否支持 Vulkan 计算（{@code reqGlEsVersion} 无关）</li>
 *   <li>{@code android.hardware.vulkan.deqp.level}→ DEQP 测试等级（形如 {@code 0x00400000}）</li>
 * </ul>
 *
 * <p>拿不到什么：真实枚举的设备扩展/特性列表。本类按「Vulkan 版本 + 硬件等级」推导
 * （对照 Android 官方 Vulkan 支持表），见 {@link #deriveExtensions} / {@link #deriveFeatures}。
 */
public final class VulkanChecker {

    private VulkanChecker() {
    }

    /** 系统特性名（PackageManager 里以常量形式提供，这里用字面量以便低版本编译也能跑）。 */
    private static final String FEATURE_VULKAN_VERSION = "android.hardware.vulkan.version";
    private static final String FEATURE_VULKAN_LEVEL = "android.hardware.vulkan.level";
    private static final String FEATURE_VULKAN_COMPUTE = "android.hardware.vulkan.compute";
    private static final String FEATURE_VULKAN_DEQP = "android.hardware.vulkan.deqp.level";

    /** 采集设备 Vulkan 能力；设备不支持 Vulkan 时返回一个 apiVersionRaw=0 的对象（不会返回 null）。 */
    public static VulkanCapabilities check(Context context) {
        int version = 0;
        int level = -1;
        boolean compute = false;
        int deqp = 0;
        try {
            PackageManager pm = context.getPackageManager();
            version = featureVersion(pm, FEATURE_VULKAN_VERSION, 0);
            level = featureVersion(pm, FEATURE_VULKAN_LEVEL, -1);
            compute = pm.hasSystemFeature(FEATURE_VULKAN_COMPUTE);
            deqp = featureVersion(pm, FEATURE_VULKAN_DEQP, 0);
        } catch (Throwable t) {
            // 任何反射/系统接口异常都不该让启动器崩：退化为「不支持 Vulkan」
        }
        return new VulkanCapabilities(version, level, compute, deqp,
                deriveExtensions(version, level), deriveFeatures(version, level, compute));
    }

    /**
     * 读取系统特性的 version 值。
     * Android 的 {@code getSystemAvailableFeatures()} 里，带版本号的特性用 {@code FeatureInfo.version}，
     * 但该字段是 {@code @hide} 的 → 用反射读；读不到再退回按特性名匹配。
     */
    private static int featureVersion(PackageManager pm, String name, int def) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.content.pm.FeatureInfo[] features = pm.getSystemAvailableFeatures();
                if (features != null) {
                    for (android.content.pm.FeatureInfo fi : features) {
                        if (fi != null && name.equals(fi.name)) {
                            // FeatureInfo.version 是 @hide 字段 → 反射取
                            try {
                                java.lang.reflect.Field f = android.content.pm.FeatureInfo.class
                                        .getField("version");
                                Object v = f.get(fi);
                                if (v instanceof Integer) {
                                    return (Integer) v;
                                }
                            } catch (Throwable ignored) {
                            }
                            // 反射失败：至少证明"该特性存在"，返回一个非 0 的保守值
                            return def == -1 ? 1 : 1;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return def;
    }

    /**
     * 按 Vulkan 版本 + 硬件等级推导可用扩展。
     *
     * <p>依据 Android 官方「Vulkan 版本 / 硬件等级」能力表（CDD）：
     * <ul>
     *   <li>Vulkan 1.0（任何等级）：核心无 swapchain（需 VK_KHR_swapchain 扩展，几乎所有设备都提供）</li>
     *   <li>Vulkan 1.1：核心并入 {@code VK_KHR_synchronization2} 的前身、
     *       {@code VK_KHR_shader_draw_parameters}、{@code VK_EXT_vertex_attribute_divisor} 仍为扩展</li>
     *   <li>Vulkan 1.2：核心并入 {@code VK_KHR_draw_indirect_count}、
     *       {@code VK_EXT_vertex_attribute_divisor} → {@code VK_KHR_vertex_attribute_divisor}</li>
     *   <li>Vulkan 1.3：核心并入 {@code VK_KHR_dynamic_rendering}、{@code VK_KHR_synchronization2}</li>
     * </ul>
     * 这里按「版本越高、可用集合越大」的单调关系推导，宁可宽松（避免误报"不支持"）。
     */
    private static List<String> deriveExtensions(int version, int level) {
        List<String> out = new ArrayList<>();
        if (version == 0) {
            return out;
        }
        int major = (version >> 22) & 0x7F;
        int minor = (version >> 12) & 0x3FF;
        boolean atLeast11 = major > 1 || (major == 1 && minor >= 1);
        boolean atLeast12 = major > 1 || (major == 1 && minor >= 2);
        boolean atLeast13 = major > 1 || (major == 1 && minor >= 3);

        // VK_KHR_swapchain：所有支持 Vulkan 的设备都必须提供（CDD 要求）
        out.add("VK_KHR_swapchain");
        // VK_KHR_synchronization2：Vulkan 1.3 核心化；1.1/1.2 上是广泛可用的扩展
        if (atLeast11) {
            out.add("VK_KHR_synchronization2");
        }
        // VK_EXT_vertex_attribute_divisor：Vulkan 1.1+ 广泛提供（26.2+ 必需）
        if (atLeast11) {
            out.add("VK_EXT_vertex_attribute_divisor");
            out.add("VK_KHR_vertex_attribute_divisor");
        }
        // VK_KHR_dynamic_rendering：Vulkan 1.2 起设备普遍支持（26.2~26.2 必需，26.3 起不要求）
        if (atLeast12) {
            out.add("VK_KHR_dynamic_rendering");
        }
        // VK_KHR_push_descriptor：Vulkan 1.1+ 普遍提供（26.2 必需，26.3 起不要求）
        if (atLeast11) {
            out.add("VK_KHR_push_descriptor");
        }
        return out;
    }

    /**
     * 按 Vulkan 版本 + 硬件等级推导可用功能。
     *
     * <p>关键判据（决定能不能跑 26.3）：
     * <ul>
     *   <li>{@code drawIndirectFirstInstance}：26.3 起必需。Vulkan 1.1（等级 0/1）**不保证**提供
     *       → 这类设备判定不支持 26.3</li>
     *   <li>{@code fillModeNonSolid}：26.2 必需。Vulkan 1.1+ 通常提供；等级 ≥1 视为可用</li>
     *   <li>{@code shaderDrawParameters}：Vulkan 1.1 核心化 → 1.1+ 可用</li>
     *   <li>{@code timelineSemaphore}：Vulkan 1.2 核心化 → 1.2+ 可用</li>
     *   <li>{@code hostQueryReset}：Vulkan 1.2 核心化 → 1.2+ 可用</li>
     *   <li>{@code samplerAnisotropy}：基础功能，Vulkan 1.0+ 多数设备提供（等级 ≥0）</li>
     * </ul>
     */
    private static List<String> deriveFeatures(int version, int level, boolean compute) {
        List<String> out = new ArrayList<>();
        if (version == 0) {
            return out;
        }
        int major = (version >> 22) & 0x7F;
        int minor = (version >> 12) & 0x3FF;
        boolean atLeast11 = major > 1 || (major == 1 && minor >= 1);
        boolean atLeast12 = major > 1 || (major == 1 && minor >= 2);

        // 基础可用项（Vulkan 1.0+）
        out.add("multiDrawIndirect");
        out.add("samplerAnisotropy");
        if (level >= 2) {
            // 硬件等级 2+ 才保证这些基础功能的完整子集
            out.add("fillModeNonSolid");
        } else if (level >= 0) {
            // 等级 0/1：多数驱动仍提供 fillModeNonSolid（26.2 必需）
            out.add("fillModeNonSolid");
        }
        if (atLeast11) {
            out.add("shaderDrawParameters");
        }
        if (atLeast12) {
            out.add("timelineSemaphore");
            out.add("hostQueryReset");
            out.add("synchronization2");
            out.add("dynamicRendering");
            out.add("vertexAttributeInstanceRateDivisor");
            // ★ drawIndirectFirstInstance：26.3 起必需。Vulkan 1.2+ 才视为可靠可用。
            out.add("drawIndirectFirstInstance");
        }
        return out;
    }
}
