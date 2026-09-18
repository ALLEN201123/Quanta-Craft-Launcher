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
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadForgeListAdapter;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadForgeUI extends BaseUI implements View.OnClickListener {
    public static final String FORGE_VERSION_MANIFEST = "https://bmclapi2.bangbang93.com/forge/minecraft/";
    private TextView back;
    public LinearLayout downloadForgeUI;
    private ListView forgeListView;
    private LinearLayout hintLayout;
    public boolean install;
    private final Handler loadingHandler;
    private ProgressBar progressBar;
    public String version;

    public DownloadForgeUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadForgeUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadForgeUI.this.forgeListView.setVisibility(8);
                    DownloadForgeUI.this.progressBar.setVisibility(0);
                    DownloadForgeUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadForgeUI.this.forgeListView.setVisibility(0);
                    DownloadForgeUI.this.progressBar.setVisibility(8);
                    DownloadForgeUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadForgeUI.this.forgeListView.setVisibility(8);
                    DownloadForgeUI.this.progressBar.setVisibility(8);
                    DownloadForgeUI.this.back.setVisibility(0);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadForgeUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_forge_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_forge_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.forgeListView = (ListView) this.activity.findViewById(R.id.forge_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_forge_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_forge);
        this.back = textView;
        textView.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.forge_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadForgeUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadForgeUI, this.activity, this.context, true);
    }

    private void init() {
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadForgeUI$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DownloadForgeUI.this.m487x120bbc2a();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadForgeUI, reason: not valid java name */
    public /* synthetic */ void m487x120bbc2a() {
        String str = "https://bmclapi2.bangbang93.com/forge/minecraft/" + this.version;
        this.loadingHandler.sendEmptyMessage(0);
        ArrayList arrayList = new ArrayList();
        try {
            arrayList.addAll(Arrays.asList((ForgeVersion[]) new Gson().fromJson(NetworkUtils.doGet(NetworkUtils.toURL(str)), ForgeVersion[].class)));
            arrayList.sort(new ForgeCompareTool());
            final DownloadForgeListAdapter downloadForgeListAdapter = new DownloadForgeListAdapter(this.context, this.activity, arrayList, this.install);
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadForgeUI$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadForgeUI.this.m486x8fc1074b(downloadForgeListAdapter);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (arrayList.size() == 0) {
            this.loadingHandler.sendEmptyMessage(2);
        } else {
            this.loadingHandler.sendEmptyMessage(1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$0$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadForgeUI, reason: not valid java name */
    public /* synthetic */ void m486x8fc1074b(DownloadForgeListAdapter downloadForgeListAdapter) {
        this.forgeListView.setAdapter((ListAdapter) downloadForgeListAdapter);
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
    private static class ForgeCompareTool implements Comparator<ForgeVersion> {
        private ForgeCompareTool() {
        }

        @Override // java.util.Comparator
        public int compare(ForgeVersion forgeVersion, ForgeVersion forgeVersion2) {
            return Integer.compare(forgeVersion2.getBuild(), forgeVersion.getBuild());
        }
    }
}
