package com.qcl.launcher.launcher.download.modloader;

import android.os.AsyncTask;

import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.io.DownloadUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * ★ 1.2.3：给远古版本装 Risugami's ModLoader（自动下载 + 覆盖进 minecraft.jar）。
 *
 * 【装法是「文件覆盖」】
 * 实测 ModLoader 的压缩包里就是一堆 .class：
 *   1.6.2  → BaseMod.class / ModLoader.class / EntityRendererProxy.class / ...（26 个）
 *   b1.7.3 → yv.class / BaseMod.class / hl.class / ...（15 个，含混淆名）
 * 所以装法就是：把这些 .class 解出来，**直接写进 minecraft.jar**（同名覆盖）。
 * 不需要 tweakClass、不需要往 libraries 里加东西、不需要改版本 json ——
 * 这也是它和 Forge / Fabric 那类加载器最本质的区别。
 *
 * 【下载源】
 * MCArchive 直链（URL 由 SHA-256 拼出），下载后校验 SHA-256。
 * 见 {@link ModLoaderVersions} 里的说明与实测记录。
 *
 * 【为什么单独做一个任务】
 * 远古版本（b1.7.3 那批）只有原版一条路可走，玩家想用 ModLoader 就得自己去
 * 论坛翻 MediaFire、手动拿压缩软件往 jar 里塞 —— 这本来就不该让玩家干。
 */
public class ModLoaderInstallTask extends AsyncTask<Object, Integer, Exception> {

    public interface Callback {
        void onStart();

        /** 成功，injected 是写进 jar 的 class 数量 */
        void onFinish(int injected);

        void onFailed(Exception e);
    }

    /**
     * ★★★ 标记文件：装在版本目录下的 {@code .modloader}。
     *
     * 为什么需要它：
     * ModLoader 是**文件覆盖式**安装 —— 它把 class 直接写进 minecraft.jar。
     * 这样一来 jar 就和官方 SHA-1 对不上了。虽然启动前的检查只在
     * 「jar 缺失或 0 字节」时才补下（不会按哈希重下），但只要玩家做一次
     * 「重装 / 修复这个版本」，jar 就会被原版覆盖，**ModLoader 白装**。
     *
     * 所以按用户要求：**装了 ModLoader 就给这个版本打标记，
     * 让后续的版本文件校验/修复跳过它**（见 CheckLibTask 里的判断）。
     *
     * 文件内容是几行纯文本，方便人工排查：
     *   mcVersion=b1.7.3
     *   sha256=<ModLoader 包的校验值>
     *   injected=15
     *   time=...
     */
    public static final String MARKER_NAME = ".modloader";

    /** 某个版本是否装过 ModLoader */
    public static boolean isModLoaderInstalled(String gameDir, String versionId) {
        return new File(new File(gameDir, "versions" + File.separator + versionId), MARKER_NAME).isFile();
    }

    /** 版本目录路径 */
    public static File versionDirOf(String gameDir, String versionId) {
        return new File(gameDir, "versions" + File.separator + versionId);
    }

    private static void writeMarker(File versionDir, String mcVersion, String sha256, int injected)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("mcVersion=").append(mcVersion).append('\n');
        sb.append("sha256=").append(sha256).append('\n');
        sb.append("injected=").append(injected).append('\n');
        sb.append("time=").append(new java.util.Date()).append('\n');
        try (OutputStream out = new FileOutputStream(new File(versionDir, MARKER_NAME))) {
            out.write(sb.toString().getBytes("UTF-8"));
        }
    }

    /**
     * 把**这个版本**的「不检查游戏文件」打开。
     *
     * 复用启动器已有的 per-version 开关，不新增字段：
     *   - 存储：{@code versions/<id>/qcl.cfg}（该版本自己的配置文件）
     *   - 字段：{@code PrivateGameSetting.notCheckMinecraft}
     *   - 生效点：{@code CheckLibTask:69} —— `if (privateGameSetting.notCheckMinecraft)`
     *   - 玩家入口：版本设置 → 「检查游戏文件」开关
     *
     * ★ 作用域就是这一个版本，不会影响其它版本（不是全局 private_game_setting.json）。
     */
    private void disableFileCheckForThisVersion(File versionDir) throws IOException {
        File cfg = new File(versionDir, "qcl.cfg");
        PrivateGameSetting setting = GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
        if (setting == null) {
            // 这个版本还没自己的配置：拿当前设置深拷贝一份当模板
            PrivateGameSetting template = activity.privateGameSetting;
            if (template == null) {
                android.util.Log.w("ModLoader", "拿不到设置模板，跳过自动关闭校验");
                return;
            }
            Gson gson = new Gson();
            setting = gson.fromJson(gson.toJson(template), PrivateGameSetting.class);
            if (setting == null) {
                return;
            }
        }
        setting.notCheckMinecraft = true;
        GsonUtils.savePrivateGameSetting(setting, cfg.getAbsolutePath());
        android.util.Log.i("ModLoader", "已关闭版本 " + versionId + " 的文件校验（写入 " + cfg.getAbsolutePath() + "）");
    }

    private final MainActivity activity;
    /** 版本目录名（versions/&lt;versionId&gt;/&lt;versionId&gt;.jar） */
    private final String versionId;
    /** 用来查 ModLoader 版本表，一般是基础 MC 版本（b1.7.3 等） */
    private final String mcVersion;
    private final DownloadTaskListAdapter adapter;
    private final Callback callback;

    /** 进度列表里的那一行 */
    private final DownloadTaskListBean bean;

    /** 下载下来的压缩包放这儿 */
    private final File cacheFile;

    public ModLoaderInstallTask(MainActivity activity, String versionId, String mcVersion,
                                DownloadTaskListAdapter adapter, Callback callback) {
        this.activity = activity;
        this.versionId = versionId;
        this.mcVersion = mcVersion;
        this.adapter = adapter;
        this.callback = callback;
        this.bean = new DownloadTaskListBean("ModLoader", "", "", "");
        this.cacheFile = new File(activity.getCacheDir(), "modloader-" + versionId + ".zip");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (callback != null) {
            callback.onStart();
        }
        if (adapter != null && !isCancelled()) {
            adapter.addDownloadTask(bean);
        }
    }

    @Override
    protected Exception doInBackground(Object... objArr) {
        try {
            ModLoaderVersions.Entry entry = ModLoaderVersions.find(mcVersion);
            if (entry == null) {
                throw new IOException("这个版本（" + mcVersion + "）没有对应的 ModLoader。");
            }
            if (!entry.isInstallable()) {
                throw new IOException("ModLoader " + entry.mcVersion + " 官方只发了 .rar 格式的包，"
                        + "启动器现在解不开 rar，暂时装不了。");
            }

            File jar = new File(new File(activity.launcherSetting.gameFileDirectory,
                    "versions" + File.separator + versionId), versionId + ".jar");
            if (!jar.isFile()) {
                throw new IOException("找不到游戏本体：" + jar.getAbsolutePath()
                        + "\n请先把这个版本装好（要有 jar），再装 ModLoader。");
            }

            // ---- 1) 下载（已有且校验通过就不重复下）----
            //   ★ b1.7.3 用的是 ModloaderFix（GitHub Release 没提供 sha256），这种没有校验值的
            //     条目就只判断文件在不在，不再比对哈希。
            boolean cacheUsable = cacheFile.isFile()
                    && (!entry.hasChecksum() || entry.sha256.equalsIgnoreCase(sha256Hex(cacheFile)));
            if (!cacheUsable) {
                bean.name = "下载 ModLoader " + entry.mcVersion;
                if (adapter != null) {
                    activity.runOnUiThread(() -> {
                        if (!isCancelled()) adapter.onProgress(bean);
                    });
                }
                boolean ok = DownloadUtil.downloadFile(entry.getUrl(), cacheFile.getAbsolutePath(),
                        null, new DownloadTask.DownloadFeedback() {
                            @Override
                            public void updateProgress(long curr, long max) {
                                if (max <= 0) return;
                                bean.progress = (int) (100 * curr / max);
                                if (adapter != null) {
                                    activity.runOnUiThread(() -> {
                                        if (!isCancelled()) adapter.onProgress(bean);
                                    });
                                }
                            }

                            @Override
                            public void updateSpeed(String speed) {
                            }
                        });
                if (!ok) {
                    throw new IOException("ModLoader 下载失败：" + entry.getUrl());
                }
                // ---- 2) 校验 SHA-256（没有校验值的条目跳过，比如 b1.7.3 的 ModloaderFix）----
                if (entry.hasChecksum()) {
                    String got = sha256Hex(cacheFile);
                    if (!entry.sha256.equalsIgnoreCase(got)) {
                        //noinspection ResultOfMethodCallIgnored
                        cacheFile.delete();
                        throw new IOException("ModLoader 校验失败（SHA-256 不一致）\n期望 " + entry.sha256
                                + "\n实际 " + got);
                    }
                }
            }

            // ---- 3) 覆盖进 minecraft.jar ----
            activity.runOnUiThread(() -> {
                bean.name = "写入 minecraft.jar";
                bean.progress = 0;
                if (adapter != null && !isCancelled()) {
                    adapter.onProgress(bean);
                }
            });
            int injected = injectClasses(cacheFile, jar);

            // ★ 打标记：记录这个版本装过 ModLoader（人工排查 + UI 判断用）
            File vDir = versionDirOf(activity.launcherSetting.gameFileDirectory, versionId);
            writeMarker(vDir, entry.mcVersion, entry.sha256, injected);

            // ★★★ 自动关闭「该版本」的文件校验 —— 用启动器**已有的**开关，不另造轮子。
            //   版本设置里本来就有「检查游戏文件」这个开关（CheckLibTask:69 判断
            //   privateGameSetting.notCheckMinecraft），这里装完 ModLoader 就自动把它打开；
            //   玩家想恢复校验，去「版本设置 → 检查游戏文件」关回去即可。
            //
            //   为什么必须开：ModLoader 是文件覆盖式安装，jar 和官方 SHA-1 已对不上
            //   （而且还按官方装法删了 META-INF），不关掉的话一次「修复版本」
            //   就会拿原版把 jar 盖掉，ModLoader 白装。
            //
            //   ★★ 注意作用域：写的是 **versions/<id>/qcl.cfg**，这是**该版本自己的**配置文件，
            //      不是全局的 private_game_setting.json，所以只影响这一个版本，
            //      绝对不会「全版本互通」。
            try {
                disableFileCheckForThisVersion(vDir);
            } catch (Throwable t) {
                // 关不掉也不能让整个安装失败：标记已经写下了，下次启动还会提示
                android.util.Log.w("ModLoader", "自动关闭该版本文件校验失败（不影响 ModLoader 已装好）", t);
            }

            if (adapter != null) {
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.onComplete(bean);
                });
            }
            if (callback != null) {
                activity.runOnUiThread(() -> callback.onFinish(injected));
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            if (adapter != null) {
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.onComplete(bean);
                });
            }
            if (callback != null) {
                activity.runOnUiThread(() -> callback.onFailed(e));
            }
            return e;
        }
    }

    /**
     * 把 ModLoader 包里的所有 .class 覆盖进目标 jar。
     *
     * 做法：先把目标 jar 的条目全部读进内存 → 用 ModLoader 的 class 覆盖同名项 →
     * 写到临时文件 → 替换原 jar。
     * ★ 必须先写临时文件再替换：直接就地改 jar，中途失败会把玩家的游戏本体写坏。
     *
     * @return 写进去的 class 数量
     */
    private int injectClasses(File classZip, File targetJar) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<String, byte[]>();

        // 1) 读原 jar 的全部条目
        // ★★★ 关键：**跳过 META-INF**。原版 minecraft.jar 是签过名的，
        //   里面有 META-INF/MOJANG_C.*（签名证书）和 META-INF/*.SF / *.RSA（摘要）。
        //   我们往 jar 里塞了新 class 之后，签名和内容就对不上了，JVM 启动时会直接抛：
        //     java.lang.SecurityException: class "xxx"'s signer information
        //     does not match signer information of other classes in the same package
        //   远古时代的官方装法（MCF 教程、FTB Wiki）里都明确写着
        //   「Delete META-INF folder in the jar」/「rm META-INF/MOJANG_C.*」，
        //   所以这里把 META-INF 整目录丢掉（MC 是用命令行指定主类启动的，不依赖 MANIFEST）。
        int stripped = 0;
        try (ZipFile jar = new ZipFile(targetJar)) {
            java.util.Enumeration<? extends ZipEntry> it = jar.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                if (e.getName().toUpperCase().startsWith("META-INF/")) {
                    stripped++;
                    continue;
                }
                entries.put(e.getName(), readAll(jar.getInputStream(e)));
            }
        }
        if (stripped > 0) {
            android.util.Log.i("ModLoader", "已移除 " + stripped + " 个 META-INF 签名条目（改 jar 后签名必然失效）");
        }

        // 2) 用 ModLoader 的 class 覆盖
        int injected = 0;
        try (ZipFile ml = new ZipFile(classZip)) {
            java.util.Enumeration<? extends ZipEntry> it = ml.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                if (e.isDirectory() || !e.getName().toLowerCase().endsWith(".class")) {
                    continue;
                }
                entries.put(e.getName(), readAll(ml.getInputStream(e)));
                injected++;
            }
        }
        if (injected == 0) {
            throw new IOException("这个 ModLoader 包里没有 .class，可能下错文件了：" + classZip);
        }

        // 3) 先写临时文件
        File tmp = new File(targetJar.getParentFile(), targetJar.getName() + ".mltmp");
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tmp))) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                ze.setTime(0L);   // 固定时间戳，保证可复现
                out.putNextEntry(ze);
                out.write(e.getValue());
                out.closeEntry();
            }
        }

        // 4) 替换原 jar（先备份再换，出问题还能回滚）
        File backup = new File(targetJar.getParentFile(), targetJar.getName() + ".mlbak");
        //noinspection ResultOfMethodCallIgnored
        backup.delete();
        if (!targetJar.renameTo(backup)) {
            throw new IOException("无法备份原 jar：" + targetJar);
        }
        if (!tmp.renameTo(targetJar)) {
            // 换不回去就把备份恢复，不能让玩家丢掉游戏本体
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(targetJar);
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            throw new IOException("写入 jar 失败，已恢复原文件：" + targetJar);
        }
        //noinspection ResultOfMethodCallIgnored
        backup.delete();
        return injected;
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static String sha256Hex(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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
            throw new IOException("SHA-256 计算失败: " + file, e);
        }
    }
}
