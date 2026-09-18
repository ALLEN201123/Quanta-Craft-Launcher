package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.control.bean.rocker.RockerStyle;
import com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog;
import com.qcl.launcher.launcher.list.local.controller.RockerStyleAdapter;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class RockerStyleManagerDialog extends Dialog implements View.OnClickListener {
    private Button create;
    private ListView listView;
    private OnStyleListChangeListener onStyleListChangeListener;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnStyleListChangeListener {
        void onStyleListChange();
    }

    public RockerStyleManagerDialog(Context context, OnStyleListChangeListener onStyleListChangeListener) {
        super(context);
        this.onStyleListChangeListener = onStyleListChangeListener;
        setContentView(R.layout.dialog_manage_rocker_style);
        setCancelable(false);
        init();
    }

    private void init() {
        this.listView = (ListView) findViewById(R.id.rocker_style_list);
        this.create = (Button) findViewById(R.id.create_rocker_style);
        this.positive = (Button) findViewById(R.id.exit);
        this.create.setOnClickListener(this);
        this.positive.setOnClickListener(this);
        refreshStyleList();
    }

    public void refreshStyleList() {
        this.listView.setAdapter((ListAdapter) new RockerStyleAdapter(getContext(), SettingUtils.getRockerStyleList(), this));
        this.onStyleListChangeListener.onStyleListChange();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.create) {
            new CreateRockerStyleDialog(getContext(), SettingUtils.getRockerStyleList(), new CreateRockerStyleDialog.OnRockerStyleCreateListener() { // from class: com.qcl.launcher.launcher.dialogs.control.RockerStyleManagerDialog.1
                @Override // com.qcl.launcher.launcher.dialogs.control.CreateRockerStyleDialog.OnRockerStyleCreateListener
                public void onRockerStyleCreate(RockerStyle rockerStyle) {
                    ArrayList<RockerStyle> rockerStyleList = SettingUtils.getRockerStyleList();
                    rockerStyleList.add(rockerStyle);
                    SettingUtils.saveRockerStyle(rockerStyleList);
                    RockerStyleManagerDialog.this.refreshStyleList();
                }
            }).show();
        }
        if (view == this.positive) {
            dismiss();
        }
    }
}
