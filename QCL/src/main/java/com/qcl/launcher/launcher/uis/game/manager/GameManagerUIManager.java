package com.qcl.launcher.launcher.uis.game.manager;

import android.content.Context;
import android.content.Intent;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.game.manager.right.AutoInstallUI;
import com.qcl.launcher.launcher.uis.game.manager.right.ModManagerUI;
import com.qcl.launcher.launcher.uis.game.manager.right.VersionSettingUI;
import com.qcl.launcher.launcher.uis.game.manager.right.WorldManagerUI;
import com.qcl.launcher.launcher.uis.tools.BaseUI;

/* loaded from: classes2.dex */
public class GameManagerUIManager {
    public AutoInstallUI autoInstallUI;
    public BaseUI[] gameManagerUIs;
    public ModManagerUI modManagerUI;
    public VersionSettingUI versionSettingUI;
    public WorldManagerUI worldManagerUI;

    public GameManagerUIManager(Context context, MainActivity mainActivity) {
        this.versionSettingUI = new VersionSettingUI(context, mainActivity);
        this.modManagerUI = new ModManagerUI(context, mainActivity);
        this.autoInstallUI = new AutoInstallUI(context, mainActivity);
        this.worldManagerUI = new WorldManagerUI(context, mainActivity);
        this.versionSettingUI.onCreate();
        this.modManagerUI.onCreate();
        this.autoInstallUI.onCreate();
        this.worldManagerUI.onCreate();
        VersionSettingUI versionSettingUI = this.versionSettingUI;
        this.gameManagerUIs = new BaseUI[]{versionSettingUI, this.modManagerUI, this.autoInstallUI, this.worldManagerUI};
        switchGameManagerUIs(versionSettingUI);
    }

    public void switchGameManagerUIs(BaseUI baseUI) {
        int i = 0;
        while (true) {
            BaseUI[] baseUIArr = this.gameManagerUIs;
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
        for (BaseUI baseUI : this.gameManagerUIs) {
            baseUI.onActivityResult(i, i2, intent);
        }
    }

    public void onPause() {
        for (BaseUI baseUI : this.gameManagerUIs) {
            baseUI.onPause();
        }
    }

    public void onResume() {
        for (BaseUI baseUI : this.gameManagerUIs) {
            baseUI.onResume();
        }
    }
}
