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
import com.qcl.launcher.launcher.mod.ManuallyCreatedModpackException;
import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackHelper;
import com.qcl.launcher.launcher.mod.UnsupportedModpackException;
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
                    activity.runOnUiThread(() -> {
                        activity.backToLastUI();
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(context.getString(R.string.dialog_package_not_support_title));
                        builder.setMessage(context.getString(R.string.dialog_package_not_support_msg));
                        builder.setPositiveButton(context.getString(R.string.dialog_package_not_support_exit), null);
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
     * ★ 1.2.3：这里原来是空的 —— 按钮接好了、清单也读出来了，
     * 但点「安装」什么都不发生。这就是「导入整合包没反应 / 导入完启动崩」的入口。
     *
     * 现在接上 MultiMC / Prism 的真实安装任务
     * （其它提供器 Curse / Modrinth / MCBBS / HMCL 的 InstallTask 目前还是空壳，先明确提示）
     */
    private void startInstall() {
        if (modpack == null || selectedPath == null) {
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

        if (!(modpack.getManifest() instanceof MultiMCInstanceConfiguration)) {
            new AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.install_package_ui_title))
                    .setMessage("目前只支持 **MultiMC / Prism Launcher** 的整合包导入。\n\n"
                            + "CurseForge / Modrinth / MCBBS / HMCL 这几种的安装逻辑在 QCL 里还没接通，"
                            + "先用手动解压的方式吧。")
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

        // 任务要靠 Activity 拿游戏目录（MultiMCModpackProvider 那边 new 的时候没有 context）
        MultiMCModpackInstallTask.setActivity(activity);

        MultiMCModpackInstallTask task = new MultiMCModpackInstallTask(
                activity,
                new File(selectedPath),
                modpack,
                (MultiMCInstanceConfiguration) modpack.getManifest(),
                targetName,
                installTaskAdapter);   // ★ 任务通过它一行一行上报进度
        task.setListener(new MultiMCModpackInstallTask.ProgressListener() {
            @Override
            public void onProgress(int percent) {
                // 有任务列表时进度在列表里逐行体现，这里不再另开进度条
            }

            @Override
            public void onFinished(Exception error) {
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
        });
        task.execute();
    }
}
