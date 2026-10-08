package com.qcl.launcher.launcher.setting;

import android.app.Activity;
import android.content.Context;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.info.contents.ContentListBean;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.launcher.setting.game.child.BoatLauncherSetting;
import com.qcl.launcher.launcher.setting.game.child.GameDirSetting;
import com.qcl.launcher.launcher.setting.game.child.JavaSetting;
import com.qcl.launcher.launcher.setting.game.child.PojavLauncherSetting;
import com.qcl.launcher.launcher.setting.game.child.RamSetting;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.launcher.setting.launcher.child.BackgroundSetting;
import com.qcl.launcher.launcher.setting.launcher.child.SourceSetting;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.file.AssetsUtils;
import com.qcl.launcher.utils.gson.GsonUtils;
import com.qcl.launcher.utils.platform.MemoryUtils;
import java.io.File;
import java.util.ArrayList;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class InitializeSetting {
    static final /* synthetic */ boolean $assertionsDisabled = false;

    public static void initializeControlPattern(Activity activity, AssetsUtils.FileOperateCallback fileOperateCallback) {
        String[] list = new File(AppManifest.CONTROLLER_DIR + "/").list();
        if (new File(AppManifest.CONTROLLER_DIR + "/").exists()) {
            if (list.length == 0) {
                AssetsUtils.getInstance(activity.getApplicationContext()).copyAssetsToSD("control", AppManifest.CONTROLLER_DIR).setFileOperateCallback(fileOperateCallback);
                return;
            }
            return;
        }
        AssetsUtils.getInstance(activity.getApplicationContext()).copyAssetsToSD("control", AppManifest.CONTROLLER_DIR).setFileOperateCallback(fileOperateCallback);
    }

    public static ArrayList<Account> initializeAccounts(Context context) {
        ArrayList<Account> arrayList = new ArrayList<>();
        if (new File(AppManifest.ACCOUNT_DIR + "/accounts.json").exists() && GsonUtils.getContentListFromFile(AppManifest.ACCOUNT_DIR + "/accounts.json").size() != 0) {
            return GsonUtils.getAccountListFromFile(AppManifest.ACCOUNT_DIR + "/accounts.json");
        }
        GsonUtils.saveAccounts(arrayList, AppManifest.ACCOUNT_DIR + "/accounts.json");
        return arrayList;
    }

    public static ArrayList<AuthlibInjectorServer> initializeAuthlibInjectorServer(Context context) {
        ArrayList<AuthlibInjectorServer> arrayList = new ArrayList<>();
        if (new File(AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json").exists() && GsonUtils.getContentListFromFile(AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json").size() != 0) {
            return GsonUtils.getServerListFromFile(AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json");
        }
        GsonUtils.saveServer(arrayList, AppManifest.ACCOUNT_DIR + "/authlib_injector_server.json");
        return arrayList;
    }

    public static ArrayList<ContentListBean> initializeContents(Context context) {
        ArrayList<ContentListBean> arrayList = new ArrayList<>();
        if (new File(AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json").exists() && GsonUtils.getContentListFromFile(AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json").size() != 0) {
            return GsonUtils.getContentListFromFile(AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json");
        }
        arrayList.add(new ContentListBean(context.getString(R.string.default_game_file_directory_list_pri), AppManifest.DEFAULT_GAME_DIR, true));
        arrayList.add(new ContentListBean(context.getString(R.string.default_game_file_directory_list_sec), AppManifest.INNER_GAME_DIR, false));
        GsonUtils.saveContents(arrayList, AppManifest.GAME_FILE_DIRECTORY_DIR + "/game_file_directories.json");
        return arrayList;
    }

    public static LauncherSetting initializeLauncherSetting() {
        if (new File(AppManifest.SETTING_DIR + "/launcher_setting.json").exists()) {
            return GsonUtils.getLauncherSettingFromFile(AppManifest.SETTING_DIR + "/launcher_setting.json");
        }
        // ★ 1.3.4：新用户默认背景改为「经典图片」（type 1）
        //   原为 type 0（动态轮播）；动态轮播已挪到「网络」选项，见 ExteriorSettingUI
        // ★★★ 1.5.0：默认下载源改为**官方**（用户要求「默认改成使用官方下载源」）。
        //   SourceSetting(autoSelect, autoSourceType, fixSourceType)：
        //     · 原值 (true, 1, 0) ⇒ autoSelect=true 时 getSource() 返回 autoSourceType=1
        //       = DOWNLOAD_URL_SOURCE_BMCLAPI（BMCLAPI 镜像）—— 所以以前默认**不是**官方源；
        //     · 现值 (true, 0, 0) ⇒ autoSourceType=0 = DOWNLOAD_URL_SOURCE_OFFICIAL，
        //       手动指定的 fixSourceType 本来就是 0（官方），两者现在一致。
        //   ★ 只改**全新安装**的默认值；已在用 BMCLAPI 的玩家其 settings.json 里已存了旧值，
        //     不受影响（可在「设置 → 下载源」里改回来）。
        LauncherSetting launcherSetting = new LauncherSetting(AppManifest.DEFAULT_GAME_DIR, new SourceSetting(true, 0, 0), 0, 64, false, true, false, false, false, "DEFAULT", "DEFAULT", new BackgroundSetting(1, "", ""), AppManifest.DEFAULT_CACHE_DIR);
        GsonUtils.saveLauncherSetting(launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
        return launcherSetting;
    }

    public static PublicGameSetting initializePublicGameSetting(Context context, MainActivity mainActivity) {
        if (new File(AppManifest.SETTING_DIR + "/public_game_setting.json").exists()) {
            return GsonUtils.getPublicGameSettingFromFile(AppManifest.SETTING_DIR + "/public_game_setting.json");
        }
        PublicGameSetting publicGameSetting = new PublicGameSetting(new Account(0, "", "", "", "", "", "", "", "", "", "", ""), AppManifest.DEBUG_DIR, SettingUtils.getLocalVersionNames(mainActivity.launcherSetting.gameFileDirectory).size() != 0 ? mainActivity.launcherSetting.gameFileDirectory + "/versions/" + SettingUtils.getLocalVersionNames(mainActivity.launcherSetting.gameFileDirectory).get(0) : "");
        GsonUtils.savePublicGameSetting(publicGameSetting, AppManifest.SETTING_DIR + "/public_game_setting.json");
        return publicGameSetting;
    }

    public static PrivateGameSetting initializePrivateGameSetting(Context context) {
        if (new File(AppManifest.SETTING_DIR + "/private_game_setting.json").exists()) {
            return GsonUtils.getPrivateGameSettingFromFile(AppManifest.SETTING_DIR + "/private_game_setting.json");
        }
        int findBestRAMAllocation = MemoryUtils.findBestRAMAllocation(context);
        PrivateGameSetting privateGameSetting = new PrivateGameSetting(false, true, true, false, false, false, false, new JavaSetting(true, AppManifest.JAVA_DIR + "/default"), "", "", "", new GameDirSetting(1, AppManifest.DEFAULT_GAME_DIR), new BoatLauncherSetting(false, "GL4ES115", "default"), new PojavLauncherSetting(true, "ng_gl4es", "default"), new RamSetting(findBestRAMAllocation, findBestRAMAllocation, true), "Default", 1.0f);
        GsonUtils.savePrivateGameSetting(privateGameSetting, AppManifest.SETTING_DIR + "/private_game_setting.json");
        return privateGameSetting;
    }
}
