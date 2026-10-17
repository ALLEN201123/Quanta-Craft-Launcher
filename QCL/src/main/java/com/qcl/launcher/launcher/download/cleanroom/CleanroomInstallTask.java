package com.qcl.launcher.launcher.download.cleanroom;

import android.os.AsyncTask;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.forge.ForgeInstallTask;
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.NetworkUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**安装 Cleanroom**（Forge 1.12.2 的社区继任分支）。
 *
 * <p>用户需求：「Cleanroom」加载器，并且要**能选旧版本**（版本选择页）。
 *
 * <p>★ 核心思路（照 FCL 的做法）：Cleanroom 的官方安装器与 Forge 的 installer **结构一致**
 *    （都是 {@code install_profile.json} + {@code version.json} + processors），
 *    所以 FCL 直接复用了 {@code ForgeNewInstallTask}。
 *    本类同样：**先把安装器下载到 Forge 期望的位置，再交给 {@link ForgeInstallTask}** 跑，
 *    不重写一套安装逻辑。
 *
 * <p>★ 两阶段设计（很重要）：
 * <ol>
 *   <li>{@link #doInBackground} 只做**下载**（网络在后台线程）；</li>
 *   <li>{@link #onPostExecute} 在 **UI 线程**启动 {@link ForgeInstallTask}
 *       —— 因为 {@code AsyncTask.execute()} 必须从创建它的线程（UI）调用，
 *       在后台线程调用会抛 {@code IllegalStateException}。</li>
 * </ol>
 *
 * <p>数据来源：{@code https://hmcl.glavo.site/metadata/cleanroom/files/cleanroom-{name}-installer.jar}
 * <p>仅支持 Minecraft **1.12.2**。
 */
public class CleanroomInstallTask extends AsyncTask<CleanroomVersion, Integer, Exception> {

    /** 安装器落地路径 —— 必须与 ForgeInstallTask 读的路径一致。 */
    private static String installerPath() {
        return AppManifest.INSTALL_DIR + "/forge/forge-installer.jar";
    }

    public interface InstallCleanroomCallback {
        void onStart();

        void onFailed(Exception e);

        void onFinish(Version version);
    }

    private final MainActivity activity;
    private final DownloadTaskListAdapter adapter;
    private final String versionName;
    private final String mcVersion;
    private final InstallCleanroomCallback callback;

    private CleanroomVersion cleanroomVersion;

    public CleanroomInstallTask(MainActivity activity, DownloadTaskListAdapter adapter,
                                String versionName, String mcVersion,
                                InstallCleanroomCallback callback) {
        this.activity = activity;
        this.adapter = adapter;
        this.versionName = versionName;
        this.mcVersion = mcVersion;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (this.callback != null) {
            this.callback.onStart();
        }
    }

    /** 阶段一：只下载安装器（后台线程）。 */
    @Override
    protected Exception doInBackground(CleanroomVersion... versions) {
        this.cleanroomVersion = (versions == null || versions.length == 0) ? null : versions[0];
        if (this.cleanroomVersion == null || this.cleanroomVersion.name == null) {
            return new Exception("没有选择 Cleanroom 版本");
        }
        // ★ Cleanroom 只支持 1.12.2 —— 不满足时明确报错，而不是装出一个坏版本
        if (!"1.12.2".equals(this.mcVersion)) {
            return new Exception("Cleanroom 只支持 Minecraft 1.12.2，当前版本是 " + this.mcVersion);
        }
        try {
            File dir = new File(AppManifest.INSTALL_DIR + "/forge");
            if (!dir.exists() && !dir.mkdirs()) {
                return new Exception("无法创建目录：" + dir.getAbsolutePath());
            }
            // 清掉上一次可能残留的解压目录，避免新旧混用
            // ★ 注意：工程里有两个 FileUtils —— 这里 import 的是 utils.io 那个，
            //   它的方法是 deleteDirectory(File) / deleteDirectoryQuietly(File)，不是 String 版。
            FileUtils.deleteDirectoryQuietly(new File(AppManifest.INSTALL_DIR + "/forge/installer"));
            File target = new File(installerPath());
            if (target.exists() && !target.delete()) {
                return new Exception("无法覆盖旧的安装器文件");
            }
            download(this.cleanroomVersion.installerUrl(), target);
            return null;
        } catch (Exception e) {
            return e;
        }
    }

    /**
     * 阶段二：在 **UI 线程**把活交给 {@link ForgeInstallTask}。
     *
     * <p>它的三个回调直接转发给我们的 callback，所以上层只需要实现一套回调。
     */
    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        if (e != null) {
            if (this.callback != null) {
                this.callback.onFailed(e);
            }
            return;
        }
        try {
            ForgeVersion fv = new ForgeVersion(null, 0, this.mcVersion,
                    null, this.cleanroomVersion.name, Collections.emptyList());
            new ForgeInstallTask(this.activity, this.versionName, this.adapter,
                    new ForgeInstallTask.InstallForgeCallback() {
                        @Override
                        public void onStart() {
                            // 已经在 onPreExecute 里回调过 onStart，这里不重复
                        }

                        @Override
                        public void onFailed(Exception ex) {
                            if (CleanroomInstallTask.this.callback != null) {
                                CleanroomInstallTask.this.callback.onFailed(ex);
                            }
                        }

                        @Override
                        public void onFinish(Version version) {
                            if (CleanroomInstallTask.this.callback != null) {
                                CleanroomInstallTask.this.callback.onFinish(version);
                            }
                        }
                    }).execute(fv);
        } catch (Throwable t) {
            if (this.callback != null) {
                this.callback.onFailed(t instanceof Exception ? (Exception) t : new Exception(t));
            }
        }
    }

    /** 简单下载（30 秒连接 / 120 秒读取超时）。 */
    private void download(String url, File target) throws Exception {
        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            URL u = NetworkUtils.toURL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int code = conn.getResponseCode();
            if (code != 200) {
                throw new Exception("下载安装器失败：HTTP " + code);
            }
            in = conn.getInputStream();
            out = new FileOutputStream(target);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Throwable ignored) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable ignored) {
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        if (!target.exists() || target.length() < 1024) {
            throw new Exception("下载的安装器不完整");
        }
    }
}
