package com.qcl.launcher.launcher.download.game;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * ★ 1.4.5：愚人节版本识别。
 *
 * <p>官方清单里愚人节版本**一律被标成 {@code snapshot}**（实测：
 * 15w14a / 1.RV-Pre1 / 3D Shareware v1.34 / 20w14infinite / 22w13oneblockatatime /
 * 23w13a_or_b / 24w14potato / 25w14craftmine 全是 snapshot），
 * 所以下载页原来只能把它们混在"快照版"里，玩家想找愚人节版本很难。
 *
 * <p>识别方式：**只按 ID 白名单精确匹配**。
 * ⚠️ 不要用"发布日期在 4 月 1 日前后"来兜底 —— 实测会误报：
 * 26w14a（2026-04-01）、21w13a（2021-04-01）、20w14a（2020-04-02）、
 * 17w13b（2017-03-31）、b1.4（2011-03-31）、26.1.1（2026-04-01）
 * 都是**正常版本**，按日期判会全部被误当成愚人节版。
 */
public final class AprilFools {

    /**
     * 已知的** Java 版**愚人节版本 ID（清单里的原名，大小写敏感）。
     * <p>注：QCL 是 Java 版启动器，清单也只用 Java 版（917 个版本），
     * 所以这里只收 Java 版的愚人节版本，基岩版的不列（它们在清单里根本不存在）。
     */
    private static final Set<String> IDS = new HashSet<String>(Arrays.asList(
            "15w14a",                   // 2015 · 爱与拥抱更新
            "1.RV-Pre1",                // 2016 · 潮流更新
            "3D Shareware v1.34",       // 2019 · 3D 共享软件
            "20w14infinite",            // 2020 · 无限快照
            "22w13oneblockatatime",     // 2022 · 一次一块
            "23w13a_or_b",              // 2023 · 投票更新
            "24w14potato",              // 2024 · 毒马铃薯更新
            "25w14craftmine",           // 2025 · 合成 mine
            // 2026 · 愚人节
            //   判定依据：2026 年官方命名规则已改成 26.x-snapshot-N / 26.x-pre-N / 26.x，
            //   而 26w14a 用的是**旧的周更命名**，且正好在 4 月 1 日发布 —— 即愚人节版本。
            //   （同一天还有个 26.1.1，那是符合 26.x 规则的正规补丁版，不算愚人节版。）
            "26w14a",
            // 2013 · 愚人节 "Java 版 2.0"。
            //   ★ 2026-10-06 修正：以前只写了 "2.0"，但清单里**根本没有叫 "2.0" 的版本**，
            //   实际是三个带颜色后缀的独立条目（对照 FCL 的愚人节列表确认，都是 2013-03-20）：
            //     2.0_blue    2013-03-20 18:00:02
            //     2.0_red     2013-03-20 18:00:01
            //     2.0_purple  2013-03-20 18:00:00
            //   漏了它们 → 愚人节分类里看不到 2.0（用户反馈）。
            "2.0_blue",
            "2.0_red",
            "2.0_purple",
            // 官方 Java 清单未收录、但归档清单里可能出现的裸 "2.0" 也一并收着（无害）
            "2.0",
            // 兜底：万一清单里用红色/蓝色/紫色的其它写法
            "2.0_blue_01",
            "2.0_red_01",
            "2.0_purple_01"
    ));

    private AprilFools() {
    }

    /** 是否愚人节版本（按 ID 白名单精确匹配）。 */
    public static boolean isAprilFools(VersionManifest.Version version) {
        return version != null && isAprilFools(version.id);
    }

    /** 便利重载：只给 id 时用。 */
    public static boolean isAprilFools(String id) {
        return id != null && IDS.contains(id);
    }
}
