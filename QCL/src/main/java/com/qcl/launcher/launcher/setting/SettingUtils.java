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
        return list;
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

