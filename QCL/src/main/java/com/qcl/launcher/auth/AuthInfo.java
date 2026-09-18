package com.qcl.launcher.auth;

import java.util.UUID;

/* loaded from: classes2.dex */
public class AuthInfo implements AutoCloseable {
    private final String accessToken;
    private final String userProperties;
    private final String username;
    private final UUID uuid;

    @Override // java.lang.AutoCloseable
    public void close() throws Exception {
    }

    public AuthInfo(String str, UUID uuid, String str2, String str3) {
        this.username = str;
        this.uuid = uuid;
        this.accessToken = str2;
        this.userProperties = str3;
    }

    public String getUsername() {
        return this.username;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getUserProperties() {
        return this.userProperties;
    }
}
