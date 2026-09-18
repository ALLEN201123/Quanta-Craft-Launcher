package com.qcl.launcher.launcher.game;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/* loaded from: classes2.dex */
public final class CompatibilityRule {
    private final Action action;
    private final Map<String, Boolean> features;
    private final OSRestriction os;

    /* loaded from: classes2.dex */
    public enum Action {
        ALLOW,
        DISALLOW
    }

    public CompatibilityRule() {
        this(Action.ALLOW, null);
    }

    public CompatibilityRule(Action action, OSRestriction oSRestriction) {
        this(action, oSRestriction, null);
    }

    public CompatibilityRule(Action action, OSRestriction oSRestriction, Map<String, Boolean> map) {
        this.action = action;
        this.os = oSRestriction;
        this.features = map;
    }

    public Optional<Action> getAppliedAction(Map<String, Boolean> map) {
        OSRestriction oSRestriction = this.os;
        if (oSRestriction != null && !oSRestriction.allow()) {
            return Optional.empty();
        }
        Map<String, Boolean> map2 = this.features;
        if (map2 != null) {
            for (Map.Entry<String, Boolean> entry : map2.entrySet()) {
                if (!Objects.equals(map.get(entry.getKey()), entry.getValue())) {
                    return Optional.empty();
                }
            }
        }
        return Optional.ofNullable(this.action);
    }

    public static boolean appliesToCurrentEnvironment(Collection<CompatibilityRule> collection) {
        return appliesToCurrentEnvironment(collection, Collections.emptyMap());
    }

    public static boolean appliesToCurrentEnvironment(Collection<CompatibilityRule> collection, Map<String, Boolean> map) {
        if (collection == null || collection.isEmpty()) {
            return true;
        }
        Action action = Action.DISALLOW;
        Iterator<CompatibilityRule> it = collection.iterator();
        while (it.hasNext()) {
            Optional<Action> appliedAction = it.next().getAppliedAction(map);
            if (appliedAction.isPresent()) {
                action = appliedAction.get();
            }
        }
        return action == Action.ALLOW;
    }

    public static boolean equals(Collection<CompatibilityRule> collection, Collection<CompatibilityRule> collection2) {
        return Objects.hashCode(collection) == Objects.hashCode(collection2);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CompatibilityRule compatibilityRule = (CompatibilityRule) obj;
        return this.action == compatibilityRule.action && Objects.equals(this.os, compatibilityRule.os) && Objects.equals(this.features, compatibilityRule.features);
    }

    public int hashCode() {
        return Objects.hash(this.action, this.os, this.features);
    }
}
