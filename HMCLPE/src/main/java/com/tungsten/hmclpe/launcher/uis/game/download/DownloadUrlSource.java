package com.tungsten.hmclpe.launcher.uis.game.download;

import android.content.Context;
import com.tungsten.hmclpe.launcher.setting.launcher.child.SourceSetting;
import java.net.URI;

public class DownloadUrlSource {
    public static final int DOWNLOAD_URL_SOURCE_OFFICIAL = 0;
    public static final int DOWNLOAD_URL_SOURCE_BMCLAPI = 1;
    // Preserve persisted source index 2; it now uses BMCLAPI, not the retired mirror.
    public static final int DOWNLOAD_URL_SOURCE_BMCLAPI_COMPAT = 2;
    public static final int VERSION_MANIFEST = 0, VERSION_JSON = 1, VERSION_JAR = 2,
            ASSETS_INDEX_JSON = 3, ASSETS_OBJ = 4, LIBRARIES = 5, FORGE_LIBRARIES = 6;
    public static final String BMCLAPI_BASE = "https://bmclapi2.bangbang93.com";
    public static final String[] OFFICIAL_URLS = {
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",
            "https://piston-meta.mojang.com", "https://piston-data.mojang.com",
            "https://piston-meta.mojang.com", "https://resources.download.minecraft.net",
            "https://libraries.minecraft.net", "https://maven.minecraftforge.net"
    };
    public static final String[] BMCLAPI_URLS = {
            BMCLAPI_BASE + "/mc/game/version_manifest_v2.json", BMCLAPI_BASE, BMCLAPI_BASE,
            BMCLAPI_BASE, BMCLAPI_BASE + "/assets", BMCLAPI_BASE + "/maven", BMCLAPI_BASE + "/maven"
    };

    public static String replaceSubUrl(String url, int source, int type) {
        if (url == null || source == DOWNLOAD_URL_SOURCE_OFFICIAL) return url;
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return url;
            String prefix;
            if ("piston-meta.mojang.com".equalsIgnoreCase(host)
                    || "piston-data.mojang.com".equalsIgnoreCase(host)
                    || "launchermeta.mojang.com".equalsIgnoreCase(host)
                    || "launcher.mojang.com".equalsIgnoreCase(host)) prefix = BMCLAPI_BASE;
            else if ("resources.download.minecraft.net".equalsIgnoreCase(host)) prefix = BMCLAPI_BASE + "/assets";
            else if ("libraries.minecraft.net".equalsIgnoreCase(host)
                    || "maven.minecraftforge.net".equalsIgnoreCase(host)
                    || "files.minecraftforge.net".equalsIgnoreCase(host)) prefix = BMCLAPI_BASE + "/maven";
            else return url; // Mod-specific repositories must not be truncated or rewritten.
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (prefix.endsWith("/maven") && path.startsWith("/maven/")) path = path.substring(6);
            return prefix + path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
        } catch (Exception ignored) {
            return url;
        }
    }

    public static String getSubUrl(int source, int type) {
        return (source == DOWNLOAD_URL_SOURCE_OFFICIAL ? OFFICIAL_URLS : BMCLAPI_URLS)[type];
    }

    public static void getBalancedSource(Context context) {
        // No ICMP-based pseudo benchmark: automatic mirror mode uses BMCLAPI.
    }

    public static int getSource(SourceSetting setting) {
        if (setting == null) return DOWNLOAD_URL_SOURCE_BMCLAPI;
        if (setting.autoSelect) return setting.autoSourceType == 0 ? DOWNLOAD_URL_SOURCE_OFFICIAL : DOWNLOAD_URL_SOURCE_BMCLAPI;
        return setting.fixSourceType == 0 ? DOWNLOAD_URL_SOURCE_OFFICIAL : DOWNLOAD_URL_SOURCE_BMCLAPI;
    }
}
