package com.qcl.launcher.launcher.list.download.minecraft;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersion;

import java.util.ArrayList;

/**
 * ★ NeoForge 版本列表适配器（照 {@link DownloadForgeListAdapter} 写）。
 *
 * 列表项沿用 item_download_game_list：
 *   id    = NeoForge 版本号（如 21.1.72）
 *   type  = 对应的 MC 版本（如 MC 1.21.1）
 */
public class DownloadNeoForgeListAdapter extends BaseAdapter {

    private final Context context;
    private final MainActivity activity;
    private final ArrayList<NeoForgeVersion> versions;
    private final OnNeoForgeVersionClickListener listener;

    public interface OnNeoForgeVersionClickListener {
        void onSelect(NeoForgeVersion version);
    }

    public DownloadNeoForgeListAdapter(Context context, MainActivity activity,
                                       ArrayList<NeoForgeVersion> versions,
                                       OnNeoForgeVersionClickListener listener) {
        this.context = context;
        this.activity = activity;
        this.versions = versions;
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return versions.size();
    }

    @Override
    public Object getItem(int i) {
        return versions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0L;
    }

    private static class ViewHolder {
        TextView neoId;
        ImageView icon;
        LinearLayout item;
        TextView mcVersion;
        TextView releaseTime;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View view2;
        ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view2 = LayoutInflater.from(context).inflate(R.layout.item_download_game_list, null);
            holder.item = view2.findViewById(R.id.item);
            holder.icon = view2.findViewById(R.id.icon);
            holder.neoId = view2.findViewById(R.id.id);
            holder.mcVersion = view2.findViewById(R.id.type);
            holder.releaseTime = view2.findViewById(R.id.release_time);
            try {
                activity.exteriorConfig.apply(holder.mcVersion);
            } catch (Throwable ignored) {
            }
            view2.setTag(holder);
        } else {
            view2 = view;
            holder = (ViewHolder) view.getTag();
        }

        final NeoForgeVersion version = versions.get(i);
        // 图标用已有的 ic_neoforge（工程里已有，不新增二进制资源）
        holder.icon.setImageDrawable(context.getDrawable(R.drawable.ic_neoforge));
        holder.neoId.setText(version.getVersion());
        holder.mcVersion.setText("MC " + version.getGameVersion());
        holder.releaseTime.setVisibility(View.GONE);
        holder.item.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelect(version);
            }
        });
        return view2;
    }
}