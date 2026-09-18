/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.graphics.Canvas
 *  android.graphics.Color
 *  android.graphics.Paint
 *  android.graphics.Paint$Style
 *  android.graphics.Path
 *  android.graphics.drawable.Drawable
 *  android.graphics.drawable.GradientDrawable
 *  android.os.Handler
 *  android.os.Vibrator
 *  android.view.MotionEvent
 *  android.view.View
 *  android.view.ViewGroup$LayoutParams
 *  android.widget.Toast
 *  androidx.appcompat.app.AlertDialog
 *  androidx.appcompat.app.AlertDialog$Builder
 *  androidx.appcompat.widget.AppCompatButton
 */
package com.qcl.launcher.control.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.control.bean.BaseButtonInfo;
import com.qcl.launcher.launcher.dialogs.control.EditButtonDialog;
import com.qcl.launcher.launcher.dialogs.control.InputDialog;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;
import java.util.ArrayList;
import java.util.Iterator;

import com.qcl.launcher.R;
@SuppressLint(value={"ViewConstructor"})
public class BaseButton
extends AppCompatButton {
    public int screenWidth;
    public int screenHeight;
    public BaseButtonInfo info;
    public MenuHelper menuHelper;
    public GradientDrawable drawableNormal;
    public GradientDrawable drawablePress;
    private float initialX;
    private float initialY;
    private float initialPositionX;
    private float initialPositionY;
    private int clickCount;
    private long firstClickTime;
    private final Paint outlinePaint;
    private boolean isKeeping = false;
    private boolean isShowing = true;
    private final Handler deleteHandler = new Handler();
    private final Runnable deleteRunnable = () -> {
        Vibrator vibrator = (Vibrator)this.getContext().getSystemService("vibrator");
        vibrator.vibrate(100L);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_delete_button_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_delete_button_content));
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_delete_button_positive), (dialogInterface, i) -> this.deleteButton());
        builder.setNegativeButton((CharSequence)this.getContext().getString(R.string.dialog_delete_button_negative), (dialogInterface, i) -> {});
        AlertDialog dialog = builder.create();
        dialog.show();
    };
    private final Handler clickHandler = new Handler();
    private final Runnable clickRunnable = new Runnable(){

        @Override
        public void run() {
            for (int code : BaseButton.this.info.outputKeycode) {
                InputBridge.sendEvent(BaseButton.this.menuHelper.launcher, code, true);
            }
            BaseButton.this.clickHandler.post(BaseButton.this.clickRunnable);
        }
    };

    public BaseButton(Context context, int screenWidth, int screenHeight, BaseButtonInfo info, MenuHelper menuHelper) {
        super(context);
        // ★★★ 幻影分身修复：禁用 AppCompatButton 默认的 elevation 状态动画，
        // 否则按住按键时系统会改 translationZ 产生阴影重影（FCL ControlButton 同款处理）。
        this.setStateListAnimator(null);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.menuHelper = menuHelper;
        this.outlinePaint = new Paint();
        this.outlinePaint.setAntiAlias(true);
        this.outlinePaint.setColor(this.getContext().getColor(R.color.colorRed));
        this.outlinePaint.setStyle(Paint.Style.STROKE);
        this.outlinePaint.setStrokeWidth(3.0f);
        this.refreshStyle(info);
    }

    @SuppressLint(value={"DrawAllocation"})
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.menuHelper.showOutline) {
            Path outlinePath = new Path();
            outlinePath.moveTo(0.0f, 0.0f);
            outlinePath.lineTo((float)this.getWidth(), 0.0f);
            outlinePath.lineTo((float)this.getWidth(), (float)this.getHeight());
            outlinePath.lineTo(0.0f, (float)this.getHeight());
            outlinePath.lineTo(0.0f, 0.0f);
            canvas.drawPath(outlinePath, this.outlinePaint);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.menuHelper.editMode) {
            switch (event.getActionMasked()) {
                case 0: {
                    this.initialX = event.getX();
                    this.initialY = event.getY();
                    this.initialPositionX = this.getX();
                    this.initialPositionY = this.getY();
                    this.deleteHandler.postDelayed(this.deleteRunnable, 600L);
                    this.setPressDrawable();
                    this.menuHelper.viewManager.layoutPanel.showReference(this.info.positionType, this.getX(), this.getY(), this.getWidth(), this.getHeight());
                    break;
                }
                case 2: {
                    float targetX = this.getX() + event.getX() - this.initialX >= 0.0f && this.getX() + event.getX() - this.initialX <= (float)(this.screenWidth - this.getWidth()) ? this.getX() + event.getX() - this.initialX : (this.getX() + event.getX() - this.initialX < 0.0f ? 0.0f : (float)(this.screenWidth - this.getWidth()));
                    float targetY = this.getY() + event.getY() - this.initialY >= 0.0f && this.getY() + event.getY() - this.initialY <= (float)(this.screenHeight - this.getHeight()) ? this.getY() + event.getY() - this.initialY : (this.getY() + event.getY() - this.initialY < 0.0f ? 0.0f : (float)(this.screenHeight - this.getHeight()));
                    this.setX(targetX);
                    this.setY(targetY);
                    this.info.xPosition.absolutePosition = ConvertUtils.px2dip(this.getContext(), targetX);
                    this.info.yPosition.absolutePosition = ConvertUtils.px2dip(this.getContext(), targetY);
                    this.info.xPosition.percentPosition = targetX / (float)(this.screenWidth - this.getWidth());
                    this.info.yPosition.percentPosition = targetY / (float)(this.screenHeight - this.getHeight());
                    this.saveButtonInfo();
                    this.menuHelper.viewManager.layoutPanel.showReference(this.info.positionType, this.getX(), this.getY(), this.getWidth(), this.getHeight());
                    if (!(Math.abs(event.getX() - this.initialX) > 1.0f) && !(Math.abs(event.getY() - this.initialY) > 1.0f)) break;
                    this.deleteHandler.removeCallbacks(this.deleteRunnable);
                    break;
                }
                case 1: 
                case 3: {
                    this.deleteHandler.removeCallbacks(this.deleteRunnable);
                    if (Math.abs(event.getX() - this.initialX) <= 10.0f && Math.abs(event.getY() - this.initialY) <= 10.0f) {
                        this.setX(this.initialPositionX);
                        this.setY(this.initialPositionY);
                        this.info.xPosition.absolutePosition = ConvertUtils.px2dip(this.getContext(), this.initialPositionX);
                        this.info.yPosition.absolutePosition = ConvertUtils.px2dip(this.getContext(), this.initialPositionY);
                        this.info.xPosition.percentPosition = this.initialPositionX / (float)(this.screenWidth - this.getWidth());
                        this.info.yPosition.percentPosition = this.initialPositionY / (float)(this.screenHeight - this.getHeight());
                        this.saveButtonInfo();
                        EditButtonDialog dialog = new EditButtonDialog(this.getContext(), this.menuHelper.viewManager, this.info.pattern, this.info.child, this.screenWidth, this.screenHeight, this, this.menuHelper.fullscreen);
                        dialog.show();
                    }
                    this.setNormalDrawable();
                    this.menuHelper.viewManager.layoutPanel.hideReference();
                }
            }
        } else {
            if (this.info.functionType == 0) {
                switch (event.getActionMasked()) {
                    case 0: {
                        if (!this.info.autoKeep) {
                            this.setPressDrawable();
                        }
                        this.initialX = event.getRawX();
                        this.initialY = event.getRawY();
                        this.initialPositionX = this.getX();
                        this.initialPositionY = this.getY();
                        if (this.info.openMenu) {
                            this.menuHelper.toggleGameMenu();
                        }
                        if (this.info.switchTouchMode) {
                            if (this.menuHelper.gameMenuSetting.touchMode == 0) {
                                this.menuHelper.spinnerTouchMode.setSelection(1);
                                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_touch_mode_attack_alert), (int)0).show();
                            } else {
                                this.menuHelper.spinnerTouchMode.setSelection(0);
                                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_touch_mode_create_alert), (int)0).show();
                            }
                        }
                        if (this.info.switchSensor) {
                            if (this.menuHelper.switchSensor.isChecked()) {
                                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_sensor_close_alert), (int)0).show();
                            } else {
                                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_sensor_open_alert), (int)0).show();
                            }
                            this.menuHelper.switchSensor.setChecked(!this.menuHelper.switchSensor.isChecked());
                        }
                        if (this.info.switchLeftPad) {
                            if (this.menuHelper.switchHalfScreen.isChecked()) {
                                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_half_screen_disable), (int)0).show();
                            } else {
                                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_half_screen_enable), (int)0).show();
                            }
                            this.menuHelper.switchHalfScreen.setChecked(!this.menuHelper.switchHalfScreen.isChecked());
                        }
                        if (!this.info.autoKeep && !this.info.autoClick) {
                            for (int code : this.info.outputKeycode) {
                                InputBridge.sendEvent(this.menuHelper.launcher, code, true);
                            }
                        }
                        if (!this.info.autoKeep && this.info.autoClick) {
                            this.clickHandler.post(this.clickRunnable);
                        }
                        if (this.info.autoKeep) {
                            if (this.isKeeping) {
                                this.setNormalDrawable();
                            } else {
                                this.setPressDrawable();
                            }
                            if (this.info.autoClick) {
                                if (this.isKeeping) {
                                    this.clickHandler.removeCallbacks(this.clickRunnable);
                                    for (int code : this.info.outputKeycode) {
                                        InputBridge.sendEvent(this.menuHelper.launcher, code, false);
                                    }
                                } else {
                                    this.clickHandler.post(this.clickRunnable);
                                }
                            } else if (this.isKeeping) {
                                for (int code : this.info.outputKeycode) {
                                    InputBridge.sendEvent(this.menuHelper.launcher, code, false);
                                }
                            } else {
                                for (int code : this.info.outputKeycode) {
                                    InputBridge.sendEvent(this.menuHelper.launcher, code, true);
                                }
                            }
                            this.isKeeping = !this.isKeeping;
                        }
                        ArrayList<String> childNames = new ArrayList<String>();
                        for (ChildLayout childLayout : SettingUtils.getChildList(this.info.pattern)) {
                            childNames.add(childLayout.name);
                        }
                        Iterator<String> iterator = this.info.visibilityControl.iterator();
                        while (iterator.hasNext()) {
                            String child = iterator.next();
                            if (childNames.contains(child)) {
                                this.menuHelper.viewManager.setChildVisibility(child);
                                continue;
                            }
                            iterator.remove();
                            this.saveButtonInfo();
                        }
                        if (this.info.outputText != null && !this.info.outputText.equals("")) {
                            if (this.menuHelper.gameCursorMode == 0) {
                                for (int i = 0; i < this.info.outputText.length(); ++i) {
                                    InputBridge.sendKeyChar(this.menuHelper.launcher, this.info.outputText.charAt(i));
                                }
                            } else {
                                InputBridge.sendEvent(this.menuHelper.launcher, 84, true);
                                InputBridge.sendEvent(this.menuHelper.launcher, 84, false);
                                new Handler().postDelayed(() -> {
                                    for (int i = 0; i < this.info.outputText.length(); ++i) {
                                        InputBridge.sendKeyChar(this.menuHelper.launcher, this.info.outputText.charAt(i));
                                    }
                                    InputBridge.sendEvent(this.menuHelper.launcher, 257, true);
                                    InputBridge.sendEvent(this.menuHelper.launcher, 257, false);
                                }, 50L);
                            }
                        }
                        if (!this.info.showInputDialog) break;
                        if (!this.menuHelper.gameMenuSetting.advanceInput) {
                            this.menuHelper.touchCharInput.switchKeyboardState();
                            break;
                        }
                        InputDialog dialog = new InputDialog(this.getContext(), this.menuHelper);
                        dialog.show();
                        break;
                    }
                    case 2: {
                        if (this.info.viewMove && this.menuHelper.gameCursorMode == 1) {
                            this.menuHelper.viewManager.setGamePointer(this.info.uuid, true, event.getRawX() - this.initialX, event.getRawY() - this.initialY);
                        }
                        if (!this.info.movable) break;
                        float targetX = this.initialPositionX + event.getRawX() - this.initialX >= 0.0f && this.initialPositionX + event.getRawX() - this.initialX <= (float)(this.screenWidth - this.getWidth()) ? this.initialPositionX + event.getRawX() - this.initialX : (this.initialPositionX + event.getRawX() - this.initialX < 0.0f ? 0.0f : (float)(this.screenWidth - this.getWidth()));
                        float targetY = this.initialPositionY + event.getRawY() - this.initialY >= 0.0f && this.initialPositionY + event.getRawY() - this.initialY <= (float)(this.screenHeight - this.getHeight()) ? this.initialPositionY + event.getRawY() - this.initialY : (this.initialPositionY + event.getRawY() - this.initialY < 0.0f ? 0.0f : (float)(this.screenHeight - this.getHeight()));
                        this.setX(targetX);
                        this.setY(targetY);
                        break;
                    }
                    case 1: 
                    case 3: {
                        if (this.info.viewMove && this.menuHelper.gameCursorMode == 1) {
                            this.menuHelper.viewManager.setGamePointer(this.info.uuid, false, event.getRawX() - this.initialX, event.getRawY() - this.initialY);
                        }
                        if (!this.info.autoKeep && !this.info.autoClick) {
                            for (int code : this.info.outputKeycode) {
                                InputBridge.sendEvent(this.menuHelper.launcher, code, false);
                            }
                        }
                        if (!this.info.autoKeep && this.info.autoClick) {
                            this.clickHandler.removeCallbacks(this.clickRunnable);
                            for (int code : this.info.outputKeycode) {
                                InputBridge.sendEvent(this.menuHelper.launcher, code, false);
                            }
                        }
                        if (this.info.autoKeep) break;
                        this.setNormalDrawable();
                    }
                }
            }
            if (this.info.functionType == 1) {
                switch (event.getActionMasked()) {
                    case 0: {
                        if (!this.info.autoKeep) {
                            this.setPressDrawable();
                        }
                        this.initialX = event.getRawX();
                        this.initialY = event.getRawY();
                        this.initialPositionX = this.getX();
                        this.initialPositionY = this.getY();
                        ++this.clickCount;
                        if (this.clickCount == 1) {
                            this.firstClickTime = System.currentTimeMillis();
                        }
                        if (this.clickCount != 2) break;
                        if (System.currentTimeMillis() - this.firstClickTime <= 500L) {
                            this.handleDoubleClick();
                            this.clickCount = 0;
                            break;
                        }
                        this.firstClickTime = System.currentTimeMillis();
                        this.clickCount = 1;
                        break;
                    }
                    case 2: {
                        if (this.info.viewMove && this.menuHelper.gameCursorMode == 1) {
                            this.menuHelper.viewManager.setGamePointer(this.info.uuid, true, event.getRawX() - this.initialX, event.getRawY() - this.initialY);
                        }
                        if (!this.info.movable) break;
                        float targetX = this.initialPositionX + event.getRawX() - this.initialX >= 0.0f && this.initialPositionX + event.getRawX() - this.initialX <= (float)(this.screenWidth - this.getWidth()) ? this.initialPositionX + event.getRawX() - this.initialX : (this.initialPositionX + event.getRawX() - this.initialX < 0.0f ? 0.0f : (float)(this.screenWidth - this.getWidth()));
                        float targetY = this.initialPositionY + event.getRawY() - this.initialY >= 0.0f && this.initialPositionY + event.getRawY() - this.initialY <= (float)(this.screenHeight - this.getHeight()) ? this.initialPositionY + event.getRawY() - this.initialY : (this.initialPositionY + event.getRawY() - this.initialY < 0.0f ? 0.0f : (float)(this.screenHeight - this.getHeight()));
                        this.setX(targetX);
                        this.setY(targetY);
                        break;
                    }
                    case 1: 
                    case 3: {
                        if (this.info.viewMove && this.menuHelper.gameCursorMode == 1) {
                            this.menuHelper.viewManager.setGamePointer(this.info.uuid, false, event.getRawX() - this.initialX, event.getRawY() - this.initialY);
                        }
                        if (this.info.autoKeep) break;
                        this.setNormalDrawable();
                    }
                }
            }
        }
        return true;
    }

    private void handleDoubleClick() {
        if (this.info.openMenu) {
            this.menuHelper.toggleGameMenu();
        }
        if (this.info.switchTouchMode) {
            if (this.menuHelper.gameMenuSetting.touchMode == 0) {
                this.menuHelper.spinnerTouchMode.setSelection(1);
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_touch_mode_attack_alert), (int)0).show();
            } else {
                this.menuHelper.spinnerTouchMode.setSelection(0);
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_touch_mode_create_alert), (int)0).show();
            }
        }
        if (this.info.switchSensor) {
            if (this.menuHelper.switchSensor.isChecked()) {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_sensor_close_alert), (int)0).show();
            } else {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_sensor_open_alert), (int)0).show();
            }
            this.menuHelper.switchSensor.setChecked(!this.menuHelper.switchSensor.isChecked());
        }
        if (this.info.switchLeftPad) {
            if (this.menuHelper.switchHalfScreen.isChecked()) {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_half_screen_disable), (int)0).show();
            } else {
                Toast.makeText((Context)this.getContext(), (CharSequence)this.getContext().getString(R.string.drawer_game_menu_control_half_screen_enable), (int)0).show();
            }
            this.menuHelper.switchHalfScreen.setChecked(!this.menuHelper.switchHalfScreen.isChecked());
        }
        if (!this.info.autoKeep) {
            for (int code : this.info.outputKeycode) {
                InputBridge.sendEvent(this.menuHelper.launcher, code, true);
                InputBridge.sendEvent(this.menuHelper.launcher, code, false);
            }
        }
        if (this.info.autoKeep) {
            if (this.isKeeping) {
                this.setNormalDrawable();
            } else {
                this.setPressDrawable();
            }
            if (this.info.autoClick) {
                if (this.isKeeping) {
                    this.clickHandler.removeCallbacks(this.clickRunnable);
                    for (int code : this.info.outputKeycode) {
                        InputBridge.sendEvent(this.menuHelper.launcher, code, false);
                    }
                } else {
                    this.clickHandler.post(this.clickRunnable);
                }
            } else if (this.isKeeping) {
                for (int code : this.info.outputKeycode) {
                    InputBridge.sendEvent(this.menuHelper.launcher, code, false);
                }
            } else {
                for (int code : this.info.outputKeycode) {
                    InputBridge.sendEvent(this.menuHelper.launcher, code, true);
                }
            }
            this.isKeeping = !this.isKeeping;
        }
        ArrayList<String> childNames = new ArrayList<String>();
        for (ChildLayout childLayout : SettingUtils.getChildList(this.info.pattern)) {
            childNames.add(childLayout.name);
        }
        Iterator<String> it = this.info.visibilityControl.iterator();
        while (it.hasNext()) {
            String child = it.next();
            if (childNames.contains(child)) {
                this.menuHelper.viewManager.setChildVisibility(child);
                continue;
            }
            it.remove();
            this.saveButtonInfo();
        }
        if (this.info.outputText != null && !this.info.outputText.equals("")) {
            if (this.menuHelper.gameCursorMode == 0) {
                for (int i = 0; i < this.info.outputText.length(); ++i) {
                    InputBridge.sendKeyChar(this.menuHelper.launcher, this.info.outputText.charAt(i));
                }
            } else {
                InputBridge.sendEvent(this.menuHelper.launcher, 84, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 84, false);
                new Handler().postDelayed(new Runnable(){

                    @Override
                    public void run() {
                        for (int i = 0; i < BaseButton.this.info.outputText.length(); ++i) {
                            InputBridge.sendKeyChar(BaseButton.this.menuHelper.launcher, BaseButton.this.info.outputText.charAt(i));
                        }
                        InputBridge.sendEvent(BaseButton.this.menuHelper.launcher, 257, true);
                        InputBridge.sendEvent(BaseButton.this.menuHelper.launcher, 257, false);
                    }
                }, 50L);
            }
        }
        if (this.info.showInputDialog) {
            if (!this.menuHelper.gameMenuSetting.advanceInput) {
                this.menuHelper.touchCharInput.switchKeyboardState();
            } else {
                InputDialog dialog = new InputDialog(this.getContext(), this.menuHelper);
                dialog.show();
            }
        }
    }

    public void setIsShowing(boolean show) {
        this.isShowing = show;
        this.refreshVisibility();
    }

    public boolean getIsShowing() {
        return this.isShowing;
    }

    public void refreshVisibility() {
        int mode = this.menuHelper.viewManager == null ? 0 : this.menuHelper.gameCursorMode;
        // ★★★ 编辑模式：只显示「当前选中子布局(currentChild)」的按键，
        // 游戏布局 / 键盘布局二选一（玩家在子布局下拉框切换），其余布局一律隐藏，
        // 避免键盘布局等其它布局的按键冒出来干扰编辑。
        if (this.menuHelper.editMode) {
            String c = this.info.child;
            boolean matches = c != null && c.equals(this.menuHelper.currentChild);
            this.setVisibility(matches ? 0 : 4);
            return;
        }
        if (this.isShowing && (this.info.showType == 0 || mode == 1 && this.info.showType == 1 || mode == 0 && this.info.showType == 2)) {
            this.setVisibility(0);
        } else {
            this.setVisibility(4);
        }
    }

    public void refresh() {
        this.invalidate();
    }

    public void refreshStyle(BaseButtonInfo info) {
        this.info = info;
        this.drawableNormal = new GradientDrawable();
        this.drawablePress = new GradientDrawable();
        this.drawableNormal.setCornerRadius((float)ConvertUtils.dip2px(this.getContext(), info.buttonStyle.cornerRadius));
        this.drawableNormal.setStroke(ConvertUtils.dip2px(this.getContext(), info.buttonStyle.strokeWidth), Color.parseColor((String)info.buttonStyle.strokeColor));
        this.drawableNormal.setColor(Color.parseColor((String)info.buttonStyle.fillColor));
        this.drawablePress.setCornerRadius((float)ConvertUtils.dip2px(this.getContext(), info.buttonStyle.cornerRadiusPress));
        this.drawablePress.setStroke(ConvertUtils.dip2px(this.getContext(), info.buttonStyle.strokeWidthPress), Color.parseColor((String)info.buttonStyle.strokeColorPress));
        this.drawablePress.setColor(Color.parseColor((String)info.buttonStyle.fillColorPress));
        this.setText(info.text);
        this.setGravity(17);
        this.setPadding(0, 0, 0, 0);
        this.setAllCaps(false);
        this.setNormalDrawable();
    }

    public void setNormalDrawable() {
        this.setTextSize(this.info.buttonStyle.textSize);
        this.setTextColor(Color.parseColor((String)this.info.buttonStyle.textColor));
        this.setBackground((Drawable)this.drawableNormal);
    }

    public void setPressDrawable() {
        this.setTextSize(this.info.buttonStyle.textSizePress);
        this.setTextColor(Color.parseColor((String)this.info.buttonStyle.textColorPress));
        this.setBackground((Drawable)this.drawablePress);
    }

    public void updateSizeAndPosition(BaseButtonInfo info) {
        int height;
        int width;
        this.info = info;
        if (info.sizeType == 0) {
            width = info.width.object == 0 ? (int)((float)this.screenWidth * info.width.percentSize) : (int)((float)this.screenHeight * info.width.percentSize);
            height = info.height.object == 0 ? (int)((float)this.screenWidth * info.height.percentSize) : (int)((float)this.screenHeight * info.height.percentSize);
        } else {
            width = ConvertUtils.dip2px(this.getContext(), info.width.absoluteSize);
            height = ConvertUtils.dip2px(this.getContext(), info.height.absoluteSize);
        }
        ViewGroup.LayoutParams layoutParams = this.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        this.setLayoutParams(layoutParams);
        if (info.positionType == 0) {
            this.setX((float)(this.screenWidth - width) * info.xPosition.percentPosition);
            this.setY((float)(this.screenHeight - height) * info.yPosition.percentPosition);
        } else {
            this.setX(ConvertUtils.dip2px(this.getContext(), info.xPosition.absolutePosition));
            this.setY(ConvertUtils.dip2px(this.getContext(), info.yPosition.absolutePosition));
        }
    }

    public void saveButtonInfo() {
        if (this.menuHelper.editMode) {
            ChildLayout childLayout = null;
            for (ChildLayout child : SettingUtils.getChildList(this.info.pattern)) {
                if (!child.name.equals(this.menuHelper.currentChild)) continue;
                childLayout = child;
            }
            assert (childLayout != null);
            boolean exist = false;
            for (int i = 0; i < childLayout.baseButtonList.size(); ++i) {
                if (!childLayout.baseButtonList.get((int)i).uuid.equals(this.info.uuid)) continue;
                childLayout.baseButtonList.get(i).refresh(this.info);
                exist = true;
            }
            if (!exist) {
                childLayout.baseButtonList.add(this.info);
            }
            ChildLayout.saveChildLayout(this.info.pattern, childLayout);
        }
    }

    public void deleteButton() {
        if (this.menuHelper.editMode) {
            ChildLayout childLayout = null;
            for (ChildLayout child : SettingUtils.getChildList(this.info.pattern)) {
                if (!child.name.equals(this.menuHelper.currentChild)) continue;
                childLayout = child;
            }
            assert (childLayout != null);
            for (int i = 0; i < childLayout.baseButtonList.size(); ++i) {
                if (!childLayout.baseButtonList.get((int)i).uuid.equals(this.info.uuid)) continue;
                childLayout.baseButtonList.remove(i);
                break;
            }
            ChildLayout.saveChildLayout(this.info.pattern, childLayout);
            this.menuHelper.viewManager.layoutPanel.removeView((View)this);
        }
    }
}

