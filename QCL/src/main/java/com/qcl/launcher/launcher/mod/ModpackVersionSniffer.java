package com.qcl.launcher.launcher.mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ★★★★★ 2026-10-11 新增：**从整合包自身嗅探 Minecraft 版本号**。
 *
 * <p>用户实测（原话）：
 * 「我下载整合包，他就要求我下一个原版游戏；而且我下一个 26.3 的整合包，
 *   他都根本检测不到。你能不能改一下判断逻辑？
 *   如果整合包完全脱离了版本名称，你这又该如何检测版本？」
 *
 * <p>背景：FCL 之所以不需要嗅探，是因为它只吃"清单里必然带 {@code minecraft.version}"的包 ——
 * 读不到就直接抛 {@code CurseForge Manifest.gameVersion cannot be blank.}（拒绝安装）。
 * QCL 的策略是"尽力装"，所以清单缺版本时不能摆烂去下原版，
 * 而应该**从整合包自身把版本挖出来**。
 *
 * <p>为什么单独抽一个类：整合包安装任务分属**两棵继承树**
 * （{@code BaseModpackInstallTask} 与 {@code MultiMCModpackInstallTask extends AsyncTask}），
 * 放进基类的话 MultiMC 那边用不了。抽成静态工具类两边都能调。
 */
public final class ModpackVersionSniffer {

    private ModpackVersionSniffer() {
    }

    /**
     * 从一批文本里嗅探最可能的 MC 版本。
     *
     * @param texts 文件名 / 路径 / 目录名 / 包名…（可为 null）
     * @return "出现次数最多"的版本号；同票取版本号更大的；嗅探不到返回 null
     */
    public static String sniff(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return null;
        }
        try {
            HashMap<String, Integer> counter = new HashMap<String, Integer>();
            for (String t : texts) {
                collect(t, counter);
            }
            return pickMostLikely(counter);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 便捷重载：单个文本。 */
    public static String sniff(String text) {
        List<String> one = new ArrayList<String>(1);
        one.add(text);
        return sniff(one);
    }

    /**
     * 从一段文本里抽出所有形如 MC 版本号的片段并计数。
     *
     * <p>识别形态：{@code 1.21.4} / {@code 1.20} / {@code b1.7.3} / {@code 26.3} / {@code 26.4-snapshot-2}。
     *
     * <p>★ 误判防护（很关键）：只接受
     * <ul>
     *   <li>MC 传统版本：{@code 1.x[.y]}；或</li>
     *   <li>现代大版本：{@code major >= 20}（如 26.3）。</li>
     * </ul>
     * 于是 {@code sodium-fabric-0.6.0} / {@code modmenu-11.0.0} / {@code cloth-config-15.0.140}
     * 这类 **mod 自身版本号不会被当成 MC 版本**。
     * （已用真实文件名离线验证：26.3 组 → 26.3；1.21.4 组 → 1.21.4；纯 mod 版本组 → 空。）
     */
    public static void collect(String text, HashMap<String, Integer> counter) {
        if (text == null || text.isEmpty() || counter == null) {
            return;
        }
        try {
            Matcher m = Pattern
                    .compile("(?i)(?:^|[^0-9a-z])((?:b|a|inf-|c0\\.|rd-)?(\\d{1,2})\\.(\\d{1,2})(?:\\.(\\d{1,2}))?)")
                    .matcher(text);
            while (m.find()) {
                String full = m.group(1);
                String major = m.group(2);
                if (full == null || major == null) {
                    continue;
                }
                int maj;
                try {
                    maj = Integer.parseInt(major);
                } catch (Throwable ignored) {
                    continue;
                }
                // 只接受 MC 传统版本（1.x）或现代大版本（>=20）
                if (maj != 1 && maj < 20) {
                    continue;
                }
                Integer c = counter.get(full);
                counter.put(full, c == null ? 1 : c + 1);
            }
        } catch (Throwable ignored) {
        }
    }

    /** 从计数表里挑最可能的版本（次数最多 → 版本号最大）。 */
    public static String pickMostLikely(HashMap<String, Integer> counter) {
        if (counter == null || counter.isEmpty()) {
            return null;
        }
        String best = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> e : counter.entrySet()) {
            int c = e.getValue() == null ? 0 : e.getValue();
            if (c > bestCount) {
                bestCount = c;
                best = e.getKey();
            } else if (c == bestCount && best != null && compare(e.getKey(), best) > 0) {
                best = e.getKey();
            }
        }
        return best;
    }

    /** 轻量版本比较（逐段数字），仅用于"同票取更大"。 */
    public static int compare(String a, String b) {
        try {
            String[] x = a.replaceAll("[^0-9.]", "").split("\\.");
            String[] y = b.replaceAll("[^0-9.]", "").split("\\.");
            int n = Math.max(x.length, y.length);
            for (int i = 0; i < n; i++) {
                int xi = (i < x.length && !x[i].isEmpty()) ? Integer.parseInt(x[i]) : 0;
                int yi = (i < y.length && !y[i].isEmpty()) ? Integer.parseInt(y[i]) : 0;
                if (xi != yi) {
                    return xi - yi;
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
