package com.qcl.launcher.launcher.game;

/* loaded from: classes2.dex */
public class GameJavaVersion {
    public static final GameJavaVersion JAVA_16 = new GameJavaVersion("java-runtime-alpha", 16);
    public static final GameJavaVersion JAVA_8 = new GameJavaVersion("jre-legacy", 8);
    private final String component;
    private final int majorVersion;

    public GameJavaVersion() {
        this("", 0);
    }

    public GameJavaVersion(String str, int i) {
        this.component = str;
        this.majorVersion = i;
    }

    public String getComponent() {
        return this.component;
    }

    public int getMajorVersion() {
        return this.majorVersion;
    }
}
