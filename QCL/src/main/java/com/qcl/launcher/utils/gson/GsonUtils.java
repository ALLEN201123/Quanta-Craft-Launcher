package com.qcl.launcher.utils.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.auth.Account;
import com.qcl.launcher.auth.authlibinjector.AuthlibInjectorServer;
import com.qcl.launcher.launcher.list.info.contents.ContentListBean;
import com.qcl.launcher.launcher.setting.game.PrivateGameSetting;
import com.qcl.launcher.launcher.setting.game.PublicGameSetting;
import com.qcl.launcher.launcher.setting.launcher.LauncherSetting;
import com.qcl.launcher.utils.file.FileStringUtils;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class GsonUtils {
    public static LauncherSetting getLauncherSettingFromFile(String str) {
        return (LauncherSetting) new Gson().fromJson(FileStringUtils.getStringFromFile(str), LauncherSetting.class);
    }

    public static PrivateGameSetting getPrivateGameSettingFromFile(String str) {
        return (PrivateGameSetting) new Gson().fromJson(FileStringUtils.getStringFromFile(str), PrivateGameSetting.class);
    }

    public static PublicGameSetting getPublicGameSettingFromFile(String str) {
        return (PublicGameSetting) new Gson().fromJson(FileStringUtils.getStringFromFile(str), PublicGameSetting.class);
    }

    public static ArrayList<ContentListBean> getContentListFromFile(String str) {
        return (ArrayList) new Gson().fromJson(FileStringUtils.getStringFromFile(str), new TypeToken<ArrayList<ContentListBean>>() { // from class: com.qcl.launcher.utils.gson.GsonUtils.1
        }.getType());
    }

    public static ArrayList<Account> getAccountListFromFile(String str) {
        return (ArrayList) new Gson().fromJson(FileStringUtils.getStringFromFile(str), new TypeToken<ArrayList<Account>>() { // from class: com.qcl.launcher.utils.gson.GsonUtils.2
        }.getType());
    }

    public static ArrayList<AuthlibInjectorServer> getServerListFromFile(String str) {
        return (ArrayList) JsonUtils.defaultGsonBuilder().registerTypeAdapter(AuthlibInjectorServer.class, new AuthlibInjectorServer.Deserializer()).create().fromJson(FileStringUtils.getStringFromFile(str), new TypeToken<ArrayList<AuthlibInjectorServer>>() { // from class: com.qcl.launcher.utils.gson.GsonUtils.3
        }.getType());
    }

    public static void saveLauncherSetting(LauncherSetting launcherSetting, String str) {
        FileStringUtils.writeFile(str, new Gson().toJson(launcherSetting));
    }

    public static void savePrivateGameSetting(PrivateGameSetting privateGameSetting, String str) {
        FileStringUtils.writeFile(str, new Gson().toJson(privateGameSetting));
    }

    public static void savePublicGameSetting(PublicGameSetting publicGameSetting, String str) {
        FileStringUtils.writeFile(str, new Gson().toJson(publicGameSetting));
    }

    public static void saveContents(ArrayList<ContentListBean> arrayList, String str) {
        FileStringUtils.writeFile(str, new Gson().toJson(arrayList));
    }

    public static void saveAccounts(ArrayList<Account> arrayList, String str) {
        FileStringUtils.writeFile(str, new Gson().toJson(arrayList));
    }

    public static void saveServer(ArrayList<AuthlibInjectorServer> arrayList, String str) {
        FileStringUtils.writeFile(str, JsonUtils.defaultGsonBuilder().registerTypeAdapter(AuthlibInjectorServer.class, new AuthlibInjectorServer.Deserializer()).create().toJson(arrayList));
    }
}
