package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.content.Context;
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
        // ★ 1.5.0：顶部「提示」条已移除（用户要求：太占地方）。
        //   原来这里对 hintLayout 是**无条件** setOnClickListener(this)，
        //   控件删掉后 findViewById 返回 null，会在 UIManager 构造期直接 NPE 把启动器打崩，
        //   所以字段 / 查找 / 监听三处必须一起删。
        this.forgeListView = (ListView) this.activity.findViewById(R.id.forge_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_forge_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_forge);
        this.back = textView;
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
        // ★★★★★ 2026-10-16 修「自动安装选 Forge → 详情页没有列表，只剩空提示词」
        //   （评测视频 60s：「还不能自己装」；用户原话：
        //     「在1.20.1整合包那里，自动安装那里选择forge，点进去，进入详情页，
        //       里面没有他的列表，只有没有任何安装列表的提示词摆在中间。
        //       玩家想要自己下载，但下也下载不了」）。
        //
        //   ★ 根因：原来**只有一个数据源** `bmclapi2.bangbang93.com`，而且
        //     <pre>
        //       try { ... } catch (IOException e) { e.printStackTrace(); }  // ← 失败被吞掉
        //       if (arrayList.size() == 0) sendEmptyMessage(2);              // ← 直接走"没有列表"
        //     </pre>
        //     一旦超时 / DNS 失败 / BMCLAPI 临时挂掉，adapter **从不设置**，
        //     界面就停在「没有任何安装列表」的提示词上 —— 用户**想自己下 Forge 也下不了**。
        //
        //   ★ 修法：**多数据源回退 + 每源重试**，全部失败才显示空提示（那时是真没数据）。
        String[] urls = {
                "https://bmclapi2.bangbang93.com/forge/minecraft/" + this.version,
                "https://bmclapi.bangbang93.com/forge/minecraft/" + this.version,
                "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json",
        };
        this.loadingHandler.sendEmptyMessage(0);
        ArrayList arrayList = new ArrayList();
        Throwable lastError = null;
        for (String str : urls) {
            if (arrayList.size() > 0) {
                break;
            }
            for (int attempt = 0; attempt < 2 && arrayList.size() == 0; attempt++) {
                try {
                    String body = NetworkUtils.doGet(NetworkUtils.toURL(str));
                    if (body == null || body.length() == 0) {
                        continue;
                    }
                    ForgeVersion[] parsed = (ForgeVersion[]) new Gson().fromJson(body, ForgeVersion[].class);
                    if (parsed != null && parsed.length > 0) {
                        arrayList.addAll(Arrays.asList(parsed));
                    }
                } catch (Throwable t) {
                    lastError = t;   // ★ 不再吞掉：记下来后继续尝试下一个源
                    try {
                        Thread.sleep(400L * (attempt + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        if (lastError != null) {
            lastError.printStackTrace();
        }
        if (arrayList.size() > 0) {
            try {
                arrayList.sort(new ForgeCompareTool());
            } catch (Throwable ignored) {
            }
            final DownloadForgeListAdapter downloadForgeListAdapter = new DownloadForgeListAdapter(this.context, this.activity, arrayList, this.install);
            this.activity.runOnUiThread(new Runnable() { // from class: com.qcl.launcher.launcher.uis.game.download.right.game.DownloadForgeUI$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    DownloadForgeUI.this.m486x8fc1074b(downloadForgeListAdapter);
                }
            });
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
        // ★ 1.5.0：`view == hintLayout` → 打开赞助页的分支已随提示条一起移除。
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
