package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SimpleFPEWrapper 兼容层（照 FCL 1.3.4.4 / #1983）。
 *
 * <p><b>SFPEW 不是独立渲染器，而是叠加在真实后端之上的 OpenGL 固定管线（FPE）兼容包装层。</b>
 * 它补的是「固定管线（glBegin/glEnd、glMatrixMode、即时模式）」这一层——现代 GLES 驱动和
 * MobileGlues 都不提供，而 Minecraft 1.16 及更早（尤其 1.8.9、b1.x 远古版）大量使用。
 *
 * <p><b>为什么这样就能让老版本用上 MobileGlues：</b>
 * MG 本身是 GLES 3.x 实现、没有 FPE，所以此前 QCL 把 mg 的最低版本卡在 1.17。
 * 叠加 SFPEW 后，游戏经 {@code libSimpleFPEWrapper.so} 解析 GL 符号（FPE 调用被翻译成
 * GLES 调用），而 MG 只承担 EGL 与驱动 —— 于是 1.17 的下限不再成立，
 * FCL 实测 1.8.9 可跑。
 *
 * <p><b>与 FCL 一致的取舍：</b>
 * <ul>
 *   <li>内置渲染器行为已知，按硬编码规则判定（见 {@link #isLayerEnabledForBuiltin}）：</li>
 *   <li>自带 FPE/完整 GL 的（Krypton Wrapper / Holy GL4ES / VGPU / Zink）**不叠加**
 *       —— 它们本来就提供固定管线，再套一层只是白白多一次透传开销；</li>
 *   <li>其余内置渲染器（MobileGlues 等）**默认叠加**；</li>
 *   <li>外部插件渲染器行为未知，由「高级设置」里的开关控制，**默认关闭**（用户显式开启）。</li>
 * </ul>
 *
 * <p>⚠️ 装了自带 FPE 实现的模组（Angelica / Actinium）时必须整层跳过 —— 这类模组自己接管
 * 旧版渲染链，套层反而冲突。该判定由启动链在扫描 mods 后调用 {@link #setFpeEmulatorModPresent}。
 */
public final class SfpewLayer {

    /** 兼容层 so 文件名（随 APK 打包在 nativeLibraryDir）。 */
    public static final String LIB_NAME = "libSimpleFPEWrapper.so";

    private static final String PREF_NAME = "launcher";
    private static final String KEY_PLUGIN_LAYER = "sfpew_plugin_layer";
    private static final String KEY_RELAXED_CONTEXT = "sfpew_relaxed_context";

    /**
     * 自带 FPE / 完整 GL 的内置渲染器：叠加层对其硬编码关闭。
     * <p>照 FCL RendererPlugin 的 {@code FPE_CAPABLE_BUILTIN_IDS}（上游 AAMC 同款规则）。
     * <p>注意 {@code mg} **不在**此列 —— 正是要给它补 FPE。
     */
    private static final java.util.Set<String> FPE_CAPABLE_BUILTIN_IDS =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "ng_gl4es",        // Krypton Wrapper —— 自带 FPE
                    "opengles2",       // Holy-GL4ES —— gl4es 自带 FPE
                    "opengles3_vgpu",  // VGPU —— 自带 FPE
                    "zink"             // Kopper Zink —— 完整 GL
            ));

    /** 自实现 FPE 模拟的模组 id（照 FCL 的 FPE_EMULATOR_MOD_IDS）。 */
    private static final java.util.Set<String> FPE_EMULATOR_MOD_IDS =
            new java.util.HashSet<String>(java.util.Arrays.asList("angelica", "actinium"));

    /** 本次启动是否装了自实现 FPE 的模组（启动链 mods 扫描后写入，仅进程内有效）。 */
    private static volatile boolean fpeEmulatorModPresent = false;

    private SfpewLayer() {
    }

    /**
     * 启动链在扫描 mods 之后调用：是否装了自带 FPE 实现的模组。
     *
     * @param modIds 已启用的模组 id 集合，可为 null
     */
    public static void setFpeEmulatorModPresent(java.util.Collection<String> modIds) {
        boolean present = false;
        if (modIds != null) {
            for (String id : modIds) {
                if (id != null && FPE_EMULATOR_MOD_IDS.contains(id.toLowerCase())) {
                    present = true;
                    break;
                }
            }
        }
        fpeEmulatorModPresent = present;
    }

    /** 手动置位（便于启动链按单条探测结果设置）。 */
    public static void setFpeEmulatorModPresent(boolean present) {
        fpeEmulatorModPresent = present;
    }

    public static boolean isFpeEmulatorModPresent() {
        return fpeEmulatorModPresent;
    }

    /**
     * 该渲染器本次启动是否叠加 SFPEW 兼容层 —— 启动链注入 env 的唯一依据。
     *
     * <p>判定顺序（照 FCL {@code isSfpewLayerEnabled}）：
     * <ol>
     *   <li>装了自实现 FPE 的模组 → <b>整层跳过</b>（含内置渲染器）；</li>
     *   <li>内置渲染器（看 {@link RendererCompat.Info#builtin}）→ 硬编码：自带 FPE 的关闭，其余开启；</li>
     *   <li>外部插件渲染器 → 读「高级设置」开关，<b>默认关闭</b>。</li>
     * </ol>
     *
     * @param rendererId 渲染器 id；null / "auto" 视为内置默认渲染器（QCL 的 opengles3 → 开启）
     */
    public static boolean isLayerEnabled(Context context, String rendererId) {
        if (fpeEmulatorModPresent) {
            return false;
        }
        if (rendererId == null || "auto".equals(rendererId)) {
            // 内置默认（opengles3）不在 ALL 表里：它没有 FPE，需要叠加
            return true;
        }
        RendererCompat.Info info = RendererCompat.find(rendererId);
        if (info != null && info.builtin) {
            return isLayerEnabledForBuiltin(info.id);
        }
        // 外部插件渲染器（含注册表里查不到的）：由用户显式开关控制
        return isPluginLayerEnabled(context);
    }

    /** 内置渲染器的硬编码判定：不在「自带 FPE」名单里就叠加。 */
    public static boolean isLayerEnabledForBuiltin(String id) {
        return id != null && !FPE_CAPABLE_BUILTIN_IDS.contains(id);
    }

    /** 「高级设置」里的插件渲染器开关（默认关闭）。 */
    public static boolean isPluginLayerEnabled(Context context) {
        return prefs(context).getBoolean(KEY_PLUGIN_LAYER, false);
    }

    public static void setPluginLayerEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_PLUGIN_LAYER, enabled).apply();
    }

    /**
     * SFPEW_RELAXED_CONTEXT：假定游戏每线程终生只用一个 GL 上下文，
     * 跳过每次调用的上下文核查（<b>默认关闭 = 严格模式</b>，照 FCL 1.3.4.4 的默认值）。
     *
     * <p>⚠️ 注意与 FCL 1.3.4.3 的区别：**1.3.4.3 里这一项默认开启**，1.3.4.4 随
     * 「SFPEW 自动选择 EGL Context 模式」改成了**默认关闭**（严格模式），
     * 只有在确认游戏线程-上下文使用方式不会出事时才手动开启。
     * 本实现跟 1.3.4.4。
     */
    public static boolean isRelaxedContextEnabled(Context context) {
        return prefs(context).getBoolean(KEY_RELAXED_CONTEXT, false);
    }

    public static void setRelaxedContextEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_RELAXED_CONTEXT, enabled).apply();
    }

    /** 兼容层 so 是否真的在 APK nativeLibraryDir 里（不给玩家一个注定失败的开关）。 */
    public static boolean isAvailable(Context context) {
        if (context == null) {
            return false;
        }
        try {
            String nativeDir = context.getApplicationInfo().nativeLibraryDir;
            if (nativeDir == null || nativeDir.isEmpty()) {
                return false;
            }
            java.io.File dir = new java.io.File(nativeDir);
            java.io.File[] files = dir.listFiles();
            if (files == null) {
                return false;
            }
            for (java.io.File f : files) {
                if (f != null && f.isFile() && f.getName().equalsIgnoreCase(LIB_NAME)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
