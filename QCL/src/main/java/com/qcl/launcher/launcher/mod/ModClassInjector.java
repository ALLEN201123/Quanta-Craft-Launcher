package com.qcl.launcher.launcher.mod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qcl.launcher.utils.io.JarMerger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * ★ 1.2.3：「class 替换型模组」（jarmod）的检测 / 登记 / 冲突 / 注入 / 删除。
 *
 * ★★ 核心判断（用户反复强调的）：**不是所有带 class 的模组都该进 jar** ——
 *   Fabric/Forge/Quilt 模组 jar 里也有 class，但它们**必须放在 mods/ 里**由加载器加载；
 *   只有**没有任何 mod 元数据、直接裸改 MC 本体 class** 的远古模组才需要注入。
 *
 *   判据：zip 里有 fabric.mod.json / quilt.mod.json / mcmod.info / mods.toml
 *        → 是加载器模组 → 放 mods/，**不注入**。
 *        没有这些元数据但带 .class（尤其 net/minecraft/、org/lwjgl/ 这些本体包）
 *        → 是 class 替换型 → 注入本体 jar。
 *
 * 登记表：versions/&lt;id&gt;/qcl_jarmods.json，格式 { "class路径": "模组名" }。
 * 冲突检测：注入前先查表，同一 class 已被别的模组占用 → 弹窗让玩家选继续/取消。
 */
public final class ModClassInjector {

    /** 登记表文件名（放在版本目录下） */
    public static final String REGISTRY_NAME = "qcl_jarmods.json";

    private static final String[] LOADER_MARKERS = {
            "fabric.mod.json", "quilt.mod.json", "mcmod.info", "mods.toml", "fml.toml"
    };

    private ModClassInjector() {
    }

    /** 登记表类型 */
    private static final Type REGISTRY_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    /**
     * 是不是「加载器模组」（有 fabric.mod.json / mcmod.info 等元数据的那种）。
     * 这种放 mods/，绝不能注入本体。
     */
    public static boolean isLoaderMod(File modZip) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(modZip);
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                String name = en.nextElement().getName();
                String low = name.toLowerCase();
                for (String marker : LOADER_MARKERS) {
                    if (low.equals(marker) || low.endsWith("/" + marker)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;   // 打不开就当不是，走普通 mods/ 流程
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * 列出 zip 里所有 .class 条目（目录分隔符统一成 /）。
     */
    public static List<String> listClasses(File modZip) {
        List<String> out = new ArrayList<>();
        ZipFile zip = null;
        try {
            zip = new ZipFile(modZip);
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                String n = e.getName().replace('\\', '/');
                if (n.endsWith(".class")) {
                    out.add(n);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException ignored) {
                }
            }
        }
        return out;
    }

    /** 读登记表（文件不存在返回空表） */
    public static Map<String, String> loadRegistry(File versionDir) {
        try {
            File f = new File(versionDir, REGISTRY_NAME);
            if (!f.isFile()) {
                return new HashMap<>();
            }
            String raw = new String(readAll(f), "UTF-8");
            Map<String, String> map = new Gson().fromJson(raw, REGISTRY_TYPE);
            return map == null ? new HashMap<String, String>() : map;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /** 写登记表 */
    public static void saveRegistry(File versionDir, Map<String, String> registry) {
        try {
            writeAll(new File(versionDir, REGISTRY_NAME),
                    new Gson().toJson(registry).getBytes("UTF-8"));
        } catch (Exception ignored) {
        }
    }

    /**
     * 冲突检测：这批 class 里，哪些已经被别的模组占用了。
     *
     * @return 冲突列表，每个元素是 "class路径 ← 已被模组XX占用"；没有冲突返回空表
     */
    public static List<String> findConflicts(Map<String, String> registry,
                                             List<String> classes, String modName) {
        List<String> out = new ArrayList<>();
        for (String c : classes) {
            String owner = registry.get(c);
            if (owner != null && !owner.equals(modName)) {
                out.add(c + "  ←  已被「" + owner + "」占用");
            }
        }
        return out;
    }

    /**
     * 把 class 替换型模组注入本体 jar，并登记。
     *
     * @return 注入的条目数；失败抛 IOException
     */
    public static int inject(File versionDir, File modZip, String modName) throws IOException {
        File jar = versionJar(versionDir);
        if (jar == null || !jar.isFile()) {
            throw new IOException("找不到本体 jar（" + versionDir + "），先装好游戏再下这种模组。");
        }
        int count = JarMerger.mergeInto(jar, java.util.Collections.singletonList(modZip));

        // 登记（class → 模组名）
        Map<String, String> registry = loadRegistry(versionDir);
        for (String c : listClasses(modZip)) {
            registry.put(c, modName);
        }
        saveRegistry(versionDir, registry);
        return count;
    }

    /**
     * 从本体 jar 里删掉某个模组注入的所有 class（Class 查看器的删除按钮用）。
     * JarMerger 只会合并不会删，所以这里自己重写整个 jar：
     * 把除目标 class 外的所有条目原样搬到临时文件，再替换回本体。
     *
     * @return 删掉的条目数
     */
    public static int removeClassesOf(File versionDir, String modName) throws IOException {
        File jar = versionJar(versionDir);
        if (jar == null || !jar.isFile()) {
            return 0;
        }
        Map<String, String> registry = loadRegistry(versionDir);
        List<String> doomed = new ArrayList<>();
        for (Map.Entry<String, String> e : registry.entrySet()) {
            if (modName.equals(e.getValue())) {
                doomed.add(e.getKey());
            }
        }
        if (doomed.isEmpty()) {
            return 0;
        }

        File tmp = new File(jar.getParentFile(), jar.getName() + ".tmp");
        int removed = 0;
        ZipFile in = new ZipFile(jar);
        ZipOutputStream outZ = new ZipOutputStream(new FileOutputStream(tmp));
        try {
            Enumeration<? extends ZipEntry> en = in.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName().replace('\\', '/');
                if (doomed.contains(n)) {
                    removed++;
                    continue;   // 跳过 = 删除
                }
                ZipEntry copy = new ZipEntry(n);
                outZ.putNextEntry(copy);
                if (!e.isDirectory()) {
                    InputStream is = in.getInputStream(e);
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = is.read(buf)) > 0) {
                        outZ.write(buf, 0, r);
                    }
                    is.close();
                }
                outZ.closeEntry();
            }
        } finally {
            outZ.close();
            in.close();
        }
        // 替换回本体
        File backup = new File(jar.getParentFile(), jar.getName() + ".bak");
        if (backup.exists() && !backup.delete()) {
            tmp.delete();
            throw new IOException("无法删除旧的备份文件 " + backup);
        }
        if (!jar.renameTo(backup)) {
            tmp.delete();
            throw new IOException("无法重命名本体 jar，删除失败。");
        }
        if (!tmp.renameTo(jar)) {
            backup.renameTo(jar);   // 尽量还原
            tmp.delete();
            throw new IOException("无法写入新的本体 jar，删除失败。");
        }
        backup.delete();

        // 更新登记表
        for (String c : doomed) {
            registry.remove(c);
        }
        saveRegistry(versionDir, registry);
        return removed;
    }

    /** 版本目录下的本体 jar（&lt;id&gt;/&lt;id&gt;.jar） */
    public static File versionJar(File versionDir) {
        File[] files = versionDir.listFiles();
        if (files == null) {
            return null;
        }
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".jar")
                    && new File(versionDir,
                    f.getName().substring(0, f.getName().length() - 4) + ".json").isFile()) {
                return f;
            }
        }
        return null;
    }

    private static byte[] readAll(File f) throws IOException {
        FileInputStream is = new FileInputStream(f);
        try {
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            int r;
            while ((r = is.read(buf, off, buf.length - off)) > 0) {
                off += r;
            }
            return buf;
        } finally {
            is.close();
        }
    }

    private static void writeAll(File f, byte[] data) throws IOException {
        FileOutputStream os = new FileOutputStream(f);
        try {
            os.write(data);
        } finally {
            os.close();
        }
    }
}
