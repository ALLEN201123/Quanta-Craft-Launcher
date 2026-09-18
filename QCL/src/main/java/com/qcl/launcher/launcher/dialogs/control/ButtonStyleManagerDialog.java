package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.control.bean.button.ButtonStyle;
import com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog;
import com.qcl.launcher.launcher.list.local.controller.ButtonStyleAdapter;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ButtonStyleManagerDialog extends Dialog implements View.OnClickListener {
    private Button create;
    private ListView listView;
    private OnStyleListChangeListener onStyleListChangeListener;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnStyleListChangeListener {
        void onStyleListChange();
    }

    public ButtonStyleManagerDialog(Context context, OnStyleListChangeListener onStyleListChangeListener) {
        super(context);
        this.onStyleListChangeListener = onStyleListChangeListener;
        setContentView(R.layout.dialog_manage_button_style);
        setCancelable(false);
        init();
    }

    private void init() {
        this.listView = (ListView) findViewById(R.id.button_style_list);
        this.create = (Button) findViewById(R.id.create_button_style);
        this.positive = (Button) findViewById(R.id.exit);
        this.create.setOnClickListener(this);
        this.positive.setOnClickListener(this);
        refreshStyleList();
    }

    public void refreshStyleList() {
        this.listView.setAdapter((ListAdapter) new ButtonStyleAdapter(getContext(), SettingUtils.getButtonStyleList(), this));
        this.onStyleListChangeListener.onStyleListChange();
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.create) {
            new CreateButtonStyleDialog(getContext(), SettingUtils.getButtonStyleList(), new CreateButtonStyleDialog.OnButtonStyleCreateListener() { // from class: com.qcl.launcher.launcher.dialogs.control.ButtonStyleManagerDialog.1
                @Override // com.qcl.launcher.launcher.dialogs.control.CreateButtonStyleDialog.OnButtonStyleCreateListener
                public void onButtonStyleCreate(ButtonStyle buttonStyle) {
                    ArrayList<ButtonStyle> buttonStyleList = SettingUtils.getButtonStyleList();
                    buttonStyleList.add(buttonStyle);
                    SettingUtils.saveButtonStyle(buttonStyleList);
                    ButtonStyleManagerDialog.this.refreshStyleList();
                }
            }).show();
        }
        if (view == this.positive) {
            dismiss();
        }
    }
}
