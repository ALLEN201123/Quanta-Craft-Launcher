/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Dialog
 *  android.content.Context
 *  android.graphics.Color
 *  android.graphics.drawable.ColorDrawable
 *  android.graphics.drawable.Drawable
 *  android.os.Build$VERSION
 *  android.text.Editable
 *  android.text.TextWatcher
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.AdapterView
 *  android.widget.AdapterView$OnItemSelectedListener
 *  android.widget.ArrayAdapter
 *  android.widget.Button
 *  android.widget.CompoundButton
 *  android.widget.CompoundButton$OnCheckedChangeListener
 *  android.widget.EditText
 *  android.widget.ImageButton
 *  android.widget.LinearLayout
 *  android.widget.SeekBar
 *  android.widget.SeekBar$OnSeekBarChangeListener
 *  android.widget.Spinner
 *  android.widget.TextView
 *  androidx.annotation.NonNull
 *  androidx.annotation.RequiresApi
 *  androidx.appcompat.widget.SwitchCompat
 */
package com.qcl.launcher.launcher.dialogs.control;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;
import com.qcl.launcher.control.bean.BaseButtonInfo;
import com.qcl.launcher.control.bean.BaseRockerViewInfo;
import com.qcl.launcher.control.bean.ViewPosition;
import com.qcl.launcher.control.bean.button.ButtonSize;
import com.qcl.launcher.control.bean.button.ButtonStyle;
import com.qcl.launcher.control.bean.rocker.RockerSize;
import com.qcl.launcher.control.bean.rocker.RockerStyle;
import com.qcl.launcher.launcher.dialogs.control.ButtonStyleManagerDialog;
import com.qcl.launcher.launcher.dialogs.control.ChildVisibilityDialog;
import com.qcl.launcher.launcher.dialogs.control.RockerStyleManagerDialog;
import com.qcl.launcher.launcher.dialogs.control.SelectKeycodeDialog;
import com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;
import java.util.ArrayList;
import java.util.UUID;

import com.qcl.launcher.R;
public class AddViewDialog
extends Dialog
implements View.OnClickListener,
AdapterView.OnItemSelectedListener,
SeekBar.OnSeekBarChangeListener,
CompoundButton.OnCheckedChangeListener,
TextWatcher {
    private String pattern;
    private String child;
    private int screenWidth;
    private int screenHeight;
    private OnViewCreateListener onViewCreateListener;
    private BaseButtonInfo baseButtonInfo;
    private BaseRockerViewInfo baseRockerViewInfo;
    private Button showButton;
    private Button showRocker;
    private LinearLayout editButton;
    private LinearLayout editRocker;
    private Button positive;
    private Button negative;
    private int viewType;
    private ArrayAdapter<String> showTypeAdapter;
    private ArrayAdapter<String> sizeTypeAdapter;
    private ArrayAdapter<String> positionTypeAdapter;
    private ArrayAdapter<String> sizeObjectAdapter;
    private ArrayAdapter<String> functionTypeAdapter;
    private ArrayAdapter<String> buttonStyleAdapter;
    private ArrayAdapter<String> followTypeAdapter;
    private ArrayAdapter<String> rockerStyleAdapter;
    private EditText editButtonText;
    private Spinner buttonShowTypeSpinner;
    private Spinner buttonSizeTypeSpinner;
    private Spinner buttonPositionTypeSpinner;
    private LinearLayout widthObjectLayout;
    private Spinner widthObjectSpinner;
    private LinearLayout heightObjectLayout;
    private Spinner heightObjectSpinner;
    private SeekBar buttonWidthSeekbar;
    private SeekBar buttonHeightSeekbar;
    private SeekBar buttonXSeekbar;
    private SeekBar buttonYSeekbar;
    private TextView buttonWidthText;
    private TextView buttonHeightText;
    private TextView buttonXText;
    private TextView buttonYText;
    private Spinner functionTypeSpinner;
    private SwitchCompat checkViewMove;
    private SwitchCompat checkAutoKeep;
    private SwitchCompat checkAutoClick;
    private SwitchCompat checkOpenMenu;
    private SwitchCompat checkMovable;
    private SwitchCompat checkTouchMode;
    private SwitchCompat checkSensor;
    private SwitchCompat checkLeftPad;
    private SwitchCompat checkOpenInput;
    private Button childVisibility;
    private EditText editOutputText;
    private Button outputKeycode;
    private Spinner selectExist;
    private SwitchCompat checkUsingExist;
    private Button createButtonStyle;
    private LinearLayout buttonStyleLayout;
    private SeekBar textSizeSeekbar;
    private SeekBar cornerRadiusSeekbar;
    private SeekBar strokeWidthSeekbar;
    private SeekBar textSizePressedSeekbar;
    private SeekBar cornerRadiusPressedSeekbar;
    private SeekBar strokeWidthPressedSeekbar;
    private TextView textSizeText;
    private TextView cornerRadiusText;
    private TextView strokeWidthText;
    private TextView textSizePressedText;
    private TextView cornerRadiusPressedText;
    private TextView strokeWidthPressedText;
    private Button selectTextColor;
    private Button selectStrokeColor;
    private Button selectFillColor;
    private Button selectTextColorPressed;
    private Button selectStrokeColorPressed;
    private Button selectFillColorPressed;
    private View textColorPre;
    private View strokeColorPre;
    private View fillColorPre;
    private View textColorPressedPre;
    private View strokeColorPressedPre;
    private View fillColorPressedPre;
    private TextView textColorText;
    private TextView strokeColorText;
    private TextView fillColorText;
    private TextView textColorPressedText;
    private TextView strokeColorPressedText;
    private TextView fillColorPressedText;
    private ImageButton addButtonWidth;
    private ImageButton addButtonHeight;
    private ImageButton addButtonX;
    private ImageButton addButtonY;
    private ImageButton addButtonTextSize;
    private ImageButton addButtonCornerRadius;
    private ImageButton addButtonStrokeWidth;
    private ImageButton addButtonTextSizePress;
    private ImageButton addButtonCornerRadiusPress;
    private ImageButton addButtonStrokeWidthPress;
    private ImageButton reduceButtonWidth;
    private ImageButton reduceButtonHeight;
    private ImageButton reduceButtonX;
    private ImageButton reduceButtonY;
    private ImageButton reduceButtonTextSize;
    private ImageButton reduceButtonCornerRadius;
    private ImageButton reduceButtonStrokeWidth;
    private ImageButton reduceButtonTextSizePress;
    private ImageButton reduceButtonCornerRadiusPress;
    private ImageButton reduceButtonStrokeWidthPress;
    private Spinner rockerShowTypeSpinner;
    private Spinner rockerSizeTypeSpinner;
    private Spinner rockerPositionTypeSpinner;
    private LinearLayout sizeObjectLayout;
    private Spinner sizeObjectSpinner;
    private SeekBar rockerSizeSeekbar;
    private SeekBar rockerXSeekbar;
    private SeekBar rockerYSeekbar;
    private TextView rockerSizeText;
    private TextView rockerXText;
    private TextView rockerYText;
    private Spinner followTypeSpinner;
    private SwitchCompat checkShift;
    private Spinner selectExistRockerStyle;
    private SwitchCompat checkUsingExistRockerStyle;
    private Button createRockerStyle;
    private LinearLayout rockerStyleLayout;
    private SeekBar rockerCornerRadiusSeekbar;
    private SeekBar rockerCornerRadiusPressSeekbar;
    private SeekBar rockerStrokeWidthSeekbar;
    private SeekBar rockerStrokeWidthPressSeekbar;
    private TextView rockerCornerRadiusText;
    private TextView rockerStrokeWidthText;
    private TextView rockerCornerRadiusPressedText;
    private TextView rockerStrokeWidthPressedText;
    private Button selectPointerColor;
    private Button selectRockerStrokeColor;
    private Button selectRockerFillColor;
    private Button selectPointerColorPressed;
    private Button selectRockerStrokeColorPressed;
    private Button selectRockerFillColorPressed;
    private View pointerColorPre;
    private View rockerStrokeColorPre;
    private View rockerFillColorPre;
    private View pointerColorPressedPre;
    private View rockerStrokeColorPressedPre;
    private View rockerFillColorPressedPre;
    private TextView pointerColorText;
    private TextView rockerStrokeColorText;
    private TextView rockerFillColorText;
    private TextView pointerColorPressedText;
    private TextView rockerStrokeColorPressedText;
    private TextView rockerFillColorPressedText;
    private ImageButton addRockerSize;
    private ImageButton addRockerX;
    private ImageButton addRockerY;
    private ImageButton addRockerCornerRadius;
    private ImageButton addRockerStrokeWidth;
    private ImageButton addRockerCornerRadiusPress;
    private ImageButton addRockerStrokeWidthPress;
    private ImageButton reduceRockerSize;
    private ImageButton reduceRockerX;
    private ImageButton reduceRockerY;
    private ImageButton reduceRockerCornerRadius;
    private ImageButton reduceRockerStrokeWidth;
    private ImageButton reduceRockerCornerRadiusPress;
    private ImageButton reduceRockerStrokeWidthPress;
    private ButtonStyle selectedButtonStyle;
    private RockerStyle selectedRockerStyle;

    @RequiresApi(api=26)
    public AddViewDialog(@NonNull Context context, String pattern, String child, int screenWidth, int screenHeight, OnViewCreateListener onViewCreateListener, boolean fullscreen) {
        super(context);
        this.pattern = pattern;
        this.child = child;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.onViewCreateListener = onViewCreateListener;
        this.viewType = 0;
        this.setContentView(R.layout.dialog_add_view);
        this.setCancelable(false);
        this.getWindow().setBackgroundDrawable((Drawable)new ColorDrawable(0));
        this.getWindow().setLayout(-1, -1);
        this.getWindow().getDecorView().setSystemUiVisibility(5894);
        if (Build.VERSION.SDK_INT >= 28) {
            this.getWindow().getAttributes().layoutInDisplayCutoutMode = fullscreen ? 1 : 2;
        }
        this.init();
    }

    @RequiresApi(api=26)
    private void init() {
        ButtonStyle styleB = SettingUtils.getButtonStyleList().get(0);
        this.baseButtonInfo = new BaseButtonInfo(UUID.randomUUID().toString(), this.pattern, this.child, "", 0, 1, new ButtonSize(50, 0.06f, 0), new ButtonSize(50, 0.06f, 0), 0, new ViewPosition(0, 0.0f), new ViewPosition(0, 0.0f), 0, false, false, false, false, false, false, false, false, false, new ArrayList<String>(), "", new ArrayList<Integer>(), true, styleB);
        RockerStyle styleR = SettingUtils.getRockerStyleList().get(0);
        this.baseRockerViewInfo = new BaseRockerViewInfo(UUID.randomUUID().toString(), this.pattern, this.child, 0, 1, new RockerSize(160, 0.2f, 0), 0, new ViewPosition(0, 0.0f), new ViewPosition(0, 0.0f), 0, true, true, styleR);
        this.showButton = (Button)this.findViewById(R.id.add_button);
        this.showRocker = (Button)this.findViewById(R.id.add_rocker);
        this.editButton = (LinearLayout)this.findViewById(R.id.add_button_layout);
        this.editRocker = (LinearLayout)this.findViewById(R.id.add_rocker_layout);
        this.positive = (Button)this.findViewById(R.id.add_current_view);
        this.negative = (Button)this.findViewById(R.id.exit);
        this.showButton.setOnClickListener((View.OnClickListener)this);
        this.showRocker.setOnClickListener((View.OnClickListener)this);
        this.positive.setOnClickListener((View.OnClickListener)this);
        this.negative.setOnClickListener((View.OnClickListener)this);
        ArrayList<String> showType = new ArrayList<String>();
        ArrayList<String> sizeType = new ArrayList<String>();
        ArrayList<String> positionType = new ArrayList<String>();
        ArrayList<String> sizeObject = new ArrayList<String>();
        ArrayList<String> functionType = new ArrayList<String>();
        ArrayList<String> followType = new ArrayList<String>();
        showType.add(this.getContext().getString(R.string.dialog_add_view_always));
        showType.add(this.getContext().getString(R.string.dialog_add_view_only_in_game));
        showType.add(this.getContext().getString(R.string.dialog_add_view_only_out_game));
        sizeType.add(this.getContext().getString(R.string.dialog_add_view_size_type_percent));
        sizeType.add(this.getContext().getString(R.string.dialog_add_view_size_type_absolute));
        positionType.add(this.getContext().getString(R.string.dialog_add_view_position_type_percent));
        positionType.add(this.getContext().getString(R.string.dialog_add_view_position_type_absolute));
        sizeObject.add(this.getContext().getString(R.string.dialog_add_view_size_type_percent_object_width));
        sizeObject.add(this.getContext().getString(R.string.dialog_add_view_size_type_percent_object_height));
        functionType.add(this.getContext().getString(R.string.dialog_add_view_button_function_type_click));
        functionType.add(this.getContext().getString(R.string.dialog_add_view_button_function_type_double_click));
        followType.add(this.getContext().getString(R.string.dialog_add_view_rocker_function_follow_none));
        followType.add(this.getContext().getString(R.string.dialog_add_view_rocker_function_follow_part));
        followType.add(this.getContext().getString(R.string.dialog_add_view_rocker_function_follow_all));
        this.showTypeAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, showType);
        this.sizeTypeAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, sizeType);
        this.positionTypeAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, positionType);
        this.sizeObjectAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, sizeObject);
        this.functionTypeAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, functionType);
        this.followTypeAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, followType);
        this.initButtonLayout();
        this.initRockerLayout();
    }

    @RequiresApi(api=26)
    @SuppressLint(value={"SetTextI18n"})
    private void initButtonLayout() {
        this.editButtonText = (EditText)this.findViewById(R.id.edit_button_text);
        this.buttonShowTypeSpinner = (Spinner)this.findViewById(R.id.button_show_type);
        this.buttonSizeTypeSpinner = (Spinner)this.findViewById(R.id.button_size_type);
        this.buttonPositionTypeSpinner = (Spinner)this.findViewById(R.id.button_position_type);
        this.widthObjectLayout = (LinearLayout)this.findViewById(R.id.width_object_layout);
        this.widthObjectSpinner = (Spinner)this.findViewById(R.id.width_object);
        this.heightObjectLayout = (LinearLayout)this.findViewById(R.id.height_object_layout);
        this.heightObjectSpinner = (Spinner)this.findViewById(R.id.height_object);
        this.buttonWidthSeekbar = (SeekBar)this.findViewById(R.id.button_width_seekbar);
        this.buttonHeightSeekbar = (SeekBar)this.findViewById(R.id.button_height_seekbar);
        this.buttonXSeekbar = (SeekBar)this.findViewById(R.id.button_x_seekbar);
        this.buttonYSeekbar = (SeekBar)this.findViewById(R.id.button_y_seekbar);
        this.buttonWidthText = (TextView)this.findViewById(R.id.width_text);
        this.buttonHeightText = (TextView)this.findViewById(R.id.height_text);
        this.buttonXText = (TextView)this.findViewById(R.id.button_x_text);
        this.buttonYText = (TextView)this.findViewById(R.id.button_y_text);
        this.functionTypeSpinner = (Spinner)this.findViewById(R.id.function_type);
        this.checkViewMove = (SwitchCompat)this.findViewById(R.id.function_extra_perspective);
        this.checkAutoKeep = (SwitchCompat)this.findViewById(R.id.function_extra_keep);
        this.checkAutoClick = (SwitchCompat)this.findViewById(R.id.function_extra_auto_click);
        this.checkOpenMenu = (SwitchCompat)this.findViewById(R.id.function_open_menu);
        this.checkMovable = (SwitchCompat)this.findViewById(R.id.function_movable);
        this.checkTouchMode = (SwitchCompat)this.findViewById(R.id.function_touch_mode);
        this.checkSensor = (SwitchCompat)this.findViewById(R.id.function_sensor);
        this.checkLeftPad = (SwitchCompat)this.findViewById(R.id.function_left_touch);
        this.checkOpenInput = (SwitchCompat)this.findViewById(R.id.function_open_input);
        this.childVisibility = (Button)this.findViewById(R.id.function_child_visibility);
        this.editOutputText = (EditText)this.findViewById(R.id.function_output_keychar);
        this.outputKeycode = (Button)this.findViewById(R.id.function_output_keycode);
        this.selectExist = (Spinner)this.findViewById(R.id.exterior_use_exist_button);
        this.checkUsingExist = (SwitchCompat)this.findViewById(R.id.switch_exterior_use_exist_button);
        this.createButtonStyle = (Button)this.findViewById(R.id.exterior_add_button_style);
        this.buttonStyleLayout = (LinearLayout)this.findViewById(R.id.button_style_layout);
        this.textSizeSeekbar = (SeekBar)this.findViewById(R.id.exterior_text_size_seekbar);
        this.cornerRadiusSeekbar = (SeekBar)this.findViewById(R.id.exterior_corner_radius_seekbar);
        this.strokeWidthSeekbar = (SeekBar)this.findViewById(R.id.exterior_stroke_width_seekbar);
        this.textSizePressedSeekbar = (SeekBar)this.findViewById(R.id.exterior_text_size_seekbar_pressed);
        this.cornerRadiusPressedSeekbar = (SeekBar)this.findViewById(R.id.exterior_corner_radius_seekbar_pressed);
        this.strokeWidthPressedSeekbar = (SeekBar)this.findViewById(R.id.exterior_stroke_width_seekbar_pressed);
        this.textSizeText = (TextView)this.findViewById(R.id.text_size_text);
        this.cornerRadiusText = (TextView)this.findViewById(R.id.corner_radius_text);
        this.strokeWidthText = (TextView)this.findViewById(R.id.stroke_width_text);
        this.textSizePressedText = (TextView)this.findViewById(R.id.text_size_pressed_text);
        this.cornerRadiusPressedText = (TextView)this.findViewById(R.id.corner_radius_pressed_text);
        this.strokeWidthPressedText = (TextView)this.findViewById(R.id.stroke_width_pressed_text);
        this.selectTextColor = (Button)this.findViewById(R.id.exterior_text_color);
        this.selectStrokeColor = (Button)this.findViewById(R.id.exterior_stroke_color);
        this.selectFillColor = (Button)this.findViewById(R.id.exterior_fill_color);
        this.selectTextColorPressed = (Button)this.findViewById(R.id.exterior_text_color_pressed);
        this.selectStrokeColorPressed = (Button)this.findViewById(R.id.exterior_stroke_color_pressed);
        this.selectFillColorPressed = (Button)this.findViewById(R.id.exterior_fill_color_pressed);
        this.textColorPre = this.findViewById(R.id.text_color_preview);
        this.strokeColorPre = this.findViewById(R.id.stroke_color_preview);
        this.fillColorPre = this.findViewById(R.id.fill_color_preview);
        this.textColorPressedPre = this.findViewById(R.id.text_color_pressed_preview);
        this.strokeColorPressedPre = this.findViewById(R.id.stroke_color_pressed_preview);
        this.fillColorPressedPre = this.findViewById(R.id.fill_color_pressed_preview);
        this.textColorText = (TextView)this.findViewById(R.id.text_color_text);
        this.strokeColorText = (TextView)this.findViewById(R.id.stroke_color_text);
        this.fillColorText = (TextView)this.findViewById(R.id.fill_color_text);
        this.textColorPressedText = (TextView)this.findViewById(R.id.text_color_pressed_text);
        this.strokeColorPressedText = (TextView)this.findViewById(R.id.stroke_color_pressed_text);
        this.fillColorPressedText = (TextView)this.findViewById(R.id.fill_color_pressed_text);
        this.addButtonWidth = (ImageButton)this.findViewById(R.id.add_button_width);
        this.addButtonHeight = (ImageButton)this.findViewById(R.id.add_button_height);
        this.addButtonX = (ImageButton)this.findViewById(R.id.add_button_x);
        this.addButtonY = (ImageButton)this.findViewById(R.id.add_button_y);
        this.addButtonTextSize = (ImageButton)this.findViewById(R.id.add_text_size);
        this.addButtonCornerRadius = (ImageButton)this.findViewById(R.id.add_corner_radius);
        this.addButtonStrokeWidth = (ImageButton)this.findViewById(R.id.add_stroke_width);
        this.addButtonTextSizePress = (ImageButton)this.findViewById(R.id.add_text_size_pressed);
        this.addButtonCornerRadiusPress = (ImageButton)this.findViewById(R.id.add_corner_radius_pressed);
        this.addButtonStrokeWidthPress = (ImageButton)this.findViewById(R.id.add_stroke_width_pressed);
        this.reduceButtonWidth = (ImageButton)this.findViewById(R.id.reduce_button_width);
        this.reduceButtonHeight = (ImageButton)this.findViewById(R.id.reduce_button_height);
        this.reduceButtonX = (ImageButton)this.findViewById(R.id.reduce_button_x);
        this.reduceButtonY = (ImageButton)this.findViewById(R.id.reduce_button_y);
        this.reduceButtonTextSize = (ImageButton)this.findViewById(R.id.reduce_text_size);
        this.reduceButtonCornerRadius = (ImageButton)this.findViewById(R.id.reduce_corner_radius);
        this.reduceButtonStrokeWidth = (ImageButton)this.findViewById(R.id.reduce_stroke_width);
        this.reduceButtonTextSizePress = (ImageButton)this.findViewById(R.id.reduce_text_size_pressed);
        this.reduceButtonCornerRadiusPress = (ImageButton)this.findViewById(R.id.reduce_corner_radius_pressed);
        this.reduceButtonStrokeWidthPress = (ImageButton)this.findViewById(R.id.reduce_stroke_width_pressed);
        this.editButtonText.setText((CharSequence)this.baseButtonInfo.text);
        this.editOutputText.setText((CharSequence)this.baseButtonInfo.outputText);
        this.buttonShowTypeSpinner.setAdapter(this.showTypeAdapter);
        this.buttonShowTypeSpinner.setSelection(this.baseButtonInfo.showType);
        this.buttonSizeTypeSpinner.setAdapter(this.sizeTypeAdapter);
        this.buttonSizeTypeSpinner.setSelection(this.baseButtonInfo.sizeType);
        this.buttonPositionTypeSpinner.setAdapter(this.positionTypeAdapter);
        this.buttonPositionTypeSpinner.setSelection(this.baseButtonInfo.positionType);
        if (this.baseButtonInfo.sizeType == 1) {
            this.widthObjectLayout.setVisibility(8);
            this.heightObjectLayout.setVisibility(8);
        }
        this.widthObjectSpinner.setAdapter(this.sizeObjectAdapter);
        this.widthObjectSpinner.setSelection(this.baseButtonInfo.width.object);
        this.heightObjectSpinner.setAdapter(this.sizeObjectAdapter);
        this.heightObjectSpinner.setSelection(this.baseButtonInfo.height.object);
        this.functionTypeSpinner.setAdapter(this.functionTypeAdapter);
        this.functionTypeSpinner.setSelection(this.baseButtonInfo.functionType);
        if (this.baseButtonInfo.sizeType == 0) {
            this.buttonWidthSeekbar.setMin(1);
            this.buttonHeightSeekbar.setMin(1);
            this.buttonWidthSeekbar.setMax(1000);
            this.buttonHeightSeekbar.setMax(1000);
            this.buttonWidthSeekbar.setProgress((int)(1000.0f * this.baseButtonInfo.width.percentSize));
            this.buttonHeightSeekbar.setProgress((int)(1000.0f * this.baseButtonInfo.height.percentSize));
            this.buttonWidthText.setText((CharSequence)((float)((int)(100.0f * this.baseButtonInfo.width.percentSize)) / 10.0f + " %"));
            this.buttonHeightText.setText((CharSequence)((float)((int)(100.0f * this.baseButtonInfo.height.percentSize)) / 10.0f + " %"));
        } else {
            this.buttonWidthSeekbar.setMin(1);
            this.buttonHeightSeekbar.setMin(1);
            this.buttonWidthSeekbar.setMax(200);
            this.buttonHeightSeekbar.setMax(200);
            this.buttonWidthSeekbar.setProgress(this.baseButtonInfo.width.absoluteSize);
            this.buttonHeightSeekbar.setProgress(this.baseButtonInfo.height.absoluteSize);
            this.buttonWidthText.setText((CharSequence)(this.baseButtonInfo.width.absoluteSize + " dp"));
            this.buttonHeightText.setText((CharSequence)(this.baseButtonInfo.height.absoluteSize + " dp"));
        }
        if (this.baseButtonInfo.positionType == 0) {
            this.buttonXSeekbar.setMax(1000);
            this.buttonYSeekbar.setMax(1000);
            this.buttonXSeekbar.setProgress((int)(1000.0f * this.baseButtonInfo.xPosition.percentPosition));
            this.buttonYSeekbar.setProgress((int)(1000.0f * this.baseButtonInfo.yPosition.percentPosition));
            this.buttonXText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.xPosition.percentPosition)) / 10.0f + " %"));
            this.buttonYText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.yPosition.percentPosition)) / 10.0f + " %"));
        } else {
            this.buttonXSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenWidth));
            this.buttonYSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenHeight));
            this.buttonXSeekbar.setProgress(this.baseButtonInfo.xPosition.absolutePosition);
            this.buttonYSeekbar.setProgress(this.baseButtonInfo.yPosition.absolutePosition);
            this.buttonXText.setText((CharSequence)(this.baseButtonInfo.xPosition.absolutePosition + " dp"));
            this.buttonYText.setText((CharSequence)(this.baseButtonInfo.yPosition.absolutePosition + " dp"));
        }
        this.onButtonSizeChange();
        this.checkViewMove.setChecked(this.baseButtonInfo.viewMove);
        this.checkAutoKeep.setChecked(this.baseButtonInfo.autoKeep);
        this.checkAutoClick.setChecked(this.baseButtonInfo.autoClick);
        this.checkOpenMenu.setChecked(this.baseButtonInfo.openMenu);
        this.checkMovable.setChecked(this.baseButtonInfo.movable);
        this.checkTouchMode.setChecked(this.baseButtonInfo.switchTouchMode);
        this.checkSensor.setChecked(this.baseButtonInfo.switchSensor);
        this.checkLeftPad.setChecked(this.baseButtonInfo.switchLeftPad);
        this.checkOpenInput.setChecked(this.baseButtonInfo.showInputDialog);
        this.refreshButtonStyleList(true);
        ArrayList<String> names = new ArrayList<String>();
        for (ButtonStyle style2 : SettingUtils.getButtonStyleList()) {
            names.add(style2.name);
        }
        if (this.baseButtonInfo.usingExist && names.contains(this.baseButtonInfo.buttonStyle.name)) {
            this.buttonStyleLayout.setVisibility(8);
            this.checkUsingExist.setChecked(true);
            this.selectExist.setSelection(this.buttonStyleAdapter.getPosition(this.baseButtonInfo.buttonStyle.name));
            this.selectedButtonStyle = this.baseButtonInfo.buttonStyle;
        } else {
            this.baseButtonInfo.usingExist = false;
            this.baseButtonInfo.buttonStyle.name = "";
            this.selectedButtonStyle = SettingUtils.getButtonStyleList().get(0);
        }
        this.refreshButtonStyleEditor();
        this.buttonShowTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.buttonSizeTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.buttonPositionTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.widthObjectSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.heightObjectSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.functionTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.selectExist.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.buttonWidthSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.buttonHeightSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.buttonXSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.buttonYSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.textSizeSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.cornerRadiusSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.strokeWidthSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.textSizePressedSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.cornerRadiusPressedSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.strokeWidthPressedSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.checkViewMove.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkAutoKeep.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkAutoClick.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkOpenMenu.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkMovable.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkTouchMode.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkSensor.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkLeftPad.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkOpenInput.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkUsingExist.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.childVisibility.setOnClickListener((View.OnClickListener)this);
        this.outputKeycode.setOnClickListener((View.OnClickListener)this);
        this.createButtonStyle.setOnClickListener((View.OnClickListener)this);
        this.selectTextColor.setOnClickListener((View.OnClickListener)this);
        this.selectStrokeColor.setOnClickListener((View.OnClickListener)this);
        this.selectFillColor.setOnClickListener((View.OnClickListener)this);
        this.selectTextColorPressed.setOnClickListener((View.OnClickListener)this);
        this.selectStrokeColorPressed.setOnClickListener((View.OnClickListener)this);
        this.selectFillColorPressed.setOnClickListener((View.OnClickListener)this);
        this.editButtonText.addTextChangedListener((TextWatcher)this);
        this.editOutputText.addTextChangedListener((TextWatcher)this);
        this.addButtonWidth.setOnClickListener((View.OnClickListener)this);
        this.addButtonHeight.setOnClickListener((View.OnClickListener)this);
        this.addButtonX.setOnClickListener((View.OnClickListener)this);
        this.addButtonY.setOnClickListener((View.OnClickListener)this);
        this.addButtonTextSize.setOnClickListener((View.OnClickListener)this);
        this.addButtonStrokeWidth.setOnClickListener((View.OnClickListener)this);
        this.addButtonCornerRadius.setOnClickListener((View.OnClickListener)this);
        this.addButtonTextSizePress.setOnClickListener((View.OnClickListener)this);
        this.addButtonStrokeWidthPress.setOnClickListener((View.OnClickListener)this);
        this.addButtonCornerRadiusPress.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonWidth.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonHeight.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonX.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonY.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonTextSize.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonStrokeWidth.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonCornerRadius.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonTextSizePress.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonStrokeWidthPress.setOnClickListener((View.OnClickListener)this);
        this.reduceButtonCornerRadiusPress.setOnClickListener((View.OnClickListener)this);
    }

    public void refreshButtonStyleList(boolean first) {
        ArrayList<ButtonStyle> styles = SettingUtils.getButtonStyleList();
        ArrayList<String> names = new ArrayList<String>();
        for (ButtonStyle style2 : styles) {
            names.add(style2.name);
        }
        this.buttonStyleAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, names);
        this.selectExist.setAdapter(this.buttonStyleAdapter);
        if (!first) {
            if (this.baseButtonInfo.usingExist && names.contains(this.baseButtonInfo.buttonStyle.name)) {
                this.buttonStyleLayout.setVisibility(8);
                this.checkUsingExist.setChecked(true);
                this.selectExist.setSelection(this.buttonStyleAdapter.getPosition(this.baseButtonInfo.buttonStyle.name));
                this.selectedButtonStyle = this.baseButtonInfo.buttonStyle;
            } else {
                this.buttonStyleLayout.setVisibility(0);
                this.checkUsingExist.setChecked(false);
                this.baseButtonInfo.usingExist = false;
                this.baseButtonInfo.buttonStyle.name = "";
                this.selectedButtonStyle = SettingUtils.getButtonStyleList().get(0);
            }
            this.refreshButtonStyleEditor();
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    private void onButtonSizeChange() {
        int yMax;
        int xMax;
        if (this.baseButtonInfo.sizeType == 0) {
            int widthObject = this.baseButtonInfo.width.object == 0 ? this.screenWidth : this.screenHeight;
            int heightObject = this.baseButtonInfo.height.object == 0 ? this.screenWidth : this.screenHeight;
            xMax = (int)((float)ConvertUtils.px2dip(this.getContext(), this.screenWidth) - (float)ConvertUtils.px2dip(this.getContext(), widthObject) * this.baseButtonInfo.width.percentSize);
            yMax = (int)((float)ConvertUtils.px2dip(this.getContext(), this.screenHeight) - (float)ConvertUtils.px2dip(this.getContext(), heightObject) * this.baseButtonInfo.height.percentSize);
        } else {
            xMax = ConvertUtils.px2dip(this.getContext(), this.screenWidth) - this.baseButtonInfo.width.absoluteSize;
            yMax = ConvertUtils.px2dip(this.getContext(), this.screenHeight) - this.baseButtonInfo.height.absoluteSize;
        }
        if (this.baseButtonInfo.positionType == 1) {
            if (this.baseButtonInfo.xPosition.absolutePosition > xMax) {
                this.baseButtonInfo.xPosition.absolutePosition = xMax;
                this.buttonXSeekbar.setProgress(xMax);
                this.buttonXText.setText((CharSequence)(xMax + " dp"));
            }
            if (this.baseButtonInfo.yPosition.absolutePosition > yMax) {
                this.baseButtonInfo.yPosition.absolutePosition = yMax;
                this.buttonYSeekbar.setProgress(yMax);
                this.buttonYText.setText((CharSequence)(yMax + " dp"));
            }
            this.buttonXSeekbar.setMax(xMax);
            this.buttonYSeekbar.setMax(yMax);
        } else {
            if (this.baseButtonInfo.xPosition.absolutePosition > xMax) {
                this.baseButtonInfo.xPosition.absolutePosition = xMax;
            }
            if (this.baseButtonInfo.yPosition.absolutePosition > yMax) {
                this.baseButtonInfo.yPosition.absolutePosition = yMax;
            }
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public void refreshButtonStyleEditor() {
        this.textSizeSeekbar.setProgress(this.baseButtonInfo.buttonStyle.textSize);
        this.cornerRadiusSeekbar.setProgress(this.baseButtonInfo.buttonStyle.cornerRadius);
        this.strokeWidthSeekbar.setProgress((int)(this.baseButtonInfo.buttonStyle.strokeWidth * 10.0f));
        this.textSizePressedSeekbar.setProgress(this.baseButtonInfo.buttonStyle.textSizePress);
        this.cornerRadiusPressedSeekbar.setProgress(this.baseButtonInfo.buttonStyle.cornerRadiusPress);
        this.strokeWidthPressedSeekbar.setProgress((int)(this.baseButtonInfo.buttonStyle.strokeWidthPress * 10.0f));
        this.textSizeText.setText((CharSequence)(this.baseButtonInfo.buttonStyle.textSize + " sp"));
        this.cornerRadiusText.setText((CharSequence)(this.baseButtonInfo.buttonStyle.cornerRadius + " dp"));
        this.strokeWidthText.setText((CharSequence)(this.baseButtonInfo.buttonStyle.strokeWidth + " dp"));
        this.textSizePressedText.setText((CharSequence)(this.baseButtonInfo.buttonStyle.textSizePress + " sp"));
        this.cornerRadiusPressedText.setText((CharSequence)(this.baseButtonInfo.buttonStyle.cornerRadiusPress + " dp"));
        this.strokeWidthPressedText.setText((CharSequence)(this.baseButtonInfo.buttonStyle.strokeWidthPress + " dp"));
        this.textColorPre.setBackgroundColor(Color.parseColor((String)this.baseButtonInfo.buttonStyle.textColor));
        this.strokeColorPre.setBackgroundColor(Color.parseColor((String)this.baseButtonInfo.buttonStyle.strokeColor));
        this.fillColorPre.setBackgroundColor(Color.parseColor((String)this.baseButtonInfo.buttonStyle.fillColor));
        this.textColorPressedPre.setBackgroundColor(Color.parseColor((String)this.baseButtonInfo.buttonStyle.textColorPress));
        this.strokeColorPressedPre.setBackgroundColor(Color.parseColor((String)this.baseButtonInfo.buttonStyle.strokeColorPress));
        this.fillColorPressedPre.setBackgroundColor(Color.parseColor((String)this.baseButtonInfo.buttonStyle.fillColorPress));
        this.textColorText.setText((CharSequence)this.baseButtonInfo.buttonStyle.textColor);
        this.strokeColorText.setText((CharSequence)this.baseButtonInfo.buttonStyle.strokeColor);
        this.fillColorText.setText((CharSequence)this.baseButtonInfo.buttonStyle.fillColor);
        this.textColorPressedText.setText((CharSequence)this.baseButtonInfo.buttonStyle.textColorPress);
        this.strokeColorPressedText.setText((CharSequence)this.baseButtonInfo.buttonStyle.strokeColorPress);
        this.fillColorPressedText.setText((CharSequence)this.baseButtonInfo.buttonStyle.fillColorPress);
    }

    @RequiresApi(api=26)
    @SuppressLint(value={"SetTextI18n"})
    private void initRockerLayout() {
        this.rockerShowTypeSpinner = (Spinner)this.findViewById(R.id.rocker_show_type);
        this.rockerSizeTypeSpinner = (Spinner)this.findViewById(R.id.rocker_size_type);
        this.rockerPositionTypeSpinner = (Spinner)this.findViewById(R.id.rocker_position_type);
        this.sizeObjectLayout = (LinearLayout)this.findViewById(R.id.size_object_layout);
        this.sizeObjectSpinner = (Spinner)this.findViewById(R.id.size_object);
        this.rockerSizeSeekbar = (SeekBar)this.findViewById(R.id.rocker_size_seekbar);
        this.rockerXSeekbar = (SeekBar)this.findViewById(R.id.rocker_x_seekbar);
        this.rockerYSeekbar = (SeekBar)this.findViewById(R.id.rocker_y_seekbar);
        this.rockerSizeText = (TextView)this.findViewById(R.id.size_text);
        this.rockerXText = (TextView)this.findViewById(R.id.rocker_x_text);
        this.rockerYText = (TextView)this.findViewById(R.id.rocker_y_text);
        this.followTypeSpinner = (Spinner)this.findViewById(R.id.function_follow);
        this.checkShift = (SwitchCompat)this.findViewById(R.id.function_shift);
        this.selectExistRockerStyle = (Spinner)this.findViewById(R.id.exterior_use_exist_rocker);
        this.checkUsingExistRockerStyle = (SwitchCompat)this.findViewById(R.id.switch_exterior_use_exist_rocker);
        this.createRockerStyle = (Button)this.findViewById(R.id.exterior_add_rocker_style);
        this.rockerStyleLayout = (LinearLayout)this.findViewById(R.id.rocker_style_layout);
        this.rockerCornerRadiusSeekbar = (SeekBar)this.findViewById(R.id.rocker_exterior_corner_radius_seekbar);
        this.rockerCornerRadiusPressSeekbar = (SeekBar)this.findViewById(R.id.rocker_exterior_corner_radius_seekbar_pressed);
        this.rockerStrokeWidthSeekbar = (SeekBar)this.findViewById(R.id.rocker_exterior_stroke_width_seekbar);
        this.rockerStrokeWidthPressSeekbar = (SeekBar)this.findViewById(R.id.rocker_exterior_stroke_width_seekbar_pressed);
        this.rockerCornerRadiusText = (TextView)this.findViewById(R.id.rocker_corner_radius_text);
        this.rockerCornerRadiusPressedText = (TextView)this.findViewById(R.id.rocker_corner_radius_press_text);
        this.rockerStrokeWidthText = (TextView)this.findViewById(R.id.rocker_stroke_width_text);
        this.rockerStrokeWidthPressedText = (TextView)this.findViewById(R.id.rocker_stroke_width_press_text);
        this.selectPointerColor = (Button)this.findViewById(R.id.exterior_pointer_color);
        this.selectRockerStrokeColor = (Button)this.findViewById(R.id.rocker_exterior_stroke_color);
        this.selectRockerFillColor = (Button)this.findViewById(R.id.rocker_exterior_fill_color);
        this.selectPointerColorPressed = (Button)this.findViewById(R.id.exterior_pointer_color_pressed);
        this.selectRockerStrokeColorPressed = (Button)this.findViewById(R.id.rocker_exterior_stroke_color_pressed);
        this.selectRockerFillColorPressed = (Button)this.findViewById(R.id.rocker_exterior_fill_color_pressed);
        this.pointerColorPre = this.findViewById(R.id.pointer_color_preview);
        this.rockerStrokeColorPre = this.findViewById(R.id.rocker_stroke_color_preview);
        this.rockerFillColorPre = this.findViewById(R.id.rocker_fill_color_preview);
        this.pointerColorPressedPre = this.findViewById(R.id.pointer_pressed_color_preview);
        this.rockerStrokeColorPressedPre = this.findViewById(R.id.rocker_stroke_pressed_color_preview);
        this.rockerFillColorPressedPre = this.findViewById(R.id.rocker_fill_pressed_color_preview);
        this.pointerColorText = (TextView)this.findViewById(R.id.pointer_color_text);
        this.rockerStrokeColorText = (TextView)this.findViewById(R.id.rocker_stroke_color_text);
        this.rockerFillColorText = (TextView)this.findViewById(R.id.rocker_fill_color_text);
        this.pointerColorPressedText = (TextView)this.findViewById(R.id.pointer_pressed_color_text);
        this.rockerStrokeColorPressedText = (TextView)this.findViewById(R.id.rocker_stroke_pressed_color_text);
        this.rockerFillColorPressedText = (TextView)this.findViewById(R.id.rocker_fill_pressed_color_text);
        this.addRockerSize = (ImageButton)this.findViewById(R.id.add_rocker_size);
        this.addRockerX = (ImageButton)this.findViewById(R.id.add_rocker_x);
        this.addRockerY = (ImageButton)this.findViewById(R.id.add_rocker_y);
        this.addRockerCornerRadius = (ImageButton)this.findViewById(R.id.add_rocker_corner_radius);
        this.addRockerStrokeWidth = (ImageButton)this.findViewById(R.id.add_rocker_stroke_width);
        this.addRockerCornerRadiusPress = (ImageButton)this.findViewById(R.id.add_rocker_corner_radius_pressed);
        this.addRockerStrokeWidthPress = (ImageButton)this.findViewById(R.id.add_rocker_stroke_width_pressed);
        this.reduceRockerSize = (ImageButton)this.findViewById(R.id.reduce_rocker_size);
        this.reduceRockerX = (ImageButton)this.findViewById(R.id.reduce_rocker_x);
        this.reduceRockerY = (ImageButton)this.findViewById(R.id.reduce_rocker_y);
        this.reduceRockerCornerRadius = (ImageButton)this.findViewById(R.id.reduce_rocker_corner_radius);
        this.reduceRockerStrokeWidth = (ImageButton)this.findViewById(R.id.reduce_rocker_stroke_width);
        this.reduceRockerCornerRadiusPress = (ImageButton)this.findViewById(R.id.reduce_rocker_corner_radius_pressed);
        this.reduceRockerStrokeWidthPress = (ImageButton)this.findViewById(R.id.reduce_rocker_stroke_width_pressed);
        this.rockerShowTypeSpinner.setAdapter(this.showTypeAdapter);
        this.rockerShowTypeSpinner.setSelection(this.baseRockerViewInfo.showType);
        this.rockerSizeTypeSpinner.setAdapter(this.sizeTypeAdapter);
        this.rockerSizeTypeSpinner.setSelection(this.baseRockerViewInfo.sizeType);
        this.rockerPositionTypeSpinner.setAdapter(this.positionTypeAdapter);
        this.rockerPositionTypeSpinner.setSelection(this.baseRockerViewInfo.positionType);
        if (this.baseRockerViewInfo.sizeType == 1) {
            this.sizeObjectLayout.setVisibility(8);
        }
        this.sizeObjectSpinner.setAdapter(this.sizeObjectAdapter);
        this.sizeObjectSpinner.setSelection(this.baseRockerViewInfo.size.object);
        this.followTypeSpinner.setAdapter(this.followTypeAdapter);
        this.followTypeSpinner.setSelection(this.baseRockerViewInfo.followType);
        if (this.baseRockerViewInfo.sizeType == 0) {
            this.rockerSizeSeekbar.setMin(1);
            this.rockerSizeSeekbar.setMax(1000);
            this.rockerSizeSeekbar.setProgress((int)(1000.0f * this.baseRockerViewInfo.size.percentSize));
            this.rockerSizeText.setText((CharSequence)((float)((int)(100.0f * this.baseRockerViewInfo.size.percentSize)) / 10.0f + " %"));
        } else {
            this.rockerSizeSeekbar.setMin(1);
            this.rockerSizeSeekbar.setMax(300);
            this.rockerSizeSeekbar.setProgress(this.baseRockerViewInfo.size.absoluteSize);
            this.rockerSizeText.setText((CharSequence)(this.baseRockerViewInfo.size.absoluteSize + " dp"));
        }
        if (this.baseRockerViewInfo.positionType == 0) {
            this.rockerXSeekbar.setMax(1000);
            this.rockerYSeekbar.setMax(1000);
            this.rockerXSeekbar.setProgress((int)(1000.0f * this.baseRockerViewInfo.xPosition.percentPosition));
            this.rockerYSeekbar.setProgress((int)(1000.0f * this.baseRockerViewInfo.yPosition.percentPosition));
            this.rockerXText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.xPosition.percentPosition)) / 10.0f + " %"));
            this.rockerYText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.yPosition.percentPosition)) / 10.0f + " %"));
        } else {
            this.rockerXSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenWidth));
            this.rockerYSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenHeight));
            this.rockerXSeekbar.setProgress(this.baseRockerViewInfo.xPosition.absolutePosition);
            this.rockerYSeekbar.setProgress(this.baseRockerViewInfo.yPosition.absolutePosition);
            this.rockerXText.setText((CharSequence)(this.baseRockerViewInfo.xPosition.absolutePosition + " dp"));
            this.rockerYText.setText((CharSequence)(this.baseRockerViewInfo.yPosition.absolutePosition + " dp"));
        }
        this.onRockerSizeChange();
        this.checkShift.setChecked(this.baseRockerViewInfo.shift);
        this.refreshRockerStyleList(true);
        ArrayList<String> names = new ArrayList<String>();
        for (RockerStyle style2 : SettingUtils.getRockerStyleList()) {
            names.add(style2.name);
        }
        if (this.baseRockerViewInfo.usingExist && names.contains(this.baseRockerViewInfo.rockerStyle.name)) {
            this.rockerStyleLayout.setVisibility(8);
            this.checkUsingExistRockerStyle.setChecked(true);
            this.selectExistRockerStyle.setSelection(this.rockerStyleAdapter.getPosition(this.baseRockerViewInfo.rockerStyle.name));
            this.selectedRockerStyle = this.baseRockerViewInfo.rockerStyle;
        } else {
            this.baseRockerViewInfo.usingExist = false;
            this.baseRockerViewInfo.rockerStyle.name = "";
            this.selectedRockerStyle = SettingUtils.getRockerStyleList().get(0);
        }
        this.refreshRockerStyleEditor();
        this.rockerShowTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.rockerSizeTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.rockerPositionTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.sizeObjectSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.followTypeSpinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.selectExistRockerStyle.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.rockerSizeSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.rockerXSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.rockerYSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.rockerCornerRadiusSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.rockerCornerRadiusPressSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.rockerStrokeWidthSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.rockerStrokeWidthPressSeekbar.setOnSeekBarChangeListener((SeekBar.OnSeekBarChangeListener)this);
        this.checkShift.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.checkUsingExistRockerStyle.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.createRockerStyle.setOnClickListener((View.OnClickListener)this);
        this.selectPointerColor.setOnClickListener((View.OnClickListener)this);
        this.selectPointerColorPressed.setOnClickListener((View.OnClickListener)this);
        this.selectRockerFillColor.setOnClickListener((View.OnClickListener)this);
        this.selectRockerFillColorPressed.setOnClickListener((View.OnClickListener)this);
        this.selectRockerStrokeColor.setOnClickListener((View.OnClickListener)this);
        this.selectRockerStrokeColorPressed.setOnClickListener((View.OnClickListener)this);
        this.addRockerSize.setOnClickListener((View.OnClickListener)this);
        this.addRockerX.setOnClickListener((View.OnClickListener)this);
        this.addRockerY.setOnClickListener((View.OnClickListener)this);
        this.addRockerStrokeWidth.setOnClickListener((View.OnClickListener)this);
        this.addRockerCornerRadius.setOnClickListener((View.OnClickListener)this);
        this.addRockerStrokeWidthPress.setOnClickListener((View.OnClickListener)this);
        this.addRockerCornerRadiusPress.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerSize.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerX.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerY.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerStrokeWidth.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerCornerRadius.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerStrokeWidthPress.setOnClickListener((View.OnClickListener)this);
        this.reduceRockerCornerRadiusPress.setOnClickListener((View.OnClickListener)this);
    }

    public void refreshRockerStyleList(boolean first) {
        ArrayList<RockerStyle> styles = SettingUtils.getRockerStyleList();
        ArrayList<String> names = new ArrayList<String>();
        for (RockerStyle style2 : styles) {
            names.add(style2.name);
        }
        this.rockerStyleAdapter = new ArrayAdapter(this.getContext(), R.layout.item_spinner, names);
        this.selectExistRockerStyle.setAdapter(this.rockerStyleAdapter);
        if (!first) {
            if (this.baseRockerViewInfo.usingExist && names.contains(this.baseRockerViewInfo.rockerStyle.name)) {
                this.rockerStyleLayout.setVisibility(8);
                this.checkUsingExistRockerStyle.setChecked(true);
                this.selectExistRockerStyle.setSelection(this.rockerStyleAdapter.getPosition(this.baseRockerViewInfo.rockerStyle.name));
                this.selectedRockerStyle = this.baseRockerViewInfo.rockerStyle;
            } else {
                this.rockerStyleLayout.setVisibility(0);
                this.checkUsingExistRockerStyle.setChecked(false);
                this.baseRockerViewInfo.usingExist = false;
                this.baseRockerViewInfo.rockerStyle.name = "";
                this.selectedRockerStyle = SettingUtils.getRockerStyleList().get(0);
            }
            this.refreshRockerStyleEditor();
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    private void onRockerSizeChange() {
        int yMax;
        int xMax;
        if (this.baseRockerViewInfo.sizeType == 0) {
            int sizeObject = this.baseRockerViewInfo.size.object == 0 ? this.screenWidth : this.screenHeight;
            xMax = (int)((float)ConvertUtils.px2dip(this.getContext(), this.screenWidth) - (float)ConvertUtils.px2dip(this.getContext(), sizeObject) * this.baseRockerViewInfo.size.percentSize);
            yMax = (int)((float)ConvertUtils.px2dip(this.getContext(), this.screenHeight) - (float)ConvertUtils.px2dip(this.getContext(), sizeObject) * this.baseRockerViewInfo.size.percentSize);
        } else {
            xMax = ConvertUtils.px2dip(this.getContext(), this.screenWidth) - this.baseRockerViewInfo.size.absoluteSize;
            yMax = ConvertUtils.px2dip(this.getContext(), this.screenHeight) - this.baseRockerViewInfo.size.absoluteSize;
        }
        if (this.baseRockerViewInfo.positionType == 1) {
            if (this.baseRockerViewInfo.xPosition.absolutePosition > xMax) {
                this.baseRockerViewInfo.xPosition.absolutePosition = xMax;
                this.rockerXSeekbar.setProgress(xMax);
                this.rockerXText.setText((CharSequence)(xMax + " dp"));
            }
            if (this.baseRockerViewInfo.yPosition.absolutePosition > yMax) {
                this.baseRockerViewInfo.yPosition.absolutePosition = yMax;
                this.rockerYSeekbar.setProgress(yMax);
                this.rockerYText.setText((CharSequence)(yMax + " dp"));
            }
            this.rockerXSeekbar.setMax(xMax);
            this.rockerYSeekbar.setMax(yMax);
        } else {
            if (this.baseRockerViewInfo.xPosition.absolutePosition > xMax) {
                this.baseRockerViewInfo.xPosition.absolutePosition = xMax;
            }
            if (this.baseRockerViewInfo.yPosition.absolutePosition > yMax) {
                this.baseRockerViewInfo.yPosition.absolutePosition = yMax;
            }
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public void refreshRockerStyleEditor() {
        this.rockerCornerRadiusSeekbar.setProgress(this.baseRockerViewInfo.rockerStyle.cornerRadius);
        this.rockerStrokeWidthSeekbar.setProgress((int)(this.baseRockerViewInfo.rockerStyle.strokeWidth * 10.0f));
        this.rockerCornerRadiusPressSeekbar.setProgress(this.baseRockerViewInfo.rockerStyle.cornerRadiusPress);
        this.rockerStrokeWidthPressSeekbar.setProgress((int)(this.baseRockerViewInfo.rockerStyle.strokeWidthPress * 10.0f));
        this.rockerCornerRadiusText.setText((CharSequence)(this.baseRockerViewInfo.rockerStyle.cornerRadius + " dp"));
        this.rockerStrokeWidthText.setText((CharSequence)(this.baseRockerViewInfo.rockerStyle.strokeWidth + " dp"));
        this.rockerCornerRadiusPressedText.setText((CharSequence)(this.baseRockerViewInfo.rockerStyle.cornerRadiusPress + " dp"));
        this.rockerStrokeWidthPressedText.setText((CharSequence)(this.baseRockerViewInfo.rockerStyle.strokeWidthPress + " dp"));
        this.pointerColorPre.setBackgroundColor(Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.pointerColor));
        this.rockerStrokeColorPre.setBackgroundColor(Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.strokeColor));
        this.rockerFillColorPre.setBackgroundColor(Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.fillColor));
        this.pointerColorPressedPre.setBackgroundColor(Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.pointerColorPress));
        this.rockerStrokeColorPressedPre.setBackgroundColor(Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.strokeColorPress));
        this.rockerFillColorPressedPre.setBackgroundColor(Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.fillColorPress));
        this.pointerColorText.setText((CharSequence)this.baseRockerViewInfo.rockerStyle.pointerColor);
        this.rockerStrokeColorText.setText((CharSequence)this.baseRockerViewInfo.rockerStyle.strokeColor);
        this.rockerFillColorText.setText((CharSequence)this.baseRockerViewInfo.rockerStyle.fillColor);
        this.pointerColorPressedText.setText((CharSequence)this.baseRockerViewInfo.rockerStyle.pointerColorPress);
        this.rockerStrokeColorPressedText.setText((CharSequence)this.baseRockerViewInfo.rockerStyle.strokeColorPress);
        this.rockerFillColorPressedText.setText((CharSequence)this.baseRockerViewInfo.rockerStyle.fillColorPress);
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onClick(View view) {
        ColorSelectorDialog colorSelectorDialog;
        Dialog dialog;
        if (view == this.showButton) {
            this.viewType = 0;
            this.showButton.setBackgroundColor(this.getContext().getColor(R.color.colorPureWhite));
            this.showRocker.setBackgroundColor(this.getContext().getColor(R.color.colorLightGray));
            this.editButton.setVisibility(0);
            this.editRocker.setVisibility(8);
        }
        if (view == this.showRocker) {
            this.viewType = 1;
            this.showButton.setBackgroundColor(this.getContext().getColor(R.color.colorLightGray));
            this.showRocker.setBackgroundColor(this.getContext().getColor(R.color.colorPureWhite));
            this.editButton.setVisibility(8);
            this.editRocker.setVisibility(0);
        }
        if (view == this.positive) {
            if (this.viewType == 0) {
                this.onViewCreateListener.onButtonCreate(this.baseButtonInfo);
            }
            if (this.viewType == 1) {
                this.onViewCreateListener.onRockerCreate(this.baseRockerViewInfo);
            }
            this.dismiss();
        }
        if (view == this.negative) {
            this.dismiss();
        }
        if (view == this.childVisibility) {
            dialog = new ChildVisibilityDialog(this.getContext(), this.pattern, this.baseButtonInfo.visibilityControl, list -> {
                this.baseButtonInfo.visibilityControl = list;
            });
            dialog.show();
        }
        if (view == this.outputKeycode) {
            dialog = new SelectKeycodeDialog(this.getContext(), this.baseButtonInfo.outputKeycode, list -> {
                this.baseButtonInfo.outputKeycode = list;
            });
            dialog.show();
        }
        if (view == this.createButtonStyle) {
            dialog = new ButtonStyleManagerDialog(this.getContext(), () -> this.refreshButtonStyleList(false));
            dialog.show();
        }
        if (view == this.selectTextColor) {
            colorSelectorDialog = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseButtonInfo.buttonStyle.textColor));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.textColorPre.setBackgroundColor(destColor);
                    AddViewDialog.this.textColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseButtonInfo.buttonStyle.textColor = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectStrokeColor) {
            colorSelectorDialog = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseButtonInfo.buttonStyle.strokeColor));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.strokeColorPre.setBackgroundColor(destColor);
                    AddViewDialog.this.strokeColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseButtonInfo.buttonStyle.strokeColor = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectFillColor) {
            colorSelectorDialog = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseButtonInfo.buttonStyle.fillColor));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.fillColorPre.setBackgroundColor(destColor);
                    AddViewDialog.this.fillColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseButtonInfo.buttonStyle.fillColor = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectTextColorPressed) {
            colorSelectorDialog = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseButtonInfo.buttonStyle.textColorPress));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.textColorPressedPre.setBackgroundColor(destColor);
                    AddViewDialog.this.textColorPressedText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseButtonInfo.buttonStyle.textColorPress = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectStrokeColorPressed) {
            colorSelectorDialog = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseButtonInfo.buttonStyle.strokeColorPress));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.strokeColorPressedPre.setBackgroundColor(destColor);
                    AddViewDialog.this.strokeColorPressedText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseButtonInfo.buttonStyle.strokeColorPress = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectFillColorPressed) {
            colorSelectorDialog = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseButtonInfo.buttonStyle.fillColorPress));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.fillColorPressedPre.setBackgroundColor(destColor);
                    AddViewDialog.this.fillColorPressedText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseButtonInfo.buttonStyle.fillColorPress = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.addButtonWidth) {
            this.buttonWidthSeekbar.setProgress(this.buttonWidthSeekbar.getProgress() + 1);
            int i = this.buttonWidthSeekbar.getProgress();
            if (this.baseButtonInfo.sizeType == 0) {
                this.baseButtonInfo.width.percentSize = (float)i / 1000.0f;
                this.buttonWidthText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.width.percentSize)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.width.absoluteSize = i;
                this.buttonWidthText.setText((CharSequence)(i + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (view == this.addButtonHeight) {
            this.buttonHeightSeekbar.setProgress(this.buttonHeightSeekbar.getProgress() + 1);
            int i = this.buttonHeightSeekbar.getProgress();
            if (this.baseButtonInfo.sizeType == 0) {
                this.baseButtonInfo.height.percentSize = (float)i / 1000.0f;
                this.buttonHeightText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.height.percentSize)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.height.absoluteSize = i;
                this.buttonHeightText.setText((CharSequence)(i + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (view == this.addButtonX) {
            this.buttonXSeekbar.setProgress(this.buttonXSeekbar.getProgress() + 1);
            int i = this.buttonXSeekbar.getProgress();
            if (this.baseButtonInfo.positionType == 0) {
                this.baseButtonInfo.xPosition.percentPosition = (float)i / 1000.0f;
                this.buttonXText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.xPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.xPosition.absolutePosition = i;
                this.buttonXText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.addButtonY) {
            this.buttonYSeekbar.setProgress(this.buttonYSeekbar.getProgress() + 1);
            int i = this.buttonYSeekbar.getProgress();
            if (this.baseButtonInfo.positionType == 0) {
                this.baseButtonInfo.yPosition.percentPosition = (float)i / 1000.0f;
                this.buttonYText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.yPosition.absolutePosition = i;
                this.buttonYText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.addButtonTextSize) {
            int i = this.textSizeSeekbar.getProgress() + 1;
            this.textSizeSeekbar.setProgress(i);
        }
        if (view == this.addButtonStrokeWidth) {
            int i = this.strokeWidthSeekbar.getProgress() + 1;
            this.strokeWidthSeekbar.setProgress(i);
        }
        if (view == this.addButtonCornerRadius) {
            int i = this.cornerRadiusSeekbar.getProgress() + 1;
            this.cornerRadiusSeekbar.setProgress(i);
        }
        if (view == this.addButtonTextSizePress) {
            int i = this.textSizePressedSeekbar.getProgress() + 1;
            this.textSizePressedSeekbar.setProgress(i);
        }
        if (view == this.addButtonStrokeWidthPress) {
            int i = this.strokeWidthPressedSeekbar.getProgress() + 1;
            this.strokeWidthPressedSeekbar.setProgress(i);
        }
        if (view == this.addButtonCornerRadiusPress) {
            int i = this.cornerRadiusPressedSeekbar.getProgress() + 1;
            this.cornerRadiusPressedSeekbar.setProgress(i);
        }
        if (view == this.reduceButtonWidth) {
            this.buttonWidthSeekbar.setProgress(this.buttonWidthSeekbar.getProgress() - 1);
            int i = this.buttonWidthSeekbar.getProgress();
            if (this.baseButtonInfo.sizeType == 0) {
                this.baseButtonInfo.width.percentSize = (float)i / 1000.0f;
                this.buttonWidthText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.width.percentSize)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.width.absoluteSize = i;
                this.buttonWidthText.setText((CharSequence)(i + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (view == this.reduceButtonHeight) {
            this.buttonHeightSeekbar.setProgress(this.buttonHeightSeekbar.getProgress() - 1);
            int i = this.buttonHeightSeekbar.getProgress();
            if (this.baseButtonInfo.sizeType == 0) {
                this.baseButtonInfo.height.percentSize = (float)i / 1000.0f;
                this.buttonHeightText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.height.percentSize)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.height.absoluteSize = i;
                this.buttonHeightText.setText((CharSequence)(i + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (view == this.reduceButtonX) {
            this.buttonXSeekbar.setProgress(this.buttonXSeekbar.getProgress() - 1);
            int i = this.buttonXSeekbar.getProgress();
            if (this.baseButtonInfo.positionType == 0) {
                this.baseButtonInfo.xPosition.percentPosition = (float)i / 1000.0f;
                this.buttonXText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.xPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.xPosition.absolutePosition = i;
                this.buttonXText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.reduceButtonY) {
            this.buttonYSeekbar.setProgress(this.buttonYSeekbar.getProgress() - 1);
            int i = this.buttonYSeekbar.getProgress();
            if (this.baseButtonInfo.positionType == 0) {
                this.baseButtonInfo.yPosition.percentPosition = (float)i / 1000.0f;
                this.buttonYText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.yPosition.absolutePosition = i;
                this.buttonYText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.reduceButtonTextSize) {
            int i = this.textSizeSeekbar.getProgress() - 1;
            this.textSizeSeekbar.setProgress(i);
        }
        if (view == this.reduceButtonStrokeWidth) {
            int i = this.strokeWidthSeekbar.getProgress() - 1;
            this.strokeWidthSeekbar.setProgress(i);
        }
        if (view == this.reduceButtonCornerRadius) {
            int i = this.cornerRadiusSeekbar.getProgress() - 1;
            this.cornerRadiusSeekbar.setProgress(i);
        }
        if (view == this.reduceButtonTextSizePress) {
            int i = this.textSizePressedSeekbar.getProgress() - 1;
            this.textSizePressedSeekbar.setProgress(i);
        }
        if (view == this.reduceButtonStrokeWidthPress) {
            int i = this.strokeWidthPressedSeekbar.getProgress() - 1;
            this.strokeWidthPressedSeekbar.setProgress(i);
        }
        if (view == this.reduceButtonCornerRadiusPress) {
            int i = this.cornerRadiusPressedSeekbar.getProgress() - 1;
            this.cornerRadiusPressedSeekbar.setProgress(i);
        }
        if (view == this.createRockerStyle) {
            RockerStyleManagerDialog dialog2 = new RockerStyleManagerDialog(this.getContext(), () -> this.refreshRockerStyleList(false));
            dialog2.show();
        }
        if (view == this.selectPointerColor) {
            ColorSelectorDialog colorSelectorDialog2 = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.pointerColor));
            colorSelectorDialog2.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.pointerColorPre.setBackgroundColor(destColor);
                    AddViewDialog.this.pointerColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseRockerViewInfo.rockerStyle.pointerColor = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog2.show();
        }
        if (view == this.selectRockerStrokeColor) {
            ColorSelectorDialog colorSelectorDialog3 = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.strokeColor));
            colorSelectorDialog3.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.rockerStrokeColorPre.setBackgroundColor(destColor);
                    AddViewDialog.this.rockerStrokeColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseRockerViewInfo.rockerStyle.strokeColor = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog3.show();
        }
        if (view == this.selectRockerFillColor) {
            ColorSelectorDialog colorSelectorDialog4 = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.fillColor));
            colorSelectorDialog4.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.rockerFillColorPre.setBackgroundColor(destColor);
                    AddViewDialog.this.rockerFillColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseRockerViewInfo.rockerStyle.fillColor = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog4.show();
        }
        if (view == this.selectPointerColorPressed) {
            ColorSelectorDialog colorSelectorDialog5 = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.pointerColorPress));
            colorSelectorDialog5.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.pointerColorPressedPre.setBackgroundColor(destColor);
                    AddViewDialog.this.pointerColorPressedText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseRockerViewInfo.rockerStyle.pointerColorPress = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog5.show();
        }
        if (view == this.selectRockerStrokeColorPressed) {
            ColorSelectorDialog colorSelectorDialog6 = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.strokeColorPress));
            colorSelectorDialog6.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.rockerStrokeColorPressedPre.setBackgroundColor(destColor);
                    AddViewDialog.this.rockerStrokeColorPressedText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseRockerViewInfo.rockerStyle.strokeColorPress = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog6.show();
        }
        if (view == this.selectRockerFillColorPressed) {
            ColorSelectorDialog colorSelectorDialog7 = new ColorSelectorDialog(this.getContext(), false, Color.parseColor((String)this.baseRockerViewInfo.rockerStyle.fillColorPress));
            colorSelectorDialog7.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                public void onColorSelected(int color2) {
                }

                @Override
                public void onPositive(int destColor) {
                    AddViewDialog.this.rockerFillColorPressedPre.setBackgroundColor(destColor);
                    AddViewDialog.this.rockerFillColorPressedText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ((AddViewDialog)AddViewDialog.this).baseRockerViewInfo.rockerStyle.fillColorPress = "#" + Integer.toHexString(destColor);
                }

                @Override
                public void onNegative(int initColor) {
                }
            });
            colorSelectorDialog7.show();
        }
        if (view == this.addRockerSize) {
            this.rockerSizeSeekbar.setProgress(this.rockerSizeSeekbar.getProgress() + 1);
            int i = this.rockerSizeSeekbar.getProgress();
            if (this.baseRockerViewInfo.sizeType == 0) {
                this.baseRockerViewInfo.size.percentSize = (float)i / 1000.0f;
                this.rockerSizeText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.size.percentSize)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.size.absoluteSize = i;
                this.rockerSizeText.setText((CharSequence)(i + " dp"));
            }
            this.onRockerSizeChange();
        }
        if (view == this.addRockerX) {
            this.rockerXSeekbar.setProgress(this.rockerXSeekbar.getProgress() + 1);
            int i = this.rockerXSeekbar.getProgress();
            if (this.baseRockerViewInfo.positionType == 0) {
                this.baseRockerViewInfo.xPosition.percentPosition = (float)i / 1000.0f;
                this.rockerXText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.xPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.xPosition.absolutePosition = i;
                this.rockerXText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.addRockerY) {
            this.rockerYSeekbar.setProgress(this.rockerYSeekbar.getProgress() + 1);
            int i = this.rockerYSeekbar.getProgress();
            if (this.baseRockerViewInfo.positionType == 0) {
                this.baseRockerViewInfo.yPosition.percentPosition = (float)i / 1000.0f;
                this.rockerYText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.yPosition.absolutePosition = i;
                this.rockerYText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.addRockerStrokeWidth) {
            int i = this.rockerStrokeWidthSeekbar.getProgress() + 1;
            this.rockerStrokeWidthSeekbar.setProgress(i);
        }
        if (view == this.addRockerCornerRadius) {
            int i = this.rockerCornerRadiusSeekbar.getProgress() + 1;
            this.rockerCornerRadiusSeekbar.setProgress(i);
        }
        if (view == this.addRockerStrokeWidthPress) {
            int i = this.rockerStrokeWidthPressSeekbar.getProgress() + 1;
            this.rockerStrokeWidthPressSeekbar.setProgress(i);
        }
        if (view == this.addRockerCornerRadiusPress) {
            int i = this.rockerCornerRadiusPressSeekbar.getProgress() + 1;
            this.rockerCornerRadiusPressSeekbar.setProgress(i);
        }
        if (view == this.reduceRockerSize) {
            this.rockerSizeSeekbar.setProgress(this.rockerSizeSeekbar.getProgress() - 1);
            int i = this.rockerSizeSeekbar.getProgress();
            if (this.baseRockerViewInfo.sizeType == 0) {
                this.baseRockerViewInfo.size.percentSize = (float)i / 1000.0f;
                this.rockerSizeText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.size.percentSize)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.size.absoluteSize = i;
                this.rockerSizeText.setText((CharSequence)(i + " dp"));
            }
            this.onRockerSizeChange();
        }
        if (view == this.reduceRockerX) {
            this.rockerXSeekbar.setProgress(this.rockerXSeekbar.getProgress() - 1);
            int i = this.rockerXSeekbar.getProgress();
            if (this.baseRockerViewInfo.positionType == 0) {
                this.baseRockerViewInfo.xPosition.percentPosition = (float)i / 1000.0f;
                this.rockerXText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.xPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.xPosition.absolutePosition = i;
                this.rockerXText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.reduceRockerY) {
            this.rockerYSeekbar.setProgress(this.rockerYSeekbar.getProgress() - 1);
            int i = this.rockerYSeekbar.getProgress();
            if (this.baseRockerViewInfo.positionType == 0) {
                this.baseRockerViewInfo.yPosition.percentPosition = (float)i / 1000.0f;
                this.rockerYText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.yPosition.absolutePosition = i;
                this.rockerYText.setText((CharSequence)(i + " dp"));
            }
        }
        if (view == this.reduceRockerStrokeWidth) {
            int i = this.rockerStrokeWidthSeekbar.getProgress() - 1;
            this.rockerStrokeWidthSeekbar.setProgress(i);
        }
        if (view == this.reduceRockerCornerRadius) {
            int i = this.rockerCornerRadiusSeekbar.getProgress() - 1;
            this.rockerCornerRadiusSeekbar.setProgress(i);
        }
        if (view == this.reduceRockerStrokeWidthPress) {
            int i = this.rockerStrokeWidthPressSeekbar.getProgress() - 1;
            this.rockerStrokeWidthPressSeekbar.setProgress(i);
        }
        if (view == this.reduceRockerCornerRadiusPress) {
            int i = this.rockerCornerRadiusPressSeekbar.getProgress() - 1;
            this.rockerCornerRadiusPressSeekbar.setProgress(i);
        }
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == this.buttonShowTypeSpinner) {
            this.baseButtonInfo.showType = i;
        }
        if (adapterView == this.buttonSizeTypeSpinner) {
            this.baseButtonInfo.sizeType = i;
            if (i == 0) {
                this.widthObjectLayout.setVisibility(0);
                this.heightObjectLayout.setVisibility(0);
                this.buttonWidthSeekbar.setMax(1000);
                this.buttonHeightSeekbar.setMax(1000);
                this.buttonWidthSeekbar.setProgress((int)(this.baseButtonInfo.width.percentSize * 1000.0f));
                this.buttonHeightSeekbar.setProgress((int)(this.baseButtonInfo.height.percentSize * 1000.0f));
                this.buttonWidthText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.width.percentSize)) / 10.0f + " %"));
                this.buttonHeightText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.height.percentSize)) / 10.0f + " %"));
            } else {
                this.widthObjectLayout.setVisibility(8);
                this.heightObjectLayout.setVisibility(8);
                this.buttonWidthSeekbar.setMax(200);
                this.buttonHeightSeekbar.setMax(200);
                this.buttonWidthSeekbar.setProgress(this.baseButtonInfo.width.absoluteSize);
                this.buttonHeightSeekbar.setProgress(this.baseButtonInfo.height.absoluteSize);
                this.buttonWidthText.setText((CharSequence)(this.baseButtonInfo.width.absoluteSize + " dp"));
                this.buttonHeightText.setText((CharSequence)(this.baseButtonInfo.height.absoluteSize + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (adapterView == this.buttonPositionTypeSpinner) {
            this.baseButtonInfo.positionType = i;
            if (i == 0) {
                this.buttonXSeekbar.setMax(1000);
                this.buttonYSeekbar.setMax(1000);
                this.buttonXSeekbar.setProgress((int)(this.baseButtonInfo.xPosition.percentPosition * 1000.0f));
                this.buttonYSeekbar.setProgress((int)(this.baseButtonInfo.yPosition.percentPosition * 1000.0f));
                this.buttonXText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.xPosition.percentPosition)) / 10.0f + " %"));
                this.buttonYText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.buttonXSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenWidth));
                this.buttonYSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenHeight));
                this.buttonXSeekbar.setProgress(this.baseButtonInfo.xPosition.absolutePosition);
                this.buttonYSeekbar.setProgress(this.baseButtonInfo.yPosition.absolutePosition);
                this.buttonXText.setText((CharSequence)(this.baseButtonInfo.xPosition.absolutePosition + " dp"));
                this.buttonYText.setText((CharSequence)(this.baseButtonInfo.yPosition.absolutePosition + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (adapterView == this.widthObjectSpinner) {
            this.baseButtonInfo.width.object = i;
            this.onButtonSizeChange();
        }
        if (adapterView == this.heightObjectSpinner) {
            this.baseButtonInfo.height.object = i;
            this.onButtonSizeChange();
        }
        if (adapterView == this.functionTypeSpinner) {
            this.baseButtonInfo.functionType = i;
        }
        if (adapterView == this.selectExist) {
            for (ButtonStyle buttonStyle : SettingUtils.getButtonStyleList()) {
                if (!buttonStyle.name.equals(this.selectExist.getItemAtPosition(i))) continue;
                this.selectedButtonStyle = buttonStyle;
            }
            if (this.baseButtonInfo.usingExist) {
                this.baseButtonInfo.buttonStyle = this.selectedButtonStyle;
            }
            this.refreshButtonStyleEditor();
        }
        if (adapterView == this.rockerShowTypeSpinner) {
            this.baseRockerViewInfo.showType = i;
        }
        if (adapterView == this.rockerSizeTypeSpinner) {
            this.baseRockerViewInfo.sizeType = i;
            if (i == 0) {
                this.sizeObjectLayout.setVisibility(0);
                this.rockerSizeSeekbar.setMax(1000);
                this.rockerSizeSeekbar.setProgress((int)(this.baseRockerViewInfo.size.percentSize * 1000.0f));
                this.rockerSizeText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.size.percentSize)) / 10.0f + " %"));
            } else {
                this.sizeObjectLayout.setVisibility(8);
                this.rockerSizeSeekbar.setMax(300);
                this.rockerSizeSeekbar.setProgress(this.baseRockerViewInfo.size.absoluteSize);
                this.rockerSizeText.setText((CharSequence)(this.baseRockerViewInfo.size.absoluteSize + " dp"));
            }
            this.onRockerSizeChange();
        }
        if (adapterView == this.rockerPositionTypeSpinner) {
            this.baseRockerViewInfo.positionType = i;
            if (i == 0) {
                this.rockerXSeekbar.setMax(1000);
                this.rockerYSeekbar.setMax(1000);
                this.rockerXSeekbar.setProgress((int)(this.baseRockerViewInfo.xPosition.percentPosition * 1000.0f));
                this.rockerYSeekbar.setProgress((int)(this.baseRockerViewInfo.yPosition.percentPosition * 1000.0f));
                this.rockerXText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.xPosition.percentPosition)) / 10.0f + " %"));
                this.rockerYText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.rockerXSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenWidth));
                this.rockerYSeekbar.setMax(ConvertUtils.px2dip(this.getContext(), this.screenHeight));
                this.rockerXSeekbar.setProgress(this.baseRockerViewInfo.xPosition.absolutePosition);
                this.rockerYSeekbar.setProgress(this.baseRockerViewInfo.yPosition.absolutePosition);
                this.rockerXText.setText((CharSequence)(this.baseRockerViewInfo.xPosition.absolutePosition + " dp"));
                this.rockerYText.setText((CharSequence)(this.baseRockerViewInfo.yPosition.absolutePosition + " dp"));
            }
            this.onRockerSizeChange();
        }
        if (adapterView == this.sizeObjectSpinner) {
            this.baseRockerViewInfo.size.object = i;
            this.onRockerSizeChange();
        }
        if (adapterView == this.followTypeSpinner) {
            this.baseRockerViewInfo.followType = i;
        }
        if (adapterView == this.selectExistRockerStyle) {
            for (RockerStyle rockerStyle : SettingUtils.getRockerStyleList()) {
                if (!rockerStyle.name.equals(this.selectExistRockerStyle.getItemAtPosition(i))) continue;
                this.selectedRockerStyle = rockerStyle;
            }
            if (this.baseRockerViewInfo.usingExist) {
                this.baseRockerViewInfo.rockerStyle = this.selectedRockerStyle;
            }
            this.refreshRockerStyleEditor();
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @SuppressLint(value={"SetTextI18n"})
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (seekBar == this.buttonWidthSeekbar && b) {
            if (this.baseButtonInfo.sizeType == 0) {
                this.baseButtonInfo.width.percentSize = (float)i / 1000.0f;
                this.buttonWidthText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.width.percentSize)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.width.absoluteSize = i;
                this.buttonWidthText.setText((CharSequence)(i + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (seekBar == this.buttonHeightSeekbar && b) {
            if (this.baseButtonInfo.sizeType == 0) {
                this.baseButtonInfo.height.percentSize = (float)i / 1000.0f;
                this.buttonHeightText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.height.percentSize)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.height.absoluteSize = i;
                this.buttonHeightText.setText((CharSequence)(i + " dp"));
            }
            this.onButtonSizeChange();
        }
        if (seekBar == this.buttonXSeekbar && b) {
            if (this.baseButtonInfo.positionType == 0) {
                this.baseButtonInfo.xPosition.percentPosition = (float)i / 1000.0f;
                this.buttonXText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.xPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.xPosition.absolutePosition = i;
                this.buttonXText.setText((CharSequence)(i + " dp"));
            }
        }
        if (seekBar == this.buttonYSeekbar && b) {
            if (this.baseButtonInfo.positionType == 0) {
                this.baseButtonInfo.yPosition.percentPosition = (float)i / 1000.0f;
                this.buttonYText.setText((CharSequence)((float)((int)(1000.0f * this.baseButtonInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseButtonInfo.yPosition.absolutePosition = i;
                this.buttonYText.setText((CharSequence)(i + " dp"));
            }
        }
        if (seekBar == this.textSizeSeekbar) {
            this.textSizeText.setText((CharSequence)(i + " sp"));
            this.baseButtonInfo.buttonStyle.textSize = i;
        }
        if (seekBar == this.cornerRadiusSeekbar) {
            this.cornerRadiusText.setText((CharSequence)(i + " dp"));
            this.baseButtonInfo.buttonStyle.cornerRadius = i;
        }
        if (seekBar == this.strokeWidthSeekbar) {
            this.strokeWidthText.setText((CharSequence)((float)i / 10.0f + " dp"));
            this.baseButtonInfo.buttonStyle.strokeWidth = (float)i / 10.0f;
        }
        if (seekBar == this.textSizePressedSeekbar) {
            this.textSizePressedText.setText((CharSequence)(i + " sp"));
            this.baseButtonInfo.buttonStyle.textSizePress = i;
        }
        if (seekBar == this.cornerRadiusPressedSeekbar) {
            this.cornerRadiusPressedText.setText((CharSequence)(i + " dp"));
            this.baseButtonInfo.buttonStyle.cornerRadiusPress = i;
        }
        if (seekBar == this.strokeWidthPressedSeekbar) {
            this.strokeWidthPressedText.setText((CharSequence)((float)i / 10.0f + " dp"));
            this.baseButtonInfo.buttonStyle.strokeWidthPress = (float)i / 10.0f;
        }
        if (seekBar == this.rockerSizeSeekbar && b) {
            if (this.baseRockerViewInfo.sizeType == 0) {
                this.baseRockerViewInfo.size.percentSize = (float)i / 1000.0f;
                this.rockerSizeText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.size.percentSize)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.size.absoluteSize = i;
                this.rockerSizeText.setText((CharSequence)(i + " dp"));
            }
            this.onRockerSizeChange();
        }
        if (seekBar == this.rockerXSeekbar && b) {
            if (this.baseRockerViewInfo.positionType == 0) {
                this.baseRockerViewInfo.xPosition.percentPosition = (float)i / 1000.0f;
                this.rockerXText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.xPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.xPosition.absolutePosition = i;
                this.rockerXText.setText((CharSequence)(i + " dp"));
            }
        }
        if (seekBar == this.rockerYSeekbar && b) {
            if (this.baseRockerViewInfo.positionType == 0) {
                this.baseRockerViewInfo.yPosition.percentPosition = (float)i / 1000.0f;
                this.rockerYText.setText((CharSequence)((float)((int)(1000.0f * this.baseRockerViewInfo.yPosition.percentPosition)) / 10.0f + " %"));
            } else {
                this.baseRockerViewInfo.yPosition.absolutePosition = i;
                this.rockerYText.setText((CharSequence)(i + " dp"));
            }
        }
        if (seekBar == this.rockerCornerRadiusSeekbar) {
            this.rockerCornerRadiusText.setText((CharSequence)(i + " dp"));
            this.baseRockerViewInfo.rockerStyle.cornerRadius = i;
        }
        if (seekBar == this.rockerStrokeWidthSeekbar) {
            this.rockerStrokeWidthText.setText((CharSequence)((float)i / 10.0f + " dp"));
            this.baseRockerViewInfo.rockerStyle.strokeWidth = (float)i / 10.0f;
        }
        if (seekBar == this.rockerCornerRadiusPressSeekbar) {
            this.rockerCornerRadiusPressedText.setText((CharSequence)(i + " dp"));
            this.baseRockerViewInfo.rockerStyle.cornerRadiusPress = i;
        }
        if (seekBar == this.rockerStrokeWidthPressSeekbar) {
            this.rockerStrokeWidthPressedText.setText((CharSequence)((float)i / 10.0f + " dp"));
            this.baseRockerViewInfo.rockerStyle.strokeWidthPress = (float)i / 10.0f;
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == this.checkViewMove) {
            this.baseButtonInfo.viewMove = b;
        }
        if (compoundButton == this.checkAutoKeep) {
            this.baseButtonInfo.autoKeep = b;
        }
        if (compoundButton == this.checkAutoClick) {
            this.baseButtonInfo.autoClick = b;
        }
        if (compoundButton == this.checkOpenMenu) {
            this.baseButtonInfo.openMenu = b;
        }
        if (compoundButton == this.checkMovable) {
            this.baseButtonInfo.movable = b;
        }
        if (compoundButton == this.checkTouchMode) {
            this.baseButtonInfo.switchTouchMode = b;
        }
        if (compoundButton == this.checkSensor) {
            this.baseButtonInfo.switchSensor = b;
        }
        if (compoundButton == this.checkLeftPad) {
            this.baseButtonInfo.switchLeftPad = b;
        }
        if (compoundButton == this.checkOpenInput) {
            this.baseButtonInfo.showInputDialog = b;
        }
        if (compoundButton == this.checkUsingExist) {
            this.baseButtonInfo.usingExist = b;
            if (b) {
                this.buttonStyleLayout.setVisibility(8);
                this.baseButtonInfo.buttonStyle = this.selectedButtonStyle;
            } else {
                this.buttonStyleLayout.setVisibility(0);
                this.baseButtonInfo.buttonStyle.name = "";
            }
            this.refreshButtonStyleEditor();
        }
        if (compoundButton == this.checkShift) {
            this.baseRockerViewInfo.shift = b;
        }
        if (compoundButton == this.checkUsingExistRockerStyle) {
            this.baseRockerViewInfo.usingExist = b;
            if (b) {
                this.rockerStyleLayout.setVisibility(8);
                this.baseRockerViewInfo.rockerStyle = this.selectedRockerStyle;
            } else {
                this.rockerStyleLayout.setVisibility(0);
                this.baseRockerViewInfo.rockerStyle.name = "";
            }
            this.refreshRockerStyleEditor();
        }
    }

    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    public void afterTextChanged(Editable editable) {
        this.baseButtonInfo.text = this.editButtonText.getText().toString();
        this.baseButtonInfo.outputText = this.editOutputText.getText().toString();
    }

    public static interface OnViewCreateListener {
        public void onButtonCreate(BaseButtonInfo var1);

        public void onRockerCreate(BaseRockerViewInfo var1);
    }
}

