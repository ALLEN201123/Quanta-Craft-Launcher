package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.GameInstallDialog;
import com.qcl.launcher.launcher.download.fabric.FabricLoaderVersion;
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderVersion;
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
        if (view == this.install) {
            if (SettingUtils.getLocalVersionNames(this.activity.launcherSetting.gameFileDirectory).contains(this.editName.getText().toString())) {
                Toast.makeText(this.context, this.context.getString(R.string.install_game_ui_exist), 0).show();
                return;
            }
            if (this.forgeVersion != null || this.optifineVersion != null) {
                this.fabricAPIVersion = null;
                this.quiltAPIVersion = null;
            }
            new GameInstallDialog(this.context, this.activity, this.editName.getText().toString(), this.version, this.forgeVersion, this.optifineVersion, this.liteLoaderVersion, this.fabricVersion, this.fabricAPIVersion, this.quiltVersion, this.quiltAPIVersion).show();
        }
    }

    private void init() {
        this.editName.setText(this.name);
        this.gameVersionText.setText(this.version.id);
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
