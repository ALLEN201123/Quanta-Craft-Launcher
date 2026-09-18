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
import com.qcl.launcher.launcher.game.DownloadInfo;
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
        // ===== FCL checkGameCompletionAsync 对齐：版本 jar 缺失或空文件时自动补下 =====
        String versionName = new File(this.launchVersion).getName();
        File versionJarFile = new File(this.launchVersion, versionName + ".jar");
        if (!versionJarFile.isFile() || versionJarFile.length() == 0) {
            DownloadInfo downloadInfo = version.getDownloadInfo();
            String jarUrl = DownloadUrlSource.replaceSubUrl(downloadInfo.getUrl(), DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource), DownloadUrlSource.VERSION_JAR);
            if (jarUrl == null || jarUrl.equals("")) {
                jarUrl = downloadInfo.getUrl();
            }
            list.add(new DownloadTaskListBean(versionName + ".jar", jarUrl, versionJarFile.getAbsolutePath(), downloadInfo.getSha1()).withFallback(downloadInfo.getUrl()));
        }
        AssetIndexInfo assetIndexInfo = version.getAssetIndex();
        if (assetIndexInfo == null || assetIndexInfo.id == null) {
            assetIndexString = "{\"objects\":{}}";
        } else {
            String localIndexPath = this.activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + assetIndexInfo.id + ".json";
            assetIndexString = null;
            if (CheckLibTask.isRightFile(localIndexPath, assetIndexInfo.getSha1())) {
                // FCL GameAssetIndexDownloadTask 对齐：本地索引必须能解析出有效对象表，损坏/空索引视为不存在
                String localIndex = FileStringUtils.getStringFromFile(localIndexPath);
                try {
                    AssetIndex parsedIndex = (AssetIndex) gson.fromJson(localIndex, AssetIndex.class);
                    if (parsedIndex != null && parsedIndex.getObjects() != null && !parsedIndex.getObjects().isEmpty()) {
                        assetIndexString = localIndex;
                    }
                }
                catch (Throwable t) {
                    assetIndexString = null;
                }
            }
            if (assetIndexString == null) {
                String assetIndexUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource), 3) + assetIndexInfo.getUrl().replace("https://launchermeta.mojang.com", "").replace("https://piston-meta.mojang.com", "");
                try {
                    assetIndexString = NetworkUtils.doGet(NetworkUtils.toURL(assetIndexUrl));
                    list.add(new DownloadTaskListBean(assetIndexInfo.id + ".json", assetIndexUrl, localIndexPath, assetIndexInfo.getSha1()).withFallback(CheckLibTask.alternateSourceUrl(assetIndexUrl, 3, assetIndexInfo.getUrl())));
                }
                catch (IOException e) {
                    e.printStackTrace();
                    return new Exception(this.activity.getString(R.string.launch_check_dialog_exception_assets_failed));
                }
            }
        }
        AssetIndex assetIndex = (AssetIndex)gson.fromJson(assetIndexString, AssetIndex.class);
        for (Library library : version.getLibraries()) {
            String libFallback;
            String libUrl;
            // FCL GameLibrariesTask 对齐：不适用当前环境的库（规则不匹配）直接跳过
            if (!library.appliesToCurrentEnvironment()) continue;
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
        int assetSource = DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource);
        for (AssetObject object : assetIndex.getObjects().values()) {
            if (CheckLibTask.isRightFile(this.activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation(), object.getHash())) continue;
            // FCL 下载候选对齐：主 URL = 用户所选源，fallback = 另一条源（BMCLAPI ↔ 官方双向兜底）
            String objUrl = DownloadUrlSource.getSubUrl(assetSource, 4) + "/" + object.getLocation();
            String objFallback = assetSource == DownloadUrlSource.DOWNLOAD_URL_SOURCE_BMCLAPI
                    ? "https://resources.download.minecraft.net/" + object.getLocation()
                    : DownloadUrlSource.BMCLAPI_BASE + "/assets/" + object.getLocation();
            list.add(new DownloadTaskListBean(object.getHash(), objUrl, this.activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation(), object.getHash()).withFallback(objFallback));
        }
        if (list.size() > 0) {
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
        }
        // ===== FCL DefaultGameRepository.reconstructAssets 对齐：资产映射 =====
        // virtual/map_to_resources 索引（pre-1.6 远古版等）把对象铺进 assets/virtual/<id>/ 与 gameDir/resources/，
        // 远古版 MC 从 gameDir/resources/ 读音效音乐——这是 b1.7.3 无声的根因修复。幂等，只复制缺失目标。
        try {
            if (assetIndex != null && assetIndex.isVirtual() && assetIndex.getObjects() != null && !assetIndex.getObjects().isEmpty()) {
                String gameDirForMap;
                if (privateGameSetting.gameDirSetting.type == 1) {
                    gameDirForMap = this.launchVersion;
                } else if (privateGameSetting.gameDirSetting.type == 2) {
                    gameDirForMap = privateGameSetting.gameDirSetting.path;
                } else {
                    gameDirForMap = this.activity.launcherSetting.gameFileDirectory;
                }
                String assetsRoot = this.activity.launcherSetting.gameFileDirectory + "/assets";
                String assetIdForMap = (assetIndexInfo != null && assetIndexInfo.id != null) ? assetIndexInfo.id : "legacy";
                String virtualRoot = assetsRoot + "/virtual/" + assetIdForMap;
                String resourcesRoot = gameDirForMap + "/resources";
                boolean mapToResources = assetIndex.needMapToResources();
                for (java.util.Map.Entry<String, AssetObject> entry : assetIndex.getObjects().entrySet()) {
                    String key = entry.getKey();
                    AssetObject mapObj = entry.getValue();
                    if (mapObj == null || key == null || key.length() == 0 || key.contains("..")) continue;
                    String original = assetsRoot + "/objects/" + mapObj.getLocation();
                    if (!new File(original).isFile()) continue;
                    File targetVirtual = new File(virtualRoot, key);
                    if (!targetVirtual.isFile()) {
                        File parentVirtual = targetVirtual.getParentFile();
                        if (parentVirtual != null) parentVirtual.mkdirs();
                        FileUtils.copyFile(original, targetVirtual.getAbsolutePath());
                    }
                    if (mapToResources) {
                        File targetRes = new File(resourcesRoot, key);
                        if (!targetRes.isFile()) {
                            File parentRes = targetRes.getParentFile();
                            if (parentRes != null) parentRes.mkdirs();
                            FileUtils.copyFile(original, targetRes.getAbsolutePath());
                        }
                    }
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
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

