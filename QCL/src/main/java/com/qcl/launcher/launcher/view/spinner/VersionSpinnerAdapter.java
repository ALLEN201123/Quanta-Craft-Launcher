package com.qcl.launcher.launcher.view.spinner;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
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

    /** ★ 1.2.5：多带一个「游戏目录」，这样没 icon.png 的版本也能按加载器取图标 */
    private final String gameFileDirectory;

    public VersionSpinnerAdapter(Context context, ArrayList<GameListBean> arrayList) {
        this(context, arrayList, "");
    }

    public VersionSpinnerAdapter(Context context, ArrayList<GameListBean> arrayList,
                                 String gameFileDirectory) {
        this.context = context;
        this.list = arrayList;
        this.gameFileDirectory = gameFileDirectory == null ? "" : gameFileDirectory;
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
            // ★ 1.2.9：改成异步加载 + 缓存（以前是主线程读文件解码）
            com.qcl.launcher.launcher.list.local.game.LocalIconLoader.loadBackground(viewHolder.icon, gameListBean.iconPath);
        } else {
            // ★★★ 1.2.5 修：原来这里按「version 里有没有逗号」分叉 —— 有逗号的
            //   （下载页装的 Fabric/Forge 版本，版本号是 "b1.7.3, xxx" 这种）显示
            //   ic_furnace（熔炉），版本设置页却按加载器显示 fabric logo，
            //   两边图标对不上。现在统一走「装了哪个加载器就显示哪个」同一套规则。
            //   （iconPath = <游戏目录>/versions/<版本名>/icon.png，取上级目录就是版本目录）
            //   version 目录：优先用 gameFileDirectory 拼，拿不到再从 iconPath 反推
            File versionDir = this.gameFileDirectory.isEmpty()
                    ? (gameListBean.iconPath.equals("") ? null : new File(gameListBean.iconPath).getParentFile())
                    : new File(this.gameFileDirectory + "/versions/" + gameListBean.name);
            int loaderIcon = ModLoaderDetector.iconRes(versionDir);
            viewHolder.icon.setBackground(this.context.getDrawable(
                    loaderIcon != 0 ? loaderIcon : R.drawable.ic_grass));
        }
        viewHolder.name.setText(gameListBean.name);
        viewHolder.version.setText(gameListBean.version);
        return view2;
    }
}
