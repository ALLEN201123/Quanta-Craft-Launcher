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
    private TextView liteLoaderVersionText;
    private TextView optiFineVersionText;
    private TextView fabricVersionText;
    private TextView fabricAPIVersionText;
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
        this.liteLoaderVersionText = (TextView)this.activity.findViewById(R.id.current_liteloader_version_text);
        this.optiFineVersionText = (TextView)this.activity.findViewById(R.id.current_optifine_version_text);
        this.fabricVersionText = (TextView)this.activity.findViewById(R.id.current_fabric_version_text);
        this.fabricAPIVersionText = (TextView)this.activity.findViewById(R.id.current_fabric_api_version_text);
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
        if (view == this.selectForgeVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadForgeUI.version = this.gameVersion;
            this.activity.uiManager.downloadForgeUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadForgeUI);
        }
        if (view == this.selectLiteLoaderVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadLiteLoaderUI.version = this.gameVersion;
            this.activity.uiManager.downloadLiteLoaderUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadLiteLoaderUI);
        }
        if (view == this.selectOptiFineVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadOptifineUI.version = this.gameVersion;
            this.activity.uiManager.downloadOptifineUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadOptifineUI);
        }
        if (view == this.selectFabricVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadFabricUI.version = this.gameVersion;
            this.activity.uiManager.downloadFabricUI.install = true;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricUI);
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
}

