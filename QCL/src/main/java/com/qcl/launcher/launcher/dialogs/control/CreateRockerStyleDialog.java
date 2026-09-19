package com.qcl.launcher.launcher.dialogs.control;

import com.qcl.launcher.utils.QclColors;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.control.bean.rocker.RockerStyle;
import com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class CreateRockerStyleDialog extends Dialog implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, TextWatcher {
    private EditText editName;
    private ArrayList<RockerStyle> list;
    private Button negative;
    private OnRockerStyleCreateListener onRockerStyleCreateListener;
    private View pointerColorPre;
    private View pointerColorPressedPre;
    private TextView pointerColorPressedText;
    private TextView pointerColorText;
    private Button positive;
    private SeekBar rockerCornerRadiusPressSeekbar;
    private TextView rockerCornerRadiusPressedText;
    private SeekBar rockerCornerRadiusSeekbar;
    private TextView rockerCornerRadiusText;
    private View rockerFillColorPre;
    private View rockerFillColorPressedPre;
    private TextView rockerFillColorPressedText;
    private TextView rockerFillColorText;
    private View rockerStrokeColorPre;
    private View rockerStrokeColorPressedPre;
    private TextView rockerStrokeColorPressedText;
    private TextView rockerStrokeColorText;
    private SeekBar rockerStrokeWidthPressSeekbar;
    private TextView rockerStrokeWidthPressedText;
    private SeekBar rockerStrokeWidthSeekbar;
    private TextView rockerStrokeWidthText;
    private RockerStyle rockerStyle;
    private Button selectPointerColor;
    private Button selectPointerColorPressed;
    private Button selectRockerFillColor;
    private Button selectRockerFillColorPressed;
    private Button selectRockerStrokeColor;
    private Button selectRockerStrokeColorPressed;

    /* loaded from: classes2.dex */
    public interface OnRockerStyleCreateListener {
        void onRockerStyleCreate(RockerStyle rockerStyle);
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public CreateRockerStyleDialog(Context context, ArrayList<RockerStyle> arrayList, OnRockerStyleCreateListener onRockerStyleCreateListener) {
        super(context);
        this.list = arrayList;
        this.onRockerStyleCreateListener = onRockerStyleCreateListener;
        setContentView(R.layout.dialog_create_rocker_style);
        setCancelable(false);
        init();
    }

    private void init() {
        this.rockerStyle = new RockerStyle();
        this.positive = (Button) findViewById(R.id.create_rocker_style);
        this.negative = (Button) findViewById(R.id.exit);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.editName = (EditText) findViewById(R.id.edit_rocker_style_name);
        this.rockerCornerRadiusSeekbar = (SeekBar) findViewById(R.id.rocker_exterior_corner_radius_seekbar);
        this.rockerCornerRadiusPressSeekbar = (SeekBar) findViewById(R.id.rocker_exterior_corner_radius_seekbar_pressed);
        this.rockerStrokeWidthSeekbar = (SeekBar) findViewById(R.id.rocker_exterior_stroke_width_seekbar);
        this.rockerStrokeWidthPressSeekbar = (SeekBar) findViewById(R.id.rocker_exterior_stroke_width_seekbar_pressed);
        this.rockerCornerRadiusText = (TextView) findViewById(R.id.rocker_corner_radius_text);
        this.rockerCornerRadiusPressedText = (TextView) findViewById(R.id.rocker_corner_radius_press_text);
        this.rockerStrokeWidthText = (TextView) findViewById(R.id.rocker_stroke_width_text);
        this.rockerStrokeWidthPressedText = (TextView) findViewById(R.id.rocker_stroke_width_press_text);
        this.selectPointerColor = (Button) findViewById(R.id.exterior_pointer_color);
        this.selectRockerStrokeColor = (Button) findViewById(R.id.rocker_exterior_stroke_color);
        this.selectRockerFillColor = (Button) findViewById(R.id.rocker_exterior_fill_color);
        this.selectPointerColorPressed = (Button) findViewById(R.id.exterior_pointer_color_pressed);
        this.selectRockerStrokeColorPressed = (Button) findViewById(R.id.rocker_exterior_stroke_color_pressed);
        this.selectRockerFillColorPressed = (Button) findViewById(R.id.rocker_exterior_fill_color_pressed);
        this.pointerColorPre = findViewById(R.id.pointer_color_preview);
        this.rockerStrokeColorPre = findViewById(R.id.rocker_stroke_color_preview);
        this.rockerFillColorPre = findViewById(R.id.rocker_fill_color_preview);
        this.pointerColorPressedPre = findViewById(R.id.pointer_pressed_color_preview);
        this.rockerStrokeColorPressedPre = findViewById(R.id.rocker_stroke_pressed_color_preview);
        this.rockerFillColorPressedPre = findViewById(R.id.rocker_fill_pressed_color_preview);
        this.pointerColorText = (TextView) findViewById(R.id.pointer_color_text);
        this.rockerStrokeColorText = (TextView) findViewById(R.id.rocker_stroke_color_text);
        this.rockerFillColorText = (TextView) findViewById(R.id.rocker_fill_color_text);
        this.pointerColorPressedText = (TextView) findViewById(R.id.pointer_pressed_color_text);
        this.rockerStrokeColorPressedText = (TextView) findViewById(R.id.rocker_stroke_pressed_color_text);
        this.rockerFillColorPressedText = (TextView) findViewById(R.id.rocker_fill_pressed_color_text);
        this.rockerCornerRadiusSeekbar.setProgress(this.rockerStyle.cornerRadius);
        this.rockerStrokeWidthSeekbar.setProgress((int) (this.rockerStyle.strokeWidth * 10.0f));
        this.rockerCornerRadiusPressSeekbar.setProgress(this.rockerStyle.cornerRadiusPress);
        this.rockerStrokeWidthPressSeekbar.setProgress((int) (this.rockerStyle.strokeWidthPress * 10.0f));
        this.rockerCornerRadiusText.setText(this.rockerStyle.cornerRadius + " dp");
        this.rockerStrokeWidthText.setText(this.rockerStyle.strokeWidth + " dp");
        this.rockerCornerRadiusPressedText.setText(this.rockerStyle.cornerRadiusPress + " dp");
        this.rockerStrokeWidthPressedText.setText(this.rockerStyle.strokeWidthPress + " dp");
        this.pointerColorPre.setBackgroundColor(QclColors.parseSafe(this.rockerStyle.pointerColor, 0xFFFFFFFF));
        this.rockerStrokeColorPre.setBackgroundColor(QclColors.parseSafe(this.rockerStyle.strokeColor, 0x33555555));
        this.rockerFillColorPre.setBackgroundColor(QclColors.parseSafe(this.rockerStyle.fillColor, 0x666E6E6E));
        this.pointerColorPressedPre.setBackgroundColor(QclColors.parseSafe(this.rockerStyle.pointerColorPress, 0xFFFFFFFF));
        this.rockerStrokeColorPressedPre.setBackgroundColor(QclColors.parseSafe(this.rockerStyle.strokeColorPress, 0x55555555));
        this.rockerFillColorPressedPre.setBackgroundColor(QclColors.parseSafe(this.rockerStyle.fillColorPress, 0x995E5E5E));
        this.pointerColorText.setText(this.rockerStyle.pointerColor);
        this.rockerStrokeColorText.setText(this.rockerStyle.strokeColor);
        this.rockerFillColorText.setText(this.rockerStyle.fillColor);
        this.pointerColorPressedText.setText(this.rockerStyle.pointerColorPress);
        this.rockerStrokeColorPressedText.setText(this.rockerStyle.strokeColorPress);
        this.rockerFillColorPressedText.setText(this.rockerStyle.fillColorPress);
        this.editName.addTextChangedListener(this);
        this.rockerCornerRadiusSeekbar.setOnSeekBarChangeListener(this);
        this.rockerCornerRadiusPressSeekbar.setOnSeekBarChangeListener(this);
        this.rockerStrokeWidthSeekbar.setOnSeekBarChangeListener(this);
        this.rockerStrokeWidthPressSeekbar.setOnSeekBarChangeListener(this);
        this.selectPointerColor.setOnClickListener(this);
        this.selectPointerColorPressed.setOnClickListener(this);
        this.selectRockerFillColor.setOnClickListener(this);
        this.selectRockerFillColorPressed.setOnClickListener(this);
        this.selectRockerStrokeColor.setOnClickListener(this);
        this.selectRockerStrokeColorPressed.setOnClickListener(this);
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        this.rockerStyle.name = this.editName.getText().toString();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            ArrayList arrayList = new ArrayList();
            Iterator<RockerStyle> it = this.list.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().name);
            }
            if (this.editName.getText().toString().equals("")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_rocker_style_name_empty), 0).show();
            } else if (arrayList.contains(this.editName.getText().toString())) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_rocker_style_name_exist), 0).show();
            } else {
                this.onRockerStyleCreateListener.onRockerStyleCreate(this.rockerStyle);
                dismiss();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
        if (view == this.selectPointerColor) {
            ColorSelectorDialog colorSelectorDialog = new ColorSelectorDialog(getContext(), false, QclColors.parseSafe(this.rockerStyle.pointerColor, 0xFFFFFFFF));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.1
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateRockerStyleDialog.this.pointerColorPre.setBackgroundColor(i);
                    CreateRockerStyleDialog.this.pointerColorText.setText(QclColors.format(i));
                    CreateRockerStyleDialog.this.rockerStyle.pointerColor = QclColors.format(i);
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectRockerStrokeColor) {
            ColorSelectorDialog colorSelectorDialog2 = new ColorSelectorDialog(getContext(), false, QclColors.parseSafe(this.rockerStyle.strokeColor, 0x33555555));
            colorSelectorDialog2.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.2
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateRockerStyleDialog.this.rockerStrokeColorPre.setBackgroundColor(i);
                    CreateRockerStyleDialog.this.rockerStrokeColorText.setText(QclColors.format(i));
                    CreateRockerStyleDialog.this.rockerStyle.strokeColor = QclColors.format(i);
                }
            });
            colorSelectorDialog2.show();
        }
        if (view == this.selectRockerFillColor) {
            ColorSelectorDialog colorSelectorDialog3 = new ColorSelectorDialog(getContext(), false, QclColors.parseSafe(this.rockerStyle.fillColor, 0x666E6E6E));
            colorSelectorDialog3.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.3
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateRockerStyleDialog.this.rockerFillColorPre.setBackgroundColor(i);
                    CreateRockerStyleDialog.this.rockerFillColorText.setText(QclColors.format(i));
                    CreateRockerStyleDialog.this.rockerStyle.fillColor = QclColors.format(i);
                }
            });
            colorSelectorDialog3.show();
        }
        if (view == this.selectPointerColorPressed) {
            ColorSelectorDialog colorSelectorDialog4 = new ColorSelectorDialog(getContext(), false, QclColors.parseSafe(this.rockerStyle.pointerColorPress, 0xFFFFFFFF));
            colorSelectorDialog4.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.4
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateRockerStyleDialog.this.pointerColorPressedPre.setBackgroundColor(i);
                    CreateRockerStyleDialog.this.pointerColorPressedText.setText(QclColors.format(i));
                    CreateRockerStyleDialog.this.rockerStyle.pointerColorPress = QclColors.format(i);
                }
            });
            colorSelectorDialog4.show();
        }
        if (view == this.selectRockerStrokeColorPressed) {
            ColorSelectorDialog colorSelectorDialog5 = new ColorSelectorDialog(getContext(), false, QclColors.parseSafe(this.rockerStyle.strokeColorPress, 0x55555555));
            colorSelectorDialog5.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.5
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateRockerStyleDialog.this.rockerStrokeColorPressedPre.setBackgroundColor(i);
                    CreateRockerStyleDialog.this.rockerStrokeColorPressedText.setText(QclColors.format(i));
                    CreateRockerStyleDialog.this.rockerStyle.strokeColorPress = QclColors.format(i);
                }
            });
            colorSelectorDialog5.show();
        }
        if (view == this.selectRockerFillColorPressed) {
            ColorSelectorDialog colorSelectorDialog6 = new ColorSelectorDialog(getContext(), false, QclColors.parseSafe(this.rockerStyle.fillColorPress, 0x995E5E5E));
            colorSelectorDialog6.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.6
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateRockerStyleDialog.this.rockerFillColorPressedPre.setBackgroundColor(i);
                    CreateRockerStyleDialog.this.rockerFillColorPressedText.setText(QclColors.format(i));
                    CreateRockerStyleDialog.this.rockerStyle.fillColorPress = QclColors.format(i);
                }
            });
            colorSelectorDialog6.show();
        }
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (seekBar == this.rockerCornerRadiusSeekbar) {
            this.rockerCornerRadiusText.setText(i + " dp");
            this.rockerStyle.cornerRadius = i;
        }
        if (seekBar == this.rockerStrokeWidthSeekbar) {
            float f = i / 10.0f;
            this.rockerStrokeWidthText.setText(f + " dp");
            this.rockerStyle.strokeWidth = f;
        }
        if (seekBar == this.rockerCornerRadiusPressSeekbar) {
            this.rockerCornerRadiusPressedText.setText(i + " dp");
            this.rockerStyle.cornerRadiusPress = i;
        }
        if (seekBar == this.rockerStrokeWidthPressSeekbar) {
            float f2 = i / 10.0f;
            this.rockerStrokeWidthPressedText.setText(f2 + " dp");
            this.rockerStyle.strokeWidthPress = f2;
        }
    }
}
