package com.qcl.launcher.launcher.dialogs.tools;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.jaredrummler.android.colorpicker.ColorPickerView;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ColorSelectorDialog extends Dialog implements ColorPickerView.OnColorChangedListener, View.OnClickListener, TextView.OnEditorActionListener {
    public Button colorFif;
    public Button colorFor;
    public ColorPickerView colorPickerView;
    public Button colorPri;
    public Button colorSec;
    public ColorSelectorDialogListener colorSelectorDialogListener;
    public Button colorSix;
    public Button colorThi;
    public Context context;
    public int currentColor;
    public View destColorBar;
    public EditText editColor;
    public int initColor;
    public View initColorBar;
    public Button negative;
    public Button positive;
    public LinearLayout recommendColorBar;
    public boolean showRecommendBar;

    /* loaded from: classes2.dex */
    public interface ColorSelectorDialogListener {
        void onColorSelected(int i);

        void onNegative(int i);

        void onPositive(int i);
    }

    public ColorSelectorDialog(Context context, boolean z, int i) {
        super(context);
        setContentView(R.layout.dialog_color_selector);
        setCancelable(false);
        this.context = context;
        this.showRecommendBar = z;
        this.initColor = i;
        init();
    }

    private void init() {
        ColorPickerView colorPickerView = (ColorPickerView) findViewById(R.id.color_picker);
        this.colorPickerView = colorPickerView;
        colorPickerView.setOnColorChangedListener(this);
        this.colorPickerView.setColor(this.initColor);
        this.currentColor = this.initColor;
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.recommend_color_bar);
        this.recommendColorBar = linearLayout;
        if (this.showRecommendBar) {
            linearLayout.setVisibility(0);
        }
        this.colorPri = (Button) findViewById(R.id.recommend_color_pri);
        this.colorSec = (Button) findViewById(R.id.recommend_color_sec);
        this.colorThi = (Button) findViewById(R.id.recommend_color_thi);
        this.colorFor = (Button) findViewById(R.id.recommend_color_for);
        this.colorFif = (Button) findViewById(R.id.recommend_color_fif);
        this.colorSix = (Button) findViewById(R.id.recommend_color_six);
        this.colorPri.setOnClickListener(this);
        this.colorSec.setOnClickListener(this);
        this.colorThi.setOnClickListener(this);
        this.colorFor.setOnClickListener(this);
        this.colorFif.setOnClickListener(this);
        this.colorSix.setOnClickListener(this);
        this.initColorBar = findViewById(R.id.init_color);
        this.destColorBar = findViewById(R.id.dest_color);
        this.initColorBar.setBackgroundColor(this.initColor);
        this.destColorBar.setBackgroundColor(this.initColor);
        EditText editText = (EditText) findViewById(R.id.color_text);
        this.editColor = editText;
        editText.setOnEditorActionListener(this);
        this.editColor.setText("#" + Integer.toHexString(this.initColor));
        this.positive = (Button) findViewById(R.id.color_picker_positive);
        this.negative = (Button) findViewById(R.id.color_picker_negative);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
    }

    private void setColor(int i) {
        this.currentColor = i;
        this.colorPickerView.setColor(i);
        this.editColor.setText("#" + Integer.toHexString(i));
        this.destColorBar.setBackgroundColor(i);
        this.colorSelectorDialogListener.onColorSelected(i);
    }

    @Override // com.jaredrummler.android.colorpicker.ColorPickerView.OnColorChangedListener
    public void onColorChanged(int i) {
        setColor(i);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.colorPri) {
            setColor(this.context.getResources().getColor(R.color.colorPrimary));
        }
        if (view == this.colorSec) {
            setColor(this.context.getResources().getColor(R.color.colorSecondary));
        }
        if (view == this.colorThi) {
            setColor(this.context.getResources().getColor(R.color.colorThird));
        }
        if (view == this.colorFor) {
            setColor(this.context.getResources().getColor(R.color.colorForth));
        }
        if (view == this.colorFif) {
            setColor(this.context.getResources().getColor(R.color.colorFifth));
        }
        if (view == this.colorSix) {
            setColor(this.context.getResources().getColor(R.color.colorSixth));
        }
        if (view == this.positive) {
            this.colorSelectorDialogListener.onPositive(this.currentColor);
            dismiss();
        }
        if (view == this.negative) {
            this.colorSelectorDialogListener.onNegative(this.initColor);
            dismiss();
        }
    }

    @Override // android.widget.TextView.OnEditorActionListener
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        setColor(Color.parseColor(textView.getText().toString()));
        return false;
    }

    public void setColorSelectorDialogListener(ColorSelectorDialogListener colorSelectorDialogListener) {
        this.colorSelectorDialogListener = colorSelectorDialogListener;
    }
}
