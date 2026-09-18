package com.qcl.launcher.launcher.game;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public final class ExtractRules {
    public static final ExtractRules EMPTY = new ExtractRules();
    private final List<String> exclude;

    public ExtractRules() {
        this.exclude = Collections.emptyList();
    }

    public ExtractRules(List<String> list) {
        this.exclude = new LinkedList(list);
    }

    public List<String> getExclude() {
        return Collections.unmodifiableList(this.exclude);
    }

    public boolean shouldExtract(final String str) {
        Stream<String> stream = this.exclude.stream();
        Objects.requireNonNull(str);
        return stream.noneMatch(new Predicate() { // from class: com.qcl.launcher.launcher.game.ExtractRules$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean startsWith;
                startsWith = str.startsWith((String) obj);
                return startsWith;
            }
        });
    }
}
