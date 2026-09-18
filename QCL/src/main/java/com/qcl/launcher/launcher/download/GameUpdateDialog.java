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
import com.qcl.launcher.launcher.download.PatchMerger;
import com.qcl.launcher.launcher.download.fabric.FabricAPIInstallTask;
import com.qcl.launcher.launcher.download.fabric.FabricInstallTask;
import com.qcl.launcher.launcher.download.fabric.FabricLoaderVersion;
import com.qcl.launcher.launcher.download.forge.ForgeDownloadTask;
import com.qcl.launcher.launcher.download.forge.ForgeInstallTask;
import com.qcl.launcher.launcher.download.forge.ForgeVersion;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderInstallTask;
import com.qcl.launcher.launcher.download.liteloader.LiteLoaderVersion;
import com.qcl.launcher.launcher.download.optifine.OptifineDownloadTask;
import com.qcl.launcher.launcher.download.optifine.OptifineInstallTask;
import com.qcl.launcher.launcher.download.optifine.OptifineVersion;
import com.qcl.launcher.launcher.download.quilt.QuiltAPIInstallTask;
import com.qcl.launcher.launcher.download.quilt.QuiltInstallTask;
import com.qcl.launcher.launcher.download.quilt.QuiltLoaderVersion;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.NetSpeed;
import com.qcl.launcher.utils.io.NetSpeedTimer;
import com.qcl.launcher.utils.platform.Bits;
import java.util.Objects;

import com.qcl.launcher.R;
public class GameUpdateDialog
extends Dialog
implements View.OnClickListener,
Handler.Callback {
    private Context context;
    private MainActivity activity;
    private String name;
    private int apiType;
    private String version;
    private ForgeVersion forgeVersion;
    private OptifineVersion optifineVersion;
    private LiteLoaderVersion liteLoaderVersion;
    private FabricLoaderVersion fabricVersion;
    private RemoteMod.Version fabricAPIVersion;
    private QuiltLoaderVersion quiltVersion;
    private RemoteMod.Version quiltAPIVersion;
    private LiteLoaderInstallTask liteLoaderInstallTask;
    private ForgeDownloadTask forgeDownloadTask;
    private ForgeInstallTask forgeInstallTask;
    private OptifineDownloadTask optifineDownloadTask;
    private OptifineInstallTask optifineInstallTask;
    private FabricInstallTask fabricInstallTask;
    private FabricAPIInstallTask fabricAPIInstallTask;
    private QuiltInstallTask quiltInstallTask;
    private QuiltAPIInstallTask quiltAPIInstallTask;
    private RecyclerView taskListView;
    private DownloadTaskListAdapter downloadTaskListAdapter;
    private TextView stateText;
    private NetSpeedTimer netSpeedTimer;
    private TextView speedText;
    private Button cancelButton;
    private Version gameVersionJson;

    public GameUpdateDialog(@NonNull Context context, MainActivity activity, String name, String gameVersion, int apiType, Object apiVersion) {
        super(context);
        this.context = context;
        this.activity = activity;
        this.name = name;
        this.version = gameVersion;
        this.apiType = apiType;
        switch (apiType) {
            case 0: {
                this.forgeVersion = (ForgeVersion)apiVersion;
                break;
            }
            case 1: {
                this.liteLoaderVersion = (LiteLoaderVersion)apiVersion;
                break;
            }
            case 2: {
                this.optifineVersion = (OptifineVersion)apiVersion;
                break;
            }
            case 3: {
                this.fabricVersion = (FabricLoaderVersion)apiVersion;
                break;
            }
            case 4: {
                this.fabricAPIVersion = (RemoteMod.Version)apiVersion;
                break;
            }
            case 5: {
                this.quiltVersion = (QuiltLoaderVersion)apiVersion;
                break;
            }
            case 6: {
                this.quiltAPIVersion = (RemoteMod.Version)apiVersion;
            }
        }
        this.setContentView(R.layout.dialog_install_update);
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
        this.stateText = (TextView)this.findViewById(R.id.state_text);
        this.speedText = (TextView)this.findViewById(R.id.download_speed_text);
        this.cancelButton = (Button)this.findViewById(R.id.cancel_install_update);
        this.cancelButton.setOnClickListener((View.OnClickListener)this);
        Handler handler = new Handler((Handler.Callback)this);
        this.netSpeedTimer = new NetSpeedTimer(this.getContext(), new NetSpeed(), handler).setDelayTime(0L).setPeriodTime(1000L);
        this.netSpeedTimer.startSpeedTimer();
        switch (this.apiType) {
            case 0: {
                this.downloadForge();
                break;
            }
            case 1: {
                this.downloadLiteLoader();
                break;
            }
            case 2: {
                this.downloadOptifine();
                break;
            }
            case 3: {
                this.downloadFabric();
                break;
            }
            case 4: {
                this.downloadFabricAPI();
                break;
            }
            case 5: {
                this.downloadQuilt();
                break;
            }
            case 6: {
                this.downloadQuiltAPI();
            }
        }
    }

    public void downloadLiteLoader() {
        this.liteLoaderInstallTask = new LiteLoaderInstallTask(this.activity, this.downloadTaskListAdapter, new LiteLoaderInstallTask.InstallLiteLoaderCallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "LiteLoader").replace("%v", GameUpdateDialog.this.liteLoaderVersion.getVersion()));
            }

            @Override
            public void onFailed(Exception e) {
                GameUpdateDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameUpdateDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameUpdateDialog.this.context, GameUpdateDialog.this.gameVersionJson, version, "liteloader", () -> GameUpdateDialog.this.exit());
                GameUpdateDialog.this.saveVersion();
            }
        });
        this.liteLoaderInstallTask.execute(new LiteLoaderVersion[]{this.liteLoaderVersion});
    }

    public void downloadForge() {
        this.forgeDownloadTask = new ForgeDownloadTask(this.activity, this.downloadTaskListAdapter, new ForgeDownloadTask.DownloadForgeCallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "Forge").replace("%v", GameUpdateDialog.this.forgeVersion.getVersion()));
            }

            @Override
            public void onFinish(Exception e) {
                if (e == null) {
                    GameUpdateDialog.this.installForge();
                } else {
                    GameUpdateDialog.this.throwException(e);
                }
            }
        });
        this.forgeDownloadTask.execute(new ForgeVersion[]{this.forgeVersion});
    }

    public void installForge() {
        this.forgeInstallTask = new ForgeInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new ForgeInstallTask.InstallForgeCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameUpdateDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameUpdateDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameUpdateDialog.this.context, GameUpdateDialog.this.gameVersionJson, version, "forge", () -> GameUpdateDialog.this.exit());
                GameUpdateDialog.this.saveVersion();
            }
        });
        this.forgeInstallTask.execute(new ForgeVersion[]{this.forgeVersion});
    }

    public void downloadOptifine() {
        this.optifineDownloadTask = new OptifineDownloadTask(this.activity, this.downloadTaskListAdapter, new OptifineDownloadTask.DownloadOptifineCallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "OptiFine").replace("%v", ((GameUpdateDialog)GameUpdateDialog.this).optifineVersion.type + "_" + ((GameUpdateDialog)GameUpdateDialog.this).optifineVersion.patch));
            }

            @Override
            public void onFinish(Exception e) {
                if (e == null) {
                    GameUpdateDialog.this.installOptifine();
                } else {
                    GameUpdateDialog.this.throwException(e);
                }
            }
        });
        this.optifineDownloadTask.execute(new OptifineVersion[]{this.optifineVersion});
    }

    public void installOptifine() {
        this.optifineInstallTask = new OptifineInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new OptifineInstallTask.InstallOptifineCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameUpdateDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameUpdateDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameUpdateDialog.this.getContext(), GameUpdateDialog.this.gameVersionJson, version, "optifine", () -> GameUpdateDialog.this.dismiss());
                GameUpdateDialog.this.saveVersion();
            }
        });
        this.optifineInstallTask.execute(new OptifineVersion[]{this.optifineVersion});
    }

    public void downloadFabric() {
        this.fabricInstallTask = new FabricInstallTask(this.activity, this.downloadTaskListAdapter, this.version, new FabricInstallTask.InstallFabricCallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "Fabric").replace("%v", ((GameUpdateDialog)GameUpdateDialog.this).fabricVersion.version));
            }

            @Override
            public void onFailed(Exception e) {
                GameUpdateDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameUpdateDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameUpdateDialog.this.getContext(), GameUpdateDialog.this.gameVersionJson, version, "fabric", () -> GameUpdateDialog.this.dismiss());
                GameUpdateDialog.this.saveVersion();
            }
        });
        this.fabricInstallTask.execute(new FabricLoaderVersion[]{this.fabricVersion});
    }

    public void downloadFabricAPI() {
        this.fabricAPIInstallTask = new FabricAPIInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new FabricAPIInstallTask.InstallFabricAPICallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "Fabric API").replace("%v", GameUpdateDialog.this.fabricAPIVersion.getVersion()));
            }

            @Override
            public void onFinish(Exception e) {
                if (e == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GameUpdateDialog.this.getContext());
                    builder.setTitle((CharSequence)GameUpdateDialog.this.getContext().getString(R.string.dialog_install_success_title));
                    builder.setMessage((CharSequence)GameUpdateDialog.this.getContext().getString(R.string.dialog_install_success_text));
                    builder.setCancelable(false);
                    builder.setPositiveButton((CharSequence)GameUpdateDialog.this.getContext().getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> GameUpdateDialog.this.activity.backToLastUI());
                    GameUpdateDialog.this.exit();
                    builder.create().show();
                } else {
                    GameUpdateDialog.this.throwException(e);
                }
            }
        });
        this.fabricAPIInstallTask.execute(new RemoteMod.Version[]{this.fabricAPIVersion});
    }

    public void downloadQuilt() {
        this.quiltInstallTask = new QuiltInstallTask(this.activity, this.downloadTaskListAdapter, this.version, new QuiltInstallTask.InstallQuiltCallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "Quilt").replace("%v", ((GameUpdateDialog)GameUpdateDialog.this).quiltVersion.version));
            }

            @Override
            public void onFailed(Exception e) {
                GameUpdateDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameUpdateDialog.this.gameVersionJson = PatchMerger.reMergePatch(GameUpdateDialog.this.getContext(), GameUpdateDialog.this.gameVersionJson, version, "quilt", () -> GameUpdateDialog.this.dismiss());
                GameUpdateDialog.this.saveVersion();
            }
        });
        this.quiltInstallTask.execute(new QuiltLoaderVersion[]{this.quiltVersion});
    }

    public void downloadQuiltAPI() {
        this.quiltAPIInstallTask = new QuiltAPIInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new QuiltAPIInstallTask.InstallQuiltAPICallback(){

            @Override
            public void onStart() {
                GameUpdateDialog.this.stateText.setText((CharSequence)GameUpdateDialog.this.context.getString(R.string.dialog_install_update_state).replace("%s", "Quilt API").replace("%v", GameUpdateDialog.this.quiltAPIVersion.getVersion()));
            }

            @Override
            public void onFinish(Exception e) {
                if (e == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GameUpdateDialog.this.getContext());
                    builder.setTitle((CharSequence)GameUpdateDialog.this.getContext().getString(R.string.dialog_install_success_title));
                    builder.setMessage((CharSequence)GameUpdateDialog.this.getContext().getString(R.string.dialog_install_success_text));
                    builder.setCancelable(false);
                    builder.setPositiveButton((CharSequence)GameUpdateDialog.this.getContext().getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> GameUpdateDialog.this.activity.backToLastUI());
                    GameUpdateDialog.this.exit();
                    builder.create().show();
                } else {
                    GameUpdateDialog.this.throwException(e);
                }
            }
        });
        this.quiltAPIInstallTask.execute(new RemoteMod.Version[]{this.quiltAPIVersion});
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
            this.activity.backToLastUI();
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
        if (this.liteLoaderInstallTask != null && this.liteLoaderInstallTask.getStatus() != null && this.liteLoaderInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.liteLoaderInstallTask.cancel(true);
        }
        if (this.forgeDownloadTask != null && this.forgeDownloadTask.getStatus() != null && this.forgeDownloadTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.forgeDownloadTask.cancel(true);
        }
        if (this.forgeInstallTask != null && this.forgeInstallTask.getStatus() != null && this.forgeInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.forgeInstallTask.cancel(true);
        }
        if (this.optifineDownloadTask != null && this.optifineDownloadTask.getStatus() != null && this.optifineDownloadTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.optifineDownloadTask.cancel(true);
        }
        if (this.optifineInstallTask != null && this.optifineInstallTask.getStatus() != null && this.optifineInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.optifineInstallTask.cancel(true);
        }
        if (this.fabricInstallTask != null && this.fabricInstallTask.getStatus() != null && this.fabricInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.fabricInstallTask.cancel(true);
        }
        if (this.fabricAPIInstallTask != null && this.fabricAPIInstallTask.getStatus() != null && this.fabricAPIInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.fabricAPIInstallTask.cancel(true);
        }
        if (this.quiltInstallTask != null && this.quiltInstallTask.getStatus() != null && this.quiltInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.quiltInstallTask.cancel(true);
        }
        if (this.quiltAPIInstallTask != null && this.quiltAPIInstallTask.getStatus() != null && this.quiltAPIInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.quiltAPIInstallTask.cancel(true);
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
            this.activity.backToLastUI();
            new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start();
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

