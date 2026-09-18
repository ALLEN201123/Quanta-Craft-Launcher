package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.RemoteMod;
import java.util.function.Function;

/* compiled from: D8$$SyntheticClass */
/* loaded from: classes2.dex */
public final /* synthetic */ class LocalModFile$$ExternalSyntheticLambda0 implements Function {
    public static final /* synthetic */ LocalModFile$$ExternalSyntheticLambda0 INSTANCE = new LocalModFile$$ExternalSyntheticLambda0();

    private /* synthetic */ LocalModFile$$ExternalSyntheticLambda0() {
    }

    @Override // java.util.function.Function
    public final Object apply(Object obj) {
        return ((RemoteMod.Version) obj).getDatePublished();
    }
}
