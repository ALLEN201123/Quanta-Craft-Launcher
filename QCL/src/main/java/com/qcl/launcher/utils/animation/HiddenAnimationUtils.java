package com.qcl.launcher.utils.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

/* loaded from: classes2.dex */
public class HiddenAnimationUtils {
    private RotateAnimation animation;
    private View down;
    private View hideView;
    private int mHeight;

    public static HiddenAnimationUtils newInstance(Context context, View view, View view2, int i) {
        return new HiddenAnimationUtils(context, view, view2, i);
    }

    private HiddenAnimationUtils(Context context, View view, View view2, int i) {
        this.hideView = view;
        this.down = view2;
        this.mHeight = i;
    }

    public void toggle() {
        startAnimation();
        if (this.hideView.getVisibility() == 0) {
            closeAnimate(this.hideView);
        } else {
            openAnim(this.hideView);
        }
    }

    private void startAnimation() {
        if (this.hideView.getVisibility() == 0) {
            this.animation = new RotateAnimation(180.0f, 0.0f, 1, 0.5f, 1, 0.5f);
        } else {
            this.animation = new RotateAnimation(0.0f, 180.0f, 1, 0.5f, 1, 0.5f);
        }
        this.animation.setDuration(30L);
        this.animation.setInterpolator(new LinearInterpolator());
        this.animation.setRepeatMode(2);
        this.animation.setFillAfter(true);
        View view = this.down;
        if (view != null) {
            view.startAnimation(this.animation);
        }
    }

    private void openAnim(View view) {
        view.setVisibility(0);
        createDropAnimator(view, 0, this.mHeight).start();
    }

    private void closeAnimate(final View view) {
        ValueAnimator createDropAnimator = createDropAnimator(view, view.getHeight(), 0);
        createDropAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.qcl.launcher.utils.animation.HiddenAnimationUtils.1
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                view.setVisibility(8);
            }
        });
        createDropAnimator.start();
    }

    private ValueAnimator createDropAnimator(final View view, int i, int i2) {
        ValueAnimator ofInt = ValueAnimator.ofInt(i, i2);
        ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.qcl.launcher.utils.animation.HiddenAnimationUtils.2
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                int intValue = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                layoutParams.height = intValue;
                view.setLayoutParams(layoutParams);
            }
        });
        return ofInt;
    }
}
