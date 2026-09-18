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
        invalidate();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        MenuFloatCallback menuFloatCallback;
        float measuredWidth;
        int action = motionEvent.getAction();
        if (action == 0) {
            this.initialX = motionEvent.getX();
            this.initialY = motionEvent.getY();
            this.downTime = System.currentTimeMillis();
            this.pressed = true;
        } else if (action == 1) {
            if (Math.abs(motionEvent.getX() - this.initialX) <= 10.0f && Math.abs(motionEvent.getY() - this.initialY) <= 10.0f && System.currentTimeMillis() - this.downTime <= 400 && (menuFloatCallback = this.callback) != null) {
                menuFloatCallback.onClick();
            }
            this.pressed = false;
        } else if (action == 2) {
            float f = 0.0f;
            if ((getX() + motionEvent.getX()) - this.initialX >= 0.0f && (getX() + motionEvent.getX()) - this.initialX <= this.screenWidth - getMeasuredWidth()) {
                measuredWidth = (getX() + motionEvent.getX()) - this.initialX;
            } else {
                measuredWidth = (getX() + motionEvent.getX()) - this.initialX < 0.0f ? 0.0f : this.screenWidth - getMeasuredWidth();
            }
            if ((getY() + motionEvent.getY()) - this.initialY >= 0.0f && (getY() + motionEvent.getY()) - this.initialY <= this.screenHeight - getMeasuredHeight()) {
                f = (getY() + motionEvent.getY()) - this.initialY;
            } else if ((getY() + motionEvent.getY()) - this.initialY >= 0.0f) {
                f = this.screenHeight - getMeasuredHeight();
            }
            if (this.menuHelper.gameMenuSetting.menuFloatSetting.movable) {
                setX(measuredWidth);
                setY(f);
                MenuFloatCallback menuFloatCallback2 = this.callback;
                if (menuFloatCallback2 != null) {
                    menuFloatCallback2.onMove(getX() / (this.screenWidth - getMeasuredWidth()), getY() / (this.screenHeight - getMeasuredHeight()));
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
