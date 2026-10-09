package com.qcl.launcher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import java.util.Locale;

/* loaded from: classes2.dex */
public class LocaleUtils {
    public static boolean isChinese(Context context) {
        int i = context.getSharedPreferences("lang", 0).getInt("lang", 0);
        // ★★★★★ 2026-10-11 修（用户实测「下载页里的模组一个中文也没有」）：
        //   原来这里写的是 `getSystemLocale() == Locale.CHINA` ——
        //   用 **`==` 比较 Locale 对象引用**！而 Locale.CHINA 只是个静态单例，
        //   getSystemLocale() 返回的通常是**另一个实例**（国产 ROM / 模拟器上尤其常见，
        //   形如 zh_CN_#Hans、zh-Hans-CN 等），引用不同 ⇒ **永远返回 false**
        //   ⇒ 判定成"非中文环境" ⇒ 调用方（DownloadResourceAdapter.displayTitleWithEn 等）
        //   直接短路返回英文，**压根不去查那张已加载好的 31062 条翻译表**。
        //   实测证据（logcat）：`slug=sodium title=Sodium isChinese=false trans=ok`
        //   —— 表是 ok 的，却被这个判定挡住了。
        //
        //   现在改成**按语言代码判断**（zh / zh_CN / zh_TW / zh-Hans… 都算中文），
        //   不再依赖对象引用。
        if (i == 2 || i == 3) {
            return true;    // 玩家在设置里显式选了简中 / 繁中
        }
        if (i == 1) {
            return false;   // 玩家显式选了英文
        }
        try {
            Locale sys = getSystemLocale();
            if (sys == null) {
                return false;
            }
            String lang = sys.getLanguage();
            return lang != null && "zh".equalsIgnoreCase(lang);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String getMinecraftLang(Context context) {
        int i = context.getSharedPreferences("lang", 0).getInt("lang", 0);
        if (i == 1) {
            return "en_us";
        }
        if (i == 2) {
            return "zh_cn";
        }
        if (i == 3) {
            return "zh_tw";
        }
        Locale systemLocale = getSystemLocale();
        return Locale.SIMPLIFIED_CHINESE.getLanguage().equals(systemLocale.getLanguage()) ? "zh_cn" : Locale.TRADITIONAL_CHINESE.getLanguage().equals(systemLocale.getLanguage()) ? "zh_tw" : "en_us";
    }

    /**
     * 按 MC 版本规范语言代码的大小写（对齐 FCL {@code FCLGameLauncher.fixLang}）：
     * 远古/低版本（< 1.11）要求地区码**大写**（{@code zh_CN}），1.11 起改为**小写**（{@code zh_cn}）；
     * 1.1 以前的版本不动。
     *
     * @param versionId 版本目录名，如 "1.20.6"、"b1.7.3"、"1.12.2-forge-xx"
     */
    public static String normalizeMinecraftLang(String versionId, String lang) {
        if (lang == null) {
            return null;
        }
        String[] parts = lang.split("_", 2);
        if (parts.length != 2) {
            return lang;
        }
        double v = parseMcVersion(versionId);
        if (v <= 0.0 || v < 1.1) {
            return lang;
        }
        boolean toUpper = v < 1.11;
        return parts[0] + "_" + (toUpper ? parts[1].toUpperCase(Locale.ROOT) : parts[1].toLowerCase(Locale.ROOT));
    }

    /** 从版本目录名解析版本号：1.20.6 -> 1.2006；b1.7.3 -> 1.0703（够做 1.1 / 1.11 比较）。 */
    private static double parseMcVersion(String versionId) {
        if (versionId == null) {
            return 0.0;
        }
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(versionId);
            if (m.find()) {
                double v = Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(2)) / 100.0;
                if (m.group(3) != null) {
                    v += Integer.parseInt(m.group(3)) / 10000.0;
                }
                return v;
            }
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    public static boolean isSimplifiedChinese(Context context) {
        return "zh_cn".equals(getMinecraftLang(context));
    }

    public static Context setLanguage(Context context) {
        return updateResources(context, context.getSharedPreferences("lang", 0).getInt("lang", 0));
    }

    public static void changeLanguage(Context context, int i) {
        SharedPreferences.Editor edit = context.getSharedPreferences("lang", 0).edit();
        edit.putInt("lang", i);
        edit.apply();
    }

    private static Context updateResources(Context context, int i) {
        Locale locale = getLocale(i);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        configuration.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(configuration);
    }

    private static Locale getLocale(int i) {
        if (i == 1) {
            return Locale.ENGLISH;
        }
        if (i == 2) {
            return Locale.CHINA;
        }
        if (i == 3) {
            return Locale.TAIWAN;
        }
        return getSystemLocale();
    }

    public static Locale getSystemLocale() {
        return LocaleList.getDefault().get(0);
    }
}
