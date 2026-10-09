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

    /** ★ 1.5.0 白天时段起止（含起、不含止）：**08:00 ~ 19:00** 显示白天图，其余显示黑夜图（用户定的时间点）。 */
    public static final int DAY_START_HOUR = 8;
    public static final int DAY_END_HOUR = 19;
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

    /**
     * 拿小时数；**优先等一次网络时间**（最多等 {@code timeoutMs} 毫秒）。
     *
     * <p>★ 为什么需要「等」：启动首帧若用本地时钟（实测设备慢 8 小时）会判错昼夜，
     *   先显示错误的配色、再等校正 = 用户会看到一次明显的「白天↔夜晚」闪变。
     *   这里在主线程短暂等待网络时间（只在缓存为空时等待），把闪变消灭在源头。
     *   超时或取不到就照常用本地时钟兜底，**绝不阻塞启动**。
     */
    private static int currentHourBlocking(int timeoutMs) {
        int h = cachedHour;
        if (h >= 0) {
            return h;
        }
        if (timeoutMs > 0) {
            try {
                java.util.concurrent.FutureTask<Integer> task =
                        new java.util.concurrent.FutureTask<>(DynamicBackground::fetchNetHour);
                new Thread(task, "qcl-daynight-boot").start();
                try {
                    Integer r = task.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (r != null && r >= 0) {
                        cachedHour = r;
                        return r;
                    }
                } catch (Throwable ignored) {
                }
            } catch (Throwable ignored) {
            }
        }
        return currentHour();
    }

    /**
     * 当前小时（0–23）——**优先用网络时间**，取不到才退回设备本地时钟。
     *
     * <p>★★ 为什么必须优先网络时间：实测 MuMu 模拟器上
     *   <pre>电脑 2026-10-09 20:24  ↔  设备 date 显示 12:21，epoch 差 28927 秒（≈8h整）</pre>
     *   设备 {@code auto_time=1}、时区标签也是 {@code Asia/Shanghai}，但时钟本体整整慢一个时区
     *   ⇒ 只读 {@code Calendar} 会在晚上 8 点判成中午 12 点，**昼夜直接反了**。
     *   所以取网络时间（HTTP 响应头 {@code Date}）当权威，本地时钟只当兜底。
     *
     * <p>★★★ **绝不能在主线程调本方法**：{@code applyAutoDayNight} /
     *   {@code applyRealTimeNightMode} 全在主线程（onCreate、onResume），
     *   Android 会直接抛 {@code NetworkOnMainThreadException}。
     *   所以拆成两半：主线程只读 {@link #cachedHour}，取网络时间丢后台线程
     *   （{@link #refreshHourAsync}），取回后再回调刷新界面。
     *
     * @return 0–23；彻底取不到时返回 -1
     */
    private static volatile int cachedHour = -1;

    /** 后台取网络时间 → 更新缓存 → 回调。并发调用只跑一个线程。 */
    private static final java.util.concurrent.atomic.AtomicBoolean FETCHING =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 主线程安全的取小时：只读缓存，绝不联网。
     * 缓存还没填上时退回本地时钟（比返回 -1 强）。
     */
    private static int currentHour() {
        int h = cachedHour;
        if (h >= 0) {
            return h;
        }
        try {
            return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        } catch (Throwable t) {
            return -1;
        }
    }

    /**
     * 后台刷新网络时间；完成后在主线程跑 {@code onDone}（可为空）。
     * 失败静默 —— 缓存与本地时钟兜底已经够用。
     */
    public static void refreshHourAsync(final Runnable onDone) {
        if (!FETCHING.compareAndSet(false, true)) {
            // 已有一次在跑：等它完成即可（不重复发请求）
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int h = fetchNetHour();
                if (h >= 0) {
                    cachedHour = h;
                }
                FETCHING.set(false);
                if (onDone != null) {
                    try {
                        new Handler(Looper.getMainLooper()).post(onDone);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }, "qcl-daynight-clock").start();
    }

    /** 真去服务器取时间（**只能在后台线程调**）。取不到返回 -1。 */
    private static int fetchNetHour() {
        // 两个源都试：任何一个拿到就算数
        String[] hosts = {
                "http://connectivitycheck.gstatic.com/generate_204",
                "http://www.baidu.com/",
        };
        for (String url : hosts) {
            try {
                java.net.HttpURLConnection c =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                c.setConnectTimeout(3000);
                c.setReadTimeout(3000);
                c.setRequestMethod("HEAD");
                try {
                    c.connect();
                } catch (Throwable ignoreConnectOnly) {
                }
                long server = c.getHeaderFieldDate("Date", 0L);
                c.disconnect();
                if (server > 0L) {
                    // Date 头是 GMT，按本地时区换算成小时
                    java.util.Calendar g = java.util.Calendar.getInstance();
                    g.setTimeInMillis(server);
                    return g.get(java.util.Calendar.HOUR_OF_DAY);
                }
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }

    /**
     * ★ 1.5.0：当前是否属于白天 —— **按现实时间**（用户实测「下午了还是晚上的壁纸」）。
     *
     * <p>★★ 这里曾经被我改成"跟随手机系统夜间模式"（注释里写着"用户明确"），
     *   结果玩家的手机一直开着深色模式，于是**下午也显示夜晚图**。
     *   ⇒ 现在改回真正的「随时间变化」。
     *
     * <p>★ 分界按用户要求固定为 **早上 8 点 / 晚上 7 点**：
     * <ul>
     *   <li>夜晚：00:00–07:59</li>
     *   <li>白天：08:00–18:59</li>
     *   <li>夜晚：19:00–23:59</li>
     * </ul>
     */
    public static boolean isDaytimeNow() {
        // ★ 首次调用（启动首帧）时缓存还是空的：短暂等一次网络时间，
        //   免得先用错的本地时钟判一次昼夜、随后再闪变。
        //   3 秒上限，取不到就退回本地时钟，绝不卡住启动。
        int hour = currentHourBlocking(3000);
        if (hour < 0) {
            // 连时间都取不到 ⇒ 退回系统夜间模式，至少不会瞎猜
            return !isSystemNightMode();
        }
        // ★ 08:00 ~ 18:59 视为白天（用户定的时间点：早 8 点 / 晚 7 点）
        return hour >= DAY_START_HOUR && hour < DAY_END_HOUR;
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
     * ★ 1.5.0：让**整套配色**也跟着现实时间走（用户：「主题色也全部改一下」）。
     *
     * <p>★ 为什么需要这一步：壁纸是我自己画的（{@link #isDaytimeNow()} 判定），
     *   而界面配色走 Android 标准的 {@code values-night/colors.xml}，
     *   **只跟随手机系统深色模式**。两者判据不同 ⇒ 会出现
     *   「下午亮着、界面却是深色」这种自相矛盾。
     *   ⇒ 用 {@code AppCompatDelegate.setDefaultNightMode()} 把主题模式也按现实时间钉住，
     *   {@code values-night} 与壁纸就**永远一致**。
     *
     * <p>调用时机：{@code MainActivity.onCreate} 里、setContentView 之前。
     *
     * @return 实际设定到的模式（便于日志排查）
     */
    public static int applyRealTimeNightMode(android.content.Context ctx) {
        int mode;
        try {
            boolean day = isDaytimeNow();
            mode = day ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                       : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
            // ★★★ 关键补充（2026-10-09 实测踩坑）：
            //   只 setDefaultNightMode 可能「看起来没反应」——
            //   ① 它是**应用级**开关，对**已经创建**的 Activity 不一定立刻重建；
            //   ② 若当前 mode 与目标相同，AppCompat 干脆不做任何事（不重建）。
            //   而我们首调时网络时间还没回来（用本地时钟，可能是错的），
            //   等校正后 mode 恰好与首调相同 ⇒ 界面停在错误那一侧。
            //   ⇒ 追加 Activity 自身的 localNightMode：直接改**这一个 Activity** 的夜间标志，
            //     它会真正让 values-night 生效（配置变更能重建 Activity：
            //     MainActivity 的 configChanges 已刻意不含 uiMode）。
            //   ⚠ `Activity.getLocalNightMode()/setLocalNightMode()` 是 **API 29+**，
            //     本工程 minSdk 26 ⇒ 不能直接调（编译不过）。
            //     统一走 AppCompat 的 `AppCompatDelegate`（兼容 API 14+）。
            try {
                if (ctx instanceof androidx.appcompat.app.AppCompatActivity) {
                    androidx.appcompat.app.AppCompatActivity ac = (androidx.appcompat.app.AppCompatActivity) ctx;
                    if (ac.getDelegate() != null
                            && ac.getDelegate().getLocalNightMode() != mode) {
                        ac.getDelegate().setLocalNightMode(mode);
                    }
                }
            } catch (Throwable ignoreLocal) {
                // 拿不到 delegate 就只靠 setDefaultNightMode（对已运行的实例可能不重建）
            }
        } catch (Throwable t) {
            mode = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        return mode;
    }


    /**
     * ★ 1.5.0：把昼夜背景应用到给定视图（按现实时间自动选图）。
     *
     * <p>供 {@code MainActivity} 启动 / 回到前台时调用；不含动画，
     * 因为这是「静态切换」而不是轮播。
     *
     * <p>★★ 同时<b>异步校正时钟</b>：先用当前缓存/本地时钟贴一次（保证立刻有图），
     *   再后台取网络时间，取回来后若判定的昼夜变了就<b>自动重贴</b>。
     *   ⇒ 设备时钟慢 8 小时这类脏环境下，用户最终看到的仍是正确的那张图。
     */
    public static void applyAutoDayNight(final Activity activity, final View view) {
        applySingle(activity, view, autoResId());
        try {
            final int before = autoResId();
            final boolean beforeDay = isDaytimeNow();
            refreshHourAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (activity.isFinishing() || view == null) {
                            return;
                        }
                        if (autoResId() != before) {
                            // ★ 网络时间把昼夜判定掰回来了 → 重贴
                            applySingle(activity, view, autoResId());
                        }
                        if (isDaytimeNow() != beforeDay) {
                            // ★ 配色（values-night）也要跟着掰回来。
                            //   setDefaultNightMode 会触发 Activity 重建，视觉上就是整体换色。
                            applyRealTimeNightMode(activity);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }
}

