/*
 * Decompiled with CFR 0.152.
 */
package com.qcl.launcher.utils.function;

import java.util.Objects;

public interface ExceptionalBiConsumer<T, U, E extends Exception> {
    public void accept(T var1, U var2) throws E;

    default public ExceptionalBiConsumer<T, U, ?> andThen(ExceptionalBiConsumer<? super T, ? super U, ?> after) {
        Objects.requireNonNull(after);
        return (l, r) -> {
            this.accept(l, r);
            after.accept(l, r);
        };
    }
}

