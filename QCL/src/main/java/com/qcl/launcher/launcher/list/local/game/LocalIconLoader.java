package com.qcl.launcher.launcher.list.local.game;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.LruCache;
import android.view.View;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ★ 1.2.9：**本地图标的异步加载器**（版本列表 / 版本下拉里的 icon.png）。
 *
 * 老代码是在 {@code getView} 里直接 `DrawableUtils.getDrawableFromFile(path)` ——
 * 也就是**在 UI 线程上读文件 + 解码图片**，列表一滚动就一帧一帧地卡；
 * 而且每次滚回来都要重新解一遍，没有缓存。
 *
 * 这里改成：内存缓存命中直接显示；否则丢到小线程池里解码，解完再贴回对应的那一行
 * （用 tag 校验行号，避免列表复用把图标贴错）。
 */
public final class LocalIconLoader {

    private static final LruCache<String, Bitmap> MEM = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getByteCount();
        }
    };

    /** 图标都很小，2 条并发足够，别跟下载抢资源 */
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);

    private LocalIconLoader() {
    }

    /**
     * 异步给某个控件设置背景图标（本地文件路径）。
     *
     * @param view 要设置背景的控件
     * @param path 图标文件路径（为空就什么都不做，保留原来的默认图标）
     */
    public static void loadBackground(final View view, final String path) {
        if (view == null || path == null || path.isEmpty()) {
            return;
        }
        Bitmap cached = MEM.get(path);
        if (cached != null && !cached.isRecycled()) {
            view.setBackground(new BitmapDrawable(view.getResources(), cached));
            return;
        }
        // 记住这一行期望的是哪张图：解码完如果行已经被复用（tag 变了）就不贴，避免贴错
        view.setTag(path);
        POOL.execute(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp = decode(path);
                if (bmp == null) {
                    return;
                }
                MEM.put(path, bmp);
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        Object tag = view.getTag();
                        if (path.equals(tag)) {
                            view.setBackground(new BitmapDrawable(view.getResources(), bmp));
                        }
                    }
                });
            }
        });
    }

    private static Bitmap decode(String path) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, o);
        } catch (Throwable t) {
            return null;
        }
    }
}
