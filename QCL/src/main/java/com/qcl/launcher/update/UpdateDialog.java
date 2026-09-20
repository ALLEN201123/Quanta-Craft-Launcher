/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Dialog
 *  android.content.Context
 *  android.content.Intent
 *  android.net.Uri
 *  android.os.Handler
 *  android.text.Html
 *  android.text.Spanned
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.ProgressBar
 *  android.widget.TextView
 *  androidx.annotation.NonNull
 *  androidx.core.content.FileProvider
 */
package com.qcl.launcher.update;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.update.LauncherVersion;
import com.qcl.launcher.update.UpdateChecker;
import com.qcl.launcher.utils.Architecture;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.io.DownloadUtil;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
public class UpdateDialog
extends Dialog
implements View.OnClickListener {
    private MainActivity activity;
    private LauncherVersion version;
    private boolean isBeta;
    private TextView versionName;
    private TextView date;
    private TextView type;
    private TextView log;
    private ProgressBar progressBar;
    private Button update;
    private Button netdisk;
    private Button github;
    private Button negative;
    private Handler handler;

    public UpdateDialog(@NonNull Context context, MainActivity activity, LauncherVersion version, boolean isBeta) {
        super(context);
        this.setContentView(R.layout.dialog_update_launcher);
        this.setCancelable(false);
        this.activity = activity;
        this.version = version;
        this.isBeta = isBeta;
        this.handler = new Handler();
        this.init();
    }

    private void init() {
        this.versionName = (TextView)this.findViewById(R.id.update_version_name);
        this.date = (TextView)this.findViewById(R.id.update_date);
        this.type = (TextView)this.findViewById(R.id.update_type);
        this.log = (TextView)this.findViewById(R.id.update_log);
        this.versionName.setText((CharSequence)this.version.versionName);
        this.date.setText((CharSequence)this.version.date);
        this.type.setText((CharSequence)this.getType(this.isBeta));
        Spanned charSequence = Html.fromHtml((String)this.version.updateLog, (int)0);
        this.log.setText((CharSequence)charSequence);
        this.progressBar = (ProgressBar)this.findViewById(R.id.update_progress);
        this.update = (Button)this.findViewById(R.id.update);
        this.netdisk = (Button)this.findViewById(R.id.netdisk);
        this.github = (Button)this.findViewById(R.id.github);
        this.negative = (Button)this.findViewById(R.id.negative);
        this.update.setOnClickListener((View.OnClickListener)this);
        this.netdisk.setOnClickListener((View.OnClickListener)this);
        this.github.setOnClickListener((View.OnClickListener)this);
        this.negative.setOnClickListener((View.OnClickListener)this);
    }

    private String getType(boolean isBeta) {
        if (isBeta) {
            return this.getContext().getString(R.string.dialog_update_beta);
        }
        return this.getContext().getString(R.string.dialog_update_release);
    }

    /**
     * ★★★ 1.2.0 照 FCL 的 UpdateDialog.getTargetArchUrl()：
     * 远程 launcher_version.json 里的 url 是「带 -all 的资产直链模板」，例如
     *   https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases/download/v1.2.0/QCL-release-1.2.0-all.apk
     * 按设备架构把 "-all" 换成真实后缀，得到对应架构包的直链。
     * 以前是直接把 url.get(0) 当直链下，而 json 里填的是发布页网页，
     * 结果把 HTML 下载成 update/latest.apk → 安装必然解析失败。
     * GitHub 的 releases/download/<tag>/<asset> 是 302 到真实资产，下载器跟得上重定向。
     */
    private String getTargetArchUrl() {
        String url = (this.version == null || this.version.url == null || this.version.url.isEmpty())
                ? "" : this.version.url.get(0);
        int a = Architecture.getRuntimeArchitecture();
        String arch;
        if (a == Architecture.ARCH_ARM) {
            arch = "arm";
        } else if (a == Architecture.ARCH_ARM64) {
            arch = "arm64";
        } else if (a == Architecture.ARCH_X86) {
            arch = "x86";
        } else if (a == Architecture.ARCH_X86_64) {
            arch = "x86_64";
        } else {
            arch = "universal";
        }
        return url.replace("-all", "-" + arch);
    }

    /** 下载失败时的兜底提示，照 FCL：给一个「网盘下载」出口。 */
    private void showDownloadFailed(String reason) {
        try {
            new AlertDialog.Builder(this.getContext())
                    .setTitle(R.string.update_failed)
                    .setMessage(R.string.update_failed_msg + (reason == null ? "" : ("\n" + reason)))
                    .setPositiveButton(R.string.dialog_update_netdisk, (d, w) -> {
                        String netdiskUrl = this.version == null ? null : this.version.netdiskUrl;
                        try {
                            Intent i = new Intent("android.intent.action.VIEW");
                            i.setData(Uri.parse(netdiskUrl != null && !netdiskUrl.isEmpty()
                                    ? netdiskUrl
                                    : "https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases/latest"));
                            this.getContext().startActivity(i);
                        } catch (Throwable ignored) {
                        }
                    })
                    .setNegativeButton(R.string.dialog_positive, null)
                    .show();
        } catch (Throwable ignored) {
        }
    }

    public void onClick(View view) {
        if (view == this.update) {
            this.update.setEnabled(false);
            this.progressBar.setVisibility(0);
            // ★★★ 1.2.0 照 FCL：按设备架构从资产直链模板换出真实下载地址。
            String finalUrl = this.getTargetArchUrl();
            new Thread(() -> {
                if (FileUtils.deleteDirectory(AppManifest.DEFAULT_CACHE_DIR + "/update")) {
                    DownloadUtil.downloadSingleFile(this.getContext(), new DownloadTaskListBean("", finalUrl, AppManifest.DEFAULT_CACHE_DIR + "/update/latest.apk", null), new DownloadTask.Feedback(){

                        @Override
                        public void addTask(DownloadTaskListBean bean) {
                        }

                        @Override
                        public void updateProgress(DownloadTaskListBean bean) {
                            UpdateDialog.this.handler.post(() -> UpdateDialog.this.progressBar.setProgress(bean.progress));
                        }

                        @Override
                        public void updateSpeed(String speed) {
                        }

                        @Override
                        public void removeTask(DownloadTaskListBean bean) {
                        }

                        @Override
                        public void onFinished(ArrayList<DownloadTaskListBean> failedFile) {
                            UpdateDialog.this.handler.post(() -> {
                                UpdateDialog.this.update.setEnabled(true);
                                UpdateDialog.this.progressBar.setVisibility(8);
                                // ★ 1.2.0：校验真的下到东西了，失败就给「网盘下载」出口（照 FCL）
                                File apk = new File(AppManifest.DEFAULT_CACHE_DIR + "/update/latest.apk");
                                if ((failedFile != null && !failedFile.isEmpty()) || !apk.isFile() || apk.length() == 0) {
                                    UpdateDialog.this.showDownloadFailed(failedFile != null && !failedFile.isEmpty()
                                            ? null : UpdateDialog.this.getContext().getString(R.string.update_failed_empty));
                                    return;
                                }
                                Intent intent = new Intent("android.intent.action.VIEW");
                                intent.addFlags(0x10000000);
                                Uri apkUri = FileProvider.getUriForFile((Context)UpdateDialog.this.getContext(), (String)UpdateDialog.this.getContext().getString(R.string.filebrowser_provider), apk);
                                intent.addFlags(1);
                                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                                UpdateDialog.this.getContext().startActivity(intent);
                            });
                        }

                        @Override
                        public void onCancelled() {
                        }
                    });
                }
            }).start();
        }
        if (view == this.netdisk) {
            // ★★★ 1.1.5：网盘下载（夸克网盘）。链接来自远程 launcher_version.json 的 netdiskUrl，
            //   为空则退回 GitHub Release —— 改链接只需改 json，无需重新构建 APK（照 FCL 的做法）。
            String netdiskUrl = this.version.netdiskUrl;
            try {
                Intent i = new Intent("android.intent.action.VIEW");
                i.setData(Uri.parse(netdiskUrl != null && !netdiskUrl.isEmpty()
                        ? netdiskUrl : "https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases/latest"));
                this.getContext().startActivity(i);
            }
            catch (Throwable ignored) {
            }
            this.dismiss();
        }
        if (view == this.github) {
            // ★★★ 1.1.5：GitHub 下载（跳 GitHub Release 页）。
            try {
                Intent i = new Intent("android.intent.action.VIEW");
                i.setData(Uri.parse("https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases/latest"));
                this.getContext().startActivity(i);
            }
            catch (Throwable ignored) {
            }
            this.dismiss();
        }
        if (view == this.negative) {
            this.dismiss();
        }
    }
}

