package com.qcl.launcher.update;

/* loaded from: classes2.dex */
public class UpdateJSON {
    public LauncherVersion latestPrerelease;
    public LauncherVersion latestRelease;

    public UpdateJSON(LauncherVersion launcherVersion, LauncherVersion launcherVersion2) {
        this.latestRelease = launcherVersion;
        this.latestPrerelease = launcherVersion2;
    }
}
