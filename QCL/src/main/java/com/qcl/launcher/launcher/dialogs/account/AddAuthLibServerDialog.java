/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Dialog
 *  android.content.Context
 *  android.os.Handler
 *  android.os.Message
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.EditText
 *  android.widget.LinearLayout
 *  android.widget.ProgressBar
 *  android.widget.TextView
 *  android.widget.Toast
 *  androidx.annotation.NonNull
 */
package com.qcl.launcher.launcher.dialogs.account;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import java.io.IOException;

import com.qcl.launcher.R;
public class AddAuthLibServerDialog
extends Dialog
implements View.OnClickListener {
    private OnAuthlibInjectorServerAddListener onAuthlibInjectorServerAddListener;
    private LinearLayout layoutPri;
    private EditText editVerifyServer;
    private Button cancel;
    private Button next;
    private LinearLayout layoutSec;
    private TextView url;
    private TextView name;
    private Button cancelSec;
    private Button back;
    private Button positive;
    private ProgressBar progressBar;
    private AuthlibInjectorServer authlibInjectorServer;
    @SuppressLint(value={"HandlerLeak"})
    public final Handler loginHandler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public AddAuthLibServerDialog(@NonNull Context context, OnAuthlibInjectorServerAddListener onAuthlibInjectorServerAddListener) {
        super(context);
        this.onAuthlibInjectorServerAddListener = onAuthlibInjectorServerAddListener;
        this.setContentView(R.layout.dialog_add_authlib_server);
        this.setCancelable(false);
        this.init();
    }

    private void init() {
        this.layoutPri = (LinearLayout)this.findViewById(R.id.add_verify_server_pri);
        this.editVerifyServer = (EditText)this.findViewById(R.id.edit_verify_server);
        this.cancel = (Button)this.findViewById(R.id.cancel);
        this.next = (Button)this.findViewById(R.id.next);
        this.layoutSec = (LinearLayout)this.findViewById(R.id.add_verify_server_sec);
        this.url = (TextView)this.findViewById(R.id.server_url);
        this.name = (TextView)this.findViewById(R.id.server_name);
        this.cancelSec = (Button)this.findViewById(R.id.cancel_sec);
        this.back = (Button)this.findViewById(R.id.back);
        this.positive = (Button)this.findViewById(R.id.add_verify_server);
        this.progressBar = (ProgressBar)this.findViewById(R.id.verify_progress);
        this.cancel.setOnClickListener((View.OnClickListener)this);
        this.next.setOnClickListener((View.OnClickListener)this);
        this.cancelSec.setOnClickListener((View.OnClickListener)this);
        this.back.setOnClickListener((View.OnClickListener)this);
        this.positive.setOnClickListener((View.OnClickListener)this);
    }

    private void next(AuthlibInjectorServer authlibInjectorServer) {
        CustomAnimationUtils.hideViewToLeft((View)this.layoutPri, this.getContext(), false);
        CustomAnimationUtils.showViewFromRight((View)this.layoutSec, this.getContext(), true);
        this.url.setText((CharSequence)authlibInjectorServer.getUrl());
        this.name.setText((CharSequence)authlibInjectorServer.getName());
        this.authlibInjectorServer = authlibInjectorServer;
    }

    private void back() {
        CustomAnimationUtils.hideViewToRight((View)this.layoutSec, this.getContext(), false);
        CustomAnimationUtils.showViewFromLeft((View)this.layoutPri, this.getContext(), true);
    }

    public void onClick(View v) {
        if (v == this.cancel || v == this.cancelSec) {
            this.dismiss();
        }
        if (v == this.next) {
            if (this.editVerifyServer.getText().toString().equals("")) {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_verify_server_empty), (int)0).show();
            } else {
                new Thread(() -> {
                    this.loginHandler.post(() -> {
                        this.progressBar.setVisibility(0);
                        this.next.setVisibility(8);
                        this.cancel.setEnabled(false);
                    });
                    try {
                        AuthlibInjectorServer authlibInjectorServer = AuthlibInjectorServer.locateServer(this.editVerifyServer.getText().toString());
                        this.loginHandler.post(() -> this.next(authlibInjectorServer));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        this.loginHandler.post(() -> Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.dialog_add_verify_server_invalid), (int)0).show());
                    }
                    this.loginHandler.post(() -> {
                        this.progressBar.setVisibility(8);
                        this.next.setVisibility(0);
                        this.cancel.setEnabled(true);
                    });
                }).start();
            }
        }
        if (v == this.back) {
            this.back();
        }
        if (v == this.positive && this.authlibInjectorServer != null) {
            this.onAuthlibInjectorServerAddListener.onServerAdd(this.authlibInjectorServer);
            this.dismiss();
        }
    }

    public static interface OnAuthlibInjectorServerAddListener {
        public void onServerAdd(AuthlibInjectorServer var1);
    }
}

