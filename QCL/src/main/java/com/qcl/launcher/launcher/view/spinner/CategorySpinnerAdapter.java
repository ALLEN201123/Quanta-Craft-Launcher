package com.qcl.launcher.launcher.view.spinner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseAddon;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class CategorySpinnerAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<RemoteModRepository.Category> list;
    private int rootId;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public CategorySpinnerAdapter(Context context, ArrayList<RemoteModRepository.Category> arrayList, int i) {
        this.context = context;
        this.list = arrayList;
        this.rootId = i;
    }

    /* loaded from: classes2.dex */
    private static class ViewHolder {
        CheckedTextView checkedTextView;

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
        String id;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_spinner_drop_down, (ViewGroup) null);
            viewHolder.checkedTextView = (CheckedTextView) view2.findViewById(R.id.checkedTextViewCustom);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        RemoteModRepository.Category category = this.list.get(i);
        int identifier = this.context.getResources().getIdentifier((category.getSelf() instanceof CurseAddon.Category ? "curse_category_" : "modrinth_category_") + category.getId().replace("-", "_"), "string", this.context.getPackageName());
        if (identifier != 0 && this.context.getString(identifier) != null) {
            id = this.context.getString(identifier);
        } else {
            id = category.getId();
        }
        if (category.getSelf() instanceof CurseAddon.Category) {
            if (((CurseAddon.Category) category.getSelf()).getParentCategoryId() == this.rootId || ((CurseAddon.Category) category.getSelf()).getParentCategoryId() == 0) {
                viewHolder.checkedTextView.setText(id);
            } else {
                viewHolder.checkedTextView.setText("    " + id);
            }
        } else {
            viewHolder.checkedTextView.setText(id);
        }
        return view2;
    }
}
