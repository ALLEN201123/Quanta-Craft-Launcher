package com.qcl.launcher.control.bean.rocker;

/* loaded from: classes2.dex */
public class RockerStyle implements Cloneable {
    public int cornerRadius;
    public int cornerRadiusPress;
    public String fillColor;
    public String fillColorPress;
    public String name;
    public String pointerColor;
    public String pointerColorPress;
    public String strokeColor;
    public String strokeColorPress;
    public float strokeWidth;
    public float strokeWidthPress;

    public RockerStyle() {
        this("", 80, 2.0f, "#1a000000", "#00ffffff", "#f6f6f6", 80, 2.0f, "#1a000000", "#00ffffff", "#40ffffff");
    }

    public RockerStyle(String str, int i, float f, String str2, String str3, String str4, int i2, float f2, String str5, String str6, String str7) {
        this.name = str;
        this.cornerRadius = i;
        this.strokeWidth = f;
        this.strokeColor = str2;
        this.fillColor = str3;
        this.pointerColor = str4;
        this.cornerRadiusPress = i2;
        this.strokeWidthPress = f2;
        this.strokeColorPress = str5;
        this.fillColorPress = str6;
        this.pointerColorPress = str7;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
