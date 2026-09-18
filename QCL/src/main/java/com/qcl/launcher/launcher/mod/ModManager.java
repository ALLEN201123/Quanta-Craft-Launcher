package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.LocalModFile;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.ZipTools;
import com.qcl.launcher.utils.string.StringUtils;
import com.qcl.launcher.utils.versioning.VersionNumber;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;

/* loaded from: classes2.dex */
public final class ModManager {
    public static final String DISABLED_EXTENSION = ".disabled";
    public static final String OLD_EXTENSION = ".old";
    private final String modsDir;
    private final TreeSet<LocalModFile> localModFiles = new TreeSet<>();
    private final HashMap<LocalMod, LocalMod> localMods = new HashMap<>();
    private boolean loaded = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ LocalMod lambda$getLocalMod$0(LocalMod localMod) {
        return localMod;
    }

    public ModManager(String str) {
        this.modsDir = str;
    }

    public Path getModsDirectory() {
        return new File(this.modsDir).toPath();
    }

    public LocalMod getLocalMod(String str, ModLoaderType modLoaderType) {
        return this.localMods.computeIfAbsent(new LocalMod(str, modLoaderType), new Function() { // from class: com.qcl.launcher.launcher.mod.ModManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Function
            public final Object apply(Object obj) {
                return ModManager.lambda$getLocalMod$0((LocalMod) obj);
            }
        });
    }

    private void addModInfo(Path path) {
        try {
            LocalModFile modInfo = getModInfo(path);
            if (modInfo.isOld()) {
                return;
            }
            this.localModFiles.add(modInfo);
        } catch (IllegalArgumentException unused) {
        }
    }

    public LocalModFile getModInfo(Path path) {
        String str;
        String removeSuffix = StringUtils.removeSuffix(FileUtils.getName(path), ".disabled", ".old");
        if (removeSuffix.endsWith(".zip") || removeSuffix.endsWith(".jar")) {
            try {
                try {
                    try {
                        try {
                            return ForgeOldModMetadata.fromFile(this, path);
                        } catch (Exception unused) {
                            return PackMcMeta.fromFile(this, path);
                        }
                    } catch (Exception unused2) {
                        return FabricModMetadata.fromFile(this, path);
                    }
                } catch (Exception unused3) {
                    return ForgeNewModMetadata.fromFile(this, path);
                }
            } catch (Exception unused4) {
                str = "";
            }
        } else if (removeSuffix.endsWith(".litemod")) {
            try {
                return LiteModMetadata.fromFile(this, path);
            } catch (Exception unused5) {
                str = "LiteLoader Mod";
            }
        } else {
            throw new IllegalArgumentException("File " + path + " is not a mod file.");
        }
        return new LocalModFile(this, getLocalMod(FileUtils.getNameWithoutExtension(path), ModLoaderType.UNKNOWN), path, FileUtils.getNameWithoutExtension(path), new LocalModFile.Description(str));
    }

    public void refreshMods() throws IOException {
        this.localModFiles.clear();
        this.localMods.clear();
        if (Files.isDirectory(getModsDirectory(), new LinkOption[0])) {
            DirectoryStream<Path> newDirectoryStream = Files.newDirectoryStream(getModsDirectory());
            try {
                for (Path path : newDirectoryStream) {
                    if (Files.isDirectory(path, new LinkOption[0]) && VersionNumber.isIntVersionNumber(FileUtils.getName(path))) {
                        DirectoryStream<Path> newDirectoryStream2 = Files.newDirectoryStream(path);
                        try {
                            Iterator<Path> it = newDirectoryStream2.iterator();
                            while (it.hasNext()) {
                                addModInfo(it.next());
                            }
                            if (newDirectoryStream2 != null) {
                                newDirectoryStream2.close();
                            }
                        } finally {
                        }
                    } else {
                        addModInfo(path);
                    }
                }
                if (newDirectoryStream != null) {
                    newDirectoryStream.close();
                }
            } catch (Throwable th) {
                if (newDirectoryStream != null) {
                    try {
                        newDirectoryStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        this.loaded = true;
    }

    public Collection<LocalModFile> getMods() throws IOException {
        if (!this.loaded) {
            refreshMods();
        }
        return this.localModFiles;
    }

    public void addMod(Path path) throws IOException {
        if (!isFileNameMod(path)) {
            throw new IllegalArgumentException("File " + path + " is not a valid mod file.");
        }
        if (!this.loaded) {
            refreshMods();
        }
        Path modsDirectory = getModsDirectory();
        Files.createDirectories(modsDirectory, new FileAttribute[0]);
        Path resolve = modsDirectory.resolve(path.getFileName());
        FileUtils.copyFile(path, resolve);
        addModInfo(resolve);
    }

    public void removeMods(LocalModFile... localModFileArr) throws IOException {
        for (LocalModFile localModFile : localModFileArr) {
            Files.deleteIfExists(localModFile.getFile());
        }
    }

    public void rollback(LocalModFile localModFile, LocalModFile localModFile2) throws IOException {
        if (!this.loaded) {
            throw new IllegalStateException("ModManager Not loaded");
        }
        if (!this.localModFiles.contains(localModFile)) {
            throw new IllegalStateException("Rolling back an unknown mod " + localModFile.getFileName());
        }
        if (localModFile.isOld()) {
            throw new IllegalArgumentException("Rolling back an old mod " + localModFile.getFileName());
        }
        if (!localModFile2.isOld()) {
            throw new IllegalArgumentException("Rolling back to an old path " + localModFile2.getFileName());
        }
        if (localModFile.getFileName().equals(localModFile2.getFileName())) {
            return;
        }
        LocalMod localMod = (LocalMod) Objects.requireNonNull(localModFile.getMod());
        if (localMod != localModFile2.getMod()) {
            throw new IllegalArgumentException("Rolling back mod " + localModFile.getFileName() + " to a different mod " + localModFile2.getFileName());
        }
        if (!localMod.getFiles().contains(localModFile) || !localMod.getOldFiles().contains(localModFile2)) {
            throw new IllegalStateException("LocalMod state corrupt");
        }
        boolean isActive = localModFile.isActive();
        localModFile.setActive(true);
        localModFile.setOld(true);
        localModFile2.setOld(false);
        localModFile2.setActive(isActive);
    }

    private Path backupMod(Path path) throws IOException {
        Path resolveSibling = path.resolveSibling(StringUtils.addSuffix(StringUtils.removeSuffix(FileUtils.getName(path), ".disabled"), ".old"));
        if (Files.exists(path, new LinkOption[0])) {
            Files.move(path, resolveSibling, StandardCopyOption.REPLACE_EXISTING);
        }
        return resolveSibling;
    }

    private Path restoreMod(Path path) throws IOException {
        Path resolveSibling = path.resolveSibling(StringUtils.removeSuffix(FileUtils.getName(path), ".old"));
        if (Files.exists(path, new LinkOption[0])) {
            Files.move(path, resolveSibling, StandardCopyOption.REPLACE_EXISTING);
        }
        return resolveSibling;
    }

    public Path setOld(LocalModFile localModFile, boolean z) throws IOException {
        if (z) {
            Path backupMod = backupMod(localModFile.getFile());
            this.localModFiles.remove(localModFile);
            return backupMod;
        }
        Path restoreMod = restoreMod(localModFile.getFile());
        this.localModFiles.add(localModFile);
        return restoreMod;
    }

    public Path disableMod(Path path) throws IOException {
        if (isOld(path)) {
            return path;
        }
        Path resolveSibling = path.resolveSibling(StringUtils.addSuffix(FileUtils.getName(path), ".disabled"));
        if (Files.exists(path, new LinkOption[0])) {
            Files.move(path, resolveSibling, StandardCopyOption.REPLACE_EXISTING);
        }
        return resolveSibling;
    }

    public Path enableMod(Path path) throws IOException {
        if (isOld(path)) {
            return path;
        }
        Path resolveSibling = path.resolveSibling(StringUtils.removeSuffix(FileUtils.getName(path), ".disabled"));
        if (Files.exists(path, new LinkOption[0])) {
            Files.move(path, resolveSibling, StandardCopyOption.REPLACE_EXISTING);
        }
        return resolveSibling;
    }

    public static String getModName(Path path) {
        return StringUtils.removeSuffix(FileUtils.getName(path), ".disabled", ".old");
    }

    public boolean isOld(Path path) {
        return FileUtils.getName(path).endsWith(".old");
    }

    public boolean isDisabled(Path path) {
        return FileUtils.getName(path).endsWith(".disabled");
    }

    public static boolean isFileNameMod(Path path) {
        String modName = getModName(path);
        return modName.endsWith(".zip") || modName.endsWith(".jar") || modName.endsWith(".litemod");
    }

    public static boolean isFileMod(Path path) {
        try {
            if (ZipTools.isFileExist(path.toString(), "mcmod.info") || ZipTools.isFileExist(path.toString(), "META-INF/mods.toml") || ZipTools.isFileExist(path.toString(), "fabric.mod.json") || ZipTools.isFileExist(path.toString(), "litemod.json")) {
                return true;
            }
            return ZipTools.isFileExist(path.toString(), "pack.mcmeta");
        } catch (IOException unused) {
            return false;
        }
    }

    public boolean hasSimpleMod(String str) {
        return Files.exists(getModsDirectory().resolve(StringUtils.removeSuffix(str, ".disabled")), new LinkOption[0]) || Files.exists(getModsDirectory().resolve(StringUtils.addSuffix(str, ".disabled")), new LinkOption[0]);
    }

    public Path getSimpleModPath(String str) {
        return getModsDirectory().resolve(str);
    }

    public static String getMcmodUrl(String str) {
        return String.format("https://www.mcmod.cn/class/%s.html", str);
    }

    public static String getModWikiUrl(String str) {
        return String.format("https://search.mcmod.cn/s?key=%s", str);
    }
}
