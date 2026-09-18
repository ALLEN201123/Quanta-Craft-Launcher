package com.qcl.launcher.task;

import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.Logging;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/* loaded from: classes2.dex */
public final class Schedulers {
    private static volatile ExecutorService IO_EXECUTOR;

    private Schedulers() {
    }

    public static ExecutorService io() {
        if (IO_EXECUTOR == null) {
            synchronized (Schedulers.class) {
                if (IO_EXECUTOR == null) {
                    IO_EXECUTOR = Lang.threadPool("IO", true, 4, 10L, TimeUnit.SECONDS);
                }
            }
        }
        return IO_EXECUTOR;
    }

    public static Executor defaultScheduler() {
        return ForkJoinPool.commonPool();
    }

    public static synchronized void shutdown() {
        synchronized (Schedulers.class) {
            Logging.LOG.info("Shutting down executor services.");
            if (IO_EXECUTOR != null) {
                IO_EXECUTOR.shutdownNow();
            }
        }
    }
}
