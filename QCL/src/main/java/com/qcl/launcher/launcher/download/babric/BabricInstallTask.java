package com.qcl.launcher.launcher.download.babric;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.utils.platform.Bits;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.utils.io.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * ★ 1.2.3：Babric 自动安装（b1.7.3 用的 Fabric 分支）。
 *
 * ★★ 关键发现：Babric 的 meta 接口和官方 Fabric **形状完全一样**，只是域名不同：
 *     官方 Fabric → https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json
 *     Babric      → https://meta.babric.glass-launcher.net/v2/versions/loader/{mc}/{loader}/profile/json
 *   返回的 profile json 结构一致（id / mainClass / libraries），
 *   所以这套安装逻辑可以**直接照搬 {@code FabricInstallTask}**，只换域名。
 *
 * 实测（2026-09-22）：
 *   loader 列表 32 个，最新稳定版 0.19.5
 *   b1.7.3 + 0.19.5 → id = fabric-loader-0.19.5-b1.7.3
 *                     mainClass = net.fabricmc.loader.impl.launch.knot.KnotClient
 *                     libraries 22 个
 *
 * ★ 注意：Babric 是 Fabric 系（加载器 + 库 + 改 mainClass），
 *   和 Risugami ModLoader「把 class 塞进 jar」不是一回事。
 */
@SuppressWarnings("ALL")
public class BabricInstallTask extends AsyncTask<String, Integer, Version> {

    /** Babric 的 meta 根地址（★ 比 Fabric 多了个 `babric.`） */
    public static final String META_BASE = "https://meta.babric.glass-launcher.net/v2/versions/loader/";

    /** 装完后写在版本目录里的标记文件（供 ModLoaderDetector 判断当前加载器） */
    public static final String MARKER_NAME = ".babric";

    /**
     * ★ 1.2.5：写 Babric 安装标记（版本目录下的 {@code .babric}）。
     *
     * 老代码用的是 {@code File.createNewFile()} —— 结果版本目录里躺着的是一个
     * **0 字节**的空文件，玩家用文件管理器打开版本文件夹会看到「0B 文件」，
     * 以为下载坏了。现在按 ModLoader 标记（{@code .modloader}）同样的做法，
     * 写几行纯文本进去：既照样能被 ModLoaderDetector 认出来是 Babric，
     * 也方便人工排查（版本、加载器版本、时间一目了然）。
     */
    public static void writeMarker(File versionDir, String mcVersion, String loaderVersion,
                                   int libraryCount) {
        try {
            if (versionDir == null) {
                return;
            }
            versionDir.mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("loader=babric\n");
            sb.append("mcVersion=").append(mcVersion == null ? "?" : mcVersion).append('\n');
            sb.append("loaderVersion=").append(loaderVersion == null ? "?" : loaderVersion).append('\n');
            sb.append("libraries=").append(libraryCount >= 0 ? String.valueOf(libraryCount) : "unknown").append('\n');
            sb.append("time=").append(new java.util.Date()).append('\n');
            OutputStream out = new FileOutputStream(new File(versionDir, MARKER_NAME));
            out.write(sb.toString().getBytes("UTF-8"));
            out.close();
        } catch (Throwable ignored) {
        }
    }

    /**
     * ★ 1.2.5：修掉「老版本装出来的 0 字节 .babric」。
     *
     * 1.2.4 及以前用的是 {@code createNewFile()} —— 版本目录里留下的是个空文件，
     * 玩家在文件管理器里看到「0B 文件」以为是坏文件。这里在打开版本设置时
     * 顺手补一次内容（只补 0 字节的，正常文件不动）。
     */
    public static void repairEmptyMarker(File versionDir, String mcVersion, String loaderVersion) {
        try {
            File marker = new File(versionDir, MARKER_NAME);
            if (marker.isFile() && marker.length() == 0) {
                writeMarker(versionDir, mcVersion, loaderVersion, -1);
            }
        } catch (Throwable ignored) {
        }
    }

    private MainActivity activity;
    private DownloadTaskListAdapter adapter;
    private String mcVersion;
    private InstallBabricCallback callback;

    private DownloadTaskListBean bean;

    public BabricInstallTask(MainActivity activity, DownloadTaskListAdapter adapter,
                             String mcVersion, InstallBabricCallback callback) {
        this.activity = activity;
        this.adapter = adapter;
        this.mcVersion = mcVersion;
        this.callback = callback;
        bean = new DownloadTaskListBean(activity.getString(R.string.dialog_install_game_install_fabric), "", "", "");
    }

    /**
     * 取 Babric 可用的加载器版本列表（新的在前）。
     *
     * @return 版本字符串数组；拿不到返回空数组
     */
    public static String[] loaderVersions() {
        try {
            String raw = NetworkUtils.doGet(NetworkUtils.toURL(META_BASE));
            if (raw == null || raw.isEmpty()) {
                return new String[0];
            }
            JsonArray arr = new com.google.gson.JsonParser().parse(raw).getAsJsonArray();
            ArrayList<String> out = new ArrayList<>();
            for (JsonElement e : arr) {
                if (!e.isJsonObject()) {
                    continue;
                }
                JsonElement v = e.getAsJsonObject().get("version");
                if (v != null && !v.isJsonNull()) {
                    out.add(v.getAsString());
                }
            }
            return out.toArray(new String[0]);
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * 取最新的**稳定版**加载器版本；没有就返回列表第一个；再没有返回 null。
     */
    public static String latestStableLoaderVersion() {
        try {
            String raw = NetworkUtils.doGet(NetworkUtils.toURL(META_BASE));
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            JsonArray arr = new com.google.gson.JsonParser().parse(raw).getAsJsonArray();
            String fallback = null;
            for (JsonElement e : arr) {
                if (!e.isJsonObject()) {
                    continue;
                }
                JsonElement v = e.getAsJsonObject().get("version");
                if (v == null || v.isJsonNull()) {
                    continue;
                }
                if (fallback == null) {
                    fallback = v.getAsString();
                }
                JsonElement st = e.getAsJsonObject().get("stable");
                if (st != null && !st.isJsonNull() && st.getAsBoolean()) {
                    return v.getAsString();
                }
            }
            return fallback;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
        if (!isCancelled()) {
            adapter.addDownloadTask(bean);
        }
    }

    @Override
    protected Version doInBackground(String... loaderVersions) {
        String loaderVersion = (loaderVersions == null || loaderVersions.length == 0)
                ? null : loaderVersions[0];
        // ★ 没指定版本就在**后台线程**取最新稳定版（不能在主线程发网络请求）
        if (loaderVersion == null || loaderVersion.isEmpty()) {
            loaderVersion = latestStableLoaderVersion();
        }
        if (loaderVersion == null || loaderVersion.isEmpty()) {
            Exception e = new IOException("拿不到 Babric 的加载器版本，请检查网络。");
            if (!isCancelled()) {
                callback.onFailed(e);
            }
            cancel(true);
            return null;
        }
        String patchUrl = META_BASE + mcVersion + "/" + loaderVersion + "/profile/json";
        try {
            String patchStr = NetworkUtils.doGet(NetworkUtils.toURL(patchUrl));
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version patch = gson.fromJson(patchStr, Version.class);
            ArrayList<DownloadTaskListBean> list = new ArrayList<>();
            for (Library library : patch.getLibraries()) {
                // ★★★ Babric 的库（lwjgl-2.9.4-babric.1 / intermediary-upstream-b1.7.3 /
                //   log4j-config 等）是 Babric 自己的产物，**BMCL 镜像上根本没有**！
                //   官方 Fabric 那套「按下载源把地址换成 BMCL 域名」在这里会把地址换坏
                //   （换成的 bmclapi2.bangbang93.com/maven/... 全 404）。
                //   所以这里**永远用 profile json 里自带的原始地址**（指向 maven.glass-launcher.net），
                //   不做任何源切换。
                String url = library.getDownload().getUrl();
                DownloadTaskListBean lb = new DownloadTaskListBean(library.getArtifactFileName(),
                        url,
                        activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(),
                        library.getDownload().getSha1());
                list.add(lb);
            }
            int maxDownloadTask = activity.launcherSetting.maxDownloadTask;
            if (activity.launcherSetting.autoDownloadTaskQuantity) {
                maxDownloadTask = 64;
            }

            DownloadUtil.DownloadMultipleFilesCallback downloadCallback = new DownloadUtil.DownloadMultipleFilesCallback() {
                @Override
                public void onTaskStart(DownloadTaskListBean bean) {
                    if (!isCancelled()) {
                        adapter.addDownloadTask(bean);
                    }
                }

                @Override
                public void onTaskProgress(DownloadTaskListBean bean) {
                    if (!isCancelled()) {
                        adapter.onProgress(bean);
                    }
                }

                @Override
                public void onTaskFinish(DownloadTaskListBean bean) {
                    if (!isCancelled()) {
                        adapter.onComplete(bean);
                    }
                }

                @Override
                public void onFailed(Exception e) {
                    e.printStackTrace();
                    if (!isCancelled()) {
                        callback.onFailed(e);
                    }
                    cancel(true);
                }
            };

            ArrayList<DownloadTaskListBean> failedFiles =
                    DownloadUtil.downloadMultipleFiles(list, maxDownloadTask, this, activity, downloadCallback);
            if (failedFiles.size() == 0) {
                // ★ id 用 "babric"，和 Fabric("fabric") 区分开
                return patch.setId("babric").setVersion(loaderVersion).setPriority(30000);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("The following files failed to download:");
                for (DownloadTaskListBean b : failedFiles) {
                    sb.append("\n\n  " + b.name);
                }
                Exception e = new Exception(sb.toString());
                e.printStackTrace();
                if (!isCancelled()) {
                    callback.onFailed(e);
                }
                cancel(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (!isCancelled()) {
                callback.onFailed(e);
            }
            cancel(true);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Version version) {
        super.onPostExecute(version);
        if (version == null) {
            callback.onFailed(new Exception("Unknown error"));
        } else {
            adapter.onComplete(bean);
            callback.onFinish(version);
        }
    }

    public interface InstallBabricCallback {
        void onStart();

        void onFailed(Exception e);

        void onFinish(Version version);
    }
}
