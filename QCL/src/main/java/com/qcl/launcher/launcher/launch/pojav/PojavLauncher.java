/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.ActivityManager
 *  android.app.ActivityManager$MemoryInfo
 *  android.content.Context
 *  android.os.Build
 *  android.os.Build$VERSION
 *  android.os.Process
 *  android.util.Log
 *  net.kdt.pojavlaunch.Logger
 *  net.kdt.pojavlaunch.utils.JREUtils
 *  net.kdt.pojavlaunch.utils.Tools
 */
package com.qcl.launcher.launcher.launch.pojav;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import com.qcl.launcher.launcher.launch.AccountPatch;
import com.qcl.launcher.launcher.launch.LaunchVersion;
import com.qcl.launcher.launcher.launch.Lwjgl333Helper;
import com.qcl.launcher.launcher.launch.QCLHooks;
import com.qcl.launcher.launcher.launch.TouchInjector;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.File;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.Vector;
import net.kdt.pojavlaunch.Logger;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.Tools;

public class PojavLauncher {
    public static Vector<String> getMcArgs(GameLaunchSetting gameLaunchSetting, Context context, int width, int height, String server) {
        try {
            File jreRelease = new File(gameLaunchSetting.javaPath, "release");
            if (!jreRelease.isFile()) {
                Logger.getInstance((Context)context).appendToLog("\u542f\u52a8\u5931\u8d25\uff1aJava \u8fd0\u884c\u5e93\u4e0d\u5b8c\u6574 \u2014\u2014 \u7f3a\u5c11 " + jreRelease.getAbsolutePath() + "\n\u8bf7\u5230\u300c\u8bbe\u7f6e \u2192 Java \u8fd0\u884c\u65f6\u300d\u91cd\u65b0\u5b89\u88c5\u8be5\u8fd0\u884c\u65f6\uff0c\u6216\u6539\u7528\u5176\u5b83\u7248\u672c\u3002");
                return null;
            }
            JREUtils.jreReleaseList = JREUtils.readJREReleaseProperties((String)gameLaunchSetting.javaPath);
            if (JREUtils.jreReleaseList == null || JREUtils.jreReleaseList.isEmpty()) {
                Logger.getInstance((Context)context).appendToLog("\u542f\u52a8\u5931\u8d25\uff1aJava \u8fd0\u884c\u5e93\u65e0\u6cd5\u89e3\u6790 \u2014\u2014 " + jreRelease.getAbsolutePath() + "\n\u8be5\u6587\u4ef6\u53ef\u80fd\u635f\u574f\uff0c\u8bf7\u91cd\u65b0\u5b89\u88c5\u8fd0\u884c\u65f6\u3002");
                return null;
            }
            LaunchVersion version = LaunchVersion.fromDirectory(new File(gameLaunchSetting.currentVersion));
            if (version == null) {
                Logger.getInstance((Context)context).appendToLog("\u542f\u52a8\u5931\u8d25\uff1a\u7248\u672c\u6587\u4ef6\u635f\u574f\u6216\u7f3a\u5c11 json \u2014\u2014 " + gameLaunchSetting.currentVersion);
                return null;
            }
            LaunchVersion.setLauncherIdentity("QCL", null);
            String javaPath = gameLaunchSetting.javaPath;
            try {
                String jvmArch = JREUtils.getJavaArchName();
                boolean jvm32 = jvmArch.equals("aarch32") || jvmArch.equals("i386");
                boolean device64 = Build.SUPPORTED_64_BIT_ABIS.length > 0;
                boolean app64 = Process.is64Bit();
                StringBuilder sb = new StringBuilder("QCL \u8fd0\u884c\u67b6\u6784: JVM=").append(jvmArch).append(jvm32 ? "\uff0832 \u4f4d\uff0c\u5806\u4e0a\u9650 2048M\uff09" : "\uff0864 \u4f4d\uff0c\u53ef\u7528\u5927\u5185\u5b58\uff09");
                if (device64 && !app64) {
                    sb.append(" | \u63d0\u793a\uff1a\u624b\u673a\u662f 64 \u4f4d\uff0c\u4f46\u5f53\u524d\u88c5\u7684\u662f 32 \u4f4d\u7248\u672c\uff0c\u91cd\u88c5 64 \u4f4d\u7248\u53ef\u89e3\u9501\u66f4\u5927\u5185\u5b58");
                }
                Logger.getInstance((Context)context).appendToLog(sb.toString());
            }
            catch (Throwable jvmArch) {
                // empty catch block
            }
            JREUtils.relocateLibPath((Context)context, (String)javaPath);
            String libraryPath = PojavLauncher.buildFclLibraryPath(context, javaPath) + ":" + JREUtils.LD_LIBRARY_PATH;
            boolean qclNeed333 = Lwjgl333Helper.needs(gameLaunchSetting.currentVersion);
            boolean qclNeed341 = !qclNeed333 && Lwjgl333Helper.needs341(gameLaunchSetting.currentVersion);
            boolean qclNeedsLwjglX = !qclNeed333 && !qclNeed341 && Lwjgl333Helper.needsLwjglX(gameLaunchSetting.currentVersion);
            try {
                System.setProperty("qcl.pojavexec.lib", "pojavexec");
                System.setProperty("qcl.pojavexec.libfile", "libpojavexec.so");
            }
            catch (Throwable sb) {
                // empty catch block
            }
            File qclNatives333 = null;
            File qclNatives341 = null;
            String qclLwjglXDiag = null;
            if (qclNeed333 || qclNeedsLwjglX) {
                qclNatives333 = Lwjgl333Helper.prepare(context);
                if (qclNatives333 != null) {
                    libraryPath = qclNatives333.getAbsolutePath() + ":" + libraryPath;
                }
            } else if (qclNeed341 && (qclNatives341 = Lwjgl333Helper.prepare341(context)) != null) {
                libraryPath = qclNatives341.getAbsolutePath() + ":" + libraryPath;
            }
            try {
                File mgDir = new File(gameLaunchSetting.game_directory, "renderer/mg");
                if (mgDir.isDirectory()) {
                    libraryPath = libraryPath + ":" + mgDir.getAbsolutePath();
                }
            }
            catch (Throwable mgDir) {
                // empty catch block
            }
            boolean isJava8 = javaPath.endsWith("default");
            boolean useCacio17 = !isJava8;
            String classPath = version.getClassPath(gameLaunchSetting.gameFileDirectory, GameLaunchSetting.isHighVersion(gameLaunchSetting), useCacio17);
            if (qclNeed333) {
                String j333 = Lwjgl333Helper.jarsClassPath(context);
                if (j333.length() > 0) {
                    classPath = j333 + ":" + classPath;
                }
            } else if (qclNeed341) {
                String j341 = Lwjgl333Helper.jarsClassPath341(context);
                if (j341.length() > 0) {
                    classPath = j341 + ":" + classPath;
                }
            } else if (qclNeedsLwjglX) {
                String jx = Lwjgl333Helper.jarsClassPath(context, true);
                qclLwjglXDiag = "len=" + jx.length() + " | " + (jx.length() > 300 ? jx.substring(0, 300) : jx);
                if (jx.length() > 0) {
                    classPath = jx + ":" + classPath;
                }
            }
            Vector<String> args = new Vector<String>();
            if (qclLwjglXDiag != null) {
                args.add("-Dqcl.lwjglx=" + qclLwjglXDiag);
            }
            Tools.getCacioJavaArgs((Context)context, args, (boolean)isJava8, (int)width, (int)height);
            if (qclNatives333 != null) {
                args.add("-Dorg.lwjgl.librarypath=" + qclNatives333.getAbsolutePath());
                args.add("-Dorg.lwjgl.glfw.libname=pojavexec");
                args.add("-Dqcl.pojavexec.lib=pojavexec");
                args.add("-Dqcl.pojavexec.libfile=libpojavexec.so");
            }
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Djava.home=" + javaPath);
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
            args.add("-Duser.home=" + new File(gameLaunchSetting.gameFileDirectory).getParent());
            if (qclNatives341 != null) {
                args.add("-Dorg.lwjgl.librarypath=" + qclNatives341.getAbsolutePath());
                args.add("-Dorg.lwjgl.glfw.libname=pojavexec");
                args.add("-Dqcl.pojavexec.lib=pojavexec");
                args.add("-Dqcl.pojavexec.libfile=libpojavexec.so");
            }
            if (qclNeed333 || qclNeed341) {
                args.add("-Djava.locale.providers=COMPAT");
                args.add("-Xint");
                args.add("-Dfile.encoding=UTF-8");
                args.add("-Dstdout.encoding=UTF-8");
                args.add("-Dstderr.encoding=UTF-8");
            } else {
                args.add("-Duser.language=" + System.getProperty("user.language"));
            }
            if (!qclNeed333 && !qclNeed341) {
                try {
                    args.add("-Duser.country=" + Locale.getDefault().getCountry());
                    args.add("-Duser.timezone=" + TimeZone.getDefault().getID());
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
            if (qclNeed333 || qclNeedsLwjglX || qclNeed341) {
                args.add("-Dorg.lwjgl.util.Debug=true");
                args.add("-Dorg.lwjgl.util.DebugLoader=true");
            }
            args.add("-Dos.name=Linux");
            args.add("-Dos.version=Android-" + Build.VERSION.RELEASE);
            args.add("-Dpojav.path.minecraft=" + gameLaunchSetting.gameFileDirectory);
            args.addAll(JREUtils.getJavaArgs((Context)context));
            args.add("-Dnet.minecraft.clientmodname=Quanta Craft Launcher");
            args.add("-Dfml.earlyprogresswindow=false");
            String[] accountArgs = AccountPatch.getAccountArgs(context, gameLaunchSetting.account);
            Collections.addAll(args, accountArgs);
            String[] JVMArgs = version.getJVMArguments(gameLaunchSetting);
            for (int i = 0; i < JVMArgs.length; ++i) {
                if (JVMArgs[i].startsWith("-DignoreList") && !JVMArgs[i].endsWith("," + new File(gameLaunchSetting.currentVersion).getName() + ".jar")) {
                    JVMArgs[i] = JVMArgs[i] + "," + new File(gameLaunchSetting.currentVersion).getName() + ".jar";
                }
                if (JVMArgs[i].startsWith("-DFabricMcEmu") || JVMArgs[i].startsWith("net.minecraft.client.main.Main")) continue;
                args.add(JVMArgs[i]);
            }
            if (qclNeed333 || qclNeed341) {
                try {
                    String qclTmp = context.getCacheDir().getAbsolutePath();
                    args.add("-Djna.tmpdir=" + qclTmp);
                    args.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + qclTmp);
                    args.add("-Dio.netty.native.workdir=" + qclTmp);
                    File qclJna = Lwjgl333Helper.jnaDir(context);
                    if (qclJna.isDirectory() && qclJna.list() != null && qclJna.list().length > 0) {
                        args.add("-Djna.boot.library.path=" + qclJna.getAbsolutePath());
                    } else {
                        args.add("-Djna.boot.library.path=" + context.getApplicationInfo().nativeLibraryDir);
                    }
                    args.add("-Djna.nosys=false");
                }
                catch (Throwable qclTmp) {
                    // empty catch block
                }
            }
            int maxRam = gameLaunchSetting.maxRam;
            int minRam = gameLaunchSetting.minRam;
            String jvmArch = JREUtils.getJavaArchName();
            if ((jvmArch.equals("aarch32") || jvmArch.equals("i386")) && maxRam > 1024) {
                maxRam = 1024;
            }
            try {
                ActivityManager am = (ActivityManager)context.getSystemService("activity");
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                int availMb = (int)(mi.availMem / 1024L / 1024L);
                int dynamicCap = Math.max(512, (int)((double)availMb * 0.7));
                if (maxRam > dynamicCap) {
                    maxRam = dynamicCap;
                }
            }
            catch (Throwable am) {
                // empty catch block
            }
            if (minRam > maxRam) {
                minRam = maxRam;
            }
            args.add("-Xms" + minRam + "M");
            args.add("-Xmx" + maxRam + "M");
            if (!gameLaunchSetting.extraJavaFlags.equals("")) {
                String[] extraJavaFlags = gameLaunchSetting.extraJavaFlags.split(" ");
                Collections.addAll(args, extraJavaFlags);
            }
            System.setProperty("qcl.highver", qclNeed333 ? "1" : "0");
            String qclEffectiveRenderer = gameLaunchSetting.pojavRenderer;
            boolean qclNeedsDesktopGl = qclNeed333 || qclNeed341;
            // SDL3 判定：版本 json 里有 lwjgl-sdl 且没有 lwjgl-glfw → 需要 SDL3 窗口后端
            final boolean qclNeedsSdl = qclHasLwjglLibrary(version, "lwjgl-sdl") && !qclHasLwjglLibrary(version, "lwjgl-glfw");
            System.setProperty("qcl.highver", qclNeedsDesktopGl ? "1" : "0");
            System.setProperty("qcl.renderer.picked", qclEffectiveRenderer);
            args.add("-Dorg.lwjgl.opengl.libname=" + JREUtils.getGraphicsLibrary((String)qclEffectiveRenderer));
            args.add("-Dorg.lwjgl.spvc.libname=spirv-cross-c-shared");
            // ★★★ 1.1.1：SDL3 版本（版本 json 里是 lwjgl-sdl）先挂上 native hooks（bytehook + SDL hook + exit hook）
            if (qclNeedsSdl) {
                QCLHooks.initializeHooks();
            }
            args.add("-cp");
            args.add(classPath);
            args.add(version.mainClass);
            String[] minecraftArgs = version.getMinecraftArguments(gameLaunchSetting, GameLaunchSetting.isHighVersion(gameLaunchSetting));
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
        catch (Exception e) {
            e.printStackTrace();
            try {
                Logger.getInstance((Context)context).appendToLog("\u542f\u52a8\u53c2\u6570\u6784\u9020\u5931\u8d25\uff1a" + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            return null;
        }
    }

    private static String buildFclLibraryPath(Context context, String javaPath) {
        StringBuilder sb = new StringBuilder();
        try {
            String javaLibDir = JREUtils.getJavaLibDir((String)javaPath);
            sb.append(javaLibDir).append(":");
            sb.append(javaLibDir).append("/jli:");
            sb.append(javaLibDir).append("/server:");
            sb.append(javaLibDir).append("/client:");
            File jreDir = new File(javaPath, "jre");
            if (jreDir.isDirectory()) {
                String jreLib = new File(jreDir, "lib/" + JREUtils.getJavaArchName()).getAbsolutePath();
                sb.append(jreLib).append(":");
                sb.append(jreLib).append("/server:");
                sb.append(jreLib).append("/client:");
            }
        }
        catch (Throwable javaLibDir) {
            // empty catch block
        }
        try {
            String libName = JREUtils.getAndroidLibDirName();
            sb.append("/system/").append(libName).append(":");
            sb.append("/vendor/").append(libName).append(":");
            sb.append("/vendor/").append(libName).append("/hw:");
            sb.append("/system_ext/").append(libName).append(":");
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        sb.append(AppManifest.POJAV_LIB_DIR).append("/lwjgl3:");
        try {
            sb.append(context.getApplicationInfo().nativeLibraryDir);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return sb.toString();
    }

    public static String getGlVersion(String currentVersion) {
        LaunchVersion version = LaunchVersion.fromDirectory(new File(currentVersion));
        if (version == null) {
            return "2";
        }
        String creationDate = version.time;
        if (creationDate == null || creationDate.isEmpty()) {
            return "2";
        }
        try {
            return Objects.requireNonNull(new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(creationDate.substring(0, creationDate.indexOf("T")))).before(new Date(111, 6, 7)) ? "1" : "2";
        }
        catch (ParseException exception) {
            Log.e((String)"OPENGL SELECTION", (String)exception.toString());
            return "2";
        }
    }

    /**
     * ★★★ 1.1.1：判断版本 json 的 libraries 里有没有某个 org.lwjgl 构件（如 lwjgl-sdl / lwjgl-glfw）。
     * 注意不能复用 {@code LaunchVersion.getLibraries()} —— 它把 org.lwjgl 开头的库全部排除了。
     */
    private static boolean qclHasLwjglLibrary(LaunchVersion version, String artifact) {
        if (version == null || version.libraries == null) {
            return false;
        }
        for (LaunchVersion.Library lib : version.libraries) {
            if (lib != null && lib.name != null && lib.name.startsWith("org.lwjgl:" + artifact)) {
                return true;
            }
        }
        return false;
    }
}

