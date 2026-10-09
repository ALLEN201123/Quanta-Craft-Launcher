package com.qcl.launcher.launcher.uis.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.LaunchCountDialog;
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
import com.qcl.launcher.launcher.launch.check.LaunchTools;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.launcher.uis.universal.setting.right.launcher.ExteriorSettingUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.launcher.view.spinner.VersionSpinnerAdapter;
import com.qcl.launcher.skin.gltf.SkinViewer;
import com.qcl.launcher.skin.gltf.SkinRenderer;
import com.qcl.launcher.skin.utils.Avatar;
// ★ 1.4.1：老格式皮肤需要归一化后再渲染（否则帽子层黑块 / 模型误判）
import com.qcl.launcher.skin.utils.NormalizedSkin;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.DrawableUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.io.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    public LinearLayout mainUI;

    private LinearLayout startAccountUI;
    private LinearLayout startGameManagerUI;
    private LinearLayout startVersionListUI;
    private LinearLayout startDownloadUI;
    private LinearLayout startMultiPlayerUI;
    private LinearLayout startSettingUI;
    // ★ 1.4.1：实验室入口（自 1.4.0 朋友源码包合并）
    // ★ 1.5.0：原来的 startLobbyUI（大厅）已整体移除（用户："没有实际用处"）。
    private LinearLayout startLabUI;
    /** ★ 1.5.0 新排版独有：左侧导航最底部的「返回上一层」按钮（照 FCL 的 back）。
     *  ★ 是**层级式返回**（backToLastUI：出栈一层），不是一键回主页。
     *  旧排版没有这个控件 → findViewById 返回 null，切页时判空即可。 */
    private LinearLayout startHomeUI;
    /**
     * ★ 1.5.0：左侧栏**主界面按钮**（在「设置」上面，图标 = 图标库 ic_baseline_home_white）。
     * 点一下**一步回到主界面**（走 {@code MainActivity.backToHome()}，会清空页面栈）。
     * ★ 与 {@link #startHomeUI}（最底部的「返回」，出栈退一层）是两个不同的东西。
     * ★ 旧排版没有这个控件 → findViewById 返回 null，切页时判空即可。
     */
    private LinearLayout startHomePageUI;

    private LinearLayout startGame;
    private TextView launchVersionText;
    /** ★ 1.5.0：启动区版本行左边的**版本图标**（随加载器自动切换：草方块 / 原版 / Forge / Fabric…）。
     *  ★★ 注意是全工程的**独立 id**（{@code launch_version_icon}）——
     *  左侧导航已有一个 {@code current_version_icon}，绝不能复用（id 重复会让
     *  findViewById 只返回第一个 → 两处图标不同步）。 */
    private ImageView launchVersionIcon;
    /** ★ 1.5.0：版本行（版本图标 + 版本名）整体 —— 点它进版本列表。
     *  ★ 原来这里是「三条杠」按钮，用户要求整个移除，改为点整行进列表（旧排版为 null）。 */
    private LinearLayout launchVersionRow;

    public ImageView accountSkinFace;
    public ImageView accountSkinHat;

    private LinearLayout accountModelView;
    private FrameLayout accountModelContainer;

    // ★★★★★ 1.5.0：主界面人物用 **FCL 的 glTF 管线**（真正的骨骼动画 + 待机随机变体）。
    //   ★★★ 1.5.0：老管线（SkinGLSurfaceView / MinecraftSkinRenderer / GameCharacter）
    //   **已彻底删除**，全站（主界面 + 皮肤对话框 + 微软换肤对话框）统一走这一套。
    private com.qcl.launcher.skin.gltf.SkinViewer skinViewer;
    private com.qcl.launcher.skin.gltf.SkinRenderer gltfRenderer;

    public TextView accountName;
    public TextView accountType;

    private ImageView versionIcon;
    private LinearLayout noVersionAlert;
    private TextView currentVersionText;

    // ★ 1.5.0：主界面公告栏（照 FCL ui_main.xml 的 announcement_container）。
    //   默认 gone，只有拉到「该显示」的公告时才点亮。旧排版没有这几个控件 → 全为 null，判空即可。
    private LinearLayout announcementContainer;
    private TextView announcementTitle;
    private TextView announcementText;
    private TextView announcementDate;
    private LinearLayout announcementHide;
    /** 当前正在展示的那条公告（点隐藏时要记它的 id）。 */
    private Announcement currentAnnouncement;

    private VersionSpinnerAdapter versionSpinnerAdapter;

    private ImageView versionListIcon;
    private ImageView downloadIcon;
    private ImageView multiplayerIcon;
    private ImageView settingIcon;

    /** ★ 1.2.3：FCL 同款 —— 版本装了哪个加载器就返回哪个的图标
     *  ★ 1.2.5：改走 ModLoaderDetector.iconRes 单一来源，保证各页面图标一致 */
    private Integer loaderIconFor(File versionDir) {
        int li = ModLoaderDetector.iconRes(versionDir);
        return li == 0 ? null : li;
    }

    /**
     * ★★★ 1.5.0：用**已缓存**的当前版本路径，**同步**填充「启动游戏」上方的版本名与图标。
     *
     * <p>解决的问题：进入启动器时版本名 / 图标「一闪而过」（先显示占位、再热切换）。
     * 根因是布局的初始 text 是占位串，而真实值只在异步线程里才写入。
     *
     * <p>缓存的 {@code currentVersion} 是完整路径 ⇒ 版本名 = 文件名，主线程立刻就能取到；
     * 图标按「加载器图标 → 远古原石 / 草方块」同一条优先级链取（与异步分支口径一致），
     * 这样异步线程稍后填的是**同一个值**，不会产生视觉变化。
     */
    /**
     * ★ 1.5.0（用户实测「切换版本后，右下角持续显示的版本没有实时刷新」）：
     * 供 {@code GameListAdapter} 在把某个版本设为「当前版本」后调用，立刻同步启动按钮上方的
     * 版本名与图标（内部就是复用下面的 applyCachedVersionName，逻辑与启动时完全一致）。
     */
    public void refreshCurrentVersionDisplay() {
        try {
            applyCachedVersionName();
        } catch (Throwable ignored) {
            // 刷新失败不影响主界面
        }
    }

    /**
 * ★ 1.5.0 辅助：按版本名在版本列表里找 {@link GameListBean}（为了拿它自带的 iconPath）。
 * 找不到就返回 null，调用方按兜底处理。
 */
private GameListBean findGameListBean(String name) {
    try {
        if (activity == null || activity.uiManager == null
                || activity.uiManager.versionListUI == null
                || activity.uiManager.versionListUI.gameList == null) {
            return null;
        }
        java.util.List<GameListBean> list = activity.uiManager.versionListUI.gameList;
        for (int i = 0; i < list.size(); i++) {
            GameListBean b = list.get(i);
            if (b != null && name != null && name.equals(b.name)) {
                return b;
            }
        }
    } catch (Throwable ignored) {
    }
    return null;
}

private void applyCachedVersionName() {
        try {
            if (activity == null || activity.publicGameSetting == null) {
                return;
            }
            String cur = activity.publicGameSetting.currentVersion;
            if (cur == null || cur.trim().isEmpty()) {
                return;   // 真的没有版本 → 保持占位，异步分支会走「无版本」逻辑
            }
            String name = new File(cur).getName();
            if (name == null || name.isEmpty()) {
                return;
            }
            if (launchVersionText != null) {
                launchVersionText.setText(name);
            }
            if (launchVersionIcon != null) {
                Integer li = loaderIconFor(new File(activity.launcherSetting.gameFileDirectory
                        + "/versions/" + name));
                if (li != null) {
                    launchVersionIcon.setBackground(context.getDrawable(li));
                } else {
                    int res = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector
                            .isLegacyVersion(name) ? R.drawable.ic_cobble : R.drawable.ic_grass;
                    launchVersionIcon.setBackground(context.getDrawable(res));
                }
            }
            // ★★★ 1.5.0 修复（用户实测「我在版本列表切版本，那个游戏设置/当前版本的图标
            //   没有跟着切换」）：原来只刷了启动按钮上方的 launchVersionIcon，
            //   **漏了左侧导航那格 versionIcon** ⇒ 在版本列表里切了版本，左栏图标还是旧的。
            //   ⇒ 这里一并刷新，并沿用与 onItemSelected 相同的优先级
            //   （版本自带 iconPath → 加载器 logo → 方块兜底）。
            if (versionIcon != null) {
                GameListBean bean = findGameListBean(name);
                String iconPath = (bean == null) ? null : bean.iconPath;
                if (iconPath != null && !iconPath.isEmpty() && new File(iconPath).exists()) {
                    versionIcon.setBackground(DrawableUtils.getDrawableFromFile(iconPath));
                } else {
                    Integer li2 = loaderIconFor(new File(activity.launcherSetting.gameFileDirectory
                            + "/versions/" + name));
                    if (li2 != null) {
                        versionIcon.setBackground(context.getDrawable(li2));
                    } else {
                        versionIcon.setBackground(context.getDrawable(R.drawable.ic_qcl_version_setting_white));
                    }
                }
            }
        } catch (Throwable ignored) {
            // 预填失败无所谓：异步分支随后仍会写入正确的值
        }
    }

    public MainUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainUI = activity.findViewById(R.id.ui_main);

        startAccountUI = activity.findViewById(R.id.start_ui_account);
        startGameManagerUI = activity.findViewById(R.id.start_ui_game_manager);
        startVersionListUI = activity.findViewById(R.id.start_ui_version_list);
        startDownloadUI = activity.findViewById(R.id.start_ui_download);
        startMultiPlayerUI = activity.findViewById(R.id.start_ui_multi_player);
        startSettingUI = activity.findViewById(R.id.start_ui_setting);
        // ★ 1.4.1 新增入口（★ 1.5.0：「大厅」已移除，只剩实验室）
        startLabUI = activity.findViewById(R.id.start_ui_lab);
        // ★ 1.5.0 新排版独有：「回主界面」按钮（旧排版为 null）
        startHomeUI = activity.findViewById(R.id.start_ui_home);
        startHomePageUI = activity.findViewById(R.id.start_ui_home_page);

        startGame = activity.findViewById(R.id.launcher_play_button);
        launchVersionText = activity.findViewById(R.id.launch_version_text);
        // ★ 1.5.0 新排版独有：版本行图标 + 进版本列表按钮（旧排版没有 → 为 null，下面统一判空）
        launchVersionIcon = activity.findViewById(R.id.launch_version_icon);
        // ★ 1.5.0 新排版独有：版本行（版本图标 + 版本名）。点它进版本列表；旧排版没有 → 为 null。
        launchVersionRow = activity.findViewById(R.id.launch_version_row);
        if (launchVersionRow != null) {
            launchVersionRow.setOnClickListener(v -> {
                // ★ 1.5.0：**没有任何版本时 → 跳下载页**（让玩家去装版本），
                //   有版本时 → 跳版本列表（用户："直接点那个版本显示就能进"）。
                String cur = activity.publicGameSetting == null
                        ? "" : activity.publicGameSetting.currentVersion;
                boolean noVersion = cur == null || cur.trim().isEmpty();
                if (noVersion) {
                    if (activity.uiManager != null && activity.uiManager.downloadUI != null) {
                        activity.uiManager.switchMainUI(activity.uiManager.downloadUI);
                    }
                } else if (activity.uiManager != null && activity.uiManager.versionListUI != null) {
                    activity.uiManager.switchMainUI(activity.uiManager.versionListUI);
                }
            });
        }

        accountSkinFace = activity.findViewById(R.id.account_skin_face);
        accountSkinHat = activity.findViewById(R.id.account_skin_hat);
        accountModelView = activity.findViewById(R.id.account_model_view);
        accountModelContainer = activity.findViewById(R.id.account_model_container);
        setupAccountModel();
        accountName = activity.findViewById(R.id.account_name_text);
        accountType = activity.findViewById(R.id.account_state_text);

        versionIcon = activity.findViewById(R.id.current_version_icon);
        noVersionAlert = activity.findViewById(R.id.no_version_alert_text);
        currentVersionText = activity.findViewById(R.id.current_version_name_text);

        // ★★★ 1.5.0 修复「进入启动器时，启动游戏上方的版本一闪而过」：
        //   原来版本名**只在异步线程里填**（要读完 gameList 才 setText），
        //   而布局里 launch_version_text 的初始 text 是
        //   @string/launcher_button_current_version（"当前版本"占位）
        //   ⇒ 进主界面先显示占位文字，几百毫秒后才热切换成真实版本名 = 肉眼可见的闪一下。
        //   publicGameSetting.currentVersion 存的是**完整路径**（启动时落盘的，版本名 = 它的文件名），
        //   ⇒ 这里在主线程**同步**先填一次；异步线程稍后读到相同值（绝大多数情况），
        //     setText 内容不变 → 不会再闪。
        applyCachedVersionName();

        // ★ 1.5.0：公告栏（新排版独有；旧排版为 null，后面统一判空）
        announcementContainer = activity.findViewById(R.id.announcement_container);
        announcementTitle = activity.findViewById(R.id.announcement_title);
        announcementText = activity.findViewById(R.id.announcement_text);
        announcementDate = activity.findViewById(R.id.announcement_date);
        announcementHide = activity.findViewById(R.id.announcement_hide);
        if (announcementContainer != null) {
            // 先藏起来，等公告拉取结果回来再决定显不显示 —— 避免「闪一下空卡片」。
            announcementContainer.setVisibility(View.GONE);
        }

        //icon
        versionListIcon = activity.findViewById(R.id.version_list_icon);
        downloadIcon = activity.findViewById(R.id.download_icon);
        multiplayerIcon = activity.findViewById(R.id.multiplayer_icon);
        settingIcon = activity.findViewById(R.id.setting_icon);

        startAccountUI.setOnClickListener(this);
        startGameManagerUI.setOnClickListener(this);
        startVersionListUI.setOnClickListener(this);
        startDownloadUI.setOnClickListener(this);
        startMultiPlayerUI.setOnClickListener(this);
        startSettingUI.setOnClickListener(this);
        // ★ 1.4.1 新增入口（★ 1.5.0：「大厅」已移除，只剩实验室）
        startLabUI.setOnClickListener(this);
        // ★ 1.5.0 新排版独有：左侧导航底部「回主界面」按钮（旧排版为 null → 跳过）
        if (startHomeUI != null) {
            startHomeUI.setOnClickListener(this);
        }
        // ★★★★★ 1.5.0 崩溃级 Bug 修复（用户实测「主界面按钮点不动」）：
        //   onClick() 里**有** `v == startHomePageUI → activity.backToHome()` 的分支，
        //   但这里**从来没给它 setOnClickListener** ⇒ 按钮完全没有监听器、点了毫无反应。
        //   （其余左栏按钮都在上面挂了，唯独这个漏了。）
        if (startHomePageUI != null) {
            startHomePageUI.setOnClickListener(this);
        }

        startGame.setOnClickListener(this);
        // ★ 1.5.0：公告栏「隐藏」按钮 —— 记住这条公告 id，以后不再显示（新排版独有）
        if (announcementHide != null) {
            announcementHide.setOnClickListener(v -> hideAnnouncement());
        }
        // ★★★ 1.1.1：长按启动按钮 → 选择渲染器（公共选择器，版本设置/全局设置共用同一套）
        startGame.setOnLongClickListener(v -> {
            // ★ 1.4.9 修复「长按启动切了渲染器，版本设置里不显示 / 全局设置被改」：
            //   · apply() 现在只在「全局入口」才动内存里的全局对象（版本级入口不碰它），
            //     所以这里切完**全局设置不会被污染**。
            //   · 切完要刷新版本设置页的显示 —— 通过 GameManagerUI 让它重读该版本 qcl.cfg。
            final String vPath = activity.publicGameSetting == null
                    ? null : activity.publicGameSetting.currentVersion;
            com.qcl.launcher.launcher.launch.RendererPicker.show(activity,
                    activity.privateGameSetting,
                    vPath,
                    () -> {
                        // 回主界面后，若游戏管理页开着，刷新它显示的渲染器名。
                        //   注意不能用 GameManagerUI.onResume() —— 那是「切 UI」的生命周期回调，
                        //   拿它当刷新用会顺手把页面切走。
                        //   正确做法：直接让版本设置页重读该版本 qcl.cfg（refresh 里会重读）。
                        try {
                            if (activity.uiManager != null
                                    && activity.uiManager.gameManagerUI != null
                                    && activity.uiManager.gameManagerUI.gameManagerUIManager != null
                                    && activity.uiManager.gameManagerUI.gameManagerUIManager.versionSettingUI != null) {
                                String vName = new java.io.File(vPath == null ? "" : vPath).getName();
                                if (!vName.isEmpty()) {
                                    activity.uiManager.gameManagerUI.gameManagerUIManager
                                            .versionSettingUI.refresh(vName);
                                }
                            }
                        } catch (Throwable ignoredRefresh) {
                            // 刷新失败不影响设置已保存
                        }
                    });
            return true;
        });
    }

    private AuthlibInjectorServer getServerFromUrl(String url){
        ArrayList<AuthlibInjectorServer> list = InitializeSetting.initializeAuthlibInjectorServer(context);
        for (int i = 0;i < list.size();i++){
            if (list.get(i).getUrl().equals(url)){
                return list.get(i);
            }
        }
        return null;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStart() {
        super.onStart();
        // ★ 1.3.7：回到主界面恢复 3D 人物（受「主界面显示账号人物」开关控制，默认开）
        boolean showModelCfg = true;
        try {
            showModelCfg = activity.launcherSetting == null || activity.launcherSetting.showAccountModel;
        } catch (Throwable ignored) {
        }
        // ★ 1.5.0：glTF 人物随主界面 onResume 恢复渲染
        if (skinViewer != null) {
            try {
                skinViewer.onResume();
            } catch (Throwable ignoredViewer) {
            }
            skinViewer.setVisibility(showModelCfg ? View.VISIBLE : View.GONE);
            // ★★★ 1.5.0 修复（用户实测「切页面再回主界面，人物会消失」）：
            //   onStop() 里把 accountModelView 整个隐藏了，而 TextureView 一旦停止渲染
            //   不会自己恢复显示 —— 光 onResume 不够，**必须重新走一遍纹理绑定**才会在屏幕上出现。
            //   refreshAccountModel() → showModel() → gltfRenderer.updateTexture(...)。
            if (showModelCfg) {
                try {
                    if (gltfRenderer == null || skinViewer == null) {
                        setupAccountModel();
                    }
                    // ★ 用户在设置里改了「人物动作」后**不必重启启动器**：
                    //   每次回到主界面都按当前设置重新 playAnimation 一次。
                    //   （之前只在 setupAccountModel 里调，那是"首次创建视图"才跑 ⇒ 改了设置没反应。）
                    if (gltfRenderer != null) {
                        gltfRenderer.playAnimation(animClipFromIndex());
                    }
                    refreshAccountModel();
                } catch (Throwable ignoredRefresh) {
                    // 恢复失败也别把主界面搞崩
                }
            }
        } else {
            // glTF 视图还没建（首次进入）→ 走老路径建一次
            try {
                setupAccountModel();
                refreshAccountModel();
            } catch (Throwable ignoredFirst) {
            }
        }
        if (!showModelCfg && accountModelView != null) {
            accountModelView.setVisibility(View.GONE);
        }
        CustomAnimationUtils.showViewFromLeft(mainUI,activity,context,true);
        activity.hideBarTitle();

        // ★ 2026-10-09 用户要求：QCL 版「新手教程」（参考 FCL 的 com/mio/util/GuideUtil.kt），
        //   **按当前左侧栏的布局**逐个引导；只看没看过的步骤。
        //   tag 存在外部私有目录 guide_tag.txt —— 想重看直接删掉那个文件即可。
        try {
            final View[] navViews = {
                    startHomePageUI, startGameManagerUI, startVersionListUI, startDownloadUI,
                    startLabUI, startMultiPlayerUI, startSettingUI, startHomeUI,
                    // ★ 2026-10-09 用户要求补上**右侧**的介绍
                    startAccountUI, launchVersionRow, startGame};
            final String[] navTags = {
                    "nav_home_page", "nav_game_manager", "nav_version_list", "nav_download",
                    "nav_lab", "nav_multi_player", "nav_setting", "nav_home",
                    "right_account", "right_version", "right_play"};
            final String[] navTitles = {
                    "主界面", "游戏管理", "版本列表", "下载", "实验室", "多人联机", "设置", "回到主界面",
                    "账号", "当前版本", "启动游戏"};
            final String[] navDescs = {
                    "随时点这里回到主界面。",
                    "管理当前版本的存档、模组、资源包。",
                    "安装 / 切换 Minecraft 版本，也能看每个版本的详情。",
                    "下载游戏版本、模组、整合包与光影。",
                    "实验性功能都收在这里。",
                    "和朋友一起玩（Terracotta 联机）。",
                    "渲染器、Java 版本、主题色、内存都在这里。",
                    "在二级页面点这个也能直接回主界面。",
                    "点这里切换 / 管理账号（离线、微软、外置登录都在这）。",
                    "这里显示要启动的版本，点一下可以换版本。",
                    "一切选好后，点这个按钮启动游戏。会先做启动前检查（Java / Vulkan / 模组 / 版本）。"};
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    GuideUtil.Step[] steps = new GuideUtil.Step[navViews.length];
                    for (int i = 0; i < navViews.length; i++) {
                        steps[i] = new GuideUtil.Step(navTags[i], navViews[i], navTitles[i], navDescs[i]);
                    }
                    GuideUtil.showOnMain(activity, steps);
                } catch (Throwable ignoredGuide) {
                }
            }, 1500L);
        } catch (Throwable ignoredGuideSetup) {
        }

        new Thread(() -> {
            ArrayList<GameListBean> gameList = SettingUtils.getLocalVersionInfo(activity.launcherSetting.gameFileDirectory,activity.publicGameSetting.currentVersion);
            activity.runOnUiThread(() -> {
                GameListBean currentVersion = new GameListBean("","","",true);
                if (!activity.publicGameSetting.currentVersion.equals("")){
                    for (int i = 0;i < gameList.size();i++) {
                        if (gameList.get(i).name.equals(activity.publicGameSetting.currentVersion.substring(activity.publicGameSetting.currentVersion.lastIndexOf("/") + 1))) {
                            currentVersion = gameList.get(i);
                        }
                    }
                }
                if (gameList.size() > 0 && currentVersion.name.equals("")) {
                    currentVersion = gameList.get(0);
                    activity.publicGameSetting.currentVersion = activity.launcherSetting.gameFileDirectory + "/versions/" + currentVersion.name;
                    GsonUtils.savePublicGameSetting(activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
                }
                versionSpinnerAdapter = new VersionSpinnerAdapter(context,gameList,activity.launcherSetting.gameFileDirectory);
                // ★★★ 1.5.0：新排版（ui_main_new.xml）**已移除**启动按钮旁的三角符号版本选择，
                //   这里 findViewById 会返回 null —— 必须判空，否则启动器一进主界面就 NPE。
                //   旧排版（ui_main.xml）仍有该 spinner，所以逻辑保留、按 null 分叉。
                Spinner gameVersionSpinner = activity.findViewById(R.id.launcher_spinner_version);
                if (gameVersionSpinner != null) {
                    gameVersionSpinner.setAdapter(versionSpinnerAdapter);
                    gameVersionSpinner.setSelection(versionSpinnerAdapter.getPosition(currentVersion));
                    gameVersionSpinner.setOnItemSelectedListener(this);
                }
                if (!currentVersion.name.equals("")){
                    // ★★★ 1.5.0 新排版（纯图标入口）：no_version_alert_text /
                    //   current_version_name_text 在布局里是 gone，**绝不能在这里被点亮**，
                    //   否则左侧导航会冒出文字、破坏「全屋只留图标」。
                    //   判据：新排版独有控件 start_ui_home（左栏返回按钮）存在 → 走纯图标分支。
                    //   ★ 原来用 launch_version_list_button 判断，但它（三条杠）已被用户要求删除，
                    //   所以改挂到 start_ui_home 上。
                    boolean iconOnly = startHomeUI != null;
                    if (!iconOnly) {
                        noVersionAlert.setVisibility(View.GONE);
                        currentVersionText.setVisibility(View.VISIBLE);
                        currentVersionText.setText(currentVersion.name);
                    }
                    launchVersionText.setText(currentVersion.name);
                    // ★ 1.5.0 图标分工（用户明确）：
                    //   · versionIcon（左侧导航第一格）→ 兜底换成**图标库的方块**；
                    //   · launchVersionIcon（启动按钮上方的版本显示）→ 兜底仍是草方块。
                    //   两处的「版本图标自动切换」都保留：有版本自带图标就用自带的，
                    //   没有就走加载器图标（Forge/Fabric…），都没有才用各自的兜底图。
                    if (!currentVersion.iconPath.equals("") && new File(currentVersion.iconPath).exists()) {
                        Drawable d = DrawableUtils.getDrawableFromFile(currentVersion.iconPath);
                        versionIcon.setBackground(d);
                        if (launchVersionIcon != null) launchVersionIcon.setBackground(d);
                    }
                    else {
                        Integer li = loaderIconFor(new File(activity.launcherSetting.gameFileDirectory
                                + "/versions/" + currentVersion.name));
                        if (li != null) {
                            Drawable d = context.getDrawable(li);
                            versionIcon.setBackground(d);
                            if (launchVersionIcon != null) launchVersionIcon.setBackground(d);
                        } else {
                            versionIcon.setBackground(context.getDrawable(R.drawable.ic_qcl_version_setting_white));
                            if (launchVersionIcon != null) {
                                // ★★★ 1.5.0：**远古版本统一用原石立方体图标**（用户指定），
                                //   其余保持草方块。当前版本名见上面的 currentVersion.name。
                                int iconRes = com.qcl.launcher.launcher.download.modloader
                                        .ModLoaderDetector.isLegacyVersion(currentVersion == null ? null : currentVersion.name)
                                        ? R.drawable.ic_cobble : R.drawable.ic_grass;
                                launchVersionIcon.setBackground(context.getDrawable(iconRes));
                            }
                        }
                    }
                }
                else {
                    boolean iconOnly = startHomeUI != null;
                    if (!iconOnly) {
                        noVersionAlert.setVisibility(View.VISIBLE);
                        currentVersionText.setVisibility(View.GONE);
                    }
                    launchVersionText.setText(context.getString(R.string.launcher_button_current_version));
                    // 没有任何版本：左栏用图标库方块，启动按钮上方仍是草方块（用户要求）
                    versionIcon.setBackground(context.getDrawable(R.drawable.ic_qcl_version_setting_white));
                    if (launchVersionIcon != null) {
                        launchVersionIcon.setBackground(context.getDrawable(R.drawable.ic_grass));
                    }
                }
            });
        }).start();

        refreshAccount();
        // ★ 1.5.0：每次回主界面刷一次公告（新排版独有；旧排版没有 announcementContainer → 直接返回）
        loadAnnouncement();
        // ★★★★★ 1.5.0：上次游戏**异常退出**留下的诊断（用户原话「崩了我咋复制日志给你？」）。
        //   游戏崩溃后游戏窗口和日志窗都没了，玩家拿不到日志 ⇒ 这里在下次进主界面时弹窗，
        //   直接把关键诊断摆出来，并给「一键复制」按钮（贴到 QQ/微信直接发）。
    }

    /**
     * ★ 1.5.0：弹出「上次游戏崩溃」的诊断框（含一键复制）。
     * 全部包在 try-catch 里 —— 诊断功能出问题绝不能影响启动器可用性。
     */

    // ============================ ★ 1.5.0 公告栏（照 FCL） ============================

    /**
     * 读公告并决定显不显示。数据源优先级：
     * <ol>
     *   <li>{@code assets/announcement.json}（内置，永远可用）</li>
     * </ol>
     * 解析失败 / 没有该显示的内容 → 隐藏公告栏，**静默处理**（公告坏了绝不能影响主界面）。
     *
     * <p>★ 关闭必须在后台线程做 IO，结果回主线程 setText（沿用本文件既有习惯）。
     */
    private void loadAnnouncement() {
        if (announcementContainer == null) {
            return;
        }
        final int versionCode = getAppVersionCode();
        new Thread(() -> {
            final ArrayList<Announcement> list;
            try {
                String json = readAssetText(Announcement.ASSET_NAME);
                list = Announcement.parseList(json);
            } catch (Throwable t) {
                activity.runOnUiThread(() -> {
                    if (announcementContainer != null) {
                        announcementContainer.setVisibility(View.GONE);
                    }
                });
                return;
            }
            activity.runOnUiThread(() -> applyAnnouncement(list, versionCode));
        }, "qcl-announcement").start();
    }

    /** 读 assets 里的文本文件（QCL 的 FileUtils 没有现成方法，这里自己读）。 */
    private String readAssetText(String name) throws IOException {
        try (java.io.InputStream is = context.getAssets().open(name)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /** 当前 APK 的 versionCode（公告按它判断 minVersion/maxVersion）。取不到就返回 0。 */
    private int getAppVersionCode() {
        try {
            android.content.pm.PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (Throwable t) {
            return 0;
        }
    }

    private void applyAnnouncement(ArrayList<Announcement> list, int versionCode) {
        if (announcementContainer == null) {
            return;
        }
        Announcement picked = null;
        if (list != null) {
            for (Announcement a : list) {
                if (a != null && a.shouldDisplay(context, versionCode)) {
                    picked = a;
                    break;
                }
            }
        }
        if (picked == null) {
            currentAnnouncement = null;
            announcementContainer.setVisibility(View.GONE);
            return;
        }
        currentAnnouncement = picked;
        if (announcementTitle != null) {
            announcementTitle.setText(picked.getDisplayTitle(context));
        }
        if (announcementText != null) {
            announcementText.setText(picked.getDisplayContent(context));
        }
        if (announcementDate != null) {
            announcementDate.setText(picked.getDate());
        }
        CustomAnimationUtils.showViewFromLeft(announcementContainer, activity, context, false);
        announcementContainer.setVisibility(View.VISIBLE);
    }

    /** 玩家点「隐藏」→ 记下 id + 收起公告栏。 */
    private void hideAnnouncement() {
        if (currentAnnouncement != null) {
            currentAnnouncement.hide(context);
        }
        currentAnnouncement = null;
        if (announcementContainer != null) {
            announcementContainer.setVisibility(View.GONE);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public void refreshAccount() {
        switch (activity.publicGameSetting.account.loginType){
            case 1:
                accountName.setText(activity.publicGameSetting.account.auth_player_name);
                accountType.setText(context.getString(R.string.item_account_type_offline));
                // 离线账号没有 texture（空串会让 setAvatar 裁剪 NPE，头像停留在默认的艾利克斯）。
                // 头像由 applyOfflineSkin 按当前皮肤生成：默认史蒂夫，导入 PNG 则用导入皮肤的脸。
                Avatar.setAvatarFromSkin(Avatar.getBitmapFromRes(context, R.drawable.skin_steve),
                        accountSkinFace, accountSkinHat);
                break;
            case 2:
                accountName.setText(activity.publicGameSetting.account.auth_player_name);
                accountType.setText(context.getString(R.string.item_account_type_mojang));
                Avatar.setAvatar(activity.publicGameSetting.account.texture, accountSkinFace, accountSkinHat);
                break;
            case 3:
                accountName.setText(activity.publicGameSetting.account.auth_player_name);
                accountType.setText(context.getString(R.string.item_account_type_microsoft));
                Avatar.setAvatar(activity.publicGameSetting.account.texture, accountSkinFace, accountSkinHat);
                break;
            case 4:
            case 5:
                accountName.setText(activity.publicGameSetting.account.auth_player_name);
                accountType.setText(getServerFromUrl(activity.publicGameSetting.account.loginServer).getName());
                Avatar.setAvatar(activity.publicGameSetting.account.texture, accountSkinFace, accountSkinHat);
                break;
            default:
                accountName.setText(context.getString(R.string.launcher_scroll_account_name));
                accountType.setText(context.getString(R.string.launcher_scroll_account_state));
                accountSkinFace.setImageBitmap(((BitmapDrawable) context.getDrawable(R.drawable.ic_steve)).getBitmap());
                accountSkinHat.setImageBitmap(null);
                break;
        }
        refreshAccountModel();
    }

    /**
     * Builds the persistent 3D character shown in the middle of the launcher.
     *
     * <p>★ 1.5.0：主界面人物**改用 FCL 的 glTF 管线**（classic-player.gltf / slim-player.gltf），
     * 因此能拿到真正的骨骼待机动画（{@code idle} + {@code idle_sub_1/2/3} 随机插播）、
     * 以及跟手旋转（{@code rotateStep}）与缩放（{@code setScale}）。
     * 老 {@link #skinGLSurfaceView}/{@link #skinRenderer}（GameCharacter）**保留给皮肤编辑器**，
     * 主界面不再使用。
     */
    /**
     * ★ 1.5.0：把设置里「人物动作」的下拉索引翻译成 glTF 模型内的 clip 名。
     *
     * <p>下拉 4 项与 glTF 模型里的 4 个真实 clip 一一对应：
     * {@code 0→idle}、{@code 1→idle_sub_1}、{@code 2→idle_sub_2}、{@code 3→idle_sub_3}。
     *
     * <p>★ 这里**故意不复用 {@code SkinAnimations.entries}**（Kotlin 的 public val 在 Java 侧
     * 不加 {@code @JvmField} 是 private 访问控制，编译直接报错）—— clip 名在 Java 侧固定一份，
     * 少一层跨语言依赖，越界（含旧存档残留值）一律回落基础待机。
     */
    private static final String[] GLTF_IDLE_CLIPS = {
            "idle", "idle_sub_1", "idle_sub_2", "idle_sub_3"
    };

    private String animClipFromIndex() {
        int idx = 0;
        try {
            idx = activity.launcherSetting == null ? 0 : activity.launcherSetting.accountModelAnim;
        } catch (Throwable ignored) {
        }
        if (idx < 0 || idx >= GLTF_IDLE_CLIPS.length) {
            return GLTF_IDLE_CLIPS[0];
        }
        return GLTF_IDLE_CLIPS[idx];
    }

    private void setupAccountModel() {
        if (accountModelView == null || skinViewer != null) {
            return;
        }
        try {
            gltfRenderer = new com.qcl.launcher.skin.gltf.SkinRenderer(context);
            skinViewer = new com.qcl.launcher.skin.gltf.SkinViewer(context);
            accountModelView.addView(skinViewer,
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT));
            skinViewer.setRenderer(gltfRenderer, context.getResources().getDisplayMetrics().density);
            // ★ 用户选的动画
            try {
                gltfRenderer.playAnimation(animClipFromIndex());
            } catch (Throwable ignoredAnim) {
                // 读不到就用默认
            }
            // ★ 人物大小：**用 FCL 的原值**（scale 默认 1.0，配 ZOOM=0.9）。
            //   ★ 之前我按"短边 dp/360"自己算，那是 GameCharacter 时代的思路，
            //     在 glTF 管线里语义不对（glTF 的大小由 cameraDistance 决定）⇒ 人物显得过小。
            //   用户可直接**双指捏合**放大/缩小（SkinViewer 已实现，范围 0.7~2.0）。
            try {
                gltfRenderer.setScale(1.0f);
            } catch (Throwable ignoredScale) {
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // 老的人物视图已随 GameCharacter 管线一起删除，无需再隐藏
    }

    /**
     * ★★★ 1.5.0：**整个老人物管线（GameCharacter / MinecraftSkinRenderer / SkinGLSurfaceView）
     *   已彻底删除**，不再有「glTF 起不来就退回老管线」的兜底。
     *
     *   原因：老 GameCharacter 只有 setWalkSwing 一个动作，**没有 FCL 的待机随机变体状态机**
     *   ⇒ 只要走到兜底，人物就变成"一直站着不动"（用户实测：皮肤对话框里就是这样）。
     *   与其让人物偶尔退化成一堆木头，不如**glTF 起不来就不显示人物**，行为可预期。
     *   ⇒ 老管线相关文件 skin/GameCharacter.java、skin/MinecraftSkinRenderer.java、
     *     skin/SkinGLSurfaceView.java、skin/body、skin/cape 均已删除。
     */

    /**
     * ★ 1.5.0 改为 public：微软换肤对话框上传成功 / 换完皮肤后要主动让主界面人物跟着换
     * （见 MicrosoftAccountSkinDialog.saveAccountAndRefresh）。
     */
public void refreshAccountModel() {
        // ★ 2026-10-11：排障开关。默认关闭（QCL_DBG_SKIN=1 时才打印），
        //   用来确定"上传后主界面到底有没有被刷新、刷的是哪个账号、皮肤字节数多少"。
        //   —— 因为"加了刷新但界面没变"这种问题，靠读代码推断容易错，必须看实证。
        final boolean dbg = "1".equals(System.getenv("QCL_DBG_SKIN"));
        if (dbg) {
            try {
                com.qcl.launcher.auth.Account a0 = activity.publicGameSetting.account;
                System.out.println("[QCL-skin] refreshAccountModel 被调用: account="
                        + (a0 == null ? "null" : a0.auth_player_name)
                        + " loginType=" + (a0 == null ? -1 : a0.loginType)
                        + " textureLen=" + (a0 == null || a0.texture == null ? 0 : a0.texture.length())
                        + " model=" + (a0 == null || a0.model == null ? "null" : a0.model.name())
                        + " gltf=" + (gltfRenderer != null)
                        + " container=" + (accountModelContainer != null));
            } catch (Throwable ignored) {
            }
        }
        if (accountModelContainer == null) {
            return;
        }
        // ★★★★★ 1.5.0 修复（用户实测「切页面再回主界面，人物会消失」的真凶）：
        //   这个守卫原本只看老字段 skinRenderer（GameCharacter）。1.5.0 老管线已删除，
        //   现在**唯一**的人物管线是 glTF ⇒ 守卫只认 gltfRenderer。
        if (gltfRenderer == null) {
            setupAccountModel();
            if (gltfRenderer == null) {
                return;
            }
        }
        try {
            com.qcl.launcher.auth.Account account = activity.publicGameSetting.account;
            // Fresh install / no account at all -> show nothing.
            if (account == null || account.loginType == 0) {
                hideModel();
                return;
            }

            // Offline accounts follow the skin picked in the offline skin editor, and nothing else.
            if (account.loginType == 1) {
                applyOfflineSkin(account);
                return;
            }

            // Mojang / Microsoft / third-party auth servers: use the skin that came with the account.
            Bitmap skin = Avatar.stringToBitmap(account.texture);
            if (skin == null) {
                hideModel();
                return;
            }
            // ★ 1.5.0：统一交给 showModel，这样**微软账户的纹理/模型也会跟着换**
            //   （微软与离线、Mojang 共用同一条路径，只是皮肤来源不同）。
            // ★★★ 1.5.0 修复（用户实测「微软账号是 slim 细手臂，但显示成粗手臂」）：
            //   这里原来**写死 `false`** ⇒ 不管微软/Mojang 的皮肤是不是 slim（3D 细手臂模型），
            //   一律切到 classic 粗手臂模型，模型与贴图错位 → 看着就是错的粗手臂。
            //   正解和离线分支一样：用 NormalizedSkin 从像素判定 slim，传给 showModel 切对模型。
            Bitmap renderSkin = skin;
            boolean skinSlim = false;
            // ★★★★★ 2026-10-11 修（用户实测「上传完之后主界面人物没啥变化」的不一致源）：
            //   **优先用玩家保存的模型选择**（account.model），只有老账号没这个字段时
            //   才回退"按皮肤像素判定"。
            //   原来这里无条件用 NormalizedSkin(...).isSlim() ⇒
            //   对话框里玩家选 classic、预览也是 classic，但**主界面按图片又判成 slim**
            //   ⇒ 两处显示不一致，看着就像"上传没生效"。
            //   （皮肤对话框里同样的逻辑已经改过，这里必须一起改，否则两边永远对不上。）
            if (account.model != null) {
                skinSlim = (account.model == com.qcl.launcher.auth.yggdrasil.TextureModel.ALEX);
                try {
                    NormalizedSkin normalized = new NormalizedSkin(skin);
                    renderSkin = normalized.isOldFormat()
                            ? normalized.getNormalizedTexture() : normalized.getOriginalTexture();
                } catch (Throwable ignored) {
                }
            } else {
                try {
                    NormalizedSkin normalized = new NormalizedSkin(skin);
                    skinSlim = normalized.isSlim();
                    renderSkin = normalized.isOldFormat() ? normalized.getNormalizedTexture() : normalized.getOriginalTexture();
                } catch (Throwable ignored) {
                }
            }
            showModel(renderSkin, decodeCape(account.capeTexture), skinSlim);
            // ★ 1.5.0：微软账号若还没有披风数据（老账号是在"披风功能"之前登录的），
            //   这里**后台补拉一次**并回填 —— 否则老账号永远看不到披风，只能重新登录。
            //   失败静默（不打扰玩家），且只在"微软账号 + 没有披风"时才会发请求。
            if (account.loginType == 3 && (account.capeTexture == null || account.capeTexture.trim().isEmpty())) {
                fetchMicrosoftCapeOnce(account);
            }
            // ★★★★★ 2026-10-11：**皮肤也让它在后台对一次**（用户要求"实时绑定"）——
            //   在官网/别的启动器换了皮肤，回 QCL 进主界面就会自动跟上，不用重登。
            //   比一次而已：一样就什么都不做；不一样才回填 + 重画。
            if (account.loginType == 3) {
                fetchMicrosoftSkinOnce(account);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void applyOfflineSkin(com.qcl.launcher.auth.Account account) {
        com.qcl.launcher.auth.offline.OfflineSkinSetting offline = account.offlineSkinSetting;
        int type = offline == null ? 0 : offline.type;

        switch (type) {
            case 1: {  // Steve
                Bitmap skin = Avatar.getBitmapFromRes(context, R.drawable.skin_steve);
                Avatar.setAvatarFromSkin(skin, accountSkinFace, accountSkinHat);
                showModel(skin, false);
                return;
            }
            case 3: {  // player-uploaded skin file: the avatar follows the imported png
                Bitmap skin = null;
                if (offline.skinPath != null && new File(offline.skinPath).isFile()) {
                    Bitmap uploaded = BitmapFactory.decodeFile(offline.skinPath);
                    if (uploaded != null) {
                        skin = uploaded;
                    }
                }
                if (skin == null) {
                    skin = Avatar.getBitmapFromRes(context, R.drawable.skin_steve);
                }
                Avatar.setAvatarFromSkin(skin, accountSkinFace, accountSkinHat);
                // ★★★ 1.4.1 修复：原写法 `skin.getHeight() != 32` 会把**任意 64x64 的 classic 皮肤**
                //   也当成细手臂（slim）→ 模型与贴图不匹配 → 脖子/腰出现黑色错位条。
                //   改成用 NormalizedSkin 正确判定（老格式一律 classic），并顺带把老格式皮肤
                //   归一化后再交给渲染器，避免帽子层黑块（与皮肤预览、游戏内保持一致）。
                Bitmap renderSkin = skin;
                boolean skinSlim = false;
                try {
                    NormalizedSkin normalized = new NormalizedSkin(skin);
                    skinSlim = normalized.isSlim();
                    renderSkin = normalized.isOldFormat() ? normalized.getNormalizedTexture() : normalized.getOriginalTexture();
                } catch (Throwable ignored) {
                }
                showModel(renderSkin, decodeCape(offline.capePath), skinSlim);
                return;
            }
            case 4:   // LittleSkin
                fetchRemoteSkin("https://mcskin.littleservice.cn/", account.auth_player_name);
                return;
            case 5:   // Blessing Skin server configured by the player
                if (offline != null && offline.server != null && !offline.server.isEmpty()) {
                    String base = offline.server.startsWith("http://")
                            ? offline.server.replace("http://", "https://") : offline.server;
                    fetchRemoteSkin(com.qcl.launcher.utils.string.StringUtils.removeSuffix(base, "/") + "/",
                            account.auth_player_name);
                    return;
                }
                showModel(Avatar.getBitmapFromRes(context, R.drawable.skin_steve), false);
                return;
            case 2:   // Alex
                Bitmap alex = Avatar.getBitmapFromRes(context, R.drawable.skin_alex);
                Avatar.setAvatarFromSkin(alex, accountSkinFace, accountSkinHat);
                showModel(alex, true);
                return;
            case 0:   // default -> Steve（用户要求：离线默认是史蒂夫）
            default:
                Bitmap steve = Avatar.getBitmapFromRes(context, R.drawable.skin_steve);
                Avatar.setAvatarFromSkin(steve, accountSkinFace, accountSkinHat);
                showModel(steve, false);
        }
    }

    /** Resolves <server><name>.json to a texture hash, then loads that texture. */
    private void fetchRemoteSkin(final String base, final String playerName) {
        new Thread(() -> {
            Bitmap skin = null;
            Bitmap cape = null;
            try {
                String json = com.qcl.launcher.utils.io.NetworkUtils.doGet(
                        com.qcl.launcher.utils.io.NetworkUtils.toURL(base + playerName + ".json"));
                com.qcl.launcher.auth.offline.SkinJson result =
                        com.qcl.launcher.utils.gson.JsonUtils.GSON.fromJson(
                                json, com.qcl.launcher.auth.offline.SkinJson.class);
                if (result != null && result.hasSkin() && result.getHash() != null) {
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection)
                            new java.net.URL(base + "textures/" + result.getHash()).openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    skin = BitmapFactory.decodeStream(connection.getInputStream());
                }
                if (result != null && result.getCapeHash() != null) {
                    java.net.HttpURLConnection capeConnection = (java.net.HttpURLConnection)
                            new java.net.URL(base + "textures/" + result.getCapeHash()).openConnection();
                    capeConnection.setDoInput(true);
                    capeConnection.connect();
                    cape = BitmapFactory.decodeStream(capeConnection.getInputStream());
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            final Bitmap loaded = skin;
            final Bitmap loadedCape = cape;
            activity.runOnUiThread(() -> {
                if (loaded != null) {
                    Avatar.setAvatarFromSkin(loaded, accountSkinFace, accountSkinHat);
                    showModel(loaded, loadedCape, true);
                } else {
                    Bitmap fallback = Avatar.getBitmapFromRes(context, R.drawable.skin_steve);
                    Avatar.setAvatarFromSkin(fallback, accountSkinFace, accountSkinHat);
                    showModel(fallback, false);
                }
            });
        }).start();
    }

    /** Only the character is hidden; its container keeps its space so the launch button stays put. */
    private void hideModel() {
        if (accountModelView != null) accountModelView.setVisibility(View.INVISIBLE);
    }

    private void showModel(final Bitmap skin, final boolean slim) {
        showModel(skin, null, slim);
    }

    private void showModel(final Bitmap skin, final Bitmap cape, final boolean slim) {
        if (skin == null) {
            hideModel();
            return;
        }
        // ★★★★★ 1.5.0：**纹理实时更新**（用户要求"换皮肤显示也跟着换"）。
        //   glTF 管线自己会做皮肤归一化（旧 64×32 转换 + slim 检测），并**自动切模型**。
        //   离线 / 微软 / Mojang / 外置认证 全部走这里，所以换账户类型也会跟着换纹理与模型。
        if (gltfRenderer != null && skinViewer != null) {
            try {
                accountModelView.setVisibility(View.VISIBLE);
                skinViewer.setVisibility(View.VISIBLE);
                gltfRenderer.updateTexture(skin, cape, slim);
                return;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        // ★★★ 1.5.0：老 GameCharacter 兜底**已删除**。glTF 不可用时直接不显示人物，
        //   绝不退回"永远站着不动"的老渲染器（那正是用户报的现象）。
        hideModel();
    }

/**
 * ★ 1.5.0：给**还没有披风数据的微软账号**后台补拉一次披风并回填。
 *
 * <p>为什么需要：披风是这批才加的（此前 {@code Msa.getTextures} 的 CAPE 分支被注释），
 * 所以**所有老账号的 {@code capeTexture} 都是空的** ⇒ 除非玩家重新登录，否则永远没披风。
 *
 * <p>约定：
 * <ul>
 *   <li>只在 {@code auth_access_token} 有值时才请求（没有 token 就不做无用功）；</li>
 *   <li>拉到后写回 account + 存盘 + 立刻刷新主界面人物；</li>
 *   <li>失败**静默**，不弹 Toast、不打断启动 —— 玩家不该因为一个披风被打扰。</li>
 * </ul>
 */
private void fetchMicrosoftCapeOnce(final com.qcl.launcher.auth.Account account) {
    if (account == null || account.auth_access_token == null
            || account.auth_access_token.trim().isEmpty()) {
        return;
    }
    // ★★★★★ 2026-10-11 修（用户实测「换肤对话框里的披风显示与主界面披风显示依旧不一致」）：
    //   与皮肤同一个坑 —— 玩家刚在对话框里**切换/隐藏了披风**，
    //   而微软服务端生效有延迟，此时用服务端数据回填会把刚做的改动覆盖回去，
    //   于是"对话框里是新披风、主界面还是旧披风"（或反过来）。
    //   保护期内一律跳过服务端同步（与 SKIN_UPLOAD_GRACE_MS 同一套机制）。
    if (System.currentTimeMillis() - lastLocalCapeChangeAt < SKIN_UPLOAD_GRACE_MS) {
        return;
    }
    // 防抖：同一次进主界面若被多次调用，别重复发请求
    if (capeFetchInFlight) {
        return;
    }
    capeFetchInFlight = true;
    final String token = account.auth_access_token;
    new Thread(() -> {
        try {
            com.qcl.launcher.auth.microsoft.Msa.MinecraftProfileResponse profile =
                    com.qcl.launcher.auth.microsoft.Msa.getMinecraftProfile("Bearer", token);
            if (profile == null) {
                return;
            }
            java.util.Map<com.qcl.launcher.auth.yggdrasil.TextureType,
                    com.qcl.launcher.auth.yggdrasil.Texture> map =
                    com.qcl.launcher.auth.microsoft.Msa.getTextures(profile).orElse(null);
            if (map == null) {
                return;
            }
            final String cape = com.qcl.launcher.auth.Account.downloadTextureAsBase64(
                    map.get(com.qcl.launcher.auth.yggdrasil.TextureType.CAPE));
            if (cape == null || cape.trim().isEmpty()) {
                return;   // 该账号本来就没有披风，别反复重试
            }
            account.capeTexture = cape;
            final String saved = cape;
            activity.runOnUiThread(() -> {
                try {
                    com.qcl.launcher.utils.gson.GsonUtils.saveAccounts(
                            activity.uiManager.accountUI.accounts,
                            com.qcl.launcher.manifest.AppManifest.ACCOUNT_DIR + "/accounts.json");
                    // 拉到了披风 ⇒ 立刻把人物重画一次
                    showModel(Avatar.stringToBitmap(account.texture), Avatar.stringToBitmap(saved), false);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
            // 静默失败
        } finally {
            capeFetchInFlight = false;
        }
    }).start();
}

private boolean capeFetchInFlight;

/**
 * ★★★★★ 2026-10-11 新增：**皮肤实时刷新**（用户要求）。
 *
 * <p>用户原话：「主界面显示的人物是跟微软账号实时绑定的吗？比如我在另一个启动器上改了
 * 我的皮肤，然后重新进入 QCL，它那个主界面没有立刻刷新。」
 *
 * <p>原来的行为：`account.texture` **只在登录那一刻**写入，之后再也不更新 ⇒
 * 在别处（官网 / 别的启动器）换了皮肤，回 QCL 看到的还是旧皮肤，只能重新登录。
 *
 * <p>现在的行为：每次进主界面都**静默**向微软要一次当前皮肤纹理，
 * 与本地比一次（base64 不同才更新）：
 * <ul>
 *   <li>一样 ⇒ 什么都不做（绝大多数情况，零感知）；</li>
 *   <li>不一样 ⇒ 回填 `account.texture` + 存盘 + **立刻重画人物**（不用重启、不用重登）；</li>
 *   <li>失败/无网络 ⇒ 静默跳过，绝不影响启动。</li>
 * </ul>
 * 与已有的 {@link #fetchMicrosoftCapeOnce} 是同一套模式（防抖 + 后台线程 + 主线程回填）。
 */
private void fetchMicrosoftSkinOnce(final com.qcl.launcher.auth.Account account) {
    if (account == null || account.loginType != 3
            || account.auth_access_token == null || account.auth_access_token.trim().isEmpty()) {
        return;
    }
    // ★★★★★ 2026-10-11【关键】本地刚上传过 ⇒ **在保护期内不许用服务端覆盖**。
    //
    //   为什么（照 FCL 的原话）—— FCL 的 MicrosoftAccountSkinDialog.kt 里明确写着：
    //     "Don't call refreshPreview() here — the binding resets to fallback
    //      before async fetch completes. The preview from updatePreviewFromFile()
    //      already shows the correct uploaded skin."
    //   即：**上传成功后不要去重新拉服务端**，因为微软服务端处理有延迟，
    //   立刻拉回来的是**旧皮肤**，会把刚上传的新皮肤覆盖掉。
    //
    //   我先前加的"每次进主界面都拉一次服务端"正好踩了这个坑 ⇒
    //   玩家上传完回主界面，主界面又被旧皮肤盖回去，与对话框里显示的新皮肤不一致。
    //
    //   现在：上传成功后的 {@link #SKIN_UPLOAD_GRACE_MS} 内**跳过服务端同步**，
    //   让本地刚上传的那张保持权威（这段窗口足够微软处理完，之后自动恢复正常同步）。
    if (System.currentTimeMillis() - lastLocalSkinUploadAt < SKIN_UPLOAD_GRACE_MS) {
        return;
    }
    if (skinFetchInFlight) {
        return;
    }
    skinFetchInFlight = true;
    final String token = account.auth_access_token;
    new Thread(() -> {
        try {
            com.qcl.launcher.auth.microsoft.Msa.MinecraftProfileResponse profile =
                    com.qcl.launcher.auth.microsoft.Msa.getMinecraftProfile("Bearer", token);
            if (profile == null) {
                return;
            }
            java.util.Map<com.qcl.launcher.auth.yggdrasil.TextureType,
                    com.qcl.launcher.auth.yggdrasil.Texture> map =
                    com.qcl.launcher.auth.microsoft.Msa.getTextures(profile).orElse(null);
            if (map == null) {
                return;
            }
            final String fresh = com.qcl.launcher.auth.Account.downloadTextureAsBase64(
                    map.get(com.qcl.launcher.auth.yggdrasil.TextureType.SKIN));
            if (fresh == null || fresh.trim().isEmpty()) {
                return;
            }
            // 与本地一致就不折腾（避免每次进主页都重画）
            final String local = account.texture;
            if (local != null && local.equals(fresh)) {
                return;
            }
            account.texture = fresh;
            activity.runOnUiThread(() -> {
                try {
                    com.qcl.launcher.utils.gson.GsonUtils.saveAccounts(
                            activity.uiManager.accountUI.accounts,
                            com.qcl.launcher.manifest.AppManifest.ACCOUNT_DIR + "/accounts.json");
                    // 皮肤变了 ⇒ 立刻重画（refreshAccountModel 会读新的 account.texture）
                    refreshAccountModel();
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
            // 静默失败：拿不到就继续用本地的
        } finally {
            skinFetchInFlight = false;
        }
    }, "qcl-skin-fetch").start();
}

private boolean skinFetchInFlight;

/**
 * ★ 2026-10-11：**本地皮肤上传保护期**（毫秒）。
 *
 * <p>玩家在换肤对话框里上传成功后，记下时间戳；在保护期内
 * {@link #fetchMicrosoftSkinOnce} **不再向服务端同步皮肤** ——
 * 因为微软服务端处理有延迟（通常几十秒），此刻拉回来的是旧皮肤，
 * 会把刚上传的新皮肤覆盖掉，造成"主界面与对话框显示不一致"。
 *
 * <p>这与 FCL 的做法一致：FCL 上传成功后刻意**不**重新拉取预览
 * （源码注释："Don't call refreshPreview() here — the binding resets to fallback
 * before async fetch completes"）。
 *
 * <p>取 5 分钟：足够服务端处理完，又不至于让"在别处换了皮肤"永远同步不过来。
 */
public static final long SKIN_UPLOAD_GRACE_MS = 5 * 60 * 1000L;

/** 最近一次本地皮肤上传成功的时间戳（0 表示还没上传过）。 */
private static volatile long lastLocalSkinUploadAt = 0L;

/**
 * ★ 2026-10-11：换肤上传成功后由 {@code MicrosoftAccountSkinDialog} 调用，
 * 标记"刚刚本地换过皮肤"，从而在保护期内跳过服务端同步。
 */
public static void markLocalSkinUploaded() {
    lastLocalSkinUploadAt = System.currentTimeMillis();
}

/** 最近一次**本地切换/隐藏披风**的时间戳（0 表示还没动过）。 */
private static volatile long lastLocalCapeChangeAt = 0L;

/**
 * ★ 2026-10-11：玩家在换肤对话框里切换或隐藏披风后由对话框调用。
 *
 * <p>作用与 {@link #markLocalSkinUploaded()} 相同：保护期内
 * {@link #fetchMicrosoftCapeOnce} **不再用服务端数据回填** ——
 * 微软服务端生效有延迟，此刻回填会把玩家刚做的改动覆盖回去，
 * 表现就是"对话框里的披风和主界面显示的不一致"。
 */
public static void markLocalCapeChanged() {
    lastLocalCapeChangeAt = System.currentTimeMillis();
}

    /**
     * ★ 1.5.0：入参既可以是**披风文件路径**（离线账号，`offlineSkinSetting.capePath`），
     * 也可以是**base64 纹理**（微软等账号，`account.capeTexture`，与 texture 同格式）。
     *
     * <p>靠前缀判断：base64 纹理不会以 {@code /} 或盘符开头。
     */
    private static Bitmap decodeCape(String pathOrBase64) {
        if (pathOrBase64 == null || pathOrBase64.trim().isEmpty()) {
            return null;
        }
        String s = pathOrBase64.trim();
        try {
            if (s.startsWith("/") || s.contains(":\\") || s.contains(":/")) {
                File file = new File(s);
                return file.isFile() ? BitmapFactory.decodeFile(s) : null;
            }
            return Avatar.stringToBitmap(s);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // ★ 1.3.7 修复：切到其他页面时，3D 人物只 onPause 不够 ——
        //   部分手机上会把画面残留在上层，遮住设置/下载/版本列表界面。这里彻底隐藏。
        // ★ 1.5.0：glTF 人物（TextureView + EGL）同样要 onPause 并隐藏，
        //   否则换页后画面会残留在上层遮住别的界面。
        if (skinViewer != null) {
            try {
                skinViewer.onPause();
            } catch (Throwable ignoredViewer) {
            }
            skinViewer.setVisibility(View.GONE);
        }
        if (accountModelView != null) {
            accountModelView.setVisibility(View.GONE);
        }
        CustomAnimationUtils.hideViewToLeft(mainUI,activity,context,true);
    }

    @Override
    public void onClick(View v) {
        // ★★★ 1.5.0 修复（用户实测）：「点击一个按钮会重复弹起关闭」。
        //   旧写法是一串**互相独立的 if**，一个 View 命中多个分支时会连着 switchMainUI 两次
        //   → 视觉上"弹起又关闭"（例如左栏 startHomeUI 与主界面按钮同帧命中）。
        //   ⇒ 改成 **if / else if 链**：一个点击只可能走一个分支。
        if (v == startAccountUI){
            activity.uiManager.switchMainUI(activity.uiManager.accountUI);
        }
        else if (v == startGameManagerUI){
            if (noVersionAlert.getVisibility() == View.VISIBLE){
                activity.uiManager.switchMainUI(activity.uiManager.versionListUI);
            }
            else {
                activity.uiManager.gameManagerUI.versionName = activity.publicGameSetting.currentVersion.substring(activity.publicGameSetting.currentVersion.lastIndexOf("/") + 1);
                activity.uiManager.switchMainUI(activity.uiManager.gameManagerUI);
            }
        }
        else if (v == startVersionListUI){
            activity.uiManager.switchMainUI(activity.uiManager.versionListUI);
        }
        else if (v == startDownloadUI){
            activity.uiManager.switchMainUI(activity.uiManager.downloadUI);
        }
        else if (v == startMultiPlayerUI){
            // ★ 1.5.0：不再弹那个白底白字的小 AlertDialog，改跳**完整的二级页面**
            //   （照 FCL 的 MultiplayerUI：左栏导航 + 右侧内容区 + 房主/房客教程）。
            activity.uiManager.switchMainUI(activity.uiManager.multiplayerUI);
        }
        else if (v == startSettingUI){
            activity.uiManager.switchMainUI(activity.uiManager.settingUI);
        }
        // ★ 1.4.1 新增：实验室（★ 1.5.0：「大厅」分支已随入口一起移除）
        else if (v == startLabUI){
            activity.uiManager.switchMainUI(activity.uiManager.labUI);
        }
        // ★★★ 1.5.0 新排版独有：左侧导航底部「返回上一层」→ **层级式返回**（backToLastUI：出栈一层）。
        //   ★ 用户明确要求：不是一键回主页，而是"退一层、再退一层，一直退到主界面"。
        //
        //   ★★★ 修复（用户实测）：「在主界面点它 → 直接退出游戏（启动器）」。
        //     根因在 MainActivity.backToLastUI()：
        //       if (uiManager.currentUI == uiManager.mainUI) { backToDeskTop(); }   ← 直接回桌面！
        //     左栏是**常驻**的，主界面上它也在那儿，一点就命中这个分支 → 整个启动器被退出。
        //     ⇒ 这里先判：已经**在**主界面就**什么都不做**（既不退也不退出），
        //       只有在二级页面时才真正出栈一层。
        //     （系统返回键走的是 MainActivity.onBackPressed，行为不变。）
        // ★★★ 1.5.0：左侧栏**主界面按钮**（在「设置」上面）→ **一步回到主界面**。
        //   走 MainActivity.backToHome()：switchMainUI(mainUI) + 清空页面栈，
        //   与下面「返回」按钮的"出栈退一层"是两种不同行为。
        else if (startHomePageUI != null && v == startHomePageUI){
            activity.backToHome();
        }
        else if (startHomeUI != null && v == startHomeUI){
            if (activity.uiManager == null || activity.uiManager.mainUI == null) {
                return;
            }
            // 已经在主界面 → 无处可退，直接忽略（绝不 backToDeskTop）
            if (activity.uiManager.currentUI == activity.uiManager.mainUI) {
                return;
            }
            activity.backToLastUI();
        }
        else if (v == startGame){
            // ★★★ 1.5.0：**没有任何版本时，点「启动游戏」不启动，直接跳下载页**。
            //   判据用 currentVersion 是否为空 —— 这是 onStart() 里唯一可靠的"有没有版本"信号：
            //   有版本时它会被写成 "<游戏目录>/versions/<版本名>"，一个都没有时保持 ""。
            //   （用 noVersionAlert 的可见性判断不可靠：新排版里那个标题是 gone，永远是 GONE。）
            String cur = activity.publicGameSetting == null ? "" : activity.publicGameSetting.currentVersion;
            if (cur == null || cur.trim().isEmpty()) {
                if (activity.uiManager != null && activity.uiManager.downloadUI != null) {
                    activity.uiManager.switchMainUI(activity.uiManager.downloadUI);
                }
                return;
            }
            String settingPath = activity.publicGameSetting.currentVersion + "/qcl.cfg";
            String finalPath;
            if (new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile(settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile(settingPath).enable)) {
                finalPath = settingPath;
            }
            else {
                finalPath = AppManifest.SETTING_DIR + "/private_game_setting.json";
            }
            Bundle bundle = new Bundle();
            bundle.putString("setting_path",finalPath);
            bundle.putBoolean("test",false);
            // ★ 1.4.5：启动次数统计 + 里程碑提示（20 / 60 / 80 / 100 …）
            maybeRemindLaunchCount(() -> LaunchTools.launch(context,activity,activity.publicGameSetting.currentVersion,bundle));
        }
    }

    /**
     * ★ 1.4.5：累计启动次数 +1；达到里程碑（20 / 60 / 80 / 100 …）时先弹一次提示，
     * 无论点「赞助一下」还是「以后再说」，关掉后都会继续启动游戏。
     * <p>同一个里程碑只弹一次（记在 launcherSetting.lastLaunchPromptAt），不会反复烦玩家。
     * <p>计数与记录都存在 launcher_setting.json 里，卸载前一直有效。
     */
    private void maybeRemindLaunchCount(Runnable afterPrompt) {
        int count = 1;
        try {
            LauncherSetting ls = activity.launcherSetting;
            if (ls != null) {
                ls.gameLaunchCount = ls.gameLaunchCount + 1;
                count = ls.gameLaunchCount;
                // 计算"已经过了几个 20 次"：20→1、40→2、60→3 …（40 故意不弹，按用户要求 20/60/80/100 的节奏）
                int milestone = milestoneFor(count);
                if (milestone > ls.lastLaunchPromptAt) {
                    ls.lastLaunchPromptAt = milestone;
                    GsonUtils.saveLauncherSetting(ls, AppManifest.SETTING_DIR + "/launcher_setting.json");
                    final int shown = count;
                    LaunchCountDialog dialog = new LaunchCountDialog(context, shown, afterPrompt::run);
                    dialog.show();
                    return;
                }
                GsonUtils.saveLauncherSetting(ls, AppManifest.SETTING_DIR + "/launcher_setting.json");
            }
        } catch (Throwable t) {
            // 统计出任何问题都不该拦住玩家启动游戏
            t.printStackTrace();
        }
        afterPrompt.run();
    }

    /**
     * 里程碑换算：20 次 → 20；60 次 → 60；80 → 80；100 → 100；之后每 +20 一次。
     * 即：第 1~20 次没有提示，第 20 次弹；21~59 不弹；第 60 次弹；61~79 不弹；
     * 第 80 次弹；81~99 不弹；第 100 次弹；之后 120 / 140 … 依次。
     */
    private static int milestoneFor(int count) {
        if (count < 20) {
            return 0;
        }
        if (count < 60) {
            return 20;
        }
        // 60 之后按 20 一档：60→60、80→80、100→100、120→120 …
        return (count / 20) * 20;
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        activity.publicGameSetting.currentVersion = activity.launcherSetting.gameFileDirectory + "/versions/" + ((GameListBean) versionSpinnerAdapter.getItem(position)).name;
        if (activity.privateGameSetting.gameDirSetting.type == 1){
            activity.uiManager.settingUI.settingUIManager.universalGameSettingUI.gameDirText.setText(activity.launcherSetting.gameFileDirectory + "/versions/" + ((GameListBean) versionSpinnerAdapter.getItem(position)).name);
        }
        GsonUtils.savePublicGameSetting(activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        // ★ 1.5.0：新排版左侧「当前版本」入口是纯图标，这个 TextView 是 gone —— 照样 setText 无害。
        currentVersionText.setText(((GameListBean) versionSpinnerAdapter.getItem(position)).name);
        launchVersionText.setText(((GameListBean) versionSpinnerAdapter.getItem(position)).name);
        if (!((GameListBean) versionSpinnerAdapter.getItem(position)).iconPath.equals("") && new File(((GameListBean) versionSpinnerAdapter.getItem(position)).iconPath).exists()) {
            Drawable d = DrawableUtils.getDrawableFromFile(((GameListBean) versionSpinnerAdapter.getItem(position)).iconPath);
            versionIcon.setBackground(d);
            if (launchVersionIcon != null) launchVersionIcon.setBackground(d);
        }
        else {
            // ★★★ 1.2.5 修：这里原来跟 VersionSpinnerAdapter 一样按「version 有没有逗号」
            //   分叉，带逗号（下载页装的 Fabric/Forge）显示 ic_furnace（熔炉），
            //   和版本设置页的加载器 logo 不一致。统一走 loaderIconFor。
            Integer li = loaderIconFor(new File(activity.launcherSetting.gameFileDirectory
                    + "/versions/" + ((GameListBean) versionSpinnerAdapter.getItem(position)).name));
            if (li != null) {
                Drawable d = context.getDrawable(li);
                versionIcon.setBackground(d);
                if (launchVersionIcon != null) launchVersionIcon.setBackground(d);
            } else {
                // ★ 1.5.0 兜底分工：左栏用图标库方块；启动按钮上方按版本类型取图标 ——
                //   ★ 远古版本 → 原石立方体（用户指定），其余 → 草方块
                versionIcon.setBackground(context.getDrawable(R.drawable.ic_qcl_version_setting_white));
                if (launchVersionIcon != null) {
                    String pickedName = ((GameListBean) versionSpinnerAdapter.getItem(position)).name;
                    int res = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector
                            .isLegacyVersion(pickedName) ? R.drawable.ic_cobble : R.drawable.ic_grass;
                    launchVersionIcon.setBackground(context.getDrawable(res));
                }
            }
        }
        changeIcon(versionIcon,themePath,"versionIcon");
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    File themePath;
    public void customTheme(){
        themePath = activity.getExternalFilesDir("Theme");

        if (!themePath.exists()){
            return;
        }
        changeIcon(versionListIcon, themePath, "versionListIcon");
        changeIcon(downloadIcon, themePath, "downloadIcon");
        changeIcon(multiplayerIcon, themePath, "multiplayerIcon");
        changeIcon(settingIcon, themePath, "settingIcon");
        changeIcon(activity.launcherLayout, themePath, "background");
        changeIcon(versionIcon, themePath, "versionIcon");
        if (new File(themePath,"color.json").exists()) {
            try {
                JSONObject jsonObject = new JSONObject(FileUtils.readText(new File(themePath,"color.json")));
                activity.exteriorConfig.primaryColor(Color.parseColor(jsonObject.getString("primaryColor")));
                activity.exteriorConfig.accentColor(Color.parseColor(jsonObject.getString("accentColor")));
                activity.exteriorConfig.apply(activity);
                // 1.0.6：顶部标题栏已移除，主题色不再需要刷到 appBar。
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changeIcon(View view, File themePath, String iconName) {
        File path = new File(themePath, iconName + ".png");
        if (path.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(path.getAbsolutePath());
            view.setBackground(new BitmapDrawable(activity.getResources(), bitmap));
        }
    }
}
