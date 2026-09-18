package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.launcher.dialogs.control.CreateChildDialog;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.list.local.controller.ChildLayoutListAdapter;
import com.qcl.launcher.launcher.list.local.controller.ControlPattern;
import com.qcl.launcher.launcher.setting.SettingUtils;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ChildManagerDialog extends Dialog implements View.OnClickListener {
    private ListView childListView;
    private ControlPattern controlPattern;
    private Button create;
    private Button exit;
    private MenuHelper menuHelper;

    public ChildManagerDialog(Context context, MenuHelper menuHelper, ControlPattern controlPattern) {
        super(context);
        this.menuHelper = menuHelper;
        this.controlPattern = controlPattern;
        setContentView(R.layout.dialog_manage_child);
        setCancelable(false);
        init();
    }

    private void init() {
        this.childListView = (ListView) findViewById(R.id.child_list);
        this.create = (Button) findViewById(R.id.create_child);
        this.exit = (Button) findViewById(R.id.exit);
        this.create.setOnClickListener(this);
        this.exit.setOnClickListener(this);
        refreshListView();
    }

    public void refreshListView() {
        this.childListView.setAdapter((ListAdapter) new ChildLayoutListAdapter(getContext(), SettingUtils.getChildList(this.controlPattern.name), this.controlPattern, this));
        this.menuHelper.refreshChildSpinner();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.create) {
            new CreateChildDialog(getContext(), this.controlPattern.name, new CreateChildDialog.OnChildAddListener() { // from class: com.qcl.launcher.launcher.dialogs.control.ChildManagerDialog.1
                @Override // com.qcl.launcher.launcher.dialogs.control.CreateChildDialog.OnChildAddListener
                public void onChildAdd(ChildLayout childLayout) {
                    ChildLayout.saveChildLayout(ChildManagerDialog.this.controlPattern.name, childLayout);
                    ChildManagerDialog.this.refreshListView();
                }
            }).show();
        }
        if (view == this.exit) {
            dismiss();
        }
    }
}
