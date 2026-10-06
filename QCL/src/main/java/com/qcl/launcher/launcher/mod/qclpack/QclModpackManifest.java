package com.qcl.launcher.launcher.mod.qclpack;

import com.qcl.launcher.launcher.mod.ModpackManifest;
import com.qcl.launcher.launcher.mod.ModpackProvider;

/**
 * 整合包 manifest 占位实现（该格式的元数据都在 zip 内的 json 里，不需要额外清单）。
 *
 * <p>★ 2026-10-06 改名：原 {@code HMCLModpackManifest} → {@code QclModpackManifest}。
 */
public final class QclModpackManifest implements ModpackManifest {
    public static final QclModpackManifest INSTANCE = new QclModpackManifest();

    private QclModpackManifest() {
    }

    @Override // com.qcl.launcher.launcher.mod.ModpackManifest
    public ModpackProvider getProvider() {
        return QclModpackProvider.INSTANCE;
    }
}
