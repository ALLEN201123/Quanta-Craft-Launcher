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
        return i == 2 || (i == 0 && getSystemLocale() == Locale.CHINA);
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
