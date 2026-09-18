/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Dialog
 *  android.content.Context
 *  android.content.Intent
 *  android.content.res.AssetManager
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.net.Uri
 *  android.os.Bundle
 *  android.os.Handler
 *  android.os.Message
 *  android.text.method.LinkMovementMethod
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.animation.Animation
 *  android.view.animation.Interpolator
 *  android.view.animation.LinearInterpolator
 *  android.view.animation.RotateAnimation
 *  android.widget.AdapterView
 *  android.widget.AdapterView$OnItemSelectedListener
 *  android.widget.Button
 *  android.widget.EditText
 *  android.widget.ImageButton
 *  android.widget.ImageView
 *  android.widget.LinearLayout
 *  android.widget.ProgressBar
 *  android.widget.Spinner
 *  android.widget.SpinnerAdapter
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.annotation.NonNull
 *  com.google.android.material.tabs.TabLayout
 *  com.google.android.material.tabs.TabLayout$OnTabSelectedListener
 *  com.google.android.material.tabs.TabLayout$Tab
 */
package com.qcl.launcher.launcher.dialogs.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.material.tabs.TabLayout;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.AuthInfo;
import com.qcl.launcher.auth.AuthenticationException;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.auth.microsoft.MicrosoftLoginActivity;
import com.qcl.launcher.auth.microsoft.Msa;
import com.qcl.launcher.auth.yggdrasil.GameProfile;
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
import com.qcl.launcher.auth.yggdrasil.YggdrasilService;
import com.qcl.launcher.auth.yggdrasil.YggdrasilSession;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.account.SelectProfileDialog;
import com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog;
import com.qcl.launcher.launcher.list.account.server.AuthlibInjectorServerSpinnerAdapter;
import com.qcl.launcher.launcher.setting.InitializeSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.skin.utils.Avatar;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.UUIDTypeAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.qcl.launcher.R;
public class AddAccountDialog
extends Dialog
implements View.OnClickListener,
TabLayout.OnTabSelectedListener,
AdapterView.OnItemSelectedListener {
    private MainActivity activity;
    private AddAccountCallback callback;
    private TabLayout tabLayout;
    private LinearLayout offlineLayout;
    private LinearLayout microsoftLayout;
    private LinearLayout externalLayout;
    private Button login;
    private Button cancel;
    private ProgressBar progressBar;
    private EditText editName;
    private EditText editUUID;
    private TextView purchaseLink;
    private LinearLayout showAdvanceSetting;
    private ImageView spinView;
    private LinearLayout editUUIDLayout;
    private LinearLayout hintLayout;
    private TextView accountSettingLink;
    private TextView helpLink;
    private TextView mPurchaseLink;
    private Spinner editServer;
    private TextView signUp;
    private ImageButton addServer;
    private EditText editEmail;
    private EditText editPassword;
    private AuthlibInjectorServerSpinnerAdapter serverListAdapter;
    private String signUpUrl;
    private AuthlibInjectorServer authlibInjectorServer;
    private Account account;
    public static final String NIDE_8_AUTH_SIGN_UP_PAGE = "https://login.mc-user.com:233/";
    @SuppressLint(value={"HandlerLeak"})
    public final Handler loginHandler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public AddAccountDialog(@NonNull Context context, MainActivity activity, AddAccountCallback callback) {
        super(context);
        this.activity = activity;
        this.callback = callback;
        this.setContentView(R.layout.dialog_add_account);
        this.setCancelable(false);
        this.init();
    }

    private void init() {
        this.tabLayout = (TabLayout)this.findViewById(R.id.add_account_tab);
        this.offlineLayout = (LinearLayout)this.findViewById(R.id.offline_layout);
        this.microsoftLayout = (LinearLayout)this.findViewById(R.id.microsoft_layout);
        this.externalLayout = (LinearLayout)this.findViewById(R.id.external_layout);
        this.tabLayout.addOnTabSelectedListener((TabLayout.OnTabSelectedListener)this);
        this.tabLayout.selectTab(this.tabLayout.getTabAt(0));
        this.login = (Button)this.findViewById(R.id.login);
        this.cancel = (Button)this.findViewById(R.id.cancel_login);
        this.progressBar = (ProgressBar)this.findViewById(R.id.login_progress);
        this.login.setOnClickListener((View.OnClickListener)this);
        this.cancel.setOnClickListener((View.OnClickListener)this);
        this.editName = (EditText)this.findViewById(R.id.edit_user_name);
        this.editUUID = (EditText)this.findViewById(R.id.edit_uuid);
        this.purchaseLink = (TextView)this.findViewById(R.id.purchase_link);
        this.purchaseLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.showAdvanceSetting = (LinearLayout)this.findViewById(R.id.show_advance_setting);
        this.showAdvanceSetting.setOnClickListener((View.OnClickListener)this);
        this.spinView = (ImageView)this.findViewById(R.id.spin_view);
        this.editUUIDLayout = (LinearLayout)this.findViewById(R.id.edit_uuid_layout);
        this.hintLayout = (LinearLayout)this.findViewById(R.id.hint_layout);
        this.accountSettingLink = (TextView)this.findViewById(R.id.setting_link);
        this.helpLink = (TextView)this.findViewById(R.id.help_link);
        this.mPurchaseLink = (TextView)this.findViewById(R.id.m_purchase_link);
        this.accountSettingLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.helpLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.mPurchaseLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.editServer = (Spinner)this.findViewById(R.id.edit_server);
        this.signUp = (TextView)this.findViewById(R.id.sign_up);
        this.addServer = (ImageButton)this.findViewById(R.id.add_server);
        this.editEmail = (EditText)this.findViewById(R.id.edit_email);
        this.editPassword = (EditText)this.findViewById(R.id.edit_password);
        this.signUp.setOnClickListener((View.OnClickListener)this);
        this.addServer.setOnClickListener((View.OnClickListener)this);
        ArrayList<AuthlibInjectorServer> authlibInjectorServers = InitializeSetting.initializeAuthlibInjectorServer(this.getContext());
        this.serverListAdapter = new AuthlibInjectorServerSpinnerAdapter(this.getContext(), authlibInjectorServers);
        this.editServer.setAdapter((SpinnerAdapter)this.serverListAdapter);
        this.editServer.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        if (authlibInjectorServers.size() == 0) {
            this.signUp.setVisibility(8);
        } else {
            this.editServer.setSelection(0);
        }
    }

    public void onClick(View view) {
        if (view == this.login) {
            if (this.offlineLayout.getVisibility() == 0) {
                if (this.editName.getText().toString().equals("")) {
                    Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_offline_account_empty_warn), (int)0).show();
                } else {
                    AssetManager manager = this.getContext().getAssets();
                    String skinTexture = "";
                    try {
                        InputStream inputStream = manager.open("img/alex.png");
                        Bitmap bitmap = BitmapFactory.decodeStream((InputStream)inputStream);
                        skinTexture = Avatar.bitmapToString(bitmap);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    Account account = new Account(1, "", "", "mojang", "0", this.editName.getText().toString(), this.editUUID.getText().toString().equals("") ? UUID.randomUUID().toString() : this.editUUID.getText().toString(), UUIDTypeAdapter.fromUUID(UUID.randomUUID()), "", "", "", skinTexture);
                    this.callback.onAccountAdd(account);
                    this.dismiss();
                }
            }
            if (this.microsoftLayout.getVisibility() == 0) {
                Intent i = new Intent(this.getContext(), MicrosoftLoginActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBoolean("fullscreen", this.activity.launcherSetting.fullscreen);
                i.putExtras(bundle);
                this.activity.startActivityForResult(i, 2000);
            }
            if (this.externalLayout.getVisibility() == 0) {
                if (this.authlibInjectorServer == null) {
                    Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_authlib_injector_account_server_warn), (int)0).show();
                } else if (this.editEmail.getText().toString().equals("") || this.editPassword.getText().toString().equals("")) {
                    Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_authlib_injector_account_empty_warn), (int)0).show();
                } else {
                    String email = this.editEmail.getText().toString();
                    String password = this.editPassword.getText().toString();
                    boolean isNide = this.authlibInjectorServer.getUrl().startsWith("https://auth.mc-user.com:233/");
                    new Thread(() -> {
                        this.loginHandler.post(() -> {
                            this.progressBar.setVisibility(0);
                            this.login.setVisibility(8);
                            this.cancel.setEnabled(false);
                            this.tabLayout.setEnabled(false);
                        });
                        YggdrasilService yggdrasilService = this.authlibInjectorServer.getYggdrasilService();
                        try {
                            YggdrasilSession yggdrasilSession = yggdrasilService.authenticate(email, password, UUID.randomUUID().toString());
                            if (yggdrasilSession.getAvailableProfiles().size() > 1) {
                                ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
                                for (GameProfile gameProfile : yggdrasilSession.getAvailableProfiles()) {
                                    if (yggdrasilService.getCompleteGameProfile(gameProfile.getId()).isPresent() && YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(gameProfile.getId()).get()).isPresent()) {
                                        Map<TextureType, Texture> map = YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(gameProfile.getId()).get()).get();
                                        Texture texture = map.get((Object)TextureType.SKIN);
                                        if (texture == null) {
                                            AssetManager manager = this.getContext().getAssets();
                                            InputStream inputStream = manager.open("img/alex.png");
                                            Bitmap skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                            bitmaps.add(skin);
                                            continue;
                                        }
                                        String u = texture.getUrl();
                                        if (!u.startsWith("https")) {
                                            u = u.replaceFirst("http", "https");
                                        }
                                        URL url = new URL(u);
                                        HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                                        httpURLConnection.setDoInput(true);
                                        httpURLConnection.connect();
                                        InputStream inputStream = httpURLConnection.getInputStream();
                                        Bitmap skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                        bitmaps.add(skin);
                                        continue;
                                    }
                                    AssetManager manager = this.getContext().getAssets();
                                    InputStream inputStream = manager.open("img/alex.png");
                                    Bitmap skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                    bitmaps.add(skin);
                                }
                                this.loginHandler.post(() -> {
                                    SelectProfileDialog dialog = new SelectProfileDialog(this.getContext(), yggdrasilService, yggdrasilSession, email, password, this.authlibInjectorServer.getUrl(), bitmaps, account -> this.callback.onAccountAdd(account), isNide);
                                    dialog.show();
                                    this.dismiss();
                                });
                            } else if (yggdrasilSession.getAvailableProfiles().size() == 1) {
                                Bitmap skin;
                                AuthInfo authInfo = yggdrasilSession.toAuthInfo();
                                if (yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).isPresent() && YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).get()).isPresent()) {
                                    Map<TextureType, Texture> map = YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).get()).get();
                                    Texture texture = map.get((Object)TextureType.SKIN);
                                    if (texture == null) {
                                        AssetManager manager = this.getContext().getAssets();
                                        InputStream inputStream = manager.open("img/alex.png");
                                        skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                    } else {
                                        String u = texture.getUrl();
                                        if (!u.startsWith("https")) {
                                            u = u.replaceFirst("http", "https");
                                        }
                                        URL url = new URL(u);
                                        HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                                        httpURLConnection.setDoInput(true);
                                        httpURLConnection.connect();
                                        InputStream inputStream = httpURLConnection.getInputStream();
                                        skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                    }
                                } else {
                                    AssetManager manager = this.getContext().getAssets();
                                    InputStream inputStream = manager.open("img/alex.png");
                                    skin = BitmapFactory.decodeStream((InputStream)inputStream);
                                }
                                this.loginHandler.post(() -> {
                                    String skinTexture = Avatar.bitmapToString(skin);
                                    this.account = new Account(isNide ? 5 : 4, email, password, "mojang", "0", yggdrasilSession.getSelectedProfile().getName(), authInfo.getUUID().toString(), authInfo.getAccessToken(), yggdrasilSession.getClientToken(), "", this.authlibInjectorServer.getUrl(), skinTexture);
                                    this.callback.onAccountAdd(this.account);
                                    this.dismiss();
                                });
                            } else {
                                this.loginHandler.post(() -> Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_authlib_injector_account_none), (int)0).show());
                            }
                        }
                        catch (AuthenticationException | IOException e) {
                            e.printStackTrace();
                            this.loginHandler.post(() -> Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_authlib_injector_account_failed), (int)0).show());
                        }
                        this.loginHandler.post(() -> {
                            this.progressBar.setVisibility(8);
                            this.login.setVisibility(0);
                            this.cancel.setEnabled(true);
                            this.tabLayout.setEnabled(true);
                        });
                    }).start();
                }
            }
        }
        if (view == this.cancel) {
            this.dismiss();
            this.callback.onCancel();
        }
        if (view == this.showAdvanceSetting) {
            RotateAnimation animation;
            if (this.editUUIDLayout.getVisibility() == 8) {
                this.editUUIDLayout.setVisibility(0);
                this.hintLayout.setVisibility(0);
                animation = new RotateAnimation(0.0f, 180.0f, 1, 0.5f, 1, 0.5f);
                animation.setDuration(30L);
                animation.setInterpolator((Interpolator)new LinearInterpolator());
                animation.setRepeatMode(2);
                animation.setFillAfter(true);
                this.spinView.startAnimation((Animation)animation);
            } else {
                this.editUUIDLayout.setVisibility(8);
                this.hintLayout.setVisibility(8);
                animation = new RotateAnimation(180.0f, 0.0f, 1, 0.5f, 1, 0.5f);
                animation.setDuration(30L);
                animation.setInterpolator((Interpolator)new LinearInterpolator());
                animation.setRepeatMode(2);
                animation.setFillAfter(true);
                this.spinView.startAnimation((Animation)animation);
            }
        }
        if (view == this.signUp) {
            Uri uri = Uri.parse((String)this.signUpUrl);
            Intent intent = new Intent("android.intent.action.VIEW", uri);
            this.getContext().startActivity(intent);
        }
        if (view == this.addServer) {
            SelectServerTypeDialog dialog = new SelectServerTypeDialog(this.getContext(), server -> {
                if (!this.activity.uiManager.accountUI.serverList.contains(server)) {
                    this.activity.uiManager.accountUI.serverList.add(server);
                    this.activity.uiManager.accountUI.serverListAdapter.notifyDataSetChanged();
                    GsonUtils.saveServer(this.activity.uiManager.accountUI.serverList, AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json");
                    ArrayList<AuthlibInjectorServer> authlibInjectorServers = InitializeSetting.initializeAuthlibInjectorServer(this.getContext());
                    this.serverListAdapter = new AuthlibInjectorServerSpinnerAdapter(this.getContext(), authlibInjectorServers);
                    this.editServer.setAdapter((SpinnerAdapter)this.serverListAdapter);
                }
            });
            dialog.show();
        }
    }

    public void login(Intent intent) {
        Uri data = null;
        if (intent != null) {
            data = intent.getData();
        }
        if (data != null && data.getScheme().equals("ms-xal-00000000402b5328") && data.getHost().equals("auth")) {
            String error = data.getQueryParameter("error");
            String error_description = data.getQueryParameter("error_description");
            if (error != null) {
                if (!error_description.startsWith("The user has denied access to the scope requested by the client application")) {
                    Toast.makeText((Context)this.getContext(), (CharSequence)("Error: " + error + ": " + error_description), (int)0).show();
                }
            } else {
                String code = data.getQueryParameter("code");
                new Thread(() -> {
                    this.loginHandler.post(() -> {
                        this.progressBar.setVisibility(0);
                        this.login.setVisibility(8);
                        this.cancel.setEnabled(false);
                        this.tabLayout.setEnabled(false);
                    });
                    try {
                        final Msa msa = new Msa(false, code);
                        if (msa.doesOwnGame) {
                            Bitmap skin;
                            Msa.MinecraftProfileResponse minecraftProfile = Msa.getMinecraftProfile(msa.tokenType, msa.mcToken);
                            Map<TextureType, Texture> map = Msa.getTextures(minecraftProfile).get();
                            Texture texture = map.get((Object)TextureType.SKIN);
                            if (texture == null) {
                                AssetManager manager = this.getContext().getAssets();
                                InputStream inputStream = manager.open("img/alex.png");
                                skin = BitmapFactory.decodeStream((InputStream)inputStream);
                            } else {
                                String u = texture.getUrl();
                                if (!u.startsWith("https")) {
                                    u = u.replaceFirst("http", "https");
                                }
                                URL url = new URL(u);
                                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                                httpURLConnection.setDoInput(true);
                                httpURLConnection.connect();
                                InputStream inputStream = httpURLConnection.getInputStream();
                                skin = BitmapFactory.decodeStream((InputStream)inputStream);
                            }
                            this.loginHandler.post(new Runnable(){

                                @Override
                                public void run() {
                                    String skinTexture = Avatar.bitmapToString(skin);
                                    AddAccountDialog.this.account = new Account(3, "", "", "mojang", "0", msa.mcName, msa.mcUuid, msa.mcToken, "00000000-0000-0000-0000-000000000000", msa.msRefreshToken, "", skinTexture);
                                    AddAccountDialog.this.callback.onAccountAdd(AddAccountDialog.this.account);
                                    AddAccountDialog.this.dismiss();
                                }
                            });
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    this.loginHandler.post(() -> {
                        this.progressBar.setVisibility(8);
                        this.login.setVisibility(0);
                        this.cancel.setEnabled(true);
                        this.tabLayout.setEnabled(true);
                    });
                }).start();
            }
        }
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if (adapterView == this.editServer) {
            this.authlibInjectorServer = (AuthlibInjectorServer)this.serverListAdapter.getItem(i);
            this.signUpUrl = this.authlibInjectorServer.getLinks().get("register");
            if (this.authlibInjectorServer.getUrl().startsWith("https://auth.mc-user.com:233/")) {
                this.signUpUrl = NIDE_8_AUTH_SIGN_UP_PAGE + this.authlibInjectorServer.getUrl().substring(this.authlibInjectorServer.getUrl().length() - 33);
            }
            if (this.signUpUrl == null) {
                this.signUp.setVisibility(8);
            } else {
                this.signUp.setVisibility(0);
            }
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public void onTabSelected(TabLayout.Tab tab) {
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_offline))) {
            this.offlineLayout.setVisibility(0);
        }
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_microsoft))) {
            this.microsoftLayout.setVisibility(0);
        }
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_external))) {
            this.externalLayout.setVisibility(0);
        }
    }

    public void onTabUnselected(TabLayout.Tab tab) {
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_offline))) {
            this.offlineLayout.setVisibility(8);
        }
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_microsoft))) {
            this.microsoftLayout.setVisibility(8);
        }
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_external))) {
            this.externalLayout.setVisibility(8);
        }
    }

    public void onTabReselected(TabLayout.Tab tab) {
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_offline))) {
            this.offlineLayout.setVisibility(0);
        }
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_microsoft))) {
            this.microsoftLayout.setVisibility(0);
        }
        if (Objects.requireNonNull(tab.getText()).toString().equals(this.getContext().getString(R.string.dialog_add_account_type_external))) {
            this.externalLayout.setVisibility(0);
        }
    }

    public static interface AddAccountCallback {
        public void onAccountAdd(Account var1);

        public void onCancel();
    }
}

