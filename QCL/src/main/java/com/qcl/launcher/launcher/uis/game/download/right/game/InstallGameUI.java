package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.GameInstallDialog;
import com.qcl.launcher.launcher.download.fabric.FabricLoaderVersion;
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderVersion;
import com.qcl.launcher.launcher.download.modloader.ModLoaderVersions;
import com.qcl.launcher.launcher.download.optifine.OptifineVersion;
import com.qcl.launcher.launcher.download.quilt.QuiltLoaderVersion;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class InstallGameUI extends BaseUI implements View.OnClickListener, TextWatcher {
    private ImageButton deleteFabricAPIVersion;
    private ImageButton deleteFabricVersion;
    private ImageButton deleteForgeVersion;
    private ImageButton deleteLiteLoaderVersion;
    private ImageButton deleteOptiFineVersion;
    private ImageButton deleteQuiltAPIVersion;
    private ImageButton deleteQuiltVersion;
    private EditText editName;
    public RemoteMod.Version fabricAPIVersion;
    private TextView fabricAPIVersionText;
    public FabricLoaderVersion fabricVersion;
    private TextView fabricVersionText;
    public ForgeVersion forgeVersion;
    private TextView forgeVersionText;
    private TextView gameVersionText;
    private Button install;
    public LinearLayout installGameUI;
    public LiteLoaderVersion liteLoaderVersion;
    private TextView liteLoaderVersionText;
    public String name;
    private TextView optiFineVersionText;
    public OptifineVersion optifineVersion;
    public RemoteMod.Version quiltAPIVersion;
    private TextView quiltAPIVersionText;
    public QuiltLoaderVersion quiltVersion;
    private TextView quiltVersionText;
    private ImageView selectFabric;
    private ImageView selectFabricAPI;
    private LinearLayout selectFabricAPIVersion;
    private LinearLayout selectFabricVersion;
    private ImageView selectForge;
    private LinearLayout selectForgeVersion;
    private ImageView selectLiteLoader;
    private LinearLayout selectLiteLoaderVersion;
    private ImageView selectOptiFine;
    private LinearLayout selectOptiFineVersion;
    private ImageView selectQuilt;
    private ImageView selectQuiltAPI;
    private LinearLayout selectQuiltAPIVersion;

    /**
     * ★ 1.2.3：Risugami's ModLoader。
     *
     * 它和上面几个加载器不一样 —— **没有「版本可以选」**：
     * 装哪一份由 MC 版本自己决定（b1.7.3 就装 b1.7.3 那份），
     * 所以这里只是一个「装 / 不装」的开关，不是选版本。
     *
     * ★ 时机：ModLoader 是往 minecraft.jar 里注入 class，
     *   必须等基础版本装完才能装 —— 所以这里只记一个标记，
     *   真正的下载+注入由 GameInstallDialog 在安装链跑完之后执行。
     */
    public boolean installModLoader;
    private LinearLayout selectModLoaderVersion;
    private TextView modLoaderVersionText;
    public boolean installBabric;
    private LinearLayout selectBabricVersion;
    private TextView babricVersionText;
    private LinearLayout selectQuiltVersion;
    public VersionManifest.Version version;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public InstallGameUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.installGameUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_game);
        EditText editText = (EditText) this.activity.findViewById(R.id.edit_game_name);
        this.editName = editText;
        editText.addTextChangedListener(this);
        this.gameVersionText = (TextView) this.activity.findViewById(R.id.minecraft_version_text);
        this.forgeVersionText = (TextView) this.activity.findViewById(R.id.forge_version_text);
        this.liteLoaderVersionText = (TextView) this.activity.findViewById(R.id.liteloader_version_text);
        this.optiFineVersionText = (TextView) this.activity.findViewById(R.id.optifine_version_text);
        this.fabricVersionText = (TextView) this.activity.findViewById(R.id.fabric_version_text);
        this.fabricAPIVersionText = (TextView) this.activity.findViewById(R.id.fabric_api_version_text);
        this.quiltVersionText = (TextView) this.activity.findViewById(R.id.quilt_version_text);
        this.quiltAPIVersionText = (TextView) this.activity.findViewById(R.id.quilt_api_version_text);
        this.deleteForgeVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_forge);
        this.deleteLiteLoaderVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_liteloader);
        this.deleteOptiFineVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_optifine);
        this.deleteFabricVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_fabric);
        this.deleteFabricAPIVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_fabric_api);
        this.deleteQuiltVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_quilt);
        this.deleteQuiltAPIVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_quilt_api);
        this.deleteForgeVersion.setOnClickListener(this);
        this.deleteLiteLoaderVersion.setOnClickListener(this);
        this.deleteOptiFineVersion.setOnClickListener(this);
        this.deleteFabricVersion.setOnClickListener(this);
        this.deleteFabricAPIVersion.setOnClickListener(this);
        this.deleteQuiltVersion.setOnClickListener(this);
        this.deleteQuiltAPIVersion.setOnClickListener(this);
        this.selectForgeVersion = (LinearLayout) this.activity.findViewById(R.id.select_forge_version);
        this.selectLiteLoaderVersion = (LinearLayout) this.activity.findViewById(R.id.select_liteloader_version);
        this.selectOptiFineVersion = (LinearLayout) this.activity.findViewById(R.id.select_optifine_version);
        this.selectFabricVersion = (LinearLayout) this.activity.findViewById(R.id.select_fabric_version);
        this.selectFabricAPIVersion = (LinearLayout) this.activity.findViewById(R.id.select_fabric_api_version);
        this.selectQuiltVersion = (LinearLayout) this.activity.findViewById(R.id.select_quilt_version);
        this.selectQuiltAPIVersion = (LinearLayout) this.activity.findViewById(R.id.select_quilt_api_version);
        this.selectForgeVersion.setOnClickListener(this);
        this.selectLiteLoaderVersion.setOnClickListener(this);
        this.selectOptiFineVersion.setOnClickListener(this);
        this.selectFabricVersion.setOnClickListener(this);
        this.selectFabricAPIVersion.setOnClickListener(this);
        this.selectQuiltVersion.setOnClickListener(this);
        this.selectQuiltAPIVersion.setOnClickListener(this);

        // ★ 1.2.3：ModLoader 那一行（图标是白底 ML）
        this.selectModLoaderVersion = (LinearLayout) this.activity.findViewById(R.id.select_modloader_version);
        this.modLoaderVersionText = (TextView) this.activity.findViewById(R.id.modloader_version_text);
        this.selectModLoaderVersion.setOnClickListener(this);
        this.selectBabricVersion = (LinearLayout) this.activity.findViewById(R.id.select_babric_version);
        this.babricVersionText = (TextView) this.activity.findViewById(R.id.babric_version_text);
        this.selectBabricVersion.setOnClickListener(this);
        this.selectForge = (ImageView) this.activity.findViewById(R.id.select_forge);
        this.selectLiteLoader = (ImageView) this.activity.findViewById(R.id.select_lite_loader);
        this.selectOptiFine = (ImageView) this.activity.findViewById(R.id.select_optifine);
        this.selectFabric = (ImageView) this.activity.findViewById(R.id.select_fabric);
        this.selectFabricAPI = (ImageView) this.activity.findViewById(R.id.select_fabric_api);
        this.selectQuilt = (ImageView) this.activity.findViewById(R.id.select_quilt);
        this.selectQuiltAPI = (ImageView) this.activity.findViewById(R.id.select_quilt_api);
        Button button = (Button) this.activity.findViewById(R.id.install_game);
        this.install = button;
        button.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.install_game_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.installGameUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.installGameUI, this.activity, this.context, true);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.deleteForgeVersion && this.forgeVersion != null) {
            this.forgeVersion = null;
            init();
        }
        if (view == this.deleteLiteLoaderVersion && this.liteLoaderVersion != null) {
            this.liteLoaderVersion = null;
            init();
        }
        if (view == this.deleteOptiFineVersion && this.optifineVersion != null) {
            this.optifineVersion = null;
            init();
        }
        if (view == this.deleteFabricVersion && this.fabricVersion != null) {
            this.fabricVersion = null;
            init();
        }
        if (view == this.deleteFabricAPIVersion && this.fabricAPIVersion != null) {
            this.fabricAPIVersion = null;
            init();
        }
        if (view == this.deleteQuiltVersion && this.quiltVersion != null) {
            this.quiltVersion = null;
            init();
        }
        if (view == this.deleteQuiltAPIVersion && this.quiltAPIVersion != null) {
            this.quiltAPIVersion = null;
            init();
        }
        if (view == this.selectForgeVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadForgeUI.version = this.version.id;
            this.activity.uiManager.downloadForgeUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadForgeUI);
        }
        if (view == this.selectLiteLoaderVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadLiteLoaderUI.version = this.version.id;
            this.activity.uiManager.downloadLiteLoaderUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadLiteLoaderUI);
        }
        if (view == this.selectOptiFineVersion && this.fabricVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadOptifineUI.version = this.version.id;
            this.activity.uiManager.downloadOptifineUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadOptifineUI);
        }
        if (view == this.selectFabricVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadFabricUI.version = this.version.id;
            this.activity.uiManager.downloadFabricUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricUI);
        }
        if (view == this.selectFabricAPIVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null) {
            this.activity.uiManager.downloadFabricAPIUI.version = this.version.id;
            this.activity.uiManager.downloadFabricAPIUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricAPIUI);
        }
        if (view == this.selectQuiltVersion && this.forgeVersion == null && this.optifineVersion == null && this.fabricVersion == null) {
            this.activity.uiManager.downloadQuiltUI.version = this.version.id;
            this.activity.uiManager.downloadQuiltUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadQuiltUI);
        }
        if (view == this.selectQuiltAPIVersion && this.forgeVersion == null && this.optifineVersion == null && this.fabricVersion == null) {
            this.activity.uiManager.downloadQuiltAPIUI.version = this.version.id;
            this.activity.uiManager.downloadQuiltAPIUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadQuiltAPIUI);
        }
        // ★ 1.2.3：ModLoader 与 Forge **互斥**，不能同时选。
        //   原因：1.3 起 FML（Forge 的加载器）已经把 RML 包含进去了，
        //   再叠一份 ModLoader 会类冲突，进游戏直接崩。
        //   而 LiteLoader 和 ModLoader 是**可以共存**的（历史上就是这么搭的），
        //   所以这里只挡 Forge。
        if (view == this.selectModLoaderVersion) {
            toggleModLoader();
        }
        if (view == this.selectBabricVersion) {
            toggleBabric();
        }
        if (view == this.selectForgeVersion && this.installModLoader) {
            new AlertDialog.Builder(this.context)
                    .setTitle("ModLoader")
                    .setMessage("已经选了 ModLoader，不能再装 Forge。\n\n"
                            + "从 1.3 起 Forge 的加载器（FML）已经把 ModLoader 包含进去了，"
                            + "两个一起装会类冲突、进游戏直接崩。\n"
                            + "要装 Forge，请先点 ModLoader 那一行取消掉。")
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }
        if (view == this.install) {            if (SettingUtils.getLocalVersionNames(this.activity.launcherSetting.gameFileDirectory).contains(this.editName.getText().toString())) {
                Toast.makeText(this.context, this.context.getString(R.string.install_game_ui_exist), 0).show();
                return;
            }
            if (this.forgeVersion != null || this.optifineVersion != null) {
                this.fabricAPIVersion = null;
                this.quiltAPIVersion = null;
            }
            // ★ 1.2.3：把「勾了 ModLoader」这件事传给安装流程，
            //   它会在基础版本装完之后（installJson 那一步）才真正去装 ModLoader
            GameInstallDialog dialog = new GameInstallDialog(this.context, this.activity, this.editName.getText().toString(), this.version, this.forgeVersion, this.optifineVersion, this.liteLoaderVersion, this.fabricVersion, this.fabricAPIVersion, this.quiltVersion, this.quiltAPIVersion);
            dialog.installModLoader = this.installModLoader;
            dialog.installBabric = this.installBabric;
            dialog.show();
        }
    }

    /**
     * ★ 1.2.3：切换 ModLoader 的「装 / 不装」。
     *
     * ★ 时机说明：这里**只是打标记**，真正的下载+注入放在
     *   GameInstallDialog 的安装链全部跑完之后 ——
     *   ModLoader 是往 minecraft.jar 里注入 class，jar 必须先装好。
     *   （这也回答了「要不要等游戏文件全下完」：是，必须等。）
     */
    /**
     * ★ 1.2.3：切换 Babric 的「装 / 不装」。
     *
     * Babric 是 b1.7.3 时代的 Fabric 分支，只给远古版本用；装最新版稳定 loader。
     * 和 ModLoader 一样这里只打标记，真正下载在 GameInstallDialog 里等本体装完后才做。
     */
    private void toggleBabric() {
        if (this.version == null) {
            return;
        }
        if (this.installBabric) {
            this.installBabric = false;
            init();
            return;
        }
        // ★★★ Babric 的 meta 里只有 b1.7.3 这一个游戏版本（实测），
        //   其它版本一律拒绝，别让玩家装完启动不了。
        String id = this.version.id;
        String low = id == null ? "" : id.toLowerCase();
        if (!"b1.7.3".equals(low)) {
            new AlertDialog.Builder(this.context)
                    .setTitle("Babric")
                    .setMessage("Babric 只支持 b1.7.3（它的 meta 里只有这一个游戏版本）。\n"
                            + "当前版本（" + id + "）装不了。")
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }
        this.installBabric = true;
        init();
    }

    private void toggleModLoader() {
        if (this.version == null) {
            return;
        }
        // 再点一下 = 取消
        if (this.installModLoader) {
            this.installModLoader = false;
            init();
            return;
        }
        ModLoaderVersions.Entry entry = ModLoaderVersions.find(this.version.id);
        if (entry == null) {
            new AlertDialog.Builder(this.context)
                    .setTitle("ModLoader")
                    .setMessage("这个版本（" + this.version.id + "）没有对应的 ModLoader。\n"
                            + "ModLoader 只支持 1.2.5 ~ 1.6.2 的正式版和部分远古版本。")
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }
        if (!entry.isInstallable()) {
            new AlertDialog.Builder(this.context)
                    .setTitle("ModLoader")
                    .setMessage("ModLoader " + entry.mcVersion + " 官方只发布了 .rar 格式的包，"
                            + "启动器现在解不开 rar（Java 自带库不支持），这个版本暂时装不了。")
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }
        this.installModLoader = true;
        init();
    }

    private void init() {
        this.editName.setText(this.name);
        this.gameVersionText.setText(this.version.id);

        // ★ 1.2.3：ModLoader 那一行的状态（它没有版本可选，只显示装/不装/不支持/冲突）
        if (this.babricVersionText != null) {
            if (this.installBabric) {
                this.babricVersionText.setText((CharSequence) "Babric");
            } else {
                this.babricVersionText.setText((CharSequence) this.context.getString(R.string.install_game_ui_none));
            }
        }
        if (this.modLoaderVersionText != null) {
            ModLoaderVersions.Entry mlEntry = this.version == null ? null
                    : ModLoaderVersions.find(this.version.id);
            if (this.forgeVersion != null) {
                // Forge 与 ModLoader 互斥（1.3 起 FML 已包含 RML，叠一起会崩）
                this.modLoaderVersionText.setText(
                        this.context.getString(R.string.install_game_ui_modloader_not_compatible));
            } else if (mlEntry == null || !mlEntry.isInstallable()) {
                this.modLoaderVersionText.setText(
                        this.context.getString(R.string.install_game_ui_modloader_not_supported));
            } else if (this.installModLoader) {
                this.modLoaderVersionText.setText(
                        this.context.getString(R.string.install_game_ui_modloader_installed));
            } else {
                this.modLoaderVersionText.setText(
                        this.context.getString(R.string.install_game_ui_none));
            }
        }
        ForgeVersion forgeVersion = this.forgeVersion;
        if (forgeVersion != null || this.optifineVersion != null) {
            this.forgeVersionText.setText(forgeVersion == null ? this.context.getString(R.string.install_game_ui_none) : forgeVersion.getVersion());
            this.optiFineVersionText.setText(this.optifineVersion == null ? this.context.getString(R.string.install_game_ui_none) : this.optifineVersion.type + "_" + this.optifineVersion.patch);
            this.fabricVersionText.setText(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.fabricAPIVersionText.setText(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.quiltVersionText.setText(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.quiltAPIVersionText.setText(this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.deleteForgeVersion.setVisibility(this.forgeVersion != null ? 0 : 8);
            this.deleteOptiFineVersion.setVisibility(this.optifineVersion != null ? 0 : 8);
            this.selectFabric.setVisibility(8);
            this.selectFabricAPI.setVisibility(8);
            this.selectQuilt.setVisibility(8);
            this.selectQuiltAPI.setVisibility(8);
        } else {
            this.forgeVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.optiFineVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.fabricVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.fabricAPIVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.quiltVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.quiltAPIVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.deleteForgeVersion.setVisibility(8);
            this.deleteOptiFineVersion.setVisibility(8);
            this.selectFabric.setVisibility(0);
            this.selectFabricAPI.setVisibility(0);
            this.selectQuilt.setVisibility(0);
            this.selectQuiltAPI.setVisibility(0);
        }
        if (this.fabricVersion != null) {
            this.forgeVersionText.setText(this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.optiFineVersionText.setText(this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.liteLoaderVersionText.setText(this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.quiltVersionText.setText(this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.quiltAPIVersionText.setText(this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.fabricVersionText.setText(this.fabricVersion.version);
            this.deleteFabricVersion.setVisibility(0);
            this.selectForge.setVisibility(8);
            this.selectLiteLoader.setVisibility(8);
            this.selectOptiFine.setVisibility(8);
            this.selectQuilt.setVisibility(8);
            this.selectQuiltAPI.setVisibility(8);
        } else {
            if (this.quiltVersion != null) {
                this.forgeVersionText.setText(this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.optiFineVersionText.setText(this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.liteLoaderVersionText.setText(this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.fabricVersionText.setText(this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.fabricAPIVersionText.setText(this.context.getString(R.string.install_game_ui_quilt_not_compatible));
                this.quiltVersionText.setText(this.quiltVersion.version);
                this.deleteQuiltVersion.setVisibility(0);
                this.selectForge.setVisibility(8);
                this.selectLiteLoader.setVisibility(8);
                this.selectOptiFine.setVisibility(8);
                this.selectFabric.setVisibility(8);
                this.selectFabricAPI.setVisibility(8);
            } else {
                if (this.forgeVersion == null) {
                    this.forgeVersionText.setText(this.context.getString(R.string.install_game_ui_none));
                }
                if (this.optifineVersion == null) {
                    this.optiFineVersionText.setText(this.context.getString(R.string.install_game_ui_none));
                }
                if (this.liteLoaderVersion == null) {
                    this.liteLoaderVersionText.setText(this.context.getString(R.string.install_game_ui_none));
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
            if (this.quiltAPIVersion == null) {
                this.quiltAPIVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            }
            this.deleteFabricVersion.setVisibility(8);
        }
        LiteLoaderVersion liteLoaderVersion = this.liteLoaderVersion;
        if (liteLoaderVersion != null) {
            this.liteLoaderVersionText.setText(liteLoaderVersion.getVersion());
            this.deleteLiteLoaderVersion.setVisibility(0);
        } else {
            if (this.fabricVersion == null && this.quiltVersion == null) {
                this.liteLoaderVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            }
            this.deleteLiteLoaderVersion.setVisibility(8);
        }
        if (this.forgeVersion != null || this.optifineVersion != null || this.quiltVersion != null) {
            this.fabricAPIVersionText.setText(this.quiltVersion != null ? this.context.getString(R.string.install_game_ui_quilt_not_compatible) : this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.deleteFabricAPIVersion.setVisibility(8);
            this.selectFabricAPI.setVisibility(8);
        } else {
            RemoteMod.Version version = this.fabricAPIVersion;
            if (version == null) {
                this.fabricAPIVersionText.setText(this.context.getString(R.string.install_game_ui_none));
                this.deleteFabricAPIVersion.setVisibility(8);
                this.selectFabricAPI.setVisibility(0);
            } else {
                this.fabricAPIVersionText.setText(version.getVersion());
                this.deleteFabricAPIVersion.setVisibility(0);
                this.selectFabricAPI.setVisibility(0);
            }
        }
        if (this.forgeVersion != null || this.optifineVersion != null || this.fabricVersion != null) {
            this.quiltAPIVersionText.setText(this.fabricVersion != null ? this.context.getString(R.string.install_game_ui_fabric_not_compatible) : this.optifineVersion != null ? this.context.getString(R.string.install_game_ui_optifine_not_compatible) : this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.deleteQuiltAPIVersion.setVisibility(8);
            this.selectQuiltAPI.setVisibility(8);
            return;
        }
        RemoteMod.Version version2 = this.quiltAPIVersion;
        if (version2 == null) {
            this.quiltAPIVersionText.setText(this.context.getString(R.string.install_game_ui_none));
            this.deleteQuiltAPIVersion.setVisibility(8);
            this.selectQuiltAPI.setVisibility(0);
        } else {
            this.quiltAPIVersionText.setText(version2.getVersion());
            this.deleteQuiltAPIVersion.setVisibility(0);
            this.selectQuiltAPI.setVisibility(0);
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        this.name = this.editName.getText().toString();
    }
}
