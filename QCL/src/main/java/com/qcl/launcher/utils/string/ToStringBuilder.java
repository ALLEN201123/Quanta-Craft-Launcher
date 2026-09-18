package com.qcl.launcher.utils.string;

/* loaded from: classes2.dex */
public class ToStringBuilder {
    private boolean first = true;
    private final StringBuilder stringBuilder;

    public ToStringBuilder(Object obj) {
        this.stringBuilder = new StringBuilder(obj.getClass().getSimpleName()).append(" [");
    }

    public ToStringBuilder append(String str, Object obj) {
        if (!this.first) {
            this.stringBuilder.append(", ");
        }
        this.first = false;
        this.stringBuilder.append(str).append('=').append(obj);
        return this;
    }

    public String toString() {
        return this.stringBuilder.toString() + "]";
    }
}
