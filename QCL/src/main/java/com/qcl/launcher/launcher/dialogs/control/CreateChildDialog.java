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
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class CreateChildDialog extends Dialog implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private EditText editName;
    private Button negative;
    private OnChildAddListener onChildAddListener;
    private String pattern;
    private Button positive;
    private Spinner spinner;
    private int visibility;

    /* loaded from: classes2.dex */
    public interface OnChildAddListener {
        void onChildAdd(ChildLayout childLayout);
    }

    @Override // android.widget.AdapterView.OnItemSelectedListener
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public CreateChildDialog(Context context, String str, OnChildAddListener onChildAddListener) {
        super(context);
        this.pattern = str;
        this.onChildAddListener = onChildAddListener;
        setContentView(R.layout.dialog_create_child);
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
        this.spinner.setSelection(0);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        ArrayList<ChildLayout> childList = SettingUtils.getChildList(this.pattern);
        ArrayList arrayList = new ArrayList();
        Iterator<ChildLayout> it = childList.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().name);
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
                this.onChildAddListener.onChildAdd(new ChildLayout(this.editName.getText().toString(), this.visibility, new ArrayList(), new ArrayList()));
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
