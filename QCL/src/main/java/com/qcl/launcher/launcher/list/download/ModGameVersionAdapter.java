package com.qcl.launcher.launcher.list.download;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.SimpleMultimap;
import com.qcl.launcher.utils.animation.HiddenAnimationUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;
import com.qcl.launcher.utils.versioning.VersionNumber;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ModGameVersionAdapter extends BaseAdapter {
    private Context context;
    private int[] layoutHeights;
    private List<String> list;
    private DownloadResourceUI ui;
    private SimpleMultimap<String, RemoteMod.Version> versions;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public class ViewHolder {
        LinearLayout item;
        LinearLayout modListLayout;
        ListView modListView;
        TextView name;
        ImageView show;

        private ViewHolder() {
        }
    }

    public ModGameVersionAdapter(Context context, SimpleMultimap<String, RemoteMod.Version> simpleMultimap, DownloadResourceUI downloadResourceUI) {
        this.context = context;
        ArrayList arrayList = new ArrayList();
        this.list = arrayList;
        arrayList.addAll((Collection) simpleMultimap.keys().stream().sorted(VersionNumber.VERSION_COMPARATOR.reversed()).collect(Collectors.toList()));
        this.versions = simpleMultimap;
        this.layoutHeights = new int[this.list.size()];
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
        final ViewHolder viewHolder;
        if (view == null) {
            viewHolder = new ViewHolder();
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_download_mod_game_version, (ViewGroup) null);
            viewHolder.item = (LinearLayout) view2.findViewById(R.id.item);
            viewHolder.show = (ImageView) view2.findViewById(R.id.show_game_version);
            viewHolder.name = (TextView) view2.findViewById(R.id.game_version);
            viewHolder.modListLayout = (LinearLayout) view2.findViewById(R.id.mod_list_layout);
            viewHolder.modListView = (ListView) view2.findViewById(R.id.mod_list);
            view2.setTag(viewHolder);
        } else {
            view2 = view;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.name.setText(this.list.get(i));
        viewHolder.modListView.setAdapter((ListAdapter) new ModVersionAdapter(this.context, new ArrayList(this.versions.get(this.list.get(i))), this.ui));
        this.layoutHeights[i] = getListViewHeight(viewHolder.modListView) + ConvertUtils.dip2px(this.context, 24.0f);
        viewHolder.item.setOnClickListener(new View.OnClickListener() { // from class: com.qcl.launcher.launcher.list.download.ModGameVersionAdapter$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                ModGameVersionAdapter.this.m403xa44b7e3c(viewHolder, i, view3);
            }
        });
        return view2;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$getView$0$com-qcl-launcher-launcher-list-download-ModGameVersionAdapter, reason: not valid java name */
    public /* synthetic */ void m403xa44b7e3c(ViewHolder viewHolder, int i, View view) {
        this.ui.refreshVersionListHeight(viewHolder.modListLayout.getVisibility() == 0 ? -this.layoutHeights[i] : this.layoutHeights[i]);
        HiddenAnimationUtils.newInstance(this.context, viewHolder.modListLayout, viewHolder.show, this.layoutHeights[i]).toggle();
    }

    public static int getListViewHeight(ListView listView) {
        int count = listView.getAdapter().getCount();
        View view = listView.getAdapter().getView(0, null, listView);
        view.measure(0, 0);
        return (view.getMeasuredHeight() * count) + (listView.getDividerHeight() * (count - 1));
    }
}
