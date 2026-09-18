package com.qcl.launcher.control.bean;

import com.qcl.launcher.control.bean.button.ButtonSize;
import com.qcl.launcher.control.bean.button.ButtonStyle;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class BaseButtonInfo implements Cloneable {
    public static final int FUNCTION_TYPE_DOUBLE_CLICK = 1;
    public static final int FUNCTION_TYPE_TOUCH = 0;
    public static final int POSITION_TYPE_ABSOLUTE = 1;
    public static final int POSITION_TYPE_PERCENT = 0;
    public static final int SHOW_TYPE_ALWAYS = 0;
    public static final int SHOW_TYPE_IN_GAME = 1;
    public static final int SHOW_TYPE_OUT_GAME = 2;
    public static final int SIZE_OBJECT_HEIGHT = 1;
    public static final int SIZE_OBJECT_WIDTH = 0;
    public static final int SIZE_TYPE_ABSOLUTE = 1;
    public static final int SIZE_TYPE_PERCENT = 0;
    public boolean autoClick;
    public boolean autoKeep;
    public ButtonStyle buttonStyle;
    public String child;
    public int functionType;
    public ButtonSize height;
    public boolean movable;
    public boolean openMenu;
    public ArrayList<Integer> outputKeycode;
    public String outputText;
    public String pattern;
    public int positionType;
    public boolean showInputDialog;
    public int showType;
    public int sizeType;
    public boolean switchLeftPad;
    public boolean switchSensor;
    public boolean switchTouchMode;
    public String text;
    public boolean usingExist;
    public String uuid;
    public boolean viewMove;
    public ArrayList<String> visibilityControl;
    public ButtonSize width;
    public ViewPosition xPosition;
    public ViewPosition yPosition;

    public BaseButtonInfo(String str, String str2, String str3, String str4, int i, int i2, ButtonSize buttonSize, ButtonSize buttonSize2, int i3, ViewPosition viewPosition, ViewPosition viewPosition2, int i4, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, boolean z6, boolean z7, boolean z8, boolean z9, ArrayList<String> arrayList, String str5, ArrayList<Integer> arrayList2, boolean z10, ButtonStyle buttonStyle) {
        this.uuid = str;
        this.pattern = str2;
        this.child = str3;
        this.text = str4;
        this.showType = i;
        this.sizeType = i2;
        this.width = buttonSize;
        this.height = buttonSize2;
        this.positionType = i3;
        this.xPosition = viewPosition;
        this.yPosition = viewPosition2;
        this.functionType = i4;
        this.viewMove = z;
        this.autoKeep = z2;
        this.autoClick = z3;
        this.openMenu = z4;
        this.movable = z5;
        this.switchTouchMode = z6;
        this.switchSensor = z7;
        this.switchLeftPad = z8;
        this.showInputDialog = z9;
        this.visibilityControl = arrayList;
        this.outputText = str5;
        this.outputKeycode = arrayList2;
        this.usingExist = z10;
        this.buttonStyle = buttonStyle;
    }

    public void refresh(BaseButtonInfo baseButtonInfo) {
        this.uuid = baseButtonInfo.uuid;
        this.pattern = baseButtonInfo.pattern;
        this.child = baseButtonInfo.child;
        this.text = baseButtonInfo.text;
        this.showType = baseButtonInfo.showType;
        this.sizeType = baseButtonInfo.sizeType;
        this.width = baseButtonInfo.width;
        this.height = baseButtonInfo.height;
        this.positionType = baseButtonInfo.positionType;
        this.xPosition = baseButtonInfo.xPosition;
        this.yPosition = baseButtonInfo.yPosition;
        this.functionType = baseButtonInfo.functionType;
        this.viewMove = baseButtonInfo.viewMove;
        this.autoKeep = baseButtonInfo.autoKeep;
        this.autoClick = baseButtonInfo.autoClick;
        this.openMenu = baseButtonInfo.openMenu;
        this.movable = baseButtonInfo.movable;
        this.switchTouchMode = baseButtonInfo.switchTouchMode;
        this.switchSensor = baseButtonInfo.switchSensor;
        this.switchLeftPad = baseButtonInfo.switchLeftPad;
        this.showInputDialog = baseButtonInfo.showInputDialog;
        this.visibilityControl = baseButtonInfo.visibilityControl;
        this.outputText = baseButtonInfo.outputText;
        this.outputKeycode = baseButtonInfo.outputKeycode;
        this.usingExist = baseButtonInfo.usingExist;
        this.buttonStyle = baseButtonInfo.buttonStyle;
    }

    public Object clone() throws CloneNotSupportedException {
        BaseButtonInfo baseButtonInfo = (BaseButtonInfo) super.clone();
        baseButtonInfo.width = (ButtonSize) this.width.clone();
        baseButtonInfo.height = (ButtonSize) this.height.clone();
        baseButtonInfo.xPosition = (ViewPosition) this.xPosition.clone();
        baseButtonInfo.yPosition = (ViewPosition) this.yPosition.clone();
        baseButtonInfo.visibilityControl = (ArrayList) this.visibilityControl.clone();
        baseButtonInfo.outputKeycode = (ArrayList) this.outputKeycode.clone();
        baseButtonInfo.buttonStyle = (ButtonStyle) this.buttonStyle.clone();
        return baseButtonInfo;
    }
}
