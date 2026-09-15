package com.qcl.launcher.launcher.uis.game.download;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

public class DownloadUI extends BaseUI implements View.OnClickListener {

    public DownloadUIManager downloadUIManager;

    public LinearLayout downloadUI;

    public LinearLayout startDownloadGameUI;
    public LinearLayout startDownloadModUI;
    public LinearLayout startDownloadPackageUI;
    public LinearLayout startDownloadResourcePackUI;
    public LinearLayout startDownloadWorldUI;
    public LinearLayout startDownloadShaderUI;

    public DownloadUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadUI = activity.findViewById(R.id.ui_download);

        startDownloadGameUI = activity.findViewById(R.id.start_download_game_ui);
        startDownloadModUI = activity.findViewById(R.id.start_download_mod_ui);
        startDownloadPackageUI = activity.findViewById(R.id.start_download_package_ui);
        startDownloadResourcePackUI = activity.findViewById(R.id.start_download_resource_pack_ui);
        startDownloadWorldUI = activity.findViewById(R.id.start_download_world_ui);
        startDownloadShaderUI = activity.findViewById(R.id.start_download_shader_ui);

        startDownloadGameUI.setOnClickListener(this);
        startDownloadModUI.setOnClickListener(this);
        startDownloadPackageUI.setOnClickListener(this);
        startDownloadResourcePackUI.setOnClickListener(this);
        startDownloadWorldUI.setOnClickListener(this);
        startDownloadShaderUI.setOnClickListener(this);

        downloadUIManager = new DownloadUIManager(context,activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.showBarTitle(context.getResources().getString(R.string.download_ui_title),canGoBackToLast(),true);
        CustomAnimationUtils.showViewFromLeft(downloadUI,activity,context,true);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(downloadUI,activity,context,true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        downloadUIManager.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    public void onPause() {
        super.onPause();
        downloadUIManager.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        downloadUIManager.onResume();
    }

    private void init(){
        downloadUIManager.downloadModUI.refreshGameList();
        downloadUIManager.downloadResourcePackUI.refreshGameList();
    }

    @Override
    public void onClick(View v) {
        if (v == startDownloadGameUI){
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadMinecraftUI);
        }
        if (v == startDownloadModUI){
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadModUI);
        }
        if (v == startDownloadPackageUI){
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadPackageUI);
        }
        if (v == startDownloadResourcePackUI){
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadResourcePackUI);
        }
        if (v == startDownloadShaderUI){
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadShaderUI);
        }
        if (v == startDownloadWorldUI){
            downloadUIManager.switchDownloadUI(downloadUIManager.downloadWorldUI);
        }
    }
}
