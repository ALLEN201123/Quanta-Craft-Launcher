package com.qcl.launcher.utils.string;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;

import com.qcl.launcher.R;

import androidx.core.graphics.drawable.DrawableCompat;

/**
 * ★★★ 1.5.0 新增：把「皮肤 / 换肤」这几个对话框里的按钮统一染成**深色 + 浅色字**，
 * 并让底色**跟随玩家自定义的主题色**。
 *
 * <p>★ 为什么需要这个（2026-10-09 实测踩坑）：
 * <ul>
 *   <li>{@code launcher_button_parent} 是**透明底**（{@code colorTransparent}），
 *       按钮按下去才显形 ⇒ 看着永远是白块；</li>
 *   <li>改用 {@code ?android:attr/colorAccent} 也不行 —— 本项目主题定义的是
 *       AppCompat 的 {@code colorAccent}，framework 那个属性**取不到值** ⇒ 同样白块。</li>
 * </ul>
 * ⇒ 正解就是本类：drawable 只给深色形状，运行时用
 * {@code Color.parseColor(getThemeColor(...))} 把主题色解析出来再
 * {@link DrawableCompat#setTint} 上去。
 *
 * <p>★ 文字色固定用 {@code qcl_dialog_btn_text}（日夜都是浅色）——
 * 用户明确要求「按钮转深色，但字不转」。
 */
public final class SkinDialogUtils {

    private SkinDialogUtils() {
    }

    /**
     * 把一个按钮染成「深色底 + 浅色字」，底色跟随当前主题色。
     *
     * @param themeColorSetting 玩家在设置里存的主题色字符串（可能为 null / "DEFAULT"）
     */
    public static void tintButton(Context context, Button btn, String themeColorSetting) {
        if (context == null || btn == null) {
            return;
        }
        try {
            int theme = resolveThemeColor(context, themeColorSetting);
            Drawable bg = btn.getBackground();
            if (bg != null) {
                // ★ 必须是 CONSTANT 默认状态：按钮处于 pressed/focused 时
                //   DrawableCompat.setTint 会把 selector 里每个 item 都染一遍。
                DrawableCompat.setTint(DrawableCompat.wrap(bg.mutate()), theme);
                btn.setBackground(DrawableCompat.wrap(bg));
            }
            btn.setTextColor(context.getResources().getColor(R.color.qcl_dialog_btn_text));
        } catch (Throwable ignored) {
            // 染不上就算了：drawable 本身已是深色兜底色，不会退化成白块
        }
    }

    /** 批量染一个布局里的所有按钮。 */
    public static void tintAll(Context context, View root, String themeColorSetting) {
        if (context == null || root == null) {
            return;
        }
        try {
            if (root instanceof Button) {
                tintButton(context, (Button) root, themeColorSetting);
            }
            if (root instanceof android.view.ViewGroup) {
                android.view.ViewGroup g = (android.view.ViewGroup) root;
                for (int i = 0; i < g.getChildCount(); i++) {
                    tintAll(context, g.getChildAt(i), themeColorSetting);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /** 解析主题色成 int：非法 / 空值一律回退项目默认的 colorAccent。 */
    public static int resolveThemeColor(Context context, String themeColorSetting) {
        int fallback = context.getColor(R.color.colorAccent);
        if (themeColorSetting == null || "DEFAULT".equals(themeColorSetting)
                || themeColorSetting.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Color.parseColor(themeColorSetting.trim());
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}