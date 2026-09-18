package com.qcl.launcher.launcher.game;

import com.qcl.launcher.utils.Lang;
import com.qcl.launcher.utils.function.ExceptionalSupplier;
import com.qcl.launcher.utils.platform.Architecture;
import com.qcl.launcher.utils.platform.OperatingSystem;
import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public final class OSRestriction {
    private final String arch;
    private final OperatingSystem name;
    private final String version;

    public OSRestriction() {
        this(OperatingSystem.UNKNOWN);
    }

    public OSRestriction(OperatingSystem operatingSystem) {
        this(operatingSystem, null);
    }

    public OSRestriction(OperatingSystem operatingSystem, String str) {
        this(operatingSystem, str, null);
    }

    public OSRestriction(OperatingSystem operatingSystem, String str, String str2) {
        this.name = operatingSystem;
        this.version = str;
        this.arch = str2;
    }

    public OperatingSystem getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getArch() {
        return this.arch;
    }

    public boolean allow() {
        if (this.name != OperatingSystem.UNKNOWN && this.name != OperatingSystem.CURRENT_OS) {
            return false;
        }
        if (this.version != null && Lang.test(new ExceptionalSupplier() { // from class: com.qcl.launcher.launcher.game.OSRestriction$$ExternalSyntheticLambda0
            @Override // com.qcl.launcher.utils.function.ExceptionalSupplier
            public final Object get() {
                return OSRestriction.this.m351lambda$allow$0$comqcllauncherlaunchergameOSRestriction();
            }
        })) {
            return false;
        }
        if (this.arch != null) {
            return !Lang.test(new ExceptionalSupplier() { // from class: com.qcl.launcher.launcher.game.OSRestriction$$ExternalSyntheticLambda1
                @Override // com.qcl.launcher.utils.function.ExceptionalSupplier
                public final Object get() {
                    return OSRestriction.this.m352lambda$allow$1$comqcllauncherlaunchergameOSRestriction();
                }
            });
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$allow$0$com-qcl-launcher-launcher-game-OSRestriction, reason: not valid java name */
    public /* synthetic */ Boolean m351lambda$allow$0$comqcllauncherlaunchergameOSRestriction() throws RuntimeException {
        return Boolean.valueOf(!Pattern.compile(this.version).matcher(OperatingSystem.SYSTEM_VERSION).matches());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$allow$1$com-qcl-launcher-launcher-game-OSRestriction, reason: not valid java name */
    public /* synthetic */ Boolean m352lambda$allow$1$comqcllauncherlaunchergameOSRestriction() throws RuntimeException {
        return Boolean.valueOf(!Pattern.compile(this.arch).matcher(Architecture.SYSTEM_ARCH.getCheckedName()).matches());
    }
}
