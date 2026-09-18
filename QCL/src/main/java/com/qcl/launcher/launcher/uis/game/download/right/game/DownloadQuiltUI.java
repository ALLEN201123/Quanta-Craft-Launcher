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
import com.qcl.launcher.launcher.download.quilt.QuiltGameVersion;
import com.qcl.launcher.launcher.download.quilt.QuiltLoaderVersion;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadQuiltListAdapter;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadQuiltUI extends BaseUI implements View.OnClickListener {
    private static final String GAME_META_URL = "https://meta.quiltmc.org/v3/versions/game";
    private static final String LOADER_META_URL = "https://meta.quiltmc.org/v3/versions/loader";
    private TextView back;
    public LinearLayout downloadQuiltUI;
    private LinearLayout hintLayout;
    public boolean install;
    private final Handler loadingHandler;
    private ProgressBar progressBar;
    private ListView quiltListView;
    public String version;

    public DownloadQuiltUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadQuiltUI.this.quiltListView.setVisibility(8);
                    DownloadQuiltUI.this.progressBar.setVisibility(0);
                    DownloadQuiltUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadQuiltUI.this.quiltListView.setVisibility(0);
                    DownloadQuiltUI.this.progressBar.setVisibility(8);
                    DownloadQuiltUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadQuiltUI.this.quiltListView.setVisibility(8);
                    DownloadQuiltUI.this.progressBar.setVisibility(8);
                    DownloadQuiltUI.this.back.setVisibility(0);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadQuiltUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_quilt_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_quilt_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.quiltListView = (ListView) this.activity.findViewById(R.id.quilt_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_quilt_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_quilt);
        this.back = textView;
        textView.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.quilt_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadQuiltUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadQuiltUI, this.activity, this.context, true);
    }

    private void init() {
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadQuiltUI.this.m495x61b10650();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadQuiltUI, reason: not valid java name */
    public /* synthetic */ void m495x61b10650() {
        this.loadingHandler.sendEmptyMessage(0);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        try {
            String doGet = NetworkUtils.doGet(NetworkUtils.toURL("https://meta.quiltmc.org/v3/versions/game"));
            Gson gson = new Gson();
            arrayList.addAll(Arrays.asList((QuiltGameVersion[]) gson.fromJson(doGet, QuiltGameVersion[].class)));
            ArrayList arrayList3 = new ArrayList();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                arrayList3.add(((QuiltGameVersion) it.next()).version);
            }
            arrayList2.addAll(Arrays.asList((QuiltLoaderVersion[]) gson.fromJson(NetworkUtils.doGet(NetworkUtils.toURL("https://meta.quiltmc.org/v3/versions/loader")), QuiltLoaderVersion[].class)));
            if (!arrayList3.contains(this.version)) {
                this.loadingHandler.sendEmptyMessage(2);
                return;
            }
            final DownloadQuiltListAdapter downloadQuiltListAdapter = new DownloadQuiltListAdapter(this.context, this.activity, this.version, arrayList2, this.install);
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadQuiltUI$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadQuiltUI.this.m494xdf665171(downloadQuiltListAdapter);
                }
            });
            this.loadingHandler.sendEmptyMessage(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$0$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadQuiltUI, reason: not valid java name */
    public /* synthetic */ void m494xdf665171(DownloadQuiltListAdapter downloadQuiltListAdapter) {
        this.quiltListView.setAdapter((ListAdapter) downloadQuiltListAdapter);
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
