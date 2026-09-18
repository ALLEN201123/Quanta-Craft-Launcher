package com.qcl.launcher.launcher.download.game;

import com.qcl.launcher.launcher.download.game.VersionManifest;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/* loaded from: classes2.dex */
public class VersionManifest {
    public LatestVersion latest;
    public Version[] versions;

    public VersionManifest(LatestVersion latestVersion, Version[] versionArr) {
        this.latest = latestVersion;
        this.versions = versionArr;
    }

    public static void sortNewestFirst(List<Version> list) {
        Collections.sort(list, new Comparator() { // from class: com.qcl.launcher.launcher.download.game.VersionManifest$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return VersionManifest.lambda$sortNewestFirst$0((VersionManifest.Version) obj, (VersionManifest.Version) obj2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$sortNewestFirst$0(Version version, Version version2) {
        if (version.releaseTime == null && version2.releaseTime == null) {
            return 0;
        }
        if (version.releaseTime == null) {
            return 1;
        }
        if (version2.releaseTime == null) {
            return -1;
        }
        return version2.releaseTime.compareTo(version.releaseTime);
    }

    /* loaded from: classes2.dex */
    public class LatestVersion {
        public String release;
        public String snapshot;

        public LatestVersion(String str, String str2) {
            this.release = str;
            this.snapshot = str2;
        }
    }

    /* loaded from: classes2.dex */
    public class Version {
        public String id;
        public Date releaseTime;
        public Date time;
        public String type;
        public String url;

        public Version(String str, String str2, String str3, Date date, Date date2) {
            this.id = str;
            this.type = str2;
            this.url = str3;
            this.time = date;
            this.releaseTime = date2;
        }
    }
}
