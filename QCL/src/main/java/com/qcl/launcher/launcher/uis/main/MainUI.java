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
import com.qcl.launcher.skin.GameCharacter;
import com.qcl.launcher.skin.MinecraftSkinRenderer;
import com.qcl.launcher.skin.SkinGLSurfaceView;
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
    private SkinGLSurfaceView skinGLSurfaceView;
    private MinecraftSkinRenderer skinRenderer;
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
        if (skinGLSurfaceView != null) {
            skinGLSurfaceView.onResume();
            skinGLSurfaceView.setVisibility(showModelCfg ? View.VISIBLE : View.GONE);
        }
        if (!showModelCfg && accountModelView != null) {
            accountModelView.setVisibility(View.GONE);
        }
        CustomAnimationUtils.showViewFromLeft(mainUI,activity,context,true);
        activity.hideBarTitle();

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
                                launchVersionIcon.setBackground(context.getDrawable(R.drawable.ic_grass));
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
    }

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
     * Builds the persistent 3D character shown in the middle of the launcher, reusing the same
     * skin renderer the offline-skin editor already uses.
     */
    private void setupAccountModel() {
        if (accountModelView == null || skinGLSurfaceView != null) {
            return;
        }
        try {
            skinRenderer = new MinecraftSkinRenderer(context, R.drawable.skin_alex, true);
            skinGLSurfaceView = new SkinGLSurfaceView(context);
            skinGLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            skinGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            skinGLSurfaceView.setZOrderOnTop(true);
            skinGLSurfaceView.setRenderer(skinRenderer, 5f);
            skinGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            skinGLSurfaceView.setPreserveEGLContextOnPause(true);
            // 人物视图占满整个长方形容器
            accountModelView.addView(skinGLSurfaceView, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT));
        } catch (Throwable t) {
            // A device without a usable GL context must not take the whole launcher down.
            t.printStackTrace();
            skinGLSurfaceView = null;
            skinRenderer = null;
        }
    }

    private void refreshAccountModel() {
        if (accountModelContainer == null) {
            return;
        }
        if (skinRenderer == null) {
            // First attempt may have failed before the view existed; try once more.
            setupAccountModel();
            if (skinRenderer == null) {
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
            accountModelView.setVisibility(View.VISIBLE);
            skinRenderer.mCharacter = new GameCharacter(true);
            skinRenderer.updateTexture(skin, null);
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
        try {
            accountModelView.setVisibility(View.VISIBLE);
            skinRenderer.mCharacter = new GameCharacter(slim);
            // The renderer only draws a cape when it is a 64x32 texture.
            Bitmap usableCape = (cape != null && cape.getWidth() == 64 && cape.getHeight() == 32) ? cape : null;
            skinRenderer.updateTexture(skin, usableCape);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static Bitmap decodeCape(String path) {
        if (path == null) return null;
        File file = new File(path);
        return file.isFile() ? BitmapFactory.decodeFile(path) : null;
    }

    @Override
    public void onStop() {
        super.onStop();
        // ★ 1.3.7 修复：切到其他页面时，3D 人物（GLSurfaceView）只 onPause 不够 ——
        //   部分手机上会把画面残留在上层，遮住设置/下载/版本列表界面。这里彻底隐藏。
        if (skinGLSurfaceView != null) {
            skinGLSurfaceView.onPause();
            skinGLSurfaceView.setVisibility(View.GONE);
        }
        if (accountModelView != null) {
            accountModelView.setVisibility(View.GONE);
        }
        CustomAnimationUtils.hideViewToLeft(mainUI,activity,context,true);
    }

    @Override
    public void onClick(View v) {
        if (v == startAccountUI){
            activity.uiManager.switchMainUI(activity.uiManager.accountUI);
        }
        if (v == startGameManagerUI){
            if (noVersionAlert.getVisibility() == View.VISIBLE){
                activity.uiManager.switchMainUI(activity.uiManager.versionListUI);
            }
            else {
                activity.uiManager.gameManagerUI.versionName = activity.publicGameSetting.currentVersion.substring(activity.publicGameSetting.currentVersion.lastIndexOf("/") + 1);
                activity.uiManager.switchMainUI(activity.uiManager.gameManagerUI);
            }
        }
        if (v == startVersionListUI){
            activity.uiManager.switchMainUI(activity.uiManager.versionListUI);
        }
        if (v == startDownloadUI){
            activity.uiManager.switchMainUI(activity.uiManager.downloadUI);
        }
        if (v == startMultiPlayerUI){
            // ★ 1.5.0：不再弹那个白底白字的小 AlertDialog，改跳**完整的二级页面**
            //   （照 FCL 的 MultiplayerUI：左栏导航 + 右侧内容区 + 房主/房客教程）。
            activity.uiManager.switchMainUI(activity.uiManager.multiplayerUI);
        }
        if (v == startSettingUI){
            activity.uiManager.switchMainUI(activity.uiManager.settingUI);
        }
        // ★ 1.4.1 新增：实验室（★ 1.5.0：「大厅」分支已随入口一起移除）
        if (v == startLabUI){
            activity.uiManager.switchMainUI(activity.uiManager.labUI);
        }
        // ★ 1.5.0 新排版独有：左侧导航底部「返回上一层」→ **层级式返回**（backToLastUI：出栈一层）。
        //   ★ 用户明确要求：不是一键回主页，而是"退一层、再退一层，一直退到主界面"。
        if (startHomeUI != null && v == startHomeUI){
            activity.backToLastUI();
        }
        if (v == startGame){
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
                // ★ 1.5.0 兜底分工：左栏用图标库方块，启动按钮上方仍用草方块
                versionIcon.setBackground(context.getDrawable(R.drawable.ic_qcl_version_setting_white));
                if (launchVersionIcon != null) {
                    launchVersionIcon.setBackground(context.getDrawable(R.drawable.ic_grass));
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
