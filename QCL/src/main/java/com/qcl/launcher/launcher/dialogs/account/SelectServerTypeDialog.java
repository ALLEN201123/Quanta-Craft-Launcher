package com.qcl.launcher.launcher.dialogs.account;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.dialogs.account.AddAuthLibServerDialog;
import com.qcl.launcher.launcher.dialogs.account.AddNide8AuthServerDialog;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class SelectServerTypeDialog extends Dialog implements View.OnClickListener {
    private LinearLayout authlib;
    private LinearLayout nide8auth;
    private OnServerAddListener onServerAddListener;

    /* loaded from: classes2.dex */
    public interface OnServerAddListener {
        void onServerAdd(AuthlibInjectorServer authlibInjectorServer);
    }

    public SelectServerTypeDialog(Context context, OnServerAddListener onServerAddListener) {
        super(context);
        this.onServerAddListener = onServerAddListener;
        setContentView(R.layout.dialog_select_server_type);
        setCancelable(false);
        init();
    }

    private void init() {
        this.authlib = (LinearLayout) findViewById(R.id.server_type_authlib);
        this.nide8auth = (LinearLayout) findViewById(R.id.server_type_nide);
        this.authlib.setOnClickListener(this);
        this.nide8auth.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.authlib) {
            new AddAuthLibServerDialog(getContext(), new AddAuthLibServerDialog.OnAuthlibInjectorServerAddListener() { // from class: com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog$$ExternalSyntheticLambda0
                @Override // com.qcl.launcher.launcher.dialogs.account.AddAuthLibServerDialog.OnAuthlibInjectorServerAddListener
                public final void onServerAdd(AuthlibInjectorServer authlibInjectorServer) {
                    SelectServerTypeDialog.this.m270x2b87db4e(authlibInjectorServer);
                }
            }).show();
        }
        if (view == this.nide8auth) {
            new AddNide8AuthServerDialog(getContext(), new AddNide8AuthServerDialog.OnNide8AuthServerAddListener() { // from class: com.qcl.launcher.launcher.dialogs.account.SelectServerTypeDialog$$ExternalSyntheticLambda1
                @Override // com.qcl.launcher.launcher.dialogs.account.AddNide8AuthServerDialog.OnNide8AuthServerAddListener
                public final void onServerAdd(AuthlibInjectorServer authlibInjectorServer) {
                    SelectServerTypeDialog.this.m271x95b7636d(authlibInjectorServer);
                }
            }).show();
        }
        dismiss();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-account-SelectServerTypeDialog, reason: not valid java name */
    public /* synthetic */ void m270x2b87db4e(AuthlibInjectorServer authlibInjectorServer) {
        this.onServerAddListener.onServerAdd(authlibInjectorServer);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-dialogs-account-SelectServerTypeDialog, reason: not valid java name */
    public /* synthetic */ void m271x95b7636d(AuthlibInjectorServer authlibInjectorServer) {
        this.onServerAddListener.onServerAdd(authlibInjectorServer);
    }
}
