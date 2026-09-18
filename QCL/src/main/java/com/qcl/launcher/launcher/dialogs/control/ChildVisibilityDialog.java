package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.qcl.launcher.launcher.list.local.controller.ChildVisibilityAdapter;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ChildVisibilityDialog extends Dialog implements View.OnClickListener {
    private ArrayList<String> list;
    private ListView listView;
    private OnChildVisibilityChangeListener onChildVisibilityChangeListener;
    private String pattern;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnChildVisibilityChangeListener {
        void onChildVisibilityChange(ArrayList<String> arrayList);
    }

    public ChildVisibilityDialog(Context context, String str, ArrayList<String> arrayList, OnChildVisibilityChangeListener onChildVisibilityChangeListener) {
        super(context);
        this.pattern = str;
        this.list = arrayList;
        this.onChildVisibilityChangeListener = onChildVisibilityChangeListener;
        setContentView(R.layout.dialog_child_visibility);
        setCancelable(false);
        init();
    }

    private void init() {
        this.listView = (ListView) findViewById(R.id.child_list);
        Button button = (Button) findViewById(R.id.exit);
        this.positive = button;
        button.setOnClickListener(this);
        this.listView.setAdapter((ListAdapter) new ChildVisibilityAdapter(getContext(), this.list, this.pattern, this));
    }

    public void changeChildList(String str, boolean z) {
        if (z) {
            this.list.add(str);
        } else {
            this.list.remove(str);
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            this.onChildVisibilityChangeListener.onChildVisibilityChange(this.list);
            dismiss();
        }
    }
}
