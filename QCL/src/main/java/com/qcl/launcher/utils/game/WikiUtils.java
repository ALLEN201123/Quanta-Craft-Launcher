package com.qcl.launcher.utils.game;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**把版本名转成对应的 Minecraft Wiki 页面地址**。
 *
 * <p>用户需求原话：「跳转对应版本 Wiki 页」。
 *
 * <p>难点在于**版本名与 Wiki 页面名不是一回事** —— Wiki 的 Java 版页面命名规则是：
 * <pre>
 *   正式版     1.20.1          → Java版1.20.1
 *   Beta       b1.7.3          → Java版Beta_1.7.3
 *   Alpha      a1.0.1_01       → Java版Alpha_v1.0.1_01        ← 注意有个 "v"
 *   Classic    c0.0.13a_03     → Java版Classic_0.0.13a_03
 *   Indev      in-20091223-1459→ Java版Indev_20091223-1459     ← 去掉 "in-"
 *   Infdev     inf-20100227-1433→Java版Infdev_20100227-1433    ← 去掉 "inf-"
 *   Pre-release b1.9-pre6      → Java版Beta_1.9-pre6
 * </pre>
 * 所以必须按前缀分别处理，不能直接拼。
 */
public final class WikiUtils {

    /** 中文 Minecraft Wiki 主站（2023 年后 fandom 迁到这里）。 */
    private static final String WIKI_BASE = "https://zh.minecraft.wiki/w/";

    /** 英文站，中文站没有页面时兜底。 */
    private static final String WIKI_EN_BASE = "https://minecraft.wiki/w/Java_Edition_";

    private WikiUtils() {
    }

    /**
     * 把启动器里的版本 id 转成 Wiki 页面名（不含命名空间前缀 {@code Java版}）。
     *
     * @param versionId 版本 id，如 {@code b1.7.3} / {@code 1.20.1} / {@code in-20091223-1459}
     * @return Wiki 页面名，如 {@code Beta_1.7.3}；无法识别时返回原始 id
     */
    public static String toWikiPageName(String versionId) {
        if (versionId == null) {
            return "";
        }
        String id = versionId.trim();
        if (id.isEmpty()) {
            return "";
        }
        String low = id.toLowerCase();

        // ★ 顺序很重要：先判更长的前缀
        if (low.startsWith("inf-")) {
            return "Infdev_" + id.substring(4);
        }
        if (low.startsWith("in-")) {
            return "Indev_" + id.substring(3);
        }
        if (low.startsWith("c0.") || low.startsWith("pc-")) {
            // Classic / Pre-classic：Wiki 上直接就是版本名
            return "Classic_" + id;
        }
        if (low.startsWith("a1.")) {
            // Alpha：Wiki 用 "Alpha_v1.x.x"
            return "Alpha_v" + id.substring(1);
        }
        if (low.startsWith("b1.")) {
            // Beta（含 pre-release / _01 等后缀）
            return "Beta_" + id.substring(1);
        }
        // 正式版（1.0 以后）以及其它：直接用版本名
        return id;
    }

    /**
     * 拼出中文 Wiki 的完整 URL。
     *
     * @param versionId 版本 id
     * @return 完整 URL；版本 id 为空时返回中文 Wiki 首页
     */
    public static String toWikiUrl(String versionId) {
        String page = toWikiPageName(versionId);
        if (page == null || page.isEmpty()) {
            return "https://zh.minecraft.wiki/";
        }
        return WIKI_BASE + "Java版" + page;
    }

    /** 英文 Wiki 的 URL（给中文站缺页面时兜底用）。 */
    public static String toEnglishWikiUrl(String versionId) {
        String page = toWikiPageName(versionId);
        if (page == null || page.isEmpty()) {
            return "https://minecraft.wiki/";
        }
        return WIKI_EN_BASE + page;
    }

    /**
     * 打开该版本的中文 Wiki 页面。
     *
     * <p>★ 用 {@code ACTION_VIEW} 交给系统浏览器；没有浏览器时静默失败（不崩）。
     *
     * @return true = 已成功发起打开请求
     */
    public static boolean openWiki(Context context, String versionId) {
        if (context == null) {
            return false;
        }
        return openUrl(context, toWikiUrl(versionId));
    }

    /** 打开任意 URL（交给系统浏览器）。 */
    public static boolean openUrl(Context context, String url) {
        if (context == null || url == null || url.isEmpty()) {
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
