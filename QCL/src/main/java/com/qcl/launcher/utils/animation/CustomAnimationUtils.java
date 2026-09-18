package com.qcl.launcher.utils.animation;

import android.content.Context;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.qcl.launcher.launcher.MainActivity;

/* loaded from: classes2.dex */
public class CustomAnimationUtils {
    public static void showViewFromLeft(View view, MainActivity mainActivity, Context context, boolean z) {
        view.setVisibility(0);
        if (mainActivity.isLoaded && z) {
            view.setAnimation(AnimationUtils.makeInAnimation(context, true));
        }
    }

    public static void hideViewToLeft(View view, MainActivity mainActivity, Context context, boolean z) {
        view.setVisibility(8);
        if (mainActivity.isLoaded && z) {
            view.setAnimation(AnimationUtils.makeOutAnimation(context, false));
        }
    }

    public static void showViewFromRight(View view, MainActivity mainActivity, Context context, boolean z) {
        view.setVisibility(0);
        if (mainActivity.isLoaded && z) {
            view.setAnimation(AnimationUtils.makeInAnimation(context, false));
        }
    }

    public static void hideViewToRight(View view, MainActivity mainActivity, Context context, boolean z) {
        view.setVisibility(8);
        if (mainActivity.isLoaded && z) {
            view.setAnimation(AnimationUtils.makeOutAnimation(context, true));
        }
    }

    public static void showViewFromLeft(View view, Context context, boolean z) {
        view.setVisibility(0);
        if (z) {
            view.setAnimation(AnimationUtils.makeInAnimation(context, true));
        }
    }

    public static void hideViewToLeft(View view, Context context, boolean z) {
        view.setVisibility(8);
        if (z) {
            view.setAnimation(AnimationUtils.makeOutAnimation(context, false));
        }
    }

    public static void showViewFromRight(View view, Context context, boolean z) {
        view.setVisibility(0);
        if (z) {
            view.setAnimation(AnimationUtils.makeInAnimation(context, false));
        }
    }

    public static void hideViewToRight(View view, Context context, boolean z) {
        view.setVisibility(8);
        if (z) {
            view.setAnimation(AnimationUtils.makeOutAnimation(context, true));
        }
    }
}
