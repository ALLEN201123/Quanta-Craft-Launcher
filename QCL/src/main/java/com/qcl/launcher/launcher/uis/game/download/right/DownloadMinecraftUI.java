package com.qcl.launcher.launcher.uis.game.download.right;

import android.content.Context;
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
import com.qcl.launcher.launcher.download.game.UnlistedVersions;
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
        // ★ 1.5.0：顶部「提示」条已移除（用户要求：太占地方，列表能多显示一条是一条）。
        //   原来这里对 hintLayout 是**无条件** setOnClickListener（控件没了会 NPE），
        //   所以字段、findViewById、监听三处必须一起删。
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
        // ★ 1.5.0：官方清单**漏收**的版本 —— Combat Test 12 个分支快照、
        //   1.18_experimental-snapshot-1..7、1.19_deep_dark_experimental_snapshot-1、
        //   *_unobfuscated（未混淆构建）11 个，以及 11w~13w 被官方删掉的 10 个周快照。
        //   它们自带 piston-meta JSON 地址 ⇒ 走正常 Mojang 安装流程（不是归档流程）。
        for (VersionManifest.Version version : UnlistedVersions.entries(this.context)) {
            if (!linkedHashMap.containsKey(version.id)) {
                linkedHashMap.put(version.id, version);
            }
        }
        // ★ 2026-10-08：1.4.9 期的「[版本清单] 计数」诊断日志已移除。
        //   当时是为了排查「周快照落进远古版」加的；该问题已结案，
        //   而且 1.5.0 实测发现 MuMu 的 logcat 缓冲只有 ~486 行、应用日志刷得又快
        //   → 这行日志根本留不住，留着只是每次开下载页白算一遍全表。
        //   以后模拟器上要取证，一律写**应用私有日志文件**，不要依赖 logcat。
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
            // ★★★ 2026-10-08 修正（群员实测反馈「预览版为啥跑正式版这一栏来了」）
            //   这里原来有个 isPlainReleaseId()，把**所有纯数字 id**（1.3/1.4/1.4.1/1.4.3/
            //   1.5/1.6/1.6.3/1.7/1.7.1）强行当「正式版」——那是 10-06 靠**未验证的假设**
            //   加的（当天日志里明明写着「待查：清单里 1.5 的 type 到底是不是 release」）。
            //   现按官方 version_manifest_v2.json 实测：这 9 个的 type **就是 snapshot**
            //   （它们是各版本的预发布版；正式版是 1.3.1 / 1.4.2 / 1.4.4 / 1.5.1 / 1.6.1 / 1.7.2，
            //     官方清单里只有 1.7.3 及之后才标 release）。
            //   ⇒ 一律**照抄清单的 type**，与 FCL 的 VersionInstallPage 口径一致：
            //     RELEASE → 正式版；PENDING / UNOBFUSCATED / SNAPSHOT → 快照版；其余 → 远古版。
            boolean release = "release".equals(next.type);
            boolean snapshotLike = "snapshot".equals(next.type)
                    || UnlistedVersions.TYPE_PENDING.equals(next.type)
                    || UnlistedVersions.TYPE_UNOBFUSCATED.equals(next.type);
            boolean show;
            if (april) {
                show = this.checkApril.isChecked();
            } else if (release) {
                show = this.checkRelease.isChecked();
            } else if (snapshotLike) {
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
        // ★ 1.5.0：原来这里还有一条 `view == hintLayout` → 打开 BMCLAPI 说明页的分支，
        //   提示条已移除，分支一并删掉（保留了也没意义，控件都不存在了）。
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
