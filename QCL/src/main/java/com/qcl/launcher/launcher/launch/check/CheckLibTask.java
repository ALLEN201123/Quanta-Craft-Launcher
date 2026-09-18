/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.os.AsyncTask
 *  androidx.recyclerview.widget.RecyclerView
 *  androidx.recyclerview.widget.RecyclerView$Adapter
 *  com.google.gson.Gson
 */
package com.qcl.launcher.launcher.launch.check;

import android.content.Context;
import android.os.AsyncTask;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.game.LegacyArchiveInstallTask;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.AssetIndex;
import com.qcl.launcher.launcher.game.AssetIndexInfo;
import com.qcl.launcher.launcher.game.AssetObject;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.platform.Bits;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.qcl.launcher.R;
public class CheckLibTask
extends AsyncTask<RecyclerView, Integer, Exception> {
    private final MainActivity activity;
    private final String launchVersion;
    private final CheckLibCallback callback;

    public CheckLibTask(MainActivity activity, String launchVersion, CheckLibCallback callback) {
        this.activity = activity;
        this.launchVersion = launchVersion;
        this.callback = callback;
    }

    protected void onPreExecute() {
        super.onPreExecute();
        this.callback.onStart();
    }

    protected Exception doInBackground(RecyclerView ... recyclerViews) {
        String assetIndexString;
        String settingPath = this.launchVersion + "/qcl.cfg";
        PrivateGameSetting privateGameSetting = new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile((String)settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile((String)settingPath).enable) ? GsonUtils.getPrivateGameSettingFromFile(settingPath) : this.activity.privateGameSetting;
        if (privateGameSetting.notCheckMinecraft) {
            return null;
        }
        String versionJson = FileStringUtils.getStringFromFile(this.launchVersion + "/" + new File(this.launchVersion).getName() + ".json");
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
        Version version = (Version)gson.fromJson(versionJson, Version.class);
        if (version == null) {
            try {
                String id2 = new File(this.launchVersion).getName();
                File jar = new File(this.launchVersion, id2 + ".jar");
                if (jar.isFile()) {
                    String rebuilt = LegacyArchiveInstallTask.buildLegacyJson((Context)this.activity, id2, "");
                    FileStringUtils.writeFile(this.launchVersion + "/" + id2 + ".json", rebuilt);
                    version = (Version)gson.fromJson(rebuilt, Version.class);
                }
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
            if (version == null) {
                return new Exception(this.activity.getString(R.string.launch_check_dialog_exception_lib_failed));
            }
        }
        ArrayList<DownloadTaskListBean> list = new ArrayList<DownloadTaskListBean>();
        AssetIndexInfo assetIndexInfo = version.getAssetIndex();
        if (assetIndexInfo == null || assetIndexInfo.id == null) {
            assetIndexString = "{\"objects\":{}}";
        } else if (CheckLibTask.isRightFile(this.activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json", assetIndexInfo.getSha1())) {
            assetIndexString = FileStringUtils.getStringFromFile(this.activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json");
        } else {
            String assetIndexUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource), 3) + assetIndexInfo.getUrl().replace("https://launchermeta.mojang.com", "").replace("https://piston-meta.mojang.com", "");
            try {
                assetIndexString = NetworkUtils.doGet(NetworkUtils.toURL(assetIndexUrl));
                list.add(new DownloadTaskListBean(assetIndexInfo.id + ".json", assetIndexUrl, this.activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json", assetIndexInfo.getSha1()).withFallback(CheckLibTask.alternateSourceUrl(assetIndexUrl, 3, assetIndexInfo.getUrl())));
            }
            catch (IOException e) {
                e.printStackTrace();
                return new Exception(this.activity.getString(R.string.launch_check_dialog_exception_assets_failed));
            }
        }
        AssetIndex assetIndex = (AssetIndex)gson.fromJson(assetIndexString, AssetIndex.class);
        for (Library library : version.getLibraries()) {
            String libFallback;
            String libUrl;
            if (CheckLibTask.isRightFile(this.activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(), library.getDownload().getSha1()) || library.getPath().contains("tv/twitch") || library.getPath().contains("lwjgl-platform-2.9.1-nightly")) continue;
            if (library.getDownload().getUrl() != null && !library.getDownload().getUrl().equals("")) {
                libUrl = library.getDownload().getUrl();
                libFallback = CheckLibTask.alternateSourceUrl(libUrl, 5, null);
            } else {
                libUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource), 5) + "/" + library.getPath();
                libFallback = "https://bmclapi2.bangbang93.com/maven/" + library.getPath();
            }
            list.add(new DownloadTaskListBean(library.getArtifactFileName(), libUrl, this.activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath(), library.getDownload().getSha1()).withFallback(libFallback));
        }
        for (AssetObject object : assetIndex.getObjects().values()) {
            if (CheckLibTask.isRightFile(this.activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation(), object.getHash())) continue;
            String objUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource), 4) + "/" + object.getLocation();
            list.add(new DownloadTaskListBean(object.getHash(), objUrl, this.activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation(), object.getHash()).withFallback("https://resources.download.minecraft.net/" + object.getLocation()));
        }
        if (list.size() == 0) {
            return null;
        }
        final DownloadTaskListAdapter downloadTaskListAdapter = new DownloadTaskListAdapter((Context)this.activity);
        this.activity.runOnUiThread(() -> {
            recyclerViews[0].setAdapter((RecyclerView.Adapter)downloadTaskListAdapter);
            recyclerViews[0].setVisibility(0);
        });
        ArrayList failedFile = new ArrayList();
        int maxTask = this.activity.launcherSetting.autoDownloadTaskQuantity ? 64 : this.activity.launcherSetting.maxDownloadTask;
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(maxTask, maxTask, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
        for (int j = 0; j < list.size(); ++j) {
            final DownloadTaskListBean bean = (DownloadTaskListBean)list.get(j);
            String url = bean.url;
            String path = bean.path;
            String sha1 = bean.sha1;
            threadPool.execute(() -> {
                int tryTimes = 5;
                for (int i = 0; i < tryTimes; ++i) {
                    if (this.isCancelled()) {
                        threadPool.shutdownNow();
                        return;
                    }
                    this.activity.runOnUiThread(() -> downloadTaskListAdapter.addDownloadTask(bean));
                    DownloadTask.DownloadFeedback fb = new DownloadTask.DownloadFeedback(){

                        @Override
                        public void updateProgress(long curr, long max) {
                            long progress = 100L * curr / max;
                            bean.progress = (int)progress;
                            CheckLibTask.this.activity.runOnUiThread(() -> downloadTaskListAdapter.onProgress(bean));
                        }

                        @Override
                        public void updateSpeed(String speed) {
                        }
                    };
                    if (DownloadTask.downloadFileMonitored(bean.urlForAttempt(i), path, sha1, fb)) {
                        this.activity.runOnUiThread(() -> downloadTaskListAdapter.onComplete(bean));
                        break;
                    }
                    if (i == tryTimes - 1) {
                        failedFile.add(bean);
                    }
                    this.activity.runOnUiThread(() -> downloadTaskListAdapter.onComplete(bean));
                }
            });
        }
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(1L, TimeUnit.HOURS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return e;
        }
        if (failedFile.size() > 0) {
            ArrayList retryList = new ArrayList(failedFile);
            failedFile.clear();
            ThreadPoolExecutor retryPool = new ThreadPoolExecutor(maxTask, maxTask, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
            for (int j = 0; j < retryList.size(); ++j) {
                final DownloadTaskListBean bean = (DownloadTaskListBean)retryList.get(j);
                retryPool.execute(() -> {
                    int tryTimes = 3;
                    for (int i = 0; i < tryTimes; ++i) {
                        if (this.isCancelled()) {
                            retryPool.shutdownNow();
                            return;
                        }
                        DownloadTask.DownloadFeedback fb = new DownloadTask.DownloadFeedback(){

                            @Override
                            public void updateProgress(long curr, long max) {
                                bean.progress = (int)(100L * curr / max);
                                CheckLibTask.this.activity.runOnUiThread(() -> downloadTaskListAdapter.onProgress(bean));
                            }

                            @Override
                            public void updateSpeed(String speed) {
                            }
                        };
                        if (DownloadTask.downloadFileMonitored(bean.urlForAttempt(i + 1), bean.path, bean.sha1, fb)) {
                            this.activity.runOnUiThread(() -> downloadTaskListAdapter.onComplete(bean));
                            break;
                        }
                        if (i != tryTimes - 1) continue;
                        failedFile.add(bean);
                        this.activity.runOnUiThread(() -> downloadTaskListAdapter.onComplete(bean));
                    }
                });
            }
            retryPool.shutdown();
            try {
                retryPool.awaitTermination(1L, TimeUnit.HOURS);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                return e;
            }
        }
        this.activity.runOnUiThread(() -> recyclerViews[0].setVisibility(8));
        if (failedFile.size() > 0) {
            return new Exception(this.activity.getString(R.string.launch_check_dialog_exception_lib_failed));
        }
        return null;
    }

    private static String alternateSourceUrl(String currentUrl, int type, String officialUrl) {
        if (currentUrl == null || currentUrl.isEmpty()) {
            return null;
        }
        if (currentUrl.startsWith("https://bmclapi2.bangbang93.com")) {
            if (officialUrl != null && !officialUrl.equals(currentUrl)) {
                return officialUrl;
            }
            return null;
        }
        String mirror = DownloadUrlSource.replaceSubUrl(currentUrl, 1, type);
        return mirror != null && !mirror.equals(currentUrl) ? mirror : null;
    }

    protected void onProgressUpdate(Integer ... values) {
        super.onProgressUpdate(values);
    }

    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        this.callback.onFinish(e);
    }

    public static boolean isRightFile(String path, String sha1) {
        if (new File(path).exists()) {
            if (sha1 != null && !sha1.equals("")) {
                return Objects.equals(FileUtils.getFileSha1(path), sha1);
            }
            return true;
        }
        return false;
    }

    public static interface CheckLibCallback {
        public void onStart();

        public void onFinish(Exception var1);
    }
}

