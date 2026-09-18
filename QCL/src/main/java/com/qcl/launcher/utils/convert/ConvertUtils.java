package com.qcl.launcher.utils.convert;

import android.content.Context;

/* loaded from: classes2.dex */
public class ConvertUtils {
    public static int dip2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static int px2dip(Context context, float f) {
        return (int) ((f / context.getResources().getDisplayMetrics().density) + 0.5f);
    }
}
