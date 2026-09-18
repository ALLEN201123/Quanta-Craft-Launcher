/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.graphics.Color
 *  android.graphics.drawable.BitmapDrawable
 *  android.graphics.drawable.Drawable
 *  android.net.Uri
 *  android.os.Build$VERSION
 *  android.os.Environment
 *  android.os.Handler
 *  android.os.Message
 *  android.text.Editable
 *  android.text.TextWatcher
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.ViewGroup
 *  android.widget.CompoundButton
 *  android.widget.CompoundButton$OnCheckedChangeListener
 *  android.widget.EditText
 *  android.widget.ImageButton
 *  android.widget.LinearLayout
 *  android.widget.RadioButton
 *  android.widget.TextView
 *  androidx.annotation.NonNull
 *  androidx.appcompat.widget.SwitchCompat
 *  com.tungsten.filepicker.Constants$SELECTION_MODES
 *  com.tungsten.filepicker.FileChooser
 */
package com.qcl.launcher.launcher.uis.universal.setting.right.launcher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.launcher.uis.tools.QclThemeUtils;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.qcl.launcher.R;
public class ExteriorSettingUI
extends BaseUI
implements View.OnClickListener,
CompoundButton.OnCheckedChangeListener {
    private static final int PICK_BACKGROUND_REQUEST = 4000;
    public LinearLayout exteriorSettingUI;
    private LinearLayout selectTheme;
    private View colorView;
    private TextView colorText;
    private LinearLayout selectPanelColor;
    private View panelColorView;
    private TextView panelColorText;
    private SwitchCompat transBarSwitch;
    private SwitchCompat fullscreenSwitch;
    private SwitchCompat grassUiSwitch;
    private LinearLayout fullscreenSetting;
    private RadioButton defaultRadio;
    private RadioButton classicRadio;
    private RadioButton customRadio;
    private RadioButton onlineRadio;
    private EditText editBgPath;
    private EditText editBgUrl;
    private ImageButton selectBgPath;
    @SuppressLint(value={"HandlerLeak"})
    public final Handler handler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public ExteriorSettingUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onCreate() {
        super.onCreate();
        this.exteriorSettingUI = (LinearLayout)this.activity.findViewById(R.id.ui_setting_exterior);
        this.selectTheme = (LinearLayout)this.activity.findViewById(R.id.select_theme);
        this.colorView = this.activity.findViewById(R.id.theme_color_view);
        this.colorText = (TextView)this.activity.findViewById(R.id.theme_color_text);
        this.selectPanelColor = (LinearLayout)this.activity.findViewById(R.id.select_panel_color);
        this.panelColorView = this.activity.findViewById(R.id.panel_color_view);
        this.panelColorText = (TextView)this.activity.findViewById(R.id.panel_color_text);
        this.transBarSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_trans_bar);
        this.fullscreenSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_full_screen);
        this.grassUiSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_grass_ui);
        this.fullscreenSetting = (LinearLayout)this.activity.findViewById(R.id.fullscreen_layout);
        this.defaultRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_default);
        this.classicRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_classic);
        this.customRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_custom);
        this.onlineRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_online);
        this.editBgPath = (EditText)this.activity.findViewById(R.id.edit_bg_path);
        this.editBgUrl = (EditText)this.activity.findViewById(R.id.edit_bg_url);
        this.selectBgPath = (ImageButton)this.activity.findViewById(R.id.select_bg_path);
        if (this.activity.launcherSetting.launcherBackground.type == 0) {
            this.defaultRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
        } else if (this.activity.launcherSetting.launcherBackground.type == 1) {
            this.classicRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
        } else if (this.activity.launcherSetting.launcherBackground.type == 2) {
            this.customRadio.setChecked(true);
            this.editBgPath.setEnabled(true);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(true);
            if (new File(this.activity.launcherSetting.launcherBackground.path).exists() && ExteriorSettingUI.isImageFile(this.activity.launcherSetting.launcherBackground.path)) {
                Bitmap bitmap = BitmapFactory.decodeFile((String)this.activity.launcherSetting.launcherBackground.path);
                this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap));
            } else {
                this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_1));
            }
        } else {
            this.onlineRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(true);
            this.selectBgPath.setEnabled(false);
            new Thread(() -> {
                try {
                    URL url = new URL(this.activity.launcherSetting.launcherBackground.url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.connect();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream((InputStream)inputStream);
                    this.handler.post(() -> this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap)));
                }
                catch (IOException e) {
                    this.handler.post(() -> this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_1)));
                    e.printStackTrace();
                }
            }).start();
        }
        this.editBgPath.setText((CharSequence)this.activity.launcherSetting.launcherBackground.path);
        this.editBgUrl.setText((CharSequence)this.activity.launcherSetting.launcherBackground.url);
        this.selectTheme.setOnClickListener((View.OnClickListener)this);
        this.selectPanelColor.setOnClickListener((View.OnClickListener)this);
        this.transBarSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.fullscreenSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.grassUiSwitch != null) {
            this.grassUiSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        this.defaultRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.classicRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.customRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.onlineRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.selectBgPath.setOnClickListener((View.OnClickListener)this);
        this.editBgPath.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                ExteriorSettingUI.this.activity.launcherSetting.launcherBackground.path = ExteriorSettingUI.this.editBgPath.getText().toString();
                GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                if (new File(ExteriorSettingUI.this.editBgPath.getText().toString()).exists() && ExteriorSettingUI.isImageFile(ExteriorSettingUI.this.editBgPath.getText().toString())) {
                    Bitmap bitmap = BitmapFactory.decodeFile((String)ExteriorSettingUI.this.editBgPath.getText().toString());
                    ExteriorSettingUI.this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap));
                } else {
                    ExteriorSettingUI.this.activity.launcherLayout.setBackground(ExteriorSettingUI.this.context.getDrawable(R.drawable.qcl_bg_1));
                }
            }
        });
        this.editBgUrl.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                ExteriorSettingUI.this.activity.launcherSetting.launcherBackground.url = ExteriorSettingUI.this.editBgUrl.getText().toString();
                GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                new Thread(() -> {
                    try {
                        URL url = new URL(ExteriorSettingUI.this.editBgUrl.getText().toString());
                        HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.connect();
                        InputStream inputStream = httpURLConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream((InputStream)inputStream);
                        ExteriorSettingUI.this.handler.post(() -> ExteriorSettingUI.this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap)));
                    }
                    catch (IOException e) {
                        ExteriorSettingUI.this.handler.post(() -> ExteriorSettingUI.this.activity.launcherLayout.setBackground(ExteriorSettingUI.this.context.getDrawable(R.drawable.qcl_bg_1)));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
        this.transBarSwitch.setChecked(this.activity.launcherSetting.transBar);
        this.fullscreenSwitch.setChecked(this.activity.launcherSetting.fullscreen);
        if (this.grassUiSwitch != null) {
            this.grassUiSwitch.setChecked(this.activity.launcherSetting.uiTheme == 1);
        }
        this.refreshColorEditable();
        if (Build.VERSION.SDK_INT < 28) {
            this.fullscreenSetting.setVisibility(8);
        }
    }

    private void refreshColorEditable() {
        boolean grass = this.activity.launcherSetting.uiTheme == 1;
        this.setEnabledRecursive((View)this.selectTheme, !grass);
        this.setEnabledRecursive((View)this.selectPanelColor, !grass);
        if (this.colorView != null) {
            this.colorView.setAlpha(grass ? 0.35f : 1.0f);
        }
        if (this.panelColorView != null) {
            this.panelColorView.setAlpha(grass ? 0.35f : 1.0f);
        }
        if (this.colorText != null) {
            this.colorText.setAlpha(grass ? 0.45f : 1.0f);
        }
        if (this.panelColorText != null) {
            this.panelColorText.setAlpha(grass ? 0.45f : 1.0f);
        }
    }

    private void setEnabledRecursive(View v, boolean enabled) {
        if (v == null) {
            return;
        }
        v.setEnabled(enabled);
        v.setClickable(enabled);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); ++i) {
                g.getChildAt(i).setEnabled(enabled);
            }
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft((View)this.exteriorSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startExteriorSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
        this.colorView.setBackgroundColor(Color.parseColor((String)ExteriorSettingUI.getThemeColor(this.context, this.activity.launcherSetting.launcherTheme)));
        this.colorText.setText((CharSequence)ExteriorSettingUI.getThemeColor(this.context, this.activity.launcherSetting.launcherTheme));
        int pc = ExteriorSettingUI.getPanelColor(this.context, this.activity.launcherSetting.panelColor);
        this.panelColorView.setBackgroundColor(pc);
        this.panelColorText.setText((CharSequence)("#" + Integer.toHexString(pc)));
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft((View)this.exteriorSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startExteriorSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4000 && data != null && resultCode == -1) {
            Uri uri = data.getData();
            this.editBgPath.setText((CharSequence)UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
        }
    }

    public static int getPanelColor(Context context, String color2) {
        if (color2 == null || color2.equals("DEFAULT") || color2.isEmpty()) {
            return context.getResources().getColor(R.color.qcl_panel_gray_alt);
        }
        try {
            return Color.parseColor((String)color2);
        }
        catch (Throwable t) {
            return context.getResources().getColor(R.color.qcl_panel_gray_alt);
        }
    }

    public static void applyPanelTint(Context context, View root, int color2) {
        Drawable bg;
        if (root == null) {
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)root;
            for (int i = 0; i < g.getChildCount(); ++i) {
                ExteriorSettingUI.applyPanelTint(context, g.getChildAt(i), color2);
            }
        }
        if ((bg = root.getBackground()) == null || bg.getConstantState() == null) {
            return;
        }
        for (int id2 : new int[]{R.drawable.launcher_view_white, R.drawable.launcher_view_light_gray}) {
            Drawable ref = context.getResources().getDrawable(id2);
            if (ref == null || ref.getConstantState() == null || !ref.getConstantState().equals(bg.getConstantState())) continue;
            bg.mutate().setTint(color2);
            break;
        }
    }

    public static String getThemeColor(Context context, String color2) {
        if (color2.equals("DEFAULT")) {
            return "#" + Integer.toHexString(context.getColor(R.color.colorAccent));
        }
        return color2;
    }

    public static boolean isImageFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile((String)filePath, (BitmapFactory.Options)options);
        return options.outWidth != -1;
    }

    public void onClick(View v) {
        ColorSelectorDialog dialog;
        if (v == this.selectTheme) {
            dialog = new ColorSelectorDialog(this.context, true, Color.parseColor((String)ExteriorSettingUI.getThemeColor(this.context, this.activity.launcherSetting.launcherTheme)));
            dialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onColorSelected(int color2) {
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(color2);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(color2);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.colorView.setBackgroundColor(color2);
                    ExteriorSettingUI.this.colorText.setText((CharSequence)("#" + Integer.toHexString(color2)));
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onPositive(int destColor) {
                    ExteriorSettingUI.this.activity.launcherSetting.launcherTheme = "#" + Integer.toHexString(destColor);
                    GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(destColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(destColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.colorView.setBackgroundColor(destColor);
                    ExteriorSettingUI.this.colorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onNegative(int initColor) {
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(initColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(initColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.colorView.setBackgroundColor(initColor);
                    ExteriorSettingUI.this.colorText.setText((CharSequence)("#" + Integer.toHexString(initColor)));
                }
            });
            dialog.show();
        }
        if (v == this.selectPanelColor) {
            dialog = new ColorSelectorDialog(this.context, true, ExteriorSettingUI.getPanelColor(this.context, this.activity.launcherSetting.panelColor));
            dialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onColorSelected(int color2) {
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(color2);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)("#" + Integer.toHexString(color2)));
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onPositive(int destColor) {
                    ExteriorSettingUI.this.activity.launcherSetting.panelColor = "#" + Integer.toHexString(destColor);
                    GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(destColor);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)("#" + Integer.toHexString(destColor)));
                    ExteriorSettingUI.applyPanelTint(ExteriorSettingUI.this.context, (View)ExteriorSettingUI.this.exteriorSettingUI, destColor);
                    ExteriorSettingUI.applyPanelTint(ExteriorSettingUI.this.context, ExteriorSettingUI.this.activity.getWindow().getDecorView(), destColor);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onNegative(int initColor) {
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(initColor);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)("#" + Integer.toHexString(initColor)));
                }
            });
            dialog.show();
        }
        if (v == this.selectBgPath) {
            Intent intent = new Intent(this.context, FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "png;jpg");
            intent.putExtra("INITIAL_DIRECTORY", new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            this.activity.startActivityForResult(intent, 4000);
        }
    }

    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == this.transBarSwitch) {
            this.activity.launcherSetting.transBar = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            if (isChecked) {
                // empty if block
            }
        }
        if (buttonView == this.fullscreenSwitch) {
            this.activity.launcherSetting.fullscreen = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            if (Build.VERSION.SDK_INT >= 28) {
                this.activity.getWindow().getAttributes().layoutInDisplayCutoutMode = isChecked ? 1 : 2;
            }
            this.activity.getWindow().setFlags(256, 256);
        }
        if (this.grassUiSwitch != null && buttonView == this.grassUiSwitch) {
            this.activity.launcherSetting.uiTheme = isChecked ? 1 : 0;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            QclThemeUtils.apply((Activity)this.activity, this.activity.launcherSetting.uiTheme);
            this.refreshColorEditable();
        }
        if (buttonView == this.defaultRadio && isChecked) {
            this.classicRadio.setChecked(false);
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherSetting.launcherBackground.type = 0;
            this.activity.refreshDynamicBackground();
        }
        if (buttonView == this.classicRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherSetting.launcherBackground.type = 1;
            this.activity.refreshDynamicBackground();
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
        }
        if (buttonView == this.customRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(true);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(true);
            this.activity.launcherSetting.launcherBackground.type = 2;
            this.activity.refreshDynamicBackground();
            this.activity.launcherSetting.launcherBackground.path = this.editBgPath.getText().toString();
            if (new File(this.editBgPath.getText().toString()).exists() && ExteriorSettingUI.isImageFile(this.editBgPath.getText().toString())) {
                Bitmap bitmap = BitmapFactory.decodeFile((String)this.editBgPath.getText().toString());
                this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap));
            } else {
                this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_1));
            }
        }
        if (buttonView == this.onlineRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            this.customRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(true);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherSetting.launcherBackground.type = 3;
            this.activity.refreshDynamicBackground();
            this.activity.launcherSetting.launcherBackground.path = this.editBgUrl.getText().toString();
            new Thread(() -> {
                try {
                    URL url = new URL(this.editBgUrl.getText().toString());
                    HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.connect();
                    InputStream inputStream = httpURLConnection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream((InputStream)inputStream);
                    this.handler.post(() -> this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap)));
                }
                catch (IOException e) {
                    this.handler.post(() -> this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_1)));
                    e.printStackTrace();
                }
            }).start();
        }
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }
}

