package com.tungsten.hmclpe.launcher.uis.game.download.right;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.download.game.LegacyVersionArchive;
import com.tungsten.hmclpe.launcher.download.game.VersionManifest;
import com.tungsten.hmclpe.launcher.list.download.minecraft.DownloadGameListAdapter;
import com.tungsten.hmclpe.launcher.uis.game.download.DownloadUrlSource;
import com.tungsten.hmclpe.launcher.uis.tools.BaseUI;
import com.tungsten.hmclpe.utils.animation.CustomAnimationUtils;
import com.tungsten.hmclpe.utils.io.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DownloadMinecraftUI extends BaseUI implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public LinearLayout downloadMinecraftUI;
    private LinearLayout hintLayout;
    private LinearLayout gameListLayout;
    private CheckBox checkRelease, checkSnapshot, checkOld;
    private LinearLayout refresh;
    private ProgressBar loadingProgress;
    private ListView mcList;
    // Accessed only on the UI thread. A failed refresh never discards a working list.
    private ArrayList<VersionManifest.Version> allList = new ArrayList<>();
    private boolean loading;

    public DownloadMinecraftUI(Context context, MainActivity activity) { super(context, activity); }

    @Override public void onCreate() {
        super.onCreate();
        downloadMinecraftUI = activity.findViewById(R.id.ui_download_minecraft);
        hintLayout = activity.findViewById(R.id.download_minecraft_hint_layout);
        hintLayout.setOnClickListener(this);
        gameListLayout = activity.findViewById(R.id.game_list_layout);
        checkRelease = activity.findViewById(R.id.checkbox_release);
        checkSnapshot = activity.findViewById(R.id.checkbox_snapshot);
        checkOld = activity.findViewById(R.id.checkbox_old);
        refresh = activity.findViewById(R.id.refresh_game_list);
        loadingProgress = activity.findViewById(R.id.loading_minecraft_list_progress);
        mcList = activity.findViewById(R.id.download_minecraft_version_list);
        // 默认只勾选正式版，快照/远古版需要玩家自己勾选
        checkRelease.setChecked(true);
        checkSnapshot.setChecked(false);
        checkOld.setChecked(false);
        checkRelease.setOnCheckedChangeListener(this);
        checkSnapshot.setOnCheckedChangeListener(this);
        checkOld.setOnCheckedChangeListener(this);
        refresh.setOnClickListener(this);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(downloadMinecraftUI, activity, context, false);
        if (activity.isLoaded) activity.uiManager.downloadUI.startDownloadGameUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
        if (allList.isEmpty()) init(); else refresh();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(downloadMinecraftUI, activity, context, false);
        if (activity.isLoaded) activity.uiManager.downloadUI.startDownloadGameUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_parent));
    }

    private void readManifest(String url, Map<String, VersionManifest.Version> merged) throws IOException {
        VersionManifest manifest = new Gson().fromJson(NetworkUtils.doGet(NetworkUtils.toURL(url)), VersionManifest.class);
        if (manifest == null || manifest.versions == null) throw new IOException("Empty version manifest");
        for (VersionManifest.Version version : manifest.versions) {
            if (version == null || version.id == null || version.type == null || version.url == null) continue;
            if (!version.url.startsWith("https://")) continue;
            // Keep real URLs and dates from upstream instead of inventing historical entries.
            if (!merged.containsKey(version.id)) merged.put(version.id, version);
        }
    }

    private void init() {
        if (loading) return;
        loading = true;
        loadingProgress.setVisibility(View.VISIBLE);
        refresh.setEnabled(false);
        final String selected = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource), DownloadUrlSource.VERSION_MANIFEST);
        new Thread(() -> {
            Map<String, VersionManifest.Version> merged = new LinkedHashMap<>();
            String official = DownloadUrlSource.getSubUrl(DownloadUrlSource.DOWNLOAD_URL_SOURCE_OFFICIAL, DownloadUrlSource.VERSION_MANIFEST);
            String mirror = DownloadUrlSource.getSubUrl(DownloadUrlSource.DOWNLOAD_URL_SOURCE_BMCLAPI, DownloadUrlSource.VERSION_MANIFEST);
            try { readManifest(selected, merged); } catch (Exception e) { e.printStackTrace(); }
            // Merge the other authoritative manifest to recover omissions or a stale mirror.
            try { readManifest(selected.equals(official) ? mirror : official, merged); } catch (Exception e) { e.printStackTrace(); }
            // Neither Mojang nor BMCLAPI lists the earliest builds; add the archive entries so the
            // history is complete instead of silently missing.
            for (VersionManifest.Version archive : LegacyVersionArchive.entries(context)) {
                if (!merged.containsKey(archive.id)) merged.put(archive.id, archive);
            }
            ArrayList<VersionManifest.Version> result = new ArrayList<>(merged.values());
            VersionManifest.sortNewestFirst(result);
            activity.runOnUiThread(() -> {
                loading = false;
                refresh.setEnabled(true);
                loadingProgress.setVisibility(View.GONE);
                gameListLayout.setVisibility(View.VISIBLE);
                if (!result.isEmpty()) allList = result;
                else Toast.makeText(context, R.string.revival_manifest_failed, Toast.LENGTH_LONG).show();
                refresh();
            });
        }, "minecraft-manifest").start();
    }

    private void refresh() {
        if (mcList == null) return;
        ArrayList<VersionManifest.Version> list = new ArrayList<>();
        for (VersionManifest.Version v : allList) {
            boolean release = "release".equals(v.type), snapshot = "snapshot".equals(v.type);
            if ((release && checkRelease.isChecked()) || (snapshot && checkSnapshot.isChecked()) || (!release && !snapshot && checkOld.isChecked())) list.add(v);
        }
        mcList.setAdapter(new DownloadGameListAdapter(context, activity, list));
    }

    @Override public void onClick(View v) {
        if (v == hintLayout) context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bmclapidoc.bangbang93.com/")));
        if (v == refresh) init();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override public void onLoaded() {
        activity.uiManager.downloadUI.startDownloadGameUI.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
    }

    @Override public void onCheckedChanged(CompoundButton button, boolean checked) { refresh(); }
}
