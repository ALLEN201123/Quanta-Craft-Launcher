package com.qcl.launcher.auth.authlibinjector;

import java.io.IOException;
import java.util.Optional;

/* loaded from: classes2.dex */
public interface AuthlibInjectorArtifactProvider {
    AuthlibInjectorArtifactInfo getArtifactInfo() throws IOException;

    Optional<AuthlibInjectorArtifactInfo> getArtifactInfoImmediately();
}
