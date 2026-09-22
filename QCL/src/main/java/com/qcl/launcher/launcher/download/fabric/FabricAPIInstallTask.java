package com.qcl.launcher.launcher.download.fabric;

import android.os.AsyncTask;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;

import java.io.IOException;

public class FabricAPIInstallTask extends AsyncTask<RemoteMod.Version,Integer,Exception> {

    private MainActivity activity;
    private String name;
    private DownloadTaskListAdapter adapter;
    private InstallFabricAPICallback callback;

    private DownloadTaskListBean bean;

    public FabricAPIInstallTask(MainActivity activity, String name, DownloadTaskListAdapter adapter, InstallFabricAPICallback callback) {
        this.activity = activity;
        this.name = name;
        this.adapter = adapter;
        this.callback = callback;

        this.bean = new DownloadTaskListBean(activity.getString(R.string.dialog_install_game_install_fabric_api),"","","");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
        if (!isCancelled()) adapter.addDownloadTask(bean);
    }

    @Override
    protected Exception doInBackground(RemoteMod.Version... versions) {
        RemoteMod.Version fabricAPIVersion = versions[0];
        String path;
        if (PublicGameSetting.isUsingIsolateSetting(activity.launcherSetting.gameFileDirectory + "/versions/" + name)) {
            path = PrivateGameSetting.getGameDir(activity.launcherSetting.gameFileDirectory,activity.launcherSetting.gameFileDirectory + "/versions/" + name, GsonUtils.getPrivateGameSettingFromFile(activity.launcherSetting.gameFileDirectory + "/versions/" + name + "/qcl.cfg").gameDirSetting);
        }
        else {
            path = PrivateGameSetting.getGameDir(activity.launcherSetting.gameFileDirectory,activity.launcherSetting.gameFileDirectory + "/versions/" + name,GsonUtils.getPrivateGameSettingFromFile(AppManifest.SETTING_DIR + "/private_game_setting.json").gameDirSetting);
        }
        String fileName = "fabric-api-" + fabricAPIVersion.getVersion() + ".jar";
        String modPath = path + "/mods/" + fileName;
        // ★ 1.2.7：先走国内镜像，失败自动回退官方地址
        String url = com.qcl.launcher.utils.io.MirrorUtils.rewriteCdn(fabricAPIVersion.getFile().getUrl());
        DownloadTaskListBean bean = new DownloadTaskListBean(fileName, url, modPath, fabricAPIVersion.getFile().getHashes().get("sha1"));
        DownloadTask.DownloadFeedback feedback = new DownloadTask.DownloadFeedback() {
            @Override
            public void updateProgress(long curr, long max) {
                long progress = 100 * curr / max;
                bean.progress = (int) progress;
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.onProgress(bean);
                });
            }

            @Override
            public void updateSpeed(String speed) {

            }
        };
        for (int i = 0;i < 5;i++) {
            try {
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.addDownloadTask(bean);
                });
                if (DownloadUtil.downloadFile(url,modPath,null,feedback)) {
                    activity.runOnUiThread(() -> {
                        if (!isCancelled()) adapter.onComplete(bean);
                    });
                    if (!isCancelled()) return null;
                }
                else {
                    activity.runOnUiThread(() -> {
                        if (!isCancelled()) adapter.onComplete(bean);
                    });
                    if (i == 4) {
                        if (!isCancelled()) return new Exception("Failed to download " + fileName);
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.onComplete(bean);
                });
                if (i == 4) {
                    if (!isCancelled()) return e;
                }
            }
        }
        if (!isCancelled()) return new Exception("Unknown error");
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        adapter.onComplete(bean);
        callback.onFinish(e);
    }

    public interface InstallFabricAPICallback{
        void onStart();
        void onFinish(Exception e);
    }
}
