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
 *  android.os.Message
 *  android.os.Vibrator
 *  android.view.MotionEvent
 *  androidx.annotation.NonNull
 *  androidx.appcompat.app.AlertDialog
 *  androidx.appcompat.app.AlertDialog$Builder
 */
package com.qcl.launcher.control.view;

import com.qcl.launcher.utils.QclColors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.qcl.launcher.control.InputBridge;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.control.bean.BaseRockerViewInfo;
import com.qcl.launcher.control.view.RockerView;
import com.qcl.launcher.launcher.dialogs.control.EditRockerDialog;
import com.qcl.launcher.launcher.list.local.controller.ChildLayout;
import com.qcl.launcher.launcher.setting.SettingUtils;
import com.qcl.launcher.utils.convert.ConvertUtils;

import com.qcl.launcher.R;
@SuppressLint(value={"ViewConstructor"})
public class BaseRockerView
extends RockerView {
    public int screenWidth;
    public int screenHeight;
    public BaseRockerViewInfo info;
    public MenuHelper menuHelper;
    public GradientDrawable drawableNormal;
    public GradientDrawable drawablePress;
    private long downTime;
    private float initialX;
    private float initialY;
    private float initialPositionX;
    private float initialPositionY;
    private boolean shiftMode = false;
    private boolean isShowing = true;
    private final Paint outlinePaint;
    private final Handler deleteHandler = new Handler();
    private final Runnable deleteRunnable = () -> {
        Vibrator vibrator = (Vibrator)this.getContext().getSystemService("vibrator");
        vibrator.vibrate(100L);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle((CharSequence)this.getContext().getString(R.string.dialog_delete_rocker_title));
        builder.setMessage((CharSequence)this.getContext().getString(R.string.dialog_delete_rocker_content));
        builder.setPositiveButton((CharSequence)this.getContext().getString(R.string.dialog_delete_rocker_positive), (dialogInterface, i) -> this.deleteRocker());
        builder.setNegativeButton((CharSequence)this.getContext().getString(R.string.dialog_delete_rocker_negative), (dialogInterface, i) -> {});
        AlertDialog dialog = builder.create();
        dialog.show();
    };
    @SuppressLint(value={"HandlerLeak"})
    public final Handler handler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                // empty if block
            }
            if (msg.what == 1) {
                // empty if block
            }
        }
    };

    public BaseRockerView(Context context, int screenWidth, int screenHeight, BaseRockerViewInfo info, MenuHelper menuHelper) {
        super(context);
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.menuHelper = menuHelper;
        this.outlinePaint = new Paint();
        this.outlinePaint.setAntiAlias(true);
        this.outlinePaint.setColor(this.getContext().getColor(R.color.colorRed));
        this.outlinePaint.setStyle(Paint.Style.STROKE);
        this.outlinePaint.setStrokeWidth(3.0f);
        this.refreshInfo(info);
    }

    @Override
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.menuHelper.editMode) {
            switch (event.getActionMasked()) {
                case 0: {
                    // ★★★ 2026-09-19：改用屏幕绝对坐标 —— event.getX()/getY() 是「相对本 View」的坐标，
                    //   而拖动时 View 自身跟着手指移动 → 相对坐标差恒 ≈0 → 拖不动/松手被误判成点击
                    //   （回弹原位 + 弹编辑对话框）。与 BaseButton 同一处病根。
                    this.downTime = System.currentTimeMillis();
                    this.initialX = event.getRawX();
                    this.initialY = event.getRawY();
                    this.initialPositionX = this.getX();
                    this.initialPositionY = this.getY();
                    this.deleteHandler.postDelayed(this.deleteRunnable, 600L);
                    this.menuHelper.viewManager.layoutPanel.showReference(this.info.positionType, this.getX(), this.getY(), this.getWidth(), this.getHeight());
                    break;
                }
                case 2: {
                    float maxX = (float) Math.max(0, this.screenWidth - this.getWidth());
                    float maxY = (float) Math.max(0, this.screenHeight - this.getHeight());
                    float dx = event.getRawX() - this.initialX;
                    float dy = event.getRawY() - this.initialY;
                    float rawX = this.initialPositionX + dx;
                    float rawY = this.initialPositionY + dy;
                    float targetX = rawX < 0.0f ? 0.0f : (rawX > maxX ? maxX : rawX);
                    float targetY = rawY < 0.0f ? 0.0f : (rawY > maxY ? maxY : rawY);
                    this.setX(targetX);
                    this.setY(targetY);
                    this.info.xPosition.absolutePosition = ConvertUtils.px2dip(this.getContext(), targetX);
                    this.info.yPosition.absolutePosition = ConvertUtils.px2dip(this.getContext(), targetY);
                    this.info.xPosition.percentPosition = maxX > 0f ? targetX / maxX : 0f;
                    this.info.yPosition.percentPosition = maxY > 0f ? targetY / maxY : 0f;
                    this.saveRockerInfo();
                    this.menuHelper.viewManager.layoutPanel.showReference(this.info.positionType, this.getX(), this.getY(), this.getWidth(), this.getHeight());
                    if (Math.abs(dx) > 3.0f || Math.abs(dy) > 3.0f) {
                        this.deleteHandler.removeCallbacks(this.deleteRunnable);
                    }
                    break;
                }
                case 1: 
                case 3: {
                    this.deleteHandler.removeCallbacks(this.deleteRunnable);
                    float totalDx = Math.abs(event.getRawX() - this.initialX);
                    float totalDy = Math.abs(event.getRawY() - this.initialY);
                    if (System.currentTimeMillis() - this.downTime <= 200L && totalDx <= 10.0f && totalDy <= 10.0f) {
                        // 确实是点击（没拖动）→ 位置不动，弹编辑对话框；★ 不再回弹到 initialPosition
                        EditRockerDialog dialog = new EditRockerDialog(this.getContext(), this.menuHelper.viewManager, this.info.pattern, this.info.child, this.screenWidth, this.screenHeight, this, this.menuHelper.fullscreen);
                        dialog.show();
                    }
                    // 拖动过 → 保持 MOVE 期间写入并保存的新位置
                    this.menuHelper.viewManager.layoutPanel.hideReference();
                }
            }
        }
        return super.onTouchEvent(event);
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
        // ★★★ 编辑模式：只显示「当前选中子布局(currentChild)」的控件，
        // 游戏布局 / 键盘布局二选一（玩家在子布局下拉框切换），其余布局一律隐藏。
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

    public void refreshInfo(final BaseRockerViewInfo info) {
        this.info = info;
        this.drawableNormal = new GradientDrawable();
        this.drawablePress = new GradientDrawable();
        this.drawableNormal.setCornerRadius((float)ConvertUtils.dip2px(this.getContext(), info.rockerStyle.cornerRadius));
        this.drawableNormal.setStroke(ConvertUtils.dip2px(this.getContext(), info.rockerStyle.strokeWidth), QclColors.parseSafe(info.rockerStyle.strokeColor, 0x33555555));
        this.drawableNormal.setColor(QclColors.parseSafe(info.rockerStyle.fillColor, 0x666E6E6E));
        this.drawablePress.setCornerRadius((float)ConvertUtils.dip2px(this.getContext(), info.rockerStyle.cornerRadiusPress));
        this.drawablePress.setStroke(ConvertUtils.dip2px(this.getContext(), info.rockerStyle.strokeWidthPress), QclColors.parseSafe(info.rockerStyle.strokeColorPress, 0x55555555));
        this.drawablePress.setColor(QclColors.parseSafe(info.rockerStyle.fillColorPress, 0x995E5E5E));
        this.setPointerColor(info.rockerStyle.pointerColor);
        this.setPointerColorPress(info.rockerStyle.pointerColorPress);
        this.setFollowType(info.followType);
        this.setDoubleClick(info.shift);
        this.setOnShakeListener(new RockerView.OnShakeListener(){

            @Override
            public void onTouch(RockerView view) {
                if (BaseRockerView.this.menuHelper.editMode) {
                    BaseRockerView.this.setFollowType(0);
                    BaseRockerView.this.setDoubleClick(false);
                } else {
                    BaseRockerView.this.setFollowType(info.followType);
                    BaseRockerView.this.setDoubleClick(info.shift);
                }
                BaseRockerView.this.setPressDrawable();
            }

            @Override
            public void onShake(RockerView view, RockerView.Direction direction) {
                if (!BaseRockerView.this.menuHelper.editMode) {
                    BaseRockerView.this.getDirectionEvent(direction);
                }
            }

            @Override
            public void onCenterDoubleClick(RockerView view) {
                if (!BaseRockerView.this.menuHelper.editMode) {
                    if (BaseRockerView.this.shiftMode) {
                        InputBridge.sendEvent(BaseRockerView.this.menuHelper.launcher, 340, false);
                    } else {
                        InputBridge.sendEvent(BaseRockerView.this.menuHelper.launcher, 340, true);
                    }
                    BaseRockerView.this.shiftMode = !BaseRockerView.this.shiftMode;
                }
            }

            @Override
            public void onFinish(RockerView view) {
                BaseRockerView.this.setNormalDrawable();
                BaseRockerView.this.setFollowType(info.followType);
                BaseRockerView.this.setDoubleClick(info.shift);
            }
        });
        this.setNormalDrawable();
    }

    public void setNormalDrawable() {
        this.setBackground((Drawable)this.drawableNormal);
    }

    public void setPressDrawable() {
        this.setBackground((Drawable)this.drawablePress);
    }

    public void getDirectionEvent(RockerView.Direction direction) {
        // ★★★ 1.1.4 修复：双击方向键开启疾跑（shiftMode）后，攻击/短暂松手会让摇杆回中、
        //   从而发送「松开 WASD」，游戏内疾跑键实际已断，但 shiftMode 还以为是开的（状态不同步）。
        //   这里在每次方向事件里，只要 shiftMode 仍为 true，就重新按住疾跑键(340)，
        //   让"锁定疾跑"在攻击/移动后不丢失。
        if (this.shiftMode) {
            InputBridge.sendEvent(this.menuHelper.launcher, 340, true);
        }
        switch (direction) {
            case DIRECTION_CENTER: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, false);
                break;
            }
            case DIRECTION_UP: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, false);
                break;
            }
            case DIRECTION_DOWN: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, false);
                break;
            }
            case DIRECTION_LEFT: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, false);
                break;
            }
            case DIRECTION_RIGHT: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, true);
                break;
            }
            case DIRECTION_UP_LEFT: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, false);
                break;
            }
            case DIRECTION_UP_RIGHT: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, true);
                break;
            }
            case DIRECTION_DOWN_LEFT: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, false);
                break;
            }
            case DIRECTION_DOWN_RIGHT: {
                InputBridge.sendEvent(this.menuHelper.launcher, 87, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 65, false);
                InputBridge.sendEvent(this.menuHelper.launcher, 83, true);
                InputBridge.sendEvent(this.menuHelper.launcher, 68, true);
            }
        }
    }

    public void updateSizeAndPosition(BaseRockerViewInfo info) {
        this.info = info;
        int size = info.sizeType == 0 ? (info.size.object == 0 ? (int)((float)this.screenWidth * info.size.percentSize) : (int)((float)this.screenHeight * info.size.percentSize)) : ConvertUtils.dip2px(this.getContext(), info.size.absoluteSize);
        this.setSize(size);
        if (info.positionType == 0) {
            this.setX((float)(this.screenWidth - size) * info.xPosition.percentPosition);
            this.setY((float)(this.screenHeight - size) * info.yPosition.percentPosition);
        } else {
            this.setX(ConvertUtils.dip2px(this.getContext(), info.xPosition.absolutePosition));
            this.setY(ConvertUtils.dip2px(this.getContext(), info.yPosition.absolutePosition));
        }
    }

    public void saveRockerInfo() {
        if (this.menuHelper.editMode) {
            ChildLayout childLayout = null;
            for (ChildLayout child : SettingUtils.getChildList(this.info.pattern)) {
                if (!child.name.equals(this.menuHelper.currentChild)) continue;
                childLayout = child;
            }
            assert (childLayout != null);
            boolean exist = false;
            for (int i = 0; i < childLayout.baseRockerViewList.size(); ++i) {
                if (!childLayout.baseRockerViewList.get((int)i).uuid.equals(this.info.uuid)) continue;
                childLayout.baseRockerViewList.get(i).refresh(this.info);
                exist = true;
            }
            if (!exist) {
                childLayout.baseRockerViewList.add(this.info);
            }
            ChildLayout.saveChildLayout(this.info.pattern, childLayout);
        }
    }

    public void deleteRocker() {
        if (this.menuHelper.editMode) {
            ChildLayout childLayout = null;
            for (ChildLayout child : SettingUtils.getChildList(this.info.pattern)) {
                if (!child.name.equals(this.menuHelper.currentChild)) continue;
                childLayout = child;
            }
            assert (childLayout != null);
            for (int i = 0; i < childLayout.baseRockerViewList.size(); ++i) {
                if (!childLayout.baseRockerViewList.get((int)i).uuid.equals(this.info.uuid)) continue;
                childLayout.baseRockerViewList.remove(i);
                break;
            }
            ChildLayout.saveChildLayout(this.info.pattern, childLayout);
            this.menuHelper.viewManager.layoutPanel.removeView(this);
        }
    }
}

