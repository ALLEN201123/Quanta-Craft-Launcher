package com.tungsten.hmclpe.launcher;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.github.gzuliyujiang.oaid.DeviceIdentifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HMCLPEApplication extends Application {

    private static final String TAG = "QCLCrash";
    /** Kept next to the launcher's own folder so a file manager can pick it up easily. */
    private static final String SHARED_CRASH_DIR = "QCL";

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        installCrashLogger();
        DeviceIdentifier.register(this);
        context = this.getApplicationContext();
    }

    /**
     * Writes any uncaught exception to disk instead of losing it, so a crash on the user's device
     * can be diagnosed without a debugger attached.
     */
    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                saveCrashLog(thread, error);
            } catch (Throwable ignored) {
                // never let the logger make things worse
            }
            // 不再直接交给系统（那会直接回主界面/被杀）：改为弹出 QCL 崩溃界面，
            // 左边完整日志、右边错误摘要，右下角「退出 / 返回启动器」。
            try {
                java.io.StringWriter buffer = new java.io.StringWriter();
                java.io.PrintWriter writer = new java.io.PrintWriter(buffer);
                writer.println("time   : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                writer.println("thread : " + thread.getName());
                writer.println("android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
                writer.println("device : " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.CPU_ABI);
                writer.println("package: " + getPackageName());
                writer.println("--------------------------------------------------");
                error.printStackTrace(writer);
                writer.flush();
                android.content.Intent intent = new android.content.Intent(HMCLPEApplication.this,
                        com.tungsten.hmclpe.launcher.CrashReportActivity.class);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(com.tungsten.hmclpe.launcher.CrashReportActivity.EXTRA_CRASH_TEXT, buffer.toString());
                intent.putExtra(com.tungsten.hmclpe.launcher.CrashReportActivity.EXTRA_SUMMARY,
                        error.getClass().getName() + ": " + String.valueOf(error.getMessage()));
                startActivity(intent);
            } catch (Throwable t) {
                if (previous != null) previous.uncaughtException(thread, error);
            }
        });
    }

    private void saveCrashLog(Thread thread, Throwable error) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        writer.println("time   : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        writer.println("thread : " + thread.getName());
        writer.println("android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        writer.println("device : " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.CPU_ABI);
        writer.println("package: " + getPackageName());
        writer.println("--------------------------------------------------");
        error.printStackTrace(writer);
        writer.flush();
        String text = buffer.toString();

        Log.e(TAG, text);

        writeTo(new File(getExternalFilesDir(null), "crash.log"), text);
        writeTo(new File(Environment.getExternalStorageDirectory(), SHARED_CRASH_DIR + "/crash.log"), text);
    }

    private void writeTo(File target, String text) {
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            try (Writer out = new OutputStreamWriter(new FileOutputStream(target, true), "UTF-8")) {
                out.write(text);
                out.write("\n\n");
            }
        } catch (Throwable ignored) {
            // the app-scoped folder can fail too on locked-down devices; logcat already has it
        }
    }

    public static Context getContext(){
        return context;
    }

    public static void releaseContext(){
        context = null;
    }

}
