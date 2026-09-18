package com.qcl.launcher.auth;

/* loaded from: classes2.dex */
public class CredentialExpiredException extends AuthenticationException {
    public CredentialExpiredException() {
    }

    public CredentialExpiredException(String str, Throwable th) {
        super(str, th);
    }

    public CredentialExpiredException(String str) {
        super(str);
    }

    public CredentialExpiredException(Throwable th) {
        super(th);
    }
}
