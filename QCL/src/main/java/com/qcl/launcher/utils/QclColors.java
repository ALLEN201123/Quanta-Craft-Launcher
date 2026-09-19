package com.qcl.launcher.utils;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

/**
 * 颜色解析 / 格式化的统一入口。
 *
 * <p>背景：项目里长期用 {@code "#" + Integer.toHexString(color)} 保存颜色，而
 * {@code Integer.toHexString} **不补零** —— 颜色值高位为 0 时会产出
 * {@code "#ff"}（2 位）、{@code "#0"}（1 位）这类**非法颜色串**，写进
 * launcher_setting.json / 布局 json 后，下次读取时 {@code Color.parseColor} 抛
 * {@code IllegalArgumentException: Unknown color} → 启动即崩（真机 OPPO PDVM00 实测）。
 *
 * <p>本类提供两个方向的修复：
 * <ul>
 *   <li>{@link #format(int)} —— 保存侧，统一输出合法的 8 位 {@code #AARRGGBB}；</li>
 *   <li>{@link #parseSafe(String, int)} —— 读取侧，null/空/DEFAULT/历史非法值一律回退默认色，
 *       保证老存档里的坏值不会再引发崩溃。</li>
 * </ul>
 */
public final class QclColors {

    private static final String TAG = "jrelog";

    private QclColors() {
    }

    /** 颜色 → 合法的 {@code #AARRGGBB} 字符串（补零，绝不会产出 "#ff" 这类非法串）。 */
    public static String format(int color) {
        return String.format("#%08X", color);
    }

    /** 安全解析颜色；null / 空 / "DEFAULT" / 任何非法值 → 返回 fallback。 */
    public static int parseSafe(String color, int fallback) {
        if (color == null) {
            return fallback;
        }
        String s = color.trim();
        if (s.isEmpty() || "DEFAULT".equalsIgnoreCase(s)) {
            return fallback;
        }
        try {
            return Color.parseColor(s);
        } catch (Throwable t) {
            Log.w(TAG, "[颜色] 非法颜色值已回退默认: " + s);
            return fallback;
        }
    }

    /** 安全解析颜色；回退值取资源里的颜色（如 R.color.colorAccent）。 */
    public static int parseSafe(Context context, String color, int fallbackRes) {
        return parseSafe(color, context.getResources().getColor(fallbackRes));
    }

    /** 安全解析颜色；回退到主题强调色（用于启动期主题着色）。 */
    public static int parseSafeTheme(Context context, String color, int fallbackRes) {
        return parseSafe(color, context.getColor(fallbackRes));
    }
}
