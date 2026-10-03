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
import com.qcl.launcher.launcher.setting.RuntimeUtils;
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
import org.lwjgl.glfw.CallbackBridge;

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
            // ★★★ 远古版本音效：JRE8 自带 ALSA 版 libjsound（Android 无后端→没声音），
            // 每次启动前兜底换成 APK 的 OpenAL 版（patchJava 只在装 JRE 时跑，升级不重装就漏了）
            RuntimeUtils.ensureJsound(context, javaPath);
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
                // ★ 2026-09-19 对齐 FCL：mg（MobileGlues）优先从「已安装的插件应用」的
                //   nativeLibraryDir 取库（FCL 的插件渲染器就是这么做的）；<gameDir>/renderer/mg
                //   作为手工放置的兜底。原来只认后者，导致玩家按官方方式装了插件 APK 也检测不到。
                String mgLibDir = com.qcl.launcher.launcher.launch.RendererCompat.resolveRendererLibDir(
                        context, "mg", gameLaunchSetting.currentVersion, gameLaunchSetting.game_directory);
                if (mgLibDir != null) {
                    libraryPath = libraryPath + ":" + mgLibDir;
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
            // ★★★ 1.4.0：远古 applet 版本画面尺寸修复。
            //   launchwrapper 的 AlphaVanillaTweakInjector 把 applet 容器尺寸写死成 854x480，
            //   而 classic（全部版本）与部分 indev 版本的游戏代码完全按容器尺寸渲染 →
            //   画面永远只占屏幕一角（其余大片空白）；infdev/alpha 因为有自调整逻辑才没事。
            //   这里把「修复版」jar（assets/game/qcl_appletfix.jar：容器尺寸改为动态读取，
            //   优先 -Dqcl.applet.width/height，其次 Cacio 屏幕尺寸，最后兜底 854x480）
            //   放到 classpath 最前 —— launchwrapper 的 LaunchClassLoader 会把同名类优先解析到它。
            try {
                String qclFixVer = new File(gameLaunchSetting.currentVersion).getName().toLowerCase();
                boolean qclAppletVer = qclFixVer.startsWith("inf")
                        || qclFixVer.startsWith("in-")
                        || qclFixVer.startsWith("a")
                        || qclFixVer.startsWith("c0.")
                        || qclFixVer.startsWith("pc-")
                        || qclFixVer.startsWith("rd")
                        || qclFixVer.contains("infdev");
                if (qclAppletVer) {
                    File qclFixJar = ensureAppletFixJar(context);
                    if (qclFixJar != null) {
                        classPath = qclFixJar.getAbsolutePath() + ":" + classPath;
                    }
                    // ★★★ 1.4.5：远古版本（indev / infdev）「载入世界」列表修复。
                    //   游戏原逻辑是 new URL("http://<2010年官方服务器>/listmaps.jsp") 取世界列表 ——
                    //   那台服务器早就没了，所以列表永远空白（用户保存完世界也看不到）。
                    //   这里把改好的 net/minecraft/client/c/e.class（run() 改成扫本地 saves/ 目录，
                    //   按修改时间倒序、带时间显示）放到 classpath 最前，让 launchwrapper 优先解析它。
                    //   ⚠️ 必须走普通 classpath，绝不能塞进 -Xbootclasspath/p：
                    //      bootstrap 加载器看不到应用类路径上的 c.i/c.r 等类 → NoClassDefFoundError 直接崩。
                    //   ⚠️ 补丁**按版本选**（只有 7 个版本真的用 FileDialog + .mclevel 存档）；
                    //      没有专属补丁的版本退回默认那份，若那份也不存在则返回 null（照旧启动）。
                    File qclSavesJar = ensureSavesListJar(context, qclFixVer);
                    if (qclSavesJar != null) {
                        classPath = qclSavesJar.getAbsolutePath() + ":" + classPath;
                    }
                }
            }
            catch (Throwable ignoredAppletFix) {
                // 修复 jar 不可用时保持原样启动（画面尺寸退回旧行为），不影响启动
            }
            Vector<String> args = new Vector<String>();
            // ★★★ 1.4.6：启动前自检远古版本运行环境（caciocavallo）。
            //   文件缺失时**这里直接补齐**，否则 Tools.getCacioJavaArgs() 会拼出空的
            //   -Xbootclasspath/p → JVM 报 Unrecognized option 起不来（踩过两次）。
            try {
                if (CacioRuntimeCheck.ensure(context, com.qcl.launcher.manifest.AppManifest.CACIOCAVALLO_DIR)) {
                    Logger.getInstance(context).appendToLog("远古版本运行环境缺失，已自动重新解包");
                }
            } catch (Throwable ignoredCacioCheck) {
            }
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
                // ★★★ 1.1.2 性能优化（照搬 FCL）：移除 -Xint。
                // 旧逻辑对高版本加 -Xint（关闭 JIT 纯解释执行），导致 MC 慢几十倍、直接"卡死人"。
                // FCL 从不对游戏加 -Xint，现代移动 JRE 跑 JIT 完全正常，故删除。
                args.add("-Dfile.encoding=UTF-8");
                args.add("-Dstdout.encoding=UTF-8");
                args.add("-Dstderr.encoding=UTF-8");
            } else {
                args.add("-Duser.language=" + System.getProperty("user.language"));
                // ★★★ 1.3.8（照 FCL）：Java <19 用 sun.*.encoding（stdout/stderr 编码），避免日志中文乱码
                args.add("-Dsun.stdout.encoding=UTF-8");
                args.add("-Dsun.stderr.encoding=UTF-8");
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
                // ★★★ 1.1.2 性能优化（照搬 FCL）：默认关闭 LWJGL 调试模式。
                // 旧逻辑无条件开 Debug=true + DebugLoader=true，会对每个 GL 调用做校验+打日志，
                // 是"卡死人"的头号原因。FCL 这三项全是注释掉的。仅调试开关开启时保留。
                if ("true".equals(System.getProperty("qcl.debug.lwjgl", "false"))) {
                    args.add("-Dorg.lwjgl.util.Debug=true");
                    args.add("-Dorg.lwjgl.util.DebugLoader=true");
                }
            }
            args.add("-Dos.name=Linux");
            args.add("-Dos.version=Android-" + Build.VERSION.RELEASE);
            args.add("-Dpojav.path.minecraft=" + gameLaunchSetting.gameFileDirectory);
            args.addAll(JREUtils.getJavaArgs((Context)context));
            args.add("-Dnet.minecraft.clientmodname=Quanta Craft Launcher");
            args.add("-Dfml.earlyprogresswindow=false");
            // ★★★ 1.3.8：全面对齐 FCL 的 JVM 参数（QCL 原先缺失的补齐）。
            //   动机：高版本装了渲染类 mod（Sodium 等）会崩溃、部分渲染异常；FCL 靠这些参数规避。
            //   ① Sodium 兼容（Sodium issue #2561）：Sodium 启动时校验 LWJGL 版本，
            //      在 Android 上误判为「不兼容版本」→ 直接崩溃。关掉该检查。
            args.add("-Dsodium.checks.issue2561=false");
            //   ② Forge/Fabric 兼容（照 FCL）：忽略证书/补丁不一致，避免加载器启动被拦。
            args.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
            args.add("-Dfml.ignorePatchDiscrepancies=true");
            args.add("-Dloader.disable_forked_guis=true");
            //   ③ 进程/CPU（照 FCL）：子进程用 FORK；显式给核心数，避免 MC 把 CPU 识别成 null。
            args.add("-Djdk.lang.Process.launchMechanism=FORK");
            try {
                args.add("-XX:ActiveProcessorCount=" + Runtime.getRuntime().availableProcessors());
            } catch (Throwable ignoredX) {
            }
            //   ④ 渲染相关（照 FCL）：LWJGL 统一走系统内存分配器；
            //      定制 GLFW stub 不自行初始化 EGL（交给渲染桥）；Vulkan 库名（zink 用）。
            args.add("-Dorg.lwjgl.system.allocator=system");
            args.add("-Dglfwstub.initEgl=false");
            args.add("-Dorg.lwjgl.vulkan.libname=libvulkan.so");
            //   ⑤ log4j2 RCE 漏洞防护（照 FCL）。
            args.add("-Djava.rmi.server.useCodebaseOnly=true");
            args.add("-Dcom.sun.jndi.rmi.object.trustURLCodebase=false");
            args.add("-Dcom.sun.jndi.cosnaming.object.trustURLCodebase=false");
            args.add("-Dlog4j2.formatMsgNoLookups=true");
            //   ⑥ MioLibPatcher（照 FCL 的 -javaagent）：字节码注入把 Sodium 的
            //      isUsingPojavLauncher() 强制返回 false —— Sodium 0.5.13+ 检测到
            //      Pojav 系启动器会执行 "kill code" 直接杀游戏，这是「装了钠就崩」的真凶。
            try {
                File qclLibPatcher = ensureMioLibPatcher(context);
                if (qclLibPatcher != null && qclLibPatcher.isFile() && qclLibPatcher.length() > 0L) {
                    args.add("-javaagent:" + qclLibPatcher.getAbsolutePath());
                }
            } catch (Throwable ignoredP) {
            }
            //   ⑦ FreeType（照 FCL）：高版本字体渲染走独立 so，显式给出 libname。
            try {
                File qclFtn = qclNatives333 != null ? qclNatives333 : qclNatives341;
                if (qclFtn != null) {
                    File qclFtf = new File(qclFtn, "libfreetype.so");
                    if (qclFtf.isFile()) {
                        args.add("-Dorg.lwjgl.freetype.libname=" + qclFtf.getAbsolutePath());
                    }
                }
            } catch (Throwable ignoredF) {
            }
            //   ⑧ 版本 jar / 启动器标识（照 FCL）：供 mod 读取。
            args.add("-Dminecraft.launcher.brand=QCL");
            try {
                args.add("-Dminecraft.launcher.version=" + context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0).versionName);
            } catch (Throwable ignoredV) {
            }
            try {
                String qclVerName = new File(gameLaunchSetting.currentVersion).getName();
                File qclVerJar = new File(gameLaunchSetting.currentVersion, qclVerName + ".jar");
                if (qclVerJar.isFile()) {
                    args.add("-Dminecraft.client.jar=" + qclVerJar.getAbsolutePath());
                }
            } catch (Throwable ignoredJ) {
            }
            //   ⑨ CPU 名（照 FCL，供 OSHI / Mod 读取，避免把 CPU 识别成 null）
            try {
                args.add("-Dcpu.name=" + getSocName());
            } catch (Throwable ignoredCpu) {
            }
            //   ⑩ 32 位设备栈大小（照 FCL）：默认 320KB 会导致 1.13+ StackOverflowError
            try {
                String qclJvmArchName = JREUtils.getJavaArchName();
                if ("aarch32".equals(qclJvmArchName) || "i386".equals(qclJvmArchName)) {
                    args.add("-Xss1m");
                }
            } catch (Throwable ignoredXss) {
            }
            //   ⑪ 1.7.2 Forge 修复（照 FCL）
            try {
                if ("1.7.2".equals(new File(gameLaunchSetting.currentVersion).getName())) {
                    args.add("-Dsort.patch=true");
                }
            } catch (Throwable ignoredF72) {
            }
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
            // ★★★ 1.1.8：远古版本（infdev/alpha/beta/classic）的兼容 Java 参数，自动注入。
            //   ① `-Djava.util.Arrays.useLegacyMergeSort=true`
            //      Java 7+ 的 TimSort 会在旧版排序比较器上抛
            //      "Comparison method violates its general contract!" → 世界加载/进入时崩。
            //   ② `-Dhttp.proxyHost=betacraft.uk`
            //      极老版本的会话/资源请求指向早已废弃的地址，走社区 BetaCraft 代理才能通过验证。
            //   ★ 覆盖所有远古版本（inf-* / in-* / pc-* / a* / b* / c0.* / rd-*）。
            //   ★ 1.4.0 修复：补上 indev（`in-20091223-1459` 这类，前缀是 in- 不是 inf）和
            //     pre-classic（`pc-132011` 这类）——此前这两类前缀一个分支都命中不了，
            //     参数永远不加，表现为「部分远古版本进不了游戏」。
            //   ★ 玩家可在 JVM 参数框（extraJavaFlags）里写 useLegacyMergeSort / proxyHost 覆盖删除。
            try {
                String qclVerArg = new File(gameLaunchSetting.currentVersion).getName().toLowerCase();
                boolean qclIsLegacy = qclVerArg.startsWith("inf")
                        || qclVerArg.startsWith("in-")
                        || qclVerArg.startsWith("a")
                        || qclVerArg.startsWith("b")
                        || qclVerArg.startsWith("c0.")
                        || qclVerArg.startsWith("pc-")
                        || qclVerArg.startsWith("rd")
                        || qclVerArg.contains("infdev");
                boolean qclPlayerOverrides = gameLaunchSetting.extraJavaFlags != null
                        && (gameLaunchSetting.extraJavaFlags.contains("useLegacyMergeSort")
                            || gameLaunchSetting.extraJavaFlags.contains("proxyHost"));
                // ★ 1.4.0：applet 容器尺寸修复的尺寸来源 —— qcl_appletfix.jar 里的
                //   AlphaVanillaTweakInjector 优先读这两个属性（真实渲染 surface 的全屏尺寸）。
                if (qclIsLegacy) {
                    try {
                        args.add("-Dqcl.applet.width=" + CallbackBridge.windowWidth);
                        args.add("-Dqcl.applet.height=" + CallbackBridge.windowHeight);
                    }
                    catch (Throwable ignoredAppletSize) {
                        // empty catch block
                    }
                }
                if (qclIsLegacy && !qclPlayerOverrides) {
                    args.add("-Dhttp.proxyHost=betacraft.uk");
                    args.add("-Djava.util.Arrays.useLegacyMergeSort=true");
                    try {
                        Logger.getInstance((Context)context).appendToLog(
                                "远古版本(" + qclVerArg + ")：已自动注入兼容参数 -Dhttp.proxyHost=betacraft.uk / -Djava.util.Arrays.useLegacyMergeSort=true");
                    }
                    catch (Throwable ignoredLog) {
                        // empty catch block
                    }
                }
            }
            catch (Throwable ignored) {
                // 版本名解析失败就不加，不影响启动
            }
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
            // ★ 说明：外部渲染器（mg）的库能被找到，靠的是**把渲染器库目录并进 LD_LIBRARY_PATH**
            //   （对齐 FCL 的 appendCommonPaths：`sb.append(pluginLibPath + ":")`）。
            //   FCL 并不修改这个 libname，QCL 同样保持原样（见 JREUtils 的 LD_LIBRARY_PATH 处理）。
            args.add("-Dorg.lwjgl.opengl.libname=" + JREUtils.getGraphicsLibrary((String)qclEffectiveRenderer));
            args.add("-Dorg.lwjgl.spvc.libname=spirv-cross-c-shared");
            // ★★★ 音效：对齐 FCL DefaultLauncher，指定 OpenAL 库路径（远古/LWJGL2 时代的 paulscode 走 LWJGL OpenAL）。
            args.add("-Dorg.lwjgl.openal.libname=" + context.getApplicationInfo().nativeLibraryDir + "/libopenal.so");
            // ★★★ 1.1.3：把「当前渲染器的 GL/EGL 库信息」传给 JREUtils（跨模块，用 System property）。
            //   对齐 FCL 的 addRendererEnv*：FCL 给**所有**渲染器都设 POJAVEXEC_EGL / SDL_OPENGL_LIBRARY，
            //   且 SDL_OPENGL_LIBRARY 用**绝对路径**。QCL 原先只在 ng_gl4es / zink 分支设 POJAVEXEC_EGL，
            //   选 mg / opengles2 / virgl / vgpu / freedreno 时该变量缺失 → ≤26.2 的 GLFW 路径
            //   窗口上下文创建失败（实测 `GLFW: Failed to create window context!`）。
            try {
                com.qcl.launcher.launcher.launch.RendererCompat.Info qclRInfo =
                        com.qcl.launcher.launcher.launch.RendererCompat.find((String) qclEffectiveRenderer);
                if (qclRInfo == null) {
                    qclRInfo = com.qcl.launcher.launcher.launch.RendererCompat.find(
                            com.qcl.launcher.launcher.launch.RendererCompat.defaultRendererId());
                }
                if (qclRInfo != null) {
                    String qclLibDir = com.qcl.launcher.launcher.launch.RendererCompat.resolveRendererLibDir(
                            context, qclRInfo.id, gameLaunchSetting.currentVersion, gameLaunchSetting.game_directory);
                    System.setProperty("qcl.renderer.glname", qclRInfo.glName == null ? "" : qclRInfo.glName);
                    System.setProperty("qcl.renderer.eglname", qclRInfo.eglName == null ? "" : qclRInfo.eglName);
                    System.setProperty("qcl.renderer.libdir", qclLibDir == null ? "" : qclLibDir);
                }
            }
            catch (Throwable qclR) {
                // 保持旧行为，不因取渲染器信息失败而影响启动
            }
            // ★★★ 1.1.1：SDL3 版本（版本 json 里是 lwjgl-sdl）先挂上 native hooks（bytehook + SDL hook + exit hook）
            if (qclNeedsSdl) {
                QCLHooks.initializeHooks();
                // ★★★ 1.1.3：hook 挂好之后，必须由**启动器侧（dalvik VM）先**完成 libSDL3.so 的首次加载，
                //   让 SDL 的 JNI_OnLoad 在 dalvik 里执行并 RegisterNatives（libSDL3.so 没有导出 Java_org_libsdl_* 符号）。
                //   否则游戏侧 LWJGL 会抢先加载，JNI_OnLoad 被隔离跳过 → SDL 处于「JNI 未就绪」脏状态
                //   → 之后 dalvik 侧再触发 JNI_OnLoad 即 SIGSEGV（26.3 启动崩溃根因）。
                org.libsdl.app.SdlBridge.preloadSdl3();
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

    /**
     * ★★★ 1.3.8：把 assets/game/MioLibPatcher.jar 解到 filesDir，供 -javaagent 使用。
     * 照 FCL 的 LauncherHelper（从 /assets/game/MioLibPatcher.jar 复制到 PLUGIN_DIR）。
     * 每次启动直接覆盖复制（811KB，代价可忽略），保证随 APK 更新。
     */
    /** ★ 1.4.0：远古 applet 版本画面尺寸修复 jar（assets/game/qcl_appletfix.jar → filesDir）。 */
    private static File ensureAppletFixJar(Context context) {
        try {
            File dst = new File(context.getFilesDir(), "qcl_appletfix.jar");
            try (java.io.InputStream in = context.getAssets().open("game/qcl_appletfix.jar");
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dst)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            return dst.isFile() && dst.length() > 0L ? dst : null;
        } catch (Throwable t) {
            try {
                Logger.getInstance(context).appendToLog("qcl_appletfix.jar 解压失败：" + t);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    /**
     * ★★★ 1.4.5：远古版本「载入世界」列表补丁 jar。
     * <p>里面是改过的 {@code net/minecraft/client/c/e.class}（run() 扫本地 saves/ 目录）
     * 与 {@code net/minecraft/client/d.class}（去掉 2010 年那套在线校验）；放进游戏 classpath 最前即可生效。
     *
     * <p>⚠️ 必须**按版本选文件**：只有"真的用文件对话框 + {@code .mclevel}` 存档"的 7 个版本
     * （{@code in-20100223}、{@code inf-20100227-1433} … {@code inf-20100325-1640}）才有补丁，
     * 其余远古版本走 {@code level.dat}，补丁对它们毫无意义 —— 硬塞反而可能出错。
     * 每个版本的补丁**必须分开打包**：类名相同（都是 {@code net/minecraft/client/c/e}），
     * 但"每帧收尾"方法名不同（{@code f_()} / {@code e_()} / {@code d_()}），
     * 所以不能合并成一个 jar。
     */
    private static File ensureSavesListJar(Context context, String versionName) {
        // ★★★ 只认**精确版本名**，绝不兜底到别的版本的补丁！
        //   历史 bug：原来匹配不到时会退回 qcl_saveslist.jar（那是 in-20100223 的补丁），
        //   把它套到别的版本上 → 类名相同但方法名/字段类型不同 → VerifyError → 存档界面直接崩。
        //   现在匹配不到就返回 null（照旧启动，不做任何注入）。
        String asset = "game/qcl_saves_" + versionName + ".jar";
        try {
            String[] probe = context.getAssets().list("game");
            boolean has = false;
            if (probe != null) {
                for (String s : probe) {
                    if (s.equals("qcl_saves_" + versionName + ".jar")) {
                        has = true;
                        break;
                    }
                }
            }
            if (!has) {
                return null;   // 该版本没有对应补丁 → 不注入
            }
        } catch (Throwable ignored) {
            return null;
        }
        try {
            File dst = new File(context.getFilesDir(), "qcl_saves_" + versionName + ".jar");
            try (java.io.InputStream in = context.getAssets().open(asset);
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dst)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            return dst.isFile() && dst.length() > 0L ? dst : null;
        } catch (Throwable t) {
            try {
                Logger.getInstance(context).appendToLog("存档列表补丁解压失败(" + asset + ")：" + t);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    private static File ensureMioLibPatcher(Context context) {        try {
            File dst = new File(context.getFilesDir(), "MioLibPatcher.jar");
            try (java.io.InputStream in = context.getAssets().open("game/MioLibPatcher.jar");
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dst)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }
            return dst.isFile() && dst.length() > 0L ? dst : null;
        } catch (Throwable t) {
            try {
                Logger.getInstance(context).appendToLog("MioLibPatcher 解压失败：" + t);
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    /**
     * ★★★ 1.3.8：SoC 名（照 FCL FCLauncher.getSocName）—— 优先 getprop ro.soc.model，
     * 拿不到回退 Build.HARDWARE。供 -Dcpu.name 使用（部分 Mod / OSHI 读它识别 CPU）。
     */
    private static String getSocName() {
        String name = null;
        try {
            java.lang.Process process = Runtime.getRuntime().exec("getprop ro.soc.model");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            name = reader.readLine();
            reader.close();
        } catch (Throwable ignored) {
        }
        return (name == null || name.trim().isEmpty()) ? Build.HARDWARE : name;
    }
}

