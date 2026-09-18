package com.qcl.launcher.control.bean.button;

/* loaded from: classes2.dex */
public class ButtonStyle implements Cloneable {
    public int cornerRadius;
    public int cornerRadiusPress;
    public String fillColor;
    public String fillColorPress;
    public String name;
    public String strokeColor;
    public String strokeColorPress;
    public float strokeWidth;
    public float strokeWidthPress;
    public String textColor;
    public String textColorPress;
    public int textSize;
    public int textSizePress;

    public ButtonStyle() {
        this("", 13, "#f6f6f6", 10, 1.0f, "#1a000000", "#00ffffff", 13, "#f6f6f6", 10, 1.0f, "#1a000000", "#40ffffff");
    }

    public ButtonStyle(String str, int i, String str2, int i2, float f, String str3, String str4, int i3, String str5, int i4, float f2, String str6, String str7) {
        this.name = str;
        this.textSize = i;
        this.textColor = str2;
        this.cornerRadius = i2;
        this.strokeWidth = f;
        this.strokeColor = str3;
        this.fillColor = str4;
        this.textSizePress = i3;
        this.textColorPress = str5;
        this.cornerRadiusPress = i4;
        this.strokeWidthPress = f2;
        this.strokeColorPress = str6;
        this.fillColorPress = str7;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
