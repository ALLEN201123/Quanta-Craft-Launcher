/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.content.Context
 */
package com.qcl.launcher.launcher.uis.game.download;

import android.content.Context;
import com.qcl.launcher.launcher.setting.launcher.child.SourceSetting;
import java.net.URI;

public class DownloadUrlSource {
    public static final int DOWNLOAD_URL_SOURCE_OFFICIAL = 0;
    public static final int DOWNLOAD_URL_SOURCE_BMCLAPI = 1;
    public static final int DOWNLOAD_URL_SOURCE_BMCLAPI_COMPAT = 2;
    public static final int VERSION_MANIFEST = 0;
    public static final int VERSION_JSON = 1;
    public static final int VERSION_JAR = 2;
    public static final int ASSETS_INDEX_JSON = 3;
    public static final int ASSETS_OBJ = 4;
    public static final int LIBRARIES = 5;
    public static final int FORGE_LIBRARIES = 6;
    public static final String BMCLAPI_BASE = "https://bmclapi2.bangbang93.com";
    public static final String[] OFFICIAL_URLS = new String[]{"https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", "https://piston-meta.mojang.com", "https://piston-data.mojang.com", "https://piston-meta.mojang.com", "https://resources.download.minecraft.net", "https://libraries.minecraft.net", "https://maven.minecraftforge.net"};
    public static final String[] BMCLAPI_URLS = new String[]{"https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json", "https://bmclapi2.bangbang93.com", "https://bmclapi2.bangbang93.com", "https://bmclapi2.bangbang93.com", "https://bmclapi2.bangbang93.com/assets", "https://bmclapi2.bangbang93.com/maven", "https://bmclapi2.bangbang93.com/maven"};

    public static String replaceSubUrl(String url, int source, int type) {
        if (url == null || source == 0) {
            return url;
        }
        try {
            String path;
            String prefix;
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return url;
            }
            if ("piston-meta.mojang.com".equalsIgnoreCase(host) || "piston-data.mojang.com".equalsIgnoreCase(host) || "launchermeta.mojang.com".equalsIgnoreCase(host) || "launcher.mojang.com".equalsIgnoreCase(host)) {
                prefix = BMCLAPI_BASE;
            } else if ("resources.download.minecraft.net".equalsIgnoreCase(host)) {
                prefix = "https://bmclapi2.bangbang93.com/assets";
            } else if ("libraries.minecraft.net".equalsIgnoreCase(host) || "maven.minecraftforge.net".equalsIgnoreCase(host) || "files.minecraftforge.net".equalsIgnoreCase(host)) {
                prefix = "https://bmclapi2.bangbang93.com/maven";
            } else {
                return url;
            }
            String string2 = path = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (prefix.endsWith("/maven") && path.startsWith("/maven/")) {
                path = path.substring(6);
            }
            return prefix + path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        }
        catch (Exception ignored) {
            return url;
        }
    }

    public static String getSubUrl(int source, int type) {
        return (source == 0 ? OFFICIAL_URLS : BMCLAPI_URLS)[type];
    }

    public static void getBalancedSource(Context context) {
    }

    public static int getSource(SourceSetting setting) {
        if (setting == null) {
            return 1;
        }
        if (setting.autoSelect) {
            return setting.autoSourceType == 0 ? 0 : 1;
        }
        return setting.fixSourceType == 0 ? 0 : 1;
    }
}

