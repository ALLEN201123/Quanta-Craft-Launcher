/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.AlertDialog$Builder
 *  android.app.Dialog
 *  android.content.Context
 *  android.os.AsyncTask
 *  android.os.AsyncTask$Status
 *  android.os.Handler
 *  android.os.Handler$Callback
 *  android.os.Message
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.annotation.NonNull
 *  androidx.recyclerview.widget.LinearLayoutManager
 *  androidx.recyclerview.widget.RecyclerView
 *  androidx.recyclerview.widget.RecyclerView$Adapter
 *  androidx.recyclerview.widget.RecyclerView$LayoutManager
 *  androidx.recyclerview.widget.SimpleItemAnimator
 */
package com.qcl.launcher.launcher.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.task.DownloadTask;
import com.qcl.launcher.utils.io.NetSpeed;
import com.qcl.launcher.utils.io.NetSpeedTimer;
import java.util.ArrayList;
import java.util.Objects;

import com.qcl.launcher.R;
public class DownloadDialog
extends Dialog
implements View.OnClickListener,
Handler.Callback {
    private MainActivity activity;
    private ArrayList<DownloadTaskListBean> list;
    private boolean alert;
    private RecyclerView taskListView;
    private DownloadTaskListAdapter downloadTaskListAdapter;
    private NetSpeedTimer netSpeedTimer;
    private TextView speedText;
    private Button cancelButton;
    private Handler handler;
    private DownloadTask downloadTask;

    /** ★ 1.2.3：全部下载成功后的回调（模组下载完要做 class 检测/注入） */
    private Runnable onComplete;

    public void setOnComplete(Runnable r) {
        this.onComplete = r;
    }

    public DownloadDialog(@NonNull Context context, MainActivity activity, ArrayList<DownloadTaskListBean> list, boolean alert) {
        super(context);
        this.activity = activity;
        this.list = list;
        this.alert = alert;
        this.setContentView(R.layout.dialog_download);
        this.setCancelable(false);
        this.init();
    }

    private void init() {
        this.handler = new Handler();
        this.taskListView = (RecyclerView)this.findViewById(R.id.download_task_list);
        this.taskListView.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager(this.getContext()));
        this.downloadTaskListAdapter = new DownloadTaskListAdapter(this.getContext());
        this.taskListView.setAdapter((RecyclerView.Adapter)this.downloadTaskListAdapter);
        Objects.requireNonNull(this.taskListView.getItemAnimator()).setAddDuration(0L);
        this.taskListView.getItemAnimator().setChangeDuration(0L);
        this.taskListView.getItemAnimator().setMoveDuration(0L);
        this.taskListView.getItemAnimator().setRemoveDuration(0L);
        ((SimpleItemAnimator)this.taskListView.getItemAnimator()).setSupportsChangeAnimations(false);
        this.speedText = (TextView)this.findViewById(R.id.download_speed_text);
        this.cancelButton = (Button)this.findViewById(R.id.cancel);
        this.cancelButton.setOnClickListener((View.OnClickListener)this);
        Handler handler = new Handler((Handler.Callback)this);
        this.netSpeedTimer = new NetSpeedTimer(this.getContext(), new NetSpeed(), handler).setDelayTime(0L).setPeriodTime(1000L);
        this.netSpeedTimer.startSpeedTimer();
        int maxDownloadTask = this.activity.launcherSetting.maxDownloadTask;
        if (this.activity.launcherSetting.autoDownloadTaskQuantity) {
            maxDownloadTask = 64;
        }
        this.downloadTask = new DownloadTask(this.getContext(), new DownloadTask.Feedback(){

            @Override
            public void addTask(DownloadTaskListBean bean) {
                DownloadDialog.this.handler.post(() -> DownloadDialog.this.downloadTaskListAdapter.addDownloadTask(bean));
            }

            @Override
            public void updateProgress(DownloadTaskListBean bean) {
                DownloadDialog.this.handler.post(() -> DownloadDialog.this.downloadTaskListAdapter.onProgress(bean));
            }

            @Override
            public void updateSpeed(String speed) {
            }

            @Override
            public void removeTask(DownloadTaskListBean bean) {
                DownloadDialog.this.handler.post(() -> DownloadDialog.this.downloadTaskListAdapter.onComplete(bean));
            }

            @Override
            public void onFinished(ArrayList<DownloadTaskListBean> failedFile) {
                DownloadDialog.this.handler.post(() -> {
                    if (failedFile.size() > 0) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("The following files failed to download:");
                        for (DownloadTaskListBean bean : failedFile) {
                            stringBuilder.append("\n\n  ").append(bean.name);
                        }
                        Exception e = new Exception(stringBuilder.toString());
                        e.printStackTrace();
                        DownloadDialog.this.throwException(e);
                    } else {
                        DownloadDialog.this.exit();
                        if (DownloadDialog.this.alert) {
                            Toast.makeText((Context)DownloadDialog.this.getContext(), (CharSequence)DownloadDialog.this.getContext().getString(R.string.dialog_download_success), (int)0).show();
                        }
                        if (DownloadDialog.this.onComplete != null) {
                            DownloadDialog.this.onComplete.run();
                        }
                    }
                });
            }

            @Override
            public void onCancelled() {
            }
        });
        this.downloadTask.setMaxTask(maxDownloadTask);
        this.downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new ArrayList[]{this.list});
    }

    private void throwException(Exception e) {
        this.activity.runOnUiThread(() -> {
            this.exit();
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
            builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_install_fail_title));
            builder.setMessage((CharSequence)e.toString());
            builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_install_fail_positive), (dialogInterface, i) -> {});
            builder.create().show();
        });
    }

    private void exit() {
        if (this.downloadTask != null && this.downloadTask.getStatus() != null && this.downloadTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.downloadTask.cancel(true);
        }
        this.netSpeedTimer.stopSpeedTimer();
        this.dismiss();
    }

    public boolean handleMessage(@NonNull Message message) {
        if (message.what == 101010) {
            String speed = (String)message.obj;
            this.speedText.setText((CharSequence)speed);
        }
        return false;
    }

    public void onClick(View view) {
        if (view == this.cancelButton) {
            this.exit();
        }
    }
}

