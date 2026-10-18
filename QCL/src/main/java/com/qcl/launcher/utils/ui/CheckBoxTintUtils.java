package com.qcl.launcher.utils.ui;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Switch;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**让所有「勾选类控件」跟着主题色走**。
 *
 * <p>用户反馈：「还有一些勾选颜色的问题，还有一些颜色的问题都没有统一」。
 *
 * <p>★ 根因：工程里 26 个 {@link CheckBox} **没有一个设 {@code buttonTint}**，
 * 它们的勾选色全部来自主题的 {@code colorAccent}，而 {@code colorAccent} 在
 * {@code values/colors.xml} 里是**写死的 {@code #556980}**（日夜都不变）。
 * 于是：
 * <ul>
 *   <li>玩家在「设置 → 外观 → 主题色」里换成**别的颜色**时，
 *       面板/按钮会跟着变，但**勾选框还是那个灰蓝色** —— 明显不统一；</li>
 *   <li>夜间模式下 {@code #556980} 压在深底上对比度也偏低。</li>
 * </ul>
 *
 * <p>★ 做法：运行时遍历 view 树，把**主题色**灌进所有勾选类控件：
 * <ul>
 *   <li>{@link CheckBox} / {@link RadioButton} / {@link Switch} →
 *       设 {@code buttonTintList}（勾选框本体）与 {@code thumbTintList} / {@code trackTintList}（开关）；</li>
 *   <li>{@link ProgressBar} → 设 {@code indeterminateTintList}（进度圈）—— 它和勾选框是同一类"强调色"，
 *       一起改才不会出现"勾选框变了、进度圈没变"。</li>
 * </ul>
 * 用 {@link ColorStateList#valueOf(int)} 生成单色状态表，选中/未选中都统一。
 */
public final class CheckBoxTintUtils {

    private CheckBoxTintUtils() {
    }

    /**
     * 把主题色应用到该 Activity 里所有勾选类控件。
     *
     * @param activity 目标 Activity
     * @param color    主题色（ARGB）。传 0 表示不改（保持 XML/主题默认）
     */
    public static void apply(Activity activity, int color) {
        if (activity == null || color == 0) {
            return;
        }
        try {
            View root = activity.getWindow() == null
                    ? null : activity.getWindow().getDecorView();
            if (root == null) {
                return;
            }
            tintRecursive(root, color);
        } catch (Throwable ignored) {
        }
    }

    /**
     * ★ 1.5.2：给**任意 View 树**染色（对话框专用）。
     *
     * <p>Dialog / AlertDialog 是独立 Window，其 view 树不在 Activity 里，
     * 用 {@link #apply(Activity, int)} 遍历不到它们。
     */
    public static void apply(View root, int color) {
        if (root == null || color == 0) {
            return;
        }
        try {
            tintRecursive(root, color);
        } catch (Throwable ignored) {
        }
    }

    private static void tintRecursive(View view, int color) {
        if (view == null) {
            return;
        }
        try {
            if (view instanceof CheckBox) {
                ((CheckBox) view).setButtonTintList(ColorStateList.valueOf(color));
            } else if (view instanceof RadioButton) {
                ((RadioButton) view).setButtonTintList(ColorStateList.valueOf(color));
            } else if (view instanceof Switch) {
                Switch sw = (Switch) view;
                ColorStateList cs = ColorStateList.valueOf(color);
                sw.setThumbTintList(cs);
                sw.setTrackTintList(cs);
            } else if (view instanceof CompoundButton) {
                ((CompoundButton) view).setButtonTintList(ColorStateList.valueOf(color));
            } else if (view instanceof ProgressBar) {
                ProgressBar pb = (ProgressBar) view;
                ColorStateList cs = ColorStateList.valueOf(color);
                pb.setIndeterminateTintList(cs);
                pb.setProgressTintList(cs);
            } else if (view instanceof ImageView) {
                // 部分"勾选态图标"是用 ImageView + background 做的，统一染一下着色
                // （只对已设 tint 的生效，不会破坏普通图片）
                ImageView iv = (ImageView) view;
                Drawable d = iv.getDrawable();
                if (d != null && iv.getImageTintList() != null) {
                    iv.setImageTintList(ColorStateList.valueOf(color));
                }
            }
        } catch (Throwable ignored) {
        }
        if (view instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) view;
            int n = g.getChildCount();
            for (int i = 0; i < n; i++) {
                tintRecursive(g.getChildAt(i), color);
            }
        }
    }
}
