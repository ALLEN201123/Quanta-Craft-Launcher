package com.qcl.launcher.launcher.download.fabric;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.platform.Bits;

import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings("ALL")
public class FabricInstallTask extends AsyncTask<FabricLoaderVersion,Integer, Version> {

    private MainActivity activity;
    private DownloadTaskListAdapter adapter;
    private String mcVersion;
    private InstallFabricCallback callback;

    private DownloadTaskListBean bean;

    /**
     * ★★★★★ 2026-10-17（1.5.2 新增）**Legacy Fabric 开关**。
     *
     * <p>用户需求里点名了「Legacy Fabric」和「Legacy Fabric API」。
     *
     * <p>Legacy Fabric 是 Fabric 官方为 **MC 1.8.9 ~ 1.13.2** 保留的旧分支，
     * 它的 meta 接口与官方 Fabric **完全同构**，只有域名不同：
     * <pre>
     *   官方 Fabric   https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/profile/json
     *   Legacy Fabric https://meta.legacyfabric.net/v2/versions/loader/{mc}/{loader}/profile/json
     * </pre>
     * ⇒ 所以不需要新写一份安装任务，把域名参数化即可
     * （少一份重复代码，也避免两个文件日后各修一半的经典问题）。
     */
    private final boolean legacy;

    /** 是否走 Legacy Fabric（1.8.9~1.13.2 的官方旧分支）。 */
    public boolean isLegacy() {
        return this.legacy;
    }

    /** 官方 Fabric 安装（原有行为，签名不变，避免动到已有调用点）。 */
    public FabricInstallTask(MainActivity activity, DownloadTaskListAdapter adapter, String mcVersion, InstallFabricCallback callback) {
        this(activity, adapter, mcVersion, callback, false);
    }

    /** @param legacy true = 装 Legacy Fabric（1.8.9~1.13.2） */
    public FabricInstallTask(MainActivity activity, DownloadTaskListAdapter adapter, String mcVersion, InstallFabricCallback callback, boolean legacy) {
        this.activity = activity;
        this.adapter = adapter;
        this.mcVersion = mcVersion;
        this.callback = callback;
        this.legacy = legacy;

        bean = new DownloadTaskListBean(activity.getString(R.string.dialog_install_game_install_fabric),"","","");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
        if (!isCancelled()) adapter.addDownloadTask(bean);
    }

    @Override
    protected Version doInBackground(FabricLoaderVersion... fabricLoaderVersions) {
        FabricLoaderVersion fabricVersion = fabricLoaderVersions[0];
        // ★ 1.5.2：官方 Fabric 与 Legacy Fabric 只差域名（接口同构）
        final String metaBase = this.legacy
                ? "https://meta.legacyfabric.net"
                : "https://meta.fabricmc.net";
        String patchUrl = metaBase + "/v2/versions/loader/" + mcVersion + "/" + fabricVersion.version + "/profile/json";
        try {
            String patchStr = NetworkUtils.doGet(NetworkUtils.toURL(patchUrl));
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version patch = gson.fromJson(patchStr,Version.class);
            ArrayList<DownloadTaskListBean> list = new ArrayList<>();
            String head;
            for (Library library : patch.getLibraries()){
                String url;
                if (DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource) == 0) {
                    url = library.getDownload().getUrl();
                }
                else if (DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource) == 1) {
                    head = DownloadUrlSource.getSubUrl(1,DownloadUrlSource.LIBRARIES) + "/";
                    url = head + library.getPath();
                }
                else {
                    head = DownloadUrlSource.getSubUrl(2,DownloadUrlSource.LIBRARIES) + "/";
                    url = head + library.getPath();
                }
                DownloadTaskListBean bean = new DownloadTaskListBean(library.getArtifactFileName(),
                        url,
                        activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(),
                        library.getDownload().getSha1());
                list.add(bean);
            }
            int maxDownloadTask = activity.launcherSetting.maxDownloadTask;
            if (activity.launcherSetting.autoDownloadTaskQuantity) {
                maxDownloadTask = 64;
            }

            DownloadUtil.DownloadMultipleFilesCallback downloadCallback = new DownloadUtil.DownloadMultipleFilesCallback() {
                @Override
                public void onTaskStart(DownloadTaskListBean bean) {
                    if (!isCancelled()) adapter.addDownloadTask(bean);
                }

                @Override
                public void onTaskProgress(DownloadTaskListBean bean) {
                    if (!isCancelled()) adapter.onProgress(bean);
                }

                @Override
                public void onTaskFinish(DownloadTaskListBean bean) {
                    if (!isCancelled()) adapter.onComplete(bean);
                }

                @Override
                public void onFailed(Exception e) {
                    e.printStackTrace();
                    if (!isCancelled()) callback.onFailed(e);
                    cancel(true);
                }
            };

            ArrayList<DownloadTaskListBean> failedFiles = DownloadUtil.downloadMultipleFiles(list, maxDownloadTask, this, activity, downloadCallback);
            if (failedFiles.size() == 0) {
                return patch.setId("fabric").setVersion(fabricVersion.version).setPriority(30000);
            }
            else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("The following files failed to download:");
                for (DownloadTaskListBean bean : failedFiles) {
                    stringBuilder.append("\n\n  " + bean.name);
                }
                Exception e = new Exception(stringBuilder.toString());
                e.printStackTrace();
                if (!isCancelled()) callback.onFailed(e);
                cancel(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (!isCancelled()) callback.onFailed(e);
            cancel(true);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Version version) {
        super.onPostExecute(version);
        if (version == null) {
            callback.onFailed(new Exception("Unknown error"));
        }
        else {
            adapter.onComplete(bean);
            callback.onFinish(version);
        }
    }

    public interface InstallFabricCallback{
        void onStart();
        void onFailed(Exception e);
        void onFinish(Version version);
    }
}
