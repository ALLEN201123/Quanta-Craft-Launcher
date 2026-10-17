package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.launcher.mod.RemoteModRepository;
import com.qcl.launcher.utils.string.ModTranslations;
import com.qcl.launcher.utils.string.StringUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/* loaded from: classes2.dex */
public abstract class LocalizedRemoteModRepository implements RemoteModRepository {
    protected abstract RemoteModRepository getBackedRemoteModRepository();

    @Override // com.qcl.launcher.launcher.mod.RemoteModRepository
    public Stream<RemoteMod> search(String str, RemoteModRepository.Category category, int i, int i2, String str2, RemoteModRepository.SortType sortType, RemoteModRepository.SortOrder sortOrder) throws IOException {
        // ★★★★★ 2026-10-17（1.5.2）**重做搜索的关键词预处理**。
        //
        //   用户需求：「模组/整合包英文查询」。
        //
        //   ★ 原实现的两个问题：
        //     ① **英文输入完全用不上翻译表** —— 只有 `CHINESE_PATTERN` 命中时才查
        //        {@code ModTranslations.searchMod()}（它按中文关键词做 LCS 匹配）。
        //        玩家只记得英文名 / 拼错一个字母时，拿不到规范条目。
        //     ② **中文多命中时用空格把最多 3 个英文名拼在一起**：
        //          `String.join(" ", arrayList)`  →  "Mekanism Thermal Nuclear"
        //        大多数 API 会把它当**多个关键词**（AND/OR），结果要么为 0 条、
        //        要么混进一堆不相关的东西 —— 玩家感受就是"搜了啥也没有"。
        //
        //   ⇒ 现在：
        //     · 中文 → 取**最匹配的 1 条**（按匹配得分排序后的第一条），只用它的英文名；
        //     · 英文 → 也走一次翻译表（新加的 searchModByEnglish），命中就用**规范英文名**
        //       （顺带纠正大小写/拼写），没命中就原样透传（API 本身支持英文）。
        String keyword = str2 == null ? "" : str2;
        if (!keyword.trim().isEmpty()) {
            ModTranslations translations = ModTranslations.getTranslationsByRepositoryType(getType());
            List<ModTranslations.Mod> hits = Collections.emptyList();
            if (StringUtils.CHINESE_PATTERN.matcher(keyword).find()) {
                hits = translations.searchMod(keyword);          // 中文关键词
            } else {
                hits = translations.searchModByEnglish(keyword);  // ★ 英文关键词（1.5.2 新增）
            }
            if (hits != null && !hits.isEmpty()) {
                // ★ 只取最匹配的一条，不再拼接 —— 拼接会把搜索结果弄脏
                ModTranslations.Mod best = hits.get(0);
                String en = best.getSubname();
                if (en == null || en.trim().isEmpty()) {
                    en = best.getCurseforge();
                }
                if (en != null && !en.trim().isEmpty()) {
                    keyword = en.trim();
                }
            }
        }
        return getBackedRemoteModRepository().search(str, category, i, i2, keyword, sortType, sortOrder);
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
