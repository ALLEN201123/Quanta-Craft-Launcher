package com.qcl.launcher.launcher.download.neoforge;

import com.google.gson.annotations.SerializedName;

/**
 * ★ NeoForge 版本条目。
 *
 * 数据来源：BMCLAPI 的 {@code /neoforge/list/<MC版本>} 接口（实测可用），
 * 形如：
 * <pre>
 * [{"installerPath":"/maven/net/neoforged/neoforge/21.1.1/neoforge-21.1.1-installer.jar",
 *   "mcversion":"1.21.1","version":"21.1.1"}]
 * </pre>
 *
 * 当 BMCLAPI 不可达时，会退回到从 maven-metadata.xml 里解析版本号，
 * 此时 {@code installerPath} 为空，由 {@link #getInstallerMavenPath()} 按标准
 * maven 路径拼出来（官方 / BMCLAPI 的 maven 都能命中）。
 */
public class NeoForgeVersion {

    /** NeoForge 自己的版本号，例如 21.1.72 */
    @SerializedName("version")
    private String version;

    /** 对应的 MC 版本，例如 1.21.1 */
    @SerializedName("mcversion")
    private String mcversion;

    /** BMCLAPI 给的相对路径，例如 /maven/net/neoforged/neoforge/21.1.1/neoforge-21.1.1-installer.jar */
    @SerializedName("installerPath")
    private String installerPath;

    public NeoForgeVersion() {
    }

    public NeoForgeVersion(String version, String mcversion, String installerPath) {
        this.version = version;
        this.mcversion = mcversion;
        this.installerPath = installerPath;
    }

    public String getVersion() {
        return version == null ? "" : version;
    }

    /** 这个 NeoForge 版本对应的 MC 版本（供列表显示与筛选） */
    public String getGameVersion() {
        return mcversion == null ? "" : mcversion;
    }

    public String getInstallerPath() {
        return installerPath;
    }

    /**
     * installer jar 在 maven 仓库里的相对路径（不带前导 /maven/）。
     * BMCLAPI 给了 {@code installerPath} 就直接用，否则按标准坐标拼。
     */
    public String getInstallerMavenPath() {
        String path = installerPath;
        if (path != null && path.length() > 0) {
            if (path.startsWith("/maven/")) {
                return path.substring("/maven/".length());
            }
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        }
        return "net/neoforged/neoforge/" + getVersion() + "/neoforge-" + getVersion() + "-installer.jar";
    }
}