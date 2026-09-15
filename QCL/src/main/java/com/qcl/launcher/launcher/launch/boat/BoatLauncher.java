package com.qcl.launcher.launcher.launch.boat;

import android.content.Context;

import com.qcl.launcher.launcher.launch.AccountPatch;
import com.qcl.launcher.launcher.setting.game.GameLaunchSetting;
import com.qcl.launcher.launcher.launch.LaunchVersion;
import com.qcl.launcher.launcher.launch.TouchInjector;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.string.StringUtils;

import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.Tools;

import java.io.File;
import java.util.Collections;
import java.util.Vector;

public class BoatLauncher {

    public static Vector<String> getMcArgs(GameLaunchSetting gameLaunchSetting , Context context,int width,int height,String server){
        try {
            LaunchVersion version = LaunchVersion.fromDirectory(new File(gameLaunchSetting.currentVersion));
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

            boolean highVersion = false;
            if (version.minimumLauncherVersion >= 21){
                highVersion = true;
            }
            String libraryPath;
            String classPath;
            String r = gameLaunchSetting.boatRenderer.equals("VirGL") ? "virgl" : "gl4es";
            boolean isJava8 = javaPath.endsWith("default");
            boolean useCacio17 = !isJava8;
            String javaLibDir = cosine.boat.LoadMe.getJavaLibDir(javaPath);
            // lwjgl-2 原生库按架构放置：assets 里有 lwjgl-2/<abi>/liblwjgl.so，
            // 启动前把当前运行时架构对应的那份复制到 lwjgl-2/liblwjgl.so（ Boat 原始布局只认这个路径）。
            // 缺失对应架构时保留原文件，避免覆盖成错误架构导致 dlopen 失败。
            try {
                int arch = com.qcl.launcher.utils.Architecture.getRuntimeArchitecture();
                String abiDir = arch == com.qcl.launcher.utils.Architecture.ARCH_ARM ? "arm"
                        : arch == com.qcl.launcher.utils.Architecture.ARCH_ARM64 ? "arm64"
                        : arch == com.qcl.launcher.utils.Architecture.ARCH_X86 ? "x86" : "x86_64";
                java.io.File srcSo = new java.io.File(AppManifest.BOAT_LIB_DIR + "/lwjgl-2/" + abiDir + "/liblwjgl.so");
                java.io.File dstSo = new java.io.File(AppManifest.BOAT_LIB_DIR + "/lwjgl-2/liblwjgl.so");
                if (srcSo.isFile()) {
                    java.io.File dstParent = dstSo.getParentFile();
                    if (dstParent != null) dstParent.mkdirs();
                    java.nio.file.Files.copy(srcSo.toPath(), dstSo.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Throwable ignored) {
            }
            if (!highVersion){
                libraryPath = javaLibDir + ":" + AppManifest.BOAT_LIB_DIR + ":" + AppManifest.BOAT_LIB_DIR + "/lwjgl-2:" + AppManifest.BOAT_LIB_DIR + "/renderer/" + r;
                // 远古版本（LWJGL2 时代）音频依赖 paulscode SoundSystem，
                // 上游 classpath 漏了这几个 jar，导致进游戏完全无声 → 这里补上。
                classPath = AppManifest.BOAT_LIB_DIR + "/lwjgl-2/lwjgl.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-2/lwjgl_util.jar:" + AudioLibs.classPath() + version.getClassPath(gameLaunchSetting.gameFileDirectory,false,false);
            }
            else {
                libraryPath = javaLibDir + ":" + AppManifest.BOAT_LIB_DIR + ":" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3:" + AppManifest.BOAT_LIB_DIR + "/renderer/" + r;
                classPath = AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-jemalloc.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-tinyfd.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-opengl.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-openal.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-glfw.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl-stb.jar:" + AppManifest.BOAT_LIB_DIR + "/lwjgl-3/lwjgl.jar:" + version.getClassPath(gameLaunchSetting.gameFileDirectory,true,useCacio17);
            }
            Vector<String> args = new Vector<String>();
            args.add(javaPath + "/bin/java");
            Tools.getCacioJavaArgs(context, args, isJava8, width, height);
            args.add("-cp");
            args.add(classPath);
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Dfml.earlyprogresswindow=false");
            args.add("-Dorg.lwjgl.util.DebugLoader=true");
            args.add("-Dorg.lwjgl.util.Debug=true");
            args.add("-Dos.name=Linux");
            args.add("-Dlwjgl.platform=Boat");
            if (gameLaunchSetting.boatRenderer.equals("VirGL")) {
                args.add("-Dorg.lwjgl.opengl.libname=libGL.so.1");
            }
            else {
                args.add("-Dorg.lwjgl.opengl.libname=libgl4es_114.so");
            }
            args.add("-Dlwjgl.platform=Boat");
            args.add("-Dos.name=Linux");
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
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
            args.add(version.mainClass);
            String[] minecraftArgs;
            minecraftArgs = version.getMinecraftArguments(gameLaunchSetting, highVersion);
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
            return null;
        }
    }

}
