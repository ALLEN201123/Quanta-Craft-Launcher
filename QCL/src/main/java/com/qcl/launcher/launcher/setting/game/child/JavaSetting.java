package com.qcl.launcher.launcher.setting.game.child;

import androidx.annotation.NonNull;

public class JavaSetting implements Cloneable{

    public boolean autoSelect;
    public String name;
    /** 运行时位数：0=自动（跟随设备/应用，64 位设备自动用 64 位）、1=强制 64 位、2=强制 32 位 */
    public int bitMode;

    public JavaSetting(boolean autoSelect,String name){
        this.autoSelect = autoSelect;
        this.name = name;
    }

    @NonNull
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
