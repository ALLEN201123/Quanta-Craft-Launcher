package com.qcl.launcher.control.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.qcl.launcher.R;
import com.qcl.launcher.utils.convert.ConvertUtils;

public class LayoutPanel extends RelativeLayout {

    private static final int POSITION_MODE_PERCENT = 0;
    private static final int POSITION_MODE_ABSOLUTE = 1;

    private int positionMode = POSITION_MODE_PERCENT;

    private float[] xReference;
    private float[] yReference;

    private boolean showBackground = false;
    private boolean showReference = false;

    private Paint linePaint;
    private Path path;
    private Paint textPaint;
    private String xText;
    private String yText;

    // ------------------------------------------------------------------
    // 1.0.6：游戏启动等待界面的背景改为**动态轮换**。
    //
    // 原来这里在构造时一次性 decode 写死的 ic_background.jpg，是静态图，
    // 所以「点启动游戏 → 等待画面」永远显示同一张旧图（用户反馈）。
    // 现在改为与主界面同源的 4 张图，每 10 秒轮换，并做 600ms 交叉淡入，
    // 避免切换时闪黑。
    //
    // ⚠️ 本类是自定义 View，背景是用 Canvas 直接 drawBitmap 画的，
    // 不是 setBackground(Drawable)，所以不能直接复用 DynamicBackground
    // （那个是给 View 设 Drawable 背景用的）。这里自己维护两帧 + 淡入进度。
    // ------------------------------------------------------------------
    private static final long BG_INTERVAL_MS = 10_000L;
    private static final int BG_FADE_MS = 600;

    /**
     * 1.0.7：等待界面最长遮挡时间。
     * 游戏真的在跑却因为回调没派发而永远卡在等待画面，是玩家最不能接受的失败方式
     * —— 至少要让画面出来，哪怕背景撤早了看到一瞬间黑屏。
     * 取 30 秒：够慢设备冷启动 JVM + 加载资源，又不至于让人等到心慌。
     */
    private static final long BG_MAX_SHOW_MS = 30_000L;

    private static final int[] BG_RES_IDS = new int[]{
            R.drawable.qcl_bg_1,
            R.drawable.qcl_bg_2,
            R.drawable.qcl_bg_3,
            R.drawable.qcl_bg_4
    };

    /** 轮换用：解码后的图缓存（按索引懒加载） */
    private final Bitmap[] bgCache = new Bitmap[BG_RES_IDS.length];
    private int bgIndex = 0;
    /** 正在淡出的上一张（null 表示没有过渡） */
    private Bitmap bgPrev = null;
    /** 淡入进度 0..1 */
    private float bgFade = 1f;
    private boolean bgRunning = false;
    /** 上一帧的时间戳，用于按真实时间推算淡入进度 */
    private long bgLastFrame = 0L;

    private final Handler bgHandler = new Handler(Looper.getMainLooper());

    /** 1.0.7：硬超时兜底，到点无条件撤走等待背景 */
    private final Runnable bgTimeout = new Runnable() {
        @Override
        public void run() {
            if (showBackground) {
                android.util.Log.w("QCL", "LayoutPanel: 等待背景超过 "
                        + BG_MAX_SHOW_MS + "ms 仍未收到画面回调，强制撤除遮挡");
                hideBackground();
            }
        }
    };

    private final Runnable bgTick = new Runnable() {
        @Override
        public void run() {
            if (!bgRunning) return;
            bgIndex = (bgIndex + 1) % BG_RES_IDS.length;
            // 把当前帧记为「上一张」，然后开始淡入新帧
            bgPrev = bgCache[(bgIndex - 1 + BG_RES_IDS.length) % BG_RES_IDS.length];
            bgFade = 0f;
            bgLastFrame = System.currentTimeMillis();
            invalidate();
            bgHandler.postDelayed(this, BG_INTERVAL_MS);
        }
    };

    public LayoutPanel(Context context) {
        super(context);
    }

    public LayoutPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setColor(getContext().getColor(R.color.colorGreen));
        linePaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(getContext().getColor(R.color.colorGreen));
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(50);

        xReference = new float[2];
        yReference = new float[2];

        // 首帧立即解码（启动等待界面要马上有东西可显示，不能等轮换第一次触发）
        getBg(0);
    }

    /** 按屏幕宽度降采样解码第 i 张图；失败返回 null（绘制时跳过，不至于崩） */
    private Bitmap decodeBg(int i) {
        if (i < 0 || i >= BG_RES_IDS.length) return null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(getContext().getResources(), BG_RES_IDS[i], bounds);
            int w = bounds.outWidth;
            if (w <= 0) return null;

            // 与 DynamicBackground 保持一致：目标是屏宽的 1.5 倍，避免放大后发糊
            int reqW = Math.max(1080, getContext().getResources().getDisplayMetrics().widthPixels * 3 / 2);
            int sample = 1;
            while (w / sample > reqW * 2) sample *= 2;

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sample;
            o.inPreferredConfig = Bitmap.Config.RGB_565;   // 背景无 alpha，省一半内存
            return BitmapFactory.decodeResource(getContext().getResources(), BG_RES_IDS[i], o);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 取第 i 张（懒加载 + 缓存） */
    private Bitmap getBg(int i) {
        if (i < 0 || i >= BG_RES_IDS.length) return null;
        if (bgCache[i] == null) {
            bgCache[i] = decodeBg(i);
        }
        return bgCache[i];
    }

    /** 开始轮换（showBackground 时调用） */
    private void startBgRotation() {
        if (bgRunning) return;
        bgRunning = true;
        bgIndex = 0;
        getBg(0);          // 首张确保已解码
        getBg(1);          // 预取下一张，避免切换那一刻现解码造成卡顿
        bgHandler.removeCallbacks(bgTick);
        bgHandler.postDelayed(bgTick, BG_INTERVAL_MS);
    }

    /** 停止轮换（游戏画面出来、背景隐藏时调用，省 CPU/内存） */
    private void stopBgRotation() {
        bgRunning = false;
        bgHandler.removeCallbacks(bgTick);
    }

    @Override
    protected void onDetachedFromWindow() {
        bgHandler.removeCallbacks(bgTimeout);
        stopBgRotation();
        super.onDetachedFromWindow();
    }

    @Override
    @SuppressLint("DrawAllocation")
    protected void onDraw(Canvas canvas) {
        if (showBackground){
            Bitmap cur = getBg(bgIndex);
            // 淡入过渡中：先画旧的，再按进度叠画新的（透明度递增）
            if (bgPrev != null && bgFade < 1f && cur != null) {
                long now = System.currentTimeMillis();
                if (bgLastFrame == 0L) bgLastFrame = now;
                bgFade = Math.min(1f, bgFade + (float)(now - bgLastFrame) / BG_FADE_MS);
                bgLastFrame = now;

                drawBgBitmap(canvas, bgPrev, 255);
                int alpha = (int) (255 * bgFade);
                drawBgBitmap(canvas, cur, alpha);

                if (bgFade < 1f) {
                    postInvalidateOnAnimation();   // 继续刷新直到淡入完成
                } else {
                    bgPrev = null;
                }
            }
            else if (cur != null) {
                drawBgBitmap(canvas, cur, 255);
            }
        }
        if (showReference){
            path = new Path();
            path.moveTo(xReference[0],0);
            path.lineTo(xReference[0],getHeight());
            path.moveTo(xReference[1],0);
            path.lineTo(xReference[1],getHeight());
            path.moveTo(0,yReference[0]);
            path.lineTo(getWidth(),yReference[0]);
            path.moveTo(0,yReference[1]);
            path.lineTo(getWidth(),yReference[1]);
            canvas.drawPath(path,linePaint);
            canvas.drawText(xText,100,100,textPaint);
            canvas.drawText(yText,100,200,textPaint);
        }
        // 1.0.6：移除原来这里的**无条件 invalidate()**。
        // 那会导致每一帧都请求重绘 → 一直刷新的死循环，白烧电和 CPU。
        // 现在只在「编辑参考线」或「背景淡入中」这两种确实需要连续刷新的场景下重绘。
        if (showReference) {
            invalidate();
        }
    }

    /** 把一张背景位图按「铺满 View」的方式画到画布上（可选透明度） */
    private void drawBgBitmap(Canvas canvas, Bitmap bmp, int alpha) {
        if (bmp == null || bmp.isRecycled()) return;
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        if (w <= 0 || h <= 0) return;
        Rect src = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
        Rect dst = new Rect(0, 0, w, h);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);
        if (alpha < 255) {
            p.setAlpha(Math.max(0, Math.min(255, alpha)));
        }
        canvas.drawBitmap(bmp, src, dst, p);
    }

    public void showReference(int positionMode, float x, float y, int width, int height){
        this.positionMode = positionMode;
        if (positionMode == POSITION_MODE_PERCENT){
            xText = "X:" + ((int) ((x / (getWidth() - width)) * 1000)) / 10f + "%";
            yText = "Y:" + ((int) ((y / (getHeight() - height)) * 1000)) / 10f + "%";
        }
        if (positionMode == POSITION_MODE_ABSOLUTE){
            xText = "X:" + ConvertUtils.px2dip(getContext(),x) + "dp";
            yText = "Y:" + ConvertUtils.px2dip(getContext(),y) + "dp";
        }
        this.xReference[0] = x;
        this.yReference[0] = y;
        this.xReference[1] = x + width;
        this.yReference[1] = y + height;
        showReference = true;
    }

    public void hideReference(){
        showReference = false;
    }

    public void showBackground(){
        showBackground = true;
        // 1.0.6：显示背景的同时启动轮换
        startBgRotation();
        // 1.0.7：第三层保险 —— 「最长遮挡时间」兜底。
        //
        // 前两层保险是：①启动器自己的 onSurfaceTextureUpdated → onPicOutput() 正规回调；
        // ②GameFrameProbe 主动采样 TextureView 位图。
        // 但只要这两层任何一层因为设备 / 驱动差异没生效，玩家就会**永远卡在等待界面**
        // （游戏其实在后台跑，只是画面被这层盖着 —— 用户 1.0.6 实机就是这个症状）。
        // 所以这里加一个硬超时：无论游戏有没有真正出画面，到点都必须把遮挡撤掉，
        // 「最坏情况是能玩，而不是永远看不见」。
        bgHandler.removeCallbacks(bgTimeout);
        bgHandler.postDelayed(bgTimeout, BG_MAX_SHOW_MS);
        invalidate();
    }

    public void hideBackground() {
        showBackground = false;
        // 游戏画面已出来，背景不再可见 → 停掉轮换，省 CPU / 电
        bgHandler.removeCallbacks(bgTimeout);
        stopBgRotation();
        // 1.0.9 关键修复：必须主动触发重绘！
        // showBackground 只改了内部标志，若没人 invalidate，View 保持最后一帧——
        // 屏幕上等待背景依然显示（真机实测：信号链路全部走通但画面不切换，
        // 只有切后台/回前台强制全量重绘后才按新状态画出游戏画面）。
        // TextureView 的内容更新走独立合成路径，不会自动带动本 View 重绘。
        invalidate();
    }
}
