package com.qcl.launcher.control.bean;

/* loaded from: classes2.dex */
public class ViewPosition implements Cloneable {
    public int absolutePosition;
    public float percentPosition;

    public ViewPosition(int i, float f) {
        this.absolutePosition = i;
        this.percentPosition = f;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
