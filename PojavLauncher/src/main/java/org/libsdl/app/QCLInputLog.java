package org.libsdl.app;

import android.util.Log;

/**
 * ★★★★★ 2026-10-11 新增：**输入链路诊断日志开关**（用户要求
 * 「你加入一个日志，然后我点击无响应」）。
 *
 * <p>背景：MC 26.4 能进主菜单但按钮点不动。已用证据排除了：
 * SDL 的 Java 层（与 FCL 99.8%~100% 一致）、原生输入桥
 * （{@code input_bridge_v3.c} 与 1.4.8 逐字节一致）、SDL hook（是 FCL 的严格超集）。
 * ⇒ 只能靠**在链路上逐环打点**看事件到底断在哪一环。
 *
 * <p>链路（你的点击要走完这 5 环才能被游戏收到）：
 * <pre>
 *   ① SDLSurface.onTouch           ← Android 触摸事件到达 Surface
 *   ② SDLSurface.onCapturedPointerEvent ← 指针捕获模式下的鼠标事件
 *   ③ SDLActivity.onNativeMouse    ← 交给 SDL 原生层
 *   ④ CallbackBridge.sendMouseKeycode / sendCursorPos ← 交给 PojavLauncher 原生桥
 *   ⑤ nativeSendMouseButton        ← 进入游戏 JVM
 * </pre>
 *
 * <p>★ 发布版把 {@link #ENABLED} 改成 false 即可全部关闭（只多一次 boolean 判断，无开销）。
 * 日志 tag 统一为 {@code QCLINPUT}，抓取方法：
 * <pre>adb shell "su -c 'logcat -d | grep -a QCLINPUT'"</pre>
 */
public final class QCLInputLog {

    /** ★ 诊断开关：true = 打日志。定位完把这个改回 false 再发版。 */
    public static final boolean ENABLED = true;

    /** 统一 tag，方便 grep。 */
    public static final String TAG = "QCLINPUT";

    /** 限流：同一环每秒最多打这么多条，避免刷屏把有用的冲掉。 */
    private static final int MAX_PER_SEC = 12;
    private static long sWindowStart = 0L;
    private static int sWindowCount = 0;

    private QCLInputLog() {
    }

    /**
     * 打一条输入链路日志。
     *
     * @param stage 环节名（如 "1-onTouch"）
     * @param msg   内容
     */
    public static void log(String stage, String msg) {
        if (!ENABLED) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (now - sWindowStart >= 1000L) {
                sWindowStart = now;
                sWindowCount = 0;
            }
            if (sWindowCount >= MAX_PER_SEC) {
                return;   // 本秒配额用完，丢弃（防止事件洪流淹没关键信息）
            }
            sWindowCount++;
            Log.w(TAG, "[" + stage + "] " + msg);
        } catch (Throwable ignored) {
        }
    }

    /** 便捷：把 MotionEvent 的关键字段格式化成一行。 */
    public static String describe(android.view.MotionEvent e) {
        if (e == null) {
            return "event=null";
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("action=").append(actionName(e.getActionMasked()));
            sb.append(" tool=").append(e.getToolType(0));
            sb.append(" btn=").append(e.getButtonState());
            sb.append(" x=").append(String.format(java.util.Locale.US, "%.1f", e.getX(0)));
            sb.append(" y=").append(String.format(java.util.Locale.US, "%.1f", e.getY(0)));
            sb.append(" ptrCount=").append(e.getPointerCount());
            sb.append(" src=0x").append(Integer.toHexString(e.getSource()));
            return sb.toString();
        } catch (Throwable t) {
            return "describe-failed";
        }
    }

    private static String actionName(int a) {
        switch (a) {
            case android.view.MotionEvent.ACTION_DOWN: return "DOWN";
            case android.view.MotionEvent.ACTION_UP: return "UP";
            case android.view.MotionEvent.ACTION_MOVE: return "MOVE";
            case android.view.MotionEvent.ACTION_CANCEL: return "CANCEL";
            case android.view.MotionEvent.ACTION_HOVER_MOVE: return "HOVER_MOVE";
            case android.view.MotionEvent.ACTION_SCROLL: return "SCROLL";
            case android.view.MotionEvent.ACTION_BUTTON_PRESS: return "BTN_PRESS";
            case android.view.MotionEvent.ACTION_BUTTON_RELEASE: return "BTN_RELEASE";
            case android.view.MotionEvent.ACTION_POINTER_DOWN: return "PTR_DOWN";
            case android.view.MotionEvent.ACTION_POINTER_UP: return "PTR_UP";
            default: return "A" + a;
        }
    }
}
