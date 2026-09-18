package com.qcl.launcher.auth.yggdrasil;

import com.qcl.launcher.auth.AuthenticationException;
import java.net.URL;
import java.util.UUID;

/* loaded from: classes2.dex */
public interface YggdrasilProvider {
    URL getAuthenticationURL() throws AuthenticationException;

    URL getInvalidationURL() throws AuthenticationException;

    URL getProfilePropertiesURL(UUID uuid) throws AuthenticationException;

    URL getRefreshmentURL() throws AuthenticationException;

    URL getSkinUploadURL(UUID uuid) throws AuthenticationException, UnsupportedOperationException;

    URL getValidationURL() throws AuthenticationException;
}
