package com.qcl.launcher.launcher.download.neoforge;

import com.google.gson.Gson;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.utils.io.NetworkUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ★ NeoForge 版本清单 / 版本号映射 / 多源回退。
 *
 * 实测（2026-10）：
 *  - 官方 {@code maven.neoforged.net} 在本机 TLS 握手失败（不可达）；
 *  - {@code bmclapi2.bangbang93.com/neoforge/maven-metadata.xml} 404；
 *  - {@code maven.aliyun.com/.../maven-metadata.xml} 404；
 *  - ✅ {@code https://bmclapi2.bangbang93.com/neoforge/list/<MC版本>} 200（结构化，带 installerPath）；
 *  - ✅ {@code https://bmclapi2.bangbang93.com/maven/net/neoforged/neoforge/maven-metadata.xml} 200（全量版本号）。
 *
 * 所以实现顺序：BMCLAPI list → BMCLAPI maven-metadata（按版本号映射 MC）→ 内置静态清单（兜底）。
 *
 * ★ NeoForge → MC 版本映射规则（20.2 起）：
 *   取版本号前三段 {@code A.B.C}，MC 版本 = {@code 1.A.B}，其中 B 为 0 时省略，
 *   即 20.2.x→1.20.2、20.3.x→1.20.3、20.4.x→1.20.4、20.6.x→1.20.6、
 *   21.0.x→1.21、21.1.x→1.21.1 …… 21.10.x→1.21.10。
 *   （1.20.1 的 NeoForge 沿用 Forge 的 47.1.x 老编号，无法从版本号推导，
 *    只能靠 BMCLAPI list 接口直接给出 mcversion，故映射函数对它返回 null。）
 */
public final class NeoForgeVersions {

    private static final String BMCLAPI_BASE = "https://bmclapi2.bangbang93.com";

    /** BMCLAPI 的结构化版本列表（优先） */
    private static final String BMCLAPI_LIST = BMCLAPI_BASE + "/neoforge/list/";
    /** 官方 maven 元数据（本机不可达，作为通用回退保留） */
    private static final String OFFICIAL_METADATA = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml";
    /** BMCLAPI 代理的 maven 元数据（实测可用） */
    private static final String BMCLAPI_METADATA = BMCLAPI_BASE + "/maven/net/neoforged/neoforge/maven-metadata.xml";

    private static final Pattern VERSION_TAG = Pattern.compile("<version>([^<]+)</version>");

    /** MC 版本筛选下拉的支持列表（NeoForge 实际发布过的 MC 版本，新的在前） */
    private static final String[] SUPPORTED_MC = new String[]{
            // ★ 1.4.3：补齐 26.x 与 1.21.11 —— 实测 BMCLAPI 的 neoforge/list/<MC版本> 全部有货：
            //   26.3→38 个构建、26.2→89、26.1.2→112、26.1→18、1.21.11→45。
            //   原先这里从 1.21.10 起步，导致 NeoForge 安装页的 MC 下拉里 26.x 一个都没有。
            "26.3", "26.2", "26.1.2", "26.1.1", "26.1", "1.21.11",
            "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5",
            "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
            "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1"
    };

    /**
     * ★ 兜底静态清单：上面几个源**全部不可达**时使用。
     * 版本号取自实测抓到的 BMCLAPI 列表（每个 MC 版本挑几个代表性版本）。
     */
    private static final Map<String, String[]> FALLBACK = new LinkedHashMap<>();

    static {
        // ★ 1.4.3：补 26.x / 1.21.11（值取自实测 BMCLAPI 列表的最新几个，非猜测）
        FALLBACK.put("26.3", new String[]{"26.3.0.39-beta", "26.3.0.38-beta", "26.3.0.37-beta"});
        FALLBACK.put("26.2", new String[]{"26.2.0.88", "26.2.0.87", "26.2.0.86"});
        FALLBACK.put("26.1.2", new String[]{"26.1.2.112", "26.1.2.111", "26.1.2.110"});
        FALLBACK.put("26.1.1", new String[]{"26.1.1.15-beta", "26.1.1.14-beta", "26.1.1.13-beta"});
        FALLBACK.put("26.1", new String[]{"26.1.0.19-beta", "26.1.0.18-beta", "26.1.0.17-beta"});
        FALLBACK.put("1.21.11", new String[]{"21.11.45", "21.11.44", "21.11.42"});
        FALLBACK.put("1.21.10", new String[]{"21.10.64", "21.10.60", "21.10.0-beta"});
        FALLBACK.put("1.21.9", new String[]{"21.9.16-beta", "21.9.12-beta", "21.9.0-beta"});
        FALLBACK.put("1.21.8", new String[]{"21.8.54", "21.8.52", "21.8.0-beta"});
        FALLBACK.put("1.21.7", new String[]{"21.7.25-beta", "21.7.20-beta", "21.7.0-beta"});
        FALLBACK.put("1.21.6", new String[]{"21.6.20-beta", "21.6.10-beta", "21.6.0-beta"});
        FALLBACK.put("1.21.5", new String[]{"21.5.98", "21.5.90", "21.5.0-beta"});
        FALLBACK.put("1.21.4", new String[]{"21.4.158", "21.4.150", "21.4.0-beta"});
        FALLBACK.put("1.21.3", new String[]{"21.3.97", "21.3.90", "21.3.2-beta"});
        FALLBACK.put("1.21.2", new String[]{"21.2.1-beta", "21.2.0-beta"});
        FALLBACK.put("1.21.1", new String[]{"21.1.252", "21.1.209", "21.1.72"});
        FALLBACK.put("1.21", new String[]{"21.0.98-beta", "21.0.90-beta", "21.0.1-beta"});
        FALLBACK.put("1.20.6", new String[]{"20.6.141", "20.6.130", "20.6.1-beta"});
        FALLBACK.put("1.20.5", new String[]{"20.5.21-beta", "20.5.16-beta", "20.5.0-beta"});
        FALLBACK.put("1.20.4", new String[]{"20.4.251", "20.4.237", "20.4.0-beta"});
        FALLBACK.put("1.20.3", new String[]{"20.3.8-beta", "20.3.5-beta", "20.3.1-beta"});
        FALLBACK.put("1.20.2", new String[]{"20.2.93", "20.2.90", "20.2.3-beta"});
        FALLBACK.put("1.20.1", new String[]{"47.1.105", "47.1.100", "47.1.5"});
    }

    private NeoForgeVersions() {
    }

    /** MC 版本筛选下拉用的列表（新的在前） */
    public static List<String> getSupportedMcVersions() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, SUPPORTED_MC);
        return list;
    }

    /**
     * NeoForge 版本号 → MC 版本号（映射规则见类注释），无法推导时返回 null。
     */
    public static String mcVersionOf(String neoVersion) {
        if (neoVersion == null) {
            return null;
        }
        String v = neoVersion;
        int dash = v.indexOf('-');
        if (dash > 0) {
            v = v.substring(0, dash);
        }
        String[] parts = v.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            // 只处理 20.x ~ 22.x（1.20 ~ 1.22）这一套；47.1.x 之类的老编号不猜
            if (major < 20 || major > 22) {
                return null;
            }
            return minor == 0 ? ("1." + major) : ("1." + major + "." + minor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * ★ 拉取某个 MC 版本下的 NeoForge 版本列表（子线程调用）。
     * 多源回退：BMCLAPI list → BMCLAPI maven-metadata（过滤）→ 官方 maven-metadata → 内置静态清单。
     */
    public static ArrayList<NeoForgeVersion> fetchForMc(String mcVersion) {
        ArrayList<NeoForgeVersion> list = new ArrayList<>();
        if (mcVersion == null || mcVersion.length() == 0) {
            return list;
        }

        // ① BMCLAPI 结构化列表
        try {
            String json = NetworkUtils.doGet(NetworkUtils.toURL(BMCLAPI_LIST + mcVersion));
            NeoForgeVersion[] arr = new Gson().fromJson(json, NeoForgeVersion[].class);
            if (arr != null) {
                for (NeoForgeVersion v : arr) {
                    if (v != null && v.getVersion().length() > 0) {
                        if (v.getGameVersion().length() == 0) {
                            // 兜底补一个 mcversion
                            v = new NeoForgeVersion(v.getVersion(), mcVersion, v.getInstallerPath());
                        }
                        list.add(v);
                    }
                }
            }
        } catch (Throwable ignored) {
            // 静默回退到下一个源
        }
        if (!list.isEmpty()) {
            list.sort(new VersionComparator());
            return list;
        }

        // ② maven-metadata（官方 / BMCLAPI 各试一遍），按映射规则过滤出该 MC 版本
        for (String meta : new String[]{BMCLAPI_METADATA, OFFICIAL_METADATA}) {
            try {
                String xml = NetworkUtils.doGet(NetworkUtils.toURL(meta));
                List<String> all = parseMavenMetadata(xml);
                for (String ver : all) {
                    if (mcVersion.equals(mcVersionOf(ver))) {
                        list.add(new NeoForgeVersion(ver, mcVersion, null));
                    }
                }
            } catch (Throwable ignored) {
                // 换下一个源
            }
            if (!list.isEmpty()) {
                list.sort(new VersionComparator());
                return list;
            }
        }

        // ③ 内置静态清单兜底
        String[] fallback = FALLBACK.get(mcVersion);
        if (fallback != null) {
            for (String ver : fallback) {
                list.add(new NeoForgeVersion(ver, mcVersion, null));
            }
        }
        list.sort(new VersionComparator());
        return list;
    }

    /** installer jar 的官方地址 */
    public static String officialInstallerUrl(NeoForgeVersion version) {
        return "https://maven.neoforged.net/releases/" + version.getInstallerMavenPath();
    }

    /** installer jar 的 BMCLAPI 镜像地址 */
    public static String bmclapiInstallerUrl(NeoForgeVersion version) {
        return BMCLAPI_BASE + "/maven/" + version.getInstallerMavenPath();
    }

    /** installer jar 的完整下载地址（按下载源选择首选地址，另一条留作回退） */
    public static String installerUrl(NeoForgeVersion version, int source) {
        if (source == DownloadUrlSource.DOWNLOAD_URL_SOURCE_OFFICIAL) {
            return officialInstallerUrl(version);
        }
        return bmclapiInstallerUrl(version);
    }

    /** 解析 maven-metadata.xml 里的 &lt;version&gt; 列表 */
    private static List<String> parseMavenMetadata(String xml) {
        List<String> list = new ArrayList<>();
        if (xml == null) {
            return list;
        }
        Matcher m = VERSION_TAG.matcher(xml);
        while (m.find()) {
            String v = m.group(1);
            if (v != null && v.trim().length() > 0) {
                list.add(v.trim());
            }
        }
        return list;
    }

    /** 版本号比较器：按数字段从大到小；同数字段正式版（无 -beta）排在 beta 前面 */
    private static final class VersionComparator implements Comparator<NeoForgeVersion> {
        @Override
        public int compare(NeoForgeVersion a, NeoForgeVersion b) {
            return -compareVersion(a.getVersion(), b.getVersion());
        }

        private int compareVersion(String va, String vb) {
            int[] na = numeric(va);
            int[] nb = numeric(vb);
            for (int i = 0; i < Math.min(na.length, nb.length); i++) {
                if (na[i] != nb[i]) {
                    return Integer.compare(na[i], nb[i]);
                }
            }
            if (na.length != nb.length) {
                return Integer.compare(na.length, nb.length);
            }
            // 数字相同：正式版 > 预发布版
            boolean ba = va != null && va.contains("-");
            boolean bb = vb != null && vb.contains("-");
            if (ba != bb) {
                return ba ? -1 : 1;
            }
            return 0;
        }

        private int[] numeric(String v) {
            if (v == null) {
                return new int[0];
            }
            int dash = v.indexOf('-');
            if (dash > 0) {
                v = v.substring(0, dash);
            }
            String[] parts = v.split("\\.");
            int[] out = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    out[i] = Integer.parseInt(parts[i].trim());
                } catch (NumberFormatException e) {
                    out[i] = -1;
                }
            }
            return out;
        }
    }
}