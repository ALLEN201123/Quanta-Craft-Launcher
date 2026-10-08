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
    /** ★ 1.5.0：Vulkan 检测行（仅 26.2+ 版本显示） */
    private android.widget.LinearLayout vulkanRow;
    private ImageView vulkanState;
    /** ★ 1.5.0：Vulkan 检测是否通过；非 26.2+ 版本恒为 true（不需要检测） */
    private boolean vulkan = true;
    /** ★ 1.5.0：实时日志区（对照 FCL TaskDialog 的 logScroll/logView） */
    private android.widget.ScrollView logScroll;
    private TextView logView;
    private final StringBuilder logBuffer = new StringBuilder();

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
        // ★ 1.2.3：控制台透明 —— 只改布局不够，Dialog 窗口自带不透明背景，必须一起清掉
        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            // ★ 1.5.0：对话框高度按屏幕自适应（对照 FCL：屏幕高 × 3/4，不低于 300dp、不高于 560dp）。
            //   布局根已是 wrap_content + 内部 ScrollView，内容再多也能上下滑动。
            try {
                android.util.DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
                int quarteScreen = (int) (dm.heightPixels * 0.78f);
                int minH = com.qcl.launcher.utils.convert.ConvertUtils.dip2px(getContext(), 300f);
                int maxH = com.qcl.launcher.utils.convert.ConvertUtils.dip2px(getContext(), 560f);
                int target = Math.max(minH, Math.min(maxH, quarteScreen));
                getWindow().setLayout(
                        (int) (dm.widthPixels * 0.62f),
                        target);
            } catch (Throwable ignored) {
            }
        }
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
        // ★ 1.5.0：Vulkan 检测行（布局里默认 gone，仅 26.2+ 显示）
        this.vulkanRow = (android.widget.LinearLayout) findViewById(R.id.check_vulkan_row);
        this.vulkanState = (ImageView) findViewById(R.id.check_vulkan_state);
        // ★ 1.5.0：实时日志区（对照 FCL TaskDialog）
        this.logScroll = (android.widget.ScrollView) findViewById(R.id.check_log_scroll);
        this.logView = (TextView) findViewById(R.id.check_log_view);
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

    /**
     * ★ 1.5.0：向实时日志区追加一行（对照 FCL TaskDialog#appendLog）。
     *
     * <p>只在主线程操作 View；非主线程调用会自动切回主线程。日志区一旦有内容即显示，
     * 并自动滚动到底部。
     */
    private void appendLog(final String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                if (LaunchCheckDialog.this.logScroll == null || LaunchCheckDialog.this.logView == null) {
                    return;
                }
                LaunchCheckDialog.this.logScroll.setVisibility(View.VISIBLE);
                LaunchCheckDialog.this.logBuffer.append(message).append('\n');
                // 防止日志无限增长：超过 4000 字时砍掉前半
                if (LaunchCheckDialog.this.logBuffer.length() > 4000) {
                    LaunchCheckDialog.this.logBuffer.delete(0, LaunchCheckDialog.this.logBuffer.length() - 2000);
                }
                LaunchCheckDialog.this.logView.setText(LaunchCheckDialog.this.logBuffer.toString());
                LaunchCheckDialog.this.logScroll.post(new Runnable() {
                    @Override
                    public void run() {
                        LaunchCheckDialog.this.logScroll.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        };
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            task.run();
        } else {
            this.activity.runOnUiThread(task);
        }
    }

    /** ★ 1.5.0：清空并隐藏日志区（对照 FCL TaskDialog#clearLog）。 */
    private void clearLog() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                LaunchCheckDialog.this.logBuffer.setLength(0);
                if (LaunchCheckDialog.this.logView != null) {
                    LaunchCheckDialog.this.logView.setText("");
                }
                if (LaunchCheckDialog.this.logScroll != null) {
                    LaunchCheckDialog.this.logScroll.setVisibility(View.GONE);
                }
            }
        };
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            task.run();
        } else {
            this.activity.runOnUiThread(task);
        }
    }

    private void startCheckTasks() {
        startVulkanCheck();
        appendLog(getContext().getString(R.string.launch_check_dialog_java) + " ...");
        this.checkJavaTask = new CheckJavaTask(this.activity, this.launchVersion, new CheckJavaTask.CheckJavaCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.1
            @Override // com.qcl.launcher.launcher.launch.check.CheckJavaTask.CheckJavaCallback
            public void onStart() {
            }

            @Override // com.qcl.launcher.launcher.launch.check.CheckJavaTask.CheckJavaCallback
            public void onFinish(Exception exc) {
                if (exc == null) {
                    LaunchCheckDialog.this.java = true;
                    LaunchCheckDialog.this.javaState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.appendLog("  OK");
                    LaunchCheckDialog.this.checkState();
                    return;
                }
                LaunchCheckDialog.this.appendLog("  " + exc);
                LaunchCheckDialog.this.throwException(exc);
            }
        });
        // ★ 1.3.0：启动前把远古版中文包打好（早就装好的 / 改过名字的版本也能自动拿到中文；
        //   已经打过同一个语言会直接跳过，不浪费时间）
        new Thread(() -> {
            try {
                java.io.File vd = new java.io.File(this.activity.launcherSetting.gameFileDirectory, "versions/" + this.launchVersion);
                if (vd.isDirectory()) {
                    com.qcl.launcher.launcher.download.game.LegacyChinesePack.applyIfNeeded(this.activity, vd, this.launchVersion);
                }
            }
            catch (Throwable ignored) {
            }
        }).start();
        this.checkLibTask = new CheckLibTask(this.activity, this.launchVersion, new CheckLibTask.CheckLibCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.2
            @Override // com.qcl.launcher.launcher.launch.check.CheckLibTask.CheckLibCallback
            public void onStart() {
            }

            @Override // com.qcl.launcher.launcher.launch.check.CheckLibTask.CheckLibCallback
            public void onFinish(Exception exc) {
                if (exc == null) {
                    LaunchCheckDialog.this.lib = true;
                    LaunchCheckDialog.this.libState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.appendLog(getContext().getString(R.string.launch_check_dialog_lib) + " ... OK");
                    LaunchCheckDialog.this.checkState();
                    return;
                }
                LaunchCheckDialog.this.appendLog("  " + exc);
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
                    LaunchCheckDialog.this.appendLog(getContext().getString(R.string.launch_check_dialog_login) + " ... OK");
                    LaunchCheckDialog.this.checkState();
                    return;
                }
                LaunchCheckDialog.this.appendLog("  " + exc);
                LaunchCheckDialog.this.throwException(exc);
            }
        });
        this.checkJavaTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Object[0]);
        this.checkLibTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.recyclerView);
        this.checkAccountTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.activity.publicGameSetting.account);
    }

    /**
     * ★ 1.5.0：启动前的 Vulkan 检测（仅 Minecraft 26.2+ 需要）。
     *
     * <p>背景：官方 26.2 起给游戏接了 Vulkan 渲染后端，26.3 起对着色器/扩展要求更严
     * （多出的必需项 {@code drawIndirectFirstInstance}，见 {@code VulkanRequirement}）。
     * 设备不达标时游戏根本进不去，与其让它黑屏崩，不如在**启动前**拦住并告诉玩家原因。
     *
     * <p>三项纪律：
     * <ol>
     *   <li>非 26.2+ 版本一律跳过（{@code vulkan} 保持 true）——绝不影响低版本启动；</li>
     *   <li>检测本身抛异常 → 视为通过（宁可放行也不误拦）；</li>
     *   <li>不合格时给「仍然启动」的口子，玩家自担风险，我们不硬拦。</li>
     * </ol>
     */
    private void startVulkanCheck() {
        boolean needed;
        try {
            needed = com.qcl.launcher.launcher.launch.vulkan.VulkanRequirement
                    .hasVulkanBackend(this.launchVersion);
        } catch (Throwable t) {
            needed = false;
        }
        if (!needed) {
            // 低版本：隐藏该行，直接视为通过
            if (this.vulkanRow != null) {
                this.vulkanRow.setVisibility(View.GONE);
            }
            this.vulkan = true;
            return;
        }
        // 26.2+：显示该行
        if (this.vulkanRow != null) {
            this.vulkanRow.setVisibility(View.VISIBLE);
        }
        try {
            com.qcl.launcher.launcher.launch.vulkan.VulkanCapabilities caps =
                    com.qcl.launcher.launcher.launch.vulkan.VulkanChecker.check(this.activity);
            com.qcl.launcher.launcher.launch.vulkan.VulkanSupport support =
                    new com.qcl.launcher.launcher.launch.vulkan.VulkanSupport(caps);
            boolean ok = caps.isSupported() && support.isVersionSupported(this.launchVersion);
            if (ok) {
                this.vulkan = true;
                if (this.vulkanState != null) {
                    this.vulkanState.setBackground(
                            this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                }
                appendLog(getContext().getString(R.string.launch_check_dialog_vulkan)
                        + " ... OK (Vulkan " + caps.versionString() + ")");
                checkState();
                return;
            }
            // 不达标：标红 + 拦住 + 给出原因与出口
            this.vulkan = false;
            if (this.vulkanState != null) {
                this.vulkanState.setBackground(
                        this.getContext().getDrawable(R.drawable.ic_baseline_close_white));
            }
            appendLog(getContext().getString(R.string.launch_check_dialog_vulkan)
                    + " ... FAILED (Vulkan " + caps.versionString()
                    + ", level " + caps.hardwareLevel + ")");
            showVulkanBlocked(caps, support);
        } catch (Throwable t) {
            // 任何异常都放行，绝不误拦玩家
            this.vulkan = true;
            if (this.vulkanState != null) {
                this.vulkanState.setBackground(
                        this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
            }
            appendLog(getContext().getString(R.string.launch_check_dialog_vulkan) + " ... SKIP");
            checkState();
        }
    }

    /** 检测不合格时的拦截弹窗：说明原因，允许「仍然启动」或「换版本」。 */
    private void showVulkanBlocked(final com.qcl.launcher.launcher.launch.vulkan.VulkanCapabilities caps,
                                   final com.qcl.launcher.launcher.launch.vulkan.VulkanSupport support) {
        String missing = com.qcl.launcher.launcher.launch.vulkan.VulkanCheckDialog
                .missingNames(support, this.launchVersion);
        String msg = getContext().getString(R.string.vulkan_check_blocked_message,
                this.launchVersion,
                caps.versionString(),
                caps.hardwareLevel < 0 ? "?" : String.valueOf(caps.hardwareLevel),
                missing);
        AlertDialog.Builder b = new AlertDialog.Builder(getContext());
        b.setTitle(R.string.vulkan_check_blocked_title);
        b.setMessage(msg);
        b.setCancelable(false);
        b.setPositiveButton(R.string.vulkan_check_blocked_continue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 玩家坚持启动：标记通过（此时 java/lib/login 三项若也已就绪会自动往下走）
                LaunchCheckDialog.this.vulkan = true;
                LaunchCheckDialog.this.checkState();
            }
        });
        b.setNeutralButton(R.string.vulkan_check_detail, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                com.qcl.launcher.launcher.launch.vulkan.VulkanCheckDialog.show(
                        LaunchCheckDialog.this.getContext(), caps, null);
                // 看详情后仍要回到拦截框做选择 → 重新弹
                LaunchCheckDialog.this.showVulkanBlocked(caps, support);
            }
        });
        b.setNegativeButton(R.string.vulkan_check_blocked_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // 退出启动，回到主界面换版本
                LaunchCheckDialog.this.exit();
            }
        });
        b.create().show();
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
        // ★ 1.5.0：Vulkan 也纳入判定（低版本恒为 true；26.2+ 不达标时已经在弹窗里拦住）
        if (this.java && this.lib && this.login && this.vulkan) {
            LaunchTask launchTask = new LaunchTask(this.activity, new LaunchTask.LaunchCallback() { // from class: com.qcl.launcher.launcher.launch.check.LaunchCheckDialog.4
                @Override // com.qcl.launcher.launcher.launch.check.LaunchTask.LaunchCallback
                public void onStart() {
                }

                @Override // com.qcl.launcher.launcher.launch.check.LaunchTask.LaunchCallback
                public void onFinish(Vector<String> vector) {
                    LaunchCheckDialog.this.launchState.setBackground(LaunchCheckDialog.this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
                    LaunchCheckDialog.this.appendLog(getContext().getString(R.string.launch_check_dialog_launch) + " ... OK");
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
