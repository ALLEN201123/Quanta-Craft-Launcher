package com.qcl.launcher.launcher.game;

/* loaded from: classes2.dex */
public interface VersionProvider {
    Version getVersion(String str) throws VersionNotFoundException;

    boolean hasVersion(String str);
}
