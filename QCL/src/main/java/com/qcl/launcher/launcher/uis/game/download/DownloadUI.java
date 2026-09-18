package com.qcl.launcher.launcher.uis.game.download;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadUI extends BaseUI implements View.OnClickListener {
    public LinearLayout downloadUI;
    public DownloadUIManager downloadUIManager;
    public LinearLayout startDownloadGameUI;
    public LinearLayout startDownloadModUI;
    public LinearLayout startDownloadPackageUI;
    public LinearLayout startDownloadResourcePackUI;
    public LinearLayout startDownloadShaderUI;
    public LinearLayout startDownloadWorldUI;

    public DownloadUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadUI = (LinearLayout) this.activity.findViewById(R.id.ui_download);
        this.startDownloadGameUI = (LinearLayout) this.activity.findViewById(R.id.start_download_game_ui);
        this.startDownloadModUI = (LinearLayout) this.activity.findViewById(R.id.start_download_mod_ui);
        this.startDownloadPackageUI = (LinearLayout) this.activity.findViewById(R.id.start_download_package_ui);
        this.startDownloadResourcePackUI = (LinearLayout) this.activity.findViewById(R.id.start_download_resource_pack_ui);
        this.startDownloadWorldUI = (LinearLayout) this.activity.findViewById(R.id.start_download_world_ui);
        this.startDownloadShaderUI = (LinearLayout) this.activity.findViewById(R.id.start_download_shader_ui);
        this.startDownloadGameUI.setOnClickListener(this);
        this.startDownloadModUI.setOnClickListener(this);
        this.startDownloadPackageUI.setOnClickListener(this);
        this.startDownloadResourcePackUI.setOnClickListener(this);
        this.startDownloadWorldUI.setOnClickListener(this);
        this.startDownloadShaderUI.setOnClickListener(this);
        this.downloadUIManager = new DownloadUIManager(this.context, this.activity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.download_ui_title), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.downloadUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadUI, this.activity, this.context, true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        this.downloadUIManager.onActivityResult(i, i2, intent);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onPause() {
        super.onPause();
        this.downloadUIManager.onPause();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onResume() {
        super.onResume();
        this.downloadUIManager.onResume();
    }

    private void init() {
        this.downloadUIManager.downloadModUI.refreshGameList();
        this.downloadUIManager.downloadResourcePackUI.refreshGameList();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.startDownloadGameUI) {
            DownloadUIManager downloadUIManager = this.downloadUIManager;
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadMinecraftUI);
        }
        if (view == this.startDownloadModUI) {
            DownloadUIManager downloadUIManager2 = this.downloadUIManager;
            downloadUIManager2.switchDownloadUI(downloadUIManager2.downloadModUI);
        }
        if (view == this.startDownloadPackageUI) {
            DownloadUIManager downloadUIManager3 = this.downloadUIManager;
            downloadUIManager3.switchDownloadUI(downloadUIManager3.downloadPackageUI);
        }
        if (view == this.startDownloadResourcePackUI) {
            DownloadUIManager downloadUIManager4 = this.downloadUIManager;
            downloadUIManager4.switchDownloadUI(downloadUIManager4.downloadResourcePackUI);
        }
        if (view == this.startDownloadShaderUI) {
            DownloadUIManager downloadUIManager5 = this.downloadUIManager;
            downloadUIManager5.switchDownloadUI(downloadUIManager5.downloadShaderUI);
        }
        if (view == this.startDownloadWorldUI) {
            DownloadUIManager downloadUIManager6 = this.downloadUIManager;
            downloadUIManager6.switchDownloadUI(downloadUIManager6.downloadWorldUI);
        }
    }
}
