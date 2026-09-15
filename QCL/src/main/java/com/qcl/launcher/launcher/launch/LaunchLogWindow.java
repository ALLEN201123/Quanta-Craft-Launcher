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
    /** 1.0.6：圆形关闭按钮（原来是无背景的裸「×」） */
    private TextView closeBtn;
    /** 1.0.7：标题栏的「选择」按钮 —— 点一下进入文本选择模式 */
    private TextView selectBtn;
    /** 1.0.6：拖动用的窗口布局参数（拖动时改 leftMargin / topMargin） */
    private FrameLayout.LayoutParams panelLp;
    /** 1.0.6：标题栏 —— 拖动把手 */
    private LinearLayout headerBar;
    private boolean attached;
    private static LaunchLogWindow current;
    private volatile boolean closed;
    /** 1.0.7：是否处于「文本选择模式」 */
    private boolean selecting;

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
            // 1.0.6：自动关闭逻辑已**整体停用**（用户要求"不要自己关闭，需要一直显示"）。
            //
            // 原来这里有三套自动关闭，都造成了用户反馈的"窗口自己消失"：
            //  ① MENU_MARKERS 命中即关 —— 那些标记（Sound engine started / Created: 16x16x4）
            //     在较早版本里就会打印，用户还在看加载过程，窗口却突然没了；
            //  ② LOG_SILENCE_MS 静默判定 —— 比较的是 logcat 时间戳（CLOCK_MONOTONIC）与
            //     uptimeMillis，两者不同源，差值恒为一个极大的数，导致窗口**一打开就立刻关闭**；
            //  ③ MAX_SHOW_MS 兜底上限 —— 到点无条件关。
            //
            // 现在三条都不再触发：窗口只在用户点右上角 ×（或随 Activity 销毁）时关闭。
            // 这里保留空实现，仅为让 show() 里的调用点继续编译通过。
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

    /**
     * 1.0.7：切换文本选择模式（由标题栏「选择」按钮触发）。
     *
     * <p>⚠️ 为什么放弃长按选中：
     * 1.0.6 曾用「TextView 的 OnLongClickListener + performLongClick()」来进选择态，
     * 但在游戏 Activity 这种**高帧率、多层重叠 View、还挂着拖动/缩放 Touch 监听**的
     * 环境里完全不可靠：
     * <ul>
     *   <li>长按手势要先经过外层的 ScrollView，容易被判成「开始滑动」而吃掉；</li>
     *   <li>窗口标题栏与缩放手柄都挂了 OnTouchListener，ACTION_DOWN 一旦被上层消费，
     *       TextView 就收不到完整手势序列；</li>
     *   <li>{@code performLongClick()} 自 Android 12 起对可选文本的 TextView 基本无效。</li>
     * </ul>
     * 改为显式按钮后，行为是确定的：点一下 = 进入/退出选择模式，不依赖手势时序。
     * 进入选择模式时把外层 ScrollView 的滚动关掉，避免"想拖选区却滚了页面"。
     */
    private void toggleTextSelection() {
        try {
            if (logView == null) return;
            selecting = !selecting;
            if (selecting) {
                // 关掉 ScrollView 的触摸拦截，让手势全部落到 TextView 上
                scroller.requestDisallowInterceptTouchEvent(true);
                // 让 TextView 自己接管：先聚焦再进入选择态
                logView.setFocusableInTouchMode(true);
                logView.requestFocus();
                logView.setTextIsSelectable(true);
                // 用长按触发一次，让系统把光标放上去并弹出选择手柄
                logView.performLongClick();
                if (selectBtn != null) selectBtn.setText("完成");
            } else {
                scroller.requestDisallowInterceptTouchEvent(false);
                android.content.ClipboardManager cm = (android.content.ClipboardManager)
                        activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                if (cm != null && logView.hasSelection()) {
                    // 退出时顺手把选中内容放进剪贴板，省得玩家再点一次"复制"
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("QCL 日志",
                            logView.getText().subSequence(logView.getSelectionStart(),
                                    logView.getSelectionEnd())));
                    toast("已复制选中的日志");
                }
                logView.clearFocus();
                if (selectBtn != null) selectBtn.setText("选择");
            }
        } catch (Throwable t) {
            // 任何一步不支持都不致命，至少不要崩
            toast("此设备不支持文本选择，可直接截图反馈");
        }
    }

    private void toast(String msg) {
        try {
            android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 1.0.7：一键把**全部日志**复制到剪贴板。
     *
     * <p>这是文本选择的兜底方案 —— 玩家反馈问题时最需要的其实是"把完整日志发给我"，
     * 逐字选反而麻烦。长按「选择」按钮即触发全选复制。
     */
    private void copyAllToClipboard() {
        try {
            if (logView == null) return;
            CharSequence text = logView.getText();
            if (text == null || text.length() == 0) {
                toast("日志还是空的，等启动跑一会儿再试");
                return;
            }
            android.content.ClipboardManager cm = (android.content.ClipboardManager)
                    activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if (cm == null) {
                toast("此设备不支持剪贴板");
                return;
            }
            cm.setPrimaryClip(android.content.ClipData.newPlainText("QCL 日志", text));
            toast("已复制全部日志（" + text.length() + " 字）");
        } catch (Throwable ignored) {
            toast("复制失败，可截图反馈");
        }
    }

    /** 触点是否落在某个 View 的屏幕矩形内（用于把点击让给关闭按钮）。 */
    private boolean isTouchInside(View v, android.view.MotionEvent event) {
        try {
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            float x = event.getRawX();
            float y = event.getRawY();
            return x >= loc[0] && x <= loc[0] + v.getWidth()
                    && y >= loc[1] && y <= loc[1] + v.getHeight();
        } catch (Throwable ignored) {
            return false;
        }
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

        // ⚠️ 外层必须是 FrameLayout：窗口既要被拖动（改 LayoutParams 的 margin），
        // 又要在右下角放一个绝对定位的缩放手柄。LinearLayout 装不下 FrameLayout.LayoutParams。
        FrameLayout box = new FrameLayout(activity);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xCC1C1C1C);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), 0x66555555);
        box.setBackground(background);
        box.setPadding(pad, pad, pad, pad);

        // 内容竖排容器（标题栏 + 日志区）
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        box.addView(column, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // 标题栏：标题 + 关闭按钮（1.0.6 起同时作为**拖动把手**）
        LinearLayout header = new LinearLayout(activity);

        headerBar = header;
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(activity);
        // 1.0.6：标题加一个「可拖动」的视觉暗示（≡ 图标），让玩家知道这里能拖
        title.setText("≡ 启动日志");
        title.setTextColor(0xFFE6E6E6);
        title.setTextSize(12);
        title.setPadding(dp(2), dp(4), dp(2), dp(4));
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // 1.0.7：「选择」按钮。
        // ⚠️ 为什么不用长按？—— 1.0.6 试过长按选中，但在游戏 Activity 这种
        // 高帧率 + 重叠 View 的环境里非常不可靠：
        //   ① 长按手势要先经过 ScrollView → 常被判成"开始滑动"从而吃掉；
        //   ② 窗口本身还挂着拖动/缩放的 Touch 监听，ACTION_DOWN 一旦被上层消费，
        //      TextView 压根收不到完整手势序列；
        //   ③ `performLongClick()` 自 Android 12 起对可选文本的 TextView 基本无效。
        // 所以 1.0.7 改为**显式的按钮**：点一下切到选择模式，行为确定、不靠手势时序。
        selectBtn = new TextView(activity);
        selectBtn.setText("选择");
        selectBtn.setTextColor(0xFFFFFFFF);
        selectBtn.setTextSize(10);
        selectBtn.setGravity(Gravity.CENTER);
        GradientDrawable selBg = new GradientDrawable();
        selBg.setCornerRadius(dp(11));
        selBg.setColor(0x33FFFFFF);
        selBg.setStroke(dp(1), 0x55FFFFFF);
        selectBtn.setBackground(selBg);
        selectBtn.setClickable(true);
        selectBtn.setFocusable(true);
        selectBtn.setPadding(dp(8), 0, dp(8), 0);
        selectBtn.setOnClickListener(v -> toggleTextSelection());
        // 长按「选择」= 一键复制全部日志（反馈问题时最常用的操作）
        selectBtn.setOnLongClickListener(v -> {
            copyAllToClipboard();
            return true;
        });
        LinearLayout.LayoutParams selLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(22));
        selLp.leftMargin = dp(4);
        header.addView(selectBtn, selLp);

        // 1.0.6：关闭按钮原来是一个裸 TextView「×」，没有任何背景和圆形边框，
        // 贴在深色面板上看起来就是"一块带字的方块"，既不美观也不像按钮。
        // 现在改为**圆形背景 + 居中 ×**，有按压反馈，尺寸也放大到易点。
        closeBtn = new TextView(activity);
        closeBtn.setText("×");
        closeBtn.setTextColor(0xFFFFFFFF);
        closeBtn.setTextSize(14);
        closeBtn.setGravity(Gravity.CENTER);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(0x33FFFFFF);
        closeBg.setStroke(dp(1), 0x55FFFFFF);
        closeBtn.setBackground(closeBg);
        closeBtn.setClickable(true);
        closeBtn.setFocusable(true);
        closeBtn.setOnClickListener(v -> close());
        int closeSize = dp(22);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeSize, closeSize);
        closeLp.leftMargin = dp(4);
        header.addView(closeBtn, closeLp);
        column.addView(header, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // 日志区
        scroller = new ScrollView(activity);
        scroller.setFillViewport(true);
        logView = new TextView(activity);
        logView.setTextColor(0xFFDDDDDD);
        logView.setTextSize(10);
        logView.setTypeface(android.graphics.Typeface.MONOSPACE);
        // 1.0.7：文本选择改由标题栏的「选择」按钮驱动（见 toggleTextSelection()）。
        //  原则：**不要给 logView 挂任何 LongClickListener** ——
        //  setTextIsSelectable 依赖 TextView 自己的长按处理来进入选择态，
        //  一旦挂上监听并且 return true，等于告诉系统"我处理完了"，
        //  系统就不会再弹选择手柄，表现为"长按完全没反应"。
        //  同时把 maxLines 去掉限制，保证长日志不被裁掉。
        logView.setTextIsSelectable(true);
        logView.setFocusable(true);
        logView.setFocusableInTouchMode(true);
        logView.setClickable(true);
        logView.setLongClickable(true);
        logView.setHorizontallyScrolling(false);
        // 单行自动换行：日志一行往往很长，横向滚动反而不好选
        logView.setMaxLines(Integer.MAX_VALUE);
        scroller.addView(logView, new ScrollView.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        column.addView(scroller, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        panelLp = new FrameLayout.LayoutParams(dp(250), dp(160),
                Gravity.BOTTOM | Gravity.START);
        panelLp.leftMargin = dp(8);
        panelLp.bottomMargin = dp(8);
        box.setLayoutParams(panelLp);

        // 1.0.6：窗口可拖动 —— 拖标题栏即可。原来窗口是钉死在左下角的，挡视线也没法让开。
        // 拖动改的是 LayoutParams 的 leftMargin / topMargin，并用 clamp 限制在父容器内。
        header.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX, downRawY;
            private int startLeft, startTop;

            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        // ⚠️ 关闭按钮和「选择」按钮都在标题栏里。要是这里一律 return true
                        // 吃掉手势，这两个按钮就永远点不到了 → 落点在按钮上时不接管。
                        if (closeBtn != null && isTouchInside(closeBtn, event)) return false;
                        if (selectBtn != null && isTouchInside(selectBtn, event)) return false;
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startLeft = panelLp.leftMargin;
                        startTop = panelLp.topMargin;
                        return true;
                    case android.view.MotionEvent.ACTION_MOVE: {
                        int dx = (int) (event.getRawX() - downRawX);
                        int dy = (int) (event.getRawY() - downRawY);
                        int newLeft = startLeft + dx;
                        int newTop = startTop + dy;
                        // clamp 到父容器范围内，避免拖出屏幕外找不回来
                        int maxLeft = Math.max(0, parent.getWidth() - panel.getWidth());
                        int maxTop = Math.max(0, parent.getHeight() - panel.getHeight());
                        newLeft = Math.max(0, Math.min(maxLeft, newLeft));
                        newTop = Math.max(0, Math.min(maxTop, newTop));
                        // 拖过之后统一改成 TOP|START + margin 定位，避免 BOTTOM 锚点导致抖动
                        panelLp.gravity = Gravity.TOP | Gravity.START;
                        panelLp.leftMargin = newLeft;
                        panelLp.topMargin = newTop;
                        panel.setLayoutParams(panelLp);
                        return true;
                    }
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        return true;
                }
                return false;
            }
        });

        parent.addView(box);
        panel = box;
        attached = true;

        // 1.0.6：右下角拖拽调大小。用户反馈"边框不能拖动"——标题栏负责挪位置，
        // 右下角这个把手负责改尺寸，两者配合才能把窗口摆到不挡视线的地方。
        try {
            TextView resizer = new TextView(activity);
            resizer.setText("◢");
            resizer.setTextColor(0x88FFFFFF);
            resizer.setTextSize(11);
            resizer.setGravity(Gravity.BOTTOM | Gravity.END);
            int rSize = dp(20);
            FrameLayout.LayoutParams rLp = new FrameLayout.LayoutParams(rSize, rSize);
            rLp.gravity = Gravity.BOTTOM | Gravity.END;
            box.addView(resizer, rLp);
            resizer.setOnTouchListener(new View.OnTouchListener() {
                private float downRawX, downRawY;
                private int startW, startH;

                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            startW = panelLp.width;
                            startH = panelLp.height;
                            return true;
                        case android.view.MotionEvent.ACTION_MOVE: {
                            int w = startW + (int) (event.getRawX() - downRawX);
                            int h = startH + (int) (event.getRawY() - downRawY);
                            // 下限保证还能看清日志、还能点到关闭按钮
                            panelLp.width = Math.max(dp(180), Math.min(dp(640), w));
                            panelLp.height = Math.max(dp(110), Math.min(dp(520), h));
                            panel.setLayoutParams(panelLp);
                            return true;
                        }
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            return true;
                    }
                    return false;
                }
            });
        } catch (Throwable ignored) {
        }

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
        // 1.0.6：**移除「命中主界面标记就自动关闭」**。
        // 原来 isMenuMarker() 一旦匹配（Sound engine started / Created: 16x16x4 等）
        // 就立刻 close()，而较早版本在主界面出现时就会打印这些内容 ——
        // 用户看到的就是"启动完自己就关了"，还没看完日志。
        // 现在不在这里关闭，由用户点 × 决定。
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
