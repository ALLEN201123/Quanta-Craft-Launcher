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
import com.qcl.launcher.launcher.download.babric.BabricInstallTask;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.utils.gson.GsonUtils;
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

    /**
     * ★ 1.2.3：玩家在安装页勾了 Risugami's ModLoader 吗。
     * 由 {@code InstallGameUI} 在 show() 之前设置。
     * 装完基础版本（installJson 那一步）之后才会真正去装它 —— 因为 ModLoader
     * 是往 minecraft.jar 里注入 class，jar 必须先装好。
     */
    public boolean installModLoader;
    public boolean installBabric;
    private BabricInstallTask babricInstallTask;
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
                // ★★★ 1.2.3：归档版（远古版本）**不走 installJson()** ——
                //   原来这里直接弹「安装成功」框，所以玩家勾了 ModLoader 也不会装。
                //   这里补上同一步（复用下面那两个方法），归档版才会真的去下 ModLoader。
                if (GameInstallDialog.this.installModLoader) {
                    GameInstallDialog.this.installModLoaderThenFinish();
                    return;
                }
                GameInstallDialog.this.showInstallSuccess();
            }
        });
        this.legacyArchiveInstallTask.execute(new VersionManifest.Version[]{this.version});
    }

    /**
     * ★ 1.2.3：装 Babric（b1.7.3 的 Fabric 分支）。
     *
     * 和 ModLoader 不同，Babric 是 Fabric 系：它下载加载器 + 依赖库 + 改 mainClass，
     * **不动本体 jar 里的 class**。所以不需要「合并 jarmods」那套。
     */
    public void downloadBabric() {
        // ★ 兜底：非 b1.7.3 直接拒绝（UI 那边挡过一次，这里再挡一次）
        String vid = this.version == null ? null : this.version.id;
        if (vid == null || !"b1.7.3".equalsIgnoreCase(vid.trim())) {
            throwException(new Exception("Babric 只支持 b1.7.3，当前版本（"
                    + (vid == null ? "未知" : vid) + "）装不了。"));
            return;
        }
        this.babricInstallTask = new BabricInstallTask(this.activity, this.downloadTaskListAdapter,
                this.version.id, new BabricInstallTask.InstallBabricCallback() {

            @Override
            public void onStart() {
            }

            @Override
            public void onFailed(Exception e) {
                GameInstallDialog.this.throwException(e);
            }

            @Override
            public void onFinish(Version version) {
                GameInstallDialog.this.gameVersionJson =
                        PatchMerger.mergePatch(GameInstallDialog.this.gameVersionJson, version);
                // 装完：写标记（让 ModLoaderDetector 认出这是 Babric）+ 关文件校验
                File dir = new File(GameInstallDialog.this.activity.launcherSetting.gameFileDirectory
                        + "/versions/" + GameInstallDialog.this.name);
                // ★ 1.2.5：写**带内容**的标记（老代码 createNewFile() 会在版本目录里
                //   留一个 0 字节的 .babric，玩家在文件管理器里看着像坏文件）
                BabricInstallTask.writeMarker(dir,
                        GameInstallDialog.this.version == null ? null : GameInstallDialog.this.version.id,
                        version.getVersion(),
                        version.getLibraries() == null ? 0 : version.getLibraries().size());
                disableFileCheck(dir);
                GameInstallDialog.this.showInstallSuccess();
            }
        });
        this.babricInstallTask.execute(new String[]{null});
    }

    /** ★ 1.2.3：关闭这个版本的「检查游戏完整性」（加载器改过版本 json，校验必然对不上） */
    private void disableFileCheck(File versionDir) {
        try {
            File cfg = new File(versionDir, "qcl.cfg");
            PrivateGameSetting st = GsonUtils.getPrivateGameSettingFromFile(cfg.getAbsolutePath());
            if (st == null) {
                PrivateGameSetting tpl = this.activity.privateGameSetting;
                if (tpl != null) {
                    com.google.gson.Gson g = new com.google.gson.Gson();
                    st = g.fromJson(g.toJson(tpl), PrivateGameSetting.class);
                }
            }
            if (st != null && !st.notCheckMinecraft) {
                st.notCheckMinecraft = true;
                GsonUtils.savePrivateGameSetting(st, cfg.getAbsolutePath());
            }
        } catch (Throwable ignored) {
        }
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

        // ★★★ 1.2.3：这里是整条安装链的终点。
        //   如果玩家勾了 ModLoader，就在**基础版本已经装完、jar 已就位**之后再装它
        //   —— ModLoader 是往 minecraft.jar 里注入 class，必须等 jar 好。
        //   装完（成功或失败）再弹「安装成功」框，让玩家看到完整过程。
        if (this.installModLoader) {
            installModLoaderThenFinish();
            return;
        }
        // ★ 1.2.3：勾了 Babric —— 同样等基础版本装完（jar 已就位）再装
        if (this.installBabric) {
            downloadBabric();
            return;
        }
        showInstallSuccess();
    }

    /** 安装成功提示（原 installJson 的收尾部分） */
    private void showInstallSuccess() {
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

    /**
     * ★ 1.2.3：把 Risugami's ModLoader 装到刚装好的这个版本上。
     *
     * 用**同一个 downloadTaskListAdapter** —— 玩家在同一个任务列表里能接着看到
     * 「下载 ModLoader」→「写入 minecraft.jar」，而不是突然跳出一个新框。
     *
     * 装完（无论成败）都会走回 showInstallSuccess / throwException，
     * 不会让流程卡住。
     */
    private void installModLoaderThenFinish() {
        com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask task =
                new com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask(
                        this.activity,
                        this.name,                     // 版本目录名
                        this.version != null ? this.version.id : this.name,   // 查 ModLoader 表用的 MC 版本
                        this.downloadTaskListAdapter,
                        new com.qcl.launcher.launcher.download.modloader.ModLoaderInstallTask.Callback() {
                            @Override
                            public void onStart() {
                            }

                            @Override
                            public void onFinish(int injected) {
                                android.util.Log.i("GameInstallDialog",
                                        "ModLoader 已写入 " + injected + " 个 class");
                                GameInstallDialog.this.showInstallSuccess();
                            }

                            @Override
                            public void onFailed(Exception e) {
                                // ModLoader 失败不该让整个安装算失败（游戏本体是好的），
                                // 但必须明确告诉玩家，不能默默吞掉
                                android.util.Log.e("GameInstallDialog", "ModLoader 安装失败", e);
                                GameInstallDialog.this.throwException(e);
                            }
                        });
        task.execute();
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

