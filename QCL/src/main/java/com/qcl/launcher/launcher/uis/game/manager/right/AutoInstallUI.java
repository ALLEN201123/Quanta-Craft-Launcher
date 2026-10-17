/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.content.Intent
 *  android.net.Uri
 *  android.os.Environment
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.ImageButton
 *  android.widget.ImageView
 *  android.widget.LinearLayout
 *  android.widget.TextView
 *  com.google.gson.Gson
 *  com.tungsten.filepicker.Constants$SELECTION_MODES
 *  com.tungsten.filepicker.FileChooser
 */
package com.qcl.launcher.launcher.uis.game.manager.right;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.GameInstallLocalDialog;
import com.qcl.launcher.launcher.download.babric.BabricInstallTask;
import com.qcl.launcher.launcher.download.OldLoaderInstallDialog;
import com.qcl.launcher.launcher.download.PatchMerger;
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
import com.qcl.launcher.launcher.download.modloader.ModLoaderVersions;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.platform.Bits;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import java.io.File;

import com.qcl.launcher.R;
public class AutoInstallUI
extends BaseUI
implements View.OnClickListener {
    private static final int SELECT_INSTALLER_REQUEST_CODE = 4700;
    public String versionName;
    private Version version;
    public LinearLayout autoInstallUI;
    private TextView gameVersionText;
    private TextView forgeVersionText;
    // ★ 1.4.2：NeoForge（automatic install 页）
    private TextView neoForgeVersionText;
    private ImageButton deleteNeoForgeVersion;
    private LinearLayout selectNeoForgeVersion;
    private ImageView selectNeoForge;
    private TextView liteLoaderVersionText;
    private TextView optiFineVersionText;
    private TextView fabricVersionText;
    private TextView fabricAPIVersionText;
    // ★ 1.5.2：Legacy Fabric（Fabric 为 MC 1.8.9~1.13.2 保留的旧分支）
    private TextView legacyFabricVersionText;
    private ImageButton deleteLegacyFabricVersion;
    private LinearLayout selectLegacyFabricVersion;
    private ImageView selectLegacyFabric;
    /** 已安装的 Legacy Fabric 加载器版本（null = 没装）。 */
    public String legacyFabricVersion;
    // ★ 1.5.2：Legacy Fabric API（Modrinth 项目 legacy-fabric-api）
    private TextView legacyFabricAPIVersionText;
    private ImageButton deleteLegacyFabricAPI;
    private LinearLayout selectLegacyFabricAPIVersion;
    private ImageView selectLegacyFabricAPI;
    /** 已安装的 Legacy Fabric API 版本（null = 没装）。 */
    public String legacyFabricAPIVersion;
    // ★ 1.5.2：Cleanroom（Forge 1.12.2 的社区继任分支）
    private TextView cleanroomVersionText;
    private ImageButton deleteCleanroom;
    private LinearLayout selectCleanroomVersion;
    private ImageView selectCleanroom;
    /** 已安装的 Cleanroom 版本（null = 没装）。 */
    public String cleanroomVersion;
    private TextView quiltVersionText;
    private TextView quiltAPIVersionText;
    private ImageButton deleteForgeVersion;
    private ImageButton deleteLiteLoaderVersion;
    private ImageButton deleteOptiFineVersion;
    private ImageButton deleteFabricVersion;
    private ImageButton deleteQuiltVersion;
    private LinearLayout selectForgeVersion;
    private LinearLayout selectLiteLoaderVersion;
    private LinearLayout selectOptiFineVersion;
    private LinearLayout selectFabricVersion;
    private LinearLayout selectFabricAPIVersion;
    private LinearLayout selectQuiltVersion;
    private LinearLayout selectQuiltAPIVersion;
    private ImageView selectForge;
    private ImageView selectLiteLoader;
    private ImageView selectOptiFine;
    private ImageView selectFabric;
    private ImageView selectFabricAPI;
    private ImageView selectQuilt;
    private ImageView selectQuiltAPI;
    private LinearLayout installLocal;
    public String gameVersion;
    public String forgeVersion;
    // ★ 1.4.2：NeoForge 版本（patch id "neoforge"）
    public String neoForgeVersion;
    public String optifineVersion;
    public String liteLoaderVersion;
    public String fabricVersion;
    public String quiltVersion;
    /** ★ 1.2.5：Babric（版本 json 里的 patch，version 就是加载器版本号） */
    public String babricVersion;
    /** ★ 1.2.5：这两个远古加载器不像 Forge 那样"选版本"，只显示 装/不装/不支持 */
    private TextView modLoaderVersionText;
    private TextView babricVersionText;
    private ImageButton deleteModLoaderVersion;
    private ImageButton deleteBabricVersion;
    private LinearLayout selectModLoaderVersion;
    private LinearLayout selectBabricVersion;
    private ImageView selectModLoader;
    private ImageView selectBabric;

    public AutoInstallUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.autoInstallUI = (LinearLayout)this.activity.findViewById(R.id.ui_auto_install);
        this.installLocal = (LinearLayout)this.activity.findViewById(R.id.install_from_local);
        this.installLocal.setOnClickListener((View.OnClickListener)this);
        this.gameVersionText = (TextView)this.activity.findViewById(R.id.current_minecraft_version_text);
        this.forgeVersionText = (TextView)this.activity.findViewById(R.id.current_forge_version_text);
        // ★ 1.4.2：NeoForge 行
        this.neoForgeVersionText = (TextView)this.activity.findViewById(R.id.current_neoforge_version_text);
        this.deleteNeoForgeVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_neoforge);
        this.deleteNeoForgeVersion.setOnClickListener((View.OnClickListener)this);
        this.liteLoaderVersionText = (TextView)this.activity.findViewById(R.id.current_liteloader_version_text);
        this.optiFineVersionText = (TextView)this.activity.findViewById(R.id.current_optifine_version_text);
        this.fabricVersionText = (TextView)this.activity.findViewById(R.id.current_fabric_version_text);
        this.fabricAPIVersionText = (TextView)this.activity.findViewById(R.id.current_fabric_api_version_text);
        // ★ 1.5.2：Legacy Fabric 那一行（布局里新加的）
        this.legacyFabricVersionText = (TextView)this.activity.findViewById(R.id.current_legacy_fabric_version_text);
        this.deleteLegacyFabricVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_legacy_fabric);
        this.selectLegacyFabricVersion = (LinearLayout)this.activity.findViewById(R.id.update_legacy_fabric_version);
        this.selectLegacyFabric = (ImageView)this.activity.findViewById(R.id.update_legacy_fabric);
        if (this.deleteLegacyFabricVersion != null) {
            this.deleteLegacyFabricVersion.setOnClickListener((View.OnClickListener)this);
        }
        if (this.selectLegacyFabricVersion != null) {
            this.selectLegacyFabricVersion.setOnClickListener((View.OnClickListener)this);
        }
        // ★ 1.5.2：Legacy Fabric API 那一行
        this.legacyFabricAPIVersionText = (TextView)this.activity.findViewById(R.id.current_legacy_fabric_api_version_text);
        this.deleteLegacyFabricAPI = (ImageButton)this.activity.findViewById(R.id.uninstall_legacy_fabric_api);
        this.selectLegacyFabricAPIVersion = (LinearLayout)this.activity.findViewById(R.id.update_legacy_fabric_api_version);
        this.selectLegacyFabricAPI = (ImageView)this.activity.findViewById(R.id.update_legacy_fabric_api);
        if (this.deleteLegacyFabricAPI != null) {
            this.deleteLegacyFabricAPI.setOnClickListener((View.OnClickListener)this);
        }
        if (this.selectLegacyFabricAPIVersion != null) {
            this.selectLegacyFabricAPIVersion.setOnClickListener((View.OnClickListener)this);
        }
        // ★ 1.5.2：Cleanroom 那一行
        this.cleanroomVersionText = (TextView)this.activity.findViewById(R.id.current_cleanroom_version_text);
        this.deleteCleanroom = (ImageButton)this.activity.findViewById(R.id.uninstall_cleanroom);
        this.selectCleanroomVersion = (LinearLayout)this.activity.findViewById(R.id.update_cleanroom_version);
        this.selectCleanroom = (ImageView)this.activity.findViewById(R.id.update_cleanroom);
        if (this.deleteCleanroom != null) {
            this.deleteCleanroom.setOnClickListener((View.OnClickListener)this);
        }
        if (this.selectCleanroomVersion != null) {
            this.selectCleanroomVersion.setOnClickListener((View.OnClickListener)this);
        }
        this.quiltVersionText = (TextView)this.activity.findViewById(R.id.current_quilt_version_text);
        this.quiltAPIVersionText = (TextView)this.activity.findViewById(R.id.current_quilt_api_version_text);
        this.deleteForgeVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_forge);
        this.deleteLiteLoaderVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_liteloader);
        this.deleteOptiFineVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_optifine);
        this.deleteFabricVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_fabric);
        this.deleteQuiltVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_quilt);
        this.deleteForgeVersion.setOnClickListener((View.OnClickListener)this);
        this.deleteLiteLoaderVersion.setOnClickListener((View.OnClickListener)this);
        this.deleteOptiFineVersion.setOnClickListener((View.OnClickListener)this);
        this.deleteFabricVersion.setOnClickListener((View.OnClickListener)this);
        this.deleteQuiltVersion.setOnClickListener((View.OnClickListener)this);
        this.selectForgeVersion = (LinearLayout)this.activity.findViewById(R.id.update_forge_version);
        // ★ 1.4.2：NeoForge 选择行
        this.selectNeoForgeVersion = (LinearLayout)this.activity.findViewById(R.id.update_neoforge_version);
        this.selectNeoForgeVersion.setOnClickListener((View.OnClickListener)this);
        this.selectLiteLoaderVersion = (LinearLayout)this.activity.findViewById(R.id.update_liteloader_version);
        this.selectOptiFineVersion = (LinearLayout)this.activity.findViewById(R.id.update_optifine_version);
        this.selectFabricVersion = (LinearLayout)this.activity.findViewById(R.id.update_fabric_version);
        this.selectFabricAPIVersion = (LinearLayout)this.activity.findViewById(R.id.update_fabric_api_version);
        this.selectQuiltVersion = (LinearLayout)this.activity.findViewById(R.id.update_quilt_version);
        this.selectQuiltAPIVersion = (LinearLayout)this.activity.findViewById(R.id.update_quilt_api_version);
        this.selectForgeVersion.setOnClickListener((View.OnClickListener)this);
        this.selectLiteLoaderVersion.setOnClickListener((View.OnClickListener)this);
        this.selectOptiFineVersion.setOnClickListener((View.OnClickListener)this);
        this.selectFabricVersion.setOnClickListener((View.OnClickListener)this);
        this.selectFabricAPIVersion.setOnClickListener((View.OnClickListener)this);
        this.selectQuiltVersion.setOnClickListener((View.OnClickListener)this);
        this.selectQuiltAPIVersion.setOnClickListener((View.OnClickListener)this);
        this.selectForge = (ImageView)this.activity.findViewById(R.id.update_forge);
        // ★ 1.4.2：NeoForge 箭头
        this.selectNeoForge = (ImageView)this.activity.findViewById(R.id.update_neoforge);
        this.selectLiteLoader = (ImageView)this.activity.findViewById(R.id.update_lite_loader);
        this.selectOptiFine = (ImageView)this.activity.findViewById(R.id.update_optifine);
        this.selectFabric = (ImageView)this.activity.findViewById(R.id.update_fabric);
        this.selectFabricAPI = (ImageView)this.activity.findViewById(R.id.update_fabric_api);
        this.selectQuilt = (ImageView)this.activity.findViewById(R.id.update_quilt);
        this.selectQuiltAPI = (ImageView)this.activity.findViewById(R.id.update_quilt_api);

        // ★ 1.2.5：ModLoader / Babric 两行（远古版本专用的加载器，
        //   原来只有「下载 → 安装游戏」页能勾，版本设置里没有入口）
        this.modLoaderVersionText = (TextView)this.activity.findViewById(R.id.current_modloader_version_text);
        this.babricVersionText = (TextView)this.activity.findViewById(R.id.current_babric_version_text);
        this.deleteModLoaderVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_modloader);
        this.deleteBabricVersion = (ImageButton)this.activity.findViewById(R.id.uninstall_babric);
        this.selectModLoaderVersion = (LinearLayout)this.activity.findViewById(R.id.update_modloader_version);
        this.selectBabricVersion = (LinearLayout)this.activity.findViewById(R.id.update_babric_version);
        this.selectModLoader = (ImageView)this.activity.findViewById(R.id.update_modloader);
        this.selectBabric = (ImageView)this.activity.findViewById(R.id.update_babric);
        if (this.deleteModLoaderVersion != null) {
            this.deleteModLoaderVersion.setOnClickListener((View.OnClickListener)this);
        }
        if (this.deleteBabricVersion != null) {
            this.deleteBabricVersion.setOnClickListener((View.OnClickListener)this);
        }
        if (this.selectModLoaderVersion != null) {
            this.selectModLoaderVersion.setOnClickListener((View.OnClickListener)this);
        }
        if (this.selectBabricVersion != null) {
            this.selectBabricVersion.setOnClickListener((View.OnClickListener)this);
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft((View)this.autoInstallUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.gameManagerUI.startAutoInstall.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft((View)this.autoInstallUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.gameManagerUI.startAutoInstall.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4700 && resultCode == -1 && data != null) {
            Uri uri = data.getData();
            GameInstallLocalDialog dialog = new GameInstallLocalDialog(this.context, this.activity, this.versionName, this.gameVersion, UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
            dialog.show();
        }
    }

    public void refresh(String versionName) {
        this.versionName = versionName;
        this.forgeVersion = null;
        this.neoForgeVersion = null;
        this.liteLoaderVersion = null;
        this.optifineVersion = null;
        this.fabricVersion = null;
        this.quiltVersion = null;
        this.babricVersion = null;
        String gameJsonText = FileStringUtils.getStringFromFile(this.activity.launcherSetting.gameFileDirectory + "/versions/" + versionName + "/" + versionName + ".json");
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
        this.version = (Version)gson.fromJson(gameJsonText, Version.class);
        if (this.version.getPatches() != null && this.version.getPatches().size() > 0) {
            for (Version p : this.version.getPatches()) {
                switch (p.getId()) {
                    case "game": {
                        this.gameVersion = p.getVersion();
                        break;
                    }
                    case "forge": {
                        this.forgeVersion = p.getVersion();
                        break;
                    }
                    case "neoforge": {
                        this.neoForgeVersion = p.getVersion();
                        break;
                    }
                    case "optifine": {
                        this.optifineVersion = p.getVersion();
                        break;
                    }
                    case "fabric": {
                        this.fabricVersion = p.getVersion();
                        break;
                    }
                    case "quilt": {
                        this.quiltVersion = p.getVersion();
                        break;
                    }
                    case "liteloader": {
                        this.liteLoaderVersion = p.getVersion();
                    }
                }
            }
        } else {
            this.gameVersion = this.version.getId();
        }
        this.refreshView();
        // ★ 1.5.2：Legacy Fabric 那一行的状态（放在最后统一刷，不插手上面那串互斥 if-else）
        this.refreshLegacyFabricRow();
    }

    /**
     * ★★★★★ 2026-10-17（1.5.2 新增）刷新 Legacy Fabric 那一行。
     *
     * <p>★ 诚实说明：Legacy Fabric 装出来就是标准的 {@code fabric} patch
     * （它本身就是 Fabric 官方为老版本保留的分支），所以这里**无法**从版本 json
     * 里区分「装的是官方 Fabric 还是 Legacy Fabric」——
     * 因此本行的状态与 Fabric 行保持一致：任一方装了都显示版本号。
     * 想换另一方，先点 × 卸载再装。
     */
    private void refreshLegacyFabricRow() {
        if (this.legacyFabricVersionText == null) {
            return;
        }
        try {
            boolean installed = this.fabricVersion != null;
            if (installed) {
                this.legacyFabricVersion = this.fabricVersion;
                this.legacyFabricVersionText.setText(this.fabricVersion);
            } else if (this.forgeVersion != null || this.optifineVersion != null
                    || this.quiltVersion != null) {
                this.legacyFabricVersionText.setText(
                        this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            } else {
                this.legacyFabricVersionText.setText(
                        this.context.getString(R.string.install_game_ui_none));
            }
            if (this.deleteLegacyFabricVersion != null) {
                this.deleteLegacyFabricVersion.setVisibility(installed ? 0 : 8);
            }
            if (this.selectLegacyFabric != null) {
                this.selectLegacyFabric.setVisibility(installed ? 8 : 0);
            }
        } catch (Throwable ignored) {
        }
        // ★ 1.5.2：Legacy Fabric API 那一行（它是个普通 mod，跟加载器状态无关）
        try {
            if (this.legacyFabricAPIVersionText != null) {
                boolean apiInstalled = this.legacyFabricAPIVersion != null;
                if (apiInstalled) {
                    this.legacyFabricAPIVersionText.setText(this.legacyFabricAPIVersion);
                } else {
                    this.legacyFabricAPIVersionText.setText(
                            this.context.getString(R.string.install_game_ui_none));
                }
                if (this.deleteLegacyFabricAPI != null) {
                    this.deleteLegacyFabricAPI.setVisibility(apiInstalled ? 0 : 8);
                }
                if (this.selectLegacyFabricAPI != null) {
                    this.selectLegacyFabricAPI.setVisibility(apiInstalled ? 8 : 0);
                }
            }
        } catch (Throwable ignored) {
        }
        // ★ 1.5.2：Cleanroom 那一行
        try {
            if (this.cleanroomVersionText != null) {
                boolean crInstalled = this.cleanroomVersion != null;
                if (crInstalled) {
                    this.cleanroomVersionText.setText(this.cleanroomVersion);
                } else if (!"1.12.2".equals(this.gameVersion)) {
                    this.cleanroomVersionText.setText("仅支持 1.12.2");
                } else {
                    this.cleanroomVersionText.setText(
                            this.context.getString(R.string.install_game_ui_none));
                }
                if (this.deleteCleanroom != null) {
                    this.deleteCleanroom.setVisibility(crInstalled ? 0 : 8);
                }
                if (this.selectCleanroom != null) {
                    this.selectCleanroom.setVisibility(crInstalled ? 8 : 0);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    private void refreshView() {
        this.gameVersionText.setText((CharSequence)this.gameVersion);
        if (this.forgeVersion != null || this.optifineVersion != null) {
            this.forgeVersionText.setText((CharSequence)(this.forgeVersion == null ? this.context.getString(R.string.install_game_ui_none) : this.forgeVersion));
            this.optiFineVersionText.setText((CharSequence)(this.optifineVersion == null ? this.context.getString(R.string.install_game_ui_none) : this.optifineVersion));
            this.fabricVersionText.setText((CharSequence)(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible)));
            this.fabricAPIVersionText.setText((CharSequence)(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible)));
            this.quiltVersionText.setText((CharSequence)(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible)));
            this.quiltAPIVersionText.setText((CharSequence)(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible)));
            this.deleteForgeVersion.setVisibility(this.forgeVersion != null ? 0 : 8);
            this.deleteOptiFineVersion.setVisibility(this.optifineVersion != null ? 0 : 8);
            this.selectFabric.setVisibility(8);
            this.selectFabricAPI.setVisibility(8);
            this.selectQuilt.setVisibility(8);
            this.selectQuiltAPI.setVisibility(8);
        } else {
            this.forgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.optiFineVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.fabricVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.fabricAPIVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.quiltVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.quiltAPIVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.deleteForgeVersion.setVisibility(8);
            this.deleteOptiFineVersion.setVisibility(8);
            this.selectFabric.setVisibility(0);
            this.selectFabricAPI.setVisibility(0);
            this.selectQuilt.setVisibility(0);
            this.selectQuiltAPI.setVisibility(0);
        }
        if (this.fabricVersion != null) {
            this.forgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.optiFineVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.liteLoaderVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.quiltVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.quiltAPIVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.fabricVersionText.setText((CharSequence)this.fabricVersion);
            this.deleteFabricVersion.setVisibility(0);
            this.selectForge.setVisibility(8);
            this.selectLiteLoader.setVisibility(8);
            this.selectOptiFine.setVisibility(8);
            this.selectQuilt.setVisibility(8);
            this.selectQuiltAPI.setVisibility(8);
        } else {
            if (this.quiltVersion != null) {
                this.forgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.optiFineVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.liteLoaderVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.fabricVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.fabricAPIVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.quiltVersionText.setText((CharSequence)this.quiltVersion);
                this.deleteQuiltVersion.setVisibility(0);
                this.selectForge.setVisibility(8);
                this.selectLiteLoader.setVisibility(8);
                this.selectOptiFine.setVisibility(8);
                this.selectFabric.setVisibility(8);
                this.selectFabricAPI.setVisibility(8);
            } else {
                if (this.forgeVersion == null) {
                    this.forgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
                }
                if (this.optifineVersion == null) {
                    this.optiFineVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
                }
                if (this.liteLoaderVersion == null) {
                    this.liteLoaderVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
                }
                this.deleteQuiltVersion.setVisibility(8);
                this.selectForge.setVisibility(0);
                this.selectLiteLoader.setVisibility(0);
                this.selectOptiFine.setVisibility(0);
                if (this.forgeVersion == null && this.optifineVersion == null) {
                    this.selectFabric.setVisibility(0);
                    this.selectFabricAPI.setVisibility(0);
                }
            }
            this.deleteFabricVersion.setVisibility(8);
        }
        if (this.liteLoaderVersion != null) {
            this.liteLoaderVersionText.setText((CharSequence)this.liteLoaderVersion);
            this.deleteLiteLoaderVersion.setVisibility(0);
        } else {
            if (this.fabricVersion == null && this.quiltVersion == null) {
                this.liteLoaderVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            }
            this.deleteLiteLoaderVersion.setVisibility(8);
        }
        this.selectForge.setBackground(this.deleteForgeVersion.getVisibility() == 0 ? this.context.getDrawable(R.drawable.ic_baseline_update_black) : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));
        // ★ 1.4.2：NeoForge 箭头
        if (this.selectNeoForge != null && this.deleteNeoForgeVersion != null) {
            this.selectNeoForge.setBackground(this.deleteNeoForgeVersion.getVisibility() == 0 ? this.context.getDrawable(R.drawable.ic_baseline_update_black) : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));
        }
        this.selectLiteLoader.setBackground(this.deleteLiteLoaderVersion.getVisibility() == 0 ? this.context.getDrawable(R.drawable.ic_baseline_update_black) : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));
        this.selectOptiFine.setBackground(this.deleteOptiFineVersion.getVisibility() == 0 ? this.context.getDrawable(R.drawable.ic_baseline_update_black) : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));
        this.selectFabric.setBackground(this.deleteFabricVersion.getVisibility() == 0 ? this.context.getDrawable(R.drawable.ic_baseline_update_black) : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));
        this.selectQuilt.setBackground(this.deleteQuiltVersion.getVisibility() == 0 ? this.context.getDrawable(R.drawable.ic_baseline_update_black) : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));

        // ★ 1.2.5：ModLoader / Babric 两行的状态（它们没有"可选版本"，只显示 装/不装/不支持）
        File loaderVersionDir = new File(this.activity.launcherSetting.gameFileDirectory
                + "/versions/" + this.versionName);
        boolean hasModLoader = ModLoaderDetector.hasModLoader(loaderVersionDir);
        boolean hasBabric = ModLoaderDetector.hasBabric(loaderVersionDir);
        // ★ 1.2.5：老版本（1.2.4 及以前）装的 Babric 留下的是 0 字节标记文件，
        //   打开这一页时顺手补上内容，玩家不会再看到「0B 文件」
        if (hasBabric) {
            BabricInstallTask.repairEmptyMarker(loaderVersionDir, this.gameVersion, this.babricVersion);
        }
        if (this.modLoaderVersionText != null) {
            if (hasModLoader) {
                this.modLoaderVersionText.setText(this.context.getString(R.string.install_game_ui_modloader_installed));
            } else if (this.forgeVersion != null) {
                // Forge 与 ModLoader 互斥（1.3 起 FML 已包含 RML，叠一起会崩）
                this.modLoaderVersionText.setText(this.context.getString(R.string.install_game_ui_modloader_not_compatible));
            } else {
                ModLoaderVersions.Entry entry = ModLoaderVersions.find(this.gameVersion);
                this.modLoaderVersionText.setText(this.context.getString(
                        (entry == null || !entry.isInstallable())
                                ? R.string.install_game_ui_modloader_not_supported
                                : R.string.install_game_ui_none));
            }
            if (this.deleteModLoaderVersion != null) {
                this.deleteModLoaderVersion.setVisibility(hasModLoader ? 0 : 8);
            }
            if (this.selectModLoader != null) {
                this.selectModLoader.setBackground(this.context.getDrawable(
                        hasModLoader ? R.drawable.ic_baseline_update_black : R.drawable.ic_baseline_arrow_forward_black));
            }
        }
        if (this.babricVersionText != null) {
            // Babric 的 meta 只有 b1.7.3 这一个游戏版本，别的版本直接写"仅 b1.7.3"
            boolean onlyB173 = "b1.7.3".equalsIgnoreCase(this.gameVersion == null ? "" : this.gameVersion.trim());
            if (hasBabric) {
                this.babricVersionText.setText(this.babricVersion == null ? "Babric" : this.babricVersion);
            } else if (!onlyB173) {
                this.babricVersionText.setText(this.context.getString(R.string.install_game_ui_babric_only_b173));
            } else {
                this.babricVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            }
            if (this.deleteBabricVersion != null) {
                this.deleteBabricVersion.setVisibility(hasBabric ? 0 : 8);
            }
            if (this.selectBabric != null) {
                this.selectBabric.setBackground(this.context.getDrawable(
                        hasBabric ? R.drawable.ic_baseline_update_black : R.drawable.ic_baseline_arrow_forward_black));
            }
        }
        // ★★★ 1.4.2：NeoForge 互斥「最后覆盖层」——必须放在 refreshView() 最末尾。
        //   上面的 Forge/Fabric/Quilt 分支是层层覆盖式的，插在中间会被后面冲掉 → 表现成「没有互斥」。
        //   依据 FCL InstallerItemGroup：NeoForge 与 Forge/Fabric/Quilt/OptiFine/LiteLoader 全互斥。
        applyNeoForgeExclusion();
    }

    /**
     * ★ 1.4.2：应用 NeoForge 的互斥显示（同 InstallGameUI.applyNeoForgeExclusion）。
     * ① 已选 NeoForge → 其它加载器行显示「Incompatible with NeoForge」且隐藏箭头；
     * ② 其它加载器已选 → NeoForge 行显示「Incompatible with XXX」且隐藏箭头。
     * 只在对方那一行自己没被选中时才改文案，避免盖掉已选行的真实版本号。
     */
    private void applyNeoForgeExclusion() {
        if (this.neoForgeVersionText == null) {
            return;
        }
        String incompatible = this.context.getString(R.string.install_game_ui_neoforge_not_compatible);
        if (this.neoForgeVersion != null) {
            this.deleteNeoForgeVersion.setVisibility(0);
            this.selectNeoForge.setVisibility(8);
            if (this.forgeVersion == null) {
                this.forgeVersionText.setText((CharSequence)incompatible);
                this.selectForge.setVisibility(8);
            }
            if (this.optifineVersion == null) {
                this.optiFineVersionText.setText((CharSequence)incompatible);
                this.selectOptiFine.setVisibility(8);
            }
            if (this.liteLoaderVersion == null) {
                this.liteLoaderVersionText.setText((CharSequence)incompatible);
                this.selectLiteLoader.setVisibility(8);
            }
            if (this.fabricVersion == null) {
                this.fabricVersionText.setText((CharSequence)incompatible);
                this.selectFabric.setVisibility(8);
            }
            if (this.quiltVersion == null) {
                this.quiltVersionText.setText((CharSequence)incompatible);
                this.selectQuilt.setVisibility(8);
            }
            // FabricAPI / QuiltAPI：AutoInstallUI 无对应版本字段，按 Fabric/Quilt 是否已选跟随
            if (this.fabricVersion == null) {
                this.fabricAPIVersionText.setText((CharSequence)incompatible);
                this.selectFabricAPI.setVisibility(8);
            }
            if (this.quiltVersion == null) {
                this.quiltAPIVersionText.setText((CharSequence)incompatible);
                this.selectQuiltAPI.setVisibility(8);
            }
            return;
        }
        // 未选 NeoForge：按对方已选项显示不兼容，否则显示「不安装」
        this.deleteNeoForgeVersion.setVisibility(8);
        if (this.forgeVersion != null) {
            this.neoForgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.selectNeoForge.setVisibility(8);
        } else if (this.fabricVersion != null) {
            this.neoForgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.selectNeoForge.setVisibility(8);
        } else if (this.quiltVersion != null) {
            this.neoForgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_quilt_not_compatible));
            this.selectNeoForge.setVisibility(8);
        } else if (this.optifineVersion != null) {
            this.neoForgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_optifine_not_compatible));
            this.selectNeoForge.setVisibility(8);
        } else if (this.liteLoaderVersion != null) {
            // QCL 无 liteLoader_not_compatible 字符串，复用 Forge 版文案（与 InstallGameUI 一致）
            this.neoForgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.selectNeoForge.setVisibility(8);
        } else {
            this.neoForgeVersionText.setText((CharSequence)this.context.getString(R.string.install_game_ui_none));
            this.selectNeoForge.setVisibility(0);
        }
        // 箭头图标随可见性刷新
        if (this.selectNeoForge != null && this.deleteNeoForgeVersion != null) {
            this.selectNeoForge.setBackground(this.deleteNeoForgeVersion.getVisibility() == 0
                    ? this.context.getDrawable(R.drawable.ic_baseline_update_black)
                    : this.context.getDrawable(R.drawable.ic_baseline_arrow_forward_black));
        }
    }

    public void uninstall(String id2) {
        this.version = PatchMerger.reMergePatch(this.context, this.version, null, id2, () -> {});
        String path = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/" + this.versionName + ".json";
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
        String s = gson.toJson((Object)this.version);
        FileStringUtils.writeFile(path, s);
        this.refresh(this.versionName);
        new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start();
    }

    public void onClick(View view) {
        if (view == this.installLocal) {
            Intent intent = new Intent(this.context, FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "jar");
            intent.putExtra("INITIAL_DIRECTORY", new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            this.activity.startActivityForResult(intent, 4700);
        }
        if (view == this.deleteForgeVersion && this.forgeVersion != null) {
            this.forgeVersion = null;
            this.uninstall("forge");
        }
        // ★ 1.4.2：NeoForge 卸载 / 选择
        if (view == this.deleteNeoForgeVersion && this.neoForgeVersion != null) {
            this.neoForgeVersion = null;
            this.uninstall("neoforge");
        }
        if (view == this.deleteLiteLoaderVersion && this.liteLoaderVersion != null) {
            this.liteLoaderVersion = null;
            this.uninstall("liteloader");
        }
        if (view == this.deleteOptiFineVersion && this.optifineVersion != null) {
            this.optifineVersion = null;
            this.uninstall("optifine");
        }
        if (view == this.deleteFabricVersion && this.fabricVersion != null) {
            this.fabricVersion = null;
            this.uninstall("fabric");
        }
        if (view == this.deleteQuiltVersion && this.quiltVersion != null) {
            this.quiltVersion = null;
            this.uninstall("quilt");
        }
        // ★★★★★ 2026-10-16 **修「自制的那两个模组加载器点了没效果、根本无法安装」**。
        //
        //   用户实测：「还有自制的那两个模组加载器，点了没效果，根本无法安装」。
        //
        //   ★ 根因：**控件绑了监听，但 onClick 里根本没有对应分支**。
        //     构造函数里明明做了：
        //       <pre>
        //         this.selectModLoaderVersion.setOnClickListener((View.OnClickListener)this);
        //         this.selectBabricVersion.setOnClickListener((View.OnClickListener)this);
        //         this.deleteModLoaderVersion.setOnClickListener((View.OnClickListener)this);
        //         this.deleteBabricVersion.setOnClickListener((View.OnClickListener)this);
        //       </pre>
        //     而 `onClick()` 从 L516 到末尾只处理了
        //     installLocal / forge / neoforge / liteloader / optifine / fabric / quilt，
        //     **ModLoader 与 Babric 一条都没有** ⇒ 点击进到 onClick 后什么都不匹配
        //     ⇒ 无反应、无提示 ⇒ 用户看到的就是"点了没效果，根本无法安装"。
        //
        //   ★ 修法：把这两个「QCL 自制加载器」的安装/卸载分支补齐。
        //     · ModLoader 是 Risugami's，往 minecraft.jar 里注入 class；
        //     · Babric 是 b1.7.3 的 Fabric 分支，下一个加载器 + 改 mainClass + 写 .babric 标记。
        //     两者的安装任务都允许 adapter 传 null（已在任务类里补判空），
        //     因为自动安装页没有「下载任务列表」。
        if (view == this.deleteModLoaderVersion) {
            this.removeLoaderMarker("modloader");
        }
        if (view == this.deleteBabricVersion) {
            this.removeLoaderMarker("babric");
        }
        if (view == this.selectModLoaderVersion) {
            this.installModLoaderHere();
        }
        if (view == this.selectBabricVersion) {
            this.installBabricHere();
        }
        // ★★★★★ 2026-10-16 修「自动安装选 Forge 没反应 / 不能自己装」（评测视频 60s）。
        //
        //   用户实测：「去到版本设置里面的自动安装，选择 forge，还不能自己安装，里面全是空白的问题」。
        //
        //   ★ 根因：这些分支原来都带一个**静默守卫** `fabricVersion == null && quiltVersion == null`。
        //     只要该实例装过 Fabric 或 Quilt，**点 Forge / NeoForge / LiteLoader / OptiFine
        //     的安装按钮就什么都不发生** —— 没有任何提示，用户只看到"点了没用"。
        //     （守卫本身有道理：Forge 与 Fabric 不能共存于同一个版本目录。
        //       但**不能静默失败**，必须告诉用户"为什么"以及"怎么办"。）
        //
        //   ⇒ 现在：能装就装；不能装就弹一条明确提示，说明冲突原因 + 建议做法。
        if (view == this.selectForgeVersion) {
            if (this.fabricVersion == null && this.quiltVersion == null) {
                this.activity.uiManager.downloadForgeUI.version = this.gameVersion;
                this.activity.uiManager.downloadForgeUI.install = true;
                this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadForgeUI);
            } else {
                toastConflict("Forge", this.fabricVersion != null ? "Fabric" : "Quilt");
            }
        }
        if (view == this.selectNeoForgeVersion) {
            if (this.fabricVersion == null && this.quiltVersion == null) {
                this.activity.uiManager.downloadNeoForgeUI.version = this.gameVersion;
                this.activity.uiManager.downloadNeoForgeUI.install = true;
                this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadNeoForgeUI);
            } else {
                toastConflict("NeoForge", this.fabricVersion != null ? "Fabric" : "Quilt");
            }
        }
        if (view == this.selectLiteLoaderVersion) {
            if (this.fabricVersion == null && this.quiltVersion == null) {
                this.activity.uiManager.downloadLiteLoaderUI.version = this.gameVersion;
                this.activity.uiManager.downloadLiteLoaderUI.install = true;
                this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadLiteLoaderUI);
            } else {
                toastConflict("LiteLoader", this.fabricVersion != null ? "Fabric" : "Quilt");
            }
        }
        if (view == this.selectOptiFineVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadOptifineUI.version = this.gameVersion;
            this.activity.uiManager.downloadOptifineUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadOptifineUI);
        }
        if (view == this.selectFabricVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadFabricUI.version = this.gameVersion;
            this.activity.uiManager.downloadFabricUI.install = true;
            // ★ 1.5.2：从 Fabric 行进来 → 官方 Fabric
            this.activity.uiManager.downloadFabricUI.legacy = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricUI);
        }
        // ★★★★★ 2026-10-17（1.5.2 新增）Legacy Fabric 入口。
        //   Legacy Fabric 与官方 Fabric 互斥（同一个 patch id = fabric），
        //   所以已装 Fabric 时点它要先卸载，这里给明确提示而不是静默不响应。
        if (view == this.selectLegacyFabricVersion) {
            if (this.forgeVersion != null || this.optifineVersion != null || this.quiltVersion != null) {
                toastConflict("Legacy Fabric",
                        this.forgeVersion != null ? "Forge"
                                : (this.optifineVersion != null ? "OptiFine" : "Quilt"));
            } else if (this.fabricVersion != null) {
                android.widget.Toast.makeText(this.context,
                        "当前版本已安装 Fabric 加载器（Legacy Fabric 与它互斥）。"
                                + "请先点右侧 × 卸载，再装 Legacy Fabric。",
                        android.widget.Toast.LENGTH_LONG).show();
            } else {
                this.activity.uiManager.downloadFabricUI.version = this.gameVersion;
                this.activity.uiManager.downloadFabricUI.install = true;
                // ★ 关键：走 Legacy Fabric 的 meta 域名
                this.activity.uiManager.downloadFabricUI.legacy = true;
                this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricUI);
            }
        }
        if (view == this.deleteLegacyFabricVersion) {
            // Legacy Fabric 装出来就是 fabric patch，所以按 fabric 卸载
            this.legacyFabricVersion = null;
            this.fabricVersion = null;
            this.uninstall("fabric");
        }
        // ★ 1.5.2：Legacy Fabric API（Modrinth 项目 legacy-fabric-api）
        if (view == this.selectLegacyFabricAPIVersion) {
            if (this.forgeVersion != null || this.optifineVersion != null || this.quiltVersion != null) {
                toastConflict("Legacy Fabric API",
                        this.forgeVersion != null ? "Forge"
                                : (this.optifineVersion != null ? "OptiFine" : "Quilt"));
            } else {
                this.activity.uiManager.downloadFabricAPIUI.version = this.gameVersion;
                this.activity.uiManager.downloadFabricAPIUI.install = true;
                // ★ 关键：列 Legacy Fabric API（legacy-fabric-api）的版本
                this.activity.uiManager.downloadFabricAPIUI.legacy = true;
                this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricAPIUI);
            }
        }
        if (view == this.deleteLegacyFabricAPI) {
            this.legacyFabricAPIVersion = null;
            this.uninstall("legacyfabric-api");
        }
        // ★ 1.5.2：Cleanroom —— 点击弹出**版本选择对话框**（能选旧版本）
        if (view == this.selectCleanroomVersion) {
            this.showCleanroomVersionPicker();
        }
        if (view == this.deleteCleanroom) {
            this.cleanroomVersion = null;
            this.uninstall("cleanroom");
        }
        if (view == this.selectFabricAPIVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadFabricAPIUI.version = this.gameVersion;
            this.activity.uiManager.downloadFabricAPIUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricAPIUI);
        }
        if (view == this.selectQuiltVersion && this.forgeVersion == null && this.optifineVersion == null && this.fabricVersion == null) {
            this.activity.uiManager.downloadQuiltUI.version = this.gameVersion;
            this.activity.uiManager.downloadQuiltUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadQuiltUI);
        }
        if (view == this.selectQuiltAPIVersion && this.forgeVersion == null && this.optifineVersion == null && this.fabricVersion == null) {
            this.activity.uiManager.downloadQuiltAPIUI.version = this.gameVersion;
            this.activity.uiManager.downloadQuiltAPIUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadQuiltAPIUI);
        }
    }

    /**
     * ★★★★★ 2026-10-16 新增：模组加载器**冲突提示**。
     *
     * <p>为什么需要（用户实测原话）：「在1.20.1整合包那里，自动安装那里选择 forge，
     * 点进去，然后进入详情页，里面没有他的列表，只有没有任何安装列表的提示词摆在中间。
     * **还不能自己装**」。
     *
     * <p>原来这些安装箭头都带一个**静默守卫**
     * {@code fabricVersion == null && quiltVersion == null}：
     * 只要该实例装过 Fabric / Quilt，点 Forge / NeoForge / LiteLoader 的安装箭头
     * **什么都不发生、也没有任何提示** ⇒ 用户以为"点不动 / 就是装不了"。
     *
     * <p>守卫本身是对的（Forge 与 Fabric 不能共存于同一个版本目录），
     * 但**必须把原因告诉用户**，否则表现为"按钮坏了"。
     *
     * @param want      用户想装的加载器（Forge / NeoForge / LiteLoader）
     * @param installed 当前已装、导致冲突的加载器（Fabric / Quilt）
     */
    private void toastConflict(String want, String installed) {
        try {
            String msg = "当前版本已安装 " + installed + "，无法再装 " + want
                    + "（二者不能共存）。请先卸载 " + installed
                    + "，或新建一个版本再装 " + want + "。";
            android.widget.Toast.makeText(this.context, msg,
                    android.widget.Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
        }
    }

    // ======================================================================
    // ★★★★★ 2026-10-16 新增：**自制加载器（ModLoader / Babric）在自动安装页的安装与卸载**
    //
    //   用户实测：「自制的那两个模组加载器，点了没效果，根本无法安装」。
    //   根因是 onClick 里没有对应分支（控件绑了监听却没人处理）——已在 onClick 补上分支，
    //   这里补上真正干活的三个方法。
    // ======================================================================

    /**
     * 在**当前这个版本**上安装 Risugami's ModLoader。
     *
     * <p>ModLoader 是往 {@code minecraft.jar} 里**注入 class**，并写一个 `.modloader` 标记文件
     * （{@code ModLoaderDetector} 靠它认出「装了 ModLoader」）。安装任务允许 adapter 传 null。
     */
    private void installModLoaderHere() {
        try {
            // 已有 Forge 时不允许再装 RML：1.3 起 FML 已包含 RML，叠一起会崩
            if (this.forgeVersion != null) {
                toastConflict("ModLoader", "Forge");
                return;
            }
            // 先确认这个 MC 版本有没有对应的 ModLoader 包
            com.qcl.launcher.launcher.download.modloader.ModLoaderVersions.Entry entry =
                    com.qcl.launcher.launcher.download.modloader.ModLoaderVersions.find(this.gameVersion);
            if (entry == null) {
                android.widget.Toast.makeText(this.context,
                        "ModLoader 不支持 Minecraft " + this.gameVersion + "（只覆盖远古版本）",
                        android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            android.widget.Toast.makeText(this.context, "正在安装 ModLoader…",
                    android.widget.Toast.LENGTH_SHORT).show();

            new com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask(
                    this.activity,
                    this.versionName,      // 版本目录名
                    this.gameVersion,      // 查 ModLoader 表用的 MC 版本
                    null,                  // ★ 自动安装页没有下载任务列表
                    new com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask.Callback() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onFinish(int injected) {
                            AutoInstallUI.this.onLoaderInstallDone(
                                    "ModLoader 安装完成（写入 " + injected + " 个 class）");
                        }

                        @Override
                        public void onFailed(Exception e) {
                            AutoInstallUI.this.onLoaderInstallFailed("ModLoader", e);
                        }
                    }).execute();
        } catch (Throwable t) {
            onLoaderInstallFailed("ModLoader", t instanceof Exception ? (Exception) t : new Exception(t));
        }
    }

    /**
     * 在**当前这个版本**上安装 Babric（b1.7.3 的 Fabric 分支）。
     *
     * <p>Babric 只支持 b1.7.3；装完会写 `.babric` 标记（不写 0 字节文件，避免看着像坏文件）。
     */
    private void installBabricHere() {
        try {
            if (!"b1.7.3".equals(this.gameVersion)) {
                android.widget.Toast.makeText(this.context,
                        "Babric 只支持 b1.7.3，当前版本是 " + this.gameVersion,
                        android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            android.widget.Toast.makeText(this.context, "正在安装 Babric…",
                    android.widget.Toast.LENGTH_SHORT).show();

            final java.io.File dir = new java.io.File(
                    this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName);

            new com.qcl.launcher.launcher.download.babric.BabricInstallTask(
                    this.activity,
                    null,                  // ★ 自动安装页没有下载任务列表
                    this.gameVersion,
                    new com.qcl.launcher.launcher.download.babric.BabricInstallTask.InstallBabricCallback() {
                        @Override
                        public void onStart() {
                        }

                        @Override
                        public void onFailed(Exception e) {
                            AutoInstallUI.this.onLoaderInstallFailed("Babric", e);
                        }

                        @Override
                        public void onFinish(com.qcl.launcher.launcher.game.Version version) {
                            try {
                                // 写标记，让 ModLoaderDetector 认出这是 Babric
                                // writeMarker(File versionDir, String mcVersion, String loaderVersion, int libraryCount)
                                com.qcl.launcher.launcher.download.babric.BabricInstallTask.writeMarker(
                                        dir,
                                        AutoInstallUI.this.gameVersion,
                                        version == null ? null : version.getVersion(),
                                        (version == null || version.getLibraries() == null)
                                                ? 0 : version.getLibraries().size());
                            } catch (Throwable ignored) {
                            }
                            AutoInstallUI.this.onLoaderInstallDone("Babric 安装完成");
                        }
                    }).execute(new String[]{null});
        } catch (Throwable t) {
            onLoaderInstallFailed("Babric", t instanceof Exception ? (Exception) t : new Exception(t));
        }
    }

    /**
     * 卸载自制加载器：删掉它的标记文件，并尽量把对应 patch 从版本 json 里摘掉。
     *
     * @param which {@code "modloader"} 或 {@code "babric"}
     */
    private void removeLoaderMarker(String which) {
        try {
            java.io.File dir = new java.io.File(
                    this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName);
            if ("modloader".equals(which)) {
                java.io.File marker = new java.io.File(
                        dir, com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask.MARKER_NAME);
                if (marker.exists()) {
                    marker.delete();
                }
                android.widget.Toast.makeText(this.context,
                        "已移除 ModLoader 标记（jar 内已注入的 class 无法自动还原，建议重装该版本）",
                        android.widget.Toast.LENGTH_LONG).show();
            } else {
                java.io.File marker = new java.io.File(
                        dir, com.qcl.launcher.launcher.download.modloader.ModLoaderDetector.BABRIC_MARKER);
                if (marker.exists()) {
                    marker.delete();
                }
                // Babric 同时是一个 patch，能从 json 里摘掉
                this.uninstall("babric");
                android.widget.Toast.makeText(this.context, "已卸载 Babric",
                        android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            this.refresh(this.versionName);
            new Thread(() -> {
                try {
                    this.activity.uiManager.versionListUI.refreshVersionList();
                } catch (Throwable ignored) {
                }
            }).start();
        } catch (Throwable ignored) {
        }
    }

    // ======================================================================
    // ★★★★★ 2026-10-17（1.5.2 新增）**Cleanroom 版本选择**
    //
    //   用户要求：「搞版本选择页，防止用户说没有旧版本的选择，
    //             用户想要下载旧版本的模组加载器」。
    //
    //   ⇒ 点 Cleanroom 行 → 拉 index.json → 弹出**全部可用版本**的列表对话框
    //     （含旧版本，按发布时间倒序，最新在最上面），选中即安装。
    //   不新开页面：复用 AlertDialog，少一个布局 + 少一套适配器 + 少一次 UIManager 注册，
    //   但「能选旧版本」这个需求完全满足。
    // ======================================================================

    private static final String CLEANROOM_INDEX_URL =
            "https://hmcl.glavo.site/metadata/cleanroom/index.json";

    /** 弹出 Cleanroom 版本选择对话框。 */
    private void showCleanroomVersionPicker() {
        // Cleanroom 只支持 1.12.2 —— 先拦掉，别让玩家选了版本再失败
        if (!"1.12.2".equals(this.gameVersion)) {
            android.widget.Toast.makeText(this.context,
                    "Cleanroom 只支持 Minecraft 1.12.2，当前版本是 " + this.gameVersion,
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (this.forgeVersion != null || this.neoForgeVersion != null) {
            toastConflict("Cleanroom",
                    this.forgeVersion != null ? "Forge" : "NeoForge");
            return;
        }
        final android.app.ProgressDialog pd = new android.app.ProgressDialog(this.context);
        pd.setMessage("正在获取 Cleanroom 版本列表…");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            try {
                String json = com.qcl.launcher.utils.io.NetworkUtils.doGet(
                        com.qcl.launcher.utils.io.NetworkUtils.toURL(CLEANROOM_INDEX_URL));
                final com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion[] arr =
                        new com.google.gson.Gson().fromJson(json,
                                com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion[].class);
                this.activity.runOnUiThread(() -> {
                    try {
                        pd.dismiss();
                    } catch (Throwable ignored) {
                    }
                    this.showCleanroomListDialog(arr);
                });
            } catch (final Exception e) {
                this.activity.runOnUiThread(() -> {
                    try {
                        pd.dismiss();
                    } catch (Throwable ignored) {
                    }
                    android.widget.Toast.makeText(this.context,
                            "获取 Cleanroom 版本列表失败：" + (e.getMessage() == null ? "网络错误" : e.getMessage()),
                            android.widget.Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /** 用列表对话框展示所有 Cleanroom 版本（最新在前），选中即装。 */
    private void showCleanroomListDialog(
            com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion[] arr) {
        if (arr == null || arr.length == 0) {
            android.widget.Toast.makeText(this.context,
                    "没有获取到任何 Cleanroom 版本", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        // 按发布时间倒序（新 → 旧）；没有时间的排最后
        java.util.List<com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion> list =
                new java.util.ArrayList<>();
        for (com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion v : arr) {
            if (v != null && v.name != null && !v.name.trim().isEmpty()) {
                list.add(v);
            }
        }
        list.sort((a, b) -> {
            String sa = a.created_at == null ? "" : a.created_at;
            String sb = b.created_at == null ? "" : b.created_at;
            return sb.compareTo(sa);
        });
        final String[] items = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion v = list.get(i);
            items[i] = v.name + (v.created_at != null && v.created_at.length() >= 10
                    ? "    (" + v.created_at.substring(0, 10) + ")" : "");
        }
        new android.app.AlertDialog.Builder(this.context)
                .setTitle("选择 Cleanroom 版本")
                .setItems(items, (d, which) -> this.installCleanroom(list.get(which)))
                .setNegativeButton("取消", null)
                .show();
    }

    /** 真正安装选中的 Cleanroom 版本。 */
    private void installCleanroom(
            com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion chosen) {
        if (chosen == null) {
            return;
        }
        android.widget.Toast.makeText(this.context,
                "正在安装 Cleanroom " + chosen.name + "…",
                android.widget.Toast.LENGTH_SHORT).show();

        new com.qcl.launcher.launcher.download.cleanroom.CleanroomInstallTask(
                this.activity,
                null,               // ★ 自动安装页没有下载任务列表（ForgeInstallTask 已加判空）
                this.versionName,
                this.gameVersion,
                new com.qcl.launcher.launcher.download.cleanroom.CleanroomInstallTask
                        .InstallCleanroomCallback() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onFailed(Exception e) {
                        AutoInstallUI.this.onLoaderInstallFailed("Cleanroom", e);
                    }

                    @Override
                    public void onFinish(com.qcl.launcher.launcher.game.Version version) {
                        AutoInstallUI.this.cleanroomVersion = chosen.name;
                        AutoInstallUI.this.onLoaderInstallDone(
                                "Cleanroom " + chosen.name + " 安装完成");
                    }
                }).execute(chosen);
    }

    /** 加载器装完：Toast + 刷新本页状态 + 刷新版本列表。 */    private void onLoaderInstallDone(final String msg) {
        try {
            this.activity.runOnUiThread(() -> {
                try {
                    android.widget.Toast.makeText(this.context, msg,
                            android.widget.Toast.LENGTH_LONG).show();
                } catch (Throwable ignored) {
                }
                this.refresh(this.versionName);
                new Thread(() -> {
                    try {
                        this.activity.uiManager.versionListUI.refreshVersionList();
                    } catch (Throwable ignored) {
                    }
                }).start();
            });
        } catch (Throwable ignored) {
        }
    }

    /** 加载器装失败：明确告知用户（绝不静默吞掉）。 */
    private void onLoaderInstallFailed(final String which, final Exception e) {
        try {
            final String detail = (e == null || e.getMessage() == null)
                    ? "未知原因" : e.getMessage();
            this.activity.runOnUiThread(() -> {
                try {
                    android.widget.Toast.makeText(this.context,
                            which + " 安装失败：" + detail, android.widget.Toast.LENGTH_LONG).show();
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }
}

