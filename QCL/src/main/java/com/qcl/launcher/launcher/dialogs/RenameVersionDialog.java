package com.qcl.launcher.launcher.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class RenameVersionDialog extends Dialog implements View.OnClickListener {
    private Button cancel;
    private Button confirm;
    private String currentVersion;
    private EditText editText;
    private OnVersionRenameListener onVersionRenameListener;

    /* loaded from: classes2.dex */
    public interface OnVersionRenameListener {
        void onRename(String str);
    }

    public RenameVersionDialog(Context context, String str, OnVersionRenameListener onVersionRenameListener) {
        super(context);
        this.currentVersion = str;
        this.onVersionRenameListener = onVersionRenameListener;
        setContentView(R.layout.dialog_rename_version);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editText = (EditText) findViewById(R.id.rename_version);
        this.confirm = (Button) findViewById(R.id.rename);
        this.cancel = (Button) findViewById(R.id.cancel);
        this.editText.setText(this.currentVersion);
        this.confirm.setOnClickListener(this);
        this.cancel.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.confirm) {
            if (this.editText.getText().toString().equals("") || this.editText.getText().toString().contains("/")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_rename_version_alert), 0).show();
            } else {
                this.onVersionRenameListener.onRename(this.editText.getText().toString());
                dismiss();
            }
        }
        if (view == this.cancel) {
            dismiss();
        }
    }
}
