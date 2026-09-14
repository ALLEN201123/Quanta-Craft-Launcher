package com.tungsten.hmclpe.launcher.setting.game;

import com.google.gson.Gson;
import com.tungsten.hmclpe.auth.Account;
import com.tungsten.hmclpe.launcher.game.Argument;
import com.tungsten.hmclpe.launcher.game.Artifact;
import com.tungsten.hmclpe.launcher.game.RuledArgument;
import com.tungsten.hmclpe.launcher.game.Version;
import com.tungsten.hmclpe.launcher.launch.LaunchVersion;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.launcher.setting.launcher.LauncherSetting;
import com.tungsten.hmclpe.utils.file.FileStringUtils;
import com.tungsten.hmclpe.utils.gson.GsonUtils;
import com.tungsten.hmclpe.utils.gson.JsonUtils;
import com.tungsten.hmclpe.utils.platform.Bits;

import java.io.File;

public class GameLaunchSetting {

    public Account account;
    public String home;
    public String currentVersion;

    public String javaPath;
    public String extraJavaFlags;
    public String extraMinecraftFlags;
    public String game_directory;
    public String boatRenderer;
    public String pojavRenderer;
    public boolean touchInjector;
    public float scaleFactor;
    public int minRam;
    public int maxRam;
    public String server;
    public String controlLayout;
    public boolean fullscreen;
    public boolean log;

    public String gameFileDirectory;

    public GameLaunchSetting(Account account,String home,String currentVersion,String javaPath,String extraJavaFlags,String extraMinecraftFlags,String game_directory,String boatRenderer,String pojavRenderer,boolean touchInjector,float scaleFactor,String gameFileDirectory,int minRam,int maxRam,String controlLayout,String server,boolean fullscreen,boolean log){
        this.account = account;
        this.home = home;
        this.currentVersion = currentVersion;

        this.javaPath = javaPath;
        this.extraJavaFlags = extraJavaFlags;
        this.extraMinecraftFlags = extraMinecraftFlags;
        this.game_directory = game_directory;
        this.boatRenderer = boatRenderer;
        this.pojavRenderer = pojavRenderer;
        this.touchInjector = touchInjector;
        this.scaleFactor = scaleFactor;
        this.minRam = minRam;
        this.maxRam = maxRam;
        this.server = server;
        this.controlLayout = controlLayout;
        this.fullscreen = fullscreen;
        this.log = log;

        this.gameFileDirectory = gameFileDirectory;
    }

    public static boolean isHighVersion(GameLaunchSetting gameLaunchSetting){
        LaunchVersion version = LaunchVersion.fromDirectory(new File(gameLaunchSetting.currentVersion));
        return version.minimumLauncherVersion >= 21;
    }

    public static boolean requiresSdl(Version version) {
        // SDL used alongside GLFW may provide controllers only, not the window.
        boolean sdl = hasLibrary(version, "lwjgl-sdl");
        boolean glfw = hasLibrary(version, "lwjgl-glfw");
        return sdl && !glfw;
    }

    private static boolean hasLibrary(Version version, String artifact) {
        for (com.tungsten.hmclpe.launcher.game.Library library : version.getLibraries()) {
            if (library != null && library.is("org.lwjgl", artifact)) return true;
        }
        for (Version patch : version.getPatches()) {
            if (hasLibrary(patch, artifact)) return true;
        }
        return false;
    }

    private static boolean isMinecraft26(String id) {
        return id != null && id.matches("26\\.\\d+(?:\\.\\d+)?(?:-(?:snapshot|pre|rc)-?\\d+)?");
    }

    public static int requiredJava(Version version) {
        // Explicit metadata wins. The fallback recognizes only Minecraft IDs,
        // never arbitrary loader version numbers or a date-like display name.
        int required = version.getJavaVersion() == null ? 8
                : version.getJavaVersion().getMajorVersion();
        if (version.getJavaVersion() == null && (isMinecraft26(version.getId())
                || isMinecraft26(version.getInheritsFrom())
                || isMinecraft26(version.getJar())
                || ("game".equals(version.getId()) && isMinecraft26(version.getVersion())))) {
            required = 25;
        }
        for (Version patch : version.getPatches()) {
            required = Math.max(required, requiredJava(patch));
        }
        return required;
    }

    public static String selectJavaRuntime(int required) {
        // Java 8 走内置的 `default`（8-arm/8-arm64/8-x86/8-x86_64），老版本必须用它。
        // 注意：不能映射到 JRE21 —— 那是我搞错了，已改回。
        // 26.x（2026 官方命名）用 Java 25；万一元数据声明了 >25，也兜底到 JRE25，别抛异常崩启动器。
        if (required > 25) required = 25;
        if (required <= 8) return "default";
        if (required <= 17) return "JRE17";
        if (required <= 21) return "JRE21";
        if (required <= 25) return "JRE25";
        throw new IllegalArgumentException("No bundled Java runtime for Java " + required);
    }

    public static int runtimeMajor(String name) {
        if ("default".equals(name)) return 8;
        if ("JRE17".equals(name)) return 17;
        if ("JRE21".equals(name)) return 21;
        if ("JRE25".equals(name)) return 25;
        return -1;
    }

    public static GameLaunchSetting getGameLaunchSetting(String privatePath,String v){
        LauncherSetting launcherSetting = GsonUtils.getLauncherSettingFromFile(AppManifest.SETTING_DIR + "/launcher_setting.json");
        PublicGameSetting publicGameSetting = GsonUtils.getPublicGameSettingFromFile(AppManifest.SETTING_DIR + "/public_game_setting.json");
        PrivateGameSetting privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(privatePath);

        String gameDir;
        if (privateGameSetting.gameDirSetting.type == 0){
            gameDir = launcherSetting.gameFileDirectory;
        }
        else if (privateGameSetting.gameDirSetting.type == 1){
            gameDir = (v == null || v.equals("")) ? publicGameSetting.currentVersion : v;
        }
        else {
            gameDir = privateGameSetting.gameDirSetting.path;
        }

        String javaPath;
        if (privateGameSetting.javaSetting.autoSelect){
            String versionJson = FileStringUtils.getStringFromFile(((v == null || v.equals("")) ? publicGameSetting.currentVersion : v) + "/" + (new File(((v == null || v.equals("")) ? publicGameSetting.currentVersion : v))).getName() + ".json");
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version version = gson.fromJson(versionJson, Version.class);
            if (version == null) {
                // Broken or empty version json (interrupted install). Fall back to the runtime the
                // player picked by hand instead of dereferencing a null Version.
                javaPath = AppManifest.JAVA_DIR + "/" + privateGameSetting.javaSetting.name;
            }
            else {
                String runtimeName = selectJavaRuntime(requiredJava(version));
                // 21/25 的主目录（JRE21/JRE25）是 aarch64 构建；其他架构必须用按架构目录
                // （21-arm/21-arm64/21-x86/21-x86_64、25-arm/...），否则 dlopen 64 位 libjvm 直接失败。
                if (runtimeName.equals("JRE21") || runtimeName.equals("JRE25")) {
                    int arch = com.tungsten.hmclpe.utils.Architecture.getRuntimeArchitecture();
                    String suffix = arch == com.tungsten.hmclpe.utils.Architecture.ARCH_ARM ? "arm"
                            : arch == com.tungsten.hmclpe.utils.Architecture.ARCH_ARM64 ? "arm64"
                            : arch == com.tungsten.hmclpe.utils.Architecture.ARCH_X86 ? "x86" : "x86_64";
                    runtimeName = (runtimeName.equals("JRE21") ? "21-" : "25-") + suffix;
                }
                javaPath = AppManifest.JAVA_DIR + "/" + runtimeName;
            }
        }
        else {
            javaPath = AppManifest.JAVA_DIR + "/" + privateGameSetting.javaSetting.name;
        }

        // 把"运行时位数"设置交给底层：0=自动跟随设备（64 位设备自动用 64 位），1=强制 64，2=强制 32
        try {
            net.kdt.pojavlaunch.utils.Architecture.setBitMode(privateGameSetting.javaSetting.bitMode);
            com.tungsten.hmclpe.utils.Architecture.setBitMode(privateGameSetting.javaSetting.bitMode);
        } catch (Throwable ignored) {
        }

        return new GameLaunchSetting(publicGameSetting.account,
                publicGameSetting.home,
                (v == null || v.equals("")) ? publicGameSetting.currentVersion : v,
                javaPath,
                privateGameSetting.extraJavaFlags,
                privateGameSetting.extraMinecraftFlags,
                gameDir,
                privateGameSetting.boatLauncherSetting.renderer,
                privateGameSetting.pojavLauncherSetting.renderer,
                privateGameSetting.touchInjector,
                privateGameSetting.scaleFactor,
                launcherSetting.gameFileDirectory,
                privateGameSetting.ramSetting.minRam,
                privateGameSetting.ramSetting.maxRam,
                privateGameSetting.controlLayout,
                privateGameSetting.server,
                launcherSetting.fullscreen,
                privateGameSetting.log);
    }

}
