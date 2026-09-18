package com.qcl.launcher.launcher.launch;

import android.content.Context;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes2.dex */
public final class RendererCompat {
    public static final Info[] ALL = {new Info("opengles2", "Holy-GL4ES", "Holy GL4ES (OpenGL 2.1)", "libgl4es_114.so", "libEGL.so", "", "1.21.4", "1.21.4", true, true), new Info("ng_gl4es", "Krypton Wrapper", "Krypton Wrapper (OpenGL 3.1+, 全版本通吃)", "libng_gl4es.so", "libEGL.so", "", "26.3", "26.2", true, true), new Info("zink", "Zink", "Kopper Zink (OpenGL 4.6, Mesa zink on Vulkan)", "libglxshim.so", "libEGL_mesa.so", "", "26.3", "26.2", true, true), new Info("opengles3_virgl", "VirGLRenderer", "VirGLRenderer (OpenGL 4.3, Mesa 软渲染)", "libOSMesa_81.so", "libEGL.so", "", "26.3", "26.2", true, true), new Info("opengles3_virgl_osmesa8", "Freedreno", "Freedreno (OpenGL 4.6, 仅高通 adreno616-a660)", "libOSMesa_8.so", "libEGL.so", "", "26.3", "26.2", true, false), new Info("opengles3_vgpu", "VGPU", "VGPU (OpenGL 2.1+)", "libvgpu.so", "libEGL.so", "", "1.16.5", "1.16.5", true, false), new Info("mg", "MobileGlues", "MobileGlues (外部渲染器，需自行导入 libMobileGlues.so)", "libMobileGlues.so", "libEGL.so", "", "26.3", "26.2", false, false)};

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
