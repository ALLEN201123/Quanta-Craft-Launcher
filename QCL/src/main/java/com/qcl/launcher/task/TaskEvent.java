package com.qcl.launcher.task;

import com.qcl.launcher.event.Event;

/* loaded from: classes2.dex */
public class TaskEvent extends Event {
    private final boolean failed;
    private final Task<?> task;

    public TaskEvent(Object obj, Task<?> task, boolean z) {
        super(obj);
        this.task = task;
        this.failed = z;
    }

    public Task<?> getTask() {
        return this.task;
    }

    public boolean isFailed() {
        return this.failed;
    }
}
