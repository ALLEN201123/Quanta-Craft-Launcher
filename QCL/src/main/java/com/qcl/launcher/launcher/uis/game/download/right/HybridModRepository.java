package com.qcl.launcher.launcher.uis.game.download.right;

import com.qcl.launcher.launcher.mod.HybridRemoteModRepository;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.launcher.mod.curse.CurseForgeRemoteModRepository;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthRemoteModRepository;

/**
 * ★ 1.5.0 用户要求：「整合包和搜索也完善一下」——<b>混合搜索</b>。
 *
 * <p>下拉里多出第三项「CurseForge + Modrinth」时用它拿到现成的混合仓库实例。
 * 真正干活的是 {@link HybridRemoteModRepository}（并发查两站 + 合并去重 + 详情页按来源分派）。
 *
 * <p>★ 故意做成<b>静态单例</b>：混合仓库内部有线程池与去重逻辑，
 *   每次切下拉都 new 一个等于反复起线程，必须复用。
 */
final class HybridModRepository {

    private HybridModRepository() {
    }

    /** 模组混合仓库（CF MODS + Modrinth MODS）。 */
    static final HybridRemoteModRepository MODS =
            new HybridRemoteModRepository(
                    CurseForgeRemoteModRepository.MODS,
                    ModrinthRemoteModRepository.MODS,
                    RemoteModRepository.Type.MOD);

    /** 整合包混合仓库（CF MODPACKS + Modrinth MODPACKS）。 */
    static final HybridRemoteModRepository MODPACKS =
            new HybridRemoteModRepository(
                    CurseForgeRemoteModRepository.MODPACKS,
                    ModrinthRemoteModRepository.MODPACKS,
                    RemoteModRepository.Type.MODPACK);

    /** 资源包混合仓库（CF RESOURCE_PACKS + Modrinth RESOURCE_PACKS）。 */
    static final HybridRemoteModRepository RESOURCE_PACKS =
            new HybridRemoteModRepository(
                    CurseForgeRemoteModRepository.RESOURCE_PACKS,
                    ModrinthRemoteModRepository.RESOURCE_PACKS,
                    RemoteModRepository.Type.RESOURCE_PACK);

    /** 光影混合仓库（只有 Modrinth 有 shader；CF 侧传 WORLDS 不合适 ⇒ 传空实现）。 */
    static final HybridRemoteModRepository SHADERS =
            new HybridRemoteModRepository(
                    CurseForgeRemoteModRepository.CUSTOMIZATIONS,
                    ModrinthRemoteModRepository.SHADERS,
                    RemoteModRepository.Type.CUSTOMIZATION);
}
