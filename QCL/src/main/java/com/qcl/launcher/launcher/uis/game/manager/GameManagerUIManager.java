package com.qcl.launcher.launcher.uis.game.manager;

import android.content.Context;
import android.content.Intent;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.game.download.DownloadUIManager;
import com.qcl.launcher.launcher.uis.game.download.right.DownloadModUI;
import com.qcl.launcher.launcher.uis.game.manager.right.AutoInstallUI;
import com.qcl.launcher.launcher.uis.game.manager.right.ModManagerUI;
import com.qcl.launcher.launcher.uis.game.manager.right.VersionSettingUI;
import com.qcl.launcher.launcher.uis.game.manager.right.WorldManagerUI;
import com.qcl.launcher.launcher.uis.tools.BaseUI;

public class GameManagerUIManager {

    public VersionSettingUI versionSettingUI;
    public ModManagerUI modManagerUI;
    public AutoInstallUI autoInstallUI;
    public WorldManagerUI worldManagerUI;

    public BaseUI[] gameManagerUIs;

    public GameManagerUIManager (Context context, MainActivity activity){
        versionSettingUI = new VersionSettingUI(context,activity);
        modManagerUI = new ModManagerUI(context,activity);
        autoInstallUI = new AutoInstallUI(context,activity);
        worldManagerUI = new WorldManagerUI(context,activity);

        versionSettingUI.onCreate();
        modManagerUI.onCreate();
        autoInstallUI.onCreate();
        worldManagerUI.onCreate();

        gameManagerUIs = new BaseUI[]{versionSettingUI,modManagerUI,autoInstallUI,worldManagerUI};
        switchGameManagerUIs(versionSettingUI);
    }

    public void switchGameManagerUIs(BaseUI ui){
        for (int i = 0;i < gameManagerUIs.length;i++){
            if (gameManagerUIs[i] == ui){
                gameManagerUIs[i].onStart();
            }
            else {
                gameManagerUIs[i].onStop();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        for (BaseUI ui : gameManagerUIs){
            ui.onActivityResult(requestCode,resultCode,data);
        }
    }

    public void onPause(){
        for (BaseUI ui : gameManagerUIs){
            ui.onPause();
        }
    }

    public void onResume(){
        for (BaseUI ui : gameManagerUIs){
            ui.onResume();
        }
    }
}
