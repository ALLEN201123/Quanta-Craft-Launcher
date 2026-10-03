package com.qcl.launcher.launcher.game;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.LongTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.io.FileUtils;
import com.qcl.launcher.utils.io.ZipTools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class World {

    private final Path file;
    private String fileName;
    private String worldName;
    private String gameVersion;
    private long lastPlayed;

    public World(Path file) throws IOException {
        this.file = file;

        if (Files.isDirectory(file))
            loadFromDirectory();
        else if (Files.isRegularFile(file))
            loadFromZip();
        else
            throw new IOException("Path " + file + " cannot be recognized as a Minecraft world");
    }

    private void loadFromDirectory() throws IOException {
        fileName = FileUtils.getName(file);
        Path levelDat = file.resolve("level.dat");
        getWorldName(levelDat);
    }

    public Path getFile() {
        return file;
    }

    public String getFileName() {
        return fileName;
    }

    public String getWorldName() {
        return worldName;
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    private void loadFromZipImpl(Path root) throws IOException {
        Path levelDat = root.resolve("level.dat");
        if (!Files.exists(levelDat))
            throw new IOException("Not a valid world zip file since level.dat cannot be found.");

        getWorldName(levelDat);
    }

    private void loadFromZip() throws IOException {
        String r = AppManifest.SAVES_CACHE_DIR + "/world";
        com.qcl.launcher.utils.file.FileUtils.deleteDirectory(r);
        ZipTools.unzipFile(file.toString(), r, false);
        Path cur = new File(r + "/level.dat").toPath();
        if (Files.isRegularFile(cur)) {
            fileName = FileUtils.getName(file);
            loadFromZipImpl(new File(r + "/").toPath());
            return;
        }

        try (Stream<Path> stream = Files.list(new File(r + "/").toPath())) {
            Path root = stream.filter(Files::isDirectory).findAny().orElseThrow(() -> new IOException("Not a valid world zip file"));
            fileName = FileUtils.getName(root);
            loadFromZipImpl(root);
        }
    }

    private void getWorldName(Path levelDat) throws IOException {
        if (!Files.exists(levelDat)) {
            throw new IOException("Not a valid world since level.dat cannot be found.");
        }

        // ★ 1.4.4：远古版本（classic / indev / infdev）的 level.dat 与现代格式差别很大
        //   （可能没有 gzip、根标签不叫 Data、缺 LastPlayed…）。原来这里是"缺标签就抛异常"，
        //   而 getWorlds() 捕获异常后 Stream.empty() → **该存档被静默丢弃**，
        //   表现就是"保存完世界也不出现在存档页"。现在改成：解析不出来也照样收录，用目录名兜底。
        CompoundTag nbt;
        try {
            nbt = parseLevelDat(levelDat);
        } catch (Throwable t) {
            Logging.LOG.log(Level.INFO, "level.dat unparsable for " + file + ", fall back to folder name");
            useFolderAsMetadata();
            return;
        }

        CompoundTag data = nbt.get("Data");
        if (data == null) {
            // 1.2 之前的旧格式：根标签本身就是数据区
            if (nbt.get("LevelName") instanceof StringTag) {
                worldName = nbt.<StringTag>get("LevelName").getValue();
            } else {
                useFolderAsMetadata();
                return;
            }
            data = nbt;
        }

        if (data.get("LevelName") instanceof StringTag)
            worldName = data.<StringTag>get("LevelName").getValue();
        else if (worldName == null)
            worldName = folderName();

        if (data.get("LastPlayed") instanceof LongTag)
            lastPlayed = data.<LongTag>get("LastPlayed").getValue();
        else
            lastPlayed = fileModifiedTime();

        gameVersion = null;
        if (data.get("Version") instanceof CompoundTag) {
            CompoundTag version = data.get("Version");

            if (version.get("Name") instanceof StringTag)
                gameVersion = version.<StringTag>get("Name").getValue();
        }
    }

    /** ★ 1.4.4：解析不出 level.dat 时用「文件夹名 + 文件夹修改时间」兜底，保证存档仍出现在列表里。 */
    private void useFolderAsMetadata() {
        if (worldName == null || worldName.isEmpty()) {
            worldName = folderName();
        }
        lastPlayed = fileModifiedTime();
        gameVersion = null;   // 未知版本 → WorldManagerUI 的过滤会把它列进当前版本
    }

    private String folderName() {
        Path p = file;
        if (p != null && p.getFileName() != null) {
            return p.getFileName().toString();
        }
        return "";
    }

    private long fileModifiedTime() {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    public void rename(String newName) throws IOException {
        if (!Files.isDirectory(file))
            throw new IOException("Not a valid world directory");

        // Change the name recorded in level.dat
        Path levelDat = file.resolve("level.dat");
        CompoundTag nbt = parseLevelDat(levelDat);
        CompoundTag data = nbt.get("Data");
        data.put(new StringTag("LevelName", newName));

        try (OutputStream os = new GZIPOutputStream(Files.newOutputStream(levelDat))) {
            NBTIO.writeTag(os, nbt);
        }

        // then change the folder's name
        Files.move(file, file.resolveSibling(newName));
    }

    public void install(Path savesDir, String name) throws IOException {
        Path worldDir;
        try {
            worldDir = savesDir.resolve(name);
        } catch (InvalidPathException e) {
            throw new IOException(e);
        }

        if (Files.isDirectory(worldDir)) {
            throw new FileAlreadyExistsException("World already exists");
        }

        if (Files.isRegularFile(file)) {
            com.qcl.launcher.utils.file.FileUtils.deleteDirectory(AppManifest.SAVES_CACHE_DIR + "/install/world");
            ZipTools.unzipFile(file.toString(),AppManifest.SAVES_CACHE_DIR + "/install/world",false);
            Path cur = new File(AppManifest.SAVES_CACHE_DIR + "/install/world/level.dat").toPath();
            if (Files.isRegularFile(cur)) {
                com.qcl.launcher.utils.file.FileUtils.rename(AppManifest.SAVES_CACHE_DIR + "/install/world",name);
                com.qcl.launcher.utils.file.FileUtils.copyDirectory(AppManifest.SAVES_CACHE_DIR + "/install/" + name,worldDir.toString());
            } else {
                try (Stream<Path> stream = Files.list(new File(AppManifest.SAVES_CACHE_DIR + "/install/world/").toPath())) {
                    List<Path> subDirs = stream.collect(Collectors.toList());
                    if (subDirs.size() != 1) {
                        throw new IOException("World zip malformed");
                    }
                    String subDirectoryName = FileUtils.getName(subDirs.get(0));
                    com.qcl.launcher.utils.file.FileUtils.rename(AppManifest.SAVES_CACHE_DIR + "/install/world/" + subDirectoryName,name);
                    com.qcl.launcher.utils.file.FileUtils.copyDirectory(AppManifest.SAVES_CACHE_DIR + "/install/world/" + name,worldDir.toString());
                }
            }
            new World(worldDir).rename(name);
        } else if (Files.isDirectory(file)) {
            FileUtils.copyDirectory(file, worldDir);
        }
    }

    public void export(String exportPath, String name) throws IOException {
        if (!Files.isDirectory(file))
            throw new IOException();

        ZipTools.zip(file.toString(), exportPath, name);
    }

    private static CompoundTag parseLevelDat(Path path) throws IOException {
        // ★ 1.4.4：远古版本的 level.dat 常常**没有 gzip 压缩**，原来只走 GZIPInputStream 一条路
        //   → 直接抛异常 → 存档被 getWorlds 丢掉（"保存完世界也不出现在存档页"）。
        //   现在先试 gzip，失败再按原始 NBT 读。
        try (InputStream is = new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path)))) {
            Tag nbt = NBTIO.readTag(is);
            if (nbt instanceof CompoundTag)
                return (CompoundTag) nbt;
        } catch (Throwable ignored) {
        }
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path))) {
            Tag nbt = NBTIO.readTag(is);
            if (nbt instanceof CompoundTag)
                return (CompoundTag) nbt;
            else
                throw new IOException("level.dat malformed");
        }
    }

    public static Stream<World> getWorlds(Path savesDir) {
        try {
            if (Files.exists(savesDir)) {
                return Files.list(savesDir).flatMap(world -> {
                    try {
                        return Stream.of(new World(world));
                    } catch (IOException e) {
                        Logging.LOG.log(Level.WARNING, "Failed to read world " + world, e);
                        return Stream.empty();
                    }
                });
            }
        } catch (IOException e) {
            Logging.LOG.log(Level.WARNING, "Failed to read saves", e);
        }
        return Stream.empty();
    }
}