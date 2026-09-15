package com.qcl.launcher.launcher.launch;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;

/**
 * 1.0.7：游戏画面输出探测（「卡在启动等待界面」的兜底）。
 *
 * <h3>要解决的问题</h3>
 * 关掉启动等待界面（露出游戏画面）的**唯一正规路径**是：
 * <pre>
 *   TextureView.onSurfaceTextureUpdated()
 *     → PojavCallback.onPicOutput() / BoatCallback.onPicOutput()
 *     → LayoutPanel.hideBackground()
 * </pre>
 * 但在实际设备上这条链路可能**永远不触发**：
 * <ul>
 *   <li>Pojav 侧渲染视图曾被 {@code setOpaque(false)} 设成透明 ——
 *       透明 TextureView 在部分驱动上不会产生 {@code onSurfaceTextureUpdated}
 *       （系统认为没有可见内容需要合成）；</li>
 *   <li>部分设备的 SurfaceFlinger 在游戏首帧尚未合成时不派发更新；</li>
 *   <li>时序竞争：回调可能早于等待界面创建。</li>
 * </ul>
 * 结果就是**游戏其实已经在后台正常渲染，但等待界面永远盖在上面**，
 * 玩家看到的现象是「卡在启动画面进不去」。
 *
 * <h3>兜底做法</h3>
 * 主动轮询 {@link TextureView#getBitmap(int, int)} 采样，
 * 只要取到「非全透明、且不是纯黑」的像素，就认定游戏画面已经出来了，
 * 立即回调放行。同时设一个最大探测时长，超时后**无条件放行** ——
 * 宁可露出尚未就绪的画面，也不要让玩家卡死在等待界面。
 *
 * <p>Pojav 与 Boat 两个后端共用这一份实现，行为保持一致。
 */
public class GameFrameProbe {

    /** 采样间隔 */
    private static final int INTERVAL_MS = 250;
    /** 最大探测次数（40 × 250ms ≈ 10 秒）；超时无条件放行 */
    private static final int MAX_TRIES = 40;

    /** 画面已输出时的回调 */
    public interface Callback {
        void onFirstFrame();
    }

    private final TextureView target;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private int tries;
    private boolean stopped;
    private boolean fired;

    public GameFrameProbe(TextureView target, Callback callback) {
        this.target = target;
        this.callback = callback;
    }

    /** 开始探测（主线程调用） */
    public void start() {
        tries = 0;
        stopped = false;
        fired = false;
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, INTERVAL_MS);
    }

    /** 停止探测（正规回调到达时调用，避免重复触发） */
    public void stop() {
        stopped = true;
        handler.removeCallbacks(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (stopped) return;
            tries++;
            boolean ready = false;
            try {
                ready = hasVisibleFrame();
            } catch (Throwable ignored) {
                // 采样失败不算致命：可能视图还没就绪，下一轮再试
            }
            if (ready || tries >= MAX_TRIES) {
                fire();
                return;
            }
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    private void fire() {
        if (fired) return;
        fired = true;
        stopped = true;
        handler.removeCallbacks(tick);
        try {
            callback.onFirstFrame();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 采样一个很小的位图，判断是否已经有可见内容。
     *
     * <p>判定刻意宽松：只要**存在任意一个 alpha &gt; 0 且不是纯黑**的像素就返回 true。
     * 因为游戏画面一旦出来就会盖住一切，早一点放行没有任何副作用。
     */
    private boolean hasVisibleFrame() {
        if (target == null || !target.isAvailable()) return false;
        Bitmap bmp = target.getBitmap(32, 18);
        if (bmp == null) return false;
        try {
            for (int y = 0; y < bmp.getHeight(); y++) {
                for (int x = 0; x < bmp.getWidth(); x++) {
                    int p = bmp.getPixel(x, y);
                    int a = (p >>> 24) & 0xFF;
                    int r = (p >> 16) & 0xFF;
                    int g = (p >> 8) & 0xFF;
                    int b = p & 0xFF;
                    if (a > 0 && (r > 8 || g > 8 || b > 8)) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            bmp.recycle();
        }
    }
}
