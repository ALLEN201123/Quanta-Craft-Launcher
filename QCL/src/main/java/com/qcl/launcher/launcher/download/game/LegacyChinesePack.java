package com.qcl.launcher.launcher.download.game;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

    /** 应用标记文件（记着"这个版本已按哪个语言打过补丁"），放在版本目录里 */
    private static final String MARKER = ".cnpack_lang";

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
        // ★ 放宽：改了名字的版本（例如「b1.7.3东上」「我的b1.7.3」）也算
        for (String p : SUPPORTED_PREFIXES) {
            if (low.contains(p)) {
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

            // 补丁类：字体（sj）+ 翻译（nh）+ 选项页（co，带「语言…」按钮）+ 语言选择页
            writeAsset(context, ASSET_DIR + "/sj.class", "sj.class", zout);
            writeAsset(context, ASSET_DIR + "/co.class", "co.class", zout);
            writeAsset(context, ASSET_DIR + "/QclLangScreen.class", "QclLangScreen.class", zout);
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
            // 语言：源文件两套都装（zh_CN/stats_zh_CN=中文，en_US_orig/stats_US_orig=英文），
            // 再把**选中语言**的内容写进 en_US.lang / stats_US.lang —— 游戏原版 StringTranslate
            // 读的就是这两个文件名，所以「启动时的语言」由这里决定；游戏内切换走 QclLangScreen。
            writeAsset(context, ASSET_DIR + "/lang/zh_CN.lang", "lang/zh_CN.lang", zout);
            writeAsset(context, ASSET_DIR + "/lang/stats_zh_CN.lang", "lang/stats_zh_CN.lang", zout);
            writeAsset(context, ASSET_DIR + "/lang/en_US_orig.lang", "lang/en_US_orig.lang", zout);
            writeAsset(context, ASSET_DIR + "/lang/stats_US_orig.lang", "lang/stats_US_orig.lang", zout);
            boolean wantEn = LANG_EN.equals(lang);
            writeAsset(context, ASSET_DIR + (wantEn ? "/lang/en_US_orig.lang" : "/lang/zh_CN.lang"), "lang/en_US.lang", zout);
            writeAsset(context, ASSET_DIR + (wantEn ? "/lang/stats_US_orig.lang" : "/lang/stats_zh_CN.lang"), "lang/stats_US.lang", zout);

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
            // ★ 选中的语言写成 <游戏根>/qcl_lang.txt —— 游戏里 StringTranslate 读它
            writeLangChoice(context, versionDir.getParentFile().getParentFile().getAbsolutePath(), lang);
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

    /** 这个版本当前按哪个语言打过补丁（没打过返回 null） */
    public static String appliedLang(File versionDir) {
        try {
            File m = new File(versionDir, MARKER);
            if (!m.isFile()) {
                return null;
            }
            byte[] b = new byte[(int) m.length()];
            java.io.FileInputStream in = new java.io.FileInputStream(m);
            int n = in.read(b);
            in.close();
            return n > 0 ? new String(b, 0, n, "UTF-8").trim() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * ★ 启动前调用：**没打过、或语言变了**才重新打一遍（打过就秒过，不浪费时间）。
     * 这样「早就装好的版本」「改了名字的版本」也能自动拿到中文。
     */
    public static boolean applyIfNeeded(Context context, File versionDir, String versionId) {
        if (context == null || versionDir == null || !isSupported(versionDir, versionId)) {
            return false;
        }
        File jar = new File(versionDir, versionId + ".jar");
        if (!jar.isFile()) {
            return false;
        }
        // ★ 以 <游戏根>/qcl_lang.txt 为准（游戏内切过语言也算数），没有才用启动器设置
        String want = com.qcl.launcher.launcher.uis.universal.setting.right.launcher.DownloadSettingUI.getLegacyLang(context);
        try {
            File rt = new File(versionDir.getParentFile() == null ? versionDir
                    : versionDir.getParentFile().getParentFile(), "qcl_lang.txt");
            if (rt.isFile()) {
                byte[] rb = new byte[(int) rt.length()];
                java.io.FileInputStream rin = new java.io.FileInputStream(rt);
                int rn = rin.read(rb);
                rin.close();
                String rs = new String(rb, 0, rn, "UTF-8").trim();
                if ("en_US".equals(rs) || "zh_CN".equals(rs)) {
                    want = rs;
                }
            }
        } catch (Throwable ignored) {
        }
        if (want.equals(appliedLang(versionDir))) {
            return true;   // 已经打过同一个语言，跳过
        }
        boolean ok = apply(context, versionDir, versionId, want);
        if (ok) {
            try {
                java.io.FileOutputStream out = new java.io.FileOutputStream(new File(versionDir, MARKER));
                out.write(want.getBytes("UTF-8"));
                out.close();
            } catch (Throwable ignored) {
            }
        }
        return ok;
    }


    /**
     * ★ 1.3.0：**扫一遍所有已安装的远古版本**，没打中文包的补上。
     *
     * 为什么需要它：中文包原来只在「下载/安装完成」时打 —— 玩家**早就装好的版本**
     * （或者改过名字、从别处拷进来的）永远不会被补上。启动器进前台时扫一遍就够了，
     * 已经打过同一语言的会直接跳过。
     */
    public static void sweepAll(Context context, String gameFileDirectory) {
        try {
            File versions = new File(gameFileDirectory, "versions");
            File[] dirs = versions.listFiles();
            if (dirs == null) {
                return;
            }
            for (File d : dirs) {
                if (!d.isDirectory()) {
                    continue;
                }
                String id = d.getName();
                if (!isSupported(id) || !new File(d, id + ".jar").isFile()) {
                    continue;
                }
                applyIfNeeded(context, d, id);
            }
        } catch (Throwable t) {
            Log.w(TAG, "扫描已装远古版本失败: " + t);
        }
    }

    /**
     * ★ 把选中的语言写到 {@code <游戏根>/qcl_lang.txt}。
     * 游戏内语言页和启动器设置里改语言，都必须写这个文件 ——
     * {@link #applyIfNeeded} 以它为准（游戏里切过、启动器里切过，两边都能生效）。
     */
    public static boolean writeLangChoice(Context context, String gameFileDirectory, String lang) {
        if (gameFileDirectory == null || gameFileDirectory.isEmpty()) {
            return false;
        }
        try {
            File root = new File(gameFileDirectory).getParentFile();
            if (root == null) {
                root = new File(gameFileDirectory);
            }
            File f = new File(root, "qcl_lang.txt");
            FileOutputStream out = new FileOutputStream(f);
            out.write((LANG_EN.equals(lang) ? "en_US" : "zh_CN").getBytes("UTF-8"));
            out.close();
            return true;
        } catch (Throwable t) {
            android.util.Log.w(TAG, "写 qcl_lang.txt 失败: " + t);
            return false;
        }
    }


    /**
     * 准确判断「这个版本是不是 b1.7.3」：
     * 优先看**版本 json 里的真 id**（玩家把目录改名成别的也认得出来）；json 读不到再用目录名兜底 ✓
     */


    /**
     * 准确判断「这个版本是不是 b1.7.3」：
     * 优先读**版本 json**（玩家把目录改名成别的也认得出来）；json 读不到再用目录名兜底 ✓
     */


    /** 按版本 json 判断是不是 b1.7.3（玩家改了目录名也认得出 ✓） */
    public static boolean isSupported(File versionDir, String versionId) {
        if(versionId == null || versionId.isEmpty()) {
            return false;
        }

        if(versionDir != null) {
            try {
                File jf = new File(versionDir, versionId + ".json");
                if(jf.isFile()) {
                    byte[] b = new byte[(int)jf.length()];
                    FileInputStream fin = new FileInputStream(jf);
                    int n = fin.read(b);
                    fin.close();
                    String json = new String(b, 0, n, "UTF-8").toLowerCase();
                    for(String pp : SUPPORTED_PREFIXES) {
                        if(json.contains(pp)) {
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        String low = versionId.toLowerCase();
        for(String pp : SUPPORTED_PREFIXES) {
            if(low.contains(pp)) {
                return true;
            }
        }
        return false;
    }

    /** 中文包会覆盖的条目 */
    private static boolean isPatchedEntry(String name) {
        if (name == null) {
            return false;
        }
        String low = name.toLowerCase();
        if (low.equals("sj.class") || low.equals("co.class")
                || low.equals("qcllangscreen.class")) {
            return true;
        }
        // ★ 原 jar 是签名过的，注入未签名类后必须去掉签名文件，否则
        //   JVM 会抛 SecurityException: signer information does not match
        if (low.startsWith("meta-inf/") && (low.endsWith(".sf") || low.endsWith(".rsa")
                || low.endsWith(".dsa") || low.endsWith("manifest.mf"))) {
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
