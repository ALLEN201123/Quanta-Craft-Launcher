package com.qcl.launcher.launcher.uis.game.download.right.game;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeInstallTask;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersion;
import com.qcl.launcher.launcher.download.neoforge.NeoForgeVersions;
import com.qcl.launcher.launcher.list.download.minecraft.DownloadNeoForgeListAdapter;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ★ NeoForge 安装页（下载页的第 7 个子页面）。
 *
 * 结构照 {@link DownloadForgeUI} 与 ui_install_forge_list.xml：
 * 版本选择列表 + 进度条，另加了「MC 版本筛选」下拉与「安装」按钮
 * （NeoForge 列表接口是按 MC 版本查的，所以筛选是必要的）。
 *
 * 与 DownloadForgeUI 的区别：本页是 DownloadUIManager 的子页面（不是 UIManager 的顶级页），
 * 所以动画用 {@code false}、背景高亮用 downloadUI 里的入口，不高亮/改标题栏
 * ——与 DownloadMinecraftUI / DownloadWorldUI 等兄弟页保持一致。
 */
public class DownloadNeoForgeUI extends BaseUI implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    public LinearLayout downloadNeoForgeUI;

    private LinearLayout hintLayout;
    private Spinner mcSpinner;
    private LinearLayout refresh;
    private ListView listView;
    private ProgressBar loadingProgress;
    private TextView noVersionText;

    private LinearLayout statusLayout;
    private TextView statusText;
    private ProgressBar installProgress;
    private TextView selectedText;
    private Button installButton;

    private final ArrayList<NeoForgeVersion> versions = new ArrayList<>();
    private DownloadNeoForgeListAdapter adapter;
    private NeoForgeVersion selectedVersion;

    private boolean loading;
    private boolean installing;
    private String loadedMc;
    private NeoForgeInstallTask installTask;

    private final Handler loadingHandler;

    public DownloadNeoForgeUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
        this.loadingHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                super.handleMessage(message);
                if (message.what == 0) {
                    listView.setVisibility(View.GONE);
                    loadingProgress.setVisibility(View.VISIBLE);
                    noVersionText.setVisibility(View.GONE);
                }
                if (message.what == 1) {
                    listView.setVisibility(View.VISIBLE);
                    loadingProgress.setVisibility(View.GONE);
                    noVersionText.setVisibility(View.GONE);
                }
                if (message.what == 2) {
                    listView.setVisibility(View.GONE);
                    loadingProgress.setVisibility(View.GONE);
                    noVersionText.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.downloadNeoForgeUI = (LinearLayout) this.activity.findViewById(R.id.ui_install_neoforge_list);
        this.hintLayout = (LinearLayout) this.activity.findViewById(R.id.download_neoforge_hint_layout);
        this.mcSpinner = (Spinner) this.activity.findViewById(R.id.neoforge_mc_version_spinner);
        this.refresh = (LinearLayout) this.activity.findViewById(R.id.refresh_neoforge_list);
        this.listView = (ListView) this.activity.findViewById(R.id.neoforge_version_list);
        this.loadingProgress = (ProgressBar) this.activity.findViewById(R.id.loading_neoforge_list_progress);
        this.noVersionText = (TextView) this.activity.findViewById(R.id.no_neoforge_version_text);
        this.statusLayout = (LinearLayout) this.activity.findViewById(R.id.neoforge_install_status_layout);
        this.statusText = (TextView) this.activity.findViewById(R.id.neoforge_install_status_text);
        this.installProgress = (ProgressBar) this.activity.findViewById(R.id.neoforge_install_progress);
        this.selectedText = (TextView) this.activity.findViewById(R.id.neoforge_selected_text);
        this.installButton = (Button) this.activity.findViewById(R.id.install_neoforge);

        this.refresh.setOnClickListener(this);
        this.noVersionText.setOnClickListener(this);
        this.hintLayout.setOnClickListener(this);
        this.installButton.setOnClickListener(this);
        this.selectedText.setText(this.context.getString(R.string.neoforge_select_version_hint));

        List<String> mcList = NeoForgeVersions.getSupportedMcVersions();
        ArrayAdapter<String> mcAdapter = new ArrayAdapter<>(this.context, R.layout.item_spinner, mcList);
        mcAdapter.setDropDownViewResource(R.layout.item_spinner_drop_down);
        this.mcSpinner.setAdapter((SpinnerAdapter) mcAdapter);
        this.mcSpinner.setOnItemSelectedListener(this);
        this.mcSpinner.setSelection(0);

        this.adapter = new DownloadNeoForgeListAdapter(this.context, this.activity, this.versions, version -> selectVersion(version));
        this.listView.setAdapter((ListAdapter) this.adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(this.downloadNeoForgeUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadNeoForgeUI.setBackground(
                    this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
        String mc = currentMc();
        if (versions.isEmpty() || !mc.equals(loadedMc)) {
            load(mc);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.downloadNeoForgeUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.downloadUI.startDownloadNeoForgeUI.setBackground(
                    this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    private String currentMc() {
        Object item = this.mcSpinner.getSelectedItem();
        return item == null ? "" : item.toString();
    }

    private void load(final String mc) {
        // onCreate 里 spinner.setSelection(0) 会先触发一次 onItemSelected，
        // 那时 adapter 还没建好 —— 直接跳过，等真正进页面（onStart）再拉。
        if (this.adapter == null || this.loading) {
            return;
        }
        this.loading = true;
        this.loadedMc = mc;
        this.loadingHandler.sendEmptyMessage(0);
        new Thread(() -> {
            final ArrayList<NeoForgeVersion> result = NeoForgeVersions.fetchForMc(mc);
            this.activity.runOnUiThread(() -> {
                this.loading = false;
                this.versions.clear();
                this.versions.addAll(result);
                this.adapter.notifyDataSetChanged();
                if (this.versions.isEmpty()) {
                    this.loadingHandler.sendEmptyMessage(2);
                } else {
                    this.loadingHandler.sendEmptyMessage(1);
                }
            });
        }, "neoforge-version-list").start();
    }

    private void selectVersion(NeoForgeVersion version) {
        this.selectedVersion = version;
        this.selectedText.setText(this.context.getString(R.string.neoforge_selected,
                version.getVersion() + "  (MC " + version.getGameVersion() + ")"));
    }

    @Override
    public void onClick(View view) {
        if (view == this.refresh || view == this.noVersionText) {
            load(currentMc());
        }
        if (view == this.installButton) {
            confirmInstall();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        load(currentMc());
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void confirmInstall() {
        if (this.installing) {
            return;
        }
        final NeoForgeVersion version = this.selectedVersion;
        if (version == null) {
            Toast.makeText(this.context, R.string.neoforge_error_select_first, Toast.LENGTH_SHORT).show();
            return;
        }
        String name = "neoforge-" + version.getVersion();
        if (SettingUtils.getLocalVersionNames(this.activity.launcherSetting.gameFileDirectory).contains(name)) {
            new AlertDialog.Builder(this.context)
                    .setTitle(R.string.neoforge_confirm_title)
                    .setMessage(this.context.getString(R.string.neoforge_already_exists, name))
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }
        new AlertDialog.Builder(this.context)
                .setTitle(R.string.neoforge_confirm_title)
                .setMessage(this.context.getString(R.string.neoforge_confirm_message,
                        version.getVersion(), version.getGameVersion()))
                .setPositiveButton(R.string.neoforge_install, (dialog, which) -> startInstall(version))
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    private void startInstall(NeoForgeVersion version) {
        this.installing = true;
        this.installButton.setEnabled(false);
        this.installProgress.setProgress(0);
        this.statusLayout.setVisibility(View.VISIBLE);
        this.statusText.setText(this.context.getString(R.string.neoforge_install_stage_installer));

        this.installTask = new NeoForgeInstallTask(this.activity, new NeoForgeInstallTask.InstallNeoForgeCallback() {
            @Override
            public void onStart() {
                statusLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onProgress(int percent, String message) {
                installProgress.setProgress(Math.max(0, Math.min(100, percent)));
                statusText.setText(message);
            }

            @Override
            public void onFailed(Exception e) {
                installing = false;
                installButton.setEnabled(true);
                statusLayout.setVisibility(View.GONE);
                new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_install_fail_title)
                        .setMessage(String.valueOf(e))
                        .setPositiveButton(R.string.dialog_install_fail_positive, null)
                        .create().show();
            }

            @Override
            public void onFinish(String versionId) {
                installing = false;
                installButton.setEnabled(true);
                statusLayout.setVisibility(View.GONE);
                if (activity.isLoaded) {
                    activity.uiManager.downloadUI.startDownloadNeoForgeUI.setBackground(
                            context.getResources().getDrawable(R.drawable.launcher_button_white));
                }
                new AlertDialog.Builder(context)
                        .setTitle(R.string.dialog_install_success_title)
                        .setMessage(context.getString(R.string.dialog_install_success_text) + "\n\n" + versionId)
                        .setPositiveButton(R.string.dialog_install_success_positive, null)
                        .create().show();
                new Thread(() -> activity.uiManager.versionListUI.refreshVersionList()).start();
            }
        });
        this.installTask.execute(version);
    }
}