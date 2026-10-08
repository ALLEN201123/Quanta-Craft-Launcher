/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Activity
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.graphics.drawable.BitmapDrawable
 *  android.graphics.drawable.ColorDrawable
 *  android.graphics.drawable.Drawable
 *  android.graphics.drawable.TransitionDrawable
 *  android.os.Handler
 *  android.os.Looper
 *  android.view.View
 *  androidx.core.content.ContextCompat
 */
package com.qcl.launcher.launcher.uis.main;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.core.content.ContextCompat;

import com.qcl.launcher.R;
public class DynamicBackground {
    private static final long INTERVAL_MS = 10000L;
    private static final int FADE_MS = 600;
    public static final int[] RES_IDS = new int[]{R.drawable.qcl_bg_1, R.drawable.qcl_bg_2, R.drawable.qcl_bg_3, R.drawable.qcl_bg_4, R.drawable.qcl_bg_5, R.drawable.qcl_bg_6};

    /** ★ 1.5.0：白天背景图（玩家提供的 26.3 截图）。 */
    public static final int RES_DAY = R.drawable.qcl_bg_day;
    /** ★ 1.5.0：黑夜背景图（玩家提供的 26.3 截图，沿用原名 ic_background_classic）。 */
    public static final int RES_NIGHT = R.drawable.ic_background_classic;

    /** 白天时段起止（含起、不含止）：6:00 ~ 18:00 显示白天图，其余显示黑夜图。 */
    public static final int DAY_START_HOUR = 6;
    public static final int DAY_END_HOUR = 18;
    private final Activity activity;
    private final View target;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Drawable[] cache = new Drawable[RES_IDS.length];
    private int index = 0;
    private boolean running = false;
    private final Runnable tick = new Runnable(){

        @Override
        public void run() {
            if (!DynamicBackground.this.running) {
                return;
            }
            DynamicBackground.this.index = (DynamicBackground.this.index + 1) % RES_IDS.length;
            DynamicBackground.this.apply(DynamicBackground.this.index, true);
            DynamicBackground.this.handler.postDelayed((Runnable)this, 10000L);
        }
    };

    public DynamicBackground(Activity activity, View target) {
        this.activity = activity;
        this.target = target;
    }

    public void start() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.apply(this.index, false);
        this.handler.postDelayed(this.tick, 10000L);
    }

    public void stop() {
        this.running = false;
        this.handler.removeCallbacks(this.tick);
    }

    /**
     * ★ 1.2.9：从后台回到前台后**保证轮播还在跑**。
     *
     * 现象：切到后台再回来，背景图就不动了（卡在一张上）。
     * 可能的原因有几种（Handler 回调被系统清掉、背景被别处换成了静态图、
     * 或者中途被 stop 过），这里不猜具体是哪一种 —— 回到前台就无条件把下一轮挂上，
     * 并且发现背景已经不是轮播的图时补一张回来。
     */
    public void ensureRunning() {
        if (!this.running) {
            this.start();
            return;
        }
        this.handler.removeCallbacks(this.tick);
        this.handler.postDelayed(this.tick, INTERVAL_MS);
        Drawable cur = this.target.getBackground();
        // 背景被主题/别处换成静态图了（或压根没有）→ 把当前这张补回来
        if (cur == null || !(cur instanceof TransitionDrawable)) {
            this.apply(this.index, false);
        }
    }

    private void apply(int i, boolean animate) {
        Drawable next = this.get(i);
        if (next == null) {
            return;
        }
        if (!animate || this.target.getBackground() == null) {
            this.target.setBackground(next);
            return;
        }
        Drawable current = this.target.getBackground();
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{current, next});
        td.setCrossFadeEnabled(true);
        this.target.setBackground((Drawable)td);
        td.startTransition(600);
    }

    private Drawable get(int i) {
        if (this.cache[i] != null) {
            return this.cache[i];
        }
        try {
            Bitmap bmp = this.decodeScaled(RES_IDS[i]);
            if (bmp == null) {
                return null;
            }
            Bitmap processed = this.dim(bmp);
            if (processed != bmp) {
                bmp.recycle();
            }
            this.cache[i] = new BitmapDrawable(this.activity.getResources(), processed);
        }
        catch (Throwable t) {
            this.cache[i] = new ColorDrawable(-1184275);
        }
        return this.cache[i];
    }

    private Bitmap decodeScaled(int resId) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeResource((Resources)this.activity.getResources(), (int)resId, (BitmapFactory.Options)o);
        int w = o.outWidth;
        int h = o.outHeight;
        if (w <= 0 || h <= 0) {
            return null;
        }
        int reqW = Math.max(1080, this.activity.getResources().getDisplayMetrics().widthPixels * 3 / 2);
        int sample = 1;
        while (w / sample > reqW * 2) {
            sample *= 2;
        }
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        d.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            return BitmapFactory.decodeResource((Resources)this.activity.getResources(), (int)resId, (BitmapFactory.Options)d);
        }
        catch (OutOfMemoryError e) {
            return null;
        }
    }

    private Bitmap dim(Bitmap src) {
        return src;
    }

    public static void applySingle(Activity activity, View view, int resId) {
        try {
            Drawable d = ContextCompat.getDrawable((Context)activity, (int)resId);
            if (d != null) {
                view.setBackground(d);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    /** ★ 1.5.0：当前是否属于白天。
     *  ★★★ 用户明确：**背景图 + UI 配色统一按「手机系统夜间模式」切换**
     *   （不是按现实时间）。系统深色模式 → 黑夜图 + 深色配色；浅色模式 → 白天图 + 亮色配色。 */
    public static boolean isDaytimeNow() {
        return !isSystemNightMode();
    }

    /** ★ 1.5.0：手机系统当前是否处于夜间（深色）模式。
     *  ★ 背景图与全应用配色（values-night/colors.xml）**共用这一个判据**，保证两者永远一致。 */
    public static boolean isSystemNightMode() {
        try {
            android.content.res.Configuration cfg = android.content.res.Resources
                    .getSystem().getConfiguration();
            int mode = cfg.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        } catch (Throwable t) {
            return false;
        }
    }

    /** ★ 1.5.0：按系统夜间模式返回该用的背景图资源 id。 */
    public static int autoResId() {
        return isDaytimeNow() ? RES_DAY : RES_NIGHT;
    }

    /**
     * ★ 1.5.0：把昼夜背景应用到给定视图（按现实时间自动选图）。
     *
     * <p>供 {@code MainActivity} 启动 / 回到前台时调用；不含动画，
     * 因为这是「静态切换」而不是轮播。
     */
    public static void applyAutoDayNight(Activity activity, View view) {
        applySingle(activity, view, autoResId());
    }
}

