package com.qcl.launcher.launcher.game;

/* loaded from: classes2.dex */
public enum ReleaseType {
    RELEASE("release"),
    SNAPSHOT("snapshot"),
    MODIFIED("modified"),
    OLD_BETA("old-beta"),
    OLD_ALPHA("old-alpha"),
    UNKNOWN("unknown");

    private final String id;

    ReleaseType(String str) {
        this.id = str;
    }

    public String getId() {
        return this.id;
    }
}
