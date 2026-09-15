package com.qcl.launcher.launcher.uis.tools;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.qcl.launcher.R;

/**
 * QCL 主题应用器：1.0.5 新增「草方块 UI」。
 *
 * <p>设计取舍：整套启动器 UI 由 30+ 个布局拼成，为每套主题复制一份布局维护成本会爆炸。
 * 这里采用「跑一遍视图树、按需替换背景 / 文字色」—— 因为**所有面板都只用 2 个 drawable**
 * （launcher_view_white / launcher_view_light_gray），递归替换这两个引用即可覆盖全局。
 *
 * <p>关键点 1：为了让「切回默认主题」能**精确还原**每个面板原来的样式，
 * 第一次处理某 View 时把它的原始背景对象存进 tag；切回默认时直接取回原对象。
 * 这样白色面板和浅灰面板不会互相串味（草方块主题下它们外观不同，必须分得清）。
 *
 * <p>关键点 2：草方块 UI 下**禁用所有半透明** —— 顶栏(#B3545454)、底栏、面板(#B8CFCFCF)、
 * 按钮全部替换为不透明实色。这是用户明确要求（“自动禁用所有半透明效果”）。
 */
public final class QclThemeUtils {

    public static final int THEME_DEFAULT = 0;
    public static final int THEME_GRASS = 1;

    /** 草方块风格文字色（深棕，比纯黑柔和且与泥土同调） */
    private static final int GRASS_TEXT = 0xFF3A2A14;
    /** 草方块风格按钮上的文字（浅色，绿底上可读） */
    private static final int GRASS_TEXT_ON_BUTTON = 0xFFF3FFE0;

    private static final int TAG_ORIGINAL_BG = 0x7F0F0001;
    private static final int TAG_ORIGINAL_TEXT = 0x7F0F0002;
    private static final int TAG_PANEL_ROLE = 0x7F0F0003;

    /** 面板角色 */
    private static final int ROLE_NONE = 0;
    private static final int ROLE_WHITE = 1;      // qcl_panel_gray
    private static final int ROLE_LIGHT_GRAY = 2; // qcl_panel_gray_alt
    private static final int ROLE_BAR = 3;        // 顶栏 / 底栏（半透明深灰）
    private static final int ROLE_BUTTON = 4;     // 按钮

    private QclThemeUtils() {
    }

    public static boolean isGrass(int uiTheme) {
        return uiTheme == THEME_GRASS;
    }

    /** 应用到整棵视图树 */
    public static void apply(Activity activity, int uiTheme) {
        try {
            applyRecursive(activity, activity.getWindow().getDecorView(), uiTheme, 0);
        } catch (Throwable ignored) {
        }
    }

    /** 应用到某子树（切页时局部刷新用） */
    public static void applyToView(Activity activity, View root, int uiTheme) {
        if (root == null) return;
        try {
            applyRecursive(activity, root, uiTheme, 0);
        } catch (Throwable ignored) {
        }
    }

    private static void applyRecursive(Activity activity, View view, int uiTheme, int depth) {
        if (view == null || depth > 60) return;

        applyToSelf(activity, view, uiTheme);

        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) {
                applyRecursive(activity, g.getChildAt(i), uiTheme, depth + 1);
            }
        }
    }

    private static void applyToSelf(Activity activity, View view, int uiTheme) {
        // ---------------- 背景 ----------------
        Drawable bg = view.getBackground();
        if (bg != null) {
            Integer role = (Integer) view.getTag(TAG_PANEL_ROLE);
            if (role == null) {
                int detected = detectPanelRole(activity, bg);
                if (detected != ROLE_NONE) {
                    role = detected;
                    view.setTag(TAG_PANEL_ROLE, role);
                    view.setTag(TAG_ORIGINAL_BG, bg); // 首次：记住原样
                }
            }
            if (role != null && role != ROLE_NONE) {
                if (uiTheme == THEME_GRASS) {
                    view.setBackground(makeGrassBackground(activity, role));
                } else {
                    Object original = view.getTag(TAG_ORIGINAL_BG);
                    if (original instanceof Drawable) view.setBackground((Drawable) original);
                }
            }
        }

        // ---------------- 文字 ----------------
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            if (uiTheme == THEME_GRASS) {
                if (tv.getTag(TAG_ORIGINAL_TEXT) == null) {
                    tv.setTag(TAG_ORIGINAL_TEXT, Integer.valueOf(tv.getCurrentTextColor()));
                }
                int cur = tv.getCurrentTextColor();
                Integer role = (Integer) view.getTag(TAG_PANEL_ROLE);
                if (role != null && role == ROLE_BUTTON) {
                    tv.setTextColor(GRASS_TEXT_ON_BUTTON);
                } else if (isDark(cur)) {
                    // 只改深色文字，避免把顶栏上的白字改糊
                    tv.setTextColor(GRASS_TEXT);
                }
            } else {
                Object orig = tv.getTag(TAG_ORIGINAL_TEXT);
                if (orig instanceof Integer) tv.setTextColor((Integer) orig);
            }
        }
    }

    /**
     * 判定背景属于哪类。用 drawable 的「ConstantState 同源判定」而不是颜色比对——
     * 因为颜色比对需要 API 24 的 GradientDrawable#getColor()，且 LayerDrawable / selector 拿不到。
     */
    private static int detectPanelRole(Activity activity, Drawable bg) {
        // 面板
        if (sameDrawable(activity, bg, R.drawable.launcher_view_white)) return ROLE_WHITE;
        if (sameDrawable(activity, bg, R.drawable.launcher_view_light_gray)) return ROLE_LIGHT_GRAY;
        // 顶栏 / 底栏（布局里用的是 @color/qcl_gray_translucent_bar → 按颜色值判）
        if (bg instanceof ColorDrawable) {
            int c = ((ColorDrawable) bg).getColor();
            if (c == 0xB3545454 || c == 0xB3575757) return ROLE_BAR;
        }
        // 按钮：主界面顶部一排 + 启动按钮
        if (sameDrawable(activity, bg, R.drawable.qcl_button_gray)) return ROLE_BUTTON;
        if (sameDrawable(activity, bg, R.drawable.launcher_button_gray)) return ROLE_BUTTON;
        if (sameDrawable(activity, bg, R.drawable.launcher_button_blue)) return ROLE_BUTTON;
        if (sameDrawable(activity, bg, R.drawable.launcher_button_white)) return ROLE_BUTTON;
        return ROLE_NONE;
    }

    /**
     * 判断两个 drawable 是否「来自同一份资源」。
     * ConstantState 是同一个对象时通常就是同一资源（同主题、同密度下成立）。
     */
    private static boolean sameDrawable(Activity activity, Drawable current, int resId) {
        try {
            Drawable target = ContextCompat.getDrawable(activity, resId);
            if (target == null) return false;
            Drawable.ConstantState a = current.getConstantState();
            Drawable.ConstantState b = target.getConstantState();
            return a != null && a == b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 按角色生成草方块风格的不透明背景（全部用真实 Alpha 草方块材质） */
    private static Drawable makeGrassBackground(Activity activity, int role) {
        switch (role) {
            case ROLE_WHITE:
                // 主面板：草方块平铺
                return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_panel);
            case ROLE_LIGHT_GRAY:
                // 次级面板：泥土平铺
                return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_panel_alt);
            case ROLE_BAR:
                // 顶栏 / 底栏：草皮横条（不透明）
                return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_bar);
            case ROLE_BUTTON:
                // 按钮：草方块平铺 + 按压变暗
                return ContextCompat.getDrawable(activity, R.drawable.qcl_grass_button_bg);
            default:
                return null;
        }
    }

    private static boolean isDark(int color) {
        int lum = (int) (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
        return lum < 100;
    }
}
