package com.qcl.launcher.launcher.mod.hmcl;

import com.qcl.launcher.launcher.mod.ModpackManifest;
import com.qcl.launcher.launcher.mod.ModpackProvider;

public final class HMCLModpackManifest implements ModpackManifest {
    public static final HMCLModpackManifest INSTANCE = new HMCLModpackManifest();

    private HMCLModpackManifest() {}

    @Override
    public ModpackProvider getProvider() {
        return HMCLModpackProvider.INSTANCE;
    }
}