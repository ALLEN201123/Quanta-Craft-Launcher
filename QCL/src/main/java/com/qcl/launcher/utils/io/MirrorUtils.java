package com.qcl.launcher.utils.io;

/**
 * ★★★ 1.2.7：Modrinth / CurseForge 的**国内镜像（MCIM）**地址改写。
 *
 * 为什么做这个：这两个站点的 API 和图片/文件 CDN 在国内**时通时不通**
 * （实测 cdn.modrinth.com 同一张图 4 分钟前 2.5 秒能下、之后 20 秒直接超时），
 * 表现就是玩家看到的「图标一直白着、加载半天出不来」「模组下载老失败」。
 *
 * MCIM（https://mod.mcimirror.top）是社区维护的镜像，**路径结构与官方 100% 一致**，
 * 直接换域名即可（官方文档：api.modrinth.com → mod.mcimirror.top/modrinth；
 * cdn.modrinth.com / media.forgecdn.net / edge.forgecdn.net → mod.mcimirror.top；
 * api.curseforge.com → mod.mcimirror.top/curseforge）。实测本项目启动器 UA 可直接使用。
 *
 * ★ 用法一律是「**镜像优先、官方兜底**」：任何一处镜像失败都必须能回退到原始地址，
 *   绝不能因为镜像挂了就让玩家什么都加载不出来。
 */
public final class MirrorUtils {

    /** MCIM 镜像根地址（不含路径后缀） */
    public static final String MCIM = "https://mod.mcimirror.top";

    private static final String[] API_HOSTS = {
            "https://api.modrinth.com",
            "https://staging-api.modrinth.com",
            "https://api.curseforge.com",
    };

    private static final String[] CDN_HOSTS = {
            "https://cdn.modrinth.com",
            "https://mediafilez.forgecdn.net",
            "https://edge.forgecdn.net",
            "https://media.forgecdn.net",
    };

    private MirrorUtils() {
    }

    /**
     * API 地址改写：api.modrinth.com → mod.mcimirror.top/modrinth，
     * api.curseforge.com → mod.mcimirror.top/curseforge。其它地址原样返回。
     */
    public static String rewriteApi(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        if (url.startsWith("https://api.modrinth.com")) {
            return MCIM + "/modrinth" + url.substring("https://api.modrinth.com".length());
        }
        if (url.startsWith("https://staging-api.modrinth.com")) {
            return MCIM + "/modrinth" + url.substring("https://staging-api.modrinth.com".length());
        }
        if (url.startsWith("https://api.curseforge.com")) {
            return MCIM + "/curseforge" + url.substring("https://api.curseforge.com".length());
        }
        for (String host : API_HOSTS) {
            if (url.startsWith(host)) {
                return MCIM + url.substring(host.length());
            }
        }
        return url;
    }

    /**
     * 图片 / 文件 CDN 地址改写：cdn.modrinth.com、media(或 edge|mediafilez).forgecdn.net
     * → mod.mcimirror.top。其它地址原样返回。
     */
    public static String rewriteCdn(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        for (String host : CDN_HOSTS) {
            if (url.startsWith(host)) {
                return MCIM + url.substring(host.length());
            }
        }
        return url;
    }

    /** 这个地址是不是可改写的（用于避免无意义的重试） */
    public static boolean canRewrite(String url) {
        return !rewriteCdn(url).equals(url) || !rewriteApi(url).equals(url);
    }
}
