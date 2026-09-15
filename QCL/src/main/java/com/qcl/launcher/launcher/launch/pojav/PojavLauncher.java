package com.qcl.launcher.launcher.launch.pojav;

import static com.qcl.launcher.launcher.setting.game.GameLaunchSetting.isHighVersion;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.qcl.launcher.launcher.launch.AccountPatch;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.launcher.launch.LaunchVersion;
import com.qcl.launcher.launcher.launch.TouchInjector;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.manifest.info.AppInfo;
import com.qcl.launcher.utils.string.StringUtils;

import net.kdt.pojavlaunch.utils.Tools;
import net.kdt.pojavlaunch.utils.JREUtils;

import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

public class PojavLauncher {

    public static Vector<String> getMcArgs(GameLaunchSetting gameLaunchSetting, Context context,int width,int height,String server){
        try {
            // ⚠️ 这里曾经不留痕迹地把异常吞掉 → 返回 null → Tools.launchMinecraft() 里
            // `args.size()` 抛 NPE 直接把启动器打死，日志里只有一句看不懂的 NPE。
            // 现在缺 release 时给出**可读的**原因（哪个 runtime、哪个文件缺），并且仍然返回 null，
            // 由调用方按 null 处理，不再让它炸到 Tools。
            File jreRelease = new File(gameLaunchSetting.javaPath, "release");
            if (!jreRelease.isFile()) {
                net.kdt.pojavlaunch.Logger.getInstance(context).appendToLog(
                        "启动失败：Java 运行库不完整 —— 缺少 " + jreRelease.getAbsolutePath()
                        + "\n请到「设置 → Java 运行时」重新安装该运行时，或改用其它版本。");
                return null;
            }
            JREUtils.jreReleaseList = JREUtils.readJREReleaseProperties(gameLaunchSetting.javaPath);
            if (JREUtils.jreReleaseList == null || JREUtils.jreReleaseList.isEmpty()) {
                net.kdt.pojavlaunch.Logger.getInstance(context).appendToLog(
                        "启动失败：Java 运行库无法解析 —— " + jreRelease.getAbsolutePath()
                        + "\n该文件可能损坏，请重新安装运行时。");
                return null;
            }
            LaunchVersion version = LaunchVersion.fromDirectory(new File(gameLaunchSetting.currentVersion));
            if (version == null) {
                net.kdt.pojavlaunch.Logger.getInstance(context).appendToLog(
                        "启动失败：版本文件损坏或缺少 json —— " + gameLaunchSetting.currentVersion);
                return null;
            }
            String javaPath = gameLaunchSetting.javaPath;
        // 运行架构自动判定：按应用实际安装的 ABI 决定用 32 位还是 64 位 JVM，
        // 结果写进启动日志（悬浮窗可见）。手机是 64 位却装成 32 位版时给出重装提示。
        try {
            String jvmArch = JREUtils.getJavaArchName();
            boolean jvm32 = jvmArch.equals("aarch32") || jvmArch.equals("i386");
            boolean device64 = android.os.Build.SUPPORTED_64_BIT_ABIS.length > 0;
            boolean app64 = android.os.Process.is64Bit();
            StringBuilder sb = new StringBuilder("QCL 运行架构: JVM=").append(jvmArch)
                    .append(jvm32 ? "（32 位，堆上限 2048M）" : "（64 位，可用大内存）");
            if (device64 && !app64) {
                sb.append(" | 提示：手机是 64 位，但当前装的是 32 位版本，重装 64 位版可解锁更大内存");
            }
            net.kdt.pojavlaunch.Logger.getInstance(context).appendToLog(sb.toString());
        } catch (Throwable ignored) {
        }

            JREUtils.relocateLibPath(context,javaPath);
            String libraryPath = JREUtils.getJavaLibDir(javaPath) + ":" + AppManifest.POJAV_LIB_DIR + "/lwjgl3:" + JREUtils.LD_LIBRARY_PATH + ":" + AppManifest.POJAV_LIB_DIR + "/lwjgl3";
            // 外部渲染器（MobileGlues/MG 等）：把 <游戏目录>/renderer/mg 挂到库路径（用户把渲染器文件放这里）
            try {
                java.io.File mgDir = new java.io.File(gameLaunchSetting.game_directory, "renderer/mg");
                if (mgDir.isDirectory()) {
                    libraryPath = libraryPath + ":" + mgDir.getAbsolutePath();
                }
            } catch (Throwable ignored) {
            }
            boolean isJava8 = javaPath.endsWith("default");
            boolean useCacio17 = !isJava8;
            String classPath = getLWJGL3ClassPath() + ":" + version.getClassPath(gameLaunchSetting.gameFileDirectory,isHighVersion(gameLaunchSetting),useCacio17);
            Vector<String> args = new Vector<String>();
            Tools.getCacioJavaArgs(context, args, isJava8, width, height);
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Djava.home=" + javaPath);
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
            args.add("-Duser.home=" + new File(gameLaunchSetting.gameFileDirectory).getParent());
            // 1.0.9 关键修复：JRE21/25 上用中文 locale 会导致 DateTimeFormatter.<clinit> NPE：
            //   ExceptionInInitializerError → DateTimeTextProvider$LocaleStore.<init> → HashMap.put(null)
            //   （真机 1.20.6 实抓：gui.<clinit> 失败 → main 退出 exit 1 → 等待界面后黑屏）
            // JRE21/25 裁剪 jimage 的 CLDR 实现对 zh locale 异常（JRE17 及以下数据完整不受影响）。
            // 修法：21/25 强制 JVM locale 为 en（游戏内语言由 options.txt 的 lang 决定，与此无关）。
            // JRE17/Java8 维持跟随系统，避免改变既有行为。
            String qclRuntimeName = new File(javaPath).getName();
            int qclRuntimeMajor = GameLaunchSetting.runtimeMajor(qclRuntimeName);
            if (qclRuntimeMajor >= 21) {
                args.add("-Duser.language=en");
                args.add("-Duser.country=US");
            } else {
                args.add("-Duser.language=" + System.getProperty("user.language"));
            }
            args.add("-Dos.name=Linux");
            args.add("-Dos.version=Android-" + Build.VERSION.RELEASE);
            args.add("-Dpojav.path.minecraft=" + gameLaunchSetting.gameFileDirectory);
            args.addAll(JREUtils.getJavaArgs(context));
            args.add("-Dnet.minecraft.clientmodname=" + AppInfo.APP_NAME);
            args.add("-Dfml.earlyprogresswindow=false");
            String[] accountArgs;
            accountArgs = AccountPatch.getAccountArgs(context,gameLaunchSetting.account);
            Collections.addAll(args,accountArgs);
            String[] JVMArgs;
            JVMArgs = version.getJVMArguments(gameLaunchSetting);
            for (int i = 0;i < JVMArgs.length;i++) {
                if (JVMArgs[i].startsWith("-DignoreList") && !JVMArgs[i].endsWith("," + new File(gameLaunchSetting.currentVersion).getName() + ".jar")) {
                    JVMArgs[i] = JVMArgs[i] + "," + new File(gameLaunchSetting.currentVersion).getName() + ".jar";
                }
                if (!JVMArgs[i].startsWith("-DFabricMcEmu") && !JVMArgs[i].startsWith("net.minecraft.client.main.Main")) {
                    args.add(JVMArgs[i]);
                }
            }
            // 32 位 JVM 地址空间有限，堆要 2GB 时 VM 初始化直接失败
            // （实测 "Could not reserve enough space for 2097152KB object heap"），
            // 随后游戏 exit(1) 会把整个启动器进程带走（白屏回主界面）。
            // 因此 32 位运行时把堆夹到 2GB 以内（用户要求从 1GB 提到 2GB）。
            int maxRam = gameLaunchSetting.maxRam;
            int minRam = gameLaunchSetting.minRam;
            String jvmArch = JREUtils.getJavaArchName();
            if (jvmArch.equals("aarch32") || jvmArch.equals("i386")) {
                // 32 位地址空间实测上限（1536 都挤不出来）→ 夹到 1024
                if (maxRam > 1024) maxRam = 1024;
            }
            // 动态：再按"玩家当前剩余内存"夹一次，设置过高时自动降下来，避免 VM 起不来
            try {
                android.app.ActivityManager am = (android.app.ActivityManager)
                        context.getSystemService(android.content.Context.ACTIVITY_SERVICE);
                android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                int availMb = (int) (mi.availMem / 1024 / 1024);
                int dynamicCap = Math.max(512, (int) (availMb * 0.7));
                if (maxRam > dynamicCap) maxRam = dynamicCap;
            } catch (Throwable ignored) {
            }
            if (minRam > maxRam) minRam = maxRam;
            args.add("-Xms" + minRam + "M");
            args.add("-Xmx" + maxRam + "M");
            if (!gameLaunchSetting.extraJavaFlags.equals("")) {
                String[] extraJavaFlags = gameLaunchSetting.extraJavaFlags.split(" ");
                Collections.addAll(args, extraJavaFlags);
            }
            args.add("-Dorg.lwjgl.opengl.libname=" + JREUtils.getGraphicsLibrary(gameLaunchSetting.pojavRenderer));
            args.add("-cp");
            args.add(classPath);
            args.add(version.mainClass);
            String[] minecraftArgs;
            minecraftArgs = version.getMinecraftArguments(gameLaunchSetting, isHighVersion(gameLaunchSetting));
            Collections.addAll(args, minecraftArgs);
            args.add("--width");
            args.add(Integer.toString(width));
            args.add("--height");
            args.add(Integer.toString(height));
            if (StringUtils.isNotBlank(server)) {
                String[] ser = server.split(":");
                args.add("--server");
                args.add(ser[0]);
                args.add("--port");
                args.add(ser.length > 1 ? ser[1] : "25565");
            }
            String[] extraMinecraftArgs = gameLaunchSetting.extraMinecraftFlags.split(" ");
            Collections.addAll(args, extraMinecraftArgs);
            return TouchInjector.rebaseArguments(gameLaunchSetting, args);
        }
        catch (Exception e){
            e.printStackTrace();
            // 把异常打到启动日志窗，否则玩家只看到「启动失败」却不知道为什么。
            try {
                net.kdt.pojavlaunch.Logger.getInstance(context).appendToLog(
                        "启动参数构造失败：" + e.getClass().getSimpleName() + ": " + e.getMessage());
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static String getLWJGL3ClassPath() {
        StringBuilder libStr = new StringBuilder();
        File lwjgl3Folder = new File(AppManifest.POJAV_LIB_DIR, "lwjgl3");
        if (/* info.arguments != null && */ lwjgl3Folder.exists()) {
            for (File file: lwjgl3Folder.listFiles()) {
                if (file.getName().endsWith(".jar")) {
                    libStr.append(file.getAbsolutePath() + ":");
                }
            }
        }
        // Remove the ':' at the end
        libStr.setLength(libStr.length() - 1);
        return libStr.toString();
    }

    public static String getGlVersion(String currentVersion){
        LaunchVersion version = LaunchVersion.fromDirectory(new File(currentVersion));
        if (version == null) {
            return "2";
        }
        String creationDate = version.time;
        if(creationDate == null || creationDate.isEmpty()){
            return "2";
        }
        try {
            return Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(creationDate.substring(0, creationDate.indexOf("T")))).before(new Date(2011-1900, 6, 7)) ? "1" : "2";
        }catch (ParseException exception){
            Log.e("OPENGL SELECTION", exception.toString());
            return "2";
        }
    }

}
