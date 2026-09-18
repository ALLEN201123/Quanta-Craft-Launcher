package com.qcl.launcher.auth;

/* loaded from: classes2.dex */
public class ServerResponseMalformedException extends AuthenticationException {
    public ServerResponseMalformedException() {
    }

    public ServerResponseMalformedException(String str) {
        super(str);
    }

    public ServerResponseMalformedException(String str, Throwable th) {
        super(str, th);
    }

    public ServerResponseMalformedException(Throwable th) {
        super(th);
    }
}
