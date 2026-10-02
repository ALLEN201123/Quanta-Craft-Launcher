package com.qcl.launcher.control.view;

import com.qcl.launcher.utils.QclColors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * ★★★ 1.4.2 重写：真·模拟摇杆（不再是「基岩版十字键」）。
 *
 * 改造要点（照 FCL control/view/ControlDirection.java 的 ROCKER 分支）：
 *  1. 视觉 = 「圆形底盘 + 可自由拖动的圆形杆头」，杆头在底盘半径内连续移动；
 *     原来画的是 8 个三角形箭头（中心/上下左右/四斜角），天然长得像十字键 → 已删除。
 *  2. 输入 = 连续角度判定（8 方向分段），但方向分段逻辑与原来一致，
 *     保证 W/A/S/D 的发送语义不变（推杆向上 = 前进 W）。
 *  3. 移除「双击中心 = 蹲下/潜行」：原来 onCenterDoubleClick 会 toggle Shift(340)，
 *     移动中容易误触导致突然蹲下 → 现在不再触发任何按键。
 *     （保留 onCenterDoubleClick 回调接口本身，方便以后接别的功能，
 *      但 BaseRockerView 里已不再拿它发 Shift。）
 *  4. 杆头回中：松手后杆头动画回到圆心。
 */
public class RockerView extends View {

    private String pointerColor = "#f6f6f6";
    private String pointerColorPress = "#40ffffff";
    private int followType = 0;
    private boolean doubleClick = true;
    private OnShakeListener onShakeListener;

    /** 当前摇杆方向 */
    private Direction tempDirection = Direction.DIRECTION_CENTER;
    private boolean touching = false;
    private int clickCount = 0;
    private long firstClickTime;
    private float initialPositionX;
    private float initialPositionY;

    /** ★ 杆头中心相对本 View 左上角的坐标（本 View 即整个底盘区域） */
    private float knobX = -1.0f;
    private float knobY = -1.0f;
    /** ★ 底盘（背景圆）与杆头（knob）的绘制参数，由 BaseRockerView 注入 */
    private int chassisStrokeWidth = 2;
    private int chassisStrokeColor = 0x33555555;
    private int chassisFillColor = 0x666E6E6E;
    private int knobStrokeWidth = 2;
    private int knobStrokeColor = 0x55555555;
    private int knobFillColor = 0x995E5E5E;
    /** 杆头直径占底盘的比例（0~1），默认 0.42（FCL 默认约 400/1000） */
    private float knobScale = 0.42f;

    private static final double ANGLE_8D_OF_0P = 22.5;
    private static final double ANGLE_8D_OF_1P = 67.5;
    private static final double ANGLE_8D_OF_2P = 112.5;
    private static final double ANGLE_8D_OF_3P = 157.5;
    private static final double ANGLE_8D_OF_4P = 202.5;
    private static final double ANGLE_8D_OF_5P = 247.5;
    private static final double ANGLE_8D_OF_6P = 292.5;
    private static final double ANGLE_8D_OF_7P = 337.5;

    public RockerView(Context context) {
        super(context);
        setClickable(true);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    // ==================== 绘制：圆盘 + 圆杆头 ====================

    @SuppressLint(value = {"DrawAllocation"})
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = this.getWidth();
        float h = this.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        float cx = w / 2.0f;
        float cy = h / 2.0f;
        float chassisRadius = Math.min(w, h) / 2.0f;

        // ① 底盘：一个圆（带描边 + 填充）
        Paint chassisFill = new Paint();
        chassisFill.setAntiAlias(true);
        chassisFill.setStyle(Paint.Style.FILL);
        chassisFill.setColor(this.chassisFillColor);
        canvas.drawCircle(cx, cy, chassisRadius - this.chassisStrokeWidth / 2.0f, chassisFill);

        Paint chassisStroke = new Paint();
        chassisStroke.setAntiAlias(true);
        chassisStroke.setStyle(Paint.Style.STROKE);
        chassisStroke.setStrokeWidth(this.chassisStrokeWidth);
        chassisStroke.setColor(this.chassisStrokeColor);
        canvas.drawCircle(cx, cy, chassisRadius - this.chassisStrokeWidth / 2.0f, chassisStroke);

        // ② 十字基准（淡线，仅作视觉参考，不是「十字键」本体）
        //    用极淡的颜色画一个「+」，让玩家知道这是摇杆区域，也不会像箭头那样喧宾夺主。
        Paint guide = new Paint();
        guide.setAntiAlias(true);
        guide.setStyle(Paint.Style.STROKE);
        guide.setStrokeWidth(Math.max(1.0f, this.chassisStrokeWidth * 0.6f));
        int guideColor = (this.chassisStrokeColor & 0x00FFFFFF) | 0x33000000;
        guide.setColor(guideColor);
        float guideLen = chassisRadius * 0.72f;
        canvas.drawLine(cx - guideLen, cy, cx + guideLen, cy, guide);
        canvas.drawLine(cx, cy - guideLen, cx, cy + guideLen, guide);

        // ③ 杆头：可自由拖动的圆
        float knobRadius = Math.max(6.0f, chassisRadius * this.knobScale);
        float kx = this.knobX;
        float ky = this.knobY;
        if (kx < 0.0f || ky < 0.0f) {
            kx = cx;
            ky = cy;
        }
        // 杆头有按压态着色
        boolean pressed = this.touching;
        Paint knobFill = new Paint();
        knobFill.setAntiAlias(true);
        knobFill.setStyle(Paint.Style.FILL);
        knobFill.setColor(pressed
                ? QclColors.parseSafe(this.pointerColorPress, 0x40FFFFFF)
                : QclColors.parseSafe(this.pointerColor, 0xF6F6F6));
        canvas.drawCircle(kx, ky, knobRadius - this.knobStrokeWidth / 2.0f, knobFill);

        Paint knobStroke = new Paint();
        knobStroke.setAntiAlias(true);
        knobStroke.setStyle(Paint.Style.STROKE);
        knobStroke.setStrokeWidth(this.knobStrokeWidth);
        knobStroke.setColor(pressed
                ? this.knobStrokeColor
                : (this.knobStrokeColor | 0xFF000000));
        canvas.drawCircle(kx, ky, knobRadius - this.knobStrokeWidth / 2.0f, knobStroke);
    }

    // ==================== 触摸：连续拖动 ====================

    public boolean onTouchEvent(MotionEvent event) {
        float cx = this.getWidth() / 2.0f;
        float cy = this.getHeight() / 2.0f;
        float chassisRadius = Math.min(this.getWidth(), this.getHeight()) / 2.0f;
        float knobRadius = Math.max(6.0f, chassisRadius * this.knobScale);
        float maxDistance = Math.max(1.0f, chassisRadius - knobRadius);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (this.calculateDistance(event.getX(), event.getY(), cx, cy) > chassisRadius) {
                    break;
                }
                this.touching = true;
                if (this.onShakeListener != null) {
                    this.onShakeListener.onTouch(this);
                }
                this.initialPositionX = this.getX();
                this.initialPositionY = this.getY();
                if (this.followType == 1 && this.calculateDistance(event.getX(), event.getY(), cx, cy) <= chassisRadius / 3.0
                        || this.followType == 2) {
                    this.setX(this.initialPositionX + event.getX() - cx);
                    this.setY(this.initialPositionY + event.getY() - cy);
                }
                this.refreshView(event, cx, cy, maxDistance);
                // ★ 双击中心：仅用于回调通知（不再默认发 Shift 蹲下），由上层决定是否处理
                if (this.calculateDistance(event.getX(), event.getY(), cx, cy) <= maxDistance * 0.5f || this.followType == 2) {
                    ++this.clickCount;
                    if (this.clickCount == 1) {
                        this.firstClickTime = System.currentTimeMillis();
                    }
                    if (this.clickCount == 2) {
                        if (System.currentTimeMillis() - this.firstClickTime <= 500L) {
                            if (this.onShakeListener != null) {
                                this.onShakeListener.onCenterDoubleClick(this);
                            }
                            this.clickCount = 0;
                        } else {
                            this.firstClickTime = System.currentTimeMillis();
                            this.clickCount = 1;
                        }
                    }
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (!this.touching) {
                    break;
                }
                this.refreshView(event, cx, cy, maxDistance);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (!this.touching) {
                    break;
                }
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
                this.knobX = cx;
                this.knobY = cy;
                this.invalidate();
                break;
            }
        }
        return true;
    }

    /**
     * ★ 核心：把手指位置映射为「杆头位置（限制在 maxDistance 内）」+「方向分段」。
     */
    private void refreshView(MotionEvent event, float cx, float cy, float maxDistance) {
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= maxDistance || len == 0.0f) {
            this.knobX = event.getX();
            this.knobY = event.getY();
        } else {
            // 超出范围 → 杆头钉在圆周上
            this.knobX = cx + maxDistance * dx / len;
            this.knobY = cy + maxDistance * dy / len;
        }
        this.invalidate();
        this.setDirection(event, cx, cy, maxDistance);
    }

    private double calculateDistance(float xPri, float yPri, float xSec, float ySec) {
        float d = (xPri - xSec) * (xPri - xSec) + (yPri - ySec) * (yPri - ySec);
        return Math.sqrt(d);
    }

    private double radian2Angle(double radian) {
        double tmp = Math.round(radian / Math.PI * 180.0);
        return tmp >= 0.0 ? tmp : 360.0 + tmp;
    }

    /** 8 方向分段（与原来一致；死区 = 中心 30% 半径内视为居中） */
    private void setDirection(MotionEvent event, float cx, float cy, float maxDistance) {
        float lenX = event.getX() - cx;
        float lenY = event.getY() - cy;
        float lenXY = (float) Math.sqrt(lenX * lenX + lenY * lenY);
        // ★ 死区：中心 30% 内不触发方向（避免手指微抖导致乱走）
        if (lenXY < maxDistance * 0.3f || lenXY == 0.0f) {
            if (this.tempDirection != Direction.DIRECTION_CENTER) {
                this.tempDirection = Direction.DIRECTION_CENTER;
                if (this.onShakeListener != null) {
                    this.onShakeListener.onShake(this, this.tempDirection);
                }
            }
            return;
        }
        double radian = Math.acos(lenX / lenXY) * (lenY < 0.0f ? -1 : 1);
        double angle = this.radian2Angle(radian);
        Direction dir = this.tempDirection;
        if ((0.0 <= angle && ANGLE_8D_OF_0P > angle) || (ANGLE_8D_OF_7P <= angle && 360.0 > angle)) {
            dir = Direction.DIRECTION_RIGHT;
        } else if (ANGLE_8D_OF_0P <= angle && ANGLE_8D_OF_1P > angle) {
            dir = Direction.DIRECTION_DOWN_RIGHT;
        } else if (ANGLE_8D_OF_1P <= angle && ANGLE_8D_OF_2P > angle) {
            dir = Direction.DIRECTION_DOWN;
        } else if (ANGLE_8D_OF_2P <= angle && ANGLE_8D_OF_3P > angle) {
            dir = Direction.DIRECTION_DOWN_LEFT;
        } else if (ANGLE_8D_OF_3P <= angle && ANGLE_8D_OF_4P > angle) {
            dir = Direction.DIRECTION_LEFT;
        } else if (ANGLE_8D_OF_4P <= angle && ANGLE_8D_OF_5P > angle) {
            dir = Direction.DIRECTION_UP_LEFT;
        } else if (ANGLE_8D_OF_5P <= angle && ANGLE_8D_OF_6P > angle) {
            dir = Direction.DIRECTION_UP;
        } else if (ANGLE_8D_OF_6P <= angle && ANGLE_8D_OF_7P > angle) {
            dir = Direction.DIRECTION_UP_RIGHT;
        }
        if (dir != this.tempDirection) {
            this.tempDirection = dir;
            if (this.onShakeListener != null) {
                this.onShakeListener.onShake(this, this.tempDirection);
            }
        }
    }

    // ==================== 配置注入 ====================

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

    /** ★ 1.4.2：注入底盘/杆头的绘制样式（由 BaseRockerView 从 RockerStyle 传入） */
    public void setChassisStyle(int strokeWidth, int strokeColor, int fillColor) {
        this.chassisStrokeWidth = Math.max(1, strokeWidth);
        this.chassisStrokeColor = strokeColor;
        this.chassisFillColor = fillColor;
    }

    public void setKnobStyle(int strokeWidth, int strokeColor, int fillColor) {
        this.knobStrokeWidth = Math.max(1, strokeWidth);
        this.knobStrokeColor = strokeColor;
        this.knobFillColor = fillColor;
    }

    /** ★ 1.4.2：杆头直径占底盘的比例（0.2~0.8） */
    public void setKnobScale(float knobScale) {
        if (knobScale < 0.2f) {
            knobScale = 0.2f;
        }
        if (knobScale > 0.8f) {
            knobScale = 0.8f;
        }
        this.knobScale = knobScale;
        this.invalidate();
    }

    public void setKnobFillColor(int color) {
        this.knobFillColor = color;
        this.invalidate();
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
