package net.minecraft.client;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * ★ QCL 给远古版本（indev / infdev）游戏侧用的存档工具类。
 *
 * <p><b>为什么单独放一个自己的包</b>：这套 2010 年的代码里
 * {@code net/minecraft/client/c.class} 与 {@code net/minecraft/client/c/} 包**同名**，
 * javac 一遇到就把 {@code c} 当包、同包类全部解析失败（试过 -cp/-sourcepath/扁平化都不行）。
 * 放在 {@code qcl.cacio} 下就没有这个歧义，编译毫无问题；游戏侧只需要用字节码补丁
 * 把 {@code net.minecraft.client.c.e.run()} 改成调用这里的方法即可。
 *
 * <p>编译要求：Java 8 字节码（{@code -source 8 -target 8}，对着设备 JRE8 的 rt.jar）。
 */
public final class QclAutoSave {

    private QclAutoSave() {
    }

    /** 远古版本存档后缀。 */
    public static final String EXT = ".mclevel";

    /** 最多给游戏显示几个槽位（游戏那边就是 5 个）。 */
    public static final int MAX_SLOTS = 5;

    /**
     * 扫存档目录，返回给游戏的列表文本（每个槽位一条，形如 {@code 世界名  -  10-03 17:00}）。
     * 按修改时间倒序 —— 刚保存的排最前面。没有存档时返回全 "-"（游戏把它们当空槽位）。
     *
     * @param dir 存档目录（游戏传 gameDir/saves）
     */
    public static String[] list(File dir) {
        String[] out = new String[MAX_SLOTS];
        for (int i = 0; i < MAX_SLOTS; i++) {
            out[i] = "-";
        }
        try {
            if (dir == null || !dir.exists()) {
                return out;
            }
            List<File> files = listFiles(dir);
            for (int i = 0; i < files.size() && i < MAX_SLOTS; i++) {
                File f = files.get(i);
                out[i] = stripExt(f.getName()) + "  -  " + time(f.lastModified());
            }
            System.out.println("[QCL-saves] 本地存档 " + files.size() + " 个，目录=" + dir.getAbsolutePath());
        } catch (Throwable t) {
            System.out.println("[QCL-saves] 扫描失败：" + t);
        }
        return out;
    }

    /**
     * 取最近保存的那个存档文件（给"加载文件"用）。
     *
     * @param dir 存档目录
     * @return 最近的 .mclevel 文件；没有则 null
     */
    public static File newest(File dir) {
        try {
            List<File> files = listFiles(dir);
            return files.isEmpty() ? null : files.get(0);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 列出目录里所有 .mclevel（按修改时间倒序）。 */
    public static List<File> listFiles(File dir) {
        List<File> files = new ArrayList<File>();
        if (dir == null) {
            return files;
        }
        File[] all = dir.listFiles();
        if (all == null) {
            return files;
        }
        for (int i = 0; i < all.length; i++) {
            File f = all[i];
            if (f.isFile() && f.getName().toLowerCase().endsWith(EXT)) {
                files.add(f);
            }
        }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                long x = a.lastModified();
                long y = b.lastModified();
                if (x == y) {
                    return 0;
                }
                return x > y ? -1 : 1;
            }
        });
        return files;
    }

    /** 去掉 .mclevel 后缀。 */
    public static String stripExt(String name) {
        if (name == null) {
            return "";
        }
        if (name.toLowerCase().endsWith(EXT)) {
            return name.substring(0, name.length() - EXT.length());
        }
        return name;
    }

    /** 给"保存"起一个不冲突的名字：世界 / 世界2 / 世界3… */
    public static String nextSaveName(File dir) {
        String base = "世界";
        try {
            if (dir == null) {
                return base;
            }
            String name = base;
            int i = 2;
            while (new File(dir, name + EXT).exists() && i < 1000) {
                name = base + i;
                i++;
            }
            return name;
        } catch (Throwable t) {
            return base;
        }
    }

    /** 格式化时间：MM-dd HH:mm */
    public static String time(long ms) {
        try {
            return new SimpleDateFormat("MM-dd HH:mm").format(new Date(ms));
        } catch (Throwable t) {
            return "";
        }
    }
}
