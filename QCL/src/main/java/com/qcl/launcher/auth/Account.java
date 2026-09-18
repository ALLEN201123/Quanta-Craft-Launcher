package com.qcl.launcher.auth;

import com.qcl.launcher.auth.offline.OfflineSkinSetting;

/* loaded from: classes2.dex */
public class Account {
    public String auth_access_token;
    public String auth_client_token;
    public String auth_player_name;
    public String auth_session;
    public String auth_uuid;
    public String email;
    public String loginServer;
    public int loginType;
    public OfflineSkinSetting offlineSkinSetting;
    public String password;
    public String refresh_token;
    public String texture;
    public String user_type;

    public Account(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String str11) {
        this.loginType = i;
        this.email = str;
        this.password = str2;
        this.user_type = str3;
        this.auth_session = str4;
        this.auth_player_name = str5;
        this.auth_uuid = str6;
        this.auth_access_token = str7;
        this.auth_client_token = str8;
        this.refresh_token = str9;
        this.loginServer = str10;
        this.texture = str11;
    }

    public void refresh(Account account) {
        this.loginType = account.loginType;
        this.email = account.email;
        this.password = account.password;
        this.user_type = account.user_type;
        this.auth_session = account.auth_session;
        this.auth_player_name = account.auth_player_name;
        this.auth_uuid = account.auth_uuid;
        this.auth_access_token = account.auth_access_token;
        this.auth_client_token = account.auth_client_token;
        this.refresh_token = account.refresh_token;
        this.loginServer = account.loginServer;
        this.texture = account.texture;
        this.offlineSkinSetting = account.offlineSkinSetting;
    }
}
