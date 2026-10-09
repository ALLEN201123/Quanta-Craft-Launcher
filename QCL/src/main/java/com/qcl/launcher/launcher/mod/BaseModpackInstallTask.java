package com.qcl.launcher.launcher.mod;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.launcher.download.PatchMerger;
import com.qcl.launcher.launcher.download.game.LegacyArchiveInstallTask;
import com.qcl.launcher.launcher.download.game.LegacyVersionArchive;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.Library;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.qclpack.QclModpackProvider;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.platform.Bits;

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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * ★ 1.5.0：所有整合包安装任务的公共基类。
 *
 * 【为什么要这个类】
 * QCL 里原先只有 {@code MultiMCModpackInstallTask} 是真实实现（已实机验证），
 * Curse / Modrinth / MCBBS / HMCL 四个提供器的 InstallTask 都是 18 行空壳
 * （构造函数空、doInBackground 直接 return null），而且 UI 里还有一道
 * 「不是 MultiMC 就弹不支持」的拦截 —— 所以除 MultiMC 外，导入任何格式都装不出东西。
 *
 * QCL 没有 fclcore 那套组件（GameBuilder / DefaultDependencyManager /
 * DefaultGameRepository / ModpackInstallTask / MinecraftInstanceTask / Unzipper /
 * CompressingUtils），所以不能照抄 FCL 的 Task 框架写法。这里照
 * {@code MultiMCModpackInstallTask} 那种「AsyncTask 内联、一步做完」的模式，
 * 把解包、写配置、准备本体、装加载器、下 mod 全部串在 {@link #install()} 里。
 *
 * 【与 MultiMCModpackInstallTask 的关系 —— 为什么有一份重复实现】
 * MultiMC 那个类已经实机验证过，**绝对不许破坏它现有行为**，所以它保持原样、
 * 不去继承本类。本类里的通用方法（进度行上报 / 解包 / 基础版本准备 / 关文件校验等）
 * 因此是它的**一份有意为之的拷贝**。重复是刻意的：宁可多一份代码，
 * 也不去动那个已经跑通的类。改这里的时候要注意与 MultiMC 那份保持同样的语义。
 *
 * 【按 zip 内容判别，而不是只信 manifest 类型】
 * {@link #detectPackFormat(ZipFile)} 只看包里的文件，用来给「manifest 解析失败」
 * 的兜底路径（{@link GenericModpackInstallTask}）选对解包目标目录。
 */
public abstract class BaseModpackInstallTask extends AsyncTask<Object, Integer, Exception> {

    // ==================== 格式常量（按 zip 内容判别用） ====================

    public static final String FORMAT_MULTIMC = "multimc";
    public static final String FORMAT_CURSE = "curse";
    public static final String FORMAT_MODRINTH = "modrinth";
    public static final String FORMAT_MCBBS = "mcbbs";
    public static final String FORMAT_HMCL = "hmcl";
    public static final String FORMAT_UNKNOWN = "unknown";

    // ==================== 进度与结果回调 ====================

    /**
     * 整合包安装需要一个能拿到游戏目录的入口。
     * 任务的部分构造签名要保持与原提供器一致（提供器里 new 的时候没有 Activity），
     * 所以由 UI 在触发安装前先 setActivity(...)。
     */
    protected static MainActivity sActivity;

    public static void setActivity(MainActivity activity) {
        sActivity = activity;
    }

    /** UI 侧接进度与结果用；不设也能跑，只是没有回调。 */
    public interface ProgressListener {
        void onProgress(int percent);

        void onFinished(Exception error);
    }

    protected ProgressListener listener;

    public void setListener(ProgressListener listener) {
        this.listener = listener;
    }

    /** 解包时的相对路径过滤器（整合包格式各有各的排除规则） */
    public interface PathFilter {
        boolean accept(String relativePath);
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

    // ==================== 基础字段 ====================

    protected final MainActivity activity;
    protected final File zipFile;
    protected final Modpack modpack;
    protected final String name;

    /** 进度列表出口（可为 null：只有百分比条时） */
    protected final DownloadTaskListAdapter adapter;

    protected BaseModpackInstallTask(MainActivity activity, File zipFile, Modpack modpack,
                                    String name, DownloadTaskListAdapter adapter) {
        this.activity = activity;
        this.zipFile = zipFile;
        this.modpack = modpack;
        this.name = name;
        this.adapter = adapter;
    }

    /** 子类实现真正的安装步骤。 */
    protected abstract void install() throws Exception;

    @Override
    protected Exception doInBackground(Object... objArr) {
        if (activity == null) {
            return new IOException("整合包安装缺少 Activity（BaseModpackInstallTask.setActivity 没有被调用）");
        }
        try {
            install();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

    // ==================== 进度行上报（与 MultiMCModpackInstallTask 同语义） ====================

    /** 往进度列表里加一行 */
    protected DownloadTaskListBean addRow(String rowName) {
        DownloadTaskListBean bean = new DownloadTaskListBean(rowName, "", "", "");
        if (adapter != null) {
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.addDownloadTask(bean);
            });
        }
        return bean;
    }

    protected void rowProgress(DownloadTaskListBean bean, int percent) {
        if (bean == null) return;
        bean.progress = percent;
        if (adapter != null) {
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.onProgress(bean);
            });
        }
    }

    protected void rowDone(DownloadTaskListBean bean) {
        if (bean == null) return;
        bean.progress = 100;
        if (adapter != null) {
            activity.runOnUiThread(() -> {
                if (!isCancelled()) adapter.onComplete(bean);
            });
        }
    }

    /** 整包总进度（0-100），没有进度列表时退回给 ProgressListener */
    protected void reportOverall(int percent) {
        publishProgress(percent);
    }

    // ==================== 公共工具 ====================

    /**
     * ★ 解析整合包清单必须用这个 Gson，不能用裸的 new Gson()。
     * Library.name 字段类型是 Artifact，裸 Gson 会抛
     * IllegalStateException: Expected BEGIN_OBJECT but was STRING。
     */
    protected static Gson gson() {
        return JsonUtils.defaultGsonBuilder()
                .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                .registerTypeAdapter(Bits.class, new Bits.Serializer())
                .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                .create();
    }

    protected Charset encoding() throws IOException {
        if (modpack != null && modpack.getEncoding() != null) {
            return modpack.getEncoding();
        }
        return ZipTools.findSuitableEncoding(zipFile.toPath());
    }

    protected ZipFile openZip() throws IOException {
        return ZipTools.openZipFile(zipFile.toPath(), encoding());
    }

    /** 游戏目录（.minecraft 根） */
    protected File gameDir() {
        return new File(activity.launcherSetting.gameFileDirectory);
    }

    /**
     * ★★★【2026-10-06 修复 · 用户实测"整合包 mod 一个都没生效"】
     *
     * <p>版本的「**运行目录**」—— 必须按**该版本自己的版本隔离设置**决定，照 FCL 的
     * {@code DefaultGameRepository.getRunDirectory(version)}：
     * <ul>
     *   <li>{@code gameDirSetting.type == 0} → 游戏根目录（未开隔离）</li>
     *   <li>{@code gameDirSetting.type == 1} → **版本目录** {@code versions/&lt;name&gt;/}（开了隔离）</li>
     *   <li>其它 → 玩家自定义的路径 {@code path}</li>
     * </ul>
     *
     * <p><b>为什么必须这样</b>：整合包安装（overrides 解包、manifest 里的 mods 下载）之前一律
     * 写死到 {@link #gameDir()}（游戏根目录）。但对**开了版本隔离**的版本，游戏只读
     * {@code versions/&lt;name&gt;/} —— 实测现象：
     * {@code .minecraft/mods} 里躺着 49 个 mod，而 {@code versions/&lt;name&gt;/mods} 是空的，
     * 游戏启动日志只有 "Loading 4 mods"（全是加载器自带的），玩家看到的就是"一堆模组没装上"。
     *
     * <p>取设置的方式与 {@code FabricAPIInstallTask} 一致：优先读该版本目录下的 {@code qcl.cfg}，
     * 读不到再退回全局私有设置。
     */
    protected File runDir() {
        try {
            String root = activity.launcherSetting.gameFileDirectory;
            String vdir = root + File.separator + "versions" + File.separator + name;
            // ★★★ 照 GameManagerUI.getGameDir（全局游戏设置里的「版本隔离」）：
            //   版本目录下的 qcl.cfg 若存在且「强制开启」或「开启」→ 用版本自己的设置；
            //   否则一律用全局 AppManifest.SETTING_DIR/private_game_setting.json。
            String settingPath = vdir + File.separator + "qcl.cfg";
            boolean useVersionCfg = new File(settingPath).exists()
                    && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null
                    && (GsonUtils.getPrivateGameSettingFromFile(settingPath).forceEnable
                        || GsonUtils.getPrivateGameSettingFromFile(settingPath).enable);
            String finalSettingPath = useVersionCfg
                    ? settingPath
                    : AppManifest.SETTING_DIR + "/private_game_setting.json";
            PrivateGameSetting s = GsonUtils.getPrivateGameSettingFromFile(finalSettingPath);
            if (s != null && s.gameDirSetting != null) {
                File d = new File(PrivateGameSetting.getGameDir(root, vdir, s.gameDirSetting));
                //noinspection ResultOfMethodCallIgnored
                d.mkdirs();
                return d;
            }
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "读取版本隔离设置失败，退回游戏根目录", t);
        }
        return gameDir();
    }

    protected File versionDir(String versionName) {
        return new File(gameDir(), "versions" + File.separator + versionName);
    }

    protected File gameVersionDir(String gameVersion) {
        return new File(gameDir(), "versions" + File.separator + gameVersion);
    }

    protected static void writeStream(InputStream in, File target) throws IOException {
        byte[] buffer = new byte[8192];
        try (OutputStream out = new FileOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    protected static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    protected static String sha1Hex(File file) throws IOException {
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

    protected DownloadTask.DownloadFeedback noopFeedback() {
        return new DownloadTask.DownloadFeedback() {
            @Override
            public void updateProgress(long curr, long max) {
            }

            @Override
            public void updateSpeed(String speed) {
            }
        };
    }

    // ==================== zip 遍历 / 解包 ====================

    protected boolean existsDirectory(ZipFile zip, String path) {
        String prefix = path + "/";
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().replace('\\', '/').startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    protected int countEntries(ZipFile zip, String subDirectory) {
        int count = 0;
        String prefix = subDirectory == null ? "" : subDirectory.replace('\\', '/');
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (!entry.isDirectory() && entry.getName().replace('\\', '/').startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    protected ZipArchiveEntry findEntryBySuffix(ZipFile zip, String suffix) {
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if (entry.getName().replace('\\', '/').endsWith(suffix)) {
                return entry;
            }
        }
        return null;
    }

    /** 读 zip 里某个条目的文本（不存在返回 null） */
    protected String readTextEntry(ZipFile zip, String entryName) {
        try {
            ZipArchiveEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                entry = findEntryBySuffix(zip, "/" + entryName);
            }
            if (entry == null) {
                return null;
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return readAll(in);
            }
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 把 zip 里的 subDirectory（相对路径，调用方保证不成前缀歧义）整棵解到 dest。
     *
     * @param subDirectory 形如 "overrides/" 或 "minecraft/"；传 "" 表示整个 zip
     * @param filter       相对路径过滤器，null 表示全收
     * @return 每个落盘文件的 SHA-1 清单（相对路径），照 FCL 的 overrides 语义
     */
    protected List<ModpackConfiguration.FileInformation> extractDirectory(
            ZipFile zip, String subDirectory, File dest, DownloadTaskListBean row, PathFilter filter)
            throws IOException {
        List<ModpackConfiguration.FileInformation> overrides =
                new ArrayList<ModpackConfiguration.FileInformation>();
        String prefix = subDirectory == null ? "" : subDirectory.replace('\\', '/');
        while (prefix.startsWith("/")) prefix = prefix.substring(1);
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";

        //noinspection ResultOfMethodCallIgnored
        dest.mkdirs();

        int total = countEntries(zip, prefix);
        int done = 0;

        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String entryName = entry.getName().replace('\\', '/');
            if (!entryName.startsWith(prefix)) {
                continue;
            }
            String relative = entryName.substring(prefix.length());
            if (relative.isEmpty()) {
                continue;
            }
            if (filter != null && !filter.accept(relative)) {
                continue;
            }
            File target = new File(dest, relative);
            File parent = target.getParentFile();
            if (parent != null) {
                //noinspection ResultOfMethodCallExpected
                parent.mkdirs();
            }
            // ★★★ 1.5.0 用户要求「整合包安装列出的文件只显示一条、看不到正在下载什么」：
            //   原来整个解包过程只对应**一个** row（"解包整合包"），几千个文件全挤在那一行里
            //   ⇒ 玩家只看到一行、看不到当前在处理哪个文件。
            //   ⇒ 现在每个文件**单独一行**，并在写入过程中滚动到最新行（行已由
            //     InstallPackageUI 的 AdapterDataObserver 自动滚动）。
            DownloadTaskListBean fileRow = addRow("  " + relative);
            try (InputStream in = zip.getInputStream(entry)) {
                writeStream(in, target);
            }
            overrides.add(new ModpackConfiguration.FileInformation(relative, sha1Hex(target)));
            rowDone(fileRow);
            done++;
            if (total > 0) {
                rowProgress(row, (int) (100.0 * done / total));
                reportOverall(2 + (int) (60.0 * done / total));
            }
        }
        return overrides;
    }

    /**
     * ★ 在 zip 里找任意层级下的目录前缀（例如 "overrides" → "MyPack/overrides/"）。
     * 有些整合包会在最外层再套一个目录，顶层找不到时必须往下找一层。
     */
    protected String findDirectoryPrefix(ZipFile zip, String directoryName) {
        String needle = "/" + directoryName + "/";
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            String n = entries.nextElement().getName().replace('\\', '/');
            if (n.startsWith(directoryName + "/")) {
                return directoryName + "/";
            }
            int idx = n.indexOf(needle);
            if (idx >= 0) {
                return n.substring(0, idx + 1 + directoryName.length() + 1);
            }
        }
        return null;
    }

    /** 在 zip 里挑出实际存在的目录前缀（带尾部斜杠），用于「按内容判别 overrides 目录」 */
    protected List<String> resolveExistingPrefixes(ZipFile zip, String... candidates) {
        List<String> result = new ArrayList<String>();
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String name = candidate.replace('\\', '/');
            while (name.startsWith("/")) name = name.substring(1);
            while (name.endsWith("/")) name = name.substring(0, name.length() - 1);
            if (name.isEmpty()) continue;
            String prefix;
            if (existsDirectory(zip, name)) {
                prefix = name + "/";
            } else {
                // 有些包外面多套一层目录：MyPack/overrides/…
                prefix = findDirectoryPrefix(zip, name);
            }
            if (prefix != null && !result.contains(prefix)) {
                result.add(prefix);
            }
        }
        return result;
    }

    /** 把多个前缀目录依次解到同一个 dest，返回合并后的 overrides 清单 */
    protected List<ModpackConfiguration.FileInformation> extractPrefixes(
            ZipFile zip, List<String> prefixes, File dest, DownloadTaskListBean row, PathFilter filter)
            throws IOException {
        List<ModpackConfiguration.FileInformation> all =
                new ArrayList<ModpackConfiguration.FileInformation>();
        for (String prefix : prefixes) {
            all.addAll(extractDirectory(zip, prefix, dest, row, filter));
        }
        return all;
    }

    // ==================== 按 zip 内容判别格式 ====================

    public static String detectPackFormat(ZipFile zip) {
        if (zip.getEntry("modrinth.index.json") != null) return FORMAT_MODRINTH;
        if (zip.getEntry("mcbbs.packmeta") != null) return FORMAT_MCBBS;
        if (zip.getEntry("manifest.json") != null) {
            String text = null;
            try (InputStream in = zip.getInputStream(zip.getEntry("manifest.json"))) {
                text = readAll(in);
            } catch (Throwable ignored) {
            }
            // Curse 与 MCBBS 都用 manifest.json，MCBBS 独有 addons / launchInfo / fileApi
            if (text != null && (text.contains("\"addons\"") || text.contains("\"launchInfo\"")
                    || text.contains("\"fileApi\""))) {
                return FORMAT_MCBBS;
            }
            return FORMAT_CURSE;
        }
        if (zip.getEntry("minecraft/pack.json") != null
                || findEntryBySuffixStatic(zip, "minecraft/pack.json") != null) {
            return FORMAT_HMCL;
        }
        if (zip.getEntry("instance.cfg") != null
                || findEntryBySuffixStatic(zip, "instance.cfg") != null) {
            return FORMAT_MULTIMC;
        }
        return FORMAT_UNKNOWN;
    }

    private static ZipArchiveEntry findEntryBySuffixStatic(ZipFile zip, String suffix) {
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            if (entry.getName().replace('\\', '/').endsWith(suffix)) {
                return entry;
            }
        }
        return null;
    }

    // ==================== 基础游戏版本（本体） ====================

    /**
     * ★ 保证 versions/&lt;整合包名&gt;/ 下有可用的 &lt;name&gt;.jar 与 &lt;name&gt;.json。
     *
     * 与 MultiMCModpackInstallTask.ensureGameInstalled 相比多了一条更省事的路径：
     *   ① 本地已经装了 gameVersion 这个原版版本 → 直接把它的 jar 与 json **复制**过来
     *      （json 里的 id / jar 改成整合包名，本体自包含，启动器按
     *       versions/&lt;name&gt;/&lt;name&gt;.jar 找 jar，见 LaunchVersion.fromDirectory）
     *   ② 本地没有 → 照 MultiMC 的老路：BMCL 版本清单拿 jar 地址 → 下载 →
     *      兜底走 betacraft 归档清单 + buildLegacyJson
     */
    protected void ensureBaseVersionInstalled(File versionDir, String gameVersion) throws Exception {
        File jar = new File(versionDir, name + ".jar");
        File json = new File(versionDir, name + ".json");
        if (jar.isFile() && jar.length() > 0 && json.isFile()) {
            return;   // 已经齐了
        }
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        if (gameVersion != null && !gameVersion.isEmpty()) {
            File baseDir = gameVersionDir(gameVersion);
            File baseJson = new File(baseDir, gameVersion + ".json");
            File baseJar = new File(baseDir, gameVersion + ".jar");
            if (baseJson.isFile() && baseJar.isFile() && baseJar.length() > 0) {
                if (!jar.isFile() || jar.length() == 0) {
                    com.qcl.launcher.utils.file.FileUtils.copyFile(baseJar.getAbsolutePath(), jar.getAbsolutePath());
                }
                try {
                    JsonObject obj = new JsonParser()
                            .parse(FileStringUtils.getStringFromFile(baseJson.getAbsolutePath()))
                            .getAsJsonObject();
                    obj.addProperty("id", name);
                    obj.addProperty("jar", name);
                    FileStringUtils.writeFile(json.getAbsolutePath(), obj.toString());
                    android.util.Log.i("ModpackInstall", "已从本地版本 " + gameVersion
                            + " 复制基础版本到 versions/" + name);
                    return;
                } catch (Throwable t) {
                    android.util.Log.w("ModpackInstall", "复制本地版本 json 失败，改走远程下载", t);
                }
            }
        }

        downloadBaseVersionRemote(versionDir, gameVersion);
    }

    /**
     * 照 MultiMCModpackInstallTask.ensureGameInstalled：本机没有原版时远程下本体。
     * 优先 BMCL（国内直连、远古版本也在），失败再走 betacraft 归档清单。
     */
    protected void downloadBaseVersionRemote(File versionDir, String gameVersion) throws Exception {
        if (gameVersion == null || gameVersion.isEmpty()) {
            throw new IOException("整合包里没写游戏版本，启动器不知道该下载哪个本体。");
        }
        File jar = new File(versionDir, name + ".jar");
        File jsonFile = new File(versionDir, name + ".json");
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        String jarUrl = null;

        // ---- 1) BMCL 版本清单 ----
        try {
            int source = DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource);
            String versionJsonUrl = DownloadUrlSource.getSubUrl(source, DownloadUrlSource.VERSION_JSON)
                    + "/version/" + gameVersion + "/json";
            String rawJson = NetworkUtils.doGet(NetworkUtils.toURL(versionJsonUrl));
            if (rawJson != null && !rawJson.isEmpty()) {
                JsonObject obj = new JsonParser().parse(rawJson).getAsJsonObject();
                JsonObject dl = obj.has("downloads") ? obj.getAsJsonObject("downloads") : null;
                if (dl != null && dl.has("client")) {
                    JsonObject client = dl.getAsJsonObject("client");
                    if (client.has("url")) {
                        jarUrl = DownloadUrlSource.replaceSubUrl(client.get("url").getAsString(), source,
                                DownloadUrlSource.VERSION_JAR);
                        // 版本 json 落盘，但 id / jar 要改成整合包名
                        obj.addProperty("id", name);
                        obj.addProperty("jar", name);
                        FileStringUtils.writeFile(jsonFile.getAbsolutePath(), obj.toString());
                    }
                }
            }
        } catch (Exception ignored) {
            // 拿不到就走下面的归档兜底
        }

        // ---- 2) 兜底：betacraft 归档清单 ----
        if (jarUrl == null) {
            VersionManifest.Version target = null;
            for (VersionManifest.Version v : LegacyVersionArchive.entries(activity)) {
                if (gameVersion.equals(v.id)) {
                    target = v;
                    break;
                }
            }
            if (target == null) {
                throw new IOException("这个整合包需要游戏本体 " + gameVersion + "，"
                        + "但启动器没有它的下载地址（BMCL 与归档清单里都没有）。"
                        + "请先在「下载 → 游戏」里把 " + gameVersion + " 装好，再来装整合包。");
            }
            jarUrl = LegacyArchiveInstallTask.resolveJarUrl(target.url);
            FileStringUtils.writeFile(jsonFile.getAbsolutePath(),
                    LegacyArchiveInstallTask.buildLegacyJson(activity, name, jarUrl));
        }

        DownloadTaskListBean row = addRow("下载游戏本体 " + gameVersion);
        final String finalJarUrl = jarUrl;
        boolean ok = DownloadUtil.downloadFile(jarUrl, jar.getAbsolutePath(), null,
                new DownloadTask.DownloadFeedback() {
                    @Override
                    public void updateProgress(long curr, long max) {
                        if (max <= 0) return;
                        row.progress = (int) (100 * curr / max);
                        if (adapter != null) {
                            activity.runOnUiThread(() -> {
                                if (!isCancelled()) adapter.onProgress(row);
                            });
                        }
                    }

                    @Override
                    public void updateSpeed(String speed) {
                    }
                });
        if (!ok) {
            //noinspection ResultOfMethodCallIgnored
            jar.delete();
            rowDone(row);
            throw new IOException("下载游戏本体失败：" + gameVersion + "\n" + finalJarUrl);
        }
        rowDone(row);
    }

    // ==================== 加载器（best-effort） ====================

    /**
     * ★ 尽量把整合包声明的加载器自动装好（当前只做 Fabric / Quilt —— 它们的 profile json
     * 是一份带库地址的 patch，取回来下库再合进版本 json 就行，与 FabricInstallTask /
     * QuiltInstallTask 的做法完全一致）。
     *
     * Forge / NeoForge / LiteLoader 需要跑官方安装器（Forge 还要跑 processor 生成 patch 过的
     * jar），涉及版本清单拉取与多任务回调串联，必须实机验证过才算完成 —— 这里**只记日志、
     * 不阻断整体安装**，玩家可以在「下载」页单独补装一次。
     *
     * 任何一步失败都只记日志，绝不让整合包导入失败（本阶段要求 best-effort）。
     */
    protected void installLoaderBestEffort(File versionDir, String gameVersion, List<String> loaderIds) {
        if (loaderIds == null || loaderIds.isEmpty()) return;
        File jsonFile = new File(versionDir, name + ".json");
        if (!jsonFile.isFile()) return;

        for (String loaderId : loaderIds) {
            if (loaderId == null || loaderId.trim().isEmpty()) continue;
            String id = loaderId.trim();
            String lower = id.toLowerCase(Locale.ROOT);

            String metaUrl = null;
            String patchId = null;
            String patchVersion = null;
            if (lower.startsWith("fabric-")) {
                patchId = "fabric";
                patchVersion = id.substring("fabric-".length());
                metaUrl = "https://meta.fabricmc.net/v2/versions/loader/" + gameVersion
                        + "/" + patchVersion + "/profile/json";
            } else if (lower.startsWith("quilt-")) {
                patchId = "quilt";
                patchVersion = id.substring("quilt-".length());
                metaUrl = "https://meta.quiltmc.org/v3/versions/loader/" + gameVersion
                        + "/" + patchVersion + "/profile/json";
            }

            DownloadTaskListBean row = addRow("安装加载器 " + id);
            if (metaUrl == null) {
                android.util.Log.i("ModpackInstall", "加载器 " + id
                        + " 需要跑官方安装器，暂未自动安装；请在「下载」页单独装一次");
                rowDone(row);
                continue;
            }
            try {
                mergeLoaderProfile(jsonFile, metaUrl, patchId, patchVersion, row);
            } catch (Throwable t) {
                android.util.Log.w("ModpackInstall", "自动安装加载器 " + id + " 失败（已跳过，不阻断导入）", t);
            }
            rowDone(row);
        }
    }

    private void mergeLoaderProfile(File jsonFile, String metaUrl, String patchId, String patchVersion,
                                    DownloadTaskListBean row) throws Exception {
        String text = NetworkUtils.doGet(NetworkUtils.toURL(metaUrl));
        if (text == null || text.trim().isEmpty()) {
            throw new IOException("加载器元数据为空: " + metaUrl);
        }
        Version patch = gson().fromJson(text, Version.class);
        if (patch == null || patch.getMainClass() == null) {
            throw new IOException("加载器元数据无法解析: " + metaUrl);
        }
        // 先把 patch 声明的库补齐（best-effort）
        downloadLibrariesBestEffort(patch, row);

        patch.setId(patchId).setVersion(patchVersion).setPriority(30000);
        Version base = gson().fromJson(
                FileStringUtils.getStringFromFile(jsonFile.getAbsolutePath()), Version.class);
        if (base == null) {
            throw new IOException("版本 json 无法解析: " + jsonFile);
        }
        Version merged = PatchMerger.mergePatch(base, patch);
        // ★★★【2026-10-06 修复 · 用户实测"下载整合包装完就崩"】
        //   `Files.newBufferedWriter(...)` 必须**显式 close**！
        //   Gson.toJson(obj, Writer) 只负责往里写、**不会 flush/close 我们传进去的流**；
        //   带着未 flush 的缓冲直接丢弃 → 版本 json 被截断在缓冲区边界
        //   （实测坏文件正好 98304 字节 = 96KB），下次解析就抛
        //   JsonSyntaxException / EOFException: End of input … → "装完整合包刷新版本列表就崩"。
        java.io.Writer w = Files.newBufferedWriter(jsonFile.toPath());
        try {
            gson().toJson(merged, w);
        } finally {
            w.close();
        }
        android.util.Log.i("ModpackInstall", "已把加载器 " + patchId + " " + patchVersion + " 合进版本 json");
    }

    /**
     * 把某个版本 json 里声明的库补下到全局 libraries/（best-effort，单个失败不阻断）。
     * HMCL 整合包的 minecraft/pack.json 以及 Fabric/Quilt 的 profile patch 都靠这一步。
     */
    protected void downloadLibrariesBestEffort(Version version, DownloadTaskListBean row) {
        if (version == null || version.getLibraries() == null || version.getLibraries().isEmpty()) {
            return;
        }
        List<Library> libraries = version.getLibraries();
        int total = libraries.size();
        int done = 0;
        int source = DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource);
        for (Library library : libraries) {
            done++;
            try {
                String relative = library.getPath();
                if (relative != null && !relative.isEmpty()) {
                    File target = new File(gameDir(), "libraries" + File.separator + relative);
                    if (!target.isFile() || target.length() == 0) {
                        String url = null;
                        try {
                            if (library.hasDownloadURL()) {
                                url = library.getDownload().getUrl();
                            }
                        } catch (Throwable ignored) {
                        }
                        if (url == null || url.isEmpty()) {
                            url = DownloadUrlSource.getSubUrl(source, DownloadUrlSource.LIBRARIES)
                                    + "/" + relative;
                        } else {
                            url = DownloadUrlSource.replaceSubUrl(url, source, DownloadUrlSource.LIBRARIES);
                        }
                        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                            // ★ 2026-10-09 用户要求：「整合包的所有文件下载也要展示给玩家看，
                            //   包括下载进度」——原来几百个库文件（含全部依赖 jar）**全挤在一行**里，
                            //   玩家根本看不出下了什么 ⇒ 现在**每个文件单独一行**（照 FCL 的逐文件任务列表）。
                            DownloadTaskListBean fileRow = addRow("下载 " + target.getName());
                            try {
                                downloadOne(url, target);
                            } finally {
                                rowDone(fileRow);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                android.util.Log.w("ModpackInstall", "库下载失败（已跳过）", t);
            }
            if (total > 0) {
                rowProgress(row, (int) (100.0 * done / total));
            }
        }
    }

    /** 把版本 json 里的库补下（读文件版） */
    protected void downloadVersionJsonLibraries(File jsonFile, DownloadTaskListBean row) {
        try {
            Version version = gson().fromJson(
                    FileStringUtils.getStringFromFile(jsonFile.getAbsolutePath()), Version.class);
            downloadLibrariesBestEffort(version, row);
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "版本 json 的库解析失败（已跳过）", t);
        }
    }

    // ==================== 写整合包配置 / 关文件校验 ====================

    /** 写 versions/&lt;name&gt;/&lt;name&gt;.json.modpack（与 MultiMC 同一约定） */
    protected void writeModpackConfig(File versionDir, String type, String pkgName, String pkgVersion,
                                      List<ModpackConfiguration.FileInformation> overrides) throws IOException {
        File configFile = new File(versionDir, name + ".json.modpack");
        ModpackManifest manifest = modpack == null ? null : modpack.getManifest();
        ModpackConfiguration<ModpackManifest> configuration =
                new ModpackConfiguration<ModpackManifest>(manifest, type,
                        pkgName == null ? name : pkgName, pkgVersion, overrides);
        // ★【2026-10-06】同样必须显式 close：Gson 不会 flush 它收到的 Writer
        java.io.Writer cw = Files.newBufferedWriter(configFile.toPath());
        try {
            gson().toJson(configuration, cw);
        } finally {
            cw.close();
        }
    }

    /** 把这个版本的「不检查游戏文件」打开（只作用于这一个版本） */
    protected void disableFileCheck(File versionDir) {
        try {
            File cfg = new File(versionDir, "qcl.cfg");
            PrivateGameSetting setting =
                    GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
            if (setting == null) {
                PrivateGameSetting t = activity.privateGameSetting;
                if (t == null) return;
                Gson g = new Gson();
                setting = g.fromJson(g.toJson(t), PrivateGameSetting.class);
                if (setting == null) return;
            }
            setting.notCheckMinecraft = true;
            FileStringUtils.writeFile(cfg.getAbsolutePath(), new Gson().toJson(setting));
            android.util.Log.i("ModpackInstall", "已自动关闭这个版本的文件校验");
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "自动关闭文件校验失败", t);
        }
    }

    // ==================== 远程 mod（best-effort，失败不阻断） ====================

    /** 下载单个远程文件；任何失败都只记日志，返回是否成功 */
    protected boolean downloadOne(String url, File target) {
        if (url == null || url.isEmpty()) return false;
        try {
            File parent = target.getParentFile();
            if (parent != null) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            return DownloadUtil.downloadFile(url, target.getAbsolutePath(), null, noopFeedback());
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "远程文件下载失败（已跳过）: " + url, t);
            return false;
        }
    }

    // ==================== 各格式共用的安装步骤 ====================

    /**
     * Curse / Modrinth / MCBBS（以及未知格式）共用的部分：overrides 解到**游戏目录**。
     *
     * @param overrideCandidates 候选 overrides 目录名（按 zip 内容挑实际存在的）
     */
    protected List<ModpackConfiguration.FileInformation> installOverridesToGameDir(
            List<String> overrideCandidates, DownloadTaskListBean row) throws IOException {
        try (ZipFile zip = openZip()) {
            List<String> prefixes = resolveExistingPrefixes(zip, overrideCandidates.toArray(new String[0]));
            if (prefixes.isEmpty()) {
                android.util.Log.i("ModpackInstall", "整合包里没有 overrides 目录，跳过");
                return new ArrayList<ModpackConfiguration.FileInformation>();
            }
            // ★【2026-10-06】按**版本隔离设置**决定解到哪（原来是写死的 gameDir()）
            return extractPrefixes(zip, prefixes, runDir(), row, null);
        }
    }

    /**
     * HMCL 风格整合包（zip 里有 minecraft/pack.json）的完整安装。
     *
     * 照 FCL 的 QclModpackInstallTask：minecraft/ 的内容解到**版本目录**（排除 pack.json）。
     * 但 FCL 用 LibraryAnalyzer + installLibraryAsync 逐个补库，QCL 没有那套，
     * 所以这里改成「把 pack.json 当 patch 合进基础版本 json」+ 从合并后的 json 补库
     * —— 与 GameInstallDialog 装 Forge / Fabric 走的是同一条 PatchMerger 路径。
     *
     * 这个方法既是 {@code QclModpackInstallTask} 的实现，也是
     * {@link GenericModpackInstallTask} 在「manifest 解析失败但 zip 内容像 HMCL」时的兜底。
     */
    protected void installHmclZip() throws Exception {
        File versionDir = versionDir(name);
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        // ---- ① 读 minecraft/pack.json（它本身就是一份版本 json）----
        String packJson;
        try (ZipFile zip = openZip()) {
            packJson = readTextEntry(zip, "minecraft/pack.json");
        }
        if (packJson == null || packJson.trim().isEmpty()) {
            throw new IOException("这个整合包里没有 minecraft/pack.json，不是有效的 HMCL 整合包。");
        }
        final String gameVersion = resolveHmclGameVersion(packJson);

        // ---- ② minecraft/ 的内容解到**版本目录**（排除 pack.json）----
        DownloadTaskListBean row = addRow("解包 minecraft/ 到版本目录");
        List<ModpackConfiguration.FileInformation> overrides;
        try (ZipFile zip = openZip()) {
            String minecraftPrefix = existsDirectory(zip, "minecraft")
                    ? "minecraft/" : findDirectoryPrefix(zip, "minecraft");
            if (minecraftPrefix == null) {
                minecraftPrefix = "minecraft/";   // 找不到就当空目录解，至少不崩
            }
            overrides = extractDirectory(zip, minecraftPrefix, versionDir, row,
                    relative -> !"pack.json".equals(relative));
        }
        rowDone(row);
        reportOverall(60);

        // ---- ③ 基础游戏版本 ----
        DownloadTaskListBean baseRow = addRow("准备基础游戏版本 "
                + (gameVersion == null ? "" : gameVersion));
        ensureBaseVersionInstalled(versionDir, gameVersion);
        rowDone(baseRow);
        reportOverall(68);

        // ---- ④ 把 pack.json 当 patch 合进基础版本 json ----
        DownloadTaskListBean mergeRow = addRow("合并 minecraft/pack.json（加载器信息）");
        File jsonFile = new File(versionDir, name + ".json");
        boolean merged = mergeVersionJsonPatchText(jsonFile, packJson, "pack", 20000);
        downloadVersionJsonLibraries(jsonFile, mergeRow);
        rowDone(mergeRow);
        reportOverall(92);
        android.util.Log.i("ModpackInstall", "HMCL pack.json 合并" + (merged ? "完成" : "跳过"));

        // ---- ⑤ 写整合包配置 ----
        // ★ 2026-10-06：type 必须与 QclModpackProvider.getName() 一致 ——
        //   ModpackHelper 的注册表就是按这个字符串查 provider 的。
        //   原来这里硬编码 "HMCL"，而 Provider 的 getName 已按用户要求改成 "QCL"，
        //   两边对不上会让装完的整合包认不出来。现在直接取 Provider 的返回值，从根上避免再次不一致。
        writeModpackConfig(versionDir, QclModpackProvider.INSTANCE.getName(),
                modpack == null ? null : modpack.getName(),
                modpack == null ? null : modpack.getVersion(),
                overrides);
        reportOverall(100);
    }

    /**
     * 把一段 patch json 叠到 versions/&lt;name&gt;/&lt;name&gt;.json 上。
     * ★ 不走「直接把 patch 写进去」是因为 patch 里的 inheritsFrom 往往指向一个
     *   本地根本不存在的父版本，启动时 LaunchVersion.fromDirectory 拿不到父级会崩。
     *
     * @return 是否真的合并了（文件缺失 / 已合过 / 解析失败都返回 false）
     */
    protected boolean mergeVersionJsonPatchText(File jsonFile, String patchJson, String patchId, int priority) {
        try {
            if (!jsonFile.isFile()) {
                return false;
            }
            Version base = gson().fromJson(
                    FileStringUtils.getStringFromFile(jsonFile.getAbsolutePath()), Version.class);
            Version patch = gson().fromJson(patchJson, Version.class);
            if (base == null || patch == null) {
                return false;
            }
            if (base.hasPatch(patchId)) {
                return false;   // 重复导入时不重复叠
            }
            patch.setId(patchId).setPriority(priority);
            Version merged = PatchMerger.mergePatch(base, patch);
            // ★★★【2026-10-06 修复】同上：必须显式 close，否则版本 json 会被截断（缓冲区没 flush）
            java.io.Writer w = Files.newBufferedWriter(jsonFile.toPath());
            try {
                gson().toJson(merged, w);
            } finally {
                w.close();
            }
            return true;
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "patch 合并失败（已跳过，不阻断导入）", t);
            return false;
        }
    }

    /** HMCL pack.json 的游戏版本：Modpack 记录的优先，否则按内容读 inheritsFrom / jar / id */
    protected String resolveHmclGameVersion(String packJson) {
        if (modpack != null && modpack.getGameVersion() != null && !modpack.getGameVersion().isEmpty()) {
            return modpack.getGameVersion();
        }
        try {
            JsonObject obj = new JsonParser().parse(packJson).getAsJsonObject();
            for (String key : new String[]{"inheritsFrom", "jar", "id"}) {
                if (obj.has(key) && !obj.get(key).isJsonNull()) {
                    String v = obj.get(key).getAsString();
                    if (v != null && !v.isEmpty()) {
                        return v;
                    }
                }
            }
        } catch (Throwable t) {
            android.util.Log.w("ModpackInstall", "pack.json 游戏版本解析失败", t);
        }
        return null;
    }
}
