package com.qcl.launcher.launcher.list.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.modloader.ModLoaderDetector;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.uis.game.download.right.resource.DownloadResourceUI;
import com.qcl.launcher.utils.LocaleUtils;
import com.qcl.launcher.utils.string.ModTranslations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class DownloadResourceAdapter extends BaseAdapter {

    private Context context;
    private MainActivity activity;
    private RemoteModRepository repository;
    private ArrayList<RemoteMod> modList;
    private int type;

    private static class ViewHolder{
        LinearLayout item;
        ImageView icon;
        TextView name;
        TextView categories;
        TextView introduction;
    }

    public DownloadResourceAdapter(Context context, MainActivity activity, RemoteModRepository repository, ArrayList<RemoteMod> modList, int type){
        this.context = context;
        this.activity = activity;
        this.repository = repository;
        this.modList = modList;
        this.type = type;
    }

    @Override
    public int getCount() {
        return modList.size();
    }

    @Override
    public Object getItem(int position) {
        return modList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.item_download_mod,null);
            viewHolder.item = convertView.findViewById(R.id.item);
            viewHolder.icon = convertView.findViewById(R.id.mod_icon);
            viewHolder.name = convertView.findViewById(R.id.mod_name);
            viewHolder.categories = convertView.findViewById(R.id.mod_categories);
            viewHolder.introduction = convertView.findViewById(R.id.mod_introduction);
            activity.exteriorConfig.apply(viewHolder.categories);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.icon.setImageDrawable(context.getDrawable(R.drawable.launcher_background_color_white));
        viewHolder.icon.setTag(position);
        new Thread(() -> {
            try {
                URL url = new URL(modList.get(position).getIconUrl());
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                Bitmap icon = BitmapFactory.decodeStream(inputStream);
                if (viewHolder.icon.getTag().equals(position)){
                    handler.post(() -> viewHolder.icon.setImageBitmap(icon));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
        StringBuilder categories = new StringBuilder();
        for (String category : modList.get(position).getCategories()) {
            boolean isCurse = modList.get(position).getPageUrl() != null && modList.get(position).getPageUrl().contains("curseforge");
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

        // ★ 1.2.3：按「你当前的版本」装的加载器，给不兼容的模组卡片标红字警告（只对模组页生效）。
        //   规则（用户定的）：没装加载器 → 只有不依赖加载器的（纯 class 型）不标；
        //   ModLoader → 依赖 ModLoader 的和纯 class 型不标；Babric → 只有 Babric/Fabric 的不标。
        //   加载器信息取自 Modrinth 的 categories（CurseForge 的 categories 是玩法分类，没有就按无依赖处理）。
        if (type == 0) {
            try {
                // ★★★ 1.2.5 修：publicGameSetting.currentVersion 里存的是**完整路径**
                //   （<游戏目录>/versions/<版本名>，见 GameListAdapter / MainUI 的赋值），
                //   老代码又给它拼了一次 "/versions/" → 拼出来的目录根本不存在
                //   → ModLoaderDetector.detect() 恒返回 null（等于「没装任何加载器」）
                //   → 不管玩家装的是 ModLoader / Babric / Fabric / Forge，
                //     整个模组页都被标上「不支持你当前的版本」。
                //   现在直接用这个路径；只有它不是目录时才退回归属拼接（兼容老数据）。
                String cur = activity.publicGameSetting.currentVersion;
                File vDir = cur == null ? null : new File(cur);
                if (vDir == null || !vDir.isDirectory()) {
                    vDir = new File(activity.launcherSetting.gameFileDirectory + "/versions/" + cur);
                }
                String currentLoader = ModLoaderDetector.detect(vDir);
                java.util.List<String> modLoaders = new java.util.ArrayList<>();
                for (String c : modList.get(position).getCategories()) {
                    String low = c.toLowerCase();
                    if (low.contains("babric")) modLoaders.add("babric");
                    else if (low.contains("modloader")) modLoaders.add("modloader");
                    else if (low.contains("fabric")) modLoaders.add("fabric");
                    // ★ neoforge 要放 forge 前面判：它包含 "forge" 字样
                    else if (low.contains("neoforge")) modLoaders.add("neoforge");
                    else if (low.contains("forge")) modLoaders.add("forge");
                    else if (low.contains("quilt")) modLoaders.add("quilt");
                    else if (low.contains("liteloader")) modLoaders.add("liteloader");
                }
                if (!ModLoaderDetector.isSupported(currentLoader, modLoaders)) {
                    android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder();
                    String warn = "⚠ 不支持你当前的版本";
                    ssb.append(warn);
                    ssb.setSpan(new android.text.style.ForegroundColorSpan(0xFFFF4444),
                            0, warn.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ssb.append("   ").append(categories);
                    viewHolder.categories.setText(ssb);
                }
            } catch (Throwable ignored) {
            }
        }
        ModTranslations modTranslations;
        if (type == 0) {
            modTranslations = ModTranslations.MOD;
        }
        else if (type == 1) {
            modTranslations = ModTranslations.MODPACK;
        }
        else {
            modTranslations = ModTranslations.EMPTY;
        }
        viewHolder.name.setText(modList.get(position).getTitle());
        if (LocaleUtils.isChinese(context)) {
            viewHolder.name.setText((modTranslations.getModByCurseForgeId(modList.get(position).getSlug()) != null && Objects.requireNonNull(modTranslations.getModByCurseForgeId(modList.get(position).getSlug())).getDisplayName() != null) ? Objects.requireNonNull(modTranslations.getModByCurseForgeId(modList.get(position).getSlug())).getDisplayName() : modList.get(position).getTitle());
        }
        viewHolder.introduction.setText(modList.get(position).getDescription());
        viewHolder.item.setOnClickListener(view -> {
            DownloadResourceUI downloadResourceUI = new DownloadResourceUI(context,activity,repository,modList.get(position),type);
            activity.uiManager.switchMainUI(downloadResourceUI);
        });
        return convertView;
    }

    @SuppressLint("HandlerLeak")
    public final Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };
}
