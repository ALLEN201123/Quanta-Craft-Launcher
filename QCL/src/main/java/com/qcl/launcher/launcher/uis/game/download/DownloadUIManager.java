package com.qcl.launcher.launcher.uis.game.download;

import android.content.Context;
import android.content.Intent;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadMinecraftUI;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadModUI;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadPackageUI;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadResourcePackUI;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadShaderUI;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadWorldUI;
import com.qcl.launcher.launcher.uis.tools.BaseUI;

/* loaded from: classes2.dex */
public class DownloadUIManager {
    public DownloadMinecraftUI downloadMinecraftUI;
    public DownloadModUI downloadModUI;
    public DownloadPackageUI downloadPackageUI;
    public DownloadResourcePackUI downloadResourcePackUI;
    public DownloadShaderUI downloadShaderUI;
    public BaseUI[] downloadUIs;
    public DownloadWorldUI downloadWorldUI;

    public DownloadUIManager(Context context, MainActivity mainActivity) {
        this.downloadMinecraftUI = new DownloadMinecraftUI(context, mainActivity);
        this.downloadModUI = new DownloadModUI(context, mainActivity);
        this.downloadPackageUI = new DownloadPackageUI(context, mainActivity);
        this.downloadResourcePackUI = new DownloadResourcePackUI(context, mainActivity);
        this.downloadWorldUI = new DownloadWorldUI(context, mainActivity);
        this.downloadShaderUI = new DownloadShaderUI(context, mainActivity);
        this.downloadMinecraftUI.onCreate();
        this.downloadModUI.onCreate();
        this.downloadPackageUI.onCreate();
        this.downloadResourcePackUI.onCreate();
        this.downloadWorldUI.onCreate();
        this.downloadShaderUI.onCreate();
        DownloadMinecraftUI downloadMinecraftUI = this.downloadMinecraftUI;
        this.downloadUIs = new BaseUI[]{downloadMinecraftUI, this.downloadModUI, this.downloadPackageUI, this.downloadResourcePackUI, this.downloadWorldUI, this.downloadShaderUI};
        switchDownloadUI(downloadMinecraftUI);
    }

    public void switchDownloadUI(BaseUI baseUI) {
        int i = 0;
        while (true) {
            BaseUI[] baseUIArr = this.downloadUIs;
            if (i >= baseUIArr.length) {
                return;
            }
            if (baseUIArr[i] == baseUI) {
                baseUIArr[i].onStart();
            } else {
                baseUIArr[i].onStop();
            }
            i++;
        }
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        for (BaseUI baseUI : this.downloadUIs) {
            baseUI.onActivityResult(i, i2, intent);
        }
    }

    public void onPause() {
        for (BaseUI baseUI : this.downloadUIs) {
            baseUI.onPause();
        }
    }

    public void onResume() {
        for (BaseUI baseUI : this.downloadUIs) {
            baseUI.onResume();
        }
    }
}
