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
            if (!com.qcl.launcher.launcher.launch.vulkan.VulkanChecker.check(ctx).isSupported()) {
                // 设备没有 Vulkan：不动，交给 VulkanCheckDialog 去解释，别把"提示"变成"崩溃"
                return "no-vulkan-support";
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
