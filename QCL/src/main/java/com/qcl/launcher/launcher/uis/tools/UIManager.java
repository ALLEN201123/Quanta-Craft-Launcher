package com.qcl.launcher.launcher.uis.tools;

import android.content.Context;
import android.content.Intent;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.uis.account.AccountUI;
import com.qcl.launcher.launcher.uis.game.download.DownloadUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricAPIUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricUI;
import com.qcl.launcher.launcher.uis.game.download.right.game.DownloadSimpleListUI;
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
    /** ★ 1.5.2：通用版本选择列表页（三个新加载器共用）。 */
    public DownloadSimpleListUI simpleListUI;
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
        simpleListUI = new DownloadSimpleListUI(context, activity);
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
        simpleListUI.onCreate();
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
                simpleListUI,
                downloadLiteLoaderUI,
                downloadOptifineUI,
                downloadFabricAPIUI,
                downloadQuiltUI,
                downloadQuiltAPIUI
        };
        uis = new ArrayList<>();
        switchMainUI(mainUI);
    }

    /**
     * ★★★★★ 2026-10-20：**合规性临时封禁清单**。
     *
     * <p>背景：2026-10-20 起 PCL / HMCL 等在中国大陆展业的第三方启动器
     * 因合规性原因关停了 Minecraft 核心文件与资源包的下载功能
     * （MineBBS 官方公告 2026-10-20 17:55；公告原文明确「与网易游戏、
     * Mojang Studio 与 Microsoft 均无直接关联」，且各开发组未公布具体原因）。
     *
     * <p>凡是「会下载 Minecraft 本体 / 分发核心文件 / 联机」的页面，全部临时封禁。
     * <p>★★★ **代码全部保留**，这里只是列出要封的页面；恢复时把本方法
     * 改成 {@code return new BaseUI[0]} 即可，不需要还原任何被删的代码。
     */
    private BaseUI[] complianceBannedUIs() {
        // ★★★★★ 2026-10-20 最终版：**下载类入口封禁，离线模式不动**。
        //
        //   用户最终要求：「必须发，离线模式现在是不是已经严厉禁止了
        //   下载的入口？遵守了法规法律」。
        //   ⇒ 规则：
        //     · **下载类页面全部封禁**（合规姿态）：下载页 7 标签 / 陶瓷联机 /
        //       安装游戏 / 安装整合包 / 各加载器版本下载；
        //     · **离线模式不做限制** —— 本地已经下载好的版本照常启动
        //       （LaunchTools 里没有加任何正版校验）。
        //
        //   ★ 封禁只发生在「进入这些页面」这一步，代码一行未删；
        //     恢复时把本方法改成 `return new BaseUI[0];` 即可。
        return new BaseUI[]{
                // —— 下载页（游戏版本 / 整合包 / 模组 / 资源包 / 光影 / 收藏 / 世界）——
                this.downloadUI,
                // —— 陶瓷联机（Terracotta）——
                this.multiplayerUI,
                // —— 安装游戏 / 安装整合包（都会下 MC 本体与核心文件）——
                this.installGameUI,
                this.installPackageUI,
                // —— 各加载器的版本下载页 ——
                this.downloadForgeUI,
                this.downloadNeoForgeUI,
                this.downloadFabricUI,
                this.downloadFabricAPIUI,
                this.downloadLiteLoaderUI,
                this.downloadOptifineUI,
                this.downloadQuiltUI,
                this.downloadQuiltAPIUI,
                // —— 三个新加载器共用的版本列表页 ——
                this.simpleListUI,
        };
    }

    /**
     * ★★★★★ 2026-10-20：**是否持有微软正版账号**。
     *
     * <p>用户要求：「是不是现在能让微软账户下载游戏版本，使用这些功能？」
     * ⇒ 有微软正版账号的玩家应当**放行**，不该被合规限制一起罚。
     *
     * <p>{@code loginType} 取值：
     * <ul>
     *   <li>0 = 未登录</li>
     *   <li>1 = 离线账号</li>
     *   <li>2 = 旧 Mojang 账号</li>
     *   <li><b>3 = 微软正版账号 ← 只有这个算"有正版授权"</b></li>
     *   <li>4 = 第三方 authlib-injector（外置登录）</li>
     *   <li>5 = Nide8Auth</li>
     * </ul>
     */
    private boolean hasGenuineAccount(MainActivity activity) {
        try {
            if (activity == null || activity.publicGameSetting == null) {
                return false;
            }
            com.qcl.launcher.auth.Account acc = activity.publicGameSetting.account;
            return acc != null && acc.loginType == 3;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 目标页面是否在合规封禁清单里。 */
    private boolean isComplianceBanned(BaseUI ui) {
        if (ui == null) {
            return false;
        }
        try {
            BaseUI[] banned = complianceBannedUIs();
            for (BaseUI b : banned) {
                if (b != null && b == ui) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public void switchMainUI(BaseUI ui) {
        // ★★★★★ 2026-10-20：**合规性临时封禁 —— 所有进入被封页面的路径都在这里被拦住**。
        //
        //   为什么拦在这一层：下载页 / 联机页的入口不止一个 ——
        //   左侧栏按钮、主界面"当前版本"那一行、游戏管理入口、
        //   版本列表页的"下载游戏"按钮、没有游戏时的引导弹窗……
        //   在每个入口各改一遍既容易漏、以后恢复也要一处处还原。
        //   拦在 switchMainUI 上 = **一处封死所有路径**，恢复时也只需改一处。
        //
        //   ★ 原逻辑一行未删，只是前面多了这道闸。
        //
        //   ★★★★★ 2026-10-20 最终（用户实测）：
        //     「是不是现在能让微软账户下载游戏版本，使用这些功能？」
        //
        //     ⇒ 拦截条件从「只看地区」改成「**地区 且 非正版**」：
        //        · 中国大陆 + **微软正版账号** → **放行**（能下载 / 联机 / 安装游戏）
        //        · 中国大陆 + 离线 / 第三方 / 无账号 → **拦下并提示**
        //        · 中国大陆以外 → **一律放行**
        //
        //     这才是"支持正版"的正确姿势：合规限制只落在"没有正版授权"的路径上，
        //     不惩罚已经买了游戏的玩家。
        if (isComplianceBanned(ui)) {
            boolean mainland = true;
            try {
                mainland = com.qcl.launcher.utils.net.RegionChecker.isMainlandChina(
                        ui == null ? null : ui.activity);
            } catch (Throwable ignored) {
            }
            boolean genuine = hasGenuineAccount(ui == null ? null : ui.activity);
            // 只有「在中国大陆」且「当前账号不是微软正版」才拦
            if (mainland && !genuine) {
                try {
                    com.qcl.launcher.utils.ui.ComplianceNotice.show(
                            ui == null ? null : ui.activity);
                } catch (Throwable ignored) {
                }
                return;
            }
            // 中国大陆 + 正版  → 放行
            // 中国大陆以外      → 放行
        }
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