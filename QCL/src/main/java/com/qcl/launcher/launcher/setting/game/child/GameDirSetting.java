package com.qcl.launcher.launcher.setting.game.child;

/* loaded from: classes2.dex */
public class GameDirSetting implements Cloneable {
    public String path;
    public int type;

    public GameDirSetting(int i, String str) {
        this.type = i;
        this.path = str;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
