package com.qcl.launcher.launcher.mod.curse;

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

/**
 * ★ 1.5.0：CurseForge 整合包（zip 里有 manifest.json + overrides/）的真实安装任务。
 *
 * 【原来是 18 行空壳】
 * 构造函数空、doInBackground 直接 return null —— 导入 Curse 整合包"看起来成功了"，
 * 实际一个文件都没装。现在继承 {@link BaseModpackInstallTask} 真正装：
 *   1. 准备基础游戏版本（manifest.minecraft.version；本地有就复制，没有就下载）
 *   2. best-effort 装加载器（manifest.minecraft.modLoaders 里 Fabric / Quilt 能自动装）
 *   3. overrides/（.minecraft 的内容：mods / config / resourcepacks…）解到**游戏目录**
 *      ★ 按 zip 内容判别：优先用 manifest 声明的 overrides 名，其次找实际存在的 overrides/
 *   4. 写 versions/&lt;name&gt;/&lt;name&gt;.json.modpack
 *   5. manifest.files 里列出的远程 mod 直链下载（best-effort，失败只记日志不阻断）
 *
 * 【照 FCL 的 CurseInstallTask 对齐】
 * FCL 的做法是 GameBuilder 装本体 + ModpackInstallTask 解 overrides 到 run 目录
 * + CurseCompletionTask 下远程 mod。QCL 没有那套组件，所以按
 * MultiMCModpackInstallTask 的写法内联在 doInBackground 里。
 * 注意 FCL 里 Curse 的 overrides 是解到 run（= 游戏目录），不是版本目录。
 */
public class CurseInstallTask extends BaseModpackInstallTask {

    private final CurseManifest manifest;

    /** 保留原提供器用的构造签名（提供器 new 的时候没有 Activity，走静态 setActivity） */
    public CurseInstallTask(File file, Modpack modpack, CurseManifest manifest, String name) {
        this(sActivity, file, modpack, manifest, name, null);
    }

    public CurseInstallTask(MainActivity activity, File file, Modpack modpack, CurseManifest manifest,
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

        // ---- ① 基础游戏版本（本体 jar + json）----
        DownloadTaskListBean baseRow = addRow("准备基础游戏版本 " + (gameVersion == null ? "" : gameVersion));
        ensureBaseVersionInstalled(versionDir, gameVersion);
        rowDone(baseRow);
        reportOverall(20);

        // ---- ② 加载器（best-effort：目前只自动装 Fabric / Quilt）----
        installLoaderBestEffort(versionDir, gameVersion, resolveLoaderIds());

        // ---- ③ overrides → 游戏目录 ----
        DownloadTaskListBean row = addRow("解包整合包 overrides");
        List<ModpackConfiguration.FileInformation> overrides = installOverridesToGameDir(overrideCandidates(), row);
        rowDone(row);
        reportOverall(75);

        // ---- ④ 写整合包配置 ----
        writeModpackConfig(versionDir, CurseModpackProvider.INSTANCE.getName(),
                manifest == null ? null : manifest.getName(),
                manifest == null ? null : manifest.getVersion(),
                overrides);
        reportOverall(82);

        // ---- ⑤ 远程 mod（best-effort）----
        downloadManifestFiles();

        reportOverall(100);
    }

    /** 游戏版本：manifest.minecraft.version 为主，缺失时退回 Modpack 自身记录的 */
    private String resolveGameVersion() {
        if (manifest != null && manifest.getMinecraft() != null) {
            String v = manifest.getMinecraft().getGameVersion();
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return modpack == null ? null : modpack.getGameVersion();
    }

    /** manifest.minecraft.modLoaders 里的 id 形如 "forge-47.2.0" / "fabric-0.15.0" */
    private List<String> resolveLoaderIds() {
        List<String> ids = new ArrayList<String>();
        if (manifest != null && manifest.getMinecraft() != null) {
            for (CurseManifestModLoader loader : manifest.getMinecraft().getModLoaders()) {
                if (loader != null && loader.getId() != null && !loader.getId().isEmpty()) {
                    ids.add(loader.getId());
                }
            }
        }
        return ids;
    }

    /** overrides 目录候选：manifest 声明优先，其次包里实际存在的 */
    private List<String> overrideCandidates() {
        List<String> candidates = new ArrayList<String>();
        if (manifest != null && manifest.getOverrides() != null && !manifest.getOverrides().isEmpty()) {
            candidates.add(manifest.getOverrides());
        }
        candidates.add("overrides");
        candidates.add("client-overrides");
        return candidates;
    }

    /**
     * manifest.files 里列的远程 mod：直接按 fileName + url 下到 mods/（best-effort）。
     * ★ 缺失 fileName / url 的条目需要查 CurseForge API 才能补全，本阶段不做（只记日志）。
     */
    private void downloadManifestFiles() {
        if (manifest == null || manifest.getFiles() == null || manifest.getFiles().isEmpty()) {
            return;
        }
        List<CurseManifestFile> files = manifest.getFiles();
        DownloadTaskListBean row = addRow("下载整合包列出的 mod（best-effort）");
        int total = files.size();
        int done = 0;
        int ok = 0;
        for (CurseManifestFile file : files) {
            done++;
            try {
                String fileName = file.getFileName();
                URL url = file.getUrl();
                if (fileName != null && url != null) {
                    // ★【2026-10-06】按版本隔离决定（原来是写死的 gameDir() → 开隔离的版本看不到 mod）
                    File target = new File(runDir(), "mods" + File.separator + fileName);
                    if ((target.isFile() && target.length() > 0) || downloadOne(url.toString(), target)) {
                        ok++;
                    }
                } else {
                    android.util.Log.i("ModpackInstall",
                            "Curse 条目缺少 fileName/url，跳过（需查 CurseForge API 补全）");
                }
            } catch (Throwable t) {
                android.util.Log.w("ModpackInstall", "Curse mod 下载失败（已跳过，不阻断）", t);
            }
            rowProgress(row, (int) (100.0 * done / total));
        }
        rowDone(row);
        android.util.Log.i("ModpackInstall", "Curse 远程 mod best-effort 完成：" + ok + "/" + total);
    }
}
