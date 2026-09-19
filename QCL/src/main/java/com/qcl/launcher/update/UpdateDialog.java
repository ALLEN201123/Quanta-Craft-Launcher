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
    private Button ignore;
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
        this.ignore = (Button)this.findViewById(R.id.ignore);
        this.netdisk = (Button)this.findViewById(R.id.netdisk);
        this.github = (Button)this.findViewById(R.id.github);
        this.negative = (Button)this.findViewById(R.id.negative);
        this.update.setOnClickListener((View.OnClickListener)this);
        this.ignore.setOnClickListener((View.OnClickListener)this);
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

    public void onClick(View view) {
        if (view == this.update) {
            this.update.setEnabled(false);
            this.ignore.setEnabled(false);
            this.progressBar.setVisibility(0);
            String finalUrl = this.version.url.get(0);
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
                                UpdateDialog.this.ignore.setEnabled(true);
                                UpdateDialog.this.progressBar.setVisibility(8);
                                Intent intent = new Intent("android.intent.action.VIEW");
                                intent.addFlags(0x10000000);
                                Uri apkUri = FileProvider.getUriForFile((Context)UpdateDialog.this.getContext(), (String)UpdateDialog.this.getContext().getString(R.string.filebrowser_provider), (File)new File(AppManifest.DEFAULT_CACHE_DIR + "/update/latest.apk"));
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
        if (view == this.ignore) {
            // ★★★ 1.1.5：忽略此更新 = 只关闭本次弹窗，**不持久化**，
            //   因此每次重新进入启动器仍会弹出（与 FCL 的预期一致，用户可再选择下载或以后再说）。
            this.dismiss();
        }
        if (view == this.netdisk) {
            // ★★★ 1.1.5：网盘下载（夸克网盘，暂为占位链接）。跳转到浏览器打开。
            try {
                Intent i = new Intent("android.intent.action.VIEW");
                i.setData(Uri.parse(this.version.url != null && !this.version.url.isEmpty()
                        ? this.version.url.get(0) : "https://github.com/ALLEN201123/Quanta-Craft-Launcher/releases/latest"));
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

