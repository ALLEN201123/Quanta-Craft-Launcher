package com.qcl.launcher.launcher.dialogs.control;

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
import com.qcl.launcher.control.bean.button.ButtonStyle;
import com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class CreateButtonStyleDialog extends Dialog implements SeekBar.OnSeekBarChangeListener, View.OnClickListener, TextWatcher {
    private ButtonStyle buttonStyle;
    private SeekBar cornerRadiusPressedSeekbar;
    private TextView cornerRadiusPressedText;
    private SeekBar cornerRadiusSeekbar;
    private TextView cornerRadiusText;
    private EditText editName;
    private View fillColorPre;
    private View fillColorPressedPre;
    private TextView fillColorPressedText;
    private TextView fillColorText;
    private ArrayList<ButtonStyle> list;
    private Button negative;
    private OnButtonStyleCreateListener onButtonStyleCreateListener;
    private Button positive;
    private Button selectFillColor;
    private Button selectFillColorPressed;
    private Button selectStrokeColor;
    private Button selectStrokeColorPressed;
    private Button selectTextColor;
    private Button selectTextColorPressed;
    private View strokeColorPre;
    private View strokeColorPressedPre;
    private TextView strokeColorPressedText;
    private TextView strokeColorText;
    private SeekBar strokeWidthPressedSeekbar;
    private TextView strokeWidthPressedText;
    private SeekBar strokeWidthSeekbar;
    private TextView strokeWidthText;
    private View textColorPre;
    private View textColorPressedPre;
    private TextView textColorPressedText;
    private TextView textColorText;
    private SeekBar textSizePressedSeekbar;
    private TextView textSizePressedText;
    private SeekBar textSizeSeekbar;
    private TextView textSizeText;

    /* loaded from: classes2.dex */
    public interface OnButtonStyleCreateListener {
        void onButtonStyleCreate(ButtonStyle buttonStyle);
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

    public CreateButtonStyleDialog(Context context, ArrayList<ButtonStyle> arrayList, OnButtonStyleCreateListener onButtonStyleCreateListener) {
        super(context);
        this.list = arrayList;
        this.onButtonStyleCreateListener = onButtonStyleCreateListener;
        setContentView(R.layout.dialog_create_button_style);
        setCancelable(false);
        init();
    }

    private void init() {
        this.buttonStyle = new ButtonStyle();
        this.positive = (Button) findViewById(R.id.create_button_style);
        this.negative = (Button) findViewById(R.id.exit);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.editName = (EditText) findViewById(R.id.edit_button_style_name);
        this.textSizeSeekbar = (SeekBar) findViewById(R.id.exterior_text_size_seekbar);
        this.cornerRadiusSeekbar = (SeekBar) findViewById(R.id.exterior_corner_radius_seekbar);
        this.strokeWidthSeekbar = (SeekBar) findViewById(R.id.exterior_stroke_width_seekbar);
        this.textSizePressedSeekbar = (SeekBar) findViewById(R.id.exterior_text_size_seekbar_pressed);
        this.cornerRadiusPressedSeekbar = (SeekBar) findViewById(R.id.exterior_corner_radius_seekbar_pressed);
        this.strokeWidthPressedSeekbar = (SeekBar) findViewById(R.id.exterior_stroke_width_seekbar_pressed);
        this.textSizeText = (TextView) findViewById(R.id.text_size_text);
        this.cornerRadiusText = (TextView) findViewById(R.id.corner_radius_text);
        this.strokeWidthText = (TextView) findViewById(R.id.stroke_width_text);
        this.textSizePressedText = (TextView) findViewById(R.id.text_size_pressed_text);
        this.cornerRadiusPressedText = (TextView) findViewById(R.id.corner_radius_pressed_text);
        this.strokeWidthPressedText = (TextView) findViewById(R.id.stroke_width_pressed_text);
        this.selectTextColor = (Button) findViewById(R.id.exterior_text_color);
        this.selectStrokeColor = (Button) findViewById(R.id.exterior_stroke_color);
        this.selectFillColor = (Button) findViewById(R.id.exterior_fill_color);
        this.selectTextColorPressed = (Button) findViewById(R.id.exterior_text_color_pressed);
        this.selectStrokeColorPressed = (Button) findViewById(R.id.exterior_stroke_color_pressed);
        this.selectFillColorPressed = (Button) findViewById(R.id.exterior_fill_color_pressed);
        this.textColorPre = findViewById(R.id.text_color_preview);
        this.strokeColorPre = findViewById(R.id.stroke_color_preview);
        this.fillColorPre = findViewById(R.id.fill_color_preview);
        this.textColorPressedPre = findViewById(R.id.text_color_pressed_preview);
        this.strokeColorPressedPre = findViewById(R.id.stroke_color_pressed_preview);
        this.fillColorPressedPre = findViewById(R.id.fill_color_pressed_preview);
        this.textColorText = (TextView) findViewById(R.id.text_color_text);
        this.strokeColorText = (TextView) findViewById(R.id.stroke_color_text);
        this.fillColorText = (TextView) findViewById(R.id.fill_color_text);
        this.textColorPressedText = (TextView) findViewById(R.id.text_color_pressed_text);
        this.strokeColorPressedText = (TextView) findViewById(R.id.stroke_color_pressed_text);
        this.fillColorPressedText = (TextView) findViewById(R.id.fill_color_pressed_text);
        this.textSizeSeekbar.setProgress(this.buttonStyle.textSize);
        this.cornerRadiusSeekbar.setProgress(this.buttonStyle.cornerRadius);
        this.strokeWidthSeekbar.setProgress((int) (this.buttonStyle.strokeWidth * 10.0f));
        this.textSizePressedSeekbar.setProgress(this.buttonStyle.textSizePress);
        this.cornerRadiusPressedSeekbar.setProgress(this.buttonStyle.cornerRadiusPress);
        this.strokeWidthPressedSeekbar.setProgress((int) (this.buttonStyle.strokeWidthPress * 10.0f));
        this.textSizeText.setText(this.buttonStyle.textSize + " sp");
        this.cornerRadiusText.setText(this.buttonStyle.cornerRadius + " dp");
        this.strokeWidthText.setText(this.buttonStyle.strokeWidth + " dp");
        this.textSizePressedText.setText(this.buttonStyle.textSizePress + " sp");
        this.cornerRadiusPressedText.setText(this.buttonStyle.cornerRadiusPress + " dp");
        this.strokeWidthPressedText.setText(this.buttonStyle.strokeWidthPress + " dp");
        this.textColorPre.setBackgroundColor(Color.parseColor(this.buttonStyle.textColor));
        this.strokeColorPre.setBackgroundColor(Color.parseColor(this.buttonStyle.strokeColor));
        this.fillColorPre.setBackgroundColor(Color.parseColor(this.buttonStyle.fillColor));
        this.textColorPressedPre.setBackgroundColor(Color.parseColor(this.buttonStyle.textColorPress));
        this.strokeColorPressedPre.setBackgroundColor(Color.parseColor(this.buttonStyle.strokeColorPress));
        this.fillColorPressedPre.setBackgroundColor(Color.parseColor(this.buttonStyle.fillColorPress));
        this.textColorText.setText(this.buttonStyle.textColor);
        this.strokeColorText.setText(this.buttonStyle.strokeColor);
        this.fillColorText.setText(this.buttonStyle.fillColor);
        this.textColorPressedText.setText(this.buttonStyle.textColorPress);
        this.strokeColorPressedText.setText(this.buttonStyle.strokeColorPress);
        this.fillColorPressedText.setText(this.buttonStyle.fillColorPress);
        this.editName.addTextChangedListener(this);
        this.textSizeSeekbar.setOnSeekBarChangeListener(this);
        this.cornerRadiusSeekbar.setOnSeekBarChangeListener(this);
        this.strokeWidthSeekbar.setOnSeekBarChangeListener(this);
        this.textSizePressedSeekbar.setOnSeekBarChangeListener(this);
        this.cornerRadiusPressedSeekbar.setOnSeekBarChangeListener(this);
        this.strokeWidthPressedSeekbar.setOnSeekBarChangeListener(this);
        this.selectTextColor.setOnClickListener(this);
        this.selectStrokeColor.setOnClickListener(this);
        this.selectFillColor.setOnClickListener(this);
        this.selectTextColorPressed.setOnClickListener(this);
        this.selectStrokeColorPressed.setOnClickListener(this);
        this.selectFillColorPressed.setOnClickListener(this);
    }

    @Override // android.widget.SeekBar.OnSeekBarChangeListener
    public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
        if (seekBar == this.textSizeSeekbar) {
            this.textSizeText.setText(i + " sp");
            this.buttonStyle.textSize = i;
        }
        if (seekBar == this.cornerRadiusSeekbar) {
            this.cornerRadiusText.setText(i + " dp");
            this.buttonStyle.cornerRadius = i;
        }
        if (seekBar == this.strokeWidthSeekbar) {
            float f = i / 10.0f;
            this.strokeWidthText.setText(f + " dp");
            this.buttonStyle.strokeWidth = f;
        }
        if (seekBar == this.textSizePressedSeekbar) {
            this.textSizePressedText.setText(i + " sp");
            this.buttonStyle.textSizePress = i;
        }
        if (seekBar == this.cornerRadiusPressedSeekbar) {
            this.cornerRadiusPressedText.setText(i + " dp");
            this.buttonStyle.cornerRadiusPress = i;
        }
        if (seekBar == this.strokeWidthPressedSeekbar) {
            float f2 = i / 10.0f;
            this.strokeWidthPressedText.setText(f2 + " dp");
            this.buttonStyle.strokeWidthPress = f2;
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            ArrayList arrayList = new ArrayList();
            Iterator<ButtonStyle> it = this.list.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().name);
            }
            if (this.editName.getText().toString().equals("")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_button_style_name_empty), 0).show();
            } else if (arrayList.contains(this.editName.getText().toString())) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_button_style_name_exist), 0).show();
            } else {
                this.onButtonStyleCreateListener.onButtonStyleCreate(this.buttonStyle);
                dismiss();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
        if (view == this.selectTextColor) {
            ColorSelectorDialog colorSelectorDialog = new ColorSelectorDialog(getContext(), false, Color.parseColor(this.buttonStyle.textColor));
            colorSelectorDialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.1
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateButtonStyleDialog.this.textColorPre.setBackgroundColor(i);
                    CreateButtonStyleDialog.this.textColorText.setText("#" + Integer.toHexString(i));
                    CreateButtonStyleDialog.this.buttonStyle.textColor = "#" + Integer.toHexString(i);
                }
            });
            colorSelectorDialog.show();
        }
        if (view == this.selectStrokeColor) {
            ColorSelectorDialog colorSelectorDialog2 = new ColorSelectorDialog(getContext(), false, Color.parseColor(this.buttonStyle.strokeColor));
            colorSelectorDialog2.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.2
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateButtonStyleDialog.this.strokeColorPre.setBackgroundColor(i);
                    CreateButtonStyleDialog.this.strokeColorText.setText("#" + Integer.toHexString(i));
                    CreateButtonStyleDialog.this.buttonStyle.strokeColor = "#" + Integer.toHexString(i);
                }
            });
            colorSelectorDialog2.show();
        }
        if (view == this.selectFillColor) {
            ColorSelectorDialog colorSelectorDialog3 = new ColorSelectorDialog(getContext(), false, Color.parseColor(this.buttonStyle.fillColor));
            colorSelectorDialog3.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.3
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateButtonStyleDialog.this.fillColorPre.setBackgroundColor(i);
                    CreateButtonStyleDialog.this.fillColorText.setText("#" + Integer.toHexString(i));
                    CreateButtonStyleDialog.this.buttonStyle.fillColor = "#" + Integer.toHexString(i);
                }
            });
            colorSelectorDialog3.show();
        }
        if (view == this.selectTextColorPressed) {
            ColorSelectorDialog colorSelectorDialog4 = new ColorSelectorDialog(getContext(), false, Color.parseColor(this.buttonStyle.textColorPress));
            colorSelectorDialog4.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.4
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateButtonStyleDialog.this.textColorPressedPre.setBackgroundColor(i);
                    CreateButtonStyleDialog.this.textColorPressedText.setText("#" + Integer.toHexString(i));
                    CreateButtonStyleDialog.this.buttonStyle.textColorPress = "#" + Integer.toHexString(i);
                }
            });
            colorSelectorDialog4.show();
        }
        if (view == this.selectStrokeColorPressed) {
            ColorSelectorDialog colorSelectorDialog5 = new ColorSelectorDialog(getContext(), false, Color.parseColor(this.buttonStyle.strokeColorPress));
            colorSelectorDialog5.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.5
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateButtonStyleDialog.this.strokeColorPressedPre.setBackgroundColor(i);
                    CreateButtonStyleDialog.this.strokeColorPressedText.setText("#" + Integer.toHexString(i));
                    CreateButtonStyleDialog.this.buttonStyle.strokeColorPress = "#" + Integer.toHexString(i);
                }
            });
            colorSelectorDialog5.show();
        }
        if (view == this.selectFillColorPressed) {
            ColorSelectorDialog colorSelectorDialog6 = new ColorSelectorDialog(getContext(), false, Color.parseColor(this.buttonStyle.fillColorPress));
            colorSelectorDialog6.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() { // from class: com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.6
                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onColorSelected(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onNegative(int i) {
                }

                @Override // com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog.ColorSelectorDialogListener
                public void onPositive(int i) {
                    CreateButtonStyleDialog.this.fillColorPressedPre.setBackgroundColor(i);
                    CreateButtonStyleDialog.this.fillColorPressedText.setText("#" + Integer.toHexString(i));
                    CreateButtonStyleDialog.this.buttonStyle.fillColorPress = "#" + Integer.toHexString(i);
                }
            });
            colorSelectorDialog6.show();
        }
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        this.buttonStyle.name = this.editName.getText().toString();
    }
}
