package net.kdt.pojavlaunch.utils;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ★★★★★ 1.5.0：崩溃诊断抓取与展示。
 *
 * <p><b>要解决的问题</b>（用户原话）：「他都崩了，我咋复制日志给你？」
 * 26.3 这类问题必须看游戏日志才能定位，但崩溃后游戏窗口和日志窗都没了，
 * 玩家只剩一个回不去的桌面 —— 拿不到任何日志，开发者只能靠猜。
 *
 * <p><b>做法</b>：
 * <ol>
 *   <li>{@link #capture} —— 游戏进程**异常退出那一刻**调用（日志文件此时还完整），
 *       从游戏日志里挑出关键行（图形后端 / Vulkan / 崩溃异常栈 / 渲染桥），
 *       连同设备信息与「实际 Vulkan 来源」一起写到
 *       {@code <debug 目录>/crash_diag.txt}；</li>
 *   <li>{@link #consumePending} —— 下次打开启动器时若有新诊断，返回其文本并清掉标记，
 *       由 UI 弹窗展示并提供「一键复制」；</li>
 *   <li>展示与复制由 QCL 模块的弹窗负责（本类在 PojavLauncher，不能编译期依赖 QCL）。</li>
 * </ol>
 *
 * <p>★ 所有方法都必须**永不抛异常** —— 诊断是辅助功能，绝不能把启动器搞崩。
 */
public final class CrashDiag {

    /** 已展示过的诊断内容的指纹（避免同一个诊断反复弹）。 */
    private static final String PREF_NAME = "launcher";
    private static final String PREF_KEY_SHOWN = "crash_diag_shown";

    /** 只保留日志尾部这么多字节，够定位又不至于把弹窗撑爆。 */
    private static final int MAX_TAIL_BYTES = 96 * 1024;

    /** 关键词过滤：这些行才是真正决定"为什么崩"的。 */
    private static final String[] KEY_LINES = {
            "Graphics Backend", "Using graphics backend", "Backend library",
            "SDL video driver", "AdrenoSupp", "OSMDroid", "Vulkan", "vulkan",
            "Turnip", "turnip", "VULKAN_PTR", "DRIVER_PATH",
            "Exception", "ERROR", "FATAL", "Caused by", "at net.minecraft",
            "at com.mojang", "Render thread", "Thread",
            "OpenGL", "GL4ES", "gl4es", "libmobileglues", "EGLBridge", "POJAV_RENDERER",
            "SDL_OPENGL_LIBRARY", "Minecraft", "exitCode"
    };

    private CrashDiag() {
    }

    /** 游戏日志文件位置（与 QCL 现有约定一致）。 */
    private static File logFile(Context ctx) {
        File dir = ctx.getExternalFilesDir("debug");
        if (dir == null) {
            dir = ctx.getFilesDir();
        }
        return new File(dir, "pojav_latest_log.txt");
    }

    private static File diagFile(Context ctx) {
        File dir = ctx.getExternalFilesDir("debug");
        if (dir == null) {
            dir = ctx.getFilesDir();
        }
        return new File(dir, "crash_diag.txt");
    }

    /**
     * 游戏异常退出时调用：抓一份诊断写到 crash_diag.txt。
     *
     * @return 抓到的诊断文本；失败返回空串（绝不抛异常）
     */
    public static String capture(Context ctx) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("==== QCL 崩溃诊断 ").append(System.currentTimeMillis()).append(" ====\n");
            sb.append("设备: ").append(android.os.Build.MANUFACTURER)
                    .append(' ').append(android.os.Build.MODEL)
                    .append(" / Android ").append(android.os.Build.VERSION.RELEASE)
                    .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
            sb.append("ABI: ").append(android.os.Build.SUPPORTED_ABIS).append('\n');
            try {
                // ★ 关键：Vulkan 到底是「自带 Turnip」「系统」还是「压根没拿到」。
                //   这是判断 26.3 能不能跑的决定性信息（OpenGL 回落 = 必崩）。
                Class<?> c = Class.forName("com.qcl.launcher.launcher.launch.vulkan.VulkanChecker");
                String src = (String) c.getMethod("getVulkanSourceSafe").invoke(null);
                sb.append("Vulkan 实际来源: ").append(src).append('\n');
            } catch (Throwable t) {
                sb.append("Vulkan 实际来源: (取不到, ").append(t.getClass().getSimpleName()).append(")\n");
            }
            sb.append("系统 Vulkan 属性: version=")
                    .append(systemProp(ctx, "android.hardware.vulkan.version"))
                    .append(" level=").append(systemProp(ctx, "android.hardware.vulkan.level"))
                    .append('\n');
            sb.append("---- 游戏日志关键行 ----\n");
            String tail = readTail(logFile(ctx));
            sb.append(filter(tail));
            if (tail.trim().isEmpty()) {
                sb.append("(游戏日志为空或不可读)\n");
            }
        } catch (Throwable ignored) {
            return "";
        }
        String text = sb.toString();
        try {
            FileOutputStream fos = new FileOutputStream(diagFile(ctx));
            fos.write(text.getBytes(StandardCharsets.UTF_8));
            fos.close();
            // 标记为「未展示」
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_KEY_SHOWN).apply();
        } catch (Throwable ignored) {
            // 写不进去就算了
        }
        return text;
    }

    private static String systemProp(Context ctx, String key) {
        // ★ android.os.SystemProperties 是隐藏 API，只能反射；
        //   QCL 里没有现成封装（别去调 SystemProperties_get，那个方法不存在）。
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get = sp.getMethod("get", String.class, String.class);
            Object v = get.invoke(null, key, "");
            return v == null ? "?" : v.toString();
        } catch (Throwable t) {
            try {
                Class<?> sp = Class.forName("android.os.SystemProperties");
                java.lang.reflect.Method get = sp.getMethod("get", String.class);
                Object v = get.invoke(null, key);
                return v == null ? "?" : v.toString();
            } catch (Throwable t2) {
                return "?";
            }
        }
    }

    /** 读文件尾部（最多 MAX_TAIL_BYTES）。 */
    private static String readTail(File f) {
        if (f == null || !f.isFile()) {
            return "";
        }
        try {
            long len = f.length();
            int want = (int) Math.min(len, MAX_TAIL_BYTES);
            FileInputStream fis = new FileInputStream(f);
            try {
                fis.skip(len - want);
                byte[] buf = new byte[want];
                int n = fis.read(buf);
                if (n <= 0) {
                    return "";
                }
                return new String(buf, 0, n, StandardCharsets.UTF_8);
            } finally {
                fis.close();
            }
        } catch (Throwable t) {
            return "";
        }
    }

    /** 只保留含关键关键词的行；一条都没有就返回尾部最后 60 行（宁可多给也别空手）。 */
    private static String filter(String tail) {
        if (tail == null || tail.isEmpty()) {
            return "";
        }
        List<String> hit = new ArrayList<String>();
        for (String line : tail.split("\n")) {
            for (String k : KEY_LINES) {
                if (line.contains(k)) {
                    hit.add(line);
                    break;
                }
            }
        }
        if (!hit.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int start = Math.max(0, hit.size() - 400);
            for (int i = start; i < hit.size(); i++) {
                sb.append(hit.get(i)).append('\n');
            }
            return sb.toString();
        }
        // 兜底：最后 60 行
        String[] ls = tail.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, ls.length - 60); i < ls.length; i++) {
            sb.append(ls[i]).append('\n');
        }
        return sb.toString();
    }

    /**
     * 启动器启动时调用：若有一次**尚未展示过**的诊断，返回其文本并标记已展示；
     * 没有就返回 null。
     */
    public static String consumePending(Context ctx) {
        try {
            File f = diagFile(ctx);
            if (!f.isFile()) {
                return null;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(f);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = fis.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
            } finally {
                fis.close();
            }
            String text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            String sig = String.valueOf(text.hashCode());
            String shown = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .getString(PREF_KEY_SHOWN, "");
            if (sig.equals(shown)) {
                return null;
            }
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(PREF_KEY_SHOWN, sig).apply();
            return text;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 清掉已有诊断（玩家点了"不再提示"时用）。 */
    public static void clear(Context ctx) {
        try {
            File f = diagFile(ctx);
            if (f.isFile()) {
                f.delete();
            }
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().remove(PREF_KEY_SHOWN).apply();
        } catch (Throwable ignored) {
        }
    }
}
