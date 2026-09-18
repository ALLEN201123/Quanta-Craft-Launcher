package com.qcl.launcher.auth.authlibinjector;

import com.qcl.launcher.utils.Logging;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

/* loaded from: classes2.dex */
public class SimpleAuthlibInjectorArtifactProvider implements AuthlibInjectorArtifactProvider {
    private Path location;

    public SimpleAuthlibInjectorArtifactProvider(Path path) {
        this.location = path;
    }

    @Override // com.qcl.launcher.auth.authlibinjector.AuthlibInjectorArtifactProvider
    public AuthlibInjectorArtifactInfo getArtifactInfo() throws IOException {
        return AuthlibInjectorArtifactInfo.from(this.location);
    }

    @Override // com.qcl.launcher.auth.authlibinjector.AuthlibInjectorArtifactProvider
    public Optional<AuthlibInjectorArtifactInfo> getArtifactInfoImmediately() {
        try {
            return Optional.of(getArtifactInfo());
        } catch (IOException e) {
            Logging.LOG.log(Level.WARNING, "Bad authlib-injector artifact", (Throwable) e);
            return Optional.empty();
        }
    }
}
