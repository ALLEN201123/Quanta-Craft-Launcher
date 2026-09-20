package com.qcl.launcher.launcher.download.game;

import android.os.AsyncTask;
import android.os.Looper;
import android.os.Environment;

import com.google.gson.Gson;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
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
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.platform.Bits;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Downloads a historical build that only exists in the Betacraft archive.
 *
 * <p>Those builds have no Mojang metadata, so this task reads the archive's own {@code <id>.info}
 * file to find the real client jar, downloads it into {@code versions/<id>/} and writes a small
 * local version json so the build shows up in the launcher like any other version.
 */
public class LegacyArchiveInstallTask extends AsyncTask<VersionManifest.Version, Integer, Exception> {

    private final MainActivity activity;
    private final DownloadTaskListAdapter adapter;
    private final Callback callback;
    private final DownloadTaskListBean bean;

    public LegacyArchiveInstallTask(MainActivity activity, DownloadTaskListAdapter adapter, Callback callback) {
        this.activity = activity;
        this.adapter = adapter;
        this.callback = callback;
        this.bean = new DownloadTaskListBean(activity.getString(R.string.dialog_install_game_install_game), "", "", "");
    }

    public interface Callback {
        void onStart();

        void onFailed(Exception e);

        void onFinish(String versionId);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
        if (!isCancelled()) adapter.addDownloadTask(bean);
    }

    /** Reads the archive metadata and returns the client jar url recorded inside it. */
    private String resolveJarUrl(String infoUrl) throws IOException {
        String info = NetworkUtils.doGet(NetworkUtils.toURL(infoUrl));
        if (info == null) throw new IOException("Empty archive metadata: " + infoUrl);
        for (String rawLine : info.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("url:")) {
                String url = line.substring(4).trim();
                if (!url.isEmpty()) return url;
            }
        }
        throw new IOException("No client url in archive metadata: " + infoUrl);
    }

    @Override
    protected Exception doInBackground(VersionManifest.Version... versions) {
        // Some helpers used further down build a Handler lazily; without a Looper on this
        // worker thread that throws "Can't create handler inside thread ... Looper.prepare()".
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        VersionManifest.Version version = versions[0];
        String id = version.id;
        try {
            String jarUrl = resolveJarUrl(version.url);

            File versionDir = new File(activity.launcherSetting.gameFileDirectory, "versions/" + id);
            //noinspection ResultOfMethodCallIgnored
            versionDir.mkdirs();
            File jarFile = new File(versionDir, id + ".jar");

            DownloadTask.DownloadFeedback feedback = new DownloadTask.DownloadFeedback() {
                @Override
                public void updateProgress(long curr, long max) {
                    if (max <= 0) return;
                    bean.progress = (int) (100 * curr / max);
                    activity.runOnUiThread(() -> {
                        if (!isCancelled()) adapter.onProgress(bean);
                    });
                }

                @Override
                public void updateSpeed(String speed) {
                }
            };

            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.addDownloadTask(bean);
            });

            boolean ok = false;
            IOException last = null;
            // 同一个 jar 试 http / https 两种源：海外站点在国内的可达性说不准，多一手总比单点强
            String secureUrl = jarUrl.startsWith("http://")
                    ? "https://" + jarUrl.substring("http://".length()) : jarUrl;
            String[] candidates = secureUrl.equals(jarUrl)
                    ? new String[]{jarUrl} : new String[]{jarUrl, secureUrl};
            for (int attempt = 0; attempt < 3 && !ok; attempt++) {
                String url = candidates[attempt % candidates.length];
                try {
                    ok = DownloadUtil.downloadFile(url, jarFile.getAbsolutePath(), null, feedback);
                } catch (IOException e) {
                    last = new IOException("下载失败 [" + url + "]: " + e.getMessage(), e);
                }
                // Betacraft occasionally answers 200 with an empty body: treat that as a failure
                // instead of writing a version json around a broken jar.
                if (!isUsableJar(jarFile)) {
                    ok = false;
                    if (last == null) {
                        last = new IOException("Downloaded archive jar is empty or not a jar: " + url);
                    }
                }
            }
            if (!ok) {
                if (!isCancelled()) {
                    activity.runOnUiThread(() -> adapter.onComplete(bean));
                    return last != null ? last : new IOException("Failed to download " + jarUrl);
                }
                return null;
            }

            writeVersionJson(versionDir, id, jarUrl);
            // 官方老版本元数据里写着的依赖（launchwrapper / LWJGL2 / jinput）和 pre-1.6 资源，
            // 装的时候就一并下载，和普通版本一样逐个显示进度。
            downloadDependencies(id);

            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.onComplete(bean);
            });
            if (!isCancelled()) {
                callback.onFinish(id);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.onComplete(bean);
            });
            return e;
        }
    }

    /**
     * Historical builds ship no metadata of their own, so the generated json is built from the
     * official b1.7.3 skeleton: launchwrapper + LWJGL 2.x libraries + the pre-1.6 asset index.
     * Those dependencies are fetched here, during the install, exactly like a normal version.
     */
    private void downloadDependencies(String id) {
        ArrayList<DownloadTaskListBean> list = new ArrayList<>();
        try {
            String versionJson = FileStringUtils.getStringFromFile(
                    activity.launcherSetting.gameFileDirectory + "/versions/" + id + "/" + id + ".json");
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version version = gson.fromJson(versionJson, Version.class);
            if (version == null) return;

            // pre-1.6 资源索引，以及索引里列出的每一个资源。
            // ⚠️ 资源索引单独兜底：它一失败之前会把整个依赖下载（包括库）一起带崩，导致
            //    "jar/json 都下好了，游戏文件和依赖却一直不动"。
            try {
                AssetIndexInfo index = version.getAssetIndex();
                if (index != null && index.id != null) {
                    String local = activity.launcherSetting.gameFileDirectory + "/assets/indexes/" + index.id + ".json";
                    if (!DownloadUtil.isRightFile(local, index.getSha1())) {
                        String url = DownloadUrlSource.getSubUrl(
                                DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource),
                                DownloadUrlSource.ASSETS_INDEX_JSON)
                                + index.getUrl().replace("https://launchermeta.mojang.com", "")
                                               .replace("https://piston-meta.mojang.com", "");
                        FileStringUtils.writeFile(local, NetworkUtils.doGet(NetworkUtils.toURL(url)));
                    }
                    AssetIndex assetIndex = gson.fromJson(FileStringUtils.getStringFromFile(local), AssetIndex.class);
                    if (assetIndex != null && assetIndex.getObjects() != null) {
                        for (AssetObject object : assetIndex.getObjects().values()) {
                            String path = activity.launcherSetting.gameFileDirectory + "/assets/objects/" + object.getLocation();
                            if (!DownloadUtil.isRightFile(path, object.getHash())) {
                                list.add(new DownloadTaskListBean(object.getHash(),
                                        DownloadUrlSource.getSubUrl(
                                                DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource),
                                                DownloadUrlSource.ASSETS_OBJ) + "/" + object.getLocation(),
                                        path, object.getHash()));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("LegacyArchive", "资源索引获取失败，跳过资源只下依赖库", e);
            }

            // 依赖库。⚠️ 必须走 replaceSubUrl：库里写的是 libraries.minecraft.net 的绝对地址，
            // 不改写的话就直连 Mojang，国内玩家基本下不动。
            // ⚠️ 单独 try/catch：某个库解析异常时不能把整段下载带崩（否则"装完什么都不下"）。
            int source = DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource);
            try {
            for (Library library : version.getLibraries()) {
                String path = activity.launcherSetting.gameFileDirectory + "/libraries/" + library.getPath();
                if (DownloadUtil.isRightFile(path, library.getDownload().getSha1())) continue;
                if (library.getPath().contains("tv/twitch")
                        || library.getPath().contains("lwjgl-platform-2.9.1-nightly")) continue;
                String url = library.getDownload().getUrl();
                if (url == null || url.isEmpty()) {
                    url = DownloadUrlSource.getSubUrl(source, DownloadUrlSource.LIBRARIES) + "/" + library.getPath();
                }
                url = DownloadUrlSource.replaceSubUrl(url, source, DownloadUrlSource.LIBRARIES);
                list.add(new DownloadTaskListBean(library.getArtifactFileName(), url, path,
                        library.getDownload().getSha1()));
            }
            } catch (Exception e) {
                android.util.Log.e("LegacyArchive", "部分依赖库解析失败，已跳过的仍会继续下载", e);
            }
        } catch (Exception e) {
            // 依赖清单拿不到就先放行：启动时的 CheckLibTask 会再补一次
            e.printStackTrace();
        }

        if (list.isEmpty()) return;

        // 先把整张清单摆出来，再逐个下载 —— 和普通版本观感一致
        activity.runOnUiThread(() -> {
            if (isCancelled()) return;
            for (DownloadTaskListBean item : list) adapter.addDownloadTask(item);
        });
        ArrayList<DownloadTaskListBean> failed = DownloadUtil.downloadMultipleFiles(list, 16, this, activity,
                new DownloadUtil.DownloadMultipleFilesCallback() {
                    @Override
                    public void onTaskStart(DownloadTaskListBean item) {
                    }

                    @Override
                    public void onTaskProgress(DownloadTaskListBean item) {
                        activity.runOnUiThread(() -> {
                            if (!isCancelled()) adapter.onProgress(item);
                        });
                    }

                    @Override
                    public void onTaskFinish(DownloadTaskListBean item) {
                        activity.runOnUiThread(() -> {
                            if (!isCancelled()) adapter.onComplete(item);
                        });
                    }

                    @Override
                    public void onFailed(Exception e) {
                        e.printStackTrace();
                    }
                });
        if (!failed.isEmpty()) {
            // 依赖下不齐不回滚版本目录：启动时的 CheckLibTask 会再补一次
            System.out.println("LegacyArchiveInstallTask: " + failed.size() + " dependency file(s) failed");
        }
    }

    /**
     * Historical builds have no Mojang metadata of their own, so the launcher would otherwise
     * generate a json with no libraries, no asset index and no launch wrapper -- which crashes
     * CheckLibTask and can never start. Fill the gaps from the bundled b1.7.3 skeleton instead:
     * launchwrapper main class, LWJGL 2.x libraries and the pre-1.6 asset index.
     */
    private void writeVersionJson(File versionDir, String id, String jarUrl) throws IOException {
        String json = buildLegacyJson(activity, id, jarUrl);
        com.qcl.launcher.utils.file.FileStringUtils.writeFile(
                new File(versionDir, id + ".json").getAbsolutePath(), json);
    }

    /** 用归档启动模板拼出一份版本 json（归档安装与启动检查的自动修复共用）。 */
    public static String buildLegacyJson(android.content.Context context, String id, String jarUrl) throws IOException {
        String time = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US)
                .format(new java.util.Date());
        String template = com.qcl.launcher.utils.file.AssetsUtils.readAssetsTxt(context,
                "legacy_launch_template.json");
        if (template == null || template.trim().isEmpty()) {
            throw new IOException("Missing legacy_launch_template.json asset");
        }
        String type = legacyTypeFor(id);
        // ★★★ 1.1.7/1.1.8：按版本类型精确选择 tweaker（对齐 FCL + launchwrapper 三个 tweaker 的分工）：
        //   - indev（in-*）            → IndevVanillaTweaker（主类 net.minecraft.client.d）
        //   - infdev（inf-*）/alpha（a*）→ AlphaVanillaTweaker（主类 net.minecraft.client.MinecraftApplet）
        //   - beta（b*）/release        → 默认 VanillaTweaker（主类 net.minecraft.client.Minecraft）
        //   - classic（c0.*）/pre-classic（pc-*）→ com.mojang.minecraft 包，三个 tweaker 都不适用，留空（默认）
        String tweak = tweakClassFor(id);
        return template
                .replace("__ID__", id)
                .replace("__TIME__", time)
                .replace("__TYPE__", type)
                .replace("__TWEAK__", tweak)
                .replace("__SOURCE__", jarUrl);
    }

    /** 按版本 id 前缀选择正确的 launchwrapper tweaker，未命中则返回空串（默认 VanillaTweaker）。 */
    private static String tweakClassFor(String id) {
        String lower = id.toLowerCase();
        // indev（in-*，主类 net.minecraft.client.d）
        if (lower.startsWith("in-")) {
            return " --tweakClass net.minecraft.launchwrapper.IndevVanillaTweaker";
        }
        // infdev（inf-*）/ alpha（a*）/ classic（c0.*）/ pre-classic（pc-*，即 rd-*，com.mojang 包）
        // → AlphaVanillaTweaker（扫描 classpath 找 Applet 子类，net.minecraft 与 com.mojang 都识别）。
        if (lower.startsWith("inf") || lower.startsWith("a") || lower.startsWith("c0.") || lower.startsWith("pc-")) {
            return " --tweakClass net.minecraft.launchwrapper.AlphaVanillaTweaker";
        }
        // beta（b*）/ release → 默认 VanillaTweaker
        return "";
    }

    private static String legacyTypeFor(String id) {
        if (id.startsWith("b")) return "old_beta";
        if (id.startsWith("1.")) return "release";
        return "old_alpha";
    }

    /** A usable client jar is a real zip with actual content, never an empty or HTML response. */
    private static boolean isUsableJar(File file) {
        if (file == null || !file.isFile() || file.length() < 1024) return false;
        try (java.io.InputStream in = new java.io.FileInputStream(file)) {
            return in.read() == 'P' && in.read() == 'K';
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        if (e != null) callback.onFailed(e);
    }
}
