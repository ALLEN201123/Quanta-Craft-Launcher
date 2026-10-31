package com.qcl.launcher.launcher.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Build;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.qcl.launcher.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * ★★★★★ 1.5.8：**查看上次启动日志**。
 *
 * <p><b>为什么需要它</b>：玩家反馈「崩了之后连崩溃弹窗都不给我弹出来 —— 就是在游戏
 * 启动中的等待界面就崩了」。
 *
 * <p>根因：崩溃发生在 <b>等待界面</b>（{@code PojavMinecraftActivity} 还没完全起来 /
 * 游戏进程在 JVM 启动阶段就死了）—— 这时候：
 * <ul>
 *   <li>{@code LaunchLogWindow} 的浮窗**跟着活动一起没了**（进程被杀）</li>
 *   <li>崩溃弹窗要等游戏进程把堆栈写进 {@code crash-reports/} 才能弹，
 *       而这种早期死亡**根本没到写崩溃报告那一步**</li>
 * </ul>
 *
 * <p>所以只能靠**日志文件**：{@code <外部files>/debug/pojav_latest_log.txt}。
 * 这个对话框直接读它，**不依赖游戏进程活着**。
 *
 * <p>功能：
 * <ul>
 *   <li>显示日志**末尾 N 行**（默认 2000 行，够看崩溃栈）</li>
 *   <li>等宽小字号 + 可滚动 + 自动滚到底（崩溃在最后）</li>
 *   <li>「复制全部」按钮 → 剪贴板（方便玩家发给作者）</li>
 *   <li>「清空」按钮 → 删掉旧日志（下次启动干净）</li>
 *   <li>显示文件大小 / 修改时间 / 行数，判断有没有内容</li>
 * </ul>
 *
 * <p>★ 设计原则：**任何异常都不能崩**（这个功能是给「已经崩了」的玩家用的，
 * 它自己再崩就没意义了）。
 */
public final class LastLaunchLogDialog {

    /** 日志文件相对 {@code getExternalFilesDir("debug")} 的文件名。 */
    private static final String LOG_NAME = "pojav_latest_log.txt";

    /** 默认最多读多少行（够覆盖一次崩溃的输出，又不会卡 UI）。 */
    private static final int DEFAULT_MAX_LINES = 2000;

    private LastLaunchLogDialog() {
    }

    /** 日志文件的绝对路径（找不到外部目录时返回 null）。 */
    public static File logFileOf(Activity activity) {
        try {
            File dir = activity.getExternalFilesDir("debug");
            if (dir == null) {
                return null;
            }
            return new File(dir, LOG_NAME);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 弹出「上次启动日志」对话框。
     *
     * @param activity 宿主 Activity
     */
    public static void show(final Activity activity) {
        if (activity == null) {
            return;
        }
        File logFile = logFileOf(activity);

        // ---------- 读日志（读不到就给明确提示，不要空白） ----------
        final String content;
        final String header;
        if (logFile == null) {
            content = activity.getString(R.string.qcl_last_log_no_external_dir);
            header = activity.getString(R.string.qcl_last_log_title);
        } else if (!logFile.isFile() || logFile.length() == 0) {
            content = activity.getString(R.string.qcl_last_log_empty);
            header = activity.getString(R.string.qcl_last_log_title);
        } else {
            content = readTail(logFile, DEFAULT_MAX_LINES);
            header = activity.getString(R.string.qcl_last_log_header,
                    humanSize(logFile.length()),
                    new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US)
                            .format(new java.util.Date(logFile.lastModified())));
        }

        // ---------- 正文：等宽小字 + 可滚动 ----------
        // ★★★★★ 1.5.8 修（用户白天实测「白底 + 白字，字母连毛都看不见」）：
        //   原来三处颜色是**硬编码深色**，Dialog 外壳却走主题（白天白底）
        //   ⇒ 白天糊成一片。现在全部按主题明暗动态算，不再硬编码。
        final boolean dark = isDarkTheme(activity);
        final int colorLogBg = dark ? 0xFF141414 : 0xFFFAFAFA;   // 正文底色
        final int colorLogFg = dark ? 0xFFE8E8E8 : 0xFF1A1A1A;   // 正文文字
        final int colorBorder = dark ? 0xFF3A3A3A : 0xFFD0D0D0;  // 边框
        final int colorHint = dark ? 0xFF9A9A9A : 0xFF6A6A6A;    // 副标题

        final TextView body = new TextView(activity);
        body.setText(content);
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        body.setTypeface(android.graphics.Typeface.MONOSPACE);
        body.setTextColor(colorLogFg);
        body.setBackgroundColor(colorLogBg);
        body.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 10), dp(activity, 8));
        body.setMovementMethod(new ScrollingMovementMethod());
        body.setTextIsSelectable(true);
        // 细边框，避免正文块和对话框背景同色糊在一起
        try {
            android.graphics.drawable.GradientDrawable bg =
                    new android.graphics.drawable.GradientDrawable();
            bg.setColor(colorLogBg);
            bg.setCornerRadius(dp(activity, 4));
            bg.setStroke(Math.max(1, dp(activity, 1)), colorBorder);
            body.setBackground(bg);
        } catch (Throwable ignored) {
        }

        final ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.addView(body, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 给一个固定高度（屏幕 62%），保证「能看见内容」而不是被挤成一条线
        int h = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.62f);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), 0);

        TextView head = new TextView(activity);
        head.setText(header);
        head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        head.setTextColor(colorHint);
        head.setPadding(0, 0, 0, dp(activity, 8));
        root.addView(head, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, h));

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setTitle(R.string.qcl_last_log_title)
                .setView(root)
                .setPositiveButton(R.string.qcl_last_log_copy, (d, w) -> {
                    copyToClipboard(activity, content);
                    Toast.makeText(activity, R.string.qcl_last_log_copied,
                            Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.qcl_last_log_clear, (d, w) -> {
                    if (logFile != null && logFile.isFile()) {
                        //noinspection ResultOfMethodCallIgnored
                        logFile.delete();
                    }
                    Toast.makeText(activity, R.string.qcl_last_log_cleared,
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = b.create();
        dialog.show();

        // 创建后自动滚到底（崩溃在最后，玩家最想看的是末尾）
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));

        // 主题色：Dialog 是独立 Window，MainActivity 的遍历够不到它 —— 手动同步一次。
        //
        // ★ 1.5.8 修：**刷完之后必须把正文/副标题的颜色再钉一次**。
        //   ThemeRefreshUtils.applyTo 会把遍历到的可疑按钮/文字统一 tint 成主题色，
        //   它不认识「日志正文」这种要自己配色的控件 —— 一旦被它刷成主题色（白天往往是白字），
        //   叠在我的浅色底上就完全看不见（用户实测的原话：「白底配色加白色字母」）。
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(R.attr.colorAccent, tv, true)
                    && tv.data != 0 && dialog.getWindow() != null) {
                com.qcl.launcher.utils.ui.ThemeRefreshUtils.applyTo(
                        dialog.getWindow().getDecorView(), tv.data);
            }
        } catch (Throwable ignored) {
        }
        // 重新钉回正文与副标题的自有配色（必须放在 applyTo 之后）
        try {
            body.setTextColor(colorLogFg);
            android.graphics.drawable.GradientDrawable bg2 =
                    new android.graphics.drawable.GradientDrawable();
            bg2.setColor(colorLogBg);
            bg2.setCornerRadius(dp(activity, 4));
            bg2.setStroke(Math.max(1, dp(activity, 1)), colorBorder);
            body.setBackground(bg2);
            head.setTextColor(colorHint);
        } catch (Throwable ignored) {
        }
    }

    // ======================================================================
    //  内部工具
    // ======================================================================

    /**
     * 读文件**末尾** maxLines 行。
     *
     * <p>为什么不用「全读再截」：日志可能几十 MB，全读进内存会 OOM。
     * 这里用「两遍法」：先按字节粗扫数行数（只数 \n），算出起始偏移，再精确读。
     * 简单可靠，且对 UTF-8 多字节字符安全（按字节跳，不切字符边界时靠
     * InputStreamReader 自行处理残余半字节）。
     */
    private static String readTail(File file, int maxLines) {
        try {
            long len = file.length();
            // 单行平均 80 字节估算：够读 maxLines 行就多留 4 倍余量
            long guess = (long) maxLines * 80L * 4L;
            long from = Math.max(0L, len - guess);

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(file), "UTF-8"));
            try {
                if (from > 0) {
                    //noinspection ResultOfMethodCallIgnored
                    br.skip(from);
                    // 跳过可能被切断的半行
                    br.readLine();
                }
                java.util.ArrayDeque<String> ring = new java.util.ArrayDeque<>(maxLines + 16);
                String line;
                while ((line = br.readLine()) != null) {
                    if (ring.size() >= maxLines) {
                        ring.pollFirst();
                    }
                    ring.addLast(line);
                }
                StringBuilder sb = new StringBuilder(ring.size() * 60);
                for (String s : ring) {
                    sb.append(s).append('\n');
                }
                return sb.toString();
            } finally {
                try {
                    br.close();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            return "读取日志失败：" + t.getClass().getSimpleName()
                    + (t.getMessage() == null ? "" : "：" + t.getMessage());
        }
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0);
    }

    private static void copyToClipboard(Activity activity, String text) {
        try {
            Object svc = activity.getSystemService(Activity.CLIPBOARD_SERVICE);
            if (svc instanceof android.content.ClipboardManager) {
                ((android.content.ClipboardManager) svc)
                        .setPrimaryClip(android.content.ClipData.newPlainText("QCL log", text));
            }
        } catch (Throwable ignored) {
        }
    }

    private static int dp(Activity a, int v) {
        return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * ★ 1.5.8：判断当前主题是深色还是浅色。
     *
     * <p>做法：读主题的 {@code android.R.attr.colorBackground}（对话框背景色），
     * 算**相对亮度**（0~1），&lt; 0.5 视为深色主题。
     *
     * <p>为什么不用 {@code uiMode & UI_MODE_NIGHT_MASK}：QCL 的夜间/日间是**自己实现**的
     * （appearance 里的开关 + 主题色），不走系统 uiMode，所以那个标志在这里不可靠。
     *
     * <p>取不到 colorBackground 时兜底：按 colorOnBackground 的亮度反推；
     * 都取不到就按深色（QCL 默认主题是深色）。
     */
    private static boolean isDarkTheme(Activity activity) {
        try {
            android.util.TypedValue tv = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.colorBackground, tv, true)) {
                int c = tv.data;
                if (c != 0) {
                    return luminance(c) < 0.5f;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            android.util.TypedValue tv2 = new android.util.TypedValue();
            if (activity.getTheme().resolveAttribute(android.R.attr.colorForeground, tv2, true)) {
                int c2 = tv2.data;
                if (c2 != 0) {
                    // 前景色亮 ⇒ 说明是深色主题
                    return luminance(c2) > 0.5f;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    /** 相对亮度（0~1），按 WCAG 的加权公式。 */
    private static float luminance(int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }

}
