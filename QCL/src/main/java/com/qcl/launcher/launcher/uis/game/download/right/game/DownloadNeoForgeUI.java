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

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersion;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersions;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadNeoForgeListAdapter;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import java.util.ArrayList;

import com.qcl.launcher.R;

/**
 * ★ NeoForge 版本选择页（★ 1.4.2 起改为「Forge 同款」）。
 *
 * 结构完全照 {@link DownloadForgeUI} 与 ui_install_forge_list.xml：
 * 提示条 + 版本列表 + 进度条 + 返回。
 *
 * 与 DownloadForgeUI 唯一的差别：NeoForge 的版本列表接口按 MC 版本查，
 * 所以这里按 {@code this.version}（由 InstallGameUI 传入的 MC 版本 id）直查，
 * 不再需要 MC 版本下拉。
 *
 * 点击某个版本 → DownloadNeoForgeListAdapter 把它写进
 * {@code installGameUI.neoForgeVersion} 并 backToLastUI()，
 * 真正的安装由 GameInstallDialog 在点「安装游戏」时统一跑。
 */
public class DownloadNeoForgeUI extends BaseUI implements View.OnClickListener {
    public LinearLayout downloadNeoForgeUI;
    private ListView listView;
    private ProgressBar progressBar;
    private TextView back;
    private final Handler loadingHandler;
    public boolean install;
    public String version;

    public DownloadNeoForgeUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    listView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    back.setVisibility(View.GONE);
                }
                if (message.what == 1) {
                    listView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    back.setVisibility(View.GONE);
                }
                if (message.what == 2) {
                    listView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    back.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.downloadNeoForgeUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_neoforge_list);
        // ★ 1.5.0：顶部「提示」条已移除（用户要求：太占地方）。
        //   原来这里是 this.hintLayout.setOnClickListener(this)（连字段都没判空），
        //   控件删掉后 findViewById 返回 null → UIManager 构造期 NPE 直接打崩启动器。
        this.listView = (ListView) this.activity.findViewById(R.id.neoforge_version_list);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.loading_neoforge_list_progress);
        TextView textView = (TextView) this.activity.findViewById(R.id.back_to_install_ui_neoforge);
        this.back = textView;
    }

    @Override
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.neoforge_list_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.downloadNeoForgeUI, this.activity, this.context, true);
        init();
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadNeoForgeUI, this.activity, this.context, true);
    }

    private void init() {
        new Thread(() -> {
            final String mc = this.version == null ? "" : this.version;
            this.loadingHandler.sendEmptyMessage(0);
            final ArrayList<NeoForgeVersion> result = NeoForgeVersions.fetchForMc(mc);
            this.activity.runOnUiThread(() -> {
                DownloadNeoForgeListAdapter adapter = new DownloadNeoForgeListAdapter(
                        this.context, this.activity, result, this.install);
                this.listView.setAdapter((ListAdapter) adapter);
            });
            if (result.isEmpty()) {
                this.loadingHandler.sendEmptyMessage(2);
            } else {
                this.loadingHandler.sendEmptyMessage(1);
            }
        }, "neoforge-version-list").start();
    }

    @Override
    public void onClick(View view) {
        // ★ 1.5.0：`view == hintLayout` → 打开 NeoForged 官网的分支已随提示条一起移除。
        if (view == this.back) {
            this.activity.backToLastUI();
        }
    }
}
