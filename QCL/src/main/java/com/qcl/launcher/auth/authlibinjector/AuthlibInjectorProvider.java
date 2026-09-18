package com.qcl.launcher.auth.authlibinjector;

import com.qcl.launcher.auth.AuthenticationException;
import com.qcl.launcher.auth.yggdrasil.YggdrasilProvider;
import com.qcl.launcher.utils.gson.UUIDTypeAdapter;
import com.qcl.launcher.utils.io.NetworkUtils;
import java.net.URL;
import java.util.UUID;

/* loaded from: classes2.dex */
public class AuthlibInjectorProvider implements YggdrasilProvider {
    private final String apiRoot;

    public AuthlibInjectorProvider(String str) {
        this.apiRoot = str;
    }

    @Override // com.qcl.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getAuthenticationURL() throws AuthenticationException {
        return NetworkUtils.toURL(this.apiRoot + "authserver/authenticate");
    }

    @Override // com.qcl.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getRefreshmentURL() throws AuthenticationException {
        return NetworkUtils.toURL(this.apiRoot + "authserver/refresh");
    }

    @Override // com.qcl.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getValidationURL() throws AuthenticationException {
        return NetworkUtils.toURL(this.apiRoot + "authserver/validate");
    }

    @Override // com.qcl.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getInvalidationURL() throws AuthenticationException {
        return NetworkUtils.toURL(this.apiRoot + "authserver/invalidate");
    }

    @Override // com.qcl.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getSkinUploadURL(UUID uuid) throws UnsupportedOperationException {
        return NetworkUtils.toURL(this.apiRoot + "api/user/profile/" + UUIDTypeAdapter.fromUUID(uuid) + "/skin");
    }

    @Override // com.qcl.launcher.auth.yggdrasil.YggdrasilProvider
    public URL getProfilePropertiesURL(UUID uuid) throws AuthenticationException {
        return NetworkUtils.toURL(this.apiRoot + "sessionserver/session/minecraft/profile/" + UUIDTypeAdapter.fromUUID(uuid));
    }

    public String toString() {
        return this.apiRoot;
    }
}
