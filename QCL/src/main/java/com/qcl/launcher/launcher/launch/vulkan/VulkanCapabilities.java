package com.qcl.launcher.launcher.launch.vulkan;

import java.util.Collections;
import java.util.List;

/**
 * ★ 1.5.0 新增：设备 Vulkan 能力（纯 Java 采集，见 {@link VulkanChecker}）。
 *
 * <p>注意：Android SDK **不提供** Vulkan 的 Java 绑定，纯 Java 无法枚举设备扩展/特性
 * （那需要 dlopen libvulkan.so + vkEnumerateDeviceExtensionProperties）。
 * 因此本类里的 {@code extensions} / {@code features} 是**按设备报告的 Vulkan 版本与硬件等级
 * 推导出的能力集合**（对照 Android 官方 Vulkan 版本/等级要求表），而不是真实枚举结果。
 *
 * <p>这份推导对「能不能跑 26.2/26.3」这个判断是足够的：
 * <ul>
 *   <li>Vulkan 1.1（等级 0/1）：缺 {@code drawIndirectFirstInstance}、{@code shaderDrawParameters}
 *       等 26.3 必需项 → 判定不支持 26.3</li>
 *   <li>Vulkan 1.2+（等级 2/3）：26.2/26.3 的必需项基本齐全 → 判定支持</li>
 * </ul>
 */
public final class VulkanCapabilities {

    /** apiVersion 的整数形式（如 0x00401000 = 1.1.0），设备不支持 Vulkan 时为 0。 */
    public final int apiVersionRaw;

    public final int apiVersionMajor;
    public final int apiVersionMinor;
    public final int apiVersionPatch;

    /** 设备报告的 Vulkan 硬件等级（0~3），未知为 -1。 */
    public final int hardwareLevel;

    /** 是否有 Vulkan 计算能力（android.hardware.vulkan.compute）。 */
    public final boolean hasCompute;

    /** DEQP 测试等级（android.hardware.vulkan.deqp.level），未知为 0。 */
    public final int deqpLevel;

    /** 推导出的可用扩展集合。 */
    public final List<String> extensions;

    /** 推导出的可用功能集合。 */
    public final List<String> features;

    public VulkanCapabilities(int apiVersionRaw, int hardwareLevel, boolean hasCompute, int deqpLevel,
                              List<String> extensions, List<String> features) {
        this.apiVersionRaw = apiVersionRaw;
        this.apiVersionMajor = (apiVersionRaw >> 22) & 0x7F;
        this.apiVersionMinor = (apiVersionRaw >> 12) & 0x3FF;
        this.apiVersionPatch = apiVersionRaw & 0xFFF;
        this.hardwareLevel = hardwareLevel;
        this.hasCompute = hasCompute;
        this.deqpLevel = deqpLevel;
        this.extensions = Collections.unmodifiableList(extensions);
        this.features = Collections.unmodifiableList(features);
    }

    /** Vulkan 版本字符串，如 "1.1.0"；不支持时为 "无"。 */
    public String versionString() {
        if (!isSupported()) {
            return "无";
        }
        return apiVersionMajor + "." + apiVersionMinor + "." + apiVersionPatch;
    }

    /** 设备是否支持 Vulkan（至少 1.0）。 */
    public boolean isSupported() {
        return apiVersionRaw != 0;
    }

    /** Vulkan 版本是否 ≥ 1.2（26.2 的运行基线，与 FCL VulkanCapabilities.isVersionSupported 一致）。 */
    public boolean isVersionSupported() {
        return apiVersionMajor > 1 || (apiVersionMajor == 1 && apiVersionMinor >= 2);
    }

    /** 设备是否支持该扩展。 */
    public boolean supportsExtension(String name) {
        return extensions.contains(name);
    }

    /** 设备是否支持该功能。 */
    public boolean supportsFeature(String name) {
        return features.contains(name);
    }
}
