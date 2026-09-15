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
        // 顶栏 / 底栏：布局里用的是 @color/qcl_gray_translucent_bar (#B3545454) /
        // qcl_gray_translucent (#B3575757) → 按颜色值判。
        // 1.0.6：原来只精确匹配这两个色值，凡是「差不多的半透明深灰」就会漏掉，
        // 表现为「切了草方块主题，标题栏还是灰的」。改为通用判定：
        //   半透明(alpha 在 0x66~0xE0) + 低饱和的深灰(R≈G≈B 且 0x40~0x80) → 视为栏。
        // 这样 bar 的任意细微色差变体都能覆盖。
        if (bg instanceof ColorDrawable) {
            int c = ((ColorDrawable) bg).getColor();
            if (isTranslucentGrayBar(c)) return ROLE_BAR;
        }
        // 按钮：主界面顶部一排 + 启动按钮 + 各页面操作按钮。
        // 1.0.6：原来只认 4 种，导致 launcher_button_gray_blue / light_gray / normal /
        // transparent_blue / white_blue / launcher_setting_button 这 6 种在草方块主题下
        // 仍然保持原灰色 —— 这就是用户反馈的「部分区域没正确替换成草方块 UI」。
        if (isButtonDrawable(activity, bg)) return ROLE_BUTTON;
        return ROLE_NONE;
    }

    /** 全部按钮类 drawable（草方块主题下统一替换为草方块按钮） */
    private static final int[] BUTTON_DRAWABLES = new int[]{
            R.drawable.qcl_button_gray,
            R.drawable.launcher_button_gray,
            R.drawable.launcher_button_blue,
            R.drawable.launcher_button_white,
            R.drawable.launcher_button_gray_blue,
            R.drawable.launcher_button_light_gray,
            R.drawable.launcher_button_normal,
            R.drawable.launcher_button_transparent_blue,
            R.drawable.launcher_button_white_blue,
            R.drawable.launcher_setting_button,
            R.drawable.launcher_button_item,
            R.drawable.launcher_button_parent,
            R.drawable.launcher_button_selected,
    };

    private static boolean isButtonDrawable(Activity activity, Drawable bg) {
        for (int id : BUTTON_DRAWABLES) {
            if (sameDrawable(activity, bg, id)) return true;
        }
        return false;
    }

    /**
     * 判定一个颜色是否是「半透明深灰栏」。
     * 覆盖 qcl_gray_translucent_bar(#B3545454)、qcl_gray_translucent(#B3575757)
     * 以及任何近似变体，避免因一两个色阶之差导致主题漏刷。
     */
    private static boolean isTranslucentGrayBar(int c) {
        int a = (c >>> 24) & 0xFF;
        int r = (c >>> 16) & 0xFF;
        int g = (c >>> 8) & 0xFF;
        int b = c & 0xFF;
        if (a < 0x66 || a > 0xE0) return false;      // 必须是有一定透明度的
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        if (max - min > 12) return false;            // 必须是接近中性灰（无色偏）
        return min >= 0x38 && max <= 0x88;           // 深灰区间
    }

    /**
     * 判断两个 drawable 是否「来自同一份资源」。
     *
     * <p>⚠️ 1.0.6 重要修正：原来只靠 ConstantState 对象相等，但对 &lt;selector&gt; 类型
     * （本项目 90% 的按钮都是 selector）**永远不成立** —— 每次 getDrawable 都会新建
     * ConstantState，所以按钮类主题替换实际上是失效的，这正是「部分区域没换成草方块」的根因。
     *
     * <p>现在改为：递归读取 Drawable 内部的 mResourceId 字段（反射，Android 各版本都是这个名字），
     * 它记录了「这个 drawable 是从哪个资源 id 加载出来的」，对 selector / ripple / layer-list
     * 的子层同样有效，稳定可靠。最后再兜一层 ConstantState 比较。
     */
    private static boolean sameDrawable(Activity activity, Drawable current, int resId) {
        try {
            if (current == null) return false;
            if (containsResId(current, resId, 0)) return true;
            // 兜底：ConstantState 对象相等（shape / bitmap 共享缓存的情况）
            Drawable target = ContextCompat.getDrawable(activity, resId);
            if (target == null) return false;
            Drawable.ConstantState a = current.getConstantState();
            Drawable.ConstantState b = target.getConstantState();
            return a != null && b != null && a == b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** 递归判断 drawable 自身或其子层是否来自指定资源 id */
    private static boolean containsResId(Drawable d, int resId, int depth) {
        if (d == null || depth > 4) return false;
        if (reflectResourceId(d) == resId) return true;
        try {
            // RippleDrawable 不是 DrawableContainer，但它有 getDrawable(int) / getLayerCount，
            // 需要单独处理它的内容层与遮罩层。
            if (d instanceof android.graphics.drawable.RippleDrawable) {
                android.graphics.drawable.RippleDrawable r = (android.graphics.drawable.RippleDrawable) d;
                int n = r.getNumberOfLayers();
                for (int i = 0; i < n; i++) {
                    if (containsResId(r.getDrawable(i), resId, depth + 1)) return true;
                }
            }
            if (d instanceof android.graphics.drawable.LayerDrawable) {
                android.graphics.drawable.LayerDrawable l = (android.graphics.drawable.LayerDrawable) d;
                for (int i = 0; i < l.getNumberOfLayers(); i++) {
                    if (containsResId(l.getDrawable(i), resId, depth + 1)) return true;
                }
            }
            if (d instanceof android.graphics.drawable.DrawableContainer) {
                Drawable.ConstantState cs = d.getConstantState();
                if (cs != null) {
                    java.lang.reflect.Method m = cs.getClass().getDeclaredMethod("getChildren");
                    m.setAccessible(true);
                    Object arr = m.invoke(cs);
                    if (arr instanceof Drawable[]) {
                        for (Drawable child : (Drawable[]) arr) {
                            if (containsResId(child, resId, depth + 1)) return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 反射读取 Drawable.mResourceId（隐藏字段，但各版本稳定存在） */
    private static int reflectResourceId(Drawable d) {
        if (d == null) return 0;
        try {
            java.lang.reflect.Field f = Drawable.class.getDeclaredField("mResourceId");
            f.setAccessible(true);
            Object v = f.get(d);
            if (v instanceof Integer) return (Integer) v;
        } catch (Throwable ignored) {
        }
        return 0;
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
