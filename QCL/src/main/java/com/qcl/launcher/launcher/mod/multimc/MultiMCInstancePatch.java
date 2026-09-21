package com.qcl.launcher.launcher.mod.multimc;

import com.google.gson.annotations.SerializedName;
import com.qcl.launcher.launcher.game.Library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ★ 1.2.3：MultiMC / Prism Launcher 整合包里 patches/ 目录下的单个 patch 文件。
 *
 * 移植自 FCL：FCL/src/main/java/com/tungsten/fclcore/mod/multimc/MultiMCInstancePatch.java
 * QCL 原先完全没有这个类，所以 patches/*.json 根本没人解析 ——
 * 这就是即使把文件解包出来，游戏也起不来的原因之一（Forge 之类的
 * mainClass / -tweakClass / 额外库全在 patch 里，不合并进版本 json 就等于没装）。
 *
 * 字段名严格照 MC/Prism 的实际 json 写：
 *   +tweakers  / +libraries 带加号前缀，是「追加」语义，不能漏掉加号。
 */
public final class MultiMCInstancePatch {

    private final String name;
    private final String version;

    @SerializedName("mcVersion")
    private final String gameVersion;
    private final String mainClass;
    private final String fileId;

    /** 需要追加的 tweakClass（Forge / LiteLoader 这类靠它启动） */
    @SerializedName("+tweakers")
    private final List<String> tweakers;

    /** 追加的库 */
    @SerializedName("+libraries")
    private final List<Library> addLibraries;

    /** 替换用的库 */
    @SerializedName("libraries")
    private final List<Library> libraries;

    /**
     * ★ 1.2.3：MultiMC 的「jarmod」列表。
     *
     * 语义：**把这些 jar 合并进 minecraft.jar**（b1.7.3 时代的加载器全靠这个）。
     * 实测样例（Cursed Fabric b1.7.3 整合包的 patches/org.multimc.jarmod.*.json）：
     * <pre>
     * { "formatVersion": 1,
     *   "jarMods": [ { "MMC-displayname": "net.fabricmc.intermediary",
     *                  "MMC-filename": "b5cee00e-....jar", "MMC-hint": "local",
     *                  "name": "org.multimc.jarmods:b5cee00e-...:1" } ],
     *   "name": "Intermediary Mappings (Required)" }
     * </pre>
     * ★ 不合并的话加载器根本不会生效（点启动必崩），这是之前漏掉的步骤。
     */
    @SerializedName("jarMods")
    private final List<MultiMCJarMod> jarMods;

    /** jarmod 条目：只需要文件名，实际 jar 在整合包的 jarmods/ 目录下 */
    public static final class MultiMCJarMod {
        @SerializedName("MMC-filename")
        private final String fileName;

        @SerializedName("name")
        private final String name;

        @SerializedName("MMC-displayname")
        private final String displayName;

        public MultiMCJarMod() {
            this(null, null, null);
        }

        public MultiMCJarMod(String fileName, String name, String displayName) {
            this.fileName = fileName;
            this.name = name;
            this.displayName = displayName;
        }

        /** jarmods/ 目录下的文件名 */
        public String getFileName() {
            return fileName;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public MultiMCInstancePatch() {
        this("", "", "", "", "", null, null, null, null);
    }

    public MultiMCInstancePatch(String name, String version, String gameVersion, String mainClass,
                                String fileId, List<String> tweakers,
                                List<Library> addLibraries, List<Library> libraries,
                                List<MultiMCJarMod> jarMods) {
        this.name = name;
        this.version = version;
        this.gameVersion = gameVersion;
        this.mainClass = mainClass;
        this.fileId = fileId;
        this.tweakers = tweakers == null ? new ArrayList<String>() : new ArrayList<String>(tweakers);
        this.addLibraries = addLibraries == null ? new ArrayList<Library>() : new ArrayList<Library>(addLibraries);
        this.libraries = libraries == null ? new ArrayList<Library>() : new ArrayList<Library>(libraries);
        this.jarMods = jarMods == null ? new ArrayList<MultiMCJarMod>() : new ArrayList<MultiMCJarMod>(jarMods);
    }

    public List<MultiMCJarMod> getJarMods() {
        return Collections.unmodifiableList(jarMods);
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getFileId() {
        return fileId;
    }

    public List<String> getTweakers() {
        return Collections.unmodifiableList(tweakers);
    }

    /**
     * 两份库合起来，跟 FCL 的 Lang.merge(_libraries, libraries) 等价。
     * （QCL 的 Lang 没有 merge，就地实现一个，行为一致：_libraries 在前，libraries 在后。）
     */
    public List<Library> getLibraries() {
        List<Library> merged = new ArrayList<Library>(addLibraries.size() + libraries.size());
        merged.addAll(addLibraries);
        merged.addAll(libraries);
        return merged;
    }

    /** 这个 patch 是否需要合并进版本（名字全是空的就当无效 patch 跳过） */
    public boolean isValid() {
        return mainClass != null && !mainClass.isEmpty()
                || !tweakers.isEmpty()
                || !getLibraries().isEmpty();
    }
}
