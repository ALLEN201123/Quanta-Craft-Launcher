/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.content.Intent
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.net.Uri
 *  android.os.Environment
 *  android.os.Handler
 *  android.text.Editable
 *  android.text.TextWatcher
 *  android.util.Log
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.EditText
 *  android.widget.ImageButton
 *  android.widget.LinearLayout
 *  android.widget.RadioButton
 *  android.widget.TextView
 *  com.tungsten.filepicker.Constants$SELECTION_MODES
 *  com.tungsten.filepicker.FileChooser
 */
package com.qcl.launcher.launcher.dialogs.account;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.offline.OfflineSkinSetting;
import com.qcl.launcher.auth.offline.SkinJson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.skin.gltf.SkinRenderer;
import com.qcl.launcher.skin.gltf.SkinViewer;
import com.qcl.launcher.skin.utils.Avatar;
import com.qcl.launcher.skin.utils.InvalidSkinException;
import com.qcl.launcher.skin.utils.NormalizedSkin;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import com.qcl.launcher.utils.string.StringUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.qcl.launcher.R;
public class SkinPreviewDialog
implements View.OnClickListener {
    private static final int SELECT_SKIN_REQUEST = 9300;
    private static final int SELECT_CAPE_REQUEST = 9400;
    private LinearLayout dialog;
    private Button fakeBackground;
    private final Context context;
    private final MainActivity activity;
    private final Account account;
    private final OfflineSkinCallback callback;
    private LinearLayout skinParentView;
    private Button positive;
    private Button negative;
    // ★★★ 1.5.0：彻底弃用老 `MinecraftSkinRenderer + GameCharacter + SkinGLSurfaceView`，
    //   **全站改用主界面那套 glTF 管线**（SkinViewer + SkinRenderer），照 FCL 的做法。
    //   原因：老 GameCharacter 只有 `setWalkSwing(sin(t)*幅度)` 一个动作，
    //   **没有 FCL 的「8~15 秒随机插播待机变体」状态机** ⇒ 对话框里人物像根木头，
    //   而主界面是 glTF 所以会动 ⇒ 两处观感不一致（用户实测报的就是这个）。
    //   换成同一套管线后，对话框与主界面人物**表现完全一致**。
    private final SkinRenderer renderer;
    private SkinViewer skinViewer;
    private final Handler handler;
    private RadioButton defaultSkin;
    private RadioButton steveSkin;
    private RadioButton alexSkin;
    private RadioButton localSkin;
    private RadioButton littleSkin;
    private RadioButton blessingSkin;
    private LinearLayout localSkinLayout;
    private EditText editSkinPath;
    private EditText editCapePath;
    private ImageButton selectSkin;
    private ImageButton selectCape;
    private LinearLayout littleSkinLayout;
    private LinearLayout blessingSkinLayout;
    private EditText editServer;
    private TextView littleSkinUrl;
    private OfflineSkinSetting offlineSkinSetting;
    private static SkinPreviewDialog skinPreviewDialog;

    public SkinPreviewDialog(Context context, MainActivity activity, Account account, OfflineSkinCallback callback) {
        this.context = context;
        this.activity = activity;
        this.account = account;
        this.callback = callback;
        this.handler = new Handler();
        // glTF 管线的待机随机变体由 SkinRenderer 内部状态机自己驱动（8~15 秒随机），
        // 不再需要老代码用 Handler 手动 SetRunning(true/false) 切换 ⇒ 两个 runnable 已删。
        this.renderer = new SkinRenderer(context);
        skinPreviewDialog = this;
        this.init();
    }

    public static SkinPreviewDialog getInstance() {
        return skinPreviewDialog;
    }

    public void show() {
        this.activity.dialogMode = true;
        this.dialog = (LinearLayout)this.activity.findViewById(R.id.dialog_offline_skin);
        this.fakeBackground = (Button)this.activity.findViewById(R.id.fake_dialog_background);
        this.dialog.setVisibility(0);
        this.fakeBackground.setVisibility(0);
        // glTF 的 TextureView 必须在 attach 后 resume 渲染线程
        if (this.skinViewer != null) {
            this.skinViewer.onResume();
        }
    }

    public void dismiss() {
        this.activity.dialogMode = false;
        // ★ 1.5.0：glTF 的 TextureView 必须 pause + 摘掉，否则 EGL 渲染线程会一直跑
        if (this.skinViewer != null) {
            this.skinViewer.onPause();
        }
        this.skinParentView.removeAllViews();
        this.dialog.setVisibility(8);
        this.fakeBackground.setVisibility(8);
        skinPreviewDialog = null;
    }

    @SuppressLint(value={"ClickableViewAccessibility"})
    private void init() {
        this.skinParentView = (LinearLayout)this.activity.findViewById(R.id.skin_parent_view);
        // ★★★ 1.5.0：人物视图从 SkinGLSurfaceView 换成 glTF 的 SkinViewer（TextureView + EGL）。
        //   ★ SkinViewer 是 TextureView，**不能 setZOrderOnTop**（那是 GLSurfaceView 独有的 API，
        //     且 TextureView 没有 SurfaceHolder）⇒ 老代码那几行 setEGLConfigChooser / getHolder()
        //     / setZOrderOnTop 全部不能留，否则编译直接不过。
        try {
            skinViewer = new SkinViewer(this.context);
            this.skinParentView.addView(skinViewer, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            skinViewer.setRenderer(this.renderer,
                    this.context.getResources().getDisplayMetrics().density);
        } catch (Throwable t) {
            // glTF 起不来也不能让对话框打不开：人物位置留空，其余功能照常
            t.printStackTrace();
            skinViewer = null;
        }
        this.positive = (Button)this.activity.findViewById(R.id.edit_skin_positive);
        this.negative = (Button)this.activity.findViewById(R.id.cancel_edit_skin);
        // ★★★ 1.5.0：确定 / 取消按钮染成「深色底 + 浅色字」，底色跟随玩家主题色
        try {
            com.qcl.launcher.utils.string.SkinDialogUtils.tintButton(this.context, this.positive,
                    (this.activity != null && this.activity.launcherSetting != null)
                            ? this.activity.launcherSetting.launcherTheme : null);
            com.qcl.launcher.utils.string.SkinDialogUtils.tintButton(this.context, this.negative,
                    (this.activity != null && this.activity.launcherSetting != null)
                            ? this.activity.launcherSetting.launcherTheme : null);
        } catch (Throwable ignored) {
        }
        this.littleSkinUrl = (TextView)this.activity.findViewById(R.id.little_skin_url);
        this.littleSkinUrl.setOnClickListener((View.OnClickListener)this);
        this.defaultSkin = (RadioButton)this.activity.findViewById(R.id.check_skin_default);
        this.steveSkin = (RadioButton)this.activity.findViewById(R.id.check_skin_steve);
        this.alexSkin = (RadioButton)this.activity.findViewById(R.id.check_skin_alex);
        this.localSkin = (RadioButton)this.activity.findViewById(R.id.check_skin_local);
        this.littleSkin = (RadioButton)this.activity.findViewById(R.id.check_skin_little);
        this.blessingSkin = (RadioButton)this.activity.findViewById(R.id.check_skin_blessing);
        this.defaultSkin.setOnClickListener((View.OnClickListener)this);
        this.steveSkin.setOnClickListener((View.OnClickListener)this);
        this.alexSkin.setOnClickListener((View.OnClickListener)this);
        this.localSkin.setOnClickListener((View.OnClickListener)this);
        this.littleSkin.setOnClickListener((View.OnClickListener)this);
        this.blessingSkin.setOnClickListener((View.OnClickListener)this);
        this.localSkinLayout = (LinearLayout)this.activity.findViewById(R.id.local_skin_layout);
        this.editSkinPath = (EditText)this.activity.findViewById(R.id.edit_skin_path);
        this.selectSkin = (ImageButton)this.activity.findViewById(R.id.select_skin);
        this.editCapePath = (EditText)this.activity.findViewById(R.id.edit_cape_path);
        this.selectCape = (ImageButton)this.activity.findViewById(R.id.select_cape);
        this.littleSkinLayout = (LinearLayout)this.activity.findViewById(R.id.little_skin_layout);
        this.blessingSkinLayout = (LinearLayout)this.activity.findViewById(R.id.blessing_skin_layout);
        this.editServer = (EditText)this.activity.findViewById(R.id.edit_blessing_server);
        this.editSkinPath.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                if (((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.type == 3) {
                    ((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath = SkinPreviewDialog.this.editSkinPath.getText().toString();
                    Bitmap skin = new File(((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).exists() ? (BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).getWidth() == 64 && (BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).getHeight() == 32 || BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).getHeight() == 64) ? BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath) : Avatar.getBitmapFromRes(SkinPreviewDialog.this.context, R.drawable.skin_steve)) : Avatar.getBitmapFromRes(SkinPreviewDialog.this.context, R.drawable.skin_steve);
                    Bitmap cape = new File(((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath).exists() ? (BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath).getWidth() == 64 && BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath).getHeight() == 32 ? BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath) : null) : null;
                    try {
                        NormalizedSkin normalizedSkin = new NormalizedSkin(skin);
                        ((SkinPreviewDialog)SkinPreviewDialog.this).renderer.updateTexture(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture(), cape, normalizedSkin.isSlim());
                    }
                    catch (InvalidSkinException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        this.editCapePath.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                if (((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.type == 3) {
                    ((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath = SkinPreviewDialog.this.editCapePath.getText().toString();
                    Bitmap skin = new File(((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).exists() ? (BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).getWidth() == 64 && (BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).getHeight() == 32 || BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath).getHeight() == 64) ? BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.skinPath) : Avatar.getBitmapFromRes(SkinPreviewDialog.this.context, R.drawable.skin_steve)) : Avatar.getBitmapFromRes(SkinPreviewDialog.this.context, R.drawable.skin_steve);
                    Bitmap cape = new File(((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath).exists() ? (BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath).getWidth() == 64 && BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath).getHeight() == 32 ? BitmapFactory.decodeFile((String)((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.capePath) : null) : null;
                    try {
                        NormalizedSkin normalizedSkin = new NormalizedSkin(skin);
                        ((SkinPreviewDialog)SkinPreviewDialog.this).renderer.updateTexture(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture(), cape, normalizedSkin.isSlim());
                    }
                    catch (InvalidSkinException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        this.selectSkin.setOnClickListener((View.OnClickListener)this);
        this.selectCape.setOnClickListener((View.OnClickListener)this);
        this.editServer.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                ((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.server = SkinPreviewDialog.this.editServer.getText().toString();
                String cslApi = ((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.server.startsWith("http://") ? ((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.server.replace("http://", "https://") : ((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.server;
                URL u = null;
                try {
                    u = new URL(StringUtils.removeSuffix(cslApi, "/") + "/" + ((SkinPreviewDialog)SkinPreviewDialog.this).account.auth_player_name + ".json");
                    Log.e((String)"cslApi", (String)(StringUtils.removeSuffix(cslApi, "/") + "/" + ((SkinPreviewDialog)SkinPreviewDialog.this).account.auth_player_name + ".json"));
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                if (u != null) {
                    new Thread(() -> {
                        try {
                            String resultText = NetworkUtils.doGet(NetworkUtils.toURL(StringUtils.removeSuffix(cslApi, "/") + "/" + ((SkinPreviewDialog)SkinPreviewDialog.this).account.auth_player_name + ".json"));
                            SkinJson result = (SkinJson)JsonUtils.GSON.fromJson(resultText, SkinJson.class);
                            if (result != null && result.hasSkin()) {
                                Bitmap cape;
                                InputStream inputStream;
                                HttpURLConnection httpURLConnection;
                                URL url;
                                Bitmap skin;
                                if (result.getHash() == null) {
                                    skin = Avatar.getBitmapFromRes(SkinPreviewDialog.this.context, R.drawable.skin_steve);
                                } else {
                                    url = new URL(StringUtils.removeSuffix(cslApi, "/") + "/textures/" + result.getHash());
                                    httpURLConnection = (HttpURLConnection)url.openConnection();
                                    httpURLConnection.setDoInput(true);
                                    httpURLConnection.connect();
                                    inputStream = httpURLConnection.getInputStream();
                                    skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                }
                                if (result.getCapeHash() == null) {
                                    cape = null;
                                } else {
                                    url = new URL(StringUtils.removeSuffix(cslApi, "/") + "/textures/" + result.getCapeHash());
                                    httpURLConnection = (HttpURLConnection)url.openConnection();
                                    httpURLConnection.setDoInput(true);
                                    httpURLConnection.connect();
                                    inputStream = httpURLConnection.getInputStream();
                                    cape = BitmapFactory.decodeStream((InputStream)inputStream);
                                }
                                SkinPreviewDialog.this.activity.runOnUiThread(() -> {
                                    if (((SkinPreviewDialog)SkinPreviewDialog.this).offlineSkinSetting.type == 5) {
                                        try {
                                            NormalizedSkin normalizedSkin = new NormalizedSkin(skin);
                                            SkinPreviewDialog.this.renderer.updateTexture(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture(), cape, normalizedSkin.isSlim());
                                        }
                                        catch (InvalidSkinException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            }
        });
        if (this.account.offlineSkinSetting == null) {
            this.offlineSkinSetting = new OfflineSkinSetting(this.context);
        } else {
            try {
                this.offlineSkinSetting = (OfflineSkinSetting)this.account.offlineSkinSetting.clone();
            }
            catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
        this.renderer.updateTexture(Avatar.stringToBitmap(this.account.texture), null);
        switch (this.offlineSkinSetting.type) {
            case 0: {
                this.switchToDefault();
                break;
            }
            case 1: {
                this.switchToSteve();
                break;
            }
            case 2: {
                this.switchToAlex();
                break;
            }
            case 3: {
                this.switchToLocal();
                break;
            }
            case 4: {
                this.switchToLittleSkin();
                break;
            }
            case 5: {
                this.switchToBlessingSkin();
            }
        }
        this.positive.setOnClickListener((View.OnClickListener)this);
        this.negative.setOnClickListener((View.OnClickListener)this);
    }

    public void onClick(View v) {
        Intent intent;
        if (v == this.positive) {
            this.callback.onPositive(this.offlineSkinSetting);
            this.dismiss();
        }
        if (v == this.negative) {
            this.dismiss();
        }
        if (v == this.littleSkinUrl) {
            Uri uri = Uri.parse((String)"https://littleskin.cn/");
            Intent intent2 = new Intent("android.intent.action.VIEW", uri);
            this.context.startActivity(intent2);
        }
        if (v == this.defaultSkin) {
            this.switchToDefault();
        }
        if (v == this.steveSkin) {
            this.switchToSteve();
        }
        if (v == this.alexSkin) {
            this.switchToAlex();
        }
        if (v == this.localSkin) {
            this.switchToLocal();
        }
        if (v == this.littleSkin) {
            this.switchToLittleSkin();
        }
        if (v == this.blessingSkin) {
            this.switchToBlessingSkin();
        }
        if (v == this.selectSkin) {
            intent = new Intent(this.context, FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "png");
            intent.putExtra("INITIAL_DIRECTORY", Environment.getExternalStorageDirectory().getAbsolutePath());
            this.activity.startActivityForResult(intent, 9300);
        }
        if (v == this.selectCape) {
            intent = new Intent(this.context, FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "png");
            intent.putExtra("INITIAL_DIRECTORY", Environment.getExternalStorageDirectory().getAbsolutePath());
            this.activity.startActivityForResult(intent, 9400);
        }
    }

    private void switchToDefault() {
        this.defaultSkin.setChecked(true);
        this.steveSkin.setChecked(false);
        this.alexSkin.setChecked(false);
        this.localSkin.setChecked(false);
        this.littleSkin.setChecked(false);
        this.blessingSkin.setChecked(false);
        this.localSkinLayout.setVisibility(8);
        this.littleSkinLayout.setVisibility(8);
        this.blessingSkinLayout.setVisibility(8);
        this.renderer.updateTexture(Avatar.getBitmapFromRes(this.context, R.drawable.skin_steve), null, false);
        this.offlineSkinSetting.type = 0;
    }

    private void switchToSteve() {
        this.defaultSkin.setChecked(false);
        this.steveSkin.setChecked(true);
        this.alexSkin.setChecked(false);
        this.localSkin.setChecked(false);
        this.littleSkin.setChecked(false);
        this.blessingSkin.setChecked(false);
        this.localSkinLayout.setVisibility(8);
        this.littleSkinLayout.setVisibility(8);
        this.blessingSkinLayout.setVisibility(8);
        this.renderer.updateTexture(Avatar.getBitmapFromRes(this.context, R.drawable.skin_steve), null, false);
        this.offlineSkinSetting.type = 1;
    }

    private void switchToAlex() {
        this.defaultSkin.setChecked(false);
        this.steveSkin.setChecked(false);
        this.alexSkin.setChecked(true);
        this.localSkin.setChecked(false);
        this.littleSkin.setChecked(false);
        this.blessingSkin.setChecked(false);
        this.localSkinLayout.setVisibility(8);
        this.littleSkinLayout.setVisibility(8);
        this.blessingSkinLayout.setVisibility(8);
        this.renderer.updateTexture(Avatar.getBitmapFromRes(this.context, R.drawable.skin_alex), null, true);
        this.offlineSkinSetting.type = 2;
    }

    private void switchToLocal() {
        this.defaultSkin.setChecked(false);
        this.steveSkin.setChecked(false);
        this.alexSkin.setChecked(false);
        this.localSkin.setChecked(true);
        this.littleSkin.setChecked(false);
        this.blessingSkin.setChecked(false);
        this.localSkinLayout.setVisibility(0);
        this.littleSkinLayout.setVisibility(8);
        this.blessingSkinLayout.setVisibility(8);
        this.offlineSkinSetting.type = 3;
        this.editSkinPath.setText((CharSequence)this.offlineSkinSetting.skinPath);
        this.editCapePath.setText((CharSequence)this.offlineSkinSetting.capePath);
    }

    private void switchToLittleSkin() {
        this.defaultSkin.setChecked(false);
        this.steveSkin.setChecked(false);
        this.alexSkin.setChecked(false);
        this.localSkin.setChecked(false);
        this.littleSkin.setChecked(true);
        this.blessingSkin.setChecked(false);
        this.localSkinLayout.setVisibility(8);
        this.littleSkinLayout.setVisibility(0);
        this.blessingSkinLayout.setVisibility(8);
        this.offlineSkinSetting.type = 4;
        new Thread(() -> {
            try {
                String resultText = NetworkUtils.doGet(NetworkUtils.toURL("https://mcskin.littleservice.cn/" + this.account.auth_player_name + ".json"));
                SkinJson result = (SkinJson)JsonUtils.GSON.fromJson(resultText, SkinJson.class);
                if (result != null && result.hasSkin()) {
                    Bitmap cape;
                    InputStream inputStream;
                    HttpURLConnection httpURLConnection;
                    URL url;
                    Bitmap skin;
                    if (result.getHash() == null) {
                        skin = Avatar.getBitmapFromRes(this.context, R.drawable.skin_steve);
                    } else {
                        url = new URL("https://mcskin.littleservice.cn/textures/" + result.getHash());
                        httpURLConnection = (HttpURLConnection)url.openConnection();
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.connect();
                        inputStream = httpURLConnection.getInputStream();
                        skin = BitmapFactory.decodeStream((InputStream)inputStream);
                    }
                    if (result.getCapeHash() == null) {
                        cape = null;
                    } else {
                        url = new URL("https://mcskin.littleservice.cn/textures/" + result.getCapeHash());
                        httpURLConnection = (HttpURLConnection)url.openConnection();
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.connect();
                        inputStream = httpURLConnection.getInputStream();
                        cape = BitmapFactory.decodeStream((InputStream)inputStream);
                    }
                    this.activity.runOnUiThread(() -> {
                        if (this.offlineSkinSetting.type == 4) {
                            try {
                                NormalizedSkin normalizedSkin = new NormalizedSkin(skin);
                                this.renderer.updateTexture(normalizedSkin.isOldFormat() ? normalizedSkin.getNormalizedTexture() : normalizedSkin.getOriginalTexture(), cape, normalizedSkin.isSlim());
                            }
                            catch (InvalidSkinException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void switchToBlessingSkin() {
        this.defaultSkin.setChecked(false);
        this.steveSkin.setChecked(false);
        this.alexSkin.setChecked(false);
        this.localSkin.setChecked(false);
        this.littleSkin.setChecked(false);
        this.blessingSkin.setChecked(true);
        this.localSkinLayout.setVisibility(8);
        this.littleSkinLayout.setVisibility(8);
        this.blessingSkinLayout.setVisibility(0);
        this.offlineSkinSetting.type = 5;
        this.editServer.setText((CharSequence)this.offlineSkinSetting.server);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String path;
        Uri uri;
        if (requestCode == 9300 && resultCode == -1 && data != null) {
            uri = data.getData();
            path = UriUtils.getRealPathFromUri_AboveApi19(this.context, uri);
            this.editSkinPath.setText((CharSequence)path);
        }
        if (requestCode == 9400 && resultCode == -1 && data != null) {
            uri = data.getData();
            path = UriUtils.getRealPathFromUri_AboveApi19(this.context, uri);
            this.editCapePath.setText((CharSequence)path);
        }
    }

    // ★ 1.5.0：人物视图已换成 glTF 的 SkinViewer（TextureView），暂停/恢复走它自己的方法，
        //   并且必须判空 —— glTF 起不来时 skinViewer 为 null（这时不显示人物，但对话框其余功能照常）。
    public void onPause() {
        if (this.skinViewer != null) {
            this.skinViewer.onPause();
        }
    }

    public void onResume() {
        if (this.skinViewer != null) {
            this.skinViewer.onResume();
        }
    }

    public static interface OfflineSkinCallback {
        public void onPositive(OfflineSkinSetting var1);
    }
}

