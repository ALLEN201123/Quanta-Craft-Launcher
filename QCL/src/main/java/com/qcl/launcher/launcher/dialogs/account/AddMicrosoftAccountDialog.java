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
 *  android.widget.Button
 *  android.widget.ProgressBar
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.annotation.NonNull
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
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.microsoft.MicrosoftLoginActivity;
import com.qcl.launcher.auth.microsoft.Msa;
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.skin.utils.Avatar;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import com.qcl.launcher.R;
public class AddMicrosoftAccountDialog
extends Dialog
implements View.OnClickListener {
    private MainActivity activity;
    private OnMicrosoftAccountAddListener onMicrosoftAccountAddListener;
    private TextView accountSettingLink;
    private TextView helpLink;
    private TextView purchaseLink;
    private Button login;
    private Button cancel;
    private ProgressBar progressBar;
    private Account account;
    @SuppressLint(value={"HandlerLeak"})
    public final Handler handler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public AddMicrosoftAccountDialog(@NonNull Context context, MainActivity activity, OnMicrosoftAccountAddListener onMicrosoftAccountAddListener) {
        super(context);
        this.activity = activity;
        this.onMicrosoftAccountAddListener = onMicrosoftAccountAddListener;
        this.setContentView(R.layout.dialog_add_microsoft_account);
        this.setCancelable(false);
        this.init();
    }

    private void init() {
        this.accountSettingLink = (TextView)this.findViewById(R.id.setting_link);
        this.helpLink = (TextView)this.findViewById(R.id.help_link);
        this.purchaseLink = (TextView)this.findViewById(R.id.purchase_link);
        this.accountSettingLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.helpLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.purchaseLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.login = (Button)this.findViewById(R.id.login_microsoft);
        this.cancel = (Button)this.findViewById(R.id.cancel_login_microsoft);
        // ★★★ 2026-10-06：原来的「登录」按钮直接走**设备码流程**。
        //   为什么改掉 WebView：内嵌 WebView 打开微软授权页在国内网络/部分机型上
        //   会被微软拦住（提示"此浏览器不安全"或直接空白），玩家根本登不上。
        //   设备码只要把 8 位码输进任意浏览器就行，不依赖 WebView，成功率高得多。
        //   注意：**不是再加一个按钮**（用户明确要求"直接整在那个登录上面"），
        //   而是让原来那个「登录」按钮就干这件事。
        this.progressBar = (ProgressBar)this.findViewById(R.id.login_progress);
        // ★★★ 2026-10-06 修复：这个进度圈一直在转（用户反馈"下面一个圆圈一直转"）。
        //   原因：它原本是给 WebView 登录流程用的（点了登录才显示、拿到结果隐藏），
        //   而登录现在改成设备码弹窗了，这个圈**永远不会被隐藏**，就一直空转。
        //   已经从布局里**物理删除**该 ProgressBar（用户要求"把那个圆圈给移除了"），
        //   这里保留 null 判断只是防止别处还引用它时崩溃。
        if (this.progressBar != null) {
            this.progressBar.setVisibility(8);
        }
        this.login.setOnClickListener((View.OnClickListener)this);
        this.cancel.setOnClickListener((View.OnClickListener)this);
    }

    public void onClick(View v) {
        if (v == this.login) {
            // ★★★ 2026-10-06：原来的 WebView 登录改成**设备码登录**。
            //   老代码（留着备查）：
            //     Intent i = new Intent(this.getContext(), MicrosoftLoginActivity.class);
            //     Bundle bundle = new Bundle();
            //     bundle.putBoolean("fullscreen", this.activity.launcherSetting.fullscreen);
            //     i.putExtras(bundle);
            //     this.activity.startActivityForResult(i, 2000);
            new DeviceCodeLoginDialog(this.activity, this.onMicrosoftAccountAddListener).show();
        }
        if (v == this.cancel) {
            this.dismiss();
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
                    this.handler.post(() -> {
                        this.progressBar.setVisibility(0);
                        this.login.setVisibility(8);
                        this.cancel.setEnabled(false);
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
                            this.handler.post(new Runnable(){

                                @Override
                                public void run() {
                                    String skinTexture = Avatar.bitmapToString(skin);
                                    AddMicrosoftAccountDialog.this.account = new Account(3, "", "", "mojang", "0", msa.mcName, msa.mcUuid, msa.mcToken, "00000000-0000-0000-0000-000000000000", msa.msRefreshToken, "", skinTexture);
                                    AddMicrosoftAccountDialog.this.onMicrosoftAccountAddListener.onPositive(AddMicrosoftAccountDialog.this.account);
                                    AddMicrosoftAccountDialog.this.dismiss();
                                }
                            });
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    this.handler.post(() -> {
                        this.progressBar.setVisibility(8);
                        this.login.setVisibility(0);
                        this.cancel.setEnabled(true);
                    });
                }).start();
            }
        }
    }

    public static interface OnMicrosoftAccountAddListener {
        public void onPositive(Account var1);
    }
}

