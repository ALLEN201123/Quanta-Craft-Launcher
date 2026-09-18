package com.qcl.launcher.skin.cape;

/* loaded from: classes2.dex */
public class CapePartSection {
    String capePartName;
    int height;
    String sideName;
    int startX;
    int startY;
    int width;

    public CapePartSection(String str, String str2, int i, int i2, int i3, int i4) {
        this.capePartName = str;
        this.sideName = str2;
        this.startX = i;
        this.startY = i2;
        this.width = i3;
        this.height = i4;
    }

    public String getCapePartName() {
        return this.capePartName;
    }

    public int getHeight() {
        return this.height;
    }

    public String getSideName() {
        return this.sideName;
    }

    public int getStartX() {
        return this.startX;
    }

    public int getStartY() {
        return this.startY;
    }

    public int getWidth() {
        return this.width;
    }
}
