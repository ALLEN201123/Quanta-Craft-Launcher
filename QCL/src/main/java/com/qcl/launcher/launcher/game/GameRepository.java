package com.qcl.launcher.launcher.game;

import com.qcl.launcher.task.Task;
import com.qcl.launcher.utils.function.ExceptionalRunnable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import okhttp3.internal.platform.Platform;

/* loaded from: classes2.dex */
public interface GameRepository extends VersionProvider {
    Path getActualAssetDirectory(String str, String str2);

    Path getAssetDirectory(String str, String str2);

    AssetIndex getAssetIndex(String str, String str2) throws IOException;

    Path getAssetObject(String str, String str2, AssetObject assetObject);

    Optional<Path> getAssetObject(String str, String str2, String str3) throws IOException;

    Optional<String> getGameVersion(Version version);

    Path getIndexFile(String str, String str2);

    File getLibrariesDirectory(Version version);

    File getLibraryFile(Version version, Library library);

    Path getLoggingObject(String str, String str2, LoggingInfo loggingInfo);

    File getNativeDirectory(String str, Platform platform);

    File getRunDirectory(String str);

    @Override // com.qcl.launcher.launcher.game.VersionProvider
    Version getVersion(String str) throws VersionNotFoundException;

    int getVersionCount();

    File getVersionJar(Version version);

    File getVersionRoot(String str);

    Collection<Version> getVersions();

    @Override // com.qcl.launcher.launcher.game.VersionProvider
    boolean hasVersion(String str);

    void refreshVersions();

    boolean renameVersion(String str, String str2);

    default Version getResolvedVersion(String str) throws VersionNotFoundException {
        return getVersion(str).resolve(this);
    }

    default Version getResolvedPreservingPatchesVersion(String str) throws VersionNotFoundException {
        return getVersion(str).resolvePreservingPatches(this);
    }

    default Task<Void> refreshVersionsAsync() {
        return Task.runAsync(new ExceptionalRunnable() { // from class: com.qcl.launcher.launcher.game.GameRepository$$ExternalSyntheticLambda0
            @Override // com.qcl.launcher.utils.function.ExceptionalRunnable
            public final void run() {
                GameRepository.this.refreshVersions();
            }
        });
    }

    default Optional<String> getGameVersion(String str) throws VersionNotFoundException {
        return getGameVersion(getVersion(str));
    }

    default File getVersionJar(String str) throws VersionNotFoundException {
        return getVersionJar(getVersion(str).resolve(this));
    }

    default List<String> getClasspath(Version version) {
        ArrayList arrayList = new ArrayList();
        for (Library library : version.getLibraries()) {
            if (library.appliesToCurrentEnvironment() && !library.isNative()) {
                File libraryFile = getLibraryFile(version, library);
                if (libraryFile.exists() && libraryFile.isFile()) {
                    arrayList.add(libraryFile.getAbsolutePath());
                }
            }
        }
        return arrayList;
    }
}
