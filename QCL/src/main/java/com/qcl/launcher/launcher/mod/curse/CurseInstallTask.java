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

    /**
     * 游戏版本解析。
     *
     * ★★★★★ 2026-10-11 重写（用户实测原话）：
     *   「我下载整合包，他就要求我下一个原版游戏；而且我下一个 26.3 的整合包，
     *     他都根本检测不到这是个 26.3 的版本。你能不能改一下判断逻辑？
     *     如果整合包完全脱离了版本名称，你这又该如何检测版本？」
     *
     * <p>原来只有两条路：{@code manifest.minecraft.version} → {@code modpack.getGameVersion()}。
     * 两者都拿不到就返回 null ⇒ `ensureBaseVersionInstalled` 直接去**下原版**，
     * 玩家看到的就是"下个整合包还逼我下原版游戏"。
     *
     * <p>现在改成**四级回退**，尽量从整合包自身把版本挖出来：
     * <ol>
     *   <li>{@code manifest.minecraft.version}（CF 官方清单，最权威）；</li>
     *   <li>从 **mods 列表里的 mod 文件名/目录名**里嗅探 MC 版本
     *       （很多整合包本体不带版本号，但里面的 mod 名带，例如
     *        {@code sodium-fabric-0.6.0+mc1.21.4.jar}、{@code iris-1.21.4.jar}、
     *        {@code mods/1.21.4/...}）—— 这是本题"脱离版本名称"时最有用的线索；</li>
     *   <li>{@code modpack.getGameVersion()}（启动器自己记的）；</li>
     *   <li>overrides / 整个 zip 的**目录名**里嗅探（{@code overrides/1.21.4/}、
     *       {@code config/26.3/} 这类按版本分目录的包）。</li>
     * </ol>
     * 全部失败才返回 null（此时仍然会去下原版，但至少给了上面四条路机会）。
     */
    private String resolveGameVersion() {
        // ① CF 官方清单
        if (manifest != null && manifest.getMinecraft() != null) {
            String v = manifest.getMinecraft().getGameVersion();
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        // ② 从整合包自身嗅探（mod 文件名 / overrides 目录名 / 包名）——
        //    实现提到基类 sniffGameVersion()，六种格式共用同一套算法
        String sniffed = sniffGameVersion();
        if (sniffed != null) {
            android.util.Log.i("ModpackInstall",
                    "整合包未声明 MC 版本，从包内名字嗅探到: " + sniffed);
            return sniffed;
        }
        // ③ 启动器自己记的
        if (modpack != null && modpack.getGameVersion() != null
                && !modpack.getGameVersion().isEmpty()) {
            return modpack.getGameVersion();
        }
        return null;
    }

    /**
     * ★ 2026-10-11：把"可能含 MC 版本号"的文本喂给基类嗅探（见
     * {@link com.qcl.launcher.launcher.mod.BaseModpackInstallTask#sniffGameVersion()}）。
     *
     * <p>Curse 整合包的 mod 文件名普遍带 MC 版本，例如：
     * <pre>
     *   sodium-fabric-0.6.0+mc26.3.jar   → 26.3
     *   iris-1.21.4.jar                  → 1.21.4
     *   fabric-api-0.119.2+1.21.4.jar    → 1.21.4
     * </pre>
     * 这是"整合包完全脱离版本名称"时最有用的线索。
     */
    @Override
    protected java.util.List<String> candidateNameTexts() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        try {
            if (manifest != null) {
                // ① mods 清单里的文件名
                if (manifest.getFiles() != null) {
                    for (CurseManifestFile f : manifest.getFiles()) {
                        if (f != null && f.getFileName() != null) {
                            out.add(f.getFileName());
                        }
                    }
                }
                // ② overrides 目录名（有些包按版本命名，如 overrides-1.21.4）
                if (manifest.getOverrides() != null) {
                    out.add(manifest.getOverrides());
                }
            }
            // ③ 包本身的名字（玩家给的整合包名里也常带版本）
            if (name != null) {
                out.add(name);
            }
        } catch (Throwable ignored) {
        }
        return out;
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
     * manifest.files 里列的远程 mod：下到 {@code mods/}。
     *
     * ★★★★ 2026-10-09 用户实测「根本没有列出很多显示模组文件下载进度」——已用真实数据取证：
     *   RLCraft 的 {@code manifest.files} 有 <b>81 条</b>，但每条<b>只有</b>
     *   {@code {projectID, fileID, required}}，<b>没有 fileName / url</b>；
     *   而这里原来是「缺 fileName/url 就跳过」⇒ 81 个全被跳过，玩家只看到 1 行。
     *   （CF 官方<b>不提供</b>整合包内文件清单 API —— {@code /v1/mods/{id}/files/{fid}/manifest}
     *   实测 404，只能下 zip 后从 {@code manifest.json} 读，再逐条回查 CF 补全。）
     *
     * <p>现在的做法（照 FCL 的 {@code TaskDialog + TaskListener} 逐条报进度的观感）：
     * <ol>
     *   <li>{@link CurseForgeRemoteModRepository#completeManifestFiles} 6 条并发补全 fileName/url；</li>
     *   <li><b>每个文件单独一行</b>（文件名就是行名），逐行推进度；</li>
     *   <li>整段再补一行总进度，让玩家一眼看到「还剩多少」。</li>
     * </ol>
     */
    private void downloadManifestFiles() {
        if (manifest == null || manifest.getFiles() == null || manifest.getFiles().isEmpty()) {
            return;
        }
        // ① 先补全（原来就是缺这一步，导致 81 个 mod 全被跳过）
        List<CurseManifestFile> files;
        try {
            files = CurseForgeRemoteModRepository.MODS.completeManifestFiles(manifest.getFiles());
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "CF 文件信息补全失败，按原样继续", t);
            files = manifest.getFiles();
        }
        DownloadTaskListBean overall = addRow("下载整合包列出的 mod（共 " + files.size() + " 个）");
        int total = files.size();
        int done = 0;
        int ok = 0;
        for (CurseManifestFile file : files) {
            done++;
            if (file == null) {
                continue;
            }
            String fileName = file.getFileName();
            URL url = file.getUrl();
            if (fileName == null || url == null) {
                // 补全也拿不到的（CF 下架 / 已删文件）：记一行，不让整包失败
                rowDone(addRow(file.getProjectID() + "/" + file.getFileID() + "（CF 已下架，跳过）"));
                rowProgress(overall, (int) (100.0 * done / total));
                continue;
            }
            // ② ★ 每个文件单独一行，行名就是文件名 —— 这才是"列出很多文件下载进度"
            DownloadTaskListBean fileRow = addRow(fileName);
            try {
                // ★【2026-10-06】按版本隔离决定（原来写死 gameDir() → 开隔离的版本看不到 mod）
                File target = new File(runDir(), "mods" + File.separator + fileName);
                if ((target.isFile() && target.length() > 0) || downloadOne(url.toString(), target, fileRow)) {
                    ok++;
                    rowDone(fileRow);
                } else {
                    // ★ DownloadTaskListBean 没有 state 字段（列表只显示 name/url/path/sha1），
                    //   失败状态只能写回行名 —— 与 Modrinth 那边逐文件成行的做法一致。
                    fileRow.name = fileName + "（下载失败）";
                    rowDone(fileRow);
                }
            } catch (Throwable t) {
                fileRow.name = fileName + "（下载失败）";
                rowDone(fileRow);
                android.util.Log.w("ModpackInstall", "Curse mod 下载失败（已跳过，不阻断）: " + fileName, t);
            }
            rowProgress(overall, (int) (100.0 * done / total));
        }
        rowDone(overall);
        android.util.Log.i("ModpackInstall", "Curse 远程 mod 完成：" + ok + "/" + total);
    }
}
