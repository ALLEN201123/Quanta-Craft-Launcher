package com.tungsten.hmclpe.launcher.launch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tungsten.hmclpe.utils.file.FileStringUtils;

import net.kdt.pojavlaunch.Logger;

import java.io.File;

/**
 * 启动日志悬浮窗：游戏启动时实时显示 JVM / Minecraft 的输出。
 *
 * <p>挂在游戏界面的控制层之上（不需要系统悬浮窗权限），玩家可以直接看到
 * 游戏侧报了什么错。游戏结束回到主界面时随 Activity 销毁自动消失。
 *
 * <p>数据源：{@link Logger}（JREUtils 的日志线程会把游戏的 stdout/stderr 喂进来）。
 */
@SuppressLint("SetTextI18n")
public class LaunchLogWindow {

    private static final int MAX_CHARS = 24000;

    private final Activity activity;
    private final ViewGroup parent;
    private final StringBuilder buffer = new StringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private View panel;
    private TextView logView;
    private ScrollView scroller;
    private boolean attached;
    private static LaunchLogWindow current;

    public LaunchLogWindow(Activity activity, ViewGroup parent) {
        this.activity = activity;
        this.parent = parent;
    }

    /** 是否额外从 logcat 抓取游戏输出（Boat 后端的 JVM 输出只在这里） */
    private static boolean captureLogcat = false;

    /** Boat 后端用：Boat 的原生层把游戏输出打到 logcat 的 jrelog 标签，Logger 里拿不到 */
    public static void showForBoat(Activity activity, ViewGroup parent) {
        captureLogcat = true;
        new LaunchLogWindow(activity, parent).show();
    }

    private void startLogcatCapture() {
        try {
            final Process process = Runtime.getRuntime().exec(
                    new String[]{"logcat", "-v", "brief", "-s", "jrelog:V"});
            final java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            Thread thread = new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        onLogLine(line);
                    }
                } catch (Throwable ignored) {
                }
            }, "qcl-logcat");
            thread.setDaemon(true);
            thread.start();
        } catch (Throwable ignored) {
        }
    }

    /** 显示窗口并开始接收日志。
     *  关闭时机跟随游戏日志：日志连续一段时间不再输出（游戏进入主界面后空闲不打印）即自动关闭；
     *  另有 2 分钟兜底上限，避免卡死时窗口一直挂着。 */
    public void show() {
        if (attached) return;
        shownAt = android.os.SystemClock.uptimeMillis();
        lastLogTime = shownAt;
        current = this;
        Logger.getInstance(activity).setLogListener(this::onLogLine);
        if (captureLogcat) {
            startLogcatCapture();
        }
        mainHandler.post(this::attach);
        mainHandler.postDelayed(silenceChecker, 1000);
    }

    /** 日志静默多久判定为"已进入主界面"（兜底） */
    private static final long LOG_SILENCE_MS = 8000;

    /** 主界面标记：游戏日志里出现这些内容，说明已经加载到主界面，日志立刻关闭 */
    private static final String[] MENU_MARKERS = {
            "MinecraftResources",      // b1.7.x：标题界面一出现就开始拉取资源
            "Sound engine started",    // 新版本：主界面声音引擎启动
            "Created: 16x16x4"         // 新版本：主界面贴图集已创建
    };
    /** 兜底：窗口最长显示时间 */
    private static final long MAX_SHOW_MS = 300_000;
    private long shownAt;
    private long lastLogTime;
    private boolean gotAnyLine;

    private final Runnable silenceChecker = new Runnable() {
        @Override
        public void run() {
            if (!attached) return;
            long now = android.os.SystemClock.uptimeMillis();
            // 加载过程中日志是持续输出的；一旦停了，说明游戏已经加载完进入主界面
            if (gotAnyLine && now - lastLogTime >= LOG_SILENCE_MS) {
                close();
                return;
            }
            if (now - shownAt >= MAX_SHOW_MS) {
                close();
                return;
            }
            mainHandler.postDelayed(this, 250);
        }
    };

    /** 关闭窗口（回到主界面时也会随 Activity 销毁自动消失）。 */
    /** 供悬浮窗开关调用：关掉当前正在显示的日志窗 */
    public static void closeCurrentIfAny() {
        LaunchLogWindow w = current;
        if (w != null) w.close();
    }

    /** 供悬浮窗开关调用：重新显示日志窗 */
    public static void showFor(Activity activity, ViewGroup parent) {
        if (current == null || !current.attached) {
            new LaunchLogWindow(activity, parent).show();
        }
    }

    public void close() {
        current = null;
        mainHandler.removeCallbacks(silenceChecker);
        Logger.getInstance(activity).setLogListener(null);
        mainHandler.post(() -> {
            if (panel != null && panel.getParent() instanceof ViewGroup) {
                ((ViewGroup) panel.getParent()).removeView(panel);
            }
            panel = null;
            attached = false;
        });
    }

    private void attach() {
        if (attached) return;
        int pad = dp(8);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xCC1C1C1C);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), 0x66555555);
        box.setBackground(background);
        box.setPadding(pad, pad, pad, pad);

        // 标题栏：标题 + 关闭按钮
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        title.setText("启动日志");
        title.setTextColor(0xFFE6E6E6);
        title.setTextSize(12);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView close = new TextView(activity);
        close.setText("×");
        close.setTextColor(0xFFFFFFFF);
        close.setTextSize(16);
        close.setPadding(dp(10), 0, dp(2), 0);
        close.setOnClickListener(v -> close());
        header.addView(close, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // 日志区
        scroller = new ScrollView(activity);
        scroller.setFillViewport(true);
        logView = new TextView(activity);
        logView.setTextColor(0xFFDDDDDD);
        logView.setTextSize(10);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        scroller.addView(logView, new ScrollView.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        box.addView(scroller, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(250), dp(160),
                Gravity.BOTTOM | Gravity.START);
        lp.leftMargin = dp(8);
        lp.bottomMargin = dp(8);
        box.setLayoutParams(lp);

        parent.addView(box);
        panel = box;
        attached = true;

        // 回填已经写入的日志（启动可能早于窗口创建）
        try {
            File file = new File(activity.getExternalFilesDir("debug"), "pojav_latest_log.txt");
            if (file.isFile()) {
                String existing = FileStringUtils.getStringFromFile(file.getAbsolutePath());
                if (existing != null && !existing.isEmpty()) {
                    buffer.append(existing);
                    logView.setText(trim(buffer));
                    scrollToEnd();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void onLogLine(String text) {
        lastLogTime = android.os.SystemClock.uptimeMillis();
        gotAnyLine = true;
        if (isMenuMarker(text)) {
            mainHandler.post(this::close);
            return;
        }
        mainHandler.post(() -> {
            if (!attached) return;
            buffer.append(text);
            if (!text.endsWith("\n")) buffer.append('\n');
            if (attached && logView != null) {
                logView.setText(trim(buffer));
                scrollToEnd();
            }
        });
    }

    private boolean isMenuMarker(String text) {
        if (text == null) return false;
        for (String marker : MENU_MARKERS) {
            if (text.contains(marker)) return true;
        }
        return false;
    }

    private void scrollToEnd() {
        if (scroller != null) scroller.post(() -> scroller.fullScroll(View.FOCUS_DOWN));
    }

    private static String trim(StringBuilder sb) {
        return sb.length() > MAX_CHARS ? sb.substring(sb.length() - MAX_CHARS) : sb.toString();
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
