package com.qcl.launcher.utils.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.qcl.launcher.R;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**改主题色后立即刷新界面，不需要重启**。
 *
 * <p>用户反馈：「主菜单左侧主栏的主题颜色，还必须退出重新登录才行；
 * 而且在『外观』里面改颜色，也必须要退出重新登录，需要修复」。
 *
 * <p>★ 根因：`ExteriorSettingUI` 改主题色时只做了三件事：
 * <pre>
 *   activity.launcherSetting.launcherTheme = 新色;
 *   activity.exteriorConfig.primaryColor(新色);
 *   activity.exteriorConfig.accentColor(新色);
 *   activity.exteriorConfig.apply(activity);     ← 只改「主题定义」
 * </pre>
 * 而 **已经 inflate 出来的 View 不会重读主题**：
 * <ul>
 *   <li>左侧导航栏背景是 {@code @color/qcl_nav_panel_bg}（一个**固定 color 资源**），
 *       已经画上去就不会变；</li>
 *   <li>所有 {@code ?attr/colorAccent} 引用的 drawable 同理。</li>
 * </ul>
 * ⇒ 于是必须 {@code recreate()} 才能看到新色，玩家感受就是"改了没用，得退出重进"。
 *
 * <p>★ 本类的做法：不重建 Activity，而是**当场把新色刷到已存在的 View 上**：
 * <ol>
 *   <li>左侧导航栏（{@code R.id.new_left_nav}）→ 直接 {@code setBackgroundColor}
 *       成「主题色 + 半透明」，立即见效；</li>
 *   <li>所有 CheckBox / Switch / ProgressBar → 交给 {@link CheckBoxTintUtils}；</li>
 *   <li>背景是 {@link GradientDrawable}（纯色 shape）的按钮 → 给它换底色，
 *       让按钮跟着主题色走（不动带图片/描边的那些，避免把图标染坏）。</li>
 * </ol>
 */
public final class ThemeRefreshUtils {

    private ThemeRefreshUtils() {
    }

    /**
     * 把新主题色立即刷到当前 Activity 的界面上。
     *
     * @param activity 目标 Activity
     * @param color    新的主题色（ARGB）
     */
    public static void apply(Activity activity, int color) {
        if (activity == null || color == 0) {
            return;
        }
        try {
            // ① 左侧导航栏：它是"主栏主题色"的直接载体，优先刷。
            //   ★ 新排版下左栏是**运行时 inflate** 到 new_left_nav_host 里的，
            //     所以既可能是 new_left_nav 本身，也可能要先从 host 里取唯一子 View。
            View nav = activity.findViewById(R.id.new_left_nav);
            if (nav == null) {
                View host = activity.findViewById(R.id.new_left_nav_host);
                if (host instanceof ViewGroup && ((ViewGroup) host).getChildCount() > 0) {
                    nav = ((ViewGroup) host).getChildAt(0);
                }
            }
            if (nav != null) {
                // 与原来的 qcl_nav_panel_bg 同样的层次：主题色 + 80% 不透明，
                // 保证壁纸还透得出来一点，同时图标/文字清晰。
                nav.setBackgroundColor((color & 0x00FFFFFF) | 0xCC000000);
            }
            // ★★★★★ 1.5.2：**左栏所有图标也跟随主题色**。
            //   用户反馈：「你新加的那个 java 按钮也没有到那个主题色那里」。
            //   根因：左栏图标都是白色 drawable（ic_qcl_*_white），从来没有被 tint 过；
            //        新加的 JAR 图标（ic_qcl_jar_white）自然也一样"没到主题色"。
            //   ⇒ 统一给左栏里所有 ImageView 的图标设主题色 tint，
            //     这样无论新旧图标都跟着主题走（视觉统一）。
            tintNavIcons(nav, color);
        } catch (Throwable ignored) {
        }
        try {
            // ② 勾选类 / 进度圈
            CheckBoxTintUtils.apply(activity, color);
        } catch (Throwable ignored) {
        }
        try {
            // ③ 纯色 shape 背景的按钮 → 换底色
            View root = activity.getWindow() == null
                    ? null : activity.getWindow().getDecorView();
            if (root != null) {
                retintButtons(root, color);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void retintButtons(View view, int color) {
        if (view == null) {
            return;
        }
        try {
            if ((view instanceof Button || view instanceof ImageButton)
                    && !(view instanceof android.widget.CompoundButton)) {
                Drawable bg = view.getBackground();
                // 只处理「纯色 shape」——带 bitmap / layer-list 的跳过，避免把图标染坏
                if (bg instanceof GradientDrawable) {
                    GradientDrawable gd = (GradientDrawable) bg;
                    // mutate() 防止影响共用同一 drawable 的其它控件
                    Drawable m = gd.mutate();
                    if (m instanceof GradientDrawable) {
                        ((GradientDrawable) m).setColor(color);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) {
                retintButtons(g.getChildAt(i), color);
            }
        }
    }

    /**
     * 导航栏的「透明档」：把主题色按给定不透明度合成，用于需要更通透的场景。
     */
    public static int withAlpha(int color, int alpha0to255) {
        int a = Math.max(0, Math.min(255, alpha0to255));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** 便捷：给某个具体 View 设「主题色 + 半透明」底色（比如其它自定义栏）。 */
    public static void tintBackground(View v, int color) {
        if (v == null || color == 0) {
            return;
        }
        try {
            v.setBackgroundColor(withAlpha(color, 0xCC));
        } catch (Throwable ignored) {
        }
    }

    /** 便捷：把某个 View 的背景 tint 设成主题色（保留原 drawable 的形状）。 */
    public static void tintBackgroundList(View v, int color) {
        if (v == null || color == 0) {
            return;
        }
        try {
            v.setBackgroundTintList(ColorStateList.valueOf(color));
        } catch (Throwable ignored) {
        }
    }

    /** 供 {@link LinearLayout} 等类型判断用（避免误 import 警告）。 */
    static boolean isPlainContainer(View v) {
        return v instanceof LinearLayout;
    }

    /**
     * ★ 1.5.2：把左栏里所有 {@link ImageView} 的**图标**染成主题色。
     *
     * <p>左栏图标原本是白色 drawable（`ic_qcl_*_white`），从来没跟随过主题色。
     * 用户点名：「你新加的那个 java 按钮也没有到那个主题色那里」——
     * 其实**所有**左栏图标都一样，所以这里统一处理，新旧一致。
     */
    private static void tintNavIcons(View view, int color) {
        if (view == null) {
            return;
        }
        try {
            if (view instanceof ImageView) {
                ImageView iv = (ImageView) view;
                // 只给「有 src/background 图标」的染色；
                // 用 SRC_ATOP 保留图形本身的透明度轮廓。
                iv.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        } catch (Throwable ignored) {
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            for (int i = 0; i < g.getChildCount(); i++) {
                tintNavIcons(g.getChildAt(i), color);
            }
        }
    }

    /** 清除左栏图标染色（回到白色原图）。 */
    public static void clearNavIconTint(Activity activity) {
        try {
            View nav = activity == null ? null : activity.findViewById(R.id.new_left_nav);
            if (nav == null) {
                View host = activity.findViewById(R.id.new_left_nav_host);
                if (host instanceof ViewGroup && ((ViewGroup) host).getChildCount() > 0) {
                    nav = ((ViewGroup) host).getChildAt(0);
                }
            }
            clearTintRecursive(nav);
        } catch (Throwable ignored) {
        }
    }

    private static void clearTintRecursive(View v) {
        if (v == null) {
            return;
        }
        try {
            if (v instanceof ImageView) {
                ((ImageView) v).clearColorFilter();
            }
        } catch (Throwable ignored) {
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                clearTintRecursive(g.getChildAt(i));
            }
        }
    }
}
