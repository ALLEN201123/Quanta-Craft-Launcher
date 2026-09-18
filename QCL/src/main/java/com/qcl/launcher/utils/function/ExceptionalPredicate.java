package com.qcl.launcher.utils.function;

import java.lang.Exception;

/* loaded from: classes2.dex */
public interface ExceptionalPredicate<T, E extends Exception> {
    boolean test(T t) throws Exception;
}
