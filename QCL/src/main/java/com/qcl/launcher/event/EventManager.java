package com.qcl.launcher.event;

import com.qcl.launcher.event.Event;
import com.qcl.launcher.utils.SimpleMultimap;
import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/* loaded from: classes2.dex */
public final class EventManager<T extends Event> {
    private final SimpleMultimap<EventPriority, Consumer<T>> handlers = new SimpleMultimap<>(new Supplier() { // from class: com.qcl.launcher.event.EventManager$$ExternalSyntheticLambda3
        @Override // java.util.function.Supplier
        public final Object get() {
            return EventManager.lambda$new$0();
        }
    }, new Supplier() { // from class: com.qcl.launcher.event.EventManager$$ExternalSyntheticLambda2
        @Override // java.util.function.Supplier
        public final Object get() {
            return EventManager.m205$r8$lambda$sMWsn4MZQR24qK4Yd75_okqFB0();
        }
    });

    /* renamed from: $r8$lambda$sMWsn4MZQR24qK4Yd75_o-kqFB0, reason: not valid java name */
    public static /* synthetic */ CopyOnWriteArraySet m205$r8$lambda$sMWsn4MZQR24qK4Yd75_okqFB0() {
        return new CopyOnWriteArraySet();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ Map lambda$new$0() {
        return new EnumMap(EventPriority.class);
    }

    public Consumer<T> registerWeak(Consumer<T> consumer) {
        register(new WeakListener(consumer));
        return consumer;
    }

    public Consumer<T> registerWeak(Consumer<T> consumer, EventPriority eventPriority) {
        register(new WeakListener(consumer), eventPriority);
        return consumer;
    }

    public void register(Consumer<T> consumer) {
        register(consumer, EventPriority.NORMAL);
    }

    public synchronized void register(Consumer<T> consumer, EventPriority eventPriority) {
        if (!this.handlers.get(eventPriority).contains(consumer)) {
            this.handlers.put(eventPriority, consumer);
        }
    }

    public void register(final Runnable runnable) {
        register(new Consumer() { // from class: com.qcl.launcher.event.EventManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                runnable.run();
            }
        });
    }

    public void register(final Runnable runnable, EventPriority eventPriority) {
        register(new Consumer() { // from class: com.qcl.launcher.event.EventManager$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                runnable.run();
            }
        }, eventPriority);
    }

    public synchronized Event.Result fireEvent(T t) {
        for (EventPriority eventPriority : EventPriority.values()) {
            Iterator<Consumer<T>> it = this.handlers.get(eventPriority).iterator();
            while (it.hasNext()) {
                it.next().accept(t);
            }
        }
        if (t.hasResult()) {
            return t.getResult();
        }
        return Event.Result.DEFAULT;
    }

    public synchronized void unregister(Consumer<T> consumer) {
        this.handlers.removeValue(consumer);
    }

    /* loaded from: classes2.dex */
    private class WeakListener implements Consumer<T> {
        private final WeakReference<Consumer<T>> ref;

        public WeakListener(Consumer<T> consumer) {
            this.ref = new WeakReference<>(consumer);
        }

        @Override // java.util.function.Consumer
        public void accept(T t) {
            Consumer<T> consumer = this.ref.get();
            if (consumer == null) {
                EventManager.this.unregister(this);
            } else {
                consumer.accept(t);
            }
        }
    }
}
