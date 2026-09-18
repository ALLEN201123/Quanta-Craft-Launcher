/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Activity
 *  android.app.Dialog
 *  android.content.Context
 *  android.content.Intent
 *  android.os.Process
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.ViewGroup
 *  android.widget.AdapterView
 *  android.widget.AdapterView$OnItemSelectedListener
 *  android.widget.ArrayAdapter
 *  android.widget.Button
 *  android.widget.CompoundButton
 *  android.widget.CompoundButton$OnCheckedChangeListener
 *  android.widget.FrameLayout
 *  android.widget.SeekBar
 *  android.widget.SeekBar$OnSeekBarChangeListener
 *  android.widget.Spinner
 *  android.widget.SpinnerAdapter
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.appcompat.app.AlertDialog
 *  androidx.appcompat.app.AlertDialog$Builder
 *  androidx.appcompat.app.AppCompatActivity
 *  androidx.appcompat.widget.SwitchCompat
 *  com.google.gson.Gson
 */
package com.qcl.launcher.control;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.google.gson.Gson;
import com.qcl.launcher.control.LwjglCharSender;
import com.qcl.launcher.control.MKManager;
import com.qcl.launcher.control.ViewManager;
import com.qcl.launcher.control.bean.BaseButtonInfo;
import com.qcl.launcher.control.bean.BaseRockerViewInfo;
import com.qcl.launcher.control.bean.button.ButtonStyle;
import com.qcl.launcher.control.view.LayoutPanel;
import com.qcl.launcher.control.view.TouchCharInput;
import com.qcl.launcher.launcher.dialogs.control.AddViewDialog;
import com.qcl.launcher.launcher.dialogs.control.ChildManagerDialog;
import com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog;
import com.qcl.launcher.launcher.dialogs.control.CreateControlPatternDialog;
import com.qcl.launcher.launcher.dialogs.control.EditControlPatternDialog;
import com.qcl.launcher.launcher.launch.LaunchLogWindow;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.list.local.controller.ControlPattern;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.setting.game.GameMenuSetting;
import com.qcl.launcher.launcher.terracotta.MultiplayerDialogHelper;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.AssetsUtils;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.file.FileUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
public class MenuHelper
implements CompoundButton.OnCheckedChangeListener,
View.OnClickListener,
AdapterView.OnItemSelectedListener,
SeekBar.OnSeekBarChangeListener {
    public Context context;
    public AppCompatActivity activity;
    public boolean fullscreen;
    public String gameDir;
    public FrameLayout drawerLayout;
    public View gameMenuContainer;
    public LayoutPanel baseLayout;
    public int launcher;
    public float scaleFactor;
    public int screenWidth;
    public int screenHeight;
    public GameMenuSetting gameMenuSetting;
    public TouchCharInput touchCharInput;
    public SwitchCompat switchMenuFloat;
    public SwitchCompat switchMenuView;
    public SwitchCompat switchMenuSlide;
    public SwitchCompat switchFloatMovable;
    public SwitchCompat switchLaunchLog;
    public SwitchCompat switchAdvanceInput;
    public SwitchCompat switchTouch;
    public SwitchCompat switchMousePatch;
    public SwitchCompat switchSensor;
    public SwitchCompat switchHalfScreen;
    public Spinner spinnerTouchMode;
    public Spinner spinnerMouseMode;
    public TextView sensitivityText;
    public SeekBar sensitivitySeekbar;
    public TextView mouseSpeedText;
    public SeekBar mouseSpeedSeekbar;
    public TextView mouseSizeText;
    public SeekBar mouseSizeSeekbar;
    public SwitchCompat switchHideUI;
    public Button openHin2nMenu;
    public Button forceExit;
    public Spinner patternSpinner;
    public SwitchCompat editModeSwitch;
    public SwitchCompat showOutlineSwitch;
    public Button editInfo;
    public Button manageChild;
    public Spinner childSpinner;
    public Button addView;
    public Button createPattern;
    public Button createButtonStyle;
    public ArrayList<ControlPattern> patternList;
    public ControlPattern currentPattern;
    public String initialPattern;
    public String currentChild;
    public boolean editMode;
    public boolean showOutline;
    public boolean enableNameEditor;
    public ArrayList<String> childLayoutList;
    public ArrayAdapter<String> childAdapter;
    public int gameCursorMode = 0;
    public ViewManager viewManager;
    public MKManager mkManager;
    public boolean enterLock;
    public float cursorX;
    public float cursorY;
    public float pointerX;
    public float pointerY;
    public float currentX;
    public float currentY;

    public MenuHelper(Context context, AppCompatActivity activity, boolean fullscreen, String gameDir, FrameLayout drawerLayout, final LayoutPanel baseLayout, final boolean editMode, final String currentPattern, int launcher, float scaleFactor) {
        this.context = context;
        this.activity = activity;
        this.fullscreen = fullscreen;
        this.gameDir = gameDir;
        this.drawerLayout = drawerLayout;
        this.baseLayout = baseLayout;
        this.editMode = editMode;
        this.showOutline = false;
        this.enableNameEditor = editMode;
        this.launcher = launcher;
        this.scaleFactor = scaleFactor;
        this.patternList = SettingUtils.getControlPatternList();
        if (this.patternList.size() == 0) {
            InitializeSetting.initializeControlPattern((Activity)activity, new AssetsUtils.FileOperateCallback(){

                @Override
                public void onSuccess() {
                    MenuHelper.this.patternList = SettingUtils.getControlPatternList();
                    MenuHelper.this.preInit(baseLayout, editMode, currentPattern);
                }

                @Override
                public void onFailed(String error) {
                }
            });
        } else {
            this.preInit(baseLayout, editMode, currentPattern);
        }
    }

    public void enableCursor() {
        this.gameCursorMode = 0;
        if (this.viewManager != null) {
            this.viewManager.enableCursor();
        }
        if (this.mkManager != null) {
            this.mkManager.enableCursor();
        }
    }

    public void disableCursor() {
        this.gameCursorMode = 1;
        if (this.viewManager != null) {
            this.viewManager.disableCursor();
        }
        if (this.mkManager != null) {
            this.mkManager.disableCursor();
        }
    }

    public void preInit(LayoutPanel baseLayout, boolean editMode, String currentPattern) {
        for (ControlPattern controlPattern : this.patternList) {
            if (!controlPattern.name.equals(currentPattern)) continue;
            this.currentPattern = controlPattern;
        }
        if (this.currentPattern == null && !this.patternList.isEmpty()) {
            this.currentPattern = this.patternList.get(0);
            currentPattern = this.currentPattern.name;
        }
        if (this.currentPattern == null) {
            this.currentPattern = new ControlPattern(currentPattern == null ? "Default" : currentPattern, "Rod123456 (bilibili UID 550905358)", "1.1", "QCL \u9ed8\u8ba4\u63a7\u952e\u5e03\u5c40 \u00b7 Quanta Craft Launcher", 1);
        }
        String string2 = this.currentChild = SettingUtils.getChildList(currentPattern).size() > 0 ? SettingUtils.getChildList((String)currentPattern).get((int)0).name : null;
        if (this.launcher == 0) {
            baseLayout.showBackground();
        }
        this.gameMenuSetting = GameMenuSetting.getGameMenuSetting();
        this.init();
    }

    @SuppressLint(value={"SetTextI18n"})
    public void init() {
        this.touchCharInput = (TouchCharInput)this.activity.findViewById(R.id.input_scanner);
        this.touchCharInput.setCharacterSender(this, new LwjglCharSender());
        this.gameMenuContainer = this.activity.findViewById(R.id.game_menu_container);
        this.switchMenuFloat = (SwitchCompat)this.activity.findViewById(R.id.switch_float_button);
        this.switchMenuView = (SwitchCompat)this.activity.findViewById(R.id.switch_bar);
        this.switchMenuSlide = (SwitchCompat)this.activity.findViewById(R.id.switch_gesture);
        this.switchFloatMovable = (SwitchCompat)this.activity.findViewById(R.id.switch_float_movable);
        this.switchLaunchLog = (SwitchCompat)this.activity.findViewById(R.id.switch_launch_log);
        this.switchAdvanceInput = (SwitchCompat)this.activity.findViewById(R.id.switch_advance_input);
        this.switchTouch = (SwitchCompat)this.activity.findViewById(R.id.switch_touch);
        this.switchMousePatch = (SwitchCompat)this.activity.findViewById(R.id.switch_mouse_patch);
        this.switchSensor = (SwitchCompat)this.activity.findViewById(R.id.switch_control_sensor);
        this.switchHalfScreen = (SwitchCompat)this.activity.findViewById(R.id.switch_half_screen);
        this.spinnerTouchMode = (Spinner)this.activity.findViewById(R.id.spinner_touch_mode);
        this.spinnerMouseMode = (Spinner)this.activity.findViewById(R.id.spinner_mouse_mode);
        this.sensitivityText = (TextView)this.activity.findViewById(R.id.sensitivity_text);
        this.sensitivitySeekbar = (SeekBar)this.activity.findViewById(R.id.sensor_sensitivity);
        this.mouseSpeedText = (TextView)this.activity.findViewById(R.id.mouse_speed_text);
        this.mouseSpeedSeekbar = (SeekBar)this.activity.findViewById(R.id.mouse_speed);
        this.mouseSizeText = (TextView)this.activity.findViewById(R.id.mouse_size_text);
        this.mouseSizeSeekbar = (SeekBar)this.activity.findViewById(R.id.mouse_size);
        this.switchHideUI = (SwitchCompat)this.activity.findViewById(R.id.switch_hide_ui);
        this.openHin2nMenu = (Button)this.activity.findViewById(R.id.open_hin2n_menu);
        this.forceExit = (Button)this.activity.findViewById(R.id.force_exit);
        this.switchMenuFloat.setChecked(this.gameMenuSetting.menuFloatSetting.enable);
        this.switchMenuView.setChecked(this.gameMenuSetting.menuViewSetting.enable);
        this.switchMenuSlide.setChecked(this.gameMenuSetting.menuSlideSetting);
        this.switchFloatMovable.setChecked(this.gameMenuSetting.menuFloatSetting.movable);
        this.switchLaunchLog.setChecked(!this.gameMenuSetting.hideLaunchLog);
        this.switchLaunchLog.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchAdvanceInput.setChecked(this.gameMenuSetting.advanceInput);
        this.switchTouch.setChecked(this.gameMenuSetting.enableTouch);
        this.switchMousePatch.setChecked(this.gameMenuSetting.mousePatch);
        this.switchSensor.setChecked(this.gameMenuSetting.enableSensor);
        this.switchHalfScreen.setChecked(this.gameMenuSetting.disableHalfScreen);
        this.switchHideUI.setChecked(this.gameMenuSetting.hideUI);
        this.switchMenuFloat.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchMenuView.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchMenuSlide.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchFloatMovable.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchAdvanceInput.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchTouch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchMousePatch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchSensor.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchHalfScreen.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.switchHideUI.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.openHin2nMenu.setOnClickListener((View.OnClickListener)this);
        this.forceExit.setOnClickListener((View.OnClickListener)this);
        ArrayList<String> touchModes = new ArrayList<String>();
        touchModes.add(this.context.getString(R.string.drawer_game_menu_control_touch_mode_create));
        touchModes.add(this.context.getString(R.string.drawer_game_menu_control_touch_mode_attack));
        ArrayAdapter touchModeAdapter = new ArrayAdapter(this.context, R.layout.item_spinner_drop_down_small, touchModes);
        this.spinnerTouchMode.setAdapter((SpinnerAdapter)touchModeAdapter);
        this.spinnerTouchMode.setSelection(this.gameMenuSetting.touchMode);
        this.spinnerTouchMode.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        ArrayList<String> mouseModes = new ArrayList<String>();
        mouseModes.add(this.context.getString(R.string.drawer_game_menu_control_mouse_mode_click));
        mouseModes.add(this.context.getString(R.string.drawer_game_menu_control_mouse_mode_slide));
        ArrayAdapter mouseModeAdapter = new ArrayAdapter(this.context, R.layout.item_spinner_drop_down_small, mouseModes);
        this.spinnerMouseMode.setAdapter((SpinnerAdapter)mouseModeAdapter);
        this.spinnerMouseMode.setSelection(this.gameMenuSetting.mouseMode);
        this.spinnerMouseMode.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.patternSpinner = (Spinner)this.activity.findViewById(R.id.current_pattern_spinner);
        this.editModeSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_edit_mode);
        this.showOutlineSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_show_outline);
        this.editInfo = (Button)this.activity.findViewById(R.id.edit_pattern_info);
        this.manageChild = (Button)this.activity.findViewById(R.id.manage_child_layout);
        this.childSpinner = (Spinner)this.activity.findViewById(R.id.current_child_spinner);
        this.addView = (Button)this.activity.findViewById(R.id.add_view);
        this.createPattern = (Button)this.activity.findViewById(R.id.create_pattern);
        this.createButtonStyle = (Button)this.activity.findViewById(R.id.create_button_style);
        this.sensitivityText.setText((CharSequence)Integer.toString(this.gameMenuSetting.sensitivity));
        this.sensitivitySeekbar.setProgress(this.gameMenuSetting.sensitivity);
        this.sensitivitySeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.mouseSpeedText.setText((CharSequence)Float.toString(this.gameMenuSetting.mouseSpeed * 100.0f));
        this.mouseSpeedSeekbar.setProgress((int)(this.gameMenuSetting.mouseSpeed * 100.0f));
        this.mouseSpeedSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.mouseSizeText.setText((CharSequence)Integer.toString(this.gameMenuSetting.mouseSize));
        this.mouseSizeSeekbar.setProgress(this.gameMenuSetting.mouseSize);
        this.mouseSizeSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        ArrayList<String> patterns = new ArrayList<String>();
        for (ControlPattern controlPattern : this.patternList) {
            patterns.add(controlPattern.name);
        }
        ArrayAdapter patternAdapter = new ArrayAdapter(this.context, R.layout.item_spinner_drop_down_small, patterns);
        this.patternSpinner.setAdapter((SpinnerAdapter)patternAdapter);
        this.patternSpinner.setSelection(patternAdapter.getPosition((Object)this.currentPattern.name));
        ArrayList<ChildLayout> list = SettingUtils.getChildList(this.currentPattern.name);
        this.childLayoutList = new ArrayList();
        for (ChildLayout childLayout : list) {
            this.childLayoutList.add(childLayout.name);
        }
        this.childAdapter = new ArrayAdapter<String>(this.context, R.layout.item_spinner_drop_down_small, this.childLayoutList);
        this.childSpinner.setAdapter(this.childAdapter);
        if (this.editMode) {
            this.editInfo.setEnabled(true);
            this.manageChild.setEnabled(true);
            this.childSpinner.setEnabled(true);
            this.addView.setEnabled(true);
            this.createPattern.setEnabled(true);
            this.createButtonStyle.setEnabled(true);
        } else {
            this.editInfo.setEnabled(false);
            this.manageChild.setEnabled(false);
            this.childSpinner.setEnabled(false);
            this.addView.setEnabled(false);
            this.createPattern.setEnabled(false);
            this.createButtonStyle.setEnabled(false);
        }
        this.editModeSwitch.setChecked(this.editMode);
        this.childSpinner.setSelection(0);
        this.patternSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.editModeSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.showOutlineSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.editInfo.setOnClickListener((View.OnClickListener)this);
        this.manageChild.setOnClickListener((View.OnClickListener)this);
        this.childSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.addView.setOnClickListener((View.OnClickListener)this);
        this.createPattern.setOnClickListener((View.OnClickListener)this);
        this.createButtonStyle.setOnClickListener((View.OnClickListener)this);
        this.baseLayout.post(() -> {
            this.screenWidth = this.baseLayout.getWidth();
            this.screenHeight = this.baseLayout.getHeight();
            this.viewManager = new ViewManager(this.context, (Activity)this.activity, this, this.baseLayout, this.launcher);
            this.mkManager = new MKManager(this);
            this.checkOpenMenuSetting();
        });
    }

    public void toggleGameMenu() {
        if (this.gameMenuContainer == null) {
            return;
        }
        boolean show = this.gameMenuContainer.getVisibility() != 0;
        this.gameMenuContainer.setVisibility(show ? 0 : 8);
        this.setGameMenuOutsideCloseEnabled(show);
    }

    public void setGameMenuOutsideCloseEnabled(boolean enabled) {
        if (this.baseLayout == null) {
            return;
        }
        if (enabled) {
            this.baseLayout.setMenuPanel(this.gameMenuContainer);
            this.baseLayout.setOutsideCloseListener(this::hideGameMenu);
            this.baseLayout.setMenuOutsideCloseEnabled(true);
        } else {
            this.baseLayout.setMenuOutsideCloseEnabled(false);
        }
    }

    public void hideGameMenu() {
        if (this.gameMenuContainer == null) {
            return;
        }
        if (this.gameMenuContainer.getVisibility() != 0) {
            return;
        }
        this.gameMenuContainer.setVisibility(8);
        this.setGameMenuOutsideCloseEnabled(false);
    }

    private void checkOpenMenuSetting() {
        if (!(this.gameMenuSetting.menuFloatSetting.enable || this.gameMenuSetting.menuViewSetting.enable || this.gameMenuSetting.menuSlideSetting)) {
            this.switchMenuFloat.setChecked(true);
        }
    }

    public void refreshChildSpinner() {
        ArrayList<ChildLayout> list = SettingUtils.getChildList(this.currentPattern.name);
        this.childLayoutList = new ArrayList();
        for (ChildLayout childLayout : list) {
            this.childLayoutList.add(childLayout.name);
        }
        this.childAdapter = new ArrayAdapter<String>(this.context, R.layout.item_spinner_drop_down_small, this.childLayoutList);
        this.childSpinner.setAdapter(this.childAdapter);
        if (this.childLayoutList.size() == 0) {
            this.currentChild = null;
        } else if (this.childLayoutList.contains(this.currentChild)) {
            this.childSpinner.setSelection(this.childAdapter.getPosition(this.currentChild));
        } else {
            this.childSpinner.setSelection(0);
            this.currentChild = this.childLayoutList.get(0);
        }
        this.viewManager.refreshLayout(this.currentPattern.name, this.currentChild, this.editMode);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == this.switchMenuFloat) {
            this.gameMenuSetting.menuFloatSetting.enable = b;
            if (b) {
                this.baseLayout.addView(this.viewManager.menuFloat);
            } else {
                this.baseLayout.removeView(this.viewManager.menuFloat);
            }
            this.checkOpenMenuSetting();
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchMenuView) {
            this.gameMenuSetting.menuViewSetting.enable = b;
            if (b) {
                this.baseLayout.addView(this.viewManager.menuView);
            } else {
                this.baseLayout.removeView(this.viewManager.menuView);
            }
            this.checkOpenMenuSetting();
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchMenuSlide) {
            this.gameMenuSetting.menuSlideSetting = b;
            this.checkOpenMenuSetting();
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchLaunchLog) {
            this.gameMenuSetting.hideLaunchLog = !b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
            if (b) {
                LaunchLogWindow.showFor((Activity)this.activity, (ViewGroup)this.drawerLayout);
            } else {
                LaunchLogWindow.closeCurrentIfAny();
            }
        }
        if (compoundButton == this.switchFloatMovable) {
            this.gameMenuSetting.menuFloatSetting.movable = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchAdvanceInput) {
            this.gameMenuSetting.advanceInput = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchTouch) {
            this.gameMenuSetting.enableTouch = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchMousePatch) {
            this.gameMenuSetting.mousePatch = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchSensor) {
            this.gameMenuSetting.enableSensor = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
            if (this.viewManager != null) {
                this.viewManager.setSensorEnable(b);
            }
        }
        if (compoundButton == this.switchHalfScreen) {
            this.gameMenuSetting.disableHalfScreen = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (compoundButton == this.switchHideUI) {
            this.gameMenuSetting.hideUI = b;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
            this.viewManager.hideUI(b);
        }
        if (compoundButton == this.editModeSwitch) {
            this.editMode = b;
            if (b) {
                this.editInfo.setEnabled(true);
                this.manageChild.setEnabled(true);
                this.childSpinner.setEnabled(true);
                this.addView.setEnabled(true);
                this.createPattern.setEnabled(true);
                this.createButtonStyle.setEnabled(true);
            } else {
                this.editInfo.setEnabled(false);
                this.manageChild.setEnabled(false);
                this.childSpinner.setEnabled(false);
                this.addView.setEnabled(false);
                this.createPattern.setEnabled(false);
                this.createButtonStyle.setEnabled(false);
            }
            this.viewManager.refreshLayout(this.currentPattern.name, this.currentChild, b);
        }
        if (compoundButton == this.showOutlineSwitch) {
            this.showOutline = b;
            if (this.viewManager != null) {
                this.viewManager.refreshViews();
            }
        }
    }

    private void showMultiplayerMenu() {
        MultiplayerDialogHelper.showInGame((Activity)this.activity, this.context);
    }

    public void onClick(View view) {
        Dialog dialog;
        if (view == this.openHin2nMenu) {
            this.showMultiplayerMenu();
        }
        if (view == this.forceExit) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setTitle((CharSequence)this.context.getString(R.string.dialog_force_exit_title));
            builder.setMessage((CharSequence)this.context.getString(R.string.dialog_force_exit_message));
            builder.setPositiveButton((CharSequence)this.context.getString(R.string.dialog_force_exit_positive), (dialogInterface, i) -> Process.killProcess((int)Process.myPid()));
            builder.setNegativeButton((CharSequence)this.context.getString(R.string.dialog_force_exit_negative), (dialogInterface, i) -> {});
            AlertDialog dialog2 = builder.create();
            dialog2.show();
        }
        if (view == this.editInfo) {
            dialog = new EditControlPatternDialog(this.context, (Activity)this.activity, this.enableNameEditor, new EditControlPatternDialog.OnPatternInfoChangeListener(){

                @Override
                public void OnInfoChange(ControlPattern controlPattern) {
                    if (MenuHelper.this.currentPattern.name.equals(MenuHelper.this.initialPattern)) {
                        MenuHelper.this.initialPattern = controlPattern.name;
                    }
                    FileUtils.rename(AppManifest.CONTROLLER_DIR + "/" + MenuHelper.this.currentPattern.name, controlPattern.name);
                    Gson gson = new Gson();
                    String string2 = gson.toJson((Object)controlPattern);
                    FileStringUtils.writeFile(AppManifest.CONTROLLER_DIR + "/" + controlPattern.name + "/info.json", string2);
                    for (ChildLayout childLayout : SettingUtils.getChildList(controlPattern.name)) {
                        for (BaseButtonInfo baseButtonInfo : childLayout.baseButtonList) {
                            baseButtonInfo.pattern = controlPattern.name;
                        }
                        for (BaseRockerViewInfo baseRockerViewInfo : childLayout.baseRockerViewList) {
                            baseRockerViewInfo.pattern = controlPattern.name;
                        }
                        ChildLayout.saveChildLayout(controlPattern.name, childLayout);
                    }
                    MenuHelper.this.patternList = SettingUtils.getControlPatternList();
                    MenuHelper.this.currentPattern = controlPattern;
                    ArrayList<String> patterns = new ArrayList<String>();
                    for (ControlPattern pattern : MenuHelper.this.patternList) {
                        patterns.add(pattern.name);
                    }
                    ArrayAdapter arrayAdapter = new ArrayAdapter(MenuHelper.this.context, R.layout.item_spinner_drop_down_small, patterns);
                    MenuHelper.this.patternSpinner.setAdapter((SpinnerAdapter)arrayAdapter);
                    MenuHelper.this.patternSpinner.setSelection(arrayAdapter.getPosition((Object)MenuHelper.this.currentPattern.name));
                }
            }, this.currentPattern);
            dialog.show();
        }
        if (view == this.manageChild) {
            dialog = new ChildManagerDialog(this.context, this, this.currentPattern);
            dialog.show();
        }
        if (view == this.createPattern) {
            dialog = new CreateControlPatternDialog(this.context, (Activity)this.activity, new CreateControlPatternDialog.OnPatternCreateListener(){

                @Override
                public void OnPatternCreate(ControlPattern controlPattern) {
                    FileUtils.createDirectory(AppManifest.CONTROLLER_DIR + "/" + controlPattern.name);
                    Gson gson = new Gson();
                    String string2 = gson.toJson((Object)controlPattern);
                    FileStringUtils.writeFile(AppManifest.CONTROLLER_DIR + "/" + controlPattern.name + "/info.json", string2);
                }
            });
            dialog.show();
        }
        if (view == this.createButtonStyle) {
            dialog = new CreateButtonStyleDialog(this.context, SettingUtils.getButtonStyleList(), new CreateButtonStyleDialog.OnButtonStyleCreateListener(){

                @Override
                public void onButtonStyleCreate(ButtonStyle buttonStyle) {
                    ArrayList<ButtonStyle> styles = SettingUtils.getButtonStyleList();
                    styles.add(buttonStyle);
                    SettingUtils.saveButtonStyle(styles);
                }
            });
            dialog.show();
        }
        if (view == this.addView) {
            if (this.currentChild == null) {
                Toast.makeText((Context)this.context, (CharSequence)this.context.getString(R.string.drawer_custom_menu_warn), (int)0).show();
            } else {
                dialog = new AddViewDialog(this.context, this.currentPattern.name, this.currentChild, this.screenWidth, this.screenHeight, new AddViewDialog.OnViewCreateListener(){

                    @Override
                    public void onButtonCreate(BaseButtonInfo baseButtonInfo) {
                        MenuHelper.this.viewManager.addButton(baseButtonInfo, 0);
                    }

                    @Override
                    public void onRockerCreate(BaseRockerViewInfo baseRockerViewInfo) {
                        MenuHelper.this.viewManager.addRocker(baseRockerViewInfo, 0);
                    }
                }, this.fullscreen);
                dialog.show();
            }
        }
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == this.spinnerTouchMode) {
            this.gameMenuSetting.touchMode = i;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (adapterView == this.spinnerMouseMode) {
            this.gameMenuSetting.mouseMode = i;
            GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
        }
        if (adapterView == this.patternSpinner) {
            String str = (String)this.patternSpinner.getItemAtPosition(i);
            for (ControlPattern controlPattern : this.patternList) {
                if (!controlPattern.name.equals(str)) continue;
                this.currentPattern = controlPattern;
                break;
            }
            this.refreshChildSpinner();
        }
        if (adapterView == this.childSpinner) {
            this.currentChild = (String)this.childSpinner.getItemAtPosition(i);
            if (this.editMode) {
                this.viewManager.refreshLayout(this.currentPattern.name, (String)this.childSpinner.getItemAtPosition(i), true);
            }
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (seekBar == this.sensitivitySeekbar) {
            this.gameMenuSetting.sensitivity = i;
            this.sensitivityText.setText((CharSequence)Integer.toString(i));
        }
        if (seekBar == this.mouseSpeedSeekbar) {
            this.gameMenuSetting.mouseSpeed = (float)i / 100.0f;
            this.mouseSpeedText.setText((CharSequence)Integer.toString(i));
        }
        if (seekBar == this.mouseSizeSeekbar) {
            this.gameMenuSetting.mouseSize = i;
            this.mouseSizeText.setText((CharSequence)Integer.toString(i));
        }
        GameMenuSetting.saveGameMenuSetting(this.gameMenuSetting);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}

