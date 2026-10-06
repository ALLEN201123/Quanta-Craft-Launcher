package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.launcher.list.install.DownloadTaskListAdapter;
import com.qcl.launcher.launcher.list.install.DownloadTaskListBean;
import com.qcl.launcher.launcher.mod.curse.CurseModpackProvider;
import com.qcl.launcher.launcher.mod.mcbbs.McbbsModpackProvider;
import com.qcl.launcher.launcher.mod.modrinth.ModrinthModpackProvider;
import com.qcl.launcher.launcher.mod.multimc.MultiMCModpackProvider;
import com.qcl.launcher.launcher.mod.qclpack.QclModpackProvider;

import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * ★ 1.5.0：未知格式整合包的**兜底**安装任务 —— 绝不再弹「不支持」。
 *
 * 用户在「导入整合包」里选任何 zip，只要没被 Curse / Modrinth / MCBBS / HMCL /
 * MultiMC 识别出来，就走这里。做法是**按 zip 内容判别**后尽力导入：
 *
 *   - 内容像 HMCL（有 minecraft/pack.json）→ 交给 {@link #installHmclZip()}：
 *     minecraft/ 解到版本目录 + pack.json 当版本 json
 *   - 有 .minecraft/ 或 overrides/ 或 client-overrides/ → 解到**游戏目录**
 *   - 什么都没有 → 整包解到**游戏目录**（宁可多铺一点文件，也不让玩家导入失败）
 *
 * 有游戏版本号（Modpack 解析成功但提供器没匹配上）时，顺带把基础版本准备好。
 * 任何一步失败都不阻断 —— 这是本阶段明确要求的 best-effort 语义。
 */
public class GenericModpackInstallTask extends BaseModpackInstallTask {

    public GenericModpackInstallTask(MainActivity activity, File file, Modpack modpack,
                                     String name, DownloadTaskListAdapter adapter) {
        super(activity, file, modpack, name, adapter);
    }

    @Override
    protected void install() throws Exception {
        File versionDir = versionDir(name);
        //noinspection ResultOfMethodCallIgnored
        versionDir.mkdirs();

        // ---- ① 按 zip 内容判别格式 ----
        String format;
        try (ZipFile zip = openZip()) {
            format = detectPackFormat(zip);
        }
        android.util.Log.i("ModpackInstall", "通用导入：按 zip 内容判别为 " + format);

        // ---- ② 内容像 HMCL 就按 HMCL 装（minecraft/ → 版本目录）----
        if (FORMAT_HMCL.equals(format)) {
            installHmclZip();
            return;
        }

        // ---- ③ 有游戏版本号时顺带把基础版本准备好 ----
        String gameVersion = modpack == null ? null : modpack.getGameVersion();
        if (gameVersion != null && !gameVersion.isEmpty()) {
            DownloadTaskListBean baseRow = addRow("准备基础游戏版本 " + gameVersion);
            ensureBaseVersionInstalled(versionDir, gameVersion);
            rowDone(baseRow);
            reportOverall(20);
        }

        // ---- ④ 解包（按内容挑目标目录）----
        DownloadTaskListBean row = addRow("解包整合包（通用模式）");
        try (ZipFile zip = openZip()) {
            List<String> prefixes = resolveExistingPrefixes(zip,
                    ".minecraft/", "client-overrides/", "overrides/", "minecraft/");
            if (prefixes.isEmpty()) {
                // 什么都没有：整包解到游戏目录，尽力导入
                android.util.Log.i("ModpackInstall", "没有识别出 .minecraft / overrides，整包解到运行目录");
                // ★【2026-10-06】按版本隔离决定（原来是写死的 gameDir()）
                extractDirectory(zip, "", runDir(), row, null);
            } else {
                extractPrefixes(zip, prefixes, runDir(), row, null);
            }
        }
        rowDone(row);

        // ---- ⑤ 写整合包配置 ----
        // ★ 2026-10-06：这里原来把内部格式枚举（小写，如 "hmcl"）直接当 type 写进去，
        //   而 ModpackHelper 的注册表用的是 Provider.getName()（如 "QCL"）→ 对不上，
        //   下次这个版本就认不出是整合包了。改成经 providerType() 映射。
        writeModpackConfig(versionDir, providerType(format), null,
                modpack == null ? null : modpack.getVersion(),
                new ArrayList<ModpackConfiguration.FileInformation>());

        reportOverall(100);
    }

    /**
     * 把内部格式枚举（小写，{@code FORMAT_*}）映射成 ModpackHelper 注册表用的 type
     * （即各 Provider 的 {@code getName()} 返回值）。
     *
     * <p>★ 2026-10-06：两者本来是两套字符串，直接拿枚举当 type 写进
     * {@code *.json.modpack} 会导致下次识别不出整合包 —— 这里统一映射。
     */
    private static String providerType(String format) {
        if (FORMAT_HMCL.equals(format)) return QclModpackProvider.INSTANCE.getName();
        if (FORMAT_CURSE.equals(format)) return CurseModpackProvider.INSTANCE.getName();
        if (FORMAT_MODRINTH.equals(format)) return ModrinthModpackProvider.INSTANCE.getName();
        if (FORMAT_MCBBS.equals(format)) return McbbsModpackProvider.INSTANCE.getName();
        if (FORMAT_MULTIMC.equals(format)) return MultiMCModpackProvider.INSTANCE.getName();
        // 认不出来的格式：兜底用 QCL，至少能被自家的 Provider 认回来
        return QclModpackProvider.INSTANCE.getName();
    }
}
