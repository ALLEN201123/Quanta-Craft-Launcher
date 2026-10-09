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
    /** ★ 2026-10-09：Vulkan 弹窗是否真的弹出来了（没弹出来就不能一直卡住启动） */
    private boolean vulkanDialogShown;
    /** ★ 2026-10-09 用户要求：模组检测行 */
    private android.widget.LinearLayout modRow;
    private ImageView modState;
    private TextView modText;
    /** ★ 2026-10-09 用户要求：游戏版本检测行 */
    private android.widget.LinearLayout gameVersionRow;
    private ImageView gameVersionState;
    private TextView gameVersionText;
    /**
     * ★ 2026-10-09 用户要求：这两项预检**必须等玩家点按钮才继续**（之前 Vulkan 弹窗一出现
     * 启动就自己往下跑了）。默认 false = 闸门关着；检测完成后由玩家按钮开闸。
     */
    private boolean preflight = false;
    private boolean preflightDialogShown;
    /** 最近一次预检结果（点行可看详情，超 FCL） */
    private LaunchPreflightChecks.Result modResult;
    private LaunchPreflightChecks.Result gameVersionResult;
    /**
     * ★ 2026-10-09 用户实测修复：「点了取消，它却在后台静默启动、然后突然跳到等待界面」。
     *
     * <p>根因：取消按钮当时写的是 `vulkan = true; cancel();` —— `Dialog.cancel()` 只关窗口、
     * **不会取消那三个 AsyncTask**；任务跑完仍会回调 `checkState()`，此刻闸门全开 ⇒ 后台启动。
     * ⇒ 加这个「已中止」标志：任何取消路径都置 true 并走 `exit()`（真取消任务），
     * `checkState()` 见到它就永不发起启动。
     */
    private boolean aborted;

    /**
     * ★★★ 1.5.0 用户实测修复（用户原话：「点仍然要启动，它没反应。再启动一次，点了它启动了一次，
     *   进入等待界面；第二次又突然黑屏，进入等待界面，然后**又给我启动了一次**」）。
     *
     * <p>根因：{@link #checkState()} **完全没有防重入**。它被四个预检任务（Java / 库 / 账号 /
     * Vulkan / 预检行）各自回调一次，只要条件都满足就**每回调一次 new 一个 LaunchTask 并 execute**
     * ⇒ 一次点击可能启动 2~3 个游戏进程；玩家再点一次又是新的一批 ⇒ 现象就是"启动两次、黑屏、反复"。
     * <p>而且 {@link #exit()} 只能 cancel `this.launchTask`（最后一个），
     * 先前已经 execute 的那些**杀不掉** —— 这正是用户怀疑的"取消没杀干净"。
     *
     * <p>⇒ 修法：<b>启动闸门</b> —— {@code launchStarted} 一旦置 true，后续 checkState 直接返回，
     * 整个对话框生命周期内**最多只会启动一次**。
     */
    private boolean launchStarted;

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
            // ★ 2026-09 用户：「控制台移到正中间」——
            //   Dialog 窗口的位置**只由 WindowManager.LayoutParams.gravity 决定**，
            //   布局文件里的 layout_gravity 管它不了（那是内容在窗口内的对齐）。
            //   所以这里显式设 CENTER，不再依赖各 ROM 的默认值（默认往往是偏上/偏左）。
            try {
                android.view.WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.gravity = android.view.Gravity.CENTER;
                getWindow().setAttributes(lp);
            } catch (Throwable ignoreGravity) {
                // 拿不到 attributes 就用默认
            }
            // ★ 1.5.0：对话框高度按屏幕自适应（对照 FCL：屏幕高 × 3/4，不低于 300dp、不高于 560dp）。
            //   布局根已是 wrap_content + 内部 ScrollView，内容再多也能上下滑动。
            try {
                android.util.DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
                int quarteScreen = (int) (dm.heightPixels * 0.78f);
                int minH = com.qcl.launcher.utils.convert.ConvertUtils.dip2px(getContext(), 300f);
                int maxH = com.qcl.launcher.utils.convert.ConvertUtils.dip2px(getContext(), 560f);
                int target = Math.max(minH, Math.min(maxH, quarteScreen));
                // ★ 2026-10-09：宽度必须**等于内容宽度**，否则窗口虽居中、内容却被钉在窗口左边。
                //   实测：窗口 0.62×1600=992px、内容固定 500dp≈750px ⇒ 面板中心 x≈679 ≠ 屏心 800。
                //   ⇒ 取「500dp 与 92% 屏宽」的较小值，配合布局根的 layout_gravity="center" 真正居中。
                int wantW = com.qcl.launcher.utils.convert.ConvertUtils.dip2px(getContext(), 500f);
                int maxW = (int) (dm.widthPixels * 0.92f);
                getWindow().setLayout(Math.min(wantW, maxW), target);
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
        // ★ 2026-10-09 用户要求：新增「模组检测」「游戏版本检测」两行
        this.modRow = (android.widget.LinearLayout) findViewById(R.id.check_mod_row);
        this.modState = (ImageView) findViewById(R.id.check_mod_state);
        this.modText = (TextView) findViewById(R.id.check_mod_text);
        this.gameVersionRow = (android.widget.LinearLayout) findViewById(R.id.check_game_version_row);
        this.gameVersionState = (ImageView) findViewById(R.id.check_game_version_state);
        this.gameVersionText = (TextView) findViewById(R.id.check_game_version_text);
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
        startVulkanCheck();
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
        // ★ 2026-10-09：主题色上屏 + 两项新预检（模组 / 游戏版本）
        applyThemeColor();
        startPreflightChecks();
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
            // ★★★★★ 1.5.0（用户实测「检测逻辑的弹窗根本没有弹出来」）：
            //   原来 `ok == true` 就直接 `return` 放行，玩家什么都不知道，
            //   结果后面照样崩（判定通过 ≠ 实际能用：系统属性填了不代表 Turnip/后端真的就绪）。
            //   ⇒ 26.2+ 一律**弹窗告知后端实况**，让玩家知情并可选择：
            //       「仍要启动」= 按现状继续；「强制 Vulkan」= 把 preferredGraphicsBackend 钉死再走；
            //       「取消」= 回版本列表。
            // ★ 2026-10-09 用户要求：**弹窗只在 26.3+ 出现**。
            //   26.2 虽然也带 Vulkan 后端，但 26.3 起才把要求收紧到"必须"；
            //   26.2 弹这个框是骚扰玩家 ⇒ 只显示行结果，不弹窗、也不关闸。
            boolean needNotice = com.qcl.launcher.launcher.launch.vulkan.VulkanRequirement
                    .needsVulkanNotice(this.launchVersion);
            if (!needNotice) {
                this.vulkan = true;
                checkState();
                return;
            }
            // ★ 2026-10-09 用户实测修复（「弹窗出来了，但启动仍在继续」）：
            //   这里**必须先关闸**。旧代码是 `vulkan = true` 之后再弹窗 ⇒ 弹窗还在屏幕上时
            //   checkState() 已经满足全部条件，启动流程自己就往下跑了，玩家根本没机会点按钮。
            //   ⇒ 改成 vulkan = false，由弹窗三个按钮里任一个来开闸。
            this.vulkan = false;
            if (this.vulkanState != null) {
                this.vulkanState.setBackground(
                        ok ? this.getContext().getDrawable(R.drawable.ic_baseline_done_white)
                           : this.getContext().getDrawable(R.drawable.ic_baseline_close_white));
            }
            showVulkanNotice(caps, support, ok);
            armGateSafety();
        } catch (Throwable t) {
            // 任何异常都放行，绝不误拦玩家
            this.vulkan = true;
            if (this.vulkanState != null) {
                this.vulkanState.setBackground(
                        this.getContext().getDrawable(R.drawable.ic_baseline_done_white));
            }
            checkState();
        }
    }

    /**
     * ★ 1.5.0：26.2+ 启动**必定**弹一次后端实况（无论判定通过与否）。
     *
     * <p>存在的意义：判定"通过"不等于实际能跑（系统属性 vs 真实加载能力 vs MC 是否降级），
     * 玩家在没有提示的情况下崩溃是最糟的体验。三个出口：
     * <ul>
     *   <li><b>仍要启动</b>：按当前状态继续（判定通过时的默认行为）；</li>
     *   <li><b>强制 Vulkan</b>：把该版本 options.txt 的 preferredGraphicsBackend 钉成 vulkan 再启动，
     *       这是绕开「MC 因上次崩溃而永久降级 OpenGL」的唯一手段；</li>
     *   <li><b>取消</b>：中止启动。</li>
     * </ul>
     */
    private void showVulkanNotice(final com.qcl.launcher.launcher.launch.vulkan.VulkanCapabilities caps,
                                  final com.qcl.launcher.launcher.launch.vulkan.VulkanSupport support,
                                  final boolean ok) {
        // ★★★★★ 1.5.0 修正（用户实测「弹窗打死也弹不出来」）：
        //   我原先是在 onCreate() → startCheckTasks() 里直接弹 —— 那个时机
        //   **本对话框自己还没 show()**，新建的 AlertDialog 拿不到父窗口 token，
        //   结果**永远弹不出来**（用户点启动看到的就是"什么提示都没有"）。
        //   ⇒ 改成：先记下内容，等本对话框真正显示后再延迟弹。
        final String src = com.qcl.launcher.launcher.launch.vulkan.VulkanChecker.getVulkanSourceSafe();
        // ★ 2026-10-09 修正：launchVersion 是**完整路径**（项目铁律：currentVersion 存路径），
        //   直接塞进文案会显示成 /storage/emulated/0/.../26.3 —— 这里只取版本名。
        final String versionName = new java.io.File(this.launchVersion).getName();
        final String msg = "本版本（" + versionName + "）需要 Vulkan 图形后端。\n"
                + "检测结果：" + (ok ? "系统报告支持 Vulkan " + caps.versionString()
                                    : "**未能确认**（Vulkan " + caps.versionString()
                                      + " / level " + (caps.hardwareLevel < 0 ? "?" : caps.hardwareLevel) + "）")
                + "\n加载器：" + ("turnip".equals(src) ? "自带 Turnip"
                                    : "system".equals(src) ? "系统 libvulkan.so"
                                    : ("none".equals(src) ? "尚未加载" : src))
                + "\n\n若启动后崩溃，多半是 Minecraft 沿用了上次的降级设置，"
                + "可点「强制 Vulkan 后启动」重试。";
        // ★ 2026-10-09：统一走主题色弹窗出口；三个按钮**任点一个**才放行（用户要求「必须点一下按钮」）。
        final Runnable show = new Runnable() {
            @Override
            public void run() {
                LaunchCheckDialog.this.vulkanDialogShown = true;
                showThemedGate(LaunchCheckDialog.this.getContext().getString(R.string.launch_check_dialog_vulkan),
                        msg,
                        "强制 VULKAN 后启动", "仍要启动", "取消",
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    com.qcl.launcher.launcher.launch.GpuBackendSelector
                                            .forceVulkan(LaunchCheckDialog.this.activity,
                                                    LaunchCheckDialog.this.launchVersion);
                                } catch (Throwable ignored) {
                                }
                                LaunchCheckDialog.this.vulkan = true;
                                LaunchCheckDialog.this.checkState();
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                LaunchCheckDialog.this.vulkan = true;
                                LaunchCheckDialog.this.checkState();
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                // ★ 2026-10-09 修复：原来这里是 `vulkan = true; cancel();`
                                //   cancel() 只关窗口、不取消 AsyncTask ⇒ 任务回来照样 checkState()
                                //   ⇒ 后台静默启动。改成 exit()：立「已中止」旗标 + 取消全部任务。
                                LaunchCheckDialog.this.exit();
                            }
                        });
            }
        };
        // 延迟一点，确保本对话框已获得窗口焦点
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(show, 600L);
        } catch (Throwable ignored) {
            show.run();
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
        // ★ 2026-10-09：玩家一旦取消，后续任何回调都不许再发起启动（修「取消后仍后台启动」）
        if (this.aborted) {
            return;
        }
        // ★★★ 1.5.0 修复「启动两次」：预检任务会多次回调 checkState()，
        //   每次都 new LaunchTask().execute() ⇒ 一次点击起多个进程，且 exit() 杀不干净。
        //   ⇒ 启动闸门：一旦发起过启动，后续一律不再发起。
        if (this.launchStarted) {
            return;
        }
        // ★ 1.5.0：Vulkan 也纳入判定（低版本恒为 true；26.2+ 不达标时已经在弹窗里拦住）
        // ★ 2026-10-09：预检（模组/版本）同理纳入 —— 有致命问题时必须等玩家点按钮才放行
        if (this.java && this.lib && this.login && this.vulkan && this.preflight) {
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
            // ★ 1.5.0：闸门在**发起前**就关上 —— 必须在 execute 之前置位，
            //   否则 execute 后紧接着来的其它回调仍会再起一个。
            this.launchStarted = true;
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
        // ★ 2026-10-09：先立「已中止」旗标，再取消任务 —— 否则已排队的回调仍会跑 checkState() 把游戏启起来
        this.aborted = true;
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

    // =================★ 2026-10-09：主题色绑定 + 模组/游戏版本预检 + 统一弹窗出口=================

    /** 当前主题色（读到设置里的 launcherTheme；取不到回默认斑驳森林色）。 */
    private int themeColor() {
        try {
            // ★ 注意：ExteriorSettingUI.getThemeColor 返回的是**字符串**（如 "#FF556980"），必须解析
            String s = com.qcl.launcher.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI
                    .getThemeColor(getContext(), this.activity.launcherSetting.launcherTheme);
            return android.graphics.Color.parseColor(s);
        } catch (Throwable t) {
            return 0xFF556980;
        }
    }

    /**
     * ★ 2026-10-09 用户要求：这个控制台要**跟设置里的主题色绑定**。
     *
     * <p>做法：把每行底色换成主题色的 40% 透明版，行内文字按底色亮度自动取黑/白（深浅都不瞎），
     * 标题染成主题色。这样玩家在设置里换主题色，这个窗口跟着变。
     */
    private void applyThemeColor() {
        try {
            int color = themeColor();
            int bg = (color & 0x00FFFFFF) | 0x66000000;
            int fg = isDark(color) ? 0xFFFFFFFF : 0xFF000000;

            View content = findViewById(android.R.id.content);
            if (!(content instanceof android.view.ViewGroup) || ((android.view.ViewGroup) content).getChildCount() == 0) {
                return;
            }
            View root = ((android.view.ViewGroup) content).getChildAt(0);
            if (!(root instanceof android.view.ViewGroup)) {
                return;
            }
            android.view.ViewGroup layout = (android.view.ViewGroup) root;
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);
                if (child instanceof android.widget.ScrollView) {
                    View inner = ((android.widget.ScrollView) child).getChildAt(0);
                    if (inner instanceof android.view.ViewGroup) {
                        android.view.ViewGroup rows = (android.view.ViewGroup) inner;
                        for (int j = 0; j < rows.getChildCount(); j++) {
                            View row = rows.getChildAt(j);
                            if (row instanceof android.widget.LinearLayout) {
                                row.setBackgroundColor(bg);
                                paintText(row, fg);
                            }
                        }
                    }
                } else if (child instanceof TextView) {
                    ((TextView) child).setTextColor(color);   // 标题
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void paintText(View v, int fg) {
        if (v instanceof TextView) {
            ((TextView) v).setTextColor(fg);
            return;
        }
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                paintText(g.getChildAt(i), fg);
            }
        }
    }

    private boolean isDark(int color) {
        double r = android.graphics.Color.red(color) / 255.0;
        double g = android.graphics.Color.green(color) / 255.0;
        double b = android.graphics.Color.blue(color) / 255.0;
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) < 0.5;
    }

    /**
     * ★ 2026-10-09：统一弹窗出口。
     * <ol>
     *   <li>标题与按钮用**设置里的主题色**上色（用户要求绑定）；</li>
     *   <li>延迟 600ms 再弹 —— 历史坑：在本对话框构造期直接弹，AlertDialog 拿不到窗口 token，永远弹不出来；</li>
     *   <li>**弹不出来就立即放行**，绝不让启动流程永久卡死。</li>
     * </ol>
     */
    private void showThemedGate(final String title, final String msg,
                                final String positive, final String negative, final String neutral,
                                final Runnable onPositive, final Runnable onNegative, final Runnable onNeutral) {
        final Runnable show = new Runnable() {
            @Override
            public void run() {
                try {
                    if (LaunchCheckDialog.this.activity == null
                            || LaunchCheckDialog.this.activity.isFinishing()) {
                        if (onNeutral != null) {
                            onNeutral.run();
                        }
                        return;
                    }
                    int color = themeColor();
                    AlertDialog.Builder b = new AlertDialog.Builder(LaunchCheckDialog.this.activity);
                    if (title != null) {
                        b.setTitle(title);
                    }
                    if (msg != null) {
                        b.setMessage(msg);
                    }
                    b.setCancelable(false);
                    if (positive != null) {
                        b.setPositiveButton(positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (onPositive != null) {
                                    onPositive.run();
                                }
                            }
                        });
                    }
                    if (negative != null) {
                        b.setNegativeButton(negative, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (onNegative != null) {
                                    onNegative.run();
                                }
                            }
                        });
                    }
                    if (neutral != null) {
                        b.setNeutralButton(neutral, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int w) {
                                if (onNeutral != null) {
                                    onNeutral.run();
                                }
                            }
                        });
                    }
                    tintDialog(b.show(), color);
                } catch (Throwable t) {
                    // 弹不出来 → 放行，避免卡死
                    if (onNeutral != null) {
                        onNeutral.run();
                    }
                }
            }
        };
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(show, 600L);
        } catch (Throwable ignored) {
            show.run();
        }
    }

    /** 把主题色刷到 AlertDialog 的标题与三个按钮上。 */
    private void tintDialog(AlertDialog dlg, int color) {
        try {
            int titleId = getContext().getResources().getIdentifier("alertTitle", "id", "android");
            View t = titleId > 0 ? dlg.findViewById(titleId) : null;
            if (t instanceof TextView) {
                ((TextView) t).setTextColor(color);
            }
            int[] btns = {DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE,
                    DialogInterface.BUTTON_NEUTRAL};
            for (int which : btns) {
                Button btn = dlg.getButton(which);
                if (btn != null) {
                    btn.setTextColor(color);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 万一弹窗没能弹出来，别把启动永久卡死（12 秒兜底；弹窗真出来了就不兜底）。 */
    private void armGateSafety() {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!LaunchCheckDialog.this.vulkanDialogShown) {
                            LaunchCheckDialog.this.vulkan = true;
                        }
                        if (!LaunchCheckDialog.this.preflightDialogShown && !LaunchCheckDialog.this.preflight) {
                            LaunchCheckDialog.this.preflight = true;
                        }
                        LaunchCheckDialog.this.checkState();
                    } catch (Throwable ignored) {
                    }
                }
            }, 12000L);
        } catch (Throwable ignored) {
        }
    }

    /** ② 两项预检：模组检测 + 游戏版本检测（后台跑，结果回主线程上屏）。 */
    /**
     * ★ 2026-10-09：当前版本的渲染器 id。
     * 优先读该版本的 qcl.cfg（玩家可能给单个版本单独设过渲染器），兜底用全局设置。
     */
    private String currentRendererId() {
        try {
            com.qcl.launcher.launcher.setting.game.PrivateGameSetting p =
                    com.qcl.launcher.utils.gson.GsonUtils.getPrivateGameSettingFromFile(this.launchVersion + "/qcl.cfg");
            if (p != null && p.pojavLauncherSetting != null
                    && p.pojavLauncherSetting.renderer != null
                    && !p.pojavLauncherSetting.renderer.isEmpty()) {
                return p.pojavLauncherSetting.renderer;
            }
        } catch (Throwable ignored) {
        }
        try {
            // 兜底：全局私有设置里的渲染器（MainActivity 上没有 gameLaunchSetting 字段）
            com.qcl.launcher.launcher.setting.game.PrivateGameSetting g = this.activity.privateGameSetting;
            if (g != null && g.pojavLauncherSetting != null
                    && g.pojavLauncherSetting.renderer != null) {
                return g.pojavLauncherSetting.renderer;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void startPreflightChecks() {
        final android.content.Context ctx = getContext();
        final String path = this.launchVersion;
        // ★ 2026-10-09：把当前渲染器 id 传下去 —— sodium/embeddium 的提示只该在 GL4ES 时出现
        final String rendererId = currentRendererId();
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    LaunchPreflightChecks.Result mod = null;
                    LaunchPreflightChecks.Result ver = null;
                    try {
                        mod = LaunchPreflightChecks.checkMods(ctx, path, rendererId);
                    } catch (Throwable ignored) {
                    }
                    try {
                        ver = LaunchPreflightChecks.checkGameVersion(ctx, path);
                    } catch (Throwable ignored) {
                    }
                    final LaunchPreflightChecks.Result fm = mod;
                    final LaunchPreflightChecks.Result fv = ver;
                    try {
                        LaunchCheckDialog.this.activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LaunchCheckDialog.this.applyPreflight(fm, fv);
                            }
                        });
                    } catch (Throwable ignored) {
                    }
                }
            }, "qcl-preflight").start();
        } catch (Throwable t) {
            releasePreflight();
        }
    }

    private void applyPreflight(LaunchPreflightChecks.Result mod, LaunchPreflightChecks.Result ver) {
        try {
            this.modResult = mod;
            this.gameVersionResult = ver;
            boolean modOk = mod == null || mod.ok;
            boolean verOk = ver == null || ver.ok;
            if (this.modState != null) {
                this.modState.setBackground(getContext().getDrawable(modOk
                        ? R.drawable.ic_baseline_done_white : R.drawable.ic_baseline_close_white));
            }
            if (this.modText != null) {
                this.modText.setText(getContext().getString(R.string.launch_check_dialog_mod)
                        + (mod == null ? "" : ("  ·  " + mod.summary)));
            }
            if (this.gameVersionState != null) {
                this.gameVersionState.setBackground(getContext().getDrawable(verOk
                        ? R.drawable.ic_baseline_done_white : R.drawable.ic_baseline_close_white));
            }
            if (this.gameVersionText != null) {
                this.gameVersionText.setText(getContext().getString(R.string.launch_check_dialog_game_version)
                        + (ver == null ? "" : ("  ·  " + ver.summary)));
            }
            // 超 FCL：行本身可点 → 逐条详情
            if (this.modRow != null) {
                this.modRow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPreflightDetail(false);
                    }
                });
            }
            if (this.gameVersionRow != null) {
                this.gameVersionRow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showPreflightDetail(true);
                    }
                });
            }
            if (!modOk || !verOk) {
                showPreflightNotice(modOk ? null : mod, verOk ? null : ver);
            } else {
                releasePreflight();
            }
        } catch (Throwable t) {
            releasePreflight();
        }
    }

    private void releasePreflight() {
        this.preflight = true;
        checkState();
    }

    /** 点行看详情（超 FCL 的核心：把问题逐条列出来，而不是只说"有问题"）。 */
    private void showPreflightDetail(boolean versionRow) {
        LaunchPreflightChecks.Result r = versionRow ? this.gameVersionResult : this.modResult;
        try {
            String title = getContext().getString(versionRow
                    ? R.string.launch_check_dialog_game_version : R.string.launch_check_dialog_mod);
            String msg = (r == null) ? "尚未完成检测。" : LaunchPreflightChecks.formatDetails(title, r);
            showThemedGate(title, msg, null, null, "知道了", null, null, null);
        } catch (Throwable ignored) {
        }
    }

    /** 预检有问题时的拦截图：**必须点按钮才继续**（用户要求）。 */
    private void showPreflightNotice(LaunchPreflightChecks.Result mod,
                                     LaunchPreflightChecks.Result ver) {
        StringBuilder sb = new StringBuilder();
        try {
            if (ver != null) {
                sb.append(LaunchPreflightChecks.formatDetails(
                        getContext().getString(R.string.launch_check_dialog_game_version), ver));
            }
            if (mod != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                // ★ 2026-10-09 用户要求：文案要说清「该模组可能不兼容 QCL 启动器」
                sb.append("以下模组可能不兼容 QCL 启动器：\n\n");
                sb.append(LaunchPreflightChecks.formatDetails(
                        getContext().getString(R.string.launch_check_dialog_mod), mod));
            }
        } catch (Throwable ignored) {
        }
        this.preflightDialogShown = true;
        showThemedGate(getContext().getString(R.string.launch_check_dialog_preflight_title),
                sb.toString(), null, "仍要启动", "取消",
                null,
                new Runnable() {
                    @Override
                    public void run() {
                        releasePreflight();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        // ★ 2026-10-09 修复：同上 —— 取消必须真中止，不能只关窗口
                        LaunchCheckDialog.this.exit();
                    }
                });
    }
}
