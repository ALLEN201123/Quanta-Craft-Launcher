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
        // 只接受以数字开头的版本串（现代版本号），远古版本号交给上层先拦掉
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
