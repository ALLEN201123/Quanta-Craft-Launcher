package com.qcl.launcher.launcher.uis.game.manager.right;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import com.qcl.launcher.R;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.dialogs.EditWorldNameDialog;
import com.qcl.launcher.launcher.dialogs.LoadingDialog;
import com.qcl.launcher.launcher.game.Argument;
import com.qcl.launcher.launcher.game.Artifact;
import com.qcl.launcher.launcher.game.RuledArgument;
import com.qcl.launcher.launcher.game.Version;
import com.qcl.launcher.launcher.game.World;
import com.qcl.launcher.launcher.list.local.save.WorldListAdapter;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.uis.tools.BaseUI;
import com.qcl.launcher.utils.animation.CustomAnimationUtils;
import com.qcl.launcher.utils.file.FileStringUtils;
import com.qcl.launcher.utils.file.UriUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.gson.JsonUtils;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.platform.Bits;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WorldManagerUI extends BaseUI implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {

    public LinearLayout worldManagerUI;

    public String versionName;
    private String version;
    private String saveDir;

    private CheckBox checkAll;
    private LinearLayout refresh;
    private LinearLayout addWorld;
    private LinearLayout download;

    private ProgressBar progressBar;
    private LinearLayout worldLayout;
    private ListView worldList;

    private ArrayList<World> allWorldList;
    private ArrayList<World> versionWorldList;

    public static final int PICK_WORLD_REQUEST = 3100;

    public WorldManagerUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        worldManagerUI = activity.findViewById(R.id.ui_world_manager);

        checkAll = activity.findViewById(R.id.show_all_world);
        refresh = activity.findViewById(R.id.refresh_local_world);
        addWorld = activity.findViewById(R.id.add_new_world);
        download = activity.findViewById(R.id.download_new_world);

        checkAll.setOnCheckedChangeListener(this);
        refresh.setOnClickListener(this);
        addWorld.setOnClickListener(this);
        download.setOnClickListener(this);

        progressBar = activity.findViewById(R.id.load_world_progress);
        worldLayout = activity.findViewById(R.id.local_world_list_layout);
        worldList = activity.findViewById(R.id.world_list);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStart() {
        super.onStart();
        CustomAnimationUtils.showViewFromLeft(worldManagerUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.gameManagerUI.startWorldManager.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(worldManagerUI,activity,context,false);
        if (activity.isLoaded){
            activity.uiManager.gameManagerUI.startWorldManager.setBackground(context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_WORLD_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            String path = UriUtils.getRealPathFromUri_AboveApi19(context,uri);
            if (path != null) {
                LoadingDialog dialog = new LoadingDialog(context);
                dialog.setLoadingText(context.getString(R.string.dialog_import_world));
                new Thread(() -> {
                    try {
                        activity.runOnUiThread(dialog::show);
                        World world = new World(new File(path).toPath());
                        activity.runOnUiThread(() -> {
                            dialog.dismiss();
                            EditWorldNameDialog editWorldNameDialog = new EditWorldNameDialog(context, world, saveDir, WorldManagerUI.this);
                            editWorldNameDialog.show();
                        });
                    } catch (IOException e) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        });
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    public void refresh(String versionName){
        this.versionName = versionName;
        PrivateGameSetting privateGameSetting;
        String settingPath = activity.launcherSetting.gameFileDirectory + "/versions/" + versionName + "/qcl.cfg";
        if (new File(settingPath).exists() && GsonUtils.getPrivateGameSettingFromFile(settingPath) != null && (GsonUtils.getPrivateGameSettingFromFile(settingPath).forceEnable || GsonUtils.getPrivateGameSettingFromFile(settingPath).enable)) {
            privateGameSetting = GsonUtils.getPrivateGameSettingFromFile(settingPath);
        }
        else {
            privateGameSetting = activity.privateGameSetting;
        }
        if (privateGameSetting.gameDirSetting.type == 0){
            saveDir = activity.launcherSetting.gameFileDirectory + "/saves";
        }
        else if (privateGameSetting.gameDirSetting.type == 1){
            saveDir = activity.launcherSetting.gameFileDirectory + "/versions/" + versionName + "/saves";
        }
        else {
            saveDir = privateGameSetting.gameDirSetting.path + "/saves";
        }
        refreshList();
    }

    private void refreshList() {
        new Thread(() -> {
            activity.runOnUiThread(() -> {
                progressBar.setVisibility(View.VISIBLE);
                worldLayout.setVisibility(View.GONE);
            });
            String gameJsonText = FileStringUtils.getStringFromFile(activity.launcherSetting.gameFileDirectory + "/versions/" + versionName +"/" + versionName + ".json");
            Gson gson = JsonUtils.defaultGsonBuilder()
                    .registerTypeAdapter(Artifact.class, new Artifact.Serializer())
                    .registerTypeAdapter(Bits.class, new Bits.Serializer())
                    .registerTypeAdapter(RuledArgument.class, new RuledArgument.Serializer())
                    .registerTypeAdapter(Argument.class, new Argument.Deserializer())
                    .create();
            Version v = gson.fromJson(gameJsonText, Version.class);
            this.version = v.getId();
            allWorldList = new ArrayList<>();
            // ★ 1.4.4：远古版本（classic / indev / infdev）的存档位置与新版本不一样 ——
            //   有时落在游戏目录的 saves/，有时落在**版本目录**的 saves/。
            //   原来只扫 saveDir 一个地方 → 在游戏里保存完世界，回到存档页却什么都看不到。
            //   现在三个候选目录全扫一遍，并按路径去重（同一个世界不会被列两次）。
            java.util.LinkedHashSet<java.nio.file.Path> seen = new java.util.LinkedHashSet<>();
            List<String> candidates = new ArrayList<>();
            candidates.add(saveDir);
            candidates.add(activity.launcherSetting.gameFileDirectory + "/saves");
            candidates.add(activity.launcherSetting.gameFileDirectory + "/versions/" + versionName + "/saves");
            for (String dir : candidates) {
                if (dir == null || seen.contains(new File(dir).toPath().toAbsolutePath())) {
                    continue;
                }
                seen.add(new File(dir).toPath().toAbsolutePath());
                List<World> found = World.getWorlds(new File(dir).toPath()).collect(Collectors.toList());
                if (!found.isEmpty()) {
                    Logging.LOG.log(Level.INFO, "Found " + found.size() + " world(s) in " + dir);
                }
                allWorldList.addAll(found);
            }
            activity.runOnUiThread(() -> {
                getNeededList();
                progressBar.setVisibility(View.GONE);
                worldLayout.setVisibility(View.VISIBLE);
            });
        }).start();
    }

    private void getNeededList() {
        versionWorldList = new ArrayList<>();
        if (checkAll.isChecked()) {
            versionWorldList.addAll(allWorldList);
        }
        else {
            for (World world : allWorldList) {
                if (world.getGameVersion() == null || world.getGameVersion().equals(version)) {
                    versionWorldList.add(world);
                }
            }
        }
        WorldListAdapter adapter = new WorldListAdapter(context,activity,versionWorldList);
        worldList.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        if (view == refresh) {
            refreshList();
        }
        if (view == addWorld) {
            Intent intent = new Intent(context, FileChooser.class);
            intent.putExtra(Constants.SELECTION_MODE, Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            // ★★★ 1.4.8：以前只允许 "zip"，导致**远古版本（indev/infdev）的单文件存档
            //   （xxx.mclevel）根本选不了** —— 用户表现就是"导入不了、没法选文件"。
            //   这里把 .mclevel / .mcworld 也放进来（World 类已支持这两类）：
            //     .mcworld = 基岩版导出的存档包，.mclevel = 远古版本单文件存档
            //   ⚠️ 分隔符必须是【分号】—— FileChooser 里是 split(";")，
            //     传逗号会让整串被当成一个后缀名，结果一个文件都列不出来
            //     （这正是"选了文件却什么都没有"的根因）。
            intent.putExtra(Constants.ALLOWED_FILE_EXTENSIONS, "zip;mclevel;mcworld");
            intent.putExtra(Constants.INITIAL_DIRECTORY, new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            activity.startActivityForResult(intent, PICK_WORLD_REQUEST);
        }
        if (view == download) {
            activity.uiManager.switchMainUI(activity.uiManager.downloadUI);
            activity.uiManager.downloadUI.downloadUIManager.switchDownloadUI(activity.uiManager.downloadUI.downloadUIManager.downloadWorldUI);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == checkAll) {
            getNeededList();
        }
    }
}
