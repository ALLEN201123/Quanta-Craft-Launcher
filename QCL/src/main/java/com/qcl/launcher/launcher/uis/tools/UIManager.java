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
import com.qcl.launcher.launcher.uis.jar.JarLauncherUI;
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
    /** ★ 1.5.2：JAR 启动器独立页面。 */
    public JarLauncherUI jarLauncherUI;

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
        jarLauncherUI = new JarLauncherUI(context, activity);
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

        jarLauncherUI.onCreate();
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
                jarLauncherUI,
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
            // ★★★★★ 2026-10-16 **修「页面卡在隐藏状态 → 全空白」**。
            //
            //   老实现这里**直接 return**。可如果这个页面的视图恰好正处于隐藏状态
            //   （被 hideViewToLeft 藏过、或上一次切换只执行了一半），
            //   那么"重复点同一个按钮"就永远不会把它重新显示出来 ⇒ **页面永远空白**。
            //   —— 这正是用户实测「重命名实例后，实例消失，接着下载页/其他页面也变成滚木」
            //      能一路串下去的机制之一。
            //
            //   ⇒ 短路之前**确保它真的可见**，再返回。
            try {
                if (ui instanceof BaseUI) {
                    BaseUI b = (BaseUI) ui;
                    // 重新走一遍 onStart：子类实现里就是 showViewFromLeft(..., setVisibility(VISIBLE))
                    b.onStart();
                }
            } catch (Throwable ignored) {
            }
            System.out.println("-------------------------------------------switch to same ui, ensure visible");
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

    /**
     * 把某个页面从「返回栈」里移除（退出/完成时调用，MainActivity 一口气调了 20+ 次）。
     *
     * ★★★★★ 2026-10-16 **修「重命名实例后实例消失 → 下载页/其他页面也跟着变滚木」**。
     *
     * <p>老实现有两个致命缺陷，正是用户描述的「影响全局」的那个根因：
     *
     * <p><b>缺陷 1 —— 循环里边遍历边删、不同步下标（漏删）：</b>
     * <pre>
     *   for (int i = 0; i &lt; uis.size(); i++) {
     *       if (uis.get(i) == ui) uis.remove(i);   // 删完 i 还 ++ ⇒ 相邻的重复项被跳过
     *   }
     * </pre>
     * {@link #switchMainUI} 的注释里已经写明「栈还只增不减，来回点几下就膨胀成一串重复项」，
     * 也就是说**同一个页面在栈里可能连续出现多次**。前向删除会把相邻的那一份漏在栈里。
     *
     * <p><b>缺陷 2（真正致命）—— 删掉的可能是 {@code currentUI}：</b>
     * 移除之后 {@code currentUI} 仍指向一个**已经不在栈里**的幽灵对象。
     * 此后 {@link #switchMainUI} 一进来就撞上这段：
     * <pre>
     *   if (ui != null &amp;&amp; ui == currentUI) {
     *       System.out.println("switch to same ui, keep state (no-op)");
     *       return;                       // ← 直接返回，什么都不做
     *   }
     * </pre>
     * 于是**目标页面永远不会 onStart()、永远保持隐藏 ⇒ 页面全空白**。
     * 这完全对应实测现象：**重命名实例之后，实例消失，接着下载页 / 其他页面也变成空白**。
     *
     * <p>★ 修法：
     * <ol>
     *   <li>用**倒序**遍历删除（不会漏删、也不会下标越界）；</li>
     *   <li>若被移除的正是 {@code currentUI}，把 {@code currentUI} 清成 {@code null}
     *       —— 这样下一次 {@code switchMainUI} 一定会真正执行 onStart，
     *       而不会命中「同一个 UI 就直接 return」的短路；</li>
     *   <li>把栈**去重**（同一页面只保留最后一次出现），从根上消除"重复项膨胀"。</li>
     * </ol>
     */
    public void removeUIIfExist(BaseUI ui) {
        if (ui == null || uis == null) {
            return;
        }
        boolean removedCurrent = false;
        // ① 倒序删除：既不漏删也不会下标越界
        for (int i = uis.size() - 1; i >= 0; i--) {
            if (uis.get(i) == ui) {
                uis.remove(i);
                if (ui == currentUI) {
                    removedCurrent = true;
                }
            }
        }
        // ② 当前页被移出栈：**在这里直接把它藏起来**
        //    —— 不能只把 currentUI 置 null 就完事：
        //    MainActivity.closeCurrentUI() 后面紧跟一串
        //      `if (uiManager.currentUI == xxxUI) xxxUI.onStop();`
        //    一旦 currentUI 变成 null，那些判断**全部不再成立**，
        //    被移除的那一页就**永远不会 onStop（视图不隐藏）** ⇒ 它一直压在栈顶页上面 ⇒ 页面看起来空白。
        if (removedCurrent) {
            try {
                ui.onStop();
            } catch (Throwable ignored) {
            }
            currentUI = null;
        }
        // ③ 去重：修掉「重复点击同一按钮导致同一页面在栈里出现多次」的历史遗留
        dedupeStack();
    }

    /** 返回栈去重（同一页面只保留**最后一次**出现的位置，保持相对顺序）。 */
    private void dedupeStack() {
        try {
            if (uis == null || uis.size() < 2) {
                return;
            }
            for (int i = uis.size() - 1; i >= 0; i--) {
                BaseUI a = uis.get(i);
                boolean removedBefore = false;
                for (int j = i - 1; j >= 0; j--) {
                    if (uis.get(j) == a) {
                        uis.remove(j);
                        removedBefore = true;
                    }
                }
                // ★ 在 i 之前删掉了元素 ⇒ 原 i 位置整体左移，必须同步 i（每轮只调一次）
                if (removedBefore) {
                    i--;
                }
            }
        } catch (Throwable ignored) {
        }
    }
}