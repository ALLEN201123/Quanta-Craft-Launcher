package com.qcl.launcher.launcher.setting.game.child;

/* loaded from: classes2.dex */
public class PojavLauncherSetting implements Cloneable {
    public boolean enable;
    public String java;
    public String renderer;

    public PojavLauncherSetting(boolean z, String str, String str2) {
        this.enable = z;
        this.renderer = str;
        this.java = str2;
    }

    public void setEnable(boolean z) {
        this.enable = z;
    }

    public boolean isEnable() {
        return this.enable;
    }

    public void setRenderer(String str) {
        this.renderer = str;
    }

    public String getRenderer() {
        return this.renderer;
    }

    public void setJava(String str) {
        this.java = str;
    }

    public String getJava() {
        return this.java;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
