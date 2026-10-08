package com.qcl.launcher.launcher.uis.multiplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.terracotta.TerracottaHelper;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import java.io.File;

/**
 * 多人联机（Terracotta | 陶瓦联机）二级页面 —— 照 FCL 的 {@code MultiplayerUI} 重做。
 *
 * <p>为什么要有这一页：原来点「多人联机」只弹一个白色小 AlertDialog，字还看不见。
 * 现在改成完整的二级页面：左栏导航（关于 Terracotta / 房主教程 / 房客教程 / 反馈 / 关于 EasyTier），
 * 右侧内容区按左栏选择互斥切换（主设置面板 / 房主教程正文 / 房客教程正文）。
 *
 * <p>开关状态与游戏内悬浮窗共用同一份 SharedPreferences（{@code qcl_multiplayer}），
 * 保证「启动器里开了，游戏里就能用」。
 */
public class MultiplayerUI extends BaseUI implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    /** 与 {@code MultiplayerDialogHelper} 共用同一个 PREF，开关状态互通。 */
    private static final String PREF = "qcl_multiplayer";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_AGREED = "agreed";

    /**
     * 页面根容器。★ 注意类型是 {@link View} 而不是 LinearLayout：
     * 布局根是 {@code androidx.constraintlayout.widget.ConstraintLayout}（照 FCL 用约束做 30% 左栏），
     * 写成 LinearLayout 会 ClassCastException（实测踩过）。
     */
    public View multiPlayerUI;

    private LinearLayout btnMain;
    private LinearLayout btnHost;
    private LinearLayout btnGuest;
    private LinearLayout btnFeedback;
    private LinearLayout btnEasytier;

    private ScrollView mainLayout;
    private ScrollView hostLayout;
    private ScrollView guestLayout;

    private SwitchCompat switchMultiplayer;
    private LinearLayout extraLayout;
    private Button shareLog;

    /** 防止 onCreate 里 setChecked 触发 onCheckedChanged 时误改配置。 */
    private boolean switchReady = false;

    public MultiplayerUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.multiPlayerUI = this.activity.findViewById(R.id.ui_multi_player);

        this.btnMain = (LinearLayout) this.activity.findViewById(R.id.about_terracotta);
        this.btnHost = (LinearLayout) this.activity.findViewById(R.id.host_tutorial);
        this.btnGuest = (LinearLayout) this.activity.findViewById(R.id.guest_tutorial);
        this.btnFeedback = (LinearLayout) this.activity.findViewById(R.id.terracotta_feedback);
        this.btnEasytier = (LinearLayout) this.activity.findViewById(R.id.about_easytier);

        if (this.btnMain != null) {
            this.btnMain.setOnClickListener(this);
        }
        if (this.btnHost != null) {
            this.btnHost.setOnClickListener(this);
        }
        if (this.btnGuest != null) {
            this.btnGuest.setOnClickListener(this);
        }
        if (this.btnFeedback != null) {
            this.btnFeedback.setOnClickListener(this);
        }
        if (this.btnEasytier != null) {
            this.btnEasytier.setOnClickListener(this);
        }

        this.mainLayout = (ScrollView) this.activity.findViewById(R.id.multiplayer_layout_settings);
        this.hostLayout = (ScrollView) this.activity.findViewById(R.id.tutorial_text_host);
        this.guestLayout = (ScrollView) this.activity.findViewById(R.id.tutorial_text_guest);

        if (this.mainLayout != null) {
            this.mainLayout.setVisibility(View.VISIBLE);
        }
        if (this.hostLayout != null) {
            this.hostLayout.setVisibility(View.GONE);
        }
        if (this.guestLayout != null) {
            this.guestLayout.setVisibility(View.GONE);
        }

        this.extraLayout = (LinearLayout) this.activity.findViewById(R.id.extra_layout);
        this.shareLog = (Button) this.activity.findViewById(R.id.export_log);
        if (this.shareLog != null) {
            this.shareLog.setOnClickListener(this);
        }

        this.switchMultiplayer = (SwitchCompat) this.activity.findViewById(R.id.enable_multiplayer);
        if (this.switchMultiplayer != null) {
            SharedPreferences sp = this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            boolean enabled = sp.getBoolean(KEY_ENABLED, false);
            this.switchMultiplayer.setChecked(enabled);
            if (this.extraLayout != null) {
                this.extraLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
            this.switchReady = true;
            this.switchMultiplayer.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 每次进来都同步一次开关（可能在游戏内或其他入口被改过）
        if (this.switchMultiplayer != null) {
            SharedPreferences sp = this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            boolean enabled = sp.getBoolean(KEY_ENABLED, false);
            this.switchReady = false;
            this.switchMultiplayer.setChecked(enabled);
            if (this.extraLayout != null) {
                this.extraLayout.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
            this.switchReady = true;
        }
        this.activity.showBarTitle(this.context.getResources().getString(R.string.terracotta), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.multiPlayerUI, this.activity, this.context, true);
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.multiPlayerUI, this.activity, this.context, true);
    }

    @Override
    public void onClick(View view) {
        if (view == this.btnMain) {
            showPanel(0);
        } else if (view == this.btnHost) {
            showPanel(1);
        } else if (view == this.btnGuest) {
            showPanel(2);
        } else if (view == this.btnFeedback) {
            openFeedback();
        } else if (view == this.btnEasytier) {
            openLink("https://easytier.cn/");
        } else if (view == this.shareLog) {
            shareLog();
        }
    }

    /** 0=主设置 1=房主教程 2=房客教程 */
    private void showPanel(int which) {
        if (this.mainLayout != null) {
            this.mainLayout.setVisibility(which == 0 ? View.VISIBLE : View.GONE);
        }
        if (this.hostLayout != null) {
            this.hostLayout.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
        }
        if (this.guestLayout != null) {
            this.guestLayout.setVisibility(which == 2 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView != this.switchMultiplayer || !this.switchReady) {
            return;
        }
        SharedPreferences sp = this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        // 首次开启时先让用户确认用户须知（与 FCL 的 terracotta_user_notice 同义）
        boolean agreed = sp.getBoolean(KEY_AGREED, false);
        if (isChecked && !agreed) {
            // 先把 UI 拨回关（不让开关显得已开），等用户确认后再真正开
            this.switchReady = false;
            this.switchMultiplayer.setChecked(false);
            this.switchReady = true;
            new android.app.AlertDialog.Builder(this.context)
                    .setTitle(this.context.getString(R.string.terracotta_enable))
                    .setMessage(this.context.getString(R.string.terracotta_confirm))
                    .setCancelable(false)
                    .setPositiveButton(this.context.getString(R.string.qcl_multiplayer_agree), (d, i) -> {
                        sp.edit().putBoolean(KEY_AGREED, true).apply();
                        // ★★ 注意：这里**不能**只 `setChecked(true)` 就完事 ——
                        //   下面的 switchReady 闸门就是为「程序性 setChecked 不要触发业务逻辑」设计的，
                        //   而此刻恰好需要执行开启逻辑（写 prefs / 显示导出日志 / 起 VPN）。
                        //   ⇒ 改为**直接调用 applySwitch()**，不要绕 onCheckedChanged。
                        this.switchReady = false;
                        this.switchMultiplayer.setChecked(true);
                        this.switchReady = true;
                        applySwitch(true);
                    })
                    .setNegativeButton(this.context.getString(R.string.qcl_cancel), (d, i) -> {
                    })
                    .create().show();
            return;
        }

        applySwitch(isChecked);
    }

    /** 真正落地开关状态：写 prefs + 起/停联机后端 + 显示/隐藏导出日志区。 */
    private void applySwitch(boolean isChecked) {
        SharedPreferences sp = this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ENABLED, isChecked).apply();
        try {
            if (isChecked) {
                TerracottaHelper.initialize(this.activity);
            } else {
                TerracottaHelper.reset(this.activity);
            }
        } catch (Throwable ignored) {
        }
        if (this.extraLayout != null) {
            this.extraLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
    }

    private void openFeedback() {
        String versionName;
        try {
            versionName = this.context.getPackageManager()
                    .getPackageInfo(this.context.getPackageName(), 0).versionName;
        } catch (Throwable e) {
            versionName = "Unknown";
        }
        if (versionName == null) {
            versionName = "Unknown";
        }
        openLink("https://docs.hmcl.net/multiplayer/feedback.html?v=v1&launcher_version=" + versionName);
    }

    private void openLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.context.startActivity(intent);
        } catch (Throwable e) {
            Toast.makeText(this.context, url, Toast.LENGTH_LONG).show();
        }
    }

    private void shareLog() {
        // ★ 工程里没有注册 FileProvider，所以不走 EXTRA_STREAM（file:// 会抛
        //   FileUriExposedException）。改为**把日志文本塞进 EXTRA_TEXT 分享**，
        //   任何 targetSdk 都安全。
        File log = new File(new File(this.context.getExternalFilesDir(null), "debug"), "terracotta.log");
        if (!log.exists()) {
            Toast.makeText(this.context,
                    this.context.getString(R.string.terracotta_export_log_share_null),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String text;
        try {
            text = readText(log);
        } catch (Throwable e) {
            Toast.makeText(this.context,
                    this.context.getString(R.string.terracotta_export_log_failed),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "terracotta.log");
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.activity.startActivity(Intent.createChooser(intent,
                    this.context.getString(R.string.terracotta_export_log_share)));
        } catch (Throwable e) {
            Toast.makeText(this.context,
                    this.context.getString(R.string.terracotta_export_log_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private static String readText(File file) throws Exception {
        java.io.BufferedReader reader = null;
        try {
            reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    new java.io.FileInputStream(file), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            int lines = 0;
            while ((line = reader.readLine()) != null && lines < 3000) {
                sb.append(line).append('\n');
                lines++;
            }
            return sb.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
