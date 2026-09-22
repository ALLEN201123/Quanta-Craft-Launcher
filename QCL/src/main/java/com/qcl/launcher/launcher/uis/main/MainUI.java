package com.qcl.launcher.launcher.uis.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.graphics.drawable.BitmapDrawable;
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
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
import com.qcl.launcher.launcher.launch.check.LaunchTools;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
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

    private LinearLayout startGame;
    private TextView launchVersionText;

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

    private VersionSpinnerAdapter versionSpinnerAdapter;

    private ImageView versionListIcon;
    private ImageView downloadIcon;
    private ImageView multiplayerIcon;
    private ImageView settingIcon;

    /** ★ 1.2.3：FCL 同款 —— 版本装了哪个加载器就返回哪个的图标 */
    private Integer loaderIconFor(File versionDir) {
        try {
            String loader = ModLoaderDetector.detect(versionDir);
            if (ModLoaderDetector.MODLOADER.equals(loader)) return R.drawable.ic_modloader;
            if (ModLoaderDetector.BABRIC.equals(loader)) return R.drawable.ic_babric;
            if (ModLoaderDetector.FABRIC.equals(loader)) return R.drawable.ic_fabric;
            if (ModLoaderDetector.FORGE.equals(loader)) return R.drawable.ic_forge;
            if (ModLoaderDetector.NEOFORGE.equals(loader)) return R.drawable.ic_neoforge;
            if (ModLoaderDetector.QUILT.equals(loader)) return R.drawable.ic_quilt;
            if (ModLoaderDetector.LITELOADER.equals(loader)) return R.drawable.ic_modloader;
        } catch (Throwable ignored) {
        }
        return null;
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

        startGame = activity.findViewById(R.id.launcher_play_button);
        launchVersionText = activity.findViewById(R.id.launch_version_text);

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

        startGame.setOnClickListener(this);
        // ★★★ 1.1.1：长按启动按钮 → 选择渲染器（公共选择器，版本设置/全局设置共用同一套）
        startGame.setOnLongClickListener(v -> {
            com.qcl.launcher.launcher.launch.RendererPicker.show(activity,
                    activity.privateGameSetting,
                    activity.publicGameSetting.currentVersion, null);
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
        if (skinGLSurfaceView != null) skinGLSurfaceView.onResume();
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
                versionSpinnerAdapter = new VersionSpinnerAdapter(context,gameList);
                Spinner gameVersionSpinner = activity.findViewById(R.id.launcher_spinner_version);
                gameVersionSpinner.setAdapter(versionSpinnerAdapter);
                gameVersionSpinner.setSelection(versionSpinnerAdapter.getPosition(currentVersion));
                gameVersionSpinner.setOnItemSelectedListener(this);
                if (!currentVersion.name.equals("")){
                    noVersionAlert.setVisibility(View.GONE);
                    currentVersionText.setVisibility(View.VISIBLE);
                    currentVersionText.setText(currentVersion.name);
                    launchVersionText.setText(currentVersion.name);
                    if (!currentVersion.iconPath.equals("") && new File(currentVersion.iconPath).exists()) {
                        versionIcon.setBackground(DrawableUtils.getDrawableFromFile(currentVersion.iconPath));
                    }
                    else {
                        Integer li = loaderIconFor(new File(activity.launcherSetting.gameFileDirectory
                                + "/versions/" + currentVersion.name));
                        versionIcon.setBackground(context.getDrawable(li != null ? li : R.drawable.ic_grass));
                    }
                }
                else {
                    noVersionAlert.setVisibility(View.VISIBLE);
                    currentVersionText.setVisibility(View.GONE);
                    launchVersionText.setText(context.getString(R.string.launcher_button_current_version));
                    versionIcon.setBackground(context.getDrawable(R.drawable.ic_grass));
                }
            });
        }).start();

        refreshAccount();
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
                // A 64x32 file uses the classic (wide-arm) layout.
                showModel(skin, decodeCape(offline.capePath), skin.getHeight() != 32);
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
        if (skinGLSurfaceView != null) skinGLSurfaceView.onPause();
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
            com.qcl.launcher.launcher.terracotta.MultiplayerDialogHelper.showEnable(activity, context);
        }
        if (v == startSettingUI){
            activity.uiManager.switchMainUI(activity.uiManager.settingUI);
        }
        if (v == startGame){
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
            LaunchTools.launch(context,activity,activity.publicGameSetting.currentVersion,bundle);
        }
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        activity.publicGameSetting.currentVersion = activity.launcherSetting.gameFileDirectory + "/versions/" + ((GameListBean) versionSpinnerAdapter.getItem(position)).name;
        if (activity.privateGameSetting.gameDirSetting.type == 1){
            activity.uiManager.settingUI.settingUIManager.universalGameSettingUI.gameDirText.setText(activity.launcherSetting.gameFileDirectory + "/versions/" + ((GameListBean) versionSpinnerAdapter.getItem(position)).name);
        }
        GsonUtils.savePublicGameSetting(activity.publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        currentVersionText.setText(((GameListBean) versionSpinnerAdapter.getItem(position)).name);
        launchVersionText.setText(((GameListBean) versionSpinnerAdapter.getItem(position)).name);
        if (!((GameListBean) versionSpinnerAdapter.getItem(position)).iconPath.equals("") && new File(((GameListBean) versionSpinnerAdapter.getItem(position)).iconPath).exists()) {
            versionIcon.setBackground(DrawableUtils.getDrawableFromFile(((GameListBean) versionSpinnerAdapter.getItem(position)).iconPath));
        }
        else {
            if (!((GameListBean) versionSpinnerAdapter.getItem(position)).version.contains(",")) {
                versionIcon.setBackground(context.getDrawable(R.drawable.ic_grass));
            }
            else {
                versionIcon.setBackground(context.getDrawable(R.drawable.ic_furnace));
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
