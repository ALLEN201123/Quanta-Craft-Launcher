package com.qcl.launcher.launcher.list.download.minecraft;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.GameUpdateDialog;
import com.qcl.launcher.launcher.mod.RemoteMod;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadQuiltAPIListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private boolean install;
    private String mcVersion;
    private ArrayList<RemoteMod.Version> versions;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public DownloadQuiltAPIListAdapter(Context context, MainActivity mainActivity, String str, ArrayList<RemoteMod.Version> arrayList, boolean z) {
        this.context = context;
        this.activity = mainActivity;
        this.mcVersion = str;
        this.versions = arrayList;
        this.install = z;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        ImageView icon;
        LinearLayout item;
        TextView mcVersion;
        TextView quiltAPIId;
        TextView time;

        private ViewHolder() {
        }
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
            viewHolder.quiltAPIId = (TextView) view2.findViewById(R.id.id);
            viewHolder.mcVersion = (TextView) view2.findViewById(R.id.type);
            viewHolder.time = (TextView) view2.findViewById(R.id.release_time);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final RemoteMod.Version version = this.versions.get(i);
        viewHolder.icon.setImageDrawable(this.context.getDrawable(R.drawable.ic_quilt));
        viewHolder.quiltAPIId.setText(version.getVersion());
        viewHolder.mcVersion.setText(this.mcVersion);
        viewHolder.time.setText(DateTimeFormatter.ofPattern(this.context.getString(R.string.time_pattern)).withZone(ZoneId.systemDefault()).format(version.getDatePublished().toInstant()));
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadQuiltAPIListAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                DownloadQuiltAPIListAdapter.this.m421x7e8ecead(version, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-minecraft-DownloadQuiltAPIListAdapter, reason: not valid java name */
    public /* synthetic */ void m421x7e8ecead(RemoteMod.Version version, View view) {
        if (this.install) {
            Context context = this.context;
            MainActivity mainActivity = this.activity;
            new GameUpdateDialog(context, mainActivity, mainActivity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.versionName, this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.gameVersion, 6, version).show();
        } else {
            this.activity.uiManager.installGameUI.quiltAPIVersion = version;
            this.activity.backToLastUI();
        }
    }
}
