package com.qcl.launcher.launcher.download.game;

import android.content.Context;

import com.google.gson.Gson;
import com.qcl.launcher.utils.file.AssetsUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Read-only index of historical builds that are absent from Mojang's own version manifest.
 *
 * <p>The list is a snapshot of the Betacraft archive. It exists so the download page can show the
 * complete history instead of silently dropping builds, and it deliberately stays read-only:
 * those builds have no Mojang metadata, so they are listed and marked, never faked as installable.
 */
public final class LegacyVersionArchive {

    /** Marker type used by the download list so the UI can label archive-only entries. */
    public static final String TYPE_ARCHIVE = "archive";

    private static final String ASSET_PATH = "legacy_version_archive.json";

    private static List<VersionManifest.Version> cache;

    private LegacyVersionArchive() {
    }

    private static final class Entry {
        private String id;
        private String otherName;
        private long releaseTime;
        private long compileTime;
        /** Date used for ordering and display; falls back to the compile date when the archive
         *  only stored a placeholder release date. */
        private long sortTime;
        private String category;
        /** Real, downloadable source: the Betacraft archive entry for this build. */
        private String infoUrl;
        private boolean officialId;
    }

    private static final class Root {
        private String source;
        private String note;
        private int count;
        private List<Entry> entries;
    }

    /** Returns the archive entries wrapped as manifest versions. Never throws, never returns null. */
    public static synchronized List<VersionManifest.Version> entries(Context context) {
        if (cache != null) return cache;

        List<VersionManifest.Version> result = new ArrayList<>();
        try {
            String raw = AssetsUtils.readAssetsTxt(context, ASSET_PATH);
            Root root = new Gson().fromJson(raw, Root.class);
            if (root != null && root.entries != null) {
                // Inner classes of VersionManifest are non-static, so they need an enclosing instance.
                VersionManifest shell = new VersionManifest(null, new VersionManifest.Version[0]);
                for (Entry entry : root.entries) {
                    if (entry == null || entry.id == null || entry.id.isEmpty()) continue;
                    // Builds that already exist under an official id (or an official alias) stay in the
                    // normal list, so only the genuinely missing ones are added here.
                    if (entry.officialId) continue;
                    long stamp = entry.sortTime > 0 ? entry.sortTime
                            : (entry.compileTime > 0 ? entry.compileTime : entry.releaseTime);
                    Date when = stamp > 0 ? new Date(stamp) : null;
                    result.add(shell.new Version(entry.id, TYPE_ARCHIVE,
                            entry.infoUrl == null ? "" : entry.infoUrl, when, when));
                }
            }
        } catch (Exception ignored) {
            // A missing or unreadable index must never break the normal version list.
        }
        cache = result;
        return cache;
    }
}
