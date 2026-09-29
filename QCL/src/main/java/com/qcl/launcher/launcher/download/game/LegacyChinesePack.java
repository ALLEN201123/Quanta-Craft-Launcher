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

    /** ★ 当前正在处理的是不是 1.0 系（决定 abe 要不要替换）✓ */
    private static boolean v10Flag = false;

    /** ★ b1.9 各版本的补丁（配置驱动）
     *  每行 = { 版本名正则, 补丁资源目录, FontRenderer 混淆名 }
     *  ★ b1.9 的 5 个 pre 版混淆名各不相同（lf/ls/mc/mb/mf），必须逐版对应 ✓ */
    private static final String[][] B19_PACKS = {
            {"b1\\.9-pre2$",      "cn_b19pre2",     "lf"},
            {"b1\\.9-pre3-1402$", "cn_b19pre31402", "ls"},
            {"b1\\.9-pre4-1415$", "cn_b19pre41415", "mc"},
            {"b1\\.9-pre5$",      "cn_b19pre5",     "mb"},
            {"b1\\.9-pre6$",      "cn_b19pre6",     "mf"},
    };

    /** ★ 当前正在处理的 b1.9 组（[资源目录, 混淆名]，null = 不是 b1.9）✓ */
    private static String[] b19Active = null;

    /** ★ 1.3.6：当前激活的 b1.0/b1.1 补丁类（"mi.class" / "mj.class"；null = 不是）——
     *  isPatchedEntry 只跳它自己（坑：mj 在 b1.0 里是方块类、mi 在 b1.1 里另有其物，绝不能混跳 ✗）✓ */
    private static String b10b11Class = null;

    /** ★ 1.3.6：新增支持的 Beta 版本（b1.2~b1.9 的变体/小版本）
     *  每行 = { 版本名正则, 补丁资源目录, FontRenderer 混淆名 }
     *  ★ 排除（**字体类内容与主版不同 → 复用主版产物会崩**，需各自生成专用补丁，1.3.7 处理）：
     *    b1.2_02-dev-20110517（开发版无主菜单、类结构大变）
     *    b1.3-pcgamer_demo / b1.4-1507 / b1.8-pre1-081459 / b1.8-pre1-091357 / b1.9-pre ✓ */
    private static final String[][] EXTRA_PACKS = {
            {"b1\\.2(_0[12])?$",          "cn_b12",         "nh"},
            {"b1\\.3-(1713|1733|1750)$",  "cn_b13",         "oi"},
            {"b1\\.3_01$",                "cn_b13",         "oi"},
            {"b1\\.3-pcgamer_demo$",      "cn_b13demo",     "ok"},
            {"b1\\.4-1634$",              "cn_b14",         "ox"},
            {"b1\\.4_01$",                "cn_b14",         "ox"},
            {"b1\\.5(_01)?$",             "cn_b15",         "rf"},
            {"b1\\.6-test_build_3$",      "cn_166t",        "se"},
            {"b1\\.7(_01|\\.2)?$",        "cn_b17",         "sj"},
            {"b1\\.8-pre1-081459$",       "cn_b18pre1",     "kg"},
            {"b1\\.8-pre1-091357$",       "cn_b18pre1",     "kg"},
            {"b1\\.8-pre2-121559$",       "cn_b18pre2",     "kh"},
            {"b1\\.8-pre2-131225$",       "cn_b18pre2",     "kh"},
            {"b1\\.9-pre$",               "cn_b19pre",      "lc"},
            {"b1\\.9-pre3-1350$",         "cn_b19pre31350", "ls"},
            {"b1\\.9-pre4-1434$",         "cn_b19pre41434", "mc"},
    };

    /** ★ 当前正在处理的扩展组（[资源目录, 混淆名]，null = 不是）✓ */
    private static String[] extraActive = null;

    /** ★ 判断版本名命中哪一组 EXTRA_PACKS；返回该行或 null ✓ */
    private static String[] matchExtra(String versionId) {
        if (versionId == null || versionId.isEmpty()) {
            return null;
        }
        String vid = versionId.trim().toLowerCase();
        for (String[] row : EXTRA_PACKS) {
            if (vid.matches(row[0])) {
                return row;
            }
        }
        return null;
    }

    /** ★ 判断版本名命中哪一组 B19_PACKS；返回该行或 null ✓ */
    private static String[] matchB19(String versionId) {
        if (versionId == null || versionId.isEmpty()) {
            return null;
        }
        String vid = versionId.trim().toLowerCase();
        for (String[] row : B19_PACKS) {
            if (vid.matches(row[0])) {
                return row;
            }
        }
        return null;
    }

    /**
     * ★ 1.3.4：**按版本挑语言覆盖** —— 少数键在不同版本里含义不同，中文说法也不同 ✓。
     *
     * 主表 {@code zh_CN.lang} 用的是「最新（1.0 / b1.9-pre6）」措辞，旧版本用 lang_over/ 里
     * 的小覆盖改回去。**实测全量对比**（b1.6.6 → b1.9-pre6 所有同名键）只有 4 个键的值变过，
     * 其中 3 个需要按版本改词，{@code key.playerlist} 只是首字母大小写、中文无差别，不用管：
     * <pre>
     *   tile.grass.name            Grass(草)                      → Grass Block(草方块)
     *   tile.stoneSlab.cobble.name  Stone Slab(石台阶)             → Cobblestone Slab(圆石台阶)
     *   menu.mods                  Mods and Texture Packs(模组与材质包) → Texture Packs(材质包)
     * </pre>
     *
     * @return 覆盖资源的 asset 路径；{@code null} = 不需要覆盖（主表本身就是对的）✓
     */
    private static String overlayFor(String versionId, File versionDir) {
        if (versionId == null || versionId.isEmpty()) {
            return null;
        }
        final String BASE = "cn_b173/lang_over/";
        String vid = versionId.trim().toLowerCase();
        // ★ 1.0 正式版 / b1.9-pre6 —— 用「最新」措辞，主表本身就是对的，不用覆盖 ✓
        if (isV10(versionDir, versionId) || vid.matches("b1\\.9-pre6$")) {
            return null;
        }
        // ★ b1.8 系 / b1.9-pre2~pre5 —— 草方块 ✓ 圆石台阶 ✓，但 menu.mods 还是旧的「模组与材质包」✗
        if (isB18(versionDir, versionId) || matchB19(versionId) != null) {
            return BASE + "over_b18.lang";
        }
        // ★ b1.6 系 / b1.7.3 系（默认）—— 三处全是旧措辞 ✗
        return BASE + "over_b16.lang";
    }

    /** ★ 当前正在处理的是不是 b1.6 系（决定 se 要不要替换）✓ */
    private static boolean b166Flag = false;

    /** ★ 是不是 b1.6 系（b1.6 / b1.6.1 ~ b1.6.6）——
     *  实测这 7 个版本的 se.class SHA1 完全相同（d5ff012e31b3），一份补丁通用 ✓
     *  ★ 1.3.4：id 整体相等（原写法用正则已够精确，这里统一走 readJsonId）✓ */
    public static boolean isB166(File versionDir, String versionId) {
        try {
            String vid = versionId == null ? "" : versionId.trim().toLowerCase();
            if (vid.matches("b1\\.6(\\.[1-6])?")) {
                return true;
            }
            String id = readJsonId(versionDir, versionId);
            if (id != null && id.toLowerCase().matches("b1\\.6(\\.[1-6])?")) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * ★ 1.3.4：从版本 json 里**精确**取顶级 {@code "id"} 的值 ✓
     *
     * 为什么需要它：原来用 {@code tail.contains("1.0.0")} 判断 1.0 —— 但
     * {@code "1.0.0-rc1"} / {@code "1.0.0-rc2-1649"} 这些**候选版**的 id 也含 {@code "1.0.0"} ✗
     * → 被当成正式 1.0 → 往它们 jar 里注入正式版才有的 {@code abe.class}（实测 rc 版根本没有
     * 这个类，正式 1.0 的 abe SHA1 = f65d6e4d）→ 污染/崩溃风险 ✗。
     * 现改为取 id 值做**整体相等**判断 ✓
     */
    private static String readJsonId(File versionDir, String versionId) {
        if (versionDir == null || versionId == null) {
            return null;
        }
        try {
            File jf = new File(versionDir, versionId + ".json");
            if (!jf.isFile()) {
                return null;
            }
            byte[] b = new byte[(int) jf.length()];
            FileInputStream fin = new FileInputStream(jf);
            int n = fin.read(b);
            fin.close();
            String js = new String(b, 0, n, "UTF-8");
            // 只认「行首（可含空白）的 "id"」—— 避开 libraries 数组里嵌套的 id ✓
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?m)^\\s*\"id\"\\s*:\\s*\"([^\"]*)\"").matcher(js);
            return m.find() ? m.group(1).trim() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** ★ 是不是 1.0（正式版）—— RetroMCP 里叫 1.0.0，Mojang 的 id 是 1.0 ✓
     *  ★ 1.3.4：id 必须**整体相等** —— 不能再用 contains，否则 1.0.0-rc1 这类候选版会被误判 ✗ */
    public static boolean isV10(File versionDir, String versionId) {
        try {
            String vid = versionId == null ? "" : versionId.trim().toLowerCase();
            if (vid.equals("1.0") || vid.equals("1.0.0")) {
                return true;
            }
            String id = readJsonId(versionDir, versionId);
            if (id != null) {
                String lowId = id.toLowerCase();
                return lowId.equals("1.0") || lowId.equals("1.0.0");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** ★ 是不是 b1.8 系（b1.8 / b1.8.1）—— 这两版 kh.class 字节完全相同，一份补丁通用 ✓
     *  ★ 1.3.4：id 整体相等 —— b1.8-pre2 这类预发布版**不**算 ✓ */
    public static boolean isB18(File versionDir, String versionId) {
        try {
            String vid = versionId == null ? "" : versionId.trim().toLowerCase();
            if (vid.equals("b1.8") || vid.equals("b1.8.1")) {
                return true;
            }
            String id = readJsonId(versionDir, versionId);
            if (id != null) {
                String lowId = id.toLowerCase();
                return lowId.equals("b1.8") || lowId.equals("b1.8.1");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** ★ 1.3.6：b1.0 系（b1.0 / b1.0_01 / b1.0.2）—— 三版 mi.class 字节完全相同（md5 ae3b9d3a），一份补丁通用 ✓
     *  ★ b1.0 无「字符表类」（字符表内联在代码里）→ 补丁的 mi 自带读表（qclReadTable）+ 表缺失回退原版行为 ✓ */
    public static boolean isB10(File versionDir, String versionId) {
        try {
            String vid = versionId == null ? "" : versionId.trim().toLowerCase();
            if (vid.equals("b1.0") || vid.equals("b1.0_01") || vid.equals("b1.0.2")) {
                return true;
            }
            String id = readJsonId(versionDir, versionId);
            if (id != null) {
                String lowId = id.toLowerCase();
                return lowId.equals("b1.0") || lowId.equals("b1.0_01") || lowId.equals("b1.0.2");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** ★ 1.3.6：b1.1 系（b1.1-1245 / b1.1-1255 / b1.1_01 / b1.1_02）—— 四版 mj.class 字节完全相同
     *  （md5 7271512b），一份补丁通用 ✓。★ 1245/1255 原版缺 font.txt（「没文字」版）→ 补丁注入 font.txt 顺手修复 ✓ */
    public static boolean isB11(File versionDir, String versionId) {
        try {
            String vid = versionId == null ? "" : versionId.trim().toLowerCase();
            if (vid.equals("b1.1-1245") || vid.equals("b1.1-1255") || vid.equals("b1.1_01") || vid.equals("b1.1_02")) {
                return true;
            }
            String id = readJsonId(versionDir, versionId);
            if (id != null) {
                String lowId = id.toLowerCase();
                return lowId.equals("b1.1-1245") || lowId.equals("b1.1-1255")
                        || lowId.equals("b1.1_01") || lowId.equals("b1.1_02");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

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
        // ★ 1.3.1：b1.8 系（b1.8 / b1.8.1）也支持中文 ✓
        if (isB18(null, versionId)) {
            return true;
        }
        // ★ 1.3.1：1.0 正式版 ✓
        if (isV10(null, versionId)) {
            return true;
        }

        // ★ 1.3.6：b1.0 系 / b1.1 系 ✓
        if (isB10(null, versionId) || isB11(null, versionId)) {
            return true;
        }
        // ★ b1.6 系 ✓
        if (isB166(null, versionId)) {
            return true;
        }
        // ★ b1.9 全系（pre2 ~ pre6）✓
        if (matchB19(versionId) != null) {
            return true;
        }
        // ★ 1.3.6：扩展的 17 个 Beta 版本 ✓
        if (matchExtra(versionId) != null) {
            return true;
        }
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
        // ★ 1.3.6：原版备份（首次注入时创建）—— 存在时作为「重新注入」的干净输入源 ✓
        File backup0 = new File(versionDir, versionId + ".jar.orig");
        if (!jar.isFile() || jar.length() < 1024) {
            Log.w(TAG, "找不到本体 jar: " + jar.getAbsolutePath());
            return false;
        }
        String langDir = LANG_EN.equals(lang) ? "lang_en" : "lang_zh";
        final boolean b18v = isB18(versionDir, versionId);
        final boolean v10v = isV10(versionDir, versionId);
        final boolean b166v = isB166(versionDir, versionId);
        b166Flag = b166v;
        // ★ 1.3.6：b1.0 系 / b1.1 系（各自只换自己的 FontRenderer：mi / mj）✓
        final boolean b10v = isB10(versionDir, versionId);
        final boolean b11v = isB11(versionDir, versionId);
        b10b11Class = b10v ? "mi.class" : (b11v ? "mj.class" : null);
        // ★ b1.9 全系：命中哪一组就换它自己的 FontRenderer ✓
        final String[] b19 = matchB19(versionId);
        b19Active = b19;
        // ★ 1.3.6：扩展 Beta 版本组 ✓
        final String[] extra = matchExtra(versionId);
        extraActive = extra;
        v10Flag = v10v;
        File tmp = new File(versionDir, versionId + ".jar.cnpatch");
        InputStream in = null;
        ZipInputStream zin = null;
        ZipOutputStream zout = null;
        java.util.zip.ZipFile zf = null;
        try {
            List<String> written = new ArrayList<>();
            // ★ 要替换/新增的条目：先记下来，边写边从 assets 塞进去
            // ★ 1.3.6：输入优先用「原版备份」—— 保证每次注入都是「原版 + 最新补丁」，
            //   而不是「上次注入的产物」叠加（否则误删的原生类无法自愈，b1.1-1245 的 co 就是例证）✓
            final File srcJar = backup0.exists() ? backup0 : jar;
            // ★ 1.3.6：改用 ZipFile（随机访问 / 读中央目录）而不是 ZipInputStream（流式）——
            //   后者遇到某些 jar（如 b1.3-pcgamer_demo 这种特殊打包）会提前返回 null，
            //   导致「原版条目一个都没复制」→ jar 只剩补丁内容 → 游戏 ClassNotFoundException ✗
            zf = new java.util.zip.ZipFile(srcJar);
            zout = new ZipOutputStream(new java.io.BufferedOutputStream(new FileOutputStream(tmp), 65536));
            byte[] buf = new byte[65536];
            java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                java.util.zip.ZipEntry entry = en.nextElement();
                String name = entry.getName();
                if (isPatchedEntry(name, b18v)) {
                    continue;   // 这些由中文包提供，跳过原版
                }
                ZipEntry ne = new ZipEntry(name);
                zout.putNextEntry(ne);
                java.io.InputStream ze = zf.getInputStream(entry);
                int n;
                while ((n = ze.read(buf)) > 0) {
                    zout.write(buf, 0, n);
                }
                ze.close();
                zout.closeEntry();
            }
            zf.close();
            zf = null;

            // 补丁类：字体（sj）+ 翻译（nh）+ 选项页（co，带「语言…」按钮）+ 语言选择页
            if (b10v) {
                // ★ 1.3.6：b1.0 系（b1.0 / b1.0_01 / b1.0.2）—— 只换 FontRenderer（mi），其余用原生 ✓
                writeAsset(context, "cn_b10/mi.class", "mi.class", zout);
            } else if (b11v) {
                // ★ 1.3.6：b1.1 系（含「没文字」的 1245 / 1255）—— 只换 FontRenderer（mj），其余用原生 ✓
                writeAsset(context, "cn_b11/mj.class", "mj.class", zout);
            } else if (b18v) {
                // ★ b1.8 / b1.8.1：只换 FontRenderer（kh），其余用原生 ✓
                writeAsset(context, "cn_b18/kh.class", "kh.class", zout);
            } else if (v10v) {
                // ★ 1.0 正式版：只换 FontRenderer（abe），其余用原生 ✓
                writeAsset(context, "cn_10/abe.class", "abe.class", zout);
            } else if (b166v) {
                // ★ b1.6 系：只换 FontRenderer（se），其余用原生 ✓
                writeAsset(context, "cn_166/se.class", "se.class", zout);
            } else if (b19 != null) {
                // ★ b1.9：只换它自己那一版的 FontRenderer，其余用原生 ✓
                writeAsset(context, b19[1] + "/" + b19[2] + ".class", b19[2] + ".class", zout);
            } else if (extra != null) {
                // ★ 1.3.6：扩展 Beta 版本 —— 只换它自己那一版的 FontRenderer，其余用原生 ✓
                writeAsset(context, extra[1] + "/" + extra[2] + ".class", extra[2] + ".class", zout);
                // ★ 1.3.6：pcgamer_demo 的主菜单按钮是硬编码英文（不走 lang），额外注入翻译后的 ei.class
                if ("cn_b13demo".equals(extra[1])) {
                    writeAsset(context, "cn_b13demo/ei.class", "ei.class", zout);
                }
            } else {
                writeAsset(context, ASSET_DIR + "/sj.class", "sj.class", zout);
                writeAsset(context, ASSET_DIR + "/co.class", "co.class", zout);
                writeAsset(context, ASSET_DIR + "/QclLangScreen.class", "QclLangScreen.class", zout);
            }
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
            // ★ 1.3.4：按版本取「语言覆盖」（少数键各版本含义不同，主表用的是最新措辞）✓
            final String over = overlayFor(versionId, versionDir);
            writeLangOverlaid(context, ASSET_DIR + "/lang/zh_CN.lang", over, "lang/zh_CN.lang", zout);
            writeAsset(context, ASSET_DIR + "/lang/stats_zh_CN.lang", "lang/stats_zh_CN.lang", zout);
            writeAsset(context, ASSET_DIR + "/lang/en_US_orig.lang", "lang/en_US_orig.lang", zout);
            writeAsset(context, ASSET_DIR + "/lang/stats_US_orig.lang", "lang/stats_US_orig.lang", zout);
            boolean wantEn = LANG_EN.equals(lang);
            if (wantEn) {
                writeAsset(context, ASSET_DIR + "/lang/en_US_orig.lang", "lang/en_US.lang", zout);
            } else {
                // ★ 生效语言是中文 → 同样要 merge 覆盖 ✓（否则旧版本会显示新版措辞）
                writeLangOverlaid(context, ASSET_DIR + "/lang/zh_CN.lang", over, "lang/en_US.lang", zout);
            }
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
            if (zf != null) {
                try {
                    zf.close();
                } catch (Throwable ignored) {
                }
            }
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
        // ★ 标记 = 语言 + "|" + APK versionCode —— 启动器升级后自动重注入 ✓
        //   否则老用户升级 APK 后，已打补丁的版本 jar 里还是旧补丁类，
        //   （1.3.4 的「标签: 值」按钮乱码就是：类修了但 jar 不更新）✓
        //   旧标记只有语言（无竖线）→ 比较必然不等 → 升级后自动重打一次 ✓
        String expect = want + "|" + apkVersionCode(context);
        if (expect.equals(appliedLang(versionDir))) {
            return true;   // 同一语言 + 同一启动器版本，跳过
        }
        boolean ok = apply(context, versionDir, versionId, want);
        if (ok) {
            try {
                java.io.FileOutputStream out = new java.io.FileOutputStream(new File(versionDir, MARKER));
                out.write(expect.getBytes("UTF-8"));
                out.close();
            } catch (Throwable ignored) {
            }
        }
        return ok;
    }

    /** 当前 APK 的 versionCode，写进补丁标记 —— 启动器升级后强制重注入 ✓ */
    private static String apkVersionCode(Context context) {
        try {
            return String.valueOf(context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode);
        } catch (Throwable t) {
            return "0";
        }
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
        // ★ 1.3.1：b1.8 系（b1.8 / b1.8.1）也支持中文 ✓
        if (isB18(versionDir, versionId)) {
            return true;
        }
        // ★ 1.3.1：1.0 正式版也支持中文 ✓
        if (isV10(versionDir, versionId)) {
            return true;
        }

        // ★ 1.3.6：b1.0 系 / b1.1 系 ✓
        if (isB10(versionDir, versionId) || isB11(versionDir, versionId)) {
            return true;
        }

        // ★ b1.6 系也支持中文 ✓
        if (isB166(versionDir, versionId)) {
            return true;
        }

        // ★ 1.3.6：扩展的 17 个 Beta 版本 ✓
        if (matchExtra(versionId) != null) {
            return true;
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
    private static boolean isPatchedEntry(String name, boolean b18) {
        // ★ b1.9：只在「当前这一组」激活时，跳过它自己的 FontRenderer 混淆名 ✓
        //   （5 个 pre 版名字各不相同：lf/ls/mc/mb/mf —— 不同名不同物，绝不能混跳过 ✗）
        if (b19Active != null && name != null
                && name.equalsIgnoreCase(b19Active[2] + ".class")) {
            return true;
        }
        // ★ 1.3.6：b1.0/b1.1 系 —— 只在对应版本激活时跳过它自己的类（mi 或 mj）✓
        if (b10b11Class != null && name != null && name.equalsIgnoreCase(b10b11Class)) {
            return true;
        }
        // ★ 1.3.6：扩展 Beta 组 —— 只在命中时跳过它自己的 FontRenderer ✓
        if (extraActive != null && name != null
                && name.equalsIgnoreCase(extraActive[2] + ".class")) {
            return true;
        }
        // ★ 1.3.6：pcgamer_demo 额外注入了 ei.class（翻译硬编码按钮），原版 ei.class 要跳过 ✓
        if ("cn_b13demo".equals(extraActive != null ? extraActive[1] : null)
                && name != null && name.equalsIgnoreCase("ei.class")) {
            return true;
        }
        if (name != null && name.equalsIgnoreCase("se.class")) {
            // ★ b1.6 系：se 是它的 FontRenderer —— 只在 b1.6 系时替换 ✓
            return b166Flag;
        }
        if (b18 && name != null && name.equalsIgnoreCase("kh.class")) {
            return true;   // b1.8 系：原版 FontRenderer，由中文包替换 ✓
        }
        if (name != null && name.equalsIgnoreCase("abe.class")) {
            // ★ 1.0 系：abe 是它的 FontRenderer —— 只在 1.0 时替换 ✓
            //   其他版本里 abe 是别的类，绝不能动 ✓（同 b1.8 的 co/sj 坑）
            return v10Flag;
        }
        if (name == null) {
            return false;
        }
        String low = name.toLowerCase();
        if (low.equals("sj.class")) {
            // ★ sj 只在「默认 b1.7.3 组」或「extra 组命中 sj（b1.7 系）」时由补丁提供（跳过原版）
            //   其余版本（b1.6/b1.8/1.0/b1.9/b1.0/b1.1/b1.2/b1.3/b1.4/b1.5）里 sj 是原生类，绝不能跳 ✗
            if (extraActive != null) {
                return "sj".equals(extraActive[2]);
            }
            return !(b18 || v10Flag || b166Flag || b19Active != null || b10b11Class != null);
        }
        if (low.equals("co.class") || low.equals("qcllangscreen.class")) {
            // ★ co/QclLangScreen 是 **b1.7.3 系** 的补丁 —— 只在默认 b1.7.3 系跳过 ✗
            //   （b1.8.1 报 co、1.0 报 sj、b1.9-pre5 报 co、b1.1-1245 报 co ✓ 四次同一个坑）
            return !(b18 || v10Flag || b166Flag || b19Active != null || b10b11Class != null || extraActive != null);
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
        // ★ 1.3.1：语言文件本包会重写，重复注入时不跳过会 ZipException: duplicate entry ✗
        if (low.startsWith("lang/")) {
            return true;
        }
        if (low.startsWith("font/glyph_") && low.endsWith(".png")) {
            return true;
        }
        return low.equals("lang/en_us.lang") || low.equals("lang/stats_us.lang");
    }

    /**
     * ★ 1.3.4：写语言文件 —— **先把版本覆盖 merge 进主表，再写进 jar** ✓
     *
     * 覆盖文件只列「与主表不同的键」，值同样用 Unicode 转义（反斜杠 + u + 四位十六进制）✓
     * （游戏用 {@code Properties.load} 读 lang，会自动解码这种转义，与 zh_CN.lang 原有写法一致）
     * 主表里没有的键追加到末尾（防御：万一覆盖写了新键也不会丢）✓
     */
    private static void writeLangOverlaid(Context context, String mainAsset, String overAsset,
                                          String entryName, ZipOutputStream zout) throws Exception {
        byte[] mainB = probe(context, mainAsset);
        if (mainB == null) {
            throw new java.io.FileNotFoundException("asset 缺失: " + mainAsset);
        }
        String text = new String(mainB, "UTF-8");
        if (overAsset != null) {
            byte[] overB = probe(context, overAsset);
            if (overB != null) {
                java.util.LinkedHashMap<String, String> ov = new java.util.LinkedHashMap<>();
                for (String l : new String(overB, "UTF-8").split("\n", -1)) {
                    String s = l.trim();
                    if (s.isEmpty() || s.startsWith("#")) {
                        continue;
                    }
                    int eq = s.indexOf('=');
                    if (eq > 0) {
                        ov.put(s.substring(0, eq).trim(), s.substring(eq + 1));
                    }
                }
                StringBuilder sb = new StringBuilder(text.length() + 256);
                for (String l : text.split("\n", -1)) {
                    String raw = l.endsWith("\r") ? l.substring(0, l.length() - 1) : l;
                    if (!raw.startsWith("#")) {
                        int eq = raw.indexOf('=');
                        if (eq > 0) {
                            String key = raw.substring(0, eq).trim();
                            String worth = ov.remove(key);
                            if (worth != null) {
                                sb.append(key).append('=').append(worth)
                                        .append(l.endsWith("\r") ? "\r\n" : "\n");
                                continue;
                            }
                        }
                    }
                    sb.append(l).append('\n');
                }
                for (java.util.Map.Entry<String, String> e : ov.entrySet()) {
                    sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
                }
                text = sb.toString();
            } else {
                Log.w(TAG, "语言覆盖缺失，按主表原样写入: " + overAsset);
            }
        }
        zout.putNextEntry(new ZipEntry(entryName));
        zout.write(text.getBytes("UTF-8"));
        zout.closeEntry();
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
