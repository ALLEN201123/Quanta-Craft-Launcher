package com.qcl.launcher.launcher.setting.game.child;

/* loaded from: classes2.dex */
public class JavaSetting implements Cloneable {
    public boolean autoSelect;
    public int bitMode;
    public String name;

    public JavaSetting(boolean z, String str) {
        this.autoSelect = z;
        this.name = str;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
