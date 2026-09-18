package com.qcl.launcher.launcher.list.local.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.dialogs.control.InputDialog;
import com.qcl.launcher.launcher.setting.SettingUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class FastTextAdapter extends BaseAdapter {
    private Context context;
    private InputDialog dialog;
    private ArrayList<String> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public FastTextAdapter(Context context, ArrayList<String> arrayList, InputDialog inputDialog) {
        this.context = context;
        this.list = arrayList;
        this.dialog = inputDialog;
    }

    /* loaded from: classes2.dex */
    private final class ViewHolder {
        ImageButton delete;
        LinearLayout item;
        TextView textView;

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
    public View getView(final int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_fast_text, (ViewGroup) null);
            viewHolder.item = (LinearLayout) view2.findViewById(R.id.item);
            viewHolder.textView = (TextView) view2.findViewById(R.id.fast_text);
            viewHolder.delete = (ImageButton) view2.findViewById(R.id.delete);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.textView.setText(this.list.get(i));
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.FastTextAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                FastTextAdapter.this.m436x20dd288d(i, view3);
            }
        });
        viewHolder.delete.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.FastTextAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                FastTextAdapter.this.m437x6e9ca08e(i, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-local-controller-FastTextAdapter, reason: not valid java name */
    public /* synthetic */ void m436x20dd288d(int i, View view) {
        this.dialog.editText.setText(">" + this.list.get(i));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-local-controller-FastTextAdapter, reason: not valid java name */
    public /* synthetic */ void m437x6e9ca08e(int i, View view) {
        this.list.remove(i);
        SettingUtils.saveFastText(this.list);
        notifyDataSetChanged();
    }
}
