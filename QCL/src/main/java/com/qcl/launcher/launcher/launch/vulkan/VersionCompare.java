package com.qcl.launcher.launcher.launch.vulkan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ★ 1.5.0 新增：Minecraft 版本号比较（升序为正），用于 Vulkan 依赖表的版本区间判定。
 *
 * <p>为什么不能直接用 {@code RendererCompat.parseVer}：那个只认「数字.数字.数字」，
 * 遇到 {@code 26.3}（只有两段）会算成 {@code 26*10000 + 3*100 = 260300}，
 * 而 {@code 1.21.11} 算成 {@code 12111} —— 混在一起比大小是错的。
 * 这里做的是**逐段数值比较**（照 FCL GameVersionNumber.compare 的行为），
 * 且对远古版本（b1.7.3 / a1.2.6 / in-20100223 等）不参与 26.x 的判定（由调用方先判现代版本）。
 *
 * <p>比较规则：
 * <ol>
 *   <li>先把版本串切成数字段（忽略非数字分隔符）：{@code 26.3 → [26,3]}、{@code 1.21.11 → [1,21,11]}</li>
 *   <li>逐段比较，缺段按 0 计：{@code 26.3 vs 26.3.0} 相等</li>
 *   <li>数字段完全无法解析时退化为字符串比较（保证不抛异常）</li>
 * </ol>
 */
final class VersionCompare {

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private VersionCompare() {
    }

    /**
 * 判断这个版本串**是否以现代版本号开头**（而不是远古版本的 inf-/b1./a1./c0./rd- 前缀）。
 *
 * <p>★ 2026-10-09 修复（用户实测「点 inf-20100223 就弹 Vulkan 检测」）：
 * 旧实现只用 {@code \d+} 抽数字，于是
 * {@code inf-20100223 → [20100223]}，与 {@code 26.2 → [26,2]} 逐段比较时
 * 第一段 {@code 20100223 > 26} ⇒ **远古版本被误判成「需要 Vulkan」**。
 *
 * <p>★ 但不能简单要求"整串是数字点"—— 那样会把 {@code 26.2-rc1}、{@code 1.21.11-pre1}
 * 这类**预览版**（确实需要 Vulkan）一起误杀。
 * ⇒ 规则分两类：
 * <ul>
 *   <li><b>远古前缀</b>（{@code inf-} {@code b1.} {@code a1.} {@code c0.} {@code rd-} {@code pre-} 等）：
 *       整个版本号**不是**现代版本 → 不可比；</li>
 *   <li><b>其它</b>（纯数字点、或数字开头带 -rc/-pre 后缀）：开头必须是数字 → 可比。</li>
 * </ul>
 */
private static boolean startsWithModernNumber(String v) {
    if (v == null || v.isEmpty()) {
        return false;
    }
    // 远古版本前缀：这些开头一律判为不可比
    String lower = v.toLowerCase(java.util.Locale.ROOT);
    for (String p : LEGACY_PREFIXES) {
        if (lower.startsWith(p)) {
            return false;
        }
    }
    // 其余要求：开头必须是数字（这样 26.2-rc1 仍算现代版本）
    char c0 = v.charAt(0);
    return c0 >= '0' && c0 <= '9';
}

/** 远古版本号前缀（小写比较）。★ 这些版本永远不会需要 Vulkan。 */
private static final String[] LEGACY_PREFIXES = {
        "inf-", "in-", "b", "a", "c", "rd-", "pre-"
};

    /**
     * 这个版本串**能不能参与与 26.x 的数值比较**。
     *
     * <p>★ 供 {@code VulkanRequirement} 在做「≥26.2 / ≥26.3」判定前先拦一道：
     * 远古版本（inf-/b1./a1./c0./rd-）返回 false ⇒ **绝不弹 Vulkan 检测**。
     * 预览版（26.2-rc1 / 1.21.11-pre1）返回 true ⇒ 判定照常，不会被误杀。
     */
    static boolean isComparable(String v) {
        return startsWithModernNumber(v);
    }

    /** a &lt; b 返回负数；a == b 返回 0；a &gt; b 返回正数。 */
    static int compare(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        int[] pa = parse(a);
        int[] pb = parse(b);
        if (pa == null || pb == null) {
            return a.compareTo(b);
        }
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int x = i < pa.length ? pa[i] : 0;
            int y = i < pb.length ? pb[i] : 0;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    private static int[] parse(String v) {
        // ★ 2026-10-09：远古版本（inf-/b1./a1./rd- 等含**非数字前缀**的）判为不可比（返回 null）。
        //   否则 \d+ 会把 inf-20100223 抽成 [20100223]，第一段就大于 26 ⇒ 被当成超新版本。
        //   ★ 但要**保留** 26.2-rc1 / 26.3-pre2 / 1.21.11-pre1 这类"数字开头 + 后缀"的版本：
        //     它们确实是现代版本、确实可能需要 Vulkan。判据改成：
        //       开头必须是数字（现代版本号），且不能是 inf-/b1./a1./c0./rd- 这类远古前缀。
        if (!startsWithModernNumber(v)) {
            return null;
        }
        Matcher m = NUMBER.matcher(v);
        java.util.List<Integer> parts = new java.util.ArrayList<>();
        while (m.find()) {
            try {
                parts.add(Integer.parseInt(m.group()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        int[] arr = new int[parts.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = parts.get(i);
        }
        return arr;
    }
}
