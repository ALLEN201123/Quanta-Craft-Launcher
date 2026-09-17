package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.res.AssetManager;

import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.Architecture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ★★★ 1.1.0 隔离方案：LWJGL 3.3.3 的按需加载器。
 *
 * <p>背景（2026-09-16 对比实验结论）：
 * 老版本（b1.x / 1.7.x / ≤1.20.4）必须用 Pojav 原版的 LWJGL 3.2.3 栈
 * （{@code assets/app_runtime/pojav/lwjgl3/lwjgl-glfw-classes.jar} —— Pojav 特制大包，
 * 同时含 LWJGL 3 核心 + GLFW stub + LWJGL 2 兼容类）；
 * 而 1.20.5+ 需要 LWJGL 3.3.3（其版本 json 声明 {@code org.lwjgl:lwjgl:3.3.3}）。
 *
 * <p>绝不能"全局替换"（会破坏老版本）——所以这里做**完全隔离**：
 * <ul>
 *   <li>3.3.3 的 jar 和 native 都放在 {@code assets/app_runtime/lwjgl333/}，与老版本零交集；</li>
 *   <li>只有"版本 json 声明 3.3.3"时才解压到应用私有目录并用它们；</li>
 *   <li>native 解压后**保持原名**（liblwjgl.so 等）放在独立目录，
 *       通过 {@code -Dorg.lwjgl.librarypath=<该目录>} 让 LWJGL 找到，
 *       避免与 APK 的 jniLibs（v1.0.9 的 3.2.3 so）冲突。</li>
 * </ul>
 */
public class Lwjgl333Helper {

    /** assets 里的隔离资源根目录 */
    private static final String ASSET_ROOT = "app_runtime/lwjgl333";
    /** 私有目录（解压目标） */
    public static final String DIR_NAME = "lwjgl333";

    private static boolean sPrepared = false;

    /** 返回解压后的 natives 目录（供 -Dorg.lwjgl.librarypath 用），失败返回 null */
    public static File nativesDir(Context context) {
        return new File(context.getFilesDir(), DIR_NAME + "/natives");
    }

    /** 返回解压后的 jars 目录 */
    public static File jarsDir(Context context) {
        return new File(context.getFilesDir(), DIR_NAME + "/jars");
    }

    /**
     * 返回解压后的 JNA native 目录（供 -Djna.boot.library.path 用）。
     * ★ 关键：MC 1.20.6/1.21 用 jna-5.14.0，而 APK jniLibs 里的是 5.1.0（v1.0.9 时代），
     * 版本不匹配会让 com.sun.jna.Native.<clinit> 失败 → oshi/LinuxOperatingSystem 初始化崩。
     * 所以高版本用这里解压出来的 5.14.0（从 Maven 的 jna-5.14.0.aar 提取）。
     */
    public static File jnaDir(Context context) {
        return new File(context.getFilesDir(), DIR_NAME + "/jna");
    }

    /**
     * 判断该版本是否需要 LWJGL 3.3.3（读版本 json 的 libraries 声明）。
     * 只有 1.20.5+ 声明 org.lwjgl:lwjgl:3.3.x —— 老版本一律 false（零影响）。
     */
    public static boolean needs(String versionPath) {
        if (versionPath == null) return false;
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) return false;
            String content = readText(new FileInputStream(json));
            return content.contains("lwjgl/3.3.3") || content.contains("lwjgl/3.3.4")
                    || content.contains("lwjgl/3.3.5") || content.contains("lwjgl/3.4.0")
                    || content.contains("lwjgl/3.4.1") || content.contains("lwjgl/3.4.2");
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 把 assets 里的 3.3.3 jar + 当前 ABI 的 native 解压到私有目录。
     *
     * @return natives 目录（可直接用于 -Dorg.lwjgl.librarypath），失败返回 null
     */
    public static File prepare(Context context) {
        File natives = nativesDir(context);
        File jars = jarsDir(context);
        try {
            // 已解压过且完整 → 直接返回（assets 更新由 app_runtime/version 触发外部重拷，这里简单判断）
            File stamp = new File(natives, ".ok");
            if (stamp.isFile() && jars.isFile()) {
                return natives;
            }
            deleteRecursively(natives);
            deleteRecursively(jars);
            natives.mkdirs();
            jars.mkdirs();

            AssetManager am = context.getAssets();
            // 1. jars
            for (String name : am.list(ASSET_ROOT + "/jars")) {
                if (name.endsWith(".jar")) copyAsset(am, ASSET_ROOT + "/jars/" + name, new File(jars, name));
            }
            // 2. natives（只解压当前 ABI 需要的；缺该 ABI 时退回 armeabi-v7a）
            String abi = abiDir();
            String[] list = am.list(ASSET_ROOT + "/natives/" + abi);
            if (list == null || list.length == 0) abi = "armeabi-v7a";
            for (String name : am.list(ASSET_ROOT + "/natives/" + abi)) {
                if (name.endsWith(".so")) copyAsset(am, ASSET_ROOT + "/natives/" + abi + "/" + name, new File(natives, name));
            }
            // ★ 同时解压 JNA 5.14.0（高版本专用，见 jnaDir 注释）
            File jnaDir = jnaDir(context);
            deleteRecursively(jnaDir);
            jnaDir.mkdirs();
            for (String name : am.list(ASSET_ROOT + "/jna/" + abi)) {
                if (name.endsWith(".so")) copyAsset(am, ASSET_ROOT + "/jna/" + abi + "/" + name, new File(jnaDir, name));
            }
            new File(natives, ".ok").createNewFile();
            sPrepared = true;
            return natives;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /** 3.3.3 的 jar classpath（以 ':' 分隔），失败返回空串 */
    public static String jarsClassPath(Context context) {
        StringBuilder sb = new StringBuilder();
        File[] files = jarsDir(context).listFiles();
        if (files != null) {
            java.util.Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
            for (File f : files) {
                if (f.getName().endsWith(".jar")) sb.append(f.getAbsolutePath()).append(':');
            }
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    // ---------------- 内部工具 ----------------

    private static String abiDir() {
        int arch = Architecture.getRuntimeArchitecture();
        if (arch == Architecture.ARCH_ARM) return "armeabi-v7a";
        if (arch == Architecture.ARCH_ARM64) return "arm64-v8a";
        if (arch == Architecture.ARCH_X86) return "x86";
        return "x86_64";
    }

    private static void copyAsset(AssetManager am, String assetPath, File dst) throws Exception {
        File parent = dst.getParentFile();
        if (parent != null && !parent.isFile()) parent.mkdirs();
        try (InputStream in = am.open(assetPath);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    private static String readText(InputStream in) throws Exception {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            in.close();
        }
    }

    private static void deleteRecursively(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursively(c);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }

    // ============ 渲染器智能选择 ============

    private static Boolean sZinkUsable = null;

    /**
     * ★★★ 运行时检测 zink 渲染链是否真正可用。
     * zink = libglxshim.so → libEGL_mesa.so → libglapi/libzink_dri。
     * 只检查"文件存在"不够（模拟器上文件在但架构不匹配），
     * 这里直接 System.load 试加载 —— 加载不上就说明这套链路用不了。
     * 结果缓存（进程内只测一次）。
     */
    public static synchronized boolean isZinkUsable(Context context) {
        if (sZinkUsable != null) return sZinkUsable;
        String dir = context.getApplicationInfo().nativeLibraryDir;
        try {
            System.load(dir + "/libglxshim.so");
        } catch (Throwable ignored) {
        }
        try {
            System.load(dir + "/libEGL_mesa.so");
            System.load(dir + "/libzink_dri.so");
            sZinkUsable = Boolean.TRUE;
        } catch (Throwable t) {
            sZinkUsable = Boolean.FALSE;
        }
        return sZinkUsable;
    }

    /**
     * ★★★ 渲染器选择（尊重玩家意图优先）：
     * <ol>
     *   <li><b>玩家显式选择了非 zink 渲染器</b>（mg / opengles2 / opengles3 / opengles3_virgl /
     *       vulkan_zink 等，包括外部导入的 MobileGlues 这类）→ <b>原样尊重</b>，绝不改写；</li>
     *   <li>玩家选的是 <b>zink</b>（或没选/auto）→ 运行时检测 zink 链路
     *       （glxshim→EGL_mesa→zink_dri）能否真正加载：
     *       能 → zink（桌面 GL 4.6，高版本与老版本通吃）；
     *       不能 → 回退 gl4es（老版本可用；高版本会走不通，提示玩家换渲染器）。</li>
     * </ol>
     * 说明：长按启动可自选渲染器，玩家选了 mg 等外部渲染器时必须保留。
     */
    public static String pickRenderer(Context context, String userRenderer) {
        // 1. 玩家显式选择（非 zink）→ 原样尊重
        if (userRenderer != null && !userRenderer.isEmpty()
                && !"zink".equals(userRenderer)
                && !"opengles3_desktopgl_zink_kopper".equals(userRenderer)) {
            return userRenderer;
        }
        // 2. zink / auto → 可用性检测
        if (isZinkUsable(context)) return "opengles3_desktopgl_zink_kopper";
        return "opengles3";
    }

    /** zink 不可用时的最终回退（老版本 gl4es；高版本也没别的选择）*/
    public static String fallbackRenderer() {
        return "opengles3";
    }

    /**
     * ★★★ 1.1.0：判断版本是否为 LWJGL 2 时代（b1.x / 远古 / 1.0~1.6 / 1.7.x）。
     * 判据：该版本 json 里声明了 org.lwjgl:lwjgl:2.9.x（或 lwjgl_util:2.9.x）。
     * 这类版本需要 lwjgl.jar / lwjgl_util.jar 提供 org.lwjgl.opengl.Display 等类，
     * 否则 NoClassDefFoundError（真机 b1.7.3 实测）。
     */
    public static boolean needsLwjgl2(String versionPath) {
        if (versionPath == null) return false;
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) return false;
            String content = readText(new FileInputStream(json));
            return content.contains("lwjgl/2.9") || content.contains("lwjgl_util/2.9")
                    || content.contains(":lwjgl:2.") || content.contains(":lwjgl_util:2.")
                    || content.contains("lwjgl/2.8");
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * ★★★ 照抄 FCL（DefaultLauncher.setLwjglVersion）：
     * 版本 json 的 LWJGL 版本 < 3.0（即 2.9.x，b1.x/远古/1.7.x 等）时，
     * 需要用 FCL 的 LWJGLX 兼容层（lwjgl-lwjglx.jar）提供 org.lwjgl.opengl.Display 等 LWJGL2 API ——
     * lwjglx 把这些 API 桥接到 LWJGL3 + Pojav 渲染桥，**不碰 GLFW**（这正是 b1.7.3 黑屏的根因：
     * 原 Pojav lwjgl-glfw-classes.jar 里的 Display 会去初始化 GLFW 而失败）。
     */
    public static boolean needsLwjglX(String versionPath) {
        if (versionPath == null) return false;
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) return false;
            String content = readText(new FileInputStream(json));
            // 找 lwjgl 的版本号：org.lwjgl:lwjgl:2.9.1 / lwjgl/2.9.0 等形式
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:org\\.lwjgl:lwjgl:|lwjgl/)(\\d+)\\.(\\d+)").matcher(content);
            while (m.find()) {
                int major = Integer.parseInt(m.group(1));
                if (major < 3) return true;      // LWJGL 2.x → 需要 LWJGLX
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }
}
