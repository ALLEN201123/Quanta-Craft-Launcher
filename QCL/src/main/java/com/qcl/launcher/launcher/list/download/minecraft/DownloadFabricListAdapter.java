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
import com.qcl.launcher.launcher.download.fabric.FabricLoaderVersion;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadFabricListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private boolean install;
    private String mcVersion;
    private ArrayList<FabricLoaderVersion> versions;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public DownloadFabricListAdapter(Context context, MainActivity mainActivity, String str, ArrayList<FabricLoaderVersion> arrayList, boolean z) {
        this.context = context;
        this.activity = mainActivity;
        this.mcVersion = str;
        this.versions = arrayList;
        this.install = z;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        TextView fabricId;
        ImageView icon;
        LinearLayout item;
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
            viewHolder.fabricId = (TextView) view2.findViewById(R.id.id);
            viewHolder.mcVersion = (TextView) view2.findViewById(R.id.release_time);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final FabricLoaderVersion fabricLoaderVersion = this.versions.get(i);
        viewHolder.icon.setImageDrawable(this.context.getDrawable(R.drawable.ic_fabric));
        viewHolder.fabricId.setText(fabricLoaderVersion.version);
        viewHolder.mcVersion.setText(this.mcVersion);
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadFabricListAdapter$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                DownloadFabricListAdapter.this.m410x52a49e17(fabricLoaderVersion, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$2$com-qcl-launcher-launcher-list-download-minecraft-DownloadFabricListAdapter, reason: not valid java name */
    public /* synthetic */ void m410x52a49e17(final FabricLoaderVersion fabricLoaderVersion, View view) {
        if (this.install) {
            if (this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.fabricVersion != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
                builder.setTitle(this.context.getString(R.string.dialog_change_version_title));
                builder.setMessage(this.context.getString(R.string.dialog_change_version_msg).replace("%s", "Fabric").replace("%v1", this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.fabricVersion).replace("%v2", fabricLoaderVersion.version));
                builder.setPositiveButton(this.context.getString(R.string.dialog_change_version_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadFabricListAdapter$$ExternalSyntheticLambda1
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DownloadFabricListAdapter.this.m408x5107a115(fabricLoaderVersion, dialogInterface, i);
                    }
                });
                builder.setNegativeButton(this.context.getString(R.string.dialog_change_version_negative), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadFabricListAdapter$$ExternalSyntheticLambda0
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DownloadFabricListAdapter.this.m409x51d61f96(dialogInterface, i);
                    }
                });
                builder.create().show();
                return;
            }
            update(fabricLoaderVersion);
            return;
        }
        this.activity.uiManager.installGameUI.fabricVersion = fabricLoaderVersion;
        this.activity.backToLastUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-minecraft-DownloadFabricListAdapter, reason: not valid java name */
    public /* synthetic */ void m408x5107a115(FabricLoaderVersion fabricLoaderVersion, DialogInterface dialogInterface, int i) {
        update(fabricLoaderVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-download-minecraft-DownloadFabricListAdapter, reason: not valid java name */
    public /* synthetic */ void m409x51d61f96(DialogInterface dialogInterface, int i) {
        this.activity.backToLastUI();
    }

    private void update(FabricLoaderVersion fabricLoaderVersion) {
        Context context = this.context;
        MainActivity mainActivity = this.activity;
        new GameUpdateDialog(context, mainActivity, mainActivity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.versionName, this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.gameVersion, 3, fabricLoaderVersion).show();
    }
}
