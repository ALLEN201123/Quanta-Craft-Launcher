package com.qcl.launcher.launcher.mod.export;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 整合包导出器。
 *
 * <p>刻意只用 {@link java.util.zip.ZipOutputStream} 实现，不依赖 commons-compress，
 * 避免部分 D8 版本在打包阶段崩溃。</p>
 *
 * <p>三种导出格式：</p>
 * <ul>
 *     <li>HMCL：根目录写 modpack.json，游戏文件放在 .minecraft/ 前缀下</li>
 *     <li>MultiMC：根目录写 mmc-pack.json 与 instance.cfg，游戏文件放在 .minecraft/ 前缀下</li>
 *     <li>服务端：只含 mods/config/scripts 与版本 loader 定义文件，另生成
 *     server.properties 模板与启动说明.txt</li>
 * </ul>
 */
public class ModpackExporter {

    public static final int TYPE_HMCL = 0;
    public static final int TYPE_MULTIMC = 1;
    public static final int TYPE_SERVER = 2;

    /** 单个文件超过此大小则跳过（200MB） */
    private static final long MAX_FILE_SIZE = 200L * 1024L * 1024L;
    /** 客户端整合包里游戏文件的前缀 */
    private static final String META_PREFIX = ".minecraft/";

    /** 导出进度回调（在工作线程中被调用，调用方需自行切回主线程） */
    public interface ProgressListener {
        /**
         * @param percent     0~100 的百分比
         * @param currentFile 当前正在打包的条目标识
         */
        void onProgress(int percent, String currentFile);
    }

    /** 待写入压缩包的一个条目：要么是磁盘文件，要么是内存里生成的文本 */
    private static class Item {
        final String entryName;
        final File file;
        final byte[] content;

        Item(String entryName, File file) {
            this.entryName = entryName;
            this.file = file;
            this.content = null;
        }

        Item(String entryName, byte[] content) {
            this.entryName = entryName;
            this.file = null;
            this.content = content;
        }
    }

    private final File gameDir;
    private final File versionDir;
    private final File outputDir;
    private final String fileName;
    private final int type;
    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final List<String> includes;
    private final ProgressListener listener;

    private volatile boolean canceled = false;
    private File outputFile;

    /**
     * @param gameDir     实际游戏工作目录（已处理版本隔离开/关）
     * @param versionDir  当前版本目录（versions/xxx），用于取 loader / 版本定义
     * @param outputDir   产物输出目录（会被自动创建）
     * @param fileName    期望的压缩包文件名（可带或不带 .zip）
     * @param type        见 TYPE_xxx
     * @param includes    要包含的目录/文件名（相对 gameDir），如 mods、config、options.txt
     */
    public ModpackExporter(File gameDir, File versionDir, File outputDir, String fileName,
                           int type, String name, String version, String author, String description,
                           List<String> includes, ProgressListener listener) {
        this.gameDir = gameDir;
        this.versionDir = versionDir;
        this.outputDir = outputDir;
        this.fileName = fileName;
        this.type = type;
        this.name = name == null ? "" : name;
        this.version = version == null ? "" : version;
        this.author = author == null ? "" : author;
        this.description = description == null ? "" : description;
        this.includes = includes == null ? new ArrayList<String>() : includes;
        this.listener = listener;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void cancel() {
        canceled = true;
    }

    public boolean isCanceled() {
        return canceled;
    }

    /** 执行导出。取消时会抛出 IOException，并删除半成品文件。 */
    public void export() throws IOException {
        List<Item> items = new ArrayList<>();
        collectItems(items);

        if (!outputDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outputDir.mkdirs();
        }
        outputFile = resolveOutputFile();

        int total = Math.max(items.size(), 1);
        int lastPercent = -1;
        try {
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            try {
                byte[] buffer = new byte[8192];
                for (int i = 0; i < items.size(); i++) {
                    checkCanceled();
                    Item item = items.get(i);
                    zos.putNextEntry(new ZipEntry(item.entryName));
                    if (item.content != null) {
                        zos.write(item.content);
                    } else {
                        InputStream in = new FileInputStream(item.file);
                        try {
                            int len;
                            while ((len = in.read(buffer)) > 0) {
                                checkCanceled();
                                zos.write(buffer, 0, len);
                            }
                        } finally {
                            in.close();
                        }
                    }
                    zos.closeEntry();

                    int percent = (int) ((i + 1) * 100L / total);
                    if (listener != null && (percent != lastPercent || i % 20 == 0)) {
                        lastPercent = percent;
                        listener.onProgress(percent, item.entryName);
                    }
                }
            } finally {
                zos.close();
            }
        } catch (IOException e) {
            // 取消或失败：删掉写了一半的压缩包
            //noinspection ResultOfMethodCallIgnored
            outputFile.delete();
            throw e;
        }
    }

    private void checkCanceled() throws IOException {
        if (canceled) {
            throw new IOException("用户已取消导出");
        }
    }

    /** 文件名去重：已存在则追加序号 */
    private File resolveOutputFile() {
        String base = sanitizeFileName(fileName);
        if (!base.toLowerCase().endsWith(".zip")) {
            base = base + ".zip";
        }
        File file = new File(outputDir, base);
        int index = 1;
        String stem = base.substring(0, base.length() - 4);
        while (file.exists()) {
            file = new File(outputDir, stem + " (" + index + ").zip");
            index++;
        }
        return file;
    }

    private static String sanitizeFileName(String raw) {
        if (raw == null) {
            return "modpack";
        }
        String s = raw.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (s.isEmpty()) {
            s = "modpack";
        }
        return s;
    }

    private void collectItems(List<Item> items) throws IOException {
        // 1. 元数据文件（都放在压缩包根目录）
        if (type == TYPE_HMCL) {
            items.add(new Item("modpack.json", utf8(buildHmclManifest())));
        } else if (type == TYPE_MULTIMC) {
            items.add(new Item("mmc-pack.json", utf8(buildMultiMcPack())));
            items.add(new Item("instance.cfg", utf8(buildInstanceCfg())));
        } else {
            items.add(new Item("server.properties", utf8(buildServerProperties())));
            items.add(new Item("启动说明.txt", utf8(buildServerReadme())));
            collectLoaderFiles(items);
        }

        // 2. 游戏文件
        String prefix = (type == TYPE_SERVER) ? "" : META_PREFIX;
        List<String> targets;
        if (type == TYPE_SERVER) {
            // 服务端包只允许 mods/config/scripts
            targets = new ArrayList<>();
            for (String entry : includes) {
                if ("mods".equals(entry) || "config".equals(entry) || "scripts".equals(entry)) {
                    targets.add(entry);
                }
            }
        } else {
            targets = includes;
        }

        for (String entry : targets) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            File target = new File(gameDir, entry);
            walk(target, entry, prefix, items);
        }
    }

    /** 服务端：带上版本目录里的 loader / 版本定义（json），供服务端还原加载器 */
    private void collectLoaderFiles(List<Item> items) {
        if (versionDir == null || !versionDir.isDirectory()) {
            return;
        }
        File[] children = versionDir.listFiles();
        if (children == null) {
            return;
        }
        String versionName = versionDir.getName();
        for (File child : children) {
            if (child.isFile() && child.getName().toLowerCase().endsWith(".json")) {
                items.add(new Item("versions/" + versionName + "/" + child.getName(), child));
            }
        }
    }

    private void walk(File target, String entryName, String prefix, List<Item> items) {
        if (target == null || !target.exists()) {
            return;
        }
        if (target.isDirectory()) {
            if (shouldSkipDir(target)) {
                return;
            }
            File[] children = target.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                walk(child, entryName + "/" + child.getName(), prefix, items);
            }
        } else if (target.isFile()) {
            if (shouldSkipFile(target)) {
                return;
            }
            items.add(new Item(prefix + entryName, target));
        }
    }

    private static boolean shouldSkipDir(File dir) {
        return "crash-reports".equals(dir.getName());
    }

    private static boolean shouldSkipFile(File file) {
        String n = file.getName();
        if (".DS_Store".equals(n)) {
            return true;
        }
        if (n.toLowerCase().endsWith(".log")) {
            return true;
        }
        return file.length() > MAX_FILE_SIZE;
    }

    private byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s.getBytes();
        }
    }

    private String gameVersion() {
        return versionDir == null ? "" : versionDir.getName();
    }

    private String buildHmclManifest() {
        JsonObject o = new JsonObject();
        o.addProperty("manifestType", "minecraftModpack");
        o.addProperty("manifestVersion", 1);
        o.addProperty("name", name);
        o.addProperty("author", author);
        o.addProperty("version", version);
        o.addProperty("description", description);
        o.addProperty("gameVersion", gameVersion());
        return o.toString();
    }

    private String buildMultiMcPack() {
        JsonObject root = new JsonObject();
        root.addProperty("formatVersion", 1);
        JsonArray components = new JsonArray();
        JsonObject minecraft = new JsonObject();
        minecraft.addProperty("uid", "net.minecraft");
        minecraft.addProperty("version", gameVersion());
        components.add(minecraft);
        root.add("components", components);
        return root.toString();
    }

    private String buildInstanceCfg() {
        return "InstanceType=OneSix\n"
                + "name=" + name + "\n";
    }

    private String buildServerProperties() {
        return "# QCL 导出的服务端配置模板\n"
                + "# 请按需修改后重命名为 server.properties\n"
                + "online-mode=true\n"
                + "server-port=25565\n"
                + "max-players=20\n"
                + "difficulty=1\n"
                + "gamemode=0\n"
                + "pvp=true\n"
                + "allow-flight=false\n"
                + "view-distance=10\n"
                + "motd=" + name + "\n";
    }

    private String buildServerReadme() {
        return "QCL 服务端整合包\n"
                + "================\n\n"
                + "整合包名称：" + name + "\n"
                + "版本：" + version + "\n"
                + "作者：" + author + "\n\n"
                + "包含内容：\n"
                + "  - mods/           模组\n"
                + "  - config/         模组配置\n"
                + "  - scripts/        脚本\n"
                + "  - versions/       版本与加载器定义\n"
                + "  - server.properties  服务端配置模板（需自行改名后使用）\n\n"
                + "使用步骤：\n"
                + "  1. 将本压缩包内的 mods/、config/、scripts/ 解压到你的服务端目录。\n"
                + "  2. 把 server.properties 复制为服务端可用的配置文件并按需修改。\n"
                + "  3. 根据 versions/ 中的加载器定义，安装对应的服务端加载器后启动。\n";
    }
}