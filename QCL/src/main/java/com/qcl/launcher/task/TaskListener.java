package com.qcl.launcher.task;

import java.util.EventListener;

/* loaded from: classes2.dex */
public abstract class TaskListener implements EventListener {
    public void onFinished(Task<?> task) {
    }

    public void onPropertiesUpdate(Task<?> task) {
    }

    public void onReady(Task<?> task) {
    }

    public void onRunning(Task<?> task) {
    }

    public void onStart() {
    }

    public void onStop(boolean z, TaskExecutor taskExecutor) {
    }

    public void onFailed(Task<?> task, Throwable th) {
        onFinished(task);
    }
}
