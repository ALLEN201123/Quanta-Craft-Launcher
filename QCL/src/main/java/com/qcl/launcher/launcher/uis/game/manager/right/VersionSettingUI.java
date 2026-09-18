/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.AlertDialog$Builder
 *  android.content.Context
 *  android.content.Intent
 *  android.net.Uri
 *  android.os.Environment
 *  android.text.Editable
 *  android.text.TextWatcher
 *  android.util.Log
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.ViewGroup
 *  android.widget.Button
 *  android.widget.CheckBox
 *  android.widget.CompoundButton
 *  android.widget.CompoundButton$OnCheckedChangeListener
 *  android.widget.EditText
 *  android.widget.ImageButton
 *  android.widget.ImageView
 *  android.widget.LinearLayout
 *  android.widget.ProgressBar
 *  android.widget.RadioButton
 *  android.widget.SeekBar
 *  android.widget.SeekBar$OnSeekBarChangeListener
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.appcompat.widget.SwitchCompat
 *  com.tungsten.filepicker.Constants$SELECTION_MODES
 *  com.tungsten.filepicker.FileChooser
 *  com.tungsten.filepicker.FolderChooser
 */
package com.qcl.launcher.launcher.uis.game.manager.right;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.control.ControllerManagerDialog;
import com.qcl.launcher.launcher.launch.RendererCompat;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.Architecture;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.animation.HiddenAnimationUtils;
import com.qcl.launcher.utils.file.DrawableUtils;
import com.qcl.launcher.utils.file.FileUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.platform.MemoryUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import com.tungsten.filepicker.FolderChooser;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
public class VersionSettingUI
extends BaseUI
implements View.OnClickListener,
CompoundButton.OnCheckedChangeListener,
SeekBar.OnSeekBarChangeListener {
    public LinearLayout versionSettingUI;
    public static final int PICK_GAME_DIR_REQUEST_ISOLATED = 7300;
    public static final int SELECT_ICON_REQUEST = 8600;
    public String versionName;
    private ImageView icon;
    private ImageButton editVersionIcon;
    private ImageButton deleteVersionIcon;
    private CheckBox checkIsolateSetting;
    private Button switchToGlobalSetting;
    private LinearLayout isolateSettingLayout;
    private PrivateGameSetting privateGameSetting;
    private LinearLayout showJavaSetting;
    private TextView javaPathText;
    private ImageView showJava;
    private LinearLayout javaSetting;
    private int javaSettingHeight;
    private LinearLayout showGameDirSetting;
    private TextView gameDirText;
    private ImageView showGameDir;
    private LinearLayout gameDirSetting;
    private int gameDirSettingHeight;
    private LinearLayout showGameLauncherSetting;
    private TextView currentLauncher;
    private ImageView showGameLauncher;
    private LinearLayout gameLauncherSetting;
    private int gameLauncherSettingHeight;
    private LinearLayout showBoatRendererSetting;
    private TextView currentBoatRenderer;
    private ImageView showBoatRenderer;
    private LinearLayout boatRendererSetting;
    private int boatRendererSettingHeight;
    private LinearLayout showPojavRendererSetting;
    private TextView currentPojavRenderer;
    private ImageView showPojavRenderer;
    private LinearLayout pojavRendererSetting;
    private int pojavRendererSettingHeight;
    private RadioButton checkJavaAuto;
    private RadioButton checkJava8;
    private RadioButton checkJava17;
    private RadioButton checkJava21;
    private RadioButton checkJava25;
    private TextView java8Path;
    private TextView java17Path;
    private TextView java21Path;
    private TextView java25Path;
    private RadioButton checkGameDirDefault;
    private RadioButton checkGameDirIsolate;
    private RadioButton checkGameDirCustom;
    private EditText editGameDir;
    private ImageButton selectGameDir;
    private RadioButton launchByBoat;
    private RadioButton launchByPojav;
    private RadioButton boatRendererGL4ES114;
    private RadioButton boatRendererVirGL;
    private RadioButton pojavRendererGL4ES114;
    private RadioButton pojavRendererVirGL;
    private CheckBox checkAutoRam;
    private SeekBar ramSeekBar;
    private EditText editRam;
    private ProgressBar ramProgressBar;
    private TextView usedRamText;
    private TextView actualRamText;
    private SeekBar scaleFactorSeekBar;
    private EditText editScaleFactor;
    private SwitchCompat checkLog;
    private SwitchCompat notCheckGameFile;
    private SwitchCompat notCheckForge;
    private SwitchCompat notCheckJVM;
    private EditText editServer;
    private EditText editJVMArgs;
    private Button manageController;
    private TextView currentControlPattern;
    private ControllerManagerDialog controllerManagerDialog;
    private SwitchCompat checkTouchInjector;

    public VersionSettingUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    @SuppressLint(value={"SetTextI18n"})
    public void onCreate() {
        super.onCreate();
        this.versionSettingUI = (LinearLayout)this.activity.findViewById(R.id.ui_version_setting);
        this.icon = (ImageView)this.activity.findViewById(R.id.version_icon_view);
        this.editVersionIcon = (ImageButton)this.activity.findViewById(R.id.edit_version_icon);
        this.deleteVersionIcon = (ImageButton)this.activity.findViewById(R.id.reset_version_icon);
        this.checkIsolateSetting = (CheckBox)this.activity.findViewById(R.id.check_isolated_setting);
        this.switchToGlobalSetting = (Button)this.activity.findViewById(R.id.start_global_game_setting_from_private);
        this.isolateSettingLayout = (LinearLayout)this.activity.findViewById(R.id.isolate_game_setting);
        this.showJavaSetting = (LinearLayout)this.activity.findViewById(R.id.show_java_selector_isolate);
        this.javaPathText = (TextView)this.activity.findViewById(R.id.java_path_text_isolate);
        this.showJava = (ImageView)this.activity.findViewById(R.id.show_java_isolate);
        this.javaSetting = (LinearLayout)this.activity.findViewById(R.id.java_setting_isolate);
        this.showGameDirSetting = (LinearLayout)this.activity.findViewById(R.id.show_game_directory_selector_isolate);
        this.gameDirText = (TextView)this.activity.findViewById(R.id.game_directory_text_isolate);
        this.showGameDir = (ImageView)this.activity.findViewById(R.id.show_game_dir_isolate);
        this.gameDirSetting = (LinearLayout)this.activity.findViewById(R.id.game_dir_setting_isolate);
        this.showGameLauncherSetting = (LinearLayout)this.activity.findViewById(R.id.show_game_launcher_selector_isolate);
        this.currentLauncher = (TextView)this.activity.findViewById(R.id.current_launcher_isolate);
        this.showGameLauncher = (ImageView)this.activity.findViewById(R.id.show_game_launcher_isolate);
        this.gameLauncherSetting = (LinearLayout)this.activity.findViewById(R.id.game_launcher_selector_isolate);
        this.showBoatRendererSetting = (LinearLayout)this.activity.findViewById(R.id.show_boat_render_selector_isolate);
        this.currentBoatRenderer = (TextView)this.activity.findViewById(R.id.current_boat_renderer_isolate);
        this.showBoatRenderer = (ImageView)this.activity.findViewById(R.id.show_boat_renderer_isolate);
        this.boatRendererSetting = (LinearLayout)this.activity.findViewById(R.id.boat_render_selector_isolate);
        this.showPojavRendererSetting = (LinearLayout)this.activity.findViewById(R.id.show_pojav_render_selector_isolate);
        this.currentPojavRenderer = (TextView)this.activity.findViewById(R.id.current_pojav_renderer_isolate);
        this.showPojavRenderer = (ImageView)this.activity.findViewById(R.id.show_pojav_renderer_isolate);
        this.pojavRendererSetting = (LinearLayout)this.activity.findViewById(R.id.pojav_render_selector_isolate);
        this.checkJavaAuto = (RadioButton)this.activity.findViewById(R.id.check_java_path_auto_isolate);
        this.checkJava8 = (RadioButton)this.activity.findViewById(R.id.check_java_path_8_isolate);
        this.checkJava17 = (RadioButton)this.activity.findViewById(R.id.check_java_path_17_isolate);
        this.checkJava21 = (RadioButton)this.activity.findViewById(R.id.check_java_path_21_isolate);
        this.checkJava25 = (RadioButton)this.activity.findViewById(R.id.check_java_path_25_isolate);
        this.java8Path = (TextView)this.activity.findViewById(R.id.java_8_path_isolate);
        this.java17Path = (TextView)this.activity.findViewById(R.id.java_17_path_isolate);
        this.java21Path = (TextView)this.activity.findViewById(R.id.java_21_path_isolate);
        this.java25Path = (TextView)this.activity.findViewById(R.id.java_25_path_isolate);
        this.java8Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/default"));
        this.java17Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE17"));
        this.java21Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE21"));
        this.java25Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE25"));
        this.checkJava25.setEnabled(Architecture.getDeviceArchitecture() != Architecture.ARCH_X86);
        this.checkGameDirDefault = (RadioButton)this.activity.findViewById(R.id.check_default_game_dir_isolate);
        this.checkGameDirIsolate = (RadioButton)this.activity.findViewById(R.id.check_isolate_game_dir_isolate);
        this.checkGameDirCustom = (RadioButton)this.activity.findViewById(R.id.check_custom_game_dir_isolate);
        this.editGameDir = (EditText)this.activity.findViewById(R.id.edit_game_dir_path_isolate);
        this.selectGameDir = (ImageButton)this.activity.findViewById(R.id.select_game_dir_path_isolate);
        this.launchByBoat = (RadioButton)this.activity.findViewById(R.id.launch_by_boat_isolate);
        this.launchByPojav = (RadioButton)this.activity.findViewById(R.id.launch_by_pojav_isolate);
        this.boatRendererGL4ES114 = (RadioButton)this.activity.findViewById(R.id.boat_renderer_gl4es_114_isolate);
        this.boatRendererVirGL = (RadioButton)this.activity.findViewById(R.id.boat_renderer_virgl_isolate);
        this.pojavRendererGL4ES114 = (RadioButton)this.activity.findViewById(R.id.pojav_renderer_gl4es_114_isolate);
        this.pojavRendererVirGL = (RadioButton)this.activity.findViewById(R.id.pojav_renderer_virgl_isolate);
        this.checkAutoRam = (CheckBox)this.activity.findViewById(R.id.check_auto_ram_isolate);
        this.ramSeekBar = (SeekBar)this.activity.findViewById(R.id.ram_seek_bar_isolate);
        this.editRam = (EditText)this.activity.findViewById(R.id.edit_ram_isolate);
        this.usedRamText = (TextView)this.activity.findViewById(R.id.used_ram_text_isolate);
        this.actualRamText = (TextView)this.activity.findViewById(R.id.actual_ram_text_isolate);
        this.ramSeekBar.setMax(MemoryUtils.getMaxAllowedRam(this.context));
        this.ramProgressBar = (ProgressBar)this.activity.findViewById(R.id.ram_progress_bar_isolate);
        this.ramProgressBar.setMax(MemoryUtils.getTotalDeviceMemory(this.context));
        this.scaleFactorSeekBar = (SeekBar)this.activity.findViewById(R.id.edit_scale_factor_isolate);
        this.editScaleFactor = (EditText)this.activity.findViewById(R.id.edit_scale_factor_text_isolate);
        this.scaleFactorSeekBar.setMax(750);
        this.checkLog = (SwitchCompat)this.activity.findViewById(R.id.switch_log_isolate);
        this.notCheckGameFile = (SwitchCompat)this.activity.findViewById(R.id.switch_check_mc_isolate);
        this.notCheckForge = (SwitchCompat)this.activity.findViewById(R.id.switch_check_forge_isolate);
        this.notCheckJVM = (SwitchCompat)this.activity.findViewById(R.id.switch_check_runtime_isolate);
        this.manageController = (Button)this.activity.findViewById(R.id.manage_control_layout_isolate);
        this.manageController.setOnClickListener((View.OnClickListener)this);
        this.currentControlPattern = (TextView)this.activity.findViewById(R.id.control_layout_isolate);
        this.checkTouchInjector = (SwitchCompat)this.activity.findViewById(R.id.switch_touch_injector_isolate);
        this.editServer = (EditText)this.activity.findViewById(R.id.edit_mc_server_isolate);
        this.editServer.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                if (VersionSettingUI.this.privateGameSetting != null) {
                    ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.server = VersionSettingUI.this.editServer.getText().toString();
                    GsonUtils.savePrivateGameSetting(VersionSettingUI.this.privateGameSetting, VersionSettingUI.this.activity.launcherSetting.gameFileDirectory + "/versions/" + VersionSettingUI.this.versionName + "/qcl.cfg");
                }
            }
        });
        this.editJVMArgs = (EditText)this.activity.findViewById(R.id.edit_jvm_arg_isolate);
        this.editJVMArgs.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                if (VersionSettingUI.this.privateGameSetting != null) {
                    ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.extraJavaFlags = VersionSettingUI.this.editJVMArgs.getText().toString();
                    GsonUtils.savePrivateGameSetting(VersionSettingUI.this.privateGameSetting, VersionSettingUI.this.activity.launcherSetting.gameFileDirectory + "/versions/" + VersionSettingUI.this.versionName + "/qcl.cfg");
                }
            }
        });
        this.editVersionIcon.setOnClickListener((View.OnClickListener)this);
        this.deleteVersionIcon.setOnClickListener((View.OnClickListener)this);
        this.checkIsolateSetting.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchToGlobalSetting.setOnClickListener((View.OnClickListener)this);
        this.checkLog.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.notCheckGameFile.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.notCheckForge.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.notCheckJVM.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.showJavaSetting.setOnClickListener((View.OnClickListener)this);
        this.showJava.setOnClickListener((View.OnClickListener)this);
        this.showGameDirSetting.setOnClickListener((View.OnClickListener)this);
        this.showGameDir.setOnClickListener((View.OnClickListener)this);
        this.showGameLauncherSetting.setOnClickListener((View.OnClickListener)this);
        this.showGameLauncher.setOnClickListener((View.OnClickListener)this);
        this.showBoatRendererSetting.setOnClickListener((View.OnClickListener)this);
        this.showBoatRenderer.setOnClickListener((View.OnClickListener)this);
        this.showPojavRendererSetting.setOnClickListener((View.OnClickListener)this);
        this.showPojavRenderer.setOnClickListener((View.OnClickListener)this);
        this.checkJavaAuto.setOnClickListener((View.OnClickListener)this);
        this.checkJava8.setOnClickListener((View.OnClickListener)this);
        this.checkJava17.setOnClickListener((View.OnClickListener)this);
        this.checkJava21.setOnClickListener((View.OnClickListener)this);
        this.checkJava25.setOnClickListener((View.OnClickListener)this);
        this.checkTouchInjector.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkGameDirDefault.setOnClickListener((View.OnClickListener)this);
        this.checkGameDirIsolate.setOnClickListener((View.OnClickListener)this);
        this.checkGameDirCustom.setOnClickListener((View.OnClickListener)this);
        this.selectGameDir.setOnClickListener((View.OnClickListener)this);
        this.editGameDir.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                if (VersionSettingUI.this.privateGameSetting != null) {
                    ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.gameDirSetting.path = VersionSettingUI.this.editGameDir.getText().toString();
                    GsonUtils.savePrivateGameSetting(VersionSettingUI.this.privateGameSetting, VersionSettingUI.this.activity.launcherSetting.gameFileDirectory + "/versions/" + VersionSettingUI.this.versionName + "/qcl.cfg");
                }
            }
        });
        this.launchByBoat.setOnClickListener((View.OnClickListener)this);
        this.launchByPojav.setOnClickListener((View.OnClickListener)this);
        this.boatRendererGL4ES114.setOnClickListener((View.OnClickListener)this);
        this.boatRendererVirGL.setOnClickListener((View.OnClickListener)this);
        this.pojavRendererGL4ES114.setOnClickListener((View.OnClickListener)this);
        this.pojavRendererVirGL.setOnClickListener((View.OnClickListener)this);
        this.checkAutoRam.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.ramSeekBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.editRam.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (VersionSettingUI.this.privateGameSetting != null) {
                    if (!VersionSettingUI.this.editRam.getText().toString().equals("")) {
                        ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.ramSetting.minRam = Integer.parseInt(VersionSettingUI.this.editRam.getText().toString());
                        ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.ramSetting.maxRam = Integer.parseInt(VersionSettingUI.this.editRam.getText().toString());
                        VersionSettingUI.this.ramSeekBar.setProgress(Integer.parseInt(VersionSettingUI.this.editRam.getText().toString()));
                    } else {
                        ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.ramSetting.minRam = 0;
                        ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.ramSetting.maxRam = 0;
                        VersionSettingUI.this.ramSeekBar.setProgress(0);
                    }
                    GsonUtils.savePrivateGameSetting(VersionSettingUI.this.privateGameSetting, VersionSettingUI.this.activity.launcherSetting.gameFileDirectory + "/versions/" + VersionSettingUI.this.versionName + "/qcl.cfg");
                }
            }
        });
        this.scaleFactorSeekBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.editScaleFactor.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (VersionSettingUI.this.privateGameSetting != null) {
                    if (!VersionSettingUI.this.editScaleFactor.getText().toString().equals("")) {
                        ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.scaleFactor = (float)Integer.parseInt(VersionSettingUI.this.editScaleFactor.getText().toString()) / 100.0f;
                        VersionSettingUI.this.scaleFactorSeekBar.setProgress(Integer.parseInt(VersionSettingUI.this.editScaleFactor.getText().toString()) * 10 - 250);
                    } else {
                        ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.scaleFactor = 0.25f;
                        VersionSettingUI.this.scaleFactorSeekBar.setProgress(0);
                    }
                    GsonUtils.savePrivateGameSetting(VersionSettingUI.this.privateGameSetting, VersionSettingUI.this.activity.launcherSetting.gameFileDirectory + "/versions/" + VersionSettingUI.this.versionName + "/qcl.cfg");
                }
            }
        });
        this.javaSetting.post(() -> {
            this.javaSettingHeight = this.javaSetting.getHeight();
            this.javaSetting.setVisibility(8);
        });
        this.gameDirSetting.post(() -> {
            this.gameDirSettingHeight = this.gameDirSetting.getHeight();
            this.gameDirSetting.setVisibility(8);
        });
        this.gameLauncherSetting.post(() -> {
            this.gameLauncherSettingHeight = this.gameLauncherSetting.getHeight();
            this.gameLauncherSetting.setVisibility(8);
        });
        this.boatRendererSetting.post(() -> {
            this.boatRendererSettingHeight = this.boatRendererSetting.getHeight();
            this.boatRendererSetting.setVisibility(8);
        });
        this.pojavRendererSetting.post(() -> {
            this.pojavRendererSettingHeight = this.pojavRendererSetting.getHeight();
            this.pojavRendererSetting.setVisibility(8);
        });
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft((View)this.versionSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.gameManagerUI.startGameSetting.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft((View)this.versionSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.gameManagerUI.startGameSetting.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onLoaded() {
        this.activity.uiManager.gameManagerUI.startGameSetting.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7300 && data != null && resultCode == -1 && this.privateGameSetting != null) {
            uri = data.getData();
            this.gameDirText.setText((CharSequence)UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
            this.editGameDir.setText((CharSequence)UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
            this.privateGameSetting.gameDirSetting.path = UriUtils.getRealPathFromUri_AboveApi19(this.context, uri);
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (requestCode == 7700 && this.controllerManagerDialog != null && data != null) {
            uri = data.getData();
            String pattern = uri.toString();
            this.currentControlPattern.setText((CharSequence)pattern);
            this.privateGameSetting.controlLayout = pattern;
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
            this.controllerManagerDialog.currentPattern = pattern;
            this.controllerManagerDialog.loadList();
        }
        if (requestCode == 3900 && this.controllerManagerDialog != null && data != null) {
            this.controllerManagerDialog.onResult(requestCode, resultCode, data);
        }
        if (requestCode == 8600 && data != null && resultCode == -1) {
            uri = data.getData();
            String path = UriUtils.getRealPathFromUri_AboveApi19(this.context, uri);
            new Thread(() -> {
                FileUtils.copyFile(path, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/icon.png");
                this.activity.runOnUiThread(() -> this.icon.setBackground(DrawableUtils.getDrawableFromFile(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/icon.png")));
            }).start();
        }
    }

    public void refresh(String versionName) {
        this.versionName = versionName;
        String settingPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + versionName + "/qcl.cfg";
        if (new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile((String)settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile((String)settingPath).enable)) {
            this.checkIsolateSetting.setChecked(true);
            this.enableSettingLayout();
            this.privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(settingPath);
        } else {
            this.checkIsolateSetting.setChecked(false);
            this.disableSettingLayout();
            this.privateGameSetting = null;
        }
        this.onRefresh();
    }

    private void enableSettingLayout() {
        for (View view : this.getAllChild((ViewGroup)this.isolateSettingLayout)) {
            view.setAlpha(1.0f);
            view.setEnabled(true);
        }
        this.switchToGlobalSetting.setEnabled(false);
        Log.e((String)"enable", (String)"true");
    }

    private void disableSettingLayout() {
        for (View view : this.getAllChild((ViewGroup)this.isolateSettingLayout)) {
            if (!(view instanceof ViewGroup)) {
                view.setAlpha(0.4f);
            }
            view.setEnabled(false);
        }
        this.switchToGlobalSetting.setEnabled(true);
        Log.e((String)"disable", (String)"true");
    }

    @SuppressLint(value={"SetTextI18n", "UseCompatLoadingForDrawables"})
    private void onRefresh() {
        if (new File(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/icon.png").exists()) {
            this.icon.setBackground(DrawableUtils.getDrawableFromFile(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/icon.png"));
        } else {
            String v = null;
            for (GameListBean bean : this.activity.uiManager.versionListUI.gameList) {
                if (!bean.name.equals(this.versionName)) continue;
                v = bean.version;
                break;
            }
            if (v == null || !v.contains(",")) {
                this.icon.setBackground(this.context.getDrawable(R.drawable.ic_grass));
            } else {
                this.icon.setBackground(this.context.getDrawable(R.drawable.ic_furnace));
            }
        }
        PrivateGameSetting setting = this.privateGameSetting == null ? this.activity.privateGameSetting : this.privateGameSetting;
        this.checkAutoRam.setChecked(setting.ramSetting.autoRam);
        this.ramProgressBar.setProgress(MemoryUtils.getTotalDeviceMemory(this.context) - MemoryUtils.getFreeDeviceMemory(this.context));
        this.ramSeekBar.setProgress(setting.ramSetting.minRam);
        this.editRam.setText((CharSequence)(setting.ramSetting.minRam + ""));
        this.usedRamText.setText((CharSequence)(this.context.getString(R.string.game_setting_ui_used_ram).replace("%s", Float.toString((float)Math.round((float)(MemoryUtils.getTotalDeviceMemory(this.context) - MemoryUtils.getFreeDeviceMemory(this.context)) / 1024.0f * 10.0f) / 10.0f)) + " / " + (float)Math.round((float)MemoryUtils.getTotalDeviceMemory(this.context) / 1024.0f * 10.0f) / 10.0f + " GB"));
        this.actualRamText.setText((CharSequence)(this.context.getString(R.string.game_setting_ui_min_distribution).replace("%s", Float.toString((float)Math.round((float)setting.ramSetting.minRam / 1024.0f * 10.0f) / 10.0f)) + " / " + this.context.getString(R.string.game_setting_ui_actual_distribution).replace("%s", Float.toString((float)Math.round((float)setting.ramSetting.minRam / 1024.0f * 10.0f) / 10.0f))));
        this.scaleFactorSeekBar.setProgress((int)(setting.scaleFactor * 1000.0f) - 250);
        this.editScaleFactor.setText((CharSequence)((int)(setting.scaleFactor * 100.0f) + ""));
        this.checkLog.setChecked(setting.log);
        this.notCheckGameFile.setChecked(setting.notCheckMinecraft);
        this.notCheckForge.setChecked(setting.notCheckForge);
        this.notCheckJVM.setChecked(setting.notCheckJvm);
        this.checkTouchInjector.setChecked(setting.touchInjector);
        this.editGameDir.setText((CharSequence)setting.gameDirSetting.path);
        this.editServer.setText((CharSequence)setting.server);
        this.editJVMArgs.setText((CharSequence)setting.extraJavaFlags);
        this.currentControlPattern.setText((CharSequence)setting.controlLayout);
        if (setting.javaSetting.autoSelect) {
            this.javaPathText.setText((CharSequence)this.context.getString(R.string.game_setting_ui_java_path_auto));
            this.checkJavaAuto.setChecked(true);
            this.checkJava8.setChecked(false);
            this.checkJava17.setChecked(false);
            this.checkJava21.setChecked(false);
            this.checkJava25.setChecked(false);
        } else {
            if (setting.javaSetting.name.equals("default")) {
                this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/default"));
                this.checkJavaAuto.setChecked(false);
                this.checkJava8.setChecked(true);
                this.checkJava17.setChecked(false);
                this.checkJava21.setChecked(false);
                this.checkJava25.setChecked(false);
            }
            if (setting.javaSetting.name.equals("JRE17")) {
                this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE17"));
                this.checkJavaAuto.setChecked(false);
                this.checkJava8.setChecked(false);
                this.checkJava17.setChecked(true);
                this.checkJava21.setChecked(false);
                this.checkJava25.setChecked(false);
            }
            if (setting.javaSetting.name.equals("JRE21")) {
                this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE21"));
                this.checkJavaAuto.setChecked(false);
                this.checkJava8.setChecked(false);
                this.checkJava17.setChecked(false);
                this.checkJava21.setChecked(true);
                this.checkJava25.setChecked(false);
            }
            if (setting.javaSetting.name.equals("JRE25")) {
                this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE25"));
                this.checkJavaAuto.setChecked(false);
                this.checkJava8.setChecked(false);
                this.checkJava17.setChecked(false);
                this.checkJava21.setChecked(false);
                this.checkJava25.setChecked(true);
            }
        }
        if (setting.gameDirSetting.type == 0) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)this.activity.launcherSetting.gameFileDirectory);
            this.checkGameDirDefault.setChecked(true);
            this.checkGameDirIsolate.setChecked(false);
            this.checkGameDirCustom.setChecked(false);
        } else if (setting.gameDirSetting.type == 1) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName));
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirIsolate.setChecked(true);
            this.checkGameDirCustom.setChecked(false);
        } else {
            this.editGameDir.setEnabled(true);
            this.selectGameDir.setEnabled(true);
            this.gameDirText.setText((CharSequence)setting.gameDirSetting.path);
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirIsolate.setChecked(false);
            this.checkGameDirCustom.setChecked(true);
        }
        if (setting.boatLauncherSetting.enable) {
            this.launchByBoat.setChecked(true);
            this.launchByPojav.setChecked(false);
            this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_boat));
        } else {
            this.launchByBoat.setChecked(false);
            this.launchByPojav.setChecked(true);
            this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_pojav));
        }
        if (setting.boatLauncherSetting.renderer.equals("libGL112.so.1") || setting.boatLauncherSetting.renderer.equals("libGL115.so.1") || setting.boatLauncherSetting.renderer.equals("libgl4es_114.so") || setting.boatLauncherSetting.renderer.equals("libvgpu.so") || setting.boatLauncherSetting.renderer.equals("GL4ES115") || setting.boatLauncherSetting.renderer.equals("GL4ES114")) {
            this.boatRendererGL4ES114.setChecked(true);
            this.boatRendererVirGL.setChecked(false);
            this.currentBoatRenderer.setText(this.context.getText(R.string.game_setting_ui_boat_renderer_gl4es_114));
        } else if (setting.boatLauncherSetting.renderer.equals("VirGL")) {
            this.boatRendererGL4ES114.setChecked(false);
            this.boatRendererVirGL.setChecked(true);
            this.currentBoatRenderer.setText(this.context.getText(R.string.game_setting_ui_boat_renderer_virgl));
        }
        if (setting.pojavLauncherSetting.renderer.equals("opengles2") || setting.pojavLauncherSetting.renderer.equals("opengles2_5") || setting.pojavLauncherSetting.renderer.equals("opengles3") || setting.pojavLauncherSetting.renderer.equals("opengles3_vgpu")) {
            this.pojavRendererGL4ES114.setChecked(true);
            this.pojavRendererVirGL.setChecked(false);
            this.currentPojavRenderer.setText(this.context.getText(R.string.game_setting_ui_pojav_renderer_gl4es_114));
        } else if (setting.pojavLauncherSetting.renderer.equals("opengles3_virgl")) {
            this.pojavRendererGL4ES114.setChecked(false);
            this.pojavRendererVirGL.setChecked(true);
            this.currentPojavRenderer.setText(this.context.getText(R.string.game_setting_ui_pojav_renderer_virgl));
        }
    }

    private ArrayList<View> getAllChild(ViewGroup viewGroup) {
        ArrayList<View> list = new ArrayList<View>();
        for (int i = 0; i < viewGroup.getChildCount(); ++i) {
            if (viewGroup.getChildAt(i) instanceof ViewGroup) {
                list.addAll(this.getAllChild((ViewGroup)viewGroup.getChildAt(i)));
            }
            list.add(viewGroup.getChildAt(i));
        }
        return list;
    }

    @SuppressLint(value={"SetTextI18n", "UseCompatLoadingForDrawables"})
    public void onClick(View v) {
        Intent intent;
        if (v == this.editVersionIcon) {
            intent = new Intent(this.context, FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "png;jpg");
            intent.putExtra("INITIAL_DIRECTORY", Environment.getExternalStorageDirectory().getAbsolutePath());
            this.activity.startActivityForResult(intent, 8600);
        }
        if (v == this.deleteVersionIcon) {
            if (new File(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/icon.png").exists()) {
                new File(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/icon.png").delete();
            }
            String ve = null;
            for (GameListBean bean : this.activity.uiManager.versionListUI.gameList) {
                if (!bean.name.equals(this.versionName)) continue;
                ve = bean.version;
                break;
            }
            if (ve == null || !ve.contains(",")) {
                this.icon.setBackground(this.context.getDrawable(R.drawable.ic_grass));
            } else {
                this.icon.setBackground(this.context.getDrawable(R.drawable.ic_furnace));
            }
        }
        if (v == this.switchToGlobalSetting) {
            this.activity.uiManager.switchMainUI(this.activity.uiManager.settingUI);
            this.activity.uiManager.settingUI.settingUIManager.switchSettingUIs(this.activity.uiManager.settingUI.settingUIManager.universalGameSettingUI);
        }
        if (v == this.showJavaSetting || v == this.showJava) {
            HiddenAnimationUtils.newInstance(this.context, (View)this.javaSetting, (View)this.showJava, this.javaSettingHeight).toggle();
        }
        if (v == this.showGameDirSetting || v == this.showGameDir) {
            HiddenAnimationUtils.newInstance(this.context, (View)this.gameDirSetting, (View)this.showGameDir, this.gameDirSettingHeight).toggle();
        }
        if (v == this.showGameLauncherSetting || v == this.showGameLauncher) {
            HiddenAnimationUtils.newInstance(this.context, (View)this.gameLauncherSetting, (View)this.showGameLauncher, this.gameLauncherSettingHeight).toggle();
        }
        if (v == this.showBoatRendererSetting || v == this.showBoatRenderer) {
            HiddenAnimationUtils.newInstance(this.context, (View)this.boatRendererSetting, (View)this.showBoatRenderer, this.boatRendererSettingHeight).toggle();
        }
        if (v == this.showPojavRendererSetting && this.showPojavRendererSetting != null) {
            this.showFullRendererDialog();
        } else if (v == this.showPojavRenderer) {
            HiddenAnimationUtils.newInstance(this.context, (View)this.pojavRendererSetting, (View)this.showPojavRenderer, this.pojavRendererSettingHeight).toggle();
        }
        if (v == this.checkJavaAuto && this.privateGameSetting != null) {
            this.javaPathText.setText((CharSequence)this.context.getString(R.string.game_setting_ui_java_path_auto));
            this.checkJava8.setChecked(false);
            this.checkJava17.setChecked(false);
            this.checkJava21.setChecked(false);
            this.checkJava25.setChecked(false);
            this.privateGameSetting.javaSetting.autoSelect = true;
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkJava8 && this.privateGameSetting != null) {
            this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/default"));
            this.checkJavaAuto.setChecked(false);
            this.checkJava17.setChecked(false);
            this.checkJava21.setChecked(false);
            this.checkJava25.setChecked(false);
            this.privateGameSetting.javaSetting.autoSelect = false;
            this.privateGameSetting.javaSetting.name = "default";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkJava17 && this.privateGameSetting != null) {
            this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE17"));
            this.checkJavaAuto.setChecked(false);
            this.checkJava8.setChecked(false);
            this.checkJava21.setChecked(false);
            this.checkJava25.setChecked(false);
            this.privateGameSetting.javaSetting.autoSelect = false;
            this.privateGameSetting.javaSetting.name = "JRE17";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkJava21 && this.privateGameSetting != null) {
            this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE21"));
            this.checkJavaAuto.setChecked(false);
            this.checkJava8.setChecked(false);
            this.checkJava17.setChecked(false);
            this.checkJava25.setChecked(false);
            this.privateGameSetting.javaSetting.autoSelect = false;
            this.privateGameSetting.javaSetting.name = "JRE21";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkJava25 && this.privateGameSetting != null) {
            this.javaPathText.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE25"));
            this.checkJavaAuto.setChecked(false);
            this.checkJava8.setChecked(false);
            this.checkJava17.setChecked(false);
            this.checkJava21.setChecked(false);
            this.privateGameSetting.javaSetting.autoSelect = false;
            this.privateGameSetting.javaSetting.name = "JRE25";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkGameDirDefault && this.privateGameSetting != null) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)this.activity.launcherSetting.gameFileDirectory);
            this.checkGameDirIsolate.setChecked(false);
            this.checkGameDirCustom.setChecked(false);
            this.privateGameSetting.gameDirSetting.type = 0;
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkGameDirIsolate && this.privateGameSetting != null) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName));
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirCustom.setChecked(false);
            this.privateGameSetting.gameDirSetting.type = 1;
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.checkGameDirCustom && this.privateGameSetting != null) {
            this.editGameDir.setEnabled(true);
            this.selectGameDir.setEnabled(true);
            this.gameDirText.setText((CharSequence)this.privateGameSetting.gameDirSetting.path);
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirIsolate.setChecked(false);
            this.privateGameSetting.gameDirSetting.type = 2;
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
        if (v == this.selectGameDir && this.privateGameSetting != null) {
            intent = new Intent(this.context, FolderChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("INITIAL_DIRECTORY", new File(AppManifest.DEFAULT_GAME_DIR).getAbsolutePath());
            this.activity.startActivityForResult(intent, 7300);
        }
        if (v == this.launchByBoat && this.privateGameSetting != null) {
            if (Architecture.getDeviceArchitecture() == Architecture.ARCH_ARM64) {
                this.launchByPojav.setChecked(false);
                this.privateGameSetting.boatLauncherSetting.enable = true;
                this.privateGameSetting.pojavLauncherSetting.enable = false;
                GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
                this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_boat));
            } else {
                Toast.makeText((Context)this.context, (CharSequence)"Not available for now!", (int)0).show();
                this.launchByBoat.setChecked(false);
                this.launchByPojav.setChecked(true);
                this.privateGameSetting.boatLauncherSetting.enable = false;
                this.privateGameSetting.pojavLauncherSetting.enable = true;
                GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
                this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_pojav));
            }
        }
        if (v == this.launchByPojav && this.privateGameSetting != null) {
            this.launchByBoat.setChecked(false);
            this.privateGameSetting.boatLauncherSetting.enable = false;
            this.privateGameSetting.pojavLauncherSetting.enable = true;
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
            this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_pojav));
        }
        if (v == this.boatRendererGL4ES114) {
            this.boatRendererVirGL.setChecked(false);
            this.privateGameSetting.boatLauncherSetting.renderer = "GL4ES114";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
            this.currentBoatRenderer.setText(this.context.getText(R.string.game_setting_ui_boat_renderer_gl4es_114));
        }
        if (v == this.boatRendererVirGL) {
            this.boatRendererGL4ES114.setChecked(false);
            this.privateGameSetting.boatLauncherSetting.renderer = "VirGL";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
            this.currentBoatRenderer.setText(this.context.getText(R.string.game_setting_ui_boat_renderer_virgl));
        }
        if (v == this.pojavRendererGL4ES114 && this.privateGameSetting != null) {
            this.pojavRendererVirGL.setChecked(false);
            this.privateGameSetting.pojavLauncherSetting.renderer = "opengles2";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
            this.currentPojavRenderer.setText(this.context.getText(R.string.game_setting_ui_pojav_renderer_gl4es_114));
        }
        if (v == this.pojavRendererVirGL && this.privateGameSetting != null) {
            this.pojavRendererGL4ES114.setChecked(false);
            this.privateGameSetting.pojavLauncherSetting.renderer = "opengles3_virgl";
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
            this.currentPojavRenderer.setText(this.context.getText(R.string.game_setting_ui_pojav_renderer_virgl));
        }
        if (v == this.manageController && this.privateGameSetting != null) {
            this.controllerManagerDialog = new ControllerManagerDialog(this.context, this.activity, this.activity.launcherSetting.fullscreen, this.privateGameSetting.controlLayout, new ControllerManagerDialog.OnPatternChangeListener(){

                @Override
                public void onPatternChange(String pattern) {
                    VersionSettingUI.this.currentControlPattern.setText((CharSequence)pattern);
                    ((VersionSettingUI)VersionSettingUI.this).privateGameSetting.controlLayout = pattern;
                    GsonUtils.savePrivateGameSetting(VersionSettingUI.this.privateGameSetting, VersionSettingUI.this.activity.launcherSetting.gameFileDirectory + "/versions/" + VersionSettingUI.this.versionName + "/qcl.cfg");
                }
            }, true);
            this.controllerManagerDialog.show();
        }
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == this.checkIsolateSetting) {
            String settingPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg";
            Log.e((String)"privateSettingPath", (String)settingPath);
            if (b) {
                this.enableSettingLayout();
                if (!new File(settingPath).exists() || GsonUtils.getPrivateGameSettingFromFile(settingPath) == null) {
                    try {
                        this.privateGameSetting = (PrivateGameSetting)this.activity.privateGameSetting.clone();
                    }
                    catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e((String)"setting", (String)"exist");
                    this.privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(settingPath);
                }
                this.privateGameSetting.enable = true;
                GsonUtils.savePrivateGameSetting(this.privateGameSetting, settingPath);
            } else {
                this.disableSettingLayout();
                if (new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null) {
                    this.privateGameSetting.enable = false;
                    try {
                        GsonUtils.savePrivateGameSetting((PrivateGameSetting)this.privateGameSetting.clone(), settingPath);
                    }
                    catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
                this.privateGameSetting = null;
            }
            this.onRefresh();
        } else {
            if (compoundButton == this.checkAutoRam && this.privateGameSetting != null) {
                this.privateGameSetting.ramSetting.autoRam = b;
            }
            if (compoundButton == this.checkLog && this.privateGameSetting != null) {
                this.privateGameSetting.log = b;
            }
            if (compoundButton == this.notCheckGameFile && this.privateGameSetting != null) {
                this.privateGameSetting.notCheckMinecraft = b;
            }
            if (compoundButton == this.notCheckForge && this.privateGameSetting != null) {
                this.privateGameSetting.notCheckForge = b;
            }
            if (compoundButton == this.notCheckJVM && this.privateGameSetting != null) {
                this.privateGameSetting.notCheckJvm = b;
            }
            if (compoundButton == this.checkTouchInjector && this.privateGameSetting != null) {
                this.privateGameSetting.touchInjector = b;
            }
            GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (seekBar == this.ramSeekBar && b && this.privateGameSetting != null) {
            this.privateGameSetting.ramSetting.minRam = i;
            this.privateGameSetting.ramSetting.maxRam = i;
            this.editRam.setText((CharSequence)(i + ""));
        }
        if (seekBar == this.scaleFactorSeekBar && b && this.privateGameSetting != null) {
            this.privateGameSetting.scaleFactor = ((float)i + 250.0f) / 1000.0f;
            this.editScaleFactor.setText((CharSequence)(i / 10 + 25 + ""));
        }
        GsonUtils.savePrivateGameSetting(this.privateGameSetting, this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.versionName + "/qcl.cfg");
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    private void showFullRendererDialog() {
        try {
            String current = this.activity.privateGameSetting.pojavLauncherSetting.renderer;
            String mcVer = null;
            try {
                String vp = this.activity.publicGameSetting.currentVersion;
                if (vp != null && !vp.isEmpty()) {
                    mcVer = new File(vp).getName();
                }
            }
            catch (Throwable vp) {
                // empty catch block
            }
            String[] ids = new String[RendererCompat.ALL.length];
            CharSequence[] labels = new String[ids.length];
            for (int i = 0; i < ids.length; ++i) {
                RendererCompat.Info info = RendererCompat.ALL[i];
                ids[i] = info.id;
                String mark = info.id.equals(current) ? "  \u2713" : "";
                String warn = RendererCompat.supports(info.id, mcVer) ? "" : "  \u26a0\u4e0d\u652f\u6301\u5f53\u524d\u7248\u672c";
                labels[i] = info.displayName + "\n\uff08\u652f\u6301 \u2264 " + info.displayMax + "\uff09" + mark + warn;
            }
            String mcVerF = mcVer;
            new AlertDialog.Builder((Context)this.activity).setTitle((CharSequence)("\u9009\u62e9\u6e32\u67d3\u5668\uff08\u5f53\u524d\u7248\u672c " + (mcVerF != null ? mcVerF : "?") + "\uff09")).setItems(labels, (d, which) -> {
                String id2 = ids[which];
                String warnText = RendererCompat.warningOf(id2, mcVerF);
                if (warnText != null) {
                    new AlertDialog.Builder((Context)this.activity).setTitle((CharSequence)"\u6e32\u67d3\u5668\u517c\u5bb9\u6027\u63d0\u793a").setMessage((CharSequence)warnText).setPositiveButton((CharSequence)"\u6211\u5c31\u8981\u7528\u8fd9\u4e2a\u6e32\u67d3\u5668", (d2, w2) -> this.applyRenderer(id2)).setNegativeButton((CharSequence)"\u53d6\u6d88", null).show();
                } else {
                    this.applyRenderer(id2);
                }
            }).setNegativeButton((CharSequence)"\u53d6\u6d88", null).show();
        }
        catch (Throwable e) {
            Toast.makeText((Context)this.activity, (CharSequence)("\u6253\u5f00\u6e32\u67d3\u5668\u9009\u62e9\u5931\u8d25: " + e.getMessage()), (int)0).show();
        }
    }

    private void applyRenderer(String id2) {
        try {
            this.activity.privateGameSetting.pojavLauncherSetting.renderer = id2;
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            RendererCompat.Info info = RendererCompat.find(id2);
            if (this.currentPojavRenderer != null && info != null) {
                this.currentPojavRenderer.setText((CharSequence)info.displayName);
            }
            Toast.makeText((Context)this.activity, (CharSequence)("\u6e32\u67d3\u5668\u5df2\u5207\u6362: " + (info != null ? info.displayName : id2)), (int)0).show();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }
}

