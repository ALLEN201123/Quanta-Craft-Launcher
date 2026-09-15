package com.qcl.launcher.launcher.launch;

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

import com.qcl.launcher.utils.file.FileStringUtils;

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
    private volatile boolean closed;

    public LaunchLogWindow(Activity activity, ViewGroup parent) {
        this.activity = activity;
        this.parent = parent;
    }

    /** 是否额外从 logcat 抓取游戏输出（Boat 后端的 JVM 输出只在这里） */
    private static boolean captureLogcat = false;

    /**
     * 需要抓取的 logcat 标签。
     *
     * <p>1.0.6：原来只抓 {@code jrelog:V}，导致窗口里常常只有零散的
     * {@code dlopen libxhook.so success} 之类 —— 而玩家真正需要的启动信息
     * （环境变量、JVM 参数、OpenJDK 警告、GL 库加载）散落在别的标签里。
     *
     * <p>现在覆盖各后端实际使用的全部标签：
     * <ul>
     *   <li>{@code jrelog} — Pojav 的 JRE 输出主通道</li>
     *   <li>{@code LIBGL} — GL4ES / VirGL 图形库</li>
     *   <li>{@code Boat} — Boat 原生层的 LoadMe / dlopen</li>
     *   <li>{@code xhook} — 原生 hook 库</li>
     *   <li>{@code System.out} / {@code System.err} — {@code LoadMe} 的 println</li>
     *   <li>{@code OpenJDK} — JVM 自身警告</li>
     * </ul>
     */
    private static final String[] LOGCAT_TAGS = {
            "jrelog", "LIBGL", "Boat", "xhook", "System.out", "System.err", "OpenJDK",
    };

    /** 单行超过这个长度就截断（避免超长串把窗口刷屏） */
    private static final int MAX_LINE_CHARS = 400;

    /** 这些行是完全无信息量的噪声，直接丢弃 */
    private static final String[] NOISE_PREFIXES = {
            "--------- beginning of",
            "GLib-GIO",           // GTK 噪声（与游戏无关）
            "libc    ",           // 无堆栈的 libc 行
            "Zygote  ",
    };

    /** Boat 后端用：Boat 的原生层把游戏输出打到 logcat 的 jrelog 标签，Logger 里拿不到 */
    public static void showForBoat(Activity activity, ViewGroup parent) {
        showForBoat(activity, parent, null);
    }

    /** Boat 后端 + 基础启动信息（1.0.6） */
    public static void showForBoat(Activity activity, ViewGroup parent, GameLaunchSettingInfo info) {
        captureLogcat = true;
        new LaunchLogWindow(activity, parent).show(info);
    }

    private void startLogcatCapture() {
        try {
            // 先清一次缓冲，避免把上次启动的旧日志翻出来
            try {
                Runtime.getRuntime().exec(new String[]{"logcat", "-c"}).waitFor();
            } catch (Throwable ignored) {
            }
            String[] cmd = new String[4 + LOGCAT_TAGS.length];
            cmd[0] = "logcat";
            cmd[1] = "-v";
            cmd[2] = "brief";
            cmd[3] = "-s";
            for (int i = 0; i < LOGCAT_TAGS.length; i++) {
                cmd[4 + i] = LOGCAT_TAGS[i] + ":V";
            }
            final Process process = Runtime.getRuntime().exec(cmd);
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


    /** 实时 tail 游戏日志文件（比 logcat 管道可靠）：Pojav 一直在写 pojav_latest_log.txt */
    private void startFileTail(Activity activity) {
        try {
            final java.io.File dir = activity.getExternalFilesDir("debug");
            if (dir == null) return;
            final java.io.File logFile = new java.io.File(dir, "pojav_latest_log.txt");
            Thread thread = new Thread(() -> {
                long offset = 0;
                java.io.BufferedReader reader = null;
                while (!closed) {
                    try {
                        if (logFile.isFile()) {
                            long len = logFile.length();
                            if (len < offset) offset = 0;   // 日志被重置则从头读
                            if (len > offset) {
                                if (reader == null) reader = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(logFile)));
                                reader.skip(offset - (reader.ready() ? 0 : offset) );
                                // 简单实现：直接按 offset 重新打开读取，避免 skip 语义差异
                                try { if (reader != null) reader.close(); } catch (Throwable ignored) {}
                                java.io.RandomAccessFile raf = new java.io.RandomAccessFile(logFile, "r");
                                raf.seek(offset);
                                String line;
                                while ((line = raf.readLine()) != null) {
                                    offset += line.length() + 1;
                                    onLogLine(line);
                                }
                                raf.close();
                            }
                        }
                        Thread.sleep(400);
                    } catch (Throwable t) {
                        try { Thread.sleep(1000); } catch (InterruptedException ignored2) {}
                    }
                }
            }, "qcl-logtail");
            thread.setDaemon(true);
            thread.start();
        } catch (Throwable ignored) {
        }
    }

    /** 显示窗口并开始接收日志。
     *  关闭时机跟随游戏日志：日志连续一段时间不再输出（游戏进入主界面后空闲不打印）即自动关闭；
     *  另有 2 分钟兜底上限，避免卡死时窗口一直挂着。 */
    public void show() {
        show(null);
    }

    /** 带基础信息显示（推荐：info 非空时窗口顶部会先打一段设备/运行时摘要） */
    public void show(GameLaunchSettingInfo info) {
        if (attached) return;
        shownAt = android.os.SystemClock.uptimeMillis();
        lastLogTime = shownAt;
        current = this;
        Logger.getInstance(activity).setLogListener(this::onLogLine);
        // 所有后端都抓 logcat：Boat 只走 logcat；Pojav 的 21/25 运行时也可能
        // 不经过 Logger 管道 —— 有 logcat 兜底，窗口永远有内容可看（用于定位启动问题）。
        startLogcatCapture();
        mainHandler.post(this::attach);
        mainHandler.post(() -> appendBasics(info));
        mainHandler.postDelayed(silenceChecker, 1000);
    }

    /** 日志静默多久判定为"已进入主界面"（兜底） */
    private static final long LOG_SILENCE_MS = 600000;

    /** 主界面标记：游戏日志里出现这些内容，说明已经加载到主界面，日志立刻关闭 */
    private static final String[] MENU_MARKERS = {
            "MinecraftResources",      // b1.7.x：标题界面一出现就开始拉取资源
            "Sound engine started",    // 新版本：主界面声音引擎启动
            "Created: 16x16x4"         // 新版本：主界面贴图集已创建
    };
    /** 兜底：窗口最长显示时间 */
    private static final long MAX_SHOW_MS = 900_000;
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

    /**
     * 打印一组「基础启动信息」。
     *
     * <p>1.0.6 新增：用户反馈日志窗内容太少，看不出环境。这里在窗口打开时主动输出
     * 设备、运行时、后端等关键信息 —— 这些在原生层/其他标签里拿不到，
     * 但对定位「为什么这个版本起不来」极有用。
     * 刻意只打**基础**内容，不打长串（如完整 classpath、完整 args）。
     */
    private void appendBasics(GameLaunchSettingInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("==== QCL 启动信息 ====").append('\n');
        sb.append("App     : ").append(activity.getPackageName()).append('\n');
        sb.append("Device  : ").append(android.os.Build.MANUFACTURER).append(' ')
                .append(android.os.Build.MODEL).append('\n');
        sb.append("Android : ").append(android.os.Build.VERSION.RELEASE)
                .append("  (API ").append(android.os.Build.VERSION.SDK_INT).append(')').append('\n');
        sb.append("ABI     : ").append(android.os.Build.SUPPORTED_ABIS.length > 0
                ? android.os.Build.SUPPORTED_ABIS[0] : "?").append('\n');
        if (info != null) {
            sb.append("Backend : ").append(info.backend).append('\n');
            sb.append("Version : ").append(info.version).append('\n');
            sb.append("Java    : ").append(info.javaRuntime).append('\n');
            sb.append("Render  : ").append(info.renderer).append('\n');
            sb.append("RAM     : ").append(info.ramMb).append(" MB").append('\n');
        }
        sb.append("====================").append('\n');
        onLogLine(sb.toString());
    }

    /** 基础信息载体（避免依赖 GameLaunchSetting 的字段签名） */
    public static class GameLaunchSettingInfo {
        public String backend = "?";
        public String version = "?";
        public String javaRuntime = "?";
        public String renderer = "?";
        public int ramMb = 0;
    }

    /**
     * 全局缓存的基础启动信息。
     *
     * <p>1.0.6：{@code MenuHelper}（悬浮窗开关）拿不到 {@code GameLaunchSetting}，
     * 但玩家从开关打开日志窗时同样应该看到基础信息。所以由启动 Activity 在创建时
     * 写一份到这里，任何入口打开日志窗都能取到。
     */
    private static GameLaunchSettingInfo sBasics;

    /** 由启动 Activity 调用：登记本局游戏的基础信息 */
    public static void setBasics(GameLaunchSettingInfo info) {
        sBasics = info;
    }

    /**
     * 供悬浮窗开关「显示日志」调用：重新显示日志窗。
     *
     * <p>⚠️ 1.0.6 修正：原来这里调的是无参 {@code show()}，导致
     * ①不打印基础信息 ②{@code captureLogcat} 沿用静态值（若首次是通过开关打开的，
     * 它还是 false → 窗口里几乎没有内容）。现在统一按「完整模式」打开。
     */
    public static void showFor(Activity activity, ViewGroup parent) {
        showFor(activity, parent, null);
    }

    /**
     * 供悬浮窗开关调用：重新显示日志窗。
     *
     * @param info 基础启动信息，可为 null（null 时也会打印设备/系统摘要）
     */
    public static void showFor(Activity activity, ViewGroup parent, GameLaunchSettingInfo info) {
        if (current != null && current.attached) return;
        // 关键：无论从哪条路径打开，都保证 logcat 抓取是开的，且会补基础信息
        captureLogcat = true;
        // 没显式传就用启动时登记的（悬浮窗开关走这条）
        new LaunchLogWindow(activity, parent).show(info != null ? info : sBasics);
    }

    private void autoScrollIfAtBottom() {
        if (scroller == null) return;
        scroller.post(() -> {
            try {
                if (!scroller.canScrollVertically(1)) {
                    scroller.fullScroll(android.view.View.FOCUS_DOWN);
                }
            } catch (Throwable ignored) {
            }
        });
    }

    public void close() {
        closed = true;
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
        logView.setTextIsSelectable(true);
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

        // 回填已经写入的日志（启动可能早于窗口创建）。
        // 1.0.6：先插基础信息，再把历史文件内容追加在后面，保证信息区在顶部。
        if (logView != null && buffer.length() > 0) {
            logView.setText(trim(buffer));
        }
        try {
            File file = new File(activity.getExternalFilesDir("debug"), "pojav_latest_log.txt");
            if (file.isFile()) {
                String existing = FileStringUtils.getStringFromFile(file.getAbsolutePath());
                if (existing != null && !existing.isEmpty()) {
                    buffer.append(existing);
                    if (logView.hasSelection()) {
                        return;   // 正在选择文本：跳过本次刷新，避免选区被重置
                    }
                    logView.setText(trim(buffer));
                    autoScrollIfAtBottom();
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
        // 1.0.6：过滤无信息量噪声 + 截断超长行，否则窗口会被刷屏看不到有用内容
        String filtered = filterLine(text);
        if (filtered == null) return;
        final String out = filtered;
        mainHandler.post(() -> {
            if (!attached) return;
            buffer.append(out);
            if (!out.endsWith("\n")) buffer.append('\n');
            if (attached && logView != null) {
                if (logView.hasSelection()) {
                    return;   // 正在选择文本：跳过本次刷新，避免选区被重置
                }
                logView.setText(trim(buffer));
                autoScrollIfAtBottom();
                scrollToEnd();
            }
        });
    }

    /**
     * 过滤单行日志：无信息量的噪声返回 null，超长行截断。
     * 目标：窗口里留下的都是「能看出启动到哪一步、哪一步出错」的有效内容。
     */
    private static String filterLine(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        for (String noise : NOISE_PREFIXES) {
            if (t.startsWith(noise)) return null;
        }
        // 超长行（如长堆栈、超长 JSON、base64）截断，保留头尾
        if (t.length() > MAX_LINE_CHARS) {
            t = t.substring(0, MAX_LINE_CHARS) + " …[已截断 " + (text.length() - MAX_LINE_CHARS) + " 字符]";
        }
        return t;
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
