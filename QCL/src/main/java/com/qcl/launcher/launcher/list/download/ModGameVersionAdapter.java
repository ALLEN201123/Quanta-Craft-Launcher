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
import com.qcl.launcher.launcher.setting.SettingUtils;
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

    /**
     * ★ 1.2.5：适配玩家「当前游戏版本」的那一组（没有就是 null）。
     * 用户要求 —— 这一组排到最前面，点一下直接进下载框；这个模组没有适配你当前的
     * 版本时就不插这一行（顺序保持原来的版本号倒序）。
     */
    private String matchedGameVersion;

    public ModGameVersionAdapter(Context context, SimpleMultimap<String, RemoteMod.Version> simpleMultimap, DownloadResourceUI downloadResourceUI) {
        this.context = context;
        this.ui = downloadResourceUI;
        this.versions = simpleMultimap;
        ArrayList arrayList = new ArrayList();
        arrayList.addAll((Collection) simpleMultimap.keys().stream().sorted(VersionNumber.VERSION_COMPARATOR.reversed()).collect(Collectors.toList()));
        // ★ 1.2.5：把「适配你当前游戏版本」的那一组提到第一个
        this.matchedGameVersion = null;
        try {
            String cur = SettingUtils.getCurrentGameVersion(downloadResourceUI.activity);
            if (cur != null && !cur.isEmpty() && arrayList.contains(cur)) {
                arrayList.remove(cur);
                arrayList.add(0, cur);
                this.matchedGameVersion = cur;
            }
        }
        catch (Throwable ignored) {
        }
        this.list = arrayList;
        this.layoutHeights = new int[this.list.size()];
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
        final String gameVersionName = this.list.get(i);
        // ★ 1.2.5：适配玩家当前版本的那一组加个标记，一眼能认出来
        final boolean isMatched = this.matchedGameVersion != null
                && this.matchedGameVersion.equals(gameVersionName);
        viewHolder.name.setText((CharSequence) (isMatched
                ? gameVersionName + "   ★ 适配你当前的版本"
                : gameVersionName));
        final ModVersionAdapter innerAdapter =
                new ModVersionAdapter(this.context, new ArrayList(this.versions.get(gameVersionName)), this.ui);
        viewHolder.modListView.setAdapter((ListAdapter) innerAdapter);
        this.layoutHeights[i] = getListViewHeight(viewHolder.modListView) + ConvertUtils.dip2px(this.context, 24.0f);
        viewHolder.item.setOnClickListener(new View.OnClickListener() {
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                // ★ 1.2.5：适配你当前版本的那一组 —— 点一下直接进下载框（列表第一项 = 最新），
                //   不用先展开再找；其它分组还是原来的展开/收起。
                if (isMatched && innerAdapter.getCount() > 0) {
                    innerAdapter.triggerDownload(0);
                    return;
                }
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
