package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.utils.string.ModTranslations;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public abstract class LocalizedRemoteModRepository implements RemoteModRepository {
    protected abstract RemoteModRepository getBackedRemoteModRepository();

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod> search(String str, RemoteModRepository.Category category, int i, int i2, String str2, RemoteModRepository.SortType sortType, RemoteModRepository.SortOrder sortOrder) throws IOException {
        if (StringUtils.CHINESE_PATTERN.matcher(str2).find()) {
            List<ModTranslations.Mod> searchMod = ModTranslations.getTranslationsByRepositoryType(getType()).searchMod(str2);
            ArrayList arrayList = new ArrayList();
            int i3 = 0;
            for (ModTranslations.Mod mod : searchMod) {
                String name = mod.getName();
                if (StringUtils.isNotBlank(mod.getSubname())) {
                    name = mod.getSubname();
                }
                arrayList.add(name);
                i3++;
                if (i3 >= 3) {
                    break;
                }
            }
            str2 = String.join(" ", arrayList);
        }
        return getBackedRemoteModRepository().search(str, category, i, i2, str2, sortType, sortOrder);
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteModRepository.Category> getCategories() throws IOException {
        return getBackedRemoteModRepository().getCategories();
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Optional<RemoteMod.Version> getRemoteVersionByLocalFile(LocalModFile localModFile, Path path) throws IOException {
        return getBackedRemoteModRepository().getRemoteVersionByLocalFile(localModFile, path);
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteMod getModById(String str) throws IOException {
        return getBackedRemoteModRepository().getModById(str);
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public RemoteMod.File getModFile(String str, String str2) throws IOException {
        return getBackedRemoteModRepository().getModFile(str, str2);
    }

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod.Version> getRemoteVersionsById(String str) throws IOException {
        return getBackedRemoteModRepository().getRemoteVersionsById(str);
    }
}
