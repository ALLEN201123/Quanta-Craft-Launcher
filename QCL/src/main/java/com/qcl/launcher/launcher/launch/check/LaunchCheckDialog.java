package com.qcl.launcher.launcher.launch.check;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.account.ReLoginDialog;
import com.qcl.launcher.launcher.launch.check.CheckAccountTask;
import com.qcl.launcher.launcher.launch.check.CheckJavaTask;
import com.qcl.launcher.launcher.launch.check.CheckLibTask;
import com.qcl.launcher.launcher.launch.check.LaunchTask;
import com.qcl.launcher.launcher.launch.pojav.PojavMinecraftActivity;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.view.list.MaxHeightRecyclerView;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.io.NetSpeed;
import com.qcl.launcher.utils.io.NetSpeedTimer;
import java.io.File;
import java.util.Objects;
import java.util.Vector;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class LaunchCheckDialog extends Dialog implements View.OnClickListener, Handler.Callback {
    private MainActivity activity;
    private Bundle bundle;
    private Button cancel;
    private CheckAccountTask checkAccountTask;
    private CheckJavaTask checkJavaTask;
    private CheckLibTask checkLibTask;
    private boolean java;
    private ImageView javaState;
    private ImageView launchState;
    private LaunchTask launchTask;
    private String launchVersion;
    private boolean lib;
    private ImageView libState;
    private boolean login;
    private ImageView loginState;
    private NetSpeedTimer netSpeedTimer;
    private MaxHeightRecyclerView recyclerView;
    private TextView speedText;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$throwException$0(DialogInterface dialogInterface, int i) {
    }

    public LaunchCheckDialog(Context context, MainActivity mainActivity, String str, Bundle bundle) {
        super(context);
        this.java = false;
        this.lib = false;
        this.login = false;
        setContentView(R.layout.dialog_launch_check);
        setCancelable(false);
        this.activity = mainActivity;
        this.launchVersion = str;
        this.bundle = bundle;
        init();
    }

    private void init() {
        this.javaState = (ImageView) findViewById(R.id.check_java_state);
        this.libState = (ImageView) findViewById(R.id.check_lib_state);
        this.loginState = (ImageView) findViewById(R.id.check_account_state);
        this.launchState = (ImageView) findViewById(R.id.check_launch_state);
        this.speedText = (TextView) findViewById(R.id.download_speed_text);
        Button button = (Button) findViewById(R.id.cancel_launch_game);
        this.cancel = button;
        button.setOnClickListener(this);
        MaxHeightRecyclerView maxHeightRecyclerView = (MaxHeightRecyclerView) findViewById(R.id.download_task_list);
        this.recyclerView = maxHeightRecyclerView;
        maxHeightRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ((RecyclerView.ItemAnimator) Objects.requireNonNull(this.recyclerView.getItemAnimator())).setAddDuration(0L);
        this.recyclerView.getItemAnimator().setChangeDuration(0L);
        this.recyclerView.getItemAnimator().setMoveDuration(0L);
        this.recyclerView.getItemAnimator().setRemoveDuration(0L);
        ((SimpleItemAnimator) this.recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        NetSpeedTimer periodTime = new NetSpeedTimer(getContext(), new NetSpeed(), new Handler(this)).setDelayTime(0L).setPeriodTime(1000L);
        this.netSpeedTimer = periodTime;
        periodTime.startSpeedTimer();
        startCheckTasks();
    }

    private void startCheckTasks() {
        this.checkJavaTask = new CheckJavaTask(this.activity, this.launchVersion, new CheckJavaTask.CheckJavaCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.1
            @Override // com.qcl.launcher.launcher.launch.check.CheckJavaTask.CheckJavaCallback
            public void onStart() {
            }

            @Override // com.qcl.launcher.launcher.launch.check.CheckJavaTask.CheckJavaCallback
            public void onFinish(Exception exc) {
                if (exc == null) {
                    LaunchCheckDialog.this.java = true;
                    LaunchCheckDialog.this.javaState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.checkState();
                    return;
                }
                LaunchCheckDialog.this.throwException(exc);
            }
        });
        this.checkLibTask = new CheckLibTask(this.activity, this.launchVersion, new CheckLibTask.CheckLibCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.2
            @Override // com.qcl.launcher.launcher.launch.check.CheckLibTask.CheckLibCallback
            public void onStart() {
            }

            @Override // com.qcl.launcher.launcher.launch.check.CheckLibTask.CheckLibCallback
            public void onFinish(Exception exc) {
                if (exc == null) {
                    LaunchCheckDialog.this.lib = true;
                    LaunchCheckDialog.this.libState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.checkState();
                    return;
                }
                LaunchCheckDialog.this.throwException(exc);
            }
        });
        this.checkAccountTask = new CheckAccountTask(this.activity, new CheckAccountTask.CheckAccountCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.3
            @Override // com.qcl.launcher.launcher.launch.check.CheckAccountTask.CheckAccountCallback
            public void onStart() {
            }

            @Override // com.qcl.launcher.launcher.launch.check.CheckAccountTask.CheckAccountCallback
            public void onFinish(Exception exc, boolean z) {
                if (!z) {
                    Context context = LaunchCheckDialog.this.getContext();
                    String str = LaunchCheckDialog.this.activity.publicGameSetting.account.email;
                    LaunchCheckDialog launchCheckDialog = LaunchCheckDialog.this;
                    new ReLoginDialog(context, str, ((AuthlibInjectorServer) Objects.requireNonNull(launchCheckDialog.getServerFromUrl(launchCheckDialog.activity.publicGameSetting.account.loginServer))).getYggdrasilService(), LaunchCheckDialog.this.activity.publicGameSetting.account, new ReLoginDialog.ReloginCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.3.1
                        @Override // com.qcl.launcher.launcher.dialogs.account.ReLoginDialog.ReloginCallback
                        public void onRelogin(Account account) {
                            int i = 0;
                            while (true) {
                                if (i >= LaunchCheckDialog.this.activity.uiManager.accountUI.accounts.size()) {
                                    break;
                                }
                                Account account2 = LaunchCheckDialog.this.activity.uiManager.accountUI.accounts.get(i);
                                if (LaunchCheckDialog.this.activity.publicGameSetting.account.email.equals(account2.email) && LaunchCheckDialog.this.activity.publicGameSetting.account.auth_player_name.equals(account2.auth_player_name) && LaunchCheckDialog.this.activity.publicGameSetting.account.auth_uuid.equals(account2.auth_uuid) && LaunchCheckDialog.this.activity.publicGameSetting.account.loginServer.equals(account2.loginServer)) {
                                    LaunchCheckDialog.this.activity.uiManager.accountUI.accounts.get(i).refresh(account);
                                    GsonUtils.saveAccounts(LaunchCheckDialog.this.activity.uiManager.accountUI.accounts, AppManifest.ACCOUNT_DIR + "/accounts.json");
                                    break;
                                }
                                i++;
                            }
                            LaunchCheckDialog.this.activity.publicGameSetting.account = account;
                            GsonUtils.savePublicGameSetting(LaunchCheckDialog.this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
                            LaunchCheckDialog.this.activity.uiManager.accountUI.accountListAdapter.notifyDataSetChanged();
                            LaunchCheckDialog.this.activity.uiManager.mainUI.refreshAccount();
                            LaunchCheckDialog.this.login = true;
                            LaunchCheckDialog.this.loginState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                            LaunchCheckDialog.this.checkState();
                        }

                        @Override // com.qcl.launcher.launcher.dialogs.account.ReLoginDialog.ReloginCallback
                        public void onCancel() {
                            LaunchCheckDialog.this.exit();
                        }
                    }).show();
                    return;
                }
                if (exc == null) {
                    LaunchCheckDialog.this.login = true;
                    LaunchCheckDialog.this.loginState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.checkState();
                    return;
                }
                LaunchCheckDialog.this.throwException(exc);
            }
        });
        this.checkJavaTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[0]);
        this.checkLibTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.recyclerView);
        this.checkAccountTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.activity.publicGameSetting.account);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public AuthlibInjectorServer getServerFromUrl(String str) {
        for (int i = 0; i < this.activity.uiManager.accountUI.serverList.size(); i++) {
            if (this.activity.uiManager.accountUI.serverList.get(i).getUrl().equals(str)) {
                return this.activity.uiManager.accountUI.serverList.get(i);
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkState() {
        if (this.java && this.lib && this.login) {
            LaunchTask launchTask = new LaunchTask(this.activity, new LaunchTask.LaunchCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.4
                @Override // com.qcl.launcher.launcher.launch.check.LaunchTask.LaunchCallback
                public void onStart() {
                }

                @Override // com.qcl.launcher.launcher.launch.check.LaunchTask.LaunchCallback
                public void onFinish(Vector<String> vector) {
                    LaunchCheckDialog.this.launchState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.bundle.putSerializable("args", vector);
                    String str = LaunchCheckDialog.this.launchVersion + "/qcl.cfg";
                    if (!new File(str).exists() || GsonUtils.getPrivateGameSettingFromFile(str) == null || (!GsonUtils.getPrivateGameSettingFromFile(str).forceEnable && !GsonUtils.getPrivateGameSettingFromFile(str).enable)) {
                        PrivateGameSetting privateGameSetting = LaunchCheckDialog.this.activity.privateGameSetting;
                    } else {
                        GsonUtils.getPrivateGameSettingFromFile(str);
                    }
                    Intent intent = new Intent(LaunchCheckDialog.this.getContext(), (Class<?>) PojavMinecraftActivity.class);
                    intent.putExtras(LaunchCheckDialog.this.bundle);
                    LaunchCheckDialog.this.dismiss();
                    LaunchCheckDialog.this.activity.launch(intent);
                }
            });
            this.launchTask = launchTask;
            launchTask.execute(this.activity.publicGameSetting.account);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void throwException(Exception exc) {
        exit();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getContext().getString(R.string.launch_failed_dialog_title));
        builder.setMessage(exc.toString());
        builder.setPositiveButton(getContext().getString(R.string.launch_failed_dialog_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                LaunchCheckDialog.lambda$throwException$0(dialogInterface, i);
            }
        });
        builder.create().show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void exit() {
        CheckLibTask checkLibTask = this.checkLibTask;
        if (checkLibTask != null && checkLibTask.getStatus() != null && this.checkLibTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.checkLibTask.cancel(true);
        }
        CheckAccountTask checkAccountTask = this.checkAccountTask;
        if (checkAccountTask != null && checkAccountTask.getStatus() != null && this.checkAccountTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.checkAccountTask.cancel(true);
        }
        CheckJavaTask checkJavaTask = this.checkJavaTask;
        if (checkJavaTask != null && checkJavaTask.getStatus() != null && this.checkJavaTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.checkJavaTask.cancel(true);
        }
        LaunchTask launchTask = this.launchTask;
        if (launchTask != null && launchTask.getStatus() != null && this.launchTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.launchTask.cancel(true);
        }
        dismiss();
    }

    @Override // android.os.Handler.Callback
    public boolean handleMessage(Message message) {
        if (message.what != 101010) {
            return false;
        }
        this.speedText.setText((String) message.obj);
        return false;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.cancel) {
            exit();
        }
    }
}
