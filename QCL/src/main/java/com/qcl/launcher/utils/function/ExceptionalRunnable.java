/*
 * Decompiled with CFR 0.152.
 */
package com.qcl.launcher.utils.function;

import java.util.concurrent.Callable;

public interface ExceptionalRunnable<E extends Exception> {
    public void run() throws E;

    default public Callable<Void> toCallable() {
        return () -> {
            this.run();
            return null;
        };
    }
}

