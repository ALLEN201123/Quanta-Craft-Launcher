package com.qcl.launcher.utils.game;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ★★★★★ 2026-10-17（1.5.2 新增）**读取一个 jar 需要哪个 Java 版本**。
 *
 * <p>照 FCL 的 {@code JarExecutorHelper.getJavaVersion()} 实现，原理：
 * <ol>
 *   <li>打开 jar，读 {@code META-INF/MANIFEST.MF} 里的 {@code Main-Class:}；</li>
 *   <li>把 {@code a.b.C} 换成 {@code a/b/C.class}，把它读出来；</li>
 *   <li>校验魔数 {@code 0xCAFEBABE}，读紧随其后的 <b>minor</b> 与 <b>major</b> 版本号；</li>
 *   <li>Java 版本 = {@code major - 44}（46 = Java 1.2，52 = Java 8，61 = Java 17 …）。</li>
 * </ol>
 *
 * <p>为什么要这么麻烦：玩家丢进来的 jar 可能是 Java 5 时代的老工具，也可能是 Java 21 的新程序。
 * 用错 Java 会直接抛 {@code UnsupportedClassVersionError}，而错误信息对普通玩家毫无意义
 * —— 所以先探测、再挑最接近的 Java。
 */
public final class JarJavaProbe {

    /** 探测失败时的返回值。 */
    public static final int UNKNOWN = -1;

    private JarJavaProbe() {
    }

    /**
     * 探测该 jar 的主类需要的最低 Java 大版本。
     *
     * @param jarPath jar 的绝对路径
     * @return 需要的 Java 大版本（如 8 / 17 / 21）；探测不出来返回 {@link #UNKNOWN}
     */
    public static int requiredJavaVersion(String jarPath) {
        if (jarPath == null) {
            return UNKNOWN;
        }
        ZipFile zip = null;
        try {
            zip = new ZipFile(jarPath);
            ZipEntry manifest = zip.getEntry("META-INF/MANIFEST.MF");
            if (manifest == null) {
                return UNKNOWN;
            }
            String manifestText = readAll(zip.getInputStream(manifest));
            String mainClass = extractLineValue(manifestText, "Main-Class:");
            if (mainClass == null) {
                return UNKNOWN;
            }
            String classEntry = mainClass.trim().replace('.', '/') + ".class";
            ZipEntry mainClassFile = zip.getEntry(classEntry);
            if (mainClassFile == null) {
                return UNKNOWN;
            }
            byte[] head = new byte[8];
            InputStream in = zip.getInputStream(mainClassFile);
            int read;
            try {
                read = in.read(head);
            } finally {
                try {
                    in.close();
                } catch (Throwable ignored) {
                }
            }
            if (read < 8) {
                return UNKNOWN;
            }
            ByteBuffer buf = ByteBuffer.wrap(head);
            if (buf.getInt() != 0xCAFEBABE) {
                return UNKNOWN;
            }
            buf.getShort();                     // minor（不用）
            int major = buf.getShort() & 0xFFFF;
            return classVersionToJava(major);
        } catch (Throwable t) {
            return UNKNOWN;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** class 文件 major 号 → Java 大版本（46 = Java 1.2 … 52 = 8 … 61 = 17 … 65 = 21）。 */
    public static int classVersionToJava(int majorClassVersion) {
        if (majorClassVersion < 46) {
            return 2;
        }
        return majorClassVersion - 44;
    }

    /** 从 MANIFEST 文本里取某一行的值（"Key: value"）。 */
    public static String extractLineValue(String text, String key) {
        if (text == null || key == null) {
            return null;
        }
        // MANIFEST 行可能以 \r\n 分隔；续行以单个空格开头，这里只取第一行足够判断 Main-Class
        int start = text.indexOf(key);
        if (start < 0) {
            return null;
        }
        start += key.length();
        int end = text.indexOf('\n', start);
        if (end < 0) {
            end = text.length();
        }
        String v = text.substring(start, end).replace("\r", "").trim();
        return v.isEmpty() ? null : v;
    }

    private static String readAll(InputStream in) throws Exception {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toString("UTF-8");
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * 把「jar 需要的 Java」映射成 QCL 可用的 Java 目录名。
     *
     * <p>QCL 目前只区分两类运行时：
     * <ul>
     *   <li>{@code jre-legacy} → Java 8（远古 / 1.16 及以前的 MC、以及绝大多数老工具 jar）</li>
     *   <li>新版运行时（17 / 21）→ 新程序</li>
     * </ul>
     * 所以这里只做二分：需要 &gt; 8 就走新版，否则走 Java 8。
     */
    public static boolean needsModernJava(int requiredJava) {
        return requiredJava > 8;
    }
}
