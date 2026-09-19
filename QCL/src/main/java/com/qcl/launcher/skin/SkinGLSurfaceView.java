package com.qcl.launcher.skin;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/* loaded from: classes2.dex */
public class SkinGLSurfaceView extends GLSurfaceView {
    private boolean alreadyCalled;
    private double initDist;
    private float initScale;
    private float mDensity;
    private float mPreviousX;
    private float mPreviousY;
    private MinecraftSkinRenderer mRenderer;
    private int priId = -1;
    private int secId = -1;

    public SkinGLSurfaceView(Context context) {
        super(context);
        this.alreadyCalled = false;
    }

    public SkinGLSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.alreadyCalled = false;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        MinecraftSkinRenderer minecraftSkinRenderer;
        // ★★★ 2026-09-19 修复崩溃：IllegalArgumentException: pointerIndex out of range
        //   双指缩放里 findPointerIndex(id) 在「第二根手指已抬起 / id 失效」时返回 -1，把 -1
        //   传给 getX/getY 就抛异常并崩进程（皮肤/角色预览界面实测）。
        //   处理：① 抬手就把 id 复位为 -1；② 取索引后校验 <0 时忽略该事件。
        int actionMaskedTop = motionEvent.getActionMasked();
        if (actionMaskedTop == 1 || actionMaskedTop == 3 || actionMaskedTop == 6) {
            this.priId = -1;
            this.secId = -1;
        }
        if (motionEvent.getPointerCount() == 1) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (motionEvent.getAction() == 2 && (minecraftSkinRenderer = this.mRenderer) != null && minecraftSkinRenderer.mCharacter != null) {
                float f = 2.0f / this.mDensity;
                this.mRenderer.mCharacter.rotateBy((x - this.mPreviousX) * f, (y - this.mPreviousY) * f);
            }
            this.mPreviousX = x;
            this.mPreviousY = y;
        }
        if (motionEvent.getPointerCount() == 2) {
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 0) {
                this.priId = motionEvent.getPointerId(motionEvent.getActionIndex());
            } else if (actionMasked == 2) {
                MinecraftSkinRenderer minecraftSkinRenderer2 = this.mRenderer;
                if (minecraftSkinRenderer2 != null && minecraftSkinRenderer2.mCharacter != null) {
                    int iPri = motionEvent.findPointerIndex(this.priId);
                    int iSec = motionEvent.findPointerIndex(this.secId);
                    if (iPri < 0 || iSec < 0) {
                        return true;   // 指针已失效 —— 忽略，不再拿 -1 去 getX/getY
                    }
                    float x2 = motionEvent.getX(iPri) - motionEvent.getX(iSec);
                    float y2 = motionEvent.getY(iPri) - motionEvent.getY(iSec);
                    double sqrt = Math.sqrt((x2 * x2) + (y2 * y2)) - this.initDist;
                    if (this.initScale + (sqrt / (Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 1.0d)) <= 2.0d && this.initScale + (sqrt / (Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 1.0d)) >= 0.7d) {
                        this.mRenderer.mCharacter.setScale((float) (this.initScale + (sqrt / (Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 1.0d))));
                    }
                }
            } else if (actionMasked == 5) {
                this.secId = motionEvent.getPointerId(motionEvent.getActionIndex());
                int iPri2 = motionEvent.findPointerIndex(this.priId);
                int iSec2 = motionEvent.findPointerIndex(this.secId);
                if (iPri2 < 0 || iSec2 < 0) {
                    return true;
                }
                float x3 = motionEvent.getX(iPri2) - motionEvent.getX(iSec2);
                float y3 = motionEvent.getY(iPri2) - motionEvent.getY(iSec2);
                this.initDist = Math.sqrt((x3 * x3) + (y3 * y3));
                MinecraftSkinRenderer minecraftSkinRenderer3 = this.mRenderer;
                if (minecraftSkinRenderer3 != null && minecraftSkinRenderer3.mCharacter != null) {
                    this.initScale = this.mRenderer.mCharacter.scale;
                }
            }
        }
        return true;
    }

    public void setRenderer(MinecraftSkinRenderer minecraftSkinRenderer, float f) {
        this.mRenderer = minecraftSkinRenderer;
        this.mDensity = f;
        super.setRenderer(minecraftSkinRenderer);
    }
}
