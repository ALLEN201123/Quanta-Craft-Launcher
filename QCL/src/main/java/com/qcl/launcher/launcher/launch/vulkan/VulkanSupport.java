package com.qcl.launcher.launcher.launch.vulkan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ★ 1.5.0 新增：设备 × Minecraft 版本的 Vulkan 支持情况评估（FCL VulkanSupport / profileSupport 的 Java 版）。
 *
 * <p>产出两个东西：
 * <ol>
 *   <li>{@link #supportFor} —— 某个具体 MC 版本的逐项依赖状态（对话框明细用）</li>
 *   <li>{@link #profileSupport()} —— 合并后的版本区间结论（对话框总体结论用）</li>
 * </ol>
 */
public final class VulkanSupport {

    /** 单条依赖在某版本下的状态。 */
    public static final class Item {
        public final VulkanRequirement.Dependency dependency;
        public final VulkanRequirement.Level level;
        public final boolean supported;

        Item(VulkanRequirement.Dependency d, VulkanRequirement.Level l, boolean s) {
            this.dependency = d;
            this.level = l;
            this.supported = s;
        }
    }

    /** 支持情况区间（since 含，until 不含，null=无上界）。 */
    public static final class Profile {
        public final String since;
        public final String until;
        public final boolean supported;

        Profile(String since, String until, boolean supported) {
            this.since = since;
            this.until = until;
            this.supported = supported;
        }

        /** 展示文本：{@code 26.2-26.2} → {@code 26.2}；{@code 26.3+} 无上界。 */
        public String versionRangeText() {
            if (until == null) {
                return since + "+";
            }
            String prev = prevNumberVersion(until);
            return prev != null && prev.equals(since) ? since : since + "-" + prev;
        }

        private static String prevNumberVersion(String v) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)(\\d+)$").matcher(v);
            if (!m.find()) {
                return null;
            }
            int n = Integer.parseInt(m.group(2));
            if (n <= 0) {
                return null;
            }
            return m.group(1) + (n - 1);
        }
    }

    private final VulkanCapabilities capabilities;

    public VulkanSupport(VulkanCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    public VulkanCapabilities capabilities() {
        return capabilities;
    }

    /** 评估某个具体 MC 版本的逐项依赖状态。 */
    public List<Item> supportFor(String mcVersion) {
        List<Item> all = new ArrayList<>();
        for (VulkanRequirement.Dependency d : VulkanRequirement.EXTENSIONS) {
            all.add(statusOf(d, mcVersion));
        }
        for (VulkanRequirement.Dependency d : VulkanRequirement.FEATURES) {
            all.add(statusOf(d, mcVersion));
        }
        return all;
    }

    private Item statusOf(VulkanRequirement.Dependency d, String mcVersion) {
        return new Item(d, d.levelAt(mcVersion), supports(d));
    }

    /** 设备是否支持该扩展/功能（与 MC 版本无关）。 */
    private boolean supports(VulkanRequirement.Dependency d) {
        return d.extension ? capabilities.supportsExtension(d.name) : capabilities.supportsFeature(d.name);
    }

    /** 该版本所需的依赖是否齐全（必需项都不缺）。 */
    public boolean isVersionSupported(String mcVersion) {
        for (Item it : supportFor(mcVersion)) {
            if (it.level == VulkanRequirement.Level.REQUIRED && !it.supported) {
                return false;
            }
        }
        return true;
    }

    /** 该版本依赖但设备缺失的必需项。 */
    public List<Item> missingRequired(String mcVersion) {
        List<Item> out = new ArrayList<>();
        for (Item it : supportFor(mcVersion)) {
            if (it.level == VulkanRequirement.Level.REQUIRED && !it.supported) {
                out.add(it);
            }
        }
        return out;
    }

    /**
     * 合并后的版本区间结论（相邻且结论一致的区间会合并）。
     * Vulkan &lt; 1.2 不满足 26.2 的运行基线 → 所有区间都判不支持。
     */
    public List<Profile> profileSupport() {
        List<String> versions = VulkanRequirement.PROFILE_VERSIONS;
        if (versions.isEmpty()) {
            return new ArrayList<>();
        }
        if (!capabilities.isVersionSupported()) {
            List<Profile> single = new ArrayList<>();
            single.add(new Profile(versions.get(0), null, false));
            return single;
        }
        List<Profile> raw = new ArrayList<>();
        for (int i = 0; i < versions.size(); i++) {
            String since = versions.get(i);
            String until = i + 1 < versions.size() ? versions.get(i + 1) : null;
            raw.add(new Profile(since, until, isVersionSupported(since)));
        }
        List<Profile> merged = new ArrayList<>();
        for (Profile p : raw) {
            Profile last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
            if (last != null && last.supported == p.supported) {
                merged.set(merged.size() - 1, new Profile(last.since, p.until, last.supported));
            } else {
                merged.add(p);
            }
        }
        return merged;
    }

    /** 总体结论文案用的枚举。 */
    public enum Summary {
        ALL_SUPPORTED, PARTIAL, NONE_SUPPORTED
    }

    public Summary summary() {
        List<Profile> profiles = profileSupport();
        boolean anySupported = false;
        boolean allSupported = !profiles.isEmpty();
        for (Profile p : profiles) {
            if (p.supported) {
                anySupported = true;
            } else {
                allSupported = false;
            }
        }
        if (profiles.isEmpty() || !anySupported) {
            return Summary.NONE_SUPPORTED;
        }
        return allSupported ? Summary.ALL_SUPPORTED : Summary.PARTIAL;
    }

    /** 第一个受支持的版本（总体结论里用："自 x 起支持"）。 */
    public String firstSupportedSince() {
        for (Profile p : profileSupport()) {
            if (p.supported) {
                return p.since;
            }
        }
        return null;
    }

    /** 依赖项展示名：缺失时加「缺」前缀由界面层决定，这里只给名字。 */
    public static String displayName(Item item) {
        return item.dependency.name;
    }

    /** 版本区间展示文本列表，形如 {@code ["26.2 支持", "26.3+ 不支持"]}，供对话框直接贴。 */
    public List<String> profileTexts(boolean zh) {
        List<String> out = new ArrayList<>();
        for (Profile p : profileSupport()) {
            String tag = p.supported ? (zh ? "支持" : "Supported") : (zh ? "不支持" : "Not supported");
            out.add(p.versionRangeText() + "  " + tag);
        }
        return out;
    }

    /** 调试用摘要。 */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "VulkanSupport{api=%s, level=%d, profiles=%s}",
                capabilities.versionString(), capabilities.hardwareLevel, profileTexts(false));
    }
}
