package com.qcl.launcher.launcher;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import com.github.gzuliyujiang.oaid.DeviceIdentifier;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* loaded from: classes2.dex */
public class QCLApplication extends Application {
    private static final String SHARED_CRASH_DIR = "QCL";
    private static final String TAG = "QCLCrash";
    private static Context context;

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        installCrashLogger();
        DeviceIdentifier.register(this);
        context = getApplicationContext();
        // ★★★ 1.1.1 SDL3：把 C++ 库提前到主线程加载，避免在游戏渲染线程 dlopen 时触发
        // libc++ 的 iostream/locale 静态初始化崩溃（fault addr 0x0）。
        try { System.loadLibrary("bytehook"); } catch (Throwable ignored) { }
        try { System.loadLibrary("SDL3"); } catch (Throwable ignored) { }
    }

    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: com.qcl.launcher.launcher.QCLApplication$$ExternalSyntheticLambda0
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public final void uncaughtException(Thread thread, Throwable th) {
                QCLApplication.this.m211x63dcaab4(defaultUncaughtExceptionHandler, thread, th);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$installCrashLogger$0$com-qcl-launcher-launcher-QCLApplication, reason: not valid java name */
    public /* synthetic */ void m211x63dcaab4(Thread.UncaughtExceptionHandler uncaughtExceptionHandler, Thread thread, Throwable th) {
        // ★★★ 1.3.0：Finalizer 线程关 ZipFile 时偶发「close failed: EIO」——
        //   本质是 zip 对应的文件被删/覆盖后，GC 才回收 fd 去 close，属无害的清理噪音。
        //   以前会当成真崩溃弹崩溃页并杀进程。现在直接吞掉，不弹窗、不杀进程。
        if (isBenignZipClose(thread, th)) {
            try {
                Log.w("QCLCrash", "忽略无害的 ZipFile 清理异常（" + thread.getName() + "）", th);
            } catch (Throwable ignored) {
            }
            return;
        }
        try {
            saveCrashLog(thread, th);
        } catch (Throwable unused) {
        }
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            printWriter.println("time   : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            printWriter.println("thread : " + thread.getName());
            printWriter.println("android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
            printWriter.println("device : " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.CPU_ABI);
            printWriter.println("package: " + getPackageName());
            printWriter.println("--------------------------------------------------");
            th.printStackTrace(printWriter);
            printWriter.flush();
            Intent intent = new Intent(this, (Class<?>) CrashReportActivity.class);
            intent.addFlags(335544320);
            intent.putExtra("crash_text", stringWriter.toString());
            intent.putExtra("crash_summary", th.getClass().getName() + ": " + String.valueOf(th.getMessage()));
            startActivity(intent);
            Process.killProcess(Process.myPid());
            System.exit(10);
        } catch (Throwable unused2) {
            if (uncaughtExceptionHandler != null) {
                uncaughtExceptionHandler.uncaughtException(thread, th);
            }
        }
    }

    /** ★ 1.3.0：判断是不是「守护线程上关 ZipFile 的 EIO」这种无害异常 */
    private static boolean isBenignZipClose(Thread thread, Throwable th) {
        if (thread == null || !thread.isDaemon()) {
            return false;
        }
        for (Throwable x = th; x != null; x = x.getCause()) {
            String msg = String.valueOf(x.getMessage());
            if (msg != null && msg.contains("close failed")) {
                return true;
            }
        }
        return false;
    }

    private void saveCrashLog(Thread thread, Throwable th) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println("time   : " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        printWriter.println("thread : " + thread.getName());
        printWriter.println("android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        printWriter.println("device : " + Build.MANUFACTURER + " " + Build.MODEL + " " + Build.CPU_ABI);
        printWriter.println("package: " + getPackageName());
        printWriter.println("--------------------------------------------------");
        th.printStackTrace(printWriter);
        printWriter.flush();
        String stringWriter2 = stringWriter.toString();
        Log.e("QCLCrash", stringWriter2);
        writeTo(new File(getExternalFilesDir(null), "crash.log"), stringWriter2);
        writeTo(new File(Environment.getExternalStorageDirectory(), "QCL/crash.log"), stringWriter2);
    }

    private void writeTo(File file, String str) {
        try {
            File parentFile = file.getParentFile();
            if (parentFile != null && !parentFile.exists()) {
                parentFile.mkdirs();
            }
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8");
            try {
                outputStreamWriter.write(str);
                outputStreamWriter.write("\n\n");
                outputStreamWriter.close();
            } finally {
            }
        } catch (Throwable unused) {
        }
    }

    public static Context getContext() {
        return context;
    }

    public static void releaseContext() {
        context = null;
    }
}
