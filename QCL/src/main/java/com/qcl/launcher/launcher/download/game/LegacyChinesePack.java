package com.qcl.launcher.launcher.download.game;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ★ 1.3.0：**远古版本中文包**（启动器自带，装完自动应用）。
 *
 * 做什么：把 {@code assets/cn_b173/} 里的东西写进这个版本的 jar：
 * <ul>
 *   <li>{@code sj.class} —— 打过补丁的 FontRenderer（字模上限 256 → 4096，支持中文）</li>
 *   <li>{@code font.txt} —— 允许显示的字符表（原版 144 + 中文标点/常用字，共 4064）</li>
 *   <li>{@code font/glyph_XX.png} + {@code font/glyph_sizes.bin} —— **Mojang 官方中文点阵**
 *       （16×16 字形，画到屏幕上只画 8px，和英文等高、不溢出）</li>
 *   <li>{@code lang/en_US.lang} + {@code lang/stats_US.lang} —— 界面文本（中文或英文）</li>
 * </ul>
 *
 * 为什么这么做：b1.7.3 的字体是**固定 256 个 ASCII 字模**，一个汉字都没有 ——
 * 不换字体，中文全是方框/乱码；界面文本硬编码在 lang 文件里，换文件就是换语言。
 *
 * ★ 语言：只支持 **简体中文 / English** 两种（{@code assets/cn_b173/lang_zh}、{@code lang_en}）。
 *   切换语言 = 用另一套 lang 重新应用一遍。
 */
public final class LegacyChinesePack {

    private static final String TAG = "QCLCnPack";

    /** 资源目录 */
    private static final String ASSET_DIR = "cn_b173";

    /** 支持的版本（前缀匹配，b1.7.3 / b1.7.3ml / b1.7.3bc ... 都用同一套混淆名） */
    private static final String[] SUPPORTED_PREFIXES = {"b1.7.3"};

    public static final String LANG_ZH = "zh_CN";
    public static final String LANG_EN = "en_US";

    private LegacyChinesePack() {
    }

    /** 这个版本能不能用中文包 */
    public static boolean isSupported(String versionId) {
        if (versionId == null || versionId.isEmpty()) {
            return false;
        }
        String low = versionId.toLowerCase();
        for (String p : SUPPORTED_PREFIXES) {
            if (low.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 给这个版本装上中文（或切回英文）。
     *
     * @param lang {@link #LANG_ZH} 或 {@link #LANG_EN}
     * @return 成功与否
     */
    public static boolean apply(Context context, File versionDir, String versionId, String lang) {
        if (context == null || versionDir == null || versionId == null) {
            return false;
        }
        File jar = new File(versionDir, versionId + ".jar");
        if (!jar.isFile() || jar.length() < 1024) {
            Log.w(TAG, "找不到本体 jar: " + jar.getAbsolutePath());
            return false;
        }
        String langDir = LANG_EN.equals(lang) ? "lang_en" : "lang_zh";
        File tmp = new File(versionDir, versionId + ".jar.cnpatch");
        InputStream in = null;
        ZipInputStream zin = null;
        ZipOutputStream zout = null;
        try {
            List<String> written = new ArrayList<>();
            // ★ 要替换/新增的条目：先记下来，边写边从 assets 塞进去
            in = new java.io.FileInputStream(jar);
            zin = new ZipInputStream(new java.io.BufferedInputStream(in, 65536));
            zout = new ZipOutputStream(new java.io.BufferedOutputStream(new FileOutputStream(tmp), 65536));
            ZipEntry entry;
            byte[] buf = new byte[65536];
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                if (isPatchedEntry(name)) {
                    continue;   // 这些由中文包提供，跳过原版
                }
                ZipEntry ne = new ZipEntry(name);
                zout.putNextEntry(ne);
                int n;
                while ((n = zin.read(buf)) > 0) {
                    zout.write(buf, 0, n);
                }
                zout.closeEntry();
            }
            zin.close();
            zin = null;
            in.close();
            in = null;

            // 补丁类
            writeAsset(context, ASSET_DIR + "/sj.class", "sj.class", zout);
            // 字符表 + 官方中文点阵
            writeAsset(context, ASSET_DIR + "/font.txt", "font.txt", zout);
            writeAsset(context, ASSET_DIR + "/font/glyph_sizes.bin", "font/glyph_sizes.bin", zout);
            for (int page = 0; page <= 0xFF; ++page) {
                String nm = String.format("glyph_%02X.png", page);
                InputStream probe = null;
                try {
                    probe = context.getAssets().open(ASSET_DIR + "/font/" + nm);
                } catch (Exception ignored) {
                    // 这一页没有（官方就没出）→ 跳过
                }
                if (probe == null) {
                    continue;
                }
                probe.close();
                writeAsset(context, ASSET_DIR + "/font/" + nm, "font/" + nm, zout);
            }
            // 语言
            writeAsset(context, ASSET_DIR + "/" + langDir + "/en_US.lang", "lang/en_US.lang", zout);
            writeAsset(context, ASSET_DIR + "/" + langDir + "/stats_US.lang", "lang/stats_US.lang", zout);

            zout.close();
            zout = null;

            // 替换本体（先备份一份原版，方便还原）
            File backup = new File(versionDir, versionId + ".jar.orig");
            if (!backup.exists()) {
                if (!jar.renameTo(backup)) {
                    Log.w(TAG, "备份原版 jar 失败，放弃打补丁");
                    //noinspection ResultOfMethodCallIgnored
                    tmp.delete();
                    return false;
                }
            } else {
                //noinspection ResultOfMethodCallIgnored
                jar.delete();
            }
            if (!tmp.renameTo(jar)) {
                Log.w(TAG, "替换本体 jar 失败");
                return false;
            }
            written.add("sj.class");
            Log.i(TAG, "已给 " + versionId + " 应用中文包（" + lang + "，写入 " + written.size() + " 项）");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "应用中文包失败: " + t);
            return false;
        } finally {
            closeQuietly(zin);
            closeQuietly(in);
            closeQuietly(zout);
            if (tmp.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        }
    }

    /** 中文包会覆盖的条目 */
    private static boolean isPatchedEntry(String name) {
        if (name == null) {
            return false;
        }
        String low = name.toLowerCase();
        if (low.equals("sj.class")) {
            return true;
        }
        if (low.equals("font.txt") || low.equals("font/glyph_sizes.bin")) {
            return true;
        }
        if (low.startsWith("font/glyph_") && low.endsWith(".png")) {
            return true;
        }
        return low.equals("lang/en_us.lang") || low.equals("lang/stats_us.lang");
    }

    private static void writeAsset(Context context, String assetPath, String entryName, ZipOutputStream zout) throws Exception {
        InputStream is = null;
        try {
            is = context.getAssets().open(assetPath);
            zout.putNextEntry(new ZipEntry(entryName));
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) > 0) {
                zout.write(buf, 0, n);
            }
            zout.closeEntry();
        } finally {
            closeQuietly(is);
        }
    }

    private static void closeQuietly(java.io.Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (Throwable ignored) {
        }
    }

    /** 只用来读一下 asset 存不存在（有些页官方没出） */
    private static byte[] probe(Context context, String assetPath) {
        InputStream is = null;
        try {
            is = context.getAssets().open(assetPath);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            closeQuietly(is);
        }
    }
}
