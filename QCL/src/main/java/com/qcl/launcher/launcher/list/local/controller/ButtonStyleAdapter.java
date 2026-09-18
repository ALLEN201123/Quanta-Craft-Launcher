package com.qcl.launcher.launcher.list.local.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import com.qcl.launcher.control.bean.button.ButtonStyle;
import com.qcl.launcher.launcher.dialogs.control.ButtonStyleManagerDialog;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ButtonStyleAdapter extends BaseAdapter {
    private Context context;
    private ButtonStyleManagerDialog dialog;
    private ArrayList<ButtonStyle> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public ButtonStyleAdapter(Context context, ArrayList<ButtonStyle> arrayList, ButtonStyleManagerDialog buttonStyleManagerDialog) {
        this.context = context;
        this.list = arrayList;
        this.dialog = buttonStyleManagerDialog;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        ImageButton delete;
        Button styleButton;
        TextView styleName;

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
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_button_style, (ViewGroup) null);
            viewHolder.styleButton = (Button) view2.findViewById(R.id.button_style);
            viewHolder.styleName = (TextView) view2.findViewById(R.id.button_style_name);
            viewHolder.delete = (ImageButton) view2.findViewById(R.id.delete_button_style);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final ButtonStyle buttonStyle = this.list.get(i);
        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(ConvertUtils.dip2px(this.context, buttonStyle.cornerRadius));
        gradientDrawable.setStroke(ConvertUtils.dip2px(this.context, buttonStyle.strokeWidth), Color.parseColor(buttonStyle.strokeColor));
        gradientDrawable.setColor(Color.parseColor(buttonStyle.fillColor));
        final GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setCornerRadius(ConvertUtils.dip2px(this.context, buttonStyle.cornerRadiusPress));
        gradientDrawable2.setStroke(ConvertUtils.dip2px(this.context, buttonStyle.strokeWidthPress), Color.parseColor(buttonStyle.strokeColorPress));
        gradientDrawable2.setColor(Color.parseColor(buttonStyle.fillColorPress));
        viewHolder.styleButton.setGravity(17);
        viewHolder.styleButton.setPadding(0, 0, 0, 0);
        viewHolder.styleButton.setText("S");
        viewHolder.styleButton.setAllCaps(false);
        viewHolder.styleButton.setTextSize(buttonStyle.textSize);
        viewHolder.styleButton.setTextColor(Color.parseColor(buttonStyle.textColor));
        viewHolder.styleButton.setBackground(gradientDrawable);
        viewHolder.styleButton.setOnTouchListener(new View.OnTouchListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ButtonStyleAdapter.1
            @Override // android.view.View.OnTouchListener
            public boolean onTouch(View view3, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == 0) {
                    Button button = (Button) view3;
                    button.setTextSize(buttonStyle.textSizePress);
                    button.setTextColor(Color.parseColor(buttonStyle.textColorPress));
                    button.setBackground(gradientDrawable2);
                }
                if (motionEvent.getActionMasked() == 1 || motionEvent.getActionMasked() == 3) {
                    Button button2 = (Button) view3;
                    button2.setTextSize(buttonStyle.textSize);
                    button2.setTextColor(Color.parseColor(buttonStyle.textColor));
                    button2.setBackground(gradientDrawable);
                }
                return true;
            }
        });
        viewHolder.styleName.setText(buttonStyle.name);
        viewHolder.delete.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.ButtonStyleAdapter.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view3) {
                ButtonStyleAdapter.this.list.remove(i);
                SettingUtils.saveButtonStyle(ButtonStyleAdapter.this.list);
                ButtonStyleAdapter.this.dialog.refreshStyleList();
            }
        });
        return view2;
    }
}
