package com.qcl.launcher.launcher.download.neoforge;

import android.content.Intent;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.ApiService;
import com.qcl.launcher.launcher.download.forge.ForgeNewInstallProfile;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.utils.io.IOUtils;
import com.qcl.launcher.utils.io.SocketServer;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.platform.Bits;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipFile;

/**
 * ★ NeoForge 安装任务（结构照 {@link com.qcl.launcher.launcher.download.forge.ForgeInstallTask}）。
 *
 * 实测（neoforge-21.1.1-installer.jar 真实结构，2026-10）：
 *  - 安装器本质是 zip，里面有：
 *      {@code version.json}      —— 版本补丁：id=neoforge-21.1.1、inheritsFrom=1.21.1、
 *                                    mainClass=cpw.mods.bootstraplauncher.BootstrapLauncher、
 *                                    arguments(jvm/game) + libraries(带 downloads.artifact.url/sha1/path)
 *      {@code install_profile.json} —— spec=1、minecraft=1.21.1、version=neoforge-21.1.1、
 *                                    json=/version.json、hideExtract=true、
 *                                    data(若干 lzma 补丁，形如 /data/client.lzma)、
 *                                    processors(10 个：jarsplitter/AutoRenamingTool/binarypatcher…)、
 *                                    libraries(88 个，含 processor 用的工具库)
 *  - 最终可启动产物是 processor 的 PATCHED 输出
 *      {@code net.neoforged:neoforge:<ver>:client} → net/neoforged/neoforge/<ver>/neoforge-<ver>-client.jar，
 *    必须由安装器自己跑 processor 才能生成，所以这里**下载完依赖后调用安装器本体**
 *    （复用工程里已有的 forge-install-bootstrapper.jar，com.bangbang93.ForgeInstaller，
 *     它内部走 net.minecraftforge.installer 的 ClientInstall/PostProcessors 逻辑，
     *     NeoForge 安装器里就是这套代码）。
 *
 * 前置条件：目标 MC 版本（inheritsFrom）必须已安装；运行安装器需要 AppManifest.JAVA_DIR 下的 JVM。
 */
public class NeoForgeInstallTask extends AsyncTask<NeoForgeVersion, Integer, String> {

    private final MainActivity activity;
    private final InstallNeoForgeCallback callback;

    private SocketServer exitServer;

    public NeoForgeInstallTask(MainActivity activity, InstallNeoForgeCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    private static String installerDir() {
        return AppManifest.INSTALL_DIR + "/neoforge";
    }

    private static String installerJarPath() {
        return installerDir() + "/neoforge-installer.jar";
    }

    private static String installerUnzipDir() {
        return installerDir() + "/installer";
    }

    /** 回调统一切到主线程，避免调用方在子线程碰 UI */
    private void postProgress(int percent, String message) {
        if (isCancelled()) {
            return;
        }
        activity.runOnUiThread(() -> callback.onProgress(percent, message));
    }

    private void postFailed(Exception e) {
        activity.runOnUiThread(() -> callback.onFailed(e));
    }

    public void cancelBuild() {
        if (exitServer != null) {
            exitServer.stop();
        }
        try {
            activity.stopService(new Intent(activity, ApiService.class));
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
    }

    @Override
    protected String doInBackground(NeoForgeVersion... args) {
        NeoForgeVersion version = args[0];
        String ver = version.getVersion();
        try {
            // ---------- 1. 下载 installer jar ----------
            postProgress(1, activity.getString(R.string.neoforge_install_stage_installer));
            FileUtils.deleteDirectory(installerDir());
            FileUtils.createDirectory(installerDir());

            int source = DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource);
            String installerUrl = NeoForgeVersions.installerUrl(version, source);
            String installerFallback = source == DownloadUrlSource.DOWNLOAD_URL_SOURCE_OFFICIAL
                    ? NeoForgeVersions.bmclapiInstallerUrl(version)
                    : NeoForgeVersions.officialInstallerUrl(version);
            if (!downloadInstaller(installerUrl, installerFallback, installerJarPath())) {
                throw new IOException(activity.getString(R.string.neoforge_error_installer_download));
            }
            if (isCancelled()) {
                return null;
            }

            // ---------- 2. 解压并读取 version.json / install_profile.json ----------
            ZipTools.unzipFile(installerJarPath(), installerUnzipDir(), false);
            String versionJson = readZipEntry(installerJarPath(), "version.json");
            String profileJson = readZipEntry(installerJarPath(), "install_profile.json");
            if (versionJson == null || profileJson == null) {
                throw new IOException(activity.getString(R.string.neoforge_error_incomplete_installer));
            }

            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version patch = gson.fromJson(versionJson, Version.class);
            ForgeNewInstallProfile profile = gson.fromJson(profileJson, ForgeNewInstallProfile.class);
            if (patch == null || profile == null) {
                throw new IOException(activity.getString(R.string.neoforge_error_incomplete_installer));
            }

            String versionId = profile.getVersion() != null && profile.getVersion().length() > 0
                    ? profile.getVersion() : patch.getId();
            String mcVersion = profile.getMinecraft() != null && profile.getMinecraft().length() > 0
                    ? profile.getMinecraft() : patch.getInheritsFrom();

            // ---------- 3. 基础版本必须已安装（NeoForge 通过 inheritsFrom 依赖它） ----------
            File baseJson = new File(activity.launcherSetting.gameFileDirectory + "/versions/" + mcVersion + "/" + mcVersion + ".json");
            if (!baseJson.isFile()) {
                throw new IOException(activity.getString(R.string.neoforge_error_need_base_version).replace("%s", mcVersion));
            }

            // ---------- 4. 下载依赖库（version.json + install_profile.json 两份） ----------
            postProgress(5, activity.getString(R.string.neoforge_install_stage_libs));
            ArrayList<DownloadTaskListBean> list = new ArrayList<>();
            addLibraries(list, patch.getLibraries());
            addLibraries(list, profile.getLibraries());

            int maxDownloadTask = activity.launcherSetting.maxDownloadTask;
            if (activity.launcherSetting.autoDownloadTaskQuantity) {
                maxDownloadTask = 64;
            }
            if (maxDownloadTask < 1) {
                maxDownloadTask = 8;
            }
            final int total = Math.max(list.size(), 1);
            final AtomicInteger done = new AtomicInteger(0);

            DownloadUtil.DownloadMultipleFilesCallback downloadCallback = new DownloadUtil.DownloadMultipleFilesCallback() {
                @Override
                public void onTaskStart(DownloadTaskListBean bean) {
                }

                @Override
                public void onTaskProgress(DownloadTaskListBean bean) {
                }

                @Override
                public void onTaskFinish(DownloadTaskListBean bean) {
                    int now = done.incrementAndGet();
                    int percent = 5 + Math.min(60, now * 60 / total);
                    postProgress(percent, activity.getString(R.string.neoforge_install_stage_libs));
                }

                @Override
                public void onFailed(Exception e) {
                    e.printStackTrace();
                }
            };

            ArrayList<DownloadTaskListBean> failed = DownloadUtil.downloadMultipleFiles(list, maxDownloadTask, this, activity, downloadCallback);
            if (isCancelled()) {
                return null;
            }
            if (failed != null && failed.size() > 0) {
                StringBuilder sb = new StringBuilder(activity.getString(R.string.neoforge_error_download_libs));
                for (DownloadTaskListBean bean : failed) {
                    sb.append("\n  ").append(bean.name);
                }
                throw new IOException(sb.toString());
            }

            // ---------- 5. 调用安装器本体（跑 processor，生成 client jar 与版本 json） ----------
            postProgress(70, activity.getString(R.string.neoforge_install_stage_build));
            int exitCode = runInstaller(versionId);
            if (isCancelled()) {
                return null;
            }
            if (exitCode != 0) {
                throw new IOException(activity.getString(R.string.neoforge_error_build_failed).replace("%d", String.valueOf(exitCode)));
            }

            // ---------- 6. 确认版本 json 落地；安装器没写就自己补一份（best-effort） ----------
            File written = new File(activity.launcherSetting.gameFileDirectory + "/versions/" + versionId + "/" + versionId + ".json");
            if (!written.isFile()) {
                writeFallbackVersionJson(patch, versionId, mcVersion, ver);
            }

            postProgress(100, activity.getString(R.string.neoforge_install_stage_done));
            return versionId;
        } catch (Exception e) {
            e.printStackTrace();
            if (!isCancelled()) {
                postFailed(e);
            }
            cancel(true);
        }
        return null;
    }

    /** 下载 installer jar（首选地址重试 2 次，再换回退地址重试 2 次） */
    private boolean downloadInstaller(String primary, String fallback, String path) {
        String[] urls = new String[]{primary, primary, fallback, fallback};
        for (String url : urls) {
            if (url == null || url.length() == 0) {
                continue;
            }
            try {
                if (DownloadUtil.downloadFile(url, path, null, installerFeedback())) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (isCancelled()) {
                return false;
            }
        }
        return false;
    }

    private DownloadTask.DownloadFeedback installerFeedback() {
        return new DownloadTask.DownloadFeedback() {
            @Override
            public void updateProgress(long curr, long max) {
                int percent = max > 0 ? (int) (Math.min(100, 100 * curr / max) * 5 / 100) : 1;
                postProgress(percent, activity.getString(R.string.neoforge_install_stage_installer));
            }

            @Override
            public void updateSpeed(String speed) {
            }
        };
    }

    private void addLibraries(List<DownloadTaskListBean> out, List<Library> libraries) {
        if (libraries == null) {
            return;
        }
        int source = DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource);
        for (Library library : libraries) {
            String url;
            if (source == DownloadUrlSource.DOWNLOAD_URL_SOURCE_OFFICIAL
                    && library.getDownload().getUrl() != null
                    && library.getDownload().getUrl().length() > 0) {
                url = library.getDownload().getUrl();
            } else {
                url = DownloadUrlSource.getSubUrl(source, DownloadUrlSource.FORGE_LIBRARIES) + "/" + library.getPath();
            }
            out.add(new DownloadTaskListBean(
                    library.getArtifactFileName(),
                    url,
                    activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(),
                    library.getDownload().getSha1()));
        }
    }

    /**
     * 调用安装器本体：Java + forge-install-bootstrapper.jar（com.bangbang93.ForgeInstaller），
     * 退出码通过 ApiService → UDP 6868 回传（与 ForgeInstallTask 同一套机制）。
     */
    private int runInstaller(String versionId) throws IOException {
        String javaPath = AppManifest.JAVA_DIR + "/default";
        ArrayList<String> args = new ArrayList<>();
        args.add(javaPath + "/bin/java");
        args.add("-Djava.io.tmpdir=" + AppManifest.DEFAULT_CACHE_DIR);
        args.add("-Dos.name=Linux");
        args.add("-Djava.library.path=" + javaPath + "/lib/aarch64/jli:" + javaPath + "/lib/aarch64");
        args.add("-classpath");
        args.add(".:" + AppManifest.PLUGIN_DIR + "/installer/forge-install-bootstrapper.jar:" + installerJarPath());
        args.add("com.bangbang93.ForgeInstaller");
        args.add(activity.launcherSetting.gameFileDirectory);
        args.add("-Xms1024M");
        args.add("-Xmx1024M");

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger exit = new AtomicInteger(Integer.MIN_VALUE);
        exitServer = new SocketServer("127.0.0.1", ApiService.API_SERVICE_PORT, (server, msg) -> {
            try {
                exit.set(Integer.parseInt(msg.trim()));
            } catch (Exception ignored) {
            }
            latch.countDown();
            server.stop();
        });
        exitServer.start();

        Intent service = new Intent(activity, ApiService.class);
        service.putStringArrayListExtra("commands", args);
        activity.startService(service);

        try {
            if (!latch.await(2, TimeUnit.HOURS)) {
                cancelBuild();
                throw new IOException(activity.getString(R.string.neoforge_error_build_timeout));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } finally {
            if (exitServer != null) {
                exitServer.stop();
            }
            try {
                activity.stopService(new Intent(activity, ApiService.class));
            } catch (Throwable ignored) {
            }
        }
        return exit.get();
    }

    /**
     * 兜底：安装器没写版本 json 时，用 install_profile 里的 version.json（补丁）
     * 写一份到 versions/<versionId>/ —— 注意缺失 processor 产出的 client jar，
     * 正常流程下不会走到这里。
     */
    private void writeFallbackVersionJson(Version patch, String versionId, String mcVersion, String ver) {
        try {
            FileUtils.createDirectory(activity.launcherSetting.gameFileDirectory + "/versions/" + versionId);
            Version v = patch.setId(versionId);
            if (v.getInheritsFrom() == null || v.getInheritsFrom().length() == 0) {
                v = v.setInheritsFrom(mcVersion);
            }
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            FileStringUtils.writeFile(
                    activity.launcherSetting.gameFileDirectory + "/versions/" + versionId + "/" + versionId + ".json",
                    gson.toJson(v));
        } catch (Throwable ignored) {
        }
    }

    /** 从 zip 里读一个文本条目（UTF-8） */
    private static String readZipEntry(String zipPath, String name) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(zipPath);
            java.util.zip.ZipEntry entry = zip.getEntry(name);
            if (entry == null) {
                return null;
            }
            InputStream in = zip.getInputStream(entry);
            try {
                return IOUtils.readFullyAsString(in, StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        cancelBuild();
    }

    @Override
    protected void onPostExecute(String versionId) {
        super.onPostExecute(versionId);
        if (versionId != null) {
            callback.onFinish(versionId);
        }
    }

    public interface InstallNeoForgeCallback {
        void onStart();

        void onProgress(int percent, String message);

        void onFailed(Exception e);

        void onFinish(String versionId);
    }
}