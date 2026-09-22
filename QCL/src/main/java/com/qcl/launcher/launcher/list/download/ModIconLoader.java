package com.qcl.launcher.launcher.list.download;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ★ 1.2.7：下载页（模组 / 整合包 / 资源包 / 光影 / 世界）的**图标加载器**。
 *
 * 【为什么单独做这个】
 * 老代码是在 {@code getView} 里对每一个条目**new 一个 Thread**去下载图标，而且：
 *   ① **没有任何超时** —— 图标服务器慢（国内连 Modrinth 的 cdn.modrinth.com 尤其慢）时
 *      线程会一直挂着，表现就是「图标一直白着、加载半天出不来」；
 *   ② **没有缓存** —— 每次滚动、每次重进页面都要重新下一遍，永远慢；
 *   ③ **没有并发上限** —— 一屏 50 个条目就是 50 条并发连接，手机流量下互相挤，成功率更低；
 *   ④ 解码用的是 {@code decodeStream}，遇到 CurseForge 的**动图 GIF 图标**会解不出来（返回 null）
 *      → ImageView 拿不到图，继续白着；
 *   ⑤ 还有个隐患：`viewHolder.icon.getTag()` 为空时直接 NPE，整个线程静默死掉。
 *
 * FCL 那边是直接交给 Glide（自带超时/内存+磁盘缓存/线程池/GIF）。这里不想为了图标再引一个
 * 大依赖（本工程的 dex 对第三方库版本很敏感，踩过 D8 的坑），所以按同样的效果手写一个：
 *   · 内存 LruCache + 磁盘缓存（第二次进来立刻就有）
 *   · 固定 6 线程池（不再一屏 50 条连接）
 *   · 连接 8s / 读取 12s 超时（拉不到就放弃，不留悬挂线程）
 *   · 解码失败（含动图 GIF）在 API 28+ 用 ImageDecoder 取首帧再试一次
 *   · 回调前检查条目位置标签，避免列表复用把图标贴错行
 */
public final class ModIconLoader {

    /** 内存缓存：直接缓存解码后的位图（图标都很小，6MB 够放几百个） */
    private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(6 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getByteCount();
        }
    };

    private static final Object LOCK = new Object();
    private static ExecutorService pool;
    private static File diskDir;

    private ModIconLoader() {
    }

    private static ExecutorService pool() {
        synchronized (LOCK) {
            if (pool == null) {
                pool = Executors.newFixedThreadPool(6);
            }
            return pool;
        }
    }

    private static File diskDir(Context context) {
        synchronized (LOCK) {
            if (diskDir == null) {
                diskDir = new File(context.getCacheDir(), "mod_icons");
                if (!diskDir.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    diskDir.mkdirs();
                }
            }
            return diskDir;
        }
    }

    private static String keyOf(String url) {
        // 用 hashCode 的十六进制当文件名（不需要密码学强度，够区分就行）
        return Integer.toHexString(url.hashCode()) + "_" + Integer.toHexString(url.length());
    }

    /**
     * 给某个列表条目加载图标。
     *
     * @param view     图标控件（调用方已按自己的方式设好占位图）
     * @param url      图标地址（可能为 null / 空 —— 那就保持占位图，不去白等）
     * @param position 这个条目在列表里的位置（用于列表复用时校验，避免贴错行）
     */
    public static void load(final Context context, final ImageView view, final String url,
                            final int position) {
        if (view == null || url == null || url.trim().isEmpty()) {
            return;
        }
        final String key = keyOf(url);

        Bitmap cached = MEM.get(key);
        if (cached != null && !cached.isRecycled()) {
            view.setImageBitmap(cached);
            return;
        }

        // 磁盘缓存：命中就直接读出来显示（第二次进页面/滚动回来是秒开）
        final File f = new File(diskDir(context), key);
        if (f.isFile()) {
            Bitmap b = decodeFile(f, 0);
            if (b != null) {
                MEM.put(key, b);
                view.setImageBitmap(b);
                return;
            }
            //noinspection ResultOfMethodCallIgnored
            f.delete();   // 缓存的图坏了，删掉重下
        }

        pool().execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = downloadAndDecode(url);
                if (bmp == null) {
                    return;
                }
                MEM.put(key, bmp);
                saveToDisk(f, bmp);
                final Bitmap result = bmp;
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        Object tag = view.getTag();
                        // 列表复用校验：这个 ImageView 现在还是不是当初那一行
                        if (!(tag instanceof Integer) || ((Integer) tag).intValue() == position) {
                            view.setImageBitmap(result);
                        }
                    }
                });
            }
        });
    }

    private static Bitmap downloadAndDecode(String url) {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(true);
            // 有些图标服务器会对没有 UA 的请求返回 403
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) QCL");
            conn.setRequestProperty("Accept", "image/*,*/*;q=0.8");
            conn.connect();
            if (conn.getResponseCode() != 200) {
                return null;
            }
            in = conn.getInputStream();
            byte[] data = readAll(in);
            if (data == null || data.length == 0) {
                return null;
            }
            Bitmap b = decodeBytes(data, 0);
            if (b == null && Build.VERSION.SDK_INT >= 28) {
                // CurseForge 的图标不少是动图 GIF / WebP，BitmapFactory 解不出来时用 ImageDecoder 取首帧
                b = decodeWithImageDecoder(data);
            }
            return b;
        } catch (Throwable e) {
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Throwable ignored) {
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static byte[] readAll(InputStream in) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Throwable e) {
            return null;
        }
    }

    private static Bitmap decodeBytes(byte[] data, int sampleSize) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sampleSize;
            o.inPreferredConfig = Bitmap.Config.RGB_565;   // 图标不需要透明度精度，省一半内存
            return BitmapFactory.decodeByteArray(data, 0, data.length, o);
        } catch (Throwable e) {
            return null;
        }
    }

    private static Bitmap decodeWithImageDecoder(byte[] data) {
        try {
            android.graphics.ImageDecoder.Source src =
                    android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(data));
            return android.graphics.ImageDecoder.decodeBitmap(src, new android.graphics.ImageDecoder.OnHeaderDecodedListener() {
                @Override
                public void onHeaderDecoded(android.graphics.ImageDecoder decoder,
                                            android.graphics.ImageDecoder.ImageInfo info,
                                            android.graphics.ImageDecoder.Source source) {
                    decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB));
                }
            });
        } catch (Throwable e) {
            return null;
        }
    }

    private static Bitmap decodeFile(File f, int sampleSize) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = sampleSize;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        } catch (Throwable e) {
            return null;
        }
    }

    private static void saveToDisk(File f, Bitmap bmp) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Throwable ignored) {
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
