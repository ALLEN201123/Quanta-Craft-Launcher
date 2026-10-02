/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.util.Log
 *  com.google.gson.Gson
 */
package com.qcl.launcher.launcher.setting;

import android.util.Log;
import com.google.gson.Gson;
import com.qcl.launcher.control.bean.button.ButtonStyle;
import com.qcl.launcher.control.bean.rocker.RockerStyle;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.list.local.controller.ControlPattern;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
import com.qcl.launcher.launcher.list.local.java.JavaListBean;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.platform.Bits;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class SettingUtils {
    private static final String JAVA_VERSION_str = "JAVA_VERSION=\"";
    private static final String OS_ARCH_str = "OS_ARCH=\"";

    /**
     * ★★★ 1.2.5：取「当前正在玩的版本」对应的**游戏版本号**（下载页「游戏版本」筛选的默认值）。
     *
     * 为什么不能直接用 {@code publicGameSetting.currentVersion}：
     *   它存的是**完整路径**（{@code <游戏目录>/versions/<版本名>}），
     *   直接丢进「游戏版本列表」做 indexOf 必然 -1 ——
     *   老代码就是这么写的，导致五个下载页（模组/整合包/光影/世界/资源包）的
     *   「游戏版本」默认全停在第 0 项（= 不筛选），玩家每次都得自己翻。
     *
     * 解析顺序：
     *   ① 版本 json 里 id == "game" 的 patch 版本号（最准：Fabric/Forge 版也能拿到本体版本）
     *   ② 版本 json 的 id 字段
     *   ③ 版本目录名（b1.7.3ml 这种带后缀的，交给调用方做前缀匹配）
     */
    public static String getCurrentGameVersion(MainActivity activity) {
        try {
            String cur = activity.publicGameSetting.currentVersion;
            if (cur == null || cur.isEmpty()) {
                return null;
            }
            File dir = new File(cur);
            String name = dir.getName();
            try {
                File json = new File(dir, name + ".json");
                if (json.isFile()) {
                    Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, new Artifact.Serializer()).registerTypeAdapter(Bits.class, new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, new Argument.Deserializer()).create();
                    Version version = (Version)gson.fromJson(FileStringUtils.getStringFromFile(json.getAbsolutePath()), Version.class);
                    if (version != null) {
                        if (version.getPatches() != null) {
                            for (Version patch : version.getPatches()) {
                                if ("game".equals(patch.getId()) && patch.getVersion() != null
                                        && !patch.getVersion().isEmpty()) {
                                    return patch.getVersion();
                                }
                            }
                        }
                        if (version.getId() != null && !version.getId().isEmpty()) {
                            return version.getId();
                        }
                    }
                }
            }
            catch (Throwable ignored) {
            }
            return name;
        }
        catch (Throwable t) {
            return null;
        }
    }

    public static ArrayList<GameListBean> getLocalVersionInfo(String path, String currentVersion) {
        ArrayList<GameListBean> list = new ArrayList<GameListBean>();
        File versionsDir = new File(path + "/versions/");
        String[] string2 = versionsDir.list();
        if (string2 == null) {
            return list;
        }
        if (versionsDir.exists()) {
            for (String str : string2) {
                if (!new File(path + "/versions/" + str + "/" + str + ".json").exists() || !new File(path + "/versions/" + str + "/" + str + ".jar").exists()) continue;
                GameListBean bean = new GameListBean("", "", "", false);
                bean.name = str;
                if (new File(path + "/versions/" + str + "/icon.png").exists()) {
                    bean.iconPath = path + "/versions/" + str + "/icon.png";
                }
                String gameJsonText = FileStringUtils.getStringFromFile(path + "/versions/" + str + "/" + str + ".json");
                Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
                Version version = (Version)gson.fromJson(gameJsonText, Version.class);
                if (version == null) {
                    Log.e((String)"QCL", (String)("\u7248\u672c json \u65e0\u6cd5\u89e3\u6790\uff0c\u4ecd\u6309\u6587\u4ef6\u5939\u540d\u663e\u793a: " + str));
                    bean.version = str;
                    bean.isSelected = currentVersion.equals(bean.name);
                    list.add(bean);
                    continue;
                }
                if (version.getPatches() != null && version.getPatches().size() > 0) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Version p : version.getPatches()) {
                        switch (p.getId()) {
                            case "game": {
                                stringBuilder.append(p.getVersion());
                                break;
                            }
                            case "forge": {
                                stringBuilder.append(", Forge: ").append(p.getVersion());
                                break;
                            }
                            case "optifine": {
                                stringBuilder.append(", OptiFine: ").append(p.getVersion());
                                break;
                            }
                            case "fabric": {
                                stringBuilder.append(", Fabric: ").append(p.getVersion());
                                break;
                            }
                            case "quilt": {
                                stringBuilder.append(", Quilt: ").append(p.getVersion());
                                break;
                            }
                            case "liteloader": {
                                stringBuilder.append(", LiteLoader: ").append(p.getVersion());
                            }
                        }
                    }
                    bean.version = stringBuilder.toString();
                } else {
                    bean.version = version.getId();
                }
                bean.isSelected = currentVersion.equals(bean.name);
                list.add(bean);
            }
        }
        // ★ 1.4.3：原来**完全没有排序**，直接按 File.list() 的任意顺序返回（实测乱排：
        //   in-20091223 / b1.0_01 / b1.8.1 / inf-… 混在一起）。
        //   现在按「正式版在前、远古版在后」分组，组内再按版本倒序。
        sortLocalVersions(list);
        return list;
    }

    /**
     * 本地版本列表排序：**正式版 → 远古版**两组，组内倒序。
     * · 正式版：按版本号数值倒序（26.3 > 1.21.11 > 1.20.6 …）
     * · 远古版：按名字倒序（这些版本名里的日期是零填充的，字符串倒序刚好等价于时间倒序：
     *   in-20100223 > in-20100110 > in-20091231 > in-20091223；c0.30 > c0.29；b1.9 > b1.8 …）
     */
    public static void sortLocalVersions(ArrayList<GameListBean> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        java.util.Collections.sort(list, new java.util.Comparator<GameListBean>() {
            @Override
            public int compare(GameListBean a, GameListBean b) {
                boolean la = isLegacyVersionName(a == null ? null : a.name);
                boolean lb = isLegacyVersionName(b == null ? null : b.name);
                if (la != lb) {
                    return la ? 1 : -1;          // 正式版在前
                }
                String na = a == null || a.name == null ? "" : a.name;
                String nb = b == null || b.name == null ? "" : b.name;
                if (la) {
                    return nb.compareTo(na);      // 远古版：名字倒序
                }
                long va = versionSortKey(na);
                long vb = versionSortKey(nb);
                if (va != vb) {
                    return va < vb ? 1 : -1;      // 正式版：版本号倒序
                }
                return nb.compareTo(na);
            }
        });
    }

    /**
     * 是否「远古版」版本名（classic / pre-classic / indev / infdev / alpha / beta / pre）。
     * ★ 不用 startsWith("c")：那会把 `Cursed-Fabric-MultiMCnew` 这类整合包误判成 classic，
     *   与 1.4.3 里 isNoTitleScreenVersion 踩过的坑同源。
     */
    public static boolean isLegacyVersionName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.trim().toLowerCase();
        // ★ 用 find()（前缀匹配），不能用 matches()：matches() 要求**整串**匹配，
        //   而这里只想匹配前缀 —— 用 matches() 会导致所有版本都判不出远古版，
        //   远古版（名字里的 8 位日期数值极大）就会把正式版顶到后面去（已实测踩到）。
        if (java.util.regex.Pattern.compile("^(c\\d|rd-|in-|inf-|a1\\.|b1\\.)").matcher(n).find()) {
            return true;
        }
        return n.contains("-pre") || n.contains("_pre") || n.contains("-rc") || n.contains("_rc");
    }

    /** 把版本号转成可比较的数值键：26.3 → 260003，1.21.11 → 12111；解析不了返回 0。 */
    private static long versionSortKey(String name) {
        if (name == null) {
            return 0L;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").matcher(name);
        if (!m.find()) {
            return 0L;
        }
        try {
            long a = Long.parseLong(m.group(1));
            long b = m.group(2) != null ? Long.parseLong(m.group(2)) : 0L;
            long c = m.group(3) != null ? Long.parseLong(m.group(3)) : 0L;
            return a * 1000000L + b * 1000L + c;
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static ArrayList<String> getLocalVersionNames(String path) {
        ArrayList<String> list = new ArrayList<String>();
        String[] string2 = new File(path + "/versions/").list();
        if (new File(path + "/versions/").exists()) {
            for (String str : string2) {
                if (!new File(path + "/versions/" + str + "/" + str + ".json").exists() || !new File(path + "/versions/" + str + "/" + str + ".jar").exists()) continue;
                list.add(str);
            }
        }
        return list;
    }

    public static ArrayList<JavaListBean> getJavaVersionInfo() {
        ArrayList<JavaListBean> list = new ArrayList<JavaListBean>();
        String javaPath = AppManifest.JAVA_DIR + "/";
        String[] string2 = new File(javaPath).list();
        if (new File(javaPath).exists()) {
            for (String str : string2) {
                JavaListBean bean = new JavaListBean("", "", "");
                bean.name = str;
                File release = new File(javaPath + bean.name, "release");
                if (release.exists() && FileStringUtils.getStringFromFile(release.getAbsolutePath()) != null) {
                    String releaseContent = FileStringUtils.getStringFromFile(release.getAbsolutePath());
                    int _JAVA_VERSION_index = releaseContent.indexOf(JAVA_VERSION_str);
                    int _OS_ARCH_index = releaseContent.indexOf(OS_ARCH_str);
                    String javaVersion = releaseContent.substring(_JAVA_VERSION_index, releaseContent.indexOf(34, _JAVA_VERSION_index));
                    String[] javaVersionSplit = javaVersion.split("\\.");
                    bean.version = javaVersionSplit[0].equals("1") ? javaVersionSplit[1] : javaVersionSplit[0];
                    bean.osArch = releaseContent.substring(_OS_ARCH_index, releaseContent.indexOf(34, _OS_ARCH_index));
                } else {
                    bean.version = "unknown version";
                    bean.osArch = "";
                }
                list.add(bean);
            }
        }
        return list;
    }

    public static ArrayList<ControlPattern> getControlPatternList() {
        ArrayList<ControlPattern> list = new ArrayList<ControlPattern>();
        String[] string2 = new File(AppManifest.CONTROLLER_DIR + "/").list();
        if (new File(AppManifest.CONTROLLER_DIR + "/").exists()) {
            for (String str : string2) {
                String info = FileStringUtils.getStringFromFile(AppManifest.CONTROLLER_DIR + "/" + str + "/info.json");
                Gson gson = new Gson();
                ControlPattern controlPattern = (ControlPattern)gson.fromJson(info, ControlPattern.class);
                list.add(controlPattern);
            }
        }
        return list;
    }

    public static ArrayList<ChildLayout> getChildList(String pattern) {
        ArrayList<ChildLayout> list = new ArrayList<ChildLayout>();
        String[] string2 = new File(AppManifest.CONTROLLER_DIR + "/" + pattern + "/").list();
        if (new File(AppManifest.CONTROLLER_DIR + "/" + pattern + "/").exists()) {
            for (String str : string2) {
                if (str.equals("info.json")) continue;
                String info = FileStringUtils.getStringFromFile(AppManifest.CONTROLLER_DIR + "/" + pattern + "/" + str);
                Gson gson = new Gson();
                ChildLayout childLayout = (ChildLayout)gson.fromJson(info, ChildLayout.class);
                list.add(childLayout);
            }
        }
        return list;
    }

    public static ArrayList<ButtonStyle> getButtonStyleList() {
        ArrayList<ButtonStyle> list = new ArrayList<ButtonStyle>();
        if (new File(AppManifest.STYLE_DIR + "/button.json").exists()) {
            String string2 = FileStringUtils.getStringFromFile(AppManifest.STYLE_DIR + "/button.json");
            Gson gson = new Gson();
            ButtonStyle[] buttonStyles = (ButtonStyle[])gson.fromJson(string2, ButtonStyle[].class);
            list.addAll(Arrays.asList(buttonStyles));
            if (list.size() == 0) {
                ButtonStyle style2 = new ButtonStyle();
                style2.name = "Default";
                list.add(style2);
                SettingUtils.saveButtonStyle(list);
            }
        } else {
            ButtonStyle style3 = new ButtonStyle();
            style3.name = "Default";
            list.add(style3);
            SettingUtils.saveButtonStyle(list);
        }
        return list;
    }

    public static void saveButtonStyle(ArrayList<ButtonStyle> list) {
        Gson gson = new Gson();
        String string2 = gson.toJson(list);
        FileStringUtils.writeFile(AppManifest.STYLE_DIR + "/button.json", string2);
    }

    public static ArrayList<RockerStyle> getRockerStyleList() {
        ArrayList<RockerStyle> list = new ArrayList<RockerStyle>();
        if (new File(AppManifest.STYLE_DIR + "/rocker.json").exists()) {
            String string2 = FileStringUtils.getStringFromFile(AppManifest.STYLE_DIR + "/rocker.json");
            Gson gson = new Gson();
            RockerStyle[] rockerStyles = (RockerStyle[])gson.fromJson(string2, RockerStyle[].class);
            list.addAll(Arrays.asList(rockerStyles));
            if (list.size() == 0) {
                RockerStyle style2 = new RockerStyle();
                style2.name = "Default";
                list.add(style2);
                SettingUtils.saveRockerStyle(list);
            }
        } else {
            RockerStyle style3 = new RockerStyle();
            style3.name = "Default";
            list.add(style3);
            SettingUtils.saveRockerStyle(list);
        }
        return list;
    }

    public static void saveRockerStyle(ArrayList<RockerStyle> list) {
        Gson gson = new Gson();
        String string2 = gson.toJson(list);
        FileStringUtils.writeFile(AppManifest.STYLE_DIR + "/rocker.json", string2);
    }

    public static ArrayList<String> getFastList() {
        ArrayList<String> list = new ArrayList<String>();
        if (new File(AppManifest.SETTING_DIR + "/fast_text.json").exists()) {
            String string2 = FileStringUtils.getStringFromFile(AppManifest.SETTING_DIR + "/fast_text.json");
            Gson gson = new Gson();
            String[] fastTexts = (String[])gson.fromJson(string2, String[].class);
            list.addAll(Arrays.asList(fastTexts));
        } else {
            list.add("/gamemode 0");
            list.add("/gamemode 1");
            list.add("/gamemode 2");
            list.add("/weather clear");
            list.add("/weather rain");
            list.add("/weather thunder");
            SettingUtils.saveFastText(list);
        }
        return list;
    }

    public static void saveFastText(ArrayList<String> list) {
        Gson gson = new Gson();
        String string2 = gson.toJson(list);
        FileStringUtils.writeFile(AppManifest.SETTING_DIR + "/fast_text.json", string2);
    }
}

