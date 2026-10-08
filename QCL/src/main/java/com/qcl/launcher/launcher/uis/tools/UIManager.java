package com.qcl.launcher.launcher.uis.tools;

import android.content.Context;
import android.content.Intent;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.account.AccountUI;
import com.qcl.launcher.launcher.uis.game.download.DownloadUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricAPIUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadForgeUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadNeoForgeUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadLiteLoaderUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadOptifineUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltAPIUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.InstallGameUI;
import com.qcl.launcher.launcher.uis.game.download.right.resource.BaseDownloadUI;
import com.qcl.launcher.launcher.uis.game.manager.GameManagerUI;
import com.qcl.launcher.launcher.uis.game.manager.universal.ExportWorldUI;
import com.qcl.launcher.launcher.uis.game.manager.universal.ModUpdateUI;
import com.qcl.launcher.launcher.uis.game.manager.universal.PackMcManagerUI;
import com.qcl.launcher.launcher.uis.game.version.VersionListUI;
import com.qcl.launcher.launcher.uis.game.version.universal.AddGameDirectoryUI;
import com.qcl.launcher.launcher.uis.game.version.universal.ExportPackageFileUI;
import com.qcl.launcher.launcher.uis.game.version.universal.ExportPackageInfoUI;
import com.qcl.launcher.launcher.uis.game.version.universal.ExportPackageTypeUI;
import com.qcl.launcher.launcher.uis.game.version.universal.InstallPackageUI;
// ★ 1.4.1 新增页面（自 1.4.0 朋友源码包合并）
import com.qcl.launcher.launcher.uis.lab.LabUI;
import com.qcl.launcher.launcher.uis.main.MainUI;
// ★ 1.5.0 新增：多人联机二级页面（照 FCL 的 MultiplayerUI 重做）
import com.qcl.launcher.launcher.uis.multiplayer.MultiplayerUI;
import com.qcl.launcher.launcher.uis.universal.setting.SettingUI;

import java.util.ArrayList;

public class UIManager {

    public MainUI mainUI;
    public AccountUI accountUI;
    public GameManagerUI gameManagerUI;
    public VersionListUI versionListUI;
    public DownloadUI downloadUI;
    public SettingUI settingUI;

    // ★ 1.4.1 新增：实验室（★ 1.5.0：原「大厅」LobbyUI 已整体移除，用户说没实际用处）
    public LabUI labUI;

    // ★ 1.5.0 新增：多人联机页（不再是一个小弹窗）
    public MultiplayerUI multiplayerUI;

    public ModUpdateUI modUpdateUI;
    public PackMcManagerUI packMcManagerUI;
    public ExportWorldUI exportWorldUI;
    public AddGameDirectoryUI addGameDirectoryUI;
    public InstallPackageUI installPackageUI;
    public ExportPackageTypeUI exportPackageTypeUI;
    public ExportPackageInfoUI exportPackageInfoUI;
    public ExportPackageFileUI exportPackageFileUI;

    public InstallGameUI installGameUI;
    public DownloadForgeUI downloadForgeUI;
    public DownloadNeoForgeUI downloadNeoForgeUI;
    public DownloadFabricUI downloadFabricUI;
    public DownloadFabricAPIUI downloadFabricAPIUI;
    public DownloadLiteLoaderUI downloadLiteLoaderUI;
    public DownloadOptifineUI downloadOptifineUI;
    public DownloadQuiltUI downloadQuiltUI;
    public DownloadQuiltAPIUI downloadQuiltAPIUI;

    public BaseUI[] mainUIs;
    public ArrayList<BaseUI> uis;
    public BaseUI currentUI;

    public UIManager (Context context, MainActivity activity){
        mainUI = new MainUI(context, activity);
        accountUI = new AccountUI(context, activity);
        gameManagerUI = new GameManagerUI(context, activity);
        versionListUI = new VersionListUI(context, activity);
        downloadUI = new DownloadUI(context, activity);
        settingUI = new SettingUI(context, activity);

        // ★ 1.4.1 新增页面（★ 1.5.0：「大厅」已移除）
        labUI = new LabUI(context, activity);

        // ★ 1.5.0 新增页面
        multiplayerUI = new MultiplayerUI(context, activity);

        modUpdateUI = new ModUpdateUI(context, activity);
        packMcManagerUI = new PackMcManagerUI(context, activity);
        exportWorldUI = new ExportWorldUI(context, activity);
        addGameDirectoryUI = new AddGameDirectoryUI(context, activity);
        installPackageUI = new InstallPackageUI(context, activity);
        exportPackageTypeUI = new ExportPackageTypeUI(context, activity);
        exportPackageInfoUI = new ExportPackageInfoUI(context, activity);
        exportPackageFileUI = new ExportPackageFileUI(context, activity);

        installGameUI = new InstallGameUI(context, activity);
        downloadForgeUI = new DownloadForgeUI(context, activity);
        downloadNeoForgeUI = new DownloadNeoForgeUI(context, activity);
        downloadFabricUI = new DownloadFabricUI(context, activity);
        downloadFabricAPIUI = new DownloadFabricAPIUI(context, activity);
        downloadLiteLoaderUI = new DownloadLiteLoaderUI(context, activity);
        downloadOptifineUI = new DownloadOptifineUI(context, activity);
        downloadQuiltUI = new DownloadQuiltUI(context, activity);
        downloadQuiltAPIUI = new DownloadQuiltAPIUI(context, activity);

        mainUI.onCreate();
        accountUI.onCreate();
        gameManagerUI.onCreate();
        versionListUI.onCreate();
        downloadUI.onCreate();
        settingUI.onCreate();

        // ★ 1.4.1 新增页面（★ 1.5.0：「大厅」已移除）
        labUI.onCreate();

        // ★ 1.5.0 新增页面
        multiplayerUI.onCreate();

        modUpdateUI.onCreate();
        packMcManagerUI.onCreate();
        exportWorldUI.onCreate();
        addGameDirectoryUI.onCreate();
        installPackageUI.onCreate();
        exportPackageTypeUI.onCreate();
        exportPackageInfoUI.onCreate();
        exportPackageFileUI.onCreate();

        installGameUI.onCreate();
        downloadForgeUI.onCreate();
        downloadNeoForgeUI.onCreate();
        downloadFabricUI.onCreate();
        downloadFabricAPIUI.onCreate();
        downloadLiteLoaderUI.onCreate();
        downloadOptifineUI.onCreate();
        downloadQuiltUI.onCreate();
        downloadQuiltAPIUI.onCreate();

        mainUIs = new BaseUI[] {
                mainUI,
                modUpdateUI,
                packMcManagerUI,
                exportWorldUI,
                addGameDirectoryUI,
                installPackageUI,
                exportPackageTypeUI,
                exportPackageInfoUI,
                exportPackageFileUI,
                accountUI,
                gameManagerUI,
                versionListUI,
                downloadUI,
                settingUI,
                labUI,
                multiplayerUI,
                installGameUI,
                downloadForgeUI,
                downloadNeoForgeUI,
                downloadFabricUI,
                downloadLiteLoaderUI,
                downloadOptifineUI,
                downloadFabricAPIUI,
                downloadQuiltUI,
                downloadQuiltAPIUI
        };
        uis = new ArrayList<>();
        switchMainUI(mainUI);
    }

    public void switchMainUI(BaseUI ui) {
        // ★★★ 1.5.0 修复（用户实测）：**重复点击同一个按钮 → 页面"弹起又关闭"**。
        //
        //   根因在下面的执行顺序：
        //     uis.add(ui);        ← 同一个对象**再次**入栈 ⇒ 栈变成 [main, vList, vList]
        //     ui.onStart();       ← 先把新页面弹起（setVisibility(VISIBLE)）
        //     uis.get(size-2).onStop();  ← 再"关掉上一个" —— 而倒数第二个**就是它自己**！
        //   于是点第二次的表现就是：刚弹起来 → 紧接着被自己关掉（视觉上"抬起又关闭"）。
        //   栈还只增不减，来回点几下就膨胀成一串重复项。
        //
        //   ⇒ 目标页面**已经**是当前的，就什么都不做：按钮保持它自己那个状态（用户原话：
        //     "不应该是点了一个同一个按钮，保持那一个按钮的状态吗"）。
        if (ui != null && ui == currentUI) {
            System.out.println("-------------------------------------------switch to same ui, keep state (no-op)");
            return;
        }
        currentUI = ui;
        uis.add(ui);
        ui.onStart();
        if (uis.size() > 1){
            // 1.0.6：加 try/catch，避免被切出去的那个页面在 onStop 里抛异常时
            // 连带打断本次切换（表现为主界面之后黑屏 / 返回栏不显示）。
            try {
                uis.get(uis.size() - 2).onStop();
            } catch (Throwable ignored) {
            }
        }
        System.out.println("-----------------------------------------------------------------------------------------------------------------------------switch to new ui");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (BaseUI ui : mainUIs) {
            ui.onActivityResult(requestCode,resultCode,data);
        }
        for (BaseUI ui : uis) {
            if (ui instanceof BaseDownloadUI) {
                ui.onActivityResult(requestCode,resultCode,data);
            }
        }
    }

    public void onPause() {
        for (BaseUI ui : mainUIs) {
            ui.onPause();
        }
        for (BaseUI ui : uis) {
            if (ui instanceof BaseDownloadUI) {
                ui.onPause();
            }
        }
    }

    public void onResume() {
        for (BaseUI ui : mainUIs) {
            ui.onResume();
        }
        for (BaseUI ui : uis) {
            if (ui instanceof BaseDownloadUI) {
                ui.onResume();
            }
        }
    }

    public void removeUIIfExist(BaseUI ui) {
        for (int i = 0;i < uis.size();i++){
            if (uis.get(i) == ui){
                uis.remove(i);
            }
        }
    }
}
