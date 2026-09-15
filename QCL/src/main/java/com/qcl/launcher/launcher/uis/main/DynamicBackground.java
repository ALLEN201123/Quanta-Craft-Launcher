package com.qcl.launcher.launcher.uis.main;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.qcl.launcher.R;

/**
 * QCL 动态背景：4 张远古风格图循环切换，每张淡出淡入。
 *
 * 设计要点：
 * 1. 图片先做「模糊 + 压暗 + 降饱和」处理 —— 背景主要用于衬托前景控件，
 *    原图直接铺会太抢眼，压暗处理后白字/面板对比度才够。
 * 2. 背景由两层叠加：底层是当前图，上层做 TransitionDrawable 淡入，
 *    这样切换时不会闪黑（直接 setBackground 会有一瞬间的空白）。
 * 3. 处理结果按「屏宽」缓存一次，避免每 10 秒重新解码 + 模糊（那会卡顿）。
 *    内存不足时用 inSampleSize 降采样，防止老设备 OOM。
 */
public class DynamicBackground {

    /** 每张图停留时长（用户要求 10 秒） */
    private static final long INTERVAL_MS = 10_000L;
    /** 淡入淡出时长 */
    private static final int FADE_MS = 600;

    private static final int[] RES_IDS = new int[]{
            R.drawable.qcl_bg_1,
            R.drawable.qcl_bg_2,
            R.drawable.qcl_bg_3,
            R.drawable.qcl_bg_4
    };

    private final Activity activity;
    private final View target;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Drawable[] cache = new Drawable[RES_IDS.length];

    private int index = 0;
    private boolean running = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            index = (index + 1) % RES_IDS.length;
            apply(index, true);
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public DynamicBackground(Activity activity, View target) {
        this.activity = activity;
        this.target = target;
    }

    /** 开始轮换（首张立即显示，不淡入） */
    public void start() {
        if (running) return;
        running = true;
        apply(index, false);
        handler.postDelayed(tick, INTERVAL_MS);
    }

    /** 停止并释放 */
    public void stop() {
        running = false;
        handler.removeCallbacks(tick);
    }

    /** 应用第 i 张背景；animate=true 时用淡入过渡 */
    private void apply(int i, boolean animate) {
        Drawable next = get(i);
        if (next == null) return;
        if (!animate || target.getBackground() == null) {
            target.setBackground(next);
            return;
        }
        // TransitionDrawable：从「当前背景」淡入到「下一张」
        Drawable current = target.getBackground();
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{current, next});
        td.setCrossFadeEnabled(true);
        target.setBackground(td);
        td.startTransition(FADE_MS);
    }

    /** 取（并懒加载）第 i 张处理后的图 */
    private Drawable get(int i) {
        if (cache[i] != null) return cache[i];
        try {
            Bitmap bmp = decodeScaled(RES_IDS[i]);
            if (bmp == null) return null;
            Bitmap processed = dim(bmp);
            if (processed != bmp) bmp.recycle();
            cache[i] = new BitmapDrawable(activity.getResources(), processed);
        } catch (Throwable t) {
            // 解码失败时退回纯色，至少不至于崩溃
            cache[i] = new ColorDrawable(0xFFEDEDED);
        }
        return cache[i];
    }

    /**
     * 按屏幕宽度降采样解码。原图可能是大图，直接整张解码 + 模糊会吃掉几十 MB 内存。
     */
    private Bitmap decodeScaled(int resId) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(activity.getResources(), resId, o);
        int w = o.outWidth, h = o.outHeight;
        if (w <= 0 || h <= 0) return null;

        // 背景不需要原始分辨率，压到屏幕宽度的一半以内即可（再放大铺满，肉眼无差）
        int reqW = Math.max(720, activity.getResources().getDisplayMetrics().widthPixels / 2);
        int sample = 1;
        while (w / sample > reqW * 2) sample *= 2;

        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        d.inPreferredConfig = Bitmap.Config.RGB_565; // 背景不需要 alpha，省一半内存
        try {
            return BitmapFactory.decodeResource(activity.getResources(), resId, d);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * 压暗 + 降饱和。让背景退到后面去，保证前景文字/面板的可读性。
     */
    private Bitmap dim(Bitmap src) {
        try {
            Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(out);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

            // 饱和度降到 75%，整体亮度压到 62%，稍微压一点让白字更清楚
            ColorMatrix cm = new ColorMatrix();
            cm.setSaturation(0.75f);
            float scale = 0.62f;
            float[] m = cm.getArray();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    m[i * 5 + j] *= scale;
                }
            }
            paint.setColorFilter(new ColorMatrixColorFilter(cm));
            canvas.drawBitmap(src, 0, 0, paint);
            return out;
        } catch (Throwable t) {
            return src;
        }
    }

    /** 静态便利：直接给某个 View 铺一张（用于不需要轮换的场景） */
    public static void applySingle(Activity activity, View view, int resId) {
        try {
            Drawable d = ContextCompat.getDrawable(activity, resId);
            if (d != null) view.setBackground(d);
        } catch (Throwable ignored) {
        }
    }
}
