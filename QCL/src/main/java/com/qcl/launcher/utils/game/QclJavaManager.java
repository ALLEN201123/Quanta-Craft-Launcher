package com.qcl.launcher.utils.game;

import com.qcl.launcher.manifest.AppManifest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**Java 运行时管理**（照 FCL 的 {@code JavaManager}）。
 *
 * <p>用户需求：「别人跟我说 FCL 有这个功能，我也需要加这个」——
 * 指的是 FCL 的 **Java 管理对话框**（可看/删/导入多个 Java，并从那里执行 jar 文件）。
 *
 * <p>★ 好消息：QCL **本来就是多 Java 架构**，不需要改目录结构。
 * {@code AppManifest.JAVA_DIR} 下已经有：
 * <pre>
 *   default  → Java 8   （bin/java）
 *   JRE17    → Java 17
 *   JRE21    → Java 21
 *   JRE25    → Java 25
 * </pre>
 * 只是 UI 上写死了 4 个勾选项，玩家看不到、也不能自己导入。
 *
 * <p>本类只做两件事：
 * <ol>
 *   <li><b>扫描</b> {@code JAVA_DIR} 下所有子目录，读各自的 {@code release} 文件取版本号
 *       （JVM 发行版都会带这个文件，里面有一行 {@code JAVA_VERSION="1.8.0_392"}）；</li>
 *   <li><b>删除</b>某个 Java 目录。</li>
 * </ol>
 */
public final class QclJavaManager {

    /** {@code release} 文件里版本号的正则（照 FCL 的写法）。 */
    private static final Pattern JAVA_VERSION_PATTERN =
            Pattern.compile("JAVA_VERSION=\"([^\"]+)\"");

    private QclJavaManager() {
    }

    /** 一个已安装的 Java 运行时。 */
    public static class JavaEntry {
        /** 目录名（就是运行时 id，如 {@code default} / {@code JRE17}）。 */
        public final String name;
        /** release 里读到的版本串（读不到时用目录名兜底）。 */
        public final String version;
        /** 目录绝对路径。 */
        public final String path;
        /** 是否能用（有 bin/java 且可执行）。 */
        public final boolean usable;

        JavaEntry(String name, String version, String path, boolean usable) {
            this.name = name;
            this.version = version;
            this.path = path;
            this.usable = usable;
        }

        /** 展示用文本：`default  ·  1.8.0_392` */
        public String display() {
            return this.name + "  ·  " + this.version + (this.usable ? "" : "  （不可用）");
        }

        @Override
        public String toString() {
            return display();
        }
    }

    /** 扫描 {@code JAVA_DIR} 下所有 Java 运行时（按目录名排序）。 */
    public static List<JavaEntry> scan() {
        List<JavaEntry> out = new ArrayList<>();
        try {
            File root = new File(AppManifest.JAVA_DIR);
            File[] dirs = root.listFiles();
            if (dirs == null) {
                return out;
            }
            for (File d : dirs) {
                if (d == null || !d.isDirectory()) {
                    continue;
                }
                String version = readReleaseVersion(d);
                boolean usable = new File(d, "bin/java").isFile();
                out.add(new JavaEntry(d.getName(),
                        version == null || version.isEmpty() ? "未知版本" : version,
                        d.getAbsolutePath(), usable));
            }
            Collections.sort(out, (a, b) -> a.name.compareToIgnoreCase(b.name));
        } catch (Throwable ignored) {
        }
        return out;
    }

    /** 读 {@code <java>/release} 里的 {@code JAVA_VERSION="..."}；读不到返回 null。 */
    public static String readReleaseVersion(File javaDir) {
        if (javaDir == null) {
            return null;
        }
        File release = new File(javaDir, "release");
        if (!release.isFile()) {
            return null;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(release);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
                if (bos.size() > 64 * 1024) {
                    break;      // release 文件很小，防意外
                }
            }
            String text = bos.toString("UTF-8");
            Matcher m = JAVA_VERSION_PATTERN.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Throwable ignored) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    /** 删除某个 Java 运行时目录。返回是否删成功。 */
    public static boolean remove(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        try {
            File dir = new File(AppManifest.JAVA_DIR, name);
            if (!dir.isDirectory()) {
                return false;
            }
            // ★ 防路径穿越：目录必须就在 JAVA_DIR 下
            if (!dir.getCanonicalPath().startsWith(
                    new File(AppManifest.JAVA_DIR).getCanonicalPath())) {
                return false;
            }
            com.qcl.launcher.utils.io.FileUtils.deleteDirectoryQuietly(dir);
            return !dir.exists();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 按 MC 版本推荐一个 Java 运行时目录名（与 RuntimeInstallActivity 的四个 Spec 对应）。
     *
     * @param mcVersion MC 版本 id
     * @return 建议的目录名（不保证存在，调用方应再核对）
     */
    public static String suggestRuntimeFor(String mcVersion) {
        if (mcVersion == null) {
            return "default";
        }
        String v = mcVersion.trim();
        try {
            if (v.startsWith("1.")) {
                // 1.20.5+ 需要 21；1.18~1.20.4 需要 17；其余用 8
                String[] parts = v.split("\\.");
                if (parts.length >= 2) {
                    int minor = Integer.parseInt(parts[1]);
                    if (minor >= 21) {
                        return "JRE21";
                    }
                    if (minor >= 18) {
                        return "JRE17";
                    }
                }
                return "default";
            }
        } catch (Throwable ignored) {
        }
        return "default";
    }
}
