package com.qcl.launcher.utils.activity;

import android.content.Context;
import android.webkit.CookieManager;
import com.qcl.launcher.utils.file.FileUtils;

/* loaded from: classes2.dex */
public class ActivityUtils {
    public static void clearCacheFiles(Context context) {
    }

    public static void clearWebViewCache(Context context) {
        FileUtils.deleteDirectory(context.getDir("webview", 0).getAbsolutePath());
        CookieManager.getInstance().removeAllCookies(null);
    }
}
