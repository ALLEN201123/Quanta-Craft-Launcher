package com.qcl.launcher.launcher.mod.modrinth;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.BaseModpackInstallTask;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackConfiguration;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ★ 1.5.0：Modrinth 整合包（modrinth.index.json + overrides/）的真实安装任务。
 *
 * 【原来是 18 行空壳】构造函数空、doInBackground 直接 return null。
 *
 * 现在真正装（照 FCL 的 ModrinthInstallTask 对齐）：
 *   1. 准备基础游戏版本（dependencies.minecraft；本地有就复制，没有就下载）
 *   2. best-effort 装加载器（dependencies 里的 fabric-loader / quilt-loader 能自动装）
 *   3. overrides/ 与 client-overrides/ 解到**游戏目录**（FCL 也是解到 run）
 *   4. 写 versions/&lt;name&gt;/&lt;name&gt;.json.modpack
 *   5. modrinth.index.json 里 files[].downloads 直链下载（best-effort，失败不阻断）
 *      —— env.client == "unsupported" 的条目跳过（服务端专用）
 */
public class ModrinthInstallTask extends BaseModpackInstallTask {

    private final ModrinthManifest manifest;

    /** 保留原提供器用的构造签名 */
    public ModrinthInstallTask(File file, Modpack modpack, ModrinthManifest manifest, String name) {
        this(sActivity, file, modpack, manifest, name, null);
    }

    public ModrinthInstallTask(MainActivity activity, File file, Modpack modpack, ModrinthManifest manifest,
                               String name, DownloadTaskListAdapter adapter) {
        super(activity, file, modpack, name, adapter);
        this.manifest = manifest;
    }

    @Override
    protected void install() throws Exception {
        File versionDir = versionDir(name);
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        final String gameVersion = resolveGameVersion();

        // ---- ① 基础游戏版本 ----
        DownloadTaskListBean baseRow = addRow("准备基础游戏版本 " + (gameVersion == null ? "" : gameVersion));
        ensureBaseVersionInstalled(versionDir, gameVersion);
        rowDone(baseRow);
        reportOverall(20);

        // ---- ② 加载器（best-effort）----
        installLoaderBestEffort(versionDir, gameVersion, resolveLoaderIds());

        // ---- ③ overrides → 游戏目录 ----
        DownloadTaskListBean row = addRow("解包整合包 overrides");
        List<String> candidates = new ArrayList<String>();
        candidates.add("overrides");
        candidates.add("client-overrides");
        List<ModpackConfiguration.FileInformation> overrides = installOverridesToGameDir(candidates, row);
        rowDone(row);
        reportOverall(75);

        // ---- ④ 写整合包配置 ----
        writeModpackConfig(versionDir, ModrinthModpackProvider.INSTANCE.getName(),
                manifest == null ? null : manifest.getName(),
                manifest == null ? null : manifest.getVersionId(),
                overrides);
        reportOverall(82);

        // ---- ⑤ 远程 mod（best-effort）----
        downloadManifestFiles();

        reportOverall(100);
    }

    private String resolveGameVersion() {
        if (manifest != null) {
            String v = manifest.getGameVersion();
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return modpack == null ? null : modpack.getGameVersion();
    }

    /** dependencies 里的加载器键值：forge / neoforge / fabric-loader / quilt-loader */
    private List<String> resolveLoaderIds() {
        List<String> ids = new ArrayList<String>();
        if (manifest == null || manifest.getDependencies() == null) {
            return ids;
        }
        for (Map.Entry<String, String> entry : manifest.getDependencies().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || value == null || value.isEmpty() || "minecraft".equals(key)) {
                continue;
            }
            if ("fabric-loader".equals(key)) {
                ids.add("fabric-" + value);
            } else if ("quilt-loader".equals(key)) {
                ids.add("quilt-" + value);
            } else if ("forge".equals(key)) {
                ids.add("forge-" + value);
            } else if ("neoforge".equals(key)) {
                ids.add("neoforge-" + value);
            }
            // 其它未知加载器键忽略
        }
        return ids;
    }

    /**
     * files[].downloads 直链下载到 gameDir/&lt;path&gt;（best-effort）。
     * path 形如 "mods/xxx.jar" / "config/yyy.json"。
     */
    private void downloadManifestFiles() {
        if (manifest == null || manifest.getFiles() == null || manifest.getFiles().isEmpty()) {
            return;
        }
        List<ModrinthManifest.File> files = manifest.getFiles();
        DownloadTaskListBean row = addRow("下载整合包列出的文件（best-effort）");
        int total = files.size();
        int done = 0;
        int ok = 0;
        for (ModrinthManifest.File file : files) {
            done++;
            try {
                Map<String, String> env = file.getEnv();
                if (env != null && "unsupported".equals(env.get("client"))) {
                    continue;   // 服务端专用
                }
                String path = file.getPath();
                List<URL> downloads = file.getDownloads();
                if (path == null || downloads == null || downloads.isEmpty()) {
                    continue;
                }
                File target = new File(gameDir(), path);
                if ((target.isFile() && target.length() > 0) || downloadOne(downloads.get(0).toString(), target)) {
                    ok++;
                }
            } catch (Throwable t) {
                android.util.Log.w("ModpackInstall", "Modrinth 文件下载失败（已跳过，不阻断）", t);
            }
            rowProgress(row, (int) (100.0 * done / total));
        }
        rowDone(row);
        android.util.Log.i("ModpackInstall", "Modrinth 远程文件 best-effort 完成：" + ok + "/" + total);
    }
}
