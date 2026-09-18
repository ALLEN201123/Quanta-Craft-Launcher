package com.qcl.launcher.launcher.setting.game.child;

/* loaded from: classes2.dex */
public class RamSetting implements Cloneable {
    public boolean autoRam;
    public int maxRam;
    public int minRam;

    public RamSetting(int i, int i2, boolean z) {
        this.minRam = i;
        this.maxRam = i2;
        this.autoRam = z;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
