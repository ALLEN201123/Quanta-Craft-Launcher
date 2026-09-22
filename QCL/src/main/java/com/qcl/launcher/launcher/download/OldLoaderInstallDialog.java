package com.qcl.launcher.launcher.download;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.babric.BabricInstallTask;
import com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.platform.Bits;

import java.io.File;

/**
 * ★ 1.2.5：给**已经装好的**版本补装「远古加载器」（Risugami's ModLoader / Babric）。
 *
 * 为什么单独做一个对话框：
 *   1.2.3 把这两个加载器做在了「下载 → 安装游戏」页（装新版本时勾选）。
 *   但玩家往往是**版本早就装好了**才想补加载器 —— 版本设置 → 自动安装 那一页
 *   原本只有 Forge / LiteLoader / OptiFine / Fabric / Quilt，没有这两个。
 *   所以这里补一个「就地安装」的入口，装法完全复用下载页那两条任务：
 *
 *   · ModLoader → {@link ModLoaderInstallTask}（往 minecraft.jar 里注入 class）
 *   · Babric    → {@link BabricInstallTask}（Fabric 系：下库 + 改 mainClass，b1.7.3 专用）
 *
 * UI 复用 {@code R.layout.dialog_install_game} 的下载任务列表，和下载页手感一致。
 */
public class OldLoaderInstallDialog extends Dialog implements View.OnClickListener {

    /** Risugami's ModLoader（远古，覆盖式装进 jar） */
    public static final String LOADER_MODLOADER = "modloader";

    /** Babric（b1.7.3 的 Fabric 分支） */
    public static final String LOADER_BABRIC = "babric";

    private final MainActivity activity;
    private final String versionName;
    private final String mcVersion;
    private final String loader;

    private DownloadTaskListAdapter adapter;
    private Button cancelButton;
    private TextView speedText;

    public OldLoaderInstallDialog(@NonNull Context context, MainActivity activity,
                                  String versionName, String mcVersion, String loader) {
        super(context);
        this.activity = activity;
        this.versionName = versionName;
        this.mcVersion = (mcVersion == null || mcVersion.isEmpty()) ? versionName : mcVersion;
        this.loader = loader;
        setContentView(R.layout.dialog_install_game);
        setCancelable(false);
        init();
    }

    private void init() {
        RecyclerView taskListView = findViewById(R.id.download_task_list);
        taskListView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DownloadTaskListAdapter(getContext());
        taskListView.setAdapter(adapter);
        speedText = findViewById(R.id.download_speed_text);
        cancelButton = findViewById(R.id.cancel_install_game);
        cancelButton.setOnClickListener(this);

        if (LOADER_MODLOADER.equals(loader)) {
            installModLoader();
        } else {
            installBabric();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == cancelButton) {
            dismiss();
            refreshVersionList();
        }
    }

    private File versionDir() {
        return new File(activity.launcherSetting.gameFileDirectory + "/versions/" + versionName);
    }

    private Gson gson() {
        return JsonUtils.defaultGsonBuilder()
                .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                .registerTypeAdapter(Bits.class, new Bits.Serializer())
                .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                .create();
    }

    private void installModLoader() {
        new ModLoaderInstallTask(activity, versionName, mcVersion, adapter,
                new ModLoaderInstallTask.Callback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onFinish(int injected) {
                        success("ModLoader 装好了，往游戏本体写入了 " + injected + " 个 class。\n"
                                + "这个版本已经自动打开「不检查游戏文件」，以后「修复版本」不会再把 jar 覆盖回去。");
                    }

                    @Override
                    public void onFailed(Exception e) {
                        failed(e);
                    }
                }).execute();
    }

    private void installBabric() {
        new BabricInstallTask(activity, adapter, mcVersion,
                new BabricInstallTask.InstallBabricCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onFailed(Exception e) {
                        failed(e);
                    }

                    @Override
                    public void onFinish(Version version) {
                        // ① 把 Babric 合进版本 json（它是 Fabric 系，靠 patch + 改 mainClass 生效）
                        mergePatchAndSave(version);
                        // ② 写标记（★ 1.2.5：带内容，不再是 0 字节空文件）
                        BabricInstallTask.writeMarker(versionDir(), mcVersion, version.getVersion(),
                                version.getLibraries() == null ? 0 : version.getLibraries().size());
                        // ③ 关掉这个版本的文件校验（加载器改了 json，哈希必然对不上）
                        disableFileCheck();
                        success("Babric 装好了（" + version.getVersion() + "，"
                                + (version.getLibraries() == null ? 0 : version.getLibraries().size())
                                + " 个依赖库）。\n"
                                + "这个版本已经自动打开「不检查游戏文件」，音效音乐照常会补下。");
                    }
                }).execute(new String[]{null});
    }

    /** 把加载器 patch 合进版本的 json（和下载页 installJson() 同一套写法） */
    private void mergePatchAndSave(Version patch) {
        try {
            String jsonPath = versionDir().getAbsolutePath() + "/" + versionName + ".json";
            Gson gson = gson();
            Version base = gson.fromJson(FileStringUtils.getStringFromFile(jsonPath), Version.class);
            Version merged = PatchMerger.mergePatch(base, patch);
            FileStringUtils.writeFile(jsonPath, gson.toJson(merged));
        } catch (Throwable t) {
            android.util.Log.e("OldLoaderInstall", "合并加载器 patch 失败", t);
        }
    }

    /** 关闭这个版本的「检查游戏文件」（复用 ModLoaderInstallTask 那套字段与存储） */
    private void disableFileCheck() {
        try {
            File dir = versionDir();
            File cfg = new File(dir, "qcl.cfg");
            PrivateGameSetting setting = GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
            if (setting == null) {
                PrivateGameSetting template = activity.privateGameSetting;
                if (template == null) {
                    return;
                }
                Gson gson = new Gson();
                setting = gson.fromJson(gson.toJson(template), PrivateGameSetting.class);
            }
            if (setting != null && !setting.notCheckMinecraft) {
                setting.notCheckMinecraft = true;
                GsonUtils.savePrivateGameSetting(setting, cfg.getAbsolutePath());
            }
        } catch (Throwable ignored) {
        }
    }

    private void success(String msg) {
        dismiss();
        refreshVersionList();
        refreshAutoInstallUI();
        new AlertDialog.Builder(getContext())
                .setTitle("安装完成")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_install_success_positive, (d, i) -> {
                })
                .create()
                .show();
    }

    private void failed(Exception e) {
        dismiss();
        refreshVersionList();
        String msg = e == null ? "未知错误" : e.getMessage();
        new AlertDialog.Builder(getContext())
                .setTitle("安装失败")
                .setMessage(msg == null ? "未知错误" : msg)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_install_success_positive, (d, i) -> {
                })
                .create()
                .show();
    }

    private void refreshVersionList() {
        try {
            new Thread(() -> activity.uiManager.versionListUI.refreshVersionList()).start();
        } catch (Throwable ignored) {
        }
    }

    private void refreshAutoInstallUI() {
        try {
            activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.refresh(versionName);
        } catch (Throwable ignored) {
        }
    }

    // ================== 卸载 ==================

    /**
     * 卸载这个版本上的远古加载器。
     *
     * · Babric：它是版本 json 里的一个 patch，走和 Forge / Fabric 完全一样的
     *   {@link PatchMerger#reMergePatch} 移除，再把标记文件删掉。
     * · ModLoader：它是「往 jar 里塞 class」的覆盖式安装，版本 json 里**没有记录**，
     *   没法靠 patch 还原。所以做法是：删标记 + 把被改过的 jar 改名成
     *   {@code xxx.jar.modloader.bak}（留着做备份，不直接删），
     *   启动前的文件检查发现 jar 不存在会自动补下**原版 jar**，
     *   等于下次启动就干净了。
     */
    public static void uninstall(Context context, MainActivity activity, String versionName,
                                 String loader) {
        File dir = new File(activity.launcherSetting.gameFileDirectory + "/versions/" + versionName);
        if (LOADER_BABRIC.equals(loader)) {
            try {
                String jsonPath = dir.getAbsolutePath() + "/" + versionName + ".json";
                Gson gson = JsonUtils.defaultGsonBuilder()
                        .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                        .registerTypeAdapter(Bits.class, new Bits.Serializer())
                        .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                        .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                        .create();
                Version v = gson.fromJson(FileStringUtils.getStringFromFile(jsonPath), Version.class);
                if (v != null) {
                    v = PatchMerger.reMergePatch(context, v, null, LOADER_BABRIC, () -> {
                    });
                    FileStringUtils.writeFile(jsonPath, gson.toJson(v));
                }
            } catch (Throwable t) {
                android.util.Log.e("OldLoaderInstall", "移除 Babric patch 失败", t);
            }
            new File(dir, BabricInstallTask.MARKER_NAME).delete();
        } else {
            new File(dir, ModLoaderInstallTask.MARKER_NAME).delete();
            File jar = new File(dir, versionName + ".jar");
            if (jar.isFile()) {
                File bak = new File(dir, versionName + ".jar.modloader.bak");
                if (bak.exists()) {
                    bak.delete();
                }
                jar.renameTo(bak);
            }
        }
        try {
            new Thread(() -> activity.uiManager.versionListUI.refreshVersionList()).start();
            activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.refresh(versionName);
        } catch (Throwable ignored) {
        }
    }
}
