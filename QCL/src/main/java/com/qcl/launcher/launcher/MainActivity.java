package com.qcl.launcher.launcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.appthemeengine.ATE;
import com.afollestad.appthemeengine.Config;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.dialogs.VerifyDialog;
import com.qcl.launcher.launcher.dialogs.account.SkinPreviewDialog;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.launcher.uis.tools.UIManager;
import com.qcl.launcher.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI;
import com.qcl.launcher.update.UpdateChecker;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static {
        System.loadLibrary("security");
    }
    public native boolean isValid(String str);
    public static native void verify();
    public static native void verifyFunc();
    public native void launch(Intent intent);
    @SuppressLint("MissingSuperCall")
    @Override
    public native void onCreate(Bundle savedInstanceState);

    public LinearLayout launcherLayout;

    public boolean isLoaded = false;
    public boolean dialogMode = false;

    public LauncherSetting launcherSetting;
    public PublicGameSetting publicGameSetting;
    public PrivateGameSetting privateGameSetting;

    public UpdateChecker updateChecker;

    // 1.0.6：顶部标题栏已整体移除（不再有 appBar / appBarTitle / backToDesktop / closeApp）。
    // 但二级页面仍需返回入口 —— 改为右下/右上角的**悬浮返回栏**（qcl_back_bar），
    // 只在二级页面点亮，主界面隐藏。只保留返回相关的 4 个控件，不再有任何标题栏元素。
    public android.widget.LinearLayout backBar;
    public android.widget.ImageButton backToLastUI;
    public android.widget.TextView currentUIText;
    public android.widget.ImageButton backToHome;
    public android.widget.ImageButton closeCurrentUI;

    public RelativeLayout uiContainer;
    public UIManager uiManager;

    public Config exteriorConfig;

    public void init(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (getIntent().getExtras().getBoolean("fullscreen")) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        new Thread(() -> {
            AppManifest.initializeManifest(MainActivity.this);
            launcherSetting = InitializeSetting.initializeLauncherSetting();
            publicGameSetting = InitializeSetting.initializePublicGameSetting(MainActivity.this,MainActivity.this);
            privateGameSetting = InitializeSetting.initializePrivateGameSetting(MainActivity.this);

            runOnUiThread(() -> {
                updateChecker = new UpdateChecker(MainActivity.this,MainActivity.this);
            });

            DownloadUrlSource.getBalancedSource(MainActivity.this);

            loadingHandler.sendEmptyMessage(0);
        }).start();
    }

    @SuppressLint("HandlerLeak")
    public final Handler loadingHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0){
                if (!isLoaded) {
                    exteriorConfig = ATE.config(MainActivity.this, null);

                    // 顶部标题栏已在 1.0.6 移除（合成台图标 + 标题 + 返回/主页/关闭等窗口按钮），
                    // 六个主界面入口按钮整体上移到原位置。这里只接管**悬浮返回栏**：
                    // 它不含任何标题栏元素，仅在二级页面点亮，为下载/版本列表/账户/设置等页面提供返回入口。
                    backBar = findViewById(R.id.qcl_back_bar);
                    backToLastUI = findViewById(R.id.back_to_last_ui);
                    currentUIText = findViewById(R.id.text_current_ui);
                    backToHome = findViewById(R.id.back_to_home);
                    closeCurrentUI = findViewById(R.id.close_current_ui);
                    if (backToLastUI != null) backToLastUI.setOnClickListener(MainActivity.this);
                    if (backToHome != null) backToHome.setOnClickListener(MainActivity.this);
                    if (closeCurrentUI != null) closeCurrentUI.setOnClickListener(MainActivity.this);

                    uiContainer = findViewById(R.id.main_ui_container);
                    uiManager = new UIManager(MainActivity.this,MainActivity.this);

                    exteriorConfig.primaryColor(Color.parseColor(ExteriorSettingUI.getThemeColor(MainActivity.this,launcherSetting.launcherTheme)));
                    exteriorConfig.accentColor(Color.parseColor(ExteriorSettingUI.getThemeColor(MainActivity.this,launcherSetting.launcherTheme)));
                    exteriorConfig.apply(MainActivity.this);

                    // Development plans are available in About; no startup warning.

                    isLoaded = true;
                    onLoad();
                    ExteriorSettingUI.applyPanelTint(MainActivity.this, getWindow().getDecorView(), ExteriorSettingUI.getPanelColor(MainActivity.this, launcherSetting.panelColor));
                    startDynamicBackgroundIfNeeded();
                    // 1.0.5：套用 UI 风格（默认 / 草方块）
                    applyUiTheme();
                }
            }
        }
    };

    /** 1.0.5 动态背景：默认背景（launcherBackground.type == 0）时启动 4 图 10 秒轮换 */
    private com.qcl.launcher.launcher.uis.main.DynamicBackground dynamicBackground;

    private void startDynamicBackgroundIfNeeded() {
        try {
            if (launcherSetting.launcherBackground.type != 0) {
                // 「经典」「自定义」「在线」背景不参与轮换
                return;
            }
            if (dynamicBackground == null) {
                dynamicBackground = new com.qcl.launcher.launcher.uis.main.DynamicBackground(this, launcherLayout);
            }
            dynamicBackground.start();
        } catch (Throwable ignored) {
        }
    }

    /** 供外观设置切换背景类型时调用：切回默认则重启轮换，切走则停掉 */
    public void refreshDynamicBackground() {
        try {
            if (launcherSetting.launcherBackground.type == 0) {
                startDynamicBackgroundIfNeeded();
            }
            else if (dynamicBackground != null) {
                dynamicBackground.stop();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 1.0.5：套用 UI 风格。草方块 UI 下：
     * 1) 面板/按钮/顶底栏换成真实 Alpha 草方块材质；
     * 2) 禁用所有半透明（顶栏、底栏、面板全部不透明实色）。
     * ⚠️ 不替换启动器整体背景图 —— 背景仍由 4 张动态壁纸负责。
     */
    public void applyUiTheme() {
        try {
            com.qcl.launcher.launcher.uis.tools.QclThemeUtils.apply(this, launcherSetting.uiTheme);
            // 1.0.6：顶部标题栏已移除，不再需要给 appBar 单独上色。
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        if (dynamicBackground != null) {
            dynamicBackground.stop();
        }
        super.onDestroy();
    }

    public void onLoad() {
        uiManager.gameManagerUI.gameManagerUIManager.versionSettingUI.onLoaded();
        uiManager.downloadUI.downloadUIManager.downloadMinecraftUI.onLoaded();
        uiManager.settingUI.settingUIManager.universalGameSettingUI.onLoaded();
        uiManager.mainUI.customTheme();
    }

    /** 顶部标题栏已移除（1.0.6），方法保留为空实现以免调用方报错。 */
    /**
     * 进入二级页面时点亮悬浮返回栏（1.0.6 重写）。
     *
     * <p>顶部标题栏已整体移除，所以不再有「标题左滑出去」的动画；
     * 改为整个返回栏（backBar）淡入/淡出，内部按参数决定「回主页」和「关闭」按钮是否显示。
     *
     * @param title 当前页面标题，null/空则不显示文字
     * @param home  是否显示「回到主界面」按钮
     * @param close 是否显示「关闭当前页面」按钮
     */
    public void showBarTitle(String title, boolean home, boolean close) {
        if (!isLoaded || backBar == null) return;
        if (title != null && !title.isEmpty()) {
            currentUIText.setText(title);
            currentUIText.setVisibility(View.VISIBLE);
        } else {
            currentUIText.setVisibility(View.GONE);
        }
        backToLastUI.setVisibility(View.VISIBLE);
        backToHome.setVisibility(home ? View.VISIBLE : View.GONE);
        closeCurrentUI.setVisibility(close ? View.VISIBLE : View.GONE);
        CustomAnimationUtils.showViewFromRight(backBar, this, this, true);
    }

    /** 回到主界面时隐藏悬浮返回栏（1.0.6 重写）。 */
    public void hideBarTitle() {
        if (!isLoaded || backBar == null) return;
        CustomAnimationUtils.hideViewToLeft(backBar, this, this, true);
        currentUIText.setText("");
    }

    public void backToLastUI() {
        if (isLoaded){
            if (uiManager.currentUI == uiManager.mainUI){
                backToDeskTop();
            }
            else {
                uiManager.uis.get(uiManager.uis.size() - 1).onStop();
                uiManager.uis.remove(uiManager.uis.size() - 1);
                uiManager.currentUI = uiManager.uis.get(uiManager.uis.size() - 1);
                uiManager.uis.get(uiManager.uis.size() - 1).onStart();
            }
        }
    }

    public void backToHome() {
        uiManager.switchMainUI(uiManager.mainUI);
        uiManager.uis.clear();
        uiManager.uis.add(uiManager.mainUI);
    }

    public void closeCurrentUI() {
        uiManager.removeUIIfExist(uiManager.exportWorldUI);
        uiManager.removeUIIfExist(uiManager.installPackageUI);
        uiManager.removeUIIfExist(uiManager.exportPackageTypeUI);
        uiManager.removeUIIfExist(uiManager.exportPackageInfoUI);
        uiManager.removeUIIfExist(uiManager.exportPackageFileUI);
        uiManager.removeUIIfExist(uiManager.installGameUI);
        uiManager.removeUIIfExist(uiManager.downloadForgeUI);
        uiManager.removeUIIfExist(uiManager.downloadFabricUI);
        uiManager.removeUIIfExist(uiManager.downloadFabricAPIUI);
        uiManager.removeUIIfExist(uiManager.downloadLiteLoaderUI);
        uiManager.removeUIIfExist(uiManager.downloadOptifineUI);
        uiManager.removeUIIfExist(uiManager.downloadQuiltUI);
        uiManager.removeUIIfExist(uiManager.downloadQuiltAPIUI);
        uiManager.uis.get(uiManager.uis.size() - 1).onStart();
        if (uiManager.currentUI == uiManager.exportWorldUI){
            uiManager.exportWorldUI.onStop();
        }
        if (uiManager.currentUI == uiManager.installPackageUI){
            uiManager.installPackageUI.onStop();
        }
        if (uiManager.currentUI == uiManager.exportPackageTypeUI){
            uiManager.exportPackageTypeUI.onStop();
        }
        if (uiManager.currentUI == uiManager.exportPackageInfoUI){
            uiManager.exportPackageInfoUI.onStop();
        }
        if (uiManager.currentUI == uiManager.exportPackageFileUI){
            uiManager.exportPackageFileUI.onStop();
        }
        if (uiManager.currentUI == uiManager.installGameUI){
            uiManager.installGameUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadForgeUI){
            uiManager.downloadForgeUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadFabricUI){
            uiManager.downloadFabricUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadFabricAPIUI){
            uiManager.downloadFabricAPIUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadLiteLoaderUI){
            uiManager.downloadLiteLoaderUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadOptifineUI){
            uiManager.downloadOptifineUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadQuiltUI){
            uiManager.downloadQuiltUI.onStop();
        }
        if (uiManager.currentUI == uiManager.downloadQuiltAPIUI){
            uiManager.downloadQuiltAPIUI.onStop();
        }
        uiManager.currentUI = uiManager.uis.get(uiManager.uis.size() - 1);
    }

    public void backToDeskTop() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        if (!dialogMode){
            backToLastUI();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (isLoaded){
            uiManager.onActivityResult(requestCode,resultCode,data);
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onActivityResult(requestCode,resultCode,data);
        }
    }

    @Override
    public void onClick(View v) {
        // 1.0.6：只保留悬浮返回栏的 3 个按钮。
        // 原顶部标题栏的「回桌面 / 退出应用」等按钮已随标题栏一起移除，
        // 系统返回键仍由 onBackPressed() -> backToLastUI() 接管。
        if (v == backToLastUI) {
            backToLastUI();
        }
        else if (v == backToHome) {
            backToHome();
        }
        else if (v == closeCurrentUI) {
            closeCurrentUI();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLanguage(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLanguage(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isLoaded){
            uiManager.onPause();
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isLoaded){
            uiManager.onResume();
        }
        if (SkinPreviewDialog.getInstance() != null) {
            SkinPreviewDialog.getInstance().onResume();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && launcherSetting != null) {
            if (launcherSetting.fullscreen) {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            } else {
                getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
            }
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
    }

    public void startVerify() {
        startVerify(new VerifyInterface() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onCancel() {
                finish();
            }
        });
    }

    public void startVerify(VerifyInterface verifyInterface) {
        SharedPreferences msh = getSharedPreferences("Security", Context.MODE_PRIVATE);
        SharedPreferences.Editor mshe = msh.edit();
        if (msh.getBoolean("verified",false) && isValid(msh.getString("code",null))) {
            verifyInterface.onSuccess();
            return;
        }
        VerifyDialog dialog = new VerifyDialog(this, this, mshe, verifyInterface);
        dialog.show();
    }

}