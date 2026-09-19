package com.qcl.launcher.control.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import com.qcl.launcher.control.MenuHelper;
import com.qcl.launcher.utils.convert.ConvertUtils;
import java.io.File;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public class MenuFloat extends View {
    private final int DEFAULT_HEIGHT;
    private final int DEFAULT_WIDTH;
    private Paint areaPaint;
    private Bitmap bitmap;
    private MenuFloatCallback callback;
    private long downTime;
    private float initialX;
    private float initialY;
    // ★★★ 2026-09-19 新增：记录按下瞬间的控件位置。
    //   拖动必须用「初始位置 + 累计位移(屏幕绝对坐标)」计算目标位置；
    //   原来用 getX() + (event.getX() - initialX)（相对本 View 的坐标），
    //   而 View 拖动时自身也在移动 → 相对坐标差恒 ≈0 → 跟手失灵、只挪一点点就停住，
    //   松开时保存的就是那个"偏移很小"的位置 → 重启后看起来又回到中间。
    private float initialPositionX;
    private float initialPositionY;
    private MenuHelper menuHelper;
    private final Paint outlinePaint;
    private Paint paint;
    private boolean pressed;
    private int screenHeight;
    private int screenWidth;
    private float xPosition;
    private float yPosition;

    /* loaded from: classes2.dex */
    public interface MenuFloatCallback {
        void onClick();

        void onMove(float f, float f2);
    }

    public MenuFloat(Context context, MenuHelper menuHelper, int i, int i2, float f, float f2) {
        super(context);
        this.DEFAULT_WIDTH = ConvertUtils.dip2px(getContext(), 40.0f);
        this.DEFAULT_HEIGHT = ConvertUtils.dip2px(getContext(), 40.0f);
        this.pressed = false;
        this.menuHelper = menuHelper;
        this.screenWidth = i;
        this.screenHeight = i2;
        this.xPosition = f;
        this.yPosition = f2;
        Paint paint = new Paint();
        this.paint = paint;
        paint.setAntiAlias(true);
        this.paint.setColor(getContext().getColor(R.color.colorDarkGray));
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(ConvertUtils.dip2px(getContext(), 2.0f));
        Paint paint2 = new Paint();
        this.areaPaint = paint2;
        paint2.setAntiAlias(true);
        Paint paint3 = new Paint();
        this.outlinePaint = paint3;
        paint3.setAntiAlias(true);
        paint3.setColor(getContext().getColor(R.color.colorRed));
        paint3.setStyle(Paint.Style.STROKE);
        paint3.setStrokeWidth(3.0f);
        File file = new File(context.getExternalFilesDir("Theme"), "floatIcon.png");
        if (file.exists()) {
            this.bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } else {
            this.bitmap = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_craft_table);
        }
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(this.DEFAULT_WIDTH, this.DEFAULT_HEIGHT);
        setX((this.screenWidth - this.DEFAULT_WIDTH) * this.xPosition);
        setY((this.screenHeight - this.DEFAULT_HEIGHT) * this.yPosition);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.pressed) {
            this.areaPaint.setColor(getContext().getColor(R.color.launcher_ui_background_light));
        } else {
            this.areaPaint.setColor(getContext().getColor(R.color.colorTransparent));
        }
        canvas.drawCircle(getMeasuredWidth() >> 1, getMeasuredHeight() >> 1, (getMeasuredWidth() >> 1) - ConvertUtils.dip2px(getContext(), 1.0f), this.paint);
        canvas.drawCircle(getMeasuredWidth() >> 1, getMeasuredHeight() >> 1, (getMeasuredWidth() >> 1) - ConvertUtils.dip2px(getContext(), 2.0f), this.areaPaint);
        canvas.drawBitmap(this.bitmap, new Rect(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight()), new Rect(ConvertUtils.dip2px(getContext(), 6.0f), ConvertUtils.dip2px(getContext(), 6.0f), ConvertUtils.dip2px(getContext(), 34.0f), ConvertUtils.dip2px(getContext(), 34.0f)), new Paint(1));
        if (this.menuHelper.showOutline) {
            Path path = new Path();
            path.moveTo(0.0f, 0.0f);
            path.lineTo(getWidth(), 0.0f);
            path.lineTo(getWidth(), getHeight());
            path.lineTo(0.0f, getHeight());
            path.lineTo(0.0f, 0.0f);
            canvas.drawPath(path, this.outlinePaint);
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        MenuFloatCallback menuFloatCallback;
        int action = motionEvent.getAction();
        float maxX = (float) Math.max(0, this.screenWidth - getMeasuredWidth());
        float maxY = (float) Math.max(0, this.screenHeight - getMeasuredHeight());
        if (action == 0) {
            // 屏幕绝对坐标 + 按下瞬间的位置（配合下面「初始位置 + 累计位移」的算法）
            this.initialX = motionEvent.getRawX();
            this.initialY = motionEvent.getRawY();
            this.initialPositionX = getX();
            this.initialPositionY = getY();
            this.downTime = System.currentTimeMillis();
            this.pressed = true;
        } else if (action == 1) {
            float totalDx = Math.abs(motionEvent.getRawX() - this.initialX);
            float totalDy = Math.abs(motionEvent.getRawY() - this.initialY);
            if (totalDx <= 10.0f && totalDy <= 10.0f && System.currentTimeMillis() - this.downTime <= 400 && (menuFloatCallback = this.callback) != null) {
                menuFloatCallback.onClick();
            }
            this.pressed = false;
            // ★ 拖动结束：把最终位置再写一次并落盘，确保重启后回到这里
            if (this.menuHelper.gameMenuSetting.menuFloatSetting.movable && (totalDx > 10.0f || totalDy > 10.0f) && maxX > 0.0f && maxY > 0.0f) {
                MenuFloatCallback cb = this.callback;
                if (cb != null) {
                    cb.onMove(getX() / maxX, getY() / maxY);
                }
            }
        } else if (action == 2) {
            if (this.menuHelper.gameMenuSetting.menuFloatSetting.movable) {
                float targetX = this.initialPositionX + (motionEvent.getRawX() - this.initialX);
                float targetY = this.initialPositionY + (motionEvent.getRawY() - this.initialY);
                targetX = targetX < 0.0f ? 0.0f : (targetX > maxX ? maxX : targetX);
                targetY = targetY < 0.0f ? 0.0f : (targetY > maxY ? maxY : targetY);
                setX(targetX);
                setY(targetY);
                MenuFloatCallback menuFloatCallback2 = this.callback;
                if (menuFloatCallback2 != null && maxX > 0.0f && maxY > 0.0f) {
                    menuFloatCallback2.onMove(targetX / maxX, targetY / maxY);
                }
            }
            this.pressed = true;
        }
        return true;
    }

    public void addCallback(MenuFloatCallback menuFloatCallback) {
        this.callback = menuFloatCallback;
    }
}
