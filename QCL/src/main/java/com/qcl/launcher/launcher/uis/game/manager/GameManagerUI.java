/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.AlertDialog$Builder
 *  android.content.Context
 *  android.content.Intent
 *  android.os.Bundle
 *  android.util.Log
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.LinearLayout
 *  androidx.appcompat.view.ContextThemeWrapper
 *  androidx.appcompat.widget.PopupMenu
 *  com.tungsten.filepicker.FileBrowser
 */
package com.qcl.launcher.launcher.uis.game.manager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.CopyVersionDialog;
import com.qcl.launcher.launcher.dialogs.RenameVersionDialog;
import com.qcl.launcher.launcher.download.AssetsUpdateDialog;
import com.qcl.launcher.launcher.launch.check.LaunchTools;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.game.manager.GameManagerUIManager;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.tungsten.filepicker.FileBrowser;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
public class GameManagerUI
extends BaseUI
implements View.OnClickListener {
    public LinearLayout gameManagerUI;
    public GameManagerUIManager gameManagerUIManager;
    public String versionPath;
    public String versionName;
    public LinearLayout startGameSetting;
    public LinearLayout startModManager;
    public LinearLayout startAutoInstall;
    public LinearLayout startWorldManager;
    private LinearLayout testGame;
    private LinearLayout browse;
    private LinearLayout manage;

    public GameManagerUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.gameManagerUI = (LinearLayout)this.activity.findViewById(R.id.ui_game_manager);
        this.startGameSetting = (LinearLayout)this.activity.findViewById(R.id.game_manager_game_setting);
        this.startModManager = (LinearLayout)this.activity.findViewById(R.id.game_manager_manage_mod);
        this.startAutoInstall = (LinearLayout)this.activity.findViewById(R.id.game_manager_auto_install);
        this.startWorldManager = (LinearLayout)this.activity.findViewById(R.id.game_manager_world);
        this.testGame = (LinearLayout)this.activity.findViewById(R.id.game_manager_test_game);
        this.browse = (LinearLayout)this.activity.findViewById(R.id.game_manager_browse);
        this.manage = (LinearLayout)this.activity.findViewById(R.id.game_manager_manage);
        this.startGameSetting.setOnClickListener((View.OnClickListener)this);
        this.startModManager.setOnClickListener((View.OnClickListener)this);
        this.startAutoInstall.setOnClickListener((View.OnClickListener)this);
        this.startWorldManager.setOnClickListener((View.OnClickListener)this);
        this.testGame.setOnClickListener((View.OnClickListener)this);
        this.browse.setOnClickListener((View.OnClickListener)this);
        this.manage.setOnClickListener((View.OnClickListener)this);
        this.gameManagerUIManager = new GameManagerUIManager(this.context, this.activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.game_manager_ui_title) + " - " + this.versionName, this.canGoBackToLast(), false);
        CustomAnimationUtils.showViewFromLeft((View)this.gameManagerUI, this.activity, this.context, true);
        this.init();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft((View)this.gameManagerUI, this.activity, this.context, true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.gameManagerUIManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.gameManagerUIManager.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.gameManagerUIManager.onResume();
    }

    @SuppressLint(value={"NonConstantResourceId"})
    public void onClick(View v) {
        PopupMenu menu2;
        ContextThemeWrapper wrapper;
        if (v == this.startGameSetting) {
            this.gameManagerUIManager.switchGameManagerUIs(this.gameManagerUIManager.versionSettingUI);
        }
        if (v == this.startModManager) {
            this.gameManagerUIManager.switchGameManagerUIs(this.gameManagerUIManager.modManagerUI);
        }
        if (v == this.startAutoInstall) {
            this.gameManagerUIManager.switchGameManagerUIs(this.gameManagerUIManager.autoInstallUI);
        }
        if (v == this.startWorldManager) {
            this.gameManagerUIManager.switchGameManagerUIs(this.gameManagerUIManager.worldManagerUI);
        }
        if (v == this.testGame) {
            this.testGame(this.versionName);
        }
        if (v == this.browse) {
            wrapper = new ContextThemeWrapper(this.context, R.style.MenuStyle);
            menu2 = new PopupMenu((Context)wrapper, (View)this.browse, 5);
            menu2.inflate(R.menu.browse_menu);
            menu2.setForceShowIcon(true);
            menu2.setOnMenuItemClickListener(item -> {
                String settingPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg";
                PrivateGameSetting privateGameSetting = new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile((String)settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile((String)settingPath).enable) ? GsonUtils.getPrivateGameSettingFromFile(settingPath) : this.activity.privateGameSetting;
                String gameDir = privateGameSetting.gameDirSetting.type == 0 ? this.activity.launcherSetting.gameFileDirectory : (privateGameSetting.gameDirSetting.type == 1 ? this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName : privateGameSetting.gameDirSetting.path);
                Intent intent = new Intent(this.context, FileBrowser.class);
                switch (item.getItemId()) {
                    case R.id.browse_game_dir: {
                        intent.putExtra("INITIAL_DIRECTORY", gameDir);
                        this.context.startActivity(intent);
                        return true;
                    }
                    case R.id.browse_mod_dir: {
                        FileUtils.createDirectory(gameDir + "/mods");
                        intent.putExtra("INITIAL_DIRECTORY", gameDir + "/mods");
                        this.context.startActivity(intent);
                        return true;
                    }
                    case R.id.browse_setting_dir: {
                        FileUtils.createDirectory(gameDir + "/config");
                        intent.putExtra("INITIAL_DIRECTORY", gameDir + "/config");
                        this.context.startActivity(intent);
                        return true;
                    }
                    case R.id.browse_resource_dir: {
                        FileUtils.createDirectory(gameDir + "/resourcepacks");
                        intent.putExtra("INITIAL_DIRECTORY", gameDir + "/resourcepacks");
                        this.context.startActivity(intent);
                        return true;
                    }
                    case R.id.browse_screenshots_dir: {
                        FileUtils.createDirectory(gameDir + "/screenshots");
                        intent.putExtra("INITIAL_DIRECTORY", gameDir + "/screenshots");
                        this.context.startActivity(intent);
                        return true;
                    }
                    case R.id.browse_save_dir: {
                        FileUtils.createDirectory(gameDir + "/saves");
                        intent.putExtra("INITIAL_DIRECTORY", gameDir + "/saves");
                        this.context.startActivity(intent);
                        return true;
                    }
                }
                return false;
            });
            menu2.show();
        }
        if (v == this.manage) {
            wrapper = new ContextThemeWrapper(this.context, R.style.MenuStyle);
            menu2 = new PopupMenu((Context)wrapper, (View)this.manage, 5);
            menu2.inflate(R.menu.manage_menu);
            menu2.setForceShowIcon(true);
            menu2.setOnMenuItemClickListener(item -> {
                String settingPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg";
                PrivateGameSetting privateGameSetting = new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile((String)settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile((String)settingPath).enable) ? GsonUtils.getPrivateGameSettingFromFile(settingPath) : this.activity.privateGameSetting;
                String gameDir = privateGameSetting.gameDirSetting.type == 0 ? this.activity.launcherSetting.gameFileDirectory : (privateGameSetting.gameDirSetting.type == 1 ? this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName : privateGameSetting.gameDirSetting.path);
                switch (item.getItemId()) {
                    case R.id.manage_test_game: {
                        this.testGame(this.versionName);
                        return true;
                    }
                    case R.id.manage_rename: {
                        RenameVersionDialog dialog = new RenameVersionDialog(this.context, this.versionName, name -> new Thread(() -> {
                            FileUtils.rename(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/" + this.versionName + ".jar", name + ".jar");
                            FileUtils.rename(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/" + this.versionName + ".json", name + ".json");
                            FileUtils.rename(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName, name);
                            if (this.versionName.equals(this.activity.publicGameSetting.currentVersion.substring(this.activity.publicGameSetting.currentVersion.lastIndexOf("/") + 1))) {
                                this.activity.publicGameSetting.currentVersion = this.activity.launcherSetting.gameFileDirectory + "/versions/" + name;
                                GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
                            }
                            this.versionName = name;
                            this.activity.runOnUiThread(() -> this.init());
                            this.activity.uiManager.versionListUI.refreshVersionList();
                        }).start());
                        dialog.show();
                        return true;
                    }
                    case R.id.manage_copy: {
                        CopyVersionDialog copyVersionDialog = new CopyVersionDialog(this.context, this.activity.uiManager.versionListUI.gameList, privateGameSetting, gameDir, this.activity.launcherSetting.gameFileDirectory + "/versions/", this.versionName, () -> new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start());
                        copyVersionDialog.show();
                        return true;
                    }
                    case R.id.manage_delete_version: {
                        AlertDialog.Builder deleteAlertBuilder = new AlertDialog.Builder(this.context);
                        deleteAlertBuilder.setTitle((CharSequence)this.context.getString(R.string.dialog_delete_version_title));
                        deleteAlertBuilder.setPositiveButton((CharSequence)this.context.getString(R.string.dialog_delete_version_positive), (dialogInterface, i) -> new Thread(() -> {
                            FileUtils.deleteDirectory(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName);
                            this.activity.uiManager.versionListUI.refreshVersionList();
                            if (this.activity.publicGameSetting.currentVersion.equals(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName) && this.activity.uiManager.versionListUI.gameList.size() > 0) {
                                this.activity.publicGameSetting.currentVersion = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.activity.uiManager.versionListUI.gameList.get((int)0).name;
                                GsonUtils.savePublicGameSetting(this.activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
                            }
                            this.activity.runOnUiThread(() -> {
                                this.activity.uiManager.uis = new ArrayList();
                                this.activity.uiManager.uis.add(this.activity.uiManager.mainUI);
                                this.activity.uiManager.uis.add(this.activity.uiManager.versionListUI);
                                this.activity.uiManager.gameManagerUI.onStop();
                                this.activity.uiManager.versionListUI.onStart();
                            });
                        }).start());
                        deleteAlertBuilder.setNegativeButton((CharSequence)this.context.getString(R.string.dialog_delete_version_negative), (dialogInterface, i) -> {});
                        if (privateGameSetting.gameDirSetting.type == 1) {
                            deleteAlertBuilder.setMessage((CharSequence)this.context.getString(R.string.dialog_delete_version_isolate_msg).replace("%s", this.versionName));
                        } else {
                            deleteAlertBuilder.setMessage((CharSequence)this.context.getString(R.string.dialog_delete_version_msg).replace("%s", this.versionName));
                        }
                        deleteAlertBuilder.create().show();
                        return true;
                    }
                    case R.id.manage_export_package: {
                        this.activity.uiManager.switchMainUI(this.activity.uiManager.exportPackageTypeUI);
                        return true;
                    }
                    case R.id.manage_update_assets: {
                        AssetsUpdateDialog assetsUpdateDialog = new AssetsUpdateDialog(this.context, this.activity, this.versionName);
                        assetsUpdateDialog.show();
                        return true;
                    }
                    case R.id.manage_delete_libs: {
                        new Thread(() -> FileUtils.deleteDirectory(this.activity.launcherSetting.gameFileDirectory + "/libraries")).start();
                        return true;
                    }
                    case R.id.manage_clear_logs: {
                        new Thread(() -> {
                            FileUtils.deleteDirectory(gameDir + "/logs");
                            FileUtils.deleteDirectory(gameDir + "/crash-reports");
                        }).start();
                        return true;
                    }
                }
                return false;
            });
            menu2.show();
        }
    }

    private void testGame(String name) {
        String settingPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + name + "/qcl.cfg";
        String finalPath = new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile((String)settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile((String)settingPath).enable) ? settingPath : AppManifest.SETTING_DIR + "/private_game_setting.json";
        Bundle bundle = new Bundle();
        bundle.putString("setting_path", finalPath);
        bundle.putBoolean("test", true);
        bundle.putString("version", this.activity.launcherSetting.gameFileDirectory + "/versions/" + name);
        LaunchTools.launch(this.context, this.activity, this.activity.launcherSetting.gameFileDirectory + "/versions/" + name, bundle);
    }

    private void init() {
        String newVersionPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName;
        this.gameManagerUIManager.modManagerUI.refresh(this.versionName);
        this.gameManagerUIManager.worldManagerUI.refresh(this.versionName);
        if (!newVersionPath.equals(this.versionPath)) {
            this.gameManagerUIManager.versionSettingUI.refresh(this.versionName);
            this.gameManagerUIManager.autoInstallUI.refresh(this.versionName);
            this.versionPath = newVersionPath;
            Log.e((String)"gameManager", (String)"refresh!");
        }
    }
}

