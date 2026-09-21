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
            if (this.ui.resourceType == 0) {
                if (this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion != null) {
                    str = this.ui.activity.launcherSetting.gameFileDirectory + "/versions/" + this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadModUI.gameVersion;
                }
            } else if (this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadResourcePackUI.gameVersion != null) {
                str = this.ui.activity.launcherSetting.gameFileDirectory + "/versions/" + this.ui.activity.uiManager.downloadUI.downloadUIManager.downloadResourcePackUI.gameVersion;
            }
            if (str != null) {
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
                if (privateGameSetting2.gameDirSetting.type == 0 || privateGameSetting2.gameDirSetting.type == 1) {
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
            DownloadTaskListBean downloadTaskListBean = new DownloadTaskListBean(name, url, append.append(this.editText.getText().toString()).toString(), "");
            ArrayList arrayList = new ArrayList();
            arrayList.add(downloadTaskListBean);
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
                    gv = this.ui.activity.publicGameSetting.currentVersion;
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
