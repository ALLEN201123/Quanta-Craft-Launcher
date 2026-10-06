package com.qcl.launcher.launcher.uis.game.version.universal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.mod.BaseModpackInstallTask;
import com.qcl.launcher.launcher.mod.GenericModpackInstallTask;
import com.qcl.launcher.launcher.mod.ManuallyCreatedModpackException;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackHelper;
import com.qcl.launcher.launcher.mod.ModpackManifest;
import com.qcl.launcher.launcher.mod.UnsupportedModpackException;
import com.qcl.launcher.launcher.mod.curse.CurseInstallTask;
import com.qcl.launcher.launcher.mod.curse.CurseManifest;
import com.qcl.launcher.launcher.mod.qclpack.QclModpackInstallTask;
import com.qcl.launcher.launcher.mod.qclpack.QclModpackManifest;
import com.qcl.launcher.launcher.mod.mcbbs.McbbsModpackLocalInstallTask;
import com.qcl.launcher.launcher.mod.mcbbs.McbbsModpackManifest;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthInstallTask;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthManifest;
import com.qcl.launcher.launcher.mod.multimc.MultiMCInstanceConfiguration;
import com.qcl.launcher.launcher.mod.multimc.MultiMCModpackInstallTask;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.StringUtils;

import java.io.File;
import java.io.IOException;

public class InstallPackageUI extends BaseUI implements View.OnClickListener {

    public static final int SELECT_PACKAGE_REQUEST = 5700;

    public LinearLayout installPackageUI;

    private LinearLayout selectLayout;
    private LinearLayout installLayout;
    private ProgressBar progressBar;

    private LinearLayout installLocal;
    private LinearLayout installOnline;

    private TextView pathText;
    private TextView nameText;
    private TextView versionText;
    private TextView authorText;
    private EditText editName;

    private Button showDescription;
    private Button install;

    public Modpack modpack;

    /** ★ 1.2.3：onActivityResult 里选中的整合包路径。原来只是个局部变量，
     *  导致「安装」按钮点了之后拿不到文件。 */
    private String selectedPath;

    /** 安装进度对话框（安装期间不让关掉）与其中的任务列表 */
    private AlertDialog installDialog;
    private DownloadTaskListAdapter installTaskAdapter;

    public InstallPackageUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        installPackageUI = activity.findViewById(R.id.ui_install_package);

        selectLayout = activity.findViewById(R.id.select_package_layout);
        installLayout = activity.findViewById(R.id.install_package_layout);
        progressBar = activity.findViewById(R.id.loading_package_info_progress);

        installLocal = activity.findViewById(R.id.install_package_local);
        installOnline = activity.findViewById(R.id.install_package_online);
        installLocal.setOnClickListener(this);
        installOnline.setOnClickListener(this);

        pathText = activity.findViewById(R.id.package_path);
        nameText = activity.findViewById(R.id.package_name);
        versionText = activity.findViewById(R.id.package_version);
        authorText = activity.findViewById(R.id.package_author);
        editName = activity.findViewById(R.id.edit_package_name);

        showDescription = activity.findViewById(R.id.show_package_description);
        install = activity.findViewById(R.id.install_package);
        showDescription.setOnClickListener(this);
        install.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        activity.showBarTitle(context.getResources().getString(R.string.install_package_ui_title),false,true);
        CustomAnimationUtils.showViewFromLeft(installPackageUI,activity,context,true);
        selectLayout.setVisibility(View.VISIBLE);
        installLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(installPackageUI,activity,context,true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PACKAGE_REQUEST && resultCode != Activity.RESULT_OK) {
            activity.backToLastUI();
        }
        if (requestCode == SELECT_PACKAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = UriUtils.getRealPathFromUri_AboveApi19(context,uri);
            this.selectedPath = path;   // ★ 安装按钮要用，存成字段
            selectLayout.setVisibility(View.GONE);
            installLayout.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                try {
                    modpack = ModpackHelper.readModpackManifest(new File(path).toPath(), ZipTools.findSuitableEncoding(new File(path).toPath()));
                    activity.runOnUiThread(() -> {
                        selectLayout.setVisibility(View.GONE);
                        installLayout.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        pathText.setText(path);
                        nameText.setText((modpack.getName() == null || StringUtils.isBlank(modpack.getName())) ? FileUtils.getNameWithoutExtension(new File(path)) : modpack.getName());
                        versionText.setText(modpack.getVersion() == null ? "" : modpack.getVersion());
                        authorText.setText(modpack.getAuthor() == null ? "" : modpack.getAuthor());
                        editName.setText((modpack.getName() == null || StringUtils.isBlank(modpack.getName())) ? FileUtils.getNameWithoutExtension(new File(path)) : modpack.getName());
                    });
                } catch (ManuallyCreatedModpackException e) {
                    e.printStackTrace();
                    modpack = null;
                    activity.runOnUiThread(() -> {
                        selectLayout.setVisibility(View.GONE);
                        installLayout.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        pathText.setText(path);
                        nameText.setText(new File(path).getName());
                        versionText.setText("");
                        authorText.setText("");
                        editName.setText(FileUtils.getNameWithoutExtension(new File(path)));
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(context.getString(R.string.dialog_package_manual_warn_title));
                        builder.setMessage(context.getString(R.string.dialog_package_manual_warn_msg));
                        builder.setPositiveButton(context.getString(R.string.dialog_package_manual_warn_positive), null);
                        builder.setNegativeButton(context.getString(R.string.dialog_package_manual_warn_negative), (dialogInterface, i) -> {
                            activity.backToLastUI();
                        });
                        builder.create().show();
                    });
                } catch (UnsupportedModpackException | IOException e) {
                    e.printStackTrace();
                    modpack = null;
                    // ★ 1.5.0：**不再弹「不支持」并回退**。
                    //   未知格式也要尽力导入（直接解压），所以照常进入安装界面，
                    //   由 GenericModpackInstallTask 按 zip 内容挑目标目录解包。
                    activity.runOnUiThread(() -> {
                        selectLayout.setVisibility(View.GONE);
                        installLayout.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        pathText.setText(path);
                        nameText.setText(new File(path).getName());
                        versionText.setText("");
                        authorText.setText("");
                        editName.setText(FileUtils.getNameWithoutExtension(new File(path)));
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("未识别的整合包格式");
                        builder.setMessage("这个压缩包不是已知的整合包格式"
                                + "（Curse / Modrinth / MCBBS / HMCL / MultiMC）。\n\n"
                                + "启动器仍会按通用方式尽力导入：把包里的 .minecraft / overrides / "
                                + "minecraft 目录内容直接解压到游戏目录或版本目录。\n"
                                + "如果包里本来就带完整的游戏目录，这样也能用。");
                        builder.setPositiveButton("继续导入", null);
                        builder.setNegativeButton(android.R.string.cancel,
                                (dialogInterface, i) -> activity.backToLastUI());
                        builder.create().show();
                    });
                }
            }).start();
        }
    }

    @Override
    public void onClick(View view) {
        if (view == installLocal) {
            Intent intent = new Intent(context, FileChooser.class);
            intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "zip;mrpack");
            intent.putExtra(Constants.INITIAL_DIRECTORY, new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            activity.startActivityForResult(intent, SELECT_PACKAGE_REQUEST);
        }
        if (view == installOnline) {

        }

        if (view == showDescription) {
            if (modpack != null && modpack.getDescription() != null && StringUtils.isNotBlank(modpack.getDescription())) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(context.getString(R.string.dialog_package_description_title));
                CharSequence charSequence = Html.fromHtml(modpack.getDescription(), 0);
                builder.setMessage(charSequence);
                builder.setPositiveButton(context.getString(R.string.dialog_package_description_positive), null);
                builder.create().show();
            }
        }
        if (view == install) {
            startInstall();
        }
    }

    /**
     * ★ 1.5.0：按格式分发到对应的安装任务。**非 MultiMC 不再拒绝。**
     *
     * 原来修好 MultiMC 之后，这里只认 MultiMCInstanceConfiguration，
     * 其它格式一律弹「目前只支持 MultiMC / Prism」并 return —— 这就是用户说的
     * 「选择其他整合包格式就不让导入了」。现在：
     *   MultiMCInstanceConfiguration → MultiMCModpackInstallTask（原逻辑原封不动）
     *   CurseManifest                → CurseInstallTask
     *   ModrinthManifest             → ModrinthInstallTask
     *   McbbsModpackManifest         → McbbsModpackLocalInstallTask
     *   QclModpackManifest          → QclModpackInstallTask
     *   其它 / 没有 manifest          → GenericModpackInstallTask（按 zip 内容尽力解压导入）
     */
    /**
     * ★ 2026-10-06 新增：供「下载页」把**下载好的整合包 zip** 直接复用本页的安装流程。
     *
     * <p>为什么加这个方法：下载页点整合包版本 → 下载 zip（DownloadDialog）→ 下载完成后
     * 必须走与「本地导入」**完全相同**的安装逻辑（按 manifest 分发到
     * MultiMC / Curse / Modrinth / MCBBS / QCL / 兜底）。与其在下载页再抄一份，
     * 不如把 zip 交回这里，复用同一套分发代码与同一个任务列表 UI。
     *
     * <p>照 FCL 的做法：FCL 是 {@code ModpackInstaller.installModpack(...)} 统一收口；
     * QCL 没有那层封装，就用本页作为收口。
     */
    public void installDownloadedZip(File zip, String targetName) {
        if (zip == null || !zip.isFile()) {
            return;
        }
        this.selectedPath = zip.getAbsolutePath();
        this.modpack = null;
        try {
            this.modpack = ModpackHelper.readModpackManifest(zip.toPath(),
                    ZipTools.findSuitableEncoding(zip.toPath()));
        } catch (Throwable ignored) {
            // 解析失败也照样往下走：会落到 GenericModpackInstallTask 尽力导入，绝不拒绝
        }
        if (targetName != null && !StringUtils.isBlank(targetName)) {
            editName.setText(targetName);
        } else {
            editName.setText(FileUtils.getNameWithoutExtension(zip));
        }
        startInstall();
    }

    private void startInstall() {
        if (selectedPath == null) {
            activity.backToLastUI();
            return;
        }

        final String targetName = editName.getText().toString().trim();
        if (StringUtils.isBlank(targetName)) {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.install_package_ui_title))
                    .setMessage("整合包名字不能为空。")
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
            return;
        }

        // 安装期间显示**任务列表**（不是光秃秃一个百分比条）：
        // 复用 GameInstallDialog 那套 dialog_install_game + DownloadTaskListAdapter，
        // 一行一个步骤（解包 / 写入配置 / 合并 patches / 拷贝库 / 检查基础版本），
        // 玩家能看清在装什么、装到哪一步。★ 这是用户明确提的要求。
        View dialogView = View.inflate(context, R.layout.dialog_install_game, null);
        RecyclerView taskListView = dialogView.findViewById(R.id.download_task_list);
        taskListView.setLayoutManager(new LinearLayoutManager(context));
        installTaskAdapter = new DownloadTaskListAdapter(context);
        taskListView.setAdapter(installTaskAdapter);
        if (taskListView.getItemAnimator() != null) {
            taskListView.getItemAnimator().setAddDuration(0L);
            taskListView.getItemAnimator().setChangeDuration(0L);
            taskListView.getItemAnimator().setMoveDuration(0L);
            taskListView.getItemAnimator().setRemoveDuration(0L);
            if (taskListView.getItemAnimator() instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) taskListView.getItemAnimator()).setSupportsChangeAnimations(false);
            }
        }

        installDialog = new AlertDialog.Builder(context)
                .setTitle("正在安装整合包：" + targetName)
                .setView(dialogView)
                .setCancelable(false)
                .create();
        installDialog.show();

        // 任务要靠 Activity 拿游戏目录（提供器 new 的时候没有 context）
        MultiMCModpackInstallTask.setActivity(activity);
        BaseModpackInstallTask.setActivity(activity);

        final File zipFile = new File(selectedPath);
        final ModpackManifest manifest = modpack == null ? null : modpack.getManifest();

        // ---- MultiMC / Prism：原有任务原封不动 ----
        if (manifest instanceof MultiMCInstanceConfiguration) {
            MultiMCModpackInstallTask task = new MultiMCModpackInstallTask(
                    activity, zipFile, modpack, (MultiMCInstanceConfiguration) manifest,
                    targetName, installTaskAdapter);
            task.setListener(new MultiMCModpackInstallTask.ProgressListener() {
                @Override
                public void onProgress(int percent) {
                    // 有任务列表时进度在列表里逐行体现，这里不再另开进度条
                }

                @Override
                public void onFinished(Exception error) {
                    finishInstall(error, targetName);
                }
            });
            task.execute();
            return;
        }

        // ---- 其它格式：分发到各自的真实安装任务 ----
        BaseModpackInstallTask task;
        if (manifest instanceof CurseManifest) {
            task = new CurseInstallTask(activity, zipFile, modpack, (CurseManifest) manifest,
                    targetName, installTaskAdapter);
        } else if (manifest instanceof ModrinthManifest) {
            task = new ModrinthInstallTask(activity, zipFile, modpack, (ModrinthManifest) manifest,
                    targetName, installTaskAdapter);
        } else if (manifest instanceof McbbsModpackManifest) {
            task = new McbbsModpackLocalInstallTask(activity, zipFile, modpack,
                    (McbbsModpackManifest) manifest, targetName, installTaskAdapter);
        } else if (manifest instanceof QclModpackManifest) {
            task = new QclModpackInstallTask(activity, zipFile, modpack, targetName, installTaskAdapter);
        } else {
            // 未知 / 没有 manifest：也尽力导入，绝不拒绝
            task = new GenericModpackInstallTask(activity, zipFile, modpack, targetName, installTaskAdapter);
        }
        task.setListener(new BaseModpackInstallTask.ProgressListener() {
            @Override
            public void onProgress(int percent) {
            }

            @Override
            public void onFinished(Exception error) {
                finishInstall(error, targetName);
            }
        });
        task.execute();
    }

    /** 安装收尾（成功与失败都走这里，供各格式任务共用） */
    private void finishInstall(Exception error, String targetName) {
        if (installDialog != null && installDialog.isShowing()) {
            installDialog.dismiss();
        }
        installDialog = null;
        installTaskAdapter = null;

        if (error == null) {
            Toast.makeText(context, "整合包安装完成：" + targetName, Toast.LENGTH_LONG).show();
            // 新版本要出现在版本列表里
            activity.uiManager.versionListUI.refreshVersionList();
            activity.backToLastUI();
        } else {
            error.printStackTrace();
            new AlertDialog.Builder(context)
                    .setTitle("整合包安装失败")
                    .setMessage(String.valueOf(error.getMessage()))
                    .setPositiveButton(android.R.string.ok, null)
                    .create().show();
        }
    }
}
