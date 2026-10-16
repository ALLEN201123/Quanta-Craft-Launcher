package com.qcl.launcher.utils.ui;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qcl.launcher.R;

/**
 * ★★★★★ 2026-10-16 新增：**全局自定义字体颜色**。
 *
 * <p>用户需求：「在外观那里加一个字体颜色自定义调节功能」。
 *
 * <p>为什么不能直接改布局里的 {@code android:textColor="@color/colorPureBlack"}：
 * 全工程有 **946 个布局**引用那个色当正文色，逐文件改既不可能维护、也会把
 * 「夜间模式自动翻白」的机制一起破坏掉。
 *
 * <p>所以采用**运行时着色**：拿到用户设定的颜色后，遍历当前 Activity 的视图树，
 * 把「正文类 TextView」的文字色换掉。判定规则保证**不会破坏其它控件**：
 *
 * <ol>
 *   <li><b>只改 TextView</b>（按钮里的白字多数是 Button/自定义控件，不受影响）；</li>
 *   <li>只改**当前文字色等于「正文色」**的 TextView
 *       —— 也就是原本写 {@code @color/colorPureBlack} 的那批。
 *       开发者特意用别的颜色标出来的（链接蓝、警示红、按钮白）**一律不动**；</li>
 *   <li>颜色为 0（= 未设置 / 恢复默认）时**什么都不做**，
 *       完全等于旧行为，绝不引入回归。</li>
 * </ol>
 *
 * <p>调用点：
 * <ul>
 *   <li>{@code MainActivity} 每次进入前台（{@code onResume}）后应用一次 ——
 *       这样所有子页面都会跟着变；</li>
 *   <li>外观设置里改完颜色后立刻应用一次，用户当场就能看到效果。</li>
 * </ul>
 */
public final class FontColorUtils {

    private FontColorUtils() {
    }

    /**
     * 把 Activity 里所有「正文类 TextView」的颜色改成 color。
     *
     * @param activity 目标 Activity
     * @param color    用户设定的颜色（含 alpha）。<b>0 或无效值时直接返回、不做任何改动。</b>
     */
    public static void apply(Activity activity, int color) {
        if (activity == null || color == 0) {
            return;
        }
        try {
            View root = activity.getWindow() == null
                    ? null : activity.getWindow().getDecorView();
            if (root == null) {
                root = activity.findViewById(android.R.id.content);
            }
            if (root == null) {
                return;
            }
            // 原「正文色」= 布局里写的 @color/colorPureBlack（日间黑 / 夜间白）
            int bodyColor = activity.getResources().getColor(R.color.colorPureBlack);
            recolor(root, color, bodyColor);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 递归着色。
     *
     * @param v         当前节点
     * @param want      用户想要的颜色
     * @param bodyColor 「正文色」基准（只有等于它的才改，避免误伤其它控件）
     */
    private static void recolor(View v, int want, int bodyColor) {
        try {
            if (v == null) {
                return;
            }
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                // ★ 关键：只改「本来就是正文色」的，别把按钮白字/链接蓝/警示红也改掉
                if (tv.getCurrentTextColor() == bodyColor) {
                    tv.setTextColor(want);
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                int n = g.getChildCount();
                for (int i = 0; i < n; i++) {
                    recolor(g.getChildAt(i), want, bodyColor);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 把 {@code #RRGGBB} / {@code #AARRGGBB} 字符串安全解析成颜色；失败返回 0。 */
    public static int parse(String s) {
        if (s == null) {
            return 0;
        }
        String t = s.trim();
        if (t.isEmpty() || "DEFAULT".equalsIgnoreCase(t)) {
            return 0;
        }
        try {
            return Color.parseColor(t);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
