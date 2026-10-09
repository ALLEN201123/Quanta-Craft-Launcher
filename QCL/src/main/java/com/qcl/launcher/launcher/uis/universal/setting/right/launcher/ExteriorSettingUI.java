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

import com.qcl.launcher.utils.QclColors;
import com.qcl.launcher.skin.gltf.SkinRenderer;

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
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.tools.ColorSelectorDialog;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
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
    // ★★★ 1.5.0 用户要求：主题色入口从「色块+十六进制小按钮」改为**下拉框**（Spinner）。
    //   点开列出预设配色，选中即全局应用。下面的两个自定义色块负责进调色板微调。
    private Spinner selectTheme;
    private View colorView;
    private TextView colorText;
    private LinearLayout selectPanelColor;
    private View panelColorView;
    private TextView panelColorText;
    /** ★ 1.5.0 新增：下拉框下面那两个「自定义色块」（点进去开调色板）。 */
    private LinearLayout customThemeBlock;
    private LinearLayout customPanelBlock;
    private View panelColorBlockView;
    private TextView panelColorBlockText;
    /** ★ 1.5.0：主题色下拉的适配器（预设配色清单）。 */
    private ThemeColorSpinnerAdapter themeColorAdapter;
    /** ★ 1.5.0：防止 onItemSelected 在初始化/程序化设值时误触发应用。 */
    private boolean themeSpinnerReady = false;
    private SwitchCompat transBarSwitch;
    private SwitchCompat fullscreenSwitch;
    private SwitchCompat showAccountModelSwitch;
    private SwitchCompat transBgSwitch;
    /** ★ 1.5.0：3D 人物动作下拉（站立/走路/跑步/旋转/挥手） */
    private Spinner accountModelAnimSpinner;
    /** ★ 1.5.0：动作那一整行（开关关闭时整行置灰不可选） */
    private LinearLayout accountModelAnimRow;
    private LinearLayout fullscreenSetting;
    private RadioButton defaultRadio;
    private RadioButton classicRadio;
    /** ★ 1.5.0：白天背景（固定显示白天图） */
    private RadioButton dayRadio;
    /** ★ 1.5.0：排版选择（旧排版 / 新排版） */
    private RadioButton layoutOldRadio;
    private RadioButton layoutNewRadio;
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
        this.selectTheme = (Spinner)this.activity.findViewById(R.id.select_theme);
        this.colorView = this.activity.findViewById(R.id.theme_color_view);
        this.colorText = (TextView)this.activity.findViewById(R.id.theme_color_text);
        this.selectPanelColor = (LinearLayout)this.activity.findViewById(R.id.select_panel_color);
        this.panelColorView = this.activity.findViewById(R.id.panel_color_view);
        this.panelColorText = (TextView)this.activity.findViewById(R.id.panel_color_text);
        // ★ 1.5.0：下拉框下方的两个自定义色块
        this.customThemeBlock = (LinearLayout)this.activity.findViewById(R.id.custom_theme_block);
        this.customPanelBlock = (LinearLayout)this.activity.findViewById(R.id.custom_panel_block);
        this.panelColorBlockView = this.activity.findViewById(R.id.panel_color_block_view);
        this.panelColorBlockText = (TextView)this.activity.findViewById(R.id.panel_color_block_text);
        setupThemeColorSpinner();
        this.transBarSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_trans_bar);
        this.fullscreenSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_full_screen);
        this.showAccountModelSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_show_account_model);
        this.transBgSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_transparent_bg);
        // ★ 1.5.0：3D 人物动作（站立/走路/跑步/旋转/挥手）
        this.accountModelAnimSpinner = (Spinner)this.activity.findViewById(R.id.spinner_account_model_anim);
        this.accountModelAnimRow = (LinearLayout)this.activity.findViewById(R.id.account_model_anim_row);
        this.fullscreenSetting = (LinearLayout)this.activity.findViewById(R.id.fullscreen_layout);
        this.defaultRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_default);
        this.classicRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_classic);
        this.dayRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_day);
        this.customRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_custom);
        this.onlineRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_online);
        // ★★★ 1.5.0：排版（旧排版 / 新排版）。这里就先把选中状态定好 —— 此刻**监听还没注册**
        //   （注册在下面 onCreate 末尾），所以不会触发 onCheckedChanged、不会误弹「重启后生效」。
        this.layoutOldRadio = (RadioButton)this.activity.findViewById(R.id.select_layout_old);
        this.layoutNewRadio = (RadioButton)this.activity.findViewById(R.id.select_layout_new);
        boolean useNewLayout = this.activity.launcherSetting.useNewLayout();
        if (this.layoutNewRadio != null) {
            this.layoutNewRadio.setChecked(useNewLayout);
        }
        if (this.layoutOldRadio != null) {
            this.layoutOldRadio.setChecked(!useNewLayout);
        }
        this.editBgPath = (EditText)this.activity.findViewById(R.id.edit_bg_path);
        this.editBgUrl = (EditText)this.activity.findViewById(R.id.edit_bg_url);
        this.selectBgPath = (ImageButton)this.activity.findViewById(R.id.select_bg_path);
        // ★ 1.3.4：type 0（自动切换/动态背景）改由「网络」选项承载；type 1（经典图片）成为「默认」
        // ★ 1.5.0：type 1 = 「默认」= 【按现实时间自动切昼夜】（老配置存量值就是 1，自动继承，无需迁移）
        //           新增 type 4 =「黑夜」（固定黑夜图）、type 5 =「白天」（固定白天图）
        if (this.activity.launcherSetting.launcherBackground.type == 0) {
            // 动态背景（自动切换）→ 选中「网络」
            this.onlineRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
        } else if (this.activity.launcherSetting.launcherBackground.type == 1) {
            // ★ 1.5.0：默认 → 按现实时间自动切昼夜
            this.defaultRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            com.qcl.launcher.launcher.uis.main.DynamicBackground
                    .applyAutoDayNight(this.activity, this.activity.launcherLayout);
        } else if (this.activity.launcherSetting.launcherBackground.type == 4) {
            // ★ 1.5.0：黑夜（固定）
            this.classicRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
        } else if (this.activity.launcherSetting.launcherBackground.type == 5) {
            // ★ 1.5.0：白天（固定）
            this.dayRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_day));
        } else if (this.activity.launcherSetting.launcherBackground.type == 2) {
            this.customRadio.setChecked(true);
            this.editBgPath.setEnabled(true);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(true);
            if (new File(this.activity.launcherSetting.launcherBackground.path).exists() && ExteriorSettingUI.isImageFile(this.activity.launcherSetting.launcherBackground.path)) {
                Bitmap bitmap = BitmapFactory.decodeFile((String)this.activity.launcherSetting.launcherBackground.path);
                this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap));
            } else {
                // ★ 1.5.0：自定义路径无效时的占位图 → 换成经典（黑夜）那张
                this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
            }
        } else {
            // ★ 1.3.4：type 3（旧「单张网络图」）与 type 0（动态背景）统一按「网络 = 自动切换」处理
            //   旧用户升级后：选中「网络」，由 DynamicBackground 接管轮播
            this.onlineRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            if (this.activity.launcherSetting.launcherBackground.type == 3) {
                this.activity.launcherSetting.launcherBackground.type = 0;
                this.activity.refreshDynamicBackground();
            }
        }
        this.editBgPath.setText((CharSequence)this.activity.launcherSetting.launcherBackground.path);
        this.editBgUrl.setText((CharSequence)this.activity.launcherSetting.launcherBackground.url);
        // ★ 1.5.0：selectTheme 已改为 Spinner（由 setupThemeColorSpinner 挂监听），不再走 onClick。
        this.selectPanelColor.setOnClickListener((View.OnClickListener)this);
        // ★ 1.5.0：下拉框下面两个自定义色块 → 开调色板
        if (this.customThemeBlock != null) {
            this.customThemeBlock.setOnClickListener((View.OnClickListener)this);
        }
        if (this.customPanelBlock != null) {
            this.customPanelBlock.setOnClickListener((View.OnClickListener)this);
        }
        this.transBarSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.fullscreenSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.showAccountModelSwitch != null) {
            this.showAccountModelSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        if (this.transBgSwitch != null) {
            this.transBgSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        this.defaultRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.classicRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.dayRadio != null) {
            this.dayRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        this.customRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.onlineRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.layoutOldRadio != null) {
            this.layoutOldRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        if (this.layoutNewRadio != null) {
            this.layoutNewRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        // ★ 1.5.0：动作下拉的监听**最后**注册（回填在前面已完成，避免一进页面就回调）
        this.registerAccountModelAnimListener();
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
                    // ★ 1.5.0：占位图 → 经典（黑夜）那张
                    ExteriorSettingUI.this.activity.launcherLayout.setBackground(ExteriorSettingUI.this.context.getDrawable(R.drawable.ic_background_classic));
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
                        // ★ 1.5.0：URL 拉取失败时的兜底 → 经典（黑夜）那张
                        ExteriorSettingUI.this.handler.post(() -> ExteriorSettingUI.this.activity.launcherLayout.setBackground(ExteriorSettingUI.this.context.getDrawable(R.drawable.ic_background_classic)));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
        this.transBarSwitch.setChecked(this.activity.launcherSetting.transBar);
        this.fullscreenSwitch.setChecked(this.activity.launcherSetting.fullscreen);
        if (this.showAccountModelSwitch != null) {
            this.showAccountModelSwitch.setChecked(this.activity.launcherSetting.showAccountModel);
        }
        // ★★★ 1.5.0：3D 人物动作 —— 回填选中项 + 按「显示人物模型」开关联动可用性。
        //   ★ 用户要求：开关**关闭时动作选择不可用**，开启后**可自由选择**。
        //   ★ 回填必须放在 setOnItemSelectedListener **之前**，否则一进页面就触发一次
        //     onItemSelected、把当前值又写回去（虽然值相同，但会多做一次无谓的磁盘写入）。
        //     ⇒ 所以这里只回填，监听在下面 registerAnimListener() 里注册。
        if (this.accountModelAnimSpinner != null) {
            int anim = this.activity.launcherSetting.accountModelAnim;
            // ★ 1.5.0：老 MinecraftSkinRenderer（连同 GameCharacter）已删除，
            //   这里只用字面量做范围校验（下拉仍是 0~5：待机/走/跑/转/挥手/点头）。
            if (anim < 0 || anim > 5) {
                anim = 0;
            }
            this.accountModelAnimSpinner.setSelection(anim);
        }
        this.applyAccountModelAnimEnabled(this.activity.launcherSetting.showAccountModel);
        // ★★★ 1.5.0：草方块开关移除后，「背景透明度」的同步必须独立出来 ——
        //   它原来被错误地塞在 grassUiSwitch 的分支里（草方块开关为 null 就永远不会同步），
        //   所以删草方块时必须把这段搬出来，否则这个开关会一直显示错的状态。
        if (this.transBgSwitch != null) {
            this.transBgSwitch.setChecked(this.activity.launcherSetting.transparentBackground);
        }
        // ★ 1.5.0：排版单选回填。用 useNewLayout()（老配置没存过这个键时算「新排版」），
        //   与 onCreate 里的初值口径一致；setChecked 与当前设置一致 → 不会触发保存/Toast。
        boolean layoutIsNew = this.activity.launcherSetting.useNewLayout();
        if (this.layoutNewRadio != null) {
            this.layoutNewRadio.setChecked(layoutIsNew);
        }
        if (this.layoutOldRadio != null) {
            this.layoutOldRadio.setChecked(!layoutIsNew);
        }
        this.refreshColorEditable();
        if (Build.VERSION.SDK_INT < 28) {
            this.fullscreenSetting.setVisibility(8);
        }
    }

    /**
     * ★ 1.5.0：草方块 UI 下线后，主题色 / 面板色**永远可编辑**。
     *   原来这里按 {@code uiTheme == 1} 把两块颜色选择器禁用并调成半透明；
     *   现在固定为「启用 + 不透明」。
     */
    private void refreshColorEditable() {
        this.setEnabledRecursive((View)this.selectTheme, true);
        this.setEnabledRecursive((View)this.selectPanelColor, true);
        if (this.customThemeBlock != null) {
            this.setEnabledRecursive((View)this.customThemeBlock, true);
        }
        if (this.customPanelBlock != null) {
            this.setEnabledRecursive((View)this.customPanelBlock, true);
        }
        if (this.colorView != null) {
            this.colorView.setAlpha(1.0f);
        }
        if (this.panelColorView != null) {
            this.panelColorView.setAlpha(1.0f);
        }
        if (this.colorText != null) {
            this.colorText.setAlpha(1.0f);
        }
        if (this.panelColorText != null) {
            this.panelColorText.setAlpha(1.0f);
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
        this.colorView.setBackgroundColor(ExteriorSettingUI.parseThemeColorSafe(this.context, this.activity.launcherSetting.launcherTheme));
        this.colorText.setText((CharSequence)ExteriorSettingUI.getThemeColor(this.context, this.activity.launcherSetting.launcherTheme));
        int pc = ExteriorSettingUI.getPanelColor(this.context, this.activity.launcherSetting.panelColor);
        this.panelColorView.setBackgroundColor(pc);
        this.panelColorText.setText((CharSequence)(QclColors.format(pc)));
        // ★ 1.5.0：下拉框下面那个"面板色自定义色块"也要显示当前面板色
        this.applyPanelColorToBlock(pc);
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
            return QclColors.parseSafe(color2, 0xFFFFFFFF);
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

    /**
     * 主题色字符串 —— 读取侧已加固：null / 空 / DEFAULT / 历史非法值（如未补零的 "#ff"）
     * 一律回退为合法的默认强调色串，绝不再把非法值抛给 Color.parseColor。
     * （真机 OPPO PDVM00 曾因存档里 "#ff" 这类值在启动期 parseColor 崩溃。）
     */
    public static String getThemeColor(Context context, String color2) {
        int fallback = context.getColor(R.color.colorAccent);
        if (color2 == null || "DEFAULT".equals(color2) || color2.trim().isEmpty()) {
            return QclColors.format(fallback);
        }
        String s = color2.trim();
        try {
            Color.parseColor(s);
            return s;
        } catch (Throwable t) {
            android.util.Log.w("jrelog", "[颜色] 主题色非法值已回退: " + s);
            return QclColors.format(fallback);
        }
    }

    /** 主题色 → int，安全解析（启动期着色用，绝不抛异常）。 */
    public static int parseThemeColorSafe(Context context, String color2) {
        return QclColors.parseSafeTheme(context, getThemeColor(context, color2), R.color.colorAccent);
    }

    public static boolean isImageFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile((String)filePath, (BitmapFactory.Options)options);
        return options.outWidth != -1;
    }

    /**
     * ★★★ 1.5.0：主题色下拉框（用户要求「最上面是颜色的下拉框」）。
     *
     * <p>预设配色清单 —— 全部是"在深浅底上都能看清"的稳色，第一项是默认（斑驳森林色）。
     * 选中某一项 → 立刻应用 + 存档（与原来调色板 onPositive 的行为一致）。
     * 想要任意颜色 → 点下面的「自定义」色块开调色板。
     */
    private void setupThemeColorSpinner() {
        if (this.selectTheme == null) {
            return;
        }
        try {
            this.themeColorAdapter = new ThemeColorSpinnerAdapter(this.context);
            this.selectTheme.setAdapter(this.themeColorAdapter);
            // 定位到当前已选中的预设（找不到则不设，避免误触发）
            String cur = ExteriorSettingUI.getThemeColor(this.context, this.activity.launcherSetting.launcherTheme);
            int pos = this.themeColorAdapter.indexOfColor(cur);
            this.selectTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // ★ 初始化阶段（程序化 setSelection）不触发应用，否则一进页面就会把
                    //   玩家存档里的自定义色覆盖成第一个预设。
                    if (!ExteriorSettingUI.this.themeSpinnerReady) {
                        return;
                    }
                    int color = ExteriorSettingUI.this.themeColorAdapter.colorAt(position);
                    ExteriorSettingUI.this.activity.launcherSetting.launcherTheme = QclColors.format(color);
                    GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting,
                            AppManifest.SETTING_DIR + "/launcher_setting.json");
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(color);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(color);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.applyThemeColorToViews(color);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            if (pos >= 0) {
                this.selectTheme.setSelection(pos);
            }
            // 设完初值才放开：后续玩家手选才会真正应用
            this.selectTheme.post(() -> ExteriorSettingUI.this.themeSpinnerReady = true);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /** 主题色变化后，把新色刷到那两个预览色块上（右侧小按钮 + 下拉框下方的大色块）。 */
    private void applyThemeColorToViews(int color) {
        if (this.colorView != null) {
            this.colorView.setBackgroundColor(color);
        }
        if (this.colorText != null) {
            this.colorText.setText(QclColors.format(color));
        }
        // ★ 注：下拉框下方的第一个"自定义色块"本身就是调色板的入口（无独立预览 View），
        //   第二个色块是面板色，与主题色无关 —— 所以这里只需刷右侧那两个预览。
    }

    /** 面板色变化后，刷到下方那个自定义面板色块。 */
    private void applyPanelColorToBlock(int color) {
        if (this.panelColorBlockView != null) {
            this.panelColorBlockView.setBackgroundColor(color);
        }
        if (this.panelColorBlockText != null) {
            this.panelColorBlockText.setText(QclColors.format(color));
        }
    }

    public void onClick(View v) {
        ColorSelectorDialog dialog;
        if (v == this.customThemeBlock) {
            // ★ 1.5.0：主题色调色板改由**下拉框下方的第一个自定义色块**触发
            //   （selectTheme 本身已是 Spinner，预设在 setupThemeColorSpinner 里处理）。
            dialog = new ColorSelectorDialog(this.context, true, ExteriorSettingUI.parseThemeColorSafe(this.context, this.activity.launcherSetting.launcherTheme));
            dialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onColorSelected(int color2) {
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(color2);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(color2);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.applyThemeColorToViews(color2);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onPositive(int destColor) {
                    ExteriorSettingUI.this.activity.launcherSetting.launcherTheme = QclColors.format(destColor);
                    GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(destColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(destColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.applyThemeColorToViews(destColor);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onNegative(int initColor) {
                    ExteriorSettingUI.this.activity.exteriorConfig.primaryColor(initColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.accentColor(initColor);
                    ExteriorSettingUI.this.activity.exteriorConfig.apply((Activity)ExteriorSettingUI.this.activity);
                    ExteriorSettingUI.this.applyThemeColorToViews(initColor);
                }
            });
            dialog.show();
        }
        // ★ 1.5.0：面板色的两个入口都走这里 —— 右侧小按钮 + 下拉框下方的第二个自定义色块。
        if (v == this.selectPanelColor || v == this.customPanelBlock) {
            dialog = new ColorSelectorDialog(this.context, true, ExteriorSettingUI.getPanelColor(this.context, this.activity.launcherSetting.panelColor));
            dialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onColorSelected(int color2) {
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(color2);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)(QclColors.format(color2)));
                    ExteriorSettingUI.this.applyPanelColorToBlock(color2);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onPositive(int destColor) {
                    ExteriorSettingUI.this.activity.launcherSetting.panelColor = QclColors.format(destColor);
                    GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(destColor);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)(QclColors.format(destColor)));
                    ExteriorSettingUI.this.applyPanelColorToBlock(destColor);
                    ExteriorSettingUI.applyPanelTint(ExteriorSettingUI.this.context, (View)ExteriorSettingUI.this.exteriorSettingUI, destColor);
                    ExteriorSettingUI.applyPanelTint(ExteriorSettingUI.this.context, ExteriorSettingUI.this.activity.getWindow().getDecorView(), destColor);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onNegative(int initColor) {
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(initColor);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)(QclColors.format(initColor)));
                    ExteriorSettingUI.this.applyPanelColorToBlock(initColor);
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
        // ★ 1.5.0：草方块 UI 开关已移除（那套主题一堆 bug，用户要求下线）。
        //   原来这里会写 launcherSetting.uiTheme 并立刻 QclThemeUtils.apply(...)。
        // 1.3.7: 主界面是否显示账号人物（默认开）
        if (this.showAccountModelSwitch != null && buttonView == this.showAccountModelSwitch) {
            this.activity.launcherSetting.showAccountModel = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            // ★ 1.5.0：开关一变，动作选择行的可用性要跟着变（关=置灰不可选，开=可选）
            this.applyAccountModelAnimEnabled(isChecked);
        }
        // ★ 1.2.3：透明界面背景（默认开；关掉恢复原来的灰色面板 #C8EDEDED）
        if (this.transBgSwitch != null && buttonView == this.transBgSwitch) {
            this.activity.launcherSetting.transparentBackground = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            View container = this.activity.findViewById(R.id.main_ui_container);
            if (container != null) {
                container.setBackgroundColor(isChecked
                        ? android.graphics.Color.TRANSPARENT
                        : android.graphics.Color.parseColor("#C8EDEDED"));
            }
        }
        // ★★★ 1.5.0：排版切换（旧排版 / 新排版）。
        //   ★ 只有「值真的变了」才写入 + 弹提示 —— 否则每次进外观页回填 setChecked
        //     都会触发一次 onCheckedChanged，一进页面就弹「重启后生效」，很烦。
        //   ★ 必须重启才生效：MainActivity.inflateMainUi() 只在初始化时 inflate **一套**
        //     （两套布局 id 相同，同时挂进视图树会 findViewById 串页），运行时换不了。
        if (buttonView == this.layoutNewRadio && isChecked) {
            this.layoutOldRadio.setChecked(false);
            if (!this.activity.launcherSetting.useNewLayout()) {
                this.activity.launcherSetting.uiStyle = 1;
                this.activity.launcherSetting.uiStyleChosen = true;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                Toast.makeText(this.context, R.string.exterior_setting_ui_layout_restart, 1).show();
            }
        }
        if (buttonView == this.layoutOldRadio && isChecked) {
            this.layoutNewRadio.setChecked(false);
            if (this.activity.launcherSetting.useNewLayout()) {
                this.activity.launcherSetting.uiStyle = 0;
                this.activity.launcherSetting.uiStyleChosen = true;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                Toast.makeText(this.context, R.string.exterior_setting_ui_layout_restart, 1).show();
            }
        }
        if (buttonView == this.defaultRadio && isChecked) {
            this.classicRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.5.0：「默认」= 按现实时间自动切昼夜（type 1）
            this.activity.launcherSetting.launcherBackground.type = 1;
            this.activity.refreshDynamicBackground();
            com.qcl.launcher.launcher.uis.main.DynamicBackground
                    .applyAutoDayNight(this.activity, this.activity.launcherLayout);
        }
        if (buttonView == this.classicRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.5.0：「黑夜」= 固定黑夜图（type 4）
            this.activity.launcherSetting.launcherBackground.type = 4;
            this.activity.refreshDynamicBackground();
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
        }
        if (this.dayRadio != null && buttonView == this.dayRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.5.0：「白天」= 固定白天图（type 5）
            this.activity.launcherSetting.launcherBackground.type = 5;
            this.activity.refreshDynamicBackground();
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_day));
        }
        if (buttonView == this.customRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
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
                // ★ 1.5.0：点「自定义」但路径还无效时的占位 → 经典（黑夜）那张
                this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
            }
        }
        if (buttonView == this.onlineRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.customRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.3.4：「网络」= 自动切换的动态背景（type 0，图片由 DynamicBackground 从网络拉取）
            this.activity.launcherSetting.launcherBackground.type = 0;
            this.activity.refreshDynamicBackground();
        }
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }

    /**
     * ★★★ 1.5.0：3D 人物动作下拉的可用性（用户：「关闭时选择不可用，开启后可自由选择」）。
     *
     * @param enabled 「显示人物模型」开关是否打开
     */
    private void applyAccountModelAnimEnabled(boolean enabled) {
        if (this.accountModelAnimSpinner != null) {
            this.accountModelAnimSpinner.setEnabled(enabled);
            // 置灰时用半透明表达「不可选」，比只 setEnabled 更直观
            this.accountModelAnimSpinner.setAlpha(enabled ? 1.0f : 0.4f);
        }
        if (this.accountModelAnimRow != null) {
            this.accountModelAnimRow.setAlpha(enabled ? 1.0f : 0.4f);
        }
    }

    /**
     * ★★★ 1.5.0：注册动作下拉的选中监听。
     *
     * <p>★ 必须**晚于** setSelection() 调用（回填在 onCreate 里做），
     * 否则一进页面就会回调一次 onItemSelected、白白写一次磁盘。
     */
    private void registerAccountModelAnimListener() {
        if (this.accountModelAnimSpinner == null) {
            return;
        }
        this.accountModelAnimSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // ★ 1.5.0：主界面人物已换成 glTF 骨骼动画，选项是 4 个**模型内真实存在的 clip**
                //   （idle + idle_sub_1/2/3），不再是老的程序化模拟动作。
                //   这里的 position 就是 clip 序号 0~3，MainUI 会据此调 playAnimation()。
                if (position < 0 || position > 3) {
                    return;
                }
                // ★ 值没变就不写盘（进页面回填时可能触发一次）
                if (ExteriorSettingUI.this.activity.launcherSetting.accountModelAnim == position) {
                    return;
                }
                ExteriorSettingUI.this.activity.launcherSetting.accountModelAnim = position;
                GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}

