package com.qcl.launcher.launcher.list.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.launcher.uis.game.manager.universal.ModUpdateUI;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ModUpdateListAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<LocalModFile.ModUpdate> list;
    private final ModUpdateUI ui;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public ModUpdateListAdapter(Context context, ModUpdateUI modUpdateUI) {
        this.context = context;
        this.ui = modUpdateUI;
        this.list = modUpdateUI.modUpdates;
    }

    /* loaded from: classes2.dex */
    private static class ViewHolder {
        CheckBox checkBox;
        TextView current;
        TextView file;
        TextView source;
        TextView target;

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
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_mod_update, (ViewGroup) null);
            viewHolder.checkBox = (CheckBox) view2.findViewById(R.id.check_update);
            viewHolder.file = (TextView) view2.findViewById(R.id.file);
            viewHolder.current = (TextView) view2.findViewById(R.id.current);
            viewHolder.target = (TextView) view2.findViewById(R.id.target);
            viewHolder.source = (TextView) view2.findViewById(R.id.source);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        final LocalModFile.ModUpdate modUpdate = this.list.get(i);
        viewHolder.checkBox.setChecked(this.ui.selectedMods.contains(modUpdate));
        viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.qcl.launcher.launcher.list.download.ModUpdateListAdapter$$ExternalSyntheticLambda0
            @Override // android.widget.CompoundButton.OnCheckedChangeListener
            public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                ModUpdateListAdapter.this.m404xf52ab395(modUpdate, compoundButton, z);
            }
        });
        viewHolder.file.setText(modUpdate.getLocalMod().getFileName());
        viewHolder.current.setText(modUpdate.getCurrentVersion().getVersion());
        viewHolder.target.setText(modUpdate.getCandidates().get(0).getVersion());
        viewHolder.source.setText("CurseForge");
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-ModUpdateListAdapter, reason: not valid java name */
    public /* synthetic */ void m404xf52ab395(LocalModFile.ModUpdate modUpdate, CompoundButton compoundButton, boolean z) {
        if (z) {
            if (this.ui.selectedMods.contains(modUpdate)) {
                return;
            }
            this.ui.selectedMods.add(modUpdate);
            return;
        }
        this.ui.selectedMods.remove(modUpdate);
    }
}
