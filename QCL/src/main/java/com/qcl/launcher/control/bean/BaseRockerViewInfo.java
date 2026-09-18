package com.qcl.launcher.control.bean;

import com.qcl.launcher.control.bean.rocker.RockerSize;
import com.qcl.launcher.control.bean.rocker.RockerStyle;

/* loaded from: classes2.dex */
public class BaseRockerViewInfo implements Cloneable {
    public static final int FUNCTION_FOLLOW_ALL = 2;
    public static final int FUNCTION_FOLLOW_CENTER = 1;
    public static final int FUNCTION_FOLLOW_NONE = 0;
    public static final int POSITION_TYPE_ABSOLUTE = 1;
    public static final int POSITION_TYPE_PERCENT = 0;
    public static final int SHOW_TYPE_ALWAYS = 0;
    public static final int SHOW_TYPE_IN_GAME = 1;
    public static final int SHOW_TYPE_OUT_GAME = 2;
    public static final int SIZE_OBJECT_HEIGHT = 1;
    public static final int SIZE_OBJECT_WIDTH = 0;
    public static final int SIZE_TYPE_ABSOLUTE = 1;
    public static final int SIZE_TYPE_PERCENT = 0;
    public String child;
    public int followType;
    public String pattern;
    public int positionType;
    public RockerStyle rockerStyle;
    public boolean shift;
    public int showType;
    public RockerSize size;
    public int sizeType;
    public boolean usingExist;
    public String uuid;
    public ViewPosition xPosition;
    public ViewPosition yPosition;

    public BaseRockerViewInfo(String str, String str2, String str3, int i, int i2, RockerSize rockerSize, int i3, ViewPosition viewPosition, ViewPosition viewPosition2, int i4, boolean z, boolean z2, RockerStyle rockerStyle) {
        this.uuid = str;
        this.pattern = str2;
        this.child = str3;
        this.showType = i;
        this.sizeType = i2;
        this.size = rockerSize;
        this.positionType = i3;
        this.xPosition = viewPosition;
        this.yPosition = viewPosition2;
        this.followType = i4;
        this.shift = z;
        this.usingExist = z2;
        this.rockerStyle = rockerStyle;
    }

    public void refresh(BaseRockerViewInfo baseRockerViewInfo) {
        this.uuid = baseRockerViewInfo.uuid;
        this.pattern = baseRockerViewInfo.pattern;
        this.child = baseRockerViewInfo.child;
        this.showType = baseRockerViewInfo.showType;
        this.sizeType = baseRockerViewInfo.sizeType;
        this.size = baseRockerViewInfo.size;
        this.positionType = baseRockerViewInfo.positionType;
        this.xPosition = baseRockerViewInfo.xPosition;
        this.yPosition = baseRockerViewInfo.yPosition;
        this.followType = baseRockerViewInfo.followType;
        this.shift = baseRockerViewInfo.shift;
        this.usingExist = baseRockerViewInfo.usingExist;
        this.rockerStyle = baseRockerViewInfo.rockerStyle;
    }

    public Object clone() throws CloneNotSupportedException {
        BaseRockerViewInfo baseRockerViewInfo = (BaseRockerViewInfo) super.clone();
        baseRockerViewInfo.size = (RockerSize) this.size.clone();
        baseRockerViewInfo.xPosition = (ViewPosition) this.xPosition.clone();
        baseRockerViewInfo.yPosition = (ViewPosition) this.yPosition.clone();
        baseRockerViewInfo.rockerStyle = (RockerStyle) this.rockerStyle.clone();
        return baseRockerViewInfo;
    }
}
