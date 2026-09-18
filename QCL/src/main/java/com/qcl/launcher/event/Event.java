package com.qcl.launcher.event;

import com.qcl.launcher.utils.string.ToStringBuilder;
import java.util.Objects;

/* loaded from: classes2.dex */
public class Event {
    private boolean canceled;
    private Result result = Result.DEFAULT;
    protected final transient Object source;

    /* loaded from: classes2.dex */
    public enum Result {
        DENY,
        DEFAULT,
        ALLOW
    }

    public boolean hasResult() {
        return false;
    }

    public boolean isCancelable() {
        return false;
    }

    public Event(Object obj) {
        Objects.requireNonNull(obj);
        this.source = obj;
    }

    public Object getSource() {
        return this.source;
    }

    public String toString() {
        return new ToStringBuilder(this).append("source", this.source).toString();
    }

    public final boolean isCanceled() {
        return this.canceled;
    }

    public final void setCanceled(boolean z) {
        if (!isCancelable()) {
            throw new UnsupportedOperationException("Attempted to cancel a non-cancelable event: " + getClass());
        }
        this.canceled = z;
    }

    public Result getResult() {
        return this.result;
    }

    public void setResult(Result result) {
        if (!hasResult()) {
            throw new UnsupportedOperationException("Attempted to set result on a no result event: " + getClass() + " of type.");
        }
        this.result = result;
    }
}
