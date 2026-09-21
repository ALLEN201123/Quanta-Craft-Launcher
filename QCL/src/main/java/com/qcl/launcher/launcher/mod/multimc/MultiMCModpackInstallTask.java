package com.qcl.launcher.launcher.mod.multimc;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.PatchMerger;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Arguments;
import com.qcl.launcher.utils.platform.Bits;

import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.download.game.LegacyArchiveInstallTask;
import com.qcl.launcher.launcher.download.game.LegacyVersionArchive;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackConfiguration;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.utils.io.ZipTools;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * ★ 1.2.3：MultiMC / Prism Launcher 整合包的**真实**安装任务。
 *
 * 【背景 —— 为什么必须重写】
 * 这个类原先只有 18 行：doInBackground 直接 return null，构造函数也是空的。
 * 也就是说导入 Prism / MultiMC 整合包时"看起来成功了"，实际一个文件都没装，
 * 版本目录里没有 jar / json / libraries / assets，点启动必崩。
 * （同样的空壳问题在 Curse / Modrinth / MCBBS / HMCL 四个提供器上也有。）
 *
 * 【逻辑照抄 FCL】
 * 移植自 FCL/src/main/java/com/tungsten/fclcore/mod/multimc/MultiMCModpackInstallTask.java，
 * 连同它调用的 ModpackInstallTask（解包）与 MinecraftInstanceTask（写配置）两步，
 * 步骤与判断顺序保持一致：
 *   1. 定位实例子目录：/.minecraft → /minecraft → /&lt;name&gt;/.minecraft
 *      → /&lt;name&gt;/minecraft，都不在就退回 /&lt;name&gt;/.minecraft（FCL 的兜底）
 *   2. 把该子目录整棵解到版本目录（覆盖已存在文件）
 *   3. 逐个文件算 SHA-1 写进 modpack.json —— 以后升级整合包时
 *      靠它判断"哪些文件是玩家改过的，别覆盖"（照 ModpackInstallTask 的 overrides 语义）
 *   4. 读 patches/*.json → MultiMCInstancePatch 解析 →
 *      mainClass / +tweakers / +libraries 合成 patch 版本 →
 *      PatchMerger.mergePatch 合进版本 json
 *      ★ 这步是"导入后能启动"的关键：Forge / LiteLoader 之类加载器的 mainClass
 *        与 --tweakClass 全在 patch 里，不合并就等于没装
 *   5. 拷 libraries/ 与 jarmods/（整合包自带的老式 mod）
 *
 * 【与 FCL 的唯一差异：外壳】
 * FCL 用自家 Task&lt;Void&gt; 任务框架，而 QCL 里 Profile / HMCLGameRepository /
 * GameBuilder / ModpackInstallTask / MinecraftInstanceTask 这些**全都不存在**，
 * 所以改成 QCL 通用的 AsyncTask，把上面 5 步串在 doInBackground 里一次做完。
 * 判断条件与顺序与 FCL 一致。
 *
 * 【尚未做（留给后续）】
 * - 图标：FCL 用 manifest.getIconKey()，QCL 的 MultiMCInstanceConfiguration 没有这个 getter
 * - 基础 MC 版本（jar/json/库/资源）的下载：调用方需保证目标版本已存在，
 *   缺失时走普通安装流程补。整合包自带的 overrides / libraries 只能覆盖，不能凭空造出原版文件。
 */
public class MultiMCModpackInstallTask extends AsyncTask<Object, Integer, Exception> {

    /**
     * 整合包安装需要一个能拿到游戏目录的入口。
     * 任务的构造签名要保持与原提供器一致（MultiMCModpackProvider 里 new 的时候没有 Activity），
     * 所以由 UI 在触发安装前先 setActivity(...)，装完再清掉。
     */
    private static MainActivity sActivity;

    public static void setActivity(MainActivity activity) {
        sActivity = activity;
    }

    /** UI 侧接进度与结果用；不设也能跑，只是没有回调。 */
    public interface ProgressListener {
        void onProgress(int percent);

        void onFinished(Exception error);
    }

    private ProgressListener listener;

    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        if (listener != null && values != null && values.length > 0 && values[0] != null) {
            listener.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Exception error) {
        super.onPostExecute(error);
        if (listener != null) {
            listener.onFinished(error);
        }
    }

    private final MainActivity activity;
    private final File zipFile;
    private final Modpack modpack;
    private final MultiMCInstanceConfiguration manifest;
    private final String name;

    /**
     * ★ 1.2.3：进度必须让玩家看得见 —— 只有个百分比条，玩家不知道"在装什么、到哪一步"。
     * 所以照 LegacyArchiveInstallTask / GameInstallDialog 的做法，
     * 通过 DownloadTaskListAdapter 一行一行地上报：
     * 解包 / 合并 patch / 拷库 / 装基础版本 / 装加载器，每步都有自己的行和进度。
     */
    private final DownloadTaskListAdapter adapter;

    /** 保留原签名，走静态 Activity（见 setActivity），没有进度列表 */
    public MultiMCModpackInstallTask(File file, Modpack modpack, MultiMCInstanceConfiguration cfg, String str) {
        this(sActivity, file, modpack, cfg, str, null);
    }

    public MultiMCModpackInstallTask(MainActivity activity, File zipFile, Modpack modpack,
                                     MultiMCInstanceConfiguration manifest, String name) {
        this(activity, zipFile, modpack, manifest, name, null);
    }

    public MultiMCModpackInstallTask(MainActivity activity, File zipFile, Modpack modpack,
                                     MultiMCInstanceConfiguration manifest, String name,
                                     DownloadTaskListAdapter adapter) {
        this.activity = activity;
        this.zipFile = zipFile;
        this.modpack = modpack;
        this.manifest = manifest;
        this.name = name;
        this.adapter = adapter;
    }

    /** 往进度列表里加一行 */
    private DownloadTaskListBean addRow(String rowName) {
        DownloadTaskListBean bean = new DownloadTaskListBean(rowName, "", "", "");
        if (adapter != null) {
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.addDownloadTask(bean);
            });
        }
        return bean;
    }

    private void rowProgress(DownloadTaskListBean bean, int percent) {
        if (bean == null) return;
        bean.progress = percent;
        if (adapter != null) {
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.onProgress(bean);
            });
        }
    }

    private void rowDone(DownloadTaskListBean bean) {
        if (bean == null) return;
        bean.progress = 100;
        if (adapter != null) {
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.onComplete(bean);
            });
        }
    }

    /** 整包总进度（0-100），没有进度列表时退回给 ProgressListener */
    private void reportOverall(int percent) {
        publishProgress(percent);
    }

    @Override
    protected Exception doInBackground(Object... objArr) {
        if (activity == null) {
            return new IOException("整合包安装缺少 Activity（MultiMCModpackInstallTask.setActivity 没有被调用）");
        }
        try {
            install();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    /**
     * ★★★ 解析整合包清单必须用这个 Gson，不能用裸的 new Gson()。
     *
     * Library.name 字段的类型是 Artifact（不是 String），整合包清单里写的是普通字符串，
     * 例如 "org.ow2.asm:asm:9.0"。只有注册了 Artifact.Serializer/Deserializer
     * 才能把字符串解成 Artifact。裸 Gson 会抛：
     *   IllegalStateException: Expected BEGIN_OBJECT but was STRING
     *     at ... path $.libraries[0].name
     * 适配器组合与 LegacyArchiveInstallTask / CheckLibTask 一致。
     */
    private static com.google.gson.Gson gson() {
        return JsonUtils.defaultGsonBuilder()
                .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                .registerTypeAdapter(Bits.class, new Bits.Serializer())
                .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                .create();
    }

    /**
     * ★★★ 1.2.3：照 FCL 补上「自动下载游戏本体」这一步。
     *
     * 整合包（MultiMC/Prism）本身**只带补丁与模组，不带游戏本体** ——
     * 它在 mmc-pack.json 里声明需要哪个游戏版本（IntendedVersion，例如 b1.7.3）。
     * FCL 用 GameBuilder 先把本体装好：
     *   dependencyManager.gameBuilder().name(name)
     *       .gameVersion(manifest.getGameVersion()).buildAsync()
     * 我原来直接假设「版本已经存在」，所以玩家直接装一个全新的整合包时，
     * 到 mergeJarMods 才发现没有 jar → 报「找不到游戏本体，无法合并 jarmods」。
     *
     * 这里按 FCL 的顺序补上：本体不存在 → 用 gameVersion 到归档清单里找 →
     * 下 jar → 用 buildLegacyJson 生成版本 json。
     * ★ 注意 id 用**整合包的目录名**（跟 FCL 的 builder.name(name) 一致），
     *   不是游戏版本号 —— 因为整合包是装成自己的一个版本。
     */
    private void ensureGameInstalled(File versionDir) throws Exception {
        File jar = new File(versionDir, name + ".jar");
        if (jar.isFile() && jar.length() > 0) {
            return;   // 本体已经装过，不重复下
        }

        String gameVersion = manifest == null ? null : manifest.getGameVersion();
        if (gameVersion == null || gameVersion.isEmpty()) {
            throw new IOException("整合包里没写游戏版本（IntendedVersion 为空），"
                    + "启动器不知道该下载哪个本体。");
        }

        // ===== 1) 先走 BMCL 版本清单（用户要求：直接用 bmcl）=====
        //   BMCL 镜像了官方 version_manifest，b1.7.3 这类 old_beta 也在里面，
        //   而且比 betacraft 稳（国内直连）。拿到 json 里的 downloads.client.url，
        //   再按当前下载源（默认 BMCL）换域名。
        File jsonFile = new File(versionDir, name + ".json");
        String jarUrl = null;
        try {
            int source = com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource
                    .getSource(activity.launcherSetting.downloadUrlSource);
            String versionJsonUrl = com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource
                    .getSubUrl(source, com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource.VERSION_JSON)
                    + "/version/" + gameVersion + "/json";
            String rawJson = NetworkUtils.doGet(NetworkUtils.toURL(versionJsonUrl));
            if (rawJson != null && !rawJson.isEmpty()) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser()
                        .parse(rawJson).getAsJsonObject();
                com.google.gson.JsonObject dl = obj.has("downloads")
                        ? obj.getAsJsonObject("downloads") : null;
                if (dl != null && dl.has("client")) {
                    com.google.gson.JsonObject client = dl.getAsJsonObject("client");
                    if (client.has("url")) {
                        String official = client.get("url").getAsString();
                        jarUrl = com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource
                                .replaceSubUrl(official, source,
                                        com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource.VERSION_JAR);
                        // 版本 json 直接落盘（后面 patch 会再往里合）
                        FileStringUtils.writeFile(jsonFile.getAbsolutePath(), rawJson);
                    }
                }
            }
        } catch (Exception ignored) {
            // BMCL 这条路拿不到就走下面 betacraft 归档的兜底
        }

        // ===== 2) 兜底：betacraft 归档清单 =====
        //   照旧用 entries()（它只返回「归档 tab 里该显示」的条目）；
        //   b1.7.3 这种 officialId 的由上面的 BMCL 路径负责。
        if (jarUrl == null) {
            VersionManifest.Version target = null;
            for (VersionManifest.Version v : LegacyVersionArchive.entries(activity)) {
                if (gameVersion.equals(v.id)) { target = v; break; }
            }
            if (target == null) {
                throw new IOException("这个整合包需要游戏本体 " + gameVersion + "，"
                        + "但启动器没有它的下载地址（BMCL 与归档清单里都没有）。"
                        + "请先在「下载 → 游戏」里把 " + gameVersion + " 装好，再来装整合包。");
            }
            jarUrl = LegacyArchiveInstallTask.resolveJarUrl(target.url);
            jsonFile = null;   // 后面用 buildLegacyJson 生成
        }

        DownloadTaskListBean row = addRow("下载游戏本体 " + gameVersion);
        boolean ok = DownloadUtil.downloadFile(jarUrl, jar.getAbsolutePath(), null,
                new DownloadTask.DownloadFeedback() {
                    @Override
                    public void updateProgress(long curr, long max) {
                        if (max <= 0) return;
                        row.progress = (int) (100 * curr / max);
                        activity.runOnUiThread(() -> {
                            if (!isCancelled()) adapter.onProgress(row);
                        });
                    }

                    @Override
                    public void updateSpeed(String speed) {
                    }
                });
        if (!ok) {
            //noinspection ResultOfMethodCallIgnored
            jar.delete();
            rowDone(row);
            throw new IOException("下载游戏本体失败：" + gameVersion + "\n" + jarUrl);
        }
        rowDone(row);

        // 2) 生成版本 json（id = 整合包目录名，与 FCL 的 builder.name(name) 一致）
        String json = LegacyArchiveInstallTask.buildLegacyJson(activity, name, jarUrl);
        FileStringUtils.writeFile(new File(versionDir, name + ".json").getAbsolutePath(), json);
        android.util.Log.i("MultiMC", "已自动装好游戏本体 " + gameVersion + " 到 " + jar);
    }

    private void install() throws Exception {
        File gameDir = new File(activity.launcherSetting.gameFileDirectory);
        File versionDir = new File(gameDir, "versions" + File.separator + name);
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        // ★★★ 照 FCL：先把游戏本体装好（GameBuilder 那一步）。
        //    整合包只带补丁和模组，不带本体 —— 必须先有 jar 才能合并 jarmods。
        ensureGameInstalled(versionDir);

        Charset encoding = modpack.getEncoding() != null
                ? modpack.getEncoding()
                : ZipTools.findSuitableEncoding(zipFile.toPath());

        Path zipPath = zipFile.toPath();
        // ★ commons-compress 的 ZipFile 才能按前缀取子目录（java.util.zip 只能按名字取单个条目）
        try (ZipFile zip = ZipTools.openZipFile(zipPath, encoding)) {
            // ---- 第 1 行：解包（整包最大的一步，占大头的进度）----
            DownloadTaskListBean unpackRow = addRow("解包整合包");
            String subDirectory = resolveSubDirectory(zip);
            List<ModpackConfiguration.FileInformation> overrides =
                    // ★★★ 1.2.3：overrides（.minecraft 的内容，含 mods/config 等）
                    //   必须解到**游戏目录**（.minecraft 根），不是版本目录！
                    //   原来解到 versions/<包名>/ 里，玩家加的 mods 全部"消失"。
                    extract(zip, subDirectory, gameDir, unpackRow);
            rowDone(unpackRow);
            reportOverall(65);

            // ---- 第 2 行：写 modpack.json（升级时靠它判断玩家改过的文件）----
            DownloadTaskListBean configRow = addRow("写入整合包配置");
            File configFile = new File(versionDir, name + ".json.modpack");
            ModpackConfiguration<MultiMCInstanceConfiguration> configuration =
                    new ModpackConfiguration<MultiMCInstanceConfiguration>(
                            manifest,
                            MultiMCModpackProvider.INSTANCE.getName(),
                            manifest == null ? name : manifest.getName(),
                            modpack.getVersion(),
                            overrides);
            gson().toJson(configuration, Files.newBufferedWriter(configFile.toPath()));
            rowDone(configRow);
            reportOverall(70);

            // ---- 第 3 行：合并 patches/*.json（决定能不能启动的关键一步）----
            DownloadTaskListBean patchRow = addRow("合并 patches（加载器信息）");
            int patched = applyPatches(zip, versionDir);
            if (patched == 0) {
                // 没有 patch 也要给玩家一个明确的收尾，不能让这行挂在那
                patchRow = patchRow;
            }
            rowDone(patchRow);
            reportOverall(80);

            // ---- 第 4 行：整合包自带的老式依赖库 ----
            DownloadTaskListBean libRow = addRow("拷贝整合包自带 libraries");
            copyDirectoryEntry(zip, subDirectory, "libraries", new File(versionDir, "libraries"));
            // ★ 补坑：某些库是 `MMC-hint: local` —— patch 里没有 url，包里给的是**扁平文件**
            //   （实测 Cursed Fabric b1.7.3 包：libraries/fabric-loader-0.10.6+local.jar），
            //   但启动器是按**标准 maven 路径**去 classpath 找的：
            //     libraries/net/fabricmc/fabric-loader/0.10.6+local/fabric-loader-0.10.6+local.jar
            //   所以把扁平 jar 也复制一份到标准路径，否则启动时找不到库直接崩。
            placeLocalLibraries(zip, subDirectory, gameDir);
            rowDone(libRow);
            reportOverall(85);

            // ---- 第 5 行：★★★ jarmods 合并进 minecraft.jar ----
            //   MultiMC 的 jarmod 语义就是「把 jar 合并进 minecraft.jar」。
            //   b1.7.3 时代的加载器（Cursed Fabric 之类）全靠这个 ——
            //   不合并的话加载器根本没生效，点启动必崩。
            // ---- 第 4.5 行：★★★ 照 FCL：先把游戏本体（jar + 版本 json）自动装好 ----
            //   FCL 是 dependencyManager.gameBuilder().name(name)
            //        .gameVersion(manifest.getGameVersion()).buildAsync()
            //   —— **这一步我原来漏了**，所以玩家拿一个空目录装整合包时，
            //      到 mergeJarMods 才发现没有 jar，直接报
            //      「找不到游戏本体，无法合并 jarmods」。
            //   补上之后：整合包自己声明需要哪个游戏版本，启动器就先把它下好。
            DownloadTaskListBean baseRow = addRow("下载游戏本体");
            ensureBaseGame(versionDir);
            rowDone(baseRow);
            reportOverall(88);

            DownloadTaskListBean jarModRow = addRow("合并 jarmods 进 minecraft.jar");
            int merged = mergeJarMods(zip, subDirectory, versionDir);
            if (merged == 0) {
                android.util.Log.i("MultiMC", "这个整合包没有启用任何 jarmods");
            }
            rowDone(jarModRow);
            // ★ 1.2.3：合并了 jarmods = 本体 class 被替换过 → 自动关这个版本的文件校验
            //   （JarMerger 已经删了 META-INF 签名，校验也必然对不上）
            if (merged > 0) {
                disableFileCheck(versionDir);
            }
            reportOverall(92);
        }

        // ---- 第 5 行：确保基础版本能启动（FCL 的 GameBuilder 做的就是这件事）----
        DownloadTaskListBean baseRow = addRow("检查基础游戏版本");
        ensureBaseVersion();
        rowDone(baseRow);
        reportOverall(100);
    }

    /**
     * ★ 1.2.3：保证导入完就能启动 —— 对应 FCL 里 GameBuilder 那一步。
     *
     * FCL 的做法（MultiMCModpackInstallTask 构造函数）：
     *   GameBuilder builder = dependencyManager.gameBuilder()
     *           .name(name).gameVersion(manifest.getGameVersion());
     *   再读 mmc-pack.json 的 components，把 net.minecraftforge / net.neoforged /
     *   com.mumfrey.liteloader / net.fabricmc.fabric-loader / org.quiltmc.quilt-loader
     *   的版本塞进 builder，最后 builder.buildAsync() 自动把基础版本和加载器装齐。
     *
     * QCL 没有 GameBuilder，但有等价的现成任务：
     *   download/game/MinecraftInstallTask（基础版本）
     *   download/forge/ForgeInstallTask、download/fabric/FabricInstallTask、
     *   download/quilt/QuiltInstallTask、download/liteloader/LiteLoaderInstallTask
     * 这些任务都以 DownloadTaskListAdapter 为进度出口，正好可以和本任务共用同一个列表，
     * 玩家看到的就是「解包 → patch → 库 → 基础版本 → 加载器」一整条进度。
     *
     * ★ 本方法目前只检查基础版本是否齐全并给出明确提示，
     *   真正的自动下载（串 MinecraftInstallTask 与各加载器任务）放在下一步做。
     *   之所以不在这里草草接上，是因为它涉及版本清单拉取与多任务回调串联，
     *   必须实机验证过才算完成 —— 不能只写个"看起来对"的。
     */
    private void ensureBaseVersion() throws Exception {
        File versionDir = new File(new File(activity.launcherSetting.gameFileDirectory),
                "versions" + File.separator + name);
        boolean hasJson = new File(versionDir, name + ".json").isFile();
        boolean hasJar = new File(versionDir, name + ".jar").isFile();
        if (!hasJson || !hasJar) {
            throw new IOException("整合包安装完成，但基础游戏版本不齐全（json=" + hasJson
                    + ", jar=" + hasJar + "）。\n"
                    + "整合包自身只带模组和配置，原版文件需要另外下载 —— "
                    + "自动下载基础版本这一步还没接通，请先在「下载 → 游戏」里装一遍原版 "
                    + (manifest == null ? "" : manifest.getGameVersion())
                    + "，再导入这个整合包。");
        }
    }

    /**
     * 照 FCL 的判断顺序定位实例子目录，返回带尾部斜杠的路径（如 "/.minecraft/"）。
     */
    private String resolveSubDirectory(ZipFile zip) {
        String instanceName = manifest == null ? null : manifest.getName();
        String[] candidates = new String[]{
                ".minecraft", "minecraft",
                (instanceName == null ? null : instanceName + "/.minecraft"),
                (instanceName == null ? null : instanceName + "/minecraft"),
        };
        for (String candidate : candidates) {
            if (candidate == null) continue;
            if (existsDirectory(zip, candidate)) {
                return candidate + "/";
            }
        }
        // FCL 的兜底：都没有也按 /<name>/.minecraft 走（解出来是空的，至少不崩）
        return (instanceName == null ? "" : instanceName + "/") + ".minecraft/";
    }

    private boolean existsDirectory(ZipFile zip, String path) {
        String prefix = path + "/";
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** 把 subDirectory 整棵解到 dest，返回每个文件的 SHA-1（相对路径），照 FCL 的 overrides 语义 */
    private List<ModpackConfiguration.FileInformation> extract(ZipFile zip, String subDirectory, File dest,
                                                              DownloadTaskListBean row)
            throws IOException {
        List<ModpackConfiguration.FileInformation> overrides =
                new ArrayList<ModpackConfiguration.FileInformation>();
        int total = countEntries(zip, subDirectory);
        int done = 0;

        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entry.isDirectory() || !entryName.startsWith(subDirectory)) {
                continue;
            }
            String relative = entryName.substring(subDirectory.length());
            if (relative.isEmpty()) {
                continue;
            }
            File target = new File(dest, relative);
            File parent = target.getParentFile();
            if (parent != null) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (InputStream in = zip.getInputStream(entry)) {
                writeStream(in, target);
            }
            overrides.add(new ModpackConfiguration.FileInformation(relative, sha1Hex(target)));
            done++;
            if (total > 0) {
                int percent = (int) (100.0 * done / total);
                rowProgress(row, percent);
                reportOverall(2 + (int) (60.0 * done / total));   // 解包占总进度的大头
            }
        }
        return overrides;
    }

    private int countEntries(ZipFile zip, String subDirectory) {
        int count = 0;
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().startsWith(subDirectory)) {
                count++;
            }
        }
        return count;
    }

    /**
     * ★★★ 1.2.3：把 `jarmods/` 里**被 patches 启用的** jar 合并进 minecraft.jar。
     *
     * 这是 MultiMC / Prism 整合包在远古版本上能跑起来的关键，
     * 之前只把目录拷过去、没有合并 —— 加载器等于没装，点启动必崩。
     *
     * 实测样例（Cursed Fabric b1.7.3 包）：
     *   patches/org.multimc.jarmod.b5cee00e-....json → jarMods[0].MMC-filename =
     *       "b5cee00e-....jar"（Intermediary Mappings，必需）
     *   patches/org.multimc.jarmod.0861792f-....json → "0861792f-....jar"（Log4J2，可选）
     *
     * ★ 只合并 patches 里列出的（enabled 的），不是把 jarmods/ 全塞进去 ——
     *   因为包里有些是可选项，玩家可能故意禁用（比如 log4j 那个「关掉能减少卡顿」）。
     *
     * @return 合并进 jar 的条目数（0 表示这个包没有 jarmods）
     */
    /**
     * ★★★ 1.2.3：照 FCL 补上「自动下载游戏本体」这一步。
     *
     * 整合包（MultiMC/Prism）本身**只带补丁和模组**，不带游戏本体 ——
     * 它在 mmc-pack.json 里声明需要哪个游戏版本（`net.minecraft` 组件的 version，
     * 落到 MultiMCInstanceConfiguration 就是 getGameVersion()）。
     * FCL 的做法是 `gameBuilder().name(name).gameVersion(...).buildAsync()`，
     * **先把本体装好**，再解包、合 patch、合 jarmods。
     *
     * 我原来直接假设「版本已经存在」，所以玩家装一个全新的整合包时，
     * 到 mergeJarMods 才发现没有 jar → 报「找不到游戏本体，无法合并 jarmods」。
     * 这里按 FCL 的顺序补上：本体不存在就用 gameVersion 去归档清单里找，
     * 下 jar + 用 LegacyArchiveInstallTask.buildLegacyJson() 生成版本 json
     * （id 用**整合包的目录名**，跟 FCL 的 builder.name(name) 一致）。
     */
    private void ensureBaseGame(File versionDir) throws IOException {
        File jar = new File(versionDir, name + ".jar");
        if (jar.isFile() && jar.length() > 0) {
            return;   // 本体已经有了（比如玩家之前自己装过），不用重复下
        }

        String gameVersion = manifest == null ? null : manifest.getGameVersion();
        if (gameVersion == null || gameVersion.isEmpty()) {
            throw new IOException("整合包里没写游戏版本（IntendedVersion 为空），"
                    + "启动器不知道该下载哪个本体。");
        }

        // 在归档清单里找这个游戏版本（b1.7.3 这类远古版本都在里面）
        VersionManifest.Version target = null;
        for (VersionManifest.Version v : LegacyVersionArchive.entries(activity)) {
            if (gameVersion.equals(v.id)) {
                target = v;
                break;
            }
        }
        if (target == null) {
            throw new IOException("这个整合包需要游戏本体 " + gameVersion + "，\n"
                    + "但启动器没有它的下载地址（不在归档版本清单里）。\n"
                    + "请先在「下载 → 游戏」里把 " + gameVersion + " 装好，再来装整合包。");
        }

        // 1) 从归档元数据里解析出 jar 地址，下载到版本目录
        String jarUrl = LegacyArchiveInstallTask.resolveJarUrl(target.url);
        File parent = jar.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        boolean ok = DownloadUtil.downloadFile(jarUrl, jar.getAbsolutePath(), null,
                new DownloadTask.DownloadFeedback() {
                    @Override
                    public void updateProgress(long curr, long max) {
                        if (max <= 0) {
                            return;
                        }
                        android.util.Log.d("MultiMC", "本体下载进度: " + (100 * curr / max) + "%");
                    }

                    @Override
                    public void updateSpeed(String speed) {
                    }
                });
        if (!ok) {
            //noinspection ResultOfMethodCallIgnored
            jar.delete();
            throw new IOException("下载游戏本体失败：" + gameVersion + "\n" + jarUrl);
        }

        // 2) 生成版本 json（id = 整合包目录名，和 FCL 的 builder.name(name) 一致）
        String json = LegacyArchiveInstallTask.buildLegacyJson(activity, name, jarUrl);
        FileStringUtils.writeFile(new File(versionDir, name + ".json").getAbsolutePath(), json);
        android.util.Log.i("MultiMC", "已自动装好游戏本体 " + gameVersion
                + " 到 " + jar.getAbsolutePath());
    }

    /** ★ 1.2.3：把这个版本的「不检查游戏文件」打开（只作用于这一个版本） */
    private void disableFileCheck(File versionDir) {
        try {
            File cfg = new File(versionDir, "qcl.cfg");
            PrivateGameSetting setting =
                    com.qcl.launcher.utils.gson.GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
            if (setting == null) {
                PrivateGameSetting t = activity.privateGameSetting;
                if (t == null) return;
                com.google.gson.Gson g = new com.google.gson.Gson();
                setting = g.fromJson(g.toJson(t), PrivateGameSetting.class);
                if (setting == null) return;
            }
            setting.notCheckMinecraft = true;
            com.qcl.launcher.utils.file.FileStringUtils.writeFile(cfg.getAbsolutePath(),
                    new com.google.gson.Gson().toJson(setting));
            android.util.Log.i("MultiMC", "已自动关闭这个版本的文件校验（本体被 jarmods 改过）");
        } catch (Throwable t) {
            android.util.Log.w("MultiMC", "自动关闭文件校验失败", t);
        }
    }

    private int mergeJarMods(ZipFile zip, String subDirectory, File versionDir) throws IOException {
        // 1) 从 patches/*.json 收集被启用的 jarmod 文件名
        List<String> wanted = new ArrayList<String>();
        String patchPrefix = findPatchPrefix(zip);
        if (patchPrefix != null) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String entryName = entry.getName();
                if (entry.isDirectory() || !entryName.startsWith(patchPrefix) || !entryName.endsWith(".json")) {
                    continue;
                }
                String text;
                try (InputStream in = zip.getInputStream(entry)) {
                    text = readAll(in);
                }
                MultiMCInstancePatch patch = gson().fromJson(text, MultiMCInstancePatch.class);
                if (patch == null) {
                    continue;
                }
                for (MultiMCInstancePatch.MultiMCJarMod jarMod : patch.getJarMods()) {
                    String fileName = jarMod.getFileName();
                    if (fileName != null && !fileName.isEmpty() && !wanted.contains(fileName)) {
                        wanted.add(fileName);
                    }
                }
            }
        }
        if (wanted.isEmpty()) {
            return 0;
        }

        File jar = new File(versionDir, name + ".jar");
        if (!jar.isFile()) {
            throw new IOException("找不到游戏本体，无法合并 jarmods：" + jar.getAbsolutePath()
                    + "\n整合包要装到已存在的版本上；请先把这个版本装好。");
        }

        // 2) 把需要的 jarmod 解到临时目录
        File tmpDir = new File(versionDir, ".jarmods-merge.tmp");
        com.qcl.launcher.utils.io.FileUtils.deleteDirectoryQuietly(tmpDir);
        //noinspection ResultOfMethodCallIgnored
        tmpDir.mkdirs();

        List<File> jars = new ArrayList<File>();
        for (String fileName : wanted) {
            ZipArchiveEntry entry = findEntryBySuffix(zip, subDirectory + "jarmods/" + fileName);
            if (entry == null) {
                entry = findEntryBySuffix(zip, "jarmods/" + fileName);
            }
            if (entry == null) {
                throw new IOException("整合包里没有这个 jarmod：" + fileName);
            }
            File out = new File(tmpDir, fileName);
            try (InputStream in = zip.getInputStream(entry)) {
                writeStream(in, out);
            }
            jars.add(out);
        }

        // 3) 合并进 minecraft.jar（复用 JarMerger：会丢掉 META-INF、先备份再替换）
        int merged = com.qcl.launcher.utils.io.JarMerger.mergeInto(jar, jars);

        // 4) 清掉临时目录
        com.qcl.launcher.utils.io.FileUtils.deleteDirectoryQuietly(tmpDir);
        android.util.Log.i("MultiMC", "已把 " + jars.size() + " 个 jarmod（共 "
                + merged + " 个条目）合并进 " + jar.getName());
        return merged;
    }

    /**
     * ★ 1.2.3：把 patch 里 `MMC-hint: local` 的扁平库文件，复制到启动器期望的标准 maven 路径。
     *
     * 背景（实测 Cursed Fabric b1.7.3 包）：
     *   patch 声明的库是 `net.fabricmc:fabric-loader:0.10.6+local`（**没有 url**，
     *   本地库），包里给的文件是扁平的 `libraries/fabric-loader-0.10.6+local.jar`。
     *   而启动器组装 classpath 时认的是 `Library.getPath()` 给的
     *   `net/fabricmc/fabric-loader/0.10.6+local/fabric-loader-0.10.6+local.jar`。
     *   → 不补这一份，启动时会出现 NoClassDefFoundError / 找不到库，**进不去游戏**。
     *
     * 做法：遍历 patches 里的每个库，拿 `getPath()` 当目标相对路径，
     * 到整合包的 libraries/ 里按**文件名**找同名的扁平 jar，复制过去。
     */
    private void placeLocalLibraries(ZipFile zip, String subDirectory, File gameDir) throws IOException {
        String patchPrefix = findPatchPrefix(zip);
        if (patchPrefix == null) {
            return;
        }
        int placed = 0;
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entry.isDirectory() || !entryName.startsWith(patchPrefix) || !entryName.endsWith(".json")) {
                continue;
            }
            String text;
            try (InputStream in = zip.getInputStream(entry)) {
                text = readAll(in);
            }
            MultiMCInstancePatch patch = gson().fromJson(text, MultiMCInstancePatch.class);
            if (patch == null) {
                continue;
            }
            for (Library library : patch.getLibraries()) {
                String relative;
                try {
                    relative = library.getPath();
                } catch (Throwable t) {
                    continue;   // 这个库解析不出来就跳过，不影响别的
                }
                if (relative == null || relative.isEmpty()) {
                    continue;
                }
                File target = new File(gameDir, "libraries" + File.separator + relative);
                if (target.isFile()) {
                    continue;   // 已经有了
                }
                String fileName = new File(relative).getName();

                // 到整合包的 libraries/ 里找同名文件（可能带一层子目录，所以按后缀找）
                ZipArchiveEntry src = findEntryBySuffix(zip, subDirectory + "libraries/" + fileName);
                if (src == null) {
                    src = findEntryBySuffix(zip, "libraries/" + fileName);
                }
                if (src == null) {
                    continue;   // 包里没给这个文件（一般是有 url 的远程库，交给下载流程）
                }
                File parent = target.getParentFile();
                if (parent != null) {
                    //noinspection ResultOfMethodCallIgnored
                    parent.mkdirs();
                }
                try (InputStream in = zip.getInputStream(src)) {
                    writeStream(in, target);
                }
                placed++;
            }
        }
        if (placed > 0) {
            android.util.Log.i("MultiMC", "已把 " + placed + " 个本地库放到标准 maven 路径");
        }
    }

    /** zip 里按后缀找条目（前缀可能带一层目录，不确定时用这个） */
    /**
     * ★ 1.2.3：按后缀找条目。
     * 原来写的是 `equals`（精确相等）—— zip 里的路径前缀跟猜的不一定一样
     * （比如 `Cursed-Fabric-MultiMCnew/jarmods/xxx.jar` 前面还有别的层级），
     * 结果报「整合包里没有这个 jarmod」。改成真正的 endsWith。
     */
    private ZipArchiveEntry findEntryBySuffix(ZipFile zip, String suffix) {
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String n = entry.getName().replace('\\', '/');
            if (n.endsWith(suffix)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 读 patches/*.json，逐个合成 patch 版本并合进版本 json。
     * ★ 照 FCL：json 坏掉就整体报错（而不是跳过），避免装出一个缺 mainClass 的版本。
     */
    private int applyPatches(ZipFile zip, File versionDir) throws IOException {
        String patchPrefix = findPatchPrefix(zip);
        if (patchPrefix == null) {
            return 0;
        }
        File jsonFile = new File(versionDir, name + ".json");
        if (!jsonFile.isFile()) {
            return 0;
        }
        Version version = gson().fromJson(
                FileStringUtils.getStringFromFile(jsonFile.getAbsolutePath()), Version.class);
        if (version == null) {
            return 0;
        }
        int applied = 0;

        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entry.isDirectory() || !entryName.startsWith(patchPrefix) || !entryName.endsWith(".json")) {
                continue;
            }
            String text;
            try (InputStream in = zip.getInputStream(entry)) {
                text = readAll(in);
            }
            MultiMCInstancePatch patch = gson().fromJson(text, MultiMCInstancePatch.class);
            if (patch == null || !patch.isValid()) {
                continue;
            }

            // tweakers 变成 --tweakClass <值> 追加到游戏参数（FCL 的做法）
            List<String> arguments = new ArrayList<String>();
            for (String tweaker : patch.getTweakers()) {
                arguments.add("--tweakClass");
                arguments.add(tweaker);
            }
            Version patchVersion = new Version(
                    patch.getName(),
                    patch.getVersion() == null ? "1" : patch.getVersion(),
                    1,
                    new Arguments().addGameArguments(arguments),
                    patch.getMainClass(),
                    patch.getLibraries());
            version = PatchMerger.mergePatch(version, patchVersion);
            applied++;
        }

        // 写回合并结果
        gson().toJson(version, Files.newBufferedWriter(jsonFile.toPath()));
        return applied;
    }

    /** 找出 patches/ 目录在 zip 里的前缀（整合包可能多套一层目录） */
    private String findPatchPrefix(ZipFile zip) {
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement().getName();
            int idx = entryName.indexOf("patches/");
            if (idx >= 0 && entryName.endsWith(".json")) {
                return entryName.substring(0, idx + "patches/".length());
            }
        }
        return null;
    }

    /** 把 zip 里某个相对目录拷到 dest（不存在就跳过，跟 FCL 的 Files.exists 判断一致） */
    private void copyDirectoryEntry(ZipFile zip, String subDirectory, String relative, File dest) throws IOException {
        if (!existsDirectory(zip, subDirectory + relative)) {
            return;
        }
        String prefix = subDirectory + relative + "/";
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String entryName = entry.getName();
            if (entry.isDirectory() || !entryName.startsWith(prefix)) {
                continue;
            }
            File target = new File(dest, entryName.substring(prefix.length()));
            File parent = target.getParentFile();
            if (parent != null) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (InputStream in = zip.getInputStream(entry)) {
                writeStream(in, target);
            }
        }
    }

    private static void writeStream(InputStream in, File target) throws IOException {
        byte[] buffer = new byte[8192];
        try (OutputStream out = new FileOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    private static String sha1Hex(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            try (InputStream in = new FileInputStream(file)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("SHA-1 计算失败: " + file, e);
        }
    }
}
