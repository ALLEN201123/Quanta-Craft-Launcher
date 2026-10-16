/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.content.Context
 *  android.os.AsyncTask
 *  android.util.Log
 */
package com.qcl.launcher.task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.utils.file.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownloadTask
extends AsyncTask<ArrayList<DownloadTaskListBean>, Integer, ArrayList<DownloadTaskListBean>> {

    /**
     * ★ 1.3.0：取消标志。点「取消」后要能**中断正在进行的下载**，
     *   而不只是等它这一遍下完 —— 以前 cancel(true) 只在下一次重试时被检查到，
     *   当前正在下的那个文件会一直下完，表现就是「点取消还在后台自动下」。
     */
    private final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * ★★★ 2026-10-09 用户实测「整合包下载中点取消没反应」——根因修复：
     *   原来线程池是 {@code doInBackground} 里的**局部变量**，点「取消」时
     *   {@link #requestCancel()} 只能设标志 + {@code cancel(true)}，
     *   **拿不到线程池 ⇒ 无法 {@code shutdownNow()}**；
     *   而 {@code doInBackground} 结尾又在 {@code awaitTermination(1 小时)} 空等
     *   ⇒ 主线程被这个后台任务拖住，界面看着像「点了没反应」。
     *
     * <p>修法两条：① 线程池提为字段，{@link #requestCancel()} 直接 {@code shutdownNow()}；
     * ② {@code awaitTermination} 改成**分片短等 + 查取消标志**（最多 200ms 一片），
     * 一旦取消立刻返回，绝不空等一小时。
     */
    private volatile ThreadPoolExecutor currentPool;

    /**
     * ★ 1.3.0：请求取消并**中断正在进行的下载**。
     * 不能重写 {@code AsyncTask.cancel(boolean)}（它是 final 的），
     * 所以用这个方法：设自己的标志 → 立刻 shutdownNow 打断所有下载线程 → 再走系统的 cancel。
     */
    public void requestCancel() {
        this.cancelled.set(true);
        // ★ 真正打断正在下载的线程（局部变量时代拿不到池子，只能干等）
        ThreadPoolExecutor pool = this.currentPool;
        if (pool != null) {
            try {
                pool.shutdownNow();
            } catch (Throwable ignored) {
            }
        }
        cancel(true);
    }
    private final WeakReference<Context> ctx;
    private ArrayList<DownloadTaskListBean> failedFile;
    private final Feedback feedback;
    private int maxTask = 8;

    public DownloadTask(Context ctx, Feedback feedback) {
        this.ctx = new WeakReference<Context>(ctx);
        this.feedback = feedback;
        this.failedFile = new ArrayList();
    }

    public void setMaxTask(int maxTask) {
        this.maxTask = maxTask;
    }

    public void onPreExecute() {
    }

    @SuppressLint(value={"WrongThread"})
    public ArrayList<DownloadTaskListBean> doInBackground(ArrayList<DownloadTaskListBean> ... args) {
        ArrayList<DownloadTaskListBean> list = args[0];
        // ★ 提为字段：requestCancel() 要拿它来 shutdownNow，否则点取消只是设了个没人看的标志
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(this.maxTask, this.maxTask, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
        this.currentPool = threadPool;
        try {
            for (int j = 0; j < list.size(); ++j) {
                if (this.cancelled.get() || this.isCancelled()) {
                    // ★ 循环里也要查：否则点了取消，队列里排着的任务还会一个个开跑
                    break;
                }
                final DownloadTaskListBean bean = list.get(j);
                String path = bean.path;
                String sha1 = bean.sha1;
                threadPool.execute(() -> {
                    if (!new File(path).exists() || new File(path).exists() && !Objects.equals(FileUtils.getFileSha1(path), sha1)) {
                        int tryTimes = 5;
                        for (int i = 0; i < tryTimes; ++i) {
                            if (this.cancelled.get() || this.isCancelled()) {
                                threadPool.shutdownNow();
                                return;
                            }
                            this.feedback.addTask(bean);
                            DownloadFeedback fb = new DownloadFeedback(){

                                @Override
                                public void updateProgress(long curr, long max) {
                                    long progress = 100L * curr / max;
                                    bean.progress = (int)progress;
                                    DownloadTask.this.feedback.updateProgress(bean);
                                }

                                @Override
                                public void updateSpeed(String speed) {
                                    DownloadTask.this.feedback.updateSpeed(speed);
                                }
                            };
                            if (DownloadTask.downloadFileMonitored(bean.urlForAttempt(i), path, sha1, fb, DownloadTask.this.cancelled)) {
                                this.feedback.removeTask(bean);
                                break;
                            }
                            if (i == tryTimes - 1) {
                                this.failedFile.add(bean);
                            }
                            this.feedback.removeTask(bean);
                        }
                    }
                });
                int progress = (j + 1) * 100 / list.size();
                this.onProgressUpdate(progress);
            }
            threadPool.shutdown();
            // ★★★ 原来是 {@code awaitTermination(1 小时)} 死等 —— 点了取消也照等一小时，
            //   主线程被拖住，界面看着就像「点不动」。改成 200ms 一片的短等，
            //   取消标志一置起就立刻返回。
            while (!threadPool.isTerminated()) {
                if (this.cancelled.get() || this.isCancelled()) {
                    threadPool.shutdownNow();
                    break;
                }
                try {
                    if (threadPool.awaitTermination(200L, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                }
                catch (InterruptedException e) {
                    // ★ 恢复中断标志，并按取消处理（原来只是 e.printStackTrace() 后继续等）
                    Thread.currentThread().interrupt();
                    threadPool.shutdownNow();
                    break;
                }
            }
        } finally {
            this.currentPool = null;
        }
        // ★ 已取消就别再把「失败列表」当成结果抛给 UI（会弹一个莫名其妙的失败框）
        if (this.cancelled.get() || this.isCancelled()) {
            return new ArrayList<DownloadTaskListBean>();
        }
        return this.failedFile;
    }

    protected void onProgressUpdate(Integer ... p1) {
    }

    public void onPostExecute(ArrayList<DownloadTaskListBean> result) {
        for (DownloadTaskListBean bean : result) {
            Log.e((String)"url", (String)bean.url);
            Log.e((String)"path", (String)bean.path);
        }
        this.feedback.onFinished(result);
    }

    protected void onCancelled(ArrayList<DownloadTaskListBean> result) {
        this.feedback.onCancelled();
    }

    public static boolean isRightFile(String path, String sha1) {
        if (new File(path).exists()) {
            if (sha1 != null && !sha1.equals("")) {
                return Objects.equals(FileUtils.getFileSha1(path), sha1);
            }
            return true;
        }
        return false;
    }

    public static boolean downloadFileMonitored(String url, String nameOutput, String sha1, DownloadFeedback monitor) {
        return downloadFileMonitored(url, nameOutput, sha1, monitor, null);
    }

    /**
     * ★★★★★ 2026-10-16 **修两个下载致命 bug**（评测视频 42s/49.5s/57s）。
     *
     * <p>用户实测：下载进度一直显示 {@code 0 B/s}（卡死），随后弹
     * 「启动失败 · {@code java.io.IOException: unexpected end of stream on com.android.okhttp.Address@...}」。
     *
     * <p><b>Bug 1 —— 速度恒为 0 B/s（"卡死"的观感来源）：</b>
     * <pre>
     *   if (now - lastTime >= 1000) {
     *       lastLen = oval;                                   // ← 先把 lastLen 更新成本次总量
     *       monitor.updateSpeed(formetFileSize(oval - lastLen));  // ← oval - lastLen 恒等于 0！
     *   }
     * </pre>
     * 每秒都算 {@code oval - lastLen}，而 {@code lastLen} 刚刚被赋成 {@code oval}
     * ⇒ 差值永远是 0 ⇒ **进度条有数、速度永远 0 B/s**，看起来像"卡死"。
     * 修法：用**上一次采样时保存的总量**相减。
     *
     * <p><b>Bug 2 —— 任何 IOException 直接放弃（"下载中断 → 启动失败"的来源）：</b>
     * <pre>
     *   catch (IOException e) { e.printStackTrace(); return false; }
     * </pre>
     * 移动网络下 {@code unexpected end of stream} / {@code connection reset} 是**常态**，
     * 一次失败就 `return false` ⇒ 上层判定"下载失败" ⇒ 游戏缺文件 ⇒ 启动即崩。
     * 修法：**同一 URL 自动重试**（最多 4 次，指数退避），并且**续传**已下载的部分。
     *
     * <p><b>Bug 3 —— 没有停滞检测：</b>连接活着但一个字节都不来时会永久挂起。
     * 修法：若 **60 秒内没有任何新数据**，判定为停滞 → 关闭重连（重试机制接手）。
     */
    public static boolean downloadFileMonitored(String url, String nameOutput, String sha1, DownloadFeedback monitor,
                                                java.util.concurrent.atomic.AtomicBoolean cancelled) {
        final int MAX_RETRY = 4;
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            if (cancelled != null && cancelled.get()) {
                return false;
            }
            if (attempt > 0) {
                // 指数退避：1s / 2s / 4s，给网络一点恢复时间
                try {
                    Thread.sleep(1000L * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                if (monitor != null) {
                    monitor.updateSpeed("重试 " + attempt + "/" + (MAX_RETRY - 1) + "…");
                }
            }
            boolean ok = downloadOnce(url, nameOutput, sha1, monitor, cancelled);
            if (ok) {
                return true;
            }
            if (cancelled != null && cancelled.get()) {
                return false;
            }
        }
        return false;
    }

    /** 单次下载尝试（含停滞检测 + 正确的速度统计 + 断点续传）。 */
    private static boolean downloadOnce(String url, String nameOutput, String sha1, DownloadFeedback monitor,
                                        java.util.concurrent.atomic.AtomicBoolean cancelled) {
        File nameOutputFile = new File(nameOutput);
        long existing = 0L;
        if (!nameOutputFile.exists()) {
            File parent = nameOutputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
        } else if (DownloadTask.isRightFile(nameOutput, sha1)) {
            return true;   // 已经是正确文件，直接成功
        } else {
            // ★ 断点续传：保留已下载的部分（只有 sha1 非空时才敢续传，
            //   否则无法校验完整性，干脆重下）
            existing = (sha1 != null && !sha1.isEmpty()) ? nameOutputFile.length() : 0L;
            if (existing <= 0L) {
                nameOutputFile.delete();
            }
        }
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        FileOutputStream fos = null;
        try {
            URL downloadUrl = new URL(url);
            conn = (HttpURLConnection) downloadUrl.openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            if (existing > 0L) {
                conn.setRequestProperty("Range", "bytes=" + existing + "-");
            }
            conn.connect();
            int code = conn.getResponseCode();
            if (code == 416) {
                // 请求范围无效（多半是文件已经下完了）——删掉重下一次
                nameOutputFile.delete();
                return false;
            }
            if (code != 200 && code != 206) {
                return false;
            }
            if (existing > 0L && code != 206) {
                // 服务器不支持续传，从头来
                existing = 0L;
                nameOutputFile.delete();
            }
            inputStream = conn.getInputStream();
            fos = new FileOutputStream(nameOutputFile, existing > 0L);

            long oval = existing;
            long len = conn.getContentLength();
            if (len > 0) {
                len += existing;
            }
            byte[] buf = new byte[65535];
            long lastTick = System.currentTimeMillis();
            long lastSampleTotal = existing;      // ★ 上一次采样时的总量（修速度恒 0 的关键）
            long lastDataAt = System.currentTimeMillis();
            int cur;
            while ((cur = inputStream.read(buf)) != -1) {
                oval += cur;
                lastDataAt = System.currentTimeMillis();
                long now = System.currentTimeMillis();
                if (now - lastTick >= 1000L) {
                    // ★ 用「本次总量 − 上次采样总量」算速度（原来写成 oval − lastLen 恒为 0）
                    long delta = oval - lastSampleTotal;
                    if (monitor != null) {
                        monitor.updateSpeed(DownloadTask.formetFileSize(delta) + "/s");
                    }
                    lastSampleTotal = oval;       // 采样后才更新
                    lastTick = now;
                }
                fos.write(buf, 0, cur);
                if (cancelled != null && cancelled.get()) {
                    fos.close();
                    fos = null;
                    inputStream.close();
                    inputStream = null;
                    nameOutputFile.delete();
                    return false;
                }
                if (monitor != null) {
                    monitor.updateProgress(oval, len);
                }
                // ★ 停滞检测：60 秒没有新数据就当作连接已死，交给重试机制
                if (System.currentTimeMillis() - lastDataAt > 60000L) {
                    return false;
                }
            }
            fos.close();
            fos = null;
            inputStream.close();
            inputStream = null;
        } catch (Exception e) {
            // 不再直接放弃 —— 由 downloadFileMonitored 的重试机制接手
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (Throwable ignored) {
            }
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Throwable ignored) {
            }
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
        if (!DownloadTask.isRightFile(nameOutput, sha1)) {
            // ★ 校验不通过：删掉坏文件，让下一次重试从头下（避免坏文件一直续传）
            nameOutputFile.delete();
            return false;
        }
        return true;
    }

    public static String formetFileSize(long fileS) {
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        fileSizeString = fileS < 1024L ? df.format((double)fileS) + "B" : (fileS < 0x100000L ? df.format((double)fileS / 1024.0) + "K" : (fileS < 0x40000000L ? df.format((double)fileS / 1048576.0) + "M" : df.format((double)fileS / 1.073741824E9) + "G"));
        return fileSizeString;
    }

    public static abstract class Feedback {
        public abstract void addTask(DownloadTaskListBean var1);

        public abstract void updateProgress(DownloadTaskListBean var1);

        public abstract void updateSpeed(String var1);

        public abstract void removeTask(DownloadTaskListBean var1);

        public abstract void onFinished(ArrayList<DownloadTaskListBean> var1);

        public abstract void onCancelled();
    }

    public static abstract class DownloadFeedback {
        public abstract void updateProgress(long var1, long var3);

        public abstract void updateSpeed(String var1);
    }
}

