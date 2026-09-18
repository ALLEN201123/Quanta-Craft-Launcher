package com.qcl.launcher.launcher.dialogs.control;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.qcl.launcher.launcher.list.local.controller.ControlPattern;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class CreateControlPatternDialog extends Dialog implements View.OnClickListener {
    private Activity activity;
    private EditText editAuthor;
    private EditText editDescribe;
    private EditText editName;
    private EditText editVersion;
    private Button negative;
    private OnPatternCreateListener onPatternCreateListener;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnPatternCreateListener {
        void OnPatternCreate(ControlPattern controlPattern);
    }

    public CreateControlPatternDialog(Context context, Activity activity, OnPatternCreateListener onPatternCreateListener) {
        super(context);
        this.activity = activity;
        setContentView(R.layout.dialog_create_control_pattern);
        this.onPatternCreateListener = onPatternCreateListener;
        setCancelable(false);
        init();
    }

    private void init() {
        this.editName = (EditText) findViewById(R.id.edit_pattern_name);
        this.editAuthor = (EditText) findViewById(R.id.edit_pattern_author);
        this.editVersion = (EditText) findViewById(R.id.edit_pattern_version);
        this.editDescribe = (EditText) findViewById(R.id.edit_pattern_describe);
        this.positive = (Button) findViewById(R.id.create_pattern);
        this.negative = (Button) findViewById(R.id.exit);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            ArrayList<ControlPattern> controlPatternList = SettingUtils.getControlPatternList();
            ArrayList arrayList = new ArrayList();
            Iterator<ControlPattern> it = controlPatternList.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().name);
            }
            boolean contains = arrayList.contains(this.editName.getText().toString());
            if (this.editName.getText().toString().equals("") || this.editName.getText().toString().contains("/")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_control_pattern_warn), 0).show();
            } else if (contains) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_control_pattern_warn_exist), 0).show();
            } else {
                this.onPatternCreateListener.OnPatternCreate(new ControlPattern(this.editName.getText().toString(), this.editAuthor.getText().toString(), this.editVersion.getText().toString(), this.editDescribe.getText().toString(), 1));
                dismiss();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
    }
}
