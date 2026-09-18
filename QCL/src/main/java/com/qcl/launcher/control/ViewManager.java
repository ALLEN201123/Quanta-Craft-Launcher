package com.qcl.launcher.control;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import com.qcl.launcher.control.bean.BaseButtonInfo;
import com.qcl.launcher.control.bean.BaseRockerViewInfo;
import com.qcl.launcher.control.view.BaseButton;
import com.qcl.launcher.control.view.BaseRockerView;
import com.qcl.launcher.control.view.LayoutPanel;
import com.qcl.launcher.control.view.MenuFloat;
import com.qcl.launcher.control.view.MenuView;
import com.qcl.launcher.control.view.TouchPad;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.launcher.setting.game.GameMenuSetting;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes2.dex */
public class ViewManager implements SensorEventListener {
    public static final float NS2S = 1.0E-9f;
    public Activity activity;
    public Context context;
    public int launcher;
    public LayoutPanel layoutPanel;
    public MenuFloat menuFloat;
    public MenuHelper menuHelper;
    public MenuView menuView;
    public int screenHeight;
    public int screenWidth;
    public Sensor sensor;
    public SensorManager sensorManager;
    public float timestamp;
    public TouchPad touchPad;
    public float[] angle = new float[3];
    public String viewMovingType = "0";

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public ViewManager(Context context, Activity activity, MenuHelper menuHelper, LayoutPanel layoutPanel, int i) {
        this.context = context;
        this.activity = activity;
        this.menuHelper = menuHelper;
        this.layoutPanel = layoutPanel;
        this.launcher = i;
        this.screenWidth = layoutPanel.getWidth();
        this.screenHeight = layoutPanel.getHeight();
        init();
    }

    private void init() {
        SensorManager sensorManager = (SensorManager) this.context.getSystemService("sensor");
        this.sensorManager = sensorManager;
        this.sensor = sensorManager.getDefaultSensor(4);
        setSensorEnable(this.menuHelper.gameMenuSetting.enableSensor);
        TouchPad touchPad = new TouchPad(this.context, this.launcher, this.layoutPanel.getWidth(), this.layoutPanel.getHeight(), this.menuHelper);
        this.touchPad = touchPad;
        this.layoutPanel.addView(touchPad);
        MenuFloat menuFloat = new MenuFloat(this.context, this.menuHelper, this.layoutPanel.getWidth(), this.layoutPanel.getHeight(), this.menuHelper.gameMenuSetting.menuFloatSetting.positionX, this.menuHelper.gameMenuSetting.menuFloatSetting.positionY);
        this.menuFloat = menuFloat;
        menuFloat.addCallback(new MenuFloat.MenuFloatCallback() { // from class: com.qcl.launcher.control.ViewManager.1
            @Override // com.qcl.launcher.control.view.MenuFloat.MenuFloatCallback
            public void onClick() {
                ViewManager.this.menuHelper.toggleGameMenu();
            }

            @Override // com.qcl.launcher.control.view.MenuFloat.MenuFloatCallback
            public void onMove(float f, float f2) {
                ViewManager.this.menuHelper.gameMenuSetting.menuFloatSetting.positionX = f;
                ViewManager.this.menuHelper.gameMenuSetting.menuFloatSetting.positionY = f2;
                GameMenuSetting.saveGameMenuSetting(ViewManager.this.menuHelper.gameMenuSetting);
            }
        });
        MenuView menuView = new MenuView(this.context, this.menuHelper, this.layoutPanel.getWidth(), this.layoutPanel.getHeight(), this.menuHelper.gameMenuSetting.menuViewSetting.mode, this.menuHelper.gameMenuSetting.menuViewSetting.yPercent);
        this.menuView = menuView;
        menuView.addCallback(new MenuView.MenuCallback() { // from class: com.qcl.launcher.control.ViewManager.2
            @Override // com.qcl.launcher.control.view.MenuView.MenuCallback
            public void onMoveModeStart() {
            }

            @Override // com.qcl.launcher.control.view.MenuView.MenuCallback
            public void onMoveModeStop() {
            }

            @Override // com.qcl.launcher.control.view.MenuView.MenuCallback
            public void onRelease() {
                ViewManager.this.menuHelper.toggleGameMenu();
            }

            @Override // com.qcl.launcher.control.view.MenuView.MenuCallback
            public void onMove(int i, float f) {
                ViewManager.this.menuHelper.gameMenuSetting.menuViewSetting.mode = i;
                ViewManager.this.menuHelper.gameMenuSetting.menuViewSetting.yPercent = f;
                GameMenuSetting.saveGameMenuSetting(ViewManager.this.menuHelper.gameMenuSetting);
            }
        });
        if (this.menuHelper.gameMenuSetting.menuFloatSetting.enable) {
            this.layoutPanel.addView(this.menuFloat);
        }
        if (this.menuHelper.gameMenuSetting.menuViewSetting.enable) {
            this.layoutPanel.addView(this.menuView);
        }
        refreshLayout(this.menuHelper.currentPattern.name, this.menuHelper.currentChild, this.menuHelper.editMode);
    }

    public void addButton(BaseButtonInfo baseButtonInfo, int i) {
        BaseButton baseButton = new BaseButton(this.context, this.screenWidth, this.screenHeight, baseButtonInfo, this.menuHelper);
        baseButton.setIsShowing(i == 0);
        this.layoutPanel.addView(baseButton);
        baseButton.updateSizeAndPosition(baseButtonInfo);
        if (this.menuHelper.editMode) {
            baseButton.saveButtonInfo();
        }
    }

    public void loadButton(BaseButtonInfo baseButtonInfo, int i) {
        BaseButton baseButton = new BaseButton(this.context, this.screenWidth, this.screenHeight, baseButtonInfo, this.menuHelper);
        baseButton.setIsShowing(i == 0);
        this.layoutPanel.addView(baseButton);
        baseButton.updateSizeAndPosition(baseButtonInfo);
    }

    public void addRocker(BaseRockerViewInfo baseRockerViewInfo, int i) {
        BaseRockerView baseRockerView = new BaseRockerView(this.context, this.screenWidth, this.screenHeight, baseRockerViewInfo, this.menuHelper);
        baseRockerView.setIsShowing(i == 0);
        this.layoutPanel.addView(baseRockerView);
        baseRockerView.updateSizeAndPosition(baseRockerViewInfo);
        if (this.menuHelper.editMode) {
            baseRockerView.saveRockerInfo();
        }
    }

    public void loadRocker(BaseRockerViewInfo baseRockerViewInfo, int i) {
        BaseRockerView baseRockerView = new BaseRockerView(this.context, this.screenWidth, this.screenHeight, baseRockerViewInfo, this.menuHelper);
        baseRockerView.setIsShowing(i == 0);
        this.layoutPanel.addView(baseRockerView);
        baseRockerView.updateSizeAndPosition(baseRockerViewInfo);
    }

    public void refreshViews() {
        for (int i = 0; i < this.layoutPanel.getChildCount(); i++) {
            if (this.layoutPanel.getChildAt(i) instanceof BaseButton) {
                ((BaseButton) this.layoutPanel.getChildAt(i)).refresh();
            }
        }
    }

    public void refreshLayout(String str, String str2, boolean z) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.layoutPanel.getChildCount(); i++) {
            if ((this.layoutPanel.getChildAt(i) instanceof BaseButton) || (this.layoutPanel.getChildAt(i) instanceof BaseRockerView)) {
                arrayList.add(this.layoutPanel.getChildAt(i));
            }
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            this.layoutPanel.removeView((View) it.next());
        }
        if (SettingUtils.getChildList(str).size() > 0) {
            Iterator<ChildLayout> it2 = SettingUtils.getChildList(str).iterator();
            while (it2.hasNext()) {
                ChildLayout next = it2.next();
                if (next != null) {
                    boolean z2 = z && next.name != null && next.name.equals(str2);
                    if (next.baseButtonList != null) {
                        Iterator<BaseButtonInfo> it3 = next.baseButtonList.iterator();
                        while (it3.hasNext()) {
                            BaseButtonInfo next2 = it3.next();
                            if (next2 != null) {
                                loadButton(next2, z2 ? 0 : next.visibility);
                            }
                        }
                    }
                    if (next.baseRockerViewList != null) {
                        Iterator<BaseRockerViewInfo> it4 = next.baseRockerViewList.iterator();
                        while (it4.hasNext()) {
                            BaseRockerViewInfo next3 = it4.next();
                            if (next3 != null) {
                                loadRocker(next3, z2 ? 0 : next.visibility);
                            }
                        }
                    }
                }
            }
        }
        hideUI(this.menuHelper.gameMenuSetting.hideUI);
    }

    public void setChildVisibility(String str) {
        for (int i = 0; i < this.layoutPanel.getChildCount(); i++) {
            if ((this.layoutPanel.getChildAt(i) instanceof BaseButton) && ((BaseButton) this.layoutPanel.getChildAt(i)).info.child.equals(str)) {
                ((BaseButton) this.layoutPanel.getChildAt(i)).setIsShowing(!((BaseButton) this.layoutPanel.getChildAt(i)).getIsShowing());
            }
            if ((this.layoutPanel.getChildAt(i) instanceof BaseRockerView) && ((BaseRockerView) this.layoutPanel.getChildAt(i)).info.child.equals(str)) {
                ((BaseRockerView) this.layoutPanel.getChildAt(i)).setIsShowing(!((BaseRockerView) this.layoutPanel.getChildAt(i)).getIsShowing());
            }
        }
    }

    public void hideUI(boolean z) {
        for (int i = 0; i < this.layoutPanel.getChildCount(); i++) {
            if (!(this.layoutPanel.getChildAt(i) instanceof TouchPad)) {
                this.layoutPanel.getChildAt(i).setAlpha(z ? 0.0f : 1.0f);
            }
        }
    }

    public void enableCursor() {
        if (this.touchPad != null) {
            InputBridge.setPointer(this.launcher, (int) (this.menuHelper.cursorX * this.menuHelper.scaleFactor), (int) (this.menuHelper.cursorY * this.menuHelper.scaleFactor));
        }
        for (int i = 0; i < this.layoutPanel.getChildCount(); i++) {
            if (this.layoutPanel.getChildAt(i) instanceof BaseButton) {
                ((BaseButton) this.layoutPanel.getChildAt(i)).refreshVisibility();
            }
            if (this.layoutPanel.getChildAt(i) instanceof BaseRockerView) {
                ((BaseRockerView) this.layoutPanel.getChildAt(i)).refreshVisibility();
            }
        }
    }

    public void disableCursor() {
        this.viewMovingType = "0";
        for (int i = 0; i < this.layoutPanel.getChildCount(); i++) {
            if (this.layoutPanel.getChildAt(i) instanceof BaseButton) {
                ((BaseButton) this.layoutPanel.getChildAt(i)).refreshVisibility();
            }
            if (this.layoutPanel.getChildAt(i) instanceof BaseRockerView) {
                ((BaseRockerView) this.layoutPanel.getChildAt(i)).refreshVisibility();
            }
        }
    }

    public void setSensorEnable(boolean z) {
        if (z) {
            this.sensorManager.registerListener(this, this.sensor, 0);
            return;
        }
        this.sensorManager.unregisterListener(this);
        if (this.menuHelper.gameCursorMode == 1) {
            InputBridge.setPointer(this.launcher, (int) this.menuHelper.currentX, (int) this.menuHelper.currentY);
        }
    }

    public void setGamePointer(String str, boolean z, float f, float f2) {
        if (this.viewMovingType.equals("0") || this.viewMovingType.equals(str)) {
            if (!this.menuHelper.gameMenuSetting.enableSensor) {
                InputBridge.setPointer(this.launcher, (int) (this.menuHelper.pointerX + (this.menuHelper.gameMenuSetting.mouseSpeed * f)), (int) (this.menuHelper.pointerY + (this.menuHelper.gameMenuSetting.mouseSpeed * f2)));
            }
            MenuHelper menuHelper = this.menuHelper;
            menuHelper.currentX = menuHelper.pointerX + (this.menuHelper.gameMenuSetting.mouseSpeed * f);
            MenuHelper menuHelper2 = this.menuHelper;
            menuHelper2.currentY = menuHelper2.pointerY + (this.menuHelper.gameMenuSetting.mouseSpeed * f2);
            this.viewMovingType = str;
            if (z) {
                return;
            }
            this.menuHelper.pointerX += f * this.menuHelper.gameMenuSetting.mouseSpeed;
            this.menuHelper.pointerY += f2 * this.menuHelper.gameMenuSetting.mouseSpeed;
            this.viewMovingType = "0";
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (this.menuHelper.gameCursorMode == 1) {
            if (this.timestamp != 0.0f) {
                float f = (((float) sensorEvent.timestamp) - this.timestamp) * 1.0E-9f;
                float[] fArr = this.angle;
                fArr[0] = fArr[0] + (sensorEvent.values[0] * f);
                float[] fArr2 = this.angle;
                fArr2[1] = fArr2[1] + (sensorEvent.values[1] * f);
                InputBridge.setPointer(this.launcher, (int) (this.menuHelper.currentX - (((float) Math.toDegrees(this.angle[0])) * this.menuHelper.gameMenuSetting.sensitivity)), (int) (this.menuHelper.currentY + (((float) Math.toDegrees(this.angle[1])) * this.menuHelper.gameMenuSetting.sensitivity)));
            }
            this.timestamp = (float) sensorEvent.timestamp;
        }
    }
}
