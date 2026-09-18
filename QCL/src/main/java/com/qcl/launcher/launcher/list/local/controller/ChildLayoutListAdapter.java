package com.qcl.launcher.launcher.list.local.controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import com.qcl.launcher.launcher.dialogs.control.ChildManagerDialog;
import com.qcl.launcher.launcher.dialogs.control.EditChildDialog;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.FileUtils;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ChildLayoutListAdapter extends BaseAdapter {
    private ChildManagerDialog childManagerDialog;
    private Context context;
    private ControlPattern controlPattern;
    private ArrayList<ChildLayout> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public ChildLayoutListAdapter(Context context, ArrayList<ChildLayout> arrayList, ControlPattern controlPattern, ChildManagerDialog childManagerDialog) {
        this.context = context;
        this.list = arrayList;
        this.controlPattern = controlPattern;
        this.childManagerDialog = childManagerDialog;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        ImageButton delete;
        ImageButton edit;
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
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_child_layout, (ViewGroup) null);
            viewHolder.name = (TextView) view2.findViewById(R.id.child_name);
            viewHolder.edit = (ImageButton) view2.findViewById(R.id.edit_child);
            viewHolder.delete = (ImageButton) view2.findViewById(R.id.delete_child);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final ChildLayout childLayout = this.list.get(i);
        viewHolder.name.setText(childLayout.name);
        viewHolder.edit.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ChildLayoutListAdapter.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view3) {
                new EditChildDialog(ChildLayoutListAdapter.this.context, ChildLayoutListAdapter.this.controlPattern.name, new EditChildDialog.OnChildChangeListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ChildLayoutListAdapter.1.1
                    @Override // com.qcl.launcher.launcher.dialogs.control.EditChildDialog.OnChildChangeListener
                    public void onChildChange(ChildLayout childLayout2) {
                        FileUtils.rename(AppManifest.CONTROLLER_DIR + "/" + ChildLayoutListAdapter.this.controlPattern.name + "/" + childLayout.name + ".json", childLayout2.name + ".json");
                        ChildLayout.saveChildLayout(ChildLayoutListAdapter.this.controlPattern.name, childLayout2);
                        ChildLayoutListAdapter.this.childManagerDialog.refreshListView();
                    }
                }, childLayout).show();
            }
        });
        viewHolder.delete.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ChildLayoutListAdapter.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view3) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChildLayoutListAdapter.this.context);
                builder.setTitle(ChildLayoutListAdapter.this.context.getString(R.string.dialog_delete_child_title));
                builder.setMessage(ChildLayoutListAdapter.this.context.getString(R.string.dialog_delete_child_content));
                builder.setPositiveButton(ChildLayoutListAdapter.this.context.getString(R.string.dialog_delete_child_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ChildLayoutListAdapter.2.1
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                        if (new File(AppManifest.CONTROLLER_DIR + "/" + ChildLayoutListAdapter.this.controlPattern.name + "/" + childLayout.name + ".json").delete()) {
                            ChildLayoutListAdapter.this.childManagerDialog.refreshListView();
                        }
                    }
                });
                builder.setNegativeButton(ChildLayoutListAdapter.this.context.getString(R.string.dialog_delete_child_negative), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ChildLayoutListAdapter.2.2
                    @Override // android.content.DialogInterface.OnClickListener
                    public void onClick(DialogInterface dialogInterface, int i2) {
                    }
                });
                builder.create().show();
            }
        });
        return view2;
    }
}
