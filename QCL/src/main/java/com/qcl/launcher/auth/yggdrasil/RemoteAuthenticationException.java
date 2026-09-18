package com.qcl.launcher.auth.yggdrasil;

import com.qcl.launcher.auth.AuthenticationException;

/* loaded from: classes2.dex */
public class RemoteAuthenticationException extends AuthenticationException {
    private final String cause;
    private final String message;
    private final String name;

    public RemoteAuthenticationException(String str, String str2, String str3) {
        super(buildMessage(str, str2, str3));
        this.name = str;
        this.message = str2;
        this.cause = str3;
    }

    public String getRemoteName() {
        return this.name;
    }

    public String getRemoteMessage() {
        return this.message;
    }

    public String getRemoteCause() {
        return this.cause;
    }

    private static String buildMessage(String str, String str2, String str3) {
        StringBuilder sb = new StringBuilder(str);
        if (str2 != null) {
            sb.append(": ").append(str2);
        }
        if (str3 != null) {
            sb.append(": ").append(str3);
        }
        return sb.toString();
    }
}
