/*
 * Decompiled with CFR 0.152.
 */
package com.qcl.launcher.utils.function;

import java.util.concurrent.Callable;

public interface ExceptionalSupplier<R, E extends Exception> {
    public R get() throws E;

    default public Callable<R> toCallable() {
        return this::get;
    }
}

