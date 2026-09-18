package com.qcl.launcher.utils.file;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.graphics.drawable.DrawableCompat;

/* loaded from: classes2.dex */
public class DrawableUtils {
    public static Drawable getDrawableFromFile(String str) {
        return new BitmapDrawable(BitmapFactory.decodeFile(str));
    }

    public static Drawable getDrawableForView(Drawable drawable, int i) {
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, i);
        return wrap;
    }
}
