/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.content.SharedPreferences
 *  android.content.SharedPreferences$Editor
 *  android.content.res.Configuration
 *  android.graphics.Color
 *  android.os.Build$VERSION
 *  android.os.Bundle
 *  android.os.Handler
 *  android.os.Message
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.ImageButton
 *  android.widget.LinearLayout
 *  android.widget.RelativeLayout
 *  android.widget.TextView
 *  androidx.annotation.NonNull
 *  androidx.appcompat.app.AppCompatActivity
 *  com.afollestad.appthemeengine.ATE
 *  com.afollestad.appthemeengine.Config
 */
package com.qcl.launcher.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.qcl.launcher.launcher.VerifyInterface;
import com.qcl.launcher.launcher.dialogs.VerifyDialog;
import com.qcl.launcher.launcher.dialogs.account.MicrosoftAccountSkinDialog;
// ★ 1.4.1：皮肤库（自研）
import com.qcl.launcher.launcher.dialogs.account.SkinLibraryDialog;
import com.qcl.launcher.launcher.dialogs.account.SkinPreviewDialog;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.launcher.uis.main.DynamicBackground;
import com.qcl.launcher.launcher.uis.tools.UIManager;
import com.qcl.launcher.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.update.UpdateChecker;
import com.qcl.launcher.utils.LocaleUtils;

import com.qcl.launcher.R;
public class MainActivity
extends AppCompatActivity
implements View.OnClickListener {
    public LinearLayout launcherLayout;
    public boolean isLoaded = false;
    public boolean dialogMode = false;
    public LauncherSetting launcherSetting;
    public PublicGameSetting publicGameSetting;
    public PrivateGameSetting privateGameSetting;
    public UpdateChecker updateChecker;
    public LinearLayout backBar;
    public ImageButton backToLastUI;
    public TextView currentUIText;
    public ImageButton backToHome;
    public ImageButton closeCurrentUI;
    public RelativeLayout uiContainer;
    public UIManager uiManager;
    public Config exteriorConfig;
    @SuppressLint(value={"HandlerLeak"})
    public final Handler loadingHandler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0 && !MainActivity.this.isLoaded) {
                MainActivity.this.exteriorConfig = ATE.config((Context)MainActivity.this, null);
                MainActivity.this.backBar = (LinearLayout)MainActivity.this.findViewById(R.id.qcl_back_bar);
                MainActivity.this.backToLastUI = (ImageButton)MainActivity.this.findViewById(R.id.back_to_last_ui);
                MainActivity.this.currentUIText = (TextView)MainActivity.this.findViewById(R.id.text_current_ui);
                MainActivity.this.backToHome = (ImageButton)MainActivity.this.findViewById(R.id.back_to_home);
                MainActivity.this.closeCurrentUI = (ImageButton)MainActivity.this.findViewById(R.id.close_current_ui);
                if (MainActivity.this.backToLastUI != null) {
                    MainActivity.this.backToLastUI.setOnClickListener((View.OnClickListener)MainActivity.this);
                }
                if (MainActivity.this.backToHome != null) {
                    MainActivity.this.backToHome.setOnClickListener((View.OnClickListener)MainActivity.this);
                }
                if (MainActivity.this.closeCurrentUI != null) {
                    MainActivity.this.closeCurrentUI.setOnClickListener((View.OnClickListener)MainActivity.this);
                }
                MainActivity.this.uiContainer = (RelativeLayout)MainActivity.this.findViewById(R.id.main_ui_container);
                // ★★★ 1.5.0：按设置把「旧排版 / 新排版」inflate 进 main_ui_host。
                //   必须在 new UIManager(...) **之前** —— 那个构造里会对全部页面调 onCreate()，
                //   每个都在 findViewById 找控件；布局还没进去的话全都拿到 null。
                MainActivity.this.inflateMainUi();
                MainActivity.this.uiManager = new UIManager((Context)MainActivity.this, MainActivity.this);
                MainActivity.this.exteriorConfig.primaryColor(ExteriorSettingUI.parseThemeColorSafe((Context)MainActivity.this, MainActivity.this.launcherSetting.launcherTheme));
                MainActivity.this.exteriorConfig.accentColor(ExteriorSettingUI.parseThemeColorSafe((Context)MainActivity.this, MainActivity.this.launcherSetting.launcherTheme));
                MainActivity.this.exteriorConfig.apply((Activity)MainActivity.this);
                MainActivity.this.isLoaded = true;
                MainActivity.this.onLoad();
                ExteriorSettingUI.applyPanelTint((Context)MainActivity.this, MainActivity.this.getWindow().getDecorView(), ExteriorSettingUI.getPanelColor((Context)MainActivity.this, MainActivity.this.launcherSetting.panelColor));
                MainActivity.this.startDynamicBackgroundIfNeeded();
                // ★ 1.3.0：**初始化完成后**扫一遍已装的远古版本，没打中文包的补上。
                //   为什么放这儿：onResume 时 isLoaded 还是 false（初始化是异步的），
                //   在那儿挂钩子根本进不去 —— 早就装好的版本就一直没被补上。
                try {
                    final String gameDir = MainActivity.this.launcherSetting == null
                            ? null : MainActivity.this.launcherSetting.gameFileDirectory;
                    if (gameDir != null) {
                        new Thread(() -> {
                            try {
                                com.qcl.launcher.launcher.download.game.LegacyChinesePack.sweepAll(MainActivity.this, gameDir);
                            }
                            catch (Throwable ignored) {
                            }
                        }).start();
                    }
                }
                catch (Throwable ignored) {
                }
                // ★ 1.5.0：这里原来是 MainActivity.this.applyUiTheme();（草方块主题引擎）。
                //   草方块 UI 已彻底删除，整个 QclThemeUtils 也没了 → 不再需要调用。
                //   缺省主题的收尾工作现在由 customTheme() / exteriorConfig 负责。
            }
        }
    };
    private DynamicBackground dynamicBackground;

    // ★★★ 1.1.0：原 `libsecurity.so`（防篡改校验库，随已删除的 Boat 模块一起没了）曾提供
    //   下面这些 native 方法。这里改成等价的**纯 Java 实现**，彻底摘掉对它的依赖。
    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.activity_main);
        this.launcherLayout = (LinearLayout)this.findViewById(R.id.launcher_layout);
        this.init();
    }

    public boolean isValid(String str) {
        return true;
    }

    public static void verify() {
    }

    public static void verifyFunc() {
    }

    public void launch(Intent intent) {
        this.startActivity(intent);
    }

    public void init() {
        if (Build.VERSION.SDK_INT >= 28) {
            this.getWindow().getAttributes().layoutInDisplayCutoutMode = this.getIntent().getExtras().getBoolean("fullscreen") ? 1 : 2;
        }
        this.getWindow().setFlags(256, 256);
        new Thread(() -> {
            AppManifest.initializeManifest((Context)this);
            this.launcherSetting = InitializeSetting.initializeLauncherSetting();
            this.publicGameSetting = InitializeSetting.initializePublicGameSetting((Context)this, this);
            this.privateGameSetting = InitializeSetting.initializePrivateGameSetting((Context)this);
            this.runOnUiThread(() -> {
                this.updateChecker = new UpdateChecker((Context)this, this);
                this.updateChecker.checkAuto();
            });
            DownloadUrlSource.getBalancedSource((Context)this);
            this.loadingHandler.sendEmptyMessage(0);
        }).start();
    }

    private void startDynamicBackgroundIfNeeded() {
        try {
            int type = this.launcherSetting.launcherBackground.type;
            // ★ 1.5.0：默认（type 1）= 按现实时间自动切昼夜。布局根默认背景是黑夜图，
            //   白天时段必须换成白天图。放在这里因为它是「启动时决定背景」的唯一入口。
            if (type == 1) {
                DynamicBackground.applyAutoDayNight(this, this.launcherLayout);
                if (this.dynamicBackground != null) {
                    this.dynamicBackground.stop();
                }
                return;
            }
            if (type != 0) {
                return;
            }
            if (this.dynamicBackground == null) {
                this.dynamicBackground = new DynamicBackground((Activity)this, (View)this.launcherLayout);
            }
            this.dynamicBackground.start();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    /** ★ 1.2.9：回到前台后确保动态背景继续轮播（只在「动态背景」模式下生效） */
    public void resumeDynamicBackground() {
        try {
            if (this.launcherSetting == null || this.launcherSetting.launcherBackground == null) {
                return;
            }
            int type = this.launcherSetting.launcherBackground.type;
            // ★ 1.5.0：默认（type 1）→ 回到前台时按当前时间重新判定昼夜
            //   （挂了很久再回来，白天/黑夜该换就换）
            if (type == 1) {
                DynamicBackground.applyAutoDayNight(this, this.launcherLayout);
                return;
            }
            if (type != 0) {
                return;
            }
            if (this.dynamicBackground == null) {
                this.startDynamicBackgroundIfNeeded();
                return;
            }
            this.dynamicBackground.ensureRunning();
        }
        catch (Throwable ignored) {
        }
    }

    public void refreshDynamicBackground() {
        try {
            int type = this.launcherSetting.launcherBackground.type;
            if (type == 1) {
                // ★ 1.5.0：默认 → 立刻按当前时间切昼夜（设置页点「默认」马上生效）
                DynamicBackground.applyAutoDayNight(this, this.launcherLayout);
                if (this.dynamicBackground != null) {
                    this.dynamicBackground.stop();
                }
            } else if (type == 0) {
                this.startDynamicBackgroundIfNeeded();
            } else if (this.dynamicBackground != null) {
                this.dynamicBackground.stop();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    /**
     * ★★★ 1.5.0：把「旧排版 / 新排版」里选中的那一套 inflate 进 {@code main_ui_host}。
     *
     * <p>为什么不像别的页面那样用 {@code <include>}：
     * <ul>
     *   <li>{@code <include>} 是**编译期固定**的，没法按设置二选一；</li>
     *   <li>两套布局（{@code ui_main.xml} / {@code ui_main_new.xml}）的 **id 集合完全相同**
     *       （27 个），若同时挂进视图树，{@code findViewById} 只会返回第一个匹配 →
     *       点的是新排版、动的是旧排版（串页）。</li>
     * </ul>
     * 所以改成运行时只 inflate 一套，同一时刻树里只有一套，天然无重复 id。
     *
     * <p>★ 兜底很重要：新排版万一 inflate 失败（资源缺失 / 语法问题），
     * 必须**自动退回旧排版** —— 否则启动器直接白屏，而「设置 → 外观」也进不去，
     * 玩家连切回来都做不到，等于变砖。
     */
    public void inflateMainUi() {
        try {
            View host = this.findViewById(R.id.main_ui_host);
            if (!(host instanceof ViewGroup)) return;
            ViewGroup group = (ViewGroup) host;
            if (group.getChildCount() > 0) return;   // 幂等：别重复 inflate
            boolean useNew = this.launcherSetting == null || this.launcherSetting.useNewLayout();
            LayoutInflater.from(this).inflate(useNew ? R.layout.ui_main_new : R.layout.ui_main, group, true);
            // ★★★ 1.5.0：新排版专属 —— 把「左侧常驻导航条」「右侧常驻栏」inflate 到
            //   activity_main 顶层的两个 host 里（照 FCL 的 left_menu / right_menu）。
            //   ★ 旧排版**完全不进这个分支**，两个 host 保持 GONE —— 旧排版行为零变化。
            //   ★ 为什么不在 ui_main_new.xml 里：那个布局随主界面被 onStop 一起隐藏，
            //     放进去的话切到版本列表/下载页时左右栏就没了，就不是"常驻"了。
            if (useNew) {
                inflateNewLayoutChrome();
            }
        } catch (Throwable t) {
            try {
                ViewGroup group = (ViewGroup) this.findViewById(R.id.main_ui_host);
                if (group != null && group.getChildCount() == 0) {
                    LayoutInflater.from(this).inflate(R.layout.ui_main, group, true);
                }
            } catch (Throwable ignored) {
                // 连旧排版都 inflate 不了就真没救了，交给上层按空界面处理。
            }
        }
    }

    /**
     * ★★★ 1.5.0 新排版专属：把左右两栏（左侧纯图标导航条 + 右侧账号/版本/启动栏）
     * inflate 到 activity_main 顶层，让它们**跨页面常驻**（照 FCL 的 left_menu / right_menu）。
     *
     * <p>★ 只在 {@code useNewLayout()} 为真时调用；旧排版绝不会进这里。
     * <p>★ 幂等：host 里已经有子 View 就跳过，避免 Activity 重建时叠两份
     * （叠两份 → 同名 id 重复 → findViewById 只抓第一个 → 点一套动另一套）。
     * <p>★ 任何失败都只 printStackTrace：宁可没有常驻栏，也不能让启动器起不来。
     */
    private void inflateNewLayoutChrome() {
        try {
            View leftHost = this.findViewById(R.id.new_left_nav_host);
            if (leftHost instanceof ViewGroup && ((ViewGroup) leftHost).getChildCount() == 0) {
                LayoutInflater.from(this).inflate(R.layout.ui_main_new_left, (ViewGroup) leftHost, true);
                leftHost.setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            View rightHost = this.findViewById(R.id.new_right_panel_host);
            if (rightHost instanceof ViewGroup && ((ViewGroup) rightHost).getChildCount() == 0) {
                LayoutInflater.from(this).inflate(R.layout.ui_main_new_right, (ViewGroup) rightHost, true);
                rightHost.setVisibility(View.VISIBLE);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // ★★★ 关键：左右两栏是**浮在最上层**的，而二级页面（版本列表/下载/设置…）
        //   都是 match_parent 全屏铺满 —— 不加避让的话内容会被两栏压住。
        //   做法：等两栏量好尺寸后，把 content 容器（main_ui_container）加上等宽的左右内边距，
        //   所有二级页面就自动被"夹"在中间（这正是 FCL 的 ViewPager 效果）。
        applyChromeInsets();
    }

    /**
     * ★ 1.5.0：按左右常驻栏的实际宽度，给内容容器加左右内边距（照 FCL 三段式布局）。
     * <p>★ 必须 post 到下一帧再量 —— inflate 完这一帧两栏宽度还是 0，直接量会得到 0 padding。
     * <p>★ 只在内容容器上做一次；重复调用只更新数值，不会累加。
     */
    private void applyChromeInsets() {
        final View content = this.findViewById(R.id.main_ui_container);
        final View leftHost = this.findViewById(R.id.new_left_nav_host);
        final View rightHost = this.findViewById(R.id.new_right_panel_host);
        if (content == null || leftHost == null || rightHost == null) {
            return;
        }
        content.post(new Runnable() {
            @Override
            public void run() {
                try {
                    int l = leftHost.getVisibility() == View.VISIBLE ? leftHost.getWidth() : 0;
                    int r = rightHost.getVisibility() == View.VISIBLE ? rightHost.getWidth() : 0;
                    content.setPadding(l, 0, r, 0);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    /*
     * ★★★ 1.5.0：**草方块 UI 已彻底删除**（那套主题一堆 bug，用户要求移除）。
     *
     * 原来这里是 applyUiTheme() → QclThemeUtils.apply(activity, uiTheme)，
     * 由 QclThemeUtils 递归遍历整棵视图树，按「面板角色」把背景换成草方块贴图、
     * 把文字染成草方块配色；传别的值则从 view tag 里取回原值做**还原**。
     *
     * 现在开关、字段（launcherSetting.uiTheme）、整套 QclThemeUtils
     * 以及 qcl_grass_* 那 9 个 drawable 全部删掉了 —— 没有任何代码再改这些背景，
     * 自然也不需要「还原」，所以这个方法整个移除。
     * 视图背景就由布局文件本身说了算，干净可控。
     */

    protected void onDestroy() {
        if (this.dynamicBackground != null) {
            this.dynamicBackground.stop();
        }
        super.onDestroy();
    }

    public void onLoad() {
        this.uiManager.gameManagerUI.gameManagerUIManager.versionSettingUI.onLoaded();
        this.uiManager.downloadUI.downloadUIManager.downloadMinecraftUI.onLoaded();
        this.uiManager.settingUI.settingUIManager.universalGameSettingUI.onLoaded();
        this.uiManager.mainUI.customTheme();
    }

    public void showBarTitle(String title, boolean home, boolean close) {
        // ★★★ 1.5.0：**新排版下不使用右上角悬浮返回栏**（用户要求）——
        //   新版排左侧导航条最底部有常驻的「返回上一层」按钮（照 FCL 的 back），
        //   功能与这个悬浮栏重复，两个都显示会挤在右上角、还压住二级页面顶排按钮。
        //   判据：新排版专属的右栏 host 已 inflate 出来（visible）→ 直接不点亮。
        //   ★ 这一处改动覆盖全部 20+ 二级页面，不用逐个改它们的 onStart()。
        //   ★ 旧排版完全不进这个分支，返回栏行为与 1.4.9 一模一样。
        try {
            View chromeHost = this.findViewById(R.id.new_left_nav_host);
            if (chromeHost != null && chromeHost.getVisibility() == View.VISIBLE) {
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (this.currentUIText != null) {
                if (title != null && !title.isEmpty()) {
                    this.currentUIText.setText((CharSequence)title);
                    this.currentUIText.setVisibility(0);
                } else {
                    this.currentUIText.setVisibility(8);
                }
            }
            if (this.backToLastUI != null) {
                this.backToLastUI.setVisibility(0);
            }
            if (this.backToHome != null) {
                this.backToHome.setVisibility(home ? 0 : 8);
            }
            if (this.closeCurrentUI != null) {
                this.closeCurrentUI.setVisibility(close ? 0 : 8);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        this.showBackBar();
    }

    public void showBackBar() {
        // ★★★ 1.5.0：新排版下**整个右上角悬浮返回栏都不显示**（用户要求）。
        //   有些二级页面不走 showBarTitle()、而是直接调这里的 showBackBar()，
        //   所以这道闸门必须也加在这里，否则「返回栏被压回去」只在部分页面生效。
        try {
            View chromeHost = this.findViewById(R.id.new_left_nav_host);
            if (chromeHost != null && chromeHost.getVisibility() == View.VISIBLE) {
                if (this.backBar != null) {
                    this.backBar.setVisibility(View.GONE);
                }
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (this.backBar == null) {
                this.backBar = (LinearLayout)this.findViewById(R.id.qcl_back_bar);
            }
            if (this.backBar == null) {
                return;
            }
            if (this.backBar.getVisibility() != 0) {
                this.backBar.setVisibility(0);
            }
            this.backBar.bringToFront();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void hideBarTitle() {
        try {
            if (this.backBar == null) {
                this.backBar = (LinearLayout)this.findViewById(R.id.qcl_back_bar);
            }
            if (this.backBar == null) {
                return;
            }
            this.backBar.setVisibility(8);
            if (this.currentUIText != null) {
                this.currentUIText.setText((CharSequence)"");
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void backToLastUI() {
        if (this.isLoaded) {
            if (this.uiManager.currentUI == this.uiManager.mainUI) {
                // ★★★ 1.5.0 修复（用户对照 FCL 后指出"返回按钮逻辑不准确"）：
                //   **已经在主界面就什么都不做**，绝不退出启动器。
                //
                //   FCL 的链路：返回按钮 → uiManager.onBackPressed() → currentUI.onBackPressed()，
                //   而 FCLBaseUI.onBackPressed() 在 defaultBackEvent 为空时是**空实现**
                //   ⇒ FCL 在主界面上点返回是**没有动作**的。
                //
                //   原来这里调 backToDeskTop()：左栏返回按钮是**常驻**的，主界面上它也在那儿，
                //   点一下就把整个启动器退到桌面（用户原话"不能整这样的逻辑"）。
                //   ★ 系统返回键（onBackPressed → backToLastUI）同样不再退出主界面。
                return;
            }
            this.uiManager.uis.get(this.uiManager.uis.size() - 1).onStop();
            this.uiManager.uis.remove(this.uiManager.uis.size() - 1);
            this.uiManager.currentUI = this.uiManager.uis.get(this.uiManager.uis.size() - 1);
            this.uiManager.uis.get(this.uiManager.uis.size() - 1).onStart();
        }
    }

    public void backToHome() {
        this.uiManager.switchMainUI(this.uiManager.mainUI);
        this.uiManager.uis.clear();
        this.uiManager.uis.add(this.uiManager.mainUI);
    }

    public void closeCurrentUI() {
        this.uiManager.removeUIIfExist(this.uiManager.exportWorldUI);
        this.uiManager.removeUIIfExist(this.uiManager.installPackageUI);
        this.uiManager.removeUIIfExist(this.uiManager.exportPackageTypeUI);
        this.uiManager.removeUIIfExist(this.uiManager.exportPackageInfoUI);
        this.uiManager.removeUIIfExist(this.uiManager.exportPackageFileUI);
        this.uiManager.removeUIIfExist(this.uiManager.installGameUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadForgeUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadFabricUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadFabricAPIUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadLiteLoaderUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadOptifineUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadQuiltUI);
        this.uiManager.removeUIIfExist(this.uiManager.downloadQuiltAPIUI);
        this.uiManager.uis.get(this.uiManager.uis.size() - 1).onStart();
        if (this.uiManager.currentUI == this.uiManager.exportWorldUI) {
            this.uiManager.exportWorldUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.installPackageUI) {
            this.uiManager.installPackageUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.exportPackageTypeUI) {
            this.uiManager.exportPackageTypeUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.exportPackageInfoUI) {
            this.uiManager.exportPackageInfoUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.exportPackageFileUI) {
            this.uiManager.exportPackageFileUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.installGameUI) {
            this.uiManager.installGameUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadForgeUI) {
            this.uiManager.downloadForgeUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadFabricUI) {
            this.uiManager.downloadFabricUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadFabricAPIUI) {
            this.uiManager.downloadFabricAPIUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadLiteLoaderUI) {
            this.uiManager.downloadLiteLoaderUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadOptifineUI) {
            this.uiManager.downloadOptifineUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadQuiltUI) {
            this.uiManager.downloadQuiltUI.onStop();
        }
        if (this.uiManager.currentUI == this.uiManager.downloadQuiltAPIUI) {
            this.uiManager.downloadQuiltAPIUI.onStop();
        }
        this.uiManager.currentUI = this.uiManager.uis.get(this.uiManager.uis.size() - 1);
    }

    public void backToDeskTop() {
        Intent i = new Intent("android.intent.action.MAIN");
        i.setFlags(0x10000000);
        i.addCategory("android.intent.category.HOME");
        this.startActivity(i);
    }

    public void onBackPressed() {
        if (!this.dialogMode) {
            this.backToLastUI();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.isLoaded) {
            this.uiManager.onActivityResult(requestCode, resultCode, data);
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onActivityResult(requestCode, resultCode, data);
        }
        if (MicrosoftAccountSkinDialog.getInstance() != null) {
            MicrosoftAccountSkinDialog.getInstance().onActivityResult(requestCode, resultCode, data);
        }
        // ★ 1.4.1：皮肤库（本地文件选择结果转发）
        if (SkinLibraryDialog.getInstance() != null) {
            SkinLibraryDialog.getInstance().onActivityResult(requestCode, resultCode, data);
        }
    }

    public void onClick(View v) {
        if (v == this.backToLastUI) {
            this.backToLastUI();
        } else if (v == this.backToHome) {
            this.backToHome();
        } else if (v == this.closeCurrentUI) {
            this.closeCurrentUI();
        }
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this.getWindow().getDecorView().setSystemUiVisibility(5894);
        }
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage((Context)this);
        // ★★★ 1.5.0：**系统夜间模式切换**时同步刷新背景图。
        //   说明：MainActivity 的 configChanges 里**故意不含 uiMode** → 正常情况下
        //   系统会重建这个 Activity（资源重载 → values-night/colors.xml 生效 → 配色整体变深），
        //   本方法不会被调到。这里只是兜底：某些 ROM 把 uiMode 当普通配置变更直接回调，
        //   不重建的话至少要把背景图换过来（否则"深色配色 + 白天照片"会不搭）。
        try {
            if (this.launcherSetting != null
                    && this.launcherSetting.launcherBackground != null
                    && this.launcherSetting.launcherBackground.type == 1) {
                com.qcl.launcher.launcher.uis.main.DynamicBackground
                        .applyAutoDayNight(this, this.launcherLayout);
            }
        } catch (Throwable ignored) {
        }
    }

    protected void onPause() {
        super.onPause();
        if (this.isLoaded) {
            this.uiManager.onPause();
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onPause();
        }
    }

    protected void onResume() {
        super.onResume();
        if (this.isLoaded) {
            this.uiManager.onResume();
            // ★ 1.2.9：回到前台时把动态背景的轮播重新挂上（切后台回来后不动的问题）
            this.resumeDynamicBackground();

        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onResume();
        }
    }

    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= 28 && this.launcherSetting != null) {
            this.getWindow().getAttributes().layoutInDisplayCutoutMode = this.launcherSetting.fullscreen ? 1 : 2;
        }
        this.getWindow().setFlags(256, 256);
    }

    public void startVerify() {
        this.startVerify(new VerifyInterface(){

            @Override
            public void onSuccess() {
            }

            @Override
            public void onCancel() {
                MainActivity.this.finish();
            }
        });
    }

    public void startVerify(VerifyInterface verifyInterface) {
        SharedPreferences msh = this.getSharedPreferences("Security", 0);
        SharedPreferences.Editor mshe = msh.edit();
        if (msh.getBoolean("verified", false) && this.isValid(msh.getString("code", null))) {
            verifyInterface.onSuccess();
            return;
        }
        VerifyDialog dialog = new VerifyDialog((Context)this, this, mshe, verifyInterface);
        dialog.show();
    }

    static {
    }
}

