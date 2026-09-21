package com.qcl.launcher.launcher.mod.multimc;

import android.os.AsyncTask;

import com.qcl.launcher.launcher.mod.Modpack;
import com.qcl.launcher.launcher.mod.ModpackProvider;
import com.qcl.launcher.utils.io.FileUtils;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.stream.Stream;

public final class MultiMCModpackProvider implements ModpackProvider {
    public static final MultiMCModpackProvider INSTANCE = new MultiMCModpackProvider();

    @Override
    public String getName() {
        return "MultiMC";
    }

    private static boolean testPath(Path root) {
        return Files.exists(root.resolve("instance.cfg"));
    }

    public static Path getRootPath(Path root) throws IOException {
        if (testPath(root)) return root;
        try (Stream<Path> stream = Files.list(root)) {
            Path candidate = stream.filter(Files::isDirectory).findAny()
                    .orElseThrow(() -> new IOException("Not a valid MultiMC modpack"));
            if (testPath(candidate)) return candidate;
            throw new IOException("Not a valid MultiMC modpack");
        }
    }

    /**
     * ★ 1.2.3：拼一段人话说明，给「查看说明」按钮用。
     *
     * MultiMC / Prism 的 instance.cfg 里没有「介绍」字段（notes 大多为空），
     * 所以我们用 mmc-pack.json 的组件清单拼：
     *   「Minecraft b1.7.3 ・ Cursed Fabric Loader 0.10.6-b1.7.3 ・ LWJGL 2 2.9.4…」
     * 这样点「查看说明」至少能看到这个包由什么组成。
     */
    private static String buildDescription(MultiMCInstanceConfiguration cfg, MultiMCManifest manifest) {
        StringBuilder sb = new StringBuilder();
        String notes = cfg == null ? null : cfg.getNotes();
        if (notes != null && !notes.trim().isEmpty()) {
            sb.append(notes.trim()).append("<br/><br/>");
        }
        sb.append("<b>整合包组成</b><br/>");
        if (manifest != null && manifest.getComponents() != null) {
            for (MultiMCManifest.MultiMCManifestComponent c : manifest.getComponents()) {
                String name = c.getCachedName();
                String ver = c.getCachedVersion();
                if (name == null) name = c.getUid();
                if (name == null) continue;
                sb.append("· ").append(name);
                if (ver != null && !ver.isEmpty()) sb.append(" ").append(ver);
                sb.append("<br/>");
            }
        }
        if (cfg != null && cfg.getGameVersion() != null) {
            sb.append("<br/>游戏版本：").append(cfg.getGameVersion());
        }
        return sb.toString();
    }

    private static String getRootEntryName(ZipFile file) throws IOException {
        final String instanceFileName = "instance.cfg";

        if (file.getEntry(instanceFileName) != null) return "";

        Enumeration<ZipArchiveEntry> entries = file.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            String entryName = entry.getName();

            int idx = entryName.indexOf('/');
            if (idx >= 0
                    && entryName.length() == idx + instanceFileName.length() + 1
                    && entryName.startsWith(instanceFileName, idx + 1))
                return entryName.substring(0, idx + 1);
        }

        throw new IOException("Not a valid MultiMC modpack");
    }

    @Override
    public Modpack readManifest(ZipFile modpackFile, Path modpackPath, Charset encoding) throws IOException {
        String rootEntryName = getRootEntryName(modpackFile);
        MultiMCManifest manifest = MultiMCManifest.readMultiMCModpackManifest(modpackFile, rootEntryName);

        String name = rootEntryName.isEmpty() ? FileUtils.getNameWithoutExtension(modpackPath) : rootEntryName.substring(0, rootEntryName.length() - 1);
        ZipArchiveEntry instanceEntry = modpackFile.getEntry(rootEntryName + "instance.cfg");

        if (instanceEntry == null)
            throw new IOException("`instance.cfg` not found, " + modpackFile + " is not a valid MultiMC modpack.");
        try (InputStream instanceStream = modpackFile.getInputStream(instanceEntry)) {
            MultiMCInstanceConfiguration cfg = new MultiMCInstanceConfiguration(name, instanceStream, manifest);
            // ★ 1.2.3：原来 author / version 是**写死的空字符串**，所以整合包页面上
            //   「作者」「版本」都显示不出来、「查看说明」按钮点了也没反应。
            //   这里给它们填上有意义的内容：
            //     version  ← IntendedVersion / mmc-pack 里 net.minecraft 的版本（如 b1.7.3）
            //     author   ← MultiMC（这类包是 MultiMC/Prism 导出的实例）
            //     description ← 用 mmc-pack 的组件清单拼一句人话，让「查看说明」有东西可看
            return new Modpack(cfg.getName(), "MultiMC", cfg.getGameVersion(),
                    cfg.getGameVersion(), buildDescription(cfg, manifest), encoding, cfg) {
                @Override
                public AsyncTask getInstallTask(File zipFile, String name) {
                    return new MultiMCModpackInstallTask(zipFile, this, cfg, name);
                }
            };
        }
    }

}