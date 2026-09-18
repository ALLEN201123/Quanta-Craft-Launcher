package com.qcl.launcher.launcher.dialogs.control;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.qcl.launcher.launcher.list.local.controller.ControlPattern;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class EditControlPatternDialog extends Dialog implements View.OnClickListener {
    private Activity activity;
    private ControlPattern controlPattern;
    private EditText editAuthor;
    private EditText editDescribe;
    private EditText editName;
    private EditText editVersion;
    private boolean enable;
    private LinearLayout layout;
    private Button negative;
    private OnPatternInfoChangeListener onPatternInfoChangeListener;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnPatternInfoChangeListener {
        void OnInfoChange(ControlPattern controlPattern);
    }

    public EditControlPatternDialog(Context context, Activity activity, boolean z, OnPatternInfoChangeListener onPatternInfoChangeListener, ControlPattern controlPattern) {
        super(context);
        setContentView(R.layout.dialog_edit_pattern_info);
        this.activity = activity;
        this.enable = z;
        this.onPatternInfoChangeListener = onPatternInfoChangeListener;
        this.controlPattern = controlPattern;
        setCancelable(false);
        init();
    }

    private void init() {
        this.layout = (LinearLayout) findViewById(R.id.pattern_name_editor);
        this.editName = (EditText) findViewById(R.id.edit_pattern_name);
        this.editAuthor = (EditText) findViewById(R.id.edit_pattern_author);
        this.editVersion = (EditText) findViewById(R.id.edit_pattern_version);
        this.editDescribe = (EditText) findViewById(R.id.edit_pattern_describe);
        this.editName.setText(this.controlPattern.name);
        this.editAuthor.setText(this.controlPattern.author);
        this.editVersion.setText(this.controlPattern.versionName);
        this.editDescribe.setText(this.controlPattern.describe);
        if (!this.enable) {
            this.layout.setVisibility(8);
        }
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
                ControlPattern next = it.next();
                if (!next.name.equals(this.controlPattern.name)) {
                    arrayList.add(next.name);
                }
            }
            boolean contains = arrayList.contains(this.editName.getText().toString());
            if (this.editName.getText().toString().equals("")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_control_pattern_warn), 0).show();
            } else if (contains) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_control_pattern_warn_exist), 0).show();
            } else {
                this.onPatternInfoChangeListener.OnInfoChange(new ControlPattern(this.editName.getText().toString(), this.editAuthor.getText().toString(), this.editVersion.getText().toString(), this.editDescribe.getText().toString(), 1));
                dismiss();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
    }
}
