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
    private int priId;
    private int secId;

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
                    float x2 = motionEvent.getX(motionEvent.findPointerIndex(this.priId)) - motionEvent.getX(motionEvent.findPointerIndex(this.secId));
                    float y2 = motionEvent.getY(motionEvent.findPointerIndex(this.priId)) - motionEvent.getY(motionEvent.findPointerIndex(this.secId));
                    double sqrt = Math.sqrt((x2 * x2) + (y2 * y2)) - this.initDist;
                    if (this.initScale + (sqrt / (Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 1.0d)) <= 2.0d && this.initScale + (sqrt / (Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 1.0d)) >= 0.7d) {
                        this.mRenderer.mCharacter.setScale((float) (this.initScale + (sqrt / (Math.sqrt((getWidth() * getWidth()) + (getHeight() * getHeight())) * 1.0d))));
                    }
                }
            } else if (actionMasked == 5) {
                this.secId = motionEvent.getPointerId(motionEvent.getActionIndex());
                float x3 = motionEvent.getX(motionEvent.findPointerIndex(this.priId)) - motionEvent.getX(motionEvent.findPointerIndex(this.secId));
                float y3 = motionEvent.getY(motionEvent.findPointerIndex(this.priId)) - motionEvent.getY(motionEvent.findPointerIndex(this.secId));
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
