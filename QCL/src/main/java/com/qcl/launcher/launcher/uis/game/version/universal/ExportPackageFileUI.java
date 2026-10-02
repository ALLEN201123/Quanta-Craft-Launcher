package com.qcl.launcher.launcher.uis.game.version.universal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.mod.export.ModpackExporter;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.gson.GsonUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class ExportPackageFileUI extends BaseUI implements View.OnClickListener {

    /** 导出产物所在目录名（位于游戏目录下） */
    private static final String OUTPUT_DIR_NAME = "QCL导出";

    public LinearLayout exportPackageFileUI;

    private CheckBox modsBox;
    private CheckBox configBox;
    private CheckBox scriptsBox;
    private CheckBox resourcepacksBox;
    private CheckBox shaderpacksBox;
    private CheckBox optionsBox;

    private Button start;
    private Button cancel;

    private LinearLayout progressArea;
    private ProgressBar progressBar;
    private TextView progressText;
    private TextView currentFileText;

    /** 上一步写入的整合包信息 */
    private int packType = ModpackExporter.TYPE_HMCL;
    private String packName = "";
    private String packVersion = "";
    private String packAuthor = "";
    private String fileName = "";

    private Handler handler;
    private volatile ModpackExporter exporter;
    private boolean exporting = false;

    public ExportPackageFileUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    /** 由 {@link ExportPackageInfoUI} 调用 */
    public void setPackInfo(int packType, String name, String version, String author, String fileName) {
        this.packType = packType;
        this.packName = name == null ? "" : name;
        this.packVersion = version == null ? "" : version;
        this.packAuthor = author == null ? "" : author;
        this.fileName = fileName == null ? "" : fileName;
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.handler = new Handler(Looper.getMainLooper());
        this.exportPackageFileUI = (LinearLayout) this.activity.findViewById(R.id.ui_export_package_file);

        this.modsBox = (CheckBox) this.activity.findViewById(R.id.export_package_file_mods);
        this.configBox = (CheckBox) this.activity.findViewById(R.id.export_package_file_config);
        this.scriptsBox = (CheckBox) this.activity.findViewById(R.id.export_package_file_scripts);
        this.resourcepacksBox = (CheckBox) this.activity.findViewById(R.id.export_package_file_resourcepacks);
        this.shaderpacksBox = (CheckBox) this.activity.findViewById(R.id.export_package_file_shaderpacks);
        this.optionsBox = (CheckBox) this.activity.findViewById(R.id.export_package_file_options);

        this.progressArea = (LinearLayout) this.activity.findViewById(R.id.export_package_progress_area);
        this.progressBar = (ProgressBar) this.activity.findViewById(R.id.export_package_progress_bar);
        this.progressText = (TextView) this.activity.findViewById(R.id.export_package_progress_text);
        this.currentFileText = (TextView) this.activity.findViewById(R.id.export_package_current_file);

        this.start = (Button) this.activity.findViewById(R.id.export_package_start);
        this.cancel = (Button) this.activity.findViewById(R.id.export_package_cancel);
        this.start.setOnClickListener(this);
        this.cancel.setOnClickListener(this);

        // 默认勾选常用的三个目录
        this.modsBox.setChecked(true);
        this.configBox.setChecked(true);
        this.scriptsBox.setChecked(true);
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.export_package_file_ui_title), false, true);
        CustomAnimationUtils.showViewFromLeft(this.exportPackageFileUI, this.activity, this.context, true);
        // 每次进入都复位到可导出状态
        this.exporting = false;
        this.exporter = null;
        this.start.setEnabled(true);
        this.cancel.setEnabled(false);
        this.progressArea.setVisibility(View.GONE);
        this.progressBar.setProgress(0);
        this.progressText.setText("");
        this.currentFileText.setText("");

        boolean server = this.packType == ModpackExporter.TYPE_SERVER;
        // 服务端包只含 mods/config/scripts
        this.resourcepacksBox.setEnabled(!server);
        this.shaderpacksBox.setEnabled(!server);
        this.optionsBox.setEnabled(!server);
        if (server) {
            this.resourcepacksBox.setChecked(false);
            this.shaderpacksBox.setChecked(false);
            this.optionsBox.setChecked(false);
        }
    }

    @Override // com.qcl.launcher.launcher.uis.tools.BaseUI, com.qcl.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.exportPackageFileUI, this.activity, this.context, true);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view == this.start) {
            startExport();
        } else if (view == this.cancel) {
            ModpackExporter current = this.exporter;
            if (current != null) {
                current.cancel();
            }
            this.cancel.setEnabled(false);
        }
    }

    private void startExport() {
        if (this.exporting) {
            return;
        }

        String versionPath = this.activity.publicGameSetting == null ? null : this.activity.publicGameSetting.currentVersion;
        if (versionPath == null || versionPath.isEmpty()) {
            Toast.makeText(this.context, this.context.getString(R.string.qcl_export_pack_no_version), Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> includes = new ArrayList<>();
        if (this.modsBox.isChecked()) {
            includes.add("mods");
        }
        if (this.configBox.isChecked()) {
            includes.add("config");
        }
        if (this.scriptsBox.isChecked()) {
            includes.add("scripts");
        }
        if (this.resourcepacksBox.isChecked()) {
            includes.add("resourcepacks");
        }
        if (this.shaderpacksBox.isChecked()) {
            includes.add("shaderpacks");
        }
        if (this.optionsBox.isChecked()) {
            includes.add("options.txt");
        }
        if (includes.isEmpty()) {
            Toast.makeText(this.context, this.context.getString(R.string.qcl_export_pack_nothing_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        String baseDir = this.activity.launcherSetting.gameFileDirectory;
        final String gameDir = resolveGameDir(baseDir, versionPath);
        final File versionDir = new File(versionPath);
        final File outputDir = new File(baseDir, OUTPUT_DIR_NAME);
        final int type = this.packType;
        final String name = this.packName;
        final String version = this.packVersion;
        final String author = this.packAuthor;
        final String zipName = this.fileName;
        final List<String> includeList = includes;

        this.exporting = true;
        this.start.setEnabled(false);
        this.cancel.setEnabled(true);
        this.progressArea.setVisibility(View.VISIBLE);
        this.progressBar.setProgress(0);
        this.progressText.setText(this.context.getString(R.string.qcl_export_pack_progress_percent, 0));
        this.currentFileText.setText("");

        new Thread(new Runnable() {
            @Override
            public void run() {
                ModpackExporter ex = new ModpackExporter(
                        new File(gameDir),
                        versionDir,
                        outputDir,
                        zipName,
                        type,
                        name,
                        version,
                        author,
                        "",
                        includeList,
                        new ModpackExporter.ProgressListener() {
                            @Override
                            public void onProgress(final int percent, final String currentFile) {
                                ExportPackageFileUI.this.handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        ExportPackageFileUI.this.progressBar.setProgress(percent);
                                        ExportPackageFileUI.this.progressText.setText(ExportPackageFileUI.this.context.getString(R.string.qcl_export_pack_progress_percent, percent));
                                        ExportPackageFileUI.this.currentFileText.setText(ExportPackageFileUI.this.context.getString(R.string.qcl_export_pack_current_file, currentFile));
                                    }
                                });
                            }
                        });
                ExportPackageFileUI.this.exporter = ex;
                try {
                    ex.export();
                    final File out = ex.getOutputFile();
                    ExportPackageFileUI.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            finishExport(true, out == null ? "" : out.getAbsolutePath(), false);
                        }
                    });
                } catch (IOException e) {
                    final boolean wasCanceled = ex.isCanceled();
                    ExportPackageFileUI.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            finishExport(false, e.getMessage(), wasCanceled);
                        }
                    });
                }
            }
        }).start();
    }

    /** 复位界面并提示结果 */
    private void finishExport(boolean success, String message, boolean canceled) {
        this.exporting = false;
        this.exporter = null;
        this.start.setEnabled(true);
        this.cancel.setEnabled(false);
        this.progressArea.setVisibility(View.GONE);

        if (success) {
            Toast.makeText(this.context, this.context.getString(R.string.qcl_export_pack_done, message), Toast.LENGTH_LONG).show();
        } else if (canceled) {
            Toast.makeText(this.context, this.context.getString(R.string.qcl_export_pack_canceled), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this.context, this.context.getString(R.string.qcl_export_pack_failed, message == null ? "" : message), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 计算实际游戏工作目录，处理版本隔离开/关两种情况。
     *
     * <p>currentVersion 存的是完整路径（&lt;游戏目录&gt;/versions/&lt;版本名&gt;）。</p>
     * <ul>
     *     <li>版本隔离开启：用版本目录下的 qcl.cfg 里的 gameDirSetting</li>
     *     <li>版本隔离关闭：用全局 privateGameSetting.gameDirSetting</li>
     * </ul>
     */
    private String resolveGameDir(String baseDir, String versionPath) {
        try {
            if (PublicGameSetting.isUsingIsolateSetting(versionPath)) {
                PrivateGameSetting setting = GsonUtils.getPrivateGameSettingFromFile(versionPath + "/qcl.cfg");
                if (setting != null && setting.gameDirSetting != null) {
                    return PrivateGameSetting.getGameDir(baseDir, versionPath, setting.gameDirSetting);
                }
            } else {
                PrivateGameSetting setting = this.activity.privateGameSetting;
                if (setting != null && setting.gameDirSetting != null) {
                    return PrivateGameSetting.getGameDir(baseDir, versionPath, setting.gameDirSetting);
                }
            }
        } catch (Throwable ignored) {
        }
        return baseDir;
    }
}