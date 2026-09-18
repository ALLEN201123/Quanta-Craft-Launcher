package com.qcl.launcher.launcher.setting.game;

import com.qcl.launcher.auth.Account;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.launch.LaunchVersion;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.platform.Bits;
import java.io.File;
import java.util.Iterator;
import net.kdt.pojavlaunch.utils.Architecture;

/* loaded from: classes2.dex */
public class GameLaunchSetting {
    public Account account;
    public String boatRenderer;
    public String controlLayout;
    public String currentVersion;
    public String extraJavaFlags;
    public String extraMinecraftFlags;
    public boolean fullscreen;
    public String gameFileDirectory;
    public String game_directory;
    public String home;
    public String javaPath;
    public boolean log;
    public int maxRam;
    public int minRam;
    public String pojavRenderer;
    public float scaleFactor;
    public String server;
    public boolean touchInjector;

    public GameLaunchSetting(Account account, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, boolean z, float f, String str9, int i, int i2, String str10, String str11, boolean z2, boolean z3) {
        this.account = account;
        this.home = str;
        this.currentVersion = str2;
        this.javaPath = str3;
        this.extraJavaFlags = str4;
        this.extraMinecraftFlags = str5;
        this.game_directory = str6;
        this.boatRenderer = str7;
        this.pojavRenderer = str8;
        this.touchInjector = z;
        this.scaleFactor = f;
        this.minRam = i;
        this.maxRam = i2;
        this.server = str11;
        this.controlLayout = str10;
        this.fullscreen = z2;
        this.log = z3;
        this.gameFileDirectory = str9;
    }

    public static boolean isHighVersion(GameLaunchSetting gameLaunchSetting) {
        return LaunchVersion.fromDirectory(new File(gameLaunchSetting.currentVersion)).minimumLauncherVersion >= 21;
    }

    public static boolean requiresSdl(Version version) {
        return hasLibrary(version, "lwjgl-sdl") && !hasLibrary(version, "lwjgl-glfw");
    }

    private static boolean hasLibrary(Version version, String str) {
        for (Library library : version.getLibraries()) {
            if (library != null && library.is("org.lwjgl", str)) {
                return true;
            }
        }
        Iterator<Version> it = version.getPatches().iterator();
        while (it.hasNext()) {
            if (hasLibrary(it.next(), str)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMinecraft26(String str) {
        return str != null && str.matches("26\\.\\d+(?:\\.\\d+)?(?:-(?:snapshot|pre|rc)-?\\d+)?");
    }

    public static int requiredJava(Version version) {
        int majorVersion = version.getJavaVersion() == null ? 8 : version.getJavaVersion().getMajorVersion();
        if (version.getJavaVersion() == null && (isMinecraft26(version.getId()) || isMinecraft26(version.getInheritsFrom()) || isMinecraft26(version.getJar()) || ("game".equals(version.getId()) && isMinecraft26(version.getVersion())))) {
            majorVersion = 25;
        }
        Iterator<Version> it = version.getPatches().iterator();
        while (it.hasNext()) {
            majorVersion = Math.max(majorVersion, requiredJava(it.next()));
        }
        return majorVersion;
    }

    public static String selectJavaRuntime(int i) {
        if (i > 25) {
            i = 25;
        }
        if (i <= 8) {
            return "default";
        }
        if (i <= 17) {
            return "JRE17";
        }
        if (i <= 21) {
            return "JRE21";
        }
        if (i <= 25) {
            return "JRE25";
        }
        throw new IllegalArgumentException("No bundled Java runtime for Java " + i);
    }

    public static int runtimeMajor(String str) {
        if ("default".equals(str)) {
            return 8;
        }
        if ("JRE17".equals(str)) {
            return 17;
        }
        if ("JRE21".equals(str)) {
            return 21;
        }
        return "JRE25".equals(str) ? 25 : -1;
    }

    public static GameLaunchSetting getGameLaunchSetting(String str, String str2) {
        String str3;
        String str4;
        String str5 = str2;
        LauncherSetting launcherSettingFromFile = GsonUtils.getLauncherSettingFromFile(AppManifest.SETTING_DIR + "/launcher_setting.json");
        PublicGameSetting publicGameSettingFromFile = GsonUtils.getPublicGameSettingFromFile(AppManifest.SETTING_DIR + "/public_game_setting.json");
        PrivateGameSetting privateGameSettingFromFile = GsonUtils.getPrivateGameSettingFromFile(str);
        if (privateGameSettingFromFile.gameDirSetting.type == 0) {
            str3 = launcherSettingFromFile.gameFileDirectory;
        } else if (privateGameSettingFromFile.gameDirSetting.type == 1) {
            str3 = (str5 == null || str5.equals("")) ? publicGameSettingFromFile.currentVersion : str5;
        } else {
            str3 = privateGameSettingFromFile.gameDirSetting.path;
        }
        String str6 = str3;
        if (privateGameSettingFromFile.javaSetting.autoSelect) {
            Version version = (Version) JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, new Artifact.Serializer()).registerTypeAdapter(Bits.class, new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, new Argument.Deserializer()).create().fromJson(FileStringUtils.getStringFromFile(((str5 == null || str5.equals("")) ? publicGameSettingFromFile.currentVersion : str5) + "/" + new File((str5 == null || str5.equals("")) ? publicGameSettingFromFile.currentVersion : str5).getName() + ".json"), Version.class);
            if (version == null) {
                str4 = AppManifest.JAVA_DIR + "/" + privateGameSettingFromFile.javaSetting.name;
            } else {
                str4 = AppManifest.JAVA_DIR + "/" + selectJavaRuntime(requiredJava(version));
            }
        } else {
            str4 = AppManifest.JAVA_DIR + "/" + privateGameSettingFromFile.javaSetting.name;
        }
        String str7 = str4;
        try {
            Architecture.setBitMode(privateGameSettingFromFile.javaSetting.bitMode);
            com.qcl.launcher.utils.Architecture.setBitMode(privateGameSettingFromFile.javaSetting.bitMode);
        } catch (Throwable unused) {
        }
        Account account = publicGameSettingFromFile.account;
        String str8 = publicGameSettingFromFile.home;
        if (str5 == null || str5.equals("")) {
            str5 = publicGameSettingFromFile.currentVersion;
        }
        return new GameLaunchSetting(account, str8, str5, str7, privateGameSettingFromFile.extraJavaFlags, privateGameSettingFromFile.extraMinecraftFlags, str6, privateGameSettingFromFile.boatLauncherSetting.renderer, privateGameSettingFromFile.pojavLauncherSetting.renderer, privateGameSettingFromFile.touchInjector, privateGameSettingFromFile.scaleFactor, launcherSettingFromFile.gameFileDirectory, privateGameSettingFromFile.ramSetting.minRam, privateGameSettingFromFile.ramSetting.maxRam, privateGameSettingFromFile.controlLayout, privateGameSettingFromFile.server, launcherSettingFromFile.fullscreen, privateGameSettingFromFile.log);
    }
}
