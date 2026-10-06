package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.io.MirrorUtils;
import com.qcl.launcher.utils.string.StringUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * ★ 2026-10-06 新增：**下载远程整合包（CurseForge / Modrinth）并自动安装**。
 *
 * <p><b>为什么需要它</b>（用户实测）：
 *   下载页「整合包」那一栏列出一堆整合包，点进去选个版本点一下 —— **什么都不发生**。
 *   根因：版本点击的处理只覆盖了 模组(0) / 资源包与光影(2) / 世界(3)，
 *   而**整合包是 resourceType == 1**，两个分支都不匹配 → 静默无响应，下载框永远不弹。
 *
 * <p><b>它做什么</b>（照 FCL 的 ModpackInstaller 流程）：
 *   ① 让玩家给整合包起个版本名（默认取包文件名）
 *   ② 下载整包 zip 到 {@code <游戏目录>/modpacks/<名字>.zip}
 *   ③ 下载完成后，把 zip 交给 {@link com.qcl.launcher.launcher.uis.game.version.universal.InstallPackageUI#installDownloadedZip}
 *      —— 由它按 manifest 分发到 MultiMC / Curse / Modrinth / MCBBS / QCL / 兜底 各安装任务
 *
 * <p><b>为什么不复用 EditDownloadNameDialog</b>：那个是给「模组」用的 —— 它的落盘逻辑是
 * "下个文件丢进 mods/ 或 resourcepacks/"，而整合包要"整包装出一个新版本"，
 * 两者终点完全不同（用户反馈过：整合包按资源包逻辑处理会串味）。
 */
public class ModpackDownloadDialog extends Dialog implements View.OnClickListener {

    private final MainActivity activity;
    private final RemoteMod.Version version;
    private final RemoteMod bean;
    private EditText editText;
    private Button positive;
    private Button negative;

    /** 从「整合包详情页」拉起下载对话框的便捷入口。 */
    public static void show(Context context, DownloadResourceUI ui, RemoteMod.Version version) {
        new ModpackDownloadDialog(context, ui, version).show();
    }

    private ModpackDownloadDialog(Context context, DownloadResourceUI ui, RemoteMod.Version version) {
        super(context);
        this.activity = ui.activity;
        this.version = version;
        this.bean = ui.bean;
        setContentView(R.layout.dialog_edit_download_name);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editText = findViewById(R.id.download_name);
        this.positive = findViewById(R.id.download);
        this.negative = findViewById(R.id.cancel);
        if (this.positive != null) {
            this.positive.setText("下载并安装");
            this.positive.setOnClickListener(this);
        }
        if (this.negative != null) {
            this.negative.setOnClickListener(this);
        }
        // 默认版本名：优先用文件名的去扩展名形式，其次包名
        if (this.editText != null) {
            String def = null;
            try {
                def = version.getFile().getFilename();
            } catch (Throwable ignored) {
            }
            if (StringUtils.isBlank(def)) {
                def = bean == null ? "整合包" : bean.getSlug();
            } else {
                int dot = def.lastIndexOf('.');
                if (dot > 0) {
                    def = def.substring(0, dot);
                }
            }
            this.editText.setText(def);
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == negative) {
            dismiss();
            return;
        }
        if (view != positive) {
            return;
        }
        String name = editText == null ? "" : editText.getText().toString().trim();
        if (StringUtils.isBlank(name)) {
            Toast.makeText(getContext(), "整合包名字不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.contains("/") || name.contains("\\")) {
            Toast.makeText(getContext(), "名字里不能带斜杠", Toast.LENGTH_SHORT).show();
            return;
        }

        String url;
        try {
            url = version.getFile().getUrl();
        } catch (Throwable t) {
            Toast.makeText(getContext(), "这个版本没有下载地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (StringUtils.isBlank(url)) {
            Toast.makeText(getContext(), "这个版本没有下载地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // 落盘位置：<游戏目录>/modpacks/<名字>.zip（玩家自己也能找到）
        File dir = new File(activity.launcherSetting.gameFileDirectory, "modpacks");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(getContext(), "无法创建 modpacks 目录", Toast.LENGTH_SHORT).show();
            return;
        }
        final File target = new File(dir, name + ".zip");

        // ★ 模组那套一样走国内镜像，失败自动回退官方地址
        DownloadTaskListBean beanTask = new DownloadTaskListBean(name,
                MirrorUtils.rewriteCdn(url), target.getAbsolutePath(), "")
                .withFallback(url);
        ArrayList<DownloadTaskListBean> list = new ArrayList<>();
        list.add(beanTask);

        final MainActivity act = activity;
        final String finalName = name;
        DownloadDialog downloadDialog = new DownloadDialog(getContext(), activity, list, true);
        // ★★★ 下载完成后**自动进入安装**（这就是"自动化下载"的收尾）：
        //   把 zip 交回 InstallPackageUI —— 它按 manifest 分发到对应安装任务，
        //   并复用「本地导入」那套任务列表 UI。走不通也不会卡死（解析失败会落到兜底任务）。
        downloadDialog.setOnComplete(() -> {
            try {
                act.uiManager.installPackageUI.installDownloadedZip(target, finalName);
            } catch (Throwable t) {
                Toast.makeText(act, "下载完成，但自动安装失败：" + t, Toast.LENGTH_LONG).show();
            }
        });
        downloadDialog.show();
        dismiss();
    }
}
