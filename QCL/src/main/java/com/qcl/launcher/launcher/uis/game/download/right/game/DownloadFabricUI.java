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
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.fabric.FabricGameVersion;
import com.qcl.launcher.launcher.download.fabric.FabricLoaderVersion;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadFabricListAdapter;
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadFabricUI extends BaseUI implements View.OnClickListener {
    private static final String BMCLAPI_GAME_META_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/game";
    private static final String BMCLAPI_LOADER_META_URL = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/loader";
    private static final String OFFICIAL_GAME_META_URL = "https://meta.fabricmc.net/v2/versions/game";
    private static final String OFFICIAL_LOADER_META_URL = "https://meta.fabricmc.net/v2/versions/loader";
    private TextView back;
    public LinearLayout downloadFabricUI;
    private ListView fabricListView;
    private LinearLayout hintLayout;
    public boolean install;
    private final Handler loadingHandler;
    private ProgressBar progressBar;
    public String version;

    public DownloadFabricUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadFabricUI.this.fabricListView.setVisibility(8);
                    DownloadFabricUI.this.progressBar.setVisibility(0);
                    DownloadFabricUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadFabricUI.this.fabricListView.setVisibility(0);
                    DownloadFabricUI.this.progressBar.setVisibility(8);
                    DownloadFabricUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadFabricUI.this.fabricListView.setVisibility(8);
                    DownloadFabricUI.this.progressBar.setVisibility(8);
                    DownloadFabricUI.this.back.setVisibility(0);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadFabricUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_fabric_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_fabric_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.fabricListView = (ListView) this.activity.findViewById(R.id.fabric_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_fabric_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_fabric);
        this.back = textView;
        textView.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.fabric_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadFabricUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadFabricUI, this.activity, this.context, true);
    }

    private void init() {
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadFabricUI.this.m485x30240bea();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadFabricUI, reason: not valid java name */
    public /* synthetic */ void m485x30240bea() {
        String str;
        String str2;
        this.loadingHandler.sendEmptyMessage(0);
        if (DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource) == 0) {
            str = "https://meta.fabricmc.net/v2/versions/loader";
            str2 = "https://meta.fabricmc.net/v2/versions/game";
        } else {
            str = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/loader";
            str2 = "https://bmclapi2.bangbang93.com/fabric-meta/v2/versions/game";
        }
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        try {
            String doGet = NetworkUtils.doGet(NetworkUtils.toURL(str2));
            Gson gson = new Gson();
            arrayList.addAll(Arrays.asList((FabricGameVersion[]) gson.fromJson(doGet, FabricGameVersion[].class)));
            ArrayList arrayList3 = new ArrayList();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                arrayList3.add(((FabricGameVersion) it.next()).version);
            }
            arrayList2.addAll(Arrays.asList((FabricLoaderVersion[]) gson.fromJson(NetworkUtils.doGet(NetworkUtils.toURL(str)), FabricLoaderVersion[].class)));
            if (!arrayList3.contains(this.version)) {
                this.loadingHandler.sendEmptyMessage(2);
                return;
            }
            final DownloadFabricListAdapter downloadFabricListAdapter = new DownloadFabricListAdapter(this.context, this.activity, this.version, arrayList2, this.install);
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricUI$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadFabricUI.this.m484x691824e9(downloadFabricListAdapter);
                }
            });
            this.loadingHandler.sendEmptyMessage(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$0$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadFabricUI, reason: not valid java name */
    public /* synthetic */ void m484x691824e9(DownloadFabricListAdapter downloadFabricListAdapter) {
        this.fabricListView.setAdapter((ListAdapter) downloadFabricListAdapter);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.hintLayout) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://afdian.net/@bangbang93")));
        }
        if (view == this.back) {
            this.activity.backToLastUI();
        }
    }
}
