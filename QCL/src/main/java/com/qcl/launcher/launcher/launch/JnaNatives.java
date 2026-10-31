package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * ★★★★★ 1.5.8：**JNA natives 兜底**（照搬 FCL 的 {@code JnaNatives.kt}，修 26.4 快照启动崩溃）。
 *
 * <h3>为什么需要它</h3>
 * <p>MC 升级会引入 QCL 的 assets 里没带的 JNA 版本（例如 <b>26.4 快照的 5.19.1</b>）。
 * 那时 {@code jna.boot.library.path} 指向的 {@code <files>/jna/<版本>/} 里**没有**
 * {@code libjnidispatch.so}，JNA 就会**从 MC 的 jar 里抽取 glibc 链接的桌面版 so** ——
 * 那个 so 在 Android 上 {@code dlopen} 必然失败，表现为：
 *
 * <pre>
 *   jna.boot.library.path 落空 → JNA 自己抽 jar 内 so → dlopen 失败
 *   → NoClassDefFoundError: Could not initialize class com.sun.jna.NativeLong
 * </pre>
 *
 * <h3>做法</h3>
 * <ol>
 *   <li><b>优先复制 APK 内 assets</b>（{@code app_runtime/jna/<版本>/natives/<abi>/libjnidispatch.so}）
 *       —— 应对「已装的 APK 没触发重装」的升级场景</li>
 *   <li><b>assets 缺失时从 Maven Central 镜像下 {@code jna-<版本>.aar} 并提取</b>当前架构的 so</li>
 * </ol>
 *
 * <p>★ 全程失败**只记日志、不阻塞启动** —— 兜底失败时退回 JNA 自己的解析逻辑（行为和改动前一致）。
 *
 * <p>★ 镜像顺序照 FCL：阿里云 → 华为 → repo1（国内可达性排序）。
 * <b>BMCLAPI 的 /maven 只代理启动器常用产物，不含 aar，不能作候选。</b>
 */
public final class JnaNatives {

    /** Maven 里的 aar 相对路径模板（%1$s = 版本号）。 */
    private static final String AAR_PATH = "net/java/dev/jna/jna/%1$s/jna-%1$s.aar";

    /** Maven Central 镜像候选（按国内可达性排序）。 */
    private static final String[] MIRRORS = new String[]{
            "https://maven.aliyun.com/repository/central/",
            "https://repo.huaweicloud.com/repository/maven/",
            "https://repo1.maven.org/maven2/",
    };

    private JnaNatives() {
    }

    /**
     * 确保 {@code targetDir/libjnidispatch.so} 存在。
     *
     * @param context    Context（取 assets 用）
     * @param jnaVersion JNA 版本号（如 {@code 5.19.1}），空则直接返回
     * @param targetDir  目标目录（QCL 用 {@code <files>/jna341} 或 {@code <files>/jna333}）
     * @return 最终可用的 so 文件；失败返回 null
     */
    public static File ensure(Context context, String jnaVersion, File targetDir) {
        if (context == null || targetDir == null
                || jnaVersion == null || jnaVersion.trim().isEmpty()) {
            return null;
        }
        String abi = abiDir();
        File target = new File(targetDir, "libjnidispatch.so");
        if (target.isFile() && target.length() > 0) {
            return target;
        }
        try {
            if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                // 建不了目录就算了，别把启动搞崩
                return null;
            }
            if (copyFromAssets(context, jnaVersion, abi, target)) {
                android.util.Log.i("jrelog", "[JNA] natives 已从 assets 复制: "
                        + jnaVersion + "/" + abi);
                return target;
            }
            if (downloadFromMavenCentral(jnaVersion, abi, target)) {
                android.util.Log.i("jrelog", "[JNA] natives 已从 Maven 下载: "
                        + jnaVersion + "/" + abi);
                return target;
            }
        } catch (Throwable t) {
            android.util.Log.w("jrelog", "[JNA] 兜底失败（退回 JNA 自行解析）: " + t);
        }
        return null;
    }

    // ======================================================================
    //  内部
    // ======================================================================

    /** 当前设备 ABI 目录名（和 assets 里的目录一致）。 */
    private static String abiDir() {
        try {
            int arch = com.qcl.launcher.utils.Architecture.getRuntimeArchitecture();
            if (arch == com.qcl.launcher.utils.Architecture.ARCH_ARM) {
                return "armeabi-v7a";
            }
            if (arch == com.qcl.launcher.utils.Architecture.ARCH_ARM64) {
                return "arm64-v8a";
            }
            if (arch == com.qcl.launcher.utils.Architecture.ARCH_X86) {
                return "x86";
            }
            return "x86_64";
        } catch (Throwable t) {
            return "arm64-v8a";
        }
    }

    /** 从 APK 的 assets 复制（对应 FCL 的 copyFromAssets）。 */
    private static boolean copyFromAssets(Context context, String ver, String abi, File target) {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        InputStream in = null;
        FileOutputStream out = null;
        try {
            AssetManager am = context.getAssets();
            in = am.open("app_runtime/jna/" + ver + "/natives/" + abi + "/libjnidispatch.so");
            out = new FileOutputStream(tmp);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            out.close();
            out = null;
            in.close();
            in = null;
            return move(tmp, target);
        } catch (Throwable t) {
            // assets 未携带该版本属预期情况，交由下载兜底
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }

    /** 从 Maven Central 镜像下 aar 并提取（对应 FCL 的 downloadFromMavenCentral）。 */
    private static boolean downloadFromMavenCentral(String ver, String abi, File target) {
        File dir = target.getParentFile();
        if (dir == null) {
            return false;
        }
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        File aar = new File(dir, "jna-" + ver + ".aar.tmp");
        try {
            for (String mirror : MIRRORS) {
                String url = mirror + String.format(AAR_PATH, ver);
                try {
                    download(url, aar);
                    if (extract(aar, "jni/" + abi + "/libjnidispatch.so", target)) {
                        return true;
                    }
                } catch (Throwable t) {
                    android.util.Log.w("jrelog", "[JNA] 镜像不可用: " + url + " — " + t);
                }
            }
            return false;
        } finally {
            //noinspection ResultOfMethodCallIgnored
            aar.delete();
        }
    }

    private static void download(String url, File dest) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " from " + url);
            }
            in = conn.getInputStream();
            out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean extract(File aar, String entryName, File target) {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        ZipFile zip = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            zip = new ZipFile(aar);
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                throw new IOException("aar 里没有 " + entryName);
            }
            in = zip.getInputStream(entry);
            out = new FileOutputStream(tmp);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            out.close();
            out = null;
            return move(tmp, target);
        } catch (Throwable t) {
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
            return false;
        } finally {
            closeQuietly(in);
            closeQuietly(out);
            if (zip != null) {
                try {
                    zip.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static boolean move(File src, File dst) {
        //noinspection ResultOfMethodCallIgnored
        dst.delete();
        return src.renameTo(dst);
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
