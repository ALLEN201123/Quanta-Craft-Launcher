package com.qcl.launcher.launcher.uis.tools;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.qcl.launcher.R;
/* loaded from: classes2.dex */
public final class QclThemeUtils {
    private static final int[] BUTTON_DRAWABLES = {R.drawable.qcl_button_gray, R.drawable.launcher_button_gray, R.drawable.launcher_button_blue, R.drawable.launcher_button_white, R.drawable.launcher_button_gray_blue, R.drawable.launcher_button_light_gray, R.drawable.launcher_button_normal, R.drawable.launcher_button_transparent_blue, R.drawable.launcher_button_white_blue, R.drawable.launcher_setting_button, R.drawable.launcher_button_item, R.drawable.launcher_button_parent, R.drawable.launcher_button_selected};
    private static final int GRASS_TEXT = -12965356;
    private static final int GRASS_TEXT_ON_BUTTON = -786464;
    private static final int ROLE_BAR = 3;
    private static final int ROLE_BUTTON = 4;
    private static final int ROLE_LIGHT_GRAY = 2;
    private static final int ROLE_NONE = 0;
    private static final int ROLE_WHITE = 1;
    private static final int TAG_ORIGINAL_BG = 2131689473;
    private static final int TAG_ORIGINAL_TEXT = 2131689474;
    private static final int TAG_PANEL_ROLE = 2131689475;
    public static final int THEME_DEFAULT = 0;
    public static final int THEME_GRASS = 1;

    public static boolean isGrass(int i) {
        return i == 1;
    }

    private QclThemeUtils() {
    }

    public static void apply(Activity activity, int i) {
        try {
            applyRecursive(activity, activity.getWindow().getDecorView(), i, 0);
        } catch (Throwable unused) {
        }
    }

    public static void applyToView(Activity activity, View view, int i) {
        if (view == null) {
            return;
        }
        try {
            applyRecursive(activity, view, i, 0);
        } catch (Throwable unused) {
        }
    }

    private static void applyRecursive(Activity activity, View view, int i, int i2) {
        if (view == null || i2 > 60) {
            return;
        }
        applyToSelf(activity, view, i);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i3 = 0; i3 < viewGroup.getChildCount(); i3++) {
                applyRecursive(activity, viewGroup.getChildAt(i3), i, i2 + 1);
            }
        }
    }

    private static void applyToSelf(Activity activity, View view, int i) {
        int detectPanelRole;
        Drawable background = view.getBackground();
        if (background != null) {
            Integer num = (Integer) view.getTag(2131689475);
            if (num == null && (detectPanelRole = detectPanelRole(activity, background)) != 0) {
                num = Integer.valueOf(detectPanelRole);
                view.setTag(2131689475, num);
                view.setTag(2131689473, background);
            }
            if (num != null && num.intValue() != 0) {
                if (i == 1) {
                    view.setBackground(makeGrassBackground(activity, num.intValue()));
                } else {
                    Object tag = view.getTag(2131689473);
                    if (tag instanceof Drawable) {
                        view.setBackground((Drawable) tag);
                    }
                }
            }
        }
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (i == 1) {
                if (textView.getTag(2131689474) == null) {
                    textView.setTag(2131689474, Integer.valueOf(textView.getCurrentTextColor()));
                }
                int currentTextColor = textView.getCurrentTextColor();
                Integer num2 = (Integer) view.getTag(2131689475);
                if (num2 != null && num2.intValue() == 4) {
                    textView.setTextColor(-786464);
                    return;
                } else {
                    if (isDark(currentTextColor)) {
                        textView.setTextColor(-12965356);
                        return;
                    }
                    return;
                }
            }
            Object tag2 = textView.getTag(2131689474);
            if (tag2 instanceof Integer) {
                textView.setTextColor(((Integer) tag2).intValue());
            }
        }
    }

    private static int detectPanelRole(Activity activity, Drawable drawable) {
        if (sameDrawable(activity, drawable, R.drawable.launcher_view_white)) {
            return 1;
        }
        if (sameDrawable(activity, drawable, R.drawable.launcher_view_light_gray)) {
            return 2;
        }
        if ((drawable instanceof ColorDrawable) && isTranslucentGrayBar(((ColorDrawable) drawable).getColor())) {
            return 3;
        }
        return isButtonDrawable(activity, drawable) ? 4 : 0;
    }

    private static boolean isButtonDrawable(Activity activity, Drawable drawable) {
        for (int i : BUTTON_DRAWABLES) {
            if (sameDrawable(activity, drawable, i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTranslucentGrayBar(int i) {
        int i2 = (i >>> 24) & 255;
        int i3 = (i >>> 16) & 255;
        int i4 = (i >>> 8) & 255;
        int i5 = i & 255;
        if (i2 < 102 || i2 > 224) {
            return false;
        }
        int max = Math.max(i3, Math.max(i4, i5));
        int min = Math.min(i3, Math.min(i4, i5));
        return max - min <= 12 && min >= 56 && max <= 136;
    }

    private static boolean sameDrawable(Activity activity, Drawable drawable, int i) {
        if (drawable == null) {
            return false;
        }
        try {
            if (containsResId(drawable, i, 0)) {
                return true;
            }
            Drawable drawable2 = ContextCompat.getDrawable(activity, i);
            if (drawable2 == null) {
                return false;
            }
            Drawable.ConstantState constantState = drawable.getConstantState();
            Drawable.ConstantState constantState2 = drawable2.getConstantState();
            return (constantState == null || constantState2 == null || constantState != constantState2) ? false : true;
        } catch (Throwable unused) {
            return false;
        }
    }

    private static boolean containsResId(Drawable drawable, int i, int i2) {
        Drawable.ConstantState constantState;
        if (drawable != null && i2 <= 4) {
            if (reflectResourceId(drawable) == i) {
                return true;
            }
            try {
                if (drawable instanceof RippleDrawable) {
                    RippleDrawable rippleDrawable = (RippleDrawable) drawable;
                    int numberOfLayers = rippleDrawable.getNumberOfLayers();
                    for (int i3 = 0; i3 < numberOfLayers; i3++) {
                        if (containsResId(rippleDrawable.getDrawable(i3), i, i2 + 1)) {
                            return true;
                        }
                    }
                }
                if (drawable instanceof LayerDrawable) {
                    LayerDrawable layerDrawable = (LayerDrawable) drawable;
                    for (int i4 = 0; i4 < layerDrawable.getNumberOfLayers(); i4++) {
                        if (containsResId(layerDrawable.getDrawable(i4), i, i2 + 1)) {
                            return true;
                        }
                    }
                }
                if ((drawable instanceof DrawableContainer) && (constantState = drawable.getConstantState()) != null) {
                    Method declaredMethod = constantState.getClass().getDeclaredMethod("getChildren", new Class[0]);
                    declaredMethod.setAccessible(true);
                    Object invoke = declaredMethod.invoke(constantState, new Object[0]);
                    if (invoke instanceof Drawable[]) {
                        for (Drawable drawable2 : (Drawable[]) invoke) {
                            if (containsResId(drawable2, i, i2 + 1)) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Throwable unused) {
            }
        }
        return false;
    }

    private static int reflectResourceId(Drawable drawable) {
        if (drawable == null) {
            return 0;
        }
        try {
            Field declaredField = Drawable.class.getDeclaredField("mResourceId");
            declaredField.setAccessible(true);
            Object obj = declaredField.get(drawable);
            if (obj instanceof Integer) {
                return ((Integer) obj).intValue();
            }
        } catch (Throwable unused) {
        }
        return 0;
    }

    private static Drawable makeGrassBackground(Activity activity, int i) {
        if (i == 1) {
            return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_panel);
        }
        if (i == 2) {
            return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_panel_alt);
        }
        if (i == 3) {
            return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_bar);
        }
        if (i != 4) {
            return null;
        }
        return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_button_bg);
    }

    private static boolean isDark(int i) {
        return ((int) (((((double) Color.red(i)) * 0.299d) + (((double) Color.green(i)) * 0.587d)) + (((double) Color.blue(i)) * 0.114d))) < 100;
    }
}
