package com.qcl.launcher.launcher.list.download;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.dialogs.EditDownloadNameDialog;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.manifest.AppManifest;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FolderChooser;
import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ModVersionAdapter extends BaseAdapter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
    private Context context;
    private List<RemoteMod.Version> list;
    private DownloadResourceUI ui;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    /* loaded from: classes2.dex */
    private class ViewHolder {
        TextView date;
        ImageView icon;
        LinearLayout item;
        TextView name;
        ImageButton select;
        TextView type;

        private ViewHolder() {
        }
    }

    public ModVersionAdapter(Context context, List<RemoteMod.Version> list, DownloadResourceUI downloadResourceUI) {
        this.context = context;
        this.list = list;
        this.ui = downloadResourceUI;
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
        Context context;
        int i2;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_download_mod_version, (ViewGroup) null);
            viewHolder.item = (LinearLayout) view2.findViewById(R.id.item);
            viewHolder.icon = (ImageView) view2.findViewById(R.id.mod_type_icon);
            viewHolder.name = (TextView) view2.findViewById(R.id.mod_name);
            viewHolder.type = (TextView) view2.findViewById(R.id.mod_type);
            viewHolder.date = (TextView) view2.findViewById(R.id.mod_date);
            viewHolder.select = (ImageButton) view2.findViewById(R.id.save_path);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        if (this.list.get(i).getVersionType() == RemoteMod.VersionType.Alpha) {
            viewHolder.icon.setBackground(this.context.getDrawable(R.drawable.ic_outline_alpha_black));
        } else if (this.list.get(i).getVersionType() == RemoteMod.VersionType.Beta) {
            viewHolder.icon.setBackground(this.context.getDrawable(R.drawable.ic_outline_beta_black));
        } else {
            viewHolder.icon.setBackground(this.context.getDrawable(R.drawable.ic_outline_release_black));
        }
        viewHolder.name.setText(this.list.get(i).getName());
        TextView textView = viewHolder.type;
        if (this.list.get(i).getVersionType() == RemoteMod.VersionType.Release) {
            context = this.context;
            i2 = R.string.download_resource_release;
        } else {
            context = this.context;
            i2 = R.string.download_resource_beta;
        }
        textView.setText(context.getString(i2));
        viewHolder.date.setText(FORMATTER.format(this.list.get(i).getDatePublished().toInstant()));
        viewHolder.select.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.ModVersionAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                ModVersionAdapter.this.m405x7d91572a(i, view3);
            }
        });
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.ModVersionAdapter$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                ModVersionAdapter.this.m406x8e4723eb(i, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-ModVersionAdapter, reason: not valid java name */
    public /* synthetic */ void m405x7d91572a(int i, View view) {
        this.ui.selectedVersion = this.list.get(i);
        Intent intent = new Intent(this.context, (Class<?>) FolderChooser.class);
        intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
        intent.putExtra("INITIAL_DIRECTORY", new File(AppManifest.DEFAULT_GAME_DIR).getAbsolutePath());
        this.ui.activity.startActivityForResult(intent, 2700);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$1$com-qcl-launcher-launcher-list-download-ModVersionAdapter, reason: not valid java name */
    public /* synthetic */ void m406x8e4723eb(int i, View view) {
        this.ui.selectedVersion = this.list.get(i);
        if (this.ui.resourceType == 0 || this.ui.resourceType == 2) {
            new EditDownloadNameDialog(this.context, this.ui, this.list.get(i), true, null).show();
        }
        int i2 = this.ui.resourceType;
        if (this.ui.resourceType == 3) {
            Intent intent = new Intent(this.context, (Class<?>) FolderChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("INITIAL_DIRECTORY", new File(AppManifest.DEFAULT_GAME_DIR).getAbsolutePath());
            this.ui.activity.startActivityForResult(intent, 2700);
        }
    }
}
