package com.qcl.launcher.launcher.list.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.string.ModTranslations;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class ModDependencyAdapter extends BaseAdapter {

    private Context context;
    private MainActivity activity;
    private RemoteModRepository repository;
    private List<RemoteMod> list;

    /** ★ 1.2.5：依赖类型表（远程 id → required/optional/...），用来标「必需/可选前置」 */
    private final java.util.Map<String, String> dependencyTypes;

    public ModDependencyAdapter (Context context,MainActivity activity,RemoteModRepository repository,List<RemoteMod> list) {
        this(context, activity, repository, list, null);
    }

    public ModDependencyAdapter (Context context,MainActivity activity,RemoteModRepository repository,List<RemoteMod> list, java.util.Map<String, String> dependencyTypes) {
        this.context = context;
        this.activity = activity;
        this.repository = repository;
        this.list = list;
        this.dependencyTypes = dependencyTypes;
    }

    /** ★ 1.2.5：这个前置是「必需」还是「可选」（拿不到类型时按必需显示） */
    private String dependencyTypeOf(int i) {
        try {
            if (this.dependencyTypes == null || this.list.get(i).getData() == null) {
                return null;
            }
            String id = this.list.get(i).getData().getRemoteId();
            return id == null ? null : this.dependencyTypes.get(id);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private class ViewHolder{
        LinearLayout item;
        ImageView icon;
        TextView name;
        TextView categories;
        TextView introduction;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder viewHolder;
        if (view == null){
            viewHolder = new ViewHolder();
            view = LayoutInflater.from(context).inflate(R.layout.item_download_mod_dependency,null);
            viewHolder.item = view.findViewById(R.id.item);
            viewHolder.icon = view.findViewById(R.id.mod_icon);
            viewHolder.name = view.findViewById(R.id.mod_name);
            viewHolder.categories = view.findViewById(R.id.mod_categories);
            viewHolder.introduction = view.findViewById(R.id.mod_introduction);
            activity.exteriorConfig.apply(viewHolder.categories);
            view.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.icon.setImageDrawable(context.getDrawable(R.drawable.launcher_background_color_white));
        viewHolder.icon.setTag(i);
        new Thread(() -> {
            try {
                URL url = new URL(list.get(i).getIconUrl());
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                Bitmap icon = BitmapFactory.decodeStream(inputStream);
                if (viewHolder.icon.getTag().equals(i)){
                    activity.runOnUiThread(() -> {
                        viewHolder.icon.setImageBitmap(icon);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        StringBuilder categories = new StringBuilder();
        for (String category : list.get(i).getCategories()) {
            boolean isCurse = list.get(i).getPageUrl() != null && list.get(i).getPageUrl().contains("curseforge");
            String c;
            int resId = context.getResources().getIdentifier((isCurse ? "curse_category_" : "modrinth_category_") + category.replace("-","_"),"string",context.getPackageName());
            if (resId != 0 && context.getString(resId) != null) {
                c = context.getString(resId);
            }
            else {
                c = category;
            }
            categories.append(c).append("   ");
        }
        viewHolder.categories.setText(categories.toString());
        ModTranslations modTranslations = ModTranslations.MOD;
        // ★ 1.2.5：名称前标出「必需前置 / 可选前置」（类型拿不到就只写「前置」）
        String depType = dependencyTypeOf(i);
        String depPrefix;
        if ("optional".equals(depType)) {
            depPrefix = "【可选前置】";
        } else if ("incompatible".equals(depType)) {
            depPrefix = "【不兼容】";
        } else if ("embedded".equals(depType)) {
            depPrefix = "【已内置】";
        } else if (depType == null) {
            depPrefix = "【前置】";
        } else {
            depPrefix = "【必需前置】";
        }
        // ★ 1.2.5：中文名要保留标签前缀（原来这里会把标签整段覆盖掉）
        String depTitle = list.get(i).getTitle();
        if (LocaleUtils.isChinese(context)) {
            depTitle = modTranslations.getModByCurseForgeId(list.get(i).getSlug()) == null ? list.get(i).getTitle() : modTranslations.getModByCurseForgeId(list.get(i).getSlug()).getDisplayName();
        }
        viewHolder.name.setText(depPrefix + depTitle);
        viewHolder.introduction.setText(list.get(i).getDescription());
        viewHolder.item.setOnClickListener(view1 -> {
            DownloadResourceUI downloadResourceUI = new DownloadResourceUI(context,activity,repository,list.get(i),0);
            activity.uiManager.switchMainUI(downloadResourceUI);
        });
        return view;
    }

}
