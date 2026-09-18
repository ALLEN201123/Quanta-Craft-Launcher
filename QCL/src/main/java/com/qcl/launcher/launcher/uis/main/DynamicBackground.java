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
}

