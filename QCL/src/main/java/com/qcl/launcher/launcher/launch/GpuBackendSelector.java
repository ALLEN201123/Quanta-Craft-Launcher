package com.qcl.launcher.launcher.launch;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ★★★★★ 1.5.0：26.2+ 启动前，把 MC 的图形后端**显式指定为 Vulkan**。
 *
 * <p><b>为什么必须有它</b>（MuMu 上实测到的死循环）：
 * <pre>
 * 崩（GLES limit=0 除零 / 其它原因）
 *   → MC 判定"上次意外关闭"
 *   → 下次启动打印 "Detected unexpected shutdown during last game startup:
 *                    forcing preferred graphics API to OpenGL"
 *   → <b>强制降级 OpenGL</b>，不再尝试 Vulkan
 *   → OpenGL 路径在 gl4es 系上必然撞 minUniformOffsetAlignment=0 → 又崩
 *   → 无限循环，永远跑不起来
 * </pre>
 * 清 {@code crash-reports/} 只能解一时（MC 还有别的持久化标记），
 * <b>唯一稳定的办法是让 MC 别自己选</b> —— 把
 * {@code options.txt} 里的 {@code preferredGraphicsBackend} 从
 * {@code "default"} 写成 {@code "vulkan"}。
 *
 * <p><b>安全阀</b>：只有**设备确实能拿到 Vulkan**（{@code VulkanChecker.check(...).isSupported()}）
 * 时才强制；否则**保持原样**并让 QCL 的 Vulkan 检测去提示玩家。
 * 盲目强制会让无 Vulkan 的设备从"提示"变成"崩溃"，反而更糟。
 */
public final class GpuBackendSelector {

    private static final String KEY = "preferredGraphicsBackend";

    private GpuBackendSelector() {
    }

    /**
     * 若该版本需要 Vulkan 后端（26.2+）且设备支持 Vulkan，则把 options.txt 的
     * {@code preferredGraphicsBackend} 设为 {@code "vulkan"}。
     *
     * @return 实际做了什么：{@code "forced-vulkan"} / {@code "kept"} / {@code "no-vulkan-support"}
     */
    public static String ensureVulkanFor26(Context ctx, String versionName, String versionDir) {
        try {
            if (!needsVulkan(versionName)) {
                return "kept";
            }
            // ★★★★★ 1.5.0 修正：**自动钉 Vulkan 只认「系统属性里的真实版本号」**。
            //   之前用了 VulkanChecker 的（含"看到 libvulkan.so 就算支持"的兜底），
            //   在 MuMu 上误判成支持 → 自动钉 vulkan → 但 MC 自己又
            //   `resetting preferred graphics API to Default` 覆盖掉，
            //   而模拟器的 Vulkan 其实是能力不足的软实现 ⇒ 反而更糟。
            //   ⇒ 自动钉只在**属性明确报了 version** 时做；属性读不到的一律交给启动弹窗，
            //     由玩家点「强制 Vulkan 后启动」自己决定（那是知情选择，不是我们替他猜）。
            int raw = com.qcl.launcher.launcher.launch.vulkan.VulkanChecker
                    .systemReportedVersion(ctx);
            if (raw == 0) {
                return "no-confirmed-vulkan";
            }
            File opt = new File(versionDir, "options.txt");
            if (!opt.isFile()) {
                return "kept";
            }
            List<String> lines = readAll(opt);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                String l = lines.get(i);
                if (l.startsWith(KEY + ":")) {
                    if (!l.equals(KEY + ":\"vulkan\"")) {
                        lines.set(i, KEY + ":\"vulkan\"");
                        changed = true;
                    }
                    break;
                }
            }
            if (!changed) {
                // 字段不存在（老 options）→ 追加一条
                lines.add(KEY + ":\"vulkan\"");
                changed = true;
            }
            if (changed) {
                writeAll(opt, lines);
            }
            return changed ? "forced-vulkan" : "kept";
        } catch (Throwable t) {
            return "kept";
        }
    }

    /**
     * ★ 1.5.0：**无条件**把该版本的 {@code preferredGraphicsBackend} 钉成 {@code "vulkan"}。
     *
     * <p>供启动检查弹窗的「强制 Vulkan 后启动」使用 —— 玩家已经明确知情并选择了，
     * 这里不再做设备能力判断（哪怕系统属性没填也照钉）。
     *
     * @return "forced-vulkan" / "no-options.txt" / "failed"
     */
    public static String forceVulkan(Context ctx, String versionName) {
        try {
            String dir = versionDirOf(ctx, versionName);
            if (dir == null) {
                return "failed";
            }
            File opt = new File(dir, "options.txt");
            if (!opt.isFile()) {
                return "no-options.txt";
            }
            List<String> lines = readAll(opt);
            boolean changed = false;
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).startsWith(KEY + ":")) {
                    if (!lines.get(i).equals(KEY + ":\"vulkan\"")) {
                        lines.set(i, KEY + ":\"vulkan\"");
                        changed = true;
                    }
                    break;
                }
            }
            if (!changed) {
                lines.add(KEY + ":\"vulkan\"");
            }
            writeAll(opt, lines);
            return "forced-vulkan";
        } catch (Throwable t) {
            return "failed";
        }
    }

    /** 由版本名推出它的目录；找不到返回 null。 */
    private static String versionDirOf(Context ctx, String versionName) {
        try {
            if (versionName == null || versionName.isEmpty()) {
                return null;
            }
            java.io.File[] roots = {
                    new File(ctx.getExternalFilesDir(null) == null
                            ? ctx.getFilesDir().getParentFile() : ctx.getExternalFilesDir(null), "game"),
                    new File("/sdcard/QCL/.minecraft/versions"),
                    new File("/sdcard/HMCL/.minecraft/versions"),
            };
            for (java.io.File r : roots) {
                if (r == null) {
                    continue;
                }
                java.io.File c = new File(r, versionName);
                if (c.isDirectory()) {
                    return c.getAbsolutePath();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** 26.2 起才有 Vulkan 后端（与 VulkanRequirement.MIN_MC_VERSION 一致）。 */
    private static boolean needsVulkan(String versionName) {
        try {
            if (versionName == null) {
                return false;
            }
            return com.qcl.launcher.launcher.launch.vulkan.VulkanRequirement
                    .hasVulkanBackend(versionName);
        } catch (Throwable t) {
            return false;
        }
    }

    private static List<String> readAll(File f) {
        List<String> out = new ArrayList<String>();
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8));
            try {
                String l;
                while ((l = r.readLine()) != null) {
                    out.add(l);
                }
            } finally {
                r.close();
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static void writeAll(File f, List<String> lines) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String l : lines) {
                sb.append(l).append('\n');
            }
            Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
            try {
                w.write(sb.toString());
                w.flush();
            } finally {
                w.close();
            }
        } catch (Throwable ignored) {
        }
    }
}
