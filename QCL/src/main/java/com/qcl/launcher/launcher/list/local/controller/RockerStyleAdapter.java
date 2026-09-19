package com.qcl.launcher.launcher.list.local.controller;

import com.qcl.launcher.utils.QclColors;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.qcl.launcher.control.bean.rocker.RockerStyle;
import com.qcl.launcher.control.view.RockerView;
import com.qcl.launcher.launcher.dialogs.control.RockerStyleManagerDialog;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class RockerStyleAdapter extends BaseAdapter {
    private Context context;
    RockerStyleManagerDialog dialog;
    private ArrayList<RockerStyle> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public RockerStyleAdapter(Context context, ArrayList<RockerStyle> arrayList, RockerStyleManagerDialog rockerStyleManagerDialog) {
        this.context = context;
        this.list = arrayList;
        this.dialog = rockerStyleManagerDialog;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        RelativeLayout container;
        ImageButton delete;
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
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_rocker_style, (ViewGroup) null);
            viewHolder.container = (RelativeLayout) view2.findViewById(R.id.rocker_style);
            viewHolder.styleName = (TextView) view2.findViewById(R.id.rocker_style_name);
            viewHolder.delete = (ImageButton) view2.findViewById(R.id.delete_rocker_style);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        RockerStyle rockerStyle = this.list.get(i);
        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(ConvertUtils.dip2px(this.context, rockerStyle.cornerRadius));
        gradientDrawable.setStroke(ConvertUtils.dip2px(this.context, rockerStyle.strokeWidth), QclColors.parseSafe(rockerStyle.strokeColor, 0x33555555));
        gradientDrawable.setColor(QclColors.parseSafe(rockerStyle.fillColor, 0x666E6E6E));
        final GradientDrawable gradientDrawable2 = new GradientDrawable();
        gradientDrawable2.setCornerRadius(ConvertUtils.dip2px(this.context, rockerStyle.cornerRadiusPress));
        gradientDrawable2.setStroke(ConvertUtils.dip2px(this.context, rockerStyle.strokeWidthPress), QclColors.parseSafe(rockerStyle.strokeColorPress, 0x55555555));
        gradientDrawable2.setColor(QclColors.parseSafe(rockerStyle.fillColorPress, 0x995E5E5E));
        RockerView rockerView = new RockerView(this.context);
        rockerView.setPointerColor(rockerStyle.pointerColor);
        rockerView.setPointerColorPress(rockerStyle.pointerColorPress);
        rockerView.setFollowType(0);
        rockerView.setDoubleClick(false);
        rockerView.setOnShakeListener(new RockerView.OnShakeListener() { // from class: com.qcl.launcher.launcher.list.local.controller.RockerStyleAdapter.1
            @Override // com.qcl.launcher.control.view.RockerView.OnShakeListener
            public void onCenterDoubleClick(RockerView rockerView2) {
            }

            @Override // com.qcl.launcher.control.view.RockerView.OnShakeListener
            public void onShake(RockerView rockerView2, RockerView.Direction direction) {
            }

            @Override // com.qcl.launcher.control.view.RockerView.OnShakeListener
            public void onTouch(RockerView rockerView2) {
                rockerView2.setBackground(gradientDrawable2);
            }

            @Override // com.qcl.launcher.control.view.RockerView.OnShakeListener
            public void onFinish(RockerView rockerView2) {
                rockerView2.setBackground(gradientDrawable);
            }
        });
        rockerView.setBackground(gradientDrawable);
        viewHolder.container.addView(rockerView);
        rockerView.setSize(ConvertUtils.dip2px(this.context, 30.0f));
        viewHolder.styleName.setText(rockerStyle.name);
        viewHolder.delete.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.local.controller.RockerStyleAdapter.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view3) {
                RockerStyleAdapter.this.list.remove(i);
                SettingUtils.saveRockerStyle(RockerStyleAdapter.this.list);
                RockerStyleAdapter.this.dialog.refreshStyleList();
            }
        });
        return view2;
    }
}
