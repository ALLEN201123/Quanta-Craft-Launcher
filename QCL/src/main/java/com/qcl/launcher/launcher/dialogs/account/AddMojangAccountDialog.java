/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Dialog
 *  android.content.Context
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.os.Handler
 *  android.os.Message
 *  android.text.method.LinkMovementMethod
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.annotation.NonNull
 *  androidx.annotation.RequiresApi
 */
package com.qcl.launcher.launcher.dialogs.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.AuthInfo;
import com.qcl.launcher.auth.AuthenticationException;
import com.qcl.launcher.auth.yggdrasil.MojangYggdrasilProvider;
import com.qcl.launcher.auth.yggdrasil.Texture;
import com.qcl.launcher.auth.yggdrasil.TextureType;
import com.qcl.launcher.auth.yggdrasil.YggdrasilService;
import com.qcl.launcher.auth.yggdrasil.YggdrasilSession;
import com.qcl.launcher.skin.utils.Avatar;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import com.qcl.launcher.R;
public class AddMojangAccountDialog
extends Dialog
implements View.OnClickListener {
    private ArrayList<Account> accounts;
    private OnMojangAccountAddListener onMojangAccountAddListener;
    private TextView editEmail;
    private TextView editPassword;
    private TextView migrateLink;
    private TextView helpLink;
    private TextView purchaseLink;
    private Button login;
    private Button cancel;
    Account account;
    @SuppressLint(value={"HandlerLeak"})
    public final Handler loginHandler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                AddMojangAccountDialog.this.onMojangAccountAddListener.onPositive(AddMojangAccountDialog.this.account);
                AddMojangAccountDialog.this.dismiss();
            }
            if (msg.what == 1) {
                Toast.makeText((Context)AddMojangAccountDialog.this.getContext(), (CharSequence)AddMojangAccountDialog.this.getContext().getString(R.string.dialog_add_mojang_account_failed), (int)0).show();
            }
        }
    };

    public AddMojangAccountDialog(@NonNull Context context, ArrayList<Account> accounts, OnMojangAccountAddListener onMojangAccountAddListener) {
        super(context);
        this.accounts = accounts;
        this.onMojangAccountAddListener = onMojangAccountAddListener;
        this.setContentView(R.layout.dialog_add_mojang_account);
        this.setCancelable(false);
        this.init();
    }

    private void init() {
        this.editEmail = (TextView)this.findViewById(R.id.edit_email);
        this.editPassword = (TextView)this.findViewById(R.id.edit_password);
        this.migrateLink = (TextView)this.findViewById(R.id.migrate_link);
        this.helpLink = (TextView)this.findViewById(R.id.help_link);
        this.purchaseLink = (TextView)this.findViewById(R.id.purchase_link);
        this.migrateLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.helpLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.purchaseLink.setMovementMethod(LinkMovementMethod.getInstance());
        this.login = (Button)this.findViewById(R.id.login_mojang);
        this.cancel = (Button)this.findViewById(R.id.cancel_login_mojang);
        this.login.setOnClickListener((View.OnClickListener)this);
        this.cancel.setOnClickListener((View.OnClickListener)this);
    }

    public void onClick(View v) {
        if (v == this.login) {
            ArrayList<String> emails = new ArrayList<String>();
            for (Account account : this.accounts) {
                if (account.loginType != 2) continue;
                emails.add(account.email);
            }
            if (emails.contains(this.editEmail.getText().toString())) {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_mojang_account_exist_warn), (int)0).show();
            } else if (this.editEmail.getText().toString().equals("") || this.editPassword.getText().toString().equals("")) {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_mojang_account_empty_warn), (int)0).show();
            } else {
                final String email = this.editEmail.getText().toString();
                final String password = this.editPassword.getText().toString();
                new Thread(){

                    @Override
                    @RequiresApi(api=24)
                    public void run() {
                        YggdrasilService yggdrasilService = new YggdrasilService(new MojangYggdrasilProvider());
                        try {
                            final YggdrasilSession yggdrasilSession = yggdrasilService.authenticate(email, password, "00000000-0000-0000-0000-000000000000");
                            final AuthInfo authInfo = yggdrasilSession.toAuthInfo();
                            Map<TextureType, Texture> map = YggdrasilService.getTextures(yggdrasilService.getCompleteGameProfile(authInfo.getUUID()).get()).get();
                            Texture texture = map.get((Object)TextureType.SKIN);
                            String u = texture.getUrl();
                            if (!u.startsWith("https")) {
                                u = u.replaceFirst("http", "https");
                            }
                            URL url = new URL(u);
                            HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                            httpURLConnection.setDoInput(true);
                            httpURLConnection.connect();
                            InputStream inputStream = httpURLConnection.getInputStream();
                            final Bitmap skin = BitmapFactory.decodeStream((InputStream)inputStream);
                            AddMojangAccountDialog.this.loginHandler.post(new Runnable(){

                                @Override
                                public void run() {
                                    String skinTexture = Avatar.bitmapToString(skin);
                                    AddMojangAccountDialog.this.account = new Account(2, email, password, "mojang", "0", authInfo.getUsername(), authInfo.getUUID().toString(), authInfo.getAccessToken(), yggdrasilSession.getClientToken(), "", "", skinTexture);
                                }
                            });
                            AddMojangAccountDialog.this.loginHandler.sendEmptyMessage(0);
                        }
                        catch (AuthenticationException | IOException e) {
                            e.printStackTrace();
                            AddMojangAccountDialog.this.loginHandler.sendEmptyMessage(1);
                        }
                    }
                }.start();
            }
        }
        if (v == this.cancel) {
            this.dismiss();
        }
    }

    public static interface OnMojangAccountAddListener {
        public void onPositive(Account var1);
    }
}

