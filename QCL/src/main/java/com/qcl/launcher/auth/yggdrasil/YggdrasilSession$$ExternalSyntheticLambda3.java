package com.qcl.launcher.auth.yggdrasil;

import java.util.Map;
import java.util.function.Function;

/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class YggdrasilSession$$ExternalSyntheticLambda3 implements Function {
    public static final /* synthetic */ YggdrasilSession$$ExternalSyntheticLambda3 INSTANCE = new YggdrasilSession$$ExternalSyntheticLambda3();

    private /* synthetic */ YggdrasilSession$$ExternalSyntheticLambda3() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return (String) ((Map.Entry) obj).getKey();
    }
}
