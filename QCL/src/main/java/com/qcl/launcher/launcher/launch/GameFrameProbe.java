package com.qcl.launcher.launcher.launch;

import android.graphics.SurfaceTexture;
import android.view.TextureView;

/**
 * 1.0.9：游戏画面输出探测（「卡在启动等待界面」的兜底）。
 *
 * <h3>要解决的问题</h3>
 * 关掉启动等待界面（露出游戏画面）的**正规路径**是：
 * <pre>
 *   TextureView.onSurfaceTextureUpdated()
 *     → PojavCallback.onPicOutput() / BoatCallback.onPicOutput()
 *     → LayoutPanel.hideBackground()
 * </pre>
 * 但在实际设备上这条链路可能**不触发或被时序竞争吞掉**：
 * <ul>
 *   <li>部分设备/驱动不派发 {@code onSurfaceTextureUpdated}（尤其透明/特殊合成路径）；</li>
 *   <li>时序竞争：游戏首帧早于 {@code onStart}（showBackground）到达时，
 *       「只通知一次」的标志被提前置位，等待界面随后又盖回来，正规回调从此断路。</li>
 * </ul>
 *
 * <h3>1.0.9 的做法（替换 1.0.7 的 getBitmap 轮询）</h3>
 * 1.0.7 用主线程每 250ms 调 {@code TextureView.getBitmap()} 采样像素：
 * <ul>
 *   <li>getBitmap 是**同步 GPU 回读**，软件渲染/转译环境下会阻塞数秒 → 启动器 ANR；</li>
 *   <li>真机驱动上 GL 内容常读出**全黑帧**，像素判定形同虚设，只能靠 10 秒超时。</li>
 * </ul>
 * 现在改为**后台线程轮询 {@link SurfaceTexture#getTimestamp()}**：
 * <ul>
 *   <li>纯元数据读取（一个 long），无 GPU/CPU 回读，**零 ANR 风险**；</li>
 *   <li>时间戳变化 = 游戏真的画了新帧，**黑帧也能检出**；</li>
 *   <li>检出即回调放行 —— 等待界面在游戏首帧后 ≤200ms 内切换，而不是等 10 秒。</li>
 * </ul>
 * 仍保留最大探测时长超时无条件放行 —— 宁可早切，也不让玩家卡死在等待界面。
 *
 * <p>Pojav 与 Boat 两个后端共用这一份实现，行为保持一致。
 */
public class GameFrameProbe {

    /** 轮询间隔（后台线程，开销可忽略） */
    private static final int INTERVAL_MS = 200;
    /** 最大探测时长（10 秒）；超时无条件放行 */
    private static final long MAX_PROBE_MS = 10_000L;

    /** 画面已输出时的回调 */
    public interface Callback {
        void onFirstFrame();
    }

    private final TextureView target;
    private final Callback callback;

    private Thread worker;
    private volatile boolean stopped;

    public GameFrameProbe(TextureView target, Callback callback) {
        this.target = target;
        this.callback = callback;
    }

    /** 开始探测（可在任意线程调用） */
    public synchronized void start() {
        stop();
        stopped = false;
        worker = new Thread(() -> {
            long deadline = System.currentTimeMillis() + MAX_PROBE_MS;
            long lastTs = currentTimestamp();
            long startTs = lastTs;
            while (!stopped) {
                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (InterruptedException e) {
                    return;
                }
                if (stopped) return;
                long ts = currentTimestamp();
                if (ts > 0 && (ts != startTs || ts != lastTs)) {
                    // 游戏画出了新帧（时间戳在推进）→ 立即放行
                    fire(true, ts, startTs);
                    return;
                }
                lastTs = ts;
                if (System.currentTimeMillis() >= deadline) {
                    // 10 秒兜底：无条件放行
                    fire(false, ts, startTs);
                    return;
                }
            }
        }, "GameFrameProbe");
        worker.setDaemon(true);
        worker.start();
    }

    /** 停止探测（正规回调到达时调用，避免重复触发） */
    public synchronized void stop() {
        stopped = true;
        Thread t = worker;
        worker = null;
        if (t != null) t.interrupt();
    }

    private long currentTimestamp() {
        try {
            if (target == null) return 0;
            SurfaceTexture st = target.getSurfaceTexture();
            if (st == null) return 0;
            return st.getTimestamp();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void fire(boolean frameDetected, long ts, long startTs) {
        try {
            android.util.Log.i("jrelog", "[GameFrameProbe] 放行: "
                    + (frameDetected
                       ? "探测到新帧 ts=" + ts + " (start=" + startTs + ")"
                       : ts > 0
                         ? "10s 超时放行（时间戳未推进 ts=" + ts + "）"
                         : "10s 超时放行（SurfaceTexture 未就绪）"));
        } catch (Throwable ignored) {
        }
        try {
            callback.onFirstFrame();
        } catch (Throwable ignored) {
        }
    }
}
