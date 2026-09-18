package com.qcl.launcher.launcher.list.download.minecraft;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadGameListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private ArrayList<VersionManifest.Version> versions;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        ImageView icon;
        LinearLayout item;
        TextView mcId;
        TextView releaseTime;
        TextView type;

        private ViewHolder() {
        }
    }

    private String getType(String str) {
        if (str.equals("release")) {
            return this.context.getString(R.string.download_minecraft_ui_release);
        }
        if (str.equals("snapshot")) {
            return this.context.getString(R.string.download_minecraft_ui_snapshot);
        }
        if ("archive".equals(str)) {
            return this.context.getString(R.string.download_minecraft_ui_archive);
        }
        return this.context.getString(R.string.download_minecraft_ui_old);
    }

    private Drawable getIcon(String str) {
        if (str.equals("release")) {
            return this.context.getDrawable(R.drawable.ic_grass);
        }
        if (str.equals("snapshot")) {
            return this.context.getDrawable(R.drawable.ic_command);
        }
        return this.context.getDrawable(R.drawable.ic_craft_table);
    }

    public DownloadGameListAdapter(Context context, MainActivity mainActivity, ArrayList<VersionManifest.Version> arrayList) {
        this.context = context;
        this.activity = mainActivity;
        this.versions = arrayList;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.versions.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.versions.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_download_game_list, (ViewGroup) null);
            viewHolder.item = (LinearLayout) view2.findViewById(R.id.item);
            viewHolder.icon = (ImageView) view2.findViewById(R.id.icon);
            viewHolder.mcId = (TextView) view2.findViewById(R.id.id);
            viewHolder.type = (TextView) view2.findViewById(R.id.type);
            viewHolder.releaseTime = (TextView) view2.findViewById(R.id.release_time);
            this.activity.exteriorConfig.apply(viewHolder.type);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final VersionManifest.Version version = this.versions.get(i);
        viewHolder.icon.setImageDrawable(getIcon(version.type));
        viewHolder.mcId.setText(version.id);
        viewHolder.type.setText(getType(version.type));
        viewHolder.releaseTime.setText(version.releaseTime == null ? "" : DateTimeFormatter.ofPattern(this.context.getString(R.string.time_pattern)).withZone(ZoneId.systemDefault()).format(version.releaseTime.toInstant()));
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadGameListAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                DownloadGameListAdapter.this.m414x716a5e8(version, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-minecraft-DownloadGameListAdapter, reason: not valid java name */
    public /* synthetic */ void m414x716a5e8(VersionManifest.Version version, View view) {
        this.activity.uiManager.installGameUI.name = version.id;
        this.activity.uiManager.installGameUI.fabricVersion = null;
        this.activity.uiManager.installGameUI.fabricAPIVersion = null;
        this.activity.uiManager.installGameUI.forgeVersion = null;
        this.activity.uiManager.installGameUI.optifineVersion = null;
        this.activity.uiManager.installGameUI.liteLoaderVersion = null;
        this.activity.uiManager.installGameUI.version = version;
        this.activity.uiManager.switchMainUI(this.activity.uiManager.installGameUI);
    }
}
