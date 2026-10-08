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

    /**
     * ★ 1.5.0：**游戏进程实际用的 Vulkan 来源**（native，来自 egl_bridge.c 的 qclGetVulkanSource）。
     * 返回 "turnip"（走自带 Turnip 软 Vulkan）/ "system"（系统 libvulkan.so）/ "none"（没拿到）。
     *
     * <p>为什么需要它：本类只读**系统属性**，高通等机型属性里有 Vulkan 就会判"支持"并放行；
     * 但若 Turnip 没接管、MC 实际回落 OpenGL，26.3+ 依旧崩 —— 两者会**脱节**。
     * 把真实来源暴露出来，才能在崩溃前说清到底是哪一环出了问题。
     *
     * <p>★ 注意：只能在**游戏进程已加载 libpojavexec 并跑过 load_vulkan() 之后**才有意义；
     * 启动前调用会得到 "none"（属正常，不代表出错）。因此所有调用点都必须 try-catch。
     */
    public static native String nativeGetVulkanSource();

    /** 安全包装：拿不到就返回 "unknown"，绝不抛给调用方。 */
    public static String getVulkanSourceSafe() {
        try {
            String s = nativeGetVulkanSource();
            return (s == null || s.isEmpty()) ? "unknown" : s;
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /**
     * ★ 1.5.0：**只返回系统属性里明确报的** Vulkan 版本号（不走"看到 libvulkan.so 就算支持"的兜底）。
     *
     * <p>用于「要不要**自动**钉 Vulkan」这个决策：属性读不到 = 不确定 ⇒ **不自动钉**，
     * 改成在启动弹窗里让玩家知情后自己选。宁可让人多按一次，也不要替他猜错。
     *
     * @return 版本号（0 表示系统属性没报，即"不确定"）
     */
    public static int systemReportedVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return featureVersion(pm, FEATURE_VULKAN_VERSION, 0);
        } catch (Throwable t) {
            return 0;
        }
    }

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
        // ★★★★★ 1.5.0 修复（模拟器实测发现的致命缺陷）：
        //   **只读系统属性会误判**。实测 MuMu：`/system/lib64/libvulkan.so` 明明存在（177232B）、
        //   游戏里 `OSMDroid: loaded vulkan, ptr=0x...` 也确实拿到了句柄，
        //   但 `pm list features` 里**没有** Vulkan 特性、`android.hardware.vulkan.version` 也是空
        //   ⇒ 原逻辑判 apiVersionRaw=0「不支持」⇒ 不钉后端 ⇒ MC 自己选 OpenGL ⇒ 26.3 除零崩。
        //   ⇒ 补一条**兜底**：属性读不到时，去看系统里到底有没有 Vulkan 加载器。
        //   （很多 ROM / 模拟器 / 魔改机都不填这个属性，但 Vulkan 是能用的。）
        if (version == 0 && hasSystemVulkanLoader()) {
            // 保守给一个「够 26.2/26.3 用的最低能力」：
            // Vulkan 1.2 + Level 2 是 MC 26.2+ 的运行基线；有加载器说明至少能起实例。
            version = 0x0102;   // VK_API_VERSION_1_2
            level = 2;
            compute = true;
        }
        return new VulkanCapabilities(version, level, compute, deqp,
                deriveExtensions(version, level), deriveFeatures(version, level, compute));
    }

    /**
     * 系统里是否**真的有** Vulkan 加载器（{@code libvulkan.so}）。
     * 覆盖 {@code /system/lib64}、{@code /system/lib}、{@code /vendor/lib64} 等常见位置，
     * 以及 {@code LD_LIBRARY_PATH} 里可能存在的副本。
     */
    private static boolean hasSystemVulkanLoader() {
        String[] dirs = {
                "/system/lib64", "/system/lib", "/vendor/lib64", "/vendor/lib",
                "/apex/com.android.runtime/lib64", "/system/lib/arm64"
        };
        String[] names = {"libvulkan.so"};
        for (String d : dirs) {
            for (String n : names) {
                try {
                    java.io.File f = new java.io.File(d, n);
                    if (f.isFile() && f.length() > 0) {
                        return true;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        // 最后一招：交给 native 试一次 dlopen（libpojavexec 里的 dlopen）
        try {
            if (nativeVulkanLoaderLoads()) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** native 侧：真正 dlopen 一次 libvulkan.so，返回是否成功（比看文件更准）。 */
    private static native boolean nativeVulkanLoaderLoads();

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
