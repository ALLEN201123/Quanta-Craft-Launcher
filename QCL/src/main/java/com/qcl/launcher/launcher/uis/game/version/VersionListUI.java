package com.qcl.launcher.launcher.uis.game.version;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.info.contents.ContentListAdapter;
import com.qcl.launcher.launcher.list.info.contents.ContentListBean;
import com.qcl.launcher.launcher.list.local.game.GameListAdapter;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
import com.qcl.launcher.launcher.view.list.ContentListView;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;

import java.util.ArrayList;

public class VersionListUI extends BaseUI implements View.OnClickListener {

    public LinearLayout versionListUI;

    private LinearLayout contentListParent;
    private LinearLayout gameDirRow;
    private ListView versionList;

    public ArrayList<ContentListBean> contentList;
    public ArrayList<GameListBean> gameList;

    private ContentListAdapter contentListAdapter;
    private GameListAdapter gameListAdapter;

    private LinearLayout startAddGameDirUI;
    private LinearLayout startDownloadMcUI;
    private LinearLayout startInstallPackageUI;
    private LinearLayout refresh;
    private LinearLayout startGlobalSettingUI;

    private TextView startDownloadMcUIText;
    private ProgressBar progressBar;

    /**
     * ★★★ 1.5.0：**顶部分类**（照 FCL page_version_list.xml 的 @id/category TabLayout）。
     * <p>0 全部 / 1 Fabric / 2 Forge / 3 NeoForge / 4 其他 —— 与 FCL 的 filterByTab 顺序一致。
     * <p>过滤依据是每个版本目录的加载器（ModLoaderDetector.detect，与图标判定同源）。
     */
    private TextView categoryAll;
    private TextView categoryFabric;
    private TextView categoryForge;
    private TextView categoryNeoForge;
    private TextView categoryOther;
    private int categoryFilter = 0;

    public VersionListUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        versionListUI = activity.findViewById(R.id.ui_version_list);

        gameDirRow = activity.findViewById(R.id.game_dir_row);
        versionList = activity.findViewById(R.id.local_version_list);

        startDownloadMcUIText = activity.findViewById(R.id.start_download_mc_ui);
        startDownloadMcUIText.setOnClickListener(this);

        startAddGameDirUI = activity.findViewById(R.id.start_add_game_directory_ui);
        startAddGameDirUI.setOnClickListener(this);
        startDownloadMcUI = activity.findViewById(R.id.start_ui_download_minecraft);
        startDownloadMcUI.setOnClickListener(this);
        startInstallPackageUI = activity.findViewById(R.id.start_install_package_ui);
        startInstallPackageUI.setOnClickListener(this);
        refresh = activity.findViewById(R.id.refresh_local_version_list);
        refresh.setOnClickListener(this);
        startGlobalSettingUI = activity.findViewById(R.id.start_ui_global_setting);
        startGlobalSettingUI.setOnClickListener(this);
        progressBar = activity.findViewById(R.id.loading_local_version_progress);

        contentListParent = activity.findViewById(R.id.content_list_parent);

        // ★★★ 1.5.0：顶部分类（照 FCL 的 @id/category Tab）—— 绑定 + 挂监听
        categoryAll = activity.findViewById(R.id.version_category_all);
        categoryFabric = activity.findViewById(R.id.version_category_fabric);
        categoryForge = activity.findViewById(R.id.version_category_forge);
        categoryNeoForge = activity.findViewById(R.id.version_category_neoforge);
        categoryOther = activity.findViewById(R.id.version_category_other);
        TextView[] catTabs = {categoryAll, categoryFabric, categoryForge, categoryNeoForge, categoryOther};
        for (TextView t : catTabs) {
            if (t != null) {
                t.setOnClickListener(this);
            }
        }

        new Thread(this::refreshVersionList).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.showBarTitle(context.getResources().getString(R.string.version_list_ui_title),canGoBackToLast(),true);
        CustomAnimationUtils.showViewFromLeft(versionListUI,activity,context,true);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(versionListUI,activity,context,true);
    }

    @Override
    public void onClick(View v) {
        if (v == startDownloadMcUIText || v == startDownloadMcUI){
            activity.uiManager.switchMainUI(activity.uiManager.downloadUI);
            activity.uiManager.downloadUI.downloadUIManager.switchDownloadUI(activity.uiManager.downloadUI.downloadUIManager.downloadMinecraftUI);
        }
        if (v == startAddGameDirUI){
            activity.uiManager.switchMainUI(activity.uiManager.addGameDirectoryUI);
        }
        if (v == startInstallPackageUI) {
            activity.uiManager.switchMainUI(activity.uiManager.installPackageUI);
        }
        if (v == refresh){
            new Thread(this::refreshVersionList).start();
        }
        if (v == startGlobalSettingUI){
            activity.uiManager.switchMainUI(activity.uiManager.settingUI);
            activity.uiManager.settingUI.settingUIManager.switchSettingUIs(activity.uiManager.settingUI.settingUIManager.universalGameSettingUI);
        }
        // ★★★ 1.5.0：顶部分类切换（顺序与 FCL 的 filterByTab 完全一致）
        else if (v == categoryAll) {
            applyCategoryFilter(0);
        }
        else if (v == categoryFabric) {
            applyCategoryFilter(1);
        }
        else if (v == categoryForge) {
            applyCategoryFilter(2);
        }
        else if (v == categoryNeoForge) {
            applyCategoryFilter(3);
        }
        else if (v == categoryOther) {
            applyCategoryFilter(4);
        }
    }

    private void init(){
        contentList = InitializeSetting.initializeContents(context);
        contentListAdapter = new ContentListAdapter(context,activity,contentList);
        // ★★★ 1.5.0：照 FCL 的 profile_list —— 目录条目改为**垂直排列、占满左栏宽度**。
        //   原来固定 240dp 宽、横向铺在顶排 ⇒ 窄屏上直接**超出屏幕**（用户实测指出
        //   "有的已经超出屏幕之外了"）。现在左栏是 30% 屏宽，每项 match_parent 铺满即可，
        //   屏幕多大就多宽，永远在可见范围内。
        gameDirRow.removeAllViews();
        int dirGap = Math.round(6 * context.getResources().getDisplayMetrics().density);
        for (int i = 0; i < contentListAdapter.getCount(); i++) {
            View dirView = contentListAdapter.getView(i, null, gameDirRow);
            LinearLayout.LayoutParams dirParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dirParams.bottomMargin = dirGap;
            gameDirRow.addView(dirView, dirParams);
        }
        // ★★★ 1.5.0 崩溃修复（用户真机崩溃日志 VersionListUI.init:172 NPE）：
        //   gameListAdapter 是在**异步线程** refreshVersionList() 里创建的，
        //   而 onStart → init() 是**同步**跑的 —— 两者存在竞态：
        //   init() 先执行完、此时 adapter 还没被赋值 →
        //   `gameListAdapter.refreshCurrentVersion(...)` 直接空指针（版本列表页都进不去）。
        //   ⇒ 不再假设 adapter 一定存在：没有就按当前 gameList 现建一个。
        if (gameListAdapter == null) {
            if (gameList == null) {
                gameList = new ArrayList<>();
            }
            gameListAdapter = new GameListAdapter(context, activity, gameList);
        }
        gameListAdapter.refreshCurrentVersion(activity.publicGameSetting.currentVersion);
    }

    public void refreshVersionList(){
        activity.runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            startDownloadMcUIText.setVisibility(View.GONE);
            versionList.setVisibility(View.GONE);
        });
        gameList = SettingUtils.getLocalVersionInfo(activity.launcherSetting.gameFileDirectory,activity.publicGameSetting.currentVersion);
        activity.runOnUiThread(() -> {
            // ★★★ 1.5.0：加载完统一交给 applyCategoryFilter —— 它会按当前分类过滤、
            //   重建 adapter 并处理「空列表提示」，取代原来写死的「有/无版本」两分支。
            applyCategoryFilter(categoryFilter);
            progressBar.setVisibility(View.GONE);
        });
    }

    /**
     * ★★★ 1.5.0：按**顶部分类**过滤版本列表（照 FCL {@code VersionListPage.filterByTab} 的语义）。
     *
     * <p>顺序与 FCL 完全一致：0 全部 / 1 Fabric / 2 Forge / 3 NeoForge / 4 其他。
     * <p>本方法还负责：切选中态、重建 adapter、更新「还没有任何版本」提示的显隐。
     *
     * @param cat 分类下标（0~4）
     */
    private void applyCategoryFilter(int cat) {
        this.categoryFilter = cat;

        // ---- 选中态：选中的用实心底 + 全不透明；未选中的用透明底 + 半透明 ----
        TextView[] tabs = {categoryAll, categoryFabric, categoryForge, categoryNeoForge, categoryOther};
        for (int i = 0; i < tabs.length; i++) {
            if (tabs[i] == null) {
                continue;
            }
            tabs[i].setBackground(context.getDrawable(
                    i == cat ? R.drawable.qcl_launch_block_bg : R.drawable.qcl_button_gray));
            tabs[i].setAlpha(i == cat ? 1.0f : 0.55f);
        }

        if (versionList == null) {
            return;
        }
        if (gameList == null) {
            gameList = new ArrayList<>();
        }

        // ---- 过滤 ----
        ArrayList<GameListBean> filtered = new ArrayList<>();
        for (GameListBean bean : gameList) {
            if (matchesCategory(bean, cat)) {
                filtered.add(bean);
            }
        }

        gameListAdapter = new GameListAdapter(context, activity, filtered);
        gameListAdapter.refreshCurrentVersion(activity.publicGameSetting.currentVersion);
        versionList.setAdapter(gameListAdapter);

        boolean empty = filtered.isEmpty();
        startDownloadMcUIText.setVisibility(empty ? View.VISIBLE : View.GONE);
        versionList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * 某个版本是否属于指定分类。
     *
     * <p>加载器判定统一走 {@code ModLoaderDetector.detect} —— 与图标判定**同源**，
     * 不会出现「图标显示 Fabric 但分类里找不到」这种不一致。
     * <p>「其他」= 前三类之外的**全部**（含 Quilt / LiteLoader / Babric / 原版无加载器）。
     */
    private boolean matchesCategory(GameListBean bean, int cat) {
        if (cat == 0) {
            return true;   // 全部
        }
        String loader = null;
        try {
            loader = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.detect(
                    new java.io.File(activity.launcherSetting.gameFileDirectory + "/versions/" + bean.name));
        } catch (Throwable ignored) {
        }
        boolean fabric = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.FABRIC.equals(loader);
        boolean forge = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.FORGE.equals(loader);
        boolean neoForge = com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.NEOFORGE.equals(loader);
        if (cat == 1) {
            return fabric;
        }
        if (cat == 2) {
            return forge;
        }
        if (cat == 3) {
            return neoForge;
        }
        return !fabric && !forge && !neoForge;
    }
}
