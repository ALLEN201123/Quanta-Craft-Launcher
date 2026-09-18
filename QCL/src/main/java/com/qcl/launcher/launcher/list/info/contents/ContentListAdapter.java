/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.view.LayoutInflater
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.ViewGroup
 *  android.widget.BaseAdapter
 *  android.widget.ImageButton
 *  android.widget.LinearLayout
 *  android.widget.TextView
 */
package com.qcl.launcher.launcher.list.info.contents;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.info.contents.ContentListBean;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.util.ArrayList;

import com.qcl.launcher.R;
public class ContentListAdapter
extends BaseAdapter {
    private Context context;
    private MainActivity activity;
    private ArrayList<ContentListBean> list;

    public ContentListAdapter(Context context, MainActivity activity, ArrayList<ContentListBean> list) {
        this.context = context;
        this.list = list;
        this.activity = activity;
    }

    public int getCount() {
        return this.list.size();
    }

    public Object getItem(int position) {
        return this.list.get(position);
    }

    public long getItemId(int position) {
        return 0L;
    }

    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from((Context)this.context).inflate(R.layout.item_content_list, null);
            viewHolder.switchContent = (LinearLayout)convertView.findViewById(R.id.switch_content);
            viewHolder.name = (TextView)convertView.findViewById(R.id.content_name);
            viewHolder.path = (TextView)convertView.findViewById(R.id.content_path);
            viewHolder.delete = (ImageButton)convertView.findViewById(R.id.delete_content);
            convertView.setTag((Object)viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        viewHolder.name.setText((CharSequence)this.list.get((int)position).name);
        viewHolder.path.setText((CharSequence)this.list.get((int)position).path);
        if (this.list.get((int)position).selected) {
            viewHolder.switchContent.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        } else {
            viewHolder.switchContent.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
        viewHolder.switchContent.setOnClickListener(new View.OnClickListener(){

            public void onClick(View v) {
                ((ContentListAdapter)ContentListAdapter.this).activity.launcherSetting.gameFileDirectory = ((ContentListBean)((ContentListAdapter)ContentListAdapter.this).list.get((int)position)).path;
                GsonUtils.saveLauncherSetting(((ContentListAdapter)ContentListAdapter.this).activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                for (int i = 0; i < ContentListAdapter.this.list.size(); ++i) {
                    ((ContentListBean)((ContentListAdapter)ContentListAdapter.this).list.get((int)i)).selected = false;
                }
                ((ContentListBean)((ContentListAdapter)ContentListAdapter.this).list.get((int)position)).selected = true;
                GsonUtils.saveContents(ContentListAdapter.this.list, AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json");
                new Thread(() -> ((ContentListAdapter)ContentListAdapter.this).activity.uiManager.versionListUI.refreshVersionList()).start();
                ContentListAdapter.this.notifyDataSetChanged();
            }
        });
        viewHolder.delete.setOnClickListener(v -> {
            boolean select = this.list.get((int)position).selected;
            this.list.remove(position);
            if (select && this.list.size() != 0) {
                this.activity.launcherSetting.gameFileDirectory = this.list.get((int)0).path;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                this.list.get((int)0).selected = true;
                new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start();
            }
            if (this.list.size() == 0) {
                this.list.add(new ContentListBean(this.context.getString(R.string.default_game_file_directory_list_pri), AppManifest.DEFAULT_GAME_DIR, true));
                this.list.add(new ContentListBean(this.context.getString(R.string.default_game_file_directory_list_sec), AppManifest.INNER_GAME_DIR, false));
                this.activity.launcherSetting.gameFileDirectory = this.list.get((int)0).path;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start();
            }
            GsonUtils.saveContents(this.list, AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json");
            this.notifyDataSetChanged();
        });
        return convertView;
    }

    private class ViewHolder {
        LinearLayout switchContent;
        TextView name;
        TextView path;
        ImageButton delete;

        private ViewHolder() {
        }
    }
}

