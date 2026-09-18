package com.qcl.launcher.utils.gson;

import java.util.HashMap;
import java.util.Map;

/* loaded from: classes2.dex */
public class JsonMap<K, V> extends HashMap<K, V> {
    public JsonMap(int i, float f) {
        super(i, f);
    }

    public JsonMap(int i) {
        super(i);
    }

    public JsonMap() {
    }

    public JsonMap(Map<? extends K, ? extends V> map) {
        super(map);
    }

    @Override // java.util.HashMap, java.util.AbstractMap, java.util.Map
    public V put(K k, V v) {
        if (k == null) {
            return null;
        }
        return (V) super.put(k, v);
    }
}
