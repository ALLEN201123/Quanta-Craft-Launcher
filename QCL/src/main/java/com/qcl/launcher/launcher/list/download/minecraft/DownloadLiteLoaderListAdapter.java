package com.qcl.launcher.launcher.list.download.minecraft;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.GameUpdateDialog;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderVersion;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadLiteLoaderListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private boolean install;
    private String mcVersion;
    private ArrayList<LiteLoaderVersion> versions;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public DownloadLiteLoaderListAdapter(Context context, MainActivity mainActivity, String str, ArrayList<LiteLoaderVersion> arrayList, boolean z) {
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
        TextView liteLoaderId;
        TextView mcVersion;

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
            viewHolder.liteLoaderId = (TextView) view2.findViewById(R.id.id);
            viewHolder.mcVersion = (TextView) view2.findViewById(R.id.release_time);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final LiteLoaderVersion liteLoaderVersion = this.versions.get(i);
        viewHolder.icon.setImageDrawable(this.context.getDrawable(R.drawable.ic_chicken));
        viewHolder.liteLoaderId.setText(liteLoaderVersion.getVersion());
        viewHolder.mcVersion.setText(this.mcVersion);
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadLiteLoaderListAdapter$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                DownloadLiteLoaderListAdapter.this.m417xd9fcd5b(liteLoaderVersion, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$2$com-qcl-launcher-launcher-list-download-minecraft-DownloadLiteLoaderListAdapter, reason: not valid java name */
    public /* synthetic */ void m417xd9fcd5b(final LiteLoaderVersion liteLoaderVersion, View view) {
        if (this.install) {
            if (this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.liteLoaderVersion != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
                builder.setTitle(this.context.getString(R.string.dialog_change_version_title));
                builder.setMessage(this.context.getString(R.string.dialog_change_version_msg).replace("%s", "LiteLoader").replace("%v1", this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.liteLoaderVersion).replace("%v2", liteLoaderVersion.getVersion()));
                builder.setPositiveButton(this.context.getString(R.string.dialog_change_version_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadLiteLoaderListAdapter$$ExternalSyntheticLambda1
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DownloadLiteLoaderListAdapter.this.m415x4cad2159(liteLoaderVersion, dialogInterface, i);
                    }
                });
                builder.setNegativeButton(this.context.getString(R.string.dialog_change_version_negative), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadLiteLoaderListAdapter$$ExternalSyntheticLambda0
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DownloadLiteLoaderListAdapter.this.m416x2d26775a(dialogInterface, i);
                    }
                });
                builder.create().show();
                return;
            }
            update(liteLoaderVersion);
            return;
        }
        this.activity.uiManager.installGameUI.liteLoaderVersion = liteLoaderVersion;
        this.activity.backToLastUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-minecraft-DownloadLiteLoaderListAdapter, reason: not valid java name */
    public /* synthetic */ void m415x4cad2159(LiteLoaderVersion liteLoaderVersion, DialogInterface dialogInterface, int i) {
        update(liteLoaderVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-download-minecraft-DownloadLiteLoaderListAdapter, reason: not valid java name */
    public /* synthetic */ void m416x2d26775a(DialogInterface dialogInterface, int i) {
        this.activity.backToLastUI();
    }

    private void update(LiteLoaderVersion liteLoaderVersion) {
        Context context = this.context;
        MainActivity mainActivity = this.activity;
        new GameUpdateDialog(context, mainActivity, mainActivity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.versionName, this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.gameVersion, 1, liteLoaderVersion).show();
    }
}
