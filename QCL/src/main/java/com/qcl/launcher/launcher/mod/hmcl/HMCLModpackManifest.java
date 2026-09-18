package com.qcl.launcher.launcher.mod.hmcl;

import com.qcl.launcher.launcher.mod.ModpackManifest;
import com.qcl.launcher.launcher.mod.ModpackProvider;

/* loaded from: classes2.dex */
public final class HMCLModpackManifest implements ModpackManifest {
    public static final HMCLModpackManifest INSTANCE = new HMCLModpackManifest();

    private HMCLModpackManifest() {
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackManifest
    public ModpackProvider getProvider() {
        return HMCLModpackProvider.INSTANCE;
    }
}
