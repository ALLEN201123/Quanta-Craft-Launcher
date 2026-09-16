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
            // ★ 1.1.0：老版本（LWJGL 2 时代，b1.x / 1.7.x 等）的 native 支持。
            // 原本只有 Boat 后端做了这件事（拷 lwjgl-2/<abi>/liblwjgl.so + 加 libraryPath），
            // Pojav 后端缺失 -> 老版本报 "[LWJGL] Failed to load a library" 后黑屏。
            // 判断：版本 json 依赖 org.lwjgl:lwjgl:2.9.x。
            final boolean qclNeedLwjgl2 = qclNeedsLwjgl2(gameLaunchSetting.currentVersion);
            if (qclNeedLwjgl2) {
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
                libraryPath = libraryPath + ":" + AppManifest.BOAT_LIB_DIR + "/lwjgl-2";
            }
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
            String classPath;
            if (qclNeedLwjgl2) {
                // 老版本：LWJGL 2 的类 + 音频（Paulscode SoundSystem，上游 classpath 漏了）
                classPath = AppManifest.BOAT_LIB_DIR + "/lwjgl-2/lwjgl.jar:"
                        + AppManifest.BOAT_LIB_DIR + "/lwjgl-2/lwjgl_util.jar:"
                        + com.qcl.launcher.launcher.launch.boat.AudioLibs.classPath() + ":"
                        + version.getClassPath(gameLaunchSetting.gameFileDirectory,false,false);
            } else {
                classPath = getLWJGL3ClassPath() + ":" + version.getClassPath(gameLaunchSetting.gameFileDirectory,isHighVersion(gameLaunchSetting),useCacio17);
            }
            Vector<String> args = new Vector<String>();
            // ★ 1.1.0 隔离：只有需要新栈的高版本（1.20.5+）才启用 cacio17
            Tools.qclUseCacio17 = qclNeedsLwjgl333(gameLaunchSetting.currentVersion);
            Tools.getCacioJavaArgs(context, args, isJava8, width, height);
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Djava.home=" + javaPath);
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
            args.add("-Duser.home=" + new File(gameLaunchSetting.gameFileDirectory).getParent());
            // 1.1.0（测试中）：JRE21 裁剪 jimage 在 zh locale 下 DateTimeFormatter.<clinit> NPE
            // （gui.<clinit> → HashMap.put(null)），实测强制 en 可稳定通过。
            // 待真机验证后再决定最终形态（跟 FCL 完全对齐 = 不传）。
            args.add("-Duser.language=en");
            args.add("-Duser.country=US");
            // 1.1.0：MC 的 gui.<clinit> 在某些环境触发 DateTimeFormatter.<clinit> 的 CLDR
            // 数据路径 NPE（LocaleStore 收到 null value）；改用 JDK 的 legacy COMPAT
            // locale provider 数据源可绕开该路径（设备实测 COMPAT 可用）。
            args.add("-Djava.locale.providers=COMPAT");
            // 1.0.9 修复：不传 -Duser.language（对齐 FCL）。
            // 之前继承自 HMCL-PE 的 `-Duser.language=系统值`（中文设备=zh）会在 JRE21/25 的
            // 裁剪 jimage 上触发 CLDR 的 DateTimeFormatter 初始化 NPE（1.20.5+/26.x 黑屏真因）。
            // FCL 的默认 JVM 参数里根本没有这一项，JVM 用默认 locale 即可正常启动。
            // 游戏内语言由 options.txt 的 lang 决定，与 JVM locale 无关。
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
            // 1.1.0：对齐 FCL 的关键 JVM 参数。
            // -XX:ActiveProcessorCount 显式限制 JVM 看到的 CPU 数 → 决定 JIT 编译线程数与
            // 类初始化并发度。MC 的 gui.<clinit> → DateTimeFormatter.<clinit> 的 NPE
            // 高度符合「多线程类初始化竞态」特征（同环境独立复现全 PASS，仅 MC 真身触发）。
            try {
                args.add("-XX:ActiveProcessorCount=" + Runtime.getRuntime().availableProcessors());
            } catch (Throwable ignored) {
            }
            if (jvmArch.equals("aarch32") || jvmArch.equals("i386")) {
                // FCL：32 位设备线程栈调到 1m，防 1.13+ 的 StackOverflowError
                args.add("-Xss1m");
            }
            args.add("-Dloader.disable_forked_guis=true");
            args.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
            args.add("-Dfml.ignorePatchDiscrepancies=true");
            args.add("-Djdk.lang.Process.launchMechanism=FORK");
            // ★ 1.1.0 关键对齐（FCL DefaultLauncher）：显式设置 JVM 三件套编码。
            // Java 19+ 起 stdout/stderr 编码独立于 file.encoding（JEP 400），不显式设置时
            // 会使用 Android native 编码 —— 可能影响类初始化期间字符串/资源数据处理
            // （MC gui.<clinit> → DateTimeFormatter.<clinit> 的 CLDR 数据路径 NPE）。
            args.add("-Dfile.encoding=UTF-8");
            args.add("-Dstdout.encoding=UTF-8");
            args.add("-Dstderr.encoding=UTF-8");
            // FCL：让 MC 能自动定位版本 jar（部分资源/校验逻辑依赖它）
            try {
                args.add("-Dminecraft.client.jar=" + new File(gameLaunchSetting.currentVersion, new File(gameLaunchSetting.currentVersion).getName() + ".jar").getAbsolutePath());
            } catch (Throwable ignored) {
            }
            // (-Xint 竞态验证已完成，1.1.0 回退：不再强加解释模式，恢复 JIT 性能)
            if (!gameLaunchSetting.extraJavaFlags.equals("")) {
                String[] extraJavaFlags = gameLaunchSetting.extraJavaFlags.split(" ");
                Collections.addAll(args, extraJavaFlags);
            }
            // ★★ 1.1.0 架构：新参数只对「需要 LWJGL 3.3.3 的高版本（1.20.5+）」启用，
            // 完全不影响老版本/远古版本（它们用 Pojav 原版的 LWJGL 3.2.3 + LWJGL 2 兼容方案）。
            // 判断方式：读版本 json，看是否有 org.lwjgl:lwjgl:3.3.x 的依赖。
            boolean qclNeedNewLwjgl = qclNeedsLwjgl333(gameLaunchSetting.currentVersion);
            if (qclNeedNewLwjgl) {
                try {
                    String qclRuntimeTmp = context.getCacheDir().getAbsolutePath();
                    // json 里遗留的空值参数（HMCL-PE 时代）会让 MC 的 oshi/JNA 初始化失败，
                    // 必须放在 JVM 参数区（mainClass 之前）覆盖。
                    args.add("-Djna.tmpdir=" + qclRuntimeTmp);
                    args.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + qclRuntimeTmp);
                    args.add("-Dio.netty.native.workdir=" + qclRuntimeTmp);
                    // JNA 用 APK 自带的 Android 版 libjnidispatch.so（避免从 jar 解压 Linux 版）
                    args.add("-Djna.boot.library.path=" + context.getApplicationInfo().nativeLibraryDir);
                    // LWJGL 的 native 加载路径（FCL 同款）
                    args.add("-Dorg.lwjgl.librarypath=" + context.getApplicationInfo().nativeLibraryDir);
                    // 定制 GLFW stub 从配置读库名，必须指向 libpojavexec.so
                    args.add("-Dorg.lwjgl.glfw.libname=pojavexec");
                } catch (Throwable ignored) {
                }
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

    /**
     * 1.1.0：判断该版本是否需要 LWJGL 3.3.3 的新方案。
     * 只有 1.20.5+ 的版本（其版本 json 依赖 org.lwjgl:lwjgl:3.3.3）才需要：
     *   - 新的 native 路径参数（jna/lwjgl.librarypath 等）
     *   - 定制编译的 GLFW stub（pojavexec 接口）
     * 老版本（3.2.3 / LWJGL 2）继续走 Pojav 原版方案，不受任何影响。
     */
    /** 1.1.0：判断该版本是否需要 LWJGL 2（老版本 b1.x / 1.7.x 等）。 */
    private static boolean qclNeedsLwjgl2(String versionPath) {
        if (versionPath == null) return false;
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) return false;
            String content = Tools.read(new java.io.FileInputStream(json));
            return content.contains("lwjgl/2.9") || content.contains("lwjgl-2.9");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean qclNeedsLwjgl333(String versionPath) {
        if (versionPath == null) return false;
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) return false;
            String content = Tools.read(new java.io.FileInputStream(json));
            // 只认 3.3.3 及以上（1.20.5+ 的依赖）
            return content.contains("lwjgl/3.3.3") || content.contains("lwjgl/3.3.4")
                || content.contains("lwjgl/3.3.5") || content.contains("lwjgl/3.4.0")
                || content.contains("lwjgl/3.4.1");
        } catch (Throwable ignored) {
            return false;
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
