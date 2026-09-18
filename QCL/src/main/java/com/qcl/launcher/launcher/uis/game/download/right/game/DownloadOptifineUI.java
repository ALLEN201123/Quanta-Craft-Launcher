package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.optifine.OptifineVersion;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadOptifineListAdapter;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class DownloadOptifineUI extends BaseUI implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    public static final String OPTIFINE_VERSION_MANIFEST = "https://bmclapi2.bangbang93.com/optifine/versionlist";
    private ArrayList<OptifineVersion> allList;
    private TextView back;
    private CheckBox checkOld;
    private CheckBox checkRelease;
    private CheckBox checkSnapshot;
    public LinearLayout downloadOptifineUI;
    private LinearLayout hintLayout;
    public boolean install;
    private LinearLayout listLayout;
    private final Handler loadingHandler;
    private ListView optifineListView;
    private ProgressBar progressBar;
    private LinearLayout refresh;
    public String version;

    public DownloadOptifineUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadOptifineUI.1
            @Override // android.os.Handler
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    DownloadOptifineUI.this.listLayout.setVisibility(8);
                    DownloadOptifineUI.this.progressBar.setVisibility(0);
                    DownloadOptifineUI.this.back.setVisibility(8);
                }
                if (message.what == 1) {
                    DownloadOptifineUI.this.listLayout.setVisibility(0);
                    DownloadOptifineUI.this.progressBar.setVisibility(8);
                    DownloadOptifineUI.this.back.setVisibility(8);
                }
                if (message.what == 2) {
                    DownloadOptifineUI.this.listLayout.setVisibility(8);
                    DownloadOptifineUI.this.progressBar.setVisibility(8);
                    DownloadOptifineUI.this.back.setVisibility(0);
                }
            }
        };
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.downloadOptifineUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_optifine_list);
        LinearLayout linearLayout = (LinearLayout) this.activity.findViewById(R.id.download_forge_hint_layout);
        this.hintLayout = linearLayout;
        linearLayout.setOnClickListener(this);
        this.listLayout = (LinearLayout) this.activity.findViewById(R.id.optifine_list_layout);
        this.checkRelease = (CheckBox) this.activity.findViewById(R.id.optifine_checkbox_release);
        this.checkSnapshot = (CheckBox) this.activity.findViewById(R.id.optifine_checkbox_snapshot);
        this.checkOld = (CheckBox) this.activity.findViewById(R.id.optifine_checkbox_old);
        this.refresh = (LinearLayout) this.activity.findViewById(R.id.refresh_optifine_list);
        this.checkRelease.setChecked(true);
        this.checkRelease.setOnCheckedChangeListener(this);
        this.checkSnapshot.setOnCheckedChangeListener(this);
        this.checkOld.setOnCheckedChangeListener(this);
        this.refresh.setOnClickListener(this);
        this.optifineListView = (ListView) this.activity.findViewById(R.id.optifine_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_optifine_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_optifine);
        this.back = textView;
        textView.setOnClickListener(this);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.optifine_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadOptifineUI, this.activity, this.context, true);
        init();
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadOptifineUI, this.activity, this.context, true);
    }

    private void init() {
        final ArrayList arrayList = new ArrayList();
        this.allList = new ArrayList<>();
        new Thread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadOptifineUI$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                DownloadOptifineUI.this.m491x6ae71b15(arrayList);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$1$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadOptifineUI, reason: not valid java name */
    public /* synthetic */ void m491x6ae71b15(ArrayList arrayList) {
        this.loadingHandler.sendEmptyMessage(0);
        try {
            for (OptifineVersion optifineVersion : (OptifineVersion[]) new Gson().fromJson(NetworkUtils.doGet(NetworkUtils.toURL("https://bmclapi2.bangbang93.com/optifine/versionlist")), OptifineVersion[].class)) {
                if (optifineVersion.mcVersion.equals(this.version) && this.checkRelease.isChecked() && !optifineVersion.patch.startsWith("pre") && !optifineVersion.patch.startsWith("alpha")) {
                    arrayList.add(optifineVersion);
                }
                if (optifineVersion.mcVersion.equals(this.version) && this.checkSnapshot.isChecked() && (optifineVersion.patch.startsWith("pre") || optifineVersion.patch.startsWith("alpha"))) {
                    arrayList.add(optifineVersion);
                }
                if (optifineVersion.mcVersion.equals(this.version)) {
                    this.allList.add(optifineVersion);
                }
            }
            arrayList.sort(new OptifineCompareTool());
            final DownloadOptifineListAdapter downloadOptifineListAdapter = new DownloadOptifineListAdapter(this.context, this.activity, arrayList, this.install);
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadOptifineUI$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadOptifineUI.this.m490x3738f054(downloadOptifineListAdapter);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (this.allList.size() == 0) {
            this.loadingHandler.sendEmptyMessage(2);
        } else {
            this.loadingHandler.sendEmptyMessage(1);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$init$0$com-qcl-launcher-launcher-uis-game-download-right-game-DownloadOptifineUI, reason: not valid java name */
    public /* synthetic */ void m490x3738f054(DownloadOptifineListAdapter downloadOptifineListAdapter) {
        this.optifineListView.setAdapter((ListAdapter) downloadOptifineListAdapter);
    }

    private void refresh() {
        ArrayList arrayList = new ArrayList();
        Iterator<OptifineVersion> it = this.allList.iterator();
        while (it.hasNext()) {
            OptifineVersion next = it.next();
            if (next.mcVersion.equals(this.version) && this.checkRelease.isChecked() && !next.patch.startsWith("pre") && !next.patch.startsWith("alpha")) {
                arrayList.add(next);
            }
            if (next.mcVersion.equals(this.version) && this.checkSnapshot.isChecked() && (next.patch.startsWith("pre") || next.patch.startsWith("alpha"))) {
                arrayList.add(next);
            }
        }
        arrayList.sort(new OptifineCompareTool());
        this.optifineListView.setAdapter((ListAdapter) new DownloadOptifineListAdapter(this.context, this.activity, arrayList, this.install));
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.hintLayout) {
            this.context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("https://afdian.net/@bangbang93")));
        }
        if (view == this.back) {
            this.activity.backToLastUI();
        }
        if (view == this.refresh) {
            init();
        }
    }

    @Override // android.widget.CompoundButton.OnCheckedChangeListener
    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class OptifineCompareTool implements Comparator<OptifineVersion> {
        private OptifineCompareTool() {
        }

        @Override // java.util.Comparator
        public int compare(OptifineVersion optifineVersion, OptifineVersion optifineVersion2) {
            String str;
            String substring;
            if (optifineVersion.patch.length() == optifineVersion2.patch.length() || (optifineVersion.patch.startsWith("pre") && optifineVersion2.patch.startsWith("pre"))) {
                if (optifineVersion.patch.length() == 2) {
                    if (optifineVersion.patch.charAt(0) > optifineVersion2.patch.charAt(0)) {
                        return -1;
                    }
                    if (optifineVersion.patch.charAt(0) == optifineVersion2.patch.charAt(0)) {
                        return Character.compare(optifineVersion2.patch.charAt(1), optifineVersion.patch.charAt(1));
                    }
                    return 1;
                }
                if (optifineVersion.type.substring(optifineVersion.type.length() - 2).charAt(0) > optifineVersion2.type.substring(optifineVersion2.type.length() - 2).charAt(0)) {
                    return -1;
                }
                if (optifineVersion.type.substring(optifineVersion.type.length() - 2).charAt(0) == optifineVersion2.type.substring(optifineVersion2.type.length() - 2).charAt(0)) {
                    if (optifineVersion.type.substring(optifineVersion.type.length() - 2).charAt(1) > optifineVersion2.type.substring(optifineVersion2.type.length() - 2).charAt(1)) {
                        return -1;
                    }
                    if (optifineVersion.type.substring(optifineVersion.type.length() - 2).charAt(1) == optifineVersion2.type.substring(optifineVersion2.type.length() - 2).charAt(1)) {
                        return Integer.compare(Integer.parseInt(optifineVersion2.patch.replace("pre", "")), Integer.parseInt(optifineVersion.patch.replace("pre", "")));
                    }
                }
                return 1;
            }
            if (optifineVersion.patch.startsWith("pre")) {
                str = optifineVersion.type.substring(optifineVersion.type.length() - 2);
                substring = optifineVersion2.patch;
            } else {
                str = optifineVersion.patch;
                substring = optifineVersion2.type.substring(optifineVersion2.type.length() - 2);
            }
            if (str.charAt(0) > substring.charAt(0)) {
                return -1;
            }
            if (str.charAt(0) == substring.charAt(0)) {
                if (str.charAt(1) > substring.charAt(1)) {
                    return -1;
                }
                if (str.charAt(1) == substring.charAt(1)) {
                    return Integer.compare(optifineVersion.patch.length(), optifineVersion2.patch.length());
                }
            }
            return 1;
        }
    }
}
