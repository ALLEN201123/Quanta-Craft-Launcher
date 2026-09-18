package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
import android.content.DialogInterface;
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
import androidx.appcompat.app.AlertDialog;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadFabricAPIListAdapter;
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
public class DownloadFabricAPIUI extends BaseUI implements View.OnClickListener {
    private static final String FABRIC_API_ID = "P7dR8mSH";
    private TextView back;
    public LinearLayout downloadFabricAPIUI;
    private ListView fabricAPIListView;
    private LinearLayout hintLayout;
    public boolean install;
    private final Handler loadingHandler;
    private ProgressBar progressBar;
    private TextView refreshText;
    public String version;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ void lambda$init$0(DialogInterface dialogInterface, int i) {
    }

    public DownloadFabricAPIUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricAPIUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadFabricAPIUI.this.fabricAPIListView.setVisibility(8);
                    DownloadFabricAPIUI.this.progressBar.setVisibility(0);
                    DownloadFabricAPIUI.this.refreshText.setVisibility(8);
                    DownloadFabricAPIUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadFabricAPIUI.this.fabricAPIListView.setVisibility(0);
                    DownloadFabricAPIUI.this.progressBar.setVisibility(8);
                    DownloadFabricAPIUI.this.refreshText.setVisibility(8);
                    DownloadFabricAPIUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadFabricAPIUI.this.fabricAPIListView.setVisibility(8);
                    DownloadFabricAPIUI.this.progressBar.setVisibility(8);
                    DownloadFabricAPIUI.this.refreshText.setVisibility(8);
                    DownloadFabricAPIUI.this.back.setVisibility(0);
                }
                if (message.what == 3) {
                    DownloadFabricAPIUI.this.fabricAPIListView.setVisibility(8);
                    DownloadFabricAPIUI.this.progressBar.setVisibility(8);
                    DownloadFabricAPIUI.this.refreshText.setVisibility(0);
                    DownloadFabricAPIUI.this.back.setVisibility(8);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadFabricAPIUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_fabric_api_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_fabric_api_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.fabricAPIListView = (ListView) this.activity.findViewById(R.id.fabric_api_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_fabric_api_list_progress);
        this.refreshText = (TextView) this.activity.findViewById(R.id.refresh_fabric_api_list);
        this.back = (TextView) this.activity.findViewById(R.id.back_to_install_ui_fabric_api);
        this.refreshText.setOnClickListener(this);
        this.back.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.fabric_api_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadFabricAPIUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadFabricAPIUI, this.activity, this.context, true);
    }

    private void init() {
        if (!this.install) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
            builder.setTitle(R.string.fabric_api_list_ui_warn);
            builder.setMessage(R.string.fabric_api_list_ui_warn_text);
            builder.setPositiveButton(R.string.fabric_api_list_ui_positive, new DialogInterface.OnClickListener() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricAPIUI$$ExternalSyntheticLambda0
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    DownloadFabricAPIUI.lambda$init$0(dialogInterface, i);
                }
            });
            builder.create().show();
        }
        refresh();
    }

    private void refresh() {
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricAPIUI$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DownloadFabricAPIUI.this.m483xe762bac6();
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$refresh$2$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadFabricAPIUI, reason: not valid java name */
    public /* synthetic */ void m483xe762bac6() {
        boolean z = false;
        this.loadingHandler.sendEmptyMessage(0);
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        try {
            arrayList.addAll((List) ModrinthRemoteModRepository.MODS.getRemoteVersionsById("P7dR8mSH").collect(Collectors.toList()));
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                RemoteMod.Version version = (RemoteMod.Version) it.next();
                if (version.getGameVersions().contains(this.version)) {
                    arrayList2.add(version);
                    z = true;
                }
            }
            if (z) {
                final DownloadFabricAPIListAdapter downloadFabricAPIListAdapter = new DownloadFabricAPIListAdapter(this.context, this.activity, this.version, arrayList2, this.install);
                this.loadingHandler.post(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadFabricAPIUI$$ExternalSyntheticLambda2
                    @Override // java.lang.Runnable
                    public final void run() {
                        DownloadFabricAPIUI.this.m482xa54b8d67(downloadFabricAPIListAdapter);
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
    /* renamed from: lambda$refresh$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadFabricAPIUI, reason: not valid java name */
    public /* synthetic */ void m482xa54b8d67(DownloadFabricAPIListAdapter downloadFabricAPIListAdapter) {
        this.fabricAPIListView.setAdapter((ListAdapter) downloadFabricAPIListAdapter);
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
