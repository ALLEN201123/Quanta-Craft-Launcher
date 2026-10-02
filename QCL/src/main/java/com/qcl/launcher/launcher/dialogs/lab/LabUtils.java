package com.qcl.launcher.launcher.dialogs.lab;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.qcl.launcher.launcher.MainActivity;
import com.qcl.launcher.manifest.AppManifest;
import com.qcl.launcher.utils.string.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 实验室相关工具方法集合。
 */
public final class LabUtils {

    private LabUtils() {
    }

    /**
     * 获取当前游戏目录（.minecraft）。
     * 优先从当前版本的路径反推（<游戏目录>/versions/<版本>），否则退回默认目录。
     */
    public static File getGameDir(MainActivity activity) {
        try {
            if (activity != null && activity.publicGameSetting != null
                    && !StringUtils.isBlank(activity.publicGameSetting.currentVersion)) {
                File versionDir = new File(activity.publicGameSetting.currentVersion);
                File versionsDir = versionDir.getParentFile();
                if (versionsDir != null && "versions".equals(versionsDir.getName()) && versionsDir.getParentFile() != null) {
                    return versionsDir.getParentFile();
                }
                // 若传入的已经是游戏目录本身
                if (versionDir.isDirectory() && new File(versionDir, "versions").isDirectory()) {
                    return versionDir;
                }
            }
        } catch (Throwable ignored) {
        }
        return new File(AppManifest.DEFAULT_GAME_DIR);
    }

    /**
     * 某个已装版本的数据包元信息 —— **全部读自该版本自己的 jar，离线且权威**。
     *
     * 为什么必须这么取（两条都是实测出来的）：
     *  ① `pack_format`：官方 version json（piston-meta）里**没有** `pack_version` 字段
     *     （实测 1.21.4 / 26.3 的顶层键都没有）；它只在**客户端 jar 内的 `version.json`** 里。
     *     实测 `1.20.6.jar` → `pack_version.data = 41`，而界面原先把最高档写成 18、默认 15
     *     ⇒ 用旧档位导出的数据包在新版本上**根本不会被加载**。
     *  ② 数据包目录名：**1.21 起改成单数**（`function/` `recipe/` `loot_table/` `structure/`）。
     *     实测 26.3 的 jar 是 `data/minecraft/function/`，而 1.20.6 是 `recipes/`、`loot_tables/`
     *     ⇒ 写死任一种都会对一半版本失效。
     * 所以两类信息都从 jar 里**探测**，不硬编码、不猜。
     */
    public static final class VersionPackInfo {
        public String versionName = "";
        /** 数据包 pack_format（读不到为 -1）。 */
        public int packFormat = -1;
        /** 新写法下的 max_format（26.x 等用 min_format/max_format 表示，没有 pack_format 键）。 */
        public int packFormatMax = -1;
        /** true = 该版本用新写法（pack.mcmeta 里写 min_format/max_format）；false = 旧写法（pack_format）。 */
        public boolean newPackFormatStyle;
        /** jar 内 version.json 的原文（兜底取值用，不对外暴露结构）。 */
        public String versionJson;
        /** 1.21+ 为 "function"，更早为 "functions"。 */
        public String functionDir = "functions";
        /** 1.21+ 为 "recipe"，更早为 "recipes"。 */
        public String recipeDir = "recipes";
        /** 是否成功读到 jar 里的信息。 */
        public boolean ok;
    }

    /** 读取当前选中版本的数据包元信息（见 {@link VersionPackInfo} 的说明）。 */
    public static VersionPackInfo readVersionPackInfo(MainActivity activity) {
        File versionDir = null;
        try {
            if (activity != null && activity.publicGameSetting != null
                    && !StringUtils.isBlank(activity.publicGameSetting.currentVersion)) {
                versionDir = new File(activity.publicGameSetting.currentVersion);
            }
        } catch (Throwable ignored) {
        }
        return readVersionPackInfo(versionDir);
    }

    /** 读取指定版本目录的数据包元信息。 */
    public static VersionPackInfo readVersionPackInfo(File versionDir) {
        VersionPackInfo info = new VersionPackInfo();
        if (versionDir == null || !versionDir.isDirectory()) {
            return info;
        }
        info.versionName = versionDir.getName();
        File jar = new File(versionDir, info.versionName + ".jar");
        if (!jar.isFile()) {
            File[] jars = versionDir.listFiles(new java.io.FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            });
            if (jars != null && jars.length > 0) {
                jar = jars[0];
            }
        }
        if (!jar.isFile()) {
            return info;
        }
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar)) {
            java.util.zip.ZipEntry vj = zip.getEntry("version.json");
            if (vj != null) {
                java.io.InputStream in = zip.getInputStream(vj);
                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                in.close();
                // 只挖 pack_version 相关字段，不引入 JSON 解析依赖
                String s = new String(bos.toByteArray(), "UTF-8");
                info.versionJson = s;
                int i = s.indexOf("pack_version");
                if (i >= 0) {
                    int j = s.indexOf("\"data\"", i);
                    if (j >= 0) {
                        int k = s.indexOf(':', j);
                        int e = k + 1;
                        while (e < s.length() && Character.isWhitespace(s.charAt(e))) {
                            e++;
                        }
                        int st = e;
                        while (e < s.length() && Character.isDigit(s.charAt(e))) {
                            e++;
                        }
                        if (e > st) {
                            info.packFormat = Integer.parseInt(s.substring(st, e));
                        }
                    }
                }
            }
            // ★★★ 最权威的来源：**游戏自带的 `data/minecraft/datapacks/*/pack.mcmeta`**
            //   它同时给出「该版本正确的 pack_format 数字」和「该版本使用哪种写法」：
            //     · 1.20.6 → {"pack": {"pack_format": 41}}
            //     · 26.3   → {"pack": {"min_format": 121, "max_format": 121}}   ← 新写法，没有 pack_format 键
            //   所以先扫它；扫不到再退回 version.json 的 pack_version。
            java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries();
            boolean sawFunction = false, sawFunctions = false, sawRecipe = false, sawRecipes = false;
            boolean mcmetaChecked = false;
            while (en.hasMoreElements()) {
                java.util.zip.ZipEntry ze = en.nextElement();
                String name = ze.getName();
                if (!mcmetaChecked && name.startsWith("data/minecraft/datapacks/")
                        && name.endsWith("/pack.mcmeta") && !ze.isDirectory()) {
                    mcmetaChecked = true;
                    try {
                        java.io.InputStream in2 = zip.getInputStream(ze);
                        java.io.ByteArrayOutputStream b2 = new java.io.ByteArrayOutputStream();
                        byte[] bf = new byte[2048];
                        int r;
                        while ((r = in2.read(bf)) > 0) {
                            b2.write(bf, 0, r);
                        }
                        in2.close();
                        String mc = new String(b2.toByteArray(), "UTF-8");
                        int pf = numberAfter(mc, "\"pack_format\"");
                        if (pf > 0) {
                            info.packFormat = pf;
                            info.newPackFormatStyle = false;
                        } else {
                            int mn = numberAfter(mc, "\"min_format\"");
                            int mx = numberAfter(mc, "\"max_format\"");
                            if (mn > 0) {
                                info.packFormat = mn;
                                info.packFormatMax = mx > 0 ? mx : mn;
                                info.newPackFormatStyle = true;
                            }
                        }
                    } catch (Throwable ignored2) {
                    }
                }
                if (!name.startsWith("data/minecraft/")) {
                    continue;
                }
                int p = name.indexOf('/', 15);
                if (p < 0) {
                    continue;
                }
                String seg = name.substring(15, p);
                if ("function".equals(seg)) {
                    sawFunction = true;
                } else if ("functions".equals(seg)) {
                    sawFunctions = true;
                } else if ("recipe".equals(seg)) {
                    sawRecipe = true;
                } else if ("recipes".equals(seg)) {
                    sawRecipes = true;
                }
            }
            // 兜底：自带 mcmeta 没扫到，就用 version.json 的 pack_version
            //   （旧 schema: {"resource":32,"data":41}；新 schema: {"resource_major":97,"data_major":121}）
            if (info.packFormat <= 0 && info.versionJson != null) {
                int d = numberAfter(info.versionJson, "\"data\"");
                if (d <= 0) {
                    d = numberAfter(info.versionJson, "\"data_major\"");
                    if (d > 0) {
                        info.newPackFormatStyle = true;
                        info.packFormatMax = d;
                    }
                }
                if (d > 0) {
                    info.packFormat = d;
                }
            }
            if (sawFunction) {
                info.functionDir = "function";
            } else if (sawFunctions) {
                info.functionDir = "functions";
            }
            if (sawRecipe) {
                info.recipeDir = "recipe";
            } else if (sawRecipes) {
                info.recipeDir = "recipes";
            }
            info.ok = true;
        } catch (Throwable ignored) {
        }
        return info;
    }

    /** 从文本里取 `"key": N` 的 N；取不到返回 -1。 */
    private static int numberAfter(String text, String key) {
        if (text == null || key == null) {
            return -1;
        }
        int i = text.indexOf(key);
        if (i < 0) {
            return -1;
        }
        int e = i + key.length();
        while (e < text.length() && text.charAt(e) != ':') {
            e++;
        }
        e++;
        while (e < text.length() && Character.isWhitespace(text.charAt(e))) {
            e++;
        }
        int st = e;
        while (e < text.length() && Character.isDigit(text.charAt(e))) {
            e++;
        }
        if (e <= st) {
            return -1;
        }
        try {
            return Integer.parseInt(text.substring(st, e));
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 投影目录 <游戏目录>/schematics，确保存在。 */
    public static File getSchematicsDir(MainActivity activity) {
        File dir = new File(getGameDir(activity), "schematics");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** 数据包目录 <游戏目录>/datapacks，确保存在。 */
    public static File getDatapacksDir(MainActivity activity) {
        File dir = new File(getGameDir(activity), "datapacks");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /** 将弹窗设置为较大的面板（高度约屏幕 85%），内容自行滚动。 */
    public static void setupDialogWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics dm = dialog.getContext().getResources().getDisplayMetrics();
        int width = (int) Math.min(dm.widthPixels * 0.92f, dm.density * 420f);
        window.setLayout(width, (int) (dm.heightPixels * 0.85f));
    }

    /** 将弹窗设置为自适应高度，宽度受限。 */
    public static void setupDialogWindowWrap(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        DisplayMetrics dm = dialog.getContext().getResources().getDisplayMetrics();
        int width = (int) Math.min(dm.widthPixels * 0.92f, dm.density * 420f);
        window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static void toast(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("qcl_lab", text));
            }
        } catch (Throwable ignored) {
        }
    }

    /** 人类可读的文件大小。 */
    public static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0f);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0f * 1024.0f));
        }
        return String.format(Locale.US, "%.1f GB", bytes / (1024.0f * 1024.0f * 1024.0f));
    }

    public static String formatTime(long millis) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date(millis));
        } catch (Throwable ignored) {
            return String.valueOf(millis);
        }
    }

    /** 数据包命名空间：只保留小写字母、数字、下划线、横线和点。 */
    public static String sanitizeNamespace(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toLowerCase(raw.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** 通用文件名过滤：去掉路径分隔符等非法字符。 */
    public static String sanitizeFileName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "_").replace("/", "_").replace(":", "_")
                .replace("*", "_").replace("?", "_").replace("\"", "_")
                .replace("<", "_").replace(">", "_").replace("|", "_").trim();
    }
}