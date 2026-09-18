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
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderGameVersions;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderVersion;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderVersionsRoot;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadLiteLoaderListAdapter;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadLiteLoaderUI extends BaseUI implements View.OnClickListener {
    public static final String LITELOADER_LIST = "http://dl.liteloader.com/versions/versions.json";
    private TextView back;
    public LinearLayout downloadLiteLoaderUI;
    private LinearLayout hintLayout;
    public boolean install;
    private ListView liteLoaderListView;
    private final Handler loadingHandler;
    private ProgressBar progressBar;
    public String version;

    public DownloadLiteLoaderUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadLiteLoaderUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadLiteLoaderUI.this.liteLoaderListView.setVisibility(8);
                    DownloadLiteLoaderUI.this.progressBar.setVisibility(0);
                    DownloadLiteLoaderUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadLiteLoaderUI.this.liteLoaderListView.setVisibility(0);
                    DownloadLiteLoaderUI.this.progressBar.setVisibility(8);
                    DownloadLiteLoaderUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadLiteLoaderUI.this.liteLoaderListView.setVisibility(8);
                    DownloadLiteLoaderUI.this.progressBar.setVisibility(8);
                    DownloadLiteLoaderUI.this.back.setVisibility(0);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadLiteLoaderUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_lite_loader_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_lite_loader_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.liteLoaderListView = (ListView) this.activity.findViewById(R.id.lite_loader_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_lite_loader_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_lite_loader);
        this.back = textView;
        textView.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.lite_loader_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadLiteLoaderUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadLiteLoaderUI, this.activity, this.context, true);
    }

    private void init() {
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadLiteLoaderUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadLiteLoaderUI.this.m489x6d76c226();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadLiteLoaderUI, reason: not valid java name */
    public /* synthetic */ void m489x6d76c226() {
        this.loadingHandler.sendEmptyMessage(0);
        ArrayList arrayList = new ArrayList();
        try {
            LiteLoaderVersionsRoot liteLoaderVersionsRoot = (LiteLoaderVersionsRoot) JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, new Artifact.Serializer()).create().fromJson(NetworkUtils.doGet(NetworkUtils.toURL("http://dl.liteloader.com/versions/versions.json")), LiteLoaderVersionsRoot.class);
            ArrayList arrayList2 = new ArrayList();
            Iterator<Map.Entry<String, LiteLoaderGameVersions>> it = liteLoaderVersionsRoot.getVersions().entrySet().iterator();
            while (it.hasNext()) {
                arrayList2.add(it.next().getKey());
            }
            if (!arrayList2.contains(this.version)) {
                this.loadingHandler.sendEmptyMessage(2);
                return;
            }
            LiteLoaderGameVersions liteLoaderGameVersions = liteLoaderVersionsRoot.getVersions().get(this.version);
            for (Map.Entry<String, LiteLoaderVersion> entry : (liteLoaderGameVersions.getArtifacts() == null ? liteLoaderGameVersions.getSnapshots() : liteLoaderGameVersions.getArtifacts()).getLiteLoader().entrySet()) {
                if (!entry.getKey().equals("latest")) {
                    arrayList.add(entry.getValue());
                }
            }
            arrayList.sort(new LiteLoaderCompareTool());
            final DownloadLiteLoaderListAdapter downloadLiteLoaderListAdapter = new DownloadLiteLoaderListAdapter(this.context, this.activity, this.version, arrayList, this.install);
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadLiteLoaderUI$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadLiteLoaderUI.this.m488x6ca843a5(downloadLiteLoaderListAdapter);
                }
            });
            this.loadingHandler.sendEmptyMessage(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$0$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadLiteLoaderUI, reason: not valid java name */
    public /* synthetic */ void m488x6ca843a5(DownloadLiteLoaderListAdapter downloadLiteLoaderListAdapter) {
        this.liteLoaderListView.setAdapter((ListAdapter) downloadLiteLoaderListAdapter);
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

    /* loaded from: classes2.dex */
    private static class LiteLoaderCompareTool implements Comparator<LiteLoaderVersion> {
        private LiteLoaderCompareTool() {
        }

        @Override // java.util.Comparator
        public int compare(LiteLoaderVersion liteLoaderVersion, LiteLoaderVersion liteLoaderVersion2) {
            return Integer.compare(Integer.parseInt(liteLoaderVersion2.getTimestamp()), Integer.parseInt(liteLoaderVersion.getTimestamp()));
        }
    }
}
