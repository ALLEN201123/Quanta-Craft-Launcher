package com.qcl.launcher.launcher.dialogs.control;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import com.qcl.launcher.control.view.KeyCodeView;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class SelectKeycodeDialog extends Dialog implements KeyCodeView.OnKeyCodeChangeListener, View.OnClickListener {
    private LinearLayout keyboard;
    private ArrayList<Integer> list;
    private LinearLayout mouse;
    private OnKeyCodesChangeListener onKeyCodesChangeListener;
    private Button positive;

    /* loaded from: classes2.dex */
    public interface OnKeyCodesChangeListener {
        void onKeyCodesChange(ArrayList<Integer> arrayList);
    }

    public SelectKeycodeDialog(Context context, ArrayList<Integer> arrayList, OnKeyCodesChangeListener onKeyCodesChangeListener) {
        super(context);
        this.list = arrayList;
        this.onKeyCodesChangeListener = onKeyCodesChangeListener;
        setContentView(R.layout.dialog_select_keycode);
        setCancelable(false);
        init();
    }

    private void init() {
        Button button = (Button) findViewById(R.id.exit);
        this.positive = button;
        button.setOnClickListener(this);
        this.keyboard = (LinearLayout) findViewById(R.id.keyboard);
        this.mouse = (LinearLayout) findViewById(R.id.mouse);
        initializeAllButton(this.keyboard);
        initializeAllButton(this.mouse);
    }

    private void initializeAllButton(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            if (viewGroup.getChildAt(i) instanceof KeyCodeView) {
                ((KeyCodeView) viewGroup.getChildAt(i)).setOnKeyCodeChangeListener(this);
                ((KeyCodeView) viewGroup.getChildAt(i)).checkSelection(this.list);
            } else if (viewGroup.getChildAt(i) instanceof ViewGroup) {
                initializeAllButton((ViewGroup) viewGroup.getChildAt(i));
            }
        }
    }

    @Override // com.qcl.launcher.control.view.KeyCodeView.OnKeyCodeChangeListener
    public void onKeyCodeAdd(int i) {
        this.list.add(Integer.valueOf(i));
    }

    @Override // com.qcl.launcher.control.view.KeyCodeView.OnKeyCodeChangeListener
    public void onKeyCodeRemove(int i) {
        for (int i2 = 0; i2 < this.list.size(); i2++) {
            if (this.list.get(i2).intValue() == i) {
                this.list.remove(i2);
            }
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.positive) {
            this.onKeyCodesChangeListener.onKeyCodesChange(this.list);
            dismiss();
        }
    }
}
