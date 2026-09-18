/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.AlertDialog$Builder
 *  android.app.Dialog
 *  android.content.Context
 *  android.os.AsyncTask$Status
 *  android.os.Handler
 *  android.os.Handler$Callback
 *  android.os.Message
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.TextView
 *  androidx.annotation.NonNull
 *  androidx.recyclerview.widget.LinearLayoutManager
 *  androidx.recyclerview.widget.RecyclerView
 *  androidx.recyclerview.widget.RecyclerView$Adapter
 *  androidx.recyclerview.widget.RecyclerView$LayoutManager
 *  androidx.recyclerview.widget.SimpleItemAnimator
 *  com.google.gson.Gson
 */
package com.qcl.launcher.launcher.download;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.google.gson.Gson;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.download.InstallerAnalyzer;
import com.qcl.launcher.launcher.download.PatchMerger;
import com.qcl.launcher.launcher.download.forge.ForgeInstallTask;
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import com.qcl.launcher.launcher.download.optifine.OptifineInstallTask;
import com.qcl.launcher.launcher.download.optifine.OptifineVersion;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.NetSpeed;
import com.qcl.launcher.utils.io.NetSpeedTimer;
import com.qcl.launcher.utils.platform.Bits;
import java.util.Objects;

import com.qcl.launcher.R;
public class GameInstallLocalDialog
extends Dialog
implements View.OnClickListener,
Handler.Callback {
    private MainActivity activity;
    private String name;
    private String gameVersion;
    private String path;
    private RecyclerView taskListView;
    private DownloadTaskListAdapter downloadTaskListAdapter;
    private NetSpeedTimer netSpeedTimer;
    private TextView speedText;
    private Button cancelButton;
    private ForgeInstallTask forgeInstallTask;
    private OptifineInstallTask optifineInstallTask;
    private Version gameVersionJson;

    public GameInstallLocalDialog(@NonNull Context context, MainActivity activity, String name, String gameVersion, String path) {
        super(context);
        this.activity = activity;
        this.name = name;
        this.gameVersion = gameVersion;
        this.path = path;
        this.setContentView(R.layout.dialog_install_local);
        this.setCancelable(false);
        this.init();
    }

    private void init() {
        String s = FileStringUtils.getStringFromFile(this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.name + "/" + this.name + ".json");
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
        this.gameVersionJson = (Version)gson.fromJson(s, Version.class);
        this.taskListView = (RecyclerView)this.findViewById(R.id.download_task_list);
        this.taskListView.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager(this.getContext()));
        this.downloadTaskListAdapter = new DownloadTaskListAdapter(this.getContext());
        this.taskListView.setAdapter((RecyclerView.Adapter)this.downloadTaskListAdapter);
        Objects.requireNonNull(this.taskListView.getItemAnimator()).setAddDuration(0L);
        this.taskListView.getItemAnimator().setChangeDuration(0L);
        this.taskListView.getItemAnimator().setMoveDuration(0L);
        this.taskListView.getItemAnimator().setRemoveDuration(0L);
        ((SimpleItemAnimator)this.taskListView.getItemAnimator()).setSupportsChangeAnimations(false);
        this.speedText = (TextView)this.findViewById(R.id.download_speed_text);
        this.cancelButton = (Button)this.findViewById(R.id.cancel_install_local);
        this.cancelButton.setOnClickListener((View.OnClickListener)this);
        Handler handler = new Handler((Handler.Callback)this);
        this.netSpeedTimer = new NetSpeedTimer(this.getContext(), new NetSpeed(), handler).setDelayTime(0L).setPeriodTime(1000L);
        this.netSpeedTimer.startSpeedTimer();
        this.check();
    }

    private void check() {
        final DownloadTaskListBean bean = new DownloadTaskListBean(this.getContext().getString(R.string.dialog_install_local_recognize), "", "", "");
        InstallerAnalyzer.checkType(this.path, new InstallerAnalyzer.CheckInstallerTypeCallback(){

            @Override
            public void onStart() {
                GameInstallLocalDialog.this.downloadTaskListAdapter.addDownloadTask(bean);
            }

            @Override
            public void onFinish(InstallerAnalyzer.Type type, Object installer) {
                if (GameInstallLocalDialog.this.isShowing()) {
                    boolean fabric = false;
                    boolean quilt = false;
                    for (Version v : GameInstallLocalDialog.this.gameVersionJson.getPatches()) {
                        if (v.getId().equals("fabric")) {
                            fabric = true;
                            break;
                        }
                        if (!v.getId().equals("quilt")) continue;
                        quilt = true;
                        break;
                    }
                    GameInstallLocalDialog.this.downloadTaskListAdapter.onComplete(bean);
                    if (type == InstallerAnalyzer.Type.FORGE) {
                        if (((ForgeVersion)installer).getGameVersion().equals(GameInstallLocalDialog.this.gameVersion)) {
                            if (fabric) {
                                GameInstallLocalDialog.this.fabricFailed();
                            } else if (quilt) {
                                GameInstallLocalDialog.this.quiltFailed();
                            } else {
                                GameInstallLocalDialog.this.installForge((ForgeVersion)installer);
                            }
                        } else {
                            GameInstallLocalDialog.this.incorrectVersion(GameInstallLocalDialog.this.gameVersion, ((ForgeVersion)installer).getGameVersion());
                        }
                    } else if (type == InstallerAnalyzer.Type.OPTIFINE) {
                        if (((OptifineVersion)installer).mcVersion.equals(GameInstallLocalDialog.this.gameVersion)) {
                            if (fabric) {
                                GameInstallLocalDialog.this.fabricFailed();
                            } else if (quilt) {
                                GameInstallLocalDialog.this.quiltFailed();
                            } else {
                                GameInstallLocalDialog.this.installOptifine((OptifineVersion)installer);
                            }
                        } else {
                            GameInstallLocalDialog.this.incorrectVersion(GameInstallLocalDialog.this.gameVersion, ((OptifineVersion)installer).mcVersion);
                        }
                    } else {
                        GameInstallLocalDialog.this.unrecognizedInstaller();
                    }
                }
            }
        });
    }

    private void unrecognizedInstaller() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_incorrect_installer_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_incorrect_installer_msg));
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_incorrect_installer_positive), (dialogInterface, i) -> {});
        this.dismiss();
        builder.create().show();
    }

    private void incorrectVersion(String expectVersion, String currentVersion) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_incorrect_version_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_incorrect_version_msg).replace("%s1", currentVersion).replace("%s2", expectVersion));
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_incorrect_version_positive), (dialogInterface, i) -> {});
        this.dismiss();
        builder.create().show();
    }

    private void fabricFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_incorrect_fabric_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_incorrect_fabric_msg));
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_incorrect_fabric_positive), (dialogInterface, i) -> {});
        this.dismiss();
        builder.create().show();
    }

    private void quiltFailed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_incorrect_quilt_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_incorrect_quilt_msg));
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_incorrect_quilt_positive), (dialogInterface, i) -> {});
        this.dismiss();
        builder.create().show();
    }

    private void installForge(ForgeVersion forgeVersion) {
        this.forgeInstallTask = new ForgeInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new ForgeInstallTask.InstallForgeCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallLocalDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameInstallLocalDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameInstallLocalDialog.this.getContext(), GameInstallLocalDialog.this.gameVersionJson, version, "forge", () -> GameInstallLocalDialog.this.dismiss());
                GameInstallLocalDialog.this.saveVersion();
            }
        });
        this.forgeInstallTask.execute(new ForgeVersion[]{forgeVersion});
    }

    private void installOptifine(OptifineVersion optifineVersion) {
        this.optifineInstallTask = new OptifineInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new OptifineInstallTask.InstallOptifineCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallLocalDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameInstallLocalDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameInstallLocalDialog.this.getContext(), GameInstallLocalDialog.this.gameVersionJson, version, "optifine", () -> GameInstallLocalDialog.this.dismiss());
                GameInstallLocalDialog.this.saveVersion();
            }
        });
        this.optifineInstallTask.execute(new OptifineVersion[]{optifineVersion});
    }

    public void saveVersion() {
        String versionPath = this.activity.launcherSetting.gameFileDirectory + "/versions/" + this.name + "/" + this.name + ".json";
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
        String s = gson.toJson((Object)this.gameVersionJson);
        FileStringUtils.writeFile(versionPath, s);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_install_success_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_install_success_text));
        builder.setCancelable(false);
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> {
            this.activity.uiManager.gameManagerUI.gameManagerUIManager.autoInstallUI.refresh(this.name);
            new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start();
        });
        this.exit();
        builder.create().show();
    }

    public void throwException(Exception e) {
        this.activity.runOnUiThread(() -> {
            this.exit();
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
            builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_install_fail_title));
            builder.setMessage((CharSequence)e.toString());
            builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_install_fail_positive), (dialogInterface, i) -> {});
            builder.create().show();
        });
    }

    private void exit() {
        if (this.forgeInstallTask != null && this.forgeInstallTask.getStatus() != null && this.forgeInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.forgeInstallTask.cancel(true);
        }
        if (this.optifineInstallTask != null && this.optifineInstallTask.getStatus() != null && this.optifineInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.optifineInstallTask.cancel(true);
        }
        if (this.forgeInstallTask != null) {
            this.forgeInstallTask.cancelBuild();
        }
        if (this.optifineInstallTask != null) {
            this.optifineInstallTask.cancelBuild();
        }
        this.netSpeedTimer.stopSpeedTimer();
        this.dismiss();
    }

    public void onClick(View view) {
        if (view == this.cancelButton) {
            this.exit();
        }
    }

    public boolean handleMessage(@NonNull Message message) {
        if (message.what == 101010) {
            String speed = (String)message.obj;
            this.speedText.setText((CharSequence)speed);
        }
        return false;
    }
}

