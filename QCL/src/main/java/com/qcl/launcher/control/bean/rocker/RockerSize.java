package com.qcl.launcher.control.bean.rocker;

/* loaded from: classes2.dex */
public class RockerSize implements Cloneable {
    public int absoluteSize;
    public int object;
    public float percentSize;

    public RockerSize(int i, float f, int i2) {
        this.absoluteSize = i;
        this.percentSize = f;
        this.object = i2;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
