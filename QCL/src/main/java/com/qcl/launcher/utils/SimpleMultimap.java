package com.qcl.launcher.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/* loaded from: classes2.dex */
public final class SimpleMultimap<K, V> {
    private final Map<K, Collection<V>> map;
    private final Supplier<Collection<V>> valuer;

    public SimpleMultimap(Supplier<Map<K, Collection<V>>> supplier, Supplier<Collection<V>> supplier2) {
        this.map = supplier.get();
        this.valuer = supplier2;
    }

    public int size() {
        return values().size();
    }

    public Set<K> keys() {
        return this.map.keySet();
    }

    public Collection<V> values() {
        Collection<V> collection = this.valuer.get();
        Iterator<Map.Entry<K, Collection<V>>> it = this.map.entrySet().iterator();
        while (it.hasNext()) {
            collection.addAll(it.next().getValue());
        }
        return collection;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean containsKey(K k) {
        return this.map.containsKey(k) && !this.map.get(k).isEmpty();
    }

    public Collection<V> get(K k) {
        return this.map.computeIfAbsent(k, new Function() { // from class: com.qcl.launcher.utils.SimpleMultimap$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return SimpleMultimap.this.m617lambda$get$0$comqcllauncherutilsSimpleMultimap(obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$get$0$com-qcl-launcher-utils-SimpleMultimap, reason: not valid java name */
    public /* synthetic */ Collection m617lambda$get$0$comqcllauncherutilsSimpleMultimap(Object obj) {
        return this.valuer.get();
    }

    public void put(K k, V v) {
        get(k).add(v);
    }

    public void putAll(K k, Collection<? extends V> collection) {
        get(k).addAll(collection);
    }

    public Collection<V> removeKey(K k) {
        return this.map.remove(k);
    }

    public boolean removeValue(V v) {
        Iterator<Collection<V>> it = this.map.values().iterator();
        boolean z = false;
        while (it.hasNext()) {
            z |= it.next().remove(v);
        }
        return z;
    }

    public boolean removeValue(K k, V v) {
        return get(k).remove(v);
    }

    public void clear() {
        this.map.clear();
    }

    public void clear(K k) {
        if (this.map.containsKey(k)) {
            this.map.get(k).clear();
        } else {
            this.map.put(k, this.valuer.get());
        }
    }
}
