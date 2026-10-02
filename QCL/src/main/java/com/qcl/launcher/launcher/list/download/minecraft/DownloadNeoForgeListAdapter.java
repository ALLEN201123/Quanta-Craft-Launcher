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

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.GameUpdateDialog;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersion;

import java.util.ArrayList;

/**
 * ★ NeoForge 版本列表适配器（★ 1.4.2 起改为「Forge 同款」模式）。
 *
 * 之前是「自带安装按钮」的独立页（回调 listener）；现对齐
 * {@link DownloadForgeListAdapter} 的语义：
 *   - install == false（从「游戏下载 → 点版本 → 安装界面」进来）→
 *       设置 {@code installGameUI.neoForgeVersion} 并 {@code backToLastUI()}，
 *       真正安装由 GameInstallDialog 在点「安装游戏」时统一跑；
 *   - install == true（从版本管理「自动安装」进来）→ 走 GameUpdateDialog 换版本。
 *
 * 列表项沿用 item_download_game_list：
 *   id    = NeoForge 版本号（如 21.1.72）
 *   type  = 对应的 MC 版本（如 1.21.1）
 */
public class DownloadNeoForgeListAdapter extends BaseAdapter {
    private final Context context;
    private final MainActivity activity;
    private final ArrayList<NeoForgeVersion> versions;
    private final boolean install;

    public DownloadNeoForgeListAdapter(Context context, MainActivity activity,
                                       ArrayList<NeoForgeVersion> versions, boolean install) {
        this.context = context;
        this.activity = activity;
        this.versions = versions;
        this.install = install;
    }

    @Override
    public int getCount() {
        return this.versions.size();
    }

    @Override
    public Object getItem(int i) {
        return this.versions.get(i);
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
            view2 = LayoutInflater.from(this.context).inflate(R.layout.item_download_game_list, (ViewGroup) null);
            holder.item = (LinearLayout) view2.findViewById(R.id.item);
            holder.icon = (ImageView) view2.findViewById(R.id.icon);
            holder.neoId = (TextView) view2.findViewById(R.id.id);
            holder.mcVersion = (TextView) view2.findViewById(R.id.type);
            holder.releaseTime = (TextView) view2.findViewById(R.id.release_time);
            try {
                this.activity.exteriorConfig.apply(holder.mcVersion);
            } catch (Throwable ignored) {
            }
            view2.setTag(holder);
        } else {
            view2 = view;
            holder = (ViewHolder) view.getTag();
        }

        final NeoForgeVersion version = this.versions.get(i);
        holder.icon.setImageDrawable(this.context.getDrawable(R.drawable.ic_neoforge));
        holder.neoId.setText(version.getVersion());
        holder.mcVersion.setText("MC " + version.getGameVersion());
        // NeoForge 的列表接口没有发布时间字段，直接隐藏
        holder.releaseTime.setVisibility(View.GONE);
        holder.item.setOnClickListener(v -> onItemClick(version));
        return view2;
    }

    /** ★ Forge 同款：install=false → 回安装界面；install=true → 自动安装换版本 */
    private void onItemClick(final NeoForgeVersion version) {
        if (this.install) {
            if (this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.neoForgeVersion != null) {
                String old = this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.neoForgeVersion;
                AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
                builder.setTitle(this.context.getString(R.string.dialog_change_version_title));
                builder.setMessage(this.context.getString(R.string.dialog_change_version_msg)
                        .replace("%s", "NeoForge").replace("%v1", old).replace("%v2", version.getVersion()));
                builder.setPositiveButton(this.context.getString(R.string.dialog_change_version_positive),
                        (dialogInterface, i) -> update(version));
                builder.setNegativeButton(this.context.getString(R.string.dialog_change_version_negative),
                        (dialogInterface, i) -> this.activity.backToLastUI());
                builder.create().show();
                return;
            }
            update(version);
            return;
        }
        this.activity.uiManager.installGameUI.neoForgeVersion = version;
        this.activity.backToLastUI();
    }

    private void update(NeoForgeVersion version) {
        MainActivity mainActivity = this.activity;
        new GameUpdateDialog(this.context, mainActivity,
                mainActivity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.versionName,
                this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.gameVersion,
                7, version).show();
    }
}
