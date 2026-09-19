package com.qcl.launcher.launcher.launch;

import android.content.Context;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public final class RendererCompat {
    /**
     * 外部渲染器库是否就位。
     *
     * <p>MobileGlues（mg）是**外部渲染器**：它的 {@code libmobileglues.so} 不进 APK，
     * 由玩家自行下载后放进 {@code <游戏目录>/renderer/mg/}（该目录会在启动时挂进
     * java.library.path，见 PojavLauncher）。因此不能用 APK 的 nativeLibraryDir 判断，
     * 否则列表里永远看不到 mg（玩家反馈"检测不到"的根因）。
     *
     * @param id          渲染器 id
     * @param versionPath 当前版本目录（{@code <gameDir>/versions/<ver>}），用于推导 gameDir
     * @param gameDir     已知的游戏目录，可为 null
     */
    /** 由版本目录推出游戏目录：{@code <gameDir>/versions/<ver>} → {@code <gameDir>}。 */
    public static String gameDirOf(String versionPath) {
        try {
            java.io.File v = new java.io.File(versionPath == null ? "" : versionPath);
            java.io.File versions = v.getParentFile();
            if (versions != null && versions.getParentFile() != null) {
                return versions.getParentFile().getAbsolutePath();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * 在**已安装的应用**里找提供该渲染器库的插件（对齐 FCL 的插件渲染器语义：
     * FCL 的 RendererPlugin 会把配置里的 {@code **|} 前缀替换为「插件的 nativeLibraryDir」，
     * 也就是说 MobileGlues 这类外部渲染器的 {@code libmobileglues.so} 来自它自己的插件 APK，
     * 而不是让玩家手工拷贝）。
     *
     * <p>实现上不依赖具体包名：查询所有带启动图标的应用，谁的 nativeLibraryDir 下有目标 so 就用谁。
     * （Android 11+ 需要在 manifest 里声明 {@code <queries>}，见 AndroidManifest.xml。）
     *
     * @return 命中则返回该插件应用的 nativeLibraryDir，否则 null
     */
    /**
     * 在目录里查找目标 so —— **忽略大小写**（MobileGlues 官方包内是 {@code libmobileglues.so}
     * 全小写，历史上代码里写成 {@code libMobileGlues.so} 驼峰，在大小写敏感的文件系统上直接判定为不存在）。
     */
    private static boolean hasLibIgnoreCase(java.io.File dir, String libName) {
        if (dir == null || libName == null) {
            return false;
        }
        java.io.File exact = new java.io.File(dir, libName);
        if (exact.isFile() && exact.length() > 0L) {
            return true;
        }
        java.io.File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (java.io.File c : children) {
            if (c.isFile() && c.length() > 0L && c.getName().equalsIgnoreCase(libName)) {
                return true;
            }
        }
        return false;
    }

    public static String findInstalledRendererLibDir(android.content.Context context, String id) {
        Info info = find(id);
        if (context == null || info == null || info.glName == null || info.glName.isEmpty()) {
            return null;
        }
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            // 与 FCL PluginManager 的扫描保持一致：裸 ACTION_MAIN（不限启动图标），范围更宽，
            // 以免某些「无启动图标但声明 MAIN」的插件应用被漏掉。
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            java.util.List<android.content.pm.ResolveInfo> list =
                    pm.queryIntentActivities(intent, 0);
            if (list == null) {
                return null;
            }
            for (android.content.pm.ResolveInfo ri : list) {
                try {
                    android.content.pm.ApplicationInfo ai = ri.activityInfo.applicationInfo;
                    if (ai == null || ai.nativeLibraryDir == null) {
                        continue;
                    }
                    if (hasLibIgnoreCase(new java.io.File(ai.nativeLibraryDir), info.glName)) {
                        return ai.nativeLibraryDir;
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * 解析外部渲染器的库目录，按优先级：
     * <ol>
     *   <li>已安装的插件应用（如 MobileGlues Plugin）的 nativeLibraryDir —— 与 FCL 一致；</li>
     *   <li>{@code <gameDir>/renderer/<id>/}（手工放置的兜底用法）。</li>
     * </ol>
     *
     * @return 命中目录；都没有则 null
     */
    public static String resolveRendererLibDir(android.content.Context context, String id,
                                              String versionPath, String gameDir) {
        String fromPlugin = findInstalledRendererLibDir(context, id);
        if (fromPlugin != null) {
            return fromPlugin;
        }
        Info info = find(id);
        if (info == null || info.glName == null || info.glName.isEmpty()) {
            return null;
        }
        java.util.List<java.io.File> roots = new java.util.ArrayList<java.io.File>();
        if (gameDir != null && !gameDir.isEmpty()) {
            roots.add(new java.io.File(gameDir));
        }
        String gd = gameDirOf(versionPath);
        if (gd != null && !gd.isEmpty()) {
            roots.add(new java.io.File(gd));
        }
        for (java.io.File root : roots) {
            java.io.File dir = new java.io.File(root, "renderer/" + id);
            if (hasLibIgnoreCase(dir, info.glName)) {
                return dir.getAbsolutePath();
            }
        }
        return null;
    }

    public static boolean isExternalLibPresent(String id, String versionPath, String gameDir) {
        Info info = find(id);
        if (info == null || info.glName == null || info.glName.isEmpty()) {
            return false;
        }
        java.util.List<java.io.File> roots = new java.util.ArrayList<java.io.File>();
        if (gameDir != null && !gameDir.isEmpty()) {
            roots.add(new java.io.File(gameDir));
        }
        // 从 <gameDir>/versions/<ver> 往上一级推 gameDir
        if (versionPath != null && !versionPath.isEmpty()) {
            java.io.File v = new java.io.File(versionPath);
            java.io.File p1 = v.getParentFile();                 // versions
            java.io.File p2 = (p1 != null) ? p1.getParentFile() : null;   // gameDir
            if (p2 != null) {
                roots.add(p2);
            }
        }
        for (java.io.File root : roots) {
            if (hasLibIgnoreCase(new java.io.File(root, "renderer/" + id), info.glName)) {
                return true;
            }
        }
        return false;
    }

    public static final Info[] ALL = {new Info("opengles2", "Holy-GL4ES", "Holy GL4ES (OpenGL 2.1)", "libgl4es_114.so", "libEGL.so", "", "1.21.4", "1.21.4", true, true), new Info("ng_gl4es", "Krypton Wrapper", "Krypton Wrapper (OpenGL 3.1+, 全版本通吃)", "libng_gl4es.so", "libEGL.so", "", "26.3", "26.2", true, true), new Info("zink", "Zink", "Kopper Zink (OpenGL 4.6, Mesa zink on Vulkan)", "libglxshim.so", "libEGL_mesa.so", "", "26.3", "26.2", true, true), new Info("opengles3_virgl", "VirGLRenderer", "VirGLRenderer (OpenGL 4.3, Mesa 软渲染)", "libOSMesa_81.so", "libEGL.so", "", "26.3", "26.2", true, true), new Info("opengles3_virgl_osmesa8", "Freedreno", "Freedreno (OpenGL 4.6, 仅高通 adreno616-a660)", "libOSMesa_8.so", "libEGL.so", "", "26.3", "26.2", true, false), new Info("opengles3_vgpu", "VGPU", "VGPU (OpenGL 2.1+)", "libvgpu.so", "libEGL.so", "", "1.16.5", "1.16.5", true, false), new Info("mg", "MobileGlues", "MobileGlues (外部渲染器，需自行导入 libmobileglues.so)", "libmobileglues.so", "libEGL.so", "", "26.3", "26.2", false, false)};

    public static String defaultRendererId() {
        return "opengles2";
    }

    /* loaded from: classes2.dex */
    public static final class Info {
        public final boolean builtin;
        public final String displayMax;
        public final String displayName;
        public final String eglName;
        public final String glName;
        public final String id;
        public final String maxMcVer;
        public final String minMcVer;
        public final String name;
        public final boolean recommended;

        Info(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, boolean z, boolean z2) {
            this.id = str;
            this.name = str2;
            this.displayName = str3;
            this.glName = str4;
            this.eglName = str5;
            this.minMcVer = str6;
            this.maxMcVer = str7;
            this.displayMax = str8;
            this.builtin = z;
            this.recommended = z2;
        }

        public String supportRangeText() {
            return (this.minMcVer.isEmpty() && this.maxMcVer.isEmpty()) ? "支持所有版本" : this.minMcVer.isEmpty() ? "支持 ≤ " + this.displayMax + "（含远古版本）" : this.maxMcVer.isEmpty() ? "支持 ≥ " + this.minMcVer : "支持 " + this.minMcVer + " ~ " + this.displayMax;
        }

        public String uiLabel() {
            String str;
            if (this.minMcVer.isEmpty() && this.maxMcVer.isEmpty()) {
                str = "支持所有版本";
            } else if (this.minMcVer.isEmpty()) {
                str = "支持 ≤ " + this.displayMax + "（含远古版本）";
            } else {
                str = this.maxMcVer.isEmpty() ? "支持 ≥ " + this.minMcVer : "支持 " + this.minMcVer + " ~ " + this.displayMax;
            }
            return this.displayName + "\n（" + str + "）" + (this.recommended ? " ★推荐" : "");
        }
    }

    public static String resolveAuto(Context context, boolean z) {
        return Lwjgl333Helper.isZinkUsable(context) ? "zink" : new File(context.getApplicationInfo().nativeLibraryDir, "libng_gl4es.so").isFile() ? "ng_gl4es" : "opengles2";
    }

    public static Info find(String str) {
        for (Info info : ALL) {
            if (info.id.equals(str)) {
                return info;
            }
        }
        return null;
    }

    private RendererCompat() {
    }

    public static long parseVer(String str) {
        if (str == null) {
            return 0L;
        }
        Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(str.trim());
        if (!matcher.find()) {
            return 0L;
        }
        return (Long.parseLong(matcher.group(1)) * 10000) + (Long.parseLong(matcher.group(2)) * 100) + (matcher.group(3) != null ? Long.parseLong(matcher.group(3)) : 0L);
    }

    public static boolean isAncient(String str) {
        if (str == null) {
            return false;
        }
        String lowerCase = str.toLowerCase();
        return lowerCase.startsWith("b") || lowerCase.startsWith("a") || lowerCase.startsWith("c") || lowerCase.startsWith("inf") || lowerCase.startsWith("rd") || lowerCase.startsWith("pre");
    }

    public static boolean supports(String str, String str2) {
        Info find;
        if ("auto".equals(str) || (find = find(str)) == null || !find.builtin || isAncient(str2)) {
            return true;
        }
        long parseVer = parseVer(str2);
        if (parseVer == 0) {
            return true;
        }
        if (find.maxMcVer.isEmpty() || parseVer <= parseVer(find.maxMcVer)) {
            return find.minMcVer.isEmpty() || parseVer >= parseVer(find.minMcVer);
        }
        return false;
    }

    public static String warningOf(String str, String str2) {
        if (supports(str, str2)) {
            return null;
        }
        Info find = find(str);
        if (find != null) {
            str = find.displayName;
        }
        return "当前渲染器「" + str + "」不支持该游戏版本（" + str2 + "）。\n\n该渲染器最高支持到 " + (find != null ? find.displayMax : "?") + "，继续使用可能导致画面错误、贴图错乱或游戏崩溃！\n\n是否仍要使用这个渲染器？";
    }
}
