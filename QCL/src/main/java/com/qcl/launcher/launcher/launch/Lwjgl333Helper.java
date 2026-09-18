/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.res.AssetManager
 */
package com.qcl.launcher.launcher.launch;

import android.content.Context;
import android.content.res.AssetManager;
import com.qcl.launcher.utils.Architecture;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lwjgl333Helper {
    private static final String ASSET_ROOT = "app_runtime/lwjgl333";
    public static final String DIR_NAME = "lwjgl333";
    private static final String ASSET_ROOT_341 = "app_runtime/lwjgl341";
    public static final String DIR_NAME_341 = "lwjgl341";
    private static boolean sPrepared = false;
    private static boolean sPrepared341 = false;
    private static Boolean sZinkUsable = null;

    public static File nativesDir(Context context) {
        return new File(context.getFilesDir(), "lwjgl333/natives");
    }

    public static File jarsDir(Context context) {
        return new File(context.getFilesDir(), "lwjgl333/jars");
    }

    public static File nativesDir341(Context context) {
        return new File(context.getFilesDir(), "lwjgl341/natives");
    }

    public static File jarsDir341(Context context) {
        return new File(context.getFilesDir(), "lwjgl341/jars");
    }

    public static boolean needs341(String versionPath) {
        String v = Lwjgl333Helper.detectLwjglVersion(versionPath);
        if (v == null) {
            return false;
        }
        int[] p = Lwjgl333Helper.parseVer(v);
        return p[0] > 3 || p[0] == 3 && p[1] >= 4;
    }

    public static File prepare341(Context context) {
        File natives = Lwjgl333Helper.nativesDir341(context);
        File jars = Lwjgl333Helper.jarsDir341(context);
        try {
            boolean nativesFresh = new File(natives, ".ok").isFile();
            if (!nativesFresh) {
                String[] nl2;
                Lwjgl333Helper.deleteRecursively(natives);
                natives.mkdirs();
                AssetManager am = context.getAssets();
                String abi = Lwjgl333Helper.abiDir();
                String[] nl = am.list("app_runtime/lwjgl341/natives/" + abi);
                if (nl == null || nl.length == 0) {
                    abi = "armeabi-v7a";
                }
                if ((nl2 = am.list("app_runtime/lwjgl341/natives/" + abi)) != null) {
                    for (String name : nl2) {
                        if (!name.endsWith(".so")) continue;
                        Lwjgl333Helper.copyAsset(am, "app_runtime/lwjgl341/natives/" + abi + "/" + name, new File(natives, name));
                    }
                }
                new File(natives, ".ok").createNewFile();
            }
            Lwjgl333Helper.deleteRecursively(jars);
            jars.mkdirs();
            AssetManager am2 = context.getAssets();
            String[] jl = am2.list("app_runtime/lwjgl341/jars");
            if (jl != null) {
                for (String name : jl) {
                    if (!name.endsWith(".jar")) continue;
                    Lwjgl333Helper.copyAsset(am2, "app_runtime/lwjgl341/jars/" + name, new File(jars, name));
                }
            }
            sPrepared341 = true;
            return natives;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public static String jarsClassPath341(Context context) {
        File merged;
        File dir = Lwjgl333Helper.jarsDir341(context);
        File[] files = dir.listFiles();
        if (files == null) {
            return "";
        }
        LinkedHashSet<File> ordered = new LinkedHashSet<File>();
        File core = new File(dir, "lwjgl.jar");
        if (core.isFile()) {
            ordered.add(core);
        }
        if ((merged = new File(dir, "lwjgl-3.4.1-merged-modules.jar")).isFile()) {
            ordered.add(merged);
        }
        ArrayList<File> rest = new ArrayList<File>();
        for (File f : files) {
            if (!f.getName().endsWith(".jar") || ordered.contains(f)) continue;
            rest.add(f);
        }
        Collections.sort(rest, (a, b) -> a.getName().compareTo(b.getName()));
        ordered.addAll(rest);
        StringBuilder sb = new StringBuilder();
        for (File f : ordered) {
            sb.append(f.getAbsolutePath()).append(':');
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    public static File jnaDir(Context context) {
        return new File(context.getFilesDir(), "lwjgl333/jna");
    }

    private static String detectLwjglVersion(String versionPath) {
        if (versionPath == null) {
            return null;
        }
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) {
                return null;
            }
            String content = Lwjgl333Helper.readText(new FileInputStream(json));
            Matcher m = Pattern.compile("(?:org\\.lwjgl\\.lwjgl:lwjgl|org\\.lwjgl:lwjgl|lwjgl)[/:](\\d+\\.\\d+\\.\\d+)").matcher(content);
            if (m.find()) {
                return m.group(1);
            }
            return null;
        }
        catch (Throwable ignored) {
            return null;
        }
    }

    private static int[] parseVer(String v) {
        try {
            String[] p = v.split("\\.");
            return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), p.length > 2 ? Integer.parseInt(p[2]) : 0};
        }
        catch (Throwable ignored) {
            return new int[]{0, 0, 0};
        }
    }

    public static boolean needs(String versionPath) {
        String v = Lwjgl333Helper.detectLwjglVersion(versionPath);
        if (v == null) {
            return false;
        }
        int[] p = Lwjgl333Helper.parseVer(v);
        if (p[0] < 3) {
            return false;
        }
        return p[0] != 3 || p[1] < 4;
    }

    public static File prepare(Context context) {
        File natives = Lwjgl333Helper.nativesDir(context);
        File jars = Lwjgl333Helper.jarsDir(context);
        try {
            boolean nativesFresh = new File(natives, ".ok").isFile();
            AssetManager am = context.getAssets();
            if (!nativesFresh) {
                Lwjgl333Helper.deleteRecursively(natives);
                natives.mkdirs();
                String abi = Lwjgl333Helper.abiDir();
                String[] list = am.list("app_runtime/lwjgl333/natives/" + abi);
                if (list == null || list.length == 0) {
                    abi = "armeabi-v7a";
                }
                for (String name : am.list("app_runtime/lwjgl333/natives/" + abi)) {
                    if (!name.endsWith(".so")) continue;
                    Lwjgl333Helper.copyAsset(am, "app_runtime/lwjgl333/natives/" + abi + "/" + name, new File(natives, name));
                }
                File jnaDir = Lwjgl333Helper.jnaDir(context);
                Lwjgl333Helper.deleteRecursively(jnaDir);
                jnaDir.mkdirs();
                for (String name : am.list("app_runtime/lwjgl333/jna/" + abi)) {
                    if (!name.endsWith(".so")) continue;
                    Lwjgl333Helper.copyAsset(am, "app_runtime/lwjgl333/jna/" + abi + "/" + name, new File(jnaDir, name));
                }
                new File(natives, ".ok").createNewFile();
            }
            Lwjgl333Helper.deleteRecursively(jars);
            jars.mkdirs();
            for (String name : am.list("app_runtime/lwjgl333/jars")) {
                if (!name.endsWith(".jar")) continue;
                Lwjgl333Helper.copyAsset(am, "app_runtime/lwjgl333/jars/" + name, new File(jars, name));
            }
            sPrepared = true;
            return natives;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public static String jarsClassPath(Context context) {
        return Lwjgl333Helper.jarsClassPath(context, false);
    }

    public static String jarsClassPath(Context context, boolean lwjgl2Era) {
        File merged;
        File lwjglx;
        StringBuilder sb = new StringBuilder();
        File dir = Lwjgl333Helper.jarsDir(context);
        File[] files = dir.listFiles();
        if (files == null) {
            return "";
        }
        LinkedHashSet<File> ordered = new LinkedHashSet<File>();
        File core = new File(dir, "lwjgl.jar");
        if (core.isFile()) {
            ordered.add(core);
        }
        if (lwjgl2Era && (lwjglx = new File(dir, "lwjgl-lwjglx.jar")).isFile()) {
            ordered.add(lwjglx);
        }
        if ((merged = new File(dir, "lwjgl-3.3.3-merged-modules.jar")).isFile()) {
            ordered.add(merged);
        }
        ArrayList<File> rest = new ArrayList<File>();
        for (File f : files) {
            if (!f.getName().endsWith(".jar") || ordered.contains(f)) continue;
            rest.add(f);
        }
        Collections.sort(rest, (a, b) -> a.getName().compareTo(b.getName()));
        ordered.addAll(rest);
        for (File f : ordered) {
            sb.append(f.getAbsolutePath()).append(':');
        }
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }

    private static String abiDir() {
        int arch = Architecture.getRuntimeArchitecture();
        if (arch == Architecture.ARCH_ARM) {
            return "armeabi-v7a";
        }
        if (arch == Architecture.ARCH_ARM64) {
            return "arm64-v8a";
        }
        if (arch == Architecture.ARCH_X86) {
            return "x86";
        }
        return "x86_64";
    }

    private static void copyAsset(AssetManager am, String assetPath, File dst) throws Exception {
        File parent = dst.getParentFile();
        if (parent != null && !parent.isFile()) {
            parent.mkdirs();
        }
        try (InputStream in = am.open(assetPath);
             FileOutputStream out = new FileOutputStream(dst);){
            int n;
            byte[] buf = new byte[65536];
            while ((n = in.read(buf)) > 0) {
                ((OutputStream)out).write(buf, 0, n);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static String readText(InputStream in) throws Exception {
        try {
            int n;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            String string2 = new String(bos.toByteArray(), "UTF-8");
            return string2;
        }
        finally {
            in.close();
        }
    }

    private static void deleteRecursively(File f) {
        File[] children;
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory() && (children = f.listFiles()) != null) {
            for (File c : children) {
                Lwjgl333Helper.deleteRecursively(c);
            }
        }
        f.delete();
    }

    public static synchronized boolean isZinkUsable(Context context) {
        if (sZinkUsable != null) {
            return sZinkUsable;
        }
        String dir = context.getApplicationInfo().nativeLibraryDir;
        try {
            System.load(dir + "/libglxshim.so");
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            System.load(dir + "/libEGL_mesa.so");
            System.load(dir + "/libzink_dri.so");
            sZinkUsable = Boolean.TRUE;
        }
        catch (Throwable t) {
            sZinkUsable = Boolean.FALSE;
        }
        return sZinkUsable;
    }

    public static String pickRenderer(Context context, String userRenderer) {
        if (!(userRenderer == null || userRenderer.isEmpty() || "zink".equals(userRenderer) || "opengles3_desktopgl_zink_kopper".equals(userRenderer))) {
            return userRenderer;
        }
        if (Lwjgl333Helper.isZinkUsable(context)) {
            return "opengles3_desktopgl_zink_kopper";
        }
        return "opengles3";
    }

    public static String fallbackRenderer() {
        return "opengles3";
    }

    public static boolean needsLwjgl2(String versionPath) {
        if (versionPath == null) {
            return false;
        }
        try {
            File dir = new File(versionPath);
            File json = new File(dir, dir.getName() + ".json");
            if (!json.isFile()) {
                return false;
            }
            String content = Lwjgl333Helper.readText(new FileInputStream(json));
            return content.contains("lwjgl/2.9") || content.contains("lwjgl_util/2.9") || content.contains(":lwjgl:2.") || content.contains(":lwjgl_util:2.") || content.contains("lwjgl/2.8");
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static boolean needsLwjglX(String versionPath) {
        String v = Lwjgl333Helper.detectLwjglVersion(versionPath);
        if (v == null) {
            return false;
        }
        return Lwjgl333Helper.parseVer(v)[0] < 3;
    }
}

