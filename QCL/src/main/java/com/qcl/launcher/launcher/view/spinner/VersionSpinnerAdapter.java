package com.qcl.launcher.launcher.view.spinner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.qcl.launcher.launcher.list.local.game.GameListBean;
import com.qcl.launcher.utils.file.DrawableUtils;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class VersionSpinnerAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<GameListBean> list;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public VersionSpinnerAdapter(Context context, ArrayList<GameListBean> arrayList) {
        this.context = context;
        this.list = arrayList;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        ImageView icon;
        TextView name;
        TextView version;

        private ViewHolder() {
        }
    }

    public int getPosition(GameListBean gameListBean) {
        for (int i = 0; i < this.list.size(); i++) {
            if (this.list.get(i).iconPath.equals(gameListBean.iconPath) && this.list.get(i).name.equals(gameListBean.name) && this.list.get(i).version.equals(gameListBean.version)) {
                return i;
            }
        }
        return 0;
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
            view2 = LayoutInflater.from(this.context).inflate(R.layout.spinner_item_local_version, (ViewGroup) null);
            viewHolder.icon = (ImageView) view2.findViewById(R.id.icon);
            viewHolder.name = (TextView) view2.findViewById(R.id.name);
            viewHolder.version = (TextView) view2.findViewById(R.id.version);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        GameListBean gameListBean = this.list.get(i);
        if (!gameListBean.iconPath.equals("") && new File(gameListBean.iconPath).exists()) {
            viewHolder.icon.setBackground(DrawableUtils.getDrawableFromFile(gameListBean.iconPath));
        } else if (!gameListBean.version.contains(",")) {
            viewHolder.icon.setBackground(this.context.getDrawable(R.drawable.ic_grass));
        } else {
            viewHolder.icon.setBackground(this.context.getDrawable(R.drawable.ic_furnace));
        }
        viewHolder.name.setText(gameListBean.name);
        viewHolder.version.setText(gameListBean.version);
        return view2;
    }
}
