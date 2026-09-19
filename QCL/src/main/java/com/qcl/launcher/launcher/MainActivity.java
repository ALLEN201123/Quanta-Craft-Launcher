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
import android.view.View;
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
import com.qcl.launcher.launcher.dialogs.account.SkinPreviewDialog;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.launcher.uis.main.DynamicBackground;
import com.qcl.launcher.launcher.uis.tools.QclThemeUtils;
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
                MainActivity.this.uiManager = new UIManager((Context)MainActivity.this, MainActivity.this);
                MainActivity.this.exteriorConfig.primaryColor(ExteriorSettingUI.parseThemeColorSafe((Context)MainActivity.this, MainActivity.this.launcherSetting.launcherTheme));
                MainActivity.this.exteriorConfig.accentColor(ExteriorSettingUI.parseThemeColorSafe((Context)MainActivity.this, MainActivity.this.launcherSetting.launcherTheme));
                MainActivity.this.exteriorConfig.apply((Activity)MainActivity.this);
                MainActivity.this.isLoaded = true;
                MainActivity.this.onLoad();
                ExteriorSettingUI.applyPanelTint((Context)MainActivity.this, MainActivity.this.getWindow().getDecorView(), ExteriorSettingUI.getPanelColor((Context)MainActivity.this, MainActivity.this.launcherSetting.panelColor));
                MainActivity.this.startDynamicBackgroundIfNeeded();
                MainActivity.this.applyUiTheme();
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
            if (this.launcherSetting.launcherBackground.type != 0) {
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

    public void refreshDynamicBackground() {
        try {
            if (this.launcherSetting.launcherBackground.type == 0) {
                this.startDynamicBackgroundIfNeeded();
            } else if (this.dynamicBackground != null) {
                this.dynamicBackground.stop();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public void applyUiTheme() {
        try {
            QclThemeUtils.apply((Activity)this, this.launcherSetting.uiTheme);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

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
                this.backToDeskTop();
            } else {
                this.uiManager.uis.get(this.uiManager.uis.size() - 1).onStop();
                this.uiManager.uis.remove(this.uiManager.uis.size() - 1);
                this.uiManager.currentUI = this.uiManager.uis.get(this.uiManager.uis.size() - 1);
                this.uiManager.uis.get(this.uiManager.uis.size() - 1).onStart();
            }
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

