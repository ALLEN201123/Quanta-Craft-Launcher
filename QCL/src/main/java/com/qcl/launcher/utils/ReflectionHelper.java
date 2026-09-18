package com.qcl.launcher.utils;

import com.qcl.launcher.utils.string.StringUtils;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/* loaded from: classes2.dex */
public final class ReflectionHelper {
    private static Method accessible0;

    private ReflectionHelper() {
    }

    static {
        try {
            Method declaredMethod = AccessibleObject.class.getDeclaredMethod("setAccessible0", Boolean.TYPE);
            accessible0 = declaredMethod;
            declaredMethod.setAccessible(true);
        } catch (Throwable unused) {
        }
    }

    public static void setAccessible(AccessibleObject accessibleObject) throws InvocationTargetException, IllegalAccessException {
        accessible0.invoke(accessibleObject, true);
    }

    public static StackTraceElement getCaller(Predicate<String> predicate) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement stackTraceElement = stackTrace[2];
        for (int i = 3; i < stackTrace.length; i++) {
            if (predicate.test(StringUtils.substringBeforeLast(stackTrace[i].getClassName(), '.')) && !stackTraceElement.getClassName().equals(stackTrace[i].getClassName())) {
                return stackTrace[i];
            }
        }
        return stackTraceElement;
    }
}
