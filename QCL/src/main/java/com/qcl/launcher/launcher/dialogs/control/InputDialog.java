package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.launcher.dialogs.control.AddFastTextDialog;
import com.qcl.launcher.launcher.list.local.controller.FastTextAdapter;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class InputDialog extends Dialog implements View.OnClickListener, TextWatcher {
    private Button addText;
    private Button clearText;
    public EditText editText;
    private ListView listView;
    private MenuHelper menuHelper;
    private Button negative;
    private Button send;

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public InputDialog(Context context, MenuHelper menuHelper) {
        super(context);
        this.menuHelper = menuHelper;
        setContentView(R.layout.dialog_input);
        setCanceledOnTouchOutside(false);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        init();
    }

    private void init() {
        this.listView = (ListView) findViewById(R.id.fast_text_list);
        this.editText = (EditText) findViewById(R.id.edit_input_text);
        this.addText = (Button) findViewById(R.id.add_fast_text);
        this.clearText = (Button) findViewById(R.id.clear_input_text);
        this.send = (Button) findViewById(R.id.send_text);
        this.negative = (Button) findViewById(R.id.exit);
        this.addText.setOnClickListener(this);
        this.clearText.setOnClickListener(this);
        this.send.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        this.editText.setText(">");
        this.editText.addTextChangedListener(this);
        refreshList();
    }

    private void refreshList() {
        this.listView.setAdapter((ListAdapter) new FastTextAdapter(getContext(), SettingUtils.getFastList(), this));
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.addText) {
            new AddFastTextDialog(getContext(), new AddFastTextDialog.OnFastTextAddListener() { // from class: com.qcl.launcher.launcher.dialogs.control.InputDialog$$ExternalSyntheticLambda0
                @Override // com.qcl.launcher.launcher.dialogs.control.AddFastTextDialog.OnFastTextAddListener
                public final void onFastTextAdd(String str) {
                    InputDialog.this.m285x4a74d595(str);
                }
            }).show();
        }
        if (view == this.clearText) {
            this.editText.setText(">");
        }
        if (view == this.send) {
            if (this.menuHelper.gameCursorMode == 0) {
                for (int i = 1; i < this.editText.getText().toString().length(); i++) {
                    InputBridge.sendKeyChar(this.menuHelper.launcher, this.editText.getText().toString().charAt(i));
                }
                dismiss();
            } else {
                InputBridge.sendEvent(this.menuHelper.launcher, 84, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 84, false);
                new Handler().postDelayed(new Runnable() { // from class: com.qcl.launcher.launcher.dialogs.control.InputDialog$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        InputDialog.this.m286x73c92ad6();
                    }
                }, 50L);
            }
        }
        if (view == this.negative) {
            dismiss();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$0$com-qcl-launcher-launcher-dialogs-control-InputDialog, reason: not valid java name */
    public /* synthetic */ void m285x4a74d595(String str) {
        ArrayList<String> fastList = SettingUtils.getFastList();
        fastList.add(str);
        SettingUtils.saveFastText(fastList);
        refreshList();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$onClick$1$com-qcl-launcher-launcher-dialogs-control-InputDialog, reason: not valid java name */
    public /* synthetic */ void m286x73c92ad6() {
        for (int i = 1; i < this.editText.getText().toString().length(); i++) {
            InputBridge.sendKeyChar(this.menuHelper.launcher, this.editText.getText().toString().charAt(i));
        }
        InputBridge.sendEvent(this.menuHelper.launcher, 257, true);
        InputBridge.sendEvent(this.menuHelper.launcher, 257, false);
        dismiss();
    }

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
        if (this.editText.getText().toString().length() < 1) {
            InputBridge.sendEvent(this.menuHelper.launcher, 259, true);
            InputBridge.sendEvent(this.menuHelper.launcher, 259, false);
            this.editText.setText(">");
            this.editText.setSelection(1);
        }
    }
}
