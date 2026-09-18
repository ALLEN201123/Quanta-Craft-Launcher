package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;
import com.qcl.launcher.control.bean.BaseButtonInfo;
import com.qcl.launcher.control.bean.BaseRockerViewInfo;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class EditChildDialog extends Dialog implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private ChildLayout childLayout;
    private EditText editName;
    private Button negative;
    private OnChildChangeListener onChildChangeListener;
    private String pattern;
    private Button positive;
    private Spinner spinner;
    private int visibility;

    /* loaded from: classes2.dex */
    public interface OnChildChangeListener {
        void onChildChange(ChildLayout childLayout);
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public EditChildDialog(Context context, String str, OnChildChangeListener onChildChangeListener, ChildLayout childLayout) {
        super(context);
        this.pattern = str;
        this.onChildChangeListener = onChildChangeListener;
        this.childLayout = childLayout;
        setContentView(R.layout.dialog_edit_child);
        setCancelable(false);
        init();
    }

    private void init() {
        this.editName = (EditText) findViewById(R.id.edit_child_name);
        this.spinner = (Spinner) findViewById(R.id.visibility_spinner);
        this.positive = (Button) findViewById(R.id.create_current_child);
        this.negative = (Button) findViewById(R.id.exit);
        this.positive.setOnClickListener(this);
        this.negative.setOnClickListener(this);
        ArrayList arrayList = new ArrayList();
        arrayList.add(getContext().getString(R.string.dialog_create_child_visibility_visible));
        arrayList.add(getContext().getString(R.string.dialog_create_child_visibility_invisible));
        this.spinner.setAdapter((SpinnerAdapter) new ArrayAdapter(getContext(), R.layout.item_spinner, arrayList));
        this.spinner.setOnItemSelectedListener(this);
        this.editName.setText(this.childLayout.name);
        if (this.childLayout.visibility == 0) {
            this.spinner.setSelection(0);
        } else {
            this.spinner.setSelection(1);
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        ArrayList<ChildLayout> childList = SettingUtils.getChildList(this.pattern);
        ArrayList arrayList = new ArrayList();
        Iterator<ChildLayout> it = childList.iterator();
        while (it.hasNext()) {
            ChildLayout next = it.next();
            if (!next.name.equals(this.childLayout.name)) {
                arrayList.add(next.name);
            }
        }
        boolean contains = arrayList.contains(this.editName.getText().toString());
        if (view == this.positive) {
            if (this.editName.getText().toString().equals("")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_child_warn), 0).show();
            } else if (this.editName.getText().toString().equals("info")) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_child_warn_info), 0).show();
            } else if (contains) {
                Toast.makeText(getContext(), getContext().getString(R.string.dialog_create_child_warn_exist), 0).show();
            } else {
                ArrayList arrayList2 = new ArrayList();
                ArrayList arrayList3 = new ArrayList();
                Iterator<ChildLayout> it2 = childList.iterator();
                while (it2.hasNext()) {
                    ChildLayout next2 = it2.next();
                    Iterator<BaseButtonInfo> it3 = next2.baseButtonList.iterator();
                    while (it3.hasNext()) {
                        BaseButtonInfo next3 = it3.next();
                        if (next3.visibilityControl.contains(this.childLayout.name)) {
                            next3.visibilityControl.remove(this.childLayout.name);
                            next3.visibilityControl.add(this.editName.getText().toString());
                            ChildLayout.saveChildLayout(this.pattern, next2);
                        }
                    }
                }
                Iterator<BaseButtonInfo> it4 = this.childLayout.baseButtonList.iterator();
                while (it4.hasNext()) {
                    BaseButtonInfo next4 = it4.next();
                    next4.child = this.editName.getText().toString();
                    if (next4.visibilityControl.contains(this.childLayout.name)) {
                        next4.visibilityControl.remove(this.childLayout.name);
                        next4.visibilityControl.add(this.editName.getText().toString());
                    }
                    arrayList2.add(next4);
                }
                Iterator<BaseRockerViewInfo> it5 = this.childLayout.baseRockerViewList.iterator();
                while (it5.hasNext()) {
                    BaseRockerViewInfo next5 = it5.next();
                    next5.child = this.editName.getText().toString();
                    arrayList3.add(next5);
                }
                this.onChildChangeListener.onChildChange(new ChildLayout(this.editName.getText().toString(), this.visibility, arrayList2, arrayList3));
                dismiss();
            }
        }
        if (view == this.negative) {
            dismiss();
        }
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        if (i == 0) {
            this.visibility = 0;
        }
        if (i == 1) {
            this.visibility = 4;
        }
    }
}
