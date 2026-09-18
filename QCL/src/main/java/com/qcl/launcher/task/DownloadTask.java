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
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(this.maxTask, this.maxTask, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
        for (int j = 0; j < list.size(); ++j) {
            final DownloadTaskListBean bean = list.get(j);
            String path = bean.path;
            String sha1 = bean.sha1;
            threadPool.execute(() -> {
                if (!new File(path).exists() || new File(path).exists() && !Objects.equals(FileUtils.getFileSha1(path), sha1)) {
                    int tryTimes = 5;
                    for (int i = 0; i < tryTimes; ++i) {
                        if (this.isCancelled()) {
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
                        if (DownloadTask.downloadFileMonitored(bean.urlForAttempt(i), path, sha1, fb)) {
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
        try {
            threadPool.awaitTermination(1L, TimeUnit.HOURS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
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
        File nameOutputFile = new File(nameOutput);
        if (!nameOutputFile.exists()) {
            nameOutputFile.getParentFile().mkdirs();
        } else if (!DownloadTask.isRightFile(nameOutput, sha1)) {
            nameOutputFile.delete();
        }
        try {
            URL downloadUrl = new URL(url);
            HttpURLConnection httpURLConnection = (HttpURLConnection)downloadUrl.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setConnectTimeout(8000);
            httpURLConnection.setReadTimeout(15000);
            httpURLConnection.setInstanceFollowRedirects(true);
            httpURLConnection.connect();
            InputStream inputStream = httpURLConnection.getInputStream();
            FileOutputStream fos = new FileOutputStream(nameOutputFile);
            int cur = 0;
            int oval = 0;
            long len = httpURLConnection.getContentLength();
            byte[] buf = new byte[65535];
            long lastTime = System.currentTimeMillis();
            long lastLen = 0L;
            while ((cur = inputStream.read(buf)) != -1) {
                oval += cur;
                if (System.currentTimeMillis() - lastTime >= 1000L) {
                    lastLen = oval;
                    lastTime = System.currentTimeMillis();
                    if (monitor != null) {
                        monitor.updateSpeed(DownloadTask.formetFileSize((long)oval - lastLen) + "/s");
                    }
                }
                fos.write(buf, 0, cur);
                if (monitor == null) continue;
                monitor.updateProgress(oval, len);
            }
            fos.close();
            inputStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!DownloadTask.isRightFile(nameOutput, sha1)) {
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

