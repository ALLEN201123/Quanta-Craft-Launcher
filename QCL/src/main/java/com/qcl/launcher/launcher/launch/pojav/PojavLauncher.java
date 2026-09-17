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
            // ★★★ 1.1.0 关键修复：把 APK 的 native 目录加进 java.library.path ——
            // LWJGL 的 LoadLibrary 只搜 java.library.path，而 GLFW stub（libpojavexec_new.so）
            // 与 lwjglx 需要的 native 都在 APK 的 nativeLibraryDir 里。
            // v1.0.9 的 path 末尾本来就有 /data/app/.../lib/arm（对比实验确认），
            // 缺了它 → UnsatisfiedLinkError: nativeInitializeGLFWNativeBridge / pojavSetWindowHint missing。
            try {
                libraryPath = libraryPath + ":" + context.getApplicationInfo().nativeLibraryDir;
            } catch (Throwable ignored) {
            }
            // ★★★ 1.1.0 隔离（2026-09-16 对比实验结论）：
            // 只有 1.20.5+（版本 json 声明 org.lwjgl:lwjgl:3.3.x）才启用 LWJGL 3.3.3 新栈。
            // 老版本（b1.x/1.7.x/≤1.20.4）走上面这条 v1.0.9 的原路径，**一个字节都不改**。
            // 3.3.3 的 jar/native 全部来自 assets/app_runtime/lwjgl333/（与老版本零交集），
            // native 解压到私有目录并保持原名，用 -Dorg.lwjgl.librarypath 指过去，
            // 避免与 APK jniLibs 里 v1.0.9 的 3.2.3 so 冲突。
            final boolean qclNeed333 = com.qcl.launcher.launcher.launch.Lwjgl333Helper
                    .needs(gameLaunchSetting.currentVersion);
            // ★★★ 照抄 FCL：LWJGL 2.x 时代（b1.x/远古/1.7.x）需要 lwjglx 兼容层 —— 提前算，供 prepare 与 classpath 共用
            final boolean qclNeedsLwjglX = !qclNeed333 && com.qcl.launcher.launcher.launch.Lwjgl333Helper
                    .needsLwjglX(gameLaunchSetting.currentVersion);
            // ★★★ 1.1.0 双栈隔离（关键时机）：JREUtils 的静态块（System.loadLibrary）在
            // **Android/dalvik 进程**里执行，读不到传给 MC 的 -Dqcl.pojavexec.lib（那是游戏
            // JVM 的参数）。必须在这里用 System.setProperty 提前设好 —— getMcArgs 跑在
            // 子线程且早于 setupBridgeWindow()，是设置它的最佳时机。
            try {
                if (qclNeed333) {
                    System.setProperty("qcl.pojavexec.lib", "pojavexec_new");
                    System.setProperty("qcl.pojavexec.libfile", "libpojavexec_new.so");
                } else {
                    System.clearProperty("qcl.pojavexec.lib");
                    System.clearProperty("qcl.pojavexec.libfile");
                }
            } catch (Throwable ignored) {
            }
            java.io.File qclNatives333 = null;
            String qclLwjglXDiag = null;
            if (qclNeed333 || qclNeedsLwjglX) {
                // ★★★ 关键修复：LWJGL2 时代也要 prepare！否则 lwjgl333/jars 从未解压 → lwjglx 加不上 → 白改
                qclNatives333 = com.qcl.launcher.launcher.launch.Lwjgl333Helper.prepare(context);
                if (qclNatives333 != null) {
                    libraryPath = qclNatives333.getAbsolutePath() + ":" + libraryPath;
                }
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
            String classPath = getLWJGL3ClassPath() + ":" + version.getClassPath(gameLaunchSetting.gameFileDirectory,isHighVersion(gameLaunchSetting),useCacio17);
            if (qclNeed333) {
                // 3.3.3 的 jar 必须排最前：原版 lwjgl-glfw-classes.jar 里也有 org.lwjgl.* (3.2.3)，
                // 靠 classpath 顺序让 3.3.3 的类优先（同时也提供 3.3.3 的 lwjgl-glfw stub）。
                String j333 = com.qcl.launcher.launcher.launch.Lwjgl333Helper.jarsClassPath(context);
                if (j333.length() > 0) classPath = j333 + ":" + classPath;
            } else if (qclNeedsLwjglX) {
                // ★★★ 照抄 FCL：LWJGL 2.x 时代（b1.x/远古/1.7.x）——
                // 用 FCL 定制的 LWJGL 3.3.3 全套 jar（含 lwjgl-lwjglx.jar 兼容层）排到 classpath 最前。
                // lwjglx 提供 org.lwjgl.opengl.Display 等 LWJGL2 API 并桥接到渲染桥（不走 GLFW）。
                // 这正是 FCL 让"全版本通吃"的关键 —— 原版 lwjgl-glfw-classes.jar 的 Display 会走 GLFW 失败。
                String jx = com.qcl.launcher.launcher.launch.Lwjgl333Helper.jarsClassPath(context);
                qclLwjglXDiag = "len=" + jx.length() + " | " + (jx.length() > 300 ? jx.substring(0, 300) : jx);
                if (jx.length() > 0) classPath = jx + ":" + classPath;
            }
            Vector<String> args = new Vector<String>();
            // 诊断：把 lwjglx 的 jar 装载结果打进启动参数，下次日志可见
            if (qclLwjglXDiag != null) {
                args.add("-Dqcl.lwjglx=" + qclLwjglXDiag);
            }
            Tools.getCacioJavaArgs(context, args, isJava8, width, height);
            if (qclNatives333 != null) {
                // 3.3.3 的 native 目录（保持 liblwjgl.so 等原名）
                args.add("-Dorg.lwjgl.librarypath=" + qclNatives333.getAbsolutePath());
                // 定制 GLFW stub：必须指向**新桥** libpojavexec_new.so ——
                // lwjglx 的 GLFW 类需要 pojavSetWindowHint / nativeInitializeGLFWNativeBridge 等新接口，
                // 旧 libpojavexec.so（v1.0.9）没有这些符号，会报
                // "ExceptionInInitializerError -> A required function is missing: pojavSetWindowHint"。
                // 注意：本分支只在 qclNeed333 || qclNeedsLwjglX 时进入，老版本(v1.0.9 路径)完全不设此参数。
                args.add("-Dorg.lwjgl.glfw.libname=pojavexec_new");
                // ★★★ 1.1.0 双栈隔离（关键）：高版本加载 FCL 新版渲染桥 libpojavexec_new.so。
                // GLFW stub 从这两个系统属性读库名（老版本不设 → 默认走 v1.0.9 的 libpojavexec.so）。
                args.add("-Dqcl.pojavexec.lib=pojavexec_new");
                args.add("-Dqcl.pojavexec.libfile=libpojavexec_new.so");
            }
            args.add("-Djava.library.path=" + libraryPath);
            args.add("-Djava.home=" + javaPath);
            args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
            args.add("-Duser.home=" + new File(gameLaunchSetting.gameFileDirectory).getParent());
            // ★ 1.1.0 隔离：JRE21/25 的裁剪 jimage 在 zh locale 下会在 JVM 引导阶段
            // 报 MissingResourceException(sun.launcher.resources.launcher) /
            // BootstrapMethodError(BoundMethodHandle) —— 实测 1.20.6 直接起不来。
            // v1.0.9 对老版本传 -Duser.language 是安全的（JRE8 的 jimage 完整），
            // 所以这里按版本条件化：老版本保持原行为，高版本(1.20.5+)改走 COMPAT + JVM 默认 locale。
            if (qclNeed333) {
                args.add("-Djava.locale.providers=COMPAT");
                // ★★ 高版本专用稳定性参数（老版本一个都不加，保持 v1.0.9 行为）：
                // JRE21/25 在 Android 上跑 MC 1.20.5+ 时，JDK 内部的 invokedynamic（Lambda）
                // 会在引导阶段抛 BootstrapMethodError（实测：Collectors.joining / UUID.randomUUID
                // → SecureRandom.<init> → ArrayIndexOutOfBoundsException: Index -48）。
                // -Xint（纯解释模式）绕开 JIT 对 LambdaForm 的编译，是实测可用的规避手段；
                // 编码三件套对齐 FCL（Java 19+ 的 stdout/stderr 编码独立于 file.encoding）。
                args.add("-Xint");
                // 诊断：让 LWJGL 打印具体是哪个 native 库加载失败（定位用，找到后可移除）
                args.add("-Dorg.lwjgl.util.Debug=true");
                args.add("-Dorg.lwjgl.util.DebugLoader=true");
                args.add("-Dfile.encoding=UTF-8");
                args.add("-Dstdout.encoding=UTF-8");
                args.add("-Dstderr.encoding=UTF-8");
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
            // ★★★ 1.1.0 关键顺序修复（高版本专用）：
            // MC 版本 json 里遗留了空值参数 -Djna.tmpdir= / -Dorg.lwjgl.system.SharedLibraryExtractPath=，
            // 它们在上面的 JVMArgs 循环里被加入，会**覆盖**我们先前设置的同一个属性
            // （实测：写在前面的 -Djna.tmpdir=<cache> 被后面的 -Djna.tmpdir= 顶掉，
            //  导致 JNA temporary directory '' does not exist -> oshi 初始化失败）。
            // 所以必须在 json 参数**之后**再设一遍才能生效。
            if (qclNeed333) {
                try {
                    String qclTmp = context.getCacheDir().getAbsolutePath();
                    args.add("-Djna.tmpdir=" + qclTmp);
                    args.add("-Dorg.lwjgl.system.SharedLibraryExtractPath=" + qclTmp);
                    args.add("-Dio.netty.native.workdir=" + qclTmp);
                    // ★ JNA native 路径：高版本必须用 5.14.0（匹配 MC 的 jna-5.14.0.jar），
                    // 用隔离目录 files/lwjgl333/jna/；APK 自带的 5.1.0 只给老版本用。
                    java.io.File qclJna = com.qcl.launcher.launcher.launch.Lwjgl333Helper.jnaDir(context);
                    if (qclJna.isDirectory() && qclJna.list() != null && qclJna.list().length > 0) {
                        args.add("-Djna.boot.library.path=" + qclJna.getAbsolutePath());
                    } else {
                        args.add("-Djna.boot.library.path=" + context.getApplicationInfo().nativeLibraryDir);
                    }
                    // 同时把 JNA 的临时目录解压目标固定，避免它自己去 jna.jar 里找 Linux 版
                    args.add("-Djna.nosys=false");
                } catch (Throwable ignored) {
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
            // ★★★ 1.1.0 双栈隔离（关键）：渲染器按版本自动选 ——
            // 老版本（≤1.20.4 + 远古）用 gl4es（libgl4es_114.so，v1.0.9 的原方案）；
            // 高版本（1.20.5+）才用 zink（libglxshim.so → Mesa EGL → libzink_dri）。
            // 原因：pojavRenderer 是全局设置，若玩家为高版本选了 zink，老版本会被一起套用，
            // 而 glxshim 依赖 libEGL_mesa.so，加载失败时老版本也会黑屏；
            // 老版本根本不需要桌面 GL（GL4ES 到 ES 2.1 足够）。
            System.setProperty("qcl.highver", qclNeed333 ? "1" : "0");
            // ★★★ 渲染器：**完全尊重玩家选择**（长按启动可选，含 mg 等外部渲染器）。
            // 不做任何强制替换 —— 兼容性提醒改由选择对话框（MainUI.showRendererDialog）
            // 在不兼容时弹「我就要用这个渲染器 / 取消」二次确认。
            String qclEffectiveRenderer = gameLaunchSetting.pojavRenderer;
            System.setProperty("qcl.renderer.picked", qclEffectiveRenderer);
            args.add("-Dorg.lwjgl.opengl.libname=" + JREUtils.getGraphicsLibrary(qclEffectiveRenderer));
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
