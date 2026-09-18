/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.content.Intent
 *  android.net.Uri
 *  android.text.Editable
 *  android.text.TextWatcher
 *  android.view.View
 *  android.view.View$OnClickListener
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
 *  android.widget.RadioGroup
 *  android.widget.SeekBar
 *  android.widget.SeekBar$OnSeekBarChangeListener
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.appcompat.widget.SwitchCompat
 *  com.tungsten.filepicker.Constants$SELECTION_MODES
 *  com.tungsten.filepicker.FolderChooser
 *  net.kdt.pojavlaunch.utils.Architecture
 */
package com.qcl.launcher.launcher.uis.universal.setting.right;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.control.ControllerManagerDialog;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.animation.HiddenAnimationUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.platform.MemoryUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FolderChooser;
import java.io.File;
import net.kdt.pojavlaunch.utils.Architecture;

import com.qcl.launcher.R;
public class UniversalGameSettingUI
extends BaseUI
implements View.OnClickListener,
CompoundButton.OnCheckedChangeListener,
SeekBar.OnSeekBarChangeListener {
    public LinearLayout universalGameSettingUI;
    public static final int PICK_GAME_DIR_REQUEST = 1500;
    private LinearLayout isolateAlertLayout;
    private TextView isolateAlertText;
    private TextView switchToIsolateSetting;
    private LinearLayout showJavaSetting;
    private TextView javaPathText;
    private ImageView showJava;
    private LinearLayout javaSetting;
    private int javaSettingHeight;
    private LinearLayout showGameDirSetting;
    public TextView gameDirText;
    private ImageView showGameDir;
    private LinearLayout gameDirSetting;
    private int gameDirSettingHeight;
    private LinearLayout showGameLauncherSetting;
    private TextView currentLauncher;
    private ImageView showGameLauncher;
    private LinearLayout gameLauncherSetting;
    private int gameLauncherSettingHeight;
    private LinearLayout showPojavRendererSetting;
    private TextView currentPojavRenderer;
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
    private RadioButton launchByPojav;
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

    public UniversalGameSettingUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    @SuppressLint(value={"SetTextI18n"})
    public void onCreate() {
        super.onCreate();
        this.universalGameSettingUI = (LinearLayout)this.activity.findViewById(R.id.ui_setting_global_game);
        this.isolateAlertLayout = (LinearLayout)this.activity.findViewById(R.id.isolate_alert_layout);
        this.isolateAlertText = (TextView)this.activity.findViewById(R.id.isolate_alert_text);
        this.switchToIsolateSetting = (TextView)this.activity.findViewById(R.id.switch_to_isolate_setting);
        this.switchToIsolateSetting.setOnClickListener((View.OnClickListener)this);
        this.showJavaSetting = (LinearLayout)this.activity.findViewById(R.id.show_java_selector);
        this.javaPathText = (TextView)this.activity.findViewById(R.id.java_path_text);
        this.showJava = (ImageView)this.activity.findViewById(R.id.show_java);
        this.javaSetting = (LinearLayout)this.activity.findViewById(R.id.java_setting);
        this.buildRuntimeBitSelector();
        this.showGameDirSetting = (LinearLayout)this.activity.findViewById(R.id.show_game_directory_selector);
        this.gameDirText = (TextView)this.activity.findViewById(R.id.game_directory_text);
        this.showGameDir = (ImageView)this.activity.findViewById(R.id.show_game_dir);
        this.gameDirSetting = (LinearLayout)this.activity.findViewById(R.id.game_dir_setting);
        this.showGameLauncherSetting = (LinearLayout)this.activity.findViewById(R.id.show_game_launcher_selector);
        this.currentLauncher = (TextView)this.activity.findViewById(R.id.current_launcher);
        this.showGameLauncher = (ImageView)this.activity.findViewById(R.id.show_game_launcher);
        this.gameLauncherSetting = (LinearLayout)this.activity.findViewById(R.id.game_launcher_selector);
        this.showPojavRendererSetting = (LinearLayout)this.activity.findViewById(R.id.show_pojav_render_selector);
        this.currentPojavRenderer = (TextView)this.activity.findViewById(R.id.current_pojav_renderer);
        this.checkJavaAuto = (RadioButton)this.activity.findViewById(R.id.check_java_path_auto);
        this.checkJava8 = (RadioButton)this.activity.findViewById(R.id.check_java_path_8);
        this.checkJava17 = (RadioButton)this.activity.findViewById(R.id.check_java_path_17);
        this.java8Path = (TextView)this.activity.findViewById(R.id.java_8_path);
        this.java17Path = (TextView)this.activity.findViewById(R.id.java_17_path);
        this.java8Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/default"));
        this.java17Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE17"));
        this.checkJava21 = (RadioButton)this.activity.findViewById(R.id.check_java_path_21);
        this.checkJava25 = (RadioButton)this.activity.findViewById(R.id.check_java_path_25);
        this.java21Path = (TextView)this.activity.findViewById(R.id.java_21_path);
        this.java25Path = (TextView)this.activity.findViewById(R.id.java_25_path);
        this.java21Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE21"));
        this.java25Path.setText((CharSequence)(AppManifest.JAVA_DIR + "/JRE25"));
        this.checkJava25.setEnabled(com.qcl.launcher.utils.Architecture.getDeviceArchitecture() != com.qcl.launcher.utils.Architecture.ARCH_X86);
        this.checkJava21.setOnClickListener((View.OnClickListener)this);
        this.checkJava25.setOnClickListener((View.OnClickListener)this);
        this.checkGameDirDefault = (RadioButton)this.activity.findViewById(R.id.check_default_game_dir);
        this.checkGameDirIsolate = (RadioButton)this.activity.findViewById(R.id.check_isolate_game_dir);
        this.checkGameDirCustom = (RadioButton)this.activity.findViewById(R.id.check_custom_game_dir);
        this.editGameDir = (EditText)this.activity.findViewById(R.id.edit_game_dir_path);
        this.selectGameDir = (ImageButton)this.activity.findViewById(R.id.select_game_dir_path);
        this.launchByPojav = (RadioButton)this.activity.findViewById(R.id.launch_by_pojav);
        this.checkAutoRam = (CheckBox)this.activity.findViewById(R.id.check_auto_ram);
        this.ramSeekBar = (SeekBar)this.activity.findViewById(R.id.ram_seek_bar);
        this.editRam = (EditText)this.activity.findViewById(R.id.edit_ram);
        this.usedRamText = (TextView)this.activity.findViewById(R.id.used_ram_text);
        this.actualRamText = (TextView)this.activity.findViewById(R.id.actual_ram_text);
        this.ramSeekBar.setMax(MemoryUtils.getMaxAllowedRam(this.context));
        this.ramProgressBar = (ProgressBar)this.activity.findViewById(R.id.ram_progress_bar);
        this.ramProgressBar.setMax(MemoryUtils.getMaxAllowedRam(this.context));
        this.scaleFactorSeekBar = (SeekBar)this.activity.findViewById(R.id.edit_scale_factor);
        this.editScaleFactor = (EditText)this.activity.findViewById(R.id.edit_scale_factor_text);
        this.scaleFactorSeekBar.setMax(750);
        this.checkLog = (SwitchCompat)this.activity.findViewById(R.id.switch_log);
        this.notCheckGameFile = (SwitchCompat)this.activity.findViewById(R.id.switch_check_mc);
        this.notCheckForge = (SwitchCompat)this.activity.findViewById(R.id.switch_check_forge);
        this.notCheckJVM = (SwitchCompat)this.activity.findViewById(R.id.switch_check_runtime);
        this.manageController = (Button)this.activity.findViewById(R.id.manage_control_layout);
        this.manageController.setOnClickListener((View.OnClickListener)this);
        this.currentControlPattern = (TextView)this.activity.findViewById(R.id.control_layout);
        this.checkTouchInjector = (SwitchCompat)this.activity.findViewById(R.id.switch_touch_injector);
        this.editServer = (EditText)this.activity.findViewById(R.id.edit_mc_server);
        this.editServer.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                UniversalGameSettingUI.this.activity.privateGameSetting.server = UniversalGameSettingUI.this.editServer.getText().toString();
                GsonUtils.savePrivateGameSetting(UniversalGameSettingUI.this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            }
        });
        this.editJVMArgs = (EditText)this.activity.findViewById(R.id.edit_jvm_arg);
        this.editJVMArgs.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                UniversalGameSettingUI.this.activity.privateGameSetting.extraJavaFlags = UniversalGameSettingUI.this.editJVMArgs.getText().toString();
                GsonUtils.savePrivateGameSetting(UniversalGameSettingUI.this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            }
        });
        this.checkLog.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.notCheckGameFile.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.notCheckForge.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.notCheckJVM.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkTouchInjector.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.showJavaSetting.setOnClickListener((View.OnClickListener)this);
        this.showJava.setOnClickListener((View.OnClickListener)this);
        this.showGameDirSetting.setOnClickListener((View.OnClickListener)this);
        this.showGameDir.setOnClickListener((View.OnClickListener)this);
        this.showGameLauncherSetting.setOnClickListener((View.OnClickListener)this);
        this.showGameLauncher.setOnClickListener((View.OnClickListener)this);
        this.showPojavRendererSetting.setOnClickListener((View.OnClickListener)this);
        this.checkJavaAuto.setOnClickListener((View.OnClickListener)this);
        this.checkJava8.setOnClickListener((View.OnClickListener)this);
        this.checkJava17.setOnClickListener((View.OnClickListener)this);
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
                UniversalGameSettingUI.this.activity.privateGameSetting.gameDirSetting.path = UniversalGameSettingUI.this.editGameDir.getText().toString();
                GsonUtils.savePrivateGameSetting(UniversalGameSettingUI.this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            }
        });
        this.launchByPojav.setOnClickListener((View.OnClickListener)this);
        this.checkAutoRam.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.ramSeekBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.editRam.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (!UniversalGameSettingUI.this.editRam.getText().toString().equals("")) {
                    UniversalGameSettingUI.this.activity.privateGameSetting.ramSetting.minRam = Integer.parseInt(UniversalGameSettingUI.this.editRam.getText().toString());
                    UniversalGameSettingUI.this.activity.privateGameSetting.ramSetting.maxRam = Integer.parseInt(UniversalGameSettingUI.this.editRam.getText().toString());
                    UniversalGameSettingUI.this.ramSeekBar.setProgress(Integer.parseInt(UniversalGameSettingUI.this.editRam.getText().toString()));
                } else {
                    UniversalGameSettingUI.this.activity.privateGameSetting.ramSetting.minRam = 0;
                    UniversalGameSettingUI.this.activity.privateGameSetting.ramSetting.maxRam = 0;
                    UniversalGameSettingUI.this.ramSeekBar.setProgress(0);
                }
                GsonUtils.savePrivateGameSetting(UniversalGameSettingUI.this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            }
        });
        this.scaleFactorSeekBar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.editScaleFactor.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                if (!UniversalGameSettingUI.this.editScaleFactor.getText().toString().equals("")) {
                    UniversalGameSettingUI.this.activity.privateGameSetting.scaleFactor = (float)Integer.parseInt(UniversalGameSettingUI.this.editScaleFactor.getText().toString()) / 100.0f;
                    UniversalGameSettingUI.this.scaleFactorSeekBar.setProgress(Integer.parseInt(UniversalGameSettingUI.this.editScaleFactor.getText().toString()) * 10 - 250);
                } else {
                    UniversalGameSettingUI.this.activity.privateGameSetting.scaleFactor = 0.25f;
                    UniversalGameSettingUI.this.scaleFactorSeekBar.setProgress(0);
                }
                GsonUtils.savePrivateGameSetting(UniversalGameSettingUI.this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
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
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft((View)this.universalGameSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startGlobalGameSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
        this.init();
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft((View)this.universalGameSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startGlobalGameSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onLoaded() {
        this.activity.uiManager.settingUI.startGlobalGameSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1500 && data != null && resultCode == -1) {
            uri = data.getData();
            this.gameDirText.setText((CharSequence)UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
            this.editGameDir.setText((CharSequence)UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
            this.activity.privateGameSetting.gameDirSetting.path = UriUtils.getRealPathFromUri_AboveApi19(this.context, uri);
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
        }
        if (requestCode == 3000 && this.controllerManagerDialog != null && data != null) {
            uri = data.getData();
            String pattern = uri.toString();
            this.currentControlPattern.setText((CharSequence)pattern);
            this.activity.privateGameSetting.controlLayout = pattern;
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            this.controllerManagerDialog.currentPattern = pattern;
            this.controllerManagerDialog.loadList();
        }
        if (requestCode == 3200 && this.controllerManagerDialog != null && data != null) {
            this.controllerManagerDialog.onResult(requestCode, resultCode, data);
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == this.checkAutoRam) {
            this.activity.privateGameSetting.ramSetting.autoRam = isChecked;
        }
        if (buttonView == this.checkLog) {
            this.activity.privateGameSetting.log = isChecked;
        }
        if (buttonView == this.notCheckGameFile) {
            this.activity.privateGameSetting.notCheckMinecraft = isChecked;
        }
        if (buttonView == this.notCheckForge) {
            this.activity.privateGameSetting.notCheckForge = isChecked;
        }
        if (buttonView == this.notCheckJVM) {
            this.activity.privateGameSetting.notCheckJvm = isChecked;
        }
        if (buttonView == this.checkTouchInjector) {
            this.activity.privateGameSetting.touchInjector = isChecked;
        }
        GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
    }

    public void refresh() {
        this.isolateAlertLayout.setVisibility(8);
    }

    private void buildRuntimeBitSelector() {
        try {
            RadioGroup group = (RadioGroup)this.activity.findViewById(R.id.qcl_runtime_bit_group);
            if (group == null) {
                return;
            }
            RadioButton auto = (RadioButton)this.activity.findViewById(R.id.qcl_runtime_bit_auto);
            RadioButton bit64 = (RadioButton)this.activity.findViewById(R.id.qcl_runtime_bit_64);
            RadioButton bit32 = (RadioButton)this.activity.findViewById(R.id.qcl_runtime_bit_32);
            int mode = this.activity.privateGameSetting.javaSetting.bitMode;
            if (mode < 0 || mode > 2) {
                mode = 0;
            }
            group.check(mode == 0 ? auto.getId() : (mode == 1 ? bit64.getId() : bit32.getId()));
            View.OnClickListener listener = v -> {
                int m;
                this.activity.privateGameSetting.javaSetting.bitMode = m = v == bit64 ? 1 : (v == bit32 ? 2 : 0);
                if (m == 1) {
                    this.activity.privateGameSetting.ramSetting.minRam = 2048;
                    this.activity.privateGameSetting.ramSetting.maxRam = 2048;
                } else if (m == 2) {
                    this.activity.privateGameSetting.ramSetting.minRam = 1024;
                    this.activity.privateGameSetting.ramSetting.maxRam = 1024;
                }
                GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
                Architecture.setBitMode((int)m);
                com.qcl.launcher.utils.Architecture.setBitMode(m);
                Toast.makeText((Context)this.context, (CharSequence)("\u8fd0\u884c\u65f6\u4f4d\u6570\uff1a" + (m == 0 ? "\u81ea\u52a8" : (m == 1 ? "\u5f3a\u5236 64 \u4f4d\uff08\u5185\u5b58 2GB\uff09" : "\u5f3a\u5236 32 \u4f4d\uff08\u5185\u5b58 1GB\uff09"))), (int)0).show();
            };
            auto.setOnClickListener(listener);
            bit64.setOnClickListener(listener);
            bit32.setOnClickListener(listener);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    private void init() {
        this.refresh();
        this.checkAutoRam.setChecked(this.activity.privateGameSetting.ramSetting.autoRam);
        this.ramProgressBar.setProgress(MemoryUtils.getTotalDeviceMemory(this.context) - MemoryUtils.getFreeDeviceMemory(this.context));
        this.ramSeekBar.setProgress(this.activity.privateGameSetting.ramSetting.minRam);
        this.editRam.setText((CharSequence)(this.activity.privateGameSetting.ramSetting.minRam + ""));
        this.usedRamText.setText((CharSequence)(this.context.getString(R.string.game_setting_ui_used_ram).replace("%s", Float.toString((float)Math.round((float)(MemoryUtils.getTotalDeviceMemory(this.context) - MemoryUtils.getFreeDeviceMemory(this.context)) / 1024.0f * 10.0f) / 10.0f)) + " / " + (float)Math.round((float)MemoryUtils.getTotalDeviceMemory(this.context) / 1024.0f * 10.0f) / 10.0f + " GB"));
        this.actualRamText.setText((CharSequence)(this.context.getString(R.string.game_setting_ui_min_distribution).replace("%s", Float.toString((float)Math.round((float)this.activity.privateGameSetting.ramSetting.minRam / 1024.0f * 10.0f) / 10.0f)) + " / " + this.context.getString(R.string.game_setting_ui_actual_distribution).replace("%s", Float.toString((float)Math.round((float)this.activity.privateGameSetting.ramSetting.minRam / 1024.0f * 10.0f) / 10.0f))));
        this.scaleFactorSeekBar.setProgress((int)(this.activity.privateGameSetting.scaleFactor * 1000.0f) - 250);
        this.editScaleFactor.setText((CharSequence)((int)(this.activity.privateGameSetting.scaleFactor * 100.0f) + ""));
        this.checkLog.setChecked(this.activity.privateGameSetting.log);
        this.notCheckGameFile.setChecked(this.activity.privateGameSetting.notCheckMinecraft);
        this.notCheckForge.setChecked(this.activity.privateGameSetting.notCheckForge);
        this.notCheckJVM.setChecked(this.activity.privateGameSetting.notCheckJvm);
        this.checkTouchInjector.setChecked(this.activity.privateGameSetting.touchInjector);
        this.editGameDir.setText((CharSequence)this.activity.privateGameSetting.gameDirSetting.path);
        this.editServer.setText((CharSequence)this.activity.privateGameSetting.server);
        this.editJVMArgs.setText((CharSequence)this.activity.privateGameSetting.extraJavaFlags);
        this.currentControlPattern.setText((CharSequence)this.activity.privateGameSetting.controlLayout);
        boolean autoJava = this.activity.privateGameSetting.javaSetting.autoSelect;
        String javaName = this.activity.privateGameSetting.javaSetting.name;
        this.javaPathText.setText((CharSequence)(autoJava ? this.context.getString(R.string.game_setting_ui_java_path_auto) : AppManifest.JAVA_DIR + "/" + javaName));
        this.checkJavaAuto.setChecked(autoJava);
        this.checkJava8.setChecked(!autoJava && "default".equals(javaName));
        this.checkJava17.setChecked(!autoJava && "JRE17".equals(javaName));
        this.checkJava21.setChecked(!autoJava && "JRE21".equals(javaName));
        this.checkJava25.setChecked(!autoJava && "JRE25".equals(javaName));
        if (this.activity.privateGameSetting.gameDirSetting.type == 0) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)this.activity.launcherSetting.gameFileDirectory);
            this.checkGameDirDefault.setChecked(true);
            this.checkGameDirIsolate.setChecked(false);
            this.checkGameDirCustom.setChecked(false);
        } else if (this.activity.privateGameSetting.gameDirSetting.type == 1) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)this.activity.publicGameSetting.currentVersion);
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirIsolate.setChecked(true);
            this.checkGameDirCustom.setChecked(false);
        } else {
            this.editGameDir.setEnabled(true);
            this.selectGameDir.setEnabled(true);
            this.gameDirText.setText((CharSequence)this.activity.privateGameSetting.gameDirSetting.path);
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirIsolate.setChecked(false);
            this.checkGameDirCustom.setChecked(true);
        }
        // ★★★ 1.1.1：Boat 后端已删除，只有 Pojav 一种后端
        this.launchByPojav.setChecked(true);
        this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_pojav));
        if (this.currentPojavRenderer != null) {
            this.currentPojavRenderer.setText((CharSequence)com.qcl.launcher.launcher.launch.RendererPicker
                    .displayNameOf(this.activity.privateGameSetting.pojavLauncherSetting.renderer));
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onClick(View v) {
        if (v == this.switchToIsolateSetting) {
            this.activity.uiManager.gameManagerUI.versionName = this.activity.publicGameSetting.currentVersion.substring(this.activity.publicGameSetting.currentVersion.lastIndexOf("/") + 1);
            this.activity.uiManager.switchMainUI(this.activity.uiManager.gameManagerUI);
            this.activity.uiManager.gameManagerUI.gameManagerUIManager.switchGameManagerUIs(this.activity.uiManager.gameManagerUI.gameManagerUIManager.versionSettingUI);
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
        if (v == this.showPojavRendererSetting) {
            com.qcl.launcher.launcher.launch.RendererPicker.show(this.activity,
                    this.activity.privateGameSetting,
                    this.activity.publicGameSetting.currentVersion,
                    () -> {
                        if (this.currentPojavRenderer != null) {
                            this.currentPojavRenderer.setText((CharSequence)
                                    com.qcl.launcher.launcher.launch.RendererPicker.displayNameOf(
                                            this.activity.privateGameSetting.pojavLauncherSetting.renderer));
                        }
                    });
        }
        if (v == this.checkJavaAuto || v == this.checkJava8 || v == this.checkJava17 || v == this.checkJava21 || v == this.checkJava25) {
            String name = v == this.checkJava8 ? "default" : (v == this.checkJava17 ? "JRE17" : (v == this.checkJava21 ? "JRE21" : "JRE25"));
            boolean bl = this.activity.privateGameSetting.javaSetting.autoSelect = v == this.checkJavaAuto;
            if (v != this.checkJavaAuto) {
                this.activity.privateGameSetting.javaSetting.name = name;
            }
            this.checkJavaAuto.setChecked(v == this.checkJavaAuto);
            this.checkJava8.setChecked(v == this.checkJava8);
            this.checkJava17.setChecked(v == this.checkJava17);
            this.checkJava21.setChecked(v == this.checkJava21);
            this.checkJava25.setChecked(v == this.checkJava25);
            this.javaPathText.setText((CharSequence)(v == this.checkJavaAuto ? this.context.getString(R.string.game_setting_ui_java_path_auto) : AppManifest.JAVA_DIR + "/" + name));
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            return;
        }
        if (v == this.checkGameDirDefault) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)this.activity.launcherSetting.gameFileDirectory);
            this.checkGameDirIsolate.setChecked(false);
            this.checkGameDirCustom.setChecked(false);
            this.activity.privateGameSetting.gameDirSetting.type = 0;
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
        }
        if (v == this.checkGameDirIsolate) {
            this.editGameDir.setEnabled(false);
            this.selectGameDir.setEnabled(false);
            this.gameDirText.setText((CharSequence)this.activity.publicGameSetting.currentVersion);
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirCustom.setChecked(false);
            this.activity.privateGameSetting.gameDirSetting.type = 1;
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
        }
        if (v == this.checkGameDirCustom) {
            this.editGameDir.setEnabled(true);
            this.selectGameDir.setEnabled(true);
            this.gameDirText.setText((CharSequence)this.activity.privateGameSetting.gameDirSetting.path);
            this.checkGameDirDefault.setChecked(false);
            this.checkGameDirIsolate.setChecked(false);
            this.activity.privateGameSetting.gameDirSetting.type = 2;
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
        }
        if (v == this.selectGameDir) {
            Intent intent = new Intent(this.context, FolderChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("INITIAL_DIRECTORY", new File(AppManifest.DEFAULT_GAME_DIR).getAbsolutePath());
            this.activity.startActivityForResult(intent, 1500);
        }
        if (v == this.launchByPojav) {
            this.activity.privateGameSetting.pojavLauncherSetting.enable = true;
            GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
            this.currentLauncher.setText(this.context.getText(R.string.game_setting_ui_game_launcher_pojav));
        }
        if (v == this.manageController) {
            this.controllerManagerDialog = new ControllerManagerDialog(this.context, this.activity, this.activity.launcherSetting.fullscreen, this.activity.privateGameSetting.controlLayout, new ControllerManagerDialog.OnPatternChangeListener(){

                @Override
                public void onPatternChange(String pattern) {
                    UniversalGameSettingUI.this.currentControlPattern.setText((CharSequence)pattern);
                    UniversalGameSettingUI.this.activity.privateGameSetting.controlLayout = pattern;
                    GsonUtils.savePrivateGameSetting(UniversalGameSettingUI.this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
                }
            }, false);
            this.controllerManagerDialog.show();
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar == this.ramSeekBar && fromUser) {
            this.activity.privateGameSetting.ramSetting.minRam = progress;
            this.activity.privateGameSetting.ramSetting.maxRam = progress;
            this.editRam.setText((CharSequence)(progress + ""));
        }
        if (seekBar == this.scaleFactorSeekBar && fromUser) {
            this.activity.privateGameSetting.scaleFactor = ((float)progress + 250.0f) / 1000.0f;
            this.editScaleFactor.setText((CharSequence)(progress / 10 + 25 + ""));
        }
        GsonUtils.savePrivateGameSetting(this.activity.privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}

