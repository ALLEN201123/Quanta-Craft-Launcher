package com.qcl.launcher.launcher.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.VerifyInterface;
import com.qcl.launcher.utils.DigestUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class VerifyDialog extends Dialog implements View.OnClickListener {
    private MainActivity activity;
    private Button cancel;
    private String code;
    private Button copy;
    private EditText editText;
    private SharedPreferences.Editor editor;
    private Button obtainPermission;
    private TextView textView;
    private Button verify;
    private VerifyInterface verifyInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$onClick$1(DialogInterface dialogInterface, int i) {
    }

    public VerifyDialog(Context context, MainActivity mainActivity, SharedPreferences.Editor editor, VerifyInterface verifyInterface) {
        super(context);
        this.activity = mainActivity;
        this.editor = editor;
        this.verifyInterface = verifyInterface;
        setContentView(R.layout.dialog_verify);
        setCancelable(false);
        init();
    }

    private void init() {
        this.code = DigestUtils.getDeviceCode(getContext());
        this.textView = (TextView) findViewById(R.id.oaid_text);
        this.editText = (EditText) findViewById(R.id.edit_verify_code);
        this.obtainPermission = (Button) findViewById(R.id.obtain_permission);
        this.cancel = (Button) findViewById(R.id.cancel);
        this.copy = (Button) findViewById(R.id.copy_oaid);
        this.verify = (Button) findViewById(R.id.verify);
        this.textView.setText(getContext().getString(R.string.dialog_verify_msg).replace("%s", this.code));
        this.obtainPermission.setOnClickListener(this);
        this.cancel.setOnClickListener(this);
        this.copy.setOnClickListener(this);
        this.verify.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.obtainPermission) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getContext().getString(R.string.dialog_obtain_permission_title));
            builder.setMessage(getContext().getString(R.string.dialog_obtain_permission_msg));
            builder.setPositiveButton(getContext().getString(R.string.dialog_obtain_permission_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.dialogs.VerifyDialog$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    VerifyDialog.this.m233lambda$onClick$0$comqcllauncherlauncherdialogsVerifyDialog(dialogInterface, i);
                }
            });
            builder.setNegativeButton(getContext().getString(R.string.dialog_obtain_permission_negative), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.dialogs.VerifyDialog$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    VerifyDialog.lambda$onClick$1(dialogInterface, i);
                }
            });
            builder.create().show();
        }
        if (view == this.cancel) {
            this.verifyInterface.onCancel();
            dismiss();
        }
        if (view == this.copy) {
            ((ClipboardManager) getContext().getSystemService("clipboard")).setPrimaryClip(ClipData.newPlainText(null, this.code));
            Toast.makeText(getContext(), getContext().getString(R.string.dialog_verify_copy_success), 0).show();
        }
        if (view == this.verify) {
            if (this.activity.isValid(this.editText.getText().toString())) {
                this.editor.putString("code", this.editText.getText().toString());
                this.editor.putBoolean("verified", true);
                this.editor.commit();
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_verify_verify_success), 0).show();
                dismiss();
                this.verifyInterface.onSuccess();
                return;
            }
            Toast.makeText(getContext(), getContext().getString(R.string.dialog_verify_verify_fail), 0).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-VerifyDialog, reason: not valid java name */
    public /* synthetic */ void m233lambda$onClick$0$comqcllauncherlauncherdialogsVerifyDialog(DialogInterface dialogInterface, int i) {
        getContext().startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://afdian.net/@tungs")));
    }
}
