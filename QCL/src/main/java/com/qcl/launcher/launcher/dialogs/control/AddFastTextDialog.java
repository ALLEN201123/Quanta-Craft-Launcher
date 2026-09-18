package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class AddFastTextDialog extends Dialog implements View.OnClickListener {
    private EditText editText;
    private Button negative;
    private OnFastTextAddListener onFastTextAddListener;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnFastTextAddListener {
        void onFastTextAdd(String str);
    }

    public AddFastTextDialog(Context context, OnFastTextAddListener onFastTextAddListener) {
        super(context);
        this.onFastTextAddListener = onFastTextAddListener;
        setContentView(R.layout.dialog_add_fast_text);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editText = (EditText) findViewById(R.id.edit_fast_text);
        this.positive = (Button) findViewById(R.id.add_fast_text);
        this.negative = (Button) findViewById(R.id.exit);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            ArrayList<String> fastList = SettingUtils.getFastList();
            if (this.editText.getText().toString().equals("")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_add_fast_text_empty), 0).show();
            } else if (fastList.contains(this.editText.getText().toString())) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_add_fast_text_exist), 0).show();
            } else {
                this.onFastTextAddListener.onFastTextAdd(this.editText.getText().toString());
                dismiss();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
    }
}
