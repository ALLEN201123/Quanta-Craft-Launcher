package com.qcl.launcher.launcher.launch;

/**
 * ★★★ 1.1.0（方案与数据参考 FCL 的 Renderer / RendererManager）：
 * 渲染器注册表 —— 全称 / GL 库 / EGL 库 / **明确支持版本范围** / 默认项。
 *
 * <p>FCL 用 data class Renderer(name, des, glName, eglName, minMCver, maxMCver) 描述，
 * 启动前用 maxMCver 比较版本，不通过就弹「该渲染器不支持该游戏版本…是否继续？」。
 * 本类沿用同样思路，数据取 FCL RendererManager 的内置值。
 */
public final class RendererCompat {

    public static final class Info {
        public final String id;           // QCL 内部标识（写进设置的渲染器值）
        public final String name;         // 短名（参考 FCL name）
        public final String displayName;  // 全称（参考 FCL des，含 OpenGL 版本与限制说明）
        public final String glName;       // GL 库
        public final String eglName;      // EGL 库
        public final String minMcVer;     // 最低支持 MC 版本（""=不限）
        public final String maxMcVer;     // 最高支持 MC 版本（""=不限）
        public final String displayMax;   // 展示用最高版本（UI 显示）
        public final boolean builtin;     // 内置 or 外部
        public final boolean recommended; // 是否推荐

        Info(String id, String name, String displayName, String glName, String eglName,
             String minMcVer, String maxMcVer, String displayMax, boolean builtin, boolean recommended) {
            this.id = id; this.name = name; this.displayName = displayName;
            this.glName = glName; this.eglName = eglName;
            this.minMcVer = minMcVer; this.maxMcVer = maxMcVer; this.displayMax = displayMax;
            this.builtin = builtin; this.recommended = recommended;
        }

        /** 支持版本范围的文字描述（UI 用），如「支持 ≤ 1.21.4（含远古版本）」 */
        public String supportRangeText() {
            if (minMcVer.isEmpty() && maxMcVer.isEmpty()) return "支持所有版本";
            if (minMcVer.isEmpty()) return "支持 ≤ " + displayMax + "（含远古版本）";
            if (maxMcVer.isEmpty()) return "支持 ≥ " + minMcVer;
            return "支持 " + minMcVer + " ~ " + displayMax;
        }

        /** UI 上显示的一行文字：全称（支持 MC x ~ y） */
        public String uiLabel() {
            String range;
            if (minMcVer.isEmpty() && maxMcVer.isEmpty()) range = "支持所有版本";
            else if (minMcVer.isEmpty()) range = "支持 ≤ " + displayMax + "（含远古版本）";
            else if (maxMcVer.isEmpty()) range = "支持 ≥ " + minMcVer;
            else range = "支持 " + minMcVer + " ~ " + displayMax;
            return displayName + "\n（" + range + "）" + (recommended ? " ★推荐" : "");
        }
    }

    /**
     * 注册表（顺序即 UI 顺序）—— 与 FCL RendererManager 的 6 个内置渲染器一一对应：
     * <pre>
     *   FCL 名              glName              eglName         maxMCver
     *   Holy-GL4ES          libgl4es_114.so     libEGL.so       1.21.4
     *   VirGLRenderer       libOSMesa_81.so     libEGL.so       26.3-snapshot-3
     *   VGPU                libvgpu.so          libEGL.so       1.16.5
     *   Zink                libglxshim.so       libEGL_mesa.so  26.3-snapshot-3
     *   Freedreno           libOSMesa_8.so      libEGL.so       26.3-snapshot-3
     *   Krypton Wrapper     libng_gl4es.so      libEGL.so       26.3-snapshot-3
     * </pre>
     * 另加 MobileGlues（外部渲染器，FCL 通过 RendererPlugin 插件加载，QCL 预留）。
     */
    public static final Info[] ALL = new Info[]{
            // id, 短名, 全称, GL库, EGL库, minVer, maxVer, displayMax, builtin, recommended
            new Info("opengles2", "Holy-GL4ES", "Holy GL4ES (OpenGL 2.1)",
                    "libgl4es_114.so", "libEGL.so", "", "1.21.4", "1.21.4", true, true),
            new Info("ng_gl4es", "Krypton Wrapper", "Krypton Wrapper (OpenGL 3.1+, 全版本通吃)",
                    "libng_gl4es.so", "libEGL.so", "", "26.3", "26.2", true, true),
            new Info("zink", "Zink", "Kopper Zink (OpenGL 4.6, Mesa zink on Vulkan)",
                    "libglxshim.so", "libEGL_mesa.so", "", "26.3", "26.2", true, true),
            new Info("opengles3_virgl", "VirGLRenderer", "VirGLRenderer (OpenGL 4.3, Mesa 软渲染)",
                    "libOSMesa_81.so", "libEGL.so", "", "26.3", "26.2", true, true),
            new Info("opengles3_virgl_osmesa8", "Freedreno", "Freedreno (OpenGL 4.6, 仅高通 adreno616-a660)",
                    "libOSMesa_8.so", "libEGL.so", "", "26.3", "26.2", true, false),
            new Info("opengles3_vgpu", "VGPU", "VGPU (OpenGL 2.1+)",
                    "libvgpu.so", "libEGL.so", "", "1.16.5", "1.16.5", true, false),
            new Info("mg", "MobileGlues", "MobileGlues (外部渲染器，需自行导入 libMobileGlues.so)",
                    "libMobileGlues.so", "libEGL.so", "", "26.3", "26.2", false, false),
    };

    /** 默认渲染器（新装用户）*/
    public static String defaultRendererId() {
        // ★ 默认 Holy GL4ES —— v1.0.9 的默认，老版本（LWJGL2 时代）实测最稳。
        // Krypton(ng_gl4es)/Zink 面向高版本（GL 3.1+），老版本选它们会崩 —— 玩家可在长按列表里自选。
        return "opengles2";
    }

    /**
     * ★★★ 自动选择的核心：把 "auto" 解析成具体渲染器。
     * 策略（从最优先到兜底）：
     * <ol>
     *   <li>zink 链路（glxshim→EGL_mesa→zink_dri）能加载 → 用 zink（桌面 GL 4.6，高低通吃）；</li>
     *   <li>否则用 Krypton Wrapper（libng_gl4es.so，OpenGL 3.1+，全版本通吃，兼容性最好）；</li>
     *   <li>再不行退回 Holy GL4ES（libgl4es_114.so，最稳）。</li>
     * </ol>
     * 这样：模拟器/x86 自动避开跑不了的 zink；真机自动享受 zink 性能。玩家无需关心细节。
     */
    public static String resolveAuto(android.content.Context context, boolean highVer) {
        try {
            String dir = context.getApplicationInfo().nativeLibraryDir;
            // 1) zink 可用？
            if (Lwjgl333Helper.isZinkUsable(context)) {
                return "zink";
            }
            // 2) Krypton 存在？
            if (new java.io.File(dir, "libng_gl4es.so").isFile()) {
                return "ng_gl4es";
            }
            // 3) 兜底
            return "opengles2";
        } catch (Throwable ignored) {
            return "opengles2";
        }
    }

    public static Info find(String id) {
        for (Info i : ALL) if (i.id.equals(id)) return i;
        return null;
    }

    private RendererCompat() {}

    /** MC 版本号 → 可比较整数（1.21.4 → 12104；26.3 → 2603）*/
    public static long parseVer(String v) {
        if (v == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(v.trim());
        if (!m.find()) return 0;
        long a = Long.parseLong(m.group(1));
        long b = Long.parseLong(m.group(2));
        long c = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;
        return a * 10000 + b * 100 + c;
    }

    /** 远古版本（b1.x / a1.x / 1.0~1.6 等）版本号小、要求低 */
    public static boolean isAncient(String mcVer) {
        if (mcVer == null) return false;
        String s = mcVer.toLowerCase();
        return s.startsWith("b") || s.startsWith("a") || s.startsWith("c")
                || s.startsWith("inf") || s.startsWith("rd") || s.startsWith("pre");
    }

    /** 渲染器是否支持该 MC 版本 */
    public static boolean supports(String rendererId, String mcVer) {
        if ("auto".equals(rendererId)) return true;   // 自动选择永远合适
        Info info = find(rendererId);
        if (info == null) return true;       // 未知/外部插件 → 不拦
        if (!info.builtin) return true;      // 外部渲染器（mg）→ 不拦
        if (isAncient(mcVer)) return true;   // 远古版本要求低
        long ver = parseVer(mcVer);
        if (ver == 0) return true;
        if (!info.maxMcVer.isEmpty() && ver > parseVer(info.maxMcVer)) return false;
        if (!info.minMcVer.isEmpty() && ver < parseVer(info.minMcVer)) return false;
        return true;
    }

    /** 不兼容时的提示文案（null = 无需提示）。文案参考 FCL 的 message_check_renderer */
    public static String warningOf(String rendererId, String mcVer) {
        if (supports(rendererId, mcVer)) return null;
        Info info = find(rendererId);
        String name = info != null ? info.displayName : rendererId;
        String max = info != null ? info.displayMax : "?";
        return "当前渲染器「" + name + "」不支持该游戏版本（" + mcVer + "）。\n\n"
                + "该渲染器最高支持到 " + max + "，继续使用可能导致画面错误、贴图错乱或游戏崩溃！\n\n"
                + "是否仍要使用这个渲染器？";
    }
}
