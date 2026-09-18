package com.qcl.launcher.task;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/* loaded from: classes2.dex */
public interface TaskCompletableFuture {
    CompletableFuture<?> all(Collection<Task<?>> collection);

    <T> CompletableFuture<T> one(Task<T> task);
}
