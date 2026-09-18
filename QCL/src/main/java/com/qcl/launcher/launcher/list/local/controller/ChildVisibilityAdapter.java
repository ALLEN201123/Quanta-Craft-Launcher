package com.qcl.launcher.launcher.list.local.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.qcl.launcher.launcher.dialogs.control.ChildVisibilityDialog;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ChildVisibilityAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> currentList;
    private ChildVisibilityDialog dialog;
    private ArrayList<ChildLayout> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public ChildVisibilityAdapter(Context context, ArrayList<String> arrayList, String str, ChildVisibilityDialog childVisibilityDialog) {
        this.context = context;
        this.currentList = arrayList;
        this.dialog = childVisibilityDialog;
        this.list = SettingUtils.getChildList(str);
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        CheckBox checkBox;
        TextView name;

        private ViewHolder() {
        }
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.list.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.list.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_child_visibility, (ViewGroup) null);
            viewHolder.name = (TextView) view2.findViewById(R.id.child_name);
            viewHolder.checkBox = (CheckBox) view2.findViewById(R.id.check_child_visibility);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final ChildLayout childLayout = this.list.get(i);
        viewHolder.name.setText(childLayout.name);
        viewHolder.checkBox.setOnCheckedChangeListener(null);
        viewHolder.checkBox.setChecked(this.currentList.contains(childLayout.name));
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ChildVisibilityAdapter.1
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ChildVisibilityAdapter.this.dialog.changeChildList(childLayout.name, z);
            }
        });
        return view2;
    }
}
