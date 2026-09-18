/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.graphics.Canvas
 *  android.graphics.Paint
 *  android.graphics.Paint$Style
 *  android.graphics.Path
 *  android.graphics.Rect
 *  android.os.Handler
 *  android.os.Looper
 *  android.util.AttributeSet
 *  android.view.MotionEvent
 *  android.view.View
 *  android.widget.RelativeLayout
 */
package com.qcl.launcher.control.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import com.qcl.launcher.launcher.uis.main.DynamicBackground;
import com.qcl.launcher.utils.convert.ConvertUtils;

import com.qcl.launcher.R;
public class LayoutPanel
extends RelativeLayout {
    private static final int POSITION_MODE_PERCENT = 0;
    private static final int POSITION_MODE_ABSOLUTE = 1;
    private int positionMode = 0;
    private float[] xReference;
    private float[] yReference;
    private boolean showBackground = false;
    private boolean showReference = false;
    private Paint linePaint;
    private Path path;
    private Paint textPaint;
    private String xText;
    private String yText;
    private static final long BG_INTERVAL_MS = 10000L;
    private static final int BG_FADE_MS = 600;
    private static final int[] BG_RES_IDS = DynamicBackground.RES_IDS;
    private final Bitmap[] bgCache = new Bitmap[BG_RES_IDS.length];
    private int bgIndex = 0;
    private Bitmap bgPrev = null;
    private float bgFade = 1.0f;
    private boolean bgRunning = false;
    private long bgLastFrame = 0L;
    private final Handler bgHandler = new Handler(Looper.getMainLooper());
    private final Runnable bgTick = new Runnable(){

        @Override
        public void run() {
            if (!LayoutPanel.this.bgRunning) {
                return;
            }
            LayoutPanel.this.bgIndex = (LayoutPanel.this.bgIndex + 1) % BG_RES_IDS.length;
            LayoutPanel.this.bgPrev = LayoutPanel.this.bgCache[(LayoutPanel.this.bgIndex - 1 + BG_RES_IDS.length) % BG_RES_IDS.length];
            LayoutPanel.this.bgFade = 0.0f;
            LayoutPanel.this.bgLastFrame = System.currentTimeMillis();
            LayoutPanel.this.invalidate();
            LayoutPanel.this.bgHandler.postDelayed((Runnable)this, 10000L);
        }
    };
    private boolean qclMenuOutsideCloseEnabled = false;
    private static final float MENU_OUTSIDE_SLOP_PX = 8.0f;
    private View qclMenuPanel;
    private OutsideCloseListener qclOutsideCloseListener;

    public LayoutPanel(Context context) {
        super(context);
    }

    public LayoutPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.linePaint = new Paint();
        this.linePaint.setAntiAlias(true);
        this.linePaint.setColor(this.getContext().getColor(R.color.colorGreen));
        this.linePaint.setStyle(Paint.Style.STROKE);
        this.textPaint = new Paint();
        this.textPaint.setAntiAlias(true);
        this.textPaint.setColor(this.getContext().getColor(R.color.colorGreen));
        this.textPaint.setStyle(Paint.Style.FILL);
        this.textPaint.setTextSize(50.0f);
        this.xReference = new float[2];
        this.yReference = new float[2];
        this.getBg(0);
    }

    private Bitmap decodeBg(int i) {
        if (i < 0 || i >= BG_RES_IDS.length) {
            return null;
        }
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeResource((Resources)this.getContext().getResources(), (int)BG_RES_IDS[i], (BitmapFactory.Options)bounds);
            int w = bounds.outWidth;
            if (w <= 0) {
                return null;
            }
            int reqW = Math.max(1080, this.getContext().getResources().getDisplayMetrics().widthPixels * 3 / 2);
            int sample = 1;
            while (w / sample > reqW * 2) {
                sample *= 2;
            }
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeResource((Resources)this.getContext().getResources(), (int)BG_RES_IDS[i], (BitmapFactory.Options)o);
        }
        catch (Throwable t) {
            return null;
        }
    }

    private Bitmap getBg(int i) {
        if (i < 0 || i >= BG_RES_IDS.length) {
            return null;
        }
        if (this.bgCache[i] == null) {
            this.bgCache[i] = this.decodeBg(i);
        }
        return this.bgCache[i];
    }

    private void startBgRotation() {
        if (this.bgRunning) {
            return;
        }
        this.bgRunning = true;
        this.bgIndex = 0;
        this.getBg(0);
        this.getBg(1);
        this.bgHandler.removeCallbacks(this.bgTick);
        this.bgHandler.postDelayed(this.bgTick, 10000L);
    }

    private void stopBgRotation() {
        this.bgRunning = false;
        this.bgHandler.removeCallbacks(this.bgTick);
    }

    protected void onDetachedFromWindow() {
        this.stopBgRotation();
        super.onDetachedFromWindow();
    }

    public void setMenuOutsideCloseEnabled(boolean enabled) {
        this.qclMenuOutsideCloseEnabled = enabled;
    }

    public void setMenuPanel(View panel) {
        this.qclMenuPanel = panel;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.qclMenuOutsideCloseEnabled && this.qclMenuPanel != null && this.qclMenuPanel.getVisibility() == 0 && ev.getActionMasked() == 0 && !this.hitTestMenuPanel(ev.getRawX(), ev.getRawY()) && this.qclOutsideCloseListener != null) {
            this.qclOutsideCloseListener.onOutsideTouched();
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean hitTestMenuPanel(float rawX, float rawY) {
        try {
            int[] loc = new int[2];
            this.qclMenuPanel.getLocationOnScreen(loc);
            return rawX >= (float)loc[0] - 8.0f && rawX <= (float)(loc[0] + this.qclMenuPanel.getWidth()) + 8.0f && rawY >= (float)loc[1] - 8.0f && rawY <= (float)(loc[1] + this.qclMenuPanel.getHeight()) + 8.0f;
        }
        catch (Throwable t) {
            return true;
        }
    }

    public void setOutsideCloseListener(OutsideCloseListener listener) {
        this.qclOutsideCloseListener = listener;
    }

    @SuppressLint(value={"DrawAllocation"})
    protected void onDraw(Canvas canvas) {
        if (this.showBackground) {
            Bitmap cur = this.getBg(this.bgIndex);
            if (this.bgPrev != null && this.bgFade < 1.0f && cur != null) {
                long now = System.currentTimeMillis();
                if (this.bgLastFrame == 0L) {
                    this.bgLastFrame = now;
                }
                this.bgFade = Math.min(1.0f, this.bgFade + (float)(now - this.bgLastFrame) / 600.0f);
                this.bgLastFrame = now;
                this.drawBgBitmap(canvas, this.bgPrev, 255);
                int alpha = (int)(255.0f * this.bgFade);
                this.drawBgBitmap(canvas, cur, alpha);
                if (this.bgFade < 1.0f) {
                    this.postInvalidateOnAnimation();
                } else {
                    this.bgPrev = null;
                }
            } else if (cur != null) {
                this.drawBgBitmap(canvas, cur, 255);
            }
        }
        if (this.showReference) {
            this.path = new Path();
            this.path.moveTo(this.xReference[0], 0.0f);
            this.path.lineTo(this.xReference[0], (float)this.getHeight());
            this.path.moveTo(this.xReference[1], 0.0f);
            this.path.lineTo(this.xReference[1], (float)this.getHeight());
            this.path.moveTo(0.0f, this.yReference[0]);
            this.path.lineTo((float)this.getWidth(), this.yReference[0]);
            this.path.moveTo(0.0f, this.yReference[1]);
            this.path.lineTo((float)this.getWidth(), this.yReference[1]);
            canvas.drawPath(this.path, this.linePaint);
            canvas.drawText(this.xText, 100.0f, 100.0f, this.textPaint);
            canvas.drawText(this.yText, 100.0f, 200.0f, this.textPaint);
        }
        if (this.showReference) {
            this.invalidate();
        }
    }

    private void drawBgBitmap(Canvas canvas, Bitmap bmp, int alpha) {
        if (bmp == null || bmp.isRecycled()) {
            return;
        }
        int w = this.getMeasuredWidth();
        int h = this.getMeasuredHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        Rect dst = new Rect(0, 0, w, h);
        Paint p = new Paint(1);
        p.setFilterBitmap(true);
        if (alpha < 255) {
            p.setAlpha(Math.max(0, Math.min(255, alpha)));
        }
        canvas.drawBitmap(bmp, src, dst, p);
    }

    public void showReference(int positionMode, float x, float y, int width, int height) {
        this.positionMode = positionMode;
        if (positionMode == 0) {
            this.xText = "X:" + (float)((int)(x / (float)(this.getWidth() - width) * 1000.0f)) / 10.0f + "%";
            this.yText = "Y:" + (float)((int)(y / (float)(this.getHeight() - height) * 1000.0f)) / 10.0f + "%";
        }
        if (positionMode == 1) {
            this.xText = "X:" + ConvertUtils.px2dip(this.getContext(), x) + "dp";
            this.yText = "Y:" + ConvertUtils.px2dip(this.getContext(), y) + "dp";
        }
        this.xReference[0] = x;
        this.yReference[0] = y;
        this.xReference[1] = x + (float)width;
        this.yReference[1] = y + (float)height;
        this.showReference = true;
    }

    public void hideReference() {
        this.showReference = false;
    }

    public void showBackground() {
        this.showBackground = true;
        this.startBgRotation();
        this.invalidate();
    }

    public void hideBackground() {
        this.showBackground = false;
        this.stopBgRotation();
        this.postInvalidateOnAnimation();
    }

    public static interface OutsideCloseListener {
        public void onOutsideTouched();
    }
}

