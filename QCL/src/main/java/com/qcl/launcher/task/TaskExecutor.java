package com.qcl.launcher.task;

import com.qcl.launcher.task.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes2.dex */
public abstract class TaskExecutor {
    protected Exception exception;
    protected final Task<?> firstTask;
    private final List<String> stages;
    protected final List<TaskListener> taskListeners = new ArrayList();
    protected final AtomicInteger totTask = new AtomicInteger(0);
    protected final AtomicBoolean cancelled = new AtomicBoolean(false);

    public abstract void cancel();

    public abstract TaskExecutor start();

    public abstract boolean test();

    public TaskExecutor(Task<?> task) {
        this.firstTask = task;
        this.stages = task instanceof Task.StagesHintTask ? ((Task.StagesHintTask) task).getStages() : Collections.emptyList();
    }

    public void addTaskListener(TaskListener taskListener) {
        this.taskListeners.add(taskListener);
    }

    public Exception getException() {
        return this.exception;
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }

    public int getTaskCount() {
        return this.totTask.get();
    }

    public List<String> getStages() {
        return this.stages;
    }
}
