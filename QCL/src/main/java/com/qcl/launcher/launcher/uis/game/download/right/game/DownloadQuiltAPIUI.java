package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadQuiltAPIListAdapter;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadQuiltAPIUI extends BaseUI implements View.OnClickListener {
    private static final String QUILT_API_ID = "qsl";
    private TextView back;
    public LinearLayout downloadQuiltAPIUI;
    private LinearLayout hintLayout;
    public boolean install;
    private final Handler loadingHandler;
    private ProgressBar progressBar;
    private ListView quiltAPIListView;
    private TextView refreshText;
    public String version;

    public DownloadQuiltAPIUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltAPIUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadQuiltAPIUI.this.quiltAPIListView.setVisibility(8);
                    DownloadQuiltAPIUI.this.progressBar.setVisibility(0);
                    DownloadQuiltAPIUI.this.refreshText.setVisibility(8);
                    DownloadQuiltAPIUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadQuiltAPIUI.this.quiltAPIListView.setVisibility(0);
                    DownloadQuiltAPIUI.this.progressBar.setVisibility(8);
                    DownloadQuiltAPIUI.this.refreshText.setVisibility(8);
                    DownloadQuiltAPIUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadQuiltAPIUI.this.quiltAPIListView.setVisibility(8);
                    DownloadQuiltAPIUI.this.progressBar.setVisibility(8);
                    DownloadQuiltAPIUI.this.refreshText.setVisibility(8);
                    DownloadQuiltAPIUI.this.back.setVisibility(0);
                }
                if (message.what == 3) {
                    DownloadQuiltAPIUI.this.quiltAPIListView.setVisibility(8);
                    DownloadQuiltAPIUI.this.progressBar.setVisibility(8);
                    DownloadQuiltAPIUI.this.refreshText.setVisibility(0);
                    DownloadQuiltAPIUI.this.back.setVisibility(8);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadQuiltAPIUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_quilt_api_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_quilt_api_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.quiltAPIListView = (ListView) this.activity.findViewById(R.id.quilt_api_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_quilt_api_list_progress);
        this.refreshText = (TextView) this.activity.findViewById(R.id.refresh_quilt_api_list);
        this.back = (TextView) this.activity.findViewById(R.id.back_to_install_ui_quilt_api);
        this.refreshText.setOnClickListener(this);
        this.back.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.quilt_api_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadQuiltAPIUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadQuiltAPIUI, this.activity, this.context, true);
    }

    private void init() {
        refresh();
    }

    private void refresh() {
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltAPIUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadQuiltAPIUI.this.m493xedcf9e83();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$refresh$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadQuiltAPIUI, reason: not valid java name */
    public /* synthetic */ void m493xedcf9e83() {
        boolean z = false;
        this.loadingHandler.sendEmptyMessage(0);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        try {
            arrayList.addAll((List) ModrinthRemoteModRepository.MODS.getRemoteVersionsById("qsl").collect(Collectors.toList()));
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RemoteMod.Version version = (RemoteMod.Version) it.next();
                if (version.getGameVersions().contains(this.version)) {
                    arrayList2.add(version);
                    z = true;
                }
            }
            if (z) {
                final DownloadQuiltAPIListAdapter downloadQuiltAPIListAdapter = new DownloadQuiltAPIListAdapter(this.context, this.activity, this.version, arrayList2, this.install);
                this.loadingHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltAPIUI$$ExternalSyntheticLambda1
                    @Override // java.lang.Runnable
                    public final void run() {
                        DownloadQuiltAPIUI.this.m492xba2173c2(downloadQuiltAPIListAdapter);
                    }
                });
                this.loadingHandler.sendEmptyMessage(1);
                return;
            }
            this.loadingHandler.sendEmptyMessage(2);
        } catch (Exception e) {
            this.loadingHandler.sendEmptyMessage(3);
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$refresh$0$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadQuiltAPIUI, reason: not valid java name */
    public /* synthetic */ void m492xba2173c2(DownloadQuiltAPIListAdapter downloadQuiltAPIListAdapter) {
        this.quiltAPIListView.setAdapter((ListAdapter) downloadQuiltAPIListAdapter);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.hintLayout) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://afdian.net/@bangbang93")));
        }
        if (view == this.refreshText) {
            refresh();
        }
        if (view == this.back) {
            this.activity.backToLastUI();
        }
    }
}
