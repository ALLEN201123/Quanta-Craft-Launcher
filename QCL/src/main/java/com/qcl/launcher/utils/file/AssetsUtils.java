package com.qcl.launcher.utils.file;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetsUtils {

    private static AssetsUtils instance;
    private static final int SUCCESS = 1;
    private static final int FAILED = 0;
    private Context context;
    private FileOperateCallback callback;
    private ProgressCallback progressCallback;
    private volatile boolean isSuccess;
    private String errorStr;

    public static AssetsUtils getInstance(Context context) {
        if (instance == null)
            instance = new AssetsUtils(context);
        return instance;
    }

    private AssetsUtils(Context context) {
        this.context = context;
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (callback != null) {
                if (msg.what == SUCCESS) {
                    callback.onSuccess();
                }
                if (msg.what == FAILED) {
                    callback.onFailed(msg.obj.toString());
                }
            }
        }
    };

    public static String readAssetsTxt(Context context,String fileName){
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String text = new String(buffer, "utf-8");
            Log.e("latest",text);
            return text;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public AssetsUtils copyAssetsToSD(final String srcPath, final String sdPath) {
        currentPosition = 0;
        try {
            totalSize = getTotalSize(context,srcPath);
            Log.e("assetsFileSize",Long.toString(totalSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            copyAssetsToDst(context, srcPath, sdPath);
            if (isSuccess)
                handler.obtainMessage(SUCCESS).sendToTarget();
            else
                handler.obtainMessage(FAILED, errorStr).sendToTarget();
        }).start();
        return this;
    }

    public AssetsUtils copyOnMainThread(final String srcPath, final String sdPath) {
        currentPosition = 0;
        try {
            totalSize = getTotalSize(context,srcPath);
            Log.e("assetsFileSize",Long.toString(totalSize));
        } catch (IOException e) {
            e.printStackTrace();
        }
        copyAssetsToDst(context, srcPath, sdPath);
        if (isSuccess) {
            handler.obtainMessage(SUCCESS).sendToTarget();
        }
        else {
            handler.obtainMessage(FAILED, errorStr).sendToTarget();
        }
        return this;
    }

    /** Synchronous, fail-fast copy for runtime installation; never copies completion markers. */
    public void copyRuntimeOrThrow(String srcPath, String dstPath) throws IOException {
        currentPosition = 0;
        currentProgress = -1;
        totalSize = getTotalSize(context, srcPath);
        copyRuntimeTree(srcPath, new File(dstPath));
    }

    private void copyRuntimeTree(String source, File target) throws IOException {
        String[] children = context.getAssets().list(source);
        if (children == null) throw new IOException("Cannot list asset: " + source);
        if (children.length > 0) {
            if (!target.isDirectory() && !target.mkdirs()) throw new IOException("Cannot create: " + target);
            for (String child : children) {
                if (!"version".equals(child)) copyRuntimeTree(source + "/" + child, new File(target, child));
            }
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Cannot create: " + parent);
        try (InputStream input = context.getAssets().open(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] bytes = new byte[65536];
            int count;
            while ((count = input.read(bytes)) != -1) {
                output.write(bytes, 0, count);
                currentPosition += count;
                int progress = totalSize == 0 ? 100 : (int) Math.min(100, currentPosition * 100 / totalSize);
                if (progress != currentProgress) {
                    currentProgress = progress;
                    ProgressCallback listener = progressCallback;
                    if (listener != null) handler.post(() -> listener.onProgress(progress));
                }
            }
            output.getFD().sync();
        }
    }

    public AssetsUtils setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
        return instance;
    }

    public void setFileOperateCallback(FileOperateCallback callback) {
        this.callback = callback;
    }

    long currentPosition = 0;
    long totalSize = 0;

    int currentProgress = 0;

    private void copyAssetsToDst(Context context, String srcPath, String dstPath) {
        try {
            String fileNames[] = context.getAssets().list(srcPath);
            if (fileNames.length > 0) {
                File file = new File(dstPath);
                if (!file.exists()) file.mkdirs();
                for (String fileName : fileNames) {
                    if (!srcPath.equals("")) { // assets 文件夹下的目录
                        copyAssetsToDst(context, srcPath + File.separator + fileName, dstPath + File.separator + fileName);
                    } else { // assets 文件夹
                        copyAssetsToDst(context, fileName, dstPath + File.separator + fileName);
                    }
                }
            } else {
                File outFile = new File(dstPath);
                InputStream is = context.getAssets().open(srcPath);
                FileOutputStream fos = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    currentPosition += byteCount;
                    fos.write(buffer, 0, byteCount);
                    if (progressCallback != null) {
                        long cur = 100L * currentPosition;
                        int progress = (int) (cur / totalSize);
                        if (progress != currentProgress) {
                            currentProgress = progress;
                            handler.post(() -> {
                                progressCallback.onProgress(progress);
                            });
                        }
                    }
                }
                fos.flush();
                is.close();
                fos.close();
            }
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            errorStr = e.getMessage();
            isSuccess = false;
        }
    }

    private long getTotalSize(Context context,String srcPath) throws IOException {
        String fileNames[] = context.getAssets().list(srcPath);
        long size = 0;
        if (fileNames.length > 0) {
            for (String fileName : fileNames) {
                if (!srcPath.equals("")) { // assets 文件夹下的目录
                    size += getTotalSize(context, srcPath + File.separator + fileName);
                } else { // assets 文件夹
                    size += getTotalSize(context, fileName);
                }
            }
        } else {
            try (InputStream is = context.getAssets().open(srcPath)) {
                size += is.available();
            }
        }
        return size;
    }

    public interface FileOperateCallback {
        void onSuccess();
        void onFailed(String error);
    }

    public interface ProgressCallback{
        void onProgress(int progress);
    }

}
