package com.tungsten.hmclpe.launcher.launch.check;

import android.os.AsyncTask;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.gson.Gson;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.game.Argument;
import com.tungsten.hmclpe.launcher.game.Artifact;
import com.tungsten.hmclpe.launcher.game.AssetIndex;
import com.tungsten.hmclpe.launcher.game.AssetIndexInfo;
import com.tungsten.hmclpe.launcher.game.AssetObject;
import com.tungsten.hmclpe.launcher.game.Library;
import com.tungsten.hmclpe.launcher.game.RuledArgument;
import com.tungsten.hmclpe.launcher.game.Version;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListAdapter;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListBean;
import com.tungsten.hmclpe.launcher.setting.game.PrivateGameSetting;
import com.tungsten.hmclpe.launcher.uis.game.download.DownloadUrlSource;
import com.tungsten.hmclpe.task.DownloadTask;
import com.tungsten.hmclpe.utils.file.FileStringUtils;
import com.tungsten.hmclpe.utils.file.FileUtils;
import com.tungsten.hmclpe.utils.gson.GsonUtils;
import com.tungsten.hmclpe.utils.gson.JsonUtils;
import com.tungsten.hmclpe.utils.io.NetworkUtils;
import com.tungsten.hmclpe.utils.platform.Bits;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CheckLibTask extends AsyncTask<RecyclerView,Integer,Exception> {

    private final MainActivity activity;
    private final String launchVersion;
    private final CheckLibCallback callback;

    public CheckLibTask (MainActivity activity, String launchVersion, CheckLibCallback callback) {
        this.activity = activity;
        this.launchVersion = launchVersion;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
    }

    @Override
    protected Exception doInBackground(RecyclerView... recyclerViews) {
        PrivateGameSetting privateGameSetting;
        String settingPath = launchVersion + "/hmclpe.cfg";
        if (new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile(settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile(settingPath).enable)) {
            privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(settingPath);
        }
        else {
            privateGameSetting = activity.privateGameSetting;
        }
        if (privateGameSetting.notCheckMinecraft) {
            return null;
        }
        String versionJson = FileStringUtils.getStringFromFile(launchVersion + "/" + new File(launchVersion).getName() + ".json");
        Gson gson = JsonUtils.defaultGsonBuilder()
                .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                .registerTypeAdapter(Bits.class, new Bits.Serializer())
                .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                .create();
        Version version = gson.fromJson(versionJson, Version.class);
        if (version == null) {
            // 归档/手拷版本可能只剩一个 jar：用归档启动模板自动重建版本 json 再继续，
            // 缺的库/资源由下面的下载清单补齐并展示进度，而不是直接报错让玩家重装。
            try {
                String id = new File(launchVersion).getName();
                File jar = new File(launchVersion, id + ".jar");
                if (jar.isFile()) {
                    String rebuilt = com.tungsten.hmclpe.launcher.download.game.LegacyArchiveInstallTask
                            .buildLegacyJson(activity, id, "");
                    FileStringUtils.writeFile(launchVersion + "/" + id + ".json", rebuilt);
                    version = gson.fromJson(rebuilt, Version.class);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (version == null) {
                // Gson returns null (instead of throwing) for an empty or half-written version json,
                // which used to take the whole launch down with a NullPointerException.
                return new Exception(activity.getString(R.string.launch_check_dialog_exception_lib_failed));
            }
        }
        String assetIndexString;
        ArrayList<DownloadTaskListBean> list = new ArrayList<>();
        AssetIndexInfo assetIndexInfo = version.getAssetIndex();
        if (assetIndexInfo == null || assetIndexInfo.id == null) {
            // Builds from before the asset system keep their assets inside the client jar, and
            // archive builds may have no index at all: there is nothing to download or verify.
            assetIndexString = "{\"objects\":{}}";
        }
        else if (isRightFile(activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json",assetIndexInfo.getSha1())) {
            assetIndexString = FileStringUtils.getStringFromFile(activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json");
        }
        else {
            String assetIndexUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource),DownloadUrlSource.ASSETS_INDEX_JSON) + assetIndexInfo.getUrl().replace("https://launchermeta.mojang.com", "").replace("https://piston-meta.mojang.com", "");
            try {
                assetIndexString = NetworkUtils.doGet(NetworkUtils.toURL(assetIndexUrl));
                list.add(new DownloadTaskListBean(assetIndexInfo.id + ".json",
                        assetIndexUrl,
                        activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json",
                        assetIndexInfo.getSha1()));
            } catch (IOException e) {
                e.printStackTrace();
                return new Exception(activity.getString(R.string.launch_check_dialog_exception_assets_failed));
            }
        }
        AssetIndex assetIndex = gson.fromJson(assetIndexString,AssetIndex.class);
        for (Library library : version.getLibraries()) {
            if (!isRightFile(activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(),library.getDownload().getSha1()) && !library.getPath().contains("tv/twitch") && !library.getPath().contains("lwjgl-platform-2.9.1-nightly")) {
                String libUrl;
                if (library.getDownload().getUrl() != null && !library.getDownload().getUrl().equals("")) {
                    libUrl = library.getDownload().getUrl();
                }
                else {
                    libUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource),DownloadUrlSource.LIBRARIES) + "/" + library.getPath();
                }
                list.add(new DownloadTaskListBean(library.getArtifactFileName(),
                        libUrl,
                        activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(),
                        library.getDownload().getSha1()));
            }
        }
        for (AssetObject object : assetIndex.getObjects().values()) {
            if (!isRightFile(activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation(),object.getHash())) {
                list.add(new DownloadTaskListBean(object.getHash(),
                        DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource),DownloadUrlSource.ASSETS_OBJ) + "/" + object.getLocation(),
                        activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation(),
                        object.getHash()));
            }
        }
        if (list.size() == 0) {
            return null;
        }
        else {
            DownloadTaskListAdapter downloadTaskListAdapter = new DownloadTaskListAdapter(activity);
            activity.runOnUiThread(() -> {
                recyclerViews[0].setAdapter(downloadTaskListAdapter);
                recyclerViews[0].setVisibility(View.VISIBLE);
            });
            ArrayList<DownloadTaskListBean> failedFile = new ArrayList<>();
            int maxTask = activity.launcherSetting.autoDownloadTaskQuantity ? 64 : activity.launcherSetting.maxDownloadTask;
            BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(maxTask);
            ExecutorService threadPool = new ThreadPoolExecutor(maxTask, maxTask,
                    0, TimeUnit.SECONDS,
                    workQueue,
                    new ThreadPoolExecutor.DiscardPolicy());
            for (int j = 0; j < list.size(); j++) {
                DownloadTaskListBean bean = list.get(j);
                String url = bean.url;
                String path = bean.path;
                String sha1 = bean.sha1;
                threadPool.execute(() -> {
                    int tryTimes = 5;
                    for (int i = 0; i < tryTimes; i++) {
                        if (isCancelled()) {
                            threadPool.shutdownNow();
                            return;
                        }
                        activity.runOnUiThread(() -> {
                            downloadTaskListAdapter.addDownloadTask(bean);
                        });
                        DownloadTask.DownloadFeedback fb = new DownloadTask.DownloadFeedback() {
                            @Override
                            public void updateProgress(long curr, long max) {
                                long progress = 100 * curr / max;
                                bean.progress = (int) progress;
                                activity.runOnUiThread(() -> {
                                    downloadTaskListAdapter.onProgress(bean);
                                });
                            }

                            @Override
                            public void updateSpeed(String speed) {

                            }
                        };
                        if (DownloadTask.downloadFileMonitored(url, path, sha1, fb)) {
                            activity.runOnUiThread(() -> {
                                downloadTaskListAdapter.onComplete(bean);
                            });
                            break;
                        }
                        else {
                            if (i == tryTimes - 1) {
                                failedFile.add(bean);
                            }
                            activity.runOnUiThread(() -> {
                                downloadTaskListAdapter.onComplete(bean);
                            });
                        }
                    }
                });
                while (!workQueue.isEmpty()) {
                    ;
                }
            }
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return e;
            }
            // 第二轮：只对第一轮失败的文件再补一次。并发下载抖动很常见，
            // 以前会直接弹"当前版本缺少必要文件"，而文件随后其实就下好了 —— 属于误报。
            if (failedFile.size() > 0) {
                ArrayList<DownloadTaskListBean> retryList = new ArrayList<>(failedFile);
                failedFile.clear();
                ExecutorService retryPool = new ThreadPoolExecutor(maxTask, maxTask,
                        0, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<>(maxTask),
                        new ThreadPoolExecutor.DiscardPolicy());
                for (int j = 0; j < retryList.size(); j++) {
                    DownloadTaskListBean bean = retryList.get(j);
                    retryPool.execute(() -> {
                        int tryTimes = 3;
                        for (int i = 0; i < tryTimes; i++) {
                            if (isCancelled()) {
                                retryPool.shutdownNow();
                                return;
                            }
                            DownloadTask.DownloadFeedback fb = new DownloadTask.DownloadFeedback() {
                                @Override
                                public void updateProgress(long curr, long max) {
                                    bean.progress = (int) (100 * curr / max);
                                    activity.runOnUiThread(() -> downloadTaskListAdapter.onProgress(bean));
                                }

                                @Override
                                public void updateSpeed(String speed) {
                                }
                            };
                            if (DownloadTask.downloadFileMonitored(bean.url, bean.path, bean.sha1, fb)) {
                                activity.runOnUiThread(() -> downloadTaskListAdapter.onComplete(bean));
                                break;
                            }
                            else if (i == tryTimes - 1) {
                                failedFile.add(bean);
                                activity.runOnUiThread(() -> downloadTaskListAdapter.onComplete(bean));
                            }
                        }
                    });
                }
                retryPool.shutdown();
                try {
                    retryPool.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return e;
                }
            }
            activity.runOnUiThread(() -> {
                recyclerViews[0].setVisibility(View.GONE);
            });
            if (failedFile.size() > 0) {
                return new Exception(activity.getString(R.string.launch_check_dialog_exception_lib_failed));
            }
            else {
                return null;
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        callback.onFinish(e);
    }

    public interface CheckLibCallback{
        void onStart();
        void onFinish(Exception e);
    }

    public static boolean isRightFile(String path,String sha1) {
        if (new File(path).exists()) {
            if (sha1 != null && !sha1.equals("")) {
                return Objects.equals(FileUtils.getFileSha1(path), sha1);
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }
}
