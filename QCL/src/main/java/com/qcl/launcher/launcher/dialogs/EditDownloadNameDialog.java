package com.qcl.launcher.launcher.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.mod.ModClassInjector;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class EditDownloadNameDialog extends Dialog implements View.OnClickListener {
    private boolean alert;
    private String dir;
    private EditText editText;
    private Button negative;
    private Button positive;
    private DownloadResourceUI ui;
    private RemoteMod.Version version;
    /**
     * ★★★★★ 2026-10-11 新增：本次下载所属的**版本目录**（形如 {@code <gameDir>/versions/26.4-snapshot-2}）。
     *
     * <p>为什么要存下来：算"东西该下到哪"时必须知道**是哪个版本**，
     * 否则「版本隔离」（gameDirSetting.type==1）无从解析 —— 这正是
     * 「开了版本隔离后模组下到全局 mods、游戏里找不到」的根因。
     */
    private String versionPathForIsolation;

    public EditDownloadNameDialog(Context context, DownloadResourceUI downloadResourceUI, RemoteMod.Version version, boolean z, String str) {
        super(context);
        this.ui = downloadResourceUI;
        this.version = version;
        this.alert = z;
        this.dir = str;
        setContentView(R.layout.dialog_edit_download_name);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editText = (EditText) findViewById(R.id.download_name);
        this.positive = (Button) findViewById(R.id.download);
        this.negative = (Button) findViewById(R.id.cancel);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.editText.setText(this.version.getFile().getFilename());
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        StringBuilder append;
        PrivateGameSetting privateGameSetting;
        if (view == this.positive && !this.editText.getText().toString().equals("") && !this.editText.getText().toString().contains("/")) {
            String str = null;
            // ★ 2026-10-11：记下"本次下载属于哪个版本目录"，供下面的版本隔离解析使用
            this.versionPathForIsolation = null;
            if (this.ui.resourceType == 0) {
                if (this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion != null) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory + "/versions/" + this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion;
                }
            } else if (this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadResourcePackUI.gameVersion != null) {
                str = this.ui.activity.launcherSetting.gameFileDirectory + "/versions/" + this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadResourcePackUI.gameVersion;
            }
            if (str != null) {
                // ★ 2026-10-11：版本目录已知 ⇒ 记下来给版本隔离用
                this.versionPathForIsolation = str;
                String str2 = str + "/qcl.cfg";
                if (new File(str2).exists() && GsonUtils.getPrivateGameSettingFromFile(str2) != null && (GsonUtils.getPrivateGameSettingFromFile(str2).forceEnable || GsonUtils.getPrivateGameSettingFromFile(str2).enable)) {
                    privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(str2);
                } else {
                    privateGameSetting = this.ui.activity.privateGameSetting;
                }
                if (privateGameSetting.gameDirSetting.type == 0) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory;
                } else if (privateGameSetting.gameDirSetting.type != 1) {
                    str = privateGameSetting.gameDirSetting.path;
                }
            } else {
                PrivateGameSetting privateGameSetting2 = this.ui.activity.privateGameSetting;
                if (privateGameSetting2 == null
                        || privateGameSetting2.gameDirSetting == null) {
                    // 全新安装 / 设置文件损坏：造一份最小可用设置（照 GameLaunchSetting 的兜底写法）。
                    // ★ 默认 GameDirSetting(1, "") = **版本隔离**，这样模组会进
                    //   versions/<版本>/mods/ —— 与「版本隔离」这个默认行为的语义一致。
                    try {
                        privateGameSetting2 = new PrivateGameSetting(
                                false, true, true, false, false, false, false,
                                new com.qcl.launcher.launcher.setting.game.child.JavaSetting(true, "default"),
                                "", "", "",
                                new com.qcl.launcher.launcher.setting.game.child.GameDirSetting(1, ""),
                                new com.qcl.launcher.launcher.setting.game.child.BoatLauncherSetting(false, "default", "default"),
                                new com.qcl.launcher.launcher.setting.game.child.PojavLauncherSetting(true,
                                        com.qcl.launcher.launcher.launch.RendererCompat.defaultRendererId(), "default"),
                                new com.qcl.launcher.launcher.setting.game.child.RamSetting(0, 0, true),
                                "Default", 1.0f);
                    } catch (Throwable ignored) {
                        // 造不出来就退回游戏根目录（绝不让下载因为读设置失败而中断）
                        privateGameSetting2 = null;
                    }
                }
                if (privateGameSetting2 == null || privateGameSetting2.gameDirSetting == null) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory;
                } else
                // ★★★★★ 2026-10-11 修（用户实测「开启全局版本隔离后，模组下载的位置不匹配：
                //   东西没进 versions/<版本>/mods，根本找不到」）：
                //
                //   真凶：原来这里**只看 gameDirSetting.type，完全没算"版本隔离"** ——
                //     type == 0 或 1 一律给 `launcherSetting.gameFileDirectory`（游戏根目录）
                //     ⇒ 模组落到全局 `.minecraft/mods/`，而开了版本隔离的版本只读
                //       `versions/<版本>/mods/` ⇒ 游戏里一个都看不到。
                //
                //   正确算法与 BaseModpackInstallTask.runDir() 完全一致：
                //     ① 版本目录存在 + qcl.cfg 开了独立设置 ⇒ 用版本自己的设置；
                //     ② 否则用全局设置；
                //     ③ 最后统一交给 PrivateGameSetting.getGameDir(root, vdir, setting) ——
                //        **必须把 vdir（版本目录）传进去**，type==1（版本隔离）才会解析成版本目录。
                if (this.versionPathForIsolation != null && !this.versionPathForIsolation.isEmpty()) {
                    str = com.qcl.launcher.launcher.setting.game.PrivateGameSetting.getGameDir(
                            this.ui.activity.launcherSetting.gameFileDirectory,
                            this.versionPathForIsolation,
                            privateGameSetting2.gameDirSetting);
                } else if (privateGameSetting2.gameDirSetting.type == 0
                        || privateGameSetting2.gameDirSetting.type == 1) {
                    // 没有版本上下文（极少见）：隔离时也无从下手，退回游戏根目录
                    str = this.ui.activity.launcherSetting.gameFileDirectory;
                } else {
                    str = privateGameSetting2.gameDirSetting.path;
                }
            }
            FileUtils.createDirectory(str + (this.ui.resourceType == 0 ? "/mods/" : "/resourcepacks/"));
            String name = this.version.getName();
            String url = this.version.getFile().getUrl();
            if (this.dir == null) {
                append = new StringBuilder().append(str).append(this.ui.resourceType != 0 ? "/resourcepacks/" : "/mods/");
            } else {
                append = new StringBuilder().append(this.dir).append("/");
            }
            // ★ 1.2.7：模组文件也走国内镜像（cdn.modrinth.com 在国内外时好时坏），
            //   镜像失败时下载器会自动重试 fallback（官方原始地址）。
            DownloadTaskListBean downloadTaskListBean = new DownloadTaskListBean(name,
                    com.qcl.launcher.utils.io.MirrorUtils.rewriteCdn(url),
                    append.append(this.editText.getText().toString()).toString(), "")
                    .withFallback(url);
            ArrayList arrayList = new ArrayList();
            arrayList.add(downloadTaskListBean);
            // ★★★ 1.2.5：模组有前置就**一起排队下载**（只对模组做，资源包/世界没有前置）。
            //   以前只下本体，前置得玩家自己回详情页一个个找 —— 漏装前置的模组进游戏就是崩。
            //   每个前置都自动挑「支持你当前游戏版本」的最新那一个版本。
            if (this.ui.resourceType == 0) {
                try {
                    appendDependencyTasks(arrayList, str + "/mods/");
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            DownloadDialog downloadDialog = new DownloadDialog(getContext(), this.ui.activity, arrayList, this.alert);
            dismiss();
            // ★★★ 1.2.3：模组下载完成后的「class 替换型模组」处理。
            //   只有**下模组**（resourceType == 0）才做；资源包/世界等直接放对应目录。
            //   判据：zip 里有 fabric.mod.json / mcmod.info 等 → 加载器模组，留 mods/；
            //        没有元数据但带 .class → 裸改本体的远古模组 → 注入本体 jar。
            //   注入前先查 qcl_jarmods.json 登记表，撞了别的模组的 class 就弹窗问玩家。
            if (this.ui.resourceType == 0) {
                String gv = this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion;
                if (gv == null || gv.isEmpty()) {
                    // ★ 1.2.5 修：currentVersion 存的是**完整路径**，
                    //   直接拿去拼 <游戏目录>/versions/<gv> 会拼出不存在的路径
                    //   （class 型模组的注入目标就找错了），这里只取目录名。
                    gv = new java.io.File(this.ui.activity.publicGameSetting.currentVersion).getName();
                }
                final String versionDirPath = this.ui.activity.launcherSetting.gameFileDirectory
                        + "/versions/" + gv;
                final String savePath = downloadTaskListBean.path;
                final String modName = this.editText.getText().toString();
                final MainActivity mainActivity = this.ui.activity;
                downloadDialog.setOnComplete(() -> new Thread(() -> {
                    try {
                        File modFile = new File(savePath);
                        if (!modFile.isFile()) {
                            return;
                        }
                        // 加载器模组（有元数据）→ 留在 mods/，不动
                        if (ModClassInjector.isLoaderMod(modFile)) {
                            return;
                        }
                        List<String> classes = ModClassInjector.listClasses(modFile);
                        if (classes.isEmpty()) {
                            return;   // 没有 class，普通资源
                        }
                        File versionDir = new File(versionDirPath);
                        // ★★★ 1.2.5：装了「现代」加载器（Babric / Fabric / Forge / NeoForge /
                        //   Quilt / LiteLoader）的版本**一律不注入本体** —— 这些加载器的模组
                        //   必须待在 mods/ 里由加载器自己加载。（只有 Risugami's ModLoader
                        //   版本、以及什么都没装的远古版本，才存在「裸改本体 class」的玩法。）
                        //   这一步是兜底：万一上面元数据没认出来，也绝不把
                        //   fabric/forge/babric 的模组塞进本体 jar。
                        String loader = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.detect(versionDir);
                        if (loader != null
                                && !com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.MODLOADER.equals(loader)) {
                            return;
                        }
                        Map<String, String> registry = ModClassInjector.loadRegistry(versionDir);
                        final List<String> conflicts =
                                ModClassInjector.findConflicts(registry, classes, modName);
                        Runnable doInject = () -> {
                            try {
                                ModClassInjector.inject(versionDir, modFile, modName);
                                //noinspection ResultOfMethodCallIgnored
                                modFile.delete();   // 注入完从 mods/ 移除
                                disableFileCheck(versionDir);
                                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity,
                                        "已把 " + modName + " 的 " + classes.size()
                                                + " 个 class 注入本体", Toast.LENGTH_LONG).show());
                            } catch (Exception ex) {
                                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity,
                                        "注入失败：" + ex.getMessage(), Toast.LENGTH_LONG).show());
                            }
                        };
                        if (!conflicts.isEmpty()) {
                            StringBuilder msg = new StringBuilder();
                            for (String c : conflicts) {
                                msg.append(c).append("\n");
                            }
                            mainActivity.runOnUiThread(() -> new AlertDialog.Builder(mainActivity)
                                    .setTitle("检测到 class 冲突")
                                    .setMessage("以下 class 已被其他模组占用：\n\n" + msg
                                            + "\n继续会把它们覆盖成新模组的版本。是否继续？")
                                    .setPositiveButton("继续", (dlg, w) -> doInject.run())
                                    .setNegativeButton("取消", (dlg, w) -> {
                                        //noinspection ResultOfMethodCallIgnored
                                        modFile.delete();
                                        Toast.makeText(mainActivity, "已取消，模组文件已删除",
                                                Toast.LENGTH_LONG).show();
                                    })
                                    .show());
                        } else {
                            doInject.run();
                        }
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }).start());
            }
            downloadDialog.show();
        }
        if (view == this.negative) {
            dismiss();
        }
    }

    /**
     * ★★★★★ 1.5.2 重写：把「这个模组的前置」以及**前置的前置**都加进同一批下载任务。
     *
     * <p>用户要求：「反正前置的前置只下载必须，所有下载模组自动下载前置只下载必须，不下载可选」。
     *
     * <p>规则（**每一层都一样**）：
     * <ul>
     *   <li>{@code required} → 下载；</li>
     *   <li>{@code optional} / {@code incompatible} / {@code embedded} → **跳过**；</li>
     *   <li>类型未知（null）→ 按必需处理（保守，宁可多装必要件也不让玩家缺前置）。</li>
     * </ul>
     *
     * <p>递归与去重：
     * <ul>
     *   <li>深度上限 {@link #MAX_DEP_DEPTH}（MC 依赖链一般 2~3 层）；</li>
     *   <li>{@code visited} 用 **project id** 记录，防 A→B→A 循环与重复；</li>
     *   <li>去重范围覆盖：本次任务表 + 当前版本 mods/ + 所有 versions/&lt;名字&gt;/mods/ + 共享 mods/，
     *       按文件名**和** slug 双重判断。</li>
     * </ul>
     *
     * @param out     下载任务表（本体的那条已经在里面了）
     * @param modsDir 当前版本的模组目录（mods/）
     */
    private void appendDependencyTasks(List<DownloadTaskListBean> out, String modsDir) {
        List<RemoteMod> deps = this.ui.getDependencies();
        if (deps == null || deps.isEmpty()) {
            return;
        }
        String mcv = com.qcl.launcher.launcher.setting.SettingUtils.getCurrentGameVersion(this.ui.activity);
        java.util.Set<String> visited = new java.util.HashSet<String>();

        for (RemoteMod dep : deps) {
            try {
                String depId = dep.getData() == null ? null : dep.getData().getRemoteId();
                String depType = depId == null ? null : this.ui.getDependencyTypes().get(depId);
                // ★ 第一层：只下必需
                if (!isRequiredDependency(depType)) {
                    continue;
                }
                if (depId != null) {
                    visited.add(depId);
                }
                RemoteMod.Version best = pickDependencyVersion(dep, mcv);
                if (best == null) {
                    continue;
                }
                if (enqueueDependency(out, modsDir, dep, best)) {
                    // ★ 只有真的排进队列，才继续往下钻（避免为已装的前置白查一堆网络）
                    collectTransitiveDependencies(out, modsDir, mcv, best, visited, 1);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** 依赖递归深度上限（防病态数据爆栈）。 */
    private static final int MAX_DEP_DEPTH = 5;

    /**
     * ★ 1.5.2：递归收集「前置的前置」。
     *
     * @param parentVer 上一层那个前置**已选中的版本**（依赖信息挂在 Version 上）
     * @param depth     当前深度（1 = 第一层前置的前置）
     */
    private void collectTransitiveDependencies(List<DownloadTaskListBean> out, String modsDir,
                                               String mcv, RemoteMod.Version parentVer,
                                               java.util.Set<String> visited, int depth) {
        if (depth > MAX_DEP_DEPTH || parentVer == null) {
            return;
        }
        List<String> ids;
        try {
            ids = parentVer.getDependencies();
        } catch (Throwable t) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            try {
                if (id == null || visited.contains(id)) {
                    continue;
                }
                visited.add(id);
                // ★ 每一层都只下必需
                if (!isRequiredDependency(parentVer.getDependencyType(id))) {
                    continue;
                }
                RemoteMod depMod = this.ui.getRepository().getModById(id);
                if (depMod == null) {
                    continue;
                }
                RemoteMod.Version v = pickDependencyVersion(depMod, mcv);
                if (v == null) {
                    continue;
                }
                if (enqueueDependency(out, modsDir, depMod, v)) {
                    collectTransitiveDependencies(out, modsDir, mcv, v, visited, depth + 1);
                }
            } catch (Throwable ignored) {
                // 单个依赖查询/入队失败不影响其它
            }
        }
    }

    /** ★ 1.5.2：只认 required；null（未知）按必需处理。 */
    private static boolean isRequiredDependency(String type) {
        if (type == null || type.isEmpty()) {
            return true;
        }
        return "required".equalsIgnoreCase(type);
    }

    /**
     * ★ 1.5.2：把一个前置排进下载队列。
     *
     * @return true = 真的排进去了（调用方可以继续递归它的依赖）
     */
    private boolean enqueueDependency(List<DownloadTaskListBean> out, String modsDir,
                                      RemoteMod mod, RemoteMod.Version ver) {
        if (ver == null || ver.getFile() == null || ver.getFile().getUrl() == null) {
            return false;
        }
        String fileName = ver.getFile().getFilename();
        if (fileName == null || fileName.isEmpty()) {
            fileName = mod.getSlug() + ".jar";
        }
        String slug = mod.getSlug();

        // ★ 跨版本去重：当前版本 + 所有 versions/*/mods/ + 共享 mods/
        if (isModInstalledAnywhere(modsDir, fileName, slug)) {
            return false;
        }
        // 本次任务表去重
        for (DownloadTaskListBean bean : out) {
            if (fileName.equals(bean.name)) {
                return false;
            }
        }
        out.add(new DownloadTaskListBean(fileName,
                com.qcl.launcher.utils.io.MirrorUtils.rewriteCdn(ver.getFile().getUrl()),
                modsDir + fileName, "").withFallback(ver.getFile().getUrl()));
        return true;
    }

    /**
     * ★ 1.5.2：这个模组是否已经装在**任意位置**（含其它版本的独立 mods/ 目录）。
     *
     * <p>用户要求：「如果检测到玩家各版本列表、版本独立列表有这个模组，就不再多余下载」。
     */
    private boolean isModInstalledAnywhere(String currentModsDir, String fileName, String slug) {
        // ① 当前版本的 mods/
        if (modsContains(currentModsDir, fileName, slug)) {
            return true;
        }
        try {
            String gameDir = this.ui.activity.launcherSetting.gameFileDirectory;
            if (gameDir == null || gameDir.isEmpty()) {
                return false;
            }
            File root = new File(gameDir);
            // ② 所有版本独立目录 versions/<name>/mods/
            File versionsDir = new File(root, "versions");
            File[] versionDirs = versionsDir.listFiles();
            if (versionDirs != null) {
                for (File vd : versionDirs) {
                    if (vd == null || !vd.isDirectory()) {
                        continue;
                    }
                    if (modsContains(new File(vd, "mods").getAbsolutePath(), fileName, slug)) {
                        return true;
                    }
                }
            }
            // ③ 共享 mods/（老布局）
            if (modsContains(new File(root, "mods").getAbsolutePath(), fileName, slug)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** mods/ 里是否有这个文件，或同 slug 的任意版本。 */
    private static boolean modsContains(String modsDirPath, String fileName, String slug) {
        try {
            if (modsDirPath == null || modsDirPath.isEmpty()) {
                return false;
            }
            File modsDir = new File(modsDirPath);
            if (!modsDir.isDirectory()) {
                return false;
            }
            if (fileName != null && !fileName.isEmpty() && new File(modsDir, fileName).exists()) {
                return true;
            }
            return slug != null && !slug.isEmpty() && hasModBySlug(modsDir, slug);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * ★★★ 1.3.8：判断 mods/ 里是否已有某个 slug 的模组。
     * 文件名以「slug + 分隔符（- _ .）」开头，或就叫 slug(.jar) 都算命中。
     * 用于前置去重 —— 避免同一个前置换个版本号又被下载一份。
     */
    private static boolean hasModBySlug(File modsDir, String slug) {
        try {
            if (modsDir == null || !modsDir.isDirectory()) {
                return false;
            }
            File[] files = modsDir.listFiles();
            if (files == null) {
                return false;
            }
            String lower = slug.toLowerCase(java.util.Locale.ROOT);
            for (File f : files) {
                if (f == null || !f.isFile()) {
                    continue;
                }
                String n = f.getName().toLowerCase(java.util.Locale.ROOT);
                if (n.equals(lower) || n.equals(lower + ".jar")
                        || n.startsWith(lower + "-") || n.startsWith(lower + "_") || n.startsWith(lower + ".")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** ★ 1.2.5：挑前置里「支持 mcv 这个游戏版本」的最新一个版本（挑不到就返回 null，跳过） */
    private RemoteMod.Version pickDependencyVersion(RemoteMod dep, String mcv) throws java.io.IOException {
        List<RemoteMod.Version> all = dep.getData().loadVersions(this.ui.getRepository())
                .collect(java.util.stream.Collectors.toList());
        RemoteMod.Version best = null;
        for (RemoteMod.Version v : all) {
            if (mcv != null && !mcv.isEmpty() && (v.getGameVersions() == null || !v.getGameVersions().contains(mcv))) {
                continue;
            }
            if (best == null || (best.getDatePublished() != null && v.getDatePublished() != null
                    && v.getDatePublished().after(best.getDatePublished()))) {
                best = v;
            }
        }
        return best;
    }

    /** ★ 1.2.3：注入 class 后这个版本必然校验不过，自动关「检查游戏完整性」 */
    private void disableFileCheck(File versionDir) {
        try {
            File cfg = new File(versionDir, "qcl.cfg");
            PrivateGameSetting st = GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
            if (st == null) {
                PrivateGameSetting tpl = this.ui.activity.privateGameSetting;
                if (tpl != null) {
                    com.google.gson.Gson g = new com.google.gson.Gson();
                    st = g.fromJson(g.toJson(tpl), PrivateGameSetting.class);
                }
            }
            if (st != null && !st.notCheckMinecraft) {
                st.notCheckMinecraft = true;
                GsonUtils.savePrivateGameSetting(st, cfg.getAbsolutePath());
            }
        } catch (Throwable ignored) {
        }
    }
}
