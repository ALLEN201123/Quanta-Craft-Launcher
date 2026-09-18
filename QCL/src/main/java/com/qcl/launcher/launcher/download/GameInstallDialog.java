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
import com.qcl.launcher.launcher.download.game.LegacyArchiveInstallTask;
import com.qcl.launcher.launcher.download.game.MinecraftInstallTask;
import com.qcl.launcher.launcher.download.game.VersionManifest;
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
import com.qcl.launcher.launcher.uis.game.download.DownloadUrlSource;
import com.qcl.launcher.utils.file.AssetsUtils;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.io.NetSpeed;
import com.qcl.launcher.utils.io.NetSpeedTimer;
import com.qcl.launcher.utils.platform.Bits;
import java.io.File;
import java.util.Objects;

import com.qcl.launcher.R;
public class GameInstallDialog
extends Dialog
implements View.OnClickListener,
Handler.Callback {
    private final Context context;
    private final MainActivity activity;
    private final String name;
    private final VersionManifest.Version version;
    private final ForgeVersion forgeVersion;
    private final OptifineVersion optifineVersion;
    private final LiteLoaderVersion liteLoaderVersion;
    private final FabricLoaderVersion fabricVersion;
    private final RemoteMod.Version fabricAPIVersion;
    private final QuiltLoaderVersion quiltVersion;
    private final RemoteMod.Version quiltAPIVersion;
    private LegacyArchiveInstallTask legacyArchiveInstallTask;
    private MinecraftInstallTask minecraftInstallTask;
    private LiteLoaderInstallTask liteLoaderInstallTask;
    private ForgeDownloadTask forgeDownloadTask;
    private ForgeInstallTask forgeInstallTask;
    private OptifineDownloadTask optifineDownloadTask;
    private OptifineInstallTask optifineInstallTask;
    private FabricInstallTask fabricInstallTask;
    private FabricAPIInstallTask fabricAPIInstallTask;
    private QuiltInstallTask quiltInstallTask;
    private QuiltAPIInstallTask quiltAPIInstallTask;
    private Version gameVersionJson;
    private RecyclerView taskListView;
    private DownloadTaskListAdapter downloadTaskListAdapter;
    private NetSpeedTimer netSpeedTimer;
    private TextView speedText;
    private Button cancelButton;

    public GameInstallDialog(@NonNull Context context, MainActivity activity, String name, VersionManifest.Version version, ForgeVersion forgeVersion, OptifineVersion optifineVersion, LiteLoaderVersion liteLoaderVersion, FabricLoaderVersion fabricVersion, RemoteMod.Version fabricAPIVersion, QuiltLoaderVersion quiltVersion, RemoteMod.Version quiltAPIVersion) {
        super(context);
        this.context = context;
        this.activity = activity;
        this.name = name;
        this.version = version;
        this.forgeVersion = forgeVersion;
        this.optifineVersion = optifineVersion;
        this.liteLoaderVersion = liteLoaderVersion;
        this.fabricVersion = fabricVersion;
        this.fabricAPIVersion = fabricAPIVersion;
        this.quiltVersion = quiltVersion;
        this.quiltAPIVersion = quiltAPIVersion;
        this.setContentView(R.layout.dialog_install_game);
        this.setCancelable(false);
        this.init();
    }

    public void onClick(View v) {
        if (v == this.cancelButton) {
            this.exit();
            this.activity.backToLastUI();
            new Thread(() -> this.activity.uiManager.versionListUI.refreshVersionList()).start();
        }
    }

    private void init() {
        this.taskListView = (RecyclerView)this.findViewById(R.id.download_task_list);
        this.taskListView.setLayoutManager((RecyclerView.LayoutManager)new LinearLayoutManager(this.context));
        this.downloadTaskListAdapter = new DownloadTaskListAdapter(this.context);
        this.taskListView.setAdapter((RecyclerView.Adapter)this.downloadTaskListAdapter);
        Objects.requireNonNull(this.taskListView.getItemAnimator()).setAddDuration(0L);
        this.taskListView.getItemAnimator().setChangeDuration(0L);
        this.taskListView.getItemAnimator().setMoveDuration(0L);
        this.taskListView.getItemAnimator().setRemoveDuration(0L);
        ((SimpleItemAnimator)this.taskListView.getItemAnimator()).setSupportsChangeAnimations(false);
        this.speedText = (TextView)this.findViewById(R.id.download_speed_text);
        this.cancelButton = (Button)this.findViewById(R.id.cancel_install_game);
        this.cancelButton.setOnClickListener((View.OnClickListener)this);
        Handler handler = new Handler((Handler.Callback)this);
        this.netSpeedTimer = new NetSpeedTimer(this.context, new NetSpeed(), handler).setDelayTime(0L).setPeriodTime(1000L);
        this.netSpeedTimer.startSpeedTimer();
        this.startDownloadTasks();
    }

    private void startDownloadTasks() {
        System.out.println("---------------------------------------------------------------source:" + DownloadUrlSource.getSource(this.activity.launcherSetting.downloadUrlSource));
        if (!new File(this.activity.launcherSetting.gameFileDirectory + "/launcher_profiles.json").exists()) {
            AssetsUtils.getInstance(this.activity.getApplicationContext()).copyAssetsToSD("launcher_profiles.json", this.activity.launcherSetting.gameFileDirectory + "/launcher_profiles.json");
        }
        this.downloadMinecraft();
    }

    public void downloadMinecraft() {
        if ("archive".equals(this.version.type)) {
            this.downloadArchivedBuild();
            return;
        }
        this.minecraftInstallTask = new MinecraftInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new MinecraftInstallTask.InstallMinecraftCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameInstallDialog.this.gameVersionJson = version;
                GameInstallDialog.this.downloadLiteLoader();
            }
        });
        this.minecraftInstallTask.execute(new VersionManifest.Version[]{this.version});
    }

    public void downloadArchivedBuild() {
        this.legacyArchiveInstallTask = new LegacyArchiveInstallTask(this.activity, this.downloadTaskListAdapter, new LegacyArchiveInstallTask.Callback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallDialog.this.throwException(e);
            }

            @Override
            public void onFinish(String versionId) {
                if (GameInstallDialog.this.liteLoaderVersion != null) {
                    GameInstallDialog.this.downloadLiteLoader();
                    return;
                }
                GameInstallDialog.this.activity.runOnUiThread(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GameInstallDialog.this.context);
                    builder.setTitle((CharSequence)GameInstallDialog.this.context.getString(R.string.dialog_install_success_title));
                    builder.setMessage((CharSequence)GameInstallDialog.this.context.getString(R.string.dialog_install_success_text));
                    builder.setCancelable(false);
                    builder.setPositiveButton((CharSequence)GameInstallDialog.this.context.getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> {
                        GameInstallDialog.this.activity.backToLastUI();
                        new Thread(() -> ((GameInstallDialog)GameInstallDialog.this).activity.uiManager.versionListUI.refreshVersionList()).start();
                    });
                    GameInstallDialog.this.exit();
                    builder.create().show();
                });
            }
        });
        this.legacyArchiveInstallTask.execute(new VersionManifest.Version[]{this.version});
    }

    public void downloadLiteLoader() {
        if (this.liteLoaderVersion != null) {
            this.liteLoaderInstallTask = new LiteLoaderInstallTask(this.activity, this.downloadTaskListAdapter, new LiteLoaderInstallTask.InstallLiteLoaderCallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFailed(Exception e) {
                    GameInstallDialog.this.throwException(e);
                }

                @Override
                public void onFinish(Version version) {
                    GameInstallDialog.this.gameVersionJson = PatchMerger.mergePatch(GameInstallDialog.this.gameVersionJson, version);
                    GameInstallDialog.this.downloadForge();
                }
            });
            this.liteLoaderInstallTask.execute(new LiteLoaderVersion[]{this.liteLoaderVersion});
        } else {
            this.downloadForge();
        }
    }

    public void downloadForge() {
        if (this.forgeVersion != null) {
            this.forgeDownloadTask = new ForgeDownloadTask(this.activity, this.downloadTaskListAdapter, new ForgeDownloadTask.DownloadForgeCallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish(Exception e) {
                    if (e == null) {
                        GameInstallDialog.this.installForge();
                    } else {
                        GameInstallDialog.this.throwException(e);
                    }
                }
            });
            this.forgeDownloadTask.execute(new ForgeVersion[]{this.forgeVersion});
        } else {
            this.downloadOptifine();
        }
    }

    public void installForge() {
        this.forgeInstallTask = new ForgeInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new ForgeInstallTask.InstallForgeCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameInstallDialog.this.gameVersionJson = PatchMerger.mergePatch(GameInstallDialog.this.gameVersionJson, version);
                GameInstallDialog.this.downloadOptifine();
            }
        });
        this.forgeInstallTask.execute(new ForgeVersion[]{this.forgeVersion});
    }

    public void downloadOptifine() {
        if (this.optifineVersion != null) {
            this.optifineDownloadTask = new OptifineDownloadTask(this.activity, this.downloadTaskListAdapter, new OptifineDownloadTask.DownloadOptifineCallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish(Exception e) {
                    if (e == null) {
                        GameInstallDialog.this.installOptifine();
                    } else {
                        GameInstallDialog.this.throwException(e);
                    }
                }
            });
            this.optifineDownloadTask.execute(new OptifineVersion[]{this.optifineVersion});
        } else {
            this.downloadFabric();
        }
    }

    public void installOptifine() {
        this.optifineInstallTask = new OptifineInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new OptifineInstallTask.InstallOptifineCallback(){

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameInstallDialog.this.gameVersionJson = PatchMerger.mergeOptifinePatch(GameInstallDialog.this.gameVersionJson, version);
                GameInstallDialog.this.downloadFabric();
            }
        });
        this.optifineInstallTask.execute(new OptifineVersion[]{this.optifineVersion});
    }

    public void downloadFabric() {
        if (this.fabricVersion != null) {
            this.fabricInstallTask = new FabricInstallTask(this.activity, this.downloadTaskListAdapter, this.version.id, new FabricInstallTask.InstallFabricCallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFailed(Exception e) {
                    GameInstallDialog.this.throwException(e);
                }

                @Override
                public void onFinish(Version version) {
                    GameInstallDialog.this.gameVersionJson = PatchMerger.mergePatch(GameInstallDialog.this.gameVersionJson, version);
                    GameInstallDialog.this.downloadFabricAPI();
                }
            });
            this.fabricInstallTask.execute(new FabricLoaderVersion[]{this.fabricVersion});
        } else {
            this.downloadFabricAPI();
        }
    }

    public void downloadFabricAPI() {
        if (this.fabricAPIVersion != null) {
            this.fabricAPIInstallTask = new FabricAPIInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new FabricAPIInstallTask.InstallFabricAPICallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish(Exception e) {
                    if (e == null) {
                        GameInstallDialog.this.downloadQuilt();
                    } else {
                        GameInstallDialog.this.throwException(e);
                    }
                }
            });
            this.fabricAPIInstallTask.execute(new RemoteMod.Version[]{this.fabricAPIVersion});
        } else {
            this.downloadQuilt();
        }
    }

    public void downloadQuilt() {
        if (this.quiltVersion != null) {
            this.quiltInstallTask = new QuiltInstallTask(this.activity, this.downloadTaskListAdapter, this.version.id, new QuiltInstallTask.InstallQuiltCallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFailed(Exception e) {
                    GameInstallDialog.this.throwException(e);
                }

                @Override
                public void onFinish(Version version) {
                    GameInstallDialog.this.gameVersionJson = PatchMerger.mergePatch(GameInstallDialog.this.gameVersionJson, version);
                    GameInstallDialog.this.downloadQuiltAPI();
                }
            });
            this.quiltInstallTask.execute(new QuiltLoaderVersion[]{this.quiltVersion});
        } else {
            this.downloadQuiltAPI();
        }
    }

    public void downloadQuiltAPI() {
        if (this.quiltAPIVersion != null) {
            this.quiltAPIInstallTask = new QuiltAPIInstallTask(this.activity, this.name, this.downloadTaskListAdapter, new QuiltAPIInstallTask.InstallQuiltAPICallback(){

                @Override
                public void onStart() {
                }

                @Override
                public void onFinish(Exception e) {
                    if (e == null) {
                        GameInstallDialog.this.installJson();
                    } else {
                        GameInstallDialog.this.throwException(e);
                    }
                }
            });
            this.quiltAPIInstallTask.execute(new RemoteMod.Version[]{this.quiltAPIVersion});
        } else {
            this.installJson();
        }
    }

    public void installJson() {
        String gameFilePath = this.activity.launcherSetting.gameFileDirectory;
        Gson gson = JsonUtils.defaultGsonBuilder().registerTypeAdapter(Artifact.class, (Object)new Artifact.Serializer()).registerTypeAdapter(Bits.class, (Object)new Bits.Serializer()).registerTypeAdapter(RuledArgument.class, (Object)new RuledArgument.Serializer()).registerTypeAdapter(Argument.class, (Object)new Argument.Deserializer()).create();
        String string2 = gson.toJson((Object)this.gameVersionJson);
        FileStringUtils.writeFile(gameFilePath + "/versions/" + this.name + "/" + this.name + ".json", string2);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle((CharSequence)this.context.getString(R.string.dialog_install_success_title));
        builder.setMessage((CharSequence)this.context.getString(R.string.dialog_install_success_text));
        builder.setCancelable(false);
        builder.setPositiveButton((CharSequence)this.context.getString(R.string.dialog_install_success_positive), (dialogInterface, i) -> {
            this.activity.backToLastUI();
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
        if (this.minecraftInstallTask != null && this.minecraftInstallTask.getStatus() != null && this.minecraftInstallTask.getStatus() == AsyncTask.Status.RUNNING) {
            this.minecraftInstallTask.cancel(true);
        }
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

    public boolean handleMessage(@NonNull Message msg) {
        if (msg.what == 101010) {
            String speed = (String)msg.obj;
            this.speedText.setText((CharSequence)speed);
        }
        return false;
    }
}

