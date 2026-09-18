package com.qcl.launcher.utils.function;

import java.lang.Exception;

/* loaded from: classes2.dex */
public interface ExceptionalFunction<T, R, E extends Exception> {
    static /* synthetic */ Object lambda$identity$0(Object obj) throws RuntimeException {
        return obj;
    }

    R apply(T t) throws Exception;

    static <T, E extends RuntimeException> ExceptionalFunction<T, T, E> identity() {
        return new ExceptionalFunction() { // from class: com.qcl.launcher.utils.function.ExceptionalFunction$$ExternalSyntheticLambda0
            @Override // com.qcl.launcher.utils.function.ExceptionalFunction
            public final Object apply(Object obj) {
                return ExceptionalFunction.lambda$identity$0(obj);
            }
        };
    }
}
