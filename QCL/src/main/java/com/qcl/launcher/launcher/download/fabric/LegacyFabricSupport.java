package com.qcl.launcher.launcher.download.fabric;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * ★★★★★ 2026-10-27（1.5.7 新增）【Legacy Fabric / Legacy Fabric API 的版本限制】。
 *
 * <p>用户反馈（QQ 群友 @5G-NR）：
 * 「能不能给 Legacy Fabric 和 Legacy Fabric API 加个版本限制，
 * 这两个就只支持 1.3-1.13.2 以及部分快照版本和愚人节版本」。
 *
 * <p><b>旧行为的问题</b>（实测）：「安装游戏」里 Legacy Fabric 那一行
 * <b>不管你选的是 1.20.1 还是 26.3 都照样点得开</b>，点进去拉到的是
 * {@code meta.legacyfabric.net} 的<b>全部</b> loader 版本（不按游戏版本过滤），
 * 选完一装就失败或装出一个进不去的实例 —— 典型的「能点、能选、装不成」。
 *
 * <p><b>修法</b>：以上游权威接口为准，把「支持的游戏版本集合」写死成常量，
 * 用来：
 * <ol>
 *   <li>在「安装游戏」界面直接<b>隐藏</b>不支持的那两行（而不是让玩家点了才报错）；</li>
 *   <li>点开版本列表前再校验一次（防止界面状态过期）。</li>
 * </ol>
 *
 * <p><b>数据来源</b>（不是我猜的，是两个接口直接转成常量的）：
 * <pre>
 *   加载器： https://meta.legacyfabric.net/v2/versions/game        → 76 个版本
 *   API  ： https://api.modrinth.com/v2/project/legacy-fabric-api  → 168 个版本
 * </pre>
 * 两者都是官方数据：前者就是 Legacy Fabric 安装器自己用的 meta，
 * 后者是它在 Modrinth 上声明的支持范围。
 *
 * <p>★ 以后上游加了新版本怎么办：重新抓一遍两个接口、把下面两个常量换掉即可。
 * 不能改成运行时拉取：那会让「没网就把那两行全藏起来」，而且渲染线程不能做联网。
 */
public final class LegacyFabricSupport {

    /** Legacy Fabric 加载器支持的完整游戏版本列表（含正式版、快照、愚人节版）。 */
    private static final String LOADER_GAME_VERSIONS =
            "1.13.2 1.12.2 1.11.2 1.10.2 1.9.4 1.8.9 1.8.8 1.8.7 1.8.6 1.8.5 1.8.4 15w14a 1.8.3 1.8.2 " +
            "1.8.2-pre7 1.8.2-pre6 1.8.2-pre5 1.8.2-pre4 1.8.2-pre3 1.8.2-pre2 1.8.2-pre1 1.8.1 1.8.1-pre5 " +
            "1.8.1-pre4 1.8.1-pre3 1.8.1-pre2 1.8.1-pre1 1.8 1.7.10 1.7.10-pre4 1.7.10-pre3 1.7.10-pre2 " +
            "1.7.10-pre1 1.7.9 1.7.8 1.7.7 1.7.6 1.7.6-pre2 1.7.6-pre1 1.7.5 1.7.4 1.7.3 13w49a 13w48b 13w48a " +
            "13w47e 13w47d 13w47c 13w47b 13w47a 1.7.2 1.7.1 1.7 1.6.4 1.6.3 1.6.2 1.6.1 1.6 1.5.2 " +
            "2point0_purple 2point0_blue 2point0_red 1.5.1 13w11a 1.5 1.4.7 1.4.5 1.4.6 1.4.4 1.4.3 1.4.2 " +
            "1.4.1 1.4 1.3.2 1.3.1 1.3";

    /** Legacy Fabric API（Modrinth 项目 legacy-fabric-api）声明支持的游戏版本列表。 */
    private static final String API_GAME_VERSIONS =
            "1.3 1.3.1 1.3.2 1.4 1.4.1 1.4.2 1.4.3 1.4.4 1.4.5 1.4.6 1.4.7 1.5 1.5.1 13w16a 13w16b 1.5.2 1.6 " +
            "1.6.1 1.6.2 13w36a 13w36b 13w37a 1.6.3 13w37b 1.6.4 1.7 1.7.1 1.7.2 13w47a 13w47b 13w47c 13w47d " +
            "13w47e 13w48a 13w48b 13w49a 1.7.3 1.7.4 14w02a 14w02b 14w02c 14w03a 14w03b 14w04a 14w04b 14w05a " +
            "14w05b 14w06a 14w06b 14w07a 1.7.5 14w08a 14w10a 14w10b 14w10c 1.7.6-pre1 1.7.6-pre2 14w11a 1.7.6 " +
            "1.7.7 1.7.8 1.7.9 14w11b 14w17a 14w18a 14w18b 14w19a 1.7.10-pre1 1.7.10-pre2 1.7.10-pre3 " +
            "1.7.10-pre4 1.7.10 1.8 1.8.1-pre1 1.8.1-pre2 1.8.1-pre3 1.8.1-pre4 1.8.1-pre5 1.8.1 1.8.2-pre1 " +
            "1.8.2-pre2 1.8.2-pre3 1.8.2-pre4 1.8.2-pre5 1.8.2-pre6 1.8.2-pre7 1.8.2 1.8.3 15w14a 1.8.4 1.8.5 " +
            "1.8.6 1.8.7 1.8.8 15w31a 15w31b 15w31c 15w32a 15w32b 15w32c 15w33a 15w33b 15w33c 15w34a 15w34b " +
            "15w34c 15w34d 15w35a 15w35b 15w35c 15w35d 15w35e 15w36a 15w36b 15w36c 15w36d 15w37a 15w38a " +
            "15w38b 15w39a 15w39b 15w39c 15w40a 15w40b 15w41a 15w41b 15w42a 15w43a 15w43b 15w43c 15w44a " +
            "15w44b 15w45a 15w46a 15w47a 15w47b 15w47c 15w49a 1.8.9 1.9 1.9.1-pre1 1.9.1-pre2 1.9.1-pre3 " +
            "1.9.1 1.9.2 1.RV-Pre1 16w14a 16w15a 16w15b 1.9.3-pre1 1.9.3-pre2 1.9.3-pre3 1.9.3 1.9.4 1.10 " +
            "1.10.1 1.10.2 1.11 16w50a 1.11.1 1.11.2 1.12 17w31a 1.12.1-pre1 1.12.1 1.12.2-pre1 1.12.2-pre2 " +
            "1.12.2";

    private static final Set<String> LOADER_SET = toSet(LOADER_GAME_VERSIONS);
    private static final Set<String> API_SET = toSet(API_GAME_VERSIONS);

    private LegacyFabricSupport() {
    }

    private static Set<String> toSet(String csv) {
        Set<String> set = new LinkedHashSet<String>();
        if (csv != null) {
            for (String t : csv.trim().split("\\s+")) {
                if (t != null && t.length() > 0) {
                    set.add(t);
                }
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /** 该游戏版本能不能装 Legacy Fabric（加载器）。 */
    public static boolean isLoaderSupported(String mcVersion) {
        return mcVersion != null && LOADER_SET.contains(mcVersion.trim());
    }

    /** 该游戏版本能不能装 Legacy Fabric API。 */
    public static boolean isApiSupported(String mcVersion) {
        return mcVersion != null && API_SET.contains(mcVersion.trim());
    }

    public static int loaderVersionCount() {
        return LOADER_SET.size();
    }

    public static int apiVersionCount() {
        return API_SET.size();
    }

    /**
     * 「只列真的支持这个版本的 loader」的接口。
     *
     * <p>不支持的版本会返回 400 / 空数组 —— 这也是之前在界面上点开只看到一片空白的原因。
     */
    public static String loaderListUrl(String mcVersion) {
        return "https://meta.legacyfabric.net/v2/versions/loader/" + mcVersion;
    }

    /** 人话版支持范围（提示文案用）。 */
    public static String rangeText() {
        return "1.3 – 1.13.2 之间的版本（共 " + loaderVersionCount() + " 个，含 1.8 / 1.8.x / 1.9.4 / "
                + "1.10.2 / 1.11.2 / 1.12.2 / 1.13.2 等）、部分快照（13w11a、1.7.10-preN、直到 15w49a）"
                + "和愚人节版本（2point0_red / 2point0_blue / 2point0_purple / 15w14a）";
    }

    /** 加载器不支持时的提示文案。 */
    public static String loaderUnsupportedText(String mcVersion) {
        return "Legacy Fabric 不支持 Minecraft " + mcVersion + "。\n\n"
                + "它只支持：" + rangeText() + "。\n\n"
                + "现代版本请用上面的「Fabric」那一行。";
    }

    /** API 不支持时的提示文案。 */
    public static String apiUnsupportedText(String mcVersion) {
        return "Legacy Fabric API 没有适用于 Minecraft " + mcVersion + " 的版本。\n\n"
                + "它只覆盖 1.3 – 1.12.2（共 " + apiVersionCount()
                + " 个版本，含部分快照与愚人节版）；1.13 及以后请用正式的 Fabric API。";
    }

    /** 支持范围一句话（设置/说明页用）。 */
    public static String summaryText() {
        return "Legacy Fabric：1.3 – 1.13.2（" + loaderVersionCount() + " 个版本）；"
                + "Legacy Fabric API：1.3 – 1.12.2（" + apiVersionCount() + " 个版本）";
    }
}
