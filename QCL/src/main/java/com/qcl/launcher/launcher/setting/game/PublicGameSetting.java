package com.qcl.launcher.launcher.setting.game;

import com.qcl.launcher.auth.Account;
import com.qcl.launcher.utils.gson.GsonUtils;
import java.io.File;

/* loaded from: classes2.dex */
public class PublicGameSetting {
    public Account account;
    public String currentVersion;
    public String home;

    public PublicGameSetting(Account account, String str, String str2) {
        this.account = account;
        this.home = str;
        this.currentVersion = str2;
    }

    public static boolean isUsingIsolateSetting(String str) {
        if (!new File(str + "/qcl.cfg").exists() || GsonUtils.getPrivateGameSettingFromFile(str + "/qcl.cfg") == null) {
            return false;
        }
        return GsonUtils.getPrivateGameSettingFromFile(new StringBuilder().append(str).append("/qcl.cfg").toString()).forceEnable || GsonUtils.getPrivateGameSettingFromFile(new StringBuilder().append(str).append("/qcl.cfg").toString()).enable;
    }
}
