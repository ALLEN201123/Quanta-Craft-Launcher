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
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadForgeListAdapter extends BaseAdapter {
    private MainActivity activity;
    private Context context;
    private boolean install;
    private ArrayList<ForgeVersion> versions;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public DownloadForgeListAdapter(Context context, MainActivity mainActivity, ArrayList<ForgeVersion> arrayList, boolean z) {
        this.context = context;
        this.activity = mainActivity;
        this.versions = arrayList;
        this.install = z;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        TextView forgeId;
        ImageView icon;
        LinearLayout item;
        TextView mcVersion;
        TextView releaseTime;

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
            viewHolder.forgeId = (TextView) view2.findViewById(R.id.id);
            viewHolder.mcVersion = (TextView) view2.findViewById(R.id.type);
            viewHolder.releaseTime = (TextView) view2.findViewById(R.id.release_time);
            this.activity.exteriorConfig.apply(viewHolder.mcVersion);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final ForgeVersion forgeVersion = this.versions.get(i);
        viewHolder.icon.setImageDrawable(this.context.getDrawable(R.drawable.ic_forge));
        viewHolder.forgeId.setText(forgeVersion.getVersion());
        viewHolder.mcVersion.setText(forgeVersion.getGameVersion());
        viewHolder.releaseTime.setText(DateTimeFormatter.ofPattern(this.context.getString(R.string.time_pattern)).withZone(ZoneId.systemDefault()).format(Instant.parse(forgeVersion.getModified())));
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadForgeListAdapter$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                DownloadForgeListAdapter.this.m413xecaed25f(forgeVersion, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$2$com-qcl-launcher-launcher-list-download-minecraft-DownloadForgeListAdapter, reason: not valid java name */
    public /* synthetic */ void m413xecaed25f(final ForgeVersion forgeVersion, View view) {
        if (this.install) {
            if (this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.forgeVersion != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
                builder.setTitle(this.context.getString(R.string.dialog_change_version_title));
                builder.setMessage(this.context.getString(R.string.dialog_change_version_msg).replace("%s", "Forge").replace("%v1", this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.forgeVersion).replace("%v2", forgeVersion.getVersion()));
                builder.setPositiveButton(this.context.getString(R.string.dialog_change_version_positive), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadForgeListAdapter$$ExternalSyntheticLambda1
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DownloadForgeListAdapter.this.m411x688077a1(forgeVersion, dialogInterface, i);
                    }
                });
                builder.setNegativeButton(this.context.getString(R.string.dialog_change_version_negative), new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.minecraft.DownloadForgeListAdapter$$ExternalSyntheticLambda0
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        DownloadForgeListAdapter.this.m412xaa97a500(dialogInterface, i);
                    }
                });
                builder.create().show();
                return;
            }
            update(forgeVersion);
            return;
        }
        this.activity.uiManager.installGameUI.forgeVersion = forgeVersion;
        this.activity.backToLastUI();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-minecraft-DownloadForgeListAdapter, reason: not valid java name */
    public /* synthetic */ void m411x688077a1(ForgeVersion forgeVersion, DialogInterface dialogInterface, int i) {
        update(forgeVersion);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-download-minecraft-DownloadForgeListAdapter, reason: not valid java name */
    public /* synthetic */ void m412xaa97a500(DialogInterface dialogInterface, int i) {
        this.activity.backToLastUI();
    }

    private void update(ForgeVersion forgeVersion) {
        Context context = this.context;
        MainActivity mainActivity = this.activity;
        new GameUpdateDialog(context, mainActivity, mainActivity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.versionName, this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.gameVersion, 0, forgeVersion).show();
    }
}
