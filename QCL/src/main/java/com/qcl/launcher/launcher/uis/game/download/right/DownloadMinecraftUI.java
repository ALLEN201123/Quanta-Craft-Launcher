package com.qcl.launcher.launcher.uis.game.download.right;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.game.LegacyVersionArchive;
import com.qcl.launcher.launcher.download.game.AprilFools;
import com.qcl.launcher.launcher.download.game.VersionManifest;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadGameListAdapter;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadMinecraftUI extends BaseUI implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private ArrayList<VersionManifest.Version> allList;
    private CheckBox checkOld;
    private CheckBox checkRelease;
    private CheckBox checkSnapshot;
    /** ★ 1.4.5：愚人节版分类 */
    private CheckBox checkApril;
    public LinearLayout downloadMinecraftUI;
    private LinearLayout gameListLayout;
    private LinearLayout hintLayout;
    private boolean loading;
    private ProgressBar loadingProgress;
    private ListView mcList;
    private LinearLayout refresh;

    public DownloadMinecraftUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.allList = new ArrayList<>();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadMinecraftUI = (LinearLayout) this.activity.findViewById(R.id.ui_download_minecraft);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_minecraft_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.gameListLayout = (LinearLayout) this.activity.findViewById(R.id.game_list_layout);
        this.checkRelease = (CheckBox) this.activity.findViewById(R.id.checkbox_release);
        this.checkSnapshot = (CheckBox) this.activity.findViewById(R.id.checkbox_snapshot);
        this.checkOld = (CheckBox) this.activity.findViewById(R.id.checkbox_old);
        this.checkApril = (CheckBox) this.activity.findViewById(R.id.checkbox_april);
        this.refresh = (LinearLayout) this.activity.findViewById(R.id.refresh_game_list);
        this.loadingProgress = (ProgressBar) this.activity.findViewById(R.id.loading_minecraft_list_progress);
        this.mcList = (ListView) this.activity.findViewById(R.id.download_minecraft_version_list);
        this.checkRelease.setChecked(true);
        this.checkSnapshot.setChecked(false);
        this.checkOld.setChecked(false);
        // ★ 1.4.5：愚人节版（清单里被标成 snapshot，单列一类方便找）
        this.checkApril.setChecked(false);
        this.checkRelease.setOnCheckedChangeListener(this);
        this.checkSnapshot.setOnCheckedChangeListener(this);
        this.checkOld.setOnCheckedChangeListener(this);
        this.checkApril.setOnCheckedChangeListener(this);
        this.refresh.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadMinecraftUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadGameUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
        if (this.allList.isEmpty()) {
            init();
        } else {
            refresh();
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadMinecraftUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadGameUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    private void readManifest(String str, Map<String, VersionManifest.Version> map) throws IOException {
        VersionManifest versionManifest = (VersionManifest) new Gson().fromJson(NetworkUtils.doGet(NetworkUtils.toURL(str)), VersionManifest.class);
        if (versionManifest == null || versionManifest.versions == null) {
            throw new IOException("Empty version manifest");
        }
        for (VersionManifest.Version version : versionManifest.versions) {
            if (version != null && version.id != null && version.type != null && version.url != null && version.url.startsWith("https://") && !map.containsKey(version.id)) {
                map.put(version.id, version);
            }
        }
    }

    private void init() {
        if (this.loading) {
            return;
        }
        this.loading = true;
        this.loadingProgress.setVisibility(0);
        this.refresh.setEnabled(false);
        final String subUrl = DownloadUrlSource.getSubUrl(DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource), 0);
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadMinecraftUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadMinecraftUI.this.m476x1832f767(subUrl);
            }
        }, "minecraft-manifest").start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$1$com-qcl-launcher-launcher-uis-game-download-right-DownloadMinecraftUI, reason: not valid java name */
    public /* synthetic */ void m476x1832f767(String str) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        String subUrl = DownloadUrlSource.getSubUrl(0, 0);
        String subUrl2 = DownloadUrlSource.getSubUrl(1, 0);
        try {
            readManifest(str, linkedHashMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (str.equals(subUrl)) {
                subUrl = subUrl2;
            }
            readManifest(subUrl, linkedHashMap);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        for (VersionManifest.Version version : LegacyVersionArchive.entries(this.context)) {
            if (!linkedHashMap.containsKey(version.id)) {
                linkedHashMap.put(version.id, version);
            }
        }
        // ★ 1.4.9 诊断：周快照必须落在「快照版」分类（type=snapshot）。
        //   若这里的 type 不是 snapshot，就会被 refresh() 归到「远古版」——
        //   用户反馈"我让你把快照弄到测试版里，结果还在远古版那里"就是这里出问题。
        try {
            int snap = 0;
            int arch = 0;
            int weekSnap = 0;
            String firstWeek = "";
            String lastWeek = "";
            String sample = "";
            // linkedHashMap 是反编译产物（无泛型），values() 只能按 Object 迭代
            for (Object obj : linkedHashMap.values()) {
                VersionManifest.Version v = (VersionManifest.Version) obj;
                String id = v.id == null ? "" : v.id;
                // 独立复算一遍周快照判定（下标 0/1 数字、下标 2 是 w、下标 3 数字）
                boolean wk = id.length() >= 4
                        && id.charAt(0) >= '0' && id.charAt(0) <= '9'
                        && id.charAt(1) >= '0' && id.charAt(1) <= '9'
                        && id.charAt(2) == 'w'
                        && id.charAt(3) >= '0' && id.charAt(3) <= '9';
                if (wk) {
                    weekSnap++;
                    if (firstWeek.isEmpty() || id.compareTo(firstWeek) < 0) firstWeek = id;
                    if (lastWeek.isEmpty() || id.compareTo(lastWeek) > 0) lastWeek = id;
                }
                if (LegacyVersionArchive.TYPE_SNAPSHOT.equals(v.type)) {
                    snap++;
                    if (sample.isEmpty()) sample = v.id;
                } else if (LegacyVersionArchive.TYPE_ARCHIVE.equals(v.type)) {
                    arch++;
                }
            }
            android.util.Log.i("QCL-DL", "[版本清单] 合计=" + linkedHashMap.size()
                    + " type=快照:" + snap + " type=远古:" + arch
                    + " | 周快照判定命中=" + weekSnap
                    + " 最早=" + firstWeek + " 最晚=" + lastWeek);
        } catch (Throwable ignored) {
        }
        final ArrayList arrayList = new ArrayList(linkedHashMap.values());
        VersionManifest.sortNewestFirst(arrayList);
        this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.DownloadMinecraftUI$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DownloadMinecraftUI.this.m475x35074426(arrayList);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$0$com-qcl-launcher-launcher-uis-game-download-right-DownloadMinecraftUI, reason: not valid java name */
    public /* synthetic */ void m475x35074426(ArrayList arrayList) {
        this.loading = false;
        this.refresh.setEnabled(true);
        this.loadingProgress.setVisibility(8);
        this.gameListLayout.setVisibility(0);
        if (arrayList.isEmpty()) {
            Toast.makeText(this.context, R.string.revival_manifest_failed, 1).show();
        } else {
            this.allList = arrayList;
        }
        refresh();
    }

    /**
     * ★ 2026-06 修正 → 见下方 2026-10-06 注释。
     * <p>
     * 判断一个版本 id 是否是「正式版形态」：纯数字 + 点分，如 1 / 1.1 / 1.4.1 / 1.5 / 1.20.6。
     * 快照一定是「数字+字母」形态（11w47a / 13w16a / 20w07a / 1.20.5-beta.1 里的 beta 那类另有 AprilFools 判定）。
     *
     * <p><b>为什么需要它</b>：实测 Mojang 官方 version_manifest_v2 把
     * {@code 1.3 / 1.4 / 1.4.1 / 1.4.3 / 1.5} 全标成 {@code type=snapshot}，
     * 只有 1.1、1.2.5 是 release。照抄清单 → 玩家在「测试版」里看到 1.1~1.5（用户实测截图）。
     * 纯数字版本号一律按正式版归类，符合直觉。
     */
    private static boolean isPlainReleaseId(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == '.') {
                if (i == 0 || i == id.length() - 1) {
                    return false;      // 开头/结尾的点 → 不合法
                }
                continue;
            }
            if (c < '0' || c > '9') {
                return false;          // 出现字母（w / beta / rc…）→ 是快照或候选版
            }
        }
        return true;
    }

    private void refresh() {
        if (this.mcList == null) {
            return;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<VersionManifest.Version> it = this.allList.iterator();
        while (it.hasNext()) {
            VersionManifest.Version next = it.next();
            // ★ 1.4.5：愚人节版优先判定 —— 它们在清单里就是 snapshot，
            //   不先摘出来的话会被"快照版"一起吃进去（用户看不到单独分类）。
            boolean april = AprilFools.isAprilFools(next);
            // ★ 2026-10-06 修复（用户实测"测试版里混了 5 个正式版"）
            //   Mojang 官方清单把 1.1 ~ 1.5 这几个纯数字正式版**标成了 snapshot**
            //   （实测官方 version_manifest_v2：1.3/1.4/1.4.1/1.4.3/1.5 全是 snapshot，
            //     只有 1.1、1.2.5 是 release）。照抄清单就会让玩家在"测试版"里
            //     看到 1.1~1.5，摸不着头脑。
            //   规则：**id 是纯数字版本号（1 / 1.1 / 1.4.1 / 1.5 …）就一律按正式版归类**，
            //     快照一定是 11w47a / 13w16a / 20w07a 这种「数字+字母」形态。
            boolean plainRelease = isPlainReleaseId(next.id);
            boolean equals = "release".equals(next.type) || plainRelease;
            boolean equals2 = "snapshot".equals(next.type) && !plainRelease;
            boolean show;
            if (april) {
                show = this.checkApril.isChecked();
            } else if (equals) {
                show = this.checkRelease.isChecked();
            } else if (equals2) {
                show = this.checkSnapshot.isChecked();
            } else {
                show = this.checkOld.isChecked();
            }
            if (show) {
                arrayList.add(next);
            }
        }
        this.mcList.setAdapter((ListAdapter) new DownloadGameListAdapter(this.context, this.activity, arrayList));
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.hintLayout) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://bmclapidoc.bangbang93.com/")));
        }
        if (view == this.refresh) {
            init();
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onLoaded() {
        this.activity.uiManager.downloadUI.startDownloadGameUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        refresh();
    }
}
