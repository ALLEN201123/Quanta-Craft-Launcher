package com.qcl.launcher.launcher.mod.mcbbs;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.PatchMerger;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.BaseModpackInstallTask;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackConfiguration;
import com.qcl.launcher.utils.file.FileStringUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * ★ 1.5.0：MCBBS 整合包（mcbbs.packmeta / manifest.json）的真实安装任务。
 *
 * 【原来是 18 行空壳】构造函数空、doInBackground 直接 return null。
 *
 * 【照 FCL 的 McbbsModpackLocalInstallTask 对齐】
 * FCL 的做法：
 *   - GameBuilder 按 manifest.addons 装本体与加载器
 *   - ModpackInstallTask(zip, run, "/overrides") → overrides 解到 run（= 游戏目录）
 *   - 往版本 json 里 addPatch 一个 id="mcbbs" 的 patch，libraries = manifest.getLibraries()
 *   - McbbsModpackCompletionTask 下 manifest.files 里列的远程文件
 *
 * QCL 这边：
 *   1. 准备基础游戏版本（本体 jar + json）
 *   2. best-effort 装加载器（addons 里 fabric / quilt 能自动装）
 *   3. overrides/ 解到**游戏目录**
 *   4. 把 manifest.libraries 作为 "mcbbs" patch 合进版本 json（与 FCL 一致）
 *   5. manifest.files 里的 CurseFile 直链下载到 mods/（best-effort，失败不阻断）
 *      ★ AddonFile 需要 fileApi 服务端接口才能拿到地址，本阶段不做（只记日志）
 *   6. 写 versions/&lt;name&gt;/&lt;name&gt;.json.modpack
 */
public class McbbsModpackLocalInstallTask extends BaseModpackInstallTask {

    private final McbbsModpackManifest manifest;

    /** 保留原提供器用的构造签名 */
    public McbbsModpackLocalInstallTask(File file, Modpack modpack,
                                        McbbsModpackManifest manifest, String name) {
        this(sActivity, file, modpack, manifest, name, null);
    }

    public McbbsModpackLocalInstallTask(MainActivity activity, File file, Modpack modpack,
                                        McbbsModpackManifest manifest, String name,
                                        DownloadTaskListAdapter adapter) {
        super(activity, file, modpack, name, adapter);
        this.manifest = manifest;
    }

    @Override
    protected void install() throws Exception {
        File versionDir = versionDir(name);
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        final String gameVersion = modpack == null ? null : modpack.getGameVersion();

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
        List<ModpackConfiguration.FileInformation> overrides = installOverridesToGameDir(candidates, row);
        rowDone(row);
        reportOverall(70);

        // ---- ④ manifest.libraries 作为 "mcbbs" patch 合进版本 json ----
        DownloadTaskListBean libRow = addRow("合并 mcbbs libraries");
        mergeMcbbsLibrariesPatch(versionDir);
        rowDone(libRow);
        reportOverall(85);

        // ---- ⑤ 写整合包配置 ----
        writeModpackConfig(versionDir, McbbsModpackProvider.INSTANCE.getName(),
                manifest == null ? null : manifest.getName(),
                manifest == null ? null : manifest.getVersion(),
                overrides);
        reportOverall(90);

        // ---- ⑥ 远程文件（best-effort）----
        downloadManifestFiles();

        reportOverall(100);
    }

    /** manifest.addons 里 id 形如 "fabric" / "forge" / "minecraft"，version 是版本号 */
    private List<String> resolveLoaderIds() {
        List<String> ids = new ArrayList<String>();
        if (manifest == null || manifest.getAddons() == null) {
            return ids;
        }
        for (McbbsModpackManifest.Addon addon : manifest.getAddons()) {
            if (addon == null || addon.getId() == null || addon.getId().isEmpty()) {
                continue;
            }
            if ("minecraft".equals(addon.getId())) {
                continue;   // 本体已经单独处理
            }
            String version = addon.getVersion() == null ? "" : addon.getVersion();
            // 统一成 "<loader>-<version>" 形式，好复用 installLoaderBestEffort 的解析
            ids.add(addon.getId() + "-" + version);
        }
        return ids;
    }

    /**
     * 照 FCL：往版本 json 里叠一个 id="mcbbs" 的 patch，libraries = manifest.getLibraries()。
     * 这些库一般是有 url 的远程库（Gson 解析用 Artifact 适配器）。
     */
    private void mergeMcbbsLibrariesPatch(File versionDir) {
        if (manifest == null || manifest.getLibraries() == null || manifest.getLibraries().isEmpty()) {
            return;
        }
        File jsonFile = new File(versionDir, name + ".json");
        if (!jsonFile.isFile()) {
            android.util.Log.i("ModpackInstall", "版本 json 不存在，跳过 mcbbs libraries patch");
            return;
        }
        try {
            Version base = gson().fromJson(
                    FileStringUtils.getStringFromFile(jsonFile.getAbsolutePath()), Version.class);
            if (base == null || base.hasPatch("mcbbs")) {
                return;
            }
            Version patch = new Version("mcbbs").setVersion(manifest.getVersion());
            patch.setLibraries(manifest.getLibraries());
            patch.setPriority(10000);
            Version merged = PatchMerger.mergePatch(base, patch);
            // ★【2026-10-06】必须显式 close，否则缓冲区未 flush → 版本 json 被截断
            java.io.Writer jw = Files.newBufferedWriter(jsonFile.toPath());
            try {
                gson().toJson(merged, jw);
            } finally {
                jw.close();
            }
            android.util.Log.i("ModpackInstall", "已叠加 mcbbs libraries patch（"
                    + manifest.getLibraries().size() + " 个库）");
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "mcbbs libraries patch 合并失败（已跳过）", t);
        }
        // 库里写的远程地址补下（best-effort）
        downloadVersionJsonDependenciesOnly(versionDir);
    }

    /** 把版本 json 里 mcbbs patch 的库下到全局 libraries/（best-effort） */
    private void downloadVersionJsonDependenciesOnly(File versionDir) {
        File jsonFile = new File(versionDir, name + ".json");
        DownloadTaskListBean row = addRow("下载 mcbbs libraries（best-effort）");
        downloadVersionJsonLibraries(jsonFile, row);
        rowDone(row);
    }

    /**
     * manifest.files 里的远程文件：
     *   - CurseFile：有 url，直接下到 mods/
     *   - AddonFile：需要 fileApi 服务端接口拿地址，本阶段不做
     */
    private void downloadManifestFiles() {
        if (manifest == null || manifest.getFiles() == null || manifest.getFiles().isEmpty()) {
            return;
        }
        List<McbbsModpackManifest.File> files = manifest.getFiles();
        DownloadTaskListBean row = addRow("下载整合包列出的文件（best-effort）");
        int total = files.size();
        int done = 0;
        int ok = 0;
        for (McbbsModpackManifest.File file : files) {
            done++;
            try {
                if (file instanceof McbbsModpackManifest.CurseFile) {
                    McbbsModpackManifest.CurseFile curseFile = (McbbsModpackManifest.CurseFile) file;
                    URL url = curseFile.getUrl();
                    String fileName = curseFile.getFileName();
                    if (url != null && fileName != null && !fileName.isEmpty()) {
                        // ★【2026-10-06】按版本隔离决定（原来是写死的 gameDir()）
                        File target = new File(runDir(), "mods" + File.separator + fileName);
                        if ((target.isFile() && target.length() > 0) || downloadOne(url.toString(), target)) {
                            ok++;
                        }
                    } else {
                        android.util.Log.i("ModpackInstall",
                                "MCBBS CurseFile 缺少 url/fileName，跳过（需查 API 补全）");
                    }
                } else if (file instanceof McbbsModpackManifest.AddonFile) {
                    android.util.Log.i("ModpackInstall",
                            "MCBBS AddonFile 需要 fileApi 接口，暂不下载：" 
                                    + ((McbbsModpackManifest.AddonFile) file).getPath());
                }
            } catch (Throwable t) {
                android.util.Log.w("ModpackInstall", "MCBBS 文件下载失败（已跳过，不阻断）", t);
            }
            rowProgress(row, (int) (100.0 * done / total));
        }
        rowDone(row);
        android.util.Log.i("ModpackInstall", "MCBBS 远程文件 best-effort 完成：" + ok + "/" + total);
    }
}
