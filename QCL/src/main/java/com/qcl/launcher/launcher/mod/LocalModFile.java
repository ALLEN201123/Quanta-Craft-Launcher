package com.qcl.launcher.launcher.mod;

import com.qcl.launcher.launcher.mod.RemoteMod;
import com.qcl.launcher.utils.Logging;
import com.qcl.launcher.utils.io.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

/* loaded from: classes2.dex */
public final class LocalModFile implements Comparable<LocalModFile> {
    private boolean active;
    private final String authors;
    private final Description description;
    private Path file;
    private final String fileName;
    private final String gameVersion;
    private final String logoPath;
    private final LocalMod mod;
    private final ModManager modManager;
    private final String name;
    private final String url;
    private final String version;

    public LocalModFile(ModManager modManager, LocalMod localMod, Path path, String str, Description description) {
        this(modManager, localMod, path, str, description, "", "", "", "", "");
    }

    public LocalModFile(ModManager modManager, LocalMod localMod, Path path, String str, Description description, String str2, String str3, String str4, String str5, String str6) {
        this.modManager = modManager;
        this.mod = localMod;
        this.file = path;
        this.name = str;
        this.description = description;
        this.authors = str2;
        this.version = str3;
        this.gameVersion = str4;
        this.url = str5;
        this.logoPath = str6;
        this.active = !modManager.isDisabled(path);
        this.fileName = FileUtils.getNameWithoutExtension(ModManager.getModName(path));
        if (isOld()) {
            localMod.getOldFiles().add(this);
        } else {
            localMod.getFiles().add(this);
        }
    }

    public ModManager getModManager() {
        return this.modManager;
    }

    public LocalMod getMod() {
        return this.mod;
    }

    public Path getFile() {
        return this.file;
    }

    public ModLoaderType getModLoaderType() {
        return this.mod.getModLoaderType();
    }

    public String getId() {
        return this.mod.getId();
    }

    public String getName() {
        return this.name;
    }

    public Description getDescription() {
        return this.description;
    }

    public String getAuthors() {
        return this.authors;
    }

    public String getVersion() {
        return this.version;
    }

    public String getGameVersion() {
        return this.gameVersion;
    }

    public String getUrl() {
        return this.url;
    }

    public String getLogoPath() {
        return this.logoPath;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean z) throws IOException {
        this.active = z;
        Path absolutePath = this.file.toAbsolutePath();
        try {
            if (z) {
                this.file = this.modManager.enableMod(absolutePath);
            } else {
                this.file = this.modManager.disableMod(absolutePath);
            }
        } catch (IOException e) {
            Logging.LOG.log(Level.SEVERE, "Unable to invert state of mod file " + absolutePath, (Throwable) e);
        }
    }

    public String getFileName() {
        return this.fileName;
    }

    public boolean isOld() {
        return this.modManager.isOld(this.file);
    }

    public void setOld(boolean z) throws IOException {
        this.file = this.modManager.setOld(this, z);
        if (z) {
            this.mod.getFiles().remove(this);
            this.mod.getOldFiles().add(this);
        } else {
            this.mod.getOldFiles().remove(this);
            this.mod.getFiles().add(this);
        }
    }

    public ModUpdate checkUpdates(final String str, RemoteModRepository remoteModRepository) throws IOException {
        final Optional<RemoteMod.Version> remoteVersionByLocalFile = remoteModRepository.getRemoteVersionByLocalFile(this, this.file);
        if (!remoteVersionByLocalFile.isPresent()) {
            return null;
        }
        List list = (List) remoteModRepository.getRemoteVersionsById(remoteVersionByLocalFile.get().getModid()).filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.LocalModFile$$ExternalSyntheticLambda2
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                boolean contains;
                contains = ((RemoteMod.Version) obj).getGameVersions().contains(str);
                return contains;
            }
        }).filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.LocalModFile$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return LocalModFile.this.m457lambda$checkUpdates$1$comqcllauncherlaunchermodLocalModFile((RemoteMod.Version) obj);
            }
        }).filter(new Predicate() { // from class: com.qcl.launcher.launcher.mod.LocalModFile$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return LocalModFile.lambda$checkUpdates$2(remoteVersionByLocalFile, (RemoteMod.Version) obj);
            }
        }).sorted(Comparator.comparing(LocalModFile$$ExternalSyntheticLambda0.INSTANCE).reversed()).collect(Collectors.toList());
        if (list.isEmpty()) {
            return null;
        }
        return new ModUpdate(this, remoteVersionByLocalFile.get(), list);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: lambda$checkUpdates$1$com-qcl-launcher-launcher-mod-LocalModFile, reason: not valid java name */
    public /* synthetic */ boolean m457lambda$checkUpdates$1$comqcllauncherlaunchermodLocalModFile(RemoteMod.Version version) {
        return version.getLoaders().contains(getModLoaderType());
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$checkUpdates$2(Optional optional, RemoteMod.Version version) {
        return version.getDatePublished().compareTo(((RemoteMod.Version) optional.get()).getDatePublished()) > 0;
    }

    @Override // java.lang.Comparable
    public int compareTo(LocalModFile localModFile) {
        return getFileName().compareTo(localModFile.getFileName());
    }

    public boolean equals(Object obj) {
        return (obj instanceof LocalModFile) && Objects.equals(getFileName(), ((LocalModFile) obj).getFileName());
    }

    public int hashCode() {
        return Objects.hash(getFileName());
    }

    /* loaded from: classes2.dex */
    public static class ModUpdate {
        private final List<RemoteMod.Version> candidates;
        private final RemoteMod.Version currentVersion;
        private final LocalModFile localModFile;

        public ModUpdate(LocalModFile localModFile, RemoteMod.Version version, List<RemoteMod.Version> list) {
            this.localModFile = localModFile;
            this.currentVersion = version;
            this.candidates = list;
        }

        public LocalModFile getLocalMod() {
            return this.localModFile;
        }

        public RemoteMod.Version getCurrentVersion() {
            return this.currentVersion;
        }

        public List<RemoteMod.Version> getCandidates() {
            return this.candidates;
        }
    }

    /* loaded from: classes2.dex */
    public static class Description {
        private final List<Part> parts;

        public Description(String str) {
            ArrayList arrayList = new ArrayList();
            this.parts = arrayList;
            arrayList.add(new Part(str, "black"));
        }

        public Description(List<Part> list) {
            this.parts = list;
        }

        public List<Part> getParts() {
            return this.parts;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            Iterator<Part> it = this.parts.iterator();
            while (it.hasNext()) {
                sb.append(it.next().text);
            }
            return sb.toString();
        }

        /* loaded from: classes2.dex */
        public static class Part {
            private final String color;
            private final String text;

            public Part(String str) {
                this(str, "");
            }

            public Part(String str, String str2) {
                this.text = (String) Objects.requireNonNull(str);
                this.color = (String) Objects.requireNonNull(str2);
            }

            public String getText() {
                return this.text;
            }

            public String getColor() {
                return this.color;
            }
        }
    }
}
