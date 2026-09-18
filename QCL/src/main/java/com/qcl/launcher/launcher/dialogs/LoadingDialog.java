package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.widget.TextView;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class LoadingDialog extends Dialog {
    private final TextView loadingText;

    public LoadingDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_loading);
        setCancelable(false);
        this.loadingText = (TextView) findViewById(R.id.loading_text);
    }

    public void setLoadingText(String str) {
        this.loadingText.setText(str);
    }
}
