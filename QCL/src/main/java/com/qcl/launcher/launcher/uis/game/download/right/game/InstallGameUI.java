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
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersion;
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
    // ★ 1.4.2：NeoForge（照 FCL InstallerItemGroup —— 与 Forge 平级、两者互斥）
    public NeoForgeVersion neoForgeVersion;
    private TextView neoForgeVersionText;
    private ImageButton deleteNeoForgeVersion;

    // ★★★★★ 2026-10-17（1.5.2 新增）Cleanroom / Legacy Fabric / Legacy Fabric API
    //   用户要求这三个也要能在「安装游戏」界面里选。
    public com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion cleanroomVersion;
    private TextView cleanroomVersionText;
    private ImageButton deleteCleanroomVersion;
    public FabricLoaderVersion legacyFabricVersion;
    private TextView legacyFabricVersionText;
    private ImageButton deleteLegacyFabricVersion;
    public RemoteMod.Version legacyFabricAPIVersion;
    private TextView legacyFabricAPIVersionText;
    private ImageButton deleteLegacyFabricAPIVersion;
    /**
     * ★ 1.5.7：Legacy Fabric / Legacy Fabric API 两个**整行**的引用。
     *
     * <p>为什么要存整行：版本限制的做法是「当前选的 MC 版本不支持 → 整行藏起来」，
     * 而不是把文案改成「不支持」还留着箭头（那样玩家照样点得进去）。
     */
    private View legacyFabricRow;
    private View legacyFabricAPIRow;
    /**
     * ★★★★★ 2026-11-18（1.6.1）【Fabric / NeoForge 的版本门槛】——
     * 留住这两个「整行」引用，供 {@link #applyLoaderVersionGate()} 按 MC 版本显示/隐藏。
     *
     * <p>群友反馈（QQ 群 @打游戏的阿坤）原话：
     * 「调整一下那个加载器的显示范围」「比如 Fabric 只在 1.14+ 显示，
     * NeoForge 只在 1.20.1+ 显示」「反正就像上次 Legacy Fabric 那种」。
     *
     * <p>即：照 1.5.7 给 Legacy Fabric 加版本限制的同一套做法，
     * 给 Fabric 和 NeoForge 也各加一条下限，低于门槛就**整行隐藏**。
     */
    private View fabricRow;
    private View neoForgeRow;
    /**
     * ★★★★★ 2026-11-19（1.6.2）其余加载器的「整行」引用。
     *
     * <p>群友第二轮补齐了全部加载器的显示范围，见 {@link #applyLoaderVersionGate()}。
     * 取不到时保持 null，gate 里判空跳过（不炸）。
     */
    private View fabricAPIRow;
    private View quiltRow;
    private View quiltAPIRow;
    private View cleanroomRow;
    private View liteLoaderRow;
    private View modLoaderRow;
    private View babricRow;
    private LinearLayout selectNeoForgeVersion;
    private ImageView selectNeoForge;
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
        // ★ 1.4.2：NeoForge 行
        this.neoForgeVersionText = (TextView) this.activity.findViewById(R.id.neoforge_version_text);
        this.deleteNeoForgeVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_neoforge);
        this.deleteNeoForgeVersion.setOnClickListener(this);
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
        // ★ 1.4.2：NeoForge 选择行
        this.selectNeoForgeVersion = (LinearLayout) this.activity.findViewById(R.id.select_neoforge_version);
        this.selectNeoForgeVersion.setOnClickListener(this);
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

        // ★★★★★ 2026-10-17（1.5.2）Cleanroom / Legacy Fabric / Legacy Fabric API 三行。
        //
        //   用户要求：这三个加载器也要出现在「安装游戏」界面。
        //   ★ 现状说明（必须诚实）：
        //     这三行的**完整选择+安装流程**目前实现于「版本设置 → 自动安装」
        //     （AutoInstallUI：Cleanroom 有版本选择对话框、Legacy Fabric / Legacy Fabric API
        //      走各自的列表页），逻辑已验证可编译。
        //     而**本界面**（下载/安装游戏对话框）的安装链需要把版本对象一路传进
        //     GameInstallDialog 并在基础版本装完后依次执行，改动面较大，尚未接入。
        //
        //   ⇒ 但**绝不能点了没反应**（用户明确骂过这个问题）。
        //     所以这里先接一个明确的引导：告诉玩家去哪儿装，而不是静默失败。
        final android.view.View.OnClickListener newLoaderGuide = v -> {
            try {
                if (v.getId() == R.id.select_cleanroom_version) {
                    pickCleanroomVersion();
                } else if (v.getId() == R.id.select_legacy_fabric_version) {
                    pickLegacyFabricVersion();
                } else if (v.getId() == R.id.select_legacy_fabric_api_version) {
                    pickLegacyFabricAPIVersion();
                }
            } catch (Throwable t) {
                android.widget.Toast.makeText(this.context,
                        "打开选择失败：" + t.getMessage(),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        };
        android.view.View vCleanroom = this.activity.findViewById(R.id.select_cleanroom_version);
        if (vCleanroom != null) {
            vCleanroom.setOnClickListener(newLoaderGuide);
        }
        android.view.View vLegacyFabric = this.activity.findViewById(R.id.select_legacy_fabric_version);
        if (vLegacyFabric != null) {
            vLegacyFabric.setOnClickListener(newLoaderGuide);
        }
        android.view.View vLegacyFabricAPI = this.activity.findViewById(R.id.select_legacy_fabric_api_version);
        if (vLegacyFabricAPI != null) {
            vLegacyFabricAPI.setOnClickListener(newLoaderGuide);
        }
        // ★ 1.5.7：留住「整行」引用，供版本限制里 setVisibility 用（见 applyLegacyFabricExclusion）
        this.legacyFabricRow = vLegacyFabric;
        this.legacyFabricAPIRow = vLegacyFabricAPI;
        // ★ 1.6.1：Fabric / NeoForge 的整行引用（见 applyLoaderVersionGate）
        //   这两个 id 用的是「选择行」容器：select_fabric_version / select_neoforge_version。
        //   取不到时保持 null，applyLoaderVersionGate 里判空跳过（不炸）。
        this.fabricRow = this.activity.findViewById(R.id.select_fabric_version);
        this.neoForgeRow = this.activity.findViewById(R.id.select_neoforge_version);
        // ★ 1.6.2：其余加载器的整行引用
        this.fabricAPIRow = this.activity.findViewById(R.id.select_fabric_api_version);
        this.quiltRow = this.activity.findViewById(R.id.select_quilt_version);
        this.quiltAPIRow = this.activity.findViewById(R.id.select_quilt_api_version);
        // Cleanroom / ModLoader / Babric 用的是「独立一行」的容器 id（1.5.2 起就是这套）
        this.cleanroomRow = this.activity.findViewById(R.id.select_cleanroom_version);
        this.liteLoaderRow = this.activity.findViewById(R.id.select_liteloader_version);
        this.modLoaderRow = this.activity.findViewById(R.id.select_modloader_version);
        this.babricRow = this.activity.findViewById(R.id.select_babric_version);
        // 绑定这三行的「版本文字」与「取消按钮」
        this.cleanroomVersionText = (TextView) this.activity.findViewById(R.id.cleanroom_version_text);
        this.deleteCleanroomVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_cleanroom);
        this.legacyFabricVersionText = (TextView) this.activity.findViewById(R.id.legacy_fabric_version_text);
        this.deleteLegacyFabricVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_legacy_fabric);
        this.legacyFabricAPIVersionText = (TextView) this.activity.findViewById(R.id.legacy_fabric_api_version_text);
        this.deleteLegacyFabricAPIVersion = (ImageButton) this.activity.findViewById(R.id.call_off_install_legacy_fabric_api);
        if (this.deleteCleanroomVersion != null) {
            this.deleteCleanroomVersion.setOnClickListener(v -> {
                this.cleanroomVersion = null;
                refreshNewLoaderRows();
            });
        }
        if (this.deleteLegacyFabricVersion != null) {
            this.deleteLegacyFabricVersion.setOnClickListener(v -> {
                this.legacyFabricVersion = null;
                refreshNewLoaderRows();
            });
        }
        if (this.deleteLegacyFabricAPIVersion != null) {
            this.deleteLegacyFabricAPIVersion.setOnClickListener(v -> {
                this.legacyFabricAPIVersion = null;
                refreshNewLoaderRows();
            });
        }

        // ★ 1.2.3：ModLoader 那一行（图标是白底 ML）
        this.selectModLoaderVersion = (LinearLayout) this.activity.findViewById(R.id.select_modloader_version);
        this.modLoaderVersionText = (TextView) this.activity.findViewById(R.id.modloader_version_text);
        this.selectModLoaderVersion.setOnClickListener(this);
        this.selectBabricVersion = (LinearLayout) this.activity.findViewById(R.id.select_babric_version);
        this.babricVersionText = (TextView) this.activity.findViewById(R.id.babric_version_text);
        this.selectBabricVersion.setOnClickListener(this);
        this.selectForge = (ImageView) this.activity.findViewById(R.id.select_forge);
        // ★ 1.4.2：NeoForge 右侧箭头
        this.selectNeoForge = (ImageView) this.activity.findViewById(R.id.select_neoforge);
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
        // ★ 1.4.2：NeoForge 取消选择
        if (view == this.deleteNeoForgeVersion && this.neoForgeVersion != null) {
            this.neoForgeVersion = null;
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
        // ★ 1.4.2：互斥规则照 FCL InstallerItemGroup ——
        //   mutualIncompatible(forge, fabric, quilt, neoForge, cleanroom) → 四者两两互斥
        //   addIncompatibles(optiFine, fabric, quilt, neoForge) / (liteLoader, fabric, quilt, neoForge)
        //   → OptiFine / LiteLoader 也与 NeoForge 互斥（QCL 无 Cleanroom）。即 NeoForge
        //   与 Forge/Fabric/Quilt/OptiFine/LiteLoader **全部互斥**。
        if (view == this.selectForgeVersion && this.fabricVersion == null && this.quiltVersion == null && this.neoForgeVersion == null) {
            this.activity.uiManager.downloadForgeUI.version = this.version.id;
            this.activity.uiManager.downloadForgeUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadForgeUI);
        }
        if (view == this.selectNeoForgeVersion && this.forgeVersion == null && this.fabricVersion == null
                && this.quiltVersion == null && this.optifineVersion == null && this.liteLoaderVersion == null) {
            this.activity.uiManager.downloadNeoForgeUI.version = this.version.id;
            this.activity.uiManager.downloadNeoForgeUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadNeoForgeUI);
        }
        if (view == this.selectLiteLoaderVersion && this.fabricVersion == null && this.quiltVersion == null && this.neoForgeVersion == null) {
            this.activity.uiManager.downloadLiteLoaderUI.version = this.version.id;
            this.activity.uiManager.downloadLiteLoaderUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadLiteLoaderUI);
        }
        if (view == this.selectOptiFineVersion && this.fabricVersion == null && this.quiltVersion == null && this.neoForgeVersion == null) {
            this.activity.uiManager.downloadOptifineUI.version = this.version.id;
            this.activity.uiManager.downloadOptifineUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadOptifineUI);
        }
        if (view == this.selectFabricVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null && this.neoForgeVersion == null) {
            this.activity.uiManager.downloadFabricUI.version = this.version.id;
            this.activity.uiManager.downloadFabricUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricUI);
        }
        if (view == this.selectFabricAPIVersion && this.forgeVersion == null && this.optifineVersion == null && this.quiltVersion == null && this.neoForgeVersion == null) {
            this.activity.uiManager.downloadFabricAPIUI.version = this.version.id;
            this.activity.uiManager.downloadFabricAPIUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadFabricAPIUI);
        }
        if (view == this.selectQuiltVersion && this.forgeVersion == null && this.optifineVersion == null && this.fabricVersion == null && this.neoForgeVersion == null) {
            this.activity.uiManager.downloadQuiltUI.version = this.version.id;
            this.activity.uiManager.downloadQuiltUI.install = false;
            this.activity.uiManager.switchMainUI(this.activity.uiManager.downloadQuiltUI);
        }
        if (view == this.selectQuiltAPIVersion && this.forgeVersion == null && this.optifineVersion == null && this.fabricVersion == null && this.neoForgeVersion == null) {
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
            // ★ 1.4.2：NeoForge 与 Forge/Fabric/Quilt 互斥（照 FCL），选了 NeoForge 就清掉它们
            if (this.neoForgeVersion != null) {
                this.forgeVersion = null;
                this.optifineVersion = null;
                this.liteLoaderVersion = null;
                this.fabricVersion = null;
                this.fabricAPIVersion = null;
                this.quiltVersion = null;
                this.quiltAPIVersion = null;
            }
            // ★ 1.2.3：把「勾了 ModLoader」这件事传给安装流程，
            //   它会在基础版本装完之后（installJson 那一步）才真正去装 ModLoader
            GameInstallDialog dialog = new GameInstallDialog(this.context, this.activity, this.editName.getText().toString(), this.version, this.forgeVersion, this.optifineVersion, this.liteLoaderVersion, this.fabricVersion, this.fabricAPIVersion, this.quiltVersion, this.quiltAPIVersion);
            dialog.neoForgeVersion = this.neoForgeVersion;
            dialog.installModLoader = this.installModLoader;
            dialog.installBabric = this.installBabric;
            // ★ 1.5.2：三个新加载器也一并交给 dialog，基础版本装完后依次执行
            dialog.cleanroomVersion = this.cleanroomVersion;
            dialog.legacyFabricVersion = this.legacyFabricVersion;
            dialog.legacyFabricAPIVersion = this.legacyFabricAPIVersion;
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
        // ★ 1.4.2：NeoForge 行的状态（独立计算，避免干扰原有互斥矩阵）
        //   互斥依据 FCL InstallerItemGroup：NeoForge 与 Forge / Fabric / Quilt 互斥，
        //   也与 OptiFine / LiteLoader 互斥。
        if (this.neoForgeVersionText != null) {
            String neoText;
            if (this.neoForgeVersion != null) {
                neoText = this.neoForgeVersion.getVersion();
                this.deleteNeoForgeVersion.setVisibility(View.VISIBLE);
                if (this.selectNeoForge != null) {
                    this.selectNeoForge.setVisibility(View.GONE);
                }
            } else {
                this.deleteNeoForgeVersion.setVisibility(View.GONE);
                if (this.forgeVersion != null) {
                    neoText = this.context.getString(R.string.install_game_ui_forge_not_compatible);
                } else if (this.fabricVersion != null) {
                    neoText = this.context.getString(R.string.install_game_ui_fabric_not_compatible);
                } else if (this.quiltVersion != null) {
                    neoText = this.context.getString(R.string.install_game_ui_quilt_not_compatible);
                } else if (this.optifineVersion != null) {
                    neoText = this.context.getString(R.string.install_game_ui_optifine_not_compatible);
                } else if (this.liteLoaderVersion != null) {
                    // QCL 无 liteLoader_not_compatible 字符串，复用 Forge 版文案（与下方兜底一致）
                    neoText = this.context.getString(R.string.install_game_ui_forge_not_compatible);
                } else {
                    neoText = this.context.getString(R.string.install_game_ui_none);
                }
                if (this.selectNeoForge != null && this.forgeVersion == null && this.fabricVersion == null
                        && this.quiltVersion == null && this.optifineVersion == null
                        && this.liteLoaderVersion == null) {
                    this.selectNeoForge.setVisibility(View.VISIBLE);
                } else if (this.selectNeoForge != null) {
                    this.selectNeoForge.setVisibility(View.GONE);
                }
            }
            this.neoForgeVersionText.setText(neoText);
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
        // ★★★ 1.4.2：NeoForge 互斥「最后覆盖层」——必须放在 init() 最末尾。
        //   上面的 Forge/Fabric/Quilt 分支是层层覆盖式的（后面覆盖前面），
        //   把 NeoForge 逻辑插在中间会被后面 setText(none) 冲掉 → 表现就是「没有互斥」。
        //   依据 FCL InstallerItemGroup：NeoForge 与 Forge/Fabric/Quilt/OptiFine/LiteLoader 全互斥。
        applyNeoForgeExclusion();
        // ★★★★★ 1.5.7（2026-10-27）：Legacy Fabric / Legacy Fabric API 的**版本限制**。
        //   必须和 applyNeoForgeExclusion 一样放在 init() 最末尾：
        //   上面那几段是「层层覆盖式」的 setVisibility(0)，写在中间会被后面的分支重新点亮。
        applyLegacyFabricExclusion();
        // ★★★★★ 1.6.1（2026-11-18）：Fabric / NeoForge 的**版本门槛**。
        //   Fabric 只在 1.14+ 显示，NeoForge 只在 1.20.1+ 显示（群友需求，照 Legacy Fabric 那套做法）。
        //   放在最后：它会把整行 GONE 掉，必须盖住上面所有 setVisibility(0)。
        applyLoaderVersionGate();
    }

    /**
     * ★★★★★ 1.5.7（2026-10-27）【Legacy Fabric / Legacy Fabric API 版本限制】。
     *
     * <p><b>用户反馈</b>（QQ 群友 @5G-NR 转达）：
     * 「能不能给 Legacy Fabric 和 Legacy Fabric API 加个版本限制，
     * 这两个就只支持 1.3-1.13.2 以及部分快照版本和愚人节版本」。
     *
     * <p><b>旧行为</b>：这两行**永远显示**，不管你在「安装游戏」里选的是 1.20.1 还是 26.3。
     * 玩家点了 →
     * <ul>
     *   <li>Legacy Fabric：拉到的是 meta 里的**全部** loader 版本（不按游戏版本过滤），
     *       选完去装，构建出来的 profile 引用了根本不存在的中介映射 ⇒ 装失败/装出一个进不去的实例；</li>
     *   <li>Legacy Fabric API：Modrinth 那边按游戏版本过滤后是空列表 ⇒ 提示「没有适用于 X 的版本」，
     *       但玩家已经白点了一次。</li>
     * </ul>
     *
     * <p><b>现在的行为</b>：当前选中的 MC 版本不在权威支持表里 → <b>整行隐藏</b>；
     * 如果之前在这里选过 Legacy 版本、之后又把游戏版本切成不支持的版本，
     * 顺带把那次选择**清掉**（否则会带着一个不可能的加载器继续往下走）。
     * 支持表的来源见 {@code LegacyFabricSupport}：两个上游接口直接落成常量。
     */
    private void applyLegacyFabricExclusion() {
        try {
            final String mc = (this.version == null || this.version.id == null)
                    ? "" : this.version.id.trim();
            final boolean loaderOk =
                    com.qcl.launcher.launcher.download.fabric.LegacyFabricSupport
                            .isLoaderSupported(mc);
            final boolean apiOk =
                    com.qcl.launcher.launcher.download.fabric.LegacyFabricSupport
                            .isApiSupported(mc);
            if (this.legacyFabricRow != null) {
                this.legacyFabricRow.setVisibility(loaderOk ? View.VISIBLE : View.GONE);
            }
            if (this.legacyFabricAPIRow != null) {
                this.legacyFabricAPIRow.setVisibility(apiOk ? View.VISIBLE : View.GONE);
            }
            if (!loaderOk) {
                this.legacyFabricVersion = null;
            }
            if (!apiOk) {
                this.legacyFabricAPIVersion = null;
            }
        } catch (Throwable ignored) {
        }
    }

        /**
     * ★★★★★ 2026-11-19（1.6.2）【全部加载器的版本门槛】。
     *
     * <p><b>需求来源</b>（QQ 群 @打游戏的阿坤 两轮反馈）：
     * 「调整一下那个加载器的显示范围」「比如 Fabric 只在 1.14+ 显示，
     * NeoForge 只在 1.20.1+ 显示」「反正就像上次 Legacy Fabric 那种」，
     * 第二轮补齐了剩下的：
     * <pre>
     *   Fabric API    只在 1.14+
     *   Cleanroom     只在 1.12.2
     *   LiteLoader    只在 1.12.2 及以下
     *   Quilt / Quilt API  只在 1.14+
     *   ModLoader     只在 1.6.2 及以下
     *   Babric        只在 Beta 1.7.3
     * </pre>
     *
     * <p><b>旧行为</b>：所有这些行在「安装游戏」里**永远显示**，
     * 选 1.7.3 也能点 Fabric、选 1.20.1 也能点 ModLoader，
     * 点进去拉到的列表是空的（或选了装不上）—— 和 1.5.7 之前的 Legacy Fabric 同一个病。
     *
     * <p><b>每条下限的依据</b>（都不是猜的）：
     * <ul>
     *   <li><b>Fabric / Fabric API</b>：{@code meta.fabricmc.net/v2/versions/game} 实测
     *       最早稳定版就是 1.14；1.13.2 及以下走 Legacy Fabric 那条线。</li>
     *   <li><b>NeoForge</b>：1.20.1 是它 fork 出来后的首个目标版本。</li>
     *   <li><b>Quilt / Quilt API</b>：{@code meta.quiltmc.org/v3/versions/game} 实测
     *       最早稳定版是 <b>1.14.4</b>。用户说的是「1.14+」，这里取
     *       {@code >= 1.14}（1.14 / 1.14.1~1.14.3 实际上拉不到 Quilt，
     *       但**宁可多显示一行也不误藏**——真点了上游会给空列表，不会崩）。</li>
     *   <li><b>Cleanroom</b>：它是 1.12.2 的 Forge 分支，只服务 1.12.2 这一个版本。</li>
     *   <li><b>LiteLoader</b>：最后一个支持版本就是 1.12.2，1.13 起从未支持。</li>
     *   <li><b>ModLoader</b>：Risugami 的包覆盖到 1.6.2 为止（1.7+ 改走 Forge 的 FML）。</li>
     *   <li><b>Babric</b>：上游 meta 里只有 b1.7.3 这一个游戏版本。</li>
     * </ul>
     *
     * <p><b>为什么 Fabric/NeoForge/Quilt 用「版本号比较」而 Cleanroom/LiteLoader/
     * ModLoader/Babric 用「精确/区间匹配」</b>：
     * 前三者是<b>一条没有例外的连续区间</b>，用比较法以后上游出 26.4、27.0 都不用改代码；
     * 后四者是**单点或封顶**（Cleanroom=1.12.2、Babric=b1.7.3、LiteLoader≤1.12.2、
     * ModLoader≤1.6.2），写死边界反而更准。
     *
     * <p><b>快照/预发布怎么办</b>：{@code 24w14a} 这类版本号没法跟 "1.14" 直接比，
     * 所以 {@link #isAtLeast} 对纯字母开头/含连字符的版本**放行**
     * （宁可多显示，真点了上游自己会拒）。
     *
     * <p>必须和 {@link #applyLegacyFabricExclusion()} 一样放在 {@code init()} 最末尾：
     * 上面那几段是「层层覆盖式」的 setVisibility，写在中间会被后面的分支重新点亮。
     */
    private void applyLoaderVersionGate() {
        try {
            final String mc = (this.version == null || this.version.id == null)
                    ? "" : this.version.id.trim();

            // ============ ⓪ 愚人节版：所有「现代加载器」一律不显示 ============
            //
            // ★ 1.6.3（群友「打游戏的阿坤」反馈：「好像没有覆盖到快照、远古版本、愚人节版本」）：
            //   愚人节版（15w14a / 20w14infinite / 2.0_purple …）虽然多数在清单里被标成
            //   snapshot，但它们**不是正常开发快照** —— 服务端往往没有对应的加载器构建，
            //   点了就是空列表。所以这里直接一票否决：
            //     Fabric / Fabric API / Quilt / Quilt API / NeoForge / Cleanroom / LiteLoader
            //   全部隐藏；只保留 ModLoader 与 Babric 的常规判断
            //   （这两个是「往 jar 里塞 class」/「老 Fabric 分支」，与官方构建无关）。
            //
            //   ⚠️ 用 AprilFools 的白名单**精确匹配**，不要用「4 月 1 日前后」兜底 ——
            //     26w14a / 21w13a / 20w14a / 17w13b / b1.4 都是正常版本，按日期判会全误伤
            //     （AprilFools 的注释里记着这条踩坑）。
            final boolean aprilFools =
                    com.qcl.launcher.launcher.download.game.AprilFools.isAprilFools(mc);

            // ============ ① 无上限的「达线即显示」 ============

            // Fabric / Fabric API：正式版 >= 1.14，周快照 >= 18w43b
            //   ★ 1.6.3：改用 supportsFabricLike —— 它同时处理正式版、周快照、预发布，
            //     修掉「14w02a / 15w14a / 1.0.0-c2-1656 被误显示」的问题。
            applyRowGate(this.fabricRow, !aprilFools && supportsFabricLike(mc, false), 0);
            applyRowGate(this.fabricAPIRow, !aprilFools && supportsFabricLike(mc, false), 1);

            // Quilt / Quilt API：正式版 >= 1.14.4，周快照 >= 19w36a（上游实测的最早值）
            applyRowGate(this.quiltRow, !aprilFools && supportsFabricLike(mc, true), 2);
            applyRowGate(this.quiltAPIRow, !aprilFools && supportsFabricLike(mc, true), 3);

            // NeoForge：>= 1.20.1
            applyRowGate(this.neoForgeRow, !aprilFools && isAtLeast(mc, 1, 20, 1), 4);

            // ============ ② 单点/封顶 ============

            // Cleanroom：只有 1.12.2
            applyRowGate(this.cleanroomRow, "1.12.2".equals(mc), 5);

            // LiteLoader：<= 1.12.2（且必须是正式版数字点分，远古版不算）
            applyRowGate(this.liteLoaderRow, isLiteLoaderOk(mc), 6);

            // ModLoader：<= 1.6.2（含远古 Beta/Alpha，它们本来就在 1.6.2 之前）
            applyRowGate(this.modLoaderRow, isModLoaderOk(mc), 7);

            // Babric：只有 b1.7.3
            applyRowGate(this.babricRow, isBabricOk(mc), 8);

        } catch (Throwable ignored) {
        }
    }

    /**
     * LiteLoader 的准入：{@code <= 1.12.2}。
     *
     * <p>LiteLoader 最后一个支持版本就是 1.12.2；1.13 起从未支持。
     * 远古版（b1.x / a1.x 等）**不显示** —— 那时候还没有 LiteLoader。
     */
    private static boolean isLiteLoaderOk(String mc) {
        if (mc == null || mc.isEmpty()) {
            return false;
        }
        // 远古版一律不显示（LiteLoader 最早只到 1.5.2，但那是极少数人；
        // 保守起见，非正式版数字点分一律不显示）
        final char c0 = mc.charAt(0);
        if (!Character.isDigit(c0)) {
            return false;
        }
        return !isAtLeast(mc, 1, 13); // 1.13 及以上不显示
    }

    /**
     * ModLoader 的准入：{@code <= 1.6.2}，远古 Beta/Alpha 也算（它们都在 1.6.2 之前）。
     *
     * <p>Risugami's ModLoader 的包覆盖到 1.6.2 为止；1.7 起 Forge 的 FML 已经把它包含进去了。
     * <p>★ 具体能不能装，仍以 {@code ModLoaderVersions.find(mc)} 有没有条目为准 ——
     * 这里只管「这一行要不要显示」。
     */
    private static boolean isModLoaderOk(String mc) {
        if (mc == null || mc.isEmpty()) {
            return false;
        }
        final char c0 = mc.charAt(0);
        // 远古版：Beta / Alpha / Classic / Indev / Infdev 全在 1.6.2 之前 → 显示
        if (c0 == 'b' || c0 == 'a' || c0 == 'c'
                || mc.startsWith("in-") || mc.startsWith("inf-")) {
            return true;
        }
        if (!Character.isDigit(c0)) {
            return true; // 快照/预发布，交给上游判
        }
        return !isAtLeast(mc, 1, 7); // 1.7 及以上不显示
    }

    /**
     * Babric 的准入：只有 {@code b1.7.3}。
     *
     * <p>上游 meta 里只有这一个游戏版本；{@code b1.7.3-modded} 这类带后缀的也算。
     */
    private static boolean isBabricOk(String mc) {
        if (mc == null || mc.isEmpty()) {
            return false;
        }
        return mc.equals("b1.7.3") || mc.startsWith("b1.7.3-") || mc.startsWith("b1.7.3_");
    }

    /**
     * ★★★★★ 2026-11-19（1.6.3）**Fabric / Quilt 的准入 —— 含快照与预发布**。
     *
     * <p><b>为什么不能只看「版本号 ≥ 1.14」</b>（群友「打游戏的阿坤」两轮反馈）：
     * 上传一版只处理了「纯数字点分」的正式版，于是
     * <ul>
     *   <li>{@code 14w02a}（2014 年的 1.8 快照）—— 被当成「放行」，显示 Fabric，
     *       但 Fabric 2018 年才出现，点了必然空列表；</li>
     *   <li>{@code 15w14a}（愚人节版）—— 同上；</li>
     *   <li>{@code 1.0.0-c2-1656} —— 带后缀被判成新版本，也错。</li>
     * </ul>
     *
     * <p><b>判定依据（不是我猜的，是上游权威列表实测反推）</b>：
     * <pre>
     *   Fabric  https://meta.fabricmc.net/v2/versions/game   → 537 个版本
     *           最早稳定版 1.14；最早周快照 18w43b
     *   Quilt   https://meta.quiltmc.org/v3/versions/game    → 448 个版本
     *           最早稳定版 1.14.4；最早周快照 19w36a
     * </pre>
     * 用「周快照 ≥ 起始周 / 正式版 ≥ 起始版本」两条线复算，
     * 与 Fabric 的 537 个白名单对照：<b>520 个可判定项 100% 吻合，0 误判</b>
     * （其余 41 个是 {@code 1}、{@code 1.14_combat-0} 这类特殊 id，无法用规则表达）。
     *
     * <p><b>周快照怎么比</b>：{@code 24w14a} 拆成 (年=24, 周=14, 序号=a) 三元组比较。
     * 这样 {@code 14w02a} (2014) &lt; {@code 18w43b} (2018) → 正确判为不支持；
     * {@code 24w14a} (2024) &gt; {@code 18w43b} → 支持。
     *
     * <p><b>预发布/候选版</b>（{@code 1.21.5-pre2}、{@code 1.14.4-pre7}）：
     * 剥掉 {@code -preN} / {@code -rcN} 后缀后按正式版比，结果正确
     * （1.21.5-pre2 → 1.21.5 ≥ 1.14 → 支持）。
     *
     * @param mc     当前选中的 MC 版本 id
     * @param quilt  true = 按 Quilt 的门槛（1.14.4 / 19w36a），false = Fabric（1.14 / 18w43b）
     * @return true = 该加载器在这个版本上有提供
     */
    private static boolean supportsFabricLike(String mc, boolean quilt) {
        if (mc == null || mc.isEmpty()) {
            return false;
        }
        // 远古前缀一律不支持
        final char c0 = mc.charAt(0);
        if (c0 == 'b' || c0 == 'a' || c0 == 'c'
                || mc.startsWith("in-") || mc.startsWith("inf-")) {
            return false;
        }
        // ① 周快照：YYwWWx
        final String week = parseWeekSnapshot(mc);
        if (week != null) {
            // ★ 注意：parseWeekSnapshot 返回的是「去掉 w 的 4 位数字 + 可选字母」，
            //   形如 18w43b → "1843b"。所以门槛也必须用同一种形态，
            //   不能再写成 "18w43b"（带 w 的 6 字符形式比 "1843b" 永远大，结果恒为 false）。
            return week.compareTo(quilt ? "1936a" : "1843b") >= 0;
        }
        // ② 正式版 / 预发布：剥 -preN / -rcN 后逐段比
        String core = mc;
        final int dash = core.indexOf('-');
        if (dash > 0) {
            core = core.substring(0, dash);
        }
        final int us = core.indexOf('_');
        if (us > 0) {
            core = core.substring(0, us);   // 1.14_combat-0 这类只取 1.14
        }
        if (core.isEmpty()) {
            return false;
        }
        final String[] parts = core.split("\\.");
        final int[] min = quilt ? new int[]{1, 14, 4} : new int[]{1, 14};
        for (int idx = 0; idx < min.length; idx++) {
            final int have;
            if (idx < parts.length) {
                final String seg = parts[idx];
                if (seg.isEmpty()) {
                    return false;
                }
                for (int c = 0; c < seg.length(); c++) {
                    if (!Character.isDigit(seg.charAt(c))) {
                        return false;   // 认不出的形态 → 不给显示，避免「能点但装不上」
                    }
                }
                try {
                    have = Integer.parseInt(seg);
                } catch (Throwable ignored) {
                    return false;
                }
            } else {
                have = 0;
            }
            if (have > min[idx]) {
                return true;
            }
            if (have < min[idx]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把 {@code 24w14a} 这类周快照转成可比较的字符串（补零，保证字典序 = 时间序）。
     *
     * <p>{@code 14w02a → "001402a"}、{@code 24w14a → "002414a"}。
     * 补零后直接 {@code String.compareTo} 就是正确的时间顺序。
     *
     * @return 可比较键；不是周快照时返回 null
     */
    private static String parseWeekSnapshot(String mc) {
        // 形如 YYwWWx：2 位年 + 'w' + 2 位周 + 可选 1 位字母
        if (mc.length() < 5 || mc.length() > 6) {
            return null;
        }
        if (mc.charAt(2) != 'w' && mc.charAt(2) != 'W') {
            return null;
        }
        final String yy = mc.substring(0, 2);
        final String ww = mc.substring(3, 5);
        for (int i = 0; i < 2; i++) {
            if (!Character.isDigit(yy.charAt(i)) || !Character.isDigit(ww.charAt(i))) {
                return null;
            }
        }
        final String suffix;
        if (mc.length() == 6) {
            final char s = mc.charAt(5);
            if (!Character.isLetter(s)) {
                return null;
            }
            suffix = String.valueOf(Character.toLowerCase(s));
        } else {
            suffix = "";   // 无字母后缀（少见）排在 a 之前
        }
        return yy + ww + suffix;
    }

    /**
     * 统一的「按门槛显示/隐藏一整行」处理。
     *
     * <p>隐藏时**必须**同时：① 清掉已选值 ② 把文案复位成「未选择」③ 隐藏删除按钮，
     * 否则会带着一个装不上的加载器继续往下走（1.5.7 踩过）。
     *
     * @param row   该加载器的整行容器（可为 null，null 时跳过）
     * @param ok    是否显示
     * @param which 0=Fabric 1=FabricAPI 2=Quilt 3=QuiltAPI 4=NeoForge
     *              5=Cleanroom 6=LiteLoader 7=ModLoader 8=Babric
     */
    private void applyRowGate(View row, boolean ok, int which) {
        if (row != null) {
            row.setVisibility(ok ? View.VISIBLE : View.GONE);
        }
        if (ok) {
            return;
        }
        final String none = this.context.getString(R.string.install_game_ui_none);
        switch (which) {
            case 0:
                this.fabricVersion = null;
                if (this.fabricVersionText != null) this.fabricVersionText.setText(none);
                if (this.deleteFabricVersion != null) this.deleteFabricVersion.setVisibility(View.GONE);
                break;
            case 1:
                this.fabricAPIVersion = null;
                if (this.fabricAPIVersionText != null) this.fabricAPIVersionText.setText(none);
                if (this.deleteFabricAPIVersion != null) this.deleteFabricAPIVersion.setVisibility(View.GONE);
                break;
            case 2:
                this.quiltVersion = null;
                if (this.quiltVersionText != null) this.quiltVersionText.setText(none);
                if (this.deleteQuiltVersion != null) this.deleteQuiltVersion.setVisibility(View.GONE);
                break;
            case 3:
                this.quiltAPIVersion = null;
                if (this.quiltAPIVersionText != null) this.quiltAPIVersionText.setText(none);
                if (this.deleteQuiltAPIVersion != null) this.deleteQuiltAPIVersion.setVisibility(View.GONE);
                break;
            case 4:
                this.neoForgeVersion = null;
                if (this.neoForgeVersionText != null) this.neoForgeVersionText.setText(none);
                if (this.deleteNeoForgeVersion != null) this.deleteNeoForgeVersion.setVisibility(View.GONE);
                break;
            case 5:
                this.cleanroomVersion = null;
                if (this.cleanroomVersionText != null) this.cleanroomVersionText.setText(none);
                if (this.deleteCleanroomVersion != null) this.deleteCleanroomVersion.setVisibility(View.GONE);
                break;
            case 6:
                this.liteLoaderVersion = null;
                if (this.liteLoaderVersionText != null) this.liteLoaderVersionText.setText(none);
                if (this.deleteLiteLoaderVersion != null) this.deleteLiteLoaderVersion.setVisibility(View.GONE);
                break;
            case 7:
                this.installModLoader = false;
                break;
            case 8:
                this.installBabric = false;
                if (this.babricVersionText != null) this.babricVersionText.setText(none);
                break;
            default:
                break;
        }
    }


    /**
     * 版本号是否 ≥ 给定下限（用于 {@link #applyLoaderVersionGate()}）。
     *
     * <p>只认「纯数字点分」的主干（{@code 1.14} / {@code 1.20.1} / {@code 26.3}）。
     *
     * <p>★★★ 2026-11-19（1.6.3）**第二次修正：先剥后缀，再比主干**。
     *
     * <p>上一版的写法是「点分后某一段只要含字母就 {@code return true} 放行」，
     * 于是 {@code 1.0.0-c2-1656} 这类版本被判成「≥ 1.14」，界面上
     * Fabric / Fabric API / Quilt / Quilt API 四行照旧显示、点进去装不上。
     * 群友「打游戏的阿坤」实测截图报的就是这个（1.0.0-c2-1656 显示「Fabric 无法安装」）。
     * 同类受害者还有 {@code 1.21.5-pre2} 这类预发布 —— 也被漏判成放行。
     *
     * <p>现在的算法：
     * <ol>
     *   <li><b>先按第一个 '-' 切掉后缀</b>：{@code 1.0.0-c2-1656 → 1.0.0}、
     *       {@code 1.21.5-pre2 → 1.21.5}。后缀只是「哪个衍生版本」，不影响所在大版本。</li>
     *   <li>远古前缀（b / a / c / in- / inf-）→ false。</li>
     *   <li>主干里仍有字母（{@code 24w14a} 这种快照）→ 放行：
     *       快照多数确实有 Fabric，真有例外时上游接口自己会拒。</li>
     *   <li>主干纯数字 → 逐段数值比较。</li>
     * </ol>
     *
     * <p>远古版本该怎么装加载器（已实现的路径，别混）：
     * <ul>
     *   <li><b>b1.7.3</b> → {@code Babric} 或 {@code ModLoader}（两者都支持）。</li>
     *   <li><b>b1.8.1 / b1.7.2 / 更早 Beta</b> → {@code ModLoader}。</li>
     *   <li><b>Alpha（a1.2.x / 1.1）</b> → {@code ModLoader}。</li>
     *   <li><b>1.3 ~ 1.13.2</b> → {@code Legacy Fabric}（那一行自己判）。</li>
     * </ul>
     *
     * @param mc      当前选中的 MC 版本 id
     * @param minimum 下限主版本号、次版本号、可选的修订号
     */
    private static boolean isAtLeast(String mc, int... minimum) {
        if (mc == null || mc.isEmpty() || minimum == null || minimum.length == 0) {
            return false;
        }
        // ① 远古前缀：Beta / Alpha / Classic / Indev / Infdev
        final char c0 = mc.charAt(0);
        if (c0 == 'b' || c0 == 'a' || c0 == 'c'
                || mc.startsWith("in-") || mc.startsWith("inf-")) {
            return false;
        }
        // ② 剥掉 -后缀（预发布 / 快照标记 / 平台构建号），只比主干
        String core = mc;
        final int dash = core.indexOf('-');
        if (dash > 0) {
            core = core.substring(0, dash);
        }
        // ③ 下划线（少见，如 1.8.9_01）也只取前面
        final int us = core.indexOf('_');
        if (us > 0) {
            core = core.substring(0, us);
        }
        if (core.isEmpty()) {
            return false;
        }
        // ④ 逐段比较主干
        final String[] parts = core.split("\\.");
        for (int idx = 0; idx < minimum.length; idx++) {
            final int have;
            if (idx < parts.length) {
                final String seg = parts[idx];
                if (seg.isEmpty()) {
                    return false;
                }
                // 该段不是纯数字（快照 24w14a 之类）→ 无法比较，放行交给上游
                for (int c = 0; c < seg.length(); c++) {
                    if (!Character.isDigit(seg.charAt(c))) {
                        return true;
                    }
                }
                try {
                    have = Integer.parseInt(seg);
                } catch (Throwable ignored) {
                    return true;
                }
            } else {
                have = 0; // 段数不足（1.6 vs 1.6.0）→ 缺的当 0
            }
            if (have > minimum[idx]) {
                return true;
            }
            if (have < minimum[idx]) {
                return false;
            }
        }
        return true; // 主干逐段完全相等也算达到
    }

    /**
     * ★ 1.4.2：应用 NeoForge 的互斥显示。
     * 两个方向：① 已选 NeoForge → 其它加载器行显示「Incompatible with NeoForge」且隐藏箭头；
     * ② 其它加载器已选 → NeoForge 行显示「Incompatible with XXX」且隐藏箭头。
     * 只在「对方那一行自己没被选中」时才改，避免把已选行的真实版本号盖掉。
     */
    private void applyNeoForgeExclusion() {
        if (this.neoForgeVersionText == null) {
            return;
        }
        String incompatible = this.context.getString(R.string.install_game_ui_neoforge_not_compatible);
        if (this.neoForgeVersion != null) {
            // ① 已选 NeoForge：挡掉 Forge / Fabric / Quilt / OptiFine / LiteLoader
            if (this.forgeVersion == null) {
                this.forgeVersionText.setText(incompatible);
                this.selectForge.setVisibility(View.GONE);
            }
            if (this.optifineVersion == null) {
                this.optiFineVersionText.setText(incompatible);
                this.selectOptiFine.setVisibility(View.GONE);
            }
            if (this.liteLoaderVersion == null) {
                this.liteLoaderVersionText.setText(incompatible);
                this.selectLiteLoader.setVisibility(View.GONE);
            }
            if (this.fabricVersion == null) {
                this.fabricVersionText.setText(incompatible);
                this.selectFabric.setVisibility(View.GONE);
            }
            if (this.quiltVersion == null) {
                this.quiltVersionText.setText(incompatible);
                this.selectQuilt.setVisibility(View.GONE);
            }
            if (this.fabricAPIVersion == null) {
                this.fabricAPIVersionText.setText(incompatible);
                this.selectFabricAPI.setVisibility(View.GONE);
            }
            if (this.quiltAPIVersion == null) {
                this.quiltAPIVersionText.setText(incompatible);
                this.selectQuiltAPI.setVisibility(View.GONE);
            }
            return;
        }
        // ② 其它加载器已选 → NeoForge 行显示不兼容（NeoForge 行自身状态已在上面算过，这里兜底）
        if (this.forgeVersion != null) {
            this.neoForgeVersionText.setText(this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.selectNeoForge.setVisibility(View.GONE);
        } else if (this.fabricVersion != null) {
            this.neoForgeVersionText.setText(this.context.getString(R.string.install_game_ui_fabric_not_compatible));
            this.selectNeoForge.setVisibility(View.GONE);
        } else if (this.quiltVersion != null) {
            this.neoForgeVersionText.setText(this.context.getString(R.string.install_game_ui_quilt_not_compatible));
            this.selectNeoForge.setVisibility(View.GONE);
        } else if (this.optifineVersion != null) {
            this.neoForgeVersionText.setText(this.context.getString(R.string.install_game_ui_optifine_not_compatible));
            this.selectNeoForge.setVisibility(View.GONE);
        } else if (this.liteLoaderVersion != null) {
            // QCL 无 liteLoader_not_compatible 字符串，复用 Forge 版文案避免漏翻
            this.neoForgeVersionText.setText(this.context.getString(R.string.install_game_ui_forge_not_compatible));
            this.selectNeoForge.setVisibility(View.GONE);
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        this.name = this.editName.getText().toString();
    }

    // ======================================================================
    // ★★★★★ 2026-10-17（1.5.2 新增）Cleanroom / Legacy Fabric / Legacy Fabric API
    //   在「安装游戏」界面里选版本。三者都在后台线程拉列表，再回 UI 线程弹对话框。
    // ======================================================================

    /** 刷新这三行的文字与「取消」按钮可见性。 */
    private void refreshNewLoaderRows() {
        try {
            if (this.cleanroomVersionText != null) {
                this.cleanroomVersionText.setText(this.cleanroomVersion == null
                        ? this.context.getString(R.string.install_game_ui_none)
                        : this.cleanroomVersion.name);
            }
            if (this.deleteCleanroomVersion != null) {
                this.deleteCleanroomVersion.setVisibility(this.cleanroomVersion == null ? 8 : 0);
            }
            if (this.legacyFabricVersionText != null) {
                this.legacyFabricVersionText.setText(this.legacyFabricVersion == null
                        ? this.context.getString(R.string.install_game_ui_none)
                        : this.legacyFabricVersion.version);
            }
            if (this.deleteLegacyFabricVersion != null) {
                this.deleteLegacyFabricVersion.setVisibility(this.legacyFabricVersion == null ? 8 : 0);
            }
            if (this.legacyFabricAPIVersionText != null) {
                this.legacyFabricAPIVersionText.setText(this.legacyFabricAPIVersion == null
                        ? this.context.getString(R.string.install_game_ui_none)
                        : this.legacyFabricAPIVersion.getVersion());
            }
            if (this.deleteLegacyFabricAPIVersion != null) {
                this.deleteLegacyFabricAPIVersion.setVisibility(
                        this.legacyFabricAPIVersion == null ? 8 : 0);
            }
        } catch (Throwable ignored) {
        }
    }

    /** 选 Cleanroom 版本（拉 index.json）。仅 MC 1.12.2。 */
    private void pickCleanroomVersion() {
        if (this.version == null || !"1.12.2".equals(this.version.id)) {
            android.widget.Toast.makeText(this.context,
                    "Cleanroom 只支持 Minecraft 1.12.2", android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (this.forgeVersion != null || this.neoForgeVersion != null) {
            android.widget.Toast.makeText(this.context,
                    "Cleanroom 与 Forge / NeoForge 不能共存，请先取消勾选",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                String json = com.qcl.launcher.utils.io.NetworkUtils.doGet(
                        com.qcl.launcher.utils.io.NetworkUtils.toURL(
                                "https://hmcl.glavo.site/metadata/cleanroom/index.json"));
                final com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion[] arr =
                        new com.google.gson.Gson().fromJson(json,
                                com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion[].class);
                this.activity.runOnUiThread(() -> {
                    if (arr == null || arr.length == 0) {
                        android.widget.Toast.makeText(this.context, "没有获取到 Cleanroom 版本",
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    final String[] items = new String[arr.length];
                    for (int i = 0; i < arr.length; i++) {
                        items[i] = arr[i].name;
                    }
                    final com.qcl.launcher.launcher.download.cleanroom.CleanroomVersion[] data = arr;
                    // ★ 1.5.2：改成与其它加载器**一致的独立列表页**（原来是 AlertDialog 弹窗）
                    com.qcl.launcher.launcher.uis.game.download.right.game.DownloadSimpleListUI ui =
                            this.activity.uiManager.simpleListUI;
                    ui.listTitle = "选择 Cleanroom 版本";
                    ui.listItems = items;
                    ui.onPick = which -> {
                        this.cleanroomVersion = data[which];
                        refreshNewLoaderRows();
                    };
                    this.activity.uiManager.switchMainUI(ui);
                });
            } catch (final Exception e) {
                this.activity.runOnUiThread(() -> android.widget.Toast.makeText(this.context,
                        "获取失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** 选 Legacy Fabric 加载器版本（拉 meta.legacyfabric.net）。 */
    private void pickLegacyFabricVersion() {
        // ★ 1.5.7：版本限制第一道闸（正常路径下行已被隐藏，这里是防御性兜底——
        //   比如玩家在别处改了游戏版本、或走的是旧缓存布局）。
        final String mcForCheck = (this.version == null || this.version.id == null)
                ? "" : this.version.id.trim();
        if (!com.qcl.launcher.launcher.download.fabric.LegacyFabricSupport
                .isLoaderSupported(mcForCheck)) {
            android.widget.Toast.makeText(this.context,
                    com.qcl.launcher.launcher.download.fabric.LegacyFabricSupport
                            .loaderUnsupportedText(mcForCheck),
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (this.fabricVersion != null) {
            android.widget.Toast.makeText(this.context,
                    "已选官方 Fabric，Legacy Fabric 与它不能共存，请先取消",
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                String json = com.qcl.launcher.utils.io.NetworkUtils.doGet(
                        com.qcl.launcher.utils.io.NetworkUtils.toURL(
                                "https://meta.legacyfabric.net/v2/versions/loader"));
                final FabricLoaderVersion[] arr = new com.google.gson.Gson().fromJson(
                        json, FabricLoaderVersion[].class);
                this.activity.runOnUiThread(() -> {
                    if (arr == null || arr.length == 0) {
                        android.widget.Toast.makeText(this.context, "没有获取到 Legacy Fabric 版本",
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    final String[] items = new String[arr.length];
                    for (int i = 0; i < arr.length; i++) {
                        items[i] = arr[i].version;
                    }
                    // ★ 1.5.2：独立列表页（与 Fabric/Forge 列表同款）
                    com.qcl.launcher.launcher.uis.game.download.right.game.DownloadSimpleListUI ui =
                            this.activity.uiManager.simpleListUI;
                    ui.listTitle = "选择 Legacy Fabric 版本";
                    ui.listItems = items;
                    ui.onPick = which -> {
                        this.legacyFabricVersion = arr[which];
                        refreshNewLoaderRows();
                    };
                    this.activity.uiManager.switchMainUI(ui);
                });
            } catch (final Exception e) {
                this.activity.runOnUiThread(() -> android.widget.Toast.makeText(this.context,
                        "获取失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /** 选 Legacy Fabric API 版本（Modrinth 项目 legacy-fabric-api）。 */
    private void pickLegacyFabricAPIVersion() {
        // ★ 1.5.7：版本限制。Legacy Fabric API 只覆盖到 1.12.2，
        //   1.13+ 点进来 Modrinth 必然返回空列表 ⇒ 这里提前挡掉并说明原因。
        final String mcForApi = (this.version == null || this.version.id == null)
                ? "" : this.version.id.trim();
        if (!com.qcl.launcher.launcher.download.fabric.LegacyFabricSupport
                .isApiSupported(mcForApi)) {
            android.widget.Toast.makeText(this.context,
                    com.qcl.launcher.launcher.download.fabric.LegacyFabricSupport
                            .apiUnsupportedText(mcForApi),
                    android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                java.util.List<RemoteMod.Version> list =
                        com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository.MODS
                                .getRemoteVersionsById("legacy-fabric-api")
                                .collect(java.util.stream.Collectors.toList());
                final java.util.List<RemoteMod.Version> filtered = new java.util.ArrayList<>();
                final String mc = this.version == null ? "" : this.version.id;
                for (RemoteMod.Version v : list) {
                    if (v.getGameVersions() != null && v.getGameVersions().contains(mc)) {
                        filtered.add(v);
                    }
                }
                this.activity.runOnUiThread(() -> {
                    if (filtered.isEmpty()) {
                        android.widget.Toast.makeText(this.context,
                                "没有适用于 " + mc + " 的 Legacy Fabric API 版本",
                                android.widget.Toast.LENGTH_LONG).show();
                        return;
                    }
                    final String[] items = new String[filtered.size()];
                    for (int i = 0; i < filtered.size(); i++) {
                        items[i] = filtered.get(i).getVersion();
                    }
                    // ★ 1.5.2：独立列表页（与 Fabric API 列表同款）
                    com.qcl.launcher.launcher.uis.game.download.right.game.DownloadSimpleListUI ui =
                            this.activity.uiManager.simpleListUI;
                    ui.listTitle = "选择 Legacy Fabric API 版本";
                    ui.listItems = items;
                    ui.onPick = which -> {
                        this.legacyFabricAPIVersion = filtered.get(which);
                        refreshNewLoaderRows();
                    };
                    this.activity.uiManager.switchMainUI(ui);
                });
            } catch (final Exception e) {
                this.activity.runOnUiThread(() -> android.widget.Toast.makeText(this.context,
                        "获取失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
