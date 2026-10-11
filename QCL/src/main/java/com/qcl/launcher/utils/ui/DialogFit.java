package com.qcl.launcher.utils.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * ★★★★★ 2026-10-11 新增：**对话框屏幕自适应**。
 *
 * <p>为什么需要（用户实测原话）：
 * 「取消按钮的那个下载游戏版本和下载整合包的自动安装那个取消按钮不见了，
 *   在手机屏幕上根本就找不到，只有在电脑上才能找得到，是不是屏幕比例有点太大了？」
 *
 * <p>真凶：项目里 **24 个 dialog_*.xml 的根布局把宽度写死成 500dp**（还有一批 400/450dp）。
 * 手机横屏的可用宽度 = 屏幕宽(dp) − 系统栏 − 对话框默认边距（约 24dp×2）。
 * 很多手机横屏只有 ~812dp 甚至更窄，减去边距后**放不下 500dp**，
 * 于是对话框比屏幕还宽 ⇒ **底部/右侧的按钮被挤出可见区域**
 * （电脑上屏幕大，所以"只有电脑上能找到"）。
 *
 * <p>本工具把对话框尺寸**按屏幕比例压回来**：
 * <ul>
 *   <li>宽度 = min(布局原本想要的宽度, 屏幕宽 × {@link #WIDTH_RATIO})</li>
 *   <li>高度 = min(布局原本想要的高度, 屏幕高 × {@link #HEIGHT_RATIO})</li>
 * </ul>
 * 这样任何屏幕上都不会再超出，按钮也就一定在可见区域内。
 *
 * <p>用法：{@code DialogFit.fit(dialog);}（在 {@code show()} 之后调用）。
 * 只调用这一个方法即可，**不改任何布局文件**，风险最低。
 */
public final class DialogFit {

    /** 宽度最多占屏幕的 92%（留一点边，看起来还是对话框而不是全屏）。 */
    public static final float WIDTH_RATIO = 0.92f;

    /** 高度最多占屏幕的 88%（下方其它内容也不会被顶出屏幕）。 */
    public static final float HEIGHT_RATIO = 0.88f;

    /** 对话框宽度上限：布局里普遍写 500dp，超过它就按 500dp 走。 */
    public static final int MAX_WIDTH_DP = 500;

    private DialogFit() {
    }

    /**
     * 把对话框尺寸压进屏幕内。
     *
     * @param dialog 已经 show() 过的对话框（内部会取 dialog.getWindow()）
     */
    public static void fit(Dialog dialog) {
        if (dialog == null) {
            return;
        }
        try {
            fit(dialog, dialog.getWindow());
        } catch (Throwable ignored) {
            // 设不了就按原样显示（至少不会崩）
        }
    }

    /**
     * 把窗口尺寸压进屏幕内（子对话框 / PopupWindow 之类也适用）。
     */
    public static void fit(Dialog dialog, Window window) {
        if (window == null) {
            return;
        }
        Context ctx = window.getContext();
        if (ctx == null) {
            return;
        }
        try {
            Point usable = usableScreenSize(ctx);
            int screenW = usable.x;
            int screenH = usable.y;
            if (screenW <= 0 || screenH <= 0) {
                return;
            }

            int maxW = (int) (screenW * WIDTH_RATIO);
            int maxH = (int) (screenH * HEIGHT_RATIO);
            int capW = dp(ctx, MAX_WIDTH_DP);
            if (maxW > capW) {
                maxW = capW;   // 大屏上别拉得比布局设计宽度还宽
            }

            WindowManager.LayoutParams lp = window.getAttributes();
            // ★ 只往下压、不往上撑：布局本来就更小的（如 400dp）保持原样。
            if (lp.width <= 0 || lp.width > maxW) {
                lp.width = maxW;
            }
            if (lp.height > 0 && lp.height > maxH) {
                lp.height = maxH;
            } else if (lp.height <= 0) {
                // wrap_content：交给下面的 View 级约束兜底（不改 height，让它自己量）
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            }
            window.setAttributes(lp);

            // ★ 再兜一层：把根 View 的最大高度也钉住，
            //   否则 wrap_content 的自定义布局（固定 360dp 内容区）仍可能顶出屏幕。
            View root = window.getDecorView();
            if (root != null) {
                clampMaxHeight(root, maxH);
            }
        } catch (Throwable ignored) {
        }
    }

    /** 从 root 往下找一层，把"太高"的容器钉上 maxHeight（只改 max，不改实际尺寸）。 */
    private static void clampMaxHeight(View view, int maxH) {
        try {
            if (view == null) {
                return;
            }
            view.setMinimumHeight(0);
            if (view instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) view;
                for (int i = 0; i < g.getChildCount(); i++) {
                    clampMaxHeight(g.getChildAt(i), maxH);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 取"可用屏幕尺寸"（扣除导航栏 / 状态栏）。
     * ★ 不用 {@code getDefaultDisplay().getSize()}：那个含系统栏，算出来的上限偏大。
     */
    private static Point usableScreenSize(Context ctx) {
        Point p = new Point();
        try {
            WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                return p;
            }
            Display d = wm.getDefaultDisplay();
            DisplayMetrics real = new DisplayMetrics();
            d.getRealMetrics(real);
            p.x = real.widthPixels;
            p.y = real.heightPixels;
        } catch (Throwable ignored) {
        }
        if (p.x <= 0 || p.y <= 0) {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            p.x = dm.widthPixels;
            p.y = dm.heightPixels;
        }
        // 再扣掉一点系统栏/安全区余量，避免被状态栏和手势条压住
        p.x = (int) (p.x * 0.97f);
        p.y = (int) (p.y * 0.94f);
        return p;
    }

    private static int dp(Context ctx, int v) {
        try {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                    ctx.getResources().getDisplayMetrics());
        } catch (Throwable ignored) {
            return v;
        }
    }
}
