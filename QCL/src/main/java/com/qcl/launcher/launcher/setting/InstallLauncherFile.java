/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.content.Intent
 *  android.os.Bundle
 *  android.util.Log
 *  net.kdt.pojavlaunch.utils.Architecture
 *  org.apache.commons.io.FileUtils
 *  org.apache.commons.io.IOUtils
 */
package com.qcl.launcher.launcher.setting;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.setting.RuntimeInstallActivity;
import com.qcl.launcher.launcher.setting.RuntimeUtils;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.AssetsUtils;
import com.qcl.launcher.utils.file.FileStringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Objects;
import net.kdt.pojavlaunch.utils.Architecture;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.qcl.launcher.R;
public class InstallLauncherFile {
    public static void checkLauncherFiles(RuntimeInstallActivity activity) {
        AssetsUtils.ProgressCallback progressCallback = progress -> activity.runOnUiThread(() -> {
            activity.loadingProgress.setProgress(progress);
            activity.loadingProgressText.setText((CharSequence)(progress + " %"));
        });
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_plugin)));
        // ★ 2026-10-09：4 个 plugin 目录也改成**并发**（原来一条条串行）
        {
            final String[][] pluginSpec = {
                    {"plugin/installer", AppManifest.PLUGIN_DIR + "/installer"},
                    {"plugin/touch", AppManifest.PLUGIN_DIR + "/touch"},
                    {"plugin/login/authlib-injector", AppManifest.PLUGIN_DIR + "/login/authlib-injector"},
                    {"plugin/login/nide8auth", AppManifest.PLUGIN_DIR + "/login/nide8auth"}};
            java.util.List<String[]> pairs = new java.util.ArrayList<>();
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (String[] s : pluginSpec) {
                if (InstallLauncherFile.needsPluginCopy(activity, s[0], s[1])) {
                    com.qcl.launcher.utils.file.FileUtils.deleteDirectory(s[1]);
                    pairs.add(s);
                    labels.add(activity.getString(R.string.loading_hint_plugin));
                }
            }
            if (!pairs.isEmpty()) {
                final AssetsUtils pluginAu = AssetsUtils.getInstance((Context) activity);
                pluginAu.setProgressCallback(null);
                Runnable[] jobs = new Runnable[pairs.size()];
                for (int i = 0; i < pairs.size(); i++) {
                    jobs[i] = InstallLauncherFile.copyJob(pluginAu, pairs.get(i)[0], pairs.get(i)[1]);
                }
                InstallLauncherFile.runParallel(activity, labels.toArray(new String[0]), jobs, progressCallback);
            }
        }
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_control)));
        // ★ 2026-09-19：默认控键布局按 info.json 内容比对更新（只动 Default，玩家自建布局不受影响）。
        InstallLauncherFile.syncDefaultControl(activity);
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_lib)));
        if (!new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").exists() || Integer.parseInt(Objects.requireNonNull(FileStringUtils.getStringFromFile(AppManifest.DEFAULT_RUNTIME_DIR + "/version"))) < Integer.parseInt(Objects.requireNonNull(AssetsUtils.readAssetsTxt((Context)activity, "app_runtime/version")))) {
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.BOAT_LIB_DIR);
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.POJAV_LIB_DIR);
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.CACIOCAVALLO_DIR);
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.CACIOCAVALLO17_DIR);
            if (new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").exists()) {
                new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").delete();
            }
            // ★ Boat 后端已彻底移除：assets/app_runtime/ 下没有 boat 目录，
            //   原来这里会无条件拷贝一次，属确定的死路径（copyOnMainThread 对不存在的路径会静默失败），已删除。
            //   BOAT_LIB_DIR 的 deleteDirectory 仍保留，用于清理历史安装残留。
            // ★ 2026-10-09 用户要求：这 4 个目录**并发**解压（原来是一条条串行）。
            //   它们互不重叠，可以一起跑；进度由 runParallel 按「完成个数」统一上报。
            final AssetsUtils au = AssetsUtils.getInstance((Context) activity);
            au.setProgressCallback(null);   // 并发期别让共享字节计数互相踩
            InstallLauncherFile.runParallel(activity,
                    new String[]{
                            activity.getString(R.string.loading_hint_lib),
                            activity.getString(R.string.loading_hint_lib),
                            activity.getString(R.string.loading_hint_lib),
                            activity.getString(R.string.loading_hint_lib)},
                    new Runnable[]{
                            InstallLauncherFile.copyJob(au, "app_runtime/pojav", AppManifest.POJAV_LIB_DIR),
                            InstallLauncherFile.copyJob(au, "app_runtime/caciocavallo", AppManifest.CACIOCAVALLO_DIR),
                            InstallLauncherFile.copyJob(au, "app_runtime/caciocavallo17", AppManifest.CACIOCAVALLO17_DIR),
                            InstallLauncherFile.copyJob(au, "app_runtime/version", AppManifest.DEFAULT_RUNTIME_DIR + "/version")},
                    progressCallback);
        }
        // ★ 2026-10-09：4 个 JRE 也**并发**装 —— 各自写各自的目录（default / JRE17 / JRE21 / JRE25），
        //   互不干扰。这是首次安装里最慢的一段，并行后能省一大半时间。
        InstallLauncherFile.runParallel(activity,
                new String[]{
                        activity.getString(R.string.loading_hint_java_8),
                        activity.getString(R.string.loading_hint_java_17),
                        activity.getString(R.string.loading_hint_java_21),
                        activity.getString(R.string.loading_hint_java_25)},
                new Runnable[]{
                        () -> InstallLauncherFile.checkJava8(activity, null),
                        () -> InstallLauncherFile.checkJava17(activity, null),
                        () -> InstallLauncherFile.checkJava21(activity, null),
                        () -> InstallLauncherFile.checkJava25(activity, null)},
                progressCallback);
        activity.runOnUiThread(() -> InstallLauncherFile.enterLauncher(activity));
    }

    public static void checkBaseFiles(RuntimeInstallActivity activity) {
        AssetsUtils.ProgressCallback progressCallback = progress -> activity.runOnUiThread(() -> {
            activity.loadingProgress.setProgress(progress);
            activity.loadingProgressText.setText((CharSequence)(progress + " %"));
        });
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_plugin)));
        InstallLauncherFile.copyPluginIfNeeded(activity, progressCallback, "plugin/installer", AppManifest.PLUGIN_DIR + "/installer");
        InstallLauncherFile.copyPluginIfNeeded(activity, progressCallback, "plugin/touch", AppManifest.PLUGIN_DIR + "/touch");
        InstallLauncherFile.copyPluginIfNeeded(activity, progressCallback, "plugin/login/authlib-injector", AppManifest.PLUGIN_DIR + "/login/authlib-injector");
        InstallLauncherFile.copyPluginIfNeeded(activity, progressCallback, "plugin/login/nide8auth", AppManifest.PLUGIN_DIR + "/login/nide8auth");
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_control)));
        // ★ 2026-09-19：默认控键布局按 info.json 内容比对更新（只动 Default，玩家自建布局不受影响）。
        InstallLauncherFile.syncDefaultControl(activity);
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_lib)));
        if (!new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").exists() || Integer.parseInt(Objects.requireNonNull(FileStringUtils.getStringFromFile(AppManifest.DEFAULT_RUNTIME_DIR + "/version"))) < Integer.parseInt(Objects.requireNonNull(AssetsUtils.readAssetsTxt((Context)activity, "app_runtime/version")))) {
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.BOAT_LIB_DIR);
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.POJAV_LIB_DIR);
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.CACIOCAVALLO_DIR);
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.CACIOCAVALLO17_DIR);
            if (new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").exists()) {
                new File(AppManifest.DEFAULT_RUNTIME_DIR + "/version").delete();
            }
            // ★ Boat 后端已彻底移除：assets/app_runtime/ 下没有 boat 目录，
            //   原来这里会无条件拷贝一次，属确定的死路径（copyOnMainThread 对不存在的路径会静默失败），已删除。
            //   BOAT_LIB_DIR 的 deleteDirectory 仍保留，用于清理历史安装残留。
            AssetsUtils.getInstance((Context)activity).setProgressCallback(progressCallback).copyOnMainThread("app_runtime/version", AppManifest.DEFAULT_RUNTIME_DIR + "/version");
        }
    }

    /**
     * 同步默认控键布局：把 assets 的 control/Default 与设备上的一份按 info.json 内容比对，
     * 不一致就重新复制（修复历史版本按钮坐标 bug 的分发通道：F5 偏下、F8/F12 绝对坐标归零等）。
     * 只动 Default 一个目录，玩家自建布局不受影响。静默执行，可在任意线程调用。
     */
    public static void syncDefaultControl(Context context) {
        try {
            String assetControlInfo = AssetsUtils.readAssetsTxt(context, "control/Default/info.json");
            String localControlInfo = FileStringUtils.getStringFromFile(AppManifest.CONTROLLER_DIR + "/Default/info.json");
            if (assetControlInfo != null && !assetControlInfo.equals(localControlInfo)) {
                android.util.Log.i("jrelog", "[控键布局] 检测到默认布局版本变化，重新复制 Default");
                com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.CONTROLLER_DIR + "/Default");
                AssetsUtils.getInstance(context).copyOnMainThread("control/Default", AppManifest.CONTROLLER_DIR + "/Default");
            }
        } catch (Throwable t) {
            android.util.Log.w("jrelog", "[控键布局] 默认布局同步失败", t);
        }
    }

    private static void copyPluginIfNeeded(RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback, String assetDir, String targetDir) {
        File versionFile = new File(targetDir, "version");
        if (versionFile.exists()) {
            try {
                int installed = Integer.parseInt(Objects.requireNonNull(FileStringUtils.getStringFromFile(versionFile.getAbsolutePath())).trim());
                int expected = Integer.parseInt(Objects.requireNonNull(AssetsUtils.readAssetsTxt((Context)activity, assetDir + "/version")).trim());
                if (installed >= expected) {
                    return;
                }
            }
            catch (Throwable throwable) {}
        } else if (new File(targetDir).isDirectory()) {
            // empty if block
        }
        com.qcl.launcher.utils.file.FileUtils.deleteDirectory(targetDir);
        AssetsUtils.getInstance((Context)activity).setProgressCallback(callback).copyOnMainThread(assetDir, targetDir);
    }

    @SuppressLint(value={"SetTextI18n"})
    /**
     * ★ 2026-10-09 用户要求：首次那个「解压并安装」页必须**并发**跑。
     *
     * <p>FCL 就是并发解压；QCL 原来把 4 个 plugin + 4 个 runtime 目录 + 4 个 JRE
     * **排成一条队一个个来**，首次安装慢得离谱。
     *
     * <p>并发度取 {@code min(4, CPU 核数)}；进度按「已完成个数」统一上报，不再依赖
     * {@code AssetsUtils} 那套共享的字节计数（多线程会互相踩）。任一任务失败会在全部结束后抛出。
     */
    private static void runParallel(final RuntimeInstallActivity activity, final String[] labels,
                                    final Runnable[] jobs, final AssetsUtils.ProgressCallback cb) {
        int threads = Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors()));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        final java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicReference<Throwable> firstError =
                new java.util.concurrent.atomic.AtomicReference<>();
        for (int i = 0; i < jobs.length; i++) {
            final Runnable job = jobs[i];
            final String label = (labels != null && i < labels.length) ? labels[i] : null;
            pool.execute(() -> {
                try {
                    if (label != null) {
                        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence) label));
                    }
                    job.run();
                } catch (Throwable t) {
                    firstError.compareAndSet(null, t);
                } finally {
                    final int p = done.incrementAndGet() * 100 / jobs.length;
                    try {
                        activity.runOnUiThread(() -> {
                            activity.loadingProgress.setProgress(p);
                            activity.loadingProgressText.setText((CharSequence) (p + " %"));
                        });
                        if (cb != null) {
                            cb.onProgress(p);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        }
        pool.shutdown();
        try {
            pool.awaitTermination(60L, java.util.concurrent.TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
        Throwable t = firstError.get();
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t != null) {
            throw new RuntimeException(t);
        }
    }

    /** 把一个 (assets 源, 目标) 的运行时目录复制包装成可并发执行的 Runnable。 */
    private static Runnable copyJob(final AssetsUtils au, final String src, final String dst) {
        return () -> {
            try {
                au.copyRuntimeOrThrow(src, dst);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** 某个 plugin 目录是否**需要重新复制**（版本号低于 assets 里的版本，或根本没装）。 */
    private static boolean needsPluginCopy(Context ctx, String assetDir, String targetDir) {
        try {
            File vf = new File(targetDir, "version");
            if (vf.exists()) {
                int installed = Integer.parseInt(
                        Objects.requireNonNull(FileStringUtils.getStringFromFile(vf.getAbsolutePath())).trim());
                int expected = Integer.parseInt(
                        Objects.requireNonNull(AssetsUtils.readAssetsTxt(ctx, assetDir + "/version")).trim());
                return installed < expected;
            }
        } catch (Throwable ignored) {
        }
        return true;   // 目录/版本文件不存在，或读不到版本 → 需要装
    }

    public static void checkJava8(RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback) {
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_java_8)));
        InstallLauncherFile.installJava8(activity, callback);
    }

    private static void installJava8(final RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback) {
        String targetPath = AppManifest.JAVA_DIR + "/default";
        String srcDir = "app_runtime/java/jre8";
        try {
            if (RuntimeUtils.isLatest((Context)activity, targetPath, srcDir) && new File(targetPath, "bin/java").exists()) {
                return;
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        try {
            RuntimeUtils.installJava((Context)activity, targetPath, srcDir, InstallLauncherFile.deviceArchName(), new RuntimeUtils.InstallListener(){

                @Override
                public void onUpdate(String detail) {
                }

                @Override
                public void onProgress(int percent) {
                    activity.runOnUiThread(() -> {
                        activity.loadingProgress.setProgress(percent);
                        activity.loadingProgressText.setText((CharSequence)(percent + " %"));
                    });
                }

                @Override
                public void onStage(String stageKey) {
                    if ("patching".equals(stageKey)) {
                        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_java_patching)));
                    }
                }
            });
            InstallLauncherFile.unpack200(activity.getApplicationContext().getApplicationInfo().nativeLibraryDir, targetPath);
            InstallLauncherFile.postPrepare((Context)activity, "default");
        }
        catch (IOException e) {
            Log.e((String)"MULTIRT", (String)"Unable to prepare default(Java 8)", (Throwable)e);
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public static void checkJava17(RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback) {
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_java_17)));
        InstallLauncherFile.installModernJava(activity, callback, "JRE17", "jre17");
    }

    @SuppressLint(value={"SetTextI18n"})
    public static void checkJava21(RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback) {
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_java_21)));
        InstallLauncherFile.installModernJava(activity, callback, "JRE21", "jre21");
    }

    @SuppressLint(value={"SetTextI18n"})
    public static void checkJava25(RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback) {
        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_java_25)));
        InstallLauncherFile.installModernJava(activity, callback, "JRE25", "jre25");
    }

    private static String deviceArchName() {
        int arch = com.qcl.launcher.utils.Architecture.getRuntimeArchitecture();
        if (arch == com.qcl.launcher.utils.Architecture.ARCH_ARM) {
            return "arm";
        }
        if (arch == com.qcl.launcher.utils.Architecture.ARCH_ARM64) {
            return "arm64";
        }
        if (arch == com.qcl.launcher.utils.Architecture.ARCH_X86) {
            return "x86";
        }
        return "x86_64";
    }

    private static void installModernJava(final RuntimeInstallActivity activity, AssetsUtils.ProgressCallback callback, String targetName, String assetName) {
        String arch = InstallLauncherFile.deviceArchName();
        if ("jre25".equals(assetName) && "x86".equals(arch)) {
            return;
        }
        String srcDir = "app_runtime/java/" + assetName;
        String targetPath = AppManifest.JAVA_DIR + "/" + targetName;
        try {
            if (RuntimeUtils.isLatest((Context)activity, targetPath, srcDir) && new File(targetPath, "lib/modules").exists()) {
                return;
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        try {
            RuntimeUtils.installJava((Context)activity, targetPath, srcDir, arch, new RuntimeUtils.InstallListener(){

                @Override
                public void onUpdate(String detail) {
                }

                @Override
                public void onProgress(int percent) {
                    activity.runOnUiThread(() -> {
                        activity.loadingProgress.setProgress(percent);
                        activity.loadingProgressText.setText((CharSequence)(percent + " %"));
                    });
                }

                @Override
                public void onStage(String stageKey) {
                    if ("patching".equals(stageKey)) {
                        activity.runOnUiThread(() -> activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_java_patching)));
                    }
                }
            });
            InstallLauncherFile.unpack200(activity.getApplicationContext().getApplicationInfo().nativeLibraryDir, targetPath);
            InstallLauncherFile.postPrepare((Context)activity, targetName);
        }
        catch (IOException e) {
            Log.e((String)"MULTIRT", (String)("Unable to prepare " + targetName), (Throwable)e);
        }
    }

    private static void unpack200(String nativeLibraryDir, String runtimePath) {
        File basePath = new File(runtimePath);
        Collection<File> files = FileUtils.listFiles((File)basePath, (String[])new String[]{"pack"}, (boolean)true);
        File workdir = new File(nativeLibraryDir);
        ProcessBuilder processBuilder = new ProcessBuilder(new String[0]).directory(workdir);
        for (File jarFile : files) {
            try {
                Process process = processBuilder.command("./libunpack200.so", "-r", jarFile.getAbsolutePath(), jarFile.getAbsolutePath().replace(".pack", "")).start();
                process.waitFor();
            }
            catch (IOException | InterruptedException e) {
                Log.e((String)"MULTIRT", (String)"Failed to unpack the runtime !");
            }
        }
    }

    public static void postPrepare(Context context, String name) throws IOException {
        File dest = new File(AppManifest.JAVA_DIR, "/" + name);
        if (!dest.exists()) {
            return;
        }
        String libFolder = "lib";
        String arch = "";
        if (Architecture.getRuntimeArchitecture() == Architecture.ARCH_ARM) {
            arch = "aarch32";
        }
        if (Architecture.getRuntimeArchitecture() == Architecture.ARCH_ARM64) {
            arch = "aarch64";
        }
        if (Architecture.getRuntimeArchitecture() == Architecture.ARCH_X86) {
            arch = "i386";
        }
        if (Architecture.getRuntimeArchitecture() == Architecture.ARCH_X86_64) {
            arch = "amd64";
        }
        if (new File(dest, libFolder + "/" + arch).exists()) {
            libFolder = libFolder + "/" + arch;
        }
        File ftIn = new File(dest, libFolder + "/libfreetype.so.6");
        File ftOut = new File(dest, libFolder + "/libfreetype.so");
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            ftIn.renameTo(ftOut);
        }
        InstallLauncherFile.copyDummyNativeLib(context, "libawt_xawt.so", dest, libFolder);
    }

    private static void copyDummyNativeLib(Context ctx, String name, File dest, String libFolder) throws IOException {
        File fileLib = new File(dest, "/" + libFolder + "/" + name);
        fileLib.delete();
        FileInputStream is = new FileInputStream(new File(ctx.getApplicationInfo().nativeLibraryDir, name));
        FileOutputStream os = new FileOutputStream(fileLib);
        IOUtils.copy((InputStream)is, (OutputStream)os);
        is.close();
        os.close();
    }

    @SuppressLint(value={"SetTextI18n"})
    public static void enterLauncher(RuntimeInstallActivity activity) {
        activity.loadingText.setText((CharSequence)activity.getString(R.string.loading_hint_ready));
        activity.loadingProgress.setProgress(100);
        activity.loadingProgressText.setText((CharSequence)"100 %");
        Intent intent = new Intent((Context)activity, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("fullscreen", activity.launcherSetting.fullscreen);
        intent.putExtras(bundle);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void enterRuntimeInstall(RuntimeInstallActivity activity) {
        activity.startActivity(new Intent((Context)activity, RuntimeInstallActivity.class));
        activity.finish();
    }
}

