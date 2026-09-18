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
 *  android.view.MotionEvent
 *  android.view.View
 *  android.view.ViewGroup$LayoutParams
 */
package com.qcl.launcher.control.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class RockerView
extends View {
    private String pointerColor = "#f6f6f6";
    private String pointerColorPress = "#40ffffff";
    private int followType = 0;
    private boolean doubleClick = true;
    private OnShakeListener onShakeListener;
    private State center = State.NORMAL;
    private State up = State.NORMAL;
    private State down = State.NORMAL;
    private State left = State.NORMAL;
    private State right = State.NORMAL;
    private State upLeft = State.HIDE;
    private State downLeft = State.HIDE;
    private State upRight = State.HIDE;
    private State downRight = State.HIDE;
    private static final double ANGLE_0 = 0.0;
    private static final double ANGLE_360 = 360.0;
    private static final double ANGLE_8D_OF_0P = 22.5;
    private static final double ANGLE_8D_OF_1P = 67.5;
    private static final double ANGLE_8D_OF_2P = 112.5;
    private static final double ANGLE_8D_OF_3P = 157.5;
    private static final double ANGLE_8D_OF_4P = 202.5;
    private static final double ANGLE_8D_OF_5P = 247.5;
    private static final double ANGLE_8D_OF_6P = 292.5;
    private static final double ANGLE_8D_OF_7P = 337.5;
    private Direction tempDirection = Direction.DIRECTION_CENTER;
    private boolean touching = false;
    private int clickCount = 0;
    private long firstClickTime;
    private float initialPositionX;
    private float initialPositionY;

    public RockerView(Context context) {
        super(context);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @SuppressLint(value={"DrawAllocation"})
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Path centerPointerPath = new Path();
        Path upPointerPath = new Path();
        Path downPointerPath = new Path();
        Path leftPointerPath = new Path();
        Path rightPointerPath = new Path();
        Path upLeftPointerPath = new Path();
        Path upRightPointerPath = new Path();
        Path downLeftPointerPath = new Path();
        Path downRightPointerPath = new Path();
        centerPointerPath.moveTo((float)(4 * this.getWidth() / 10), (float)(this.getHeight() / 2));
        centerPointerPath.lineTo((float)(this.getWidth() / 2), (float)(4 * this.getHeight() / 10));
        centerPointerPath.lineTo((float)(6 * this.getWidth() / 10), (float)(this.getHeight() / 2));
        centerPointerPath.lineTo((float)(this.getWidth() / 2), (float)(6 * this.getHeight() / 10));
        centerPointerPath.lineTo((float)(4 * this.getWidth() / 10), (float)(this.getHeight() / 2));
        upPointerPath.moveTo((float)(this.getWidth() / 2), (float)(this.getHeight() / 10));
        upPointerPath.lineTo((float)(this.getWidth() / 2 - this.getWidth() / 10), (float)(2 * this.getHeight() / 10));
        upPointerPath.lineTo((float)(this.getWidth() / 2 + this.getWidth() / 10), (float)(2 * this.getHeight() / 10));
        downPointerPath.moveTo((float)(this.getWidth() / 2), (float)(9 * this.getHeight() / 10));
        downPointerPath.lineTo((float)(this.getWidth() / 2 - this.getWidth() / 10), (float)(8 * this.getHeight() / 10));
        downPointerPath.lineTo((float)(this.getWidth() / 2 + this.getWidth() / 10), (float)(8 * this.getHeight() / 10));
        leftPointerPath.moveTo((float)(this.getWidth() / 10), (float)(this.getHeight() / 2));
        leftPointerPath.lineTo((float)(2 * (this.getWidth() / 10)), (float)(this.getHeight() / 2 - this.getHeight() / 10));
        leftPointerPath.lineTo((float)(2 * (this.getWidth() / 10)), (float)(this.getHeight() / 2 + this.getHeight() / 10));
        rightPointerPath.moveTo((float)(9 * (this.getWidth() / 10)), (float)(this.getHeight() / 2));
        rightPointerPath.lineTo((float)(8 * (this.getWidth() / 10)), (float)(this.getHeight() / 2 - this.getHeight() / 10));
        rightPointerPath.lineTo((float)(8 * (this.getWidth() / 10)), (float)(this.getHeight() / 2 + this.getHeight() / 10));
        upLeftPointerPath.moveTo((float)(2 * (this.getWidth() / 10)), (float)(2 * (this.getHeight() / 10)));
        upLeftPointerPath.lineTo((float)(3 * (this.getWidth() / 10)), (float)(2 * (this.getHeight() / 10)));
        upLeftPointerPath.lineTo((float)(2 * (this.getWidth() / 10)), (float)(3 * (this.getHeight() / 10)));
        downLeftPointerPath.moveTo((float)(2 * (this.getWidth() / 10)), (float)(8 * (this.getHeight() / 10)));
        downLeftPointerPath.lineTo((float)(3 * (this.getWidth() / 10)), (float)(8 * (this.getHeight() / 10)));
        downLeftPointerPath.lineTo((float)(2 * (this.getWidth() / 10)), (float)(7 * (this.getHeight() / 10)));
        upRightPointerPath.moveTo((float)(8 * (this.getWidth() / 10)), (float)(2 * (this.getHeight() / 10)));
        upRightPointerPath.lineTo((float)(7 * (this.getWidth() / 10)), (float)(2 * (this.getHeight() / 10)));
        upRightPointerPath.lineTo((float)(8 * (this.getWidth() / 10)), (float)(3 * (this.getHeight() / 10)));
        downRightPointerPath.moveTo((float)(8 * (this.getWidth() / 10)), (float)(8 * (this.getHeight() / 10)));
        downRightPointerPath.lineTo((float)(7 * (this.getWidth() / 10)), (float)(8 * (this.getHeight() / 10)));
        downRightPointerPath.lineTo((float)(8 * (this.getWidth() / 10)), (float)(7 * (this.getHeight() / 10)));
        Paint pointerPaint = new Paint();
        pointerPaint.setAntiAlias(true);
        pointerPaint.setColor(Color.parseColor((String)this.pointerColor));
        pointerPaint.setStyle(Paint.Style.FILL);
        Paint pointerPaintPress = new Paint();
        pointerPaintPress.setAntiAlias(true);
        pointerPaintPress.setColor(Color.parseColor((String)this.pointerColorPress));
        pointerPaintPress.setStyle(Paint.Style.FILL);
        if (this.center == State.NORMAL) {
            canvas.drawPath(centerPointerPath, pointerPaint);
        }
        if (this.up == State.NORMAL) {
            canvas.drawPath(upPointerPath, pointerPaint);
        }
        if (this.down == State.NORMAL) {
            canvas.drawPath(downPointerPath, pointerPaint);
        }
        if (this.left == State.NORMAL) {
            canvas.drawPath(leftPointerPath, pointerPaint);
        }
        if (this.right == State.NORMAL) {
            canvas.drawPath(rightPointerPath, pointerPaint);
        }
        if (this.upLeft == State.NORMAL) {
            canvas.drawPath(upLeftPointerPath, pointerPaint);
        }
        if (this.upRight == State.NORMAL) {
            canvas.drawPath(upRightPointerPath, pointerPaint);
        }
        if (this.downLeft == State.NORMAL) {
            canvas.drawPath(downLeftPointerPath, pointerPaint);
        }
        if (this.downRight == State.NORMAL) {
            canvas.drawPath(downRightPointerPath, pointerPaint);
        }
        if (this.center == State.PRESS) {
            canvas.drawPath(centerPointerPath, pointerPaintPress);
        }
        if (this.up == State.PRESS) {
            canvas.drawPath(upPointerPath, pointerPaintPress);
        }
        if (this.down == State.PRESS) {
            canvas.drawPath(downPointerPath, pointerPaintPress);
        }
        if (this.left == State.PRESS) {
            canvas.drawPath(leftPointerPath, pointerPaintPress);
        }
        if (this.right == State.PRESS) {
            canvas.drawPath(rightPointerPath, pointerPaintPress);
        }
        if (this.upLeft == State.PRESS) {
            canvas.drawPath(upLeftPointerPath, pointerPaintPress);
        }
        if (this.upRight == State.PRESS) {
            canvas.drawPath(upRightPointerPath, pointerPaintPress);
        }
        if (this.downLeft == State.PRESS) {
            canvas.drawPath(downLeftPointerPath, pointerPaintPress);
        }
        if (this.downRight == State.PRESS) {
            canvas.drawPath(downRightPointerPath, pointerPaintPress);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case 0: {
                float centerX = this.getWidth() / 2;
                float centerY = this.getHeight() / 2;
                if (!(this.calculateDistance(event.getX(), event.getY(), centerX, centerY) <= (double)(this.getWidth() / 2))) break;
                this.touching = true;
                if (this.onShakeListener != null) {
                    this.onShakeListener.onTouch(this);
                }
                this.initialPositionX = this.getX();
                this.initialPositionY = this.getY();
                if (this.followType == 1 && this.calculateDistance(event.getX(), event.getY(), centerX, centerY) <= (double)(this.getWidth() / 6) || this.followType == 2) {
                    this.setX(this.initialPositionX + event.getX() - centerX);
                    this.setY(this.initialPositionY + event.getY() - centerY);
                }
                if (this.calculateDistance(event.getX(), event.getY(), centerX, centerY) <= (double)(this.getWidth() / 6)) {
                    this.tempDirection = Direction.DIRECTION_CENTER;
                    this.center = State.PRESS;
                    this.up = State.NORMAL;
                    this.down = State.NORMAL;
                    this.left = State.NORMAL;
                    this.right = State.NORMAL;
                    this.upLeft = State.HIDE;
                    this.downLeft = State.HIDE;
                    this.upRight = State.HIDE;
                    this.downRight = State.HIDE;
                    if (this.onShakeListener != null) {
                        this.onShakeListener.onShake(this, this.tempDirection);
                    }
                }
                this.refreshView(event);
                if (!(this.calculateDistance(event.getX(), event.getY(), centerX, centerY) <= (double)(this.getWidth() / 6)) && this.followType != 2 || !this.doubleClick) break;
                ++this.clickCount;
                if (this.clickCount == 1) {
                    this.firstClickTime = System.currentTimeMillis();
                }
                if (this.clickCount != 2) break;
                if (System.currentTimeMillis() - this.firstClickTime <= 500L) {
                    if (this.onShakeListener != null) {
                        this.onShakeListener.onCenterDoubleClick(this);
                    }
                    this.clickCount = 0;
                    break;
                }
                this.firstClickTime = System.currentTimeMillis();
                this.clickCount = 1;
                break;
            }
            case 2: {
                if (!this.touching) break;
                this.refreshView(event);
                break;
            }
            case 1: 
            case 3: {
                if (!this.touching) break;
                this.center = State.NORMAL;
                this.up = State.NORMAL;
                this.down = State.NORMAL;
                this.left = State.NORMAL;
                this.right = State.NORMAL;
                this.upLeft = State.HIDE;
                this.downLeft = State.HIDE;
                this.upRight = State.HIDE;
                this.downRight = State.HIDE;
                if (this.tempDirection != Direction.DIRECTION_CENTER) {
                    this.tempDirection = Direction.DIRECTION_CENTER;
                    if (this.onShakeListener != null) {
                        this.onShakeListener.onShake(this, this.tempDirection);
                    }
                }
                if (this.followType != 0) {
                    this.setX(this.initialPositionX);
                    this.setY(this.initialPositionY);
                }
                if (this.onShakeListener != null) {
                    this.onShakeListener.onFinish(this);
                }
                this.touching = false;
            }
        }
        return true;
    }

    public void setSize(int size) {
        ViewGroup.LayoutParams params = this.getLayoutParams();
        params.width = size;
        params.height = size;
        this.setLayoutParams(params);
    }

    public void setPointerColor(String pointerColor) {
        this.pointerColor = pointerColor;
    }

    public void setPointerColorPress(String pointerColorPress) {
        this.pointerColorPress = pointerColorPress;
    }

    public void setFollowType(int followType) {
        this.followType = followType;
    }

    public void setDoubleClick(boolean doubleClick) {
        this.doubleClick = doubleClick;
    }

    public void setOnShakeListener(OnShakeListener onShakeListener) {
        this.onShakeListener = onShakeListener;
    }

    private void refreshView(MotionEvent event) {
        float centerX = this.getWidth() / 2;
        float centerY = this.getHeight() / 2;
        if (this.calculateDistance(event.getX(), event.getY(), centerX, centerY) <= (double)(this.getWidth() / 6)) {
            if (this.tempDirection != Direction.DIRECTION_CENTER) {
                this.tempDirection = Direction.DIRECTION_CENTER;
                this.center = State.PRESS;
                this.up = State.NORMAL;
                this.down = State.NORMAL;
                this.left = State.NORMAL;
                this.right = State.NORMAL;
                this.upLeft = State.HIDE;
                this.downLeft = State.HIDE;
                this.upRight = State.HIDE;
                this.downRight = State.HIDE;
                if (this.onShakeListener != null) {
                    this.onShakeListener.onShake(this, this.tempDirection);
                }
            }
        } else {
            this.setDirection(event);
        }
    }

    private double calculateDistance(float xPri, float yPri, float xSec, float ySec) {
        float d = (xPri - xSec) * (xPri - xSec) + (yPri - ySec) * (yPri - ySec);
        return Math.sqrt(d);
    }

    private double radian2Angle(double radian) {
        double tmp = Math.round(radian / Math.PI * 180.0);
        return tmp >= 0.0 ? tmp : 360.0 + tmp;
    }

    private void setDirection(MotionEvent event) {
        float lenY;
        float lenXY;
        float centerX = this.getWidth() / 2;
        float centerY = this.getHeight() / 2;
        float lenX = event.getX() - centerX;
        double radian = Math.acos(lenX / (lenXY = (float)Math.sqrt(lenX * lenX + (lenY = event.getY() - centerY) * lenY))) * (double)(event.getY() < centerY ? -1 : 1);
        double angle = this.radian2Angle(radian);
        if ((0.0 <= angle && 22.5 > angle || 337.5 <= angle && 360.0 > angle) && this.tempDirection != Direction.DIRECTION_RIGHT) {
            this.tempDirection = Direction.DIRECTION_RIGHT;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.NORMAL;
            this.left = State.NORMAL;
            this.right = State.PRESS;
            this.upLeft = State.HIDE;
            this.downLeft = State.HIDE;
            this.upRight = State.NORMAL;
            this.downRight = State.NORMAL;
        } else if (22.5 <= angle && 67.5 > angle && this.tempDirection != Direction.DIRECTION_DOWN_RIGHT) {
            this.tempDirection = Direction.DIRECTION_DOWN_RIGHT;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.NORMAL;
            this.left = State.NORMAL;
            this.right = State.NORMAL;
            this.downRight = State.PRESS;
        } else if (67.5 <= angle && 112.5 > angle && this.tempDirection != Direction.DIRECTION_DOWN) {
            this.tempDirection = Direction.DIRECTION_DOWN;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.PRESS;
            this.left = State.NORMAL;
            this.right = State.NORMAL;
            this.upLeft = State.HIDE;
            this.downLeft = State.NORMAL;
            this.upRight = State.HIDE;
            this.downRight = State.NORMAL;
        } else if (112.5 <= angle && 157.5 > angle && this.tempDirection != Direction.DIRECTION_DOWN_LEFT) {
            this.tempDirection = Direction.DIRECTION_DOWN_LEFT;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.NORMAL;
            this.left = State.NORMAL;
            this.right = State.NORMAL;
            this.downLeft = State.PRESS;
        } else if (157.5 <= angle && 202.5 > angle && this.tempDirection != Direction.DIRECTION_LEFT) {
            this.tempDirection = Direction.DIRECTION_LEFT;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.NORMAL;
            this.left = State.PRESS;
            this.right = State.NORMAL;
            this.upLeft = State.NORMAL;
            this.downLeft = State.NORMAL;
            this.upRight = State.HIDE;
            this.downRight = State.HIDE;
        } else if (202.5 <= angle && 247.5 > angle && this.tempDirection != Direction.DIRECTION_UP_LEFT) {
            this.tempDirection = Direction.DIRECTION_UP_LEFT;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.NORMAL;
            this.left = State.NORMAL;
            this.right = State.NORMAL;
            this.upLeft = State.PRESS;
        } else if (247.5 <= angle && 292.5 > angle && this.tempDirection != Direction.DIRECTION_UP) {
            this.tempDirection = Direction.DIRECTION_UP;
            this.center = State.NORMAL;
            this.up = State.PRESS;
            this.down = State.NORMAL;
            this.left = State.NORMAL;
            this.right = State.NORMAL;
            this.upLeft = State.NORMAL;
            this.downLeft = State.HIDE;
            this.upRight = State.NORMAL;
            this.downRight = State.HIDE;
        } else if (292.5 <= angle && 337.5 > angle && this.tempDirection != Direction.DIRECTION_UP_RIGHT) {
            this.tempDirection = Direction.DIRECTION_UP_RIGHT;
            this.center = State.NORMAL;
            this.up = State.NORMAL;
            this.down = State.NORMAL;
            this.left = State.NORMAL;
            this.right = State.NORMAL;
            this.upRight = State.PRESS;
        }
        if (this.onShakeListener != null) {
            this.onShakeListener.onShake(this, this.tempDirection);
        }
    }

    public static enum State {
        NORMAL,
        PRESS,
        HIDE;

    }

    public static enum Direction {
        DIRECTION_LEFT,
        DIRECTION_RIGHT,
        DIRECTION_UP,
        DIRECTION_DOWN,
        DIRECTION_UP_LEFT,
        DIRECTION_UP_RIGHT,
        DIRECTION_DOWN_LEFT,
        DIRECTION_DOWN_RIGHT,
        DIRECTION_CENTER;

    }

    public static interface OnShakeListener {
        public void onTouch(RockerView var1);

        public void onShake(RockerView var1, Direction var2);

        public void onCenterDoubleClick(RockerView var1);

        public void onFinish(RockerView var1);
    }
}

