package com.qcl.launcher.launcher.download.cleanroom;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**Cleanroom 的一个可用版本**。
 *
 * <p>Cleanroom 是 Forge 1.12.2 的社区继任分支（继续维护 1.12.2 的模组生态）。
 *
 * <p>数据来源（照 FCL 最新源码 {@code CleanroomVersionList} 确认）：
 * <pre>
 *   GET https://hmcl.glavo.site/metadata/cleanroom/index.json
 *   → [ { "name": "0.4.3-alpha", "created_at": "2024-..." }, ... ]
 *   安装器 https://hmcl.glavo.site/metadata/cleanroom/files/cleanroom-{name}-installer.jar
 * </pre>
 * 它**只支持 Minecraft 1.12.2**。
 */
public class CleanroomVersion {

    /** 版本名（如 {@code 0.4.3-alpha}），也用于拼安装器文件名。 */
    public String name;

    /** 发布时间（ISO-8601 字符串，仅用于排序/展示，可能为 null）。 */
    public String created_at;

    public CleanroomVersion() {
    }

    public CleanroomVersion(String name, String createdAt) {
        this.name = name;
        this.created_at = createdAt;
    }

    /** 安装器 jar 的完整下载地址。 */
    public String installerUrl() {
        return "https://hmcl.glavo.site/metadata/cleanroom/files/cleanroom-"
                + this.name + "-installer.jar";
    }

    @Override
    public String toString() {
        return this.name == null ? "" : this.name;
    }
}
