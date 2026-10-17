package com.qcl.launcher.launcher.download.forge;

import android.os.AsyncTask;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.utils.io.SSLSocketClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForgeDownloadTask extends AsyncTask<ForgeVersion,Integer,Exception> {

    private MainActivity activity;
    private DownloadTaskListAdapter adapter;
    private DownloadForgeCallback callback;

    private DownloadTaskListBean bean;

    public ForgeDownloadTask (MainActivity activity,DownloadTaskListAdapter adapter, DownloadForgeCallback callback) {
        this.activity = activity;
        this.adapter = adapter;
        this.callback = callback;

        this.bean = new DownloadTaskListBean(activity.getString(R.string.dialog_install_game_download_forge),"","","");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
        if (!isCancelled()) adapter.addDownloadTask(bean);
    }

    @Override
    protected Exception doInBackground(ForgeVersion... forgeVersions) {
        ForgeVersion forgeVersion = forgeVersions[0];
        String mirror = null;
        try {
            String baseUrl = "https://bmclapi2.bangbang93.com/forge/download/" + forgeVersion.getBuild();
            Request request = new Request.Builder().url(baseUrl).build();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(SSLSocketClient.getSSLSocketFactory(),(X509TrustManager) SSLSocketClient.getTrustManager()[0]);
            builder.hostnameVerifier(SSLSocketClient.getHostnameVerifier());
            builder.connectTimeout(20, TimeUnit.SECONDS);
            builder.readTimeout(20, TimeUnit.SECONDS);
            builder.writeTimeout(20, TimeUnit.SECONDS);
            builder.followRedirects(false);
            builder.followSslRedirects(true);
            OkHttpClient okHttpClient = builder.build();
            Call call = okHttpClient.newCall(request);
            Response response = call.execute();

            // ★★★★★ 2026-10-16 **修「安装 Forge 一直失败」**。
            //
            //   用户实测：「安装 Forge 还一直失败」。
            //
            //   ★ 根因：这里 `followRedirects(false)` **明确不跟随重定向**，
            //     然后直接 `response.headers().get("Location").replace(...)`：
            //     <pre>
            //       String redirectUrl = response.headers().get("Location").replace("/maven","");
            //                                              ↑ 头不存在时是 null
            //     </pre>
            //     只要镜像站这一次**直接返回 200**（不重定向）、或 302 但没带 `Location`、
            //     或改成 307/308，这里就 **NPE**。而外层 `catch (IOException e)`
            //     **抓不到 NullPointerException** ⇒ 异常直接抛出 AsyncTask
            //     ⇒ 安装流程中断、上层只看到"失败" ⇒ **Forge 永远装不上**。
            //
            //   ★ 修法（三层兜底，任何一种服务端行为都能继续）：
            //     ① `Location` 为 null / 空 → 说明**本身就是文件**，直接用 `baseUrl` 下载；
            //     ② 有 `Location` → 照旧拼镜像；
            //     ③ `catch` 从 `IOException` 放宽到 `Exception`，绝不让任何异常逃出去。
            String location = response.headers().get("Location");
            if (location == null || location.trim().isEmpty()) {
                // 服务端直接给了内容（或至少没重定向）——直接下 baseUrl
                mirror = baseUrl;
            } else {
                String redirectUrl = location.replace("/maven", "");
                String base;
                if (DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource) == 0) {
                    base = "https://maven.minecraftforge.net";
                }
                else if (DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource) == 1) {
                    base = "https://bmclapi2.bangbang93.com/maven";
                }
                else {
                    base = "https://bmclapi2.bangbang93.com/maven";
                }
                mirror = base + redirectUrl;
            }
            if (response.body() != null) {
                try { response.close(); } catch (Throwable ignored) { }
            }
        } catch (Exception e) {
            // ★ 不再只抓 IOException —— NPE / IllegalArgumentException 等同样要兜住，
            //   否则会直接把异常抛到 AsyncTask 之外，表现为"点了就失败且没有原因"。
            e.printStackTrace();
            if (!isCancelled()) return e;
        }
        if (mirror == null || mirror.trim().isEmpty()) {
            Exception e = new Exception("无法解析 Forge 安装器的下载地址（镜像未返回 Location，且直连也不可用）");
            e.printStackTrace();
            if (!isCancelled()) return e;
            return null;
        }
        String path = AppManifest.INSTALL_DIR + "/forge/forge-installer.jar";
        FileUtils.deleteDirectory(AppManifest.INSTALL_DIR);
        DownloadTaskListBean bean = new DownloadTaskListBean("forge-installer.jar", mirror,path,null);
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
                if (DownloadUtil.downloadFile(mirror,path,null,feedback)) {
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
                        if (!isCancelled()) return new Exception("Failed to download forge-installer.jar");
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

    public interface DownloadForgeCallback{
        void onStart();
        void onFinish(Exception e);
    }
}
