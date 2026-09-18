package com.qcl.launcher.launcher.download;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.game.AssetsUpdateTask;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.utils.io.NetSpeed;
import com.qcl.launcher.utils.io.NetSpeedTimer;
import java.util.Objects;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AssetsUpdateDialog extends Dialog implements View.OnClickListener, Handler.Callback {
    private MainActivity activity;
    private AssetsUpdateTask assetsUpdateTask;
    private Button cancelButton;
    private DownloadTaskListAdapter downloadTaskListAdapter;
    private String name;
    private NetSpeedTimer netSpeedTimer;
    private TextView speedText;
    private RecyclerView taskListView;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$throwException$0(DialogInterface dialogInterface, int i) {
    }

    public AssetsUpdateDialog(Context context, MainActivity mainActivity, String str) {
        super(context);
        this.activity = mainActivity;
        this.name = str;
        setContentView(R.layout.dialog_install_assets);
        setCancelable(false);
        init();
    }

    private void init() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.download_task_list);
        this.taskListView = recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        DownloadTaskListAdapter downloadTaskListAdapter = new DownloadTaskListAdapter(getContext());
        this.downloadTaskListAdapter = downloadTaskListAdapter;
        this.taskListView.setAdapter(downloadTaskListAdapter);
        ((RecyclerView.ItemAnimator) Objects.requireNonNull(this.taskListView.getItemAnimator())).setAddDuration(0L);
        this.taskListView.getItemAnimator().setChangeDuration(0L);
        this.taskListView.getItemAnimator().setMoveDuration(0L);
        this.taskListView.getItemAnimator().setRemoveDuration(0L);
        ((SimpleItemAnimator) this.taskListView.getItemAnimator()).setSupportsChangeAnimations(false);
        this.speedText = (TextView) findViewById(R.id.download_speed_text);
        Button button = (Button) findViewById(R.id.cancel_install_assets);
        this.cancelButton = button;
        button.setOnClickListener(this);
        NetSpeedTimer periodTime = new NetSpeedTimer(getContext(), new NetSpeed(), new Handler(this)).setDelayTime(0L).setPeriodTime(1000L);
        this.netSpeedTimer = periodTime;
        periodTime.startSpeedTimer();
        update();
    }

    public void update() {
        AssetsUpdateTask assetsUpdateTask = new AssetsUpdateTask(this.activity, this.downloadTaskListAdapter, new AssetsUpdateTask.AssetsUpdateCallback() { // from class: com.qcl.launcher.launcher.download.AssetsUpdateDialog.1
            @Override // com.qcl.launcher.launcher.download.game.AssetsUpdateTask.AssetsUpdateCallback
            public void onStart() {
            }

            @Override // com.qcl.launcher.launcher.download.game.AssetsUpdateTask.AssetsUpdateCallback
            public void onFinish(Exception exc) {
                if (exc == null) {
                    AssetsUpdateDialog.this.exit();
                } else {
                    AssetsUpdateDialog.this.throwException(exc);
                }
            }
        });
        this.assetsUpdateTask = assetsUpdateTask;
        assetsUpdateTask.execute(this.name);
    }

    public void throwException(final Exception exc) {
        this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.download.AssetsUpdateDialog$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                AssetsUpdateDialog.this.m287xf12143e9(exc);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$throwException$1$com-qcl-launcher-launcher-download-AssetsUpdateDialog, reason: not valid java name */
    public /* synthetic */ void m287xf12143e9(Exception exc) {
        exit();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getContext().getString(R.string.dialog_install_fail_title));
        builder.setMessage(exc.toString());
        builder.setPositiveButton(getContext().getString(R.string.dialog_install_fail_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.download.AssetsUpdateDialog$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                AssetsUpdateDialog.lambda$throwException$0(dialogInterface, i);
            }
        });
        builder.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exit() {
        AssetsUpdateTask assetsUpdateTask = this.assetsUpdateTask;
        if (assetsUpdateTask != null && assetsUpdateTask.getStatus() != null && this.assetsUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.assetsUpdateTask.cancel(true);
        }
        this.netSpeedTimer.stopSpeedTimer();
        dismiss();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.cancelButton) {
            exit();
        }
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message message) {
        if (message.what != 101010) {
            return false;
        }
        this.speedText.setText((String) message.obj);
        return false;
    }
}
