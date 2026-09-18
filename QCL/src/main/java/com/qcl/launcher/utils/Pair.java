package com.qcl.launcher.utils;

import java.util.Map;
import java.util.Objects;

/* loaded from: classes2.dex */
public final class Pair<K, V> implements Map.Entry<K, V> {
    private K key;
    private V value;

    public static <K, V> Pair<K, V> pair(K k, V v) {
        return new Pair<>(k, v);
    }

    private Pair(K k, V v) {
        this.key = k;
        this.value = v;
    }

    @Override // java.util.Map.Entry
    public K getKey() {
        return this.key;
    }

    public void setKey(K k) {
        this.key = k;
    }

    @Override // java.util.Map.Entry
    public V getValue() {
        return this.value;
    }

    @Override // java.util.Map.Entry
    public V setValue(V v) {
        V v2 = this.value;
        this.value = v;
        return v2;
    }

    @Override // java.util.Map.Entry
    public int hashCode() {
        return ((161 + Objects.hashCode(this.key)) * 23) + Objects.hashCode(this.value);
    }

    @Override // java.util.Map.Entry
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Pair pair = (Pair) obj;
        return Objects.equals(this.key, pair.key) && Objects.equals(this.value, pair.value);
    }

    public String toString() {
        return "(" + this.key + ", " + this.value + ")";
    }
}
