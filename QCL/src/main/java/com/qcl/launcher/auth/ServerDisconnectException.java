package com.qcl.launcher.auth;

/* loaded from: classes2.dex */
public class ServerDisconnectException extends AuthenticationException {
    public ServerDisconnectException() {
    }

    public ServerDisconnectException(String str) {
        super(str);
    }

    public ServerDisconnectException(String str, Throwable th) {
        super(str, th);
    }

    public ServerDisconnectException(Throwable th) {
        super(th);
    }
}
